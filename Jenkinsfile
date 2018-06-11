pipeline {
  agent any

  environment {
    PROJECT_NAME = 'runtime'
    BUILD_OUTPUT_FILE = "${WORKSPACE}/build.output"
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
          mvn build-helper:parse-version versions:set -DnewVersion=${env.BRANCH_NAME}

          mvn -T 4 install -Dmaven.test.failure.ignore=true

          echo `ls nifi-assembly/target/ | grep '.tar.gz\$'` > compile_target
        """

        script {
          env.compile_target = readFile("compile_target").trim()
        }
        // compile_target = 'runtime-1.7.0-SNAPSHOT-bin.tar.gz'

        slackSend (
          color: 'good',
          message: "${env.EMOJI_SERVICE_RUNTIME} *Compile Finished*\nJenkins Job `${env.JOB_NAME}`, Build Number `${env.BUILD_NUMBER}`\nGenerated `${env.compile_target}`"
        )
      }
    }

    stage('Copy to Ansible host') {
      steps {
        slackSend (
          color: 'good',
          message: "${env.EMOJI_SERVICE_RUNTIME} *Copy to Ansible host*\nJenkins Job *`${env.JOB_NAME}`*, Build Number `${env.BUILD_NUMBER}`"
        )

        // nifi-assembly/target/runtime-1.7.0-SNAPSHOT-bin.tar.gz
        sh """
          scp nifi-assembly/target/${env.compile_target} root@${env.ANSIBLE_DEPLOY_HOST}:${env.ANSIBLE_DEVOPS_PATH}/ansible/packages/services/
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
          ssh root@${env.ANSIBLE_DEPLOY_HOST} "cd ${env.ANSIBLE_DEVOPS_PATH} && python update.py --name orchsym-dev --service ${env.PROJECT_NAME}/${env.RUNTIME_DEV_VERSION} --skip-download"
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

    stage('Upload to Samba') {
      steps {
        slackSend (
          color: 'good',
          message: "${env.EMOJI_SERVICE_RUNTIME} *Upload to Samba*\nJenkins Job `${env.JOB_NAME}`, Build Number `${env.BUILD_NUMBER}`"
        )

        sh """
          sudo rsync --progress ${env.WORKSPACE}/nifi-assembly/target/${env.compile_target} ${env.SAMBA_LOCAL_MOUNT_PATH}/${env.SAMBA_UPLOAD_PATH_RUNTIME}/
        """

        slackSend (
          color: 'good',
          message: "${env.EMOJI_SERVICE_RUNTIME} *Upload to Samba Finished*\nJenkins Job `${env.JOB_NAME}`, Build Number `${env.BUILD_NUMBER}`\nYou can get it from Samba Server `//${env.SAMBA_SERVER}/${env.SAMBA_UPLOAD_PATH_RUNTIME}/${env.compile_target}`"
        )
      }
    }

    stage('Upload to s3') {
      steps {
        slackSend (
          color: 'good',
          message: "${env.EMOJI_SERVICE_RUNTIME}*Upload to S2*\nJenkins Job `${env.JOB_NAME}`, Build Number `${env.BUILD_NUMBER}`"
        )

        sh """
          s3cmd put --acl-public ${env.WORKSPACE}/nifi-assembly/target/${env.compile_target} ${env.S3_PACKAGES_URL}/services/
        """

        slackSend (
          color: 'good',
          message: "${env.EMOJI_SERVICE_RUNTIME} *Upload to S2 Finished*\nJenkins Job `${env.JOB_NAME}`, Build Number `${env.BUILD_NUMBER}`\nYou can download it from ${env.DOWNLOAD_PACKAGES_URL_BASE}/services/${env.compile_target}"
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
