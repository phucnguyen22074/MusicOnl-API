package com.example.demo.repositories;

import com.example.demo.entities.Genres;
import com.example.demo.entities.Songs;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GenreRepository extends JpaRepository<Genres, Integer> {
	Optional<Genres> findByName(String name);
}
