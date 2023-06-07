#!/usr/bin/env bash
# Start up a postgres container with initial user/database setup.
MARIADB_VERSION=10.4

start() {
    echo "attempting to remove old $CONTAINER container..."
    docker rm -f $CONTAINER

    # start up postgres
    echo "starting up container...$CONTAINER"
    BASEDIR=$(dirname "$0")
    root_pass="dbpwd"
    docker create --name $CONTAINER --rm \
           -e MARIADB_ROOT_PASSWORD=$root_pass -p "5432:3306" mariadb:$MARIADB_VERSION
    docker cp $BASEDIR/local-mariadb-init.sql $CONTAINER:/docker-entrypoint-initdb.d/docker_mariadb_init.sql
    docker start $CONTAINER

    until [[ $(docker exec -t $CONTAINER mysql -u root -p$root_pass -e 'SELECT 1;' | grep -o -m 1 1) -eq "1" ]]
    do
      # sleep until container starts
      sleep 5
    done
    echo "started container...$CONTAINER"

    # validate mariadb
    echo "running mariadb validation..."
    docker exec $CONTAINER mysql --user dbuser -pdbpwd -e 'USE tanagra_db;SELECT VERSION();SELECT NOW();'
    if [ 0 -eq $? ]; then
        echo "mariadb validation succeeded."
    else
        echo "mariadb validation failed."
        exit 1
    fi
}

stop() {
    echo "Stopping docker $CONTAINER container..."
    docker stop $CONTAINER || echo "$CONTAINER stop failed. container already stopped."
    docker rm -v $CONTAINER
    exit 0
}

CONTAINER=tanagra
COMMAND=$1

if [ ${#@} == 0 ]; then
    echo "Usage: $0 stop|start"
    exit 1
fi

if [ $COMMAND = "start" ]; then
    start
elif [ $COMMAND = "stop" ]; then
    stop
else
    exit 1
fi
