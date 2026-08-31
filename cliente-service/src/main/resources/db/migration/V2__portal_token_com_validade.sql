alter table clients add column portal_token_expires_at timestamp(6) with time zone;

update clients
   set portal_token_expires_at = now() + interval '90 days'
 where portal_token is not null;
