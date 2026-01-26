# TODO List

* [ ] Format date and datetime in the parameters correctly
* [x] Using typesafe qute templates

# Completion

* [ ] Clean up code
* [ ] Create unit and integration tests
* [ ] Write user documentation

# Create Demo Project

* [ ] Security integration with JobRunr Dashboard

# Documentation

## Implementation Guide

* [ ] Simple Jobs
* [ ] Batch Jobs
* [ | Parameters, Types, Default Values, Allowed Types
* [ ] Success and Failure Callbacks
* [ ] Using Rest API (Run Jobs Externally, Clone Jobs, Get Progress)
* [ ] Only Configuration Jobs are Discovered

## User Guide

* [ ] Configuring Jobs
* [ ] Actions on Scheduling Jobs
* [ ] Monitoring Jobs
* [ ] Deep Links,
* [ ] Rerun failed child jobs in the Dashboard

## Arc42

### Important Points

* It does only support JobRunr Pro
* JobRunr Control ist just a quarkus extension that provides a UI and REST API to manage JobRunr jobs
* It only uses the JobRunr Persistence, not other Persistence is used (e.g. for storing job) definitions)
* JobRunr Control Extension use the JobrRunr Pro java API and not the database directliy
* The extension is built using Hexagonal Architecture (Clean Architecture) principles
* The extension is modularized into domain, application, infrastructure, adapter layers
* The extension provides role-based access control for security (viewer, configurator, admin)
* It only supports Quarkus framework at the moment
* It only supports the JobRequest/JobRequestHandler pattern
* It uses Jackson for serialization and deserialization of job parameters
* The Configurable Job Configuration is collected at build time using Quarkus build steps
* It is not tested for quarkus native mode
* Do not go into details, only high level architecture and important decisions
* Only docuemnt the classes/interfaces/annotation that are used by the users of the extension, all other classes should
  be considered internal and not part of the public API