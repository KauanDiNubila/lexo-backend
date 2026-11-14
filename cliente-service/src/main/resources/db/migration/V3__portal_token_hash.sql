alter table clients rename column portal_token to portal_token_hash;

update clients
   set portal_token_hash = encode(sha256(convert_to(portal_token_hash, 'UTF8')), 'hex')
 where portal_token_hash is not null;
