pipeline {
  agent any

  environment {
    PROJECT_NAME = 'runtime'
    BUILD_OUTPUT_FILE = "${WORKSPACE}/build.output"
    RUNTIME_DEV_VERSION = 'master'
    RUNTIME_TEST_VERSION = 'master'
    RUNTIME_STAGE_VERSION = 'master'
    RUNTIME_PROD_VERSION = 'master'
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
          mvn -T 4 install -Dmaven.test.failure.ignore=true

          echo `ls nifi-assembly/target/ | grep '.tar.gz\$'` > compile_target
        """

        script {
          env.compile_target = readFile("compile_target").trim()
        }

        slackSend (
          color: 'good',
          message: "${env.EMOJI_SERVICE_RUNTIME} *Compile Finished*\nJenkins Job `${env.JOB_NAME}`, Build Number `${env.BUILD_NUMBER}`\nGenerated `${env.compile_target}`"
        )
      }
    }

    stage('Deploy to Dev') {
      when { branch "${env.RUNTIME_DEV_VERSION}" }

      steps {
        slackSend (
          color: 'good',
          message: "${env.EMOJI_SERVICE_RUNTIME} *Updating the Orchsym runtime `Dev` Environment (`${env.RUNTIME_DEV_ENVIRONMENT}`)* ${env.EMOJI_DEPLOY_START}\nJenkins Job *`${env.JOB_NAME}`*, Build Number `${env.BUILD_NUMBER}`"
        )

        // nifi-assembly/target/runtime-1.7.0-SNAPSHOT-bin.tar.gz
        sh """
          scp nifi-assembly/target/${env.compile_target} root@${env.RUNTIME_DEV_ENVIRONMENT}:/data/packages/services/
          tar -xzvf /data/packages/services/${env.compile_target} -C ${env.ORCHSYM_INSTALL_BASE_DIR}/
          cd ${env.ORCHSYM_INSTALL_BASE_DIR}/runtime && bash bin/nifi.sh  restart
        """

        slackSend(
          color: 'good',
          message: "${env.EMOJI_SERVICE_RUNTIME} *Orchsym runtime `Dev` Environment (`${env.RUNTIME_DEV_ENVIRONMENT}`) has been updated! ${env.EMOJI_DEPLOY_SUCCESS}*\nJenkins Job *`${env.JOB_NAME}`*, Build Number `${env.BUILD_NUMBER}`"
        )
      }
    }

    stage('Deploy to Test') {
      when { branch "${env.RUNTIME_TEST_VERSION}" }

      steps {
        slackSend (
          color: 'good',
          message: "${env.EMOJI_SERVICE_RUNTIME} *Updating the Orchsym runtime `Test` Environment (`${env.RUNTIME_TEST_ENVIRONMENT}`)* ${env.EMOJI_DEPLOY_START}\nJenkins Job *`${env.JOB_NAME}`*, Build Number `${env.BUILD_NUMBER}`"
        )

        // nifi-assembly/target/runtime-1.7.0-SNAPSHOT-bin.tar.gz
        sh """
          scp nifi-assembly/target/${env.compile_target} root@${env.RUNTIME_TEST_ENVIRONMENT}:/data/packages/services/
          tar -xzvf /data/packages/services/${env.compile_target} -C ${env.ORCHSYM_INSTALL_BASE_DIR}/
          cd ${env.ORCHSYM_INSTALL_BASE_DIR}/runtime && bash bin/nifi.sh  restart
        """

        slackSend(
          color: 'good',
          message: "${env.EMOJI_SERVICE_RUNTIME} *Orchsym runtime `Test` Environment (`${env.RUNTIME_TEST_ENVIRONMENT}`) has been updated! ${env.EMOJI_DEPLOY_SUCCESS}*\nJenkins Job *`${env.JOB_NAME}`*, Build Number `${env.BUILD_NUMBER}`"
        )
      }
    }


    stage('Deploy to Stage') {
      when { branch "${env.RUNTIME_STAGE_VERSION}" }

      steps {
        echo "TODO"
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
          message: "${env.EMOJI_SERVICE_RUNTIME} *Uploading to Samba*\nJenkins Job `${env.JOB_NAME}`, Build Number `${env.BUILD_NUMBER}`"
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
          message: "${env.EMOJI_SERVICE_RUNTIME}*Uploading to S2*\nJenkins Job `${env.JOB_NAME}`, Build Number `${env.BUILD_NUMBER}`"
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
        message: "${env.EMOJI_SERVICE_RUNTIME} *Build Succceed* ${env.EMOJI_BUILD_SUCCESS} Jenkins Job *`${env.JOB_NAME}`*, Build Number `${env.BUILD_NUMBER}`\nSee: ${env.BUILD_URL}\n${env.build_output}"
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
