package dev.lunov.p2p_server.dto;

public class ResetPasswordRequest {
    private String username;
    private String newPassword;
    private String biometricToken;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    
    public String getBiometricToken() { return biometricToken; }
    public void setBiometricToken(String biometricToken) { this.biometricToken = biometricToken; }
}
