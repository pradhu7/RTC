path "secret/sftp/unit-test/*" {
  capabilities = ["create", "read", "update", "delete", "list"]
}
path "secret/sftp/unit-test" {
  capabilities = ["read", "list"]
}

path "secret/sftp/*" {
  capabilities = ["create", "read", "update", "delete", "list"]
}
path "secret/sftp" {
  capabilities = ["read", "list"]
}
