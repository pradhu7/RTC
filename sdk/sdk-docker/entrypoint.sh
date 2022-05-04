#!/usr/bin/env bash
set -e

MODEL_PATH=${MODEL_PATH:-"/app/model"}

DEFAULT_APP_MODULE="apxensemble.service.router:gunicorn_start(model_path=\"${MODEL_PATH}\")"

if [ -f /app/gunicorn_conf.py ]; then
    DEFAULT_GUNICORN_CONF=/app/gunicorn_conf.py
else
    DEFAULT_GUNICORN_CONF=/app/docker/gunicorn_conf.py
fi

export APP_MODULE=${APP_MODULE:-"$DEFAULT_APP_MODULE"}
export GUNICORN_CONF=${GUNICORN_CONF:-$DEFAULT_GUNICORN_CONF}

exec ddtrace-run gunicorn -c "$GUNICORN_CONF" "$APP_MODULE" --access-logfile - --error-logfile -
