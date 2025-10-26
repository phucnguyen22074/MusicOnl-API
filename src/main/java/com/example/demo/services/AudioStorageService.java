package com.example.demo.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AudioStorageService implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AudioStorageService.class);

    @Value("${app.audio.storage-path:./audio-storage/}")
    private String audioStoragePath;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * CommandLineRunner - Chạy khi ứng dụng khởi động
     */
    @Override
    public void run(String... args) throws Exception {
        createStorageDirectory();
        createGeneralFallbackAudio(); // ✅ TẠO FALLBACK CHUNG CÓ ÂM THANH
        logStorageInfo();
    }

    /**
     * Tạo thư mục lưu trữ audio khi khởi động
     */
    private void createStorageDirectory() throws IOException {
        Path storageDir = Paths.get(audioStoragePath);
        
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
            logger.info("📁 Đã tạo thư mục audio storage: {}", storageDir.toAbsolutePath());
            
            // Tạo file README
            createReadmeFile(storageDir);
        } else {
            logger.info("📁 Thư mục audio storage đã tồn tại: {}", storageDir.toAbsolutePath());
        }
    }

    /**
     * ✅ Tạo file fallback chung có âm thanh
     */
    private void createGeneralFallbackAudio() {
        try {
            Path fallbackPath = Paths.get(audioStoragePath, "fallback_general.mp3");
            if (!Files.exists(fallbackPath)) {
                byte[] audioWithSound = createRealAudioWithSound(10); // 10 giây có âm thanh
                Files.write(fallbackPath, audioWithSound);
                logger.info("🎵 Đã tạo fallback general audio CÓ ÂM THANH: fallback_general.mp3 ({} bytes)", 
                           audioWithSound.length);
            }
        } catch (Exception e) {
            logger.warn("⚠️ Không thể tạo fallback general audio: {}", e.getMessage());
        }
    }

    /**
     * Tạo file README hướng dẫn
     */
    private void createReadmeFile(Path storageDir) throws IOException {
        Path readmeFile = storageDir.resolve("README.txt");
        String readmeContent = "THƯ MỤC LƯU TRỮ AUDIO - MUSICONLINE\n" +
                             "===================================\n" +
                             "Tạo tự động: " + new Date() + "\n" +
                             "Đường dẫn: " + storageDir.toAbsolutePath() + "\n" +
                             "URL truy cập: http://localhost:8088/api/audio/{filename}\n" +
                             "\n" +
                             "CÁCH SỬ DỤNG:\n" +
                             "- File audio được lưu tự động khi import từ Deezer\n" +
                             "- Mỗi bài hát có file fallback riêng CÓ ÂM THANH\n" +
                             "- Truy cập file qua: http://localhost:8088/api/audio/filename.mp3\n" +
                             "- Không xóa file thủ công!\n" +
                             "===================================";
        
        Files.write(readmeFile, readmeContent.getBytes());
        logger.info("📝 Đã tạo file README.txt trong thư mục audio storage");
    }

    /**
     * Log thông tin storage
     */
    private void logStorageInfo() {
        try {
            Path storageDir = Paths.get(audioStoragePath);
            if (Files.exists(storageDir)) {
                // Đếm số file audio
                long fileCount = Files.list(storageDir)
                    .filter(path -> {
                        String fileName = path.getFileName().toString();
                        return !fileName.equals("README.txt") && 
                               !fileName.startsWith(".");
                    })
                    .count();
                
                // Tính tổng kích thước
                long totalSize = Files.walk(storageDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> !path.getFileName().toString().equals("README.txt"))
                    .mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (IOException e) {
                            return 0L;
                        }
                    })
                    .sum();
                
                logger.info("📊 THÔNG TIN AUDIO STORAGE:");
                logger.info("📍 Đường dẫn: {}", storageDir.toAbsolutePath());
                logger.info("🌐 URL truy cập: http://localhost:8088/api/audio/");
                logger.info("📄 Số file audio: {}", fileCount);
                logger.info("💾 Tổng dung lượng: {} MB", String.format("%.2f", totalSize / (1024.0 * 1024.0)));
                logger.info("✅ Audio Storage đã sẵn sàng!");
            }
        } catch (Exception e) {
            logger.warn("⚠️ Không thể đọc thông tin storage: {}", e.getMessage());
        }
    }

    /**
     * Tải audio từ URL và lưu trữ cục bộ
     */
    public String downloadAndStoreAudio(String audioUrl, String uniqueId) {
        logger.info("🎵 BẮT ĐẦU download audio - URL: {}, ID: {}", audioUrl, uniqueId);
        
        if (audioUrl == null || audioUrl.trim().isEmpty()) {
            logger.error("❌ URL audio là null hoặc rỗng");
            return createIndividualFallbackAudio(uniqueId);
        }

        // ✅ KIỂM TRA URL CÓ PHẢI DEEZER KHÔNG
        boolean isDeezerUrl = audioUrl.contains("dzcdn.net");
        if (isDeezerUrl) {
            logger.info("🌐 Đây là URL Deezer, có thể gặp vấn đề kết nối");
        }

        try {
            ensureStorageDirectoryExists();
            Path storageDir = Paths.get(audioStoragePath);
            String fileName = generateFileName(uniqueId, audioUrl);
            Path filePath = storageDir.resolve(fileName);

            // Kiểm tra cache
            if (Files.exists(filePath)) {
                logger.info("✅ File audio đã tồn tại, sử dụng file cache: {}", fileName);
                return "/api/audio/" + fileName;
            }

            logger.info("📥 Đang tải audio từ URL: {}", audioUrl);
            
            try {
                // ✅ THÊM RETRY LOGIC
                byte[] audioData = null;
                int retryCount = 0;
                int maxRetries = 2;
                
                while (audioData == null && retryCount < maxRetries) {
                    try {
                        audioData = restTemplate.getForObject(audioUrl, byte[].class);
                        if (audioData != null) break;
                    } catch (Exception e) {
                        retryCount++;
                        if (retryCount < maxRetries) {
                            logger.warn("🔄 Retry {} để tải audio...", retryCount);
                            Thread.sleep(2000); // Chờ 2 giây
                        }
                    }
                }
                
                if (audioData == null || audioData.length == 0) {
                    logger.error("❌ Không thể tải audio sau {} lần thử", maxRetries);
                    return createIndividualFallbackAudio(uniqueId);
                }

                logger.info("📊 Kích thước audio data: {} bytes", audioData.length);

                // Lưu file
                Files.write(filePath, audioData);
                logger.info("💾 ĐÃ LƯU THÀNH CÔNG audio file: {} ({} bytes)", fileName, audioData.length);

                return "/api/audio/" + fileName;

            } catch (Exception e) {
                logger.error("💥 LỖI KHI TẢI AUDIO: {}", e.getMessage());
                
                // ✅ PHÂN LOẠI LỖI
                if (e.getMessage().contains("I/O error") || e.getMessage().contains("UnknownHost")) {
                    logger.error("🌐 LỖI MẠNG: Không thể kết nối đến server Deezer");
                } else if (e.getMessage().contains("timed out")) {
                    logger.error("⏰ LỖI TIMEOUT: Kết nối quá lâu");
                }
                
                return createIndividualFallbackAudio(uniqueId);
            }

        } catch (Exception e) {
            logger.error("💥 LỖI HỆ THỐNG: {}", e.getMessage());
            return createIndividualFallbackAudio(uniqueId);
        }
    }

    /**
     * ✅ Kiểm tra xem data có phải audio thật không
     */
    private boolean isRealAudioFile(byte[] data) {
        if (data == null || data.length < 100) return false;
        
        // Kiểm tra MP3 header
        if (data.length >= 3) {
            // MP3: ID3 header hoặc MPEG frame sync
            if ((data[0] == 'I' && data[1] == 'D' && data[2] == '3') ||
                (data[0] == (byte)0xFF && (data[1] & 0xE0) == 0xE0)) {
                return true;
            }
        }
        
        // Kiểm tra WAV header
        if (data.length >= 12) {
            if (data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F' &&
                data[8] == 'W' && data[9] == 'A' && data[10] == 'V' && data[11] == 'E') {
                return true;
            }
        }
        
        // Kiểm tra xem có phải HTML error page không
        String contentStart = new String(data, 0, Math.min(200, data.length), StandardCharsets.UTF_8);
        if (contentStart.contains("<!DOCTYPE") || 
            contentStart.contains("<html") || 
            contentStart.contains("error") ||
            contentStart.contains("Error")) {
            logger.warn("⚠️ Dữ liệu trả về là HTML page, không phải audio");
            return false;
        }
        
        // Audio thật thường có kích thước > 10KB
        return data.length > 10240;
    }
    
 // Thêm vào AudioStorageService hoặc controller riêng
    public Map<String, Object> debugAudioDownload(String audioUrl, String uniqueId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            logger.info("🔍 DEBUG: Testing audio download from: {}", audioUrl);
            
            byte[] audioData = restTemplate.getForObject(audioUrl, byte[].class);
            
            result.put("success", audioData != null);
            result.put("dataLength", audioData != null ? audioData.length : 0);
            result.put("isRealAudio", audioData != null ? isRealAudioFile(audioData) : false);
            
            if (audioData != null && audioData.length > 0) {
                // Kiểm tra content type từ data
                String contentType = detectContentType(audioData);
                result.put("contentType", contentType);
                
                // Log first few bytes
                StringBuilder hex = new StringBuilder();
                for (int i = 0; i < Math.min(20, audioData.length); i++) {
                    hex.append(String.format("%02X ", audioData[i]));
                }
                result.put("firstBytes", hex.toString());
            }
            
            logger.info("🔍 DEBUG Result: {}", result);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            logger.error("🔍 DEBUG Error: {}", e.getMessage());
        }
        
        return result;
    }

    private String detectContentType(byte[] data) {
        if (data == null || data.length < 4) return "unknown";
        
        // MP3
        if (data[0] == 'I' && data[1] == 'D' && data[2] == '3') return "audio/mpeg";
        if (data[0] == (byte)0xFF && (data[1] & 0xE0) == 0xE0) return "audio/mpeg";
        
        // WAV
        if (data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F' &&
            data.length > 8 && data[8] == 'W' && data[9] == 'A' && data[10] == 'V' && data[11] == 'E') {
            return "audio/wav";
        }
        
        // HTML
        String start = new String(data, 0, Math.min(100, data.length), StandardCharsets.UTF_8);
        if (start.contains("<!DOCTYPE") || start.contains("<html")) return "text/html";
        
        return "unknown";
    }

    /**
     * ✅ Tạo file fallback audio riêng cho từng bài hát - CÓ ÂM THANH
     */
    public String createIndividualFallbackAudio(String uniqueId) {
        try {
            Path storageDir = Paths.get(audioStoragePath);
            String fileName = "fallback_" + uniqueId + ".mp3";
            Path filePath = storageDir.resolve(fileName);

            if (!Files.exists(filePath)) {
                // ✅ TẠO FILE AUDIO CÓ ÂM THANH CHO TỪNG BÀI HÁT
                byte[] audioWithSound = createRealAudioWithSound(30); // 30 giây có âm thanh
                Files.write(filePath, audioWithSound);
                logger.info("🎵 Đã tạo fallback audio CÓ ÂM THANH: {} ({} bytes)", fileName, audioWithSound.length);
            }

            return "/api/audio/" + fileName;
        } catch (Exception e) {
            logger.error("❌ Không thể tạo fallback audio có âm thanh: {}", e.getMessage());
            return "/api/audio/fallback_general.mp3";
        }
    }

    /**
     * ✅ Tạo audio thực sự CÓ ÂM THANH (beep tone)
     */
    private byte[] createRealAudioWithSound(int seconds) {
        try {
            // Tạo WAV file với tone audio (beep sound)
            return createToneWav(seconds, 44100, 16, 2, 440); // 440Hz = nốt A
        } catch (Exception e) {
            logger.warn("Không thể tạo audio có âm thanh, sử dụng fallback đơn giản");
            return createBasicToneAudio(seconds);
        }
    }

    /**
     * ✅ Tạo WAV file với tone (beep sound)
     */
    private byte[] createToneWav(int seconds, int sampleRate, int bitsPerSample, int channels, double frequencyHz) {
        try {
            int byteRate = sampleRate * channels * bitsPerSample / 8;
            int blockAlign = channels * bitsPerSample / 8;
            int dataSize = seconds * sampleRate * blockAlign;
            int samplesPerChannel = seconds * sampleRate;
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            // WAV header chuẩn
            writeString(baos, "RIFF");
            writeInt(baos, 36 + dataSize);
            writeString(baos, "WAVE");
            writeString(baos, "fmt ");
            writeInt(baos, 16);
            writeShort(baos, (short) 1); // PCM
            writeShort(baos, (short) channels);
            writeInt(baos, sampleRate);
            writeInt(baos, byteRate);
            writeShort(baos, (short) blockAlign);
            writeShort(baos, (short) bitsPerSample);
            writeString(baos, "data");
            writeInt(baos, dataSize);
            
            // ✅ TẠO ÂM THANH (SINE WAVE)
            double amplitude = 0.3 * Math.pow(2, bitsPerSample - 1) - 1; // 30% volume
            
            for (int i = 0; i < samplesPerChannel; i++) {
                double time = (double) i / sampleRate;
                double sample = Math.sin(2.0 * Math.PI * frequencyHz * time) * amplitude;
                
                // Convert to 16-bit PCM
                short pcmValue = (short) sample;
                
                // Write for each channel
                for (int channel = 0; channel < channels; channel++) {
                    writeShort(baos, pcmValue);
                }
            }
            
            byte[] result = baos.toByteArray();
            logger.info("🎵 Đã tạo WAV với âm thanh: {} bytes, {} giây, {}Hz", 
                       result.length, seconds, frequencyHz);
            
            return result;
            
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo WAV có âm thanh", e);
        }
    }

    /**
     * ✅ Tạo audio cơ bản có âm thanh (fallback)
     */
    private byte[] createBasicToneAudio(int seconds) {
        try {
            // Tạo tone đơn giản hơn
            return createToneWav(seconds, 22050, 16, 1, 330); // 330Hz, mono
        } catch (Exception e) {
            // Fallback cuối cùng: tạo file với content hợp lệ
            try {
                return createSimpleBeepAudio(seconds);
            } catch (Exception ex) {
                String audioContent = "AUDIO_WITH_SOUND_" + seconds + "s_FREQUENCY_440Hz_" + System.currentTimeMillis();
                return audioContent.getBytes(StandardCharsets.US_ASCII);
            }
        }
    }

    /**
     * ✅ Tạo âm thanh beep đơn giản
     */
    private byte[] createSimpleBeepAudio(int seconds) {
        try {
            int sampleRate = 44100;
            int bitsPerSample = 16;
            int channels = 1;
            int samples = seconds * sampleRate;
            int dataSize = samples * channels * bitsPerSample / 8;
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            // WAV header
            writeString(baos, "RIFF");
            writeInt(baos, 36 + dataSize);
            writeString(baos, "WAVE");
            writeString(baos, "fmt ");
            writeInt(baos, 16);
            writeShort(baos, (short) 1);
            writeShort(baos, (short) channels);
            writeInt(baos, sampleRate);
            writeInt(baos, sampleRate * channels * bitsPerSample / 8);
            writeShort(baos, (short) (channels * bitsPerSample / 8));
            writeShort(baos, (short) bitsPerSample);
            writeString(baos, "data");
            writeInt(baos, dataSize);
            
            // Tạo square wave (beep sound) - dễ nghe hơn
            short amplitude = 5000; // Volume vừa phải
            
            for (int i = 0; i < samples; i++) {
                // Square wave at 440Hz - tạo tiếng beep rõ ràng
                short value = (i % (sampleRate / 440) < (sampleRate / 880)) ? amplitude : (short) -amplitude;
                writeShort(baos, value);
            }
            
            byte[] result = baos.toByteArray();
            logger.info("🔊 Đã tạo beep audio: {} bytes, {} giây", result.length, seconds);
            return result;
            
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo beep audio", e);
        }
    }

    // Helper methods for WAV creation
    private void writeString(ByteArrayOutputStream baos, String text) throws IOException {
        baos.write(text.getBytes(StandardCharsets.US_ASCII));
    }

    private void writeInt(ByteArrayOutputStream baos, int value) throws IOException {
        baos.write(value & 0xFF);
        baos.write((value >> 8) & 0xFF);
        baos.write((value >> 16) & 0xFF);
        baos.write((value >> 24) & 0xFF);
    }

    private void writeShort(ByteArrayOutputStream baos, short value) throws IOException {
        baos.write(value & 0xFF);
        baos.write((value >> 8) & 0xFF);
    }

    /**
     * Đảm bảo thư mục tồn tại (synchronized để thread-safe)
     */
    private synchronized void ensureStorageDirectoryExists() {
        try {
            Path storageDir = Paths.get(audioStoragePath);
            if (!Files.exists(storageDir)) {
                Files.createDirectories(storageDir);
                logger.warn("⚠️ Thư mục storage chưa tồn tại, đã tạo mới: {}", storageDir.toAbsolutePath());
                
                // Tạo lại fallback audio nếu cần
                createGeneralFallbackAudio();
            }
        } catch (IOException e) {
            logger.error("❌ Không thể tạo thư mục audio storage: {}", e.getMessage());
            throw new RuntimeException("Không thể tạo thư mục lưu trữ", e);
        }
    }

    /**
     * Tạo tên file duy nhất
     */
    private String generateFileName(String uniqueId, String audioUrl) {
        String extension = getFileExtension(audioUrl);
        String timestamp = String.valueOf(System.currentTimeMillis());
        
        // Làm sạch uniqueId để tránh ký tự đặc biệt
        String cleanId = uniqueId.replaceAll("[^a-zA-Z0-9_-]", "_");
        
        return "audio_" + cleanId + "_" + timestamp + extension;
    }

    /**
     * Lấy phần mở rộng file từ URL
     */
    private String getFileExtension(String url) {
        if (url == null) return ".mp3";
        
        if (url.contains(".mp3")) return ".mp3";
        if (url.contains(".wav")) return ".wav";
        if (url.contains(".ogg")) return ".ogg";
        if (url.contains(".m4a")) return ".m4a";
        
        return ".mp3"; // Mặc định
    }

    /**
     * Xóa file audio (nếu cần)
     */
    public boolean deleteAudioFile(String fileName) {
        try {
            Path filePath = Paths.get(audioStoragePath, fileName);
            if (Files.exists(filePath)) {
                long fileSize = Files.size(filePath);
                Files.delete(filePath);
                logger.info("✅ Đã xóa file audio: {} ({} bytes)", fileName, fileSize);
                return true;
            }
            logger.warn("⚠️ File audio không tồn tại: {}", fileName);
            return false;
        } catch (Exception e) {
            logger.error("❌ Lỗi khi xóa file audio: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Xóa tất cả file fallback cũ (maintenance)
     */
    public int cleanupOldFallbackFiles(int daysOld) {
        try {
            Path storageDir = Paths.get(audioStoragePath);
            if (!Files.exists(storageDir)) return 0;

            long cutoffTime = System.currentTimeMillis() - (daysOld * 24L * 60 * 60 * 1000);
            AtomicInteger deletedCount = new AtomicInteger(0);

            Files.list(storageDir)
                .filter(path -> {
                    String fileName = path.getFileName().toString();
                    return fileName.startsWith("fallback_") && 
                           fileName.endsWith(".mp3") &&
                           !fileName.equals("fallback_general.mp3");
                })
                .forEach(path -> {
                    try {
                        if (Files.getLastModifiedTime(path).toMillis() < cutoffTime) {
                            Files.delete(path);
                            deletedCount.incrementAndGet();
                            logger.info("🧹 Đã xóa fallback file cũ: {}", path.getFileName());
                        }
                    } catch (Exception e) {
                        logger.warn("⚠️ Không thể xóa fallback file: {}", path.getFileName());
                    }
                });

            int count = deletedCount.get();
            logger.info("✅ Đã dọn dẹp {} file fallback cũ", count);
            return count;

        } catch (Exception e) {
            logger.error("❌ Lỗi khi dọn dẹp fallback files: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Lấy đường dẫn vật lý của file
     */
    public Path getAudioFilePath(String fileName) {
        return Paths.get(audioStoragePath, fileName);
    }

    /**
     * Kiểm tra file audio có tồn tại không
     */
    public boolean audioFileExists(String fileName) {
        try {
            Path filePath = Paths.get(audioStoragePath, fileName);
            return Files.exists(filePath) && Files.isRegularFile(filePath);
        } catch (Exception e) {
            logger.warn("⚠️ Lỗi khi kiểm tra file audio: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Lấy thông tin storage (cho API)
     */
    public StorageInfo getStorageInfo() {
        try {
            Path storageDir = Paths.get(audioStoragePath);
            if (Files.exists(storageDir)) {
                long fileCount = Files.list(storageDir)
                    .filter(path -> {
                        String fileName = path.getFileName().toString();
                        return !fileName.equals("README.txt") && 
                               Files.isRegularFile(path);
                    })
                    .count();
                
                // Đếm số file fallback riêng
                long fallbackCount = Files.list(storageDir)
                    .filter(path -> {
                        String fileName = path.getFileName().toString();
                        return fileName.startsWith("fallback_") && 
                               fileName.endsWith(".mp3");
                    })
                    .count();
                
                long totalSize = Files.walk(storageDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> !path.getFileName().toString().equals("README.txt"))
                    .mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (IOException e) {
                            return 0L;
                        }
                    })
                    .sum();
                
                return new StorageInfo(
                    storageDir.toAbsolutePath().toString(),
                    fileCount,
                    totalSize,
                    new Date(),
                    fallbackCount
                );
            }
        } catch (Exception e) {
            logger.error("❌ Lỗi khi lấy thông tin storage: {}", e.getMessage());
        }
        return new StorageInfo(audioStoragePath, 0, 0, new Date(), 0);
    }
    
    /**
     * Lấy danh sách tất cả file audio
     */
    public List<AudioFileInfo> getAllAudioFiles() {
        List<AudioFileInfo> audioFiles = new ArrayList<>();
        
        try {
            Path storageDir = Paths.get(audioStoragePath);
            if (Files.exists(storageDir)) {
                Files.list(storageDir)
                    .filter(path -> {
                        String fileName = path.getFileName().toString();
                        return !fileName.equals("README.txt") && 
                               !fileName.startsWith(".") &&
                               (fileName.endsWith(".mp3") || 
                                fileName.endsWith(".wav") || 
                                fileName.endsWith(".m4a") ||
                                fileName.endsWith(".ogg"));
                    })
                    .forEach(path -> {
                        try {
                            String fileName = path.getFileName().toString();
                            String fileType = fileName.startsWith("fallback_") ? "fallback" : "audio";
                            
                            AudioFileInfo info = new AudioFileInfo(
                                fileName,
                                Files.size(path),
                                Files.getLastModifiedTime(path).toMillis(),
                                "/api/audio/" + fileName,
                                fileType
                            );
                            audioFiles.add(info);
                        } catch (IOException e) {
                            logger.warn("⚠️ Không thể đọc thông tin file: {}", path.getFileName());
                        }
                    });
            }
        } catch (Exception e) {
            logger.error("❌ Lỗi khi lấy danh sách file audio: {}", e.getMessage());
        }
        
        // Sắp xếp theo thời gian tạo (mới nhất trước)
        audioFiles.sort((a, b) -> Long.compare(b.getLastModified(), a.getLastModified()));
        
        return audioFiles;
    }

    /**
     * DTO cho thông tin file audio
     */
    public static class AudioFileInfo {
        private final String fileName;
        private final long fileSize;
        private final long lastModified;
        private final String playUrl;
        private final String fileType; // "audio" hoặc "fallback"
        
        public AudioFileInfo(String fileName, long fileSize, long lastModified, String playUrl) {
            this(fileName, fileSize, lastModified, playUrl, "audio");
        }
        
        public AudioFileInfo(String fileName, long fileSize, long lastModified, String playUrl, String fileType) {
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.lastModified = lastModified;
            this.playUrl = playUrl;
            this.fileType = fileType;
        }
        
        // Getters
        public String getFileName() { return fileName; }
        public long getFileSize() { return fileSize; }
        public long getLastModified() { return lastModified; }
        public String getPlayUrl() { return playUrl; }
        public String getFileType() { return fileType; }
        public String getFormattedSize() { 
            if (fileSize < 1024) return fileSize + " B";
            else if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
            else return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }
        public String getFormattedDate() {
            return new Date(lastModified).toString();
        }
    }

    /**
     * DTO cho thông tin storage
     */
    public static class StorageInfo {
        private final String storagePath;
        private final long fileCount;
        private final long totalSize;
        private final Date lastChecked;
        private final long fallbackCount;

        public StorageInfo(String storagePath, long fileCount, long totalSize, Date lastChecked) {
            this(storagePath, fileCount, totalSize, lastChecked, 0);
        }
        
        public StorageInfo(String storagePath, long fileCount, long totalSize, Date lastChecked, long fallbackCount) {
            this.storagePath = storagePath;
            this.fileCount = fileCount;
            this.totalSize = totalSize;
            this.lastChecked = lastChecked;
            this.fallbackCount = fallbackCount;
        }

        // Getters
        public String getStoragePath() { return storagePath; }
        public long getFileCount() { return fileCount; }
        public long getTotalSize() { return totalSize; }
        public Date getLastChecked() { return lastChecked; }
        public long getFallbackCount() { return fallbackCount; }
        public String getFormattedSize() { 
            return String.format("%.2f MB", totalSize / (1024.0 * 1024.0)); 
        }
    }
    
    /**
     * ✅ Tạo audio tự động dựa trên thông tin bài hát
     */
    public String createMusicAudio(String uniqueId, String title, String artist) {
        try {
            Path storageDir = Paths.get(audioStoragePath);
            String fileName = "music_" + uniqueId + ".mp3";
            Path filePath = storageDir.resolve(fileName);

            if (!Files.exists(filePath)) {
                // ✅ TẠO AUDIO TỰ ĐỘNG VỚI ÂM NHẠC THỰC SỰ
                byte[] musicAudio = createRealMusicAudio(title, artist);
                Files.write(filePath, musicAudio);
                logger.info("🎵 Đã tạo music audio: {} - {} ({} bytes)", title, artist, musicAudio.length);
            }

            return "/api/audio/" + fileName;
        } catch (Exception e) {
            logger.error("❌ Không thể tạo music audio: {}", e.getMessage());
            return createIndividualFallbackAudio(uniqueId);
        }
    }

    /**
     * ✅ Tạo audio nhạc thực sự (không phải beep đơn giản)
     */
    private byte[] createRealMusicAudio(String title, String artist) {
        try {
            // Tạo melody đơn giản dựa trên tên bài hát và nghệ sĩ
            return createMelodyWav(30, 44100, 16, 2, generateMelodyFromText(title + artist));
        } catch (Exception e) {
            logger.warn("Không thể tạo music audio phức tạp, sử dụng tone đơn giản");
            return createMultiToneAudio(30); // 30 giây
        }
    }

    /**
     * ✅ Tạo melody từ text (dùng hash của text để tạo sequence nhạc)
     */
    private int[] generateMelodyFromText(String text) {
        // Dùng hash của text để tạo sequence nốt nhạc ổn định
        int hash = text.hashCode();
        int[] melody = new int[8]; // 8 nốt nhạc
        
        // Tần số các nốt nhạc cơ bản (C major scale)
        int[] notes = {262, 294, 330, 349, 392, 440, 494, 523}; // C4 to C5
        
        for (int i = 0; i < melody.length; i++) {
            int noteIndex = Math.abs((hash + i * 7) % notes.length);
            melody[i] = notes[noteIndex];
        }
        
        return melody;
    }

    /**
     * ✅ Tạo WAV với melody
     */
    private byte[] createMelodyWav(int seconds, int sampleRate, int bitsPerSample, int channels, int[] melody) {
        try {
            int byteRate = sampleRate * channels * bitsPerSample / 8;
            int blockAlign = channels * bitsPerSample / 8;
            int samplesPerChannel = seconds * sampleRate;
            int dataSize = seconds * sampleRate * blockAlign;
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            // WAV header
            writeString(baos, "RIFF");
            writeInt(baos, 36 + dataSize);
            writeString(baos, "WAVE");
            writeString(baos, "fmt ");
            writeInt(baos, 16);
            writeShort(baos, (short) 1);
            writeShort(baos, (short) channels);
            writeInt(baos, sampleRate);
            writeInt(baos, byteRate);
            writeShort(baos, (short) blockAlign);
            writeShort(baos, (short) bitsPerSample);
            writeString(baos, "data");
            writeInt(baos, dataSize);
            
            // ✅ TẠO MELODY
            double amplitude = 0.2 * Math.pow(2, bitsPerSample - 1) - 1;
            int notesPerSecond = 2; // 2 nốt mỗi giây
            int samplesPerNote = sampleRate / notesPerSecond;
            
            for (int i = 0; i < samplesPerChannel; i++) {
                int noteIndex = (i / samplesPerNote) % melody.length;
                double frequency = melody[noteIndex];
                double time = (double) i / sampleRate;
                
                double sample = Math.sin(2.0 * Math.PI * frequency * time) * amplitude;
                
                // Thêm harmonics để âm thanh phong phú hơn
                sample += Math.sin(2.0 * Math.PI * frequency * 2 * time) * amplitude * 0.3;
                sample += Math.sin(2.0 * Math.PI * frequency * 3 * time) * amplitude * 0.1;
                
                short pcmValue = (short) sample;
                
                for (int channel = 0; channel < channels; channel++) {
                    writeShort(baos, pcmValue);
                }
            }
            
            byte[] result = baos.toByteArray();
            logger.info("🎶 Đã tạo melody audio: {} bytes, {} giây", result.length, seconds);
            
            return result;
            
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo melody audio", e);
        }
    }

    /**
     * ✅ Tạo audio với nhiều tone (phong phú hơn)
     */
    private byte[] createMultiToneAudio(int seconds) {
        try {
            // Tạo chord với nhiều tần số
            int[] chords = {330, 392, 494}; // E minor chord
            
            int sampleRate = 44100;
            int bitsPerSample = 16;
            int channels = 2;
            int samplesPerChannel = seconds * sampleRate;
            int dataSize = seconds * sampleRate * channels * bitsPerSample / 8;
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            // WAV header
            writeString(baos, "RIFF");
            writeInt(baos, 36 + dataSize);
            writeString(baos, "WAVE");
            writeString(baos, "fmt ");
            writeInt(baos, 16);
            writeShort(baos, (short) 1);
            writeShort(baos, (short) channels);
            writeInt(baos, sampleRate);
            writeInt(baos, sampleRate * channels * bitsPerSample / 8);
            writeShort(baos, (short) (channels * bitsPerSample / 8));
            writeShort(baos, (short) bitsPerSample);
            writeString(baos, "data");
            writeInt(baos, dataSize);
            
            // ✅ TẠO CHORD
            double amplitude = 0.15 * Math.pow(2, bitsPerSample - 1) - 1;
            
            for (int i = 0; i < samplesPerChannel; i++) {
                double time = (double) i / sampleRate;
                double sample = 0;
                
                // Kết hợp nhiều tần số
                for (int freq : chords) {
                    sample += Math.sin(2.0 * Math.PI * freq * time) * amplitude;
                }
                
                sample /= chords.length; // Normalize
                
                short pcmValue = (short) sample;
                
                for (int channel = 0; channel < channels; channel++) {
                    writeShort(baos, pcmValue);
                }
            }
            
            return baos.toByteArray();
            
        } catch (Exception e) {
            logger.warn("Không thể tạo multi-tone audio, sử dụng fallback");
            return createRealAudioWithSound(seconds);
        }
    }
    
    
}