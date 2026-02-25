package dev.lunov.p2p_server.dto;

public class ReportRequest {
    private String reportedContext;
    private String reason;

    public ReportRequest() {}

    public ReportRequest(String reportedContext, String reason) {
        this.reportedContext = reportedContext;
        this.reason = reason;
    }

    public String getReportedContext() { return reportedContext; }
    public void setReportedContext(String reportedContext) { this.reportedContext = reportedContext; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
