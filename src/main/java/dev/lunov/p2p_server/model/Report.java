package dev.lunov.p2p_server.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String reporterUsername;

    @Column(nullable = false)
    private String reportedContext; // E.g., username, or message ID

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReportStatus status;

    public enum ReportStatus {
        PENDING, RESOLVED
    }

    public Report() {}

    public Report(String reporterUsername, String reportedContext, String reason) {
        this.reporterUsername = reporterUsername;
        this.reportedContext = reportedContext;
        this.reason = reason;
        this.timestamp = LocalDateTime.now();
        this.status = ReportStatus.PENDING;
    }

    public Long getId() { return id; }
    public String getReporterUsername() { return reporterUsername; }
    public void setReporterUsername(String reporterUsername) { this.reporterUsername = reporterUsername; }
    public String getReportedContext() { return reportedContext; }
    public void setReportedContext(String reportedContext) { this.reportedContext = reportedContext; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }
}
