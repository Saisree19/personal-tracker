CREATE TABLE IF NOT EXISTS tasks (
    id UUID PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    application VARCHAR(100),
    complexity VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    closed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    archived_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_tasks_user_closed ON tasks(user_id, closed_at);
CREATE INDEX IF NOT EXISTS idx_tasks_application ON tasks(application);
