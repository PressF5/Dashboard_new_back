--liquibase formatted sql
--changeset dashboard:003-admin-auth
create table admin_role (
    id bigserial primary key,
    code varchar(50) not null unique,
    name varchar(150) not null
);

insert into admin_role (code, name) values
    ('STATISTICS_ADMIN', 'Администратор основной статистики'),
    ('PUBLICATIONS_ADMIN', 'Администратор статистики публикаций'),
    ('TILDA_ADMIN', 'Администратор статистики Tilda'),
    ('DISTANCE_ADMIN', 'Администратор статистики дистанционного обучения');


create table admin_user (
    id bigserial primary key,
    username varchar(100) not null,
    email varchar(255) not null,
    password_hash varchar(255) not null,
    role_id bigint not null references admin_role(id),
    enabled boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create unique index uk_admin_user_username_lower
    on admin_user (lower(username));

create unique index uk_admin_user_email_lower
    on admin_user (lower(email));

create unique index uk_admin_user_active_role
    on admin_user (role_id)
    where enabled = true;


create table admin_refresh_token (
    id bigserial primary key,
    admin_user_id bigint not null references admin_user(id) on delete cascade,
    token_hash varchar(64) not null unique,
    token_family uuid not null,
    expires_at timestamptz not null,
    revoked_at timestamptz,
    replaced_by_id bigint references admin_refresh_token(id),
    created_at timestamptz not null default now()
);

create index idx_admin_refresh_token_user
    on admin_refresh_token (admin_user_id);

create index idx_admin_refresh_token_family
    on admin_refresh_token (token_family);

create index idx_admin_refresh_token_expires
    on admin_refresh_token (expires_at);