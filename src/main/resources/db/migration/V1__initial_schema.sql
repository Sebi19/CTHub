-- Enable the Trigram extension for similarity checks
CREATE EXTENSION IF NOT EXISTS pg_trgm;

create sequence comp_seq
    increment by 50;

create sequence nom_seq
    increment by 50;

create sequence place_seq
    increment by 50;

create sequence profile_seq
    increment by 50;

create sequence rg_seq
    increment by 50;

create sequence team_seq
    increment by 50;

create sequence user_seq
    increment by 50;

create table if not exists season
(
    id            varchar(255) not null
        primary key,
    active        boolean      not null,
    name          varchar(255) not null,
    overview_hash varchar(255),
    start_year    integer      not null
        constraint uk_season_start_year
            unique,
    max_points    integer
);

create table if not exists competition
(
    id                     bigint       not null
        primary key,
    active                 boolean      not null,
    awards_hash            varchar(255),
    contact_email          varchar(255),
    contact_name           text,
    country                varchar(255),
    date                   date,
    detail_hash            varchar(255),
    location               text,
    max_team_count         integer      not null,
    name                   varchar(255) not null,
    qualification_url_part varchar(255),
    registered_team_count  integer      not null,
    results_available      boolean      not null,
    results_url_part       varchar(255),
    robot_game_hash        varchar(255),
    type                   varchar(255) not null
        constraint competition_type_check
            check ((type)::text = ANY
                ((ARRAY ['REGIONAL'::character varying, 'QUALIFICATION'::character varying, 'FINAL'::character varying])::text[])),
    url_part               varchar(255) not null,
    season_id              varchar(255) not null
        constraint fk_competition_season_id
            references season,
    end_date               date,
    constraint uk_competition_season_id_url_part
        unique (season_id, url_part)
);

create table if not exists competition_links
(
    competition_id bigint       not null
        constraint fk_competition_links_competition_id
            references competition,
    label          varchar(255),
    url            varchar(255) not null
);

create table if not exists season_team
(
    id              bigint       not null
        primary key,
    active          boolean      not null,
    city            varchar(255),
    country         varchar(255),
    fll_id          varchar(255) not null,
    institution     varchar(255),
    name            varchar(255) not null,
    season_id       varchar(255) not null
        constraint fk_season_team_season_id
            references season,
    team_profile_id bigint,
    constraint uk_season_team_season_id_fll_id
        unique (season_id, fll_id)
);

create table if not exists nomination
(
    id              bigint       not null
        primary key,
    category        varchar(255) not null
        constraint nomination_category_check
            check ((category)::text = ANY
                ((ARRAY ['CHAMPION'::character varying, 'RESEARCH'::character varying, 'CORE_VALUES'::character varying, 'ROBOT_DESIGN'::character varying, 'COACHING'::character varying, 'ROBOT_GAME'::character varying])::text[])),
    is_award_winner boolean      not null,
    competition_id  bigint       not null
        constraint fk_nomination_competition_id
            references competition,
    season_team_id  bigint       not null
        constraint fk_nomination_season_team_id
            references season_team
);

create table if not exists place
(
    id             bigint  not null
        primary key,
    advancing      boolean not null,
    place          integer not null,
    competition_id bigint  not null
        constraint fk_place_competition_id
            references competition,
    season_team_id bigint  not null
        constraint fk_place_season_team_id
            references season_team
);

create table if not exists robot_game_result
(
    id             bigint  not null
        primary key,
    best_pr        integer not null,
    f1             integer,
    f2             integer,
    pr1            integer not null,
    pr2            integer not null,
    pr3            integer not null,
    qf             integer,
    r16            integer,
    rank           integer not null,
    sf             integer,
    competition_id bigint  not null
        constraint fk_robot_game_result_competition_id
            references competition,
    season_team_id bigint  not null
        constraint fk_robot_game_result_season_team_id
            references season_team,
    prelim_rank    integer,
    constraint uk_robot_game_result_competition_id_season_team_id
        unique (season_team_id, competition_id)
);

create table if not exists season_team_links
(
    season_team_id bigint       not null
        constraint fk_season_team_links_season_team_id
            references season_team,
    label          varchar(255),
    url            varchar(255) not null
);

create table if not exists season_team_registered_competitions
(
    season_team_id             bigint not null
        constraint fk_season_team_registered_competitions_season_team_id
            references season_team,
    registered_competitions_id bigint not null
        constraint fk_season_team_registered_competitions_competitions_id
            references competition,
    primary key (season_team_id, registered_competitions_id)
);

create table if not exists app_user
(
    id       bigint       not null
        primary key,
    email    varchar(255) not null
        constraint uk_app_user_email
            unique,
    password varchar(255) not null,
    role     varchar(255)
        constraint app_user_role_check
            check ((role)::text = ANY ((ARRAY ['USER'::character varying, 'ADMIN'::character varying])::text[]))
);

create table if not exists image
(
    id                uuid         not null
        primary key,
    content_type      varchar(255) not null,
    data              oid,
    original_filename varchar(255),
    size_bytes        bigint
);

create table if not exists team_profile
(
    id                 bigint       not null
        primary key,
    custom_url         varchar(255) not null
        constraint uk_team_profile_custom_url
            unique,
    display_name       varchar(255) not null,
    profile_image_uuid uuid
);

create table if not exists season_team_profile
(
    season_team_id   bigint       not null
        primary key
        constraint fk_season_team_profile_season_team_id
            references season_team,
    avatar_mode      varchar(255) not null
        constraint season_team_profile_avatar_mode_check
            check ((avatar_mode)::text = ANY
                ((ARRAY ['INHERIT'::character varying, 'HIDE'::character varying, 'CUSTOM'::character varying])::text[])),
    custom_avatar_id uuid,
    team_profile_id  bigint       not null
        constraint fk_season_team_profile_team_profile_id
            references team_profile
);
