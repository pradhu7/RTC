May 10, 2016

#### framework setup:
1.  Copy __init__.py and user_accounts.py to python-apxapi/apxapi/

2.  Copy core*.py to python-apxapi/

#### data store setup:
3.  initialize the redis & cassandra stores

4.  run user-accounts/ua-init.sh to set up baseline system

5.  cd python-apxapi/

6.  python core1667-setup.py

#### run tests (as many times as needed):
7.  python core1667-test1.py
8.  python core1667-test2.py
