package com.example.demo.services;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.AlbumsDTO;

public interface AlbumsService {
	List<AlbumsDTO> findAll();
    AlbumsDTO addAlbum(String title, Integer artistId, MultipartFile coverFile);
    AlbumsDTO updateAlbum(Integer id, String title, Integer artistId, MultipartFile coverFile);
    boolean deleteAlbum(Integer id);
    List<AlbumsDTO> findByArtist(Integer artistId);
    List<AlbumsDTO> searchByTitle(String keyword);
}
