def call(Map config) {
  return [
    [
      path: '/home/jenkins/.cache',
      claimName: "${config.project}-home-jenkins-npm",
      sizeGiB: 1.5
    ]
  ]
}
