package com.example.demo.services;

import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.ArtistsDTO;

public interface ArtistService {
	public String addArtist(String name, String bio, MultipartFile file);
	
	public String updateArtist(Integer id, String name, String bio, MultipartFile file);
	
	public boolean deleteArtist(Integer id);
	
	public Iterable<ArtistsDTO> findAllArtist();
}
