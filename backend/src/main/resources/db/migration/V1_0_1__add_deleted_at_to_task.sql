-- Add deleted_at column to task table for soft delete functionality
-- Issue #275: Task deletion must be soft delete, not hard delete
-- This prevents cascading deletions that destroy historical student attempt data

ALTER TABLE task
ADD COLUMN deleted_at DATETIME NULL
COMMENT 'Timestamp for soft delete - task is active if NULL, deleted if set';

CREATE INDEX idx_task_deleted_at ON task(deleted_at);