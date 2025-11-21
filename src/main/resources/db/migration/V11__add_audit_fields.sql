-- Add audit fields to all main tables

-- Add audit fields to users table
ALTER TABLE users
ADD COLUMN IF NOT EXISTS created_by UUID,
ADD COLUMN IF NOT EXISTS last_modified_by UUID;

-- Add audit fields to feedback table
ALTER TABLE feedback
ADD COLUMN IF NOT EXISTS created_by UUID,
ADD COLUMN IF NOT EXISTS last_modified_by UUID;

-- Add audit fields to absence_requests table
ALTER TABLE absence_requests
ADD COLUMN IF NOT EXISTS created_by UUID,
ADD COLUMN IF NOT EXISTS last_modified_by UUID;
