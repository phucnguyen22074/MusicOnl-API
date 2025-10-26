package com.example.demo.services;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public interface MailService {
	public boolean send(String from, String to, String subject, String content);
	public boolean sendHtmlMail(String to, String subject, String username);
}
