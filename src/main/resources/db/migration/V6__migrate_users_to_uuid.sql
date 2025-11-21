-- Flyway V6 migration: Migrate user IDs from BIGINT to UUID

-- Step 1: Add new UUID columns to all tables
ALTER TABLE users ADD COLUMN uuid_id UUID DEFAULT gen_random_uuid();
ALTER TABLE feedback ADD COLUMN uuid_from_user_id UUID;
ALTER TABLE feedback ADD COLUMN uuid_to_user_id UUID;
ALTER TABLE absence_requests ADD COLUMN uuid_user_id UUID;
ALTER TABLE absence_requests ADD COLUMN uuid_approver_id UUID;
ALTER TABLE valid_tokens ADD COLUMN uuid_user_id UUID;
ALTER TABLE invalid_tokens ADD COLUMN uuid_user_id UUID;

-- Step 2: Create a temporary mapping and update UUID values
UPDATE users SET uuid_id = gen_random_uuid();

-- Step 3: Update foreign key references in feedback table
UPDATE feedback f SET uuid_from_user_id = (SELECT u.uuid_id FROM users u WHERE u.id = f.from_user_id);
UPDATE feedback f SET uuid_to_user_id = (SELECT u.uuid_id FROM users u WHERE u.id = f.to_user_id);

-- Step 4: Update foreign key references in absence_requests table
UPDATE absence_requests ar SET uuid_user_id = (SELECT u.uuid_id FROM users u WHERE u.id = ar.user_id);
UPDATE absence_requests ar SET uuid_approver_id = (SELECT u.uuid_id FROM users u WHERE u.id = ar.approver_id) WHERE ar.approver_id IS NOT NULL;

-- Step 5: Update foreign key references in valid_tokens and invalid_tokens tables
UPDATE valid_tokens vt SET uuid_user_id = (SELECT u.uuid_id FROM users u WHERE u.id = vt.user_id);
UPDATE invalid_tokens it SET uuid_user_id = (SELECT u.uuid_id FROM users u WHERE u.id = it.user_id);

-- Step 6: Add UUID manager_id column and update it
ALTER TABLE users ADD COLUMN uuid_manager_id UUID;
UPDATE users u SET uuid_manager_id = (SELECT m.uuid_id FROM users m WHERE m.id = u.manager_id) WHERE u.manager_id IS NOT NULL;

-- Step 7: Drop old foreign key constraints
ALTER TABLE users DROP CONSTRAINT IF EXISTS fk_manager;
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_manager_id_fkey;
ALTER TABLE feedback DROP CONSTRAINT IF EXISTS feedback_from_user_id_fkey;
ALTER TABLE feedback DROP CONSTRAINT IF EXISTS feedback_to_user_id_fkey;
ALTER TABLE absence_requests DROP CONSTRAINT IF EXISTS absence_requests_user_id_fkey;
ALTER TABLE absence_requests DROP CONSTRAINT IF EXISTS absence_requests_approver_id_fkey;
ALTER TABLE valid_tokens DROP CONSTRAINT IF EXISTS valid_tokens_user_id_fkey;
ALTER TABLE invalid_tokens DROP CONSTRAINT IF EXISTS invalid_tokens_user_id_fkey;

-- Step 8: Drop old indexes
DROP INDEX IF EXISTS idx_users_manager_id;
DROP INDEX IF EXISTS idx_feedback_from_user;
DROP INDEX IF EXISTS idx_feedback_to_user;
DROP INDEX IF EXISTS idx_absence_user;
DROP INDEX IF EXISTS idx_valid_tokens_user_id;
DROP INDEX IF EXISTS idx_invalid_tokens_user_id;

-- Step 9: Drop old columns
ALTER TABLE users DROP COLUMN id;
ALTER TABLE users DROP COLUMN manager_id;
ALTER TABLE feedback DROP COLUMN from_user_id;
ALTER TABLE feedback DROP COLUMN to_user_id;
ALTER TABLE absence_requests DROP COLUMN user_id;
ALTER TABLE absence_requests DROP COLUMN approver_id;
ALTER TABLE valid_tokens DROP COLUMN user_id;
ALTER TABLE invalid_tokens DROP COLUMN user_id;

-- Step 10: Rename UUID columns to their final names
ALTER TABLE users RENAME COLUMN uuid_id TO id;
ALTER TABLE users RENAME COLUMN uuid_manager_id TO manager_id;
ALTER TABLE feedback RENAME COLUMN uuid_from_user_id TO from_user_id;
ALTER TABLE feedback RENAME COLUMN uuid_to_user_id TO to_user_id;
ALTER TABLE absence_requests RENAME COLUMN uuid_user_id TO user_id;
ALTER TABLE absence_requests RENAME COLUMN uuid_approver_id TO approver_id;
ALTER TABLE valid_tokens RENAME COLUMN uuid_user_id TO user_id;
ALTER TABLE invalid_tokens RENAME COLUMN uuid_user_id TO user_id;

-- Step 11: Add primary key and constraints
ALTER TABLE users ADD PRIMARY KEY (id);
ALTER TABLE feedback ALTER COLUMN from_user_id SET NOT NULL;
ALTER TABLE feedback ALTER COLUMN to_user_id SET NOT NULL;
ALTER TABLE absence_requests ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE valid_tokens ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE invalid_tokens ALTER COLUMN user_id SET NOT NULL;

-- Step 12: Recreate foreign key constraints
ALTER TABLE users ADD CONSTRAINT fk_manager FOREIGN KEY (manager_id) REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE feedback ADD CONSTRAINT fk_feedback_from_user FOREIGN KEY (from_user_id) REFERENCES users(id);
ALTER TABLE feedback ADD CONSTRAINT fk_feedback_to_user FOREIGN KEY (to_user_id) REFERENCES users(id);
ALTER TABLE absence_requests ADD CONSTRAINT fk_absence_user FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE absence_requests ADD CONSTRAINT fk_absence_approver FOREIGN KEY (approver_id) REFERENCES users(id);
ALTER TABLE valid_tokens ADD CONSTRAINT fk_valid_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE invalid_tokens ADD CONSTRAINT fk_invalid_tokens_user FOREIGN KEY (user_id) REFERENCES users(id);

-- Step 13: Recreate indexes
CREATE INDEX idx_users_manager_id ON users(manager_id);
CREATE INDEX idx_feedback_from_user ON feedback(from_user_id);
CREATE INDEX idx_feedback_to_user ON feedback(to_user_id);
CREATE INDEX idx_absence_user ON absence_requests(user_id);
CREATE INDEX idx_valid_tokens_user_id ON valid_tokens(user_id);
CREATE INDEX idx_invalid_tokens_user_id ON invalid_tokens(user_id);
