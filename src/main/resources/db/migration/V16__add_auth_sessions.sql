-- Server-side login sessions with rotating refresh tokens.
--
-- An auth_session is one signed-in browser/device. It lives up to 90 days (sliding: every refresh
-- extends it) and is the unit that can be listed, revoked and that push subscriptions belong to.
-- The short-lived HTTP session is only an access cache in front of it.
--
-- refresh_token is the rotation chain of a session. Every refresh rotates the presented token
-- (rotated_at) and inserts exactly one successor pointing at it (parent_id, unique). Presenting a
-- token that has already been rotated outside the short grace window is treated as theft and
-- revokes the whole session ("reuse detection").
create table if not exists auth_session
(
    id            uuid         not null
        primary key,
    user_id       bigint       not null
        constraint fk_auth_session_user_id
            references app_user
            on delete cascade,
    created_at    timestamptz  not null default now(),
    last_used_at  timestamptz  not null default now(),
    expires_at    timestamptz  not null,
    revoked_at    timestamptz,
    revoke_reason varchar(32)
        constraint auth_session_revoke_reason_check
            check (revoke_reason is null or (revoke_reason)::text = any ((array [
                'LOGOUT'::character varying,
                'USER_REVOKED'::character varying,
                'REUSE_DETECTED'::character varying,
                'REPLACED'::character varying,
                'CREDENTIALS_CHANGED'::character varying,
                'ACCOUNT_DISABLED'::character varying
                ])::text[])),
    user_agent    varchar(512),
    ip_address    varchar(64),
    -- Comma-separated AuthMethod values used to establish the session (e.g. "PASSWORD,TOTP").
    auth_methods  varchar(128) not null,
    -- Last time the user proved their identity on this session; gates sensitive account changes.
    step_up_at    timestamptz
);

create index if not exists idx_auth_session_user_id on auth_session (user_id);
create index if not exists idx_auth_session_expires_at on auth_session (expires_at);

create sequence refresh_token_seq
    increment by 50;

create table if not exists refresh_token
(
    id         bigint      not null
        primary key,
    session_id uuid        not null
        constraint fk_refresh_token_session_id
            references auth_session
            on delete cascade,
    -- The token this one replaced; unique, so a rotation can only ever have one successor.
    parent_id  bigint
        constraint uk_refresh_token_parent_id
            unique
        constraint fk_refresh_token_parent_id
            references refresh_token
            on delete cascade,
    token_hash varchar(64) not null
        constraint uk_refresh_token_token_hash
            unique,
    created_at timestamptz not null default now(),
    rotated_at timestamptz
);

create index if not exists idx_refresh_token_session_id on refresh_token (session_id);
