package dev.lunov.p2p_server.config;

import dev.lunov.p2p_server.model.Role;
import dev.lunov.p2p_server.model.User;
import dev.lunov.p2p_server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Value("${admin.username}")
    private String username;

    @Value("${admin.password}")
    private String password;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Find all existing admins
        java.util.List<User> allAdmins = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN)
                .toList();

        // If the configured username already exists, just update their password and ensure they are admin
        java.util.Optional<User> adminOpt = userRepository.findByUsername(username);
        
        if (adminOpt.isPresent()) {
            User existingAdmin = adminOpt.get();
            existingAdmin.setPassword(passwordEncoder.encode(password));
            existingAdmin.setRole(Role.ADMIN);
            userRepository.save(existingAdmin);
            
            // Demote or delete other admins (like the default 'admin') if they don't match the new username
            for (User oldAdmin : allAdmins) {
                if (!oldAdmin.getUsername().equals(username)) {
                    oldAdmin.setRole(Role.USER); // Demote old default admin to regular user
                    userRepository.save(oldAdmin);
                }
            }
            System.out.println("✅ Admin user synced with environment variables");
        } else {
            // New admin configured, let's demote old ones first
            for (User oldAdmin : allAdmins) {
                oldAdmin.setRole(Role.USER);
                userRepository.save(oldAdmin);
            }
            
            // Create the new admin
            User admin = new User(
                    username,
                    passwordEncoder.encode(password),
                    Role.ADMIN
            );
            userRepository.save(admin);
            System.out.println("✅ Default admin user created from environment variables");
        }
    }
}
