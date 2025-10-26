package com.example.demo.controllers;

import com.example.demo.dto.PlaylistDTO;
import com.example.demo.services.PlaylistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {

	@Autowired
	private PlaylistService playlistService;

	@GetMapping("/all")
	public ResponseEntity<List<PlaylistDTO>> getAll() {
		return ResponseEntity.ok(playlistService.findAll());
	}

	@GetMapping("/user/{userId}")
	public ResponseEntity<List<PlaylistDTO>> getByUser(@PathVariable Integer userId) {
		return ResponseEntity.ok(playlistService.findByUser(userId));
	}

	@PostMapping("/add")
	public ResponseEntity<?> addPlaylist(@RequestParam Integer userId, @RequestParam String name,
			@RequestParam(required = false) String description) {
		PlaylistDTO dto = playlistService.addPlaylist(userId, name, description);
		if (dto == null)
			return ResponseEntity.badRequest().body("User not found");
		return ResponseEntity.ok(dto);
	}

	@PutMapping("/update/{id}")
	public ResponseEntity<?> updatePlaylist(@PathVariable Integer id, @RequestParam(required = false) String name,
			@RequestParam(required = false) String description) {
		PlaylistDTO dto = playlistService.updatePlaylist(id, name, description);
		if (dto == null)
			return ResponseEntity.notFound().build();
		return ResponseEntity.ok(dto);
	}

	@DeleteMapping("/delete/{id}")
	public ResponseEntity<?> deletePlaylist(@PathVariable Integer id) {
		boolean deleted = playlistService.deletePlaylist(id);
		if (!deleted)
			return ResponseEntity.notFound().build();
		return ResponseEntity.ok("Playlist deleted successfully");
	}

	// Add song to playlist
	@PostMapping("/{playlistId}/add-song/{songId}")
	public ResponseEntity<?> addSong(@PathVariable Integer playlistId, @PathVariable Integer songId) {
		boolean added = playlistService.addSongToPlaylist(playlistId, songId);
		if (!added)
			return ResponseEntity.badRequest().body("Playlist or Song not found");
		return ResponseEntity.ok("Song added to playlist");
	}

	// Remove song from playlist
	@DeleteMapping("/{playlistId}/remove-song/{songId}")
	public ResponseEntity<?> removeSong(@PathVariable Integer playlistId, @PathVariable Integer songId) {
		boolean removed = playlistService.removeSongFromPlaylist(playlistId, songId);
		if (!removed)
			return ResponseEntity.badRequest().body("Playlist or Song not found");
		return ResponseEntity.ok("Song removed from playlist");
	}
}
