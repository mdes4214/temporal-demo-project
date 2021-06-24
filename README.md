# Temporal Demo Project for Practice

Practice and demo project about Temporal.io.

## Temporal.io

References:
* [Temporal.io](https://temporal.io/)
* [Temporal.io Java documents](https://docs.temporal.io/docs/java)
* [temporalio/docker-compose](https://github.com/temporalio/docker-compose)
* [tsurdilo/temporal-patient-onboarding](https://github.com/tsurdilo/temporal-patient-onboarding)
* [Temporal Introduction and Demo (YouTube)](https://youtu.be/23rX78xqYUg)
* [temporal-sdk in javadoc.io](https://www.javadoc.io/doc/io.temporal/temporal-sdk/latest/index.html)
* [Temporal community](https://community.temporal.io/)

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
### 1. Workflow and Activities are registered to a Worker instead of directly saving in the Temporal server.

> Reference: [https://docs.temporal.io/docs/server-architecture](https://docs.temporal.io/docs/server-architecture)

![Temporal Architecture](https://docs.temporal.io/assets/images/temporal-high-level-abstracted-relationships-cefbdc8dec2539f22c8a7d8e4e08d6b9.png)

### 2. How Temporal get HA (High Availability)?

> Reference: 
> * [https://docs.temporal.io/docs/server/namespaces](https://docs.temporal.io/docs/server/namespaces)
> * [https://docs.temporal.io/docs/server/configuration/](https://docs.temporal.io/docs/server/configuration/)
> * [https://docs.temporal.io/docs/server/multi-cluster/](https://docs.temporal.io/docs/server/multi-cluster/)
> * [https://community.temporal.io/t/global-domain-registration-in-java-sdk/282](https://community.temporal.io/t/global-domain-registration-in-java-sdk/282)
> 
> Keyword: namespace, cluster

* #### Using Namespace and Cluster configuration

The Temporal Global Namespace feature provides clients with the capability to ***continue their Workflow execution from another cluster in the event of a datacenter failover***. Although you can configure a Global Namespace to be replicated to any number of clusters, it is only considered active in a single cluster.

Client applications need to run workers polling on Activity/Decision tasks on all clusters. ***Temporal will only dispatch tasks on the current active cluster; workers on the standby cluster will sit idle until the Global Namespace is failed over.***

Example of `development.yaml` in the Temporal server:

Cluster A
```yaml
clusterMetadata:
  enableGlobalNamespace: true
  replicationConsumer:
    type: kafka
  failoverVersionIncrement: 10
  masterClusterName: "ClusterA"
  currentClusterName: "ClusterA"
  clusterInformation:
    ClusterA:
      enabled: true
      initialFailoverVersion: 1
      rpcName: "frontend"
      rpcAddress: "localhost:7933"
    ClusterB:
      enabled: true
      initialFailoverVersion: 2
      rpcName: "frontend"
      rpcAddress: "localhost:8933"
kafka:
  clusters:
    test:
      brokers:
        - 127.0.0.1:9092
  topics:
    ClusterA:
      cluster: test
    ClusterA-dlq:
      cluster: test
    ClusterB:
      cluster: test
    ClusterB-dlq:
      cluster: test
  temporal-cluster-topics:
    ClusterA:
      topic: ClusterA
      dlq-topic: ClusterA-dlq
    ClusterB:
      topic: ClusterB
      dlq-topic: ClusterB-dlq

publicClient:
  hostPort: "localhost:7933"
```
Cluster B
```yaml
clusterMetadata:
  enableGlobalNamespace: true
  replicationConsumer:
    type: kafka
  failoverVersionIncrement: 10
  masterClusterName: "ClusterA"
  currentClusterName: "ClusterB"
  clusterInformation:
    ClusterA:
      enabled: true
      initialFailoverVersion: 1
      rpcName: "frontend"
      rpcAddress: "localhost:7933"
    ClusterB:
      enabled: true
      initialFailoverVersion: 2
      rpcName: "frontend"
      rpcAddress: "localhost:8933"

kafka:
  clusters:
    test:
      brokers:
        - 127.0.0.1:9092
  topics:
    ClusterA:
      cluster: test
    ClusterA-dlq:
      cluster: test
    ClusterB:
      cluster: test
    ClusterB-dlq:
      cluster: test
  temporal-cluster-topics:
    ClusterA:
      topic: ClusterA
      dlq-topic: ClusterA-dlq
    ClusterB:
      topic: ClusterB
      dlq-topic: ClusterB-dlq

publicClient:
  hostPort: "localhost:8933"
```

* #### What happens to outstanding Activities after failover?

***Temporal does not forward Activity completions across clusters.*** Any outstanding Activity will eventually timeout based on the configuration. Your application should have ***retry*** logic in place so that the Activity gets retried and dispatched again to a worker after the failover to the new DC. ***Handling this is pretty much the same as Activity timeout caused by a worker restart even without Global Namespaces.***

* #### What happens when a start or signal API call is made to a standby cluster?

Temporal will reject the call and return `NamespaceNotActiveError`. It is the responsibility of the application to forward the failed call to active cluster based on information provided in the error.

* #### What is the recommended pattern to send external events to an active cluster?

The recommendation at this point is to ***publish events to a Kafka topic*** if they can be generated in any cluster. Then, have a consumer that consumes from the aggregated Kafka topic in the same cluster and sends them to Temporal. Both the Kafka consumer and Global Namespace need to be failed over together.

### 3. Scalibility of Temporal

> Reference: [https://docs.temporal.io/blog/workflow-engine-principles/](https://docs.temporal.io/blog/workflow-engine-principles/)
> 
> Keyword: sharding, partitioning, routing, transactional transfer

TODO

### 4. When the Workflow has a new version, how to update the code to keep both running old Workflow and new version?

> Reference: 
> * [https://docs.temporal.io/docs/java/versioning](https://docs.temporal.io/docs/java/versioning)
> * [https://www.javadoc.io/doc/io.temporal/temporal-sdk/latest/io/temporal/workflow/Workflow.html](https://www.javadoc.io/doc/io.temporal/temporal-sdk/latest/io/temporal/workflow/Workflow.html)
> 
> Keyword: versioning, getVersion

TODO

### 5. Side Effect in Activity

> * [https://docs.temporal.io/docs/java/side-effect/](https://docs.temporal.io/docs/java/side-effect/)
> * [https://www.javadoc.io/doc/io.temporal/temporal-sdk/latest/io/temporal/workflow/Workflow.html](https://www.javadoc.io/doc/io.temporal/temporal-sdk/latest/io/temporal/workflow/Workflow.html)
> 
> Keyword: sideEffect

TODO
