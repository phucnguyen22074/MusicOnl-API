package com.example.demo.repositories;

import com.example.demo.entities.Artists;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArtistRepository extends JpaRepository<Artists, Integer> {
    Optional<Artists> findByName(String name);
}
