#!/usr/bin/env groovy

def get_branch_name() {
    if (env.BRANCH_NAME.startsWith("trunk")) {
        return "trunk"
    } else {
        try {
            return env.BRANCH_NAME.split('/')[-2]
        } catch(Exception e) {
            error "Could not find branch, the convention is branches/beta/<version>/owb"
        }
    }
}

def branch_name = get_branch_name()
def aws_profile = env.aws_bucket_profile
def artifact_bucket = env.aws_artifact_bucket
def now = new Date()
def timestamp = now.format("yyyy-MM-dd_HH-mm-ss")

pipeline {
    agent {
        node {
            label 'master'
        }
    }
    stages {
        stage('Build') {
            steps {
                sh "make zip_artifact BRANCH=$branch_name BUILD_NUMBER=$BUILD_ID COMMIT_ID=$SVN_REVISION_1 DATE=$timestamp"
            }
        }
        stage('Upload') {
            steps {
                script {
                    if (aws_profile) {
                        sh "make upload BRANCH=$branch_name BUILD_NUMBER=$BUILD_ID COMMIT_ID=$SVN_REVISION_1 AWS_PROFILE=$aws_profile BUCKET_NAME=$aws_artifact_bucket DATE=$timestamp "
                    } else {
                        sh "make upload BRANCH=$branch_name BUILD_NUMBER=$BUILD_ID COMMIT_ID=$SVN_REVISION_1 BUCKET_NAME=$aws_artifact_bucket DATE=$timestamp "
                    }
                }
            }
        }
    }
}
