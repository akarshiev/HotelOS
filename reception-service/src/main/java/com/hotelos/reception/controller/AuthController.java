package com.hotelos.reception.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        if ("admin".equals(username) && "admin123".equals(password)) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "username", username,
                "role", "ADMIN",
                "message", "Login successful"
            ));
        }

        return ResponseEntity.status(401).body(Map.of(
            "success", false,
            "message", "Invalid credentials"
        ));
    }

    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify() {
        return ResponseEntity.ok(Map.of(
            "authenticated", true,
            "username", "admin",
            "role", "ADMIN"
        ));
    }
}
