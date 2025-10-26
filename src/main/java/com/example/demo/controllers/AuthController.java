package com.example.demo.controllers;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.UsersDTO;
import com.example.demo.services.JWTService;
import com.example.demo.services.MailService;
import com.example.demo.services.UserServiceDTO;
import com.example.demo.dto.ChangePasswordRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.util.MimeTypeUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JWTService jwtService;

    @Autowired
    private UserServiceDTO accountService;
    
    @Autowired
    private MailService mailService;

    static class JwtResponse {
        private String token;
        private UsersDTO user;
        private String message;
        private boolean success;

        public JwtResponse(String token, UsersDTO user) {
            this.token = token;
            this.user = user;
            this.success = true;
            this.message = "Login successful";
        }

        // Getters
        public String getToken() { return token; }
        public UsersDTO getUser() { return user; }
        public String getMessage() { return message; }
        public boolean isSuccess() { return success; }
    }
    
    /* GET */
    @GetMapping(value = "find/{id}", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
    public ResponseEntity<UsersDTO> find(@PathVariable("id") int id) {
        try {
            return new ResponseEntity<>(accountService.find(id), HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error finding user by id: {}", id, e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    /* POST - JWT Login */
    @PostMapping("/login")
    public ResponseEntity<?> loginJWT(@Validated @RequestBody LoginRequest request) {
        try {
            logger.debug("Login attempt for email: {}", request.getEmail());
            
            // 1) Xác thực credentials
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            // 2) Tạo JWT token
            String token = jwtService.generateToken(request.getEmail());

            // 3) Lấy thông tin user
            UsersDTO userDto = accountService.findByEmailDTO(request.getEmail());
            
            if (userDto == null) {
                logger.warn("User not found after successful authentication: {}", request.getEmail());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("success", false, "message", "User data not found"));
            }

            logger.info("Successful login for user: {}", request.getEmail());
            return ResponseEntity.ok(new JwtResponse(token, userDto));

        } catch (BadCredentialsException ex) {
            logger.warn("Invalid credentials for email: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Invalid email or password"));
                    
        } catch (Exception ex) {
            logger.error("Login error for email: {}", request.getEmail(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }

    // Thêm endpoint để verify token (tuỳ chọn)
    @PostMapping("/verify")
    public ResponseEntity<?> verifyToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Invalid authorization header"));
            }

            String token = authHeader.substring(7);
            boolean isValid = jwtService.validToken(token);
            
            if (isValid) {
                String username = jwtService.getUsernameFromJWT(token);
                return ResponseEntity.ok(Map.of("success", true, "message", "Token is valid", "username", username));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Invalid token"));
            }
        } catch (Exception ex) {
            logger.error("Token verification error", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Token verification failed"));
        }
    }
    
    @PostMapping(value = "/register", consumes = MimeTypeUtils.APPLICATION_JSON_VALUE, produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> register(@Validated @RequestBody UsersDTO usersDTO) {
        try {
            // Lưu user mới
            var savedUser = accountService.save(usersDTO);

            if (savedUser != null) {
            	mailService.sendHtmlMail(savedUser.getEmail(), "Chào mừng đến với Music Online", savedUser.getUsername());
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(Map.of(
                                "success", true,
                                "message", "User registered successfully",
                                "userId", savedUser.getUserId(),
                                "email", savedUser.getEmail()
                        ));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "Failed to register user"));
            }
        } catch (Exception ex) {
            logger.error("Error registering user with email: {}", usersDTO.getEmail(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }
    
    @PostMapping(value = "/change-password", consumes = MimeTypeUtils.APPLICATION_JSON_VALUE, produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request, 
                                            @RequestHeader("Authorization") String authHeader) {
        try {
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "New password and confirm password do not match"));
            }

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Missing or invalid token"));
            }

            // Lấy email từ JWT token
            String token = authHeader.substring(7);
            String email = jwtService.getUsernameFromJWT(token);

            boolean updated = accountService.changePassword(email, request.getOldPassword(), request.getNewPassword());

            if (updated) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Password updated successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "Invalid old password or update failed"));
            }
        } catch (Exception e) {
            logger.error("Error changing password", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }
    
 /*PUT*/
    
    @PutMapping(value = "/update", consumes = MimeTypeUtils.APPLICATION_JSON_VALUE, produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateUser(@Validated @RequestBody UsersDTO usersDTO){
    	try {
			boolean update = accountService.update(usersDTO);
			if (update) {
				return ResponseEntity.ok(Map.of(
						"success", true,
                        "message", "User updated successfully",
                        "email", usersDTO.getEmail()
						));
			} else {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "User not found"));
			}
		} catch (Exception e) {
			logger.error("Error updating user with email: {}", usersDTO.getEmail(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "Internal server error"));
		}
    }
    
    @PutMapping(value = "/avatar", consumes = {"multipart/form-data"})
    public ResponseEntity<?> updateAvatar(
            @RequestParam("email") String email,
            @RequestParam("file") MultipartFile file) {
        try {
            String avatarUrl = accountService.updateAvatar(email, file);
            if (avatarUrl != null) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Avatar updated successfully",
                    "avatarUrl", avatarUrl
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "User not found or update failed"));
            }
        } catch (Exception e) {
            logger.error("Error updating avatar for user: {}", email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }

}