package com.auca.portal.controller;

import com.auca.portal.service.AucaFinanceClient;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class StudentPortalAuthController {

    @Autowired
    private AucaFinanceClient aucaFinanceClient;

    @Value("${jwt.secret}")
    private String jwtSecret;

@PostMapping("/signin")
    public ResponseEntity<?> signin(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        Map<String, Object> aucaResponse = aucaFinanceClient.login(username, password);
 
        if (aucaResponse == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Invalid credentials");
            return ResponseEntity.status(401).body(error);
        }

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());

        String token = Jwts.builder()
            .setSubject(username)
            .claim("username", aucaResponse.get("username"))
            .claim("email", aucaResponse.get("email"))
            .claim("role", aucaResponse.get("role"))
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("token", "Bearer " + token);
        response.put("studentId", username);
        response.put("username", aucaResponse.get("username"));
        response.put("email", aucaResponse.get("email"));
        response.put("fullName", aucaResponse.get("fullName"));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getStudent(@PathVariable String studentId) {
        Map<String, Object> studentData = aucaFinanceClient.getStudentData(studentId);

        if (studentData == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Student not found");
            return ResponseEntity.status(404).body(error);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("student", studentData);

        return ResponseEntity.ok(response);
    }
}
