package com.example.demo.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entities.Likes;
import com.example.demo.entities.LikesId;

@Repository
public interface LikesRepository extends JpaRepository<Likes, LikesId>{
	public List<Likes> findByUsers_UserId(Integer userId);
	public List<Likes> findBySongs_SongId(Integer songId);
	public boolean existsByUsers_UserIdAndSongs_SongId(Integer userId, Integer songId);
	public void deleteByUsers_UserIdAndSongs_SongId(Integer userId, Integer songId);
	public int countBySongs_SongId(Integer songId);
}
