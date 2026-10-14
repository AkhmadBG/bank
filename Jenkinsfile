
pipeline {
	agent any

	options {
		skipDefaultCheckout(true)
		timestamps()
	}

	environment {
		IMAGE_TAG = 'local'
		HELM_RELEASE = 'bank'
		HELM_CHART = 'helm/bank'
	}

	stages {
		stage('Checkout') {
			steps {
				checkout scm
			}
		}

		stage('Build and Test') {
			steps {
				bat 'mvnw.cmd -B clean verify'
			}
		}

		stage('Build Docker Images') {
			steps {
				bat '''
                    docker build -f Dockerfile --build-arg JAR_FILE=front/target/front-0.0.1-SNAPSHOT.jar -t bank/front:%IMAGE_TAG% .
                    docker build -f Dockerfile --build-arg JAR_FILE=infrastructure/gateway/target/gateway-0.0.1-SNAPSHOT.jar -t bank/gateway:%IMAGE_TAG% .
                    docker build -f Dockerfile --build-arg JAR_FILE=business/accounts/target/accounts-0.0.1-SNAPSHOT.jar -t bank/accounts:%IMAGE_TAG% .
                    docker build -f Dockerfile --build-arg JAR_FILE=business/cash/target/cash-0.0.1-SNAPSHOT.jar -t bank/cash:%IMAGE_TAG% .
                    docker build -f Dockerfile --build-arg JAR_FILE=business/transfer/target/transfer-0.0.1-SNAPSHOT.jar -t bank/transfer:%IMAGE_TAG% .
                    docker build -f Dockerfile --build-arg JAR_FILE=business/notifications/target/notifications-0.0.1-SNAPSHOT.jar -t bank/notifications:%IMAGE_TAG% .
                '''
			}
		}

		stage('Validate Kubernetes and Helm') {
			steps {
				bat 'docker info'
				bat 'kubectl cluster-info'
				bat 'helm lint %HELM_CHART%'
				bat 'helm template %HELM_RELEASE% %HELM_CHART% > NUL'
			}
		}

		stage('Deploy to Kubernetes') {
			steps {
				bat 'helm upgrade --install %HELM_RELEASE% %HELM_CHART% --wait --timeout 10m'
			}
		}

		stage('Verify Deployment') {
			steps {
				bat 'kubectl get pods'
				bat 'kubectl get services'
				bat 'helm status %HELM_RELEASE%'
			}
		}
	}

	post {
		success {
			echo 'CI/CD pipeline completed successfully.'
		}

		failure {
			echo 'CI/CD pipeline failed. Check the stage logs.'
		}
	}
}
