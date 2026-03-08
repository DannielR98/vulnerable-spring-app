package com.vulnerable.app;

import com.vulnerable.app.model.User;
import com.vulnerable.app.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;

/**
 * VulnBank — Deliberately Vulnerable Spring Boot Application
 *
 * ⚠️  FOR EDUCATIONAL USE ONLY
 * ⚠️  DO NOT DEPLOY TO PRODUCTION
 * ⚠️  Contains intentional SQL Injection, XSS, and Broken Auth vulnerabilities
 */
@SpringBootApplication
public class VulnerableApplication {

    public static void main(String[] args) {
        System.out.println("""
            ██╗   ██╗██╗   ██╗██╗     ███╗   ██╗██████╗  █████╗ ███╗   ██╗██╗  ██╗
            ██║   ██║██║   ██║██║     ████╗  ██║██╔══██╗██╔══██╗████╗  ██║██║ ██╔╝
            ██║   ██║██║   ██║██║     ██╔██╗ ██║██████╔╝███████║██╔██╗ ██║█████╔╝
            ╚██╗ ██╔╝██║   ██║██║     ██║╚██╗██║██╔══██╗██╔══██║██║╚██╗██║██╔═██╗
             ╚████╔╝ ╚██████╔╝███████╗██║ ╚████║██████╔╝██║  ██║██║ ╚████║██║  ██╗
              ╚═══╝   ╚═════╝ ╚══════╝╚═╝  ╚═══╝╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═══╝╚═╝  ╚═╝
                         ⚠️  VULNERABLE APP — EDUCATIONAL USE ONLY  ⚠️
            """);
        SpringApplication.run(VulnerableApplication.class, args);
    }

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository) {
        return args -> {
            if (userRepository.count() == 0) {
                userRepository.save(new User(null, "admin", "admin123", "admin@bank.local", "ADMIN", BigDecimal.valueOf(99999.00)));
                userRepository.save(new User(null, "alice", "password1", "alice@bank.local", "USER", BigDecimal.valueOf(1500.00)));
                userRepository.save(new User(null, "bob", "qwerty", "bob@bank.local", "USER", BigDecimal.valueOf(300.00)));
                userRepository.save(new User(null, "charlie", "letmein", "charlie@bank.local", "USER", BigDecimal.valueOf(750.00)));
            }
        };
    }
}
