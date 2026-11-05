--liquibase formatted sql
--changeset dashboard:004-remove-admin-email
drop index if exists uk_admin_user_email_lower;

alter table admin_user
    drop column email;