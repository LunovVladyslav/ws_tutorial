package dev.lunov.p2p_server.controller;

import dev.lunov.p2p_server.dto.ReportRequest;
import dev.lunov.p2p_server.model.Report;
import dev.lunov.p2p_server.repository.ReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportRepository reportRepository;

    @PostMapping
    public ResponseEntity<String> submitReport(@RequestBody ReportRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String reporterUsername = auth.getName();

        Report report = new Report(reporterUsername, request.getReportedContext(), request.getReason());
        reportRepository.save(report);

        return ResponseEntity.ok("Report submitted successfully");
    }
}
