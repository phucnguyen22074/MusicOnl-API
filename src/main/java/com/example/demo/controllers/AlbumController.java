package com.example.demo.controllers;

import com.example.demo.dto.AlbumsDTO;
import com.example.demo.services.AlbumsService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/albums")
public class AlbumController {

    @Autowired
    private AlbumsService albumService;

    @GetMapping("/all")
    public ResponseEntity<List<AlbumsDTO>> getAllAlbums() {
        return ResponseEntity.ok(albumService.findAll());
    }

    @PostMapping("/add")
    public ResponseEntity<?> addAlbum(
            @RequestParam("title") String title,
            @RequestParam(value = "artistId", required = false) Integer artistId,
            @RequestParam(value = "coverFile", required = false) MultipartFile coverFile) {
        AlbumsDTO album = albumService.addAlbum(title, artistId, coverFile);
        return ResponseEntity.ok(album);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateAlbum(
            @PathVariable Integer id,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "artistId", required = false) Integer artistId,
            @RequestParam(value = "coverFile", required = false) MultipartFile coverFile) {
        AlbumsDTO updated = albumService.updateAlbum(id, title, artistId, coverFile);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteAlbum(@PathVariable Integer id) {
        boolean deleted = albumService.deleteAlbum(id);
        return ResponseEntity.ok(deleted ? "Album deleted successfully!" : "Album not found!");
    }

    @GetMapping("/artist/{artistId}")
    public ResponseEntity<List<AlbumsDTO>> getAlbumsByArtist(@PathVariable Integer artistId) {
        return ResponseEntity.ok(albumService.findByArtist(artistId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<AlbumsDTO>> searchAlbums(@RequestParam String keyword) {
        return ResponseEntity.ok(albumService.searchByTitle(keyword));
    }
}
