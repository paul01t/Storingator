package com.neto.Storingator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration.class)
public class StoringatorApplication {

	public static void main(String[] args) {
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("java.net.preferIPv4Addresses", "true");
		SpringApplication.run(StoringatorApplication.class, args);
	}

}