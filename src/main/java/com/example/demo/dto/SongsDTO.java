package com.example.demo.dto;

import java.util.Date;
import java.util.Set;

import lombok.Data;

@Data
public class SongsDTO {
    private Integer songId;
    private String title;
    private Integer duration;
    private String filePath;
    private String lyrics;
    private Date releaseDate;
    private String imageUrl;
    private String status;
    private Date createdAt;

    // Quan hệ đơn giản: chỉ trả về ID hoặc tên
    private Integer albumId;
    private String albumTitle;

    private Integer userId;
    private String username;

    private Set<ArtistsDTO> artists;  // nhiều ca sĩ
    private Set<String> genres;       // chỉ tên genre cho gọn

    // getter/setter
//    public Integer getSongId() {
//        return songId;
//    }
//    public void setSongId(Integer songId) {
//        this.songId = songId;
//    }
//    public String getTitle() {
//        return title;
//    }
//    public void setTitle(String title) {
//        this.title = title;
//    }
//    public Integer getDuration() {
//        return duration;
//    }
//    public void setDuration(Integer duration) {
//        this.duration = duration;
//    }
//    public String getFilePath() {
//        return filePath;
//    }
//    public void setFilePath(String filePath) {
//        this.filePath = filePath;
//    }
//    public String getLyrics() {
//        return lyrics;
//    }
//    public void setLyrics(String lyrics) {
//        this.lyrics = lyrics;
//    }
//    public Date getReleaseDate() {
//        return releaseDate;
//    }
//    public void setReleaseDate(Date releaseDate) {
//        this.releaseDate = releaseDate;
//    }
//    public String getImageUrl() {
//        return imageUrl;
//    }
//    public void setImageUrl(String imageUrl) {
//        this.imageUrl = imageUrl;
//    }
//    public String getStatus() {
//        return status;
//    }
//    public void setStatus(String status) {
//        this.status = status;
//    }
//    public Date getCreatedAt() {
//        return createdAt;
//    }
//    public void setCreatedAt(Date createdAt) {
//        this.createdAt = createdAt;
//    }
//    public Integer getAlbumId() {
//        return albumId;
//    }
//    public void setAlbumId(Integer albumId) {
//        this.albumId = albumId;
//    }
//    public String getAlbumTitle() {
//        return albumTitle;
//    }
//    public void setAlbumTitle(String albumTitle) {
//        this.albumTitle = albumTitle;
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
//    public Set<ArtistsDTO> getArtists() {
//        return artists;
//    }
//    public void setArtists(Set<ArtistsDTO> artists) {
//        this.artists = artists;
//    }
//    public Set<String> getGenres() {
//        return genres;
//    }
//    public void setGenres(Set<String> genres) {
//        this.genres = genres;
//    }
}
