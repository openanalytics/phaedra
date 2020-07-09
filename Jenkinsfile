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

        stage('build and deploy to nexus'){

            steps {

                container('phaedra-build') {

                     configFileProvider([]) {

                         sh 'mvn -U clean install deploy'

                     }
                }
            }
        }
    }
}