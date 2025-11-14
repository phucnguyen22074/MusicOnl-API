package com.example.demo.services;

import com.example.demo.dto.ArtistsDTO;
import com.example.demo.dto.SongsDTO;
import com.example.demo.entities.Albums;
import com.example.demo.entities.Artists;
import com.example.demo.entities.Genres;
import com.example.demo.entities.Songs;
import com.example.demo.entities.Users;
import com.example.demo.repositories.AlbumRepository;
import com.example.demo.repositories.ArtistRepository;
import com.example.demo.repositories.GenreRepository;
import com.example.demo.repositories.SongRepository;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SongsServiceImpl implements SongsService {

    private static final Logger logger = LoggerFactory.getLogger(SongsServiceImpl.class);
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final String[] MUSIC_EXT = {".mp3", ".wav"};
    private static final String[] IMAGE_EXT = {".jpg", ".jpeg", ".png"};

    @Autowired private SongRepository songRepo;
    @Autowired private AlbumRepository albumRepo;
    @Autowired private ArtistRepository artistRepo;
    @Autowired private GenreRepository genreRepo;
    @Autowired private ModelMapper mapper;

    @Value("${app.audio.storage-path}") private String musicDir;
    @Value("${app.upload.image-dir:${user.dir}/target/classes/static/assets/images}") private String imageDir;
    @Value("${musics_url}") private String musicUrlPrefix;
    @Value("${images_url}") private String imageUrlPrefix;
    @Value("${app.upload.default-image:no-image.jpg}") private String defaultImage;

    // ================= FIND ALL =================
    @Override
    public List<SongsDTO> findAll() {
        return songRepo.findAllWithRelations().stream()
                .map(this::toDtoWithUrls)
                .toList();
    }

    // ================= ADD SONG =================
    @Transactional
    @Override
    public SongsDTO addSong(String title, Integer duration, String lyrics, String status,
                            Integer albumId, List<Integer> artistIds, List<Integer> genreIds,
                            MultipartFile file, MultipartFile image) {

        validateSongInput(title, duration, file); // BẮT BUỘC file nhạc

        Songs song = new Songs();
        song.setTitle(title.trim());
        song.setDuration(duration);
        song.setLyrics(lyrics);
        song.setStatus(status);
        song.setCreatedAt(new Date());

        setAlbumIfValid(song, albumId);
        setArtistsIfValid(song, artistIds);
        setGenresIfValid(song, genreIds);

        // BẮT BUỘC upload file nhạc
        String musicFileName = uploadFile(file, musicDir, MUSIC_EXT, "file nhạc");

        // Ảnh: nếu không có → dùng mặc định
        String imageFileName = image != null && !image.isEmpty()
                ? uploadFile(image, imageDir, IMAGE_EXT, "ảnh bìa")
                : defaultImage;

        ensureDefaultImageExists();

        song.setFilePath(musicFileName);
        song.setImageUrl(imageFileName);

        songRepo.save(song);
        return toDtoWithUrls(song);
    }

    // ================= UPDATE SONG =================
    @Transactional
    @Override
    public SongsDTO updateSong(Integer id, String title, Integer duration, String lyrics, String status,
                               Integer albumId, List<Integer> artistIds, List<Integer> genreIds,
                               MultipartFile file, MultipartFile image) {

        Songs song = getSongOrThrow(id);

        if (title != null && !title.trim().isEmpty()) song.setTitle(title.trim());
        if (duration != null && duration > 0) song.setDuration(duration);
        if (lyrics != null) song.setLyrics(lyrics);
        if (status != null) song.setStatus(status);

        if (albumId != null) setAlbumIfValid(song, albumId);
        if (artistIds != null && !artistIds.isEmpty()) setArtistsIfValid(song, artistIds);
        if (genreIds != null && !genreIds.isEmpty()) setGenresIfValid(song, genreIds);

        // Cập nhật file nhạc (nếu có)
        if (file != null && !file.isEmpty()) {
            String oldFile = song.getFilePath();
            String newFile = uploadFile(file, musicDir, MUSIC_EXT, "file nhạc");
            song.setFilePath(newFile);
            deleteOldFile(oldFile, musicDir);
        }

        // Cập nhật ảnh (nếu có)
        if (image != null && !image.isEmpty()) {
            String oldImg = song.getImageUrl();
            String newImg = uploadFile(image, imageDir, IMAGE_EXT, "ảnh bìa");
            song.setImageUrl(newImg);
            deleteOldFile(oldImg, imageDir);
        } else if (song.getImageUrl() == null || song.getImageUrl().isBlank()) {
            song.setImageUrl(defaultImage);
            ensureDefaultImageExists();
        }

        songRepo.save(song);
        return toDtoWithUrls(song);
    }

    // ================= DELETE SONG =================
    @Transactional
    @Override
    public boolean deleteSong(Integer id) {
        return songRepo.findById(id).map(song -> {
            deleteOldFile(song.getFilePath(), musicDir);
            deleteOldFile(song.getImageUrl(), imageDir);
            songRepo.delete(song);
            return true;
        }).orElse(false);
    }

    // ================= FIND BY... =================
    @Override public List<SongsDTO> findByGenre(Integer genreId) {
        validateId(genreId, "Genre ID");
        return songRepo.findByGenreses_GenreId(genreId).stream()
                .map(this::toDtoWithUrls).toList();
    }

    @Override public List<SongsDTO> findByArtist(Integer artistId) {
        validateId(artistId, "Artist ID");
        if (!artistRepo.existsById(artistId)) throw new IllegalArgumentException("Không tìm thấy Artist");
        return songRepo.findByartists_ArtistId(artistId).stream()
                .map(this::toDtoWithUrls).toList();
    }

    @Override public List<SongsDTO> findByTitleContaining(String keyword) {
        if (keyword == null || keyword.isBlank()) throw new IllegalArgumentException("Từ khóa không được để trống");
        return songRepo.findByTitleContainingIgnoreCase(keyword).stream()
                .map(this::toDtoWithUrls).toList();
    }

    // ================= HELPER METHODS =================

    private void validateSongInput(String title, Integer duration, MultipartFile file) {
        if (title == null || title.trim().isEmpty())
            throw new IllegalArgumentException("Tiêu đề bài hát không được để trống");
        if (duration != null && duration <= 0)
            throw new IllegalArgumentException("Thời lượng phải lớn hơn 0");
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("File nhạc là bắt buộc");
    }

    private void validateId(Integer id, String name) {
        if (id == null) throw new IllegalArgumentException(name + " không được để trống");
    }

    private void validateFile(MultipartFile file, String[] exts, long maxSize, String type) {
        if (file.getSize() > maxSize)
            throw new IllegalArgumentException(type + " vượt quá " + (maxSize / (1024*1024)) + "MB");
        String name = file.getOriginalFilename();
        if (name == null || !hasValidExtension(name, exts))
            throw new IllegalArgumentException(type + " chỉ cho phép: " + Arrays.toString(exts));
    }

    private boolean hasValidExtension(String fileName, String[] exts) {
        String ext = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
        return Arrays.asList(exts).contains(ext);
    }

    private String uploadFile(MultipartFile file, String dir, String[] exts, String type) {
        validateFile(file, exts, MAX_FILE_SIZE, type);
        try {
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename().replace(" ", "_");
            Path path = Paths.get(dir);
            Files.createDirectories(path);
            Files.copy(file.getInputStream(), path.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (IOException e) {
            logger.error("Lỗi upload {}", type, e);
            throw new RuntimeException("Không thể lưu " + type, e);
        }
    }

    private void ensureDefaultImageExists() {
        Path target = Paths.get(imageDir, defaultImage);
        if (Files.exists(target)) return;

        try {
            Files.createDirectories(target.getParent());
            var resource = getClass().getClassLoader().getResourceAsStream("static/assets/" + defaultImage);
            if (resource != null) {
                Files.copy(resource, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            logger.warn("Không thể tạo ảnh mặc định: {}", defaultImage, e);
        }
    }

    private void deleteOldFile(String fileName, String dir) {
        if (fileName == null || fileName.isBlank() || fileName.equals(defaultImage)) return;
        try {
            Files.deleteIfExists(Paths.get(dir, fileName));
        } catch (IOException e) {
            logger.warn("Không thể xóa file cũ: {}", fileName, e);
        }
    }

    private Songs getSongOrThrow(Integer id) {
        return songRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Song với ID: " + id));
    }

    private void setAlbumIfValid(Songs song, Integer albumId) {
        if (albumId != null) {
            Albums album = albumRepo.findById(albumId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Album với ID: " + albumId));
            song.setAlbums(album);
        }
    }

    private void setArtistsIfValid(Songs song, List<Integer> artistIds) {
        if (artistIds != null && !artistIds.isEmpty()) {
            Set<Artists> artists = new HashSet<>(artistRepo.findAllById(artistIds));
            if (artists.size() != artistIds.size())
                throw new IllegalArgumentException("Một hoặc nhiều Artist không tồn tại");
            song.setartists(artists);
        }
    }

    private void setGenresIfValid(Songs song, List<Integer> genreIds) {
        if (genreIds != null && !genreIds.isEmpty()) {
            Set<Genres> genres = new HashSet<>(genreRepo.findAllById(genreIds));
            if (genres.size() != genreIds.size())
                throw new IllegalArgumentException("Một hoặc nhiều Genre không tồn tại");
            song.setGenreses(genres);
        }
    }

    public SongsDTO toDtoWithUrls(Songs song) {
        SongsDTO dto = mapper.map(song, SongsDTO.class); // Chỉ map field cơ bản

        // === ALBUM ===
        Albums album = song.getAlbums();
        if (album != null) {
            dto.setAlbumId(album.getAlbumId());
            dto.setAlbumTitle(album.getTitle());
        }

        // === USER ===
        Users user = song.getUsers();
        if (user != null) {
            dto.setUserId(user.getUserId());
            dto.setUsername(user.getUsername());
        }

        // === ARTISTS ===
        Set<Artists> artists = song.getartists();
        if (artists != null && !artists.isEmpty()) {
            Set<ArtistsDTO> artistDTOs = artists.stream()
                    .map(artist -> mapper.map(artist, ArtistsDTO.class))
                    .collect(Collectors.toSet());
            dto.setArtists(artistDTOs);
        }

        // === GENRES ===
        Set<Genres> genres = song.getGenreses();
        if (genres != null && !genres.isEmpty()) {
            Set<String> genreNames = genres.stream()
                    .map(Genres::getName)
                    .collect(Collectors.toSet());
            dto.setGenres(genreNames);
        }

        return dto;
    }

    @Transactional
    @Override
    public void incrementListenCount(Integer songId) {
        if (songId == null || songId <= 0) {
            logger.warn("Invalid songId: {}", songId);
            return;
        }

        songRepo.findById(songId).ifPresent(song -> {
            Integer count = song.getListenCount();
            song.setListenCount(count != null ? count + 1 : 1);
            songRepo.save(song);
            logger.info("Listen count updated for song {}: {}", songId, song.getListenCount());
        });
    }
}