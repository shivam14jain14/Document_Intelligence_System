-- ============================================================
-- Categories (admin-managed)
-- ============================================================
CREATE TABLE categories (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Seed default categories
INSERT INTO categories (name, description) VALUES
    ('Legal',       'Contracts, agreements, compliance documents'),
    ('HR',          'Policies, employee handbooks, onboarding'),
    ('Finance',     'Financial reports, budgets, invoices'),
    ('Security',    'Security policies, audits, assessments'),
    ('Engineering', 'Technical docs, architecture, runbooks'),
    ('General',     'Uncategorized documents');

-- ============================================================
-- User status: PENDING (awaiting approval) / ACTIVE / DISABLED
-- ============================================================
ALTER TABLE users ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

-- Existing users remain ACTIVE; new self-registrations will be PENDING.

-- ============================================================
-- Audit log
-- ============================================================
CREATE TABLE audit_logs (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_email  VARCHAR(255),
    action      VARCHAR(50)  NOT NULL,        -- LOGIN, REGISTER, UPLOAD, DELETE_DOC, QUERY, CREATE_USER, APPROVE_USER, etc.
    target      VARCHAR(500),                 -- document name, user email, etc.
    details     TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_created_at ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_action     ON audit_logs(action);
