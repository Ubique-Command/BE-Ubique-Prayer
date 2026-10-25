package com.ubique.prayer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

// Authentication will be supplied by the auth domain; do not create a default user.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class PrayerApplication {

	public static void main(String[] args) {
		SpringApplication.run(PrayerApplication.class, args);
	}

}
