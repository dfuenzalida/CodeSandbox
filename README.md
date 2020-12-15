# GroovyService

## Description

GroovyService is a RESTful wrapper around a Docker container that runs the Groovy interpreter with code submitted by users.

Running the code in a container allows simpler management of system resources and handling security: the code submitted by the user(s) runs in a sandbox, minimizing the risk of compromising the host service and allowing simpler definition of limits to the execution of those tasks.

## Requisites

* Docker
* Java 11 JDK 

Optional

* Apache Maven

## First time setup

To install the Java 11 JDK and Docker in Ubuntu 20.04, you can run the following:

```
$ sudo apt-get install openjdk-11-jdk docker.io
```

To add your current user to the `docker` group:

```
$ sudo adduser $USER docker
```

You'll need to log out and log back in for the new group membership to be reflected.

You can save some time by downloading the `groovy` container image and validating that it runs:

```
$ docker run -it --rm groovy groovy --version
[... output elided ...]
Groovy Version: 3.0.7 JVM: 1.8.0_275 Vendor: AdoptOpenJDK OS: Linux
```

Note that in the output above, the Java version reported is `1.8.0_275`. This is fine because it's the *Java runtime of the container*. The service (GroovyService) will run on a different JDK.


## Development

* In Linux, the user account that will be running the service needs to belong to the `docker` group. You can run `adduser <username> docker` and log out and log back in.
* Run `GroovysvcApplication` from your IDE or run `./mvnw spring-boot:run`
* Once the service is up and running, it will be available on port 8080 by default. You can submit task requests using tools like Postman or cURL, or use a browser-based UI added for convenience by opening http://localhost:8080/

### Packaging

You can create the JAR file with `./mvnw clean package`. This file can be run directly with `java -jar target/groovysvc-0.0.1-SNAPSHOT.jar`

## Design

The service is intentionally simple: a RESTful wrapper around a Task Runner that receives requests containing code and passing the code to executors that will store the code in a temporary file and pass it to docker instances that will run the code.

```
     [Web UI]
        |
        v
  [REST frontend] <---> [Executor Service] <---> [Container]
        |                     |
        v                     |
    [Task DB]<----------------`

```

* The Web UI is mostly a convenience so that you can submit more complex programs through a form so you don't have to squeeze and reformat the code as a JSON string into the request.
  * The Web UI periodically pulls the data for all tasks and refreshes the UI using the `/api/tasks` endpoint.
* The REST frontend performs some basic validation and it either throws errors that are formatted by Spring or returns JSON arrays or objects with the data about the task(s) in the service
* The Task DB an instance of the `JPARepository` interface from Spring and uses an in-memory H2 database.
* The Executor Service receives a Task object and it prepares some files and configuration to call the `docker` executable with a given command and script file. Eventually the Task finishes and the results are captured back into the Task instance and stored in the Task DB.

### Task schema

Tasks are internally represented as objects with the following properties:

* `id` - a numeric identifier of this instance of the task
* `name` - a human-readable name for this task
* `state` - a human-readable identifier of the execution state of this task. Can take one of the following values: `CREATED`, `RUNNING`, `COMPLETE`. Note that tasks fail to run at all (invalid code, exceptions, etc.) will also reach the `COMPLETE` state.
* `lang` - the identifier of the programming language used for the code of this task. Possible values include `groovy`, `perl` and `echo` (which can be used for debugging). `echo` just repeats the name of the temporary file used to store the code.
* `code` - the source code of the task. Ideally, it should be a valid program in the language selected in the `lang` property.
* `stout` - the contents of the *standard output* of the program are copied to this property once the task has fully run.
* `sterr` - the contents of the *standard error* of the program are copied to this property once the task has fully run.
* `exitCode` - the exit code of executing the code is copied to this value. Non-zero values usually signal that the program ended abnormally (runtime exceptions, errors, timeout, etc.)

* `createdDate` - an ISO 8601 timestamp when the task was created in the service
* `startedDate` - an ISO 8601 timestamp when the task started executing in the service
* `endDate` - an ISO 8601 timestamp when the task finished executing in the service

### API endpoints

The following is the list of the APIs implemented in this version of the service. The POST request expects a JSON object in the body of the request, they return JSON responses.


* `GET /api/tasks` - Added mostly as a convenience for the browser-based UI, this API returns an array with all the tasks submitted to the service

* `POST /api/tasks` - Submits a new task request. Expects a JSON object with the following properties:
  * `name` - (Optional) human-readable name for this task (optional)
  * `lang` - (Required) the programming language used to write the code of the task, by default it needs to be one of `groovy`, `echo` or `perl`.
  * `code` - (Required) the actual code submitted for execution. Note that the task runs in an environment where no extra libraries are provided beyond the core libraries of Java, Groovy and Perl.
  
* `GET /api/tasks/<taskId>` - Given a valid `taskId`, returns a single JSON object with the details of the task.

### Configuration

The following parameters in the `application.properties` file can be used to change the behaviour of the service:

* `groovyService.validLangs` - contains a comma-separated list of valid scripts that can be executed in the container. By default it contains `groovy,perl,echo`
* `groovyService.threadPoolSize` - defines the size of the thread pool that can work on tasks (eg. how many containers can run simultaneously). By default it's set to `3`.
* `groovyService.timeoutSecs` - the maximum time (in seconds) that a task can be running before being killed. By default it's set to `10` seconds. Internally it uses the `timeout` command from GNU coreutils, so it also accepts one of the following suffixes: 's' for seconds (the default), 'm' for minutes, 'h' for hours or 'd' for days. NOTE: A duration of `0` disables the timeout.

### Limitations

* The service does not implement any mechanism of **authentication and authorization** - if a request meets the bare minimum validation is queued and (eventually) executed. It is trivial an unlimited amount of "slow" tasks and prevent other users to submit tasks. An enhancement would be to include:
  * Authentication and authorization - only valid users of an organization should be allowed to see *their own* tasks and submit tasks
  * A rate/quota system (complex): for a given principal or account there should be a limit on the number of tasks they can submit per unit of time (eg. "5 tasks per minute") and/or runtime (eg. "100 seconds of runtime per minute, per client")

* **Breaking out the container**: containers are generally safe but it could be possible to [run scripts that run shell commands](https://stackoverflow.com/a/159270/483566) and extract precise version information of the environment and then attempt to recreate [known exploits for Docker](https://cve.mitre.org/cgi-bin/cvekey.cgi?keyword=docker), like the following:

```
def sout = new StringBuilder(), serr = new StringBuilder()
def proc = 'uname -r -v'.execute() // prints the Kernel version
proc.consumeProcessOutput(sout, serr)
proc.waitForOrKill(1000)
println "OUT:$sout\n\nERR:$err"
```

* Running the latest version of the container and using current, patched version of the JDK and Docker helps to prevent this scenario

### Example tasks

The following programs can be used to test the service:

---

A Groovy program that prints the result of `(2*3*4)`:

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

---

You can submit the following Groovy program through the Web UI to validate the timeout feature:

```groovy
println(1)
Thread.sleep(1000)
println(2)
Thread.sleep(1000)
println(3)
Thread.sleep(1000)
println(4)
Thread.sleep(1000)
println(5)
Thread.sleep(1000)
println(6)
Thread.sleep(1000)
println(7)
Thread.sleep(1000)
println(8)
Thread.sleep(1000)
println(9)
Thread.sleep(1000)
println(10)
Thread.sleep(1000)
println(11)
Thread.sleep(1000)
```

When submitted, the program above should NOT execute fully. Only some of the first digits should print but the default timeout of 10 seconds should cause the task to be killed early (note: some time is lost loading the container, the JDK, the Groovy classes and the script), so less than 10 actual seconds are available to run the program.

---

Create 20 tasks that take several seconds to complete (useful to test the size of the task pool). In the Bash script below, each task prints its index, waits 5 seconds, then prints "ok"


```
for i in $(seq 1 20); do curl -X POST localhost:8080/api/tasks -H 'Content-type:application/json' -d "{\"name\": \"task $i\",\"lang\":\"groovy\",\"code\": \"println($i)\nsleep(5000)\nprintln('ok')\"}" ; echo; done
```

---

A simple program that throws an Exception:

```
$ curl --no-progress-meter -X POST localhost:8080/api/tasks -H 'Content-type:application/json' -d '{"name": "example", "lang": "groovy", "code": "println(1/0)"}'
```


---

Error example

```
curl -X POST localhost:8080/api/tasks -H 'Content-type:application/json' -d '{"name": "invalid task", "lang": "bad-image-name"}'
```
---




## Useful link references

* https://stackoverflow.com/questions/48299352/how-to-limit-docker-run-execution-time

* https://stackoverflow.com/a/48299490/483566
