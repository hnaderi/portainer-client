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

package dev.hnaderi

import java.util.Base64

package object portainer {
  type ServerName = String
  type LoginToken = String
  type APIToken = String
  type Username = String
  type Password = String

  private implicit class StringEncodingOps(val str: String) extends AnyVal {
    def base64: String =
      Base64.getEncoder().encodeToString(str.getBytes())
  }

  type CommandLine[F[_]] = CLICommand => F[Unit]
}
