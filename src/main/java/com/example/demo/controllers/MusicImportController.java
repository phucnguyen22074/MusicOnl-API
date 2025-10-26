package com.example.demo.controllers;

import com.example.demo.entities.Songs;
import com.example.demo.repositories.SongRepository;
import com.example.demo.services.MusicImportService;
import com.example.demo.services.SongsService;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/import")
public class MusicImportController {

    private static final Logger logger = LoggerFactory.getLogger(MusicImportController.class);
    
    private final MusicImportService musicImportService;

    public MusicImportController(MusicImportService musicImportService) {
        this.musicImportService = musicImportService;
    }
    
    @Autowired
    private SongRepository songRepository;

    /**
     * Import nhạc hot từ Deezer chart (top global)
     * GET /api/import/deezer/chart
     */
    @GetMapping("/deezer/chart")
    public ResponseEntity<ImportResponse> importFromDeezerChart() {
        try {
            logger.info("🎵 Nhận request import từ Deezer Chart");
            int count = musicImportService.importSongsFromDeezerChart();
            
            ImportResponse response = new ImportResponse(
                true, 
                "✅ Import thành công " + count + " bài hát từ Deezer Chart", 
                count
            );
            
            logger.info("🎵 Import từ Chart hoàn tất: {} bài hát", count);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Lỗi khi import từ Deezer Chart", e);
            
            ImportResponse response = new ImportResponse(
                false, 
                "❌ Import thất bại: " + e.getMessage(), 
                0
            );
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @GetMapping("/debug/network-status")
    public ResponseEntity<Map<String, Object>> checkNetworkStatus() {
        Map<String, Object> status = new HashMap<>();
        RestTemplate restTemplate = new RestTemplate();
        try {
            // Kiểm tra kết nối internet
            restTemplate.getForObject("https://www.google.com", String.class);
            status.put("internet", "CONNECTED");
        } catch (Exception e) {
            status.put("internet", "DISCONNECTED");
            status.put("internetError", e.getMessage());
        }
        
        try {
            
			// Kiểm tra kết nối Deezer
            restTemplate.getForObject("https://api.deezer.com", String.class);
            status.put("deezerApi", "CONNECTED");
        } catch (Exception e) {
            status.put("deezerApi", "DISCONNECTED");
            status.put("deezerError", e.getMessage());
        }
        
        try {
            // Kiểm tra kết nối CDN Deezer
            restTemplate.getForObject("https://cdns-preview.dzcdn.net", String.class);
            status.put("deezerCdn", "CONNECTED");
        } catch (Exception e) {
            status.put("deezerCdn", "DISCONNECTED");
            status.put("cdnError", e.getMessage());
        }
        
        status.put("serverTime", new Date());
        status.put("recommendation", "Nếu Deezer CDN bị chặn, hãy sử dụng audio service khác");
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * 🆕 ENDPOINT: Kiểm tra thông tin bài hát đã import
     * GET /api/import/songs/list
     */
    @GetMapping("/songs/list")
    public ResponseEntity<Map<String, Object>> getImportedSongs() {
        try {
            // Giả sử bạn có songService hoặc songRepo
            List<Songs> songs = songRepository.findAll(); // hoặc songService.getAllSongs()
            
            List<Map<String, Object>> songList = songs.stream()
                .map(song -> {
                    Map<String, Object> songInfo = new HashMap<>();
                    songInfo.put("id", song.getSongId());
                    songInfo.put("title", song.getTitle());
                    songInfo.put("duration", song.getDuration());
                    songInfo.put("filePath", song.getFilePath());
                    songInfo.put("createdAt", song.getCreatedAt());
                    
                    // Lấy artist name
                    String artistName = "Unknown";
                    try {
                        if (!song.getartists().isEmpty()) {
                            artistName = song.getartists().iterator().next().getName();
                        }
                    } catch (Exception e) {
                        try {
                            if (!song.getartists().isEmpty()) {
                                artistName = song.getartists().iterator().next().getName();
                            }
                        } catch (Exception e2) {
                            // Ignore
                        }
                    }
                    songInfo.put("artist", artistName);
                    
                    return songInfo;
                })
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalSongs", songList.size());
            response.put("songs", songList);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Lỗi khi lấy danh sách bài hát: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi lấy danh sách bài hát: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Import nhạc từ Deezer search 
     * GET /api/import/deezer/search?query=soobin
     */
    @GetMapping("/deezer/search")
    public ResponseEntity<ImportResponse> importFromDeezerSearch(
            @RequestParam String query,
            @RequestParam(required = false, defaultValue = "50") int limit) { // ✅ SỬA: int thay vì Integer
            
        try {
            if (query == null || query.trim().isEmpty()) {
                ImportResponse response = new ImportResponse(
                    false, 
                    "❌ Query không được để trống", 
                    0
                );
                return ResponseEntity.badRequest().body(response);
            }
            
            logger.info("🎵 Nhận request import từ Deezer Search với query: {}, limit: {}", query, limit);
            int count = musicImportService.importSongsFromDeezerSearch(query);
            
            ImportResponse response = new ImportResponse(
                true, 
                "✅ Import thành công " + count + " bài hát từ tìm kiếm: " + query, 
                count
            );
            
            logger.info("🎵 Import từ Search hoàn tất: {} bài hát", count);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("⚠️ Tham số không hợp lệ: {}", e.getMessage());
            
            ImportResponse response = new ImportResponse(
                false, 
                "⚠️ Tham số không hợp lệ: " + e.getMessage(), 
                0
            );
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            logger.error("❌ Lỗi khi import từ Deezer Search với query: {}", query, e);
            
            ImportResponse response = new ImportResponse(
                false, 
                "❌ Import thất bại: " + e.getMessage(), 
                0
            );
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 🆕 ENDPOINT MỚI: Kiểm tra trạng thái audio storage
     * GET /api/import/audio-status
     */
    @GetMapping("/audio-status")
    public ResponseEntity<AudioStatusResponse> getAudioStorageStatus() {
        try {
            String status = musicImportService.getAudioStorageStatus();
            
            AudioStatusResponse response = new AudioStatusResponse(
                true,
                "✅ Kiểm tra trạng thái audio storage thành công",
                status
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Lỗi khi kiểm tra audio status", e);
            
            AudioStatusResponse response = new AudioStatusResponse(
                false,
                "❌ Kiểm tra trạng thái thất bại: " + e.getMessage(),
                null
            );
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 🆕 ENDPOINT MỚI: Bật/tắt chế độ tải audio cục bộ
     * PUT /api/import/audio-download?enable=true
     */
    @PutMapping("/audio-download")
    public ResponseEntity<ImportResponse> toggleAudioDownload(@RequestParam boolean enable) {
        try {
            musicImportService.setEnableAudioDownload(enable);
            
            String message = enable ? 
                "✅ Đã bật chế độ tải audio cục bộ" : 
                "⚠️ Đã tắt chế độ tải audio cục bộ (sử dụng URL trực tiếp)";
            
            ImportResponse response = new ImportResponse(
                true,
                message,
                0
            );
            
            logger.info(message);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Lỗi khi thay đổi chế độ audio download", e);
            
            ImportResponse response = new ImportResponse(
                false,
                "❌ Thay đổi chế độ audio thất bại: " + e.getMessage(),
                0
            );
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * DTO class để trả về response chuẩn
     */
    public static class ImportResponse {
        private boolean success;
        private String message;
        private int importedCount;

        public ImportResponse() {
            // Constructor mặc định cho Jackson
        }

        public ImportResponse(boolean success, String message, int importedCount) {
            this.success = success;
            this.message = message;
            this.importedCount = importedCount;
        }

        // Getters and Setters
        public boolean isSuccess() { 
            return success; 
        }
        
        public void setSuccess(boolean success) { 
            this.success = success; 
        }

        public String getMessage() { 
            return message; 
        }
        
        public void setMessage(String message) { 
            this.message = message; 
        }

        public int getImportedCount() { 
            return importedCount; 
        }
        
        public void setImportedCount(int importedCount) { 
            this.importedCount = importedCount; 
        }
    }

    /**
     * 🆕 DTO MỚI: Response cho audio status
     */
    public static class AudioStatusResponse {
        private boolean success;
        private String message;
        private String storageStatus;

        public AudioStatusResponse() {
            // Constructor mặc định cho Jackson
        }

        public AudioStatusResponse(boolean success, String message, String storageStatus) {
            this.success = success;
            this.message = message;
            this.storageStatus = storageStatus;
        }

        // Getters and Setters
        public boolean isSuccess() { 
            return success; 
        }
        
        public void setSuccess(boolean success) { 
            this.success = success; 
        }

        public String getMessage() { 
            return message; 
        }
        
        public void setMessage(String message) { 
            this.message = message; 
        }

        public String getStorageStatus() { 
            return storageStatus; 
        }
        
        public void setStorageStatus(String storageStatus) { 
            this.storageStatus = storageStatus; 
        }
    }
}