pipeline {
  agent any

  environment {
    PROJECT_NAME = 'runtime'
    BUILD_OUTPUT_FILE = "${WORKSPACE}/build.output"
  }

  triggers { pollSCM('0 1 * * *') }

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
          echo "Build based on last commit: ${env.GIT_COMMIT}" >> ${env.BUILD_OUTPUT_FILE}
          echo >> ${env.BUILD_OUTPUT_FILE}
        """

        slackSend ( color: 'good', message: "*Build Started* Jenkins Job `${env.JOB_NAME}`, Build Number `${env.BUILD_NUMBER}`" )
      }
    }

    stage('Build') {
      steps {
        sh """
          mvn -T 4 clean install -Dmaven.test.failure.ignore=true

          echo `ls nifi-assembly/target/ | grep '.tar.gz\$'` > compile_target
        """

        script {
          env.compile_target = readFile("compile_target").trim()
        }

        slackSend ( color: 'good', message: "*Compile Finished* Jenkins Job `${env.JOB_NAME}`, Build Number `${env.BUILD_NUMBER}`\nGenerated `${env.compile_target}`" )
      }
    }

    stage('Upload to s3') {
      steps {
        slackSend ( color: 'good', message: "*Uploading* to S2. Jenkins Job `${env.JOB_NAME}`, Build Number `${env.BUILD_NUMBER}`" )

        sh """
          s3cmd put --acl-public ${env.WORKSPACE}/nifi-assembly/target/${env.compile_target} ${env.S3_PACKAGES_URL}/files/
        """

        slackSend ( color: 'good', message: "*Upload Finished* Jenkins Job `${env.JOB_NAME}`, Build Number `${env.BUILD_NUMBER}`\nYou can download it from ${env.DOWNLOAD_FILE_URL_BASE}/${env.compile_target} " )
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

    success {
      echo "Sending message to Slack"
      slackSend ( color: 'good', message: "*Build Succceed* Jenkins Job `${env.JOB_NAME}`, Build Number `${env.BUILD_NUMBER}`\nSee: ${env.BUILD_URL}\n${env.build_output}" )
    }

    failure {
      echo "Sending message to Slack"
      slackSend ( color: 'danger', message: "*Build Failed* Jenkins Job `${env.JOB_NAME}`, Build Number `${env.BUILD_NUMBER}`\nSee: ${env.BUILD_URL}" )
    }
  }
}
