DELETE FROM questionnaire_questions;

INSERT INTO questionnaire_questions (question_key, question_text, active, sort_order) VALUES
('job_title', 'What is your current job title?', true, 1),
('department', 'Which department or function are you part of?', true, 2),
('phone_number', 'What is your work phone number?', true, 3),
('office_location', 'Which office or city are you primarily based in?', true, 4),
('manager_name', 'Who is your direct manager?', true, 5),
('team_name', 'What team do you work with most closely?', true, 6),
('time_zone', 'What time zone do you usually work in?', true, 7),
('preferred_language', 'What language do you prefer for communication and documentation?', true, 8),
('work_mode', 'Do you work onsite, hybrid, or fully remote?', true, 9),
('primary_use_case', 'What do you mainly want to use this platform for in your day-to-day work?', true, 10);
