-- Number-matching confirmation for cross-device registration.
--
-- When a registration is started, the originating browser is handed a REGISTRATION_STATUS token and
-- shown a two-digit number. The e-mailed link then asks whoever opens it to pick that number out of
-- three. Picking correctly hands the session to the originating browser; the alternatives ("continue
-- here" / "this wasn't me") terminate it.
--
-- These columns describe that challenge and are only ever populated on REGISTRATION_STATUS rows -
-- the row that represents the registration attempt. They stay null for every other token type.
alter table user_token
    add column challenge_number smallint,
    add column challenge_attempts smallint not null default 0,
    add column challenge_outcome varchar(32),
    add column challenge_opened_at timestamptz;

alter table user_token
    add constraint user_token_challenge_outcome_check
        check (challenge_outcome is null or (challenge_outcome)::text = any ((array [
            'CONFIRMED'::character varying,
            'CLAIMED_ELSEWHERE'::character varying,
            'CANCELLED'::character varying
            ])::text[]));
