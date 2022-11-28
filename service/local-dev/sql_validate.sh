#!/bin/bash

# validate the local postgres is alive
echo "sleeping for 5 seconds during postgres boot..."
sleep 5
# User and password as set in local-postgres-init.sql
PGPASSWORD=dbpwd psql --username dbuser -d tanagra_db -c "SELECT VERSION();SELECT NOW()"
