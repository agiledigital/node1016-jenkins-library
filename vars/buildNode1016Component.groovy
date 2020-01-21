/*
 * Toolform-compatible Jenkins 2 Pipeline build step for NodeJS 10.16 apps using the node1016 builder
 */

def call(Map config) {

  final supportedPackageManagers = ['yarn', 'npm']
  def artifactDir = "${config.project}-${config.component}-artifacts"
  def testOutput = "${config.project}-${config.component}-tests.xml"
  def packageManager = (config.packageManager?.trim()) ? config.packageManager : 'yarn'
  
  
  if (!packageManager in supportedPackageManagers) {
    throw new Exception("Unsupported package manager [${packageManager}]. Expected one of [${supportedPackageManagers}]")
  }

  final run = { cmd ->
    ansiColor('xterm') {
      dir(config.baseDir) {
        sh "NODE_OPTIONS=--max-old-space-size=4096 JEST_JUNIT_OUTPUT=${testOutput} ${packageManager} ${cmd}"
      }
    }
  }
  
  container("node1016-builder") {

    stage('Build Details') {
      echo "Project:   ${config.project}"
      echo "Component: ${config.component}"
      echo "BuildNumber: ${config.buildNumber}"
    }

    stage('Install dependencies') {
      run "install"
    }

    stage('Test') {
      run 'test --ci --testResultsProcessor="jest-junit"'
      junit allowEmptyResults: true, testResults: testOutput
    }

  }

   container('node1016-builder') {
    stage('Build') {
      run "build"
    }
   }
  
  if(config.stage == 'dist') {

    container('node1016-builder') {

      stage('Package') {
        sh "mkdir -p ${artifactDir}"

        run "install --production --ignore-scripts --prefer-offline"
        sh "mv ${config.baseDir}/node_modules ${config.baseDir}/package.json ${artifactDir}"

        // The build and dist folders may exisit depending on builder.
        // Copy them into the artifact if they exist. e.g. React uses build, NodeJS defualt is dist.
        if(fileExists("${config.baseDir}/dist")) {
          sh "mv ${config.baseDir}/dist ${artifactDir}"
        }
        
        if(fileExists("${config.baseDir}/build")) {
          sh "mv ${config.baseDir}/build ${artifactDir}"
        }
        
        if(fileExists("${config.baseDir}/serverless.yml")) {
          sh "mv ${config.baseDir}/serverless.yml ${artifactDir}"
        }

        // The static folder and application specific config files 
        // should also be staged if they exist.
        if(fileExists("${config.baseDir}/static")) {
          sh "mv ${config.baseDir}/static ${artifactDir}"
        }

        if(fileExists("${config.baseDir}/next.config.js")) {
          sh "mv ${config.baseDir}/next.config.js ${artifactDir}"
        }
      }
    }

    stage('Archive to Jenkins') {
      def tarName = "${config.project}-${config.component}-${config.buildNumber}.tar.gz"
      sh "tar -czvf \"${tarName}\" -C \"${artifactDir}\" ."
      archiveArtifacts tarName
    }

  }

}
