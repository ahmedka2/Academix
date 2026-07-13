package com.academix;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AcademixApplication {

	public static void main(String[] args) {
		loadDotenv();
		SpringApplication.run(AcademixApplication.class, args);
	}

	private static void loadDotenv() {
		Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
		copyIfPresent(dotenv, "SPRING_DATASOURCE_URL");
		copyIfPresent(dotenv, "SPRING_DATASOURCE_USERNAME");
		copyIfPresent(dotenv, "SPRING_DATASOURCE_PASSWORD");
		copyIfPresent(dotenv, "SERVER_PORT");
		copyIfPresent(dotenv, "JWT_SECRET");
		copyIfPresent(dotenv, "JWT_EXPIRATION_MS");
		copyIfPresent(dotenv, "ADMIN_FULL_NAME");
		copyIfPresent(dotenv, "ADMIN_EMAIL");
		copyIfPresent(dotenv, "ADMIN_PASSWORD");
	}

	private static void copyIfPresent(Dotenv dotenv, String key) {
		if (System.getProperty(key) == null && System.getenv(key) == null && dotenv.entries().stream().anyMatch(entry -> entry.getKey().equals(key))) {
			System.setProperty(key, dotenv.get(key));
		}
	}
}
