-- Optional account credentials (passkeys, TOTP) and retirement of the old token-based flows.

-- WebAuthn user handle: random, opaque, stable per account (never the e-mail or the numeric id).
alter table app_user
    add column webauthn_user_handle varchar(64)
        constraint uk_app_user_webauthn_user_handle
            unique;

-- TOTP (RFC 6238). Secrets are AES-GCM encrypted with app.security.totp-encryption-key.
-- totp_pending_secret_enc holds a secret during enrolment until the first code confirms it.
-- totp_last_used_step blocks replaying a code within its validity window.
alter table app_user
    add column totp_secret_enc         varchar(255),
    add column totp_pending_secret_enc varchar(255),
    add column totp_enabled_at         timestamptz,
    add column totp_last_used_step     bigint;

create sequence webauthn_credential_seq
    increment by 50;

create table if not exists webauthn_credential
(
    id                        bigint       not null
        primary key,
    user_id                   bigint       not null
        constraint fk_webauthn_credential_user_id
            references app_user
            on delete cascade,
    -- base64url credential id as reported by the authenticator
    credential_id             varchar(1366) not null
        constraint uk_webauthn_credential_credential_id
            unique,
    -- serialized AttestedCredentialData (AAGUID, credential id, COSE public key)
    attested_credential_data  bytea        not null,
    sign_count                bigint       not null default 0,
    transports                varchar(255),
    backup_eligible           boolean      not null default false,
    backup_state              boolean      not null default false,
    uv_initialized            boolean      not null default false,
    name                      varchar(64)  not null,
    created_at                timestamptz  not null default now(),
    last_used_at              timestamptz
);

create index if not exists idx_webauthn_credential_user_id on webauthn_credential (user_id);

-- Registration, password reset and login links now run through login_attempt. user_token only
-- carries e-mail change confirmations from here on; the new address travels in payload.
delete
from user_token;

alter table user_token
    drop constraint if exists user_token_challenge_outcome_check;
alter table user_token
    drop column if exists challenge_number,
    drop column if exists challenge_attempts,
    drop column if exists challenge_outcome,
    drop column if exists challenge_opened_at;

alter table user_token
    drop constraint if exists user_token_type_check;
alter table user_token
    add constraint user_token_type_check
        check ((type)::text = any ((array ['EMAIL_CHANGE'::character varying])::text[]));

alter table user_token
    add column payload varchar(255);
