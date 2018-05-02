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
          echo "Build based on last commit: ${env.GIT_COMMIT}" >> ${env.BUILD_OUTPUT_FILE}
          echo >> ${env.BUILD_OUTPUT_FILE}
        """

        slackSend ( color: 'good', message: "*Build Started* Jenkins Job `${env.JOB_NAME}`, Build Number `${env.BUILD_NUMBER}`" )
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

        slackSend ( color: 'good', message: "*Compile Finished* Jenkins Job `${env.JOB_NAME}`, Build Number `${env.BUILD_NUMBER}`\nGenerated `${env.compile_target}`" )
      }
    }

    stage('Deploy') {
      steps {
        slackSend ( color: 'good', message: "*Deploying/Updating the NIFI `test` Environment* Jenkins Job `${env.JOB_NAME}`, Build Number `${env.BUILD_NUMBER}`\nOpen `${env.JOB_URL}${env.BUILD_NUMBER}/input/` and click `Proceed` or `Abort`" )
        input ( id: "CONTINUE", message: 'Continue to Deploy...' )

        sshPublisher(
          publishers: [
            sshPublisherDesc(
              configName: 'nifi_deploy_1',
              transfers: [
                sshTransfer(
                  excludes: '',
                  execCommand: "cd ${env.JENKINS_SSH_UPLOAD_DIR} && tar -xzvf ${env.compile_target} -C ${env.ORCHSYM_INSTALL_BASE_DIR}/; cd ${env.ORCHSYM_INSTALL_BASE_DIR}/runtime && bash bin/nifi.sh  restart",
                  execTimeout: 120000,
                  flatten: false,
                  makeEmptyDirs: false,
                  noDefaultExcludes: false,
                  patternSeparator: '[, ]+',
                  remoteDirectory: '',
                  remoteDirectorySDF: false,
                  removePrefix: 'nifi-assembly/target/',
                  //sourceFiles: "nifi-assembly/target/runtime-1.7.0-SNAPSHOT-bin.tar.gz"
                  sourceFiles: "nifi-assembly/target/${env.compile_target}"
                )
              ],
              usePromotionTimestamp: false,
              useWorkspaceInPromotion: false,
              verbose: false
            )
          ]
        )

        slackSend( color: 'good', message: "NIFI `test` Environment has been updated!")
      }
    }


    stage('Upload to Samba') {
      steps {
        slackSend ( color: 'good', message: "*Uploading to Samba* Jenkins Job `${env.JOB_NAME}`, Build Number `${env.BUILD_NUMBER}`" )

        sh """
          sudo rsync --progress ${env.WORKSPACE}/nifi-assembly/target/${env.compile_target} ${env.SAMBA_LOCAL_MOUNT_PATH}/${env.SAMBA_UPLOAD_PATH_RUNTIME}/
        """

        slackSend ( color: 'good', message: "*Upload to Samba Finished* Jenkins Job `${env.JOB_NAME}`, Build Number `${env.BUILD_NUMBER}`\nYou can get it from Samba Server `//${env.SAMBA_SERVER}/${env.SAMBA_UPLOAD_PATH_RUNTIME}/${env.compile_target}` " )
      }
    }

    stage('Upload to s3') {
      steps {
        slackSend ( color: 'good', message: "*Uploading to S2* Jenkins Job `${env.JOB_NAME}`, Build Number `${env.BUILD_NUMBER}`" )

        sh """
          s3cmd put --acl-public ${env.WORKSPACE}/nifi-assembly/target/${env.compile_target} ${env.S3_PACKAGES_URL}/files/
        """

        slackSend ( color: 'good', message: "*Upload to S2 Finished* Jenkins Job `${env.JOB_NAME}`, Build Number `${env.BUILD_NUMBER}`\nYou can download it from ${env.DOWNLOAD_FILE_URL_BASE}/${env.compile_target} " )
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
