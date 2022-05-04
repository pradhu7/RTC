drop database if exists providers;
create database providers;

DROP TABLE IF EXISTS providers.provider_opps;
CREATE TABLE provider_opps(
  projId varchar(64) NOT NULL,
  code varchar(64) NOT NULL,
  patId varchar(64) NOT NULL,
  pdsId varchar(64) NOT NULL,
  raf decimal(7,3) NOT NULL,
  diseaseCategory varchar(128) NOT NULL,
  decision varchar(64) NOT NULL,
  npi varchar(32) NOT NULL,
  providerGroup varchar(128) NOT NULL,
  providerName varchar(128) NOT NULL,
  isClaimed BOOL NOT NULL,
  reportableRaf decimal(7,3) NOT NULL,
  hierarchyFiltered BOOL NOT NULL,
  deliveryDate INT NULL,
  responseDate INT NULL,
  primary key (projId, code, patId)
);