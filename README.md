# GroovyService

## Description

## Requisites

* Java 11

### Optional

* Apache Maven

## Development


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
