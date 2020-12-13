# GroovyService

## Description

GroovyService is a RESTful wrapper around a Docker container that runs the Groovy interpreter with code provided by the user.

Running the code in a container allows simpler management of system resources and handling security: the code submitted by the user(s) runs in a sandbox.

## Requisites

* Docker
* Java 11

### Optional

* Apache Maven

## Development

* Run `GroovysvcApplication` from your IDE or run `./mvnw spring-boot:run`

### Packaging

You can create the JAR file with `./mvnw clean package`

### Usage

Valid example

```
$ curl --no-progress-meter -X POST localhost:8080/api/tasks -H 'Content-type:application/json' -d '{"name": "example", "lang": "groovy", "code": "println(2*3*4)"}' | json_pp

{
   "code" : "println(2*3*4)",
   "createdDate" : "2020-12-12T08:34:09.160+00:00",
   "endDate" : "2020-12-12T08:34:10.995+00:00",
   "exitCode" : 0,
   "id" : 10,
   "lang" : "groovy",
   "name" : "example",
   "startedDate" : "2020-12-12T08:34:09.162+00:00",
   "state" : "COMPLETE",
   "stderr" : "",
   "stdout" : "24"
}
```

Error example

```
curl -X POST localhost:8080/api/tasks -H 'Content-type:application/json' -d '{"name": "invalid task", "lang": "bad-image-name"}'
```
