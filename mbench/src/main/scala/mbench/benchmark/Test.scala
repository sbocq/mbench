/*
* Copyright (C) 2013 Alcatel-Lucent.
*
* See the NOTICE file distributed with this work for additional
* information regarding copyright ownership.
* Licensed to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package mbench.benchmark

/**
 * Generic test specification.
 *
 * @tparam S The type of setup of a runtime configuration required by the first argument of the test.
 * @tparam C The type of value of a static configuration required by the second argument of the test.
 * @tparam I The type of input of the benchmark.
 */
sealed trait Test[-S, -C, -I] extends Serializable {

  /**
   * The name of the test.
   */
  def name: String

  /**
   * The generic test function.
   * @param setup a setup created by a runtime configuration.
   * @param value a value carried by a static configuration.
   * @param i the input of the benchmark.
   */
  def run(setup: S, value: C, i: I): Unit

}

/**
 * The class of tests that that take only on the input of a benchmark as
 * parameter.
 *
 * @tparam I The type of input of the benchmark.
 * @param name the name of the test.
 * @param f the test function to benchmark.
 */
class InputTest[I](val name: String, f: I => Unit) extends Test[Any, Any, I] {
  def run(setup: Any, value: Any, i: I): Unit = f(i)
}

/**
 * The class of tests that take a setup created by a runtime
 * configuration and the input of a benchmark as parameters.
 *
 * @tparam S The type of setup of a runtime configuration required by the first argument of the test.
 * @tparam I The type of input of the benchmark.
 *
 * @param name the name of the test.
 * @param f the test function to benchmark.
 */
class RuntimeTest[S, I](val name: String, f: (S, I) => Unit) extends Test[S, Any, I] {
  def run(setup: S, value: Any, i: I): Unit = f(setup, i)
}

/**
 * The class of tests that take the value of a static
 * configuration and the input of a benchmark as parameters.
 *
 * @tparam C The type of value of a static configuration required by the second argument of the test.
 * @tparam I The type of input of the benchmark.
 *
 * @param name the name of the test.
 * @param f the test function to benchmark.
 */
class StaticTest[C, I](val name: String, f: (C, I) => Unit) extends Test[Any, C, I] {
  def run(setup: Any, value: C, i: I): Unit = f(value, i)
}

/**
 * The class of tests that take a setup created by a runtime
 * configuration and the value of a static configuration as
 * parameters.
 *
 * @tparam S The type of setup of a runtime configuration required by the first argument of the test.
 * @tparam C The type of value of a static configuration required by the second argument of the test.
 *
 * @param name the name of the test.
 * @param f the test function to benchmark.
 */
class RuntimeStaticTest[S, C](val name: String, f: (S, C) => Unit) extends Test[S, C, Any] {
  def run(setup: S, value: C, i: Any): Unit = f(setup, value)
}

object Test {

  /**
   * Create a test that depends only on the input of a benchmark.
   *
   * @tparam I The type of input of the benchmark.
   *
   * @param name the name of the test.
   * @param f the test function.
   * @return a test specification.
   */
  def input[I](name: String, f: I => Unit): InputTest[I] =
    new InputTest(name, f)

  /**
   * Create a test that take the value of a static
   * configuration and the input of a benchmark as parameters.
   *
   * @tparam C The type of value of a static configuration required by the second argument of the test.
   * @tparam I The type of input of the benchmark.
   *
   * @param name the name of the test.
   * @param f the test function.
   * @return a test specification.
   */
  def static[C, I](name: String, f: (C, I) => Unit): StaticTest[C, I] =
    new StaticTest(name, f)

  /**
   * Create a test that takes a setup created by a runtime
   * configuration and the input of a benchmark as parameters.
   *
   * @tparam S The type of setup of a runtime configuration required by the first argument of the test.
   * @tparam I The type of input of the benchmark.
   *
   * @param name the name of the test.
   * @param f the test function.
   * @return a test specification.
   */
  def runtime[S, I](name: String, f: (S, I) => Unit): RuntimeTest[S, I] =
    new RuntimeTest(name, f)

  /**
   * Factory method for runtime tests that take a setup created by a runtime
   * configuration and the value of a static configuration as parameters.
   *
   * @tparam S The type of setup of a runtime configuration required by the first argument of the test.
   * @tparam C The type of value of a static configuration required by the second argument of the test.
   *
   * @param name the name of the test.
   * @param f the test function.
   * @return a test specification.
   */
  def runtimeStatic[S, C](name: String, f: (S, C) => Unit): RuntimeStaticTest[S, C] =
    new RuntimeStaticTest(name, f)

  /**
   * Create a generic test that takes a setup created by a runtime
   * configuration, the value of a static configuration and the input
   * of the benchmark as parameters.
   *
   * @tparam S The type of setup of a runtime configuration required by the first argument of the test.
   * @tparam C The type of value of a static configuration required by the second argument of the test.
   * @tparam I The type of input of the benchmark.
   *
   * @param name the name of the test.
   * @param f the test function.
   * @return a test specification.
   */
  def apply[S, C, I](name: String, f: (S, C, I) => Unit): Test[S, C, I] = {
    val n = name
    new Test[S, C, I] {
      def name = n
      def run(setup: S, value: C, i: I) = f(setup, value, i)
    }
  }

}