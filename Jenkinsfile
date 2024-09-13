@Library('jenkins-shared-library@master') _

def agentLabel = null

node('master') {
    stage('Agent definition'){
        checkout scm
        def tag_name = sh (returnStdout: true, script: 'git describe --exact-match --tags').trim()
        def platform = sh (returnStdout: true, script: "echo ${tag_name} | cut -d '/' -f1").trim()
        def environment = sh (returnStdout: true, script: "echo ${tag_name} | cut -d '/' -f2").trim()
        def version = sh (returnStdout: true, script: "echo ${tag_name} | cut -d '/' -f3").trim()

        if (platform == 'ios') {
            AGENT_LABEL = "ios-builder"
            UE_BATCH_FILES_PATH='/Users/Shared/EpicGames/UE_5.3/Engine/Build/BatchFiles'
        }
        if (platform == 'android'){
            AGENT_LABEL = "android-builder"
            UE_BATCH_FILES_PATH='/ue5/Engine/Build/BatchFiles'
        }

        println "[DEBUG] # ========= Building project: =========== #"
        println "[DEBUG]       tag_name:        ${tag_name}"
        println "[DEBUG]       platform:        ${platform}"
        println "[DEBUG]       environment:     ${environment}"
        println "[DEBUG]       version:         ${version}"
        println "[DEBUG]       runner label:    ${AGENT_LABEL}"
        println "[DEBUG] # ======================================= #"
   }
}

pipeline {
         
    environment {
        PROJECT_NAME        = 'MyApp'
        DISCORD_WEBHOOK_URL = credentials('discord webhook secret id')
    }

    agent { label "${AGENT_LABEL}" }
    
    stages {
            
        stage('setting env') {    
            steps{
                script{
                    if (AGENT_LABEL.equals('ios-builder')) {
                        PreBuild.checkUser(user:'admin',script:this)
                        KeychainUnlocker.listKeychains(script:this)
                        KeychainUnlocker.createTempKeychain(script:this)
                        KeychainUnlocker.appendTempKeychain(script:this)
                        KeychainUnlocker.unlockKeychains(script:this)
                        KeychainUnlocker.certImportTempKeychain(script:this)
                        KeychainUnlocker.infoDeveloperIdentity(script:this)
                        KeychainUnlocker.setPartitionList(script:this)
                    }
                    if (AGENT_LABEL.equals('android-builder')) {
                        PreBuild.checkUser(user:'ue5',script:this)
                    }
                }
            }
        }

        stage('UE IOS build ') {
            steps {
                script{
                    UnrealEngine.buildCookRun(
                            ue_batch_files_path:UE_BATCH_FILES_PATH,
                            script:this)
                    if (AGENT_LABEL.equals('ios-builder')) {
                        UnrealEngine.generateXcodeFiles(script:this)
                    } else {
                        println "[DEBUG] Not ios build, skip XCode files generation"
                    }
                }
            }
        }

        stage('XCode') {        
            steps{         
                script{
                    if (AGENT_LABEL.equals('ios-builder')) {
                        KeychainUnlocker.unlockKeychains(script:this)
                        XCodeBuilder.listScheme(script:this)
                        XCodeBuilder.buildScheme(script:this)
                        XCodeBuilder.generateArchive(script:this)
                        XCodeBuilder.setInfoPlist(script:this)
                        XCodeBuilder.exportArchive(script:this)
                    } else {
                        println "[DEBUG] Not ios build, skip XCode configuration"
                    }
                }
            }
        }
    }

    post { 
        always {
            script{
                if (AGENT_LABEL.equals('ios-builder')) {
                    XCodeBuilder.cleanCache(script:this)
                    KeychainUnlocker.deleteTempKeychain(script:this)
                    KeychainUnlocker.listKeychains(script:this)
                    Notify.discord(
                        script:this,
                        BUILD_URL:"${BUILD_URL}",
                        projectName:"${env.PROJECT_NAME}",
                        currentResult:currentBuild.currentResult,
                        discordWebhook:"${env.DISCORD_WEBHOOK_URL}")
                } else {
                    println "[DEBUG] Not ios build, skip post script for ios build."
                }
            }
        }
    }
}

