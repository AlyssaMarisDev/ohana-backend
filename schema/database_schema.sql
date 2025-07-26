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
  `is_default` tinyint(1) DEFAULT 0 COMMENT 'Whether this is the default household for the member',
  `invited_by` char(36) DEFAULT NULL COMMENT 'ID of the member who sent the invitation',
  `invited_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'When the invitation was sent',
  `joined_at` timestamp NULL DEFAULT NULL COMMENT 'When the member joined the household',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record update timestamp',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_household_member` (`household_id`, `member_id`),
  UNIQUE KEY `uk_member_default_household` (`member_id`, `is_default`) WHERE `is_default` = TRUE,
  KEY `idx_household_id` (`household_id`),
  KEY `idx_member_id` (`member_id`),
  KEY `idx_invited_by` (`invited_by`),
  KEY `idx_active_members` (`household_id`, `is_active`),
  KEY `idx_default_household` (`member_id`, `is_default`),
  CONSTRAINT `fk_household_members_household_id` FOREIGN KEY (`household_id`) REFERENCES `households` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_household_members_member_id` FOREIGN KEY (`member_id`) REFERENCES `members` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_household_members_invited_by` FOREIGN KEY (`invited_by`) REFERENCES `members` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Junction table for household membership';

-- Tags table - stores tags that households can use for tasks (including default tags)
CREATE TABLE `tags` (
  `id` char(36) NOT NULL COMMENT 'UUID for tag identification',
  `name` varchar(100) NOT NULL COMMENT 'Name of the tag',
  `color` varchar(7) DEFAULT '#3B82F6' COMMENT 'Color of the tag in hex format',
  `household_id` char(36) NULL COMMENT 'ID of the household this tag belongs to (NULL for default tags)',
  `is_default` boolean NOT NULL DEFAULT FALSE COMMENT 'Whether this is a default tag available to all households',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record update timestamp',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_household_tag_name` (`household_id`, `name`),
  UNIQUE KEY `uk_default_tag_name` (`name`) WHERE `is_default` = TRUE,
  KEY `idx_household_id` (`household_id`),
  KEY `idx_is_default` (`is_default`),
  KEY `idx_name` (`name`),
  CONSTRAINT `fk_tags_household_id` FOREIGN KEY (`household_id`) REFERENCES `households` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Stores tags that households can use for tasks (including default tags)';

-- Tasks table - stores task information
CREATE TABLE `tasks` (
  `id` char(36) NOT NULL COMMENT 'UUID for task identification',
  `title` varchar(255) NOT NULL COMMENT 'Title of the task',
  `description` text COMMENT 'Description of the task',
  `due_date` timestamp NULL DEFAULT NULL COMMENT 'Due date for the task',
  `status` varchar(50) NOT NULL DEFAULT 'pending' COMMENT 'Status of the task (pending, in_progress, completed)',
  `completed_at` timestamp NULL DEFAULT NULL COMMENT 'Timestamp when the task was completed',
  `created_by` char(36) NOT NULL COMMENT 'ID of the member who created the task',
  `household_id` char(36) NOT NULL COMMENT 'ID of the household this task belongs to',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record update timestamp',
  PRIMARY KEY (`id`),
  KEY `idx_created_by` (`created_by`),
  KEY `idx_household_id` (`household_id`),
  KEY `idx_status` (`status`),
  KEY `idx_due_date` (`due_date`),
  KEY `idx_completed_at` (`completed_at`),
  KEY `idx_title` (`title`),
  CONSTRAINT `fk_tasks_created_by` FOREIGN KEY (`created_by`) REFERENCES `members` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_tasks_household_id` FOREIGN KEY (`household_id`) REFERENCES `households` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Stores task information';

-- Task tags junction table - manages task-tag relationships
CREATE TABLE `task_tags` (
  `id` char(36) NOT NULL COMMENT 'UUID for task tag record',
  `task_id` char(36) NOT NULL COMMENT 'Reference to task',
  `tag_id` char(36) NOT NULL COMMENT 'Reference to tag',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_tag` (`task_id`, `tag_id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_tag_id` (`tag_id`),
  CONSTRAINT `fk_task_tags_task_id` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_task_tags_tag_id` FOREIGN KEY (`tag_id`) REFERENCES `tags` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Junction table for task-tag relationships';

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

-- Insert default tags (available to all households)
INSERT INTO `tags` (`id`, `name`, `color`, `household_id`, `is_default`, `created_at`, `updated_at`) VALUES
(UUID(), 'metas', '#FF6B6B', NULL, TRUE, NOW(), NOW()),
(UUID(), 'adult', '#4ECDC4', NULL, TRUE, NOW(), NOW()),
(UUID(), 'work', '#45B7D1', NULL, TRUE, NOW(), NOW()),
(UUID(), 'kids', '#96CEB4', NULL, TRUE, NOW(), NOW()),
(UUID(), 'chores', '#FFEAA7', NULL, TRUE, NOW(), NOW());

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

-- Create tag permissions table
CREATE TABLE `tag_permissions` (
  `id` char(36) NOT NULL COMMENT 'UUID for tag permission record',
  `household_member_id` char(36) NOT NULL COMMENT 'Reference to household member',
  `permission_type` varchar(20) NOT NULL COMMENT 'Type of permission (ALLOW_ALL_EXCEPT, DENY_ALL_EXCEPT)',
  `tag_ids` JSON NOT NULL COMMENT 'Array of tag IDs for the exception list',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record update timestamp',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_household_member_permission` (`household_member_id`),
  KEY `idx_household_member_id` (`household_member_id`),
  KEY `idx_permission_type` (`permission_type`),
  CONSTRAINT `fk_tag_permissions_household_member_id` FOREIGN KEY (`household_member_id`) REFERENCES `household_members` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_permission_type` CHECK (`permission_type` IN ('CAN_VIEW_TAGS'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Stores tag-based viewing permissions for household members';

-- Show table structures
DESCRIBE members;
DESCRIBE households;
DESCRIBE household_members;
DESCRIBE tags;
DESCRIBE tasks;
DESCRIBE task_tags;
DESCRIBE refresh_tokens;
DESCRIBE tag_permissions;