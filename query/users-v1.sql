-- 아이디와 비밀번호를 등록
INSERT INTO users (username, password) VALUES ('user', 'pass');

-- 아이디와 비밀번호를 통해 식별자를 포함한 정보를 취득
SELECT id, username, password FROM users WHERE username = 'user';

-- ID를 이용하여 비밀번호를 갱신
UPDATE users SET password = 'password' WHERE id = 1;

-- ID를 이용하여 삭제
DELETE FROM users WHERE id = 1;