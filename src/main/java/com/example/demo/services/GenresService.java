package com.example.demo.services;

import java.util.List;

import com.example.demo.dto.GenresDTO;
import com.example.demo.dto.SongsDTO;

public interface GenresService {
	public List<GenresDTO> findAll();
	public GenresDTO addGenre(String name, String color, String icon);
	public GenresDTO updateGenre(Integer id, String name, String color, String icon);
	public boolean deleteGenre(Integer id);
}
