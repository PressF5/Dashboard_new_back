--liquibase formatted sql
--changeset dashboard:001-initial-schema
create table msu (
    id bigserial primary key,
    name VARCHAR(200) NOT NULL UNIQUE,
    created_at timestamptz not null default now()
);

create table educational_organization (
    id bigserial primary key,
    msu_id bigint not null references msu(id),
    name VARCHAR(300) NOT NULL,
    created_at timestamptz not null default now(),
    constraint uk_organization_msu_name unique (msu_id, name)
);

create table import_batch (
    id bigserial primary key,
    uploaded_at timestamptz not null default now(),
    snapshot_date date not null,
    original_filename varchar(255),
    status varchar(20) not null,
    message text,
    schools_read integer not null default 0,
    created_or_updated_msu integer not null default 0
);

create index idx_import_batch_uploaded_at on import_batch(uploaded_at desc);

create table statistics_snapshot (
    id bigserial primary key,
    snapshot_date date not null unique,
    source_import_id bigint references import_batch(id),
    created_at timestamptz not null default now()
);
create table school_statistics (
    id bigserial primary key,
    snapshot_id bigint not null references statistics_snapshot(id) on delete cascade,
    organization_id bigint not null references educational_organization(id),
    metrics jsonb not null default '{}'::jsonb,
    constraint uk_school_statistics_snapshot_org unique (snapshot_id, organization_id)
);

create table msu_statistics (
    id bigserial primary key,
    snapshot_id bigint not null references statistics_snapshot(id) on delete cascade,
    msu_id bigint not null references msu(id),
    metrics jsonb not null default '{}'::jsonb,
    constraint uk_msu_statistics_snapshot_msu unique (snapshot_id, msu_id)
);

create table dashboard_metric (
    metric_key varchar(80) primary key,
    label varchar(300) not null,
    metric_order integer not null,
    category_key varchar(80) not null,
    category_label varchar(200) not null,
    unit varchar(30), is_percentage boolean not null default false
);

create index idx_school_statistics_snapshot on school_statistics(snapshot_id);
create index idx_msu_statistics_snapshot on msu_statistics(snapshot_id);
