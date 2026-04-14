FROM mysql:8.0

COPY src/main/resources/db/migration /docker-entrypoint-initdb.d/

VOLUME ["/var/lib/mysql"]

EXPOSE 3306