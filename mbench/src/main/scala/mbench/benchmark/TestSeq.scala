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

import TestSeq.{ State => S }

/**
 * A sequence specification is a class that permits to combine tests that must be benchmarked in a sequence
 * at each run. This could be for example because of their side-effects or because they add and then remove
 * elements from a data structure.
 *
 * Example:
 * for {
 *  uid  <- test("AddUser", addUsers("toto"))
 *   _   <- test("DeleteUser", delUser(uid))
 * } yield ()
 *
 */
class SeqSpec[A](val run: S => (S, A)) {

  def map[B](g: A => B): SeqSpec[B] =
    new SeqSpec[B]({ s0 =>
      val (s1, a) = run(s0)
      (s1, g(a))
    })

  def flatMap[B](g: A => SeqSpec[B]): SeqSpec[B] =
    new SeqSpec[B]({ s0 =>
      val (s1, a) = run(s0)
      g(a).run(s1)
    })

}

/**
 * Generic test sequence specification.
 *
 * @tparam S The type of setup of a runtime configuration required by the first argument of the test.
 * @tparam C The type of value of a static configuration required by the second argument of the test.
 * @tparam I The type of input of the benchmark.
 */
trait TestSeq[-S, -C, -I] extends Serializable {

  /**
   * The name of the test.
   */
  def name: String

  /**
   * The generic test function.
   *
   * @param setup a setup created by a runtime configuration.
   * @param value a value carried by a static configuration.
   * @param i the input of the benchmark.
   * @return a sequence specification
   */
  def run(setup: S, value: C, i: I): SeqSpec[_]

}

/**
 * The class of test sequences that that take only on the input of a benchmark as
 * parameter.
 *
 * @tparam I The type of input of the benchmark.
 * @param name the name of the test.
 * @param f a function that creates a sequence specification to benchmark.
 */
class InputTestSeq[I](val name: String, f: I => SeqSpec[_]) extends TestSeq[Any, Any, I] {
  def run(setup: Any, config: Any, i: I): SeqSpec[_] = f(i)
}

/**
 * The class of test sequences that take a setup created by a runtime
 * configuration and the input of a benchmark as parameters.
 *
 * @tparam S The type of setup of a runtime configuration required by the first argument of the test.
 * @tparam I The type of input of the benchmark.
 *
 * @param name the name of the test.
 * @param f a function that creates a sequence specification to benchmark.
 */
class RuntimeTestSeq[S, I](val name: String, f: (S, I) => SeqSpec[_]) extends TestSeq[S, Any, I] {
  def run(setup: S, value: Any, i: I): SeqSpec[_] = f(setup, i)
}

/**
 * The class of test sequences that take the value of a static
 * configuration and the input of a benchmark as parameters.
 *
 * @tparam C The type of value of a static configuration required by the second argument of the test.
 * @tparam I The type of input of the benchmark.
 *
 * @param name the name of the test.
 * @param f a function that creates a sequence specification to benchmark.
 */
class StaticTestSeq[C, I](val name: String, f: (C, I) => SeqSpec[_]) extends TestSeq[Any, C, I] {
  def run(setup: Any, value: C, i: I): SeqSpec[_] = f(value, i)
}

/**
 * The class of test sequences that take a setup created by a runtime
 * configuration and the value of a static configuration as
 * parameters.
 *
 * @tparam S The type of setup of a runtime configuration required by the first argument of the test.
 * @tparam C The type of value of a static configuration required by the second argument of the test.
 *
 * @param name the name of the test.
 * @param f a function that creates a sequence specification to benchmark.
 */
class RuntimeStaticTestSeq[S, C](val name: String, f: (S, C) => SeqSpec[_]) extends TestSeq[S, C, Any] {
  def run(setup: S, value: C, i: Any): SeqSpec[_] = f(setup, value)
}

object TestSeq {

  /**
   * Create a test sequence specification that depends only on the input of a benchmark.
   *
   * @tparam I The type of input of the benchmark.
   *
   * @param name the name of the test.
   * @param f a function that creates a sequence specification.
   * @return a test sequence specification
   */
  def input[I](name: String, f: I => SeqSpec[_]): InputTestSeq[I] =
    new InputTestSeq(name, f)

  /**
   * Create a test sequence specification that takes the value of a static
   * configuration and the input of a benchmark as parameters.
   *
   * @tparam C The type of value of a static configuration required by the second argument of the test.
   * @tparam I The type of input of the benchmark.
   *
   * @param name the name of the test.
   * @param f a function that creates a sequence specification.
   * @return a test sequence specification
   */
  def static[C, I](name: String, f: (C, I) => SeqSpec[_]): StaticTestSeq[C, I] =
    new StaticTestSeq(name, f)

  /**
   * Create a test sequence specification that takes a setup created by a runtime
   * configuration and the input of a benchmark as parameters.
   *
   * @tparam S The type of setup of a runtime configuration required by the first argument of the test.
   * @tparam I The type of input of the benchmark.
   *
   * @param name the name of the test.
   * @param f a function that creates a sequence specification.
   * @return a test sequence specification
   */
  def runtime[S, I](name: String, f: (S, I) => SeqSpec[_]): RuntimeTestSeq[S, I] =
    new RuntimeTestSeq(name, f)

  /**
   * Factory method for runtime tests that takes a setup created by a runtime
   * configuration and the value of a static configuration as parameters.
   *
   * @tparam S The type of setup of a runtime configuration required by the first argument of the test.
   * @tparam C The type of value of a static configuration required by the second argument of the test.
   *
   * @param name the name of the test.
   * @param f a function that creates a sequence specification.
   * @return a test sequence specification
   */
  def runtimeStatic[S, C](name: String, f: (S, C) => SeqSpec[_]): RuntimeStaticTestSeq[S, C] =
    new RuntimeStaticTestSeq(name, f)

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
   * @param f a function that creates a sequence specification.
   * @return a test sequence specification
   */
  def apply[S, C, I](name: String, f: (S, C, I) => SeqSpec[_]): TestSeq[S, C, I] = {
    val n = name
    new TestSeq[S, C, I] {
      def name = n
      def run(setup: S, value: C, i: I) = f(setup, value, i)
    }
  }

  // for {
  //   uid <- test("AddUser", addUsers(toto))
  //   _   <- test("DeleteUser", delUser(uid))
  // } yield ()

  private[mbench] case class State(names: Vector[String], ms: Vector[Double])

  private[mbench] object State {
    val empty = State(Vector.empty, Vector.empty)
  }

  /**
   * Perform an action that must be benchmarked
   *
   * @param name the name of the action used as benchmark name
   * @param thunk the thunk of code to evaluate.
   * @return a test sequence.
   */
  def test[A](name: String, thunk: => A): SeqSpec[A] = new SeqSpec({ s0 =>
    val (e, a) = Measure.time(thunk)
    val names1 = s0.names :+ name
    val ms1 = s0.ms :+ e
    val s1 = State(names1, ms1)
    (s1, a)
  })

  /**
   * Perform an action that must not be benchmarked (e.g. clean up)
   *
   * @param thunk the thunk of code to execute outside the benchmark.
   * @return a test sequence.
   */
  def ignore[A](thunk: => A): SeqSpec[A] = new SeqSpec({ s0 =>
    (s0, thunk)
  })
}