-- Ohana Backend Database Schema
-- This script creates all necessary tables for the Ohana application

-- Create database if it doesn't exist
CREATE DATABASE IF NOT EXISTS ohana CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Use the ohana database
USE ohana;

-- Members table - stores user information
CREATE TABLE `members` (
  `id` char(36) NOT NULL COMMENT 'UUID for member identification',
  `name` varchar(100) NOT NULL COMMENT 'Full name of the member',
  `age` int DEFAULT NULL COMMENT 'Age of the member (optional)',
  `gender` varchar(10) DEFAULT NULL COMMENT 'Gender of the member (optional)',
  `email` varchar(255) NOT NULL COMMENT 'Email address (unique)',
  `password` varchar(255) NOT NULL COMMENT 'Hashed password',
  `salt` varbinary(255) NOT NULL COMMENT 'Salt used for password hashing',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record update timestamp',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_name` (`name`),
  KEY `idx_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Stores member/user information';

-- Households table - stores household information
CREATE TABLE `households` (
  `id` char(36) NOT NULL COMMENT 'UUID for household identification',
  `name` varchar(255) NOT NULL COMMENT 'Name of the household',
  `description` text COMMENT 'Description of the household',
  `created_by` char(36) NOT NULL COMMENT 'ID of the member who created the household',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record update timestamp',
  PRIMARY KEY (`id`),
  KEY `idx_created_by` (`created_by`),
  KEY `idx_name` (`name`),
  CONSTRAINT `fk_households_created_by` FOREIGN KEY (`created_by`) REFERENCES `members` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Stores household information';

-- Household members junction table - manages household membership
CREATE TABLE `household_members` (
  `id` char(36) NOT NULL COMMENT 'UUID for household member record',
  `household_id` char(36) NOT NULL COMMENT 'Reference to household',
  `member_id` char(36) NOT NULL COMMENT 'Reference to member',
  `role` varchar(50) DEFAULT 'member' COMMENT 'Role in the household (admin, member)',
  `is_active` tinyint(1) DEFAULT 0 COMMENT 'Whether the member is active in the household',
  `invited_by` char(36) DEFAULT NULL COMMENT 'ID of the member who sent the invitation',
  `invited_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'When the invitation was sent',
  `joined_at` timestamp NULL DEFAULT NULL COMMENT 'When the member joined the household',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record update timestamp',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_household_member` (`household_id`, `member_id`),
  KEY `idx_household_id` (`household_id`),
  KEY `idx_member_id` (`member_id`),
  KEY `idx_invited_by` (`invited_by`),
  KEY `idx_active_members` (`household_id`, `is_active`),
  CONSTRAINT `fk_household_members_household_id` FOREIGN KEY (`household_id`) REFERENCES `households` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_household_members_member_id` FOREIGN KEY (`member_id`) REFERENCES `members` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_household_members_invited_by` FOREIGN KEY (`invited_by`) REFERENCES `members` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Junction table for household membership';

-- Tasks table - stores task information
CREATE TABLE `tasks` (
  `id` char(36) NOT NULL COMMENT 'UUID for task identification',
  `title` varchar(255) NOT NULL COMMENT 'Title of the task',
  `description` text COMMENT 'Description of the task',
  `due_date` timestamp NULL DEFAULT NULL COMMENT 'Due date for the task',
  `status` varchar(50) NOT NULL DEFAULT 'pending' COMMENT 'Status of the task (pending, in_progress, completed)',
  `created_by` char(36) NOT NULL COMMENT 'ID of the member who created the task',
  `household_id` char(36) NOT NULL COMMENT 'ID of the household this task belongs to',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record update timestamp',
  PRIMARY KEY (`id`),
  KEY `idx_created_by` (`created_by`),
  KEY `idx_household_id` (`household_id`),
  KEY `idx_status` (`status`),
  KEY `idx_due_date` (`due_date`),
  KEY `idx_title` (`title`),
  CONSTRAINT `fk_tasks_created_by` FOREIGN KEY (`created_by`) REFERENCES `members` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_tasks_household_id` FOREIGN KEY (`household_id`) REFERENCES `households` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Stores task information';

-- Refresh tokens table - stores refresh tokens for authentication
CREATE TABLE `refresh_tokens` (
  `id` char(36) NOT NULL COMMENT 'UUID for refresh token record',
  `token` text NOT NULL COMMENT 'The refresh token value',
  `user_id` char(36) NOT NULL COMMENT 'ID of the user this token belongs to',
  `expires_at` timestamp NOT NULL COMMENT 'When the refresh token expires',
  `is_revoked` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'Whether the token has been revoked',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',
  `revoked_at` timestamp NULL DEFAULT NULL COMMENT 'When the token was revoked',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token` (`token`(255)),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_expires_at` (`expires_at`),
  KEY `idx_is_revoked` (`is_revoked`),
  KEY `idx_user_active_tokens` (`user_id`, `is_revoked`),
  CONSTRAINT `fk_refresh_tokens_user_id` FOREIGN KEY (`user_id`) REFERENCES `members` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Stores refresh tokens for authentication';

-- Insert sample data (optional)
-- Uncomment the following lines if you want to insert sample data

/*
-- Sample member
INSERT INTO `members` (`id`, `name`, `email`, `password`, `salt`) VALUES
('550e8400-e29b-41d4-a716-446655440000', 'John Doe', 'john@example.com', 'hashed_password_here', UNHEX('salt_bytes_here'));

-- Sample household
INSERT INTO `households` (`id`, `name`, `description`, `created_by`) VALUES
('550e8400-e29b-41d4-a716-446655440001', 'Smith Family', 'The Smith family household', '550e8400-e29b-41d4-a716-446655440000');

-- Sample household member
INSERT INTO `household_members` (`id`, `household_id`, `member_id`, `role`, `is_active`, `invited_by`, `joined_at`) VALUES
('550e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440000', 'admin', 1, '550e8400-e29b-41d4-a716-446655440000', NOW());

-- Sample task
INSERT INTO `tasks` (`id`, `title`, `description`, `due_date`, `status`, `created_by`, `household_id`) VALUES
('550e8400-e29b-41d4-a716-446655440003', 'Buy groceries', 'Purchase items for the week', DATE_ADD(NOW(), INTERVAL 2 DAY), 'pending', '550e8400-e29b-41d4-a716-446655440000', '550e8400-e29b-41d4-a716-446655440001');
*/

-- Show table information
SHOW TABLES;

-- Show table structures
DESCRIBE members;
DESCRIBE households;
DESCRIBE household_members;
DESCRIBE tasks;
DESCRIBE refresh_tokens;