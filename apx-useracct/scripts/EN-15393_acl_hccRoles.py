import apxapi

prd = apxapi.APXSession('user', environment=apxapi.PRD, do_auth=False)

#Create new operation
resp = prd.useraccounts.post_operation('ManageProjectUsers', 'Manage Project Users')

#Setup role VENDOR_ADMIN at Org level #permissions to ManageProjectUsers
vendor_admin_desc = "User with permissions to manage project users at organization"
vendor_admin_privs = [{'aclTarget': 'target-org','forMember': True,'operation': 'ManageProjectUsers'},
{'aclTarget': 'target-org','forMember': True,'operation': 'ViewUsers'}]

resp = prd.useraccounts.post_role('Customer', "VENDOR_ADMIN", vendor_admin_desc, privileges=vendor_admin_privs)

print(resp, resp.content)

