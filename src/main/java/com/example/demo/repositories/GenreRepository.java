package com.example.demo.repositories;

import com.example.demo.entities.Genres;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GenreRepository extends JpaRepository<Genres, Integer> {
    Optional<Genres> findByName(String name);
}
