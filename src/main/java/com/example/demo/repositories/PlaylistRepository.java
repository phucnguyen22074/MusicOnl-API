package com.example.demo.repositories;

import com.example.demo.entities.Playlists;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlaylistRepository extends JpaRepository<Playlists, Integer> {
    List<Playlists> findByUsers_UserId(Integer userId);
}
