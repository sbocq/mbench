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
 * A reporter creates a report that is opened at the beginning of a benchmark
 * and updated repeatedly for each input of the benchmark as new results become
 * available.
 *
 * @tparam R The type of the result created by the reporter after
 *           a benchmark has terminated.
 */
abstract class Reporter[R] extends Serializable { outer =>

  /**
   * Open an new report.
   *
   * @param benchName   the name of the benchmark.
   * @param testName    the name of the test.
   * @param configName  the name of the config.
   * @param labels      the labels of the data reported by the benchmark.
   * @return a reporter
   */
  def open(benchName: String, testName: String, config: Config[_, _, _], labels: Vector[Label[_]]): Report[R]

  /**
   * Create another reporter that will be opened simultaneously with this
   * reporter.
   *
   * @param next the next reporter.
   * @param a reporter that combines both reporters and returns the result of the second reporter.
   * @return a reporter
   */
  def >>[B](next: Reporter[B]): Reporter[B] =
    new Reporter[B] {
      def open(benchName: String, testName: String, config: Config[_, _, _], labels: Vector[Label[_]]): Report[B] =
        outer.open(benchName, testName, config, labels) >> next.open(benchName, testName, config, labels)
    }

}