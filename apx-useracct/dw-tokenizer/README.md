
The Tokenizer is the dropwizard service that is called over HTTP by
edge servers and will do the following:

* validate external tokens
* convert an external token into a short-lived internal token
* reference count ("use count") the external token


The token to be tested is passed in the normal Authorization HTTP header.


API:
  POST /tokens

  validates the token that's in the Authorization HTTP header and if valid
  creates an internal request token and returns it with a 201 Location
  header

  returns 4xx if invalid external token


API:
  DELETE /tokens/{internal}

  invalidates the given internal token and decrements the use count; external
  token should still be passed in http header for consistency

  returns 200 if deleted, 4xx if bad internal token id


  
Configuration:
  needs redis connectivity info

Code:
  needs shared restbase code

Data Access Requirements
===
* Redis -> Write
* Fluent -> Write
* Graphite -> Write

