package com.example.demo.controllers;

import com.example.demo.dto.LikeRequestDTO;
import com.example.demo.dto.SongsDTO;
import com.example.demo.services.LikeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/likes")
public class LikesController {

    @Autowired
    private LikeService likeService;

    // ================= LIKE SONG =================
    @PostMapping("/like")
    public ResponseEntity<Map<String, Object>> likeSong(@Valid @RequestBody LikeRequestDTO request) {
        boolean liked = likeService.likeSong(request.getUserId(), request.getSongId());
        Map<String, Object> response = new HashMap<>();
        if (liked) {
            response.put("message", "Song liked successfully!");
            response.put("liked", true);
            return ResponseEntity.ok(response);
        } else {
            response.put("message", "Already liked or invalid user/song!");
            response.put("liked", false);
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ================= UNLIKE SONG =================
    @DeleteMapping("/unlike")
    public ResponseEntity<Map<String, Object>> unlikeSong(@Valid @RequestBody LikeRequestDTO request) {
        boolean unliked = likeService.unlikeSong(request.getUserId(), request.getSongId());
        Map<String, Object> response = new HashMap<>();
        if (unliked) {
            response.put("message", "Song unliked successfully!");
            response.put("unliked", true);
            return ResponseEntity.ok(response);
        } else {
            response.put("message", "Like not found or invalid data!");
            response.put("unliked", false);
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ================= CHECK IF LIKED =================
    @GetMapping("/is-liked")
    public ResponseEntity<Map<String, Boolean>> isLiked(@Valid @ModelAttribute LikeRequestDTO request) {
        boolean liked = likeService.isLiked(request.getUserId(), request.getSongId());
        Map<String, Boolean> response = new HashMap<>();
        response.put("liked", liked);
        return ResponseEntity.ok(response);
    }

    // ================= COUNT LIKES =================
    @GetMapping("/count/{songId}")
    public ResponseEntity<Map<String, Integer>> countLikes(@PathVariable Integer songId) {
        int count = likeService.countLikesBySong(songId);
        Map<String, Integer> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    // ================= GET LIKED SONGS BY USER =================
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SongsDTO>> getLikedSongsByUser(@PathVariable Integer userId) {
        List<SongsDTO> songs = likeService.getLikedSongsByUser(userId);
        return ResponseEntity.ok(songs);
    }
}