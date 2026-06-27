-- Advance-Based Registration with Approval Workflow
-- Run this migration on the existing byod_db database

ALTER TABLE student_device_logs
  ADD COLUMN scheduled_entry_date DATE DEFAULT NULL,
  ADD COLUMN approval_status VARCHAR(20) DEFAULT 'Pending',
  ADD COLUMN approval_remarks TEXT DEFAULT NULL,
  ADD COLUMN approved_by VARCHAR(100) DEFAULT NULL,
  ADD COLUMN approval_date DATETIME DEFAULT NULL;

-- Mark all existing records as Approved (backward compatibility)
UPDATE student_device_logs SET approval_status = 'Approved' WHERE approval_status IS NULL;

-- Soft delete support
ALTER TABLE student_device_logs
  ADD COLUMN is_deleted TINYINT(1) DEFAULT 0;
