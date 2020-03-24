-- !Ups

create table if not exists pending_registrations (
    registration_key varchar(255) primary key unique not null,
    user_name varchar(255) not null unique,
    hashed_password varchar(255) not null,
    mail_address varchar(255) not null unique,
    created_on timestamp not null
);


-- !Downs


drop table if exists pending_registrations;

