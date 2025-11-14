package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AlbumRequestDTO {
	@NotBlank(message = "Title cannot be empty")
    private String title;

    @NotNull(message = "Artist ID cannot be null")
    private Integer artistId;
}
