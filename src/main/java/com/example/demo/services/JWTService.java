package com.example.demo.services;

public interface JWTService {

	public String generateToken(String username);

	public String getUsernameFromJWT(String token);

	public boolean validToken(String token);
}
