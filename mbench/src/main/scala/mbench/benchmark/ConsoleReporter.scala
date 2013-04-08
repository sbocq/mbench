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

import java.io.{ PrintWriter, PrintStream }

/** 
 * A benchmark reporter that reports its output on the console.
 */
object ConsoleReporter extends Reporter[Unit] {

  def open(benchName: String, testName: String, config: Config[_, _, _], labels: Vector[Label[_]]): Report[Unit] =
    apply(System.err).open(benchName, testName, config, labels)

  def apply(stream: PrintStream): Reporter[Unit] = new Reporter[Unit] {
    def open(benchName: String, testName: String, config: Config[_, _, _], labels: Vector[Label[_]]): Report[Unit] = {
      val out = new PrintWriter(stream, true)
      new ConsoleReport(out, benchName, testName, config.toString, labels)
    }
  }

  /**
   * Console Report
   */
  class ConsoleReport(out: PrintWriter, benchName: String, testName: String, configName: String, labels: Vector[Label[_]]) extends Report[Unit] {

    init()

    def init() {
      comment("start")
    }

    def close() {
      comment("end - " + MBench.getDateString(new java.util.Date(), "dd/MM/yyyy HH::mm::ss"))
    }

    def comment(s: String): Unit = {
      out.print(benchName + ":")
      out.print(testName + ":")
      out.print(configName + ":")
      out.println(s)
    }

    private def cell(label: Label[_], value: Any): Unit = {
      out.print('[')
      out.print(label.format(value))
      out.print(']')
    }

    def update(values: Vector[Any]): ConsoleReport = {
      out.print(benchName + ":" + testName + ":" + configName + ":")
      cell(labels.head, values.head)
      out.print(" -> ")

      val ls = labels.tail.iterator
      val vs = values.tail.iterator

      while (!ls.isEmpty) {
        cell(ls.next, vs.next)
      }
      out.println
      this
    }
  }

}