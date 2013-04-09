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
package gnuplot

import fs.{ FsObject, File }
import benchmark.Label

/**
 * A file that contains data suitable for rendering in Gnuplot.
 *
 * Column values are separated by white spaces. The columns are the following:
 *  - The first column corresponds to input values passed to a benchmark (X-axis).
 *  - The second and third columns store the time and coefficient of variations.
 *  - The other columns correspond are user specified columns (e.g. throughput, speedup, ...)
 *
 * @param  file the file holding the data
 * @param  benchName the name of the benchmark
 * @param  testName the name of the test
 * @param  configName the tag identifying a specific benchmark configuration (might be empty)
 * @param  labels the labels of the each column
 */
case class DatFile(file: File, benchName: String, testName: String, configNames: Seq[String], labels: Vector[Label[_]]) extends FsObject {

  def path = file.path

  /**
   * Find the label identified by its name.
   *
   * @param name the name of the label that must be found
   * @return the label with the same name (case insensitive)
   */
  def labelOf(name: String): Label[_] =
    Label.labelOf(name, labels)

  /**
   * Find the columns number of a label based on its name (case insensitive).
   *
   * Column indexes start at 1.
   *
   * @param name the name of the label whose column number must be known
   * @return the column number of the label
   */
  def columnIndexOf(label: Label[_]): Int =
    Label.indexOf(label, labels) + 1

  def loadColumn[T](label: Label[T]): Seq[Option[T]] =
    DatFile.loadColumn[T](file, label.name, columnIndexOf(label), label.valueOf(_))

  override def toString = "DatFile(" + file.path.name + "," + testName + "," + configNames.mkString("", ",", ",") + labels.mkString("[", ",", "]") + ")"
}

object DatFile {

  /**
   * Comment character
   */
  final val CCH = '#'

  /**
   * Create a file name for a .dat file given some benchmark information
   *
   * @param testName the name of the benchmark
   * @param configName a tag identifying a configuration in a multi-configuration benchmark (might be empty).
   *
   * @return a file name
   */
  def mkFileName(testName: String, configNames: Seq[String]): String =
    benchmark.formatFileName(testName, configNames: _*) + ".dat"

  /**
   * Create a new .dat file for a benchmark.
   *
   * @param folder the folder where the file must belong to
   * @param testName the name of the benchmark
   * @param configName a tag identifying a configuration in a multi-configuration benchmark (might be empty).
   * @param label the labels of the input and the values measured in the benchmarks.
   *
   * @return a .dat file
   */
  def mk(folder: fs.Folder, benchName: String, testName: String, configNames: Seq[String], labels: Vector[Label[_]]): DatFile =
    DatFile(folder.newFile(mkFileName(testName, configNames)), benchName, testName, configNames, labels)

  import scala.collection.mutable.ListBuffer

  private def loadColumn[T](file: File, labelName: String, columnIndex: Int, valueOf: String => T): Seq[Option[T]] = {
    loadColumn(file.name, labelName, valueOf, 1, columnIndex)(file.bufferedReader, new ListBuffer[Option[T]]())
  }

  private[this] final def loadColumn[T](fileName: String, labelName: String, valueOf: String => T, rowIndex: Int, columnIndex: Int)(in: java.io.BufferedReader, lb: ListBuffer[Option[T]]): Seq[Option[T]] = {
    val array = readNextRow(in)
    if (array == null) lb.result()
    else if (array.length < columnIndex) {
      System.err.println("Warning '" + fileName + "': row " + rowIndex + ": missing value for label " + labelName)
      lb.append(None)
      loadColumn(fileName, labelName, valueOf, rowIndex + 1, columnIndex)(in, lb)
    } else {
      val value = array(columnIndex - 1)
      try {
        lb.append(Some(valueOf(value)))
      } catch {
        case _ =>
          System.err.println("Warning '" + fileName + "': row " + rowIndex + ": cannot cast value " + value)
          lb.append(None)
      }
      loadColumn(fileName, labelName, valueOf, rowIndex + 1, columnIndex)(in, lb)
    }
  }

  private[this] final def readNextRow(in: java.io.BufferedReader): Array[String] = {
    val s = in.readLine().trim()
    if (s == null) null
    else if (s.charAt(0) == CCH || s.isEmpty) readNextRow(in)
    else s.split("\\s+")
  }

}