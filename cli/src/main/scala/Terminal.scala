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

import cats.Show
import cats.effect._

trait Terminal[F[_]] {
  def println[O: Show](o: O): F[Unit]
  def error[O: Show](o: O): F[Unit]
  def readPassword: F[Password]
}

object Terminal {
  def apply[F[_]](
      password: F[Password]
  )(implicit con: std.Console[F]): Terminal[F] = new Terminal[F] {

    override def println[O: Show](o: O): F[Unit] = con.println(o)

    override def error[O: Show](o: O): F[Unit] = con.error(o)

    override def readPassword: F[Password] = password

  }
}
