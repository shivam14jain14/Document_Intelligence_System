CREATE TABLE questionnaire_questions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_key VARCHAR(100) NOT NULL UNIQUE,
    question_text VARCHAR(1000) NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT true,
    sort_order  INT NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_questionnaire_questions_active_sort
    ON questionnaire_questions(active, sort_order);

INSERT INTO questionnaire_questions (question_key, question_text, active, sort_order) VALUES
('company_name', 'What is your company name?', true, 1),
('industry', 'What industry is your company in?', true, 2),
('company_size', 'How many employees does your company have?', true, 3),
('products', 'What are your company''s primary products or services?', true, 4),
('hq_location', 'Where is your company headquartered?', true, 5),
('primary_contact', 'Who is the primary business contact for this account?', true, 6),
('security_contact', 'Who is your primary IT or security point of contact?', true, 7),
('cloud_providers', 'Which cloud providers do you use (AWS, Azure, GCP, on-prem)?', true, 8),
('doc_storage', 'Where are your documents primarily stored today, and roughly what volume?', true, 9),
('desired_outcomes', 'What outcomes are you hoping to get from this platform?', true, 10);
