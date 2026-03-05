# Migration Files Removed

All SQL migration files have been removed from each service. JPA/Hibernate is now configured to auto-create the schema on startup (`ddl-auto=create`).

- To reset the database, simply drop all service databases and restart the application.
- After the first successful run, set `ddl-auto=validate` or `none` for production safety.
- See each service's `db/schema.sql` for the intended schema (for documentation only).
