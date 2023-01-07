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
import cats.implicits._
import com.monovore.decline._

object Main extends IOApp {

  val userOpt =
    Opts
      .option[String]("target", help = "Person to greet.")
      .withDefault("world")

  val quietOpt = Opts.flag("quiet", help = "Whether to be quiet.").orFalse

  val a = userOpt product quietOpt

  val pctl = Command("portainer cli", "header")(a)

  override def run(args: List[String]): IO[ExitCode] =
    pctl.parse(args) match {
      case Left(value) => IO.println(value.toString).as(ExitCode.Error)
      case Right(_)    => IO.println("Ok!").as(ExitCode.Success)
    }

}
