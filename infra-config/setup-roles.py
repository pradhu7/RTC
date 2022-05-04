import apxapi
import os
import sys
from pprint import pprint

if sys.argv[1] == 'PROD':
    env = apxapi.PRD
else:
    env = apxapi.DEV

print(f"using environment {env}. arg {sys.argv[1]}")
d = apxapi.session(environment=env)

operation_suffix = os.environ.get("OPERATION_SUFFIX", "")
role_suffix = os.environ.get("ROLE_SUFFIX", "")

DELETE = False
sftp_role_descriptions = {
    f"VIEW_SFTP{role_suffix}": "Viewing sftp users and servers",
    f"MANAGE_SFTP{role_suffix}": "Updating and viewing sftp users and servers"
}

SFTP_ROLE_SET_NAME = "System"
sftpOperations = {
    f"readSftpUsers{operation_suffix}": {
        "description": "view users inside of sftp servers",
        "roles": [
            f"VIEW_SFTP{role_suffix}",
            f"MANAGE_SFTP{role_suffix}",
        ]
    },
    f"createSftpUsers{operation_suffix}": {
        "description": "manage users inside of sftp servers",
        "roles": [
            f"MANAGE_SFTP{role_suffix}"
        ]
    },
    f"updateSftpUsers{operation_suffix}": {
        "description": "manage users inside of sftp servers",
        "roles": [
            f"MANAGE_SFTP{role_suffix}"
        ]
    },
    f"deleteSftpUsers{operation_suffix}": {
        "description": "manage users inside of sftp servers",
        "roles": [
            f"MANAGE_SFTP{role_suffix}"
        ]
    },
    f"readSftpServers{operation_suffix}": {
        "description": "get information about sftp servers",
        "roles": [
            f"VIEW_SFTP{role_suffix}",
            f"MANAGE_SFTP{role_suffix}"
        ]
    },
}

role_sets = [role_set for role_set in d.useraccounts.get_system_roles().json() if
             role_set['nameID'] == SFTP_ROLE_SET_NAME]
if len(role_sets) > 1:
    raise ValueError(f"More than 1 role found with ID {SFTP_ROLE_NAME}")

role_set = {}
if len(role_sets) == 1:
    role_set = role_sets[0]
    pprint(role_set)

operations = [operation['name'] for operation in d.useraccounts.get_operations().json() if
              operation['name'] in sftpOperations.keys()]

add_ops = {}
for operation, info in sftpOperations.items():
    if not operation in operations:
        add_ops[operation] = info["description"]

all_roles = [role for operation, info in sftpOperations.items() for role in info["roles"]]
all_roles = list(set(all_roles))

current_role_names = [role["name"] for role in role_set.get("roles", [])]

role_privs = {}

for role in role_set.get("roles", []):
    if role["name"] in all_roles:
        role_privs[role["name"]] = role["privileges"]

for role in all_roles:
    if not role in role_privs.keys():
        role_privs[role] = []

for op, info in sftpOperations.items():
    for role in info["roles"]:
        role_privs[role] = [priv for priv in role_privs.get(role) if priv["operation"] != op]
        if not DELETE:
            role_privs[role].append({
                "aclTarget": "*",
                "forMember": True,
                "operation": op
            })

print(f"modifying role group {SFTP_ROLE_SET_NAME} with:")
pprint(role_privs)

for operation, description in add_ops.items():
    print(f"adding operation {operation}:{description}")
    response = d.useraccounts.post_operation(operation, description)
    if not response.status_code in [200, 201]:
        print(f"error creating operation {operation}")

for role, privs in role_privs.items():
    if role in current_role_names:
        print(f"updating role {role}")
        response = d.useraccounts.put_role(SFTP_ROLE_SET_NAME, role, privs)
        if not response.ok:
            print(f"error updating role {response.url}")
            print(response.content)
        print(f"updated role {response.url} - {response.status_code}")
    else:
        print(f"creating role {role}")
        description = sftp_role_descriptions.get(role)
        response = d.useraccounts.post_role(SFTP_ROLE_SET_NAME, role, description, privs)
        if not response.ok:
            print(f"error creating role {role}")
            print(response.content)
        print(f"created role {response.url} - {response.status_code}")
