package dev.lunov.p2p_server.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class AppConfigController {

    private String currentVersion = "1.0.0";
    private String releaseNotes = "Initial release.";
    private String downloadUrl = "";
    
    private final Path uploadDir = Paths.get("uploads");

    public AppConfigController() {
        try {
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/version")
    public ResponseEntity<Map<String, String>> getAppVersion() {
        Map<String, String> response = new HashMap<>();
        response.put("version", currentVersion);
        response.put("releaseNotes", releaseNotes);
        response.put("downloadUrl", downloadUrl);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/download-apk")
    public ResponseEntity<Resource> downloadApk() {
        try {
            Path file = uploadDir.resolve("app.apk");
            if (!Files.exists(file)) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new UrlResource(file.toUri());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"app.apk\"")
                    .contentType(MediaType.parseMediaType("application/vnd.android.package-archive"))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    public void updateConfig(String version, String notes, String externalUrl, MultipartFile apkFile) throws IOException {
        this.currentVersion = version != null ? version : this.currentVersion;
        this.releaseNotes = notes != null ? notes : this.releaseNotes;
        
        if (apkFile != null && !apkFile.isEmpty()) {
            Path targetFile = uploadDir.resolve("app.apk");
            Files.copy(apkFile.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
            this.downloadUrl = "/api/config/download-apk";
        } else if (externalUrl != null && !externalUrl.trim().isEmpty()) {
            this.downloadUrl = externalUrl;
        }
    }
}
