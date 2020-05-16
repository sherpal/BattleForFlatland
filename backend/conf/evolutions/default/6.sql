-- !Ups

alter table game_tables add column game_config_str text;

-- !Downs

alter table game_tables drop column game_config_str;