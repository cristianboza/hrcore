-- Add createdById column to absence_requests table
ALTER TABLE absence_requests 
ADD COLUMN created_by_id UUID;

-- Update existing records to set created_by_id same as user_id (self-created)
UPDATE absence_requests 
SET created_by_id = user_id 
WHERE created_by_id IS NULL;

-- Make the column not null after data migration
ALTER TABLE absence_requests 
ALTER COLUMN created_by_id SET NOT NULL;
