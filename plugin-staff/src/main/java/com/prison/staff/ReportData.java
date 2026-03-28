package com.prison.staff;

import java.time.LocalDateTime;

public record ReportData(
    long id,
    String reporterUuid,
    String reportedUuid,
    String reporterName,
    String reportedName,
    String reason,
    ReportStatus status,
    String resolutionNote,   // null until closed
    String assignedToUuid,   // null until assigned
    LocalDateTime createdAt
) {
    public enum ReportStatus { PENDING, REVIEWED, CLOSED }
}
