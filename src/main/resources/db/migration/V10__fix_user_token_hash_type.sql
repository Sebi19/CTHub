-- The user_token.token_hash column was created as char(64) in V8, but the entity maps it with
-- length = 64 which Hibernate validates as varchar(64). Align the column type with the entity mapping.
alter table user_token
    alter column token_hash type varchar(64) using token_hash::varchar(64);
