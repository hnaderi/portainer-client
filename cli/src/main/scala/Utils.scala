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

import cats.data._
import cats.effect._
import cats.implicits._

import java.nio.file.Files
import java.nio.file.Path
import scala.jdk.CollectionConverters._

private object Utils {
  def readLined[F[_]: Async](path: Path): F[List[String]] = Async[F].blocking(
    Files.readAllLines(path).asScala.toList
  )
  def readLines[F[_]: Async](path: Path): F[String] = Async[F].blocking(
    Files.readAllLines(path).asScala.mkString
  )
  def readEnvFile[F[_]: Async](path: Path): F[Map[String, String]] =
    readLined(path).flatMap(
      _.traverse(InlineEnv.validate).fold(
        InvalidEnvFile(_).raiseError[F, Map[String, String]],
        vars => Async[F].pure(vars.map(v => (v.key, v.value)).toMap)
      )
    )

  def readFileMap[F[_]: Async](fm: FileMapping): F[(String, String)] =
    readLines(fm.source).map((fm.name, _))

  def readFileMaps[F[_]: Async](
      fms: NonEmptyList[FileMapping]
  ): F[Map[String, String]] = fms.traverse(readFileMap(_)).map(_.toList.toMap)

  final case class InvalidEnvFile(errors: NonEmptyList[String])
      extends Exception(
        s"Invalid env file!\n${errors.mkString_("\n")}"
      )

}
