package com.example.demo.services;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.ArtistsDTO;

public interface ArtistService {
	public ArtistsDTO addArtist(String name, String bio, MultipartFile file);
	
	public ArtistsDTO updateArtist(Integer id, String name, String bio, MultipartFile file);
	
	public boolean deleteArtist(Integer id);
	
	public Iterable<ArtistsDTO> findAllArtist();
	
	public List<ArtistsDTO> findAllArtist(Pageable pageable);
}
