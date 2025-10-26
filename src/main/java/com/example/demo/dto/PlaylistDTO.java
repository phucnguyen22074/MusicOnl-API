package com.example.demo.dto;

import java.util.Date;
import java.util.Set;

import lombok.Data;

@Data
public class PlaylistDTO {
    private Integer playlistId;
    private String name;
    private String description;
    private String coverImage;
    private Date createdAt;

    private Integer userId;
    private String username;
    private Set<SongsDTO> songs;

    // GETTERS & SETTERS
//    public Integer getPlaylistId() {
//        return playlistId;
//    }
//    public void setPlaylistId(Integer playlistId) {
//        this.playlistId = playlistId;
//    }
//    public String getName() {
//        return name;
//    }
//    public void setName(String name) {
//        this.name = name;
//    }
//    public String getDescription() {
//        return description;
//    }
//    public void setDescription(String description) {
//        this.description = description;
//    }
//    public String getCoverImage() {
//        return coverImage;
//    }
//    public void setCoverImage(String coverImage) {
//        this.coverImage = coverImage;
//    }
//    public Date getCreatedAt() {
//        return createdAt;
//    }
//    public void setCreatedAt(Date createdAt) {
//        this.createdAt = createdAt;
//    }
//    public Integer getUserId() {
//        return userId;
//    }
//    public void setUserId(Integer userId) {
//        this.userId = userId;
//    }
//    public String getUsername() {
//        return username;
//    }
//    public void setUsername(String username) {
//        this.username = username;
//    }
//    public Set<SongsDTO> getSongs() {
//        return songs;
//    }
//    public void setSongs(Set<SongsDTO> songs) {
//        this.songs = songs;
//    }
}
