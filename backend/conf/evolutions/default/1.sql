-- !Ups

create table if not exists users (
    user_id varchar(255) primary key unique not null,
    user_name varchar(255) not null unique,
    hashed_password varchar(255) not null,
    mail_address varchar(255) not null unique,
    created_on timestamp not null
);

create table if not exists roles (
    role_id varchar(255) primary key unique not null,
    role_name varchar(255) not null
);

create table if not exists cross_users_roles (
    user_id varchar(255) references users(user_id) on delete cascade,
    role_id varchar(255) references roles(role_id)
);

-- !Downs


drop table if exists cross_users_roles;
drop table if exists users;
drop table if exists roles;

