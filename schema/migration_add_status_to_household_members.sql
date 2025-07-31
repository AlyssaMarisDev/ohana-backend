-- Migration: Add status column to household_members table
-- This migration replaces the is_active boolean with a status enum

-- Add the status column
ALTER TABLE `household_members`
ADD COLUMN `status` varchar(20) NOT NULL DEFAULT 'invited'
COMMENT 'Status of the member (active, invited, inactive)'
AFTER `role`;

-- Update existing data to map is_active to status
UPDATE `household_members`
SET `status` = CASE
    WHEN `is_active` = 1 THEN 'active'
    ELSE 'invited'
END;

-- Add index for the new status column
CREATE INDEX `idx_household_member_status` ON `household_members` (`status`);

-- Update the existing index to use status instead of is_active
DROP INDEX `idx_active_members` ON `household_members`;
CREATE INDEX `idx_household_members_status` ON `household_members` (`household_id`, `status`);

-- Note: The is_active column will be removed in a separate migration