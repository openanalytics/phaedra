pipeline {
    agent {
        kubernetes {
            yamlFile 'kubernetesPod.yaml'
        }
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '3'))
    }

    stages {
        stage('Build and Deploy to nexus') {
            steps {
                withCredentials([usernameColonPassword(credentialsId: 'oa-jenkins', variable: 'USERPASS')]) {
                    container('phaedra-build') {
                        sh 'mvn -U clean deploy'
                    }
                }
            }
        }
    }
}
