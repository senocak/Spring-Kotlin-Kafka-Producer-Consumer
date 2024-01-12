create table if not exists roles(
    id         uuid not null primary key,
    name       varchar(255) not null,
    created_at timestamp,
    updated_at timestamp
);

create table if not exists users(
    id         uuid not null primary key,
    email      varchar(255) constraint uk_users_email unique not null,
    name       varchar(255) not null,
    password   varchar(255) not null,
    created_at timestamp not null default CURRENT_TIMESTAMP,
    updated_at timestamp not null default CURRENT_TIMESTAMP
);

create table if not exists user_roles(
    user_id uuid not null constraint fk_userrole_user_id references users,
    role_id uuid not null constraint fk_userrole_role_id references roles,
    primary key (user_id, role_id)
);