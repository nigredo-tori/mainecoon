/*
 * Copyright 2017 Kailuo Wang
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

package mainecoon
package tests

import cats.arrow.FunctionK

import scala.util.{Success, Try}
import cats.~>

class FinalAlgTests extends MainecoonTestSuite {
  test("companion apply") {
    import Interpreters.tryInterpreter
    SafeAlg[Try].parseInt("10") should be(Success(10))
  }

  test("auto derive") {
    implicit val fk: Try ~> Option = λ[FunctionK[Try, Option]](_.toOption)
    import Interpreters.tryInterpreter
    SafeAlg[Option].parseInt("10") should be(Some(10))
  }

}