package com.eiu.pdds;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;

@SpringBootApplication
public class PddsApplication {

    private static final Logger log = LoggerFactory.getLogger(PddsApplication.class);

    public static void main(String[] args) {
        loadDotenvIfPresent();
        SpringApplication.run(PddsApplication.class, args);
    }

    private static void loadDotenvIfPresent() {
        File currentDir = new File(".");
        File parentDir = currentDir.getAbsoluteFile().getParentFile();

        File targetEnv = null;
        if (new File(".env").exists()) {
            targetEnv = new File(".env");
        } else if (parentDir != null && new File(parentDir, ".env").exists()) {
            targetEnv = new File(parentDir, ".env");
        }

        if (targetEnv != null) {
            try {
                Dotenv dotenv = Dotenv.configure()
                        .directory(targetEnv.getParent() == null ? "." : targetEnv.getParent())
                        .filename(targetEnv.getName())
                        .ignoreIfMissing()
                        .load();

                dotenv.entries().forEach(entry -> {
                    if (System.getProperty(entry.getKey()) == null && System.getenv(entry.getKey()) == null) {
                        System.setProperty(entry.getKey(), entry.getValue());
                    }
                });
                log.info("Loaded environment variables from: {}", targetEnv.getAbsolutePath());
            } catch (Exception e) {
                log.warn("Could not load .env file: {}", e.getMessage());
            }
        }
    }
}
