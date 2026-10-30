create table event_publication (
    id uuid not null,
    listener_id varchar(512) not null,
    event_type varchar(512) not null,
    serialized_event varchar(4000) not null,
    publication_date timestamptz not null,
    completion_date timestamptz,
    status varchar(20),
    completion_attempts int not null default 0,
    last_resubmission_date timestamptz,

    constraint pk_event_publication primary key (id),
    constraint chk_event_publication_status check ( status in (
        'PUBLISHED', 'PROCESSING', 'COMPLETED', 'FAILED', 'RESUBMITTED'
    ) )
);
create index idx_event_publication_serialized_event_hash on event_publication using hash (serialized_event);
create index idx_event_publication_by_completion_date on event_publication(completion_date);
