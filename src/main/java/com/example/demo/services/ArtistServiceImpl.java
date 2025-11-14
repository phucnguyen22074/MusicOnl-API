package com.example.demo.services;

import com.example.demo.dto.ArtistsDTO;
import com.example.demo.entities.Artists;
import com.example.demo.repositories.ArtistRepository;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
public class ArtistServiceImpl implements ArtistService {

    private static final Logger logger = LoggerFactory.getLogger(ArtistServiceImpl.class);
    private static final String[] ALLOWED_EXT = {".jpg", ".jpeg", ".png", ".gif"};
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Value("${app.upload.artist-dir}")
    private String uploadDir;

    @Value("${artist_images}")
    private String imageUrlPrefix;

    @Value("${app.upload.default-image:no-image.jpg}")
    private String defaultImageName;

    // ================= ADD ARTIST =================
    @Transactional
    @Override
    public ArtistsDTO addArtist(String name, String bio, MultipartFile file) {
        validateAddArtist(name, file);
        String fileName = uploadOrUseDefault(file);
        Artists artist = createArtist(name, bio, fileName);
        artistRepository.save(artist);
        ArtistsDTO dto = modelMapper.map(artist, ArtistsDTO.class);
        dto.setImageUrl(imageUrlPrefix + fileName);
        return dto;
    }

    // ================= UPDATE ARTIST =================
    @Transactional
    @Override
    public ArtistsDTO updateArtist(Integer id, String name, String bio, MultipartFile file) {
        Artists artist = artistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Artist not found with ID: " + id));

        if (name != null && !name.isBlank()) artist.setName(name);
        if (bio != null && !bio.isBlank()) artist.setBio(bio);

        String newFileName = null;
        if (file != null && !file.isEmpty()) {
            validateImageFile(file);
            newFileName = uploadImage(file);
            deleteOldImage(artist.getImageUrl());
            artist.setImageUrl(newFileName);
        } else if (artist.getImageUrl() == null || artist.getImageUrl().isBlank()) {
            // Nếu không có ảnh cũ và không upload mới → dùng ảnh mặc định
            newFileName = defaultImageName;
            artist.setImageUrl(newFileName);
            ensureDefaultImageExists(); // Tạo ảnh mặc định nếu chưa có
        }

        artistRepository.save(artist);
        ArtistsDTO dto = modelMapper.map(artist, ArtistsDTO.class);
        String finalImage = (newFileName != null) ? newFileName : artist.getImageUrl();
        dto.setImageUrl(imageUrlPrefix + finalImage);
        return dto;
    }

    // ================= DELETE ARTIST =================
    @Transactional
    @Override
    public boolean deleteArtist(Integer id) {
        return artistRepository.findById(id).map(artist -> {
            deleteOldImage(artist.getImageUrl());
            artistRepository.delete(artist);
            return true;
        }).orElse(false);
    }

    // ================= FIND ALL =================
    @Override
    public List<ArtistsDTO> findAllArtist() {
        List<Artists> artists = artistRepository.findAll();
        return enhanceImageUrls(artists);
    }

    @Override
    public List<ArtistsDTO> findAllArtist(Pageable pageable) {
        Page<Artists> page = artistRepository.findAll(pageable);
        return enhanceImageUrls(page.getContent());
    }

    private List<ArtistsDTO> enhanceImageUrls(List<Artists> artists) {
        return artists.stream()
                .map(artist -> {
                    ArtistsDTO dto = modelMapper.map(artist, ArtistsDTO.class);
                    String img = artist.getImageUrl();
                    if (img == null || img.isBlank()) {
                        img = defaultImageName;
                        ensureDefaultImageExists();
                    }
                    dto.setImageUrl(imageUrlPrefix + img);
                    return dto;
                })
                .toList();
    }

    // ================= HELPER METHODS =================

    private void validateAddArtist(String name, MultipartFile file) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên nghệ sĩ không được để trống");
        }
        // Không bắt buộc file nữa → dùng ảnh mặc định
    }

    private void validateImageFile(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Kích thước file vượt quá 5MB");
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || !hasValidExtension(fileName)) {
            throw new IllegalArgumentException("Chỉ cho phép định dạng: jpg, jpeg, png, gif");
        }
    }

    private boolean hasValidExtension(String fileName) {
        String lowerName = fileName.toLowerCase();
        return Arrays.stream(ALLOWED_EXT).anyMatch(lowerName::endsWith);
    }

    private String uploadOrUseDefault(MultipartFile file) {
        if (file != null && !file.isEmpty()) {
            validateImageFile(file);
            return uploadImage(file);
        } else {
            ensureDefaultImageExists();
            return defaultImageName;
        }
    }

    private String uploadImage(MultipartFile file) {
        try {
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename().replace(" ", "_");
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            Path dest = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (Exception e) {
            logger.error("Lỗi upload ảnh nghệ sĩ", e);
            throw new RuntimeException("Không thể lưu file ảnh", e);
        }
    }

    private void deleteOldImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank() || imageUrl.equals(defaultImageName)) return;
        try {
            Path filePath = Paths.get(uploadDir, imageUrl);
            Files.deleteIfExists(filePath);
        } catch (Exception e) {
            logger.warn("Không thể xóa ảnh cũ: {}", imageUrl, e);
        }
    }

    private Artists createArtist(String name, String bio, String imageUrl) {
        Artists artist = new Artists();
        artist.setName(name);
        artist.setBio(bio);
        artist.setImageUrl(imageUrl);
        artist.setCreatedAt(new Date());
        return artist;
    }

    // Tạo ảnh mặc định nếu chưa có
    private void ensureDefaultImageExists() {
        Path defaultPath = Paths.get(uploadDir, defaultImageName);
        if (Files.exists(defaultPath)) return;

        try {
            Path targetDir = Paths.get(uploadDir);
            Files.createDirectories(targetDir);

            // Copy từ classpath (src/main/resources/static/assets/no-image.jpg)
            var resource = getClass().getClassLoader().getResourceAsStream("static/assets/no-image.jpg");
            if (resource != null) {
                Files.copy(resource, defaultPath, StandardCopyOption.REPLACE_EXISTING);
            } else {
                // Tạo file rỗng nếu không có
                Files.createFile(defaultPath);
            }
        } catch (IOException e) {
            logger.warn("Không thể tạo ảnh mặc định: {}", defaultImageName, e);
        }
    }
}