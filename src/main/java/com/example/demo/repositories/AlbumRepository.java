package com.example.demo.repositories;

import com.example.demo.entities.Albums;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlbumRepository extends JpaRepository<Albums, Integer> {
    Optional<Albums> findByTitle(String title);
    
    List<Albums> findByArtists_ArtistId(Integer artistId);
    List<Albums> findByTitleContainingIgnoreCase(String keyword);
}
