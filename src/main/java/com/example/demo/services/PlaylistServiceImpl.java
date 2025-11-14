package com.example.demo.services;

import com.example.demo.dto.PlaylistDTO;
import com.example.demo.entities.Playlists;
import com.example.demo.entities.Playlistsongs;
import com.example.demo.entities.PlaylistsongsId;
import com.example.demo.entities.Songs;
import com.example.demo.entities.Users;
import com.example.demo.repositories.PlaylistRepository;
import com.example.demo.repositories.SongRepository;
import com.example.demo.repositories.UserRepository;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class PlaylistServiceImpl implements PlaylistService {

    @Autowired private PlaylistRepository playlistRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private SongRepository songRepo;
    @Autowired private ModelMapper mapper;

    // ================= FIND ALL =================
    @Override
    public List<PlaylistDTO> findAll() {
        return playlistRepo.findAll().stream()
                .map(p -> mapper.map(p, PlaylistDTO  .class))
                .toList();
    }

    // ================= FIND BY USER =================
    @Override
    public List<PlaylistDTO> findByUser(Integer userId) {
        validateId(userId, "User ID");
        return playlistRepo.findByUsers_UserId(userId).stream()
                .map(p -> mapper.map(p, PlaylistDTO.class))
                .toList();
    }

    // ================= ADD PLAYLIST =================
    @Transactional
    @Override
    public PlaylistDTO addPlaylist(Integer userId, String name, String description) {
        validateId(userId, "User ID");
        validateName(name);

        Users user = getUserOrThrow(userId);
        Playlists playlist = new Playlists();
        playlist.setName(name);
        playlist.setDescription(description);
        playlist.setUsers(user);
        playlist.setCreatedAt(new Date());

        playlistRepo.save(playlist);
        return mapper.map(playlist, PlaylistDTO.class);
    }

    // ================= UPDATE PLAYLIST =================
    @Transactional
    @Override
    public PlaylistDTO updatePlaylist(Integer id, String name, String description) {
        validateId(id, "Playlist ID");
        Playlists playlist = getPlaylistOrThrow(id);

        if (name != null && !name.isBlank()) playlist.setName(name);
        if (description != null && !description.isBlank()) playlist.setDescription(description);

        playlistRepo.save(playlist);
        return mapper.map(playlist, PlaylistDTO.class);
    }

    // ================= DELETE PLAYLIST =================
    @Transactional
    @Override
    public boolean deletePlaylist(Integer id) {
        validateId(id, "Playlist ID");
        return playlistRepo.findById(id)
                .map(playlist -> {
                    playlistRepo.delete(playlist);
                    return true;
                })
                .orElse(false);
    }

    // ================= ADD SONG TO PLAYLIST =================
    @Transactional
    @Override
    public boolean addSongToPlaylist(Integer playlistId, Integer songId) {
        validateId(playlistId, "Playlist ID");
        validateId(songId, "Song ID");

        Playlists playlist = getPlaylistOrThrow(playlistId);
        Songs song = getSongOrThrow(songId);

        boolean exists = playlist.getPlaylistsongses().stream()
                .anyMatch(ps -> ps.getSongs().getSongId().equals(songId));

        if (exists) return false;

        Playlistsongs link = new Playlistsongs();
        link.setId(new PlaylistsongsId(playlistId, songId));
        link.setPlaylists(playlist);
        link.setSongs(song);
        link.setAddedAt(new Date());

        playlist.getPlaylistsongses().add(link);
        playlistRepo.save(playlist);
        return true;
    }

    // ================= REMOVE SONG FROM PLAYLIST =================
    @Transactional
    @Override
    public boolean removeSongFromPlaylist(Integer playlistId, Integer songId) {
        validateId(playlistId, "Playlist ID");
        validateId(songId, "Song ID");

        Playlists playlist = getPlaylistOrThrow(playlistId);

        return playlist.getPlaylistsongses().stream()
                .filter(ps -> ps.getSongs().getSongId().equals(songId))
                .findFirst()
                .map(toRemove -> {
                    playlist.getPlaylistsongses().remove(toRemove);
                    playlistRepo.save(playlist);
                    return true;
                })
                .orElse(false);
    }

    // ================= HELPER METHODS =================

    private void validateId(Integer id, String fieldName) {
        if (id == null) {
            throw new IllegalArgumentException(fieldName + " không được để trống");
        }
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên playlist không được để trống");
        }
    }

    private Users getUserOrThrow(Integer userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy User với ID: " + userId));
    }

    private Playlists getPlaylistOrThrow(Integer playlistId) {
        return playlistRepo.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Playlist với ID: " + playlistId));
    }

    private Songs getSongOrThrow(Integer songId) {
        return songRepo.findById(songId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Song với ID: " + songId));
    }
}