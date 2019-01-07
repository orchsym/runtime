pipeline {
  agent any

  environment {
    PROJECT_NAME = 'runtime'
    BUILD_OUTPUT_FILE = "${WORKSPACE}/build.output"
    BUILD_VERSION_NAME= "1.7.1-${BRANCH_NAME}"
    VERSION_NAME= "${BRANCH_NAME}"
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
          echo "\nBuild based on last commit: ${env.GIT_COMMIT}\n" >> ${env.BUILD_OUTPUT_FILE}
        """

        dingTalk (
          accessToken: "${env.DINGDING_ACCESS_TOKEN}",
          message: "### ${env.RUNTIME_EMOJI} [${JOB_NAME}${BUILD_DISPLAY_NAME}](${BUILD_URL}) \n\n **Build Started** ${env.EMOJI_BUILD_START} \n\n commit: ${env.GIT_COMMIT}",
        )
      }
    }

    stage('Compile') {
      steps {
        sh """
          mvn clean
          mvn build-helper:parse-version versions:set -DgenerateBackupPoms=false -DnewVersion=${env.BUILD_VERSION_NAME}
          mvn -T 4 install -Dmaven.test.failure.ignore=true
          echo "\n${env.PROJECT_NAME}-${env.VERSION_NAME}.tar.gz" >> ${env.BUILD_OUTPUT_FILE}
        """

        dingTalk (
          accessToken: "${env.DINGDING_ACCESS_TOKEN}",
          message: "### ${env.RUNTIME_EMOJI} [${JOB_NAME}${BUILD_DISPLAY_NAME}](${BUILD_URL}) \n\n **Compile finished** ${env.EMOJI_BUILD_START} \n\n commit: ${env.GIT_COMMIT}",
        )
      }
    }

    stage('Copy to Ansible host') {
      when { not { expression { BRANCH_NAME ==~ '^PR.*' } } }

      steps {
        sh """
          scp orchsym/orchsym-assembly/target/${env.PROJECT_NAME}-${env.VERSION_NAME}.tar.gz root@${env.ANSIBLE_DEPLOY_HOST}:${env.ANSIBLE_DEVOPS_PATH}/ansible/packages/services/
        """

        dingTalk (
          accessToken: "${env.DINGDING_ACCESS_TOKEN}",
          message: "### ${env.RUNTIME_EMOJI} [${JOB_NAME}${BUILD_DISPLAY_NAME}](${BUILD_URL}) \n\n **Copy finished**",
        )
      }
    }

    stage('Deploy to Dev') {
      when { branch "${env.RUNTIME_DEV_VERSION}" }

      steps {
        dingTalk (
          accessToken: "${env.DINGDING_ACCESS_TOKEN}",
          message: "### ${env.RUNTIME_EMOJI} [${JOB_NAME}${BUILD_DISPLAY_NAME}](${BUILD_URL}) \n\n **Updating `Dev` (`${env.RUNTIME_DEV_ENVIRONMENT}`)**",
        )

        sh """
          ssh root@${env.ANSIBLE_DEPLOY_HOST} "cd ${env.ANSIBLE_DEVOPS_PATH} && python update.py --name ${env.RUNTIME_DEV_NAME} --service ${env.PROJECT_NAME}/${env.VERSION_NAME}"
        """

        dingTalk (
          accessToken: "${env.DINGDING_ACCESS_TOKEN}",
          message: "### ${env.RUNTIME_EMOJI} [${JOB_NAME}${BUILD_DISPLAY_NAME}](${BUILD_URL}) \n\n **Updated `Dev` (`${env.RUNTIME_DEV_ENVIRONMENT}`)**",
        )
      }
    }

    stage('Deploy to Test') {
      when { branch "${env.RUNTIME_TEST_VERSION}" }

      steps {
        dingTalk (
          accessToken: "${env.DINGDING_ACCESS_TOKEN}",
          message: "### ${env.RUNTIME_EMOJI} [${JOB_NAME}${BUILD_DISPLAY_NAME}](${BUILD_URL}) \n\n **Updating `Test` (`${env.RUNTIME_TEST_ENVIRONMENT}`)**",
        )

        sh """
          ssh root@${env.ANSIBLE_DEPLOY_HOST} "cd ${env.ANSIBLE_DEVOPS_PATH} && python update.py --name ${env.RUNTIME_TEST_NAME} --service ${env.PROJECT_NAME}/${env.VERSION_NAME}"
        """

        dingTalk (
          accessToken: "${env.DINGDING_ACCESS_TOKEN}",
          message: "### ${env.RUNTIME_EMOJI} [${JOB_NAME}${BUILD_DISPLAY_NAME}](${BUILD_URL}) \n\n **Updated `Test` (`${env.RUNTIME_TEST_ENVIRONMENT}`)**",
        )
      }
    }

    stage('Deploy to Stage') {
      when { branch "${env.RUNTIME_STAGE_VERSION}" }

      steps {
        dingTalk (
          accessToken: "${env.DINGDING_ACCESS_TOKEN}",
          message: "### ${env.RUNTIME_EMOJI} [${JOB_NAME}${BUILD_DISPLAY_NAME}](${BUILD_URL}) \n\n **Updating `Stage` (`${env.RUNTIME_STAGE_ENVIRONMENT}`)**",
        )

        sh """
          ssh root@${env.ANSIBLE_DEPLOY_HOST} "cd ${env.ANSIBLE_DEVOPS_PATH} && python update.py --name ${env.RUNTIME_STAGE_NAME} --service ${env.PROJECT_NAME}/${env.VERSION_NAME}"
        """

        dingTalk (
          accessToken: "${env.DINGDING_ACCESS_TOKEN}",
          message: "### ${env.RUNTIME_EMOJI} [${JOB_NAME}${BUILD_DISPLAY_NAME}](${BUILD_URL}) \n\n **Updated `Stage` (`${env.RUNTIME_STAGE_ENVIRONMENT}`)**",
        )
      }
    }

    stage('Deploy to Prod') {
      when { branch "${env.RUNTIME_PROD_VERSION}" }

      steps {
        dingTalk (
          accessToken: "${env.DINGDING_ACCESS_TOKEN}",
          message: "### ${env.RUNTIME_EMOJI} [${JOB_NAME}${BUILD_DISPLAY_NAME}](${BUILD_URL}) \n\n **Updating `Prod` (`${env.RUNTIME_PROD_ENVIRONMENT}`)**",
        )

        sh """
          ssh root@${env.ANSIBLE_DEPLOY_HOST} "cd ${env.ANSIBLE_DEVOPS_PATH} && python update.py --name ${env.RUNTIME_PROD_NAME} --service ${env.PROJECT_NAME}/${env.VERSION_NAME}"
        """

        dingTalk (
          accessToken: "${env.DINGDING_ACCESS_TOKEN}",
          message: "### ${env.RUNTIME_EMOJI} [${JOB_NAME}${BUILD_DISPLAY_NAME}](${BUILD_URL}) \n\n **Updated `Prod` (`${env.RUNTIME_PROD_ENVIRONMENT}`)**",
        )
      }
    }

    stage('Build/Push docker image') {
      when { not { expression { BRANCH_NAME ==~ '^PR.*' } } }

      steps {
        dingTalk (
          accessToken: "${env.DINGDING_ACCESS_TOKEN}",
          message: "### ${env.RUNTIME_EMOJI} [${JOB_NAME}${BUILD_DISPLAY_NAME}](${BUILD_URL}) \n\n **Build/Push docker image**",
        )

        sh "echo '\nGenerated Docker Images:' >> ${env.BUILD_OUTPUT_FILE}"

        script {
          docker.withRegistry("http://${env.DOCKER_REGISTRY_ADDR2}", "${env.DOCKER_REGISTRY_SECRET_ID2}") {
            image = docker.build("${env.DOCKER_REGISTRY_ADDR2}/${env.DOCKER_REGISTRY_PROJECT_NAME}/${env.PROJECT_NAME}:${env.VERSION_NAME}",  "--build-arg VERSION_NAME=${env.VERSION_NAME} --pull -f Dockerfile .")
            image.push()
            sh "echo '\n${env.DOCKER_REGISTRY_ADDR2}/${env.DOCKER_REGISTRY_PROJECT_NAME}/${env.PROJECT_NAME}:${env.VERSION_NAME}' >> ${env.BUILD_OUTPUT_FILE}"
            if (env.BRANCH_NAME == 'master') {
              image.push('latest')
              sh "echo '\n${env.DOCKER_REGISTRY_ADDR2}/${env.DOCKER_REGISTRY_PROJECT_NAME}/${env.PROJECT_NAME}:latest' >> ${env.BUILD_OUTPUT_FILE}"
            }
          }
        }

        dingTalk (
          accessToken: "${env.DINGDING_ACCESS_TOKEN}",
          message: "### ${env.RUNTIME_EMOJI} [${JOB_NAME}${BUILD_DISPLAY_NAME}](${BUILD_URL}) \n\n **Build/Push docker image finished**",
        )
      }
    }

    stage('Upload to Samba') {
      when { not { expression { BRANCH_NAME ==~ '^PR.*' } } }

      steps {
        dingTalk (
          accessToken: "${env.DINGDING_ACCESS_TOKEN}",
          message: "### ${env.RUNTIME_EMOJI} [${JOB_NAME}${BUILD_DISPLAY_NAME}](${BUILD_URL}) \n\n **Upload to Samba**",
        )

        sh """
          sudo rsync --progress ${env.WORKSPACE}/orchsym/orchsym-assembly/target/${env.PROJECT_NAME}-${env.VERSION_NAME}.tar.gz ${env.SAMBA_LOCAL_MOUNT_PATH}/${env.SAMBA_UPLOAD_PATH_RUNTIME}/
        """

        dingTalk (
          accessToken: "${env.DINGDING_ACCESS_TOKEN}",
          message: "### ${env.RUNTIME_EMOJI} [${JOB_NAME}${BUILD_DISPLAY_NAME}](${BUILD_URL}) \n\n **Upload to Samba finished** \n\n You can get it from Samba Server `//${env.SAMBA_SERVER}/${env.SAMBA_UPLOAD_PATH_RUNTIME}/${env.PROJECT_NAME}-${env.VERSION_NAME}.tar.gz`",
        )
      }
    }

    stage('Upload to s3') {
      when { not { expression { BRANCH_NAME ==~ '^PR.*' } } }

      steps {
        dingTalk (
          accessToken: "${env.DINGDING_ACCESS_TOKEN}",
          message: "### ${env.RUNTIME_EMOJI} [${JOB_NAME}${BUILD_DISPLAY_NAME}](${BUILD_URL}) \n\n **Upload to S3**",
        )


        sh """
          s3cmd put --acl-public ${env.WORKSPACE}/orchsym/orchsym-assembly/target/${env.PROJECT_NAME}-${env.VERSION_NAME}.tar.gz ${env.S3_PACKAGES_URL}/services/
        """

        dingTalk (
          accessToken: "${env.DINGDING_ACCESS_TOKEN}",
          message: "### ${env.RUNTIME_EMOJI} [${JOB_NAME}${BUILD_DISPLAY_NAME}](${BUILD_URL}) \n\n **Upload to S3 finished** \n\n You can download it from ${env.DOWNLOAD_PACKAGES_URL_BASE}/services/${env.PROJECT_NAME}-${env.VERSION_NAME}.tar.gz",
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
      dingTalk (
        accessToken: "${env.DINGDING_ACCESS_TOKEN}",
        message: "### ${env.RUNTIME_EMOJI} [${JOB_NAME}${BUILD_DISPLAY_NAME}](${BUILD_URL}) \n\n **Build Aborted**",
      )
    }

    unstable {
      dingTalk (
        accessToken: "${env.DINGDING_ACCESS_TOKEN}",
        message: "### ${env.RUNTIME_EMOJI} [${JOB_NAME}${BUILD_DISPLAY_NAME}](${BUILD_URL}) \n\n **Build Unstable**",
      )
    }

    success {
      dingTalk (
        accessToken: "${env.DINGDING_ACCESS_TOKEN}",
        message: "### ${env.RUNTIME_EMOJI} [${JOB_NAME}${BUILD_DISPLAY_NAME}](${BUILD_URL}) \n\n **Build Succeed**${env.EMOJI_BUILD_SUCCESS} \n\n ${env.build_output}",
      )
    }

    failure {
      dingTalk (
        accessToken: "${env.DINGDING_ACCESS_TOKEN}",
        message: "### ${env.RUNTIME_EMOJI} [${JOB_NAME}${BUILD_DISPLAY_NAME}](${BUILD_URL}) \n\n **Build Failed** ${env.EMOJI_BUILD_FAILURE}",
      )
    }
  }
}
