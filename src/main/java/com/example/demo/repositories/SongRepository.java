package com.example.demo.repositories;

import com.example.demo.entities.Songs;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SongRepository extends JpaRepository<Songs, Integer> {
	boolean existsByTitleAndArtists_ArtistId(String title, Integer artistId);

	boolean existsByTitleAndArtists_Name(String title, String name);

	List<Songs> findByGenreses_GenreId(Integer genreId);

	List<Songs> findByartists_ArtistId(Integer artistId);

	List<Songs> findByTitleContainingIgnoreCase(String title);
}
