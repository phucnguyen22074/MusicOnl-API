package com.example.demo.services;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.ArtistsDTO;
import com.example.demo.entities.Artists;
import com.example.demo.repositories.ArtistRepository;

@Service
public class ArtistServiceImpl implements ArtistService {

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private Environment environment;

    @Autowired
    private ModelMapper modelMapper;

    private final String[] allowedExtensions = {"jpg", "jpeg", "png", "gif"};

    // ================= ADD ARTIST =================
    @Override
    public String addArtist(String name, String bio, MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("File ảnh không được để trống");
            }

            // Kiểm tra đuôi file
            if (!isValidImageExtension(file.getOriginalFilename())) {
                throw new IllegalArgumentException("Chỉ cho phép các định dạng ảnh: jpg, jpeg, png, gif");
            }

            // Folder lưu file
            File uploadsFolder = new File(new ClassPathResource(".").getFile().getPath() + "/static/assets/artist_images");
            if (!uploadsFolder.exists()) {
                uploadsFolder.mkdirs();
            }

            // Tạo file name unique
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path path = Path.of(uploadsFolder.getAbsolutePath(), fileName);

            // Copy file vào folder
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

            // Tạo artist
            Artists artist = new Artists();
            artist.setName(name);
            artist.setBio(bio);
            artist.setImageUrl(fileName);
            artist.setCreatedAt(new Date());

            artistRepository.save(artist);

            return environment.getProperty("artist_images") + fileName;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ================= UPDATE ARTIST =================
    @Override
    public String updateArtist(Integer id, String name, String bio, MultipartFile file) {
        try {
            Artists artist = artistRepository.findById(id).orElse(null);
            if (artist == null) {
                return null;
            }

            if (name != null && !name.isBlank()) {
                artist.setName(name);
            }
            if (bio != null && !bio.isBlank()) {
                artist.setBio(bio);
            }

            // Nếu có file mới
            if (file != null && !file.isEmpty()) {
                if (!isValidImageExtension(file.getOriginalFilename())) {
                    throw new IllegalArgumentException("Chỉ cho phép các định dạng ảnh: jpg, jpeg, png, gif");
                }

                File uploadsFolder = new File(new ClassPathResource(".").getFile().getPath() + "/static/assets/artist_images");
                if (!uploadsFolder.exists()) {
                    uploadsFolder.mkdirs();
                }

                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                Path path = Path.of(uploadsFolder.getAbsolutePath(), fileName);
                Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

                // Xóa ảnh cũ nếu có
                if (artist.getImageUrl() != null) {
                    File oldFile = new File(uploadsFolder, artist.getImageUrl());
                    if (oldFile.exists()) {
                        oldFile.delete();
                    }
                }

                artist.setImageUrl(fileName);
            }

            artistRepository.save(artist);
            return environment.getProperty("artist_images") + artist.getImageUrl();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ================= DELETE ARTIST =================
    @Override
    public boolean deleteArtist(Integer id) {
        try {
            Artists artist = artistRepository.findById(id).orElse(null);
            if (artist == null) {
                return false;
            }

            if (artist.getImageUrl() != null) {
                File uploadsFolder = new File(new ClassPathResource(".").getFile().getPath() + "/static/assets/artist_images");
                File file = new File(uploadsFolder, artist.getImageUrl());
                if (file.exists()) {
                    file.delete();
                }
            }

            artistRepository.delete(artist);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ================= FIND ALL =================
    @Override
    public Iterable<ArtistsDTO> findAllArtist() {
        return modelMapper.map(artistRepository.findAll(), new TypeToken<List<ArtistsDTO>>() {
        }.getType());
    }

    // ================= HELPER =================
    private boolean isValidImageExtension(String fileName) {
        if (fileName == null) return false;
        String lower = fileName.toLowerCase();
        for (String ext : allowedExtensions) {
            if (lower.endsWith("." + ext)) {
                return true;
            }
        }
        return false;
    }
}
