package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PlaylistRequestDTO {
	@NotNull(message = "User ID không được để trống")
    private Integer userId;

    @NotBlank(message = "Tên playlist không được để trống")
    private String name;

    private String description;
}
