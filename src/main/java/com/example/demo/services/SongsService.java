package com.example.demo.services;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.SongsDTO;

public interface SongsService {

	public List<SongsDTO> findAll();

	public SongsDTO addSong(String title, Integer duration, String lyrics, String status, Integer albumId,
			List<Integer> artistIds, List<Integer> genreIds, MultipartFile file, MultipartFile image);

	public SongsDTO updateSong(Integer id, String title, Integer duration, String lyrics, String status,
			Integer albumId, List<Integer> artistIds, List<Integer> genreIds, MultipartFile file, MultipartFile image);

	public boolean deleteSong(Integer id);

	public List<SongsDTO> findByGenre(Integer genreId);

	public List<SongsDTO> findByArtist(Integer artistId);

	public List<SongsDTO> findByTitleContaining(String keyword);
}
