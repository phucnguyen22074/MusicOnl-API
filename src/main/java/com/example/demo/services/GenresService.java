package com.example.demo.services;

import java.util.List;

import com.example.demo.dto.GenresDTO;

public interface GenresService {
	public List<GenresDTO> findAll();
	public GenresDTO addGenre(String name);
	public GenresDTO updateGenre(Integer id, String name);
	public boolean deleteGenre(Integer id);
}
