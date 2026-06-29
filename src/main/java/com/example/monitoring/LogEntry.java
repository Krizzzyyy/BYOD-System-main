package com.example.monitoring;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class LogEntry {
    private final StringProperty studentName;
    private final StringProperty studentId;
    private final StringProperty deviceSerial;
    private final StringProperty status;
    private final StringProperty lastLog;
    private final StringProperty approvalStatus;
    private final StringProperty userType;
    private final StringProperty scheduledEntryDate;
    private final int logId;

    public LogEntry(int logId, String studentName, String studentId, String deviceSerial,
                    String status, String lastLog, String approvalStatus, String scheduledEntryDate, String userType) {
        this.logId = logId;
        this.studentName = new SimpleStringProperty(studentName);
        this.studentId = new SimpleStringProperty(studentId);
        this.deviceSerial = new SimpleStringProperty(deviceSerial);
        this.status = new SimpleStringProperty(status);
        this.lastLog = new SimpleStringProperty(lastLog);
        this.approvalStatus = new SimpleStringProperty(approvalStatus != null ? approvalStatus : "Approved");
        this.userType = new SimpleStringProperty(userType != null ? userType : "N/A");
        this.scheduledEntryDate = new SimpleStringProperty(scheduledEntryDate != null ? scheduledEntryDate : "-");
    }

    public int getLogId() { return logId; }
    public String getStudentName() { return studentName.get(); }
    public StringProperty studentNameProperty() { return studentName; }
    public String getStudentId() { return studentId.get(); }
    public StringProperty studentIdProperty() { return studentId; }
    public String getDeviceSerial() { return deviceSerial.get(); }
    public StringProperty deviceSerialProperty() { return deviceSerial; }
    public String getStatus() { return status.get(); }
    public StringProperty statusProperty() { return status; }
    public String getLastLog() { return lastLog.get(); }
    public StringProperty lastLogProperty() { return lastLog; }
    public String getApprovalStatus() { return approvalStatus.get(); }
    public StringProperty approvalStatusProperty() { return approvalStatus; }
    public String getScheduledEntryDate() { return scheduledEntryDate.get(); }
    public String getUserType() { return userType.get(); }
    public StringProperty userTypeProperty() { return userType; }
    public StringProperty scheduledEntryDateProperty() { return scheduledEntryDate; }
}
