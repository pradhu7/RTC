#health-metric
This enables a system to support `healthcheck` endpoint available in a custom http `admin` port.

## Steps to setup `healthcheck`
a. Enable in reference.conf / application.conf
```hocon
app-config {
  metric {
    redis {
      ok       = {max: 500}
      warning  = {min: 501, max: 2000}
      critical = {min: 2001}
    }
    elasticsearch {
      ok       = {max: 2000}
      warning  = {min: 10000, max: 2001}
      critical = {min: 10001}
    }
    indexer {
      ok       = {max: 2000}
      warning  = {min: 10000, max: 2001}
      critical = {min: 10001}
    }
  }

  http_admin {
    enabled = true
    server = "localhost"
    port = 9080
    ssl {
      enable_ssl = false
      keystore_type = "PKCS12" //or jks
      keystore_file = "/path/to/keystore.file"
      keystore_password = "password"
    }
  }
}  

```


a. Enable actor
```scala
  context.actorOf(HealthMetricActor.props(appConfig), HealthMetricActor.actorName)
```

b. Enable http endpoint
```scala
    private val httpAdminBindOption: Option[Future[Http.ServerBinding]] = AdminUtils.initHTTPAdmin(appConfig)
    //...
    
    override def postStop(): Unit = { //actor-shutdown hook.
    ...
    httpAdminBindOption.map { bindFuture =>
      bindFuture.flatMap {_.unbind()}.andThen {
        case _ => log.info("Http admin shutdown complete.")
      }
    }
    super.postStop()
   }
```

Note: 
The current state of this module is not written in library style of usage yet.