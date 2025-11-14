package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LikeRequestDTO {
	@NotNull(message = "User ID không được để trống")
    private Integer userId;

    @NotNull(message = "Song ID không được để trống")
    private Integer songId;
}
