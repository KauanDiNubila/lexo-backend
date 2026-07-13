create table organizations (
    id varchar(36) not null,
    name varchar(255) not null,
    plan varchar(255) not null,
    trial_ends_at timestamp(6) with time zone,
    stripe_customer_id varchar(255) unique,
    stripe_subscription_id varchar(255) unique,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    primary key (id)
);

create table users (
    id varchar(36) not null,
    organization_id varchar(255) not null,
    name varchar(255) not null,
    email varchar(255) not null unique,
    password_hash varchar(255) not null,
    role varchar(255) not null,
    totp_secret varchar(255),
    totp_enabled boolean not null,
    totp_pending_secret varchar(255),
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    primary key (id)
);

create index idx_user_org on users (organization_id);

create table user_invites (
    id varchar(36) not null,
    organization_id varchar(255) not null,
    name varchar(255) not null,
    email varchar(255) not null,
    role varchar(255) not null,
    token varchar(255) not null unique,
    expires_at timestamp(6) with time zone not null,
    accepted_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    primary key (id)
);

create index idx_invite_org on user_invites (organization_id);
create index idx_invite_token on user_invites (token);

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
