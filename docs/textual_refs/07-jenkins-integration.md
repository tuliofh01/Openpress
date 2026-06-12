# Openpress — Jenkins CI/CD Pipeline

> **Prerequisites:** Jenkins server with Docker, Git, and Gradle plugins installed
> **Status:** 🚧 Planned — No `Jenkinsfile` exists in the project root yet. This document describes the target CI/CD pipeline and should be used as an implementation guide once the project is ready for automated deployments.

---

## 1. Jenkinsfile

Create `Jenkinsfile` in the project root:

```groovy
pipeline {
    agent any

    tools {
        gradle 'gradle-8.13'
        jdk 'jdk21'
    }

    environment {
        DOCKER_IMAGE = "openpress-api:${env.BUILD_ID}"
        DOCKER_REGISTRY = "docker.io/yourusername"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Compile') {
            steps {
                sh './gradlew compileKotlin --no-daemon'
            }
        }

        stage('Test') {
            steps {
                sh './gradlew test --no-daemon'
            }
            post {
                always {
                    junit 'build/reports/tests/**/*.xml'
                }
            }
        }

        stage('Build Fat JAR') {
            steps {
                sh './gradlew buildFatJar --no-daemon'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh """
                    docker build -t ${DOCKER_IMAGE} .
                    docker tag ${DOCKER_IMAGE} ${DOCKER_REGISTRY}:latest
                """
            }
        }

        stage('Push to Registry') {
            steps {
                withDockerRegistry(
                    credentialsId: 'docker-hub-credentials',
                    url: 'https://index.docker.io/v1/'
                ) {
                    sh "docker push ${DOCKER_REGISTRY}:latest"
                }
            }
        }

        stage('Deploy') {
            steps {
                sh """
                    docker compose -f docker-compose.yml down
                    docker compose -f docker-compose.yml up -d
                """
            }
        }
    }

    post {
        failure {
            emailext(
                subject: "Openpress Pipeline Failed: ${env.BUILD_ID}",
                body: "Check ${env.BUILD_URL} for details.",
                to: "team@example.com"
            )
        }
        success {
            emailext(
                subject: "Openpress Pipeline Succeeded: ${env.BUILD_ID}",
                body: "Deployed version ${env.BUILD_ID}.",
                to: "team@example.com"
            )
        }
    }
}
```

---

## 2. Jenkins Plugins Required

| Plugin | Purpose |
|--------|---------|
| **Pipeline** | Run Jenkinsfile-based pipelines |
| **Git** | Checkout source code from GitHub |
| **Gradle** | Run Gradle tasks |
| **Docker Pipeline** | Build and push Docker images |
| **JUnit** | Publish test reports |
| **Email Extension** | Send pipeline notifications |
| **Credentials Binding** | Manage secrets (Docker Hub, API keys) |

---

## 3. Setting Up Credentials in Jenkins

Go to **Dashboard → Manage Jenkins → Credentials** and add:

| ID | Type | Value |
|----|------|-------|
| `docker-hub-credentials` | Username with password | Docker Hub username + token |
| `github-token` | Secret text | GitHub personal access token |
| `stripe-secret-key` | Secret text | `sk_test_...` |
| `jwt-secret` | Secret text | Your JWT signing secret |

Reference credentials in your pipeline:

```groovy
environment {
    STRIPE_SECRET_KEY = credentials('stripe-secret-key')
    JWT_SECRET = credentials('jwt-secret')
}
```

---

## 4. Running the Pipeline

```bash
# Option 1: Via Jenkins UI
# 1. Create a new Pipeline job
# 2. Set "Pipeline from SCM" → Git → your repo URL
# 3. Script Path: Jenkinsfile
# 4. Save and Build

# Option 2: Via Jenkins CLI
java -jar jenkins-cli.jar -s http://localhost:8080/ build openpress-pipeline
```

---

## 5. Blue/Green Deployment Strategy (Optional)

For zero-downtime deployments, extend the pipeline:

```groovy
stage('Blue/Green Deploy') {
    steps {
        sh """
            docker compose -f docker-compose.blue.yml up -d
            sleep 10  # Health check
            docker compose -f docker-compose.green.yml down
        """
    }
}
```

---

## 6. Troubleshooting

| Issue | Solution |
|-------|----------|
| `Permission denied` when running Docker | Add Jenkins user to `docker` group: `sudo usermod -aG docker jenkins` |
| Gradle out of memory | Set `GRADLE_OPTS="-Xmx2g"` in Jenkins global config |
| Docker not found | Install Docker pipeline plugin and ensure Docker is available on Jenkins agent |
| Webhook not triggering | Configure GitHub webhook → `http://jenkins:8080/github-webhook/` |
