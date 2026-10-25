create sequence user_token_seq
    increment by 50;

create table if not exists user_token
(
    id           bigint       not null
        primary key,
    user_id      bigint       not null
        constraint fk_user_token_user_id
            references app_user
            on delete cascade,
    type         varchar(32)  not null
        constraint user_token_type_check
            check ((type)::text = ANY ((ARRAY ['EMAIL_VERIFICATION'::character varying, 'PASSWORD_RESET'::character varying])::text[])),
    token_hash   char(64)     not null
        constraint uk_user_token_token_hash
            unique,
    expires_at   timestamptz  not null,
    consumed_at  timestamptz,
    created_at   timestamptz  not null default now()
);

create index if not exists idx_user_token_user_id_type on user_token (user_id, type);
