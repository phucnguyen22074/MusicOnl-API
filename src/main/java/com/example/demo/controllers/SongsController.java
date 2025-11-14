package com.example.demo.controllers;

import com.example.demo.dto.SongRequestDTO;
import com.example.demo.dto.SongsDTO;
import com.example.demo.services.SongsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/songs")
public class SongsController {

	@Autowired
	private SongsService songsService;

	// ================= GET ALL =================
	@GetMapping("/all")
	public ResponseEntity<List<SongsDTO>> getAllSongs() {
		return ResponseEntity.ok(songsService.findAll());
	}

	// ================= ADD SONG =================
	@PostMapping("/add")
	public ResponseEntity<?> addSong(@Valid @ModelAttribute SongRequestDTO request) {
		// Validate file nhạc bắt buộc (MultipartFile không validate bằng @Valid)
		if (request.getFile() == null || request.getFile().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error", "File nhạc là bắt buộc"));
		}

		SongsDTO song = songsService.addSong(request.getTitle(), request.getDuration(), request.getLyrics(),
				request.getStatus(), request.getAlbumId(), request.getArtistIds(), request.getGenreIds(),
				request.getFile(), request.getImage());
		return ResponseEntity.status(HttpStatus.CREATED).body(song);
	}

	// ================= UPDATE listenCount SONG =================
	@PostMapping("/{id}/listen")
	public ResponseEntity<Void> incrementListenCount(@PathVariable Integer id) {
		songsService.incrementListenCount(id);
		return ResponseEntity.noContent().build();
	}

	// ================= UPDATE SONG =================
	@PutMapping("/update/{id}")
	public ResponseEntity<?> updateSong(@PathVariable Integer id, @ModelAttribute SongRequestDTO request) {

		SongsDTO song = songsService.updateSong(id, request.getTitle(), request.getDuration(), request.getLyrics(),
				request.getStatus(), request.getAlbumId(), request.getArtistIds(), request.getGenreIds(),
				request.getFile(), request.getImage());
		return ResponseEntity.ok(song);
	}

	// ================= DELETE SONG =================
	@DeleteMapping("/delete/{id}")
	public ResponseEntity<Map<String, String>> deleteSong(@PathVariable Integer id) {
		boolean deleted = songsService.deleteSong(id);
		Map<String, String> response = new HashMap<>();
		response.put("message", deleted ? "Song deleted successfully!" : "Song not found!");
		return ResponseEntity.ok(response);
	}

	// ================= FILTER SONGS =================
	@GetMapping("/filter/genre/{genreId}")
	public ResponseEntity<List<SongsDTO>> getSongsByGenre(@PathVariable Integer genreId) {
		return ResponseEntity.ok(songsService.findByGenre(genreId));
	}

	@GetMapping("/filter/artist/{artistId}")
	public ResponseEntity<List<SongsDTO>> getSongsByArtist(@PathVariable Integer artistId) {
		return ResponseEntity.ok(songsService.findByArtist(artistId));
	}

	@GetMapping("/filter/title")
	public ResponseEntity<List<SongsDTO>> getSongsByTitle(@RequestParam("q") String keyword) {
		return ResponseEntity.ok(songsService.findByTitleContaining(keyword));
	}
}