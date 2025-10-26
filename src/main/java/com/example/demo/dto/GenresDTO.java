package com.example.demo.dto;

import java.util.Date;

import lombok.Data;

@Data
public class GenresDTO {
    private Integer genreId;
    private String name;
    private String description;
    private Date createdAt;

    // Getter & Setter
//    public Integer getGenreId() {
//        return genreId;
//    }
//    public void setGenreId(Integer genreId) {
//        this.genreId = genreId;
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
//    public Date getCreatedAt() {
//        return createdAt;
//    }
//    public void setCreatedAt(Date createdAt) {
//        this.createdAt = createdAt;
//    }
    
    
}
