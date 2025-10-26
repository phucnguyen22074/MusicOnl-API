package com.example.demo.services;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.UsersDTO;
import com.example.demo.entities.Users;

public interface UserServiceDTO extends UserDetailsService{

	public Users findByEmail(String email);
	
	public UsersDTO findByEmailDTO(String email);
	
	public UsersDTO find(int id);
	
	public Users save(UsersDTO usersDTO);
	
	public boolean update(UsersDTO usersDTO);
	
	public String updateAvatar(String email, MultipartFile file);
	
	public boolean changePassword(String email, String oldPassword, String newPassword);

}
