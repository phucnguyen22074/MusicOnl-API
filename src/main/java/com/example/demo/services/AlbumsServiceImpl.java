package com.example.demo.services;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.AlbumsDTO;
import com.example.demo.entities.Albums;
import com.example.demo.entities.Artists;
import com.example.demo.repositories.AlbumRepository;
import com.example.demo.repositories.ArtistRepository;

@Service
public class AlbumsServiceImpl implements AlbumsService{
	@Autowired
    private AlbumRepository albumRepo;

    @Autowired
    private ArtistRepository artistRepo;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private Environment environment;

    private static final String[] ALLOWED_IMAGE_EXT = {".jpg", ".jpeg", ".png"};

    @Override
    public List<AlbumsDTO> findAll() {
        List<Albums> albums = albumRepo.findAll();
        return modelMapper.map(albums, new TypeToken<List<AlbumsDTO>>(){}.getType());
    }

    @Override
    public AlbumsDTO addAlbum(String title, Integer artistId, MultipartFile coverFile) {
        try {
            if (title == null || title.isBlank()) {
                throw new IllegalArgumentException("Title cannot be empty");
            }

            Albums album = new Albums();
            album.setTitle(title);
            album.setReleaseDate(new Date());

            if (artistId != null) {
                Artists artist = artistRepo.findById(artistId).orElse(null);
                if (artist != null) album.setArtists(artist);
            }

            if (coverFile != null && !coverFile.isEmpty()) {
                String fileName = uploadCover(coverFile);
                album.setCoverUrl(fileName);
            }

            albumRepo.save(album);
            return modelMapper.map(album, AlbumsDTO.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to add album");
        }
    }

    @Override
    public AlbumsDTO updateAlbum(Integer id, String title, Integer artistId, MultipartFile coverFile) {
        try {
            Albums album = albumRepo.findById(id).orElse(null);
            if (album == null) throw new RuntimeException("Album not found");

            if (title != null && !title.isBlank()) album.setTitle(title);

            if (artistId != null) {
                Artists artist = artistRepo.findById(artistId).orElse(null);
                if (artist != null) album.setArtists(artist);
            }

            if (coverFile != null && !coverFile.isEmpty()) {
                String fileName = uploadCover(coverFile);
                album.setCoverUrl(fileName);
            }

            albumRepo.save(album);
            return modelMapper.map(album, AlbumsDTO.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to update album");
        }
    }

    @Override
    public boolean deleteAlbum(Integer id) {
        try {
            Albums album = albumRepo.findById(id).orElse(null);
            if (album == null) return false;
            albumRepo.delete(album);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<AlbumsDTO> findByArtist(Integer artistId) {
        List<Albums> albums = albumRepo.findByArtists_ArtistId(artistId);
        return modelMapper.map(albums, new TypeToken<List<AlbumsDTO>>(){}.getType());
    }

    @Override
    public List<AlbumsDTO> searchByTitle(String keyword) {
        List<Albums> albums = albumRepo.findByTitleContainingIgnoreCase(keyword);
        return modelMapper.map(albums, new TypeToken<List<AlbumsDTO>>(){}.getType());
    }

    private String uploadCover(MultipartFile file) throws Exception {
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename().replace(" ", "_");
        String ext = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();

        if (!Arrays.asList(ALLOWED_IMAGE_EXT).contains(ext)) {
            throw new IllegalArgumentException("Invalid file type (allowed: jpg, jpeg, png)");
        }

        File uploadDir = new File(new ClassPathResource(".").getFile().getPath() + "/static/assets/album_images");
        if (!uploadDir.exists()) uploadDir.mkdirs();

        Path dest = Path.of(uploadDir.getAbsolutePath(), fileName);
        Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

        return fileName;
    }
}
