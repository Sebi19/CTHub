-- Number-matching login/verification attempts (replaces the registration-only challenge on user_token).
--
-- One row per "prove it's you" attempt started from a browser ("browser A"). Browser A is bound to it
-- by a random nonce in an HttpOnly cookie (only its hash is stored). The attempt is approved either
-- from a push notification on another signed-in device, or via the e-mailed link (whose token hash is
-- stored separately and only issued when the mail is actually sent).
--
-- user_id stays null for addresses without an account: the account is only created once the attempt
-- is approved, i.e. once the mailbox owner proved control.
--
-- "Expired" is not a stored status; it is derived from expires_at.
create table if not exists login_attempt
(
    id                     uuid         not null
        primary key,
    purpose                varchar(16)  not null
        constraint login_attempt_purpose_check
            check ((purpose)::text = any ((array ['LOGIN'::character varying, 'STEP_UP'::character varying])::text[])),
    email                  varchar(255) not null,
    user_id                bigint
        constraint fk_login_attempt_user_id
            references app_user
            on delete cascade,
    step_up_session_id     uuid
        constraint fk_login_attempt_step_up_session_id
            references auth_session
            on delete cascade,
    nonce_hash             varchar(64)  not null
        constraint uk_login_attempt_nonce_hash
            unique,
    link_token_hash        varchar(64)
        constraint uk_login_attempt_link_token_hash
            unique,
    challenge_number       smallint     not null,
    status                 varchar(32)  not null
        constraint login_attempt_status_check
            check ((status)::text = any ((array [
                'PENDING'::character varying,
                'APPROVED'::character varying,
                'COMPLETED'::character varying,
                'CLAIMED_ELSEWHERE'::character varying,
                'REJECTED'::character varying,
                'CANCELLED'::character varying
                ])::text[])),
    locale                 varchar(16),
    initiator_user_agent   varchar(512),
    initiator_ip           varchar(64),
    -- Push endpoint of the initiating browser itself; never sent the approval push.
    excluded_push_endpoint text,
    pushed_at              timestamptz,
    push_opened_at         timestamptz,
    email_due_at           timestamptz,
    email_sent_at          timestamptz,
    link_opened_at         timestamptz,
    created_at             timestamptz  not null default now(),
    expires_at             timestamptz  not null,
    resolved_at            timestamptz,
    resolved_via           varchar(16)
        constraint login_attempt_resolved_via_check
            check (resolved_via is null or (resolved_via)::text = any ((array [
                'SAME_BROWSER'::character varying,
                'EMAIL'::character varying,
                'PUSH'::character varying,
                'CLAIMED_HERE'::character varying
                ])::text[])),
    resolved_by_session_id uuid
        constraint fk_login_attempt_resolved_by_session_id
            references auth_session
            on delete set null
);

create index if not exists idx_login_attempt_email_due
    on login_attempt (email_due_at)
    where status = 'PENDING' and email_sent_at is null;
create index if not exists idx_login_attempt_user_id on login_attempt (user_id);
create index if not exists idx_login_attempt_expires_at on login_attempt (expires_at);
