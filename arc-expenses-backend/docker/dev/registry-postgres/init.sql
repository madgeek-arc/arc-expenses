CREATE USER vrasidas WITH PASSWORD 'paparia';

CREATE DATABASE registry WITH OWNER vrasidas;

\c registry;

create extension tablefunc;
