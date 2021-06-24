# Temporal Demo Project for Practice

Practice and demo project about Temporal.io.

## Temporal.io

References:
* [Temporal.io](https://temporal.io/)
* [Temporal.io Java documents](https://docs.temporal.io/docs/java)
* [temporalio/docker-compose](https://github.com/temporalio/docker-compose)
* [tsurdilo/temporal-patient-onboarding](https://github.com/tsurdilo/temporal-patient-onboarding)
* [Temporal Introduction and Demo (YouTube)](https://youtu.be/23rX78xqYUg)
* [Temporal-sdk in Javadoc.io](https://www.javadoc.io/doc/io.temporal/temporal-sdk/latest/index.html)

## Requirements

1. Docker  
    * [Windows Subsystem for Linux Installation Guide for Windows 10](https://docs.microsoft.com/zh-tw/windows/wsl/install-win10)
    * [Get started with Docker remote containers on WSL 2](https://docs.microsoft.com/zh-tw/windows/wsl/tutorials/wsl-containers)
3. Maven

## Run the Demo

1. Start the Temporal Service (docker compose):

```shell script
 git clone https://github.com/temporalio/docker-compose.git
 cd  docker-compose
 docker-compose up
```

2. Start the Demo Project:

```shell script
git clone https://github.com/mdes4214/temporal-demo-project.git
cd temporal-demo-project
mvn clean install quarkus:dev
```

3. Access the Swagger UI via: [http://localhost:8080/q/swagger-ui/](http://localhost:8080/q/swagger-ui/)

4. Workflow and Trigger Methods  
    1. Trigger the processing and send an order (`POST` `http://localhost:8080/processingOrder`)
    2. Wait for picking goods  
        * if want to simulate exception happended for retry and compensation testing, set `isException=true` (`POST` `http://localhost:8080/processingOrder/simulateException`)
    3. Approve the order (`POST` `http://localhost:8080/processingOrder/approve`)
    4. Wait for shipping the order
    5. Order processing completed

## Note
### Workflow and Activities are registered to a Worker instead of directly saving in the Temporal server.
> Reference: [https://docs.temporal.io/docs/server-architecture](https://docs.temporal.io/docs/server-architecture)

![Temporal Architecture](https://docs.temporal.io/assets/images/temporal-high-level-abstracted-relationships-cefbdc8dec2539f22c8a7d8e4e08d6b9.png)

### How Temporal get HA (High Availability)?
### Scalibility of Temporal
### When the Workflow has a new version, how to update the code to keep both running old Workflow and new version?
### Side Effect in Activity
