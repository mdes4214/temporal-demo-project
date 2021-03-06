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
        * if want to simulate exception happened for retry and compensation testing, set `isException=true` (`POST` `http://localhost:8080/processingOrder/simulateException`)
    3. Approve the order (`POST` `http://localhost:8080/processingOrder/approve`)
    4. Wait for shipping the order
    5. Order processing completed

## Note
### Table of Contents
1. [Workflow and Activities are registered to a Worker instead of directly saving in the Temporal server.](#worker)
2. [How does Temporal get HA (High Availability)?](#HA)
3. [Scalability of Temporal](#scalability)
4. [When the Workflow has a new version, how to update the code to keep both running old Workflow and new version?](#version)
5. [Side Effect in Workflow](#sideEffect)

### 1. Workflow and Activities are registered to a Worker instead of directly saving in the Temporal server. <a name="worker"></a>

> Reference: 
> * [https://docs.temporal.io/docs/concepts/workers](https://docs.temporal.io/docs/concepts/workers)
> * [https://docs.temporal.io/docs/content/what-is-a-temporal-cluster](https://docs.temporal.io/docs/content/what-is-a-temporal-cluster)

![Temporal Architecture](img/temporal_architecture.png)

### 2. How does Temporal get HA (High Availability)? <a name="HA"></a>

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

### 3. Scalability of Temporal <a name="scalability"></a>

> Reference: [https://docs.temporal.io/blog/workflow-engine-principles/](https://docs.temporal.io/blog/workflow-engine-principles/)
> 
> Keyword: sharding, partitioning, routing, transfer queue

* #### Workflow as unit of scalability

For Temporal's target usecases, we decided not to design for scaling up a single workflow instance. Every workflow should be limited in size, but we can infinitely scale out the number of workflows.
    
So if you need to run a million tasks, don't implement them all inside a single workflow, ***have a single workflow that creates a thousand child workflows, each of which run a thousand tasks.*** This way, each of the instances will be bounded.

* #### Multiple Hosts

Once we can assume each instance has a limited size, we can start ***distributing them across multiple machines***. Scaling out a fleet of machines becomes practical because each instance is guaranteed to fit within a single machine.

* #### Multiple Stores

If you want to have a very, very large system, you need to scale out the database as well. A single database instance will be a bottleneck.

![Sharding](img/scalability_1.png)

* #### The Task Queue Problem

For example if you have activities which listen on a task queue named `foo`, how do you get activity tasks waiting to be executed? If you store those activity tasks in every shard, you need to go to all shards and ask if they have anything for task queue `foo`.

If moving the queue into its a separate component with its own persistence, the problem above will be solved. But as soon as queues live outside of core shards of workflow state, we don't have transactions across them anymore.

The way we solved it in Temporal is using ***Transfer Queues***. The idea is that ***every shard which stores workflow state also stores a queue***. 10,000 shards, 10,000 queues. Every time we make an update to a shard we can also make an update to the queue because it lives in the same partition.

![Transfer Queue](img/scalability_2.png)

So if we need to start a workflow, we:
1. create a state for that workflow
2. create workflow tasks for the worker to pick up
3. add the task to the local queue of that shard
4. This will be committed to the database atomically
5. a thread pulls from that queue and transfers that message to the queuing subsystem.

![Transfer Queue add task](img/scalability_3.png)

### 4. When the Workflow has a new version, how to update the code to keep both running old Workflow and new version? <a name="version"></a>

#### 4-1. Versioning

> Reference: 
> * [https://docs.temporal.io/docs/java/versioning](https://docs.temporal.io/docs/java/versioning)
> * [https://www.javadoc.io/doc/io.temporal/temporal-sdk/latest/io/temporal/workflow/Workflow.html](https://www.javadoc.io/doc/io.temporal/temporal-sdk/latest/io/temporal/workflow/Workflow.html)
> 
> Keyword: versioning, getVersion

`Workflow.getVersion` is used to safely perform backwards incompatible changes to workflow definitions. It is not allowed to update workflow code while there are workflows running as it is going to break determinism. The solution is to have both old code that is used to ***replay*** existing workflows as well as the new one that is used when it is executed for the first time.

```java
public static int getVersion(java.lang.String changeId,
                             int minSupported,
                             int maxSupported)
```
* `getVersion` returns `maxSupported` version when is executed for the first time. This version is ***recorded into the workflow history as a marker event***.
* Even if `maxSupported` version is changed, ***the version that was recorded is returned on replay***.
* `Workflow.DEFAULT_VERSION` constant contains version of code that wasn't versioned before.

The backwards compatible way to execute the update is
```java
 int version = Workflow.getVersion("fooChange", Workflow.DEFAULT_VERSION, 1);
 String result;
 if (version == Workflow.DEFAULT_VERSION) {
   result = testActivities.activity1();
 } else {
   result = testActivities.activity2();
 }
```
 
Then later if we want to have another change:
```java
 int version = Workflow.getVersion("fooChange", Workflow.DEFAULT_VERSION, 2);
 String result;
 if (version == Workflow.DEFAULT_VERSION) {
   result = testActivities.activity1();
 } else if (version == 1) {
   result = testActivities.activity2();
 } else {
   result = testActivities.activity3();
 }
```
 
Later when there are no workflow executions running DefaultVersion the correspondent branch can be removed:
``` java
 int version = Workflow.getVersion("fooChange", 1, 2);
 String result;
 if (version == 1) {
   result = testActivities.activity2();
 } else {
   result = testActivities.activity3();
 }
```
 
It is recommended to keep the GetVersion() call even if single branch is left:
``` java
 Workflow.getVersion("fooChange", 2, 2);
 result = testActivities.activity3();
```

#### 4-2. How to make executed and ongoing workflows won't hit the new code, but all new workflow will hit?

> Reference: 
> * [https://community.temporal.io/t/potential-impact-of-workflow-update/1760](https://community.temporal.io/t/potential-impact-of-workflow-update/1760)

**Condition:** We have a workflow version `V0` and an executed and ongoing task `R0`. Due to the business logic adjustment, we need to modify the last activity of the workflow. This make a new workflow version `V1`. Now, though `R0` hasn't arrived the last activity, we still expect that `R0` will complete the `V0` workflow instead of `V1`. And we also expect that all the new tasks (`R1`, `R2`, ...) will choose the codepath `V1`.

> Note that if simply use `4-1` versioning method, because the adjusted activity stage will be executed by `R0` ***first time*** (first time call the `Workflow.getVersion()`) after the workflow updated from `V0` to `V1`, the `R0` will choose `V1` codepath instead of continueing `V0`.

**Solution:** We should add a `Workflow.getVersion()` at the beginning  of the whole workflow, to make sure every task will get a version before running the activities. This is like a workaround  because that in one task, the same changeId will always return the same version, and this can let the `R0` choose `V0` in the adjusted activity stage.

We need to add `Workflow.getVersion()` even though the workflow doesn't have any change.
```kotlin
// workflow implement
override fun processOrder(order: Order): Order {
   // we need to add this at the beginning of the whole workflow
   val versionWorkflow = Workflow.getVersion("workflowChange", Workflow.DEFAULT_VERSION, Workflow.DEFAULT_VERSION) 

   logger.info("Start processing the order $order... [$versionWorkflow]") 
   // versionWorkflow = -1

   ... // some activities

}
```
After updating the last activity, we will update the workflow version number. **And all the changes will use the changeId same as the beginning of workflow.**
```kotlin
// workflow implement
override fun processOrder(order: Order): Order {
   val versionWorkflow = Workflow.getVersion("workflowChange", Workflow.DEFAULT_VERSION, 1)

   logger.info("Start processing the order $order... [$versionWorkflow]") 
   // in R0: versionWorkflow = -1
   // in R1: versionWorkflow = 1

   ... // some activities

   // the activity stage which need to be updated
   val versionUpdate = Workflow.getVersion("workflowChange", Workflow.DEFAULT_VERSION, 1)
   // in R0: versionUpdate = -1
   // in R1: versionUpdate = 1
   // due to the same changeId = "workflowChange", versionUpdate always be same as versionWorkflow
   
   if (versionUpdate != Workflow.DEFAULT_VERSION) {
       logger.info("choose the new codepath only if the workflow is running after version updated")
       // new codepath TODO
   }
}
```

### 5. Side Effect in Workflow <a name="sideEffect"></a>

> Reference: 
> * [https://docs.temporal.io/docs/java/side-effect/](https://docs.temporal.io/docs/java/side-effect/)
> * [https://www.javadoc.io/doc/io.temporal/temporal-sdk/latest/io/temporal/workflow/Workflow.html](https://www.javadoc.io/doc/io.temporal/temporal-sdk/latest/io/temporal/workflow/Workflow.html)
> 
> Keyword: sideEffect

Workflow code must be ***deterministic***. This is important so Temporal can ***replay*** your Workflow to the point of failure and continue its execution.

Workflow code that includes arbitrary side effects (for example getting a random number or generating a random UUID, etc), can ***cause unpredictable results during replay***.

Being able to add some non-deterministic code inside your Workflow is in some cases important, and you can do that using `Workflow.sideEffect`.

```java
// implementation of the @WorkflowMethod
public void execute() {
    int randomInt = Workflow.sideEffect( int.class, () -> {
        Random random = new SecureRandom();
        return random.nextInt();
    });

    String userHome = Workflow.sideEffect(String.class, () -> System.getenv("USER_HOME"));

    if(randomInt % 2 == 0) {
        // ...
    } else {
        // ...
    }
}
```

The result of `Workflow.sideEffect` is ***recorded into the Workflow history, meaning that during a replay it will be returned from the history without executing its code again.***

Note that you ***shouldn't modify the Workflow state inside `Workflow.sideEffect`***. For that you should only use the `Workflow.sideEffect` return value.
