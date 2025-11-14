package com.example.demo.controllers;

import com.example.demo.dto.ArtistRequestDTO;
import com.example.demo.dto.ArtistsDTO;
import com.example.demo.services.ArtistService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/artists")
public class ArtistController {

    @Autowired
    private ArtistService artistService;

    // ================= GET ALL WITH PAGINATION =================
    @GetMapping("/all")
    public ResponseEntity<List<ArtistsDTO>> getAllArtists(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(artistService.findAllArtist(pageable));
    }

    // ================= ADD ARTIST =================
    @PostMapping("/add")
    public ResponseEntity<ArtistsDTO> addArtist(
            @Valid @ModelAttribute ArtistRequestDTO request,
            @RequestParam(value = "image", required = false) MultipartFile imageFile) {

        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException("File ảnh không được để trống");
        }

        ArtistsDTO artist = artistService.addArtist(request.getName(), request.getBio(), imageFile);
        return ResponseEntity.status(HttpStatus.CREATED).body(artist);
    }

    // ================= UPDATE ARTIST =================
    @PutMapping("/update/{id}")
    public ResponseEntity<ArtistsDTO> updateArtist(
            @PathVariable Integer id,
            @Valid @ModelAttribute ArtistRequestDTO request,
            @RequestParam(value = "image", required = false) MultipartFile imageFile) {

        ArtistsDTO updated = artistService.updateArtist(id, request.getName(), request.getBio(), imageFile);
        return ResponseEntity.ok(updated);
    }

    // ================= DELETE ARTIST =================
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, String>> deleteArtist(@PathVariable Integer id) {
        boolean deleted = artistService.deleteArtist(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", deleted ? "Artist deleted successfully!" : "Artist not found!");
        return ResponseEntity.ok(response);
    }
}