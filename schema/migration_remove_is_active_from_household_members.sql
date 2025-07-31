-- Migration: Remove is_active column from household_members table
-- This migration removes the deprecated is_active column since we now use the status column

-- Remove the is_active column
ALTER TABLE `household_members` DROP COLUMN `is_active`;

-- Note: The status column should already be in place from the previous migration
-- This migration completes the transition from is_active boolean to status enum