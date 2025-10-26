package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class MusicOnlineApplication {

	public static void main(String[] args) {
		SpringApplication.run(MusicOnlineApplication.class, args);
	}
	
//	@Bean
//    public RestTemplate restTemplate() {
//        return new RestTemplate();
//    }

}
