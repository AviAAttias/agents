CREATE SCHEMA IF NOT EXISTS shared;

-- H2 compatibility (PostgreSQL mode) so unqualified CREATE TABLE lands in shared
SET SCHEMA shared;
