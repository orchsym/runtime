pipeline {
  agent any

  environment {
    PROJECT_NAME = 'runtime'
    BUILD_OUTPUT_FILE = "${WORKSPACE}/build.output"
    VERSION_NAME= "1.7.0-${BRANCH_NAME}"
  }

  tools {
    // the 'Maven' is the name pre-configured in Global Tool Configuration
    maven 'Maven'
  }

  stages {
    stage('NOCI check') {
      steps {
        script {
          noci action: 'check'
        }
      }
    }

    stage('Print information') {
      steps {
        sh """
          printenv|sort
          rm -rf ${env.BUILD_OUTPUT_FILE} || true
          echo "Build based on last commit: ${env.GIT_COMMIT}\n" >> ${env.BUILD_OUTPUT_FILE}
        """

        slackSend (
          color: 'good',
          message: "${env.EMOJI_SERVICE_RUNTIME} *Build Started ${env.EMOJI_BUILD_START} commit: ${env.GIT_COMMIT}*\nJenkins Job *`${env.JOB_NAME}`*, Build Number `${env.BUILD_NUMBER}`\nSee: ${env.BUILD_URL}"
        )
      }
    }

    stage('Build') {
      steps {
        sh """
          mvn clean
          mvn build-helper:parse-version versions:set -DgenerateBackupPoms=false -DnewVersion=${env.VERSION_NAME}
          mvn -T 4 install -Dmaven.test.failure.ignore=true
          echo ${env.PROJECT_NAME}-${env.VERSION_NAME}.tar.gz >> ${env.BUILD_OUTPUT_FILE}
        """

        slackSend (
          color: 'good',
          message: "${env.EMOJI_SERVICE_RUNTIME} *Compile Finished*\nJenkins Job `${env.JOB_NAME}`, Build Number `${env.BUILD_NUMBER}`\nGenerated `${env.PROJECT_NAME}-${env.VERSION_NAME}.tar.gz`"
        )
      }
    }

    stage('Copy to Ansible host') {
      when { not { expression { BRANCH_NAME ==~ '^PR.*' } } }

      steps {
        slackSend (
          color: 'good',
          message: "${env.EMOJI_SERVICE_RUNTIME} *Copy to Ansible host*\nJenkins Job *`${env.JOB_NAME}`*, Build Number `${env.BUILD_NUMBER}`"
        )

        sh """
          scp nifi-assembly/target/${env.PROJECT_NAME}-${env.VERSION_NAME}.tar.gz root@${env.ANSIBLE_DEPLOY_HOST}:${env.ANSIBLE_DEVOPS_PATH}/ansible/packages/services/
        """

        slackSend(
          color: 'good',
          message: "${env.EMOJI_SERVICE_RUNTIME} *Copy finished*\nJenkins Job *`${env.JOB_NAME}`*, Build Number `${env.BUILD_NUMBER}`"
        )
      }
    }

    stage('Deploy to Dev') {
      when { branch "${env.RUNTIME_DEV_VERSION}" }

      steps {
        slackSend (
          color: 'good',
          message: "${env.EMOJI_SERVICE_RUNTIME} *Updating `Dev` (`${env.ORCHSYM_DEV_ENVIRONMENT}`)* ${env.EMOJI_DEPLOY_START}\nJenkins Job *`${env.JOB_NAME}`*, Build Number `${env.BUILD_NUMBER}`"
        )

        sh """
          ssh root@${env.ANSIBLE_DEPLOY_HOST} "cd ${env.ANSIBLE_DEVOPS_PATH} && python update.py --name orchsym-dev --service ${env.PROJECT_NAME}/${env.VERSION_NAME}"
        """

        slackSend(
          color: 'good',
          message: "${env.EMOJI_SERVICE_RUNTIME} *Updated `Dev` (`${env.ORCHSYM_DEV_ENVIRONMENT}`)* ${env.EMOJI_DEPLOY_SUCCESS}\nJenkins Job *`${env.JOB_NAME}`*, Build Number `${env.BUILD_NUMBER}`"
        )
      }
    }

    stage('Deploy to Test') {
      when { branch "${env.RUNTIME_TEST_VERSION}" }

      steps {
        echo "TODO"
      }
    }

    stage('Deploy to Stage') {
      when { branch "${env.RUNTIME_STAGE_VERSION}" }

      steps {
        slackSend (
          color: 'good',
          message: "${env.EMOJI_SERVICE_RUNTIME} *Updating `Stage` (`${env.ORCHSYM_STAGE_ENVIRONMENT}`)* ${env.EMOJI_DEPLOY_START}\nJenkins Job *`${env.JOB_NAME}`*, Build Number `${env.BUILD_NUMBER}`"
        )

        sh """
          ssh root@${env.ANSIBLE_DEPLOY_HOST} "cd ${env.ANSIBLE_DEVOPS_PATH} && python update.py --name orchsym-stage --service ${env.PROJECT_NAME}/${env.RUNTIME_STAGE_VERSION} --skip-download"
        """

        slackSend(
          color: 'good',
          message: "${env.EMOJI_SERVICE_RUNTIME} *Updated `Stage` (`${env.ORCHSYM_STAGE_ENVIRONMENT}`)* ${env.EMOJI_DEPLOY_SUCCESS}\nJenkins Job *`${env.JOB_NAME}`*, Build Number `${env.BUILD_NUMBER}`"
        )
      }
    }

    stage('Deploy to Prod') {
      when { branch "${env.RUNTIME_PROD_VERSION}" }

      steps {
        echo "TODO"
      }
    }

    stage('Build/Push docker image') {
      when { not { expression { BRANCH_NAME ==~ '^PR.*' } } }

      steps {
        slackSend(
          color: 'good',
          message: "${env.EMOJI_SERVICE_RUNTIME} *Build/Push docker image*\nJenkins Job *`${env.JOB_NAME}`*, Build Number `${env.BUILD_NUMBER}`"
        )

        sh "echo Generated Docker Images: >> ${env.BUILD_OUTPUT_FILE}"

        script {
          docker.withRegistry("http://${env.DOCKER_REGISTRY_ADDR2}", "${env.DOCKER_REGISTRY_SECRET_ID2}") {
            image = docker.build("${env.DOCKER_REGISTRY_ADDR2}/${env.DOCKER_REGISTRY_PROJECT_NAME}/${env.PROJECT_NAME}:${env.BRANCH_NAME}",  "--build-arg VERSION_NAME=${env.VERSION_NAME} --pull -f Dockerfile .")
            image.push()
            sh "echo ${env.DOCKER_REGISTRY_ADDR2}/${env.DOCKER_REGISTRY_PROJECT_NAME}/${env.PROJECT_NAME}:${env.VERSION_NAME} >> ${env.BUILD_OUTPUT_FILE}"
            if (env.BRANCH_NAME == 'master') {
              image.push('latest')
              sh "echo ${env.DOCKER_REGISTRY_ADDR2}/${env.DOCKER_REGISTRY_PROJECT_NAME}/${env.PROJECT_NAME}:latest >> ${env.BUILD_OUTPUT_FILE}"
            }
          }
        }

        slackSend(
          color: 'good',
          message: "${env.EMOJI_SERVICE_RUNTIME} *Build/Push docker image finished*\nJenkins Job *`${env.JOB_NAME}`*, Build Number `${env.BUILD_NUMBER}`"
        )
      }
    }

    stage('Upload to Samba') {
      when { not { expression { BRANCH_NAME ==~ '^PR.*' } } }

      steps {
        slackSend (
          color: 'good',
          message: "${env.EMOJI_SERVICE_RUNTIME} *Upload to Samba*\nJenkins Job `${env.JOB_NAME}`, Build Number `${env.BUILD_NUMBER}`"
        )

        sh """
          sudo rsync --progress ${env.WORKSPACE}/nifi-assembly/target/${env.PROJECT_NAME}-${env.VERSION_NAME}.tar.gz ${env.SAMBA_LOCAL_MOUNT_PATH}/${env.SAMBA_UPLOAD_PATH_RUNTIME}/
        """

        slackSend (
          color: 'good',
          message: "${env.EMOJI_SERVICE_RUNTIME} *Upload to Samba Finished*\nJenkins Job `${env.JOB_NAME}`, Build Number `${env.BUILD_NUMBER}`\nYou can get it from Samba Server `//${env.SAMBA_SERVER}/${env.SAMBA_UPLOAD_PATH_RUNTIME}/${env.PROJECT_NAME}-${env.VERSION_NAME}.tar.gz`"
        )
      }
    }

    stage('Upload to s3') {
      when { not { expression { BRANCH_NAME ==~ '^PR.*' } } }

      steps {
        slackSend (
          color: 'good',
          message: "${env.EMOJI_SERVICE_RUNTIME}*Upload to S2*\nJenkins Job `${env.JOB_NAME}`, Build Number `${env.BUILD_NUMBER}`"
        )

        sh """
          s3cmd put --acl-public ${env.WORKSPACE}/nifi-assembly/target/${env.PROJECT_NAME}-${env.VERSION_NAME}.tar.gz ${env.S3_PACKAGES_URL}/services/
        """

        slackSend (
          color: 'good',
          message: "${env.EMOJI_SERVICE_RUNTIME} *Upload to S2 Finished*\nJenkins Job `${env.JOB_NAME}`, Build Number `${env.BUILD_NUMBER}`\nYou can download it from ${env.DOWNLOAD_PACKAGES_URL_BASE}/services/${env.PROJECT_NAME}-${env.VERSION_NAME}.tar.gz"
        )
      }
    }

    stage('Capture Output') {
      steps {
        script {
          env.build_output = readFile "build.output"
        }
      }
    }
  }

  post {
    always {
      noci action: 'postProcess'
    }

    aborted {
      slackSend (
        color: 'good',
        message: "${env.EMOJI_SERVICE_RUNTIME} *Build Aborted* Jenkins Job *`${env.JOB_NAME}`*, Build Number `${env.BUILD_NUMBER}`\nSee: ${env.BUILD_URL}\n${env.build_output}"
      )
    }

    unstable {
      slackSend (
        color: 'good',
        message: "${env.EMOJI_SERVICE_RUNTIME} *Build Unstable* Jenkins Job *`${env.JOB_NAME}`*, Build Number `${env.BUILD_NUMBER}`\nSee: ${env.BUILD_URL}\n${env.build_output}"
      )
    }

    success {
      slackSend (
        color: 'good',
        message: "${env.EMOJI_SERVICE_RUNTIME} *Build Succeed* ${env.EMOJI_BUILD_SUCCESS} Jenkins Job *`${env.JOB_NAME}`*, Build Number `${env.BUILD_NUMBER}`\nSee: ${env.BUILD_URL}\n${env.build_output}"
      )
    }


    failure {
      slackSend (
        color: 'danger',
        message: "${env.EMOJI_SERVICE_RUNTIME} *Build Failed* ${env.EMOJI_BUILD_FAILURE} Jenkins Job *`${env.JOB_NAME}`*, Build Number `${env.BUILD_NUMBER}`\nSee: ${env.BUILD_URL}"
      )
    }

  }
}
