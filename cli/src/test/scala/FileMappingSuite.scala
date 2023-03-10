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

import munit.FunSuite

import java.nio.file.Paths

class FileMappingSuite extends FunSuite {
  test("Empty") {
    assertEquals(
      FileMapping(""),
      None
    )
  }

  test("Invalid") {
    assertEquals(
      FileMapping("file.txt=config"),
      None
    )
  }

  test("Valid") {
    assertEquals(
      FileMapping("file.txt:config"),
      Some(FileMapping(Paths.get("file.txt"), "config"))
    )
  }

  test("Trims input") {
    assertEquals(
      FileMapping(" file.txt : config "),
      Some(FileMapping(Paths.get("file.txt"), "config"))
    )
  }
}
