-- Flyway V1 migration: create users table with roles and insert initial users

CREATE TABLE IF NOT EXISTS users (
  id SERIAL PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  first_name VARCHAR(100) NOT NULL,
  last_name VARCHAR(100) NOT NULL,
  phone VARCHAR(20),
  department VARCHAR(100),
  role VARCHAR(50) NOT NULL DEFAULT 'EMPLOYEE',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert initial users with different roles (matching Keycloak realm)
INSERT INTO users (email, first_name, last_name, phone, department, role)
VALUES
  ('admin@hrcore.com', 'Super', 'Admin', '+1-555-0001', 'IT', 'SUPER_ADMIN'),
  ('manager@hrcore.com', 'John', 'Manager', '+1-555-1001', 'HR', 'MANAGER'),
  ('employee1@hrcore.com', 'Jane', 'Smith', '+1-555-1002', 'Engineering', 'EMPLOYEE'),
  ('employee2@hrcore.com', 'Bob', 'Johnson', '+1-555-1003', 'Engineering', 'EMPLOYEE')
ON CONFLICT (email) DO NOTHING;
