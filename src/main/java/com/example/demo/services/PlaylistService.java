package com.example.demo.services;

import java.util.List;

import com.example.demo.dto.PlaylistDTO;

public interface PlaylistService {
	public List<PlaylistDTO> findAll();

	public List<PlaylistDTO> findByUser(Integer userId);

	public PlaylistDTO addPlaylist(Integer userId, String name, String description);

	public PlaylistDTO updatePlaylist(Integer id, String name, String description);

	public boolean deletePlaylist(Integer id);

	public boolean addSongToPlaylist(Integer playlistId, Integer songId);

	public boolean removeSongFromPlaylist(Integer playlistId, Integer songId);
}
