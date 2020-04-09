-- !Ups

create table if not exists users_in_game_tables (
    game_id varchar(255) not null references game_tables(game_id) on delete cascade,
    user_id varchar(255) not null references users(user_id) on delete cascade,
    joined_on timestamp not null
);


-- !Downs

drop table if exists users_in_game_tables;
