
"""
Apixio API Library
~~~~~~~~~~~~~~~~~~

This library is for accessing Apixio authenticated endpoints.  It can be used
with either a username and password or an external token, and it defaults to
using the staging environment.

Example usage for username/password:

   >>> import apxapi
   >>> s = apxapi.APXSession("slydon@apixio.com")
   Password:
   >>> s.useraccounts.customer_projects()
   <Response [200]>
   >>> s.external_token()
   u'TA_fb71d668-d5cd-4504-9176-cbdca380940a'
   >>> s.internal_token()
   u'TB_513d818f-5f71-45ee-bc83-294c85d288b0'

Example usage for external token:

   >>> import apxapi
   >>> s = apxapi.APXSession(e_token='TA_fb71d668-d5cd-4504-9176-cbdca380940a')
   >>> s.useraccounts.customer_projects()
   <Response [200]>
   >>> s.internal_token()
   u'TB_39840931-7d9c-4214-bfcb-a8928079c9ef'

There also exists a fast session method that populates values from config file:

  >>> from apxapi import session
  >>> s = session()
  >>> s.internal_token()
  u'TB_39840931-7d9c-4214-bfcb-a8928079c9ef'
"""

import requests
import threading
import os
from getpass import getpass
try:
    from ConfigParser import RawConfigParser
except ImportError:
    from configparser import RawConfigParser

from . import acl_admin
from . import adhoc_reports
from . import bundler
from . import cmp_msvc
from . import data_orchestrator
from . import event_cloud
from . import hcc_reports
from . import hcc_reports_indexer
from . import pipeline_admin
from . import user_accounts
from . import base_service
from . import doc_receiver
from . import opp_router
from . import es

# prevents: InsecurePlatformWarning: A true SSLContext object is not available
requests.packages.urllib3.disable_warnings()


STG = 'staging'
PRD = 'production'
ENG = 'engineering'
LOC = 'local'
DEV = 'development'
DEMO = 'demo'


ENVMAP = {STG: {'acladmin': 'https://acladmin-stg.apixio.com',
                'adhocreports': 'https://hcc-reports-2-stg.apixio.com:7097',
                'bundler': 'https://bundler-stg.apixio.com:8443',
                'cmpmsvc': 'https://cmp-stg2.apixio.com:7087',
                'dataorchestrator': 'https://dataorchestrator-stg.apixio.com:7085',
                'docreceiver': 'https://dr-stg.apixio.com:9443',
                'es': 'http://elasticsearch-stg.apixio.com:9200',
                'eventcloudhcc': 'http://event-cloud-stg.apixio.com:8075',
                'eventcloudpipeline': 'http://event-cloud-stg.apixio.com:8076',
                'hccreports': 'https://hcc-reports-2-stg.apixio.com:7097',
                'hccreportsindexer': 'https://hcc-reports-2-stg.apixio.com:7087',
                'opprouter' : 'https://hcc-opprouter-stg.apixio.com:8443',
                'pipelineadmin': 'https://coordinator-stg.apixio.com:8066',
                'tokenizer': 'https://tokenizer-stg.apixio.com:7075',
                'useraccount': 'https://useraccount-stg.apixio.com:7076'
               },
          PRD: {'acladmin': 'https://acladmin.apixio.com',
                'adhocreports': 'https://hcc-reports-2.apixio.com:7097',
                'bundler': 'https://bundler.apixio.com:8443',
                'cmpmsvc': 'https://cmp-2.apixio.com:7087',
                'dataorchestrator': 'https://dataorchestrator.apixio.com:7085',
                'docreceiver': 'https://dr.apixio.com',
                'es': 'http://elasticsearch.apixio.com:9200',
                'eventcloudhcc': 'http://event-cloud-2.apixio.com:8075',
                'eventcloudpipeline': 'http://event-cloud.apixio.com:8076',
                'hccreports': 'https://hcc-reports-2.apixio.com:7097',
                'hccreportsindexer': 'https://hcc-reports-2.apixio.com:7087',
                'opprouter' : 'https://hcc-opprouter.apixio.com:8443',
                'pipelineadmin': 'https://coordinator.apixio.com:8066',
                'tokenizer': 'https://tokenizer.apixio.com:7075',
                'useraccount': 'https://useraccount.apixio.com:7076'
               },
          DEMO: {'acladmin': 'https://acladmin.apixio.com',
                'adhocreports': 'https://hcc-reports-2.apixio.com:7097',
                'bundler': 'https://hcc-demo-backend.apixio.com:8443',
                'cmpmsvc': 'https://hcc-demo-backend.apixio.com:7087',
                'dataorchestrator': 'https://dataorchestrator.apixio.com:7085',
                'es': 'http://elasticsearch.apixio.com:9200',
                'eventcloudhcc': 'http://event-cloud-2.apixio.com:8075',
                'eventcloudpipeline': 'http://event-cloud.apixio.com:8076',
                'hccreports': 'https://hcc-demo-backend.apixio.com:7097',
                'hccreportsindexer': 'https://hcc-reports-2.apixio.com:7087',
                'opprouter' : 'https://hcc-demo-backend.apixio.com:8443',
                'pipelineadmin': 'https://coordinator.apixio.com:8066',
                'tokenizer': 'https://tokenizer-ua.apixio.com:7075',
                'useraccount': 'https://useraccount-ua.apixio.com:7076'
               },
          ENG: {'acladmin': 'https://acladmin-stg.apixio.com',
                'adhocreports': 'https://hcc-reports-2-stg2.apixio.com:7097',
                'bundler': 'https://bundler-stg2.apixio.com:8443',
                'cmpmsvc': 'https://cmp-stg2.apixio.com:7087',
                'dataorchestrator': 'https://dataorchestrator-stg.apixio.com:7085',
                'docreceiver': 'https://dr-stg.apixio.com:9443',
                'es': 'http://elasticsearch-stg.apixio.com:9200',
                'eventcloudhcc': 'http://event-cloud-stg2.apixio.com:8075',
                'eventcloudpipeline': 'http://event-cloud-stg.apixio.com:8076',
                'hccreports': 'https://hcc-reports-2-stg2.apixio.com:7097',
                'hccreportsindexer': 'https://hcc-reports-2-stg2.apixio.com:7087',
                'opprouter': 'https://hcc-opprouter-stg2.apixio.com:8443',
                'pipelineadmin': 'https://coordinator-stg.apixio.com:8066',
                'tokenizer': 'https://tokenizer-stg.apixio.com:7075',
                'useraccount': 'https://useraccount-stg.apixio.com:7076'
               },
          LOC: {'acladmin': 'https://acladmin-stg.apixio.com',
                'adhocreports': 'https://hcc-reports-2-stg2.apixio.com:7097',
                'bundler': 'https://bundler-stg2.apixio.com:8443',
                'cmpmsvc': 'https://cmp-stg2.apixio.com:7087',
                'dataorchestrator': 'https://dataorchestrator-stg.apixio.com:7085',
                'docreceiver': 'https://dr-stg.apixio.com:9443',
                'es': 'http://elasticsearch-stg.apixio.com:9200',
                'eventcloudhcc': 'http://event-cloud-stg2.apixio.com:8075',
                'eventcloudpipeline': 'http://event-cloud-stg.apixio.com:8076',
                'hccreports': 'https://hcc-reports-2-stg2.apixio.com:7097',
                'hccreportsindexer': 'https://hcc-reports-2-stg2.apixio.com:7087',
                'opprouter': 'https://hcc-opprouter-stg2.apixio.com:8443',
                'pipelineadmin': 'https://coordinator-stg.apixio.com:8066',
                'tokenizer': 'http://localhost:8075',
                'useraccount': 'http://localhost:8076'
               },
          # TODO: These URLs will need to be updated as they are deployed
          DEV: {'acladmin': 'https://acladmin-stg.apixio.com',
                'adhocreports': 'https://hcc-reports-dev.apixio.com:7097',
                'cmpmsvc': 'https://cmp-dev.apixio.com:7087',
                'dataorchestrator': 'https://data-orchestrator-dev.apixio.com:7085',
                'docreceiver': 'https://dr-dev.apixio.com:9443',
                'es': 'http://elasticsearch-stg.apixio.com:9200',
                'eventcloudhcc': 'http://event-cloud-stg.apixio.com:8075',
                'eventcloudpipeline': 'http://event-cloud-stg.apixio.com:8076',
                'hccreports': 'https://hcc-reports-dev.apixio.com:7097',
                'hccreportsindexer': 'https://hcc-reports-dev.apixio.com:7087',
                'opprouter': 'https://hcc-opprouter-dev.apixio.com:8443',
                'pipelineadmin': 'https://coordinator-dev.apixio.com:8066',
                'tokenizer': 'https://tokenizer-dev.apixio.com:7075',
                'useraccount': 'https://useraccount-dev.apixio.com:7076',
                'bundler': 'https://bundler-dev.apixio.com:8443'
               }
         }

def session(environment=STG):
    """Creates a very quick session using a config file to store usernames and passwords."""
    cfg = RawConfigParser()
    fp = os.path.expanduser("~/.apxapi")
    if not os.path.exists(fp):
        raise EnvironmentError("configuration file not found at %s" % fp)
    cfg.read(fp)
    return APXSession(cfg.get(environment, 'username'), cfg.get(environment, 'password'),
                      environment=environment)

def create_apx_session(username=None, password=None, e_token=None, environment=STG):
    """Creates an APXSession.  Kept around for some backwards compatability."""
    return APXSession(username, password, e_token, environment)


def password_auth_fng(username, password, auth_url):
    """Function generator to renew the external token using username and password."""
    def fn():
        try:
            res = requests.post(auth_url, data={'email': username, 'password': password})
            return res.json()['token']
        except KeyboardInterrupt:
            raise
        except Exception as e:
            raise APXAuthException('Unable to renew external token using username/password' + str(e) )
    return fn

def co_password_auth_fng(username, password, auth_url):
    """Function generator to renew the co token using username and password."""
    def fn():
        try:
            res = requests.post(auth_url, data={'username': username, 'password': password})
            print(res.json())
            return res.json()['token']
        except KeyboardInterrupt:
            raise
        except:
            raise APXAuthException('Unable to renew external token using username/password')
    return fn

def etoken_auth_fng(e_token):
    """Function generator to be unable to renew the external token."""
    def fn():
        raise APXAuthException('External token %s expired and unable to renew.' % e_token)
    return fn

def itoken_auth_fng(i_token):
    """Function generator to be unable to renew the internal token."""
    def fn():
        raise APXAuthException('Internal token %s expired.' % i_token)
    return fn


class APXAuthException(Exception):
    pass


class APXSession(requests.Session):
    """Create an APXSession using either external token or a username and possible password.

    The session is an extension of a normal requests Session and has all methods associated with
    it.

    The session has internal members that allow programatic access to the Apixio APIs. Each returns
    a response object.  Invoke services() to see which all are available.
    """

    def __init__(self, username=None, password=None, e_token=None, environment=STG, timeout=None,
                 do_auth=True, co_username=None, co_password=None, i_token=None):
        super(APXSession, self).__init__()

        if do_auth:
          if username is None and e_token is None and i_token is None:
              raise TypeError('You need to provide a valid username or external token or internal token')

          auth_url = ENVMAP[environment]['useraccount'] + '/auths'
          token_url = ENVMAP[environment]['tokenizer'] + '/tokens'

          if i_token is not None or e_token is not None:
              auth_fn =  itoken_auth_fng(i_token) if e_token is None else etoken_auth_fng(e_token)
              self.apx_handler = APXHTTPAdapter(token_url, auth_fn, e_token, timeout, i_token = i_token)
          else:
              if password is None or password == '':
                  password = getpass('Password: ')
              fn = password_auth_fng(username, password, auth_url)
              self.apx_handler = APXHTTPAdapter(token_url, fn, timeout=timeout)

          self.mount('https://', self.apx_handler)
          self.mount('http://', self.apx_handler)

        if co_username is not None:
            if co_password is None or co_password == '':
                co_password = getpass('Password: ')
            auth_url = ENVMAP[environment]['docreceiver'] + '/auth/token'
            fn = co_password_auth_fng(co_username, co_password, auth_url)
            self.co_handler = COHTTPAdapter(fn, timeout=timeout)
            #This only works as long as we don't have any hosts other than dr with this prefix
            self.mount('https://dr', self.co_handler)
            self.docreceiver = doc_receiver.DocReceiver(ENVMAP[environment]['docreceiver'], self)

        # this is where to setup the helper classes for each service
        self.acladmin = acl_admin.ACLAdmin(ENVMAP[environment]['acladmin'], self)
        self.adhocreports = adhoc_reports.AdhocReports(ENVMAP[environment]['adhocreports'], self)
        self.bundler = bundler.Bundler(ENVMAP[environment]['bundler'], self)
        self.cmpmsvc = cmp_msvc.CMPMSVC(ENVMAP[environment]['cmpmsvc'], self)
        self.dataorchestrator = data_orchestrator.DataOrchestrator(
            ENVMAP[environment]['dataorchestrator'], self)
        self.es = es.ElasticSearch(ENVMAP[environment]['es'], self)
        self.eventcloudhcc = event_cloud.EventCloud(ENVMAP[environment]['eventcloudhcc'], self)
        self.eventcloudpipeline = event_cloud.EventCloud(ENVMAP[environment]['eventcloudpipeline'], self)
        self.hccreports = hcc_reports.HCCReports(ENVMAP[environment]['hccreports'], self)
        self.hccreportsindexer = hcc_reports_indexer.HCCReportsIndexer(
            ENVMAP[environment]['hccreportsindexer'], self)
        self.opprouter = opp_router.OPPRouter(ENVMAP[environment]['opprouter'], self)
        self.pipelineadmin = pipeline_admin.PipelineAdmin(ENVMAP[environment]['pipelineadmin'], self)
        self.useraccounts = user_accounts.UserAccounts(ENVMAP[environment]['useraccount'], self)

    def external_token(self):
        """Get the external token currently in use."""
        return self.apx_handler.external_token()

    def external_co_token(self):
        """Get the external care-optimizer token currently in use."""
        return self.co_handler.external_token()

    def internal_token(self):
        """Get the internal token currently in use."""
        return self.apx_handler.internal_token()

    def services(self):
        """List of the services enabled by this object."""
        return [x for x in dir(self) if isinstance(getattr(self, x), base_service.BaseService)]


class APXHTTPAdapter(requests.adapters.HTTPAdapter):
    def __init__(self, token_url, e_token_fn, e_token=None, timeout=None, pool_connections=10,
                 pool_maxsize=10, max_retries=0, pool_block=False, i_token=None):
        super(APXHTTPAdapter, self).__init__(pool_connections,pool_maxsize,max_retries,pool_block)
        self._apx_token_url = token_url
        self._apx_e_token = e_token
        self._apx_e_token_fn = e_token_fn
        self._apx_timeout = timeout
        self._apx_i_token = i_token
        self._swap_lock = threading.Lock()

    def _swap_token(self):
        old_i_token = self._apx_i_token
        with self._swap_lock:
            if self._apx_i_token != old_i_token:  #some other thread already did a token swap
                return
            if self._apx_e_token is None:
                self._apx_e_token = self._apx_e_token_fn()
            try:
                res = requests.post(self._apx_token_url,
                                    headers={'Authorization': 'Apixio ' + self._apx_e_token})
                self._apx_i_token = res.json()['token']
            except KeyboardInterrupt:
                raise
            except Exception:
                self._apx_e_token = self._apx_e_token_fn()
                try:
                    res = requests.post(self._apx_token_url,
                                        headers={'Authorization': 'Apixio ' + self._apx_e_token})
                    self._apx_i_token = res.json()['token']
                except KeyboardInterrupt:
                    raise
                except Exception:
                    raise APXAuthException('Unable to swap external for internal token')

    def external_token(self):
        if self._apx_e_token is None:
            self._apx_e_token = self._apx_e_token_fn()
        return self._apx_e_token

    def internal_token(self):
        if self._apx_i_token is None:
            self._swap_token()
        return self._apx_i_token

    def add_headers(self, request, **kwargs):
        if self._apx_i_token is None:
            self._swap_token()
        request.headers['Authorization'] = 'Apixio ' + self._apx_i_token

    def send(self, request, stream=False, timeout=None, verify=True, cert=None, proxies=None):
        timeout = timeout if (timeout is not None) else self._apx_timeout
        result = super(APXHTTPAdapter, self).send(request, stream, timeout, verify, cert, proxies)
        if result.status_code == 401:
            self._swap_token()
            result = super(APXHTTPAdapter, self).send(request, stream, timeout, verify, cert, proxies)
        return result


class COHTTPAdapter(requests.adapters.HTTPAdapter):
    def __init__(self, e_token_fn, e_token=None, timeout=None, pool_connections=10,
                 pool_maxsize=10, max_retries=0, pool_block=False):
        super(COHTTPAdapter, self).__init__(pool_connections,pool_maxsize,max_retries,pool_block)
        self._apx_e_token = e_token
        self._apx_e_token_fn = e_token_fn
        self._apx_timeout = timeout

    def external_token(self):
        if self._apx_e_token is None:
            self._apx_e_token = self._apx_e_token_fn()
        return self._apx_e_token

    def send(self, request, stream=False, timeout=None, verify=True, cert=None, proxies=None):
        timeout = timeout if (timeout is not None) else self._apx_timeout
        result = super(COHTTPAdapter, self).send(request, stream, timeout, verify, cert, proxies)
        return result
