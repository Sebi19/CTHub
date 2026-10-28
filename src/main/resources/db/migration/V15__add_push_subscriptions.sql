-- Web Push subscriptions: one row per browser/device that agreed to receive push notifications.
--
-- The endpoint is a capability URL issued by the browser's push service and identifies the device.
-- It is unique: when a different user signs in on the same browser and enables push, the row moves
-- to that user instead of delivering to both. Rows are deleted when the push service reports the
-- subscription as gone (404/410), when the user removes the device, or with the user (cascade).
create sequence push_subscription_seq
    increment by 50;

create table if not exists push_subscription
(
    id           bigint       not null
        primary key,
    user_id      bigint       not null
        constraint fk_push_subscription_user_id
            references app_user
            on delete cascade,
    endpoint     text         not null
        constraint uk_push_subscription_endpoint
            unique,
    p256dh       varchar(128) not null,
    auth         varchar(64)  not null,
    user_agent   varchar(512),
    installed    boolean      not null default false,
    created_at   timestamptz  not null default now(),
    last_seen_at timestamptz  not null default now()
);

create index if not exists idx_push_subscription_user_id
    on push_subscription (user_id);
