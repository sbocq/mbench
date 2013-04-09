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

import fs.Folder
import benchmark.{ Label, formatNames }

/**
 * The class of plots.
 */
class Plot(
    val fileName: String,
    val label: Label[_],
    val datFiles: Seq[DatFile],
    val settings: Seq[String]) {

  def benchmarkName: String = {
    val names = datFiles.map(_.benchName).distinct
    if (names.length == 1) names.head else "overview"
  }

  import Utils._

  private def include(f: DatFile => String): Option[DatFile => Option[String]] =
    if (exists(datFiles, f)) {
      if (isUnique(datFiles, f).isDefined) None
      else {
        Some(datFile => {
          val s = f(datFile)
          if (s.isEmpty) None else Some(s)
        })
      }
    } else None

  private def combineF(fs: Seq[Option[DatFile => Option[String]]]): DatFile => Some[String] = datFile =>
    if (fs.isEmpty) Some("???")
    else Some(formatNames(fs.flatten.map(_.apply(datFile)), ":"))

  private val plotLineTitle: DatFile => Option[String] =
    if (datFiles.isEmpty || datFiles.tail.isEmpty) // single line 
      _ => None
    else {
      combineF(Seq(
        include(_.testName),
        include(_.configNames.mkString(":"))
      ))
    }

  private def plotLine(plotFolder: Folder, srcFile: DatFile) = {
    val dataCol = srcFile.columnIndexOf(label)
    val cvarCol = srcFile.columnIndexOf(Label.cvar)

    val s = "'%1$s' using 1:%2$d:($%2$d*$%3$d)/100 with errorlines".format(
      plotFolder.relativePathTo(srcFile),
      dataCol,
      cvarCol
    )

    val title = plotLineTitle(srcFile)
    s ++ " title \"" + title.getOrElse("") + "\""
  }

  def fileNameLabel: String = {
    val R = ("([a-zA-Z0-9.]*)").r
    val ylab = R.findFirstIn(label.name).getOrElse("???").trim
    fileName + "%" + ylab + ".plt"
  }

  private[this] final val titleSuffix: String = " - " + Host.Hardware.shortString + ", " + Host.javaRuntimeVersion + {
    val options = Host.uncommonJvmSpecialOptions.filterNot(Gnuplot.titleIncludeOptions.get.contains)
    if (options.isEmpty) "" else options.mkString(", [", ",", "]")
  }

  def save(folder: Folder): Unit = {
    if (datFiles.isEmpty)
      return System.out.println("Plot " + fileName + ":no data")

    val out = folder.newFile(fileNameLabel).printWriter(false)

    val xlabel = datFiles.head.labels(0)

    val titlePrefix =
      if (fileName.startsWith(benchmarkName)) // overview
        fileName.replace('_', ':')
      else
        benchmarkName + ":" + fileName.replace('_', ':')

    out.println("set title \"" + titlePrefix + titleSuffix + "\"")
    out.println("set xlabel \"" + xlabel.capitalize + "\"")
    out.println("set ylabel \"" + label.capitalize + "\"")
    settings.foreach(out.println)
    out.println("#set key out")
    out.println()

    out.println("#set term png")
    out.println("#set output \"" + fileName + "%" + label.name + ".png\"")
    out.println("#set term postscript eps enhanced")
    out.println("#set output \"" + fileName + "%" + label.name + ".eps\"")
    out.println()

    out.print("plot " + plotLine(folder, datFiles.head))
    datFiles.tail.foreach { datFile =>
      out.println(",\\")
      out.print(plotLine(folder, datFile))
    }
    out.println()
    out.println("pause -1")
    out.close()
  }

}

object Plot {

  /**
   * Set the major X tics
   */
  def xtics(i: Int): String = "set xtics " + i

  /**
   * Set the major Y tics
   */
  def ytics(i: Int): String = "set ytics " + i

  /**
   * Add tics on the X-axis
   */
  def addxtics(x1: Int, xs: Int*): String =
    "set xtics add (" + x1 + ")" +
      xs.tail.map(xi => ";set xtics add (" + xi + ")")

  /**
   * set logarithmic Y-axis
   */
  val logy: String = "set log y"

  def single(title: String,
    datFile: DatFile,
    settings: Seq[String],
    label: Label[_]): Plot = new Plot(title, label, Seq(datFile), settings)

  def multi(title: String,
    datFiles: Seq[DatFile],
    settings: Seq[String],
    label: Label[_]): Plot = {
    if (datFiles.isEmpty)
      return new Plot(title, label, datFiles, settings)

    val dfs = datFiles.filter(_.labels.contains(label))

    if (dfs.isEmpty)
      return error(title, label, "No .dat file with label " + label + " specified for " + title)

    if (dfs.map(_.labels(0)).distinct.length != 1)
      return error(title, label,
        "multi plots must have the same label for the X-axis. Found: " +
          dfs.map(d => d.file + "->" + d.labels(0)).mkString("[", ",", "]")
      )

    new Plot(title, label, dfs, settings)
  }

  private def error(title: String, label: Label[_], msg: String): Plot = new Plot(title, label, Nil, Seq.empty) {
    override def save(folder: Folder): Unit = {
      val out = folder.newFile(fileNameLabel + "-error").printWriter(false)
      out.println(msg)
      out.close()
    }
  }
}