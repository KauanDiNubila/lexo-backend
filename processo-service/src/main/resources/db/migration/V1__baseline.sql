create table cases (
    id varchar(36) not null,
    organization_id varchar(255) not null,
    client_id varchar(255) not null,
    number varchar(255) not null,
    area varchar(255),
    status varchar(255) not null,
    description text,
    responsavel_id varchar(255),
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    primary key (id),
    constraint uq_case_org_number unique (organization_id, number)
);

create index idx_case_org on cases (organization_id);
create index idx_case_client on cases (client_id);
create index idx_case_responsavel on cases (responsavel_id);
create index idx_case_status on cases (status);

create table andamentos (
    id varchar(36) not null,
    organization_id varchar(255) not null,
    case_id varchar(255) not null,
    title varchar(255) not null,
    description text,
    date timestamp(6) with time zone not null,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    primary key (id)
);

create index idx_andamento_org on andamentos (organization_id);
create index idx_andamento_case on andamentos (case_id);
create index idx_andamento_date on andamentos (date);

create table deadlines (
    id varchar(36) not null,
    organization_id varchar(255) not null,
    case_id varchar(255) not null,
    type varchar(255) not null,
    status varchar(255) not null,
    title varchar(255) not null,
    description text,
    date timestamp(6) with time zone not null,
    notified_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    primary key (id)
);

create index idx_deadline_org on deadlines (organization_id);
create index idx_deadline_case on deadlines (case_id);
create index idx_deadline_date on deadlines (date);
create index idx_deadline_status on deadlines (status);

create table activity_logs (
    id varchar(36) not null,
    organization_id varchar(255) not null,
    case_id varchar(255) not null,
    user_id varchar(255),
    user_name varchar(255) not null,
    action varchar(255) not null,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    primary key (id)
);

create index idx_activity_org on activity_logs (organization_id);
create index idx_activity_case on activity_logs (case_id);
create index idx_activity_created on activity_logs (created_at);
