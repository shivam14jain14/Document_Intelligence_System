ALTER TABLE questionnaire_responses
    ADD COLUMN answer_sources JSONB NOT NULL DEFAULT '{}';
