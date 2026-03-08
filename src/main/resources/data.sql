-- Seed data for demo
INSERT INTO users (id, username, password, email, role, balance) VALUES
(1, 'admin', 'admin123', 'admin@bank.local', 'ADMIN', 99999.00),
(2, 'alice', 'password1', 'alice@bank.local', 'USER', 1500.00),
(3, 'bob',   'qwerty',   'bob@bank.local',   'USER', 300.00),
(4, 'charlie','letmein', 'charlie@bank.local','USER', 750.00);

INSERT INTO messages (id, author, content) VALUES
(1, 'admin', 'Welcome to VulnBank! <em>Stay safe.</em>'),
(2, 'alice', 'Hello everyone!');
