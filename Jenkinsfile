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
            stages {
            	// Build phaedra
                stage('Build') {
                    steps {
                        container('phaedra-build') {
                            sh 'mvn -U clean install'
                        }
                    }
                }
                // Deploy to nexus
                stage('Deploy') {
                    steps {
                        withCredentials([usernameColonPassword(credentialsId: 'oa-deployment', variable: 'USERPASS')]) {
                            container('phaedra-build') {
                                sh 'mvn -U clean deply'
                            }
                        }
                    }
                }
            }
        }
    }
}
