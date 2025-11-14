package com.example.demo.controllers;

import com.example.demo.dto.GenresDTO;
import com.example.demo.services.GenresService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/genres")
@CrossOrigin("*") // Thêm CORS nếu cần
public class GenresController {

    @Autowired
    private GenresService genresService;

    // GET ALL
    @GetMapping("/all")
    public ResponseEntity<List<GenresDTO>> getAllGenres() {
        return ResponseEntity.ok(genresService.findAll());
    }

    // ADD - Sử dụng @RequestBody để nhận JSON
    @PostMapping("/add")
    public ResponseEntity<?> addGenre(@RequestBody GenresDTO genreDTO) {
        try {
            GenresDTO dto = genresService.addGenre(
                genreDTO.getName(), 
                genreDTO.getColor(), 
                genreDTO.getIcon()
            );
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error adding genre: " + e.getMessage());
        }
    }

    // UPDATE - Sử dụng @RequestBody để nhận JSON
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateGenre(
            @PathVariable("id") Integer id,
            @RequestBody GenresDTO genreDTO) {
        try {
            GenresDTO dto = genresService.updateGenre(
                id, 
                genreDTO.getName(), 
                genreDTO.getColor(), 
                genreDTO.getIcon()
            );
            if (dto != null) {
                return ResponseEntity.ok(dto);
            }
            return ResponseEntity.status(404).body("Genre not found");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating genre: " + e.getMessage());
        }
    }

    // DELETE
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteGenre(@PathVariable("id") Integer id) {
        try {
            boolean deleted = genresService.deleteGenre(id);
            if (deleted) {
                return ResponseEntity.ok("Genre deleted successfully");
            }
            return ResponseEntity.status(404).body("Genre not found");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting genre: " + e.getMessage());
        }
    }
}