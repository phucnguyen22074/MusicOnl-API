package com.example.demo.controllers;

import com.example.demo.dto.SongsDTO;
import com.example.demo.services.SongsService;
import com.google.zxing.NotFoundException;

import org.hibernate.service.spi.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.List;

@RestController
@RequestMapping("/api/songs")
public class SongsController {

    private static final Logger logger = LoggerFactory.getLogger(SongsController.class);

    @Autowired
    private SongsService songsService;

    // ================= GET ALL =================
    @GetMapping("/all")
    public ResponseEntity<List<SongsDTO>> getAllSongs() {
        List<SongsDTO> songs = songsService.findAll();
        return ResponseEntity.ok(songs);
    }

    // ================= ADD SONG =================
    @PostMapping("/add")
    public ResponseEntity<?> addSong(@Valid @ModelAttribute SongRequestDTO request) {
        SongsDTO song = songsService.addSong(
                request.getTitle(),
                request.getDuration(),
                request.getLyrics(),
                request.getStatus(),
                request.getAlbumId(),
                request.getArtistIds(),
                request.getGenreIds(),
                request.getFile(),
                request.getImage()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(song);
    }

    // ================= UPDATE SONG =================
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateSong(@PathVariable("id") Integer id, @ModelAttribute SongRequestDTO request) {
        SongsDTO song = songsService.updateSong(
                id,
                request.getTitle(),
                request.getDuration(),
                request.getLyrics(),
                request.getStatus(),
                request.getAlbumId(),
                request.getArtistIds(),
                request.getGenreIds(),
                request.getFile(),
                request.getImage()
        );
        return ResponseEntity.ok(song);
    }

    // ================= DELETE SONG =================
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteSong(@PathVariable("id") Integer id) {
        boolean deleted = songsService.deleteSong(id);
        return ResponseEntity.ok("Song deleted successfully");
    }
    
 // ================= FILTER SONGS =================
    @GetMapping("/filter/genre/{genreId}")
    public ResponseEntity<List<SongsDTO>> getSongsByGenre(@PathVariable Integer genreId) {
        List<SongsDTO> songs = songsService.findByGenre(genreId);
        return ResponseEntity.ok(songs);
    }

    @GetMapping("/filter/artist/{artistId}")
    public ResponseEntity<List<SongsDTO>> getSongsByArtist(@PathVariable Integer artistId) {
        List<SongsDTO> songs = songsService.findByArtist(artistId);
        return ResponseEntity.ok(songs);
    }

    @GetMapping("/filter/title")
    public ResponseEntity<List<SongsDTO>> getSongsByTitle(@RequestParam("q") String keyword) {
        List<SongsDTO> songs = songsService.findByTitleContaining(keyword);
        return ResponseEntity.ok(songs);
    }


    // ================= EXCEPTION HANDLING =================
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.error("Validation error: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException ex) {
        logger.error("Not found error: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ErrorResponse> handleServiceException(ServiceException ex) {
        logger.error("Service error: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An error occurred while processing the request");
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        logger.error("Unexpected error: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An unexpected error occurred");
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

// DTO for Request Validation
class SongRequestDTO {
    @NotBlank(message = "Title is required")
    private String title;

    @Positive(message = "Duration must be positive")
    private Integer duration;

    private String lyrics;
    private String status;
    private Integer albumId;
    private List<Integer> artistIds;
    private List<Integer> genreIds;
    private MultipartFile file;
    private MultipartFile image;

    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    public String getLyrics() { return lyrics; }
    public void setLyrics(String lyrics) { this.lyrics = lyrics; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getAlbumId() { return albumId; }
    public void setAlbumId(Integer albumId) { this.albumId = albumId; }
    public List<Integer> getArtistIds() { return artistIds; }
    public void setArtistIds(List<Integer> artistIds) { this.artistIds = artistIds; }
    public List<Integer> getGenreIds() { return genreIds; }
    public void setGenreIds(List<Integer> genreIds) { this.genreIds = genreIds; }
    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }
    public MultipartFile getImage() { return image; }
    public void setImage(MultipartFile image) { this.image = image; }
}

// Error Response Class
class ErrorResponse {
    private int status;
    private String message;

    public ErrorResponse(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}