package com.example.demo.dto;

import java.util.Date;
import java.util.Set;

import lombok.Data;

@Data
public class AlbumsDTO {
	private Integer albumId;
    private String title;
    private Date releaseDate;
    private String coverUrl;

    private Integer artistId;
    private String artistName;

    private Set<SongsDTO> songs; 
}
