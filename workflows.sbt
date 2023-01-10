// NOTE apparently githubWorkflowCheck does not work as intended on windows
// due to file separator differences
ThisBuild / githubWorkflowGeneratedCI ~= {
  _.map { job =>
    if (job.id == "build")
      job.copy(
        steps = job.steps.map {
          case step: WorkflowStep.Run
              if step.commands.exists(_ contains "githubWorkflowCheck") =>
            step.copy(cond = Some("!startsWith(matrix.os, 'windows')"))
          case other => other // unchanged
        }
      )
    else job
  }
}

ThisBuild / githubWorkflowBuildPostamble ++= Seq(
  WorkflowStep.Sbt(
    List("cliNative/nativeLink"),
    name = Some("Building native binaries")
  ),
  WorkflowStep.Run(
    List(
      "mkdir -p dist",
      "cp cli/.native/target/scala-2.13/portainer-cli-out dist/portainer-${{ matrix.os }}"
    ),
    id = Some("rename_artifact"),
    name = Some("Renaming artifacts"),
    cond =
      Some(" startsWith(matrix.scala, '2') && matrix.project == 'rootNative' ")
  ),
  WorkflowStep.Use(
    UseRef.Public("actions", "upload-artifact", "v3"),
    name = Some("Upload native artifacts"),
    params = Map(
      "name" -> "client-$${{ matrix.os }}",
      "path" -> "dist/*"
    ),
    cond =
      Some(" startsWith(matrix.scala, '2') && matrix.project == 'rootNative' ")
  )
)
