create table clients (
    id varchar(36) not null,
    organization_id varchar(255) not null,
    name varchar(255) not null,
    document varchar(255),
    email varchar(255),
    phone varchar(255),
    notes text,
    portal_token varchar(255) unique,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    primary key (id)
);

create index idx_client_org on clients (organization_id);
