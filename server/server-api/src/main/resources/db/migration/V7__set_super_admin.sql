-- 将"鼠鼠大王"用户设置为超级管理员
-- 执行前请确认用户存在，如果用户名不同请修改
UPDATE `user` SET `role` = 2 WHERE `username` = '鼠鼠大王';
