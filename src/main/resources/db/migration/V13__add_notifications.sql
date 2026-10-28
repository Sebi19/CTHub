-- In-app notifications: one row per recipient, the source of truth for every delivery channel
-- (live SSE now, e-mail and Web Push later).
--
-- Rows store the notification type plus its parameters rather than rendered text, so each channel
-- renders in the recipient's current language.
--
-- "type" deliberately has no CHECK constraint (unlike user_token.type): notification types are
-- expected to grow often, and they are validated by the NotificationType enum instead.
create sequence notification_seq
    increment by 50;

create table if not exists notification
(
    id          bigint        not null
        primary key,
    user_id     bigint        not null
        constraint fk_notification_user_id
            references app_user
            on delete cascade,
    type        varchar(64)   not null,
    params      text,
    link        varchar(2048),
    -- Only for one-off events (e.g. "results published for competition X"). Unique per recipient and
    -- permanent, so never derive it from something that legitimately repeats.
    dedupe_key  varchar(255),
    created_at  timestamptz   not null default now(),
    read_at     timestamptz,
    constraint uk_notification_user_id_dedupe_key
        unique (user_id, dedupe_key)
);

-- Keyset pagination of a user's notifications, newest first.
create index if not exists idx_notification_user_id_id on notification (user_id, id desc);

-- Unread badge count.
create index if not exists idx_notification_user_id_unread on notification (user_id) where read_at is null;

-- Retention cleanup of old read notifications.
create index if not exists idx_notification_read_at on notification (read_at) where read_at is not null;
