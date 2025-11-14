package com.example.demo.services;

import com.example.demo.dto.SongsDTO;
import com.example.demo.entities.Likes;
import com.example.demo.entities.LikesId;
import com.example.demo.entities.Songs;
import com.example.demo.entities.Users;
import com.example.demo.repositories.LikesRepository;
import com.example.demo.repositories.SongRepository;
import com.example.demo.repositories.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class LikeServiceImpl implements LikeService {

    @Autowired private LikesRepository likesRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private SongRepository songRepo;
    @Autowired private ModelMapper mapper;

    // ================= LIKE SONG =================
    @Transactional
    @Override
    public boolean likeSong(Integer userId, Integer songId) {
        validateIds(userId, songId);

        if (likesRepo.existsByUsers_UserIdAndSongs_SongId(userId, songId)) {
            return false; // Đã like rồi
        }

        Users user = getUserOrThrow(userId);
        Songs song = getSongOrThrow(songId);

        Likes like = new Likes();
        like.setId(new LikesId(userId, songId));
        like.setUsers(user);
        like.setSongs(song);
        like.setLikedAt(new Date());

        likesRepo.save(like);
        return true;
    }

    // ================= UNLIKE SONG =================
    @Transactional
    @Override
    public boolean unlikeSong(Integer userId, Integer songId) {
        validateIds(userId, songId);

        if (!likesRepo.existsByUsers_UserIdAndSongs_SongId(userId, songId)) {
            return false; // Chưa like
        }

        likesRepo.deleteByUsers_UserIdAndSongs_SongId(userId, songId);
        return true;
    }

    // ================= CHECK LIKED =================
    @Override
    public boolean isLiked(Integer userId, Integer songId) {
        validateIds(userId, songId);
        return likesRepo.existsByUsers_UserIdAndSongs_SongId(userId, songId);
    }

    // ================= COUNT LIKES =================
    @Override
    public int countLikesBySong(Integer songId) {
        if (songId == null) throw new IllegalArgumentException("Song ID không được để trống");
        return likesRepo.countBySongs_SongId(songId);
    }

    // ================= GET LIKED SONGS =================
    @Override
    public List<SongsDTO> getLikedSongsByUser(Integer userId) {
        if (userId == null) throw new IllegalArgumentException("User ID không được để trống");

        return likesRepo.findByUsers_UserId(userId).stream()
                .map(like -> like.getSongs())
                .filter(song -> song != null)
                .map(song -> mapper.map(song, SongsDTO.class))
                .toList();
    }

    // ================= HELPER METHODS =================

    private void validateIds(Integer userId, Integer songId) {
        if (userId == null || songId == null) {
            throw new IllegalArgumentException("User ID và Song ID không được để trống");
        }
    }

    private Users getUserOrThrow(Integer userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy User với ID: " + userId));
    }

    private Songs getSongOrThrow(Integer songId) {
        return songRepo.findById(songId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Song với ID: " + songId));
    }
}