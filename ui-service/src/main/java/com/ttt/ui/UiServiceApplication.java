package com.ttt.ui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.ttt")
public class UiServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UiServiceApplication.class, args);
    }
}
