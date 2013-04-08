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
 * Report that is updated in real-time by a benchmark.
 *
 * @tparam R final result of the Report once the benchmark is over
 */
abstract class Report[R] { outer =>

  /**
   * Update a subsequent row of values in a report.
   *
   * This method is invoked by the benchmark incrementally at the time
   * measurements become available.
   *
   * The values have the same size and order as the list of labels
   * passed to the `open` method of the reporter.
   *
   * @param values the new values.
   * @return an update report.
   */
  def update(values: Vector[Any]): Report[R]

  /**
   * Method invoked when the benchmark is over to close a report.
   */
  def close(): R

  /**
   * Update simultaneously another report with this report.
   *
   *  @param other the other report to updated
   *  @return the result of the other report
   */
  def >>[B](other: Report[B]): Report[B] =
    new Report[B] {
      def update(values: Vector[Any]): Report[B] = {
        val r1 = outer.update(values)
        val r2 = other.update(values)
        r1 >> r2
      }

      def close(): B = { outer.close(); other.close() }
    }

}