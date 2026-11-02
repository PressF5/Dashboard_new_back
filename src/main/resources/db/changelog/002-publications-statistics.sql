--liquibase formatted sql
--changeset dashboard:002-publications-statistics
create table publication_snapshot (
    id bigserial primary key,
    snapshot_date date not null unique,
    source_import_id bigint references import_batch(id),
    created_at timestamptz not null default now()
);

create table publication_msu_statistics (
    id bigserial primary key,
    snapshot_id bigint not null references publication_snapshot(id) on delete cascade,
    msu_id bigint not null references msu(id),
    source_code varchar(40),
    social_publications bigint,
    website_publications bigint,
    parent_chat_publications bigint,
    thematic_videos bigint,
    publication_reach bigint,
    organizations_percent numeric(6,2),
    constraint uk_publication_snapshot_msu unique(snapshot_id, msu_id)
);

create index idx_publication_snapshot_date on publication_snapshot(snapshot_date desc);
