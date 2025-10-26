package com.example.demo.services;

import java.util.List;

import com.example.demo.dto.SongsDTO;

public interface LikeService {
	public boolean likeSong(Integer userId, Integer songId);
	public boolean unlikeSong(Integer userId, Integer songId);
	public boolean isLiked(Integer userId, Integer songId);
	public int countLikesBySong(Integer songId);
	public List<SongsDTO> getLikedSongsByUser(Integer userId);
}
