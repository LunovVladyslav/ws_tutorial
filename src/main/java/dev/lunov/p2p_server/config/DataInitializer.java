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
        if (userRepository.findByUsername(username).isEmpty()) {
            User admin = new User(
                    username,
                    passwordEncoder.encode(password),
                    Role.ADMIN
            );
            userRepository.save(admin);
            System.out.println("✅ Default admin user created");
        }
    }
}
