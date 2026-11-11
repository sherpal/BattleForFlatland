-- The games of the menus that are not over, kept by the `open-menu-games` projection (see `MenuGames`), so
-- that they are loaded again when the server restarts.
create table open_menu_game (
   game_id integer not null primary key
);
