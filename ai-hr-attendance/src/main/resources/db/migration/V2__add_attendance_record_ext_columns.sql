ALTER TABLE attendance_record
  ADD COLUMN check_in_location VARCHAR(255) NULL,
  ADD COLUMN check_out_location VARCHAR(255) NULL,
  ADD COLUMN check_in_location_valid BIT(1) NOT NULL DEFAULT b'0',
  ADD COLUMN check_out_location_valid BIT(1) NOT NULL DEFAULT b'0',
  ADD COLUMN check_in_device_type VARCHAR(64) NULL,
  ADD COLUMN check_out_device_type VARCHAR(64) NULL;

