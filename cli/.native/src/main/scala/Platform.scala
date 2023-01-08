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
  protected def readPassword: IO[String] = IO.blocking(read)

  private val size: Int = 100

  private def read: String = {
    import scala.scalanative.unsafe._
    import scala.scalanative.unsigned._
    import scala.scalanative.posix.termios._
    import scala.scalanative.posix.unistd.STDIN_FILENO
    import scala.scalanative.libc.stdio._
    import scala.scalanative.libc.string.strlen

    puts(c"Enter password: ")

    // get settings of the actual terminal
    val oldT = stackalloc[termios]()
    val newT = stackalloc[termios]()
    if (tcgetattr(STDIN_FILENO, oldT) < 0)
      perror(c"Cannot get terminal attributes!")

    // do not echo the characters
    !newT = !oldT

    // FIXME does not work for some reason!!!
    newT._4 = newT._4 & ~(ECHO)

    // set this as the new terminal options
    if (tcsetattr(STDIN_FILENO, TCSANOW, newT.toPtr) < 0)
      perror(c"Cannot set terminal attributes!")

    // get the password
    // the user can add chars and delete if he puts it wrong
    // the input process is done when he hits the enter
    // the \n is stored, we replace it with \0
    val password: CString = stackalloc(size.toULong)
    fgets(password, size, stdin)
    password.update(strlen(password) - 1.toUInt, 0.toByte)

    tcsetattr(STDIN_FILENO, TCSANOW, oldT)

    fromCString(password)
  }
}
