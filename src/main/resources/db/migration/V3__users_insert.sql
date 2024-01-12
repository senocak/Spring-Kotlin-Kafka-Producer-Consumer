INSERT INTO users (id, created_at, updated_at, email, name, password)
    VALUES ('0dba845d-12f7-41ad-bd7a-6ec587b33ae1', '2023-07-16 10:34:44.000000', '2023-07-16 10:34:44.000000', 'anil1@senocak.com', 'Lucienne', '$2a$10$znsjvm5Y06ZJmpaWGHmmNu4iDJYhk369LR.R3liw2T4RjJcnt9c12');
INSERT INTO users (id, created_at, updated_at, email, name, password)
    VALUES ('0dba845d-12f7-41ad-bd7a-6ec587b33ae2', '2023-07-16 10:34:44.000000', '2023-07-16 10:34:44.000000', 'anil2@senocak.com', 'Kiley', '$2a$10$znsjvm5Y06ZJmpaWGHmmNu4iDJYhk369LR.R3liw2T4RjJcnt9c12');

INSERT INTO user_roles (user_id, role_id)
    VALUES ('0dba845d-12f7-41ad-bd7a-6ec587b33ae1', '57665174-625f-469a-9c2b-21e289afc141');
INSERT INTO user_roles (user_id, role_id)
    VALUES ('0dba845d-12f7-41ad-bd7a-6ec587b33ae2', '57665174-625f-469a-9c2b-21e289afc142');
