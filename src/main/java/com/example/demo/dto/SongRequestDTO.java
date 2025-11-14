package com.example.demo.dto;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class SongRequestDTO {
	@NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    @Positive(message = "Thời lượng phải lớn hơn 0")
    private Integer duration;

    private String lyrics;
    private String status;
    private Integer albumId;
    private List<Integer> artistIds;
    private List<Integer> genreIds;

    // File nhạc: bắt buộc → không dùng @Valid ở đây (MultipartFile)
    private MultipartFile file;

    // Ảnh: không bắt buộc
    private MultipartFile image;
}
