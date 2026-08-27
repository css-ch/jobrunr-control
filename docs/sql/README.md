# Database Schema for JobRunr Control

This directory contains SQL DDL scripts for creating and upgrading the tables used by **JobRunr Control**.

## Initial schema

The scripts in this directory create the complete current schema, including:

* `jobrunr_control_parameter_sets` (required only when using the `EXTERNAL` parameter storage strategy)
* `jobrunr_control_batch_recap`
* `jobrunr_control_batch_messages`

### Manual Table Creation

**You must create the required tables manually** before starting your application with external parameter storage enabled.

Choose the appropriate SQL script for your database:

- **PostgreSQL**: `postgresql.sql`
- **Oracle**: `oracle.sql`
- **MySQL/MariaDB**: `mysql.sql`
- **H2**: `h2.sql` (for testing only)

### Upgrade from 2.3.2

When upgrading an existing database from version 2.3.2 to 2.4.0, run the migration script for your database
before starting the application with the new version:

- **PostgreSQL**: `migrations/2.3.2-to-2.4.0/postgresql.sql`
- **Oracle**: `migrations/2.3.2-to-2.4.0/oracle.sql`
- **MySQL/MariaDB**: `migrations/2.3.2-to-2.4.0/mysql.sql`
- **H2**: `migrations/2.3.2-to-2.4.0/h2.sql`

The migration adds the `ATTEMPT_NR` and `IS_LATEST` columns to `jobrunr_control_batch_messages` and creates the
indexes required for retry-aware message queries. The MySQL/MariaDB script is intended to be run once; the other
scripts are safe to run repeatedly.

### Example: PostgreSQL

```bash
psql -U your_user -d your_database -f docs/sql/postgresql.sql
```

### Example: Oracle

```bash
sqlplus your_user/your_password@your_database @docs/sql/oracle.sql
```

### Schema Overview

| Column            | Type         | Description                                       |
|-------------------|--------------|---------------------------------------------------|
| `id`              | VARCHAR(36)  | Unique identifier (UUID)                          |
| `job_type`        | VARCHAR(500) | Fully qualified job class name                    |
| `parameters_json` | JSON/JSONB   | Job parameters stored as JSON                     |
| `created_at`      | TIMESTAMP    | Timestamp when the parameter set was created      |
| `updated_at`      | TIMESTAMP    | Timestamp when the parameter set was last updated |
| `version`         | BIGINT       | Optimistic locking version                        |

### Indexes

The following indexes are created to optimize query performance:

- `idx_param_set_job_type` on `job_type`
- `idx_param_set_created` on `created_at`
- `idx_param_set_updated` on `updated_at`

## Configuration

After creating the table, configure your application:

```properties
# Enable external parameter storage
quarkus.jobrunr-control.parameter-storage.strategy=EXTERNAL
# Configure your database connection (Agroal DataSource)
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/mydb
quarkus.datasource.username=myuser
quarkus.datasource.password=mypassword
```

## Migration from Liquibase

If you previously used Liquibase (version < 1.3.1), the table structure remains the same. You can continue using your
existing table without any schema changes.

**Important**: Remove the following from your `application.properties`:

```properties
# Remove these Liquibase configurations
quarkus.jobrunr-control.liquibase.enabled=true
quarkus.liquibase.migrate-at-start=true
quarkus.liquibase.change-log=db/changeLog.xml
```

## Notes

- The table name is fixed as `jobrunr_control_parameter_sets`. Table prefix support may be added in a future version.
- Ensure your database user has CREATE TABLE permissions when running the DDL scripts.
- For production environments, review and adjust the scripts according to your organization's database standards.
