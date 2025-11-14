package com.example.demo.services;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.AlbumsDTO;

public interface AlbumsService {
	public List<AlbumsDTO> findAll();
	public List<AlbumsDTO> findAll(Pageable pageable);
	public AlbumsDTO addAlbum(String title, Integer artistId, MultipartFile coverFile);
	public AlbumsDTO updateAlbum(Integer id, String title, Integer artistId, MultipartFile coverFile);
	public boolean deleteAlbum(Integer id);
	public List<AlbumsDTO> findByArtist(Integer artistId);
	public List<AlbumsDTO> findByArtist(Integer artistId, Pageable pageable);
	public List<AlbumsDTO> searchByTitle(String keyword);
}
