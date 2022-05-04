user-account
============


# ACL Data Model

Access Control List structures/objects include only those that are directly related
to determining whether or not a user ("subject") can access some object.  The overall
user account system contains more objects than those just for ACL (e.g., tokens, users,
etc.).

ACL structures are kept in both Redis and Cassandra.  The structures kept in Redis are
the definitions of the AccessType and Operations objects, while the structures kept
in Cassandra are the UserGroups and actual permissions granted.

The reason for this split is that the AccessType and Operations are mostly static and
it is not anticipated that a large number of either one will needed, nor will complicated
lookup be required.  On the other hand, there could be a large number of permissions,
and we know that more complicted lookups are required for permissions.

## Redis Structures

There are two ACL objects kept in Redis:

* AccessTypes
* Operations

Both of these use the existing Redis object infrastructure first created for the Coordinator.
Both of these also have the same general needs regarding storage and access:

* store a uniquely identified set of name=value fields
* be able to enumerate all instances of the object type (AccessType or Operation)
* be able to locate by (external) name a particular instance
* be able to locate by ID a particular instance

The general pattern to support the above is to have a UUID-like key as the Redis key
to hold onto the fields of the instance, and to use two type-specific keys to support
the enumeration-of-all and lookup-by-name operations.

Specifically, for AccessTypes, the Redis keys/structures used are:

* key `{prefix} {XUUID}` with a HASH as the Redis value; records the fields of the AccessType
* key `{prefix} access-x-all` with a LIST as the Redis value; list elements are the XUUID of
  all the AccessType instances
* key `{prefix} access-x-byname` with a HASH as the Reids value; hash keys are the AccessType
  name and the hash value is the XUUID of the AccessType object

(where `{prefix}` is the configured Redis key value in the .yaml files).

For Operations, the Redis keys/structures used are:

* key `{prefix} {XUUID}` with a HASH as the Redis value; records the fields of the AccessType
* key `{prefix} oper-x-all` with a LIST as the Redis value; list elements are the XUUID of
  all the AccessType instances
* key `{prefix} oper-x-byname` with a HASH as the Reids value; hash keys are the AccessType
  name and the hash value is the XUUID of the AccessType object

Some example keys:

* stagingauth-O_d22281a1-0f2d-47b8-88be-31da5b4495de (value type is HASH)
* stagingauth-oper-x-byname (value type is HASH)
* stagingauth-oper-x-all (value type is LIST)
* stagingauth-A_4dead805-bb6c-4d27-93da-a499986411b1 (value type is HASH)
* stagingauth-access-x-byname (value type is HASH)
* stagingauth-access-x-all (value type is LIST)


## Cassandra Structures

The following structures are kept in Cassandra:

* UserGroups
* Permission information

### UserGroups

The UserGroup data model maintains a by-group and by-member view of the relationships, and it
allows both enumeration of all groups and lookup of a group by name.

The CqlCrud class exposes a simple rowkey/column-name/column-value model (basically the pre-CQL
Cassandra data model) that works well for the ACL needs.  There are four rowkeys used for UserGroups:

* to maintain the list of all groups:  rowkey="grplist", col-name={GroupID}, col-value={JSONObject}
* to support lookups by group name:  rowkey="bygrpname", col-name={CanonicalName}, col-value={GroupID}
* to support listing members of a group:  rowkey="bygrp."{GroupID}, col-name={UserID}, col-value="x"
* to support listing groups a User is a member of:  rowkey="bymem."{UserID}, col-name={GroupID}, col-value="x"

The JSONObject is the JSON representation of the Java UserGroup object.  Quoted strings are string literals,
while "{}" identifiers are replaced with actual runtime values.

As an example, if there is a single UserGroup with the name "My group" and GroupID "theGroup", with a single user with ID "theUser" (which,
is an invalid format as a real UserID would have a UUID in it), the following rowkeys/columns would be kept
in Cassandra (using twisted notational syntax...):

* rowkey="grplist"; column1:{"theGroup"="{...json fields...}"}
* rowkey="bygrpname"; column1:{"my group"="theGroup"}
* rowkey="bygrp.theGroup"; column1:{"theUser"="x"}
* rowkey="bymem.theUser"; column1:{"theGroup"="x"}

### Permission Information

The actual ACL permission information is a 3-way denormalization of the data for faster lookup.  It uses
5 rowkeys to be able to quickly respond with yes/no to the question of, "Does Subject x have permission to
perform Operation p on Object o?"

The rowkeys used are:

* rowkey={Subject}:{Operation}; col-name={Object}, col-value="true"
* rowkey={Subject}:{Object}; col-name={Operation}, col-value="true"
* rowkey={Object}:{Operation}; col-name={Subject}, col-value="true"
* rowkey="objCache"; col-name={Object}, col-value={lastUpdatedTime}
* rowkey="allsubs:"{Operation}; col-name={Subject}, col-value="x"

The first three rowkeys record the different ways of looking up Subject/Operation/Object using
two of them at a time to get the third.

The fourth rowkey is used to invalidate "by Object" caches.

The fifth rowkey is used to support a delete of an Operation, where we need to know what Subjects
were given rights to perform that Operation that is being deleted.

As an example, assume two Operations "Read" and "Write", two Subjects "Jill" and "Jack", and two
Objects "BluePill" and "RedPill".  If Jill can Read the BluePill and the RedPill, while Jack can
Read and Write only the RedPill, the rowkeys and columns would be (using a similar twisted syntax):

* rowkey=Jill:Read; column1:{BluePill=x}, column2:{RedPill=x}
* rowkey=Jill:BluePill; column1:{Read=x}
* rowkey=Jill:RedPill; column1:{Read=x}
* rowkey=RedPill:Read; column1:{Jill=x}
* rowkey=BluePill:Read; column1:{Jill=x}
* rowkey=Jack:Read; column1:{RedPill=x}
* rowkey=Jack:Write; column1:{RedPill=x}
* rowkey=Jack:RedPill; column1:{Read=x}, column2:{Write=x}
* rowkey=RedPill:Read; column1:{Jack=x}
* rowkey=RedPill:Write; column1:{Jack=x}
* rowkey=allsubs:Read; column1:{Jill=x}, column2:{Jack=x}
* rowkey=allsubs:Write; column1:{Jack=x}
* rowkey=objCache; column1:{RedPill=1234}, column2={BluePill=1235}

Data Access Requirements
===
Redis -> Write
Fluent -> Write
Graphite -> Write

### Handle bouncycastle error

If you try to run locally and see the bouncycastle error (ex. `Exception in thread "main" com.apixio.security.exception.ApixioSecurityException: JCE cannot authenticate the provider BC`)
please refer to `https://sites.google.com/a/apixio.com/engineering/dev-environment/bouncycastle-provider` to debug