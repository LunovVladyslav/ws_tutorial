package dev.lunov.p2p_server.controller;

import dev.lunov.p2p_server.model.Peer;
import dev.lunov.p2p_server.model.Report;
import dev.lunov.p2p_server.model.Role;
import dev.lunov.p2p_server.model.User;
import dev.lunov.p2p_server.repository.ReportRepository;
import dev.lunov.p2p_server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import dev.lunov.p2p_server.model.PublicChannel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private SignalController signalController;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AppConfigController appConfigController;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/peers")
    public ResponseEntity<Map<String, Peer>> getActivePeers() {
        return ResponseEntity.ok(signalController.getPeers());
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody User reqUser) {
        if (userRepository.findByUsername(reqUser.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }
        User user = new User(
                reqUser.getUsername(),
                passwordEncoder.encode(reqUser.getPassword()),
                reqUser.getRole() != null ? reqUser.getRole() : Role.USER
        );
        userRepository.save(user);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{id}/ban")
    public ResponseEntity<?> banUser(@PathVariable Long id, @RequestParam int hours) {
        return userRepository.findById(id).map(user -> {
            if (hours == -1) {
                // Permanent ban (roughly 100 years from now)
                user.setBannedUntil(java.time.LocalDateTime.now().plusYears(100));
            } else {
                user.setBannedUntil(java.time.LocalDateTime.now().plusHours(hours));
            }
            userRepository.save(user);
            return ResponseEntity.ok().build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/users/{id}/unban")
    public ResponseEntity<?> unbanUser(@PathVariable Long id) {
        return userRepository.findById(id).map(user -> {
            user.setBannedUntil(null);
            userRepository.save(user);
            return ResponseEntity.ok().build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/reports")
    public ResponseEntity<List<Report>> getAllReports() {
        return ResponseEntity.ok(reportRepository.findAllByOrderByTimestampDesc());
    }

    @PutMapping("/reports/{id}/resolve")
    public ResponseEntity<?> resolveReport(@PathVariable Long id) {
        return reportRepository.findById(id).map(report -> {
            report.setStatus(Report.ReportStatus.RESOLVED);
            reportRepository.save(report);
            return ResponseEntity.ok().build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/logs")
    public ResponseEntity<String> getLogs() {
        Path logFile = Paths.get("logs/app.log");
        if (!Files.exists(logFile)) {
            return ResponseEntity.ok("No logs found.");
        }
        try (Stream<String> lines = Files.lines(logFile)) {
            // Get last 1000 lines for performance and memory reasons
            List<String> last1000 = lines.collect(Collectors.toList());
            if (last1000.size() > 1000) {
                last1000 = last1000.subList(last1000.size() - 1000, last1000.size());
            }
            return ResponseEntity.ok(String.join("\n", last1000));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Error reading logs: " + e.getMessage());
        }
    }

    // --- Channels Management ---
    @GetMapping("/channels")
    public ResponseEntity<Map<String, PublicChannel>> getAllChannels() {
        return ResponseEntity.ok(signalController.getChannels());
    }

    @PostMapping("/channels")
    public ResponseEntity<?> createChannel(@RequestBody PublicChannel channel) {
        if (channel.id() == null || channel.id().isEmpty()) {
            channel = new PublicChannel(
                java.util.UUID.randomUUID().toString(),
                channel.name(),
                channel.tags() != null ? channel.tags() : "",
                "system_admin"
            );
        }
        signalController.addPublicChannel(channel);
        return ResponseEntity.ok(channel);
    }

    @DeleteMapping("/channels/{id}")
    public ResponseEntity<?> deleteChannel(@PathVariable String id) {
        signalController.removePublicChannel(id);
        return ResponseEntity.ok().build();
    }

    // --- System Notifications ---
    @PostMapping("/notifications")
    public ResponseEntity<?> sendSystemNotification(@RequestBody Map<String, String> payload) {
        String message = payload.get("message");
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Message cannot be empty");
        }
        signalController.broadcastSystemNotification(message);
        return ResponseEntity.ok().build();
    }

    // --- App Configuration Update ---
    @PostMapping("/config/version")
    public ResponseEntity<?> updateVersion(
            @RequestParam(required = false) String version,
            @RequestParam(required = false) String releaseNotes,
            @RequestParam(required = false) String externalUrl,
            @RequestParam(required = false) MultipartFile apkFile) {
        try {
            appConfigController.updateConfig(version, releaseNotes, externalUrl, apkFile);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Failed to update config: " + e.getMessage());
        }
    }
}
