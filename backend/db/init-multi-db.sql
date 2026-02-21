SELECT 'CREATE DATABASE personal_tracker_auth'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'personal_tracker_auth')\gexec

SELECT 'CREATE DATABASE personal_tracker_task'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'personal_tracker_task')\gexec
