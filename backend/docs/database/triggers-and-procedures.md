# Nettdetektivene – Triggers & Stored Procedures Documentation

## Overview

This document describes all triggers and stored procedures, including:

* Purpose
* Behavior
* Side-effects
* Important constraints

---

## TRIGGERS

---

### trg_classroom_auto_instructor

**Table:** `classroom`
**Timing:** AFTER INSERT

**Purpose:**
Ensures that every classroom automatically has at least one instructor — the creator.

**Behavior:**

* When a new classroom is created, the `created_by` user is automatically inserted into `classroom_instructor`.

**Side-effects:**

* Prevents orphan classrooms without instructors
* Removes need for application-level double insert

---

### trg_attempt_sync

**Table:** `task_attempt`
**Timing:** AFTER INSERT

**Purpose:**
Keeps `task_best_score` and `user_profile` in sync with gameplay.

---

#### Behavior

1. **Upserts `task_best_score`:**

    * Creates row if none exists
    * Updates existing row:

        * `best_score` → max(previous, new)
        * `total_attempts` → incremented
        * `first_passed_at` → set only once
        * `last_attempted_at` → always updated

2. **Awards XP (only once per task):**

    * Only when:

        * `passed = 1`
        * AND task has never been passed before
    * XP added: **10 per task**

3. **Updates level (`xp_level`):**

| XP Range | Level |
| -------- | ----- |
| 0–49     | 1     |
| 50–149   | 2     |
| 150–299  | 3     |
| 300–499  | 4     |
| 500+     | 5     |

---

#### Side-effects

* Prevents XP farming
* Keeps leaderboard/cache table consistent
* Eliminates need for manual recalculation logic

---

### trg_mystery_approved

**Table:** `mystery_submission`
**Timing:** AFTER UPDATE

**Purpose:**
Handles approval logic for weekly mystery submissions.

---

#### Behavior

Triggered only when:

```sql
NEW.mys_status = 'approved' AND OLD.mys_status <> 'approved'
```

---

#### Side-effects

1. **Creates notification**

    * Sent to student (`submitted_by`)
    * Message stored in `notification.msg`

2. **Awards achievement**

    * Looks up:

      ```sql
      trigger_type = 'ukens_mysterium'
      ```
    * Inserts into `user_achievement` (idempotent via `INSERT IGNORE`)

---

## STORED PROCEDURES

---

### sp_submit_attempt

**Purpose:**
Single entry point for recording a task attempt.

---

#### Parameters

| Name           | Type     | Description |
| -------------- | -------- | ----------- |
| p_attempt_id   | CHAR(36) | UUID        |
| p_user_id      | CHAR(36) | Student     |
| p_task_id      | CHAR(36) | Task        |
| p_classroom_id | CHAR(36) | Classroom   |
| p_score        | SMALLINT | Score       |
| p_passed       | TINYINT  | 1 = passed  |

---

#### Behavior

1. Checks:

    * `classroom.is_game_enabled = 1`
    * Throws error if disabled

2. Inserts into `task_attempt`

3. Trigger (`trg_attempt_sync`) automatically:

    * Updates best score
    * Awards XP

4. Returns:

    * Updated `task_best_score`
    * Current `xp` and `xp_level`

---

#### Transaction

* Wrapped in explicit transaction
* Ensures atomicity with trigger side-effects

---

### sp_request_display_name_change

**Purpose:**
Allows a student to request a new display name.

---

#### Parameters

| Name                     | Type         |
| ------------------------ | ------------ |
| p_user_id                | CHAR(36)     |
| p_requested_display_name | VARCHAR(255) |

---

#### Validation

Fails if:

* Student not in classroom
* Name is empty
* Name equals current name

---

#### Behavior

Updates `classroom_student`:

| Field                    | Value     |
| ------------------------ | --------- |
| requested_display_name   | new value |
| name_change_status       | 'pending' |
| name_change_requested_at | NOW()     |
| name_change_reviewed_*   | NULL      |

---

#### Result

Returns updated row.

---

### sp_review_display_name_change

**Purpose:**
Allows a teacher to approve/reject name changes.

---

#### Parameters

| Name              | Type        |
| ----------------- | ----------- |
| p_teacher_user_id | CHAR(36)    |
| p_student_user_id | CHAR(36)    |
| p_decision        | VARCHAR(20) |

---

#### Validation

Fails if:

* Student not in classroom
* No pending request
* Teacher not assigned to classroom
* Invalid decision

---

#### Behavior

##### If approved:

* `display_name ← requested_display_name`
* `name_change_status = 'approved'`

##### If rejected:

* `display_name unchanged`
* `name_change_status = 'rejected'`

##### Always:

* Sets:

    * `name_change_reviewed_by`
    * `name_change_reviewed_at`

---

#### Result

Returns updated row.

---

## Design Notes

* Triggers handle **automatic consistency**
* Stored procedures handle **business logic and validation**
* Separation ensures:

    * DB integrity is enforced centrally
    * Application logic remains thin
* All UUIDs use `CHAR(36)` consistently
