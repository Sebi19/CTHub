-- Push subscriptions belong to a login session, not just to a user: when a device signs out or its
-- session expires/is revoked, it must stop receiving pushes (in particular login approvals).
--
-- Existing subscriptions predate sessions and cannot be attributed to one. Every user is signed out
-- by this release anyway (sessions replace remember-me cookies), so they are dropped; the app
-- re-registers the device's subscription with its new session after the next sign-in.
delete
from push_subscription;

alter table push_subscription
    add column auth_session_id uuid not null
        constraint fk_push_subscription_auth_session_id
            references auth_session
            on delete cascade;

create index if not exists idx_push_subscription_auth_session_id
    on push_subscription (auth_session_id);
