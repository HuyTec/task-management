ALTER TABLE projects
    ADD COLUMN status VARCHAR(20);

UPDATE projects project
SET status = CASE
    WHEN EXISTS (
        SELECT 1
        FROM tasks task
        WHERE task.project_id = project.id
    )
    AND NOT EXISTS (
        SELECT 1
        FROM tasks task
        WHERE task.project_id = project.id
          AND task.status <> 'DONE'
    ) THEN 'COMPLETED'
    ELSE 'ACTIVE'
END;

ALTER TABLE projects
    ALTER COLUMN status SET DEFAULT 'PLANNING';

ALTER TABLE projects
    ALTER COLUMN status SET NOT NULL;

ALTER TABLE projects
    ADD CONSTRAINT chk_projects_status
    CHECK (status IN ('PLANNING', 'ACTIVE', 'ON_HOLD', 'COMPLETED', 'ARCHIVED'));
