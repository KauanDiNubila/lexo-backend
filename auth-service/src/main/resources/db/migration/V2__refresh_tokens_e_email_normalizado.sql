create table refresh_tokens (
    id varchar(36) not null,
    user_id varchar(255) not null,
    token_hash varchar(64) not null unique,
    expires_at timestamp(6) with time zone not null,
    revoked_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    primary key (id)
);

create index idx_refresh_token_user on refresh_tokens (user_id);

update users set email = lower(trim(email));

create unique index uq_users_email_lower on users (lower(email));
