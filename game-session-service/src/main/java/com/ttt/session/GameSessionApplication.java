package com.ttt.session;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.ttt")
@EnableFeignClients
public class GameSessionApplication {
    public static void main(String[] args) {
        SpringApplication.run(GameSessionApplication.class, args);
    }
}
