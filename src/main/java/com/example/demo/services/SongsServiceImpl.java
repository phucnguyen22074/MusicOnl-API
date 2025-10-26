package com.example.demo.services;

import com.example.demo.dto.SongsDTO;
import com.example.demo.entities.Albums;
import com.example.demo.entities.Artists;
import com.example.demo.entities.Genres;
import com.example.demo.entities.Songs;
import com.example.demo.repositories.AlbumRepository;
import com.example.demo.repositories.ArtistRepository;
import com.example.demo.repositories.GenreRepository;
import com.example.demo.repositories.SongRepository;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Service
public class SongsServiceImpl implements SongsService {

    private static final Logger logger = LoggerFactory.getLogger(SongsServiceImpl.class);
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final String[] ALLOWED_MUSIC_EXTENSIONS = {".mp3", ".wav"};
    private static final String[] ALLOWED_IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png"};

    @Autowired
    private SongRepository songRepo;

    @Autowired
    private AlbumRepository albumRepo;

    @Autowired
    private ArtistRepository artistRepo;

    @Autowired
    private GenreRepository genreRepo;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private Environment environment;

    @Override
    public List<SongsDTO> findAll() {
        try {
            List<Songs> songs = songRepo.findAll();
            return modelMapper.map(songs, new TypeToken<List<SongsDTO>>() {}.getType());
        } catch (Exception e) {
            logger.error("Error fetching all songs", e);
            throw new ServiceException("Failed to fetch songs", e);
        }
    }
    
    

    @Override
    public SongsDTO addSong(String title, Integer duration, String lyrics, String status,
                            Integer albumId, List<Integer> artistIds, List<Integer> genreIds,
                            MultipartFile file, MultipartFile image) {
        try {
            // Validate inputs
            if (title == null || title.trim().isEmpty()) {
                throw new IllegalArgumentException("Title is required");
            }
            if (duration != null && duration <= 0) {
                throw new IllegalArgumentException("Duration must be positive");
            }

            Songs song = new Songs();
            song.setTitle(title.trim());
            song.setDuration(duration);
            song.setLyrics(lyrics);
            song.setStatus(status);
            song.setCreatedAt(new Date());

            // Validate and set album
            if (albumId != null) {
                Albums album = albumRepo.findById(albumId)
                        .orElseThrow(() -> new NotFoundException("Album not found with ID: " + albumId));
                song.setAlbums(album);
            }

            // Validate and set artists
            if (artistIds != null && !artistIds.isEmpty()) {
                Set<Artists> artists = new HashSet<>(artistRepo.findAllById(artistIds));
                if (artists.size() != artistIds.size()) {
                    throw new NotFoundException("One or more artists not found");
                }
                song.setartists(artists);
            }

            // Validate and set genres
            if (genreIds != null && !genreIds.isEmpty()) {
                Set<Genres> genres = new HashSet<>(genreRepo.findAllById(genreIds));
                if (genres.size() != genreIds.size()) {
                    throw new NotFoundException("One or more genres not found");
                }
                song.setGenreses(genres);
            }

            // Validate and upload music file
            if (file != null && !file.isEmpty()) {
                validateFile(file, ALLOWED_MUSIC_EXTENSIONS, MAX_FILE_SIZE);
                String fileName = uploadFile(file, "/static/assets/musics");
                song.setFilePath(fileName);
            }

            // Validate and upload image
            if (image != null && !image.isEmpty()) {
                validateFile(image, ALLOWED_IMAGE_EXTENSIONS, MAX_FILE_SIZE);
                String fileName = uploadFile(image, "/static/assets/images");
                song.setImageUrl(fileName);
            }

            songRepo.save(song);
            return modelMapper.map(song, SongsDTO.class);

        } catch (IllegalArgumentException | NotFoundException e) {
            logger.error("Validation error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error adding song", e);
            throw new ServiceException("Failed to add song", e);
        }
    }

    @Override
    public SongsDTO updateSong(Integer id, String title, Integer duration, String lyrics, String status,
                               Integer albumId, List<Integer> artistIds, List<Integer> genreIds,
                               MultipartFile file, MultipartFile image) {
        try {
            // Validate song existence
            Songs song = songRepo.findById(id)
                    .orElseThrow(() -> new NotFoundException("Song not found with ID: " + id));

            // Validate and update fields
            if (title != null) {
                if (title.trim().isEmpty()) {
                    throw new IllegalArgumentException("Title cannot be empty");
                }
                song.setTitle(title.trim());
            }
            if (duration != null) {
                if (duration <= 0) {
                    throw new IllegalArgumentException("Duration must be positive");
                }
                song.setDuration(duration);
            }
            if (lyrics != null) song.setLyrics(lyrics);
            if (status != null) song.setStatus(status);

            // Validate and update album
            if (albumId != null) {
                Albums album = albumRepo.findById(albumId)
                        .orElseThrow(() -> new NotFoundException("Album not found with ID: " + albumId));
                song.setAlbums(album);
            }

            // Validate and update artists
            if (artistIds != null && !artistIds.isEmpty()) {
                Set<Artists> artists = new HashSet<>(artistRepo.findAllById(artistIds));
                if (artists.size() != artistIds.size()) {
                    throw new NotFoundException("One or more artists not found");
                }
                song.setartists(artists);
            }

            // Validate and update genres
            if (genreIds != null && !genreIds.isEmpty()) {
                Set<Genres> genres = new HashSet<>(genreRepo.findAllById(genreIds));
                if (genres.size() != genreIds.size()) {
                    throw new NotFoundException("One or more genres not found");
                }
                song.setGenreses(genres);
            }

            // Validate and update music file
            if (file != null && !file.isEmpty()) {
                validateFile(file, ALLOWED_MUSIC_EXTENSIONS, MAX_FILE_SIZE);
                String fileName = uploadFile(file, "/static/assets/musics");
                song.setFilePath(fileName);
            }

            // Validate and update image
            if (image != null && !image.isEmpty()) {
                validateFile(image, ALLOWED_IMAGE_EXTENSIONS, MAX_FILE_SIZE);
                String fileName = uploadFile(image, "/static/assets/images");
                song.setImageUrl(fileName);
            }

            songRepo.save(song);
            return modelMapper.map(song, SongsDTO.class);

        } catch (IllegalArgumentException | NotFoundException e) {
            logger.error("Validation error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error updating song with ID: {}", id, e);
            throw new ServiceException("Failed to update song", e);
        }
    }

    @Override
    public boolean deleteSong(Integer id) {
        try {
            Songs song = songRepo.findById(id)
                    .orElseThrow(() -> new NotFoundException("Song not found with ID: " + id));
            songRepo.delete(song);
            return true;
        } catch (NotFoundException e) {
            logger.error("Validation error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error deleting song with ID: {}", id, e);
            throw new ServiceException("Failed to delete song", e);
        }
    }
    
    @Override
    public List<SongsDTO> findByGenre(Integer genreId) {
        try {
            List<Songs> songs = songRepo.findByGenreses_GenreId(genreId);
            return modelMapper.map(songs, new TypeToken<List<SongsDTO>>() {}.getType());
        } catch (Exception e) {
            logger.error("Error fetching songs by genre", e);
            throw new ServiceException("Failed to fetch songs by genre", e);
        }
    }

    @Override
    public List<SongsDTO> findByArtist(Integer artistId) {
        try {
            if (!artistRepo.existsById(artistId)) throw new NotFoundException("Artist not found with ID: " + artistId);
            List<Songs> songs = songRepo.findByartists_ArtistId(artistId);
            return modelMapper.map(songs, new TypeToken<List<SongsDTO>>() {}.getType());
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error fetching songs by artist {}", artistId, e);
            throw new ServiceException("Failed to fetch songs by artist", e);
        }
    }


    @Override
    public List<SongsDTO> findByTitleContaining(String keyword) {
        try {
            List<Songs> songs = songRepo.findByTitleContainingIgnoreCase(keyword);
            return modelMapper.map(songs, new TypeToken<List<SongsDTO>>() {}.getType());
        } catch (Exception e) {
            logger.error("Error fetching songs by title", e);
            throw new ServiceException("Failed to fetch songs by title", e);
        }
    }


    private void validateFile(MultipartFile file, String[] allowedExtensions, long maxSize) {
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || !hasValidExtension(originalFileName, allowedExtensions)) {
            throw new IllegalArgumentException("Invalid file type. Allowed types: " + Arrays.toString(allowedExtensions));
        }
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File size exceeds limit of " + (maxSize / (1024 * 1024)) + "MB");
        }
    }

    private boolean hasValidExtension(String fileName, String[] allowedExtensions) {
        String extension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
        return Arrays.asList(allowedExtensions).contains(extension);
    }

    private String uploadFile(MultipartFile file, String uploadPath) throws IOException {
        File uploadFolder = new File(new ClassPathResource(".").getFile().getPath() + uploadPath);
        if (!uploadFolder.exists()) uploadFolder.mkdirs();

        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path path = Path.of(uploadFolder.getAbsolutePath(), fileName);
        Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
        return fileName;
    }
}

// Custom Exception Classes
class ServiceException extends RuntimeException {
    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}