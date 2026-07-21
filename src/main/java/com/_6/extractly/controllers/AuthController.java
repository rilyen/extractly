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

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // temporary hardcoded admin domain for authentication
    private static final String ADMIN_DOMAIN = "@aetherautomation.com";

    private static final String EMAIL_REGEX = "^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$";

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    @ResponseBody
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return error("email", "Email is required.");
        }
        if (!request.getEmail().matches(EMAIL_REGEX)) {
            return error("email", "Email must be valid.");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            return error("password", "Password is required.");
        }
        if (request.getPassword().length() < 8) {
            return error("password", "Password must be at least 8 characters.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            return error("email", "Email already registered.");
        }

        // determine role based on email domain
        Role role = request.getEmail().toLowerCase().endsWith(ADMIN_DOMAIN) ? Role.ADMIN : Role.USER;
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        User user = new User(request.getEmail(), hashedPassword, role);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "User registered successfully."));
    }

    @PostMapping("/login")
    @ResponseBody
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpSession session) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return error("email", "Email is required.");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            return error("password", "Password is required.");
        }

        var userOpt = userRepository.findByEmail(request.getEmail());

        if (userOpt.isEmpty() || !passwordEncoder.matches(request.getPassword(), userOpt.get().getPassword())) {
            return error("credentials", "Invalid email or password.");
        }

        User user = userOpt.get();
        session.setAttribute("email", user.getEmail());
        session.setAttribute("role", user.getRole().name());

        return ResponseEntity.ok(Map.of("message", "Login successful."));
    }

    @PostMapping("/logout")
    @ResponseBody
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("message", "Logged out."));
    }

    private ResponseEntity<Map<String, String>> error(String field, String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(field, message));
    }
}