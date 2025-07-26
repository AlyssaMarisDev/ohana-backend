-- Migration: Add completed_at column to tasks table
-- This migration adds a completed_at timestamp column to track when tasks are completed

USE ohana;

-- Add completed_at column to tasks table
ALTER TABLE `tasks`
ADD COLUMN `completed_at` timestamp NULL DEFAULT NULL COMMENT 'Timestamp when the task was completed' AFTER `status`;

-- Add index for better query performance
ALTER TABLE `tasks`
ADD INDEX `idx_completed_at` (`completed_at`);

-- Update existing completed tasks to have a completed_at timestamp
-- This sets completed_at to updated_at for tasks that are already marked as completed
UPDATE `tasks`
SET `completed_at` = `updated_at`
WHERE `status` = 'completed' AND `completed_at` IS NULL;