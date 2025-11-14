package com.example.demo.dto;

import lombok.Data;

import java.util.Date;
import java.util.Set;

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
    private Integer listenCount;
    private Date createdAt;

    // Album info (nếu có)
    private Integer albumId;
    private String albumTitle;

    // User info (người upload)
    private Integer userId;
    private String username;

    // Danh sách ca sĩ (dùng ArtistsDTO để hiển thị chi tiết)
    private Set<ArtistsDTO> artists;

    // Danh sách tên thể loại (chỉ tên cho gọn)
    private Set<String> genres;
}