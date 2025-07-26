-- Migration: Add tag permissions functionality
-- This migration adds support for tag-based task viewing permissions

-- Create enum for permission types
-- Note: MySQL doesn't support native enums in the same way as PostgreSQL,
-- so we'll use a VARCHAR with a CHECK constraint

-- Create tag permissions table
CREATE TABLE `household_member_tag_permissions` (
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
  CONSTRAINT `chk_permission_type` CHECK (`permission_type` IN ('ALLOW_ALL_EXCEPT', 'DENY_ALL_EXCEPT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Stores tag-based viewing permissions for household members';

-- Insert sample permissions (optional - uncomment if needed)
/*
-- Example: Adult can view all tags except "work" and "bob's work"
INSERT INTO `household_member_tag_permissions` (`id`, `household_member_id`, `permission_type`, `tag_ids`) VALUES
(UUID(), 'household_member_id_here', 'ALLOW_ALL_EXCEPT', JSON_ARRAY('work_tag_id', 'bob_work_tag_id'));

-- Example: Child can only view "kids" tag
INSERT INTO `household_member_tag_permissions` (`id`, `household_member_id`, `permission_type`, `tag_ids`) VALUES
(UUID(), 'household_member_id_here', 'DENY_ALL_EXCEPT', JSON_ARRAY('kids_tag_id'));
*/