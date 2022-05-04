# consul-service
This service provides 

a. discovering system services (during the start of the microservice)

b. refreshing running list of application services

## discovering system services

a. Update reference.conf

```HOCON
app-config {
  system_endpoints = ["elasticsearch", "redis", "cql", "cqlInternal", "cqlApplication"]
  consul {
        enable_discovery = true
        server = "10.1.4.214"
        port = 8500
        dc = "stg"
        token = "id"
  }
  elasticsearch {
    consul.resolve_by_consul = true
    consul.service.name = "elasticsearch"
    consul.service.node = "elasticsearch-stg1" //Optional 'service.node' if not provided the system would use the first one from consul.
  }

  kafka {
    consul.resolve_by_consul = false

    server = "10.1.16.178"
    port = 9092
    topic = "test1"
    consumer_group = "group1"
  }
}
```
Note: For the above config, the system would generate `elasticsearch.server` & `elasticsearch.port` from consul. It may also optionally generate `elasticsearch.servers = []` and `elasticsearch.ports = []` if consul provides additional entries for a given `service.name` entry.

b. Add the following to the main thread

```scala
    val endpointFromConsul = {
      ConsulSystemEndpointDiscoveryUtils.discoverSystemEndpoints(appConfig)
    }
    implicit val resolvedAppConfig: Config = endpointFromConsul.withFallback(appConfig).resolve()
```

## discovering application endpoints


```HOCON
app-config {
  application_endpoints = []
  consul {
        enable_discovery = true
        server = "10.1.4.214"
        port = 8500
        dc = "stg"
        token = "id"
  }
}
```
b. Add the following to the child of user actor.

```scala
  if (appConfig.getConfig("consul").getBoolean("enable_discovery")) {
    context.actorOf(ConsulActor.props(appConfig, self), ConsulActor.actorName)
  }
```

Note: The service is not currently in library style API yet.
