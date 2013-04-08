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

package mbench
package benchmark

/** A reporter that produces a table represented as an array stored in memory. 
 */
object TableReporter extends Reporter[Table] {

  def open(benchName: String, testName: String, config: Config[_, _, _], labels: Vector[Label[_]]): Report[Table] =
    apply(MBench.logout).open(benchName, testName, config, labels)

  import java.io.PrintStream

  def apply(stream: PrintStream): Reporter[Table] = ConsoleReporter(stream) >> new Reporter[Table] {
    def open(benchName: String, testName: String, config: Config[_, _, _], labels: Vector[Label[_]]): Report[Table] =
      new TableReport(benchName, testName, config.toString, labels, Vector.empty)
  }

  class TableReport(benchName: String, testName: String, configName: String, labels: Vector[Label[_]], rows: Vector[Vector[Any]]) extends Report[Table] {

    def update(values: Vector[Any]): TableReport =
      new TableReport(benchName, testName, configName, labels, rows :+ values)

    def close(): Table = new Table(benchName, testName, configName, labels, rows)

  }
}
