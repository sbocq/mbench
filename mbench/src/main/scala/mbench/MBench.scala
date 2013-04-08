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

import properties._
import fs.Path

/**
 * MBench properties
 *
 *  Properties can be overridden by creating a `mbench.properties` file. For example, to override
 *  the root directory where reports are created, add the property:
 *
 *  `reports.dir = myreports`
 *
 *  in `mbench.properties`.
 *
 */
object MBench {

  /**
   * The file name holding the main configuration properties.
   */
  private val properties = Properties.load("mbench.properties")

  /**
   * The directory where to store the results of the benchmark.
   *
   * (default: reports)
   */
  val reportsDir = Property("reports.dir", properties, Path("reports"))

  /**
   * By default, this program logs information on the standard error.
   * The this property to true to log on the standard output instead.
   *
   * (default: false)
   */
  val logStdout = Property("log.stdout", properties, false)

  /**
   * Options that should not be reported if they are used.
   *
   * (default: +DoEscapeAnalysis,MaxPermSize)
   */
  val commonSpecialOptions = Property("common.special.options", properties, Many("+DoEscapeAnalysis", "MaxPermSize"))

  /**
   * Print some more information.
   *
   * (default: false)
   */
  val verbose = Property("verbose", properties, false)

  /**
   * The directory named after the date when the benchmark was ran.
   *
   * (default: the current date)
   */
  val newDateDir = Property("new.date.dir", properties, Path(getDateString("yyyy-MM-dd-HH'h'mm")))

  /**
   * Lock the date folder of a benchmark.
   *
   * Set `lock.date` to `true` when fine tuning a benchmark. This way, reports
   * will always be generated in the same date directory. This behavior can
   * be turned off either by removing this property or temporarily, by deleting the
   * 'mbench.last.date' created after a benchmark has ran.
   *
   * (default: false)
   */
  val lockDate = Property("lock.date", properties, false)

  private val lastDirFileName = "mbench.last.date"
  private val ld = Properties.load(lastDirFileName)
  private val lastDate = Property("last.date.dir", ld, "")

  val logout = if (logStdout.get) System.out else System.err

  private[mbench] val dateDir: Path = {
    def resetDateDir: Path = {
      val file = new java.io.File(lastDirFileName)
      if (file.exists()) file.delete()
      val tdir = newDateDir.get
      val out = new java.io.PrintWriter(new java.io.FileWriter(file, true))
      out.println(lastDate.name + "=" + tdir)
      out.close()
      tdir
    }

    if (lockDate.get) {
      if (lastDate.get.isEmpty) {
        resetDateDir
      } else {
        val path = reportsDir.get / Host.name / lastDate.get
        if (path.exists) Path(lastDate.get) else resetDateDir
      }
    } else resetDateDir
  }

  /**
   * Folder where benchmark results are saved
   */
  private[mbench] val folder = fs.Folder(reportsDir.get) / Host.name / dateDir

  /**
   * Return the leaf folder in which benchmark files are created. Using the default
   * properties, this folder is `reports/date/[-XX options]/benchmarkName`.
   *
   * @param benchmarkName the name of the benchmark.
   * @return the benchmark folder.
   */
  def benchmarkFolder(benchmarkName: String) = {
    // This is not worth mentionaing as it is turned on by default on jdk7
    val sOpts = mbench.Host.uncommonJvmSpecialOptions.mkString("")

    if (sOpts.isEmpty) folder / benchmarkName else folder / sOpts / benchmarkName
  }

  import java.util.Date
  import java.text.DateFormat
  import java.text.SimpleDateFormat

  /**
   * Format the current date according a specific date format
   *
   * @param format the date format.
   * @return the current date formatted as a string.
   */
  def getDateString(format: String): String =
    getDateString(new Date(), format)

  /**
   * Format a date according a specific date format
   *
   * @param date the date.
   * @param format the date format.
   * @return the date formatted as a string.
   */
  def getDateString(date: Date, format: String): String = {
    val dateFormat = new SimpleDateFormat(format);
    dateFormat.format(date);
  }

}