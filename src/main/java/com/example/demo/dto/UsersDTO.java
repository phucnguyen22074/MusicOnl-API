package com.example.demo.dto;

import java.util.Date;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import lombok.Data;

@Data
public class UsersDTO {

    private Integer userId;
    private String username;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String role;
    private Date createdAt;
    
    @JsonProperty(access = Access.WRITE_ONLY)
    private String password;

    // Nếu muốn trả về thông tin liên quan có thể dùng thêm
    private Set<Integer> artistIds;   // id các artist mà user gắn với
    private Set<Integer> playlistIds; // id các playlist mà user tạo
    private Set<Integer> likedSongIds; // id các bài hát user like

//    public UsersDTO() {
//    }
//
//    public UsersDTO(Integer userId, String username, String email, String fullName,
//                    String avatarUrl, String role, Date createdAt) {
//        this.userId = userId;
//        this.username = username;
//        this.email = email;
//        this.fullName = fullName;
//        this.avatarUrl = avatarUrl;
//        this.role = role;
//        this.createdAt = createdAt;
//    }
//
//    // Getter & Setter
//    public Integer getUserId() {
//        return userId;
//    }
//
//    public void setUserId(Integer userId) {
//        this.userId = userId;
//    }
//
//    public String getUsername() {
//        return username;
//    }
//
//    public void setUsername(String username) {
//        this.username = username;
//    }
//
//    public String getEmail() {
//        return email;
//    }
//
//    public void setEmail(String email) {
//        this.email = email;
//    }
//
//    public String getFullName() {
//        return fullName;
//    }
//
//    public void setFullName(String fullName) {
//        this.fullName = fullName;
//    }
//
//    public String getAvatarUrl() {
//        return avatarUrl;
//    }
//
//    public void setAvatarUrl(String avatarUrl) {
//        this.avatarUrl = avatarUrl;
//    }
//
//    public String getRole() {
//        return role;
//    }
//
//    public void setRole(String role) {
//        this.role = role;
//    }
//
//    public Date getCreatedAt() {
//        return createdAt;
//    }
//
//    public void setCreatedAt(Date createdAt) {
//        this.createdAt = createdAt;
//    }
//
//    public Set<Integer> getArtistIds() {
//        return artistIds;
//    }
//
//    public void setArtistIds(Set<Integer> artistIds) {
//        this.artistIds = artistIds;
//    }
//
//    public Set<Integer> getPlaylistIds() {
//        return playlistIds;
//    }
//
//    public void setPlaylistIds(Set<Integer> playlistIds) {
//        this.playlistIds = playlistIds;
//    }
//
//    public Set<Integer> getLikedSongIds() {
//        return likedSongIds;
//    }
//
//    public void setLikedSongIds(Set<Integer> likedSongIds) {
//        this.likedSongIds = likedSongIds;
//    }
//
//	public String getPassword() {
//		return password;
//	}
//
//	public void setPassword(String password) {
//		this.password = password;
//	}
    
}
