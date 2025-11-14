package com.example.demo.services;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.dto.GenresDTO;
import com.example.demo.dto.SongsDTO;
import com.example.demo.entities.Genres;
import com.example.demo.entities.Songs;
import com.example.demo.repositories.GenreRepository;

@Service
public class GenresServiceImpl implements GenresService {
	@Autowired
	private GenreRepository genreRepository;

	@Autowired
	private ModelMapper modelMapper;

	@Override
	public List<GenresDTO> findAll() {
		List<Genres> list = genreRepository.findAll();
		return modelMapper.map(list, new TypeToken<List<GenresDTO>>() {
		}.getType());
	}

	@Override
	public GenresDTO addGenre(String name, String color, String icon) {
		Genres genre = new Genres();
		genre.setName(name);
		genre.setColor(color);
		genre.setIcon(icon);
		genreRepository.save(genre);
		return modelMapper.map(genre, GenresDTO.class);
	}

	@Override
	public GenresDTO updateGenre(Integer id, String name, String color, String icon) {
		Genres genre = genreRepository.findById(id).orElse(null);
		if (genre == null) return null;
		
		if (name != null && !name.isBlank())
			genre.setName(name);
		if (color != null)
			genre.setColor(color);
		if (icon != null)
			genre.setIcon(icon);
			
		genreRepository.save(genre);
		return modelMapper.map(genre, GenresDTO.class);
	}

	@Override
	public boolean deleteGenre(Integer id) {
		Genres genre = genreRepository.findById(id).orElse(null);
		if (genre == null) return false;
		genreRepository.delete(genre);
		return true;
	}

	// Overload methods để hỗ trợ backward compatibility
	public GenresDTO addGenre(String name) {
		return addGenre(name, null, null);
	}

	public GenresDTO updateGenre(Integer id, String name) {
		return updateGenre(id, name, null, null);
	}

}