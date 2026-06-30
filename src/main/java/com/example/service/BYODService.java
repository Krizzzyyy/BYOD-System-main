package com.example.service;

import java.sql.*;
import java.io.File;

import com.example.registration.RegistrationValidator;
import com.google.zxing.*;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;

public class BYODService {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/byod_db";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";


    // FIXED: Added ingress_time = CURRENT_TIMESTAMP so new registrations appear on the dashboard today
    public int registerStudent(String formId, String sid, String ln, String fn, String ys, String cp,
                               String cn, String dt, String bm, String cd, String sn,
                               String scheduledDate, String userType) throws Exception {
        String sql = "INSERT INTO student_device_logs (form_id, student_id, last_name, first_name, year_section, course_program, contact_number, device_type, brand_model, color_description, serial_number, scheduled_entry_date, approval_status, submitted_time, user_type) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,'Pending', CURRENT_TIMESTAMP, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, formId);
            ps.setString(2, sid);
            ps.setString(3, ln);
            ps.setString(4, fn);
            ps.setString(5, ys);
            ps.setString(6, cp);
            ps.setString(7, cn);
            ps.setString(8, dt);
            ps.setString(9, bm);
            ps.setString(10, cd);
            ps.setString(11, (sn != null && !sn.isBlank()) ? sn : null);
            ps.setString(12, (scheduledDate != null && !scheduledDate.isBlank()) ? scheduledDate : null);
            ps.setString(13, userType);
            ps.executeUpdate();
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) return generatedKeys.getInt(1);
            }
        }
        return -1;
    }

    public String generateFormId() throws Exception {
        java.time.LocalDate today = java.time.LocalDate.now();
        String datePart = String.format("%04d-%02d%02d",
                today.getYear(), today.getMonthValue(), today.getDayOfMonth());

        // Finds the highest sequence number already used today and adds 1; starts at 1 if none exist
        String sql = "SELECT MAX(CAST(SUBSTRING_INDEX(form_id, '-', -1) AS UNSIGNED)) as max_seq " +
                "FROM student_device_logs WHERE form_id LIKE ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, datePart + "-%");
            try (ResultSet rs = ps.executeQuery()) {
                int nextSeq = 1;
                if (rs.next()) {
                    int maxSeq = rs.getInt("max_seq");
                    if (!rs.wasNull()) nextSeq = maxSeq + 1;
                }
                return datePart + "-" + String.format("%04d", nextSeq);
            }
        }
    }
    /**
     * Fetches registered student profiles filtered by a specific date.
     * If targetDate is null, it returns all records.
     */
    public List<String[]> fetchStudentsByDate(String targetDate) {
        List<String[]> students = new ArrayList<>();

        // Dynamic query: adds the date filter only if a date is provided
        String sql = "SELECT form_id, student_id, " +
                "MAX(first_name) as first_name, " +
                "MAX(last_name) as last_name, " +
                "MAX(user_type) as user_type, " +
                "MAX(course_program) as course_program, " +
                "MAX(year_section) as year_section, " +
                "MAX(contact_number) as contact_number, " +
                "GROUP_CONCAT(DISTINCT device_type SEPARATOR ', ') as device_type, " +
                "GROUP_CONCAT(DISTINCT brand_model SEPARATOR ', ') as brand_model, " +
                "MAX(color_description) as color_description, " +
                "MAX(scheduled_entry_date) as scheduled_entry_date, " +
                "MAX(approval_status) as approval_status, " +
                "MAX(approval_remarks) as approval_remarks " +
                "FROM student_device_logs " +
                "WHERE student_id IS NOT NULL AND (is_deleted = 0 OR is_deleted IS NULL) " +
                (targetDate != null ? "AND DATE(ingress_time) = ? " : "") +
                "GROUP BY form_id, student_id " +
                "ORDER BY last_name ASC";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (targetDate != null) {
                ps.setString(1, targetDate);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String fullName = rs.getString("first_name") + " " + rs.getString("last_name");
                    students.add(new String[]{
                            rs.getString("form_id"),
                            fullName,
                            rs.getString("user_type") != null ? rs.getString("user_type") : "N/A",
                            rs.getString("student_id"),
                            rs.getString("course_program") != null ? rs.getString("course_program") : "N/A",
                            rs.getString("year_section") != null ? rs.getString("year_section") : "N/A",
                            rs.getString("contact_number") != null ? rs.getString("contact_number") : "N/A",
                            rs.getString("device_type") != null ? rs.getString("device_type") : "Unknown",
                            rs.getString("brand_model") != null ? rs.getString("brand_model") : "N/A",
                            rs.getString("color_description") != null ? rs.getString("color_description") : "N/A",
                            rs.getString("scheduled_entry_date") != null ? rs.getString("scheduled_entry_date") : "N/A",
                            rs.getString("approval_status") != null ? rs.getString("approval_status") : "N/A",
                            rs.getString("approval_remarks") != null ? rs.getString("approval_remarks") : ""
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching filtered students list: " + e.getMessage());
        }
        return students;
    }

    // Auto-promotes Approved entries to Ready for Entry once their scheduled date arrives
    public void checkAndUpdateReadyForEntry() throws Exception {
        String sql = "UPDATE student_device_logs SET approval_status = 'Ready for Entry' WHERE approval_status = 'Approved' AND scheduled_entry_date IS NOT NULL AND scheduled_entry_date <= CURRENT_DATE";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    // Returns the current approval_status for a given formId
    public String getStatusByFormId(String formId) throws Exception {
        String sql = "SELECT approval_status FROM student_device_logs WHERE form_id = ? LIMIT 1";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, formId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("approval_status");
            }
        }
        return null;
    }

    // Explicitly updates a student's active logs to set an Egress timestamp for ALL devices at once
    public void updateEgress(String formId) throws Exception {
        String sql = "UPDATE student_device_logs SET egress_time = CURRENT_TIMESTAMP, approval_status = 'Checked Out' WHERE form_id = ? AND egress_time IS NULL";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, formId);
            ps.executeUpdate();
        }
    }


    // Sets ingress_time and status to Checked In once the QR is scanned at entry
    public void updateIngressByFormId(String formId) throws Exception {
        String sql = "UPDATE student_device_logs SET ingress_time = CURRENT_TIMESTAMP, approval_status = 'Checked In' WHERE form_id = ? AND ingress_time IS NULL";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, formId);
            ps.executeUpdate();
        }
    }

    // Adds a clean new Ingress log item for an existing student context
    public void updateIngress(String formId) throws Exception {
        checkAndUpdateReadyForEntry();
        String sql = "UPDATE student_device_logs SET ingress_time = CURRENT_TIMESTAMP, approval_status = 'Checked In' WHERE form_id = ? AND ingress_time IS NULL";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, formId);
            ps.executeUpdate();
        }
    }

    // Merges multiple devices into a single comma-separated row for the monitoring table
    // Only returns Approved registrations (backward-compatible: NULL approval_status treated as Approved)
    public List<Object[]> fetchLogs() throws Exception {
        List<Object[]> logs = new ArrayList<>();
        String sql = "SELECT " +
                "form_id, " +
                "MIN(log_id) as log_id, " +
                "MAX(student_id) as student_id, " +
                "MAX(last_name) as last_name, " +
                "MAX(first_name) as first_name, " +
                "GROUP_CONCAT(brand_model SEPARATOR ', ') as brand_models, " +
                "MAX(ingress_time) as ingress_time, " +
                "CASE WHEN SUM(CASE WHEN egress_time IS NULL THEN 1 ELSE 0 END) > 0 THEN NULL ELSE MAX(egress_time) END as final_egress, " +
                "MAX(approval_status) as approval_status, " +
                "MAX(scheduled_entry_date) as scheduled_entry_date, " +
                "MAX(user_type) as user_type " +
                "FROM student_device_logs " +
                "WHERE approval_status IN ('Approved', 'Ready for Entry', 'Checked In', 'Checked Out') " +
                "GROUP BY form_id " +
                "ORDER BY MAX(ingress_time) DESC";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                logs.add(new Object[]{
                        rs.getString("form_id"),
                        rs.getInt("log_id"),
                        rs.getString("student_id"),
                        rs.getString("last_name") + ", " + rs.getString("first_name"),
                        rs.getString("brand_models"),
                        rs.getString("ingress_time"),
                        rs.getString("final_egress"),
                        rs.getString("approval_status"),
                        rs.getString("scheduled_entry_date"),
                        rs.getString("user_type")
                });
            }
        }
        return logs;
    }

    // Fetches all Pending registrations for the Pending Approvals tab
    public List<Object[]> fetchPendingApprovals() throws Exception {
        List<Object[]> pending = new ArrayList<>();
        String sql = "SELECT " +
                "form_id, " +
                "MIN(log_id) as log_id, " +
                "MAX(student_id) as student_id, " +
                "MAX(last_name) as last_name, " +
                "MAX(first_name) as first_name, " +
                "GROUP_CONCAT(brand_model SEPARATOR ', ') as brand_models, " +
                "MAX(scheduled_entry_date) as scheduled_entry_date, " +
                "MAX(course_program) as course_program, " +
                "MAX(contact_number) as contact_number, " +
                "MAX(user_type) as user_type " +
                "FROM student_device_logs " +
                "WHERE approval_status = 'Pending' " +
                "GROUP BY form_id " +
                "ORDER BY MAX(scheduled_entry_date) ASC";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                pending.add(new Object[]{
                        rs.getString("form_id"),
                        rs.getInt("log_id"),
                        rs.getString("student_id"),
                        rs.getString("last_name") + ", " + rs.getString("first_name"),
                        rs.getString("brand_models"),
                        rs.getString("scheduled_entry_date"),
                        rs.getString("course_program"),
                        rs.getString("contact_number"),
                        rs.getString("user_type")
                });
            }
        }
        return pending;
    }

    // Approves a pending registration and generates the QR code for it
    public String approveRegistration(String formId, String approvedBy) throws Exception {
        String sql = "UPDATE student_device_logs SET approval_status = 'Approved', approved_by = ?, approval_date = CURRENT_TIMESTAMP WHERE form_id = ? AND approval_status = 'Pending'";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, approvedBy);
            ps.setString(2, formId);
            ps.executeUpdate();
        }

        String payloadSql = "SELECT MAX(last_name) as last_name, MAX(first_name) as first_name, MAX(year_section) as year_section, MAX(course_program) as course_program, MAX(contact_number) as contact_number FROM student_device_logs WHERE form_id = ?";
        String payload = formId;
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(payloadSql)) {
            ps.setString(1, formId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    payload = String.join("|", formId, rs.getString("last_name"), rs.getString("first_name"),
                            rs.getString("year_section"), rs.getString("course_program"), rs.getString("contact_number"));
                }
            }
        }
        return generateQR(payload, formId, System.getProperty("user.dir"));
    }

    // Disapproves a pending registration with a reason
    public void disapproveRegistration(String formId, String reason, String approvedBy) throws Exception {
        String sql = "UPDATE student_device_logs SET approval_status = 'Disapproved', approval_remarks = ?, approved_by = ?, approval_date = CURRENT_TIMESTAMP WHERE form_id = ? AND approval_status = 'Pending'";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reason);
            ps.setString(2, approvedBy);
            ps.setString(3, formId);
            ps.executeUpdate();
        }
    }

    // Cancels a registration with a reason
    public void cancelRegistration(String formId, String reason, String cancelledBy) throws Exception {
        String sql = "UPDATE student_device_logs SET approval_status = 'Cancelled', approval_remarks = ?, approved_by = ?, approval_date = CURRENT_TIMESTAMP WHERE form_id = ? AND approval_status IN ('Pending', 'Approved')";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reason);
            ps.setString(2, cancelledBy);
            ps.setString(3, formId);
            ps.executeUpdate();
        }
    }

    // Gathers analytical numbers using standard dynamic SQL evaluations
    // Only counts Approved registrations (backward-compatible: NULL approval_status treated as Approved)
    public Map<String, Integer> fetchDashboardMetrics() throws Exception {
        Map<String, Integer> metrics = new HashMap<>();

        String sql = "SELECT " +
                "  (SELECT COUNT(DISTINCT student_id) FROM student_device_logs WHERE approval_status = 'Approved' OR approval_status IS NULL) as total_students, " +
                "  (SELECT COUNT(*) FROM student_device_logs WHERE approval_status = 'Approved' OR approval_status IS NULL) as total_devices, " +
                "  (SELECT COUNT(*) FROM student_device_logs WHERE egress_time IS NULL AND (approval_status = 'Approved' OR approval_status IS NULL)) as devices_inside, " +
                "  (SELECT COUNT(*) FROM student_device_logs WHERE DATE(ingress_time) = CURRENT_DATE AND (approval_status = 'Approved' OR approval_status IS NULL)) as ingress_today, " +
                "  (SELECT COUNT(*) FROM student_device_logs WHERE DATE(egress_time) = CURRENT_DATE AND (approval_status = 'Approved' OR approval_status IS NULL)) as egress_today";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                metrics.put("totalStudents", rs.getInt("total_students"));
                metrics.put("totalDevices", rs.getInt("total_devices"));
                metrics.put("devicesInside", rs.getInt("devices_inside"));
                metrics.put("ingressToday", rs.getInt("ingress_today"));
                metrics.put("egressToday", rs.getInt("egress_today"));
            }
        }
        return metrics;
    }

    public String generateQR(String payload, String formId, String outputDir) throws Exception {
        String qrDir = System.getProperty("user.home") + File.separator + "BYOD_QR_Codes";
        java.nio.file.Path dirPath = FileSystems.getDefault().getPath(qrDir);
        if (!java.nio.file.Files.exists(dirPath)) {
            java.nio.file.Files.createDirectories(dirPath);
        }
        String path = qrDir + File.separator + "QR_" + formId + ".png";
        BitMatrix matrix = new MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, 400, 400);
        MatrixToImageWriter.writeToPath(matrix, "PNG", FileSystems.getDefault().getPath(path));
        return path;
    }

    /* ════════════════════════════════════════════════════════════════════ */
    /* ── REPORT GENERATION METRIC QUERIES FOR REPORTSCONTROLLER ────────── */
    /* ════════════════════════════════════════════════════════════════════ */

    /**
     * Pulls the count of hardware grouped by device type from student_device_logs.
     */
    public Map<String, Integer> fetchInventoryBreakdown() {
        Map<String, Integer> breakdown = new HashMap<>();
        breakdown.put("Display devices", 0);
        breakdown.put("Appliances", 0);
        breakdown.put("Sounds and light equipment", 0);
        breakdown.put("Other project prototypes", 0);
        breakdown.put("Rentable items (DDMI)", 0);

        String sql = "SELECT device_type, COUNT(*) as count FROM student_device_logs WHERE device_type IS NOT NULL GROUP BY device_type";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String type = rs.getString("device_type");
                int count = rs.getInt("count");

                if (type == null || type.isBlank()) continue;
                if (breakdown.containsKey(type)) {
                    breakdown.put(type, count);
                } else {
                    breakdown.put("Other project prototypes", breakdown.get("Other project prototypes") + count);
                }
            }
        } catch (Exception e) {
            System.err.println("Error pulling inventory breakdown: " + e.getMessage());
        }
        return breakdown;
    }

    /**
     * Fetches all registered student profiles for the reports master table list.
     * Groups devices by type and brand for a single row per student.
     */
    public List<String[]> fetchRegisteredStudentsList() {
        return fetchFilteredStudentsList(null, null);
    }

    /**
     * Fetches filtered student profiles by status and/or period.
     * @param status  "All", "Approved", "Disapproved", "Cancelled", "Pending", or null for all
     * @param period  "This Month", "Last Month", "Last 3 Months", "This Year", or null for all
     */
    public List<String[]> fetchFilteredStudentsList(String status, String period) {
        List<String[]> students = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
                "SELECT form_id, student_id, " +
                        "MAX(first_name) as first_name, " +
                        "MAX(last_name) as last_name, " +
                        "MAX(user_type) as user_type, " +
                        "MAX(course_program) as course_program, " +
                        "MAX(year_section) as year_section, " +
                        "MAX(contact_number) as contact_number, " +
                        "GROUP_CONCAT(DISTINCT device_type SEPARATOR ', ') as device_type, " +
                        "GROUP_CONCAT(DISTINCT brand_model SEPARATOR ', ') as brand_model, " +
                        "MAX(color_description) as color_description, " +
                        "MAX(scheduled_entry_date) as scheduled_entry_date, " +
                        "MAX(approval_status) as approval_status, " +
                        "MAX(approval_remarks) as approval_remarks " +
                        "FROM student_device_logs " +
                        "WHERE student_id IS NOT NULL AND (is_deleted = 0 OR is_deleted IS NULL) "
        );

        List<Object> params = new ArrayList<>();

        if (status != null && !status.equals("All")) {
            sql.append("AND approval_status = ? ");
            params.add(status);
        }

        if (period != null && !period.equals("All")) {
            switch (period) {
                case "This Month":
                    sql.append("AND YEAR(ingress_time) = YEAR(CURDATE()) AND MONTH(ingress_time) = MONTH(CURDATE()) ");
                    break;
                case "Last Month":
                    sql.append("AND YEAR(ingress_time) = YEAR(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)) " +
                               "AND MONTH(ingress_time) = MONTH(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)) ");
                    break;
                case "Last 3 Months":
                    sql.append("AND ingress_time >= DATE_SUB(CURDATE(), INTERVAL 3 MONTH) ");
                    break;
                case "This Year":
                    sql.append("AND YEAR(ingress_time) = YEAR(CURDATE()) ");
                    break;
            }
        }

        sql.append("GROUP BY form_id, student_id ORDER BY last_name ASC");

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String fullName = rs.getString("first_name") + " " + rs.getString("last_name");
                    students.add(new String[]{
                            rs.getString("form_id"),
                            fullName,
                            rs.getString("user_type") != null ? rs.getString("user_type") : "N/A",
                            rs.getString("student_id"),
                            rs.getString("course_program") != null ? rs.getString("course_program") : "N/A",
                            rs.getString("year_section") != null ? rs.getString("year_section") : "N/A",
                            rs.getString("contact_number") != null ? rs.getString("contact_number") : "N/A",
                            rs.getString("device_type") != null ? rs.getString("device_type") : "Unknown",
                            rs.getString("brand_model") != null ? rs.getString("brand_model") : "N/A",
                            rs.getString("color_description") != null ? rs.getString("color_description") : "N/A",
                            rs.getString("scheduled_entry_date") != null ? rs.getString("scheduled_entry_date") : "N/A",
                            rs.getString("approval_status") != null ? rs.getString("approval_status") : "N/A",
                            rs.getString("approval_remarks") != null ? rs.getString("approval_remarks") : ""
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching filtered students list: " + e.getMessage());
        }
        return students;
    }

    /**
     * Generates weekly ingress and egress totals for the current month's chart visualization.
     */
    public Map<String, Map<String, Integer>> fetchWeeklyChartData() {
        Map<String, Map<String, Integer>> chartData = new HashMap<>();
        Map<String, Integer> ingressMap = new HashMap<>();
        Map<String, Integer> egressMap = new HashMap<>();

        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (String day : days) {
            ingressMap.put(day, 0);
            egressMap.put(day, 0);
        }

        String sql = "SELECT DAYNAME(ingress_time) as day_name, " +
                "COUNT(*) as ingress_count, " +
                "SUM(CASE WHEN egress_time IS NOT NULL THEN 1 ELSE 0 END) as egress_count " +
                "FROM student_device_logs " +
                "WHERE YEARWEEK(ingress_time, 1) = YEARWEEK(CURDATE(), 1) " +
                "GROUP BY day_name";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String day = rs.getString("day_name").substring(0, 3); // Gets "Mon", "Tue" etc.
                ingressMap.put(day, rs.getInt("ingress_count"));
                egressMap.put(day, rs.getInt("egress_count"));
            }
        } catch (SQLException e) { e.printStackTrace(); }

        chartData.put("Ingress", ingressMap);
        chartData.put("Egress", egressMap);
        return chartData;
    }

    public Map<String, Map<String, Integer>> fetchDailyChartData() {
        Map<String, Integer> ingressMap = new LinkedHashMap<>();
        Map<String, Integer> egressMap  = new LinkedHashMap<>();
        String[] hours = {
                "12AM","1AM","2AM","3AM","4AM","5AM","6AM","7AM","8AM","9AM","10AM","11AM",
                "12PM","1PM","2PM","3PM","4PM","5PM","6PM","7PM","8PM","9PM","10PM","11PM"
        };
        for (String h : hours) { ingressMap.put(h, 0); egressMap.put(h, 0); }

        String sql = "SELECT DATE_FORMAT(ingress_time, '%l%p') as hr, COUNT(*) as ic, " +
                "SUM(CASE WHEN egress_time IS NOT NULL THEN 1 ELSE 0 END) as ec " +
                "FROM student_device_logs " +
                "WHERE DATE(ingress_time) = CURDATE() " +
                "GROUP BY hr";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String key = rs.getString("hr");
                if (ingressMap.containsKey(key)) {
                    ingressMap.put(key, rs.getInt("ic"));
                    egressMap.put(key, rs.getInt("ec"));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }

        Map<String, Map<String, Integer>> result = new LinkedHashMap<>();
        result.put("Ingress", ingressMap);
        result.put("Egress", egressMap);
        return result;
    }

    public Map<String, Map<String, Integer>> fetchWeeklyByDayChartData() {
        Map<String, Integer> ingressMap = new LinkedHashMap<>();
        Map<String, Integer> egressMap  = new LinkedHashMap<>();
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (String d : days) { ingressMap.put(d, 0); egressMap.put(d, 0); }

        String sql = "SELECT DAYNAME(ingress_time) as dn, COUNT(*) as ic, " +
                "SUM(CASE WHEN egress_time IS NOT NULL THEN 1 ELSE 0 END) as ec " +
                "FROM student_device_logs " +
                "WHERE ingress_time >= DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY) " +
                "AND ingress_time < DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY) " +
                "GROUP BY dn";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String key = rs.getString("dn").substring(0, 3);
                if (ingressMap.containsKey(key)) {
                    ingressMap.put(key, rs.getInt("ic"));
                    egressMap.put(key, rs.getInt("ec"));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }

        Map<String, Map<String, Integer>> result = new LinkedHashMap<>();
        result.put("Ingress", ingressMap);
        result.put("Egress", egressMap);
        return result;
    }

    public Map<String, Map<String, Integer>> fetchMonthlyChartData() {
        Map<String, Integer> ingressMap = new LinkedHashMap<>();
        Map<String, Integer> egressMap  = new LinkedHashMap<>();
        String[] months = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
        for (String m : months) { ingressMap.put(m, 0); egressMap.put(m, 0); }

        String sql = "SELECT DATE_FORMAT(ingress_time,'%b') as mn, COUNT(*) as ic, " +
                "SUM(CASE WHEN egress_time IS NOT NULL THEN 1 ELSE 0 END) as ec " +
                "FROM student_device_logs WHERE YEAR(ingress_time) = YEAR(CURDATE()) GROUP BY mn";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String key = rs.getString("mn");
                if (ingressMap.containsKey(key)) {
                    ingressMap.put(key, rs.getInt("ic"));
                    egressMap.put(key, rs.getInt("ec"));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }

        return Map.of("Ingress", ingressMap, "Egress", egressMap);
    }

    public Map<String, Map<String, Integer>> fetchQuarterlyChartData() {
        Map<String, Integer> ingressMap = new LinkedHashMap<>();
        Map<String, Integer> egressMap  = new LinkedHashMap<>();
        String[] quarters = {"Q1 (Jan-Mar)", "Q2 (Apr-Jun)", "Q3 (Jul-Sep)", "Q4 (Oct-Dec)"};
        for (String q : quarters) { ingressMap.put(q, 0); egressMap.put(q, 0); }

        String sql = "SELECT CONCAT('Q', QUARTER(ingress_time), " +
                "CASE QUARTER(ingress_time) WHEN 1 THEN ' (Jan-Mar)' WHEN 2 THEN ' (Apr-Jun)' " +
                "WHEN 3 THEN ' (Jul-Sep)' ELSE ' (Oct-Dec)' END) as qn, " +
                "COUNT(*) as ic, SUM(CASE WHEN egress_time IS NOT NULL THEN 1 ELSE 0 END) as ec " +
                "FROM student_device_logs WHERE YEAR(ingress_time) = YEAR(CURDATE()) GROUP BY qn";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String key = rs.getString("qn");
                if (ingressMap.containsKey(key)) {
                    ingressMap.put(key, rs.getInt("ic"));
                    egressMap.put(key, rs.getInt("ec"));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }

        Map<String, Map<String, Integer>> result = new LinkedHashMap<>();
        result.put("Ingress", ingressMap);
        result.put("Egress", egressMap);
        return result;
    }

    public Map<String, Map<String, Integer>> fetchAnnualChartData() {
        Map<String, Integer> ingressMap = new LinkedHashMap<>();
        Map<String, Integer> egressMap  = new LinkedHashMap<>();

        String sqlYears = "SELECT DISTINCT YEAR(ingress_time) as yr FROM student_device_logs ORDER BY yr ASC";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sqlYears)) {
            while (rs.next()) {
                String yr = String.valueOf(rs.getInt("yr"));
                ingressMap.put(yr, 0);
                egressMap.put(yr, 0);
            }
        } catch (SQLException e) { e.printStackTrace(); }

        if (ingressMap.isEmpty()) {
            String yr = String.valueOf(java.time.LocalDate.now().getYear());
            ingressMap.put(yr, 0);
            egressMap.put(yr, 0);
        }

        String sql = "SELECT YEAR(ingress_time) as yr, COUNT(*) as ic, " +
                "SUM(CASE WHEN egress_time IS NOT NULL THEN 1 ELSE 0 END) as ec " +
                "FROM student_device_logs GROUP BY yr ORDER BY yr ASC";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String key = String.valueOf(rs.getInt("yr"));
                if (ingressMap.containsKey(key)) {
                    ingressMap.put(key, rs.getInt("ic"));
                    egressMap.put(key, rs.getInt("ec"));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }

        Map<String, Map<String, Integer>> result = new LinkedHashMap<>();
        result.put("Ingress", ingressMap);
        result.put("Egress", egressMap);
        return result;
    }

    /* ════════════════════════════════════════════════════════════════════ */
    /* ── CRUD OPERATIONS FOR REPORTS CONTROLLER ────────────────────────── */
    /* ════════════════════════════════════════════════════════════════════ */

    // FIXED: Added ingress_time = CURRENT_TIMESTAMP so manual inserts show up on the dashboard today
    public boolean insertRegisteredStudent(String id, String name, String dept, String type, String brandModel, String phone, String status, String remarks) {
        String fn = "";
        String ln = name;

        if (name != null && name.contains(", ")) {
            String[] parts = name.split(", ", 2);
            ln = parts[0];
            fn = parts.length > 1 ? parts[1] : "";
        } else if (name != null && name.contains(" ")) {
            String[] parts = name.split(" ", 2);
            fn = parts[0];
            ln = parts[1];
        }

        String formId;
        try { formId = generateFormId(); } catch (Exception e) { return false; }

        String sql = "INSERT INTO student_device_logs (form_id, first_name, last_name, course_program, device_type, brand_model, contact_number, approval_status, approval_remarks, ingress_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, formId);
            ps.setString(2, fn);
            ps.setString(3, ln);
            ps.setString(4, dept);
            ps.setString(5, type);
            ps.setString(6, brandModel);
            ps.setString(7, phone);
            ps.setString(8, status);
            ps.setString(9, remarks);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error inserting registered student: " + e.getMessage());
            return false;
        }
    }

    // 2. UPDATE: Modifies an existing student's record based on their Student ID
    public boolean updateRegisteredStudent(String id, String name, String dept, String type, String brandModel, String phone, String status, String remarks, String userCategory, String yearSection, String colorDesc, String entryDate) {
        String fn = "";
        String ln = name;

        if (name != null && name.contains(", ")) {
            String[] parts = name.split(", ", 2);
            ln = parts[0];
            fn = parts.length > 1 ? parts[1] : "";
        } else if (name != null && name.contains(" ")) {
            String[] parts = name.split(" ", 2);
            fn = parts[0];
            ln = parts[1];
        }

        String sql = "UPDATE student_device_logs SET first_name = ?, last_name = ?, course_program = ?, device_type = ?, brand_model = ?, contact_number = ?, approval_status = ?, approval_remarks = ?, user_type = ?, year_section = ?, color_description = ?, scheduled_entry_date = ? WHERE form_id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, fn);
            ps.setString(2, ln);
            ps.setString(3, dept);
            ps.setString(4, type);
            ps.setString(5, brandModel);
            ps.setString(6, phone);
            ps.setString(7, status);
            ps.setString(8, remarks);
            ps.setString(9, userCategory);
            ps.setString(10, yearSection);
            ps.setString(11, colorDesc);
            ps.setString(12, (entryDate != null && !entryDate.isBlank()) ? entryDate : null);
            ps.setString(13, id);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error updating registered student: " + e.getMessage());
            return false;
        }
    }

    // 3. SOFT DELETE: Marks a student's records as deleted (trash bin logic)
    public boolean deleteRegisteredStudent(String id) {
        String sql = "UPDATE student_device_logs SET is_deleted = 1 WHERE form_id = ? AND (is_deleted = 0 OR is_deleted IS NULL)";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error soft-deleting registered student: " + e.getMessage());
            return false;
        }
    }

    // Restore a soft-deleted student from trash
    public boolean restoreRegisteredStudent(String id) {
        String sql = "UPDATE student_device_logs SET is_deleted = 0 WHERE form_id = ? AND is_deleted = 1";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error restoring registered student: " + e.getMessage());
            return false;
        }
    }

    // Permanently delete (empty trash)
    public boolean permanentlyDeleteStudent(String id) {
        String sql = "DELETE FROM student_device_logs WHERE form_id = ? AND is_deleted = 1";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error permanently deleting student: " + e.getMessage());
            return false;
        }
    }

    // Fetch soft-deleted students (trash bin)
    public List<String[]> fetchDeletedStudentsList() {
        List<String[]> students = new ArrayList<>();

        String sql = "SELECT form_id, student_id, " +
                "MAX(first_name) as first_name, " +
                "MAX(last_name) as last_name, " +
                "MAX(user_type) as user_type, " +
                "MAX(course_program) as course_program, " +
                "MAX(year_section) as year_section, " +
                "MAX(contact_number) as contact_number, " +
                "GROUP_CONCAT(DISTINCT device_type SEPARATOR ', ') as device_type, " +
                "GROUP_CONCAT(DISTINCT brand_model SEPARATOR ', ') as brand_model, " +
                "MAX(color_description) as color_description, " +
                "MAX(scheduled_entry_date) as scheduled_entry_date, " +
                "MAX(approval_status) as approval_status, " +
                "MAX(approval_remarks) as approval_remarks " +
                "FROM student_device_logs WHERE student_id IS NOT NULL AND is_deleted = 1 " +
                "GROUP BY form_id, student_id " +
                "ORDER BY last_name ASC";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String fullName = rs.getString("first_name") + " " + rs.getString("last_name");
                students.add(new String[]{
                        rs.getString("form_id"),
                        fullName,
                        rs.getString("user_type") != null ? rs.getString("user_type") : "N/A",
                        rs.getString("student_id"),
                        rs.getString("course_program") != null ? rs.getString("course_program") : "N/A",
                        rs.getString("year_section") != null ? rs.getString("year_section") : "N/A",
                        rs.getString("contact_number") != null ? rs.getString("contact_number") : "N/A",
                        rs.getString("device_type") != null ? rs.getString("device_type") : "Unknown",
                        rs.getString("brand_model") != null ? rs.getString("brand_model") : "N/A",
                        rs.getString("color_description") != null ? rs.getString("color_description") : "N/A",
                        rs.getString("scheduled_entry_date") != null ? rs.getString("scheduled_entry_date") : "N/A",
                        rs.getString("approval_status") != null ? rs.getString("approval_status") : "N/A",
                        rs.getString("approval_remarks") != null ? rs.getString("approval_remarks") : ""
                });
            }
        } catch (Exception e) {
            System.err.println("Error fetching deleted students list: " + e.getMessage());
        }
        return students;
    }

    public String getDbUrl()  { return DB_URL; }
    public String getDbUser() { return DB_USER; }
    public String getDbPass() { return DB_PASS; }

    public List<String[]> fetchExportByDateRange(java.time.LocalDate from, java.time.LocalDate to, java.util.List<String> userTypes, java.util.List<String> deviceTypes) {
        List<String[]> rows = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT form_id, student_id, CONCAT(first_name,' ',last_name), user_type, course_program, year_section, device_type, brand_model, color_description, contact_number, scheduled_entry_date, approval_status, approval_remarks " +
                        "FROM student_device_logs WHERE (is_deleted = 0 OR is_deleted IS NULL)"
        );
        if (from != null) sql.append(" AND DATE(submitted_time) >= '").append(from).append("'");
        if (to   != null) sql.append(" AND DATE(submitted_time) <= '").append(to).append("'");
        if (userTypes != null && !userTypes.isEmpty()) {
            sql.append(" AND user_type IN ('").append(String.join("','", userTypes)).append("')");
        }
        if (deviceTypes != null && !deviceTypes.isEmpty()) {
            sql.append(" AND device_type IN ('").append(String.join("','", deviceTypes)).append("')");
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             java.sql.Statement st = conn.createStatement();
             java.sql.ResultSet rs = st.executeQuery(sql.toString())) {
            while (rs.next()) {
                rows.add(new String[]{ rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8),
                        rs.getString(9), rs.getString(10), rs.getString(11), rs.getString(12), rs.getString(13) });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return rows;
    }

    public List<String[]> fetchExportByPeriodScope(String type, String value, java.util.List<String> userTypes, java.util.List<String> deviceTypes) {
        List<String[]> rows = new ArrayList<>();
        String condition;
        switch (type) {
            case "Annual" -> condition = "YEAR(submitted_time) = " + value;
            case "Quarter" -> {
                int yr = Integer.parseInt(value.replaceAll(".*,\\s*", "").trim());
                int qr = Integer.parseInt(value.replaceAll("Q(\\d).*", "$1"));
                condition = "YEAR(submitted_time) = " + yr + " AND QUARTER(submitted_time) = " + qr;
            }
            case "Month" -> {
                String[] parts = value.split(" ");
                String[] monthNames = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
                int mn = java.util.Arrays.asList(monthNames).indexOf(parts[0]) + 1;
                int yr = Integer.parseInt(parts[1]);
                condition = "YEAR(submitted_time) = " + yr + " AND MONTH(submitted_time) = " + mn;
            }
            default -> condition = "1=1";
        }

        StringBuilder sql = new StringBuilder(
                "SELECT form_id, student_id, CONCAT(first_name,' ',last_name), user_type, course_program, year_section, device_type, brand_model, color_description, contact_number, scheduled_entry_date, approval_status, approval_remarks " +
                        "FROM student_device_logs WHERE (is_deleted = 0 OR is_deleted IS NULL) AND " + condition
        );
        if (userTypes != null && !userTypes.isEmpty()) {
            sql.append(" AND user_type IN ('").append(String.join("','", userTypes)).append("')");
        }
        if (deviceTypes != null && !deviceTypes.isEmpty()) {
            sql.append(" AND device_type IN ('").append(String.join("','", deviceTypes)).append("')");
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             java.sql.Statement st = conn.createStatement();
             java.sql.ResultSet rs = st.executeQuery(sql.toString())) {
            while (rs.next()) {
                rows.add(new String[]{ rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8),
                        rs.getString(9), rs.getString(10), rs.getString(11), rs.getString(12), rs.getString(13) });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return rows;
    }

}