package com.example.demo.repositories;

import com.example.demo.entities.Songs;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SongRepository extends JpaRepository<Songs, Integer> {
	boolean existsByTitleAndArtists_ArtistId(String title, Integer artistId);

	boolean existsByTitleAndArtists_Name(String title, String name);

	List<Songs> findByGenreses_GenreId(Integer genreId);

	List<Songs> findByartists_ArtistId(Integer artistId);

	List<Songs> findByTitleContainingIgnoreCase(String title);

	@Query("SELECT s FROM Songs s " + "LEFT JOIN FETCH s.albums a " + "LEFT JOIN FETCH s.users u "
			+ "LEFT JOIN FETCH s.artists ar " + "LEFT JOIN FETCH s.genreses g")
	List<Songs> findAllWithRelations();
	
	@Modifying
	@Query(value = "UPDATE songs SET listen_count = COALESCE(listen_count, 0) + 1 WHERE song_id = :songId", nativeQuery = true)
	void incrementListenCountNative(@Param("songId") Integer songId);
}
