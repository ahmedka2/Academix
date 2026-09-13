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
                    // 'verify' (not 'package') so the jacoco:report execution runs and
                    // writes target/site/jacoco/jacoco.xml for the Sonar stage to import.
                    sh 'mvn -B clean verify'
                }
            }
            post {
                always {
                    junit testResults: 'back/target/surefire-reports/*.xml', allowEmptyResults: true
                    // Optional coverage trend graph in Jenkins itself. Requires the
                    // "Coverage Plugin" to be installed on the controller - uncomment
                    // once it is, otherwise the build fails on an unknown step.
                    // recordCoverage(tools: [[parser: 'JACOCO', pattern: 'back/target/site/jacoco/jacoco.xml']])
                }
            }
        }

        stage('Backend: SonarQube analysis') {
            steps {
                dir('back') {
                    withSonarQubeEnv('SonarQube') {
                        sh 'mvn -B org.sonarsource.scanner.maven:sonar-maven-plugin:5.1.0.4751:sonar'
                    }
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
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
