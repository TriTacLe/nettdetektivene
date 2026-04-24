#!/bin/bash

# Deletes all fotografen tasks to allow re-creating them with correct format
# Usage: ./cleanup-fotografen-tasks.sh

MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-root}"
MYSQL_DATABASE="${MYSQL_DATABASE:-nd-db}"

db() { docker exec nd-db mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" -e "$1" 2>/dev/null; }

echo "Cleaning up fotografen tasks..."

# Delete classroom_task associations first (foreign key constraint)
db "DELETE FROM classroom_task WHERE task_id IN (SELECT task_id FROM task WHERE stop_location = 'fotografen')"

# Delete the fotografen tasks
db "DELETE FROM task WHERE stop_location = 'fotografen'"

echo "Done. Fotografen tasks deleted."
echo "Now run: ./setup-classroom.sh"
