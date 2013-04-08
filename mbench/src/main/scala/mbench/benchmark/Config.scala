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
 * A configuration for a test in a benchmark.
 *
 * In addition to the input of a benchmark, tests can take a configuration
 * as parameter, see the factory methods in [[mbench.benchmark.Test]] or [[mbench.benchmark.TestSeq]].
 * A configuration is either empty, a static configuration `C` (see [[mbench.benchmark.StaticConfig]]), a
 * runtime configuration (see [[mbench.benchmark.RuntimeConfig]]) that creates a setup `S`,
 * or the composition of runtime configuration and a static configuration.
 *
 * Runtime and static configurations are created using the factory methods in the
 * companion object of this class. Eventually, a runtime configuration can be combined
 * with a static configuration using its `and` combinator.
 *
 * See also the factory methods in the companion objects of [[mbench.benchmark.Test]]
 * and [[mbench.benchmark.TestSeq]], which make it easy to declare tests that depend
 * on either the input of a benchmark, a runtime and/or a static configurations.
 *
 * @tparam I The type of the input of a benchmark.
 * @tparam C The type of the static configuration.
 * @tparam S The type of setup created by a runtime configuration.
 */
sealed trait Config[-I, +C, S] extends Serializable {

  /**
   * The names of the different configurations composing this
   * configuration. These names are used to generate reporting information
   * (e.g. file names).
   */
  def names: Seq[String]

  /**
   * The value of the static component of this configuration.
   */
  def value: C

  /**
   * Method that creates a setup that depends on the input of a
   * benchmark before the evaluation of a test in several runs.
   *
   * @param i the input of the benchmark.
   * @return a setup for the value i.
   */
  def setUp(i: I): S

  /**
   * Method that tears down a setup after the evaluation of a test.
   *
   * @param runtime the runtime created before the benchmark.
   * @return unit
   */
  def tearDown(setup: S): Unit

  override def toString =
    names.mkString("-")

}

/**
 * A static configuration is a configuration that remains constant throughout the execution
 * of a benchmark.
 *
 * Tests that depend on a static configuration are created either using the `static` factory
 * method, or the `runtimeStatic` methods of their companion object if they also depend
 * on a runtime configuration (see [[mbench.benchmark.Test]], [[mbench.benchmark.TestSeq]] or
 * [[mbench.benchmark.RuntimeConfig]]).
 *
 * Static configurations may also be also combined together using the `extend` method.
 *
 * @param names the names of the static configuration.
 */
class StaticConfig[+C](val names: Seq[String], _value: => C) extends Config[Any, C, Any] {
  // by-name parameters are always serializable!

  def value = _value

  def setUp(i: Any): Any = None

  def tearDown(runtime: Any): Unit = ()

  /**
   * Extends this static configuration with another static configuration.
   *
   * @param name the name of the additional information (used in reporting).
   * @param data the additional static information.
   * @return     the new config.
   */
  def extend[D](config: StaticConfig[D]): StaticConfig[(C, D)] =
    new StaticConfig[(C, D)](names ++ config.names, (_value, config.value))

  /**
   * Update the parameters of this configuration.
   *
   * @param f  the update function.
   * @return   the updated configuration.
   */
  def update[D](f: C => D): StaticConfig[D] =
    new StaticConfig[D](names, f(value))

}

/**
 * A runtime configuration creates an new setup `S` for each input `I` of
 * a benchmark before it evaluates the test over several runs. This means that
 * the time it takes to create a setup (e.g. a thread pool) is not accounted in
 * the execution times measured for a given test. For instance, for each
 * input `I` of the benchmark:
 *
 *   1. the benchmark creates a new setup by calling the `setUp` method,
 *   2. the test is executed a number of times, as specified by the benchmark, and the
 *      its execution time is measured,
 *   3. then, its runtime configuration is teared down using the `tearDown` method.
 *
 * Tests that depend on a runtime configuration are created either using the `runtime` factory
 * method, or the `runtimeStatic` methods of their companion object if they also depend
 * on a static configuration (see [[mbench.benchmark.Test]], [[mbench.benchmark.TestSeq]] or
 * [[mbench.benchmark.RuntimeConfig]]).
 *
 * @tparam I the input of the benchmark used to instantiate a new setup.
 * @tparam S the type of setup instantiated by this configuration (e.g. an `ExecutorService`).
 *
 * @param name the name of the runtime configuration
 * @param setup    a function that creates a setup that depends on the input of a
 *        benchmark before the evaluation of a test in several runs.
 * @param teardown a function that tears down a setup after the evaluation of a test.
 *
 */
class RuntimeConfig[-I, S](val name: String, setup: I => S, teardown: S => Unit) extends Config[I, Any, S] {

  def names = Seq(name.toOption).flatten

  def value: Any = None

  def setUp(i: I): S = setup(i)

  def tearDown(r: S) = teardown(r)

  /**
   * Combine this runtime configuration with a static configuration.
   *
   * @param config a static configuration.
   * @return       a final configuration for a test
   */
  def and[C](config: StaticConfig[C]): Config[I, C, S] = new Config[I, C, S] {
    def names = RuntimeConfig.this.names ++ config.names
    def value = config.value
    def setUp(i: I): S = RuntimeConfig.this.setUp(i)
    def tearDown(setup: S): Unit = RuntimeConfig.this.tearDown(setup)
  }

}

/**
 * Factory methods for configuration types.
 */
object Config {

  private[benchmark] trait NoSetup
  private[benchmark] object NoSetup extends NoSetup

  /**
   * The empty configuration.
   */
  private[benchmark] def empty[C](implicit ev: Any <:< C): Config[Any, C, Any] = new Config[Any, C, Any] {
    def names = Seq.empty
    def value = ev(None)
    def setUp(i: Any) = NoSetup
    def tearDown(setup: Any): Unit = ()
  }

  /**
   * Create a static configuration.
   *
   * @param name   the name of the configuration.
   * @param value  the value of this static configuration.
   * @return a static configuration
   */
  def static[C](name: String, value: => C): StaticConfig[C] =
    new StaticConfig(Seq(name.toOption).flatten, value)

  /**
   * Create an anonymous static configuration.
   *
   * Since it is anonymous, it will not be mentioned in the reports of a benchmark.
   * This reduces the amount of information presented when all the benchmarks
   * share the same configuration.
   *
   * @param value  the value of this static configuration.
   * @return a static configuration
   */
  def static[C](value: => C): StaticConfig[C] =
    new StaticConfig(Seq.empty, value)

  /**
   * Create a runtime configuration.
   *
   * @param name     the name of the configuration.
   * @param setup    a function that creates a setup that depends on the input of a
   *        benchmark before the evaluation of a test in several runs.
   * @param teardown a function that tears down a setup after the evaluation of a test.
   * @return a runtime configuration
   */
  def runtime[I, S](name: String, setup: I => S, teardown: S => Unit): RuntimeConfig[I, S] =
    new RuntimeConfig[I, S](name, setup, teardown)

  /**
   * Create an anonymous runtime configuration.
   *
   * Since it is anonymous, it will not be mentioned in the reports of a benchmark.
   * This reduces the amount of information presented when all the benchmarks
   * share the same configuration.
   *
   * @param setup    a function that creates a setup that depends on the input of a
   *        benchmark before the evaluation of a test in several runs.
   * @param teardown a function that tears down a setup after the evaluation of a test.
   * @return a runtime configuration
   */
  def runtime[S](setup: => S, teardown: S => Unit): RuntimeConfig[Any, S] =
    new RuntimeConfig[Any, S]("", _ => setup, teardown)

}