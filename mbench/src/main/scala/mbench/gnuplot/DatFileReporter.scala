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
import benchmark.{ Reporter, Report, Label, ConsoleReporter }

object DatFileReporter extends Reporter[DatFile] {
  import benchmark.Config

  def open(benchName: String, testName: String, config: Config[_, _, _], labels: Vector[Label[_]]): Report[DatFile] =
    apply(MBench.benchmarkFolder(benchName)).open(benchName, testName, config, labels)

  def apply(folder: Folder): Reporter[DatFile] =
    apply(folder, MBench.logout)

  import java.io.PrintStream

  def apply(folder: Folder, stream: PrintStream): Reporter[DatFile] = ConsoleReporter(stream) >> new Reporter[DatFile] {

    def open(benchName: String, testName: String, config: Config[_, _, _], labels: Vector[Label[_]]): Report[DatFile] = {
      new DatReport(config.value.toString, DatFile.mk(folder, benchName, testName, config.names, labels))
    }
  }

  class DatReport(configString: String, datFile: DatFile) extends Report[DatFile] {

    private val out = datFile.file.printWriter(true)

    private def labels = datFile.labels

    import DatReport.{ format, cFmt }
    import DatFile.CCH

    preamble()

    def preamble() {
      Host.describeRuntime.foreach(comment)
      comment("")
      comment("date:        " + MBench.getDateString("dd/MM/yyyy HH::mm::ss"))
      comment("benchmark:   " + datFile.benchName)
      comment("test:        " + datFile.testName)
      comment("config:      " + configString)

      out.println()

      out.print(CCH)
      out.print("| ")
      out.print(cFmt format labels.head.toString)
      labels.tail foreach { l =>
        out.print(" | ")
        out.print(cFmt format l.toString)
      }
      out.println(" |")
    }

    def comment(s: String): Unit = {
      out.print(CCH)
      out.println(s)
    }

    def update(values: Vector[Any]): DatReport = {
      out.print(format(values.head))

      val vs = values.tail.iterator

      while (vs.hasNext) {
        out.print(' ')
        out.print(format(vs.next))
      }
      out.println()
      this
    }

    def close(): DatFile = {
      out.println()
      comment("end time:" + MBench.getDateString(new java.util.Date(), "dd/MM/yyyy HH::mm::ss"))
      out.close()
      datFile
    }

  }

  object DatReport {

    val cellWidth = java.lang.Long.MIN_VALUE.toString.length

    private val dFmt: String =
      "%" + cellWidth + "d"

    private val sFmt: String =
      "%" + cellWidth + "s"

    private val cFmt: String =
      "%" + (cellWidth - 2) + "s"

    trait CustomFormat {
      def format(a: Any): String
    }

    private val fFmt: CustomFormat = new CustomFormat {
      val f = new java.text.DecimalFormat("#.000", new java.text.DecimalFormatSymbols(java.util.Locale.US))
      def format(a: Any): String =
        sFmt format (f format a)
    }

    def format(value: Any): String = value match {
      case l: Long => dFmt format l
      case d: Double => fFmt format d
      case i: Int => dFmt format i
      case f: Float => fFmt format f
      case s: Short => sFmt format s
      case _ => sFmt format value.toString
    }

  }
}
