package dev.lunov.p2p_server.controller;

import dev.lunov.p2p_server.dto.AuthRequest;
import dev.lunov.p2p_server.dto.AuthResponse;
import dev.lunov.p2p_server.dto.ResetPasswordRequest;
import dev.lunov.p2p_server.model.Role;
import dev.lunov.p2p_server.model.User;
import dev.lunov.p2p_server.repository.UserRepository;
import dev.lunov.p2p_server.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/check-username")
    public ResponseEntity<?> checkUsername(@RequestParam String username) {
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Username required");
        }
        boolean isAvailable = userRepository.findByUsername(username.trim()).isEmpty();
        return ResponseEntity.ok(java.util.Collections.singletonMap("available", isAvailable));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody AuthRequest request) {
        if (request.getUsername() == null || request.getPassword() == null) {
            return ResponseEntity.badRequest().body(new AuthResponse("Username and password required"));
        }
        
        String username = request.getUsername().trim();
        String password = request.getPassword().trim();

        if (!username.matches("^[a-zA-Z0-9]{3,20}$")) {
            return ResponseEntity.badRequest().body(new AuthResponse("Username must contain only 3-20 letters and digits"));
        }

        if (password.length() < 6) {
            return ResponseEntity.badRequest().body(new AuthResponse("Password must be at least 6 characters long"));
        }

        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body(new AuthResponse("Username already exists"));
        }

        User newUser = new User(
                username,
                passwordEncoder.encode(password),
                Role.USER, // By default, new registrations are regular users
                request.getBiometricToken()
        );
        userRepository.save(newUser);

        String token = jwtUtil.generateToken(newUser.getUsername(), newUser.getRole().name());
        return ResponseEntity.ok(new AuthResponse(token, newUser.getUsername(), newUser.getRole().name()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        String username = request.getUsername() != null ? request.getUsername().trim() : "";
        String password = request.getPassword() != null ? request.getPassword().trim() : "";
        
        Optional<User> optUser = userRepository.findByUsername(username);

        if (optUser.isEmpty() || !passwordEncoder.matches(password, optUser.get().getPassword())) {
            return ResponseEntity.status(401).body(new AuthResponse("Invalid username or password"));
        }

        User user = optUser.get();
        if (user.isBanned()) {
            return ResponseEntity.status(403).body(new AuthResponse("Account is banned until " + user.getBannedUntil()));
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        
        return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), user.getRole().name()));
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<AuthResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        if (request.getUsername() == null || request.getNewPassword() == null || request.getBiometricToken() == null) {
            return ResponseEntity.badRequest().body(new AuthResponse("Username, new password, and biometric token are required"));
        }

        String username = request.getUsername().trim();
        String newPassword = request.getNewPassword().trim();
        String token = request.getBiometricToken().trim();

        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(new AuthResponse("Password must be at least 6 characters long"));
        }

        Optional<User> optUser = userRepository.findByUsername(username);
        if (optUser.isEmpty()) {
            return ResponseEntity.badRequest().body(new AuthResponse("User not found"));
        }

        User user = optUser.get();
        if (user.getBiometricToken() == null || !user.getBiometricToken().equals(token)) {
            return ResponseEntity.status(401).body(new AuthResponse("Invalid biometric token"));
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        String jwt = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        return ResponseEntity.ok(new AuthResponse(jwt, user.getUsername(), user.getRole().name()));
    }
}
