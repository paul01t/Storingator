-- Storingator baseline schema.
-- Managed by Flyway; Hibernate runs with ddl-auto=validate against this.
-- Dropping the schema and re-running Fresh Flyway applies this whole file.

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

CREATE TABLE note (
    seq                UUID PRIMARY KEY,
    user_seq           UUID,
    title              VARCHAR(255),
    content            BYTEA,
    nonce              BYTEA,
    encrypt_on_device  BOOLEAN,
    timestamp          VARCHAR(255)
);

-- Foreign keys with ON DELETE CASCADE so removing a user cleans up all data.
ALTER TABLE activeusers
    ADD CONSTRAINT fk_activeusers_user FOREIGN KEY (userseq) REFERENCES appuser (id) ON DELETE CASCADE;

ALTER TABLE logindata
    ADD CONSTRAINT fk_logindata_user FOREIGN KEY (user_seq) REFERENCES appuser (id) ON DELETE CASCADE;

ALTER TABLE note
    ADD CONSTRAINT fk_note_user FOREIGN KEY (user_seq) REFERENCES appuser (id) ON DELETE CASCADE;