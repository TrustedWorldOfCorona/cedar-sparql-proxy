# CEDAR SPARQL proxy
A proof-of-principle that allows SPARQL queries on CEDAR template instances.

## Setup
- Copy or rename `src/main/resourcex/example-application.yml` to `application.yml`
- Paste your CEDAR API key in the `cedar.token` property
- Run `mvn spring-boot:run` in the root of the project
- Execute a query and observe the results

## Example queries
Query a single template instance:
```shell
curl http://localhost:8080/resource\?id=https://repo.metadatacenter.org/template-instances/2b5fbe93-5439-44af-9c20-b77c6336a1e5&q=SELECT%20\?s%20\?p%20\?o%20%7B%20%3Fs%20%3Fp%20%3Fo%20%7D
```
Query a folder with a single template instance:
```shell
curl http://localhost:8080/folder\?id=https://repo.metadatacenter.org/folders/b06136a8-d4fb-4ad6-b939-ed1b37ea829d&q=SELECT%20\?s%20\?p%20\?o%20%7B%20%3Fs%20%3Fp%20%3Fo%20%7D
```
Query a parent folder with subfolders and instances:
```shell
curl http://localhost:8080/folder\?id=https://repo.metadatacenter.org/folders/cc9fbb7e-d15f-48db-a123-d20635877a0f&q=SELECT%20\?s%20\?p%20\?o%20%7B%20%3Fs%20%3Fp%20%3Fo%20%7D
```
