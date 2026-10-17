\getenv link_management_password LINK_MANAGEMENT_DB_PASSWORD
\getenv redirect_password REDIRECT_DB_PASSWORD
\getenv analytics_password ANALYTICS_DB_PASSWORD

CREATE ROLE link_management LOGIN PASSWORD :'link_management_password';
CREATE ROLE redirect LOGIN PASSWORD :'redirect_password';
CREATE ROLE analytics LOGIN PASSWORD :'analytics_password';

CREATE DATABASE link_management_db OWNER link_management;
CREATE DATABASE redirect_db OWNER redirect;
CREATE DATABASE analytics_db OWNER analytics;

REVOKE CONNECT ON DATABASE link_management_db FROM PUBLIC;
REVOKE CONNECT ON DATABASE redirect_db FROM PUBLIC;
REVOKE CONNECT ON DATABASE analytics_db FROM PUBLIC;
