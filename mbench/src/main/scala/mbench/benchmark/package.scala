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

/**
 * Collection of benchmarking functions
 */
package object benchmark {

  /**
   * State of the banchmark
   */
  type State = Map[String, Any]

  private[benchmark] val EmptyState: State = Map.empty

  private[benchmark] def NO_SETUP[I]: (Object, I) => I = (_, i) => i
  private[benchmark] def NO_TEARDOWN[S]: S => Unit = _ => ()
  private[benchmark] def NO_CONFIG[C]: Option[C] = None
  private[benchmark] val NO_MULTI_TAG: String = ""

  val StdoutReporter = ConsoleReporter(System.out)

  val StderrReporter = ConsoleReporter(System.err)

  class EnrichedString(s: String) {
    def perSeconds: String = s + "/" + Label.time.unit
    def toOption: Option[String] = if (s.isEmpty) None else Some(s)
  }

  implicit def enrichString(s: String): EnrichedString = new EnrichedString(s)

  def formatFileName(s1: String, ss: String*): String =
    formatFileName((s1 +: ss).map(_.toOption))

  def formatFileName(ss: Seq[Option[String]]): String =
    formatNames(ss, "_")

  def formatNames(ss: Seq[Option[String]], sep: String): String =
    ss.flatten.mkString(sep)
}