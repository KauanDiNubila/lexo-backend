create table audit_logs (
    id varchar(36) not null,
    organization_id varchar(255) not null,
    user_id varchar(255),
    user_name varchar(255) not null,
    action varchar(255) not null,
    entity_type varchar(255),
    entity_id varchar(255),
    description text not null,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    primary key (id)
);

create index idx_audit_org on audit_logs (organization_id);
create index idx_audit_created on audit_logs (created_at);
