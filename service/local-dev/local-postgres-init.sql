-- Initial SQL commands to initialize a local postgres instance with for development.
CREATE DATABASE tanagra_db;
CREATE ROLE dbuser WITH LOGIN ENCRYPTED PASSWORD 'dbpwd';