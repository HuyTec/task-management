ALTER TABLE tasks
    ADD COLUMN project_id BIGINT;

ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_project
    FOREIGN KEY (project_id) REFERENCES projects (id)
    ON DELETE SET NULL;

CREATE INDEX idx_tasks_project_id ON tasks (project_id);
