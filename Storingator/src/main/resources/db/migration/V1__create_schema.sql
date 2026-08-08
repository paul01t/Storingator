-- Storingator baseline schema.
-- Managed by Flyway; Hibernate runs with ddl-auto=validate against this.

CREATE TABLE appuser (
    id         UUID PRIMARY KEY,
    username   VARCHAR(255) UNIQUE,
    email      VARCHAR(255) UNIQUE,
    password   VARCHAR(255),
    timestamp  VARCHAR(255),
    vault_salt BYTEA
);

CREATE TABLE activeusers (
    seq       UUID PRIMARY KEY,
    userseq   UUID,
    username  VARCHAR(255),
    loginkey  VARCHAR(255) UNIQUE,
    startdate TIMESTAMP
);

CREATE TABLE logindata (
    seq                UUID PRIMARY KEY,
    user_seq           UUID,
    platform           VARCHAR(255),
    username           VARCHAR(255),
    email              VARCHAR(255),
    password           BYTEA,
    nonce              BYTEA,
    extra_info         VARCHAR(255),
    encrypt_on_device  BOOLEAN
);