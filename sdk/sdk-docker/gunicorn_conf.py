from __future__ import print_function

import json
import multiprocessing
import os

workers_per_core_str = os.getenv("WORKERS_PER_CORE", "1")
web_concurrency_str = os.getenv("WEB_CONCURRENCY", None)
host = os.getenv("HOST", "0.0.0.0")
port = os.getenv("PORT", "8080")
bind_env = os.getenv("BIND", None)
use_loglevel = os.getenv("LOG_LEVEL", "info")
use_worker_class = os.getenv('WORKER_CLASS', "sync")
metrics_host = os.getenv('DD_AGENT_HOST', 'localhost')
metrics_port = 8125
container_name = os.getenv('HOSTNAME')
container_ip = os.getenv('POD_IP')

if bind_env:
    use_bind = bind_env
else:
    use_bind = "{host}:{port}".format(host=host, port=port)

cores = multiprocessing.cpu_count()
workers_per_core = float(workers_per_core_str)
default_web_concurrency = workers_per_core * cores
if web_concurrency_str:
    web_concurrency = int(web_concurrency_str)
    assert web_concurrency > 0
else:
    web_concurrency = int(default_web_concurrency)

# Gunicorn config variables
loglevel = use_loglevel
workers = web_concurrency
bind = use_bind
keepalive = 120
errorlog = "-" # error log to stderr
worker_class = use_worker_class
statsd_host = '{host}:{port}'.format(host=metrics_host, port=metrics_port)
dogstatsd_tags = 'app:apx-sdk,container_name:{container_name},container_ip:{container_ip}'.format(container_name=container_name,
                                                                                           container_ip=container_ip)

# For debugging and testing
log_data = {
    "loglevel": loglevel,
    "workers": workers,
    "bind": bind,
    # Additional, non-gunicorn variables
    "workers_per_core": workers_per_core,
    "host": host,
    "port": port,
    "statsd_host": statsd_host,
    "worker_class": worker_class
}
print(json.dumps(log_data))
