-- Add user_role column to valid_tokens table
ALTER TABLE valid_tokens 
ADD COLUMN user_role VARCHAR(50);

-- Set default role for existing tokens (they will be invalidated on next login anyway)
UPDATE valid_tokens SET user_role = 'EMPLOYEE' WHERE user_role IS NULL;

-- Make the column NOT NULL
ALTER TABLE valid_tokens 
ALTER COLUMN user_role SET NOT NULL;
