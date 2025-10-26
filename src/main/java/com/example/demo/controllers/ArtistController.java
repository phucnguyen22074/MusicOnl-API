package com.example.demo.controllers;

import com.example.demo.dto.ArtistsDTO;
import com.example.demo.entities.Artists;
import com.example.demo.repositories.ArtistRepository;
import com.example.demo.services.ArtistService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/artists")
public class ArtistController {

    @Autowired
    private ArtistService artistService;
    
    @GetMapping("/all")
    public ResponseEntity<Iterable<ArtistsDTO>> findAllArtists() {
        return ResponseEntity.ok(artistService.findAllArtist());
    }

    @PostMapping("/add")
    public ResponseEntity<?> addArtist(
            @RequestParam("name") String name,
            @RequestParam(value = "bio", required = false) String bio,
            @RequestParam("image") MultipartFile imageFile) {

        String url = artistService.addArtist(name, bio, imageFile);
        if (url != null) {
            return ResponseEntity.ok("Artist created! Image URL: " + url);
        } else {
            return ResponseEntity.status(500).body("Failed to create artist");
        }
    }
    
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateArtist(
            @PathVariable("id") Integer id,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "bio", required = false) String bio,
            @RequestParam(value = "image", required = false) MultipartFile imageFile) {

        String url = artistService.updateArtist(id, name, bio, imageFile);
        if (url != null) {
            return ResponseEntity.ok("Artist updated! Image URL: " + url);
        } else {
            return ResponseEntity.status(404).body("Artist not found or update failed");
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteArtist(@PathVariable("id") Integer id) {
        boolean deleted = artistService.deleteArtist(id);
        if (deleted) {
            return ResponseEntity.ok("Artist deleted successfully");
        } else {
            return ResponseEntity.status(404).body("Artist not found or delete failed");
        }
    }
}
