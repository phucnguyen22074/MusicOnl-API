package com.example.demo.repositories;

import com.example.demo.entities.Albums;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlbumRepository extends JpaRepository<Albums, Integer> {
    Optional<Albums> findByTitle(String title);
    List<Albums> findByArtists_ArtistId(Integer artistId);
    List<Albums> findByTitleContainingIgnoreCase(String keyword);
    boolean existsByTitle(String title);
    Page<Albums> findByArtists_ArtistId(Integer artistId, Pageable pageable);
    
    @Query("SELECT a FROM Albums a LEFT JOIN FETCH a.artists LEFT JOIN FETCH a.songses")
    Page<Albums> findAllWithRelations(Pageable pageable);
    
    @Query("SELECT a FROM Albums a " +
            "LEFT JOIN FETCH a.artists " +
            "LEFT JOIN FETCH a.songses " +
            "WHERE a.albumId = :id")
     Optional<Albums> findByIdWithRelations(@Param("id") Integer id);
}
