FROM 088921318242.dkr.ecr.us-west-2.amazonaws.com/python:3.8

### cwd for this build seems to be apx-sdk/

COPY . /app
#COPY ./docker/pip.conf /etc/pip.conf
COPY docker/pip.conf /etc/pip.conf

USER root
WORKDIR /app
RUN apt-get update -y && \
    apt-get -y install build-essential zip nano && \
    pip install -r /app/docker/requirements.txt && \
    pip install ddtrace && \
    chmod a+x /app/docker/entrypoint.sh

# should be removed later
# ARG ARTIFACT_USER
# ARG ARTIFACT_PASS
# WORKDIR /tmp
#RUN wget --http-user=${ARTIFACT_USER} --http-password=${ARTIFACT_PASS} -O model.zip \
#    https://repos.apixio.com/artifactory/models/models/production_model/10.1.0/production_model_10_1_0.zip && \
#    unzip model.zip -d /app/model && \
#    rm -f model.zip

WORKDIR /home/user
USER 1000
EXPOSE 4500/tcp
ENV PYTHONPATH=/app
WORKDIR /app/sdkcore/src/main/python/test
CMD ["python3","sdkannotest.py","4500","1","/etc/config.yaml"]

# ENTRYPOINT [ "/app/docker/entrypoint.sh" ]
