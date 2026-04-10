-- Secure Dokument Service – H2 schema (dev profile)
-- Används när spring.jpa.hibernate.ddl-auto=none

CREATE TABLE IF NOT EXISTS arenden (
    id              UUID         NOT NULL,
    organisation_id VARCHAR(255) NOT NULL,
    title           VARCHAR(255) NOT NULL,
    description     VARCHAR(2000),
    status          VARCHAR(20)  NOT NULL,
    created_by      VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP    NOT NULL,
    CONSTRAINT pk_arenden PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS dokument (
    id              UUID         NOT NULL,
    organisation_id VARCHAR(255) NOT NULL,
    title           VARCHAR(255) NOT NULL,
    description     VARCHAR(2000),
    classification  VARCHAR(20)  NOT NULL,
    arende_id       UUID,
    created_by      VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP    NOT NULL,
    updated_by      VARCHAR(255),
    updated_at      TIMESTAMP,
    CONSTRAINT pk_dokuments PRIMARY KEY (id),
    CONSTRAINT fk_dokuments_arende FOREIGN KEY (arende_id) REFERENCES arenden (id)
);
