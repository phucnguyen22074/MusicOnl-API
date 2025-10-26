package com.example.demo.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.SongsDTO;
import com.example.demo.services.LikeService;

import jakarta.persistence.criteria.CriteriaBuilder.In;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;



@RestController
@RequestMapping("/api/likes")
public class LikesController {
	
	@Autowired
	private LikeService likesService;
	
	@PostMapping("/like")
    public ResponseEntity<?> likeSong(@RequestParam Integer userId, @RequestParam Integer songId) {
        boolean liked = likesService.likeSong(userId, songId);
        if (liked) return ResponseEntity.ok("Song liked successfully!");
        else return ResponseEntity.badRequest().body("Already liked or invalid user/song!");
    }
	
	@DeleteMapping("/unlike")
    public ResponseEntity<?> unlikeSong(@RequestParam Integer userId, @RequestParam Integer songId) {
        boolean unliked = likesService.unlikeSong(userId, songId);
        if (unliked) return ResponseEntity.ok("Song unliked successfully!");
        else return ResponseEntity.badRequest().body("Like not found or invalid data!");
    }
	
	@GetMapping("is-liked")
	public ResponseEntity<?> isLiked(@RequestParam Integer userId, @RequestParam Integer songId) {
		boolean liked = likesService.isLiked(userId, songId);
		return ResponseEntity.ok(liked);
	}
	
	@GetMapping("/count/{songId}")
    public ResponseEntity<?> countLikes(@PathVariable Integer songId) {
        int count = likesService.countLikesBySong(songId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SongsDTO>> getLikedSongsByUser(@PathVariable Integer userId) {
        List<SongsDTO> songs = likesService.getLikedSongsByUser(userId);
        return ResponseEntity.ok(songs);
    }
	
}
