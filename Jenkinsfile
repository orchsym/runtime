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

        slackSend ( color: 'good', message: "*Build Started, based on commit: ${env.GIT_COMMIT}* Jenkins Job `${env.JOB_NAME}`, Build Number `${env.BUILD_NUMBER}`" )
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

    stage('Deploy to Dev') {
      when { branch 'develop' }

      steps {
        slackSend ( color: 'good', message: "*Deploying/Updating the runtime `Dev` Environment (${env.ORCHSYM_DEV_ENVIRONMENT}), using branch* Jenkins Job `${env.JOB_NAME}`, Build Number `${env.BUILD_NUMBER}`\nOpen `${env.JOB_URL}${env.BUILD_NUMBER}/input/` and click `Proceed` or `Abort`" )

        // input ( id: "CONTINUE", message: 'Continue to Deploy to Dev...' )

        sshPublisher(
          publishers: [
            sshPublisherDesc(
              configName: 'runtime_dev_environment',
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

        slackSend( color: 'good', message: "runtime `Dev` Environment (${env.ORCHSYM_DEV_ENVIRONMENT}) has been updated!")
      }
    }

    stage('Deploy to Test') {
      when { branch 'master' }

      steps {
        slackSend ( color: 'good', message: "*Deploying/Updating the runtime `Test` Environment (${env.RUNTIME_Test_ENVIRONMENT})* Jenkins Job `${env.JOB_NAME}`, Build Number `${env.BUILD_NUMBER}`\nOpen `${env.JOB_URL}${env.BUILD_NUMBER}/input/` and click `Proceed` or `Abort`" )

        input ( id: "CONTINUE", message: "Continue to Deploy to Test... " )

        sshPublisher(
          publishers: [
            sshPublisherDesc(
              configName: 'runtime_test_environment',
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

        slackSend( color: 'good', message: "runtime `Test` Environment has been updated!")
      }
    }


    stage('Deploy to Stage') {
      when { branch 'master' }

      steps {
        echo "TODO"
      }
    }

    stage('Deploy to Prod') {
      when { branch 'master' }

      steps {
        echo "TODO"
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
          s3cmd put --acl-public ${env.WORKSPACE}/nifi-assembly/target/${env.compile_target} ${env.S3_PACKAGES_URL}/services/
        """

        slackSend ( color: 'good', message: "*Upload to S2 Finished* Jenkins Job `${env.JOB_NAME}`, Build Number `${env.BUILD_NUMBER}`\nYou can download it from ${env.DOWNLOAD_PACKAGES_URL_BASE}/services/${env.compile_target} " )
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
