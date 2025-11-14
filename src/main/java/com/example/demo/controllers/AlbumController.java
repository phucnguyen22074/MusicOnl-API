package com.example.demo.controllers;

import com.example.demo.dto.AlbumRequestDTO;
import com.example.demo.dto.AlbumsDTO;
import com.example.demo.services.AlbumsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/albums")
public class AlbumController {

    @Autowired
    private AlbumsService albumService;

    @GetMapping("/all")
    public ResponseEntity<List<AlbumsDTO>> getAllAlbums(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(albumService.findAll(pageable));
    }

    @PostMapping("/add")
    public ResponseEntity<AlbumsDTO> addAlbum(
            @Valid @ModelAttribute AlbumRequestDTO request,
            @RequestParam(value = "coverFile", required = false) MultipartFile coverFile) {
        AlbumsDTO album = albumService.addAlbum(request.getTitle(), request.getArtistId(), coverFile);
        return ResponseEntity.status(HttpStatus.CREATED).body(album);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<AlbumsDTO> updateAlbum(
            @PathVariable Integer id,
            @Valid @ModelAttribute AlbumRequestDTO request,
            @RequestParam(value = "coverFile", required = false) MultipartFile coverFile) {
        AlbumsDTO updated = albumService.updateAlbum(id, request.getTitle(), request.getArtistId(), coverFile);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, String>> deleteAlbum(@PathVariable Integer id) {
        boolean deleted = albumService.deleteAlbum(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", deleted ? "Album deleted successfully!" : "Album not found!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/artist/{artistId}")
    public ResponseEntity<List<AlbumsDTO>> getAlbumsByArtist(
            @PathVariable Integer artistId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(albumService.findByArtist(artistId, pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<List<AlbumsDTO>> searchAlbums(@RequestParam String keyword) {
        return ResponseEntity.ok(albumService.searchByTitle(keyword));
    }
}