-- The mail_outbox.headers column was originally created as jsonb in V6, but MailOutboxEntry maps it
-- via a plain text JPA AttributeConverter (MailHeadersConverter), not Hibernate's JSON type support.
-- Align the column type with the entity mapping.
alter table mail_outbox
    alter column headers type text using headers::text;
