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
import benchmark.{ Label, Measure, formatFileName }

/**
 * This object contains many methods to create plots and save them using
 * reasonable defaults.
 */
object Gnuplot {

  private def allLabels(datFiles: Seq[DatFile]): Seq[Label[_]] =
    Label.time +: datFiles.map(f => f.labels.drop(Measure.labels.size + 1)).flatten.distinct

  def apply(datFile: DatFile, settings: Seq[String], label: Label[_]): Plot =
    Plot.single(datFile.testName, datFile, settings, label)

  def apply(datFile: DatFile, settings: Seq[String], labels: Seq[Label[_]]): Seq[Plot] =
    labels map (label => apply(datFile, settings, label))

  def apply(datFile: DatFile, settings: Seq[String], label: Label[_], labels: Label[_]*): Seq[Plot] =
    apply(datFile, settings, label +: labels)

  def apply(datFile: DatFile, s1: String, ss: String*): Seq[Plot] =
    apply(datFile, s1 +: ss, allLabels(Seq(datFile)))

  // Single plot -> no settings

  def apply(datFile: DatFile, label: Label[_]): Plot =
    apply(datFile, Seq.empty, label)

  def apply(datFile: DatFile, labels: Seq[Label[_]]): Seq[Plot] =
    labels map (label => apply(datFile, label))

  def apply(datFile: DatFile, label: Label[_], labels: Label[_]*): Seq[Plot] =
    apply(datFile, label +: labels)

  def apply(datFile: DatFile): Seq[Plot] =
    apply(datFile, allLabels(Seq(datFile)))

  // Multi plots -> explicit title -> with settings

  def apply(title: String, datFiles: Seq[DatFile], settings: Seq[String], label: Label[_]): Plot =
    Plot.multi(title, datFiles, settings, label)

  def apply(title: String, datFiles: Seq[DatFile], settings: Seq[String], labels: Seq[Label[_]]): Seq[Plot] = {
    val labs = if (labels.isEmpty) allLabels(datFiles) else labels
    labels map (label => apply(title, datFiles, settings, label))
  }

  def apply(title: String, datFiles: Seq[DatFile], settings: Seq[String], label: Label[_], labels: Label[_]*): Seq[Plot] =
    apply(title, datFiles, settings, label +: labels)

  def apply(title: String, datFiles: Seq[DatFile], s1: String, ss: String*): Seq[Plot] =
    apply(title, datFiles, s1 +: ss, allLabels(datFiles))

  //  
  // Multi plots -> explicit title
  //

  def apply(title: String, datFiles: Seq[DatFile], label: Label[_]): Plot =
    apply(title, datFiles, Seq.empty, label)

  def apply(title: String, datFiles: Seq[DatFile], labels: Seq[Label[_]]): Seq[Plot] = {
    val labs = if (labels.isEmpty) allLabels(datFiles) else labels
    labels map (label => apply(title, datFiles, label))
  }

  def apply(title: String, datFiles: Seq[DatFile], label: Label[_], labels: Label[_]*): Seq[Plot] =
    apply(title, datFiles, label +: labels)

  def apply(title: String, datFiles: Seq[DatFile]): Seq[Plot] =
    apply(title, datFiles, allLabels(datFiles))

  // Multi plots (guess title)
  import Utils._

  private def guessTitle(datFiles: Seq[DatFile]): String = {
    val dfs = filterOutIdeal(datFiles)

    def include(f: DatFile => String): Option[String] =
      if (exists(dfs, f)) isUnique(dfs, f) else None

    val title = formatFileName(Seq(
      include(_.testName).orElse(Some(benchmarkName(dfs))),
      include(_.configNames.mkString("_"))
    ))

    if (title.isEmpty)
      "???"
    else
      title
  }

  private def benchmarkName(datFiles: Seq[DatFile]): String = {
    val names = datFiles.map(_.benchName).distinct
    if (names.length == 1) names.head else "overview"
  }

  def apply(datFiles: Seq[DatFile], labels: Seq[Label[_]]): Seq[Plot] =
    apply(guessTitle(datFiles), datFiles, labels)

  def apply(datFiles: Seq[DatFile], label: Label[_], labels: Label[_]*): Seq[Plot] =
    apply(guessTitle(datFiles), datFiles, label, labels: _*)

  def apply(datFiles: Seq[DatFile]): Seq[Plot] =
    apply(guessTitle(datFiles), datFiles)

  // multi plots -> with settings

  def apply(datFiles: Seq[DatFile], settings: Seq[String], labels: Seq[Label[_]]): Seq[Plot] =
    apply(guessTitle(datFiles), datFiles, settings, labels)

  def apply(datFiles: Seq[DatFile], settings: Seq[String], label: Label[_], labels: Label[_]*): Seq[Plot] =
    apply(guessTitle(datFiles), datFiles, settings, label, labels: _*)

  def apply(datFiles: Seq[DatFile], s1: String, ss: String*): Seq[Plot] =
    apply(guessTitle(datFiles), datFiles, s1, ss: _*)

  //
  // multi plots -> save methods
  //

  def save(folder: Folder, plot: Plot): Unit =
    plot.save(folder)

  def save(folder: Folder, plots: Seq[Plot]): Unit =
    plots.foreach(save(folder, _))

  def save(folder: String, plot: Plot): Unit =
    plot.save(MBench.folder / folder)

  def save(folder: String, plots: Seq[Plot]): Unit =
    plots.foreach(save(folder, _))

  // save methods with guess work
  def save(plot: Plot): Unit =
    save(MBench.benchmarkFolder(plot.benchmarkName), plot)

  def save(plots: Seq[Plot]): Unit =
    plots.foreach(save)

}
