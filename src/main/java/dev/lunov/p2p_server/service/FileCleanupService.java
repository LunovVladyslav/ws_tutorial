package dev.lunov.p2p_server.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

@Service
public class FileCleanupService {

    private final Path uploadDir = Paths.get("uploads", "channels");

    // Run every 5 minutes to clean up ephemeral channel files
    @Scheduled(fixedRate = 300000)
    public void cleanupOldFiles() {
        if (!Files.exists(uploadDir)) {
            return;
        }

        Instant cutoffTime = Instant.now().minus(5, ChronoUnit.MINUTES);

        try (Stream<Path> files = Files.walk(uploadDir)) {
            files.filter(Files::isRegularFile).forEach(file -> {
                try {
                    BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
                    Instant creationTime = attrs.creationTime().toInstant();

                    if (creationTime.isBefore(cutoffTime)) {
                        Files.delete(file);
                        System.out.println("Cleaned up expired file: " + file.getFileName());
                    }
                } catch (IOException e) {
                    System.err.println("Failed to attributes or delete file: " + file.getFileName());
                }
            });
        } catch (IOException e) {
            System.err.println("Failed to walk upload directory for cleanup: " + e.getMessage());
        }
    }
}
