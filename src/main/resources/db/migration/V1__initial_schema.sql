-- Baseline initial schema
-- If your database is empty, this will create all tables
-- If not, baselineOnMigrate(true) will mark this as baseline

-- Ensure database selected (no-op if provided by URL)
-- CREATE DATABASE IF NOT EXISTS ohana;
-- USE ohana;

-- Members
CREATE TABLE IF NOT EXISTS `members` (
  `id` char(36) NOT NULL,
  `name` varchar(100) NOT NULL,
  `age` int DEFAULT NULL,
  `gender` varchar(10) DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `salt` varbinary(255) NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_members_email` (`email`),
  KEY `idx_members_name` (`name`),
  KEY `idx_members_email` (`email`)
);

-- Households
CREATE TABLE IF NOT EXISTS `households` (
  `id` char(36) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` text,
  `created_by` char(36) NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_households_created_by` (`created_by`),
  KEY `idx_households_name` (`name`),
  CONSTRAINT `fk_households_created_by` FOREIGN KEY (`created_by`) REFERENCES `members` (`id`) ON DELETE CASCADE
);

-- Household members
CREATE TABLE IF NOT EXISTS `household_members` (
  `id` char(36) NOT NULL,
  `household_id` char(36) NOT NULL,
  `member_id` char(36) NOT NULL,
  `role` varchar(50) DEFAULT 'member',
  `status` varchar(20) NOT NULL DEFAULT 'invited',
  `is_default` tinyint(1) DEFAULT 0,
  `invited_by` char(36) DEFAULT NULL,
  `invited_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `joined_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_household_member` (`household_id`, `member_id`),
  -- Note: partial unique index not supported across all dialects; enforced in application logic
  KEY `idx_household_members_household_id` (`household_id`),
  KEY `idx_household_members_member_id` (`member_id`),
  KEY `idx_household_members_invited_by` (`invited_by`),
  KEY `idx_household_members_status_combo` (`household_id`, `status`),
  KEY `idx_household_members_status` (`status`),
  KEY `idx_household_members_default_household` (`member_id`, `is_default`),
  CONSTRAINT `fk_household_members_household_id` FOREIGN KEY (`household_id`) REFERENCES `households` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_household_members_member_id` FOREIGN KEY (`member_id`) REFERENCES `members` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_household_members_invited_by` FOREIGN KEY (`invited_by`) REFERENCES `members` (`id`) ON DELETE SET NULL
);

-- Tags
CREATE TABLE IF NOT EXISTS `tags` (
  `id` char(36) NOT NULL,
  `name` varchar(100) NOT NULL,
  `color` varchar(7) DEFAULT '#3B82F6',
  `household_id` char(36) NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tags_household_name` (`household_id`, `name`),
  KEY `idx_tags_household_id` (`household_id`),
  KEY `idx_tags_name` (`name`),
  CONSTRAINT `fk_tags_household_id` FOREIGN KEY (`household_id`) REFERENCES `households` (`id`) ON DELETE CASCADE
);

-- Tasks
CREATE TABLE IF NOT EXISTS `tasks` (
  `id` char(36) NOT NULL,
  `title` varchar(255) NOT NULL,
  `description` text,
  `due_date` timestamp NULL DEFAULT NULL,
  `status` varchar(50) NOT NULL DEFAULT 'pending',
  `completed_at` timestamp NULL DEFAULT NULL,
  `created_by` char(36) NOT NULL,
  `household_id` char(36) NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tasks_created_by` (`created_by`),
  KEY `idx_tasks_household_id` (`household_id`),
  KEY `idx_tasks_status` (`status`),
  KEY `idx_tasks_due_date` (`due_date`),
  KEY `idx_tasks_completed_at` (`completed_at`),
  KEY `idx_tasks_title` (`title`),
  CONSTRAINT `fk_tasks_created_by` FOREIGN KEY (`created_by`) REFERENCES `members` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_tasks_household_id` FOREIGN KEY (`household_id`) REFERENCES `households` (`id`) ON DELETE CASCADE
);

-- Task tags
CREATE TABLE IF NOT EXISTS `task_tags` (
  `id` char(36) NOT NULL,
  `task_id` char(36) NOT NULL,
  `tag_id` char(36) NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_tags_task_tag` (`task_id`, `tag_id`),
  KEY `idx_task_tags_task_id` (`task_id`),
  KEY `idx_task_tags_tag_id` (`tag_id`),
  CONSTRAINT `fk_task_tags_task_id` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_task_tags_tag_id` FOREIGN KEY (`tag_id`) REFERENCES `tags` (`id`) ON DELETE CASCADE
);

-- Refresh tokens
CREATE TABLE IF NOT EXISTS `refresh_tokens` (
  `id` char(36) NOT NULL,
  `token` varchar(255) NOT NULL,
  `user_id` char(36) NOT NULL,
  `expires_at` timestamp NOT NULL,
  `is_revoked` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `revoked_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refresh_tokens_token` (`token`),
  KEY `idx_refresh_tokens_user_id` (`user_id`),
  KEY `idx_refresh_tokens_expires_at` (`expires_at`),
  KEY `idx_refresh_tokens_is_revoked` (`is_revoked`),
  KEY `idx_refresh_tokens_user_active_tokens` (`user_id`, `is_revoked`)
);

-- Permissions
CREATE TABLE IF NOT EXISTS `permissions` (
  `id` char(36) NOT NULL,
  `household_member_id` char(36) NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permissions_household_member` (`household_member_id`),
  KEY `idx_permissions_household_member_id` (`household_member_id`),
  CONSTRAINT `fk_permissions_household_member_id` FOREIGN KEY (`household_member_id`) REFERENCES `household_members` (`id`) ON DELETE CASCADE
);

-- Tag permissions
CREATE TABLE IF NOT EXISTS `tag_permissions` (
  `id` char(36) NOT NULL,
  `permission_id` char(36) NOT NULL,
  `tag_id` char(36) NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_permissions_permission_tag` (`permission_id`, `tag_id`),
  KEY `idx_tag_permissions_permission_id` (`permission_id`),
  KEY `idx_tag_permissions_tag_id` (`tag_id`),
  CONSTRAINT `fk_tag_permissions_permission_id` FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_tag_permissions_tag_id` FOREIGN KEY (`tag_id`) REFERENCES `tags` (`id`) ON DELETE CASCADE
);


