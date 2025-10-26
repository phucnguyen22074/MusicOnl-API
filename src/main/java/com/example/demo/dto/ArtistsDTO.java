package com.example.demo.dto;

import java.util.Date;

import lombok.Data;

@Data
public class ArtistsDTO {
    private Integer artistId;
    private String name;
    private String bio;
    private String imageUrl;
    private String Image;
    private Date createdAt;

//    public Integer getArtistId() {
//        return artistId;
//    }
//    public void setArtistId(Integer artistId) {
//        this.artistId = artistId;
//    }
//
//    public String getName() {
//        return name;
//    }
//    public void setName(String name) {
//        this.name = name;
//    }
//
//    public String getBio() {
//        return bio;
//    }
//    public void setBio(String bio) {
//        this.bio = bio;
//    }
//
//    public String getImageUrl() {
//        return imageUrl;
//    }
//    public void setImageUrl(String imageUrl) {
//        this.imageUrl = imageUrl;
//    }
//
//    public Date getCreatedAt() {
//        return createdAt;
//    }
//    public void setCreatedAt(Date createdAt) {
//        this.createdAt = createdAt;
//    }
//	public String getImage() {
//		return Image;
//	}
//	public void setImage(String image) {
//		Image = image;
//	}
    
}
