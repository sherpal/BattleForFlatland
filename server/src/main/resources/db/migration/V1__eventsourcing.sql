-- This is sqlite, times are encoded as integer with second precision

create table raw_event_envelope(
   offset           integer not null primary key autoincrement, -- global, gapless-per-insert log position, used by projections
   entity_id        integer not null,
   sequence_number  integer not null,
   event_payload    text not null,
   entity_kind      text not null,
   timestamp        integer not null
);

create unique index envelope_index on raw_event_envelope (entity_id, entity_kind, sequence_number);

-- Tracks how far each registered projection has read through raw_event_envelope, by offset.
create table projection_checkpoint (
   projection_name text not null primary key,
   last_offset     integer not null,
   updated_at      integer not null
);
