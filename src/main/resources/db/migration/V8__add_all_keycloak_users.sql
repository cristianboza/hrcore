-- Insert all users from Keycloak realm
-- Password: admin123, manager123, employee123

-- Super Admin
INSERT INTO users (id, email, first_name, last_name, phone, department, role, manager_id, created_at, updated_at)
VALUES 
    (gen_random_uuid(), 'admin@hrcore.com', 'Super', 'Admin', NULL, 'Administration', 'SUPER_ADMIN', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (email) DO NOTHING;

-- Manager 1 (Engineering)
INSERT INTO users (id, email, first_name, last_name, phone, department, role, manager_id, created_at, updated_at)
VALUES 
    (gen_random_uuid(), 'manager1@hrcore.com', 'John', 'Manager', '+1-555-0101', 'Engineering', 'MANAGER', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (email) DO NOTHING;

-- Manager 2 (Sales)
INSERT INTO users (id, email, first_name, last_name, phone, department, role, manager_id, created_at, updated_at)
VALUES 
    (gen_random_uuid(), 'manager2@hrcore.com', 'Sarah', 'Director', '+1-555-0102', 'Sales', 'MANAGER', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (email) DO NOTHING;

-- Engineering Team Employees
INSERT INTO users (id, email, first_name, last_name, phone, department, role, manager_id, created_at, updated_at)
VALUES 
    (gen_random_uuid(), 'employee1@hrcore.com', 'Jane', 'Smith', '+1-555-0201', 'Engineering', 'EMPLOYEE', 
     (SELECT id FROM users WHERE email = 'manager1@hrcore.com'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'employee2@hrcore.com', 'Bob', 'Johnson', '+1-555-0202', 'Engineering', 'EMPLOYEE', 
     (SELECT id FROM users WHERE email = 'manager1@hrcore.com'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'employee3@hrcore.com', 'Alice', 'Brown', '+1-555-0203', 'Engineering', 'EMPLOYEE', 
     (SELECT id FROM users WHERE email = 'manager1@hrcore.com'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'employee4@hrcore.com', 'Charlie', 'Davis', '+1-555-0204', 'Engineering', 'EMPLOYEE', 
     (SELECT id FROM users WHERE email = 'manager1@hrcore.com'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (email) DO NOTHING;

-- Sales Team Employees
INSERT INTO users (id, email, first_name, last_name, phone, department, role, manager_id, created_at, updated_at)
VALUES 
    (gen_random_uuid(), 'employee5@hrcore.com', 'Diana', 'Wilson', '+1-555-0301', 'Sales', 'EMPLOYEE', 
     (SELECT id FROM users WHERE email = 'manager2@hrcore.com'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'employee6@hrcore.com', 'Edward', 'Martinez', '+1-555-0302', 'Sales', 'EMPLOYEE', 
     (SELECT id FROM users WHERE email = 'manager2@hrcore.com'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'employee7@hrcore.com', 'Fiona', 'Garcia', '+1-555-0303', 'Sales', 'EMPLOYEE', 
     (SELECT id FROM users WHERE email = 'manager2@hrcore.com'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'employee8@hrcore.com', 'George', 'Anderson', '+1-555-0304', 'Sales', 'EMPLOYEE', 
     (SELECT id FROM users WHERE email = 'manager2@hrcore.com'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (email) DO NOTHING;
