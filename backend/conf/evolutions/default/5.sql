-- !Ups

create table if not exists game_credentials (
    game_id varchar(255) not null unique references game_tables(game_id) on delete cascade,
    game_secret varchar(255) not null
);

create table if not exists game_user_credentials (
    user_id varchar(255) not null,
    game_id varchar(255) not null references game_credentials(game_id) on delete cascade,
    user_secret varchar(255) not null
);


-- !Downs

drop table if exists game_credentials;
drop table if exists game_user_credentials;
