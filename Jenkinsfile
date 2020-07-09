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
                withCredentials([usernameColonPassword(credentialsId: 'admin', variable: 'USERPASS')]) {
                    container('phaedra-build') {
                        sh 'mvn -U clean deploy'
                    }
                }
            }
        }
    }
}
