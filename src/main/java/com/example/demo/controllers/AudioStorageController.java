package com.example.demo.controllers;

import com.example.demo.services.AudioStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audio-storage")
public class AudioStorageController {

    private static final Logger logger = LoggerFactory.getLogger(AudioStorageController.class);
    
    private final AudioStorageService audioStorageService;

    public AudioStorageController(AudioStorageService audioStorageService) {
        this.audioStorageService = audioStorageService;
    }

    /**
     * GET /api/audio-storage/files - Lấy danh sách tất cả file audio
     */
    @GetMapping("/files")
    public ResponseEntity<Map<String, Object>> getAllAudioFiles() {
        try {
            List<AudioStorageService.AudioFileInfo> audioFiles = audioStorageService.getAllAudioFiles();
            AudioStorageService.StorageInfo storageInfo = audioStorageService.getStorageInfo();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("storageInfo", storageInfo);
            response.put("audioFiles", audioFiles);
            response.put("totalFiles", audioFiles.size());
            
            logger.info("Trả về danh sách {} file audio", audioFiles.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Lỗi khi lấy danh sách file audio: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi lấy danh sách file: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
 // Thêm vào MusicImportController.java
    @GetMapping("/test-audio-download")
    public ResponseEntity<Map<String, Object>> testAudioDownload() {
        try {
            String testUrl = "https://cdns-preview.dzcdn.net/stream/c-deda7fa9316d9e9e880d2c6207e92260-3.mp3";
            String testId = "test_audio_123";
            
            String result = audioStorageService.downloadAndStoreAudio(testUrl, testId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result != null);
            response.put("filePath", result);
            response.put("message", result != null ? "✅ Test audio download thành công" : "❌ Test audio download thất bại");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "❌ Lỗi test audio download: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @GetMapping("/debug/download-test")
    public ResponseEntity<Map<String, Object>> debugDownloadTest() {
        try {
            // ✅ THỬ NHIỀU URL DEEZER KHÁC NHAU
            String[] testUrls = {
                "https://cdns-preview-1.dzcdn.net/stream/c-1deda7fa9316d9e9e880d2c6207e92260-3.mp3",
                "https://cdns-preview.dzcdn.net/stream/c-deda7fa9316d9e9e880d2c6207e92260-2.mp3", 
                "https://e-cdns-preview-1.dzcdn.net/stream/c-1deda7fa9316d9e9e880d2c6207e92260-3.mp3",
                "https://mp3-1d.dzcdn.net/stream/c-1deda7fa9316d9e9e880d2c6207e92260-3.mp3"
            };
            
            Map<String, Object> finalResult = new HashMap<>();
            List<Map<String, Object>> testResults = new ArrayList<>();
            
            for (String testUrl : testUrls) {
                Map<String, Object> testResult = new HashMap<>();
                testResult.put("testUrl", testUrl);
                
                try {
                    String testId = "debug_test_" + System.currentTimeMillis();
                    Map<String, Object> debugResult = audioStorageService.debugAudioDownload(testUrl, testId);
                    testResult.put("debugResult", debugResult);
                    
                    // Thử download thật
                    String downloadResult = audioStorageService.downloadAndStoreAudio(testUrl, testId);
                    testResult.put("downloadResult", downloadResult);
                    testResult.put("success", !downloadResult.contains("fallback"));
                    
                } catch (Exception e) {
                    testResult.put("error", e.getMessage());
                    testResult.put("success", false);
                }
                
                testResults.add(testResult);
            }
            
            finalResult.put("testResults", testResults);
            finalResult.put("message", "Đã test " + testUrls.length + " URL Deezer khác nhau");
            
            return ResponseEntity.ok(finalResult);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/audio-storage/info - Lấy thông tin storage
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getStorageInfo() {
        try {
            AudioStorageService.StorageInfo storageInfo = audioStorageService.getStorageInfo();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("storageInfo", storageInfo);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Lỗi khi lấy thông tin storage: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi lấy thông tin storage: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * DELETE /api/audio-storage/files/{fileName} - Xóa file audio
     */
    @DeleteMapping("/files/{fileName}")
    public ResponseEntity<Map<String, Object>> deleteAudioFile(@PathVariable String fileName) {
        try {
            boolean deleted = audioStorageService.deleteAudioFile(fileName);
            
            Map<String, Object> response = new HashMap<>();
            if (deleted) {
                response.put("success", true);
                response.put("message", "Đã xóa file: " + fileName);
                logger.info("Đã xóa file audio: {}", fileName);
            } else {
                response.put("success", false);
                response.put("message", "Không tìm thấy file: " + fileName);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Lỗi khi xóa file audio {}: {}", fileName, e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi xóa file: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
}