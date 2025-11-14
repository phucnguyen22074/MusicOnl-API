// src/main/java/com/example/demo/dto/ArtistRequestDTO.java
package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;


@Data
public class ArtistRequestDTO {

    @NotBlank(message = "Tên nghệ sĩ không được để trống")
    @Size(max = 100, message = "Tên nghệ sĩ không được quá 100 ký tự")
    private String name;

    @Size(max = 1000, message = "Tiểu sử không được quá 1000 ký tự")
    private String bio;
}