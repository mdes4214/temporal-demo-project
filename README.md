# Temporal Demo Project for Practice

Practice and demo project about Temporal.io.

## Temporal.io

References:
* [Temporal.io Java documents](https://docs.temporal.io/docs/java)
* [tmeporalio/docker-compose](https://github.com/temporalio/docker-compose)
* [tsurdilo/temporal-patient-onboarding](https://github.com/tsurdilo/temporal-patient-onboarding)
* [Temporal Introduction and Demo (Youtube)](https://youtu.be/23rX78xqYUg)

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
    3. Approve the order (`POST` `http://localhost:8080/processingOrder/approve`)
    4. Wait for shipping the order
    5. Order processing finished
