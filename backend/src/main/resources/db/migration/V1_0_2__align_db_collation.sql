-- Align the database default collation with the column collation.
-- Without this, MySQL 8's default utf8mb4_0900_ai_ci is used for every
-- new connection (including the one Flyway creates triggers/SPs from),
-- and DECLAREd CHAR locals inside routines end up with a collation
-- different from the columns they are compared against.

ALTER DATABASE `nettdetektivene` CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
