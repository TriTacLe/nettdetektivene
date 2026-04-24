#!/bin/bash

# Inserts the NTNU institution needed for classroom creation

docker exec nd-db mysql -uroot -proot nd-db \
  -e "INSERT IGNORE INTO institution (institution_id, inst_name) VALUES ('aaaaaaaa-0000-0000-0000-000000000001', 'NTNU');" 2>/dev/null

echo "Done. Institution NTNU seeded."
