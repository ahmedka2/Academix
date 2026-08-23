pipeline {
    agent any

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    stages {
        stage('Backend: build & test') {
            steps {
                dir('back') {
                    sh 'mvn -B clean package'
                }
            }
            post {
                always {
                    junit testResults: 'back/target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Frontend: install & build') {
            steps {
                dir('front') {
                    sh 'npm ci'
                    sh 'npm run build'
                }
            }
        }

        stage('Docker: build images') {
            steps {
                sh 'docker compose build'
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'back/target/*.jar', allowEmptyArchive: true
        }
    }
}
