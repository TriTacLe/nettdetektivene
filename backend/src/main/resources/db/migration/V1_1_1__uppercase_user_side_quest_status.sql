-- Align user_side_quest.status with the Java SideQuestStatus enum (uppercase).
-- The original V1_0_8 migration declared this column with lowercase ENUM values,
-- which caused Hibernate (EnumType.STRING) to throw IllegalArgumentException when
-- reading rows back, since SideQuestStatus.valueOf("active") does not match the
-- uppercase enum constants.

ALTER TABLE user_side_quest
    MODIFY status ENUM('ACTIVE', 'COMPLETED', 'ABANDONED') NOT NULL DEFAULT 'ACTIVE';
