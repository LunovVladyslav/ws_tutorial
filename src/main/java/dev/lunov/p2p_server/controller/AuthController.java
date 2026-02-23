package dev.lunov.p2p_server.controller;

import dev.lunov.p2p_server.dto.AuthRequest;
import dev.lunov.p2p_server.dto.AuthResponse;
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

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody AuthRequest request) {
        if (request.getUsername() == null || request.getPassword() == null) {
            return ResponseEntity.badRequest().body(new AuthResponse("Username and password required"));
        }

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body(new AuthResponse("Username already exists"));
        }

        User newUser = new User(
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                Role.USER // By default, new registrations are regular users
        );
        userRepository.save(newUser);

        String token = jwtUtil.generateToken(newUser.getUsername(), newUser.getRole().name());
        return ResponseEntity.ok(new AuthResponse(token, newUser.getUsername(), newUser.getRole().name()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        Optional<User> optUser = userRepository.findByUsername(request.getUsername());

        if (optUser.isEmpty() || !passwordEncoder.matches(request.getPassword(), optUser.get().getPassword())) {
            return ResponseEntity.status(401).body(new AuthResponse("Invalid username or password"));
        }

        User user = optUser.get();
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        
        return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), user.getRole().name()));
    }
}
