create table password_resets (
    id varchar(36) not null,
    user_id varchar(36) not null references users (id) on delete cascade,
    token_hash varchar(64) not null unique,
    expires_at timestamp(6) with time zone not null,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    primary key (id)
);

create index idx_password_reset_user on password_resets (user_id);
