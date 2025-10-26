package com.example.demo.services;

import java.util.Date;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.dto.PlaylistDTO;
import com.example.demo.entities.Playlists;
import com.example.demo.entities.Playlistsongs;
import com.example.demo.entities.PlaylistsongsId;
import com.example.demo.entities.Songs;
import com.example.demo.entities.Users;
import com.example.demo.repositories.PlaylistRepository;
import com.example.demo.repositories.SongRepository;
import com.example.demo.repositories.UserRepository;

@Service
public class PlaylistServiceImpl implements PlaylistService {

	@Autowired
	private PlaylistRepository playlistRepository;

	@Autowired
	private SongRepository songRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ModelMapper modelMapper;

	@Override
	public List<PlaylistDTO> findAll() {
		List<Playlists> list = playlistRepository.findAll();
		return modelMapper.map(list, new TypeToken<List<PlaylistDTO>>(){}.getType());
	}

	@Override
	public List<PlaylistDTO> findByUser(Integer userId) {
		List<Playlists> list = playlistRepository.findByUsers_UserId(userId);
		return modelMapper.map(list, new TypeToken<List<PlaylistDTO>>(){}.getType());
	}

	@Override
	public PlaylistDTO addPlaylist(Integer userId, String name, String description) {
		Users user = userRepository.findById(userId).orElse(null);
		if (userId == null) return null;
		
		Playlists playlist = new Playlists();
        playlist.setName(name);
        playlist.setDescription(description);
        playlist.setUsers(user);
        playlist.setCreatedAt(new Date());
        playlistRepository.save(playlist);
        return modelMapper.map(playlist, PlaylistDTO.class);
	}

	@Override
	public PlaylistDTO updatePlaylist(Integer id, String name, String description) {
		Playlists playlist = playlistRepository.findById(id).orElse(null);
		if(playlist == null) return null;
		
		if(name != null && !name.isBlank()) playlist.setName(name);
		if(description != null && !description.isBlank()) playlist.setDescription(description);
		
		playlistRepository.save(playlist);
		return modelMapper.map(playlist, PlaylistDTO.class);
	}

	@Override
	public boolean deletePlaylist(Integer id) {
		Playlists playlist = playlistRepository.findById(id).orElse(null);
		if(playlist == null) return false;
		playlistRepository.delete(playlist);
		return true;
	}

	@Override
	public boolean addSongToPlaylist(Integer playlistId, Integer songId) {
		Playlists playlist = playlistRepository.findById(playlistId).orElse(null);
		Songs song = songRepository.findById(songId).orElse(null);
		if(playlist == null || song == null) return false;
		
		boolean exists = playlist.getPlaylistsongses().stream().anyMatch(ps -> ps.getSongs().getSongId().equals(songId));
		
		if (exists) return false;
		Playlistsongs link = new Playlistsongs();
	    link.setId(new PlaylistsongsId(playlistId, songId));
	    link.setPlaylists(playlist);
	    link.setSongs(song);
	    link.setAddedAt(new Date());

	    playlist.getPlaylistsongses().add(link);
	    playlistRepository.save(playlist);
	    return true;
	}

	@Override
	public boolean removeSongFromPlaylist(Integer playlistId, Integer songId) {
		Playlists playlist = playlistRepository.findById(playlistId).orElse(null);
	    if (playlist == null) return false;

	    // Tìm bản ghi trung gian
	    Playlistsongs toRemove = playlist.getPlaylistsongses().stream()
	            .filter(ps -> ps.getSongs().getSongId().equals(songId))
	            .findFirst()
	            .orElse(null);

	    if (toRemove == null) return false;

	    playlist.getPlaylistsongses().remove(toRemove);
	    playlistRepository.save(playlist);
	    return true;
	}

}
