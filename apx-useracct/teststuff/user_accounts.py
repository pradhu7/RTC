from . import base_service
import json


class UserAccounts(base_service.BaseService):

    def get_junk(self, id):
        return self.session.get('%s/junk/%s' % (self.url, id))
 
    ### Projects

    def post_project(self, name, desc, prjType, orgID, pdsID, dosStart, dosEnd, payYr, sweep, passType):
        payload = {
            'name': name,
            'description': desc,
            'type': prjType,
            'organizationID': orgID,
            'patientDataSetID': pdsID,
            'sweep': sweep,
            'passType': passType
        }
        return self.session.post('%s/projects/' % (self.url), json=payload)

    def get_projects_all(self):
        return self.session.get('%s/projects' % self.url)

    def get_project(self, project_id):
        return self.session.get('%s/projects/%s?properties=all' % (self.url, project_id))

    def get_project_users(self, project_id):
        return self.session.get('%s/projects/%s/members' % (self.url, project_id))

    def get_project_user_roles(self, project_id, user_email):
        return self.session.get('%s/projects/%s/users/%s/roles' % (self.url, project_id, user_email))

    def get_project_properties(self, project_id, bag="gen"):
        return self.session.get('%s/projects/%s/properties/%s' % (self.url, project_id, bag))

    def get_project_for_user(self, user_id):
        return self.session.get('%s/projects/users/%s' % (self.url, user_id))

    def get_projects_by_org(self, org_id):
        params = self.create_query_params(orgID=org_id)
        return self.session.get('%s/projects' % (self.url), params = params)

    def put_project_user_role(self, project_id, user_email, role_name):
        """Adds the user to the specified role for a project"""
        return self.session.put('%s/projects/%s/users/%s/roles/%s' % (self.url, project_id, user_email, role_name))

    def delete_project_user_role(self, project_id, user_email, role_name):
        return self.session.delete('%s/projects/%s/users/%s/roles/%s' % (self.url, project_id, user_email, role_name))

    ### Patient Data Sets

    def post_pds(self, name, desc, owningOrgID):
        payload = {
            'name': name,
            'description': desc,
            'owningOrgID': owningOrgID
        }
        return self.session.post('%s/patientdatasets/' % (self.url), json=payload)

    def get_pds_all(self):
        return self.session.get('%s/patientdatasets' % (self.url))

    def get_pds_properties(self):
        return self.session.get('%s/patientdatasets/properties' % (self.url))

    def get_pds_propdefs(self):
        return self.session.get('%s/patientdatasets/propdefs' % (self.url))

    def get_project_propdef_phase(self):
        return self.session.get('%s/projects/propdefs/phase' % (self.url))

    def get_pds(self, pds_id):
        return self.session.get('%s/patientdatasets/%s' % (self.url, pds_id))

    ### Texts

    def get_texts(self, content=None):
        params = self.create_query_params(content=content)
        return self.session.get('%s/texts' % (self.url,), params=params)
    
    def put_texts(self, template_id, contents):
        payload = {
            'name': template_id,
            'contents': contents
        }
        return self.session.put('%s/texts' % (self.url,), json=payload)
        
    ### Users

    def get_uorg(self, org_id):
        return self.session.get('%s/uorgs/%s' % (self.url, org_id))

    def get_user(self, user_id):
        return self.session.get('%s/users/%s' % (self.url, user_id))

    def get_user_all(self):
        return self.session.get('%s/users/' % self.url)

    def get_user_roles(self, user_id):
        return self.session.get('%s/users/%s/roles' % (self.url, user_id))

    def post_user(self, email, org_id, skip_email, resend_only):
        payload = {
            'email': email,
            'organizationID': org_id,
            'skipEmail': skip_email,
            'resendOnly': resend_only
        }
        return self.session.post('%s/users' % self.url, json=payload)

    def put_user_password(self, user_id, password):
        payload = {
            'password': password
        }
        return self.session.put('%s/users/priv/%s' % (self.url, user_id), json=payload)

    def put_user_active(self, user_id, is_active):
        payload = {
            'state': is_active
        }
        return self.session.put('%s/users/%s/activation' % (self.url, user_id), json=payload)
    def put_user_setting(self,username,setting,value):
        return self.session.put('%s/users/%s/%s' % (self.url,username,setting),
            data=json.dumps({setting:value}), headers={'Content-Type': 'application/json'})
        
    # call with template_name='forgotpassword' if you want a default
    def post_user_pass_email(self, user_id, template_name):
        payload = {
            'templateName': template_name
        }
        return self.session.post('%s/users/%s/pass_email' % (self.url, user_id), json=payload)

    def get_user_org(self, user_id):
        return self.session.get('%s/users/%s/org' % (self.url, user_id))

    def delete_user(self, user_id):
        return self.session.delete('%s/users/%s' % (self.url, user_id))

    ### User Organizations

    def get_uorg_all(self):
        return self.session.get('%s/uorgs/' % self.url)

    def get_uorg(self, org_id):
        return self.session.get('%s/uorgs/%s' % (self.url, org_id))

    def post_uorg(self, name, description, type, external_id):
        payload = {
            'name': name,
            'description': description,
            'type': type,
            'externalID': external_id
        }
        return self.session.post('%s/uorgs' % self.url, json=payload)

    def put_uorg(self, uorg_id, name, description):
        payload = {
            'name': name,
            'description': description
        }
        return self.session.put('%s/uorgs/%s' % (self.url, uorg_id), json=payload)

    def put_uorg_setting(self, uorg_id, setting, value):
        return self.session.put('%s/uorgs/%s/%s' % (self.url,uorg_id,setting),
            data=json.dumps({setting:value}), headers={'Content-Type': 'application/json'})

    def put_uorg_user(self, org_id, user_email):
        return self.session.put('%s/uorgs/%s/members/%s' % (self.url, org_id, user_email))

    def delete_uorg_user(self, org_id, user_email):
        return self.session.delete('%s/uorgs/%s/members/%s' % (self.url, org_id, user_email))

    def get_system_org(self):
        return self.session.get('%s/uorgs?type=System' % self.url)

    def get_uorg_members(self, org_id):
        return self.session.get('%s/uorgs/%s/members' % (self.url, org_id))

    def put_uorg_user_role(self, org_id, user_id, role):
        return self.session.put('%s/uorgs/%s/roles/%s/%s' % (self.url, org_id, role, user_id))

    def delete_uorg_user_role(self, org_id, user_id, role):
        return self.session.delete('%s/uorgs/%s/roles/%s/%s' % (self.url, org_id, role, user_id))

    ### Roles api

    def get_system_roles(self):
        return self.session.get('%s/rolesets' % self.url)
    
    def post_role(self, role_set_name, role_name, role_description, privileges=[]):
        payload = {
            'name': role_name,
            'description': role_description,
            'privileges': privileges
        } 
        return self.session.post('%s/rolesets/%s' % (self.url, role_set_name), json=payload)

    def put_role(self, role_set_name, role_name, privileges):
        return self.session.put('%s/rolesets/%s/%s' % (self.url, role_set_name, role_name), json=privileges)
    
    def get_operations(self):
        return self.session.get('%s/aclop' % (self.url))
    
    def post_operation(self, name, description, access_types=[]):
        payload = {
            'name': name,
            'description': description,
            'accessTypes': access_types
        } 
        return self.session.put('%s/aclop' % (self.url), json=payload)

    def get_pass_policies(self):
        return self.session.get('%s/passpolicies' % (self.url))
  
    def get_pass_policy(self, policy_name):
        return self.session.get('%s/passpolicies/%s' % (self.url, policy_name))
 
    def new_pass_policy(self, policy):
        return self.session.post('%s/passpolicies' % (self.url), data=policy)
 
    def update_pass_policy(self, policy_name, policy):
        return self.session.put('%s/passpolicies/%s' % (self.url, policy_name),
            data=policy, headers={'Content-Type': 'application/json'})

    def has_permission(self, permission, org_id, user=None):
        url = '%s/perms/%s/%s'
        data = [self.url, permission, org_id]
        if user:
            url += "/%s"
            data.insert(1, user)
        
        return self.session.get(url % tuple(data))
