package com.example.demo.services;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.AlbumsDTO;
import com.example.demo.dto.SongsDTO;
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

    private static final Logger logger = LoggerFactory.getLogger(AlbumsServiceImpl.class);
    private static final String[] ALLOWED_IMAGE_EXT = {".jpg", ".jpeg", ".png"};
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    
    @Value("${file.upload-dir:/static/assets/album_images}")
    private String uploadDir;

    @Override
    public List<AlbumsDTO> findAll() {
        List<Albums> albums = albumRepo.findAll();
        return modelMapper.map(albums, new TypeToken<List<AlbumsDTO>>(){}.getType());
    }
    
    @Override
    public List<AlbumsDTO> findAll(Pageable pageable) {
        return albumRepo.findAllWithRelations(pageable)
                .getContent()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    @Override
    public AlbumsDTO addAlbum(String title, Integer artistId, MultipartFile coverFile) {
        if(title == null || title.isBlank()) throw new IllegalArgumentException("Title cannot be emty");
        if (albumRepo.existsByTitle(title)) 
            throw new IllegalArgumentException("Album with title '" + title + "' already exists");
        Albums album = new Albums();
        album.setReleaseDate(new Date());
        return saveAlbum(album, title, artistId, coverFile);
    }

    @Transactional
    @Override
    public AlbumsDTO updateAlbum(Integer id, String title, Integer artistId, MultipartFile coverFile) {
    	Albums album = albumRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Album not found"));
            return saveAlbum(album, title, artistId, coverFile);
    }

    @Transactional
    @Override
    public boolean deleteAlbum(Integer id) {
        try {
            Albums album = albumRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Album not found"));
            albumRepo.delete(album);
            return true;
        } catch (Exception e) {
            logger.error("Failed to delete album with ID: {}", id, e);
            return false;
        }
    }

    @Override
    public List<AlbumsDTO> findByArtist(Integer artistId) {
        List<Albums> albums = albumRepo.findByArtists_ArtistId(artistId);
        return modelMapper.map(albums, new TypeToken<List<AlbumsDTO>>(){}.getType());
    }
    
    @Override
    public List<AlbumsDTO> findByArtist(Integer artistId, Pageable pageable) {
        Page<Albums> albumsPage = albumRepo.findByArtists_ArtistId(artistId, pageable);
        return modelMapper.map(albumsPage.getContent(), new TypeToken<List<AlbumsDTO>>(){}.getType());
    }

    @Override
    public List<AlbumsDTO> searchByTitle(String keyword) {
        List<Albums> albums = albumRepo.findByTitleContainingIgnoreCase(keyword);
        return modelMapper.map(albums, new TypeToken<List<AlbumsDTO>>(){}.getType());
    }

    private String uploadCover(MultipartFile file) throws Exception {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds limit of 5MB");
        }

        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename().replace(" ", "_");
        String ext = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();

        if (!Arrays.asList(ALLOWED_IMAGE_EXT).contains(ext)) {
            throw new IllegalArgumentException("Invalid file type (allowed: jpg, jpeg, png)");
        }

        Path uploadPath = Path.of(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path dest = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

        return fileName;
    }
    
    private AlbumsDTO saveAlbum(Albums album, String title, Integer artistId, MultipartFile coverFile) {
        try {
            if (title != null && !title.isBlank()) {
                album.setTitle(title);
            }

            if (artistId != null) {
                Artists artist = artistRepo.findById(artistId)
                    .orElseThrow(() -> new IllegalArgumentException("Artist with ID " + artistId + " not found"));
                album.setArtists(artist);
            }

            if (coverFile != null && !coverFile.isEmpty()) {
                String fileName = uploadCover(coverFile);
                album.setCoverUrl(fileName);
            }

            albumRepo.save(album);
            return modelMapper.map(album, AlbumsDTO.class);
        } catch (Exception e) {
            logger.error("Failed to save album", e);
            throw new RuntimeException("Failed to save album: " + e.getMessage());
        }
    }
    
    private AlbumsDTO toDto(Albums album) {
        AlbumsDTO dto = modelMapper.map(album, AlbumsDTO.class);

        // Set artist
        if (album.getArtists() != null) {
            dto.setArtistId(album.getArtists().getArtistId());
            dto.setArtistName(album.getArtists().getName());
        }

        // Set songs (đã được fetch)
        Set<SongsDTO> songDtos = album.getSongses().stream()
                .map(song -> modelMapper.map(song, SongsDTO.class))
                .collect(Collectors.toSet());
        dto.setSongs(songDtos);

        return dto;
    }
}
