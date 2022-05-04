library('jenkins_shared_libraries')
import org.apache.commons.io.FilenameUtils

def getPomBuilds() {
  relative_projects_paths = sh(
    script: "mvn -Dexec.executable='git' -Dexec.args='rev-parse --show-prefix' exec:exec -q",
    returnStdout: true
  ).trim().split('\n')
  def modules = []
  for (path in relative_projects_paths) {
    def path_no_trailing_slash = FilenameUtils.getPathNoEndSeparator(path)
    modules.add(path_no_trailing_slash)
  }
  return modules
}

def buildParallelPomBuilds(List mvn_args, int parallelism = 8) {
  def parallel_builds = [:]
  def semaphore_id = "${env.BUILD_TAG}"
  for (l_module in getPomBuilds()) {
    def l_mvn_args = mvn_args
    def module = l_module
    parallel_builds[module] = {
      // use a semaphore via gnu-parallel. this is a file based semaphore and will work across multiple processes
      // as long long as they share a filesystem. use --fg to block it from returning after it gets the semaphore
      for(l_mvn_args_item in l_mvn_args) {
        sh "sem --fg --id ${semaphore_id} -j ${parallelism} 'mvn -pl ${module} ${l_mvn_args_item}'"
      }
    }
  }
  return parallel_builds
}

def getDockerBuilds() {
  pom_build_paths = getPomBuilds()
  def modules= [:]
  for (pom_build in pom_build_paths) {
    def docker_json_path = pom_build + '/.docker_build.json'
    if (fileExists(docker_json_path)) {
      def props = readJSON file: docker_json_path
      def module = FilenameUtils.getBaseName(pom_build)
      modules[module] = props
    }
  }
  return modules
}

def buildParallelDockerBuilds(project_version) {
  def parallel_builds = [:]
  getDockerBuilds().each { l_module, l_props ->
    def module = l_module
    def props = l_props
    parallel_builds[module] = {
      buildAndStgDeploy(module, project_version, props)
    }
  }
  return parallel_builds
}

pipeline {
    agent {
        kubernetes {
            // Truncate label down to 61 characters for k8s label
            // restrictions.
            label 'jenkins-mono-' + env.BRANCH_NAME.replaceAll("\\.", "_").replaceAll("/", "_").substring(0, Math.min(env.BRANCH_NAME.size(), 61-'jenkins-mono-'.size()))  // all your pods will be named with this prefix, followed by a unique id
          idleMinutes 0  // how long the pod will live after no jobs have run on it
          yamlFile 'buildbox.yaml'  // path to the pod definition relative to the root of our project
          defaultContainer 'mvn'  // define a default container if more than a few stages use it, will default to jnlp container
        }
    }

    environment {
        REPO_NAME = "mono"
        SLACK_CHANNEL = "#eng-mono-repository"
        DOCKER_HOST = "tcp://localhost:2376"
    }

    options {
        timestamps()
        ansiColor('xterm')
        disableConcurrentBuilds()
        buildDiscarder(logRotator(daysToKeepStr: '3',
                                  numToKeepStr: '3'))
    }

    parameters {
        booleanParam(name: 'PUBLISH_SNAPSHOT', defaultValue: false, description: 'Publish a SNAPSHOT, even if not on `dev` branch.')
        booleanParam(name: 'FORCE_SKIP_TESTS', defaultValue: false, description: 'Skip unit tests. Only use this if there are issues with Jenkins and tests pass locally.')
        string(name: 'STG_DEPLOY', defaultValue: "", description: 'A comma separated list of modules force deploy to stg')
    }

    stages {
        stage ("Clear prior build cruft") {
            steps {
                deleteDir()
                checkout scm
            }
        }
        stage ("Analyse") {
            steps {
                container('cloc') {
                  sh 'cloc --exclude-list-file=.clocignore --by-file --xml --out=cloc.xml .'
                }
            }
        }

        stage ("Publish reports") {
            steps {
                sloccountPublish encoding: '', pattern: '**/cloc.xml'
            }
        }

        stage("Package/Assembly") {
            steps {
                //script {
                //  parallel(buildParallelPomBuilds("-DskipTests clean install"))
                //  parallel(buildParallelPomBuilds("-Pscala-2.12 -Dscala-2.12 -DskipTests clean install"))
                //}
                sh 'mvn -T2C -DskipTests clean install'
                sh 'mvn -T2C -Pscala-2.12 -Dscala-2.12 -DskipTests clean install'
            }
        }


        stage("Unit Tests") {
            environment {
                VAULT_TOKEN = credentials("vault-stg-token")
                APX_VAULT_TOKEN = credentials("vault-stg-token")
            }
            steps {
                script {
                    if (params.FORCE_SKIP_TESTS) {
                        slackSend botUser: true, channel: "${SLACK_CHANNEL}", message: "FORCE_SKIP_TESTS for `${REPO_NAME}` for ${env.GIT_BRANCH}"
                    } else {
                        sh 'curl https://mapping-stg.apixio.com:8443/mapping/v0/code > model/src/test/resources/code-mapping.json'
                        sh 'curl https://mapping-stg.apixio.com:8443/mapping/v0/icd > model/src/test/resources/icd-model.json'
                        sh 'curl https://mapping-stg.apixio.com:8443/mapping/v0/hcc > model/src/test/resources/hcc-model.json'
                        sh 'curl https://mapping-stg.apixio.com:8443/mapping/v0/cpt > model/src/test/resources/cpt-model.json'
                        sh 'curl https://mapping-stg.apixio.com:8443/mapping/v0/measure > model/src/test/resources/qualitymeasure-model.json'
                        sh 'curl https://mapping-stg.apixio.com:8443/mapping/v0/fact > model/src/test/resources/qualityfact-model.json'
                        sh 'curl https://mapping-stg.apixio.com:8443/mapping/v0/conjecture > model/src/test/resources/qualityconjecture-model.json'
                        sh 'curl https://mapping-stg.apixio.com:8443/mapping/v0/billtype > model/src/test/resources/billtype-model.json'
                        sh 'curl https://mapping-stg.apixio.com:8443/mapping/v0/riskgroups > model/src/test/resources/riskgroups-model.json'
                        sh 'curl https://mapping-stg.apixio.com:8443/mapping/v2/code > model/src/test/resources/code-mapping-v2.json'
                        sh 'curl https://mapping-stg.apixio.com:8443/mapping/v2/icd > model/src/test/resources/icd-model-v2.json'
                        sh 'curl https://mapping-stg.apixio.com:8443/mapping/v2/hcc > model/src/test/resources/hcc-model-v2.json'
                        sh 'curl https://mapping-stg.apixio.com:8443/mapping/v2/cpt > model/src/test/resources/cpt-model-v2.json'


                        // For Ghostcript ImageConvertToolsTest:
                        // Duy installed libgs-devel on the Jenkins base container 04/18/2022
                        // sh 'rm -rf ghostscript; mkdir ghostscript && curl --insecure https://coordinator-dev.apixio.com:7066/resources/ocr/ghostscript-9.19-all-20170110.tar.gz | tar xzf - -C ghostscript'
                        // sh 'export LD_LIBRARY_PATH=../ghostscript/ghostscript-9.19/libs:../ghostscript/ghostscript-9.19/deplibs'


                        parallel(buildParallelPomBuilds([
                          "-X -e clean test",
                           "-X -Pscala-2.12 -Dscala-2.12 -e clean test"
                        ]))
                    }
                }
            }
            post {
                always {
                    jacoco()
                    // sh 'export -n LD_LIBRARY_PATH'
                }
            }
        }

        stage("Integration Tests") {
            environment {
                VAULT_TOKEN = credentials("vault-stg-token")
                APX_VAULT_TOKEN = credentials("vault-stg-token")
            }
            steps {
                script {
                    if (params.FORCE_SKIP_TESTS) {
                        slackSend botUser: true, channel: "${SLACK_CHANNEL}", message: "FORCE_SKIP_TESTS for `${REPO_NAME}` for ${env.GIT_BRANCH}"
                    } else {
                        parallel(buildParallelPomBuilds([
                          "-X -e -P IntegrationTests clean test",
                          "-Pscala-2.12 -Dscala-2.12 -X -e -P IntegrationTests clean test"
                        ]))
                        // sh 'mvn -X -e -P IntegrationTests -T1C clean test'
                        // sh 'mvn -Pscala-2.12 -Dscala-2.12 -X -e -P IntegrationTests -T1C clean test'
                    }
                }
            }
        }

        stage("Publish SNAPSHOT from dev") {
            when {
                anyOf {
                    branch 'dev'
                    expression { return params.PUBLISH_SNAPSHOT }
                }
            }
            steps {
                sh 'mvn -f pom.xml -DskipTests=true -Dmaven.install.skip=true -T4C -DaltDeploymentRepository=apixio.snapshots.build::default::https://repos.apixio.com/nexus/content/repositories/snapshots/ deploy'
                script {
                    version = sh returnStdout: true, script: "mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep -v '^\\[' | tail -n1"
                }
                sh 'mvn -f pom.xml -Pscala-2.12 -Dscala-2.12 -DskipTests=true -Dmaven.install.skip=true -T4C -DaltDeploymentRepository=apixio.snapshots.build::default::https://repos.apixio.com/nexus/content/repositories/snapshots/ deploy'
                script {
                    version = sh returnStdout: true, script: "mvn -Pscala-2.12 -Dscala-2.12 org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep -v '^\\[' | tail -n1"
                }
                sh '''
                    cd schemas/target/python
                    python setup.py sdist
                    twine upload dist/*.tar.gz -r artifactory
                '''
                script {
                  parallel(buildParallelDockerBuilds(version))
                }
                slackSend botUser: true, channel: "${SLACK_CHANNEL}", message: "Publishing SNAPSHOT of `${REPO_NAME}` $version for ${env.GIT_BRANCH} published to Artifactory"
            }
        }

        stage("Publish Release from master") {
            when {
                branch 'master'
            }
            steps {
                sh 'mvn -f pom.xml -DskipTests=true -Dmaven.install.skip=true -T4C -DaltDeploymentRepository=apixio.releases.build::default::https://repos.apixio.com/nexus/content/repositories/releases/ deploy'
                script {
                    version = sh returnStdout: true, script: "mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep -v '^\\[' | tail -n1"
                }
                sh 'mvn -f pom.xml -Pscala-2.12 -Dscala-2.12 -DskipTests=true -Dmaven.install.skip=true -T4C -DaltDeploymentRepository=apixio.releases.build::default::https://repos.apixio.com/nexus/content/repositories/releases/ deploy'
                script {
                    version = sh returnStdout: true, script: "mvn -Pscala-2.12 -Dscala-2.12 org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep -v '^\\[' | tail -n1"
                }
                sh '''
                    cd schemas/target/python
                    python setup.py sdist
                    twine upload dist/*.tar.gz -r artifactory
                '''
                script {
                  parallel(buildParallelDockerBuilds(version))
                }
                slackSend botUser: true, channel: "${SLACK_CHANNEL}", message: "Publishing Release to Artifactory of `${REPO_NAME}` $version for ${env.GIT_BRANCH}"
            }
        }
    }
}
