-- Registration is now e-mail-only: the account row is created before the user has supplied a name
-- or chosen a password, so those columns can no longer be NOT NULL. Profile completion is tracked
-- separately and enforced at login time.
alter table app_user
    alter column first_name drop not null;
alter table app_user
    alter column last_name drop not null;
alter table app_user
    alter column password drop not null;

alter table app_user
    add column profile_completed_at timestamptz;

-- Existing accounts (seeded admin and anything registered under the old flow) already have a name
-- and a password, so they count as complete and must not be prompted on their next login.
update app_user
set profile_completed_at = now()
where first_name is not null
  and last_name is not null
  and password is not null;

-- V8 pinned user_token.type to the two token types that existed then. Passwordless login links and
-- the never-e-mailed registration-status handle both need their own types.
alter table user_token
    drop constraint if exists user_token_type_check;
alter table user_token
    add constraint user_token_type_check
        check ((type)::text = any ((array [
            'EMAIL_VERIFICATION'::character varying,
            'PASSWORD_RESET'::character varying,
            'LOGIN_LINK'::character varying,
            'REGISTRATION_STATUS'::character varying
            ])::text[]));
