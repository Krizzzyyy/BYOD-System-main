package com.example.monitoring;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class LogEntry {
    private final StringProperty studentName;
    private final StringProperty studentId;
    private final StringProperty deviceSerial;
    private final StringProperty status;
    private final StringProperty lastLog;

    public LogEntry(String studentName, String studentId, String deviceSerial, String status, String lastLog) {
        this.studentName = new SimpleStringProperty(studentName);
        this.studentId = new SimpleStringProperty(studentId);
        this.deviceSerial = new SimpleStringProperty(deviceSerial);
        this.status = new SimpleStringProperty(status);
        this.lastLog = new SimpleStringProperty(lastLog);
    }

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
}
