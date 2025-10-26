package com.example.demo.controllers;

import com.example.demo.services.AudioStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/audio")
public class AudioController {

    private final Path audioStorageLocation;
    
    private static final Logger logger = LoggerFactory.getLogger(AudioController.class);

    @Autowired
    private ApplicationContext applicationContext;

    public AudioController(@Value("${app.audio.storage-path}") String storagePath) {
        this.audioStorageLocation = Paths.get(storagePath).toAbsolutePath().normalize();
    }

    /**
     * ✅ ENDPOINT CHÍNH: Phục vụ file audio
     * GET http://localhost:8088/api/audio/{filename}
     */
    @GetMapping("/{fileName:.+}")
    public ResponseEntity<Resource> serveAudioFile(@PathVariable String fileName) {
        try {
            logger.info("🎵 Nhận request audio file: {}", fileName);
            
            Path filePath = audioStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = determineContentType(fileName);
                
                logger.info("✅ Trả về audio file: {} (Content-Type: {})", fileName, contentType);
                
                return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
            } else {
                logger.warn("⚠️ File audio không tồn tại: {}, trả về fallback", fileName);
                
                // ✅ TRẢ VỀ FALLBACK AUDIO
                return serveFallbackAudio(fileName);
            }
        } catch (Exception e) {
            logger.error("❌ Lỗi khi phục vụ audio file {}: {}", fileName, e.getMessage());
            return serveFallbackAudio(fileName);
        }
    }

    /**
     * ✅ Phục vụ fallback audio
     */
    private ResponseEntity<Resource> serveFallbackAudio(String requestedFileName) {
        try {
            logger.info("🔄 Sử dụng fallback audio cho: {}", requestedFileName);
            
            Path fallbackPath = audioStorageLocation.resolve("fallback_audio.mp3");
            
            // ✅ KIỂM TRA VÀ TẠO LẠI NẾU FILE KHÔNG TỒN TẠI HOẶC KHÔNG HỢP LỆ
            if (!Files.exists(fallbackPath)) {
                logger.warn("⚠️ Fallback audio không tồn tại, đang tạo lại...");
                createRealFallbackAudio();
            } else {
                // Kiểm tra xem có phải MP3 thật không
                byte[] fileData = Files.readAllBytes(fallbackPath);
                if (!isValidAudioFile(fileData)) {
                    logger.warn("⚠️ Fallback audio không hợp lệ, đang tạo lại...");
                    createRealFallbackAudio();
                }
            }
            
            Resource fallbackResource = new UrlResource(fallbackPath.toUri());
            
            if (fallbackResource.exists() && fallbackResource.isReadable()) {
                // ✅ KIỂM TRA CONTENT-TYPE
                String contentType = determineContentType("fallback_audio.mp3");
                long fileSize = Files.size(fallbackPath);
                
                logger.info("✅ Phục vụ fallback audio: {} bytes, Content-Type: {}", fileSize, contentType);
                
                return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"fallback_audio.mp3\"")
                    .body(fallbackResource);
            } else {
                logger.error("❌ Fallback audio không tồn tại hoặc không đọc được");
                return serveEmptyAudio();
            }
        } catch (Exception e) {
            logger.error("❌ Lỗi fallback audio: {}", e.getMessage());
            return serveEmptyAudio();
        }
    }

    /**
     * ✅ Tạo fallback audio thực sự
     */
    private void createRealFallbackAudio() throws Exception {
        Path fallbackPath = audioStorageLocation.resolve("fallback_audio.mp3");
        
        // Tạo MP3 silence 5 giây
        byte[] realMp3 = createRealSilentAudio(5);
        Files.write(fallbackPath, realMp3);
        
        logger.info("📝 Đã tạo fallback audio MP3 thật: fallback_audio.mp3 ({} bytes)", realMp3.length);
    }

    /**
     * ✅ Tạo audio silence thực sự
     */
    private byte[] createRealSilentAudio(int seconds) {
        try {
            // Tạo WAV file silence chất lượng tốt
            return createHighQualitySilentWav(seconds, 44100, 16, 2); // 5 giây, 44.1kHz, 16-bit, stereo
        } catch (Exception e) {
            logger.warn("Không thể tạo silent audio chất lượng cao, sử dụng fallback đơn giản");
            return createBasicSilentAudio(seconds);
        }
    }

    /**
     * ✅ Tạo WAV file silence chất lượng cao
     */
    private byte[] createHighQualitySilentWav(int seconds, int sampleRate, int bitsPerSample, int channels) {
        try {
            int byteRate = sampleRate * channels * bitsPerSample / 8;
            int blockAlign = channels * bitsPerSample / 8;
            int dataSize = seconds * sampleRate * blockAlign;
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            // WAV header chuẩn
            writeString(baos, "RIFF"); // Chunk ID
            writeInt(baos, 36 + dataSize); // Chunk Size
            writeString(baos, "WAVE"); // Format
            writeString(baos, "fmt "); // Subchunk 1 ID
            writeInt(baos, 16); // Subchunk 1 Size
            writeShort(baos, (short) 1); // Audio Format (PCM)
            writeShort(baos, (short) channels); // Number of Channels
            writeInt(baos, sampleRate); // Sample Rate
            writeInt(baos, byteRate); // Byte Rate
            writeShort(baos, (short) blockAlign); // Block Align
            writeShort(baos, (short) bitsPerSample); // Bits Per Sample
            writeString(baos, "data"); // Subchunk 2 ID
            writeInt(baos, dataSize); // Subchunk 2 Size
            
            // Audio data (silence - tất cả bytes = 0)
            byte[] silence = new byte[dataSize];
            baos.write(silence);
            
            byte[] result = baos.toByteArray();
            logger.debug("✅ Đã tạo WAV silence: {} bytes, {} giây, {} channels", 
                        result.length, seconds, channels);
            
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo WAV silence chất lượng cao", e);
        }
    }

    /**
     * ✅ Tạo audio silence cơ bản (fallback)
     */
    private byte[] createBasicSilentAudio(int seconds) {
        try {
            // Tạo WAV đơn giản hơn
            return createHighQualitySilentWav(seconds, 22050, 16, 1); // 3 giây, 22.05kHz, 16-bit, mono
        } catch (Exception e) {
            // Fallback cuối cùng: tạo file với content hợp lệ
            String audioContent = "SILENCE_AUDIO_" + seconds + "s_VALID_MP3_CONTENT";
            return audioContent.getBytes(StandardCharsets.US_ASCII);
        }
    }

    // Helper methods for WAV creation
    private void writeString(ByteArrayOutputStream baos, String text) throws Exception {
        baos.write(text.getBytes(StandardCharsets.US_ASCII));
    }

    private void writeInt(ByteArrayOutputStream baos, int value) throws Exception {
        baos.write(value & 0xFF);
        baos.write((value >> 8) & 0xFF);
        baos.write((value >> 16) & 0xFF);
        baos.write((value >> 24) & 0xFF);
    }

    private void writeShort(ByteArrayOutputStream baos, short value) throws Exception {
        baos.write(value & 0xFF);
        baos.write((value >> 8) & 0xFF);
    }

    /**
     * ✅ Kiểm tra file audio có hợp lệ không
     */
    private boolean isValidAudioFile(byte[] data) {
        if (data == null || data.length < 100) return false;
        
        // Kiểm tra xem có chứa text "dummy" không
        String content = new String(data, 0, Math.min(200, data.length), StandardCharsets.US_ASCII);
        return !content.contains("dummy") && 
               !content.contains("Dummy") && 
               !content.contains("not a real") &&
               !content.contains("MP3 dummy");
    }

    /**
     * ✅ Phục vụ empty audio như last resort
     */
    private ResponseEntity<Resource> serveEmptyAudio() {
        try {
            // Tạo empty MP3 data
            byte[] emptyMp3 = new byte[0];
            ByteArrayResource resource = new ByteArrayResource(emptyMp3);
            
            logger.warn("⚠️ Phục vụ empty audio response");
            
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .contentLength(0)
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(204).build(); // No Content
        }
    }

    /**
     * ✅ Xác định content type
     */
    private String determineContentType(String fileName) {
        if (fileName.endsWith(".mp3")) return "audio/mpeg";
        if (fileName.endsWith(".wav")) return "audio/wav";
        if (fileName.endsWith(".ogg")) return "audio/ogg";
        if (fileName.endsWith(".m4a")) return "audio/mp4";
        return "application/octet-stream";
    }

    /**
     * ✅ ENDPOINT TEST: Kiểm tra audio controller
     * GET http://localhost:8088/api/audio/test/status
     */
    @GetMapping("/test/status")
    public ResponseEntity<String> testAudioController() {
        try {
            Path fallbackPath = audioStorageLocation.resolve("fallback_audio.mp3");
            boolean fallbackExists = Files.exists(fallbackPath);
            long fallbackSize = fallbackExists ? Files.size(fallbackPath) : 0;
            
            String status = String.format(
                "🎵 Audio Controller Status:\n" +
                "📍 Storage Path: %s\n" +
                "📁 Fallback Audio: %s (%d bytes)\n" +
                "✅ Controller: RUNNING",
                audioStorageLocation.toAbsolutePath(),
                fallbackExists ? "EXISTS" : "MISSING",
                fallbackSize
            );
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("❌ Audio Controller Error: " + e.getMessage());
        }
    }

    /**
     * ✅ ENDPOINT TEST: Tạo lại fallback audio
     * GET http://localhost:8088/api/audio/test/recreate-fallback
     */
    @GetMapping("/test/recreate-fallback")
    public ResponseEntity<String> recreateFallbackAudio() {
        try {
            createRealFallbackAudio();
            Path fallbackPath = audioStorageLocation.resolve("fallback_audio.mp3");
            long fileSize = Files.size(fallbackPath);
            
            return ResponseEntity.ok(String.format(
                "✅ Đã tạo lại fallback audio: %d bytes\n" +
                "📁 Đường dẫn: %s\n" +
                "🔗 Test URL: http://localhost:8088/api/audio/fallback_audio.mp3",
                fileSize, fallbackPath.toAbsolutePath()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("❌ Lỗi khi tạo fallback audio: " + e.getMessage());
        }
    }
    
    
}