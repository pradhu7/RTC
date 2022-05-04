# infra-config

How to start the infra-config application
---

1. Run `mvn clean install` to build your application
1. Start application with `java -jar target/infra-config-1.0-SNAPSHOT.jar server config.yml`
1. To check that your application is running enter url `http://localhost:8080`

Models
---

## SftpUserModel

This is an object that represents a single sftp user. Contains information about username, password, ssh key, and any
other relevant information

| name | description | optional (read/write/yes/no) |
|---|---|---|
| Username | the username that is used to connect | write (taken from the path) |
| Password | the password that is used to connect. On GET this will be a argon2 hash. On PUT this will be the raw password. The password is not storted inside of vault and cannot be recovered. | write (will be generated if missing) |
| PublicKey | a public key associated with the account for authentication | yes |
| PrivateKey | a private key associated with the account for authentication. On PUT this will return the raw private key. On GET this field will be missing | yes |
| SFTPServerId | the name of the sftp instance this user is associated to | write (taken from path) |
| PDS | a list of PDS's that are associated with this user | yes |
| AcceptedIpNetwork | what network CIDR's are allowed to use this user | no |
| Group | What Apixio org or internal group the user is associated with | no |
| HomeDirectoryDetails | Information about the chroot point for the user | write (can not be set) |
| HomeDirectoryType | Information about the type of home directory. used internally by transfer family | write (can not be set) |
| Initialized | If the s3 directories have been initalized | no |

### generating ssh keys

To generate an ssh key for an account a PUT may be done to the user endpoint (with all relevant fields) with
the `privateKey` field set to `generate <key format> <key size>`. If a PATCH request is done then you only need to
specify the `PrivateKey` field with the relevant

It is recommended to use PATCH whenever updating a single parameter sa it does not require the entire document be sent
as a payload. When doing a PUT it will attempt to merge what is in vault with the request but you may get unexpected
results.

The following key formats and sizes are currently supported

| Key Type| Key Sizes|
|---|---|
| ssh-rsa | 2048, 4096, 8192 |
| ecdsa-sha2-nistp384 | 384 |
| ecdsa-sha2-nistp521 | 521 |

To generate an RSA key with a size of 2048 you would specify `generate ssh-rsa 2048` in the PrivateKey field. The
private key will be returned in the response object of the PUT under the `PrivateKey` field.

It is recommended to use an ECDSA key where possible.

### generating a password

#### PATCH

To generate an password for an account using PATCH make sure the `Password` field is set to empty, null, or absent and
specify `generatePassword` as a query parameter set to `true`. No other fields are required and an empty json document
can also be sent for this. The password will be returned in the response object of the PATCH request under
the `Password` field.

You can also use this PATCH to set the password to a specific value. When doing so you may leave out
the `generatePassword` parameter and instead set the `Password` field in the payload to what you would like the password
set to.

It is recommended to use PATCH whenever updating a single parameter sa it does not require the entire document be sent
as a payload. When doing a PUT it will attempt to merge what is in vault with the request but you may get unexpected
results.

#### PUT

To generate a new password for an existing account a PUT may be done to the user endpoint (with all relevant fields)
with the `Password` field set to an empty string, null, or absent and the query parameter `resetPassword` set to `true`.
The password will be returned in the response object of the PUT request under the `Password` field.

You can also use PUT to set the password to a specific value. When doing so make sure the `resetPassword` parameter is
set to `true` and instead set the `Password` field in the payload to what you would like the password set to.

It is recommended to use PATCH whenever updating a single parameter sa it does not require the entire document be sent
as a payload. When doing a PUT it will attempt to merge what is in vault with the request but you may get unexpected
results.

### Initalizing S3

By default when a user is created or updated using POST or PUT an empty marker is writtent to s3 to initialize the user
directories. These directories are `USER/to_apixio` and `USER/from_apixio`. This functionality can be disabled by
setting the query parameter `initializeS3=false`.

On PATCH this is not done by default but can be done by setting `initializeS3=true` as a query parameter.

### JSON Schema

```json
{
  "type": "object",
  "additionalProperties": false,
  "properties": {
    "Username": {
      "type": "string"
    },
    "Password": {
      "type": "string"
    },
    "PublicKey": {
      "type": "string"
    },
    "PrivateKey": {
      "type": "string"
    },
    "SFTPServerId": {
      "type": "string"
    },
    "PDS": {
      "type": "array",
      "items": {
        "type": "string"
      }
    },
    "AcceptedIpNetwork": {
      "type": "array",
      "items": {
        "type": "string"
      }
    },
    "Group": {
      "type": "string"
    },
    "HomeDirectoryDetails": {
      "type": "string"
    },
    "HomeDirectoryType": {
      "type": "string"
    },
    "Initialized": {
      "type": "boolean"
    }
  }
}
```

## SftpServerConfig

| name | description | 
|---|---|
| region | what AWS region the SFTP instance the server is associated to | 
| hostname | the DNS or IP address to use for connecting to the server |
| port | the port to use for connecting |
| securityGroupId | The security group to be managed/updated for allowing access to the SFTP instance |
| awsRole | an optional AWS role that has privileges to modify the security group. this role will be assumed by the SFTP instance when modifying the security group |
| s3Bucket | the sftp bucket that this sftp server is mapped to |

### JSON Schema

```json
{
  "type": "object",
  "additionalProperties": false,
  "properties": {
    "region": {
      "type": "string"
    },
    "hostname": {
      "type": "string"
    },
    "port": {
      "type": "integer"
    },
    "securityGroupId": {
      "type": "string"
    },
    "awsRole": {
      "type": "string"
    },
    "s3Bucket": {
      "type": "string"
    }
  }
}
```

REST endpoints
---

| endpoint | description | return model | Supported Methods |
|---|---|---|---|
| `/api/v1/sftp` | Returns all SFTP server instance configurations. The key will be the ID of the instance | `Map<String, SftpServerConfig>`| `GET` |
| `/api/v1/sftp/{sftpServer}` | Returns information about a specific sftp server | `SftpServerConfig` | `GET` |
| `/api/v1/sftp/{sftpServer}/users` | Returns list of SftpUserModel objects representing all the users able to authenticate | `List<SftpUserModel>` | `GET` |
| `/api/v1/sftp/{sftpServer}/users/{sftpUser}` | Endpoint for manging or getting information about a specific user. Used to create, update, and delete users as well | `SftpUserModel` | `GET, PUT, PATCH, POST, DELETE` |
| `/api/v1/sftp/{sftpServer}/users/{sftpUser}/validate/sshkey` | Endpoint for verifying that a private key patches a users public key | `{"result": Boolean, "message": String }` | `POST` |
| `/api/v1/sftp/{sftpServer}/users/{sftpUser}/validate/password` | Endpoint for verifying that a password patches a users hashed password| `{"result": Boolean, "message": String }` | `POST` |

Using the REST endpoints
---

### getting SFTP server information

```shell script
$ curl -H 'Authorization: Apixio  $APX_TOKEN_INTERNAL' -H 'Content-Type: application/json' http://localhost:8080/api/v1/sftp | jq
{
  "unit-test": {
    "region": "us-west-2",
    "hostname": "sftp.apixio.com",
    "port": 22,
    "securityGroupId": "sg-08289385027345988",
    "awsRole": null
  }
}
```

### Get information about a specific server

```shell script
$ curl -H 'Authorization: Apixio  $APX_TOKEN_INTERNAL' -H 'Content-Type: application/json' http://localhost:8080/api/v1/sftp/unit-test -X GET | jq
{
  "region": "us-west-2",
  "hostname": "sftp.apixio.com",
  "port": 22,
  "securityGroupId": "sg-08289385027345988",
  "awsRole": null
}
```

### Get All users in a server

```shell script
$ curl -H 'Authorization: Apixio  $APX_TOKEN_INTERNAL' -H 'Content-Type: application/json' http://localhost:8080/api/v1/sftp/unit-test/users -X GET | jq
[
  {
    "Username": "kmcgovern",
    "Password": "O/9:/[PkI-Vhq6b7%Ya:@s7s",
    "SftpServerId": "unit-test",
    "AcceptedIpNetwork": [
      "10.0.0.0/8",
      "192.168.0.0/24",
      "192.168.10.10/32"
    ],
    "Group": "INTERNAL"
  },
  {
    "Username": "test-user",
    "Password": "1[?#*V+U=cZI;:J08dUFCPpy",
    "SftpServerId": "test",
    "AcceptedIpNetwork": [
      "10.0.0.0/8",
      "192.168.0.0/24",
      "192.168.10.10/32",
      "192.168.10.11/32"
    ],
    "Group": "INTERNAL"
  }
]
```

### Create a user

```shell script
curl -H 'Authorization: Apixio  $APX_TOKEN_INTERNAL' -H 'Content-Type: application/json' http://localhost:8080/api/v1/sftp/unit-test/users/kmcgovern -X PUT -d @kmcgovern-test.json
```

### Get information about a user

```shell script
$ curl -H 'Authorization: Apixio  $APX_TOKEN_INTERNAL' -H 'Content-Type: application/json' http://localhost:8080/api/v1/sftp/unit-test/users/kmcgovern -X GET | jq
{
  "Username": "kmcgovern",
  "Password": "C2-^/GA+0~q(]@m+:&Neod$V",
  "SftpServerId": "unit-test",
  "AcceptedIpNetwork": [
    "10.0.0.0/8",
    "192.168.0.0/24",
    "192.168.10.10/32"
  ],
  "Group": "INTERNAL"
}
```

### Delete a User

```shell script
curl -H 'Authorization: Apixio  $APX_TOKEN_INTERNAL' -H 'Content-Type: application/json' http://localhost:8080/api/v1/sftp/unit-test/users/kmcgovern -X DELETE
```

### Generate a new password for a user

```shell script
curl -H 'Authorization: Apixio  $APX_TOKEN_INTERNAL' -H 'Content-Type: application/json' http://localhost:8080/api/v1/sftp/unit-test/users/kmcgovern?generatePassword=true -X PATCH
{
  "Username": "kmcgovern",
  "Password": ".=,iuCB1i?x#S]}kL3rUQ15;",
  "PublicKey": "ecdsa-sha2-nistp521 AAAAE2VjZHNhLXNoYTItbmlzdHA1MjEAAAAIbmlzdHA1MjEAAACFBAHV0H23ZDyfafwDJ9zDMfOS5rMAuAWlgTIJXcIuFQBsk+iV0Co3Mp25N3o9VtlUJgonQCaWdhPQfCb0VcwOYPuO2gG077CO8UwCAleCVm4Nt+VVXNHzzR/OLtIcgq700IfibDMoOFdsgEEf5GLY38pCGt8HetZtvM0FYpkriEaZJAK7Zw==",
  "SFTPServerID": "unit-test",
  "AcceptedIpNetwork": [
    "192.168.10.22/32"
  ],
  "Group": "INTERNAL",
  "HomeDirectoryDetails": "[{\"Entry\":\"/\",\"Target\":\"/apixio-test-sftp-bucket/kmcgovern\"}]",
  "HomeDirectoryType": "LOGICAL",
  "Role": "arn:aws:iam::088921318242:role/stg/infra-config-test",
  "Active": true
}
```

### Verify a password for a user

```shell script
curl -H "Authorization: Apixio $APX_TOKEN"  http://localhost:8080/api/v1/sftp/unit-test/users/kmcgovern/validate/password --data-binary "C2-^/GA+0~q(]@m+:&Neod$V" 2>/dev/null | jq
{
  "result": true,
  "message": ""
}
```

### Generate a new private key for a user

```shell script
curl -H 'Authorization: Apixio  $APX_TOKEN_INTERNAL' -H 'Content-Type: application/json' http://localhost:8080/api/v1/sftp/unit-test/users/kmcgovern -X PATCH -d '{"PrivateKey": "generate ecdsa-sha2-nistp521 512"}'
{
  "Username": "kmcgovern",
  "PublicKey": "ecdsa-sha2-nistp521 AAAAE2VjZHNhLXNoYTItbmlzdHA1MjEAAAAIbmlzdHA1MjEAAACFBAD0+Vck2ReBS/C3phu+0vefRhoVpJTcJ9QyBt9Pi3nPXnxXJD/xdIm6yVulB3L5WVGC6V9rMxTNf1YdqMZovdjHkgHRtEHBMUpX+wzud7SdEK0rrc1ynkbBjcHo8pcaqyj9NJy5AyMlGOMtx0DyUt31mA3vZi9SuaxOEXlE7RKMq8vDSA==",
  "PrivateKey": "-----BEGIN OPENSSH PRIVATE KEY-----\nb3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAAArAAAABNlY2RzYS\n1zaGEyLW5pc3RwNTIxAAAACG5pc3RwNTIxAAAAhQQA9PlXJNkXgUvwt6YbvtL3n0YaFaSU\n3CfUMgbfT4t5z158VyQ/8XSJuslbpQdy+VlRgulfazMUzX9WHajGaL3Yx5IB0bRBwTFKV/\nsM7ne0nRCtK63Ncp5GwY3B6PKXGqso/TScuQMjJRjjLcdA8lLd9ZgN72YvUrmsThF5RO0S\njKvLw0gAAAEAMJQLNDCUCzQAAAATZWNkc2Etc2hhMi1uaXN0cDUyMQAAAAhuaXN0cDUyMQ\nAAAIUEAPT5VyTZF4FL8LemG77S959GGhWklNwn1DIG30+Lec9efFckP/F0ibrJW6UHcvlZ\nUYLpX2szFM1/Vh2oxmi92MeSAdG0QcExSlf7DO53tJ0QrSutzXKeRsGNwejylxqrKP00nL\nkDIyUY4y3HQPJS3fWYDe9mL1K5rE4ReUTtEoyry8NIAAAAQSVvZC8X2oA68eW3SCHNgtSZ\nkorrvmWwXSDVvQfWe3nhQ29L/cRH09muGG/EsEc180Rr6oNn69JVTfnrR8mIzC8YAAAAAA\nECAw==\n-----END OPENSSH PRIVATE KEY-----\n",
  "SFTPServerID": "unit-test",
  "AcceptedIpNetwork": [
    "192.168.10.22/32"
  ],
  "Group": "INTERNAL",
  "HomeDirectoryDetails": "[{\"Entry\":\"/\",\"Target\":\"/apixio-test-sftp-bucket/kmcgovern\"}]",
  "HomeDirectoryType": "LOGICAL",
  "Role": "arn:aws:iam::088921318242:role/stg/infra-config-test",
  "Active": true
}
```

### Verify an ssh private key for a user

Note: `--data-binary` is needed on curl so newlines are not filterd out. if using another client make sure that the post
data contains newlines. Your client may remove/modify newline characters. This will make the validation fail even if it
should otherwise succeed.

```shell script
curl -H "Authorization: Apixio $APX_TOKEN"  http://localhost:8080/api/v1/sftp/unit-test/users/kmcgovern/validate/sshkey --data-binary @testkey 2>/dev/null | jq
{
  "result": true,
  "message": ""
}
```

### Initalize s3 chroot directory for a user

```shell script
curl -H 'Authorization: Apixio  $APX_TOKEN_INTERNAL' -H 'Content-Type: application/json' http://localhost:8080/api/v1/sftp/unit-test/users/kmcgovern?initializeS3=true -X PATCH 2>/dev/null | jq
{
  "Username": "kmcgovern",
  "PublicKey": "ecdsa-sha2-nistp521 AAAAE2VjZHNhLXNoYTItbmlzdHA1MjEAAAAIbmlzdHA1MjEAAACFBAD0+Vck2ReBS/C3phu+0vefRhoVpJTcJ9QyBt9Pi3nPXnxXJD/xdIm6yVulB3L5WVGC6V9rMxTNf1YdqMZovdjHkgHRtEHBMUpX+wzud7SdEK0rrc1ynkbBjcHo8pcaqyj9NJy5AyMlGOMtx0DyUt31mA3vZi9SuaxOEXlE7RKMq8vDSA==",
  "PrivateKey": "-----BEGIN OPENSSH PRIVATE KEY-----\nb3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAAArAAAABNlY2RzYS\n1zaGEyLW5pc3RwNTIxAAAACG5pc3RwNTIxAAAAhQQA9PlXJNkXgUvwt6YbvtL3n0YaFaSU\n3CfUMgbfT4t5z158VyQ/8XSJuslbpQdy+VlRgulfazMUzX9WHajGaL3Yx5IB0bRBwTFKV/\nsM7ne0nRCtK63Ncp5GwY3B6PKXGqso/TScuQMjJRjjLcdA8lLd9ZgN72YvUrmsThF5RO0S\njKvLw0gAAAEAMJQLNDCUCzQAAAATZWNkc2Etc2hhMi1uaXN0cDUyMQAAAAhuaXN0cDUyMQ\nAAAIUEAPT5VyTZF4FL8LemG77S959GGhWklNwn1DIG30+Lec9efFckP/F0ibrJW6UHcvlZ\nUYLpX2szFM1/Vh2oxmi92MeSAdG0QcExSlf7DO53tJ0QrSutzXKeRsGNwejylxqrKP00nL\nkDIyUY4y3HQPJS3fWYDe9mL1K5rE4ReUTtEoyry8NIAAAAQSVvZC8X2oA68eW3SCHNgtSZ\nkorrvmWwXSDVvQfWe3nhQ29L/cRH09muGG/EsEc180Rr6oNn69JVTfnrR8mIzC8YAAAAAA\nECAw==\n-----END OPENSSH PRIVATE KEY-----\n",
  "SFTPServerID": "unit-test",
  "AcceptedIpNetwork": [
    "192.168.10.22/32"
  ],
  "Group": "INTERNAL",
  "HomeDirectoryDetails": "[{\"Entry\":\"/\",\"Target\":\"/apixio-test-sftp-bucket/kmcgovern\"}]",
  "HomeDirectoryType": "LOGICAL",
  "Role": "arn:aws:iam::088921318242:role/stg/infra-config-test",
  "Active": true
}
```
