create table invoices (
    id varchar(36) not null,
    organization_id varchar(255) not null,
    client_id varchar(255) not null,
    case_id varchar(255),
    description varchar(255) not null,
    amount numeric(12, 2) not null,
    status varchar(255) not null,
    due_date timestamp(6) with time zone not null,
    paid_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    primary key (id)
);

create index idx_invoice_org on invoices (organization_id);
create index idx_invoice_client on invoices (client_id);
create index idx_invoice_due on invoices (due_date);
create index idx_invoice_status on invoices (status);
