-- To create tables:
-- From mysql terminal, run
-- mysql> source ./create_completeness_database.sql;
-- Or from commandline, run
-- $ mysql -uroot -p < create_completeness_database.sql


CREATE DATABASE IF NOT EXISTS completeness;

USE completeness;

CREATE TABLE IF NOT EXISTS patients (
    patient_id VARCHAR(255) NOT NULL,
    status VARCHAR(255),
    update_date TIMESTAMP DEFAULT '1970-01-01 00:00:01',
    indexer_version VARCHAR(255),
    details TEXT,
    PRIMARY KEY (patient_id)
);

CREATE TABLE IF NOT EXISTS documents (
    patient_id VARCHAR(255) NOT NULL,
    document_id VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    update_date TIMESTAMP DEFAULT '1970-01-01 00:00:01',
    indexer_type VARCHAR(255) NOT NULL,
    indexer_version VARCHAR(255) NOT NULL,
    details TEXT,
    PRIMARY KEY (patient_id,document_id)
);

CREATE TABLE IF NOT EXISTS pages (
    patient_id VARCHAR(255) NOT NULL,
    document_id VARCHAR(255) NOT NULL,
    page_number VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    update_date TIMESTAMP DEFAULT '1970-01-01 00:00:01',
    indexer_type VARCHAR(255) NOT NULL,
    indexer_version VARCHAR(255) NOT NULL,
    details TEXT,
    PRIMARY KEY (patient_id,document_id,page_number)
);