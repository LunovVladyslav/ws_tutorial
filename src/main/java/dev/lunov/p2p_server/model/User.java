package dev.lunov.p2p_server.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "banned_until")
    private java.time.LocalDateTime bannedUntil;

    @Column(name = "biometric_token")
    private String biometricToken;

    public User() {}

    public User(String username, String password, Role role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public User(String username, String password, Role role, String biometricToken) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.biometricToken = biometricToken;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    
    public java.time.LocalDateTime getBannedUntil() { return bannedUntil; }
    public void setBannedUntil(java.time.LocalDateTime bannedUntil) { this.bannedUntil = bannedUntil; }
    
    public String getBiometricToken() { return biometricToken; }
    public void setBiometricToken(String biometricToken) { this.biometricToken = biometricToken; }
    
    public boolean isBanned() {
        return bannedUntil != null && bannedUntil.isAfter(java.time.LocalDateTime.now());
    }
}
