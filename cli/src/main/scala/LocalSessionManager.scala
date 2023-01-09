/*
 * Copyright 2023 Hossein Naderi
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

package dev.hnaderi.portainer

import cats.effect._
import io.circe.jawn.decode
import io.circe.syntax._

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import scala.jdk.CollectionConverters._

final case class LocalSessionManager(path: String) extends SessionManager[IO] {
  private val portainerRc = Paths.get(path)
  private val readSessionsFile = IO {
    if (Files.exists(portainerRc))
      Some(Files.readAllLines(portainerRc).asScala.mkString)
    else None
  }

  def load: IO[Sessions] =
    readSessionsFile
      .map {
        case Some(string) => decode[Sessions](string)
        case None         => Right(Sessions(Map.empty))
      }
      .flatMap(IO.fromEither)

  def save(sessions: Sessions): IO[Unit] =
    IO {
      if (Files.exists(portainerRc)) {
        Files.delete(portainerRc)
      }
      Files.write(
        portainerRc,
        sessions.asJson.noSpaces.getBytes(),
        StandardOpenOption.CREATE_NEW
      )
      ()
    }

  def add(name: String, session: Session): IO[Unit] =
    load.map(s => Sessions(s.servers.updated(name, session))).flatMap(save)

  def get(name: String): IO[Option[Session]] = load.map(_.servers.get(name))
}
