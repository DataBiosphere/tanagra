-- Initial SQL commands to initialize a local postgres instance with for development.
CREATE DATABASE tanagra;
CREATE ROLE dbuser WITH LOGIN ENCRYPTED PASSWORD 'dbpwd';
