# APIXIO protobuf based log schemas

The objective here is to provide a mechanism by which our programmers can create logs that go
out over the fluent logging bus in a controlled way that preserves schema so that tools like
drill, spark etc that flourish when there are schemas have a controlled data type to work with

The use of protobuf is intended to be to create logs and not necessarily to receive logs. The
intention of the system is to use the protobuf to create the log object and then convert it to
json which is then sent over the fluent bus, once it is on that bus, we expect it to remain json
and for our logging infrastructure to pick up from there and for receivers not to need to reconstruct
the object on the other side unless the processor is a hand built application. To support that
we place log type information in the logs so that the correct selection of the protobuf parser
is possible

## Conventions:

1. Log field names are in snake case. When a log is converted to json, the preserve field name option
should be used to assure the snake case is preserved

2. Log structure of any log type is as follows:

```
  message SomeComonponentLog {
    LogHeader         header             // non-optional
    Network           network            // non-optional, has optional fields 
    Performance       performance        // optional
    User              user               // optional based on log type (non-optional for API for example)
    ComponentLogType  log_type           // non-optional
    oneof payload {                      // non-optional
      Logtype1    type1
      Logtype2    type2
      etc...
      }
  }
``` 
>Note that in protobuf all fields are by default optional, we have denoted which fields should be considered 
optional


## Log types
* AppLog --- any internal measure or observation we want to make on an app. The loglevel can be set by the creator as needed
* API Log --- all HTTP calls must be logged and the API Log format requires all of the log structure and should be set as an event as this is something that has occurred in out system and we want to preserve that 
* EventLog --- data / event related to your application that should be preseved. Events are preserved as real data in our system and as such can be used downstream as a backup of your transaction data or a means by which to prototype a feature or provide operational management features without effecting other domain models


