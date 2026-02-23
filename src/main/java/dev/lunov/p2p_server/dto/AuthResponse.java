package dev.lunov.p2p_server.dto;

public class AuthResponse {
    private String token;
    private String username;
    private String role;
    private String error;

    public AuthResponse(String token, String username, String role) {
        this.token = token;
        this.username = username;
        this.role = role;
    }

    public AuthResponse(String error) {
        this.error = error;
    }

    public String getToken() { return token; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public String getError() { return error; }
}
