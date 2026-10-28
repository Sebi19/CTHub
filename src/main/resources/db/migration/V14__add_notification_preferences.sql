-- Per-user delivery preferences: which notification types a user wants on which extra channel
-- (e-mail now, Web Push later). In-app notifications are always on and never stored here.
--
-- Only explicit choices are stored. A missing row means "use the type's default", which lives in
-- code (NotificationType#isEnabledByDefault), so defaults can change without a data migration.
--
-- Like notification.type, "type" and "channel" have no CHECK constraint: both are validated by
-- their Java enums and are expected to grow.
create sequence notification_preference_seq
    increment by 50;

create table if not exists notification_preference
(
    id         bigint      not null
        primary key,
    user_id    bigint      not null
        constraint fk_notification_preference_user_id
            references app_user
            on delete cascade,
    type       varchar(64) not null,
    channel    varchar(32) not null,
    enabled    boolean     not null,
    updated_at timestamptz not null default now(),
    constraint uk_notification_preference_user_type_channel
        unique (user_id, type, channel)
);

-- Delivery-time lookup: "which of these recipients want type X on channel Y?"
create index if not exists idx_notification_preference_type_channel
    on notification_preference (type, channel, user_id);
