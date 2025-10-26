package com.example.demo.services;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.UsersDTO;
import com.example.demo.entities.Users;
import com.example.demo.repositories.UserRepository;

@Service
public class UserServiceDTOImpl implements UserServiceDTO {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceDTOImpl(UserRepository userRepository,
                              ModelMapper modelMapper,
                              PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email không được để trống.");
        }
        Users u = findByEmail(email);
        if (u == null) {
            throw new UsernameNotFoundException("Không tìm thấy tài khoản với email: " + email);
        }

        String authorityName = "ROLE_" + (u.getRole() == null ? "USER" : u.getRole().toUpperCase(Locale.ROOT));
        return new User(u.getEmail(), u.getPassword(), List.of(new SimpleGrantedAuthority(authorityName)));
    }

    @Override
    public Users findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email không được để trống.");
        }
        return userRepository.findbyEmail(email);
    }

    @Override
    public UsersDTO findByEmailDTO(String email) {
        Users user = userRepository.findbyEmail(email);
        return (user != null) ? modelMapper.map(user, UsersDTO.class) : null;
    }

    @Override
    public UsersDTO find(int id) {
        return userRepository.findById(id)
                .map(user -> modelMapper.map(user, UsersDTO.class))
                .orElse(null);
    }

    @Override
    public Users save(UsersDTO usersDTO) {
        try {
            Users users = modelMapper.map(usersDTO, Users.class);
            users.setPassword(passwordEncoder.encode(usersDTO.getPassword()));
            users.setRole("USER");
            users.setCreatedAt(new Date());
            users.setAvatarUrl("no-image.gif");
            return userRepository.save(users);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    @Override
	public boolean update(UsersDTO usersDTO) {
		try {
			Users users = userRepository.findbyEmail(usersDTO.getEmail());
			if (users == null) return false;
			if (usersDTO.getUsername() != null && !usersDTO.getUsername().isEmpty()) {
				users.setUsername(usersDTO.getUsername());
			}
			if (usersDTO.getFullName() != null && !usersDTO.getFullName().isEmpty()) {
				users.setFullName(usersDTO.getFullName());
			}
			if (usersDTO.getPassword() != null && !usersDTO.getPassword().isEmpty()) {
				users.setPassword(passwordEncoder.encode(usersDTO.getPassword()));
			}
			modelMapper.getConfiguration().setSkipNullEnabled(true);
			userRepository.save(users);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
    
    @Override
    public String updateAvatar(String email, MultipartFile file) {
        try {
            Users user = userRepository.findbyEmail(email);
            if (user == null)  return null;
            File uploadsFolder = new File(new ClassPathResource(".").getFile().getPath() + "/static/assets/images");
			if (!uploadsFolder.exists()) uploadsFolder.mkdirs();
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path path = Paths.get(uploadsFolder.getAbsolutePath(), fileName);
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            user.setAvatarUrl(fileName);
            userRepository.save(user);
            return "http://localhost:8088/assets/images/" + fileName;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

	@Override
	public boolean changePassword(String email, String oldPassword, String newPassword) {
		try {
			Users user = userRepository.findbyEmail(email);
			if (user == null) {
				return false;
			}
			
			if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
				return false;
			}
			
			user.setPassword(passwordEncoder.encode(newPassword));
			userRepository.save(user);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

    
}

