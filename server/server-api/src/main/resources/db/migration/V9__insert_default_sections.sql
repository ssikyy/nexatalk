-- V9: Insert default sections for the forum
INSERT INTO section (name, description, icon_url, sort_order, status, created_at, updated_at) VALUES
('技术讨论', '分享编程经验、技术心得和行业动态', '', 100, 0, NOW(), NOW()),
('生活随想', '记录生活点滴、感悟人生', '', 90, 0, NOW(), NOW()),
('问答求助', '提出问题、寻求帮助', '', 80, 0, NOW(), NOW()),
('资源共享', '分享有用的工具、资源和教程', '', 70, 0, NOW(), NOW()),
('灌水区', '轻松聊天、随便聊聊', '', 60, 0, NOW(), NOW());
