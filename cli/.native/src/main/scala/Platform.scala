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

import cats.effect.IO
import cats.effect.IOApp
import cats.effect.kernel.Resource
import org.http4s.client.Client
import org.http4s.curl.CurlApp

abstract class Platform extends IOApp with CurlApp {
  protected def client: Resource[IO, Client[IO]] = Resource.pure(curlClient)

  private val echoOff = IO(platform_terminal.setStdinEcho(0))
  private val echoOn = IO(platform_terminal.setStdinEcho(1))
  private val noEcho = Resource.make(echoOff)(_ => echoOn)

  protected val terminal: Terminal[IO] = Terminal(
    IO.println("password: ") >> noEcho.surround(IO.readLine)
  )

}

import scalanative.unsafe._

@extern
private object platform_terminal {
  @name("dev_hnaderi_set_stdin_echo")
  def setStdinEcho(enable: CChar): Unit = extern
}
