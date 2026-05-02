# Nettdetektivene – Database Documentation

## Overview
This document describes the database schema, including:
- Purpose of each table
- Columns with datatypes
- Foreign key relationships

The schema uses UUIDs (`CHAR(36)`) for domain primary keys. Authentication support tables keep `BIGINT UNSIGNED AUTO_INCREMENT` for their own primary keys, while `app_user.user_id` is a UUID.

---

## AUTH

### app_user
Stores the core user account for every person in the system.

| Column | Type |
|---|---|
| user_id | CHAR(36) |
| display_name | VARCHAR(255) |
| email | VARCHAR(255) |
| is_active | TINYINT(1) |
| global_last_seen_at | DATETIME |
| created_at | DATETIME |
| updated_at | DATETIME |

**Foreign keys:** None

---

### app_user_local_credential
Stores password-based login information for users who authenticate locally.

| Column | Type |
|---|---|
| credential_id | BIGINT UNSIGNED |
| user_id | CHAR(36) |
| password_hash | VARCHAR(255) |
| must_change_pw | TINYINT(1) |
| last_changed_at | DATETIME |
| failed_attempts | TINYINT UNSIGNED |
| locked_until | DATETIME |
| created_at | DATETIME |
| updated_at | DATETIME |

**Foreign keys:**
- `user_id` → `app_user.user_id`

---

### app_user_refresh_token
Stores refresh tokens used for session renewal.

| Column | Type |
|---|---|
| token_id | BIGINT UNSIGNED |
| user_id | CHAR(36) |
| token_hash | CHAR(64) |
| issued_at | DATETIME |
| expires_at | DATETIME |
| revoked_at | DATETIME |
| ip_address | VARBINARY(16) |

**Foreign keys:**
- `user_id` → `app_user.user_id`

---

### app_user_identity
Stores linked third-party identities such as SSO providers.

| Column | Type |
|---|---|
| identity_id | BIGINT UNSIGNED |
| user_id | CHAR(36) |
| provider_name | VARCHAR(50) |
| provider_user_id | VARCHAR(255) |
| provider_email | VARCHAR(255) |
| created_at | DATETIME |

**Foreign keys:**
- `user_id` → `app_user.user_id`

---

## ROLES

### role
Defines the available system roles.

| Column | Type |
|---|---|
| role_id | CHAR(36) |
| role_name | VARCHAR(50) |

**Foreign keys:** None

---

### user_role
Maps users to one or more roles.

| Column | Type |
|---|---|
| user_id | CHAR(36) |
| role_id | CHAR(36) |

**Foreign keys:**
- `user_id` → `app_user.user_id`
- `role_id` → `role.role_id`

---

## PROFILE

### user_profile
Stores game-specific profile data for a user, separate from authentication.

| Column | Type |
|---|---|
| profile_id | CHAR(36) |
| display_name | VARCHAR(255) |
| user_id | CHAR(36) |
| feide_id | VARCHAR(255) |
| avatar_config | JSON |
| xp | INT UNSIGNED |
| xp_level | TINYINT UNSIGNED |
| created_at | DATETIME |
| updated_at | DATETIME |

**Foreign keys:**
- `user_id` → `app_user.user_id`

---

## INSTITUTION

### institution
Represents an institution such as a school.

| Column | Type |
|---|---|
| institution_id | CHAR(36) |
| inst_name | VARCHAR(255) |
| created_at | DATETIME |
| updated_at | DATETIME |

**Foreign keys:** None

---

## CLASSROOM

### classroom
Represents a classroom belonging to an institution.

| Column | Type |
|---|---|
| classroom_id | CHAR(36) |
| institution_id | CHAR(36) |
| title | VARCHAR(255) |
| class_desc | TEXT |
| join_code | VARCHAR(20) |
| created_by | CHAR(36) |
| is_game_enabled | TINYINT(1) |
| deleted_at | DATETIME |
| created_at | DATETIME |
| updated_at | DATETIME |

**Foreign keys:**
- `institution_id` → `institution.institution_id`
- `created_by` → `app_user.user_id`

---

### classroom_instructor
Links instructors to classrooms.

| Column | Type |
|---|---|
| classroom_id | CHAR(36) |
| user_id | CHAR(36) |
| assigned_at | DATETIME |

**Foreign keys:**
- `classroom_id` → `classroom.classroom_id`
- `user_id` → `app_user.user_id`

---

### classroom_student
Links students to classrooms and stores their approved in-class display name.

A student can only belong to one classroom at a time because `user_id` is unique in this table. If a student wants to change their display name after joining a classroom, the requested name is stored separately and must be reviewed by a teacher before it replaces the approved `display_name`.

| Column | Type |
|---|---|
| classroom_id | CHAR(36) |
| user_id | CHAR(36) |
| display_name | VARCHAR(255) |
| is_allowed | TINYINT(1) |
| requested_display_name | VARCHAR(255) |
| name_change_status | ENUM('none', 'pending', 'approved', 'rejected') |
| name_change_requested_at | DATETIME |
| name_change_reviewed_by | CHAR(36) |
| name_change_reviewed_at | DATETIME |
| joined_at | DATETIME |

**Foreign keys:**
- `classroom_id` → `classroom.classroom_id`
- `user_id` → `app_user.user_id`
- `name_change_reviewed_by` → `app_user.user_id`

---

## TASKS

### task
Stores both built-in system tasks and teacher-created tasks.

| Column | Type |
|---|---|
| task_id | CHAR(36) |
| title | VARCHAR(255) |
| task_type | ENUM('news', 'photo', 'email', 'password', 'marketplace', 'social', 'boss') |
| stop_location | VARCHAR(100) |
| difficulty | TINYINT UNSIGNED |
| content | JSON |
| owner_id | CHAR(36) |
| is_active | TINYINT(1) |
| created_at | DATETIME |
| updated_at | DATETIME |

**Foreign keys:**
- `owner_id` → `app_user.user_id`

---

### classroom_task
Assigns tasks to a specific classroom.

| Column | Type |
|---|---|
| classroom_id | CHAR(36) |
| task_id | CHAR(36) |
| sort_order | SMALLINT UNSIGNED |
| is_active | TINYINT(1) |

**Foreign keys:**
- `classroom_id` → `classroom.classroom_id`
- `task_id` → `task.task_id`

---

## STUDENT PROGRESS

### task_attempt
Stores the full history of every task attempt.

| Column | Type |
|---|---|
| attempt_id | CHAR(36) |
| user_id | CHAR(36) |
| task_id | CHAR(36) |
| classroom_id | CHAR(36) |
| score | SMALLINT UNSIGNED |
| passed | TINYINT(1) |
| attempted_at | DATETIME |

**Foreign keys:**
- `user_id` → `app_user.user_id`
- `task_id` → `task.task_id`
- `classroom_id` → `classroom.classroom_id`

---

### task_best_score
Stores the best recorded result for each user-task-classroom combination.

| Column | Type |
|---|---|
| user_id | CHAR(36) |
| task_id | CHAR(36) |
| classroom_id | CHAR(36) |
| best_score | SMALLINT UNSIGNED |
| total_attempts | SMALLINT UNSIGNED |
| first_passed_at | DATETIME |
| last_attempted_at | DATETIME |

**Foreign keys:**
- `user_id` → `app_user.user_id`
- `task_id` → `task.task_id`
- `classroom_id` → `classroom.classroom_id`

---

## ACHIEVEMENTS

### achievement
Defines achievements, badges, or medals that can be earned.

| Column | Type |
|---|---|
| achievement_id | CHAR(36) |
| title | VARCHAR(255) |
| desc_ach | TEXT |
| icon | TEXT |
| trigger_type | VARCHAR(50) |
| trigger_ref | VARCHAR(100) |
| created_at | DATETIME |

**Foreign keys:** None

---

### user_achievement
Links achievements to users and optionally to a classroom context.

| Column | Type |
|---|---|
| user_id | CHAR(36) |
| achievement_id | CHAR(36) |
| classroom_id | CHAR(36) |
| earned_at | DATETIME |

**Foreign keys:**
- `user_id` → `app_user.user_id`
- `achievement_id` → `achievement.achievement_id`
- `classroom_id` → `classroom.classroom_id`

---

## NOTEBOOK

### notebook_entry
Stores one notebook entry per user, classroom, and stop location.

| Column | Type |
|---|---|
| entry_id | CHAR(36) |
| user_id | CHAR(36) |
| classroom_id | CHAR(36) |
| stop_location | VARCHAR(100) |
| auto_notes | JSON |
| student_reflection | TEXT |
| created_at | DATETIME |
| updated_at | DATETIME |

**Foreign keys:**
- `user_id` → `app_user.user_id`
- `classroom_id` → `classroom.classroom_id`

---

## UKENS MYSTERIUM

### mystery_submission
Stores mystery submissions made by students and reviewed by teachers.

| Column | Type |
|---|---|
| submission_id | CHAR(36) |
| classroom_id | CHAR(36) |
| submitted_by | CHAR(36) |
| mys_desc | TEXT |
| attachment_url | VARCHAR(500) |
| mys_status | ENUM('pending', 'approved', 'rejected') |
| reviewed_by | CHAR(36) |
| reviewed_at | DATETIME |
| is_featured | TINYINT(1) |
| week_number | TINYINT UNSIGNED |
| week_year | SMALLINT UNSIGNED |
| submitted_at | DATETIME |

**Foreign keys:**
- `classroom_id` → `classroom.classroom_id`
- `submitted_by` → `app_user.user_id`
- `reviewed_by` → `app_user.user_id`

---

## NOTIFICATIONS

### notification
Stores notifications sent to users, typically instructors.

| Column | Type |
|---|---|
| notification_id | CHAR(36) |
| recipient_id | CHAR(36) |
| classroom_id | CHAR(36) |
| msg | VARCHAR(500) |
| ref_type | VARCHAR(50) |
| ref_id | CHAR(36) |
| is_read | TINYINT(1) |
| created_at | DATETIME |

**Foreign keys:**
- `recipient_id` → `app_user.user_id`
- `classroom_id` → `classroom.classroom_id`

---

## Notes
- `classroom_student.user_id` is unique, which enforces that a student can only join one classroom.
- Name changes after joining are modeled as a request-and-approval flow through `requested_display_name`, `name_change_status`, and the review columns.
- JSON columns are used where the content is flexible, such as avatar configuration, task content, and notebook auto-notes.
- ENUM columns are used where the valid values are fixed and controlled by the application.
