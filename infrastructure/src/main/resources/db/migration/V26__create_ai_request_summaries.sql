create table ai_request_summaries (
    id              bigint          not null auto_increment,
    request_id      bigint          not null,
    request_version bigint          not null,
    summary         longtext        not null,
    provider        varchar(100)    null,
    model           varchar(100)    null,
    generated_at    datetime(6)     not null,
    constraint pk_ai_request_summaries primary key (id),
    constraint uq_ai_summary_request_version unique (request_id, request_version),
    constraint fk_ai_summary_request foreign key (request_id) references academic_requests (id) on delete cascade
);

create index idx_ai_summary_request_id on ai_request_summaries (request_id);
