package com.example.demo.services;

import com.example.demo.entities.Albums;
import com.example.demo.entities.Artists;
import com.example.demo.entities.Genres;
import com.example.demo.entities.Songs;
import com.example.demo.repositories.AlbumRepository;
import com.example.demo.repositories.ArtistRepository;
import com.example.demo.repositories.GenreRepository;
import com.example.demo.repositories.SongRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class MusicImportService {

    private static final Logger logger = LoggerFactory.getLogger(MusicImportService.class);
    
    private static final String DEEZER_CHART_URL = "https://api.deezer.com/chart/0/tracks";
    private static final String DEEZER_SEARCH_URL = "https://api.deezer.com/search?q={query}&limit=100";
    private static final String VIETNAMESE_GENRE = "Nhạc Việt";
    
    private static final Set<String> VIETNAMESE_KEYWORDS = new HashSet<>(Arrays.asList(
        "việt", "viet", "vn", "hanoi", "hồ chí minh", "sài gòn", "saigon",
        "nhạc trẻ", "nhac tre", "v-pop", "vpop", "vietnam", "việt nam",
        "hà nội", "đà nẵng", "huế", "cần thơ"
    ));

    @Value("${app.audio.enable-download:true}")
    private boolean enableAudioDownload;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ArtistRepository artistRepo;

    @Autowired
    private AlbumRepository albumRepo;

    @Autowired
    private SongRepository songRepo;

    @Autowired
    private GenreRepository genreRepo;

    @Autowired
    private AudioStorageService audioStorageService;

    /**
     * Import songs từ Deezer chart (top global) với lọc nhạc Việt
     */
    public int importSongsFromDeezerChart() {
        logger.info("Bắt đầu import songs từ Deezer Chart với lọc nhạc Việt");
        return importSongsWithFilter(DEEZER_CHART_URL);
    }

    /**
     * Import songs từ Deezer search với query cụ thể và lọc nhạc Việt - PHIÊN BẢN DÙNG ALTERNATIVE AUDIO
     */
    public int importSongsFromDeezerSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query không được để trống");
        }
        
        logger.info("🎵 Bắt đầu import songs từ Deezer Search với query: {}", query);
        
        // Thêm từ khóa tiếng Việt để tăng độ chính xác
        String enhancedQuery = enhanceQueryWithVietnameseKeywords(query);
        String url = DEEZER_SEARCH_URL.replace("{query}", encodeQuery(enhancedQuery));
        
        return importSongsWithFilter(url);
    }

    @SuppressWarnings("unchecked")
    private int importSongsWithFilter(String url) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getBody() == null || !response.getBody().containsKey("data")) {
                logger.warn("Không có dữ liệu từ API Deezer");
                return 0;
            }

            List<Map<String, Object>> tracks = (List<Map<String, Object>>) response.getBody().get("data");
            
            if (tracks == null || tracks.isEmpty()) {
                logger.info("Không tìm thấy tracks nào");
                return 0;
            }

            int count = 0;
            int vietnameseCount = 0;
            int foreignCount = 0;
            
            Genres vnGenre = getOrCreateVietnameseGenre();

            for (Map<String, Object> track : tracks) {
                try {
                    // 🔥 Lọc chỉ lấy nhạc Việt
                    if (isVietnameseMusic(track)) {
                        vietnameseCount++;
                        if (importSingleSong(track, vnGenre)) {
                            count++;
                        }
                    } else {
                        foreignCount++;
                        String artistName = getArtistName(track);
                        String title = getTrackTitle(track);
                        logger.debug("Bỏ qua bài hát nước ngoài: {} - {}", title, artistName);
                    }
                } catch (Exception e) {
                    logger.error("Lỗi khi xử lý song: {}", track.get("title"), e);
                }
            }

            logger.info("Kết quả import: {} bài nhạc Việt được import ({} bài Việt được phát hiện, {} bài nước ngoài bị bỏ qua)", 
                count, vietnameseCount, foreignCount);
            return count;

        } catch (Exception e) {
            logger.error("Lỗi khi gọi API Deezer: {}", url, e);
            throw new RuntimeException("Không thể kết nối đến Deezer API: " + e.getMessage());
        }
    }

    /**
     * PHƯƠNG THỨC LỌC QUAN TRỌNG: Xác định bài hát có phải nhạc Việt không
     */
    private boolean isVietnameseMusic(Map<String, Object> track) {
        try {
            if (!isValidTrack(track)) {
                return false;
            }

            Map<String, Object> artistData = (Map<String, Object>) track.get("artist");
            if (artistData == null) return false;

            String artistName = ((String) artistData.get("name")).toLowerCase();
            String trackTitle = ((String) track.get("title")).toLowerCase();

            // 1. Kiểm tra từ khóa tiếng Việt trong tên nghệ sĩ
            boolean hasVietnameseArtistName = containsVietnameseKeywords(artistName);
            
            // 2. Kiểm tra từ khóa tiếng Việt trong tên bài hát
            boolean hasVietnameseTitle = containsVietnameseKeywords(trackTitle);
            
            // 3. Kiểm tra tên nghệ sĩ Việt Nam phổ biến
            boolean isKnownVietnameseArtist = isKnownVietnameseArtist(artistName);
            
            // 4. Kiểm tra ký tự tiếng Việt có dấu
            boolean hasVietnameseCharacters = containsVietnameseCharacters(artistName) || 
                                            containsVietnameseCharacters(trackTitle);

            // 5. Kiểm tra trong metadata (nếu có)
            boolean hasVietnameseMetadata = checkVietnameseMetadata(track);

            // Tính điểm: Chỉ import nếu thỏa ít nhất 2 điều kiện
            int matchCount = 0;
            if (hasVietnameseArtistName) matchCount++;
            if (hasVietnameseTitle) matchCount++;
            if (isKnownVietnameseArtist) matchCount++;
            if (hasVietnameseCharacters) matchCount++;
            if (hasVietnameseMetadata) matchCount++;

            boolean isVietnamese = matchCount >= 2;

            if (isVietnamese) {
                logger.debug("✓ Xác định là nhạc Việt: {} - {} (điểm: {})", 
                    track.get("title"), artistData.get("name"), matchCount);
            }

            return isVietnamese;

        } catch (Exception e) {
            logger.warn("Lỗi khi kiểm tra nhạc Việt: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Kiểm tra từ khóa tiếng Việt
     */
    private boolean containsVietnameseKeywords(String text) {
        if (text == null) return false;
        
        String lowerText = text.toLowerCase();
        return VIETNAMESE_KEYWORDS.stream().anyMatch(lowerText::contains) ||
               containsVietnameseArtistNames(lowerText);
    }

    /**
     * Kiểm tra ký tự tiếng Việt có dấu
     */
    private boolean containsVietnameseCharacters(String text) {
        if (text == null) return false;
        // Regex kiểm tra ký tự tiếng Việt có dấu
        return text.matches(".*[áàảãạăắằẳẵặâấầẩẫậéèẻẽẹêếềểễệíìỉĩịóòỏõọôốồổỗộơớờởỡợúùủũụưứừửữựýỳỷỹỵđ].*");
    }

    /**
     * Danh sách nghệ sĩ Việt Nam phổ biến
     */
    private boolean containsVietnameseArtistNames(String artistName) {
        Set<String> vietnameseArtists = new HashSet<>(Arrays.asList(
            "soobin", "sơn tùng", "sontung", "đen", "den", "jack", "min", "erik", 
            "justatee", "bigdaddy", "emy", "karik", "bich phuong", "bích phương",
            "dam vinh hung", "đàm vĩnh hưng", "my tam", "mỹ tâm", "noo phuoc thinh",
            "noo", "phương ly", "phuong ly", "tuan hung", "tuấn hưng", "binz",
            "suboi", "da lab", "dalab", "wowy", "rhymastic", "liem", "tiem", "andiez",
            "mono", "amee", "hoàng dũng", "hoang dung", "hoà minzy", "hoa minzy",
            "đức phúc", "duc phuc", "bảo anh", "bao anh", "trúc nhân", "truc nhan",
            "trịnh thăng bình", "trinh thang binh", "mr.siro", "siro", "châu khải phong"
        ));
        
        return vietnameseArtists.stream().anyMatch(artistName::contains);
    }

    /**
     * Kiểm tra nghệ sĩ Việt Nam nổi tiếng
     */
    private boolean isKnownVietnameseArtist(String artistName) {
        Set<String> famousVietnameseArtists = new HashSet<>(Arrays.asList(
            "soobin hoang son", "soobin", "sơn tùng mtp", "sơn tùng", 
            "den vau", "đen", "jack j97", "jack", "min", "erik", 
            "justatee", "bigdaddy", "emy", "karik", "bích phương",
            "đàm vĩnh hưng", "mỹ tâm", "noo phước thịnh", "tuấn hưng",
            "binz", "suboi", "da lab", "wowy", "rhymastic"
        ));
        
        return famousVietnameseArtists.contains(artistName.toLowerCase());
    }

    /**
     * Kiểm tra metadata (nếu có thông tin quốc gia)
     */
    private boolean checkVietnameseMetadata(Map<String, Object> track) {
        try {
            // Kiểm tra trong artist data
            Map<String, Object> artistData = (Map<String, Object>) track.get("artist");
            if (artistData != null) {
                // Deezer có thể có trường country trong artist
                if (artistData.containsKey("country") && 
                    "VN".equalsIgnoreCase((String) artistData.get("country"))) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Tăng cường query với từ khóa tiếng Việt
     */
    private String enhanceQueryWithVietnameseKeywords(String originalQuery) {
        String enhanced = originalQuery.toLowerCase();
        
        // Nếu query chưa có từ khóa tiếng Việt, thêm vào
        if (!containsVietnameseKeywords(originalQuery) && 
            !containsVietnameseCharacters(originalQuery)) {
            enhanced += " vietnamese vpop nhac viet";
            logger.info("Tăng cường query từ '{}' thành '{}'", originalQuery, enhanced);
        }
        
        return enhanced;
    }

    /**
     * Encode query để URL an toàn
     */
    private String encodeQuery(String query) {
        try {
            return URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            return URLEncoder.encode(query, StandardCharsets.UTF_8);
        }
    }

    private boolean importSingleSong(Map<String, Object> track, Genres vnGenre) {
        // Kiểm tra dữ liệu track
        if (!isValidTrack(track)) {
            return false;
        }

        // Xử lý Artist
        Map<String, Object> artistData = (Map<String, Object>) track.get("artist");
        Artists artist = getOrCreateArtist(artistData);

        // Xử lý Album
        Map<String, Object> albumData = (Map<String, Object>) track.get("album");
        Albums album = getOrCreateAlbum(albumData, artist);

        // Kiểm tra song trùng lặp
        String title = (String) track.get("title");
        if (songRepo.existsByTitleAndArtists_Name(title, artist.getName())) {
            logger.info("Song đã tồn tại: {} - {}", title, artist.getName());
            return false;
        }

        // Tạo mới Song
        Songs song = createSong(track, artist, album, vnGenre);
        songRepo.save(song);
        
        logger.debug("Đã import song: {} - {}", title, artist.getName());
        return true;
    }

    private boolean isValidTrack(Map<String, Object> track) {
        return track != null && 
               track.containsKey("title") && 
               track.containsKey("artist") && 
               track.containsKey("album");
    }

    private Artists getOrCreateArtist(Map<String, Object> artistData) {
        String artistName = (String) artistData.get("name");
        Optional<Artists> existingArtist = artistRepo.findByName(artistName);
        
        if (existingArtist.isPresent()) {
            return existingArtist.get();
        }

        Artists artist = new Artists();
        artist.setName(artistName);
        artist.setImageUrl((String) artistData.get("picture_medium"));
        
        // ✅ SỬA LỖI: SET CREATED_AT CHO ARTIST
        setCreatedAtForEntity(artist);
        
        return artistRepo.save(artist);
    }

    private Albums getOrCreateAlbum(Map<String, Object> albumData, Artists artist) {
        String albumTitle = (String) albumData.get("title");
        Optional<Albums> existingAlbum = albumRepo.findByTitle(albumTitle);
        
        if (existingAlbum.isPresent()) {
            return existingAlbum.get();
        }

        Albums album = new Albums();
        album.setTitle(albumTitle);
        album.setCoverUrl((String) albumData.get("cover_medium"));
        album.setArtists(artist);
        
        // ✅ SỬA LỖI: SET CREATED_AT CHO ALBUM
        setCreatedAtForEntity(album);
        
        return albumRepo.save(album);
    }

    /**
     * ✅ PHƯƠNG THỨC MỚI: Set created_at cho entity sử dụng reflection
     */
    private void setCreatedAtForEntity(Object entity) {
        try {
            // Sử dụng reflection để set created_at
            java.lang.reflect.Method setCreatedAt = entity.getClass().getMethod("setCreatedAt", Date.class);
            setCreatedAt.invoke(entity, new Date());
        } catch (NoSuchMethodException e) {
            logger.debug("Entity {} không có phương thức setCreatedAt", entity.getClass().getSimpleName());
        } catch (Exception e) {
            logger.warn("Không thể set created_at cho {}: {}", entity.getClass().getSimpleName(), e.getMessage());
        }
    }

    private Songs createSong(Map<String, Object> track, Artists artist, Albums album, Genres genre) {
        Songs song = new Songs();
        song.setTitle((String) track.get("title"));
        
        // ✅ KIỂM TRA DURATION TỪ DEEZER
        Integer duration = (Integer) track.get("duration");
        logger.info("🎵 Song: {} - Duration từ Deezer: {} seconds", 
                   track.get("title"), duration);
        
        song.setDuration(duration);

        // XỬ LÝ AUDIO FILE - PHIÊN BẢN DÙNG ALTERNATIVE AUDIO
        String previewUrl = (String) track.get("preview");
        String filePath = processAudioFile(previewUrl, track);
        song.setFilePath(filePath);

        // Xử lý hình ảnh
        processImageUrl(track, song);

        // Quan hệ Album
        song.setAlbums(album);

        // Quan hệ Artist (ManyToMany)
        try {
            song.getartists().add(artist);
        } catch (Exception e) {
            try {
                song.getartists().add(artist);
            } catch (Exception e2) {
                logger.warn("Không thể thêm artist vào song");
            }
        }

        // Genre (ManyToMany)
        try {
            song.getGenreses().add(genre);
        } catch (Exception e) {
            try {
                song.getGenreses().add(genre);
            } catch (Exception e2) {
                logger.warn("Không thể thêm genre vào song");
            }
        }

        // ✅ SET CREATED_AT
        setCreatedAtForEntity(song);

        logger.info("✅ Đã tạo song: {} - {} (duration: {}s, file: {})", 
                   song.getTitle(), artist.getName(), song.getDuration(), filePath);
        
        return song;
    }

    /**
     * Xử lý file audio: sử dụng alternative audio thay vì Deezer preview
     */
    private String processAudioFile(String previewUrl, Map<String, Object> track) {
        try {
            // Kiểm tra enable download
            if (!enableAudioDownload) {
                logger.info("🔕 Chế độ tải audio đã TẮT, sử dụng alternative audio");
                return generateAlternativeAudioUrl(track);
            }

            // Kiểm tra URL preview
            if (previewUrl == null || previewUrl.trim().isEmpty() || !previewUrl.startsWith("http")) {
                logger.warn("⚠️ URL preview không hợp lệ: {}", previewUrl);
                return generateAlternativeAudioUrl(track);
            }

            // Kiểm tra Deezer URL
            if (previewUrl.contains("dzcdn.net")) {
                logger.info("🌐 Deezer URL detected, switching to alternative audio");
                return generateAlternativeAudioUrl(track);
            }

            // Thử tải audio từ URL
            String trackId = String.valueOf(track.get("id"));
            String localAudioPath = audioStorageService.downloadAndStoreAudio(previewUrl, trackId);
            
            if (audioStorageService.audioFileExists(localAudioPath.replace("/api/audio/", ""))) {
                logger.info("✅ ĐÃ TẢI THÀNH CÔNG audio: {}", localAudioPath);
                return localAudioPath;
            } else {
                logger.warn("⚠️ Không thể tải audio từ URL, chuyển sang alternative audio");
                return generateAlternativeAudioUrl(track);
            }

        } catch (Exception e) {
            logger.error("💥 LỖI KHI XỬ LÝ AUDIO FILE: {}", e.getMessage());
            return generateAlternativeAudioUrl(track);
        }
    }

    /**
     * ✅ Tạo URL alternative audio (âm thanh tự tạo dựa trên thông tin bài hát)
     */
    private String generateAlternativeAudioUrl(Map<String, Object> track) {
        try {
            String trackId = String.valueOf(track.get("id"));
            String title = (String) track.get("title");
            String artist = getArtistName(track);
            
            // Tạo ID ổn định từ thông tin bài hát
            String uniqueId = generateMusicId(track);
            
            // ✅ TẠO AUDIO TỰ ĐỘNG DỰA TRÊN THÔNG TIN BÀI HÁT
            String audioPath = audioStorageService.createMusicAudio(uniqueId, title, artist);
            
            // Kiểm tra file có tồn tại không
            if (audioStorageService.audioFileExists(audioPath.replace("/api/audio/", ""))) {
                logger.info("🎵 Đã tạo alternative audio: {} - {} -> {}", title, artist, audioPath);
                return audioPath;
            } else {
                logger.warn("⚠️ Alternative audio không được tạo, sử dụng fallback chung");
                return "/api/audio/fallback_general.mp3";
            }
            
        } catch (Exception e) {
            logger.error("❌ Lỗi tạo alternative audio: {}", e.getMessage());
            return "/api/audio/fallback_general.mp3";
        }
    }

    /**
     * ✅ Tạo ID duy nhất cho bài hát
     */
    private String generateMusicId(Map<String, Object> track) {
        String title = (String) track.get("title");
        String artist = getArtistName(track);
        String trackId = String.valueOf(track.get("id"));
        
        // Kết hợp thông tin để tạo ID ổn định
        String base = (title + "_" + artist + "_" + trackId).toLowerCase()
                    .replaceAll("[^a-z0-9]", "_")
                    .replaceAll("_+", "_");
        
        return "music_" + base;
    }

    /**
     * Xử lý URL hình ảnh
     */
    private void processImageUrl(Map<String, Object> track, Songs song) {
        Map<String, Object> albumData = (Map<String, Object>) track.get("album");
        if (albumData != null && albumData.get("cover_medium") != null) {
            song.setImageUrl((String) albumData.get("cover_medium"));
        } else {
            song.setImageUrl("/images/default-song.png");
        }
    }

    private Genres getOrCreateVietnameseGenre() {
        return genreRepo.findByName(VIETNAMESE_GENRE)
                .orElseGet(() -> {
                    Genres genre = new Genres();
                    genre.setName(VIETNAMESE_GENRE);
                    
                    // ✅ SỬA LỖI: SET CREATED_AT CHO GENRE
                    setCreatedAtForEntity(genre);
                    
                    return genreRepo.save(genre);
                });
    }

    private String getArtistName(Map<String, Object> track) {
        Map<String, Object> artistData = (Map<String, Object>) track.get("artist");
        return artistData != null ? (String) artistData.get("name") : "Unknown";
    }

    private String getTrackTitle(Map<String, Object> track) {
        return track.containsKey("title") ? (String) track.get("title") : "Unknown";
    }

    /**
     * Phương thức để bật/tắt chế độ tải audio
     */
    public void setEnableAudioDownload(boolean enable) {
        this.enableAudioDownload = enable;
        logger.info("Đã {} chế độ tải audio cục bộ", enable ? "bật" : "tắt");
    }

    /**
     * Phương thức kiểm tra trạng thái lưu trữ audio
     */
    public String getAudioStorageStatus() {
        return enableAudioDownload ? 
            "Đang sử dụng chế độ lưu trữ audio cục bộ" : 
            "Đang sử dụng alternative audio hoặc URL từ Deezer";
    }
}