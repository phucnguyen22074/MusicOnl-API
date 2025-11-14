package com.example.demo.controllers;

import com.example.demo.dto.PlaylistDTO;
import com.example.demo.dto.PlaylistRequestDTO;
import com.example.demo.services.PlaylistService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {

    @Autowired
    private PlaylistService playlistService;

    // ================= GET ALL =================
    @GetMapping("/all")
    public ResponseEntity<List<PlaylistDTO>> getAll() {
        return ResponseEntity.ok(playlistService.findAll());
    }

    // ================= GET BY USER =================
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PlaylistDTO>> getByUser(@PathVariable Integer userId) {
        return ResponseEntity.ok(playlistService.findByUser(userId));
    }

    // ================= ADD PLAYLIST =================
    @PostMapping("/add")
    public ResponseEntity<?> addPlaylist(@Valid @RequestBody PlaylistRequestDTO request) {
        try {
            PlaylistDTO dto = playlistService.addPlaylist(
                    request.getUserId(),
                    request.getName(),
                    request.getDescription()
            );
            return ResponseEntity.status(201).body(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ================= UPDATE PLAYLIST =================
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updatePlaylist(
            @PathVariable Integer id,
            @Valid @ModelAttribute PlaylistRequestDTO request) {
        try {
            PlaylistDTO dto = playlistService.updatePlaylist(
                    id,
                    request.getName(),
                    request.getDescription()
            );
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    // ================= DELETE PLAYLIST =================
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, String>> deletePlaylist(@PathVariable Integer id) {
        boolean deleted = playlistService.deletePlaylist(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", deleted ? "Playlist deleted successfully!" : "Playlist not found!");
        return ResponseEntity.ok(response);
    }

    // ================= ADD SONG TO PLAYLIST =================
    @PostMapping("/{playlistId}/add-song/{songId}")
    public ResponseEntity<Map<String, Object>> addSong(
            @PathVariable Integer playlistId,
            @PathVariable Integer songId) {
        boolean added = playlistService.addSongToPlaylist(playlistId, songId);
        Map<String, Object> response = new HashMap<>();
        if (added) {
            response.put("message", "Song added to playlist successfully!");
            response.put("added", true);
            return ResponseEntity.ok(response);
        } else {
            response.put("error", "Playlist or Song not found, or song already in playlist!");
            response.put("added", false);
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ================= REMOVE SONG FROM PLAYLIST =================
    @DeleteMapping("/{playlistId}/remove-song/{songId}")
    public ResponseEntity<Map<String, Object>> removeSong(
            @PathVariable Integer playlistId,
            @PathVariable Integer songId) {
        boolean removed = playlistService.removeSongFromPlaylist(playlistId, songId);
        Map<String, Object> response = new HashMap<>();
        if (removed) {
            response.put("message", "Song removed from playlist successfully!");
            response.put("removed", true);
            return ResponseEntity.ok(response);
        } else {
            response.put("error", "Playlist or Song not found, or song not in playlist!");
            response.put("removed", false);
            return ResponseEntity.badRequest().body(response);
        }
    }
}