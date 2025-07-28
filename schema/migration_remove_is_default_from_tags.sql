-- Migration: Remove is_default from tags table and require all tags to be associated with a household
-- This migration updates the tags table to remove the is_default column and make household_id required

-- Drop existing default tags (they will be recreated per household)
DELETE FROM tags WHERE is_default = TRUE;

-- Drop the unique constraint on default tag names
ALTER TABLE tags DROP INDEX uk_default_tag_name;

-- Drop the index on is_default
ALTER TABLE tags DROP INDEX idx_is_default;

-- Make household_id NOT NULL (all tags must be associated with a household)
ALTER TABLE tags MODIFY COLUMN household_id char(36) NOT NULL COMMENT 'ID of the household this tag belongs to';

-- Drop the is_default column
ALTER TABLE tags DROP COLUMN is_default;

-- Update the unique constraint to only apply to household-specific tags
-- (Since all tags now have a household_id, this constraint applies to all tags)
-- The existing uk_household_tag_name constraint already handles this correctly

-- Add a comment to clarify the new structure
ALTER TABLE tags COMMENT = 'Stores tags that households use for tasks. All tags must be associated with a household.';