# Use the official MySQL 8 image as the base
FROM mysql:8.0

# Set the environment variables.
ENV MYSQL_ROOT_PASSWORD=admin
ENV MYSQL_DATABASE=BeatsDB
ENV MYSQL_USER=root
ENV MYSQL_PASSWORD=admin

# Copy the init.sql script to the docker-entrypoint-initdb.d directory
# COPY init.sql /docker-entrypoint-initdb.d/

# Expose the MySQL port
EXPOSE 3306
