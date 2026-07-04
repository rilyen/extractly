package com._6.extractly.controllers;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com._6.extractly.dto.LoginRequest;
import com._6.extractly.dto.RegisterRequest;
import com._6.extractly.models.Role;
import com._6.extractly.models.User;
import com._6.extractly.repositories.UserRepository;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class AuthController {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // temporary hardcoded admin domain for authentication
    private static final String ADMIN_DOMAIN = "@aetherautomation.com";

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Handles POST /register
    @PostMapping("/register")
    @ResponseBody
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Email already registered."));
        }

        // determine role based on email domain
        Role role = request.getEmail().toLowerCase().endsWith(ADMIN_DOMAIN) ? Role.ADMIN : Role.USER;
        
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        
        User user = new User(request.getEmail(), hashedPassword, role);
        userRepository.save(user);
        
        return ResponseEntity.ok(Map.of("message", "User registered successfully."));
    }

    // Handles POST /login
    @PostMapping("/login")
    @ResponseBody
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpSession session) {

        var userOpt = userRepository.findByEmail(request.getEmail());

        if (userOpt.isEmpty() || !passwordEncoder.matches(request.getPassword(), userOpt.get().getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid email or password."));
        }

        User user = userOpt.get();

        session.setAttribute("email", user.getEmail());
        session.setAttribute("role", user.getRole().name());

        return ResponseEntity.ok(Map.of("message", "Login successful."));
    }

    // Handles POST /logout
    @PostMapping("/logout")
    @ResponseBody
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("message", "Logged out."));
    }
}
