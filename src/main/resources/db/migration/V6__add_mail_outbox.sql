create sequence mail_outbox_seq
    increment by 50;

create table if not exists mail_outbox
(
    id                   bigint        not null
        primary key,
    recipient_email      varchar(255)  not null,
    recipient_name       varchar(255),
    subject              varchar(998)  not null,
    html_body            text          not null,
    text_body            text          not null,
    template             varchar(255)  not null,
    category             varchar(255)  not null
        constraint mail_outbox_category_check
            check ((category)::text = ANY
                ((ARRAY ['TRANSACTIONAL'::character varying, 'NOTIFICATION'::character varying, 'DIGEST'::character varying])::text[])),
    locale               varchar(16)   not null,
    reply_to             varchar(255),
    cc                   text,
    bcc                  text,
    headers              jsonb,
    status               varchar(32)   not null
        constraint mail_outbox_status_check
            check ((status)::text = ANY
                ((ARRAY ['PENDING'::character varying, 'SENDING'::character varying, 'SENT'::character varying, 'FAILED'::character varying, 'CANCELLED'::character varying])::text[])),
    attempts             integer       not null default 0,
    next_attempt_at      timestamptz   not null,
    last_error           text,
    provider             varchar(64),
    provider_message_id  varchar(255),
    dedupe_key           varchar(255)
        constraint uk_mail_outbox_dedupe_key
            unique,
    created_at           timestamptz   not null default now(),
    updated_at           timestamptz   not null default now(),
    sent_at              timestamptz
);

create index if not exists idx_mail_outbox_status_next_attempt on mail_outbox (status, next_attempt_at);
