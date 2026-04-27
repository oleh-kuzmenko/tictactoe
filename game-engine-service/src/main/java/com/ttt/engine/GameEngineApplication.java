package com.ttt.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.ttt")
public class GameEngineApplication {
    public static void main(String[] args) {
        SpringApplication.run(GameEngineApplication.class, args);
    }
}
