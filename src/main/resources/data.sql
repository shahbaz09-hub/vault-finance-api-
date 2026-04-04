-- ============================================================
-- Seed Data: Default Admin User
-- Password: admin123 (BCrypt encoded)
-- ============================================================
INSERT IGNORE INTO users (name, email, password, role, status, created_at, updated_at)
VALUES (
    'Super Admin',
    'admin@finance.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'ADMIN',
    'ACTIVE',
    NOW(),
    NOW()
);
