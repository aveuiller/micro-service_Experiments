name := "transporterOrganizations-organizations"

// Dockerization
packageName := "organizations"
dockerUsername := Some("experiments")
version in Docker := "latest"

dockerBaseImage := "openjdk:8-jre"
dockerExposedPorts in Docker := Seq(9000, 9443)
dockerExposedVolumes := Seq("/opt/docker/logs")