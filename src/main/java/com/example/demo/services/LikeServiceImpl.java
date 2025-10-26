package com.example.demo.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.dto.SongsDTO;
import com.example.demo.entities.Likes;
import com.example.demo.entities.LikesId;
import com.example.demo.entities.Songs;
import com.example.demo.entities.Users;
import com.example.demo.repositories.LikesRepository;
import com.example.demo.repositories.SongRepository;
import com.example.demo.repositories.UserRepository;

@Service
public class LikeServiceImpl implements LikeService{
	
	@Autowired
    private LikesRepository likesRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private ModelMapper modelMapper;

	@Override
	public boolean likeSong(Integer userId, Integer songId) {
		if(likesRepository.existsByUsers_UserIdAndSongs_SongId(userId, songId)) return false;
		Users user = userRepository.findById(userId).orElse(null);
		Songs song = songRepository.findById(songId).orElse(null);
		if(user == null || song == null) return false;
		
		Likes like = new Likes();
		like.setId(new LikesId(userId, songId));
        like.setUsers(user);
        like.setSongs(song);
        like.setLikedAt(new Date());
        likesRepository.save(like);
        return true;
		
	}

	@Override
	public boolean unlikeSong(Integer userId, Integer songId) {
		if (!likesRepository.existsByUsers_UserIdAndSongs_SongId(userId, songId)) return false;
        likesRepository.deleteByUsers_UserIdAndSongs_SongId(userId, songId);
        return true;
	}

	@Override
	public boolean isLiked(Integer userId, Integer songId) {
		return likesRepository.existsByUsers_UserIdAndSongs_SongId(userId, songId);
	}

	@Override
	public int countLikesBySong(Integer songId) {
		return likesRepository.findBySongs_SongId(songId).size();
	}

	@Override
	public List<SongsDTO> getLikedSongsByUser(Integer userId) {
		List<Likes> likes = likesRepository.findByUsers_UserId(userId);
        List<SongsDTO> songs = new ArrayList<>();

        for (Likes like : likes) {
            if (like.getSongs() != null) {
                songs.add(modelMapper.map(like.getSongs(), SongsDTO.class));
            }
        }

        return songs;
	}

}
