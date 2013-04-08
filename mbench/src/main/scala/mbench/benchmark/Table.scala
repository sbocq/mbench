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

/**
 * A table that is represented as an array stored in memory.
 */
class Table(val benchName: String, val testName: String, val configName: String, val labels: Vector[Label[_]], val rows: Vector[Seq[Any]]) extends Serializable {

  private def showCell(label: Label[_], value: Any): String =
    '[' + label.format(value) + ']'

  private def showTitle: String =
    benchName + ":" + testName + ":" +
      (if (!configName.isEmpty) configName else "") +
      ":\r\n"

  private def showRow(labels: Seq[Label[_]], values: Seq[Any]): String = {
    val sb = new StringBuffer()
    sb append showCell(labels.head, values.head)
    sb append " -> "

    val ls = labels.tail.iterator
    val vs = values.tail.iterator

    while (!ls.isEmpty) {
      sb append showCell(ls.next, vs.next)
    }
    sb append "\r\n"
    sb.toString
  }

  private def showRows: String =
    rows.foldLeft(new StringBuffer())((sb, v) => sb append showRow(labels, v)).toString

  override def toString =
    showTitle + showRows
}
