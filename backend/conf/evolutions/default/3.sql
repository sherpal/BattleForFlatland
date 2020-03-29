-- !Ups

create table if not exists game_tables (
    game_id varchar(255) primary key unique not null,
    game_name varchar(255) not null unique,
    game_hashed_password varchar(255),
    game_creator_id varchar(255) not null references users(user_id) on delete cascade,
    created_on timestamp not null
);


-- !Downs

drop table if exists game_tables;
