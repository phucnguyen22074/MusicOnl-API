package com.example.demo.controllers;

import com.example.demo.dto.GenresDTO;
import com.example.demo.services.GenresService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/genres")
public class GenresController {

    @Autowired
    private GenresService genresService;

    // GET ALL
    @GetMapping("/all")
    public ResponseEntity<List<GenresDTO>> getAllGenres() {
        return ResponseEntity.ok(genresService.findAll());
    }

    // ADD
    @PostMapping("/add")
    public ResponseEntity<?> addGenre(
            @RequestParam("name") String name) {
        GenresDTO dto = genresService.addGenre(name);
        return ResponseEntity.ok(dto);
    }

    // UPDATE
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateGenre(
            @PathVariable("id") Integer id,
            @RequestParam(value = "name", required = false) String name) {
        GenresDTO dto = genresService.updateGenre(id, name);
        if (dto != null) {
            return ResponseEntity.ok(dto);
        }
        return ResponseEntity.status(404).body("Genre not found");
    }

    // DELETE
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteGenre(@PathVariable("id") Integer id) {
        boolean deleted = genresService.deleteGenre(id);
        if (deleted) {
            return ResponseEntity.ok("Genre deleted successfully");
        }
        return ResponseEntity.status(404).body("Genre not found");
    }
}
