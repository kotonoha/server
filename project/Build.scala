/*
* Copyright 2012 eiennohito
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import java.util.Date
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import sbt._
import Keys._


object Settings {

  val ourScalaVer = "2.9.1"

  val buildOrganization = "org.eiennohito"

  val buildSettings = Defaults.defaultSettings ++ Seq (
    organization := buildOrganization,
    scalaVersion := ourScalaVer
  )
}

object GitData {
  def gitVer = {
    val builder = new FileRepositoryBuilder();
    val repository = builder.setGitDir(file("./.git"))
      .readEnvironment() // scan environment GIT_* variables
      .findGitDir() // scan up the file system tree
      .build();
    println("repository built")
    val head = repository.resolve("HEAD")
    println("head is " + head)
    val refwalk = new RevWalk(repository)
    val commit = refwalk.parseCommit(head)
    val time = new Date(commit.getCommitTime * 1000L)
    val id = head.abbreviate(10).name()
    (time, id)
  }
}


object KotonohaBuild extends Build {
  import Settings._

  val gitId = TaskKey[String]("gitId", "Git commit id")
  val gitDate = TaskKey[Long]("gitDate", "Git commit date")

  val gitdata = {
    val (time, id) = GitData.gitVer
    (
      gitId := id,
      gitDate := time.getTime
    )
  }


  lazy val kotonoha = Project(
    id = "kotonoha",
    base = file("."),
    settings = buildSettings ++ Seq(gitdata._1) ++ Seq(gitdata._2)
  ) dependsOn(model)

  lazy val model = Project(
    id = "model",
    base = file("model"),
    settings = buildSettings
  )

}
