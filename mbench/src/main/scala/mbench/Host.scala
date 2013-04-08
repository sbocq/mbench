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

/**
 * Host properties.
 */
object Host {

  val properties = Properties.load("host.properties")

  /**
   * Number of cores available on the host platform.
   *
   * (default: `Runtime.getRuntime().availableProcessors`)
   */
  val cores = Property("cores", properties, Runtime.getRuntime().availableProcessors)

  import java.net.{ InetAddress, UnknownHostException }

  private[this] def getLocalHostName: String = {
    try {
      InetAddress.getLocalHost().getHostName()
    } catch {
      case uhe: UnknownHostException => // Problem with DNS settings
        InetAddress.getByName("localhost").getHostName
    }
  }

  /** Host name. */
  val name: String = getLocalHostName
  /** OS name. */
  val OS: String = System.getProperty("os.name")
  /** JVM name. */
  val jvmVendor: String = System.getProperty("java.vm.vendor")
  /** JVM version. */
  val jvmVersion: String = System.getProperty("java.vm.version")
  /** Java runtime version. */
  val javaRuntimeVersion = System.getProperty("java.runtime.version")
  /** JVM name. */
  val jvmName: String = System.getProperty("java.vm.name")

  /** JVM options. */
  val jvmOptions: List[String] = {
    import scala.collection.JavaConversions._
    val RuntimemxBean = java.lang.management.ManagementFactory.getRuntimeMXBean()
    val args = RuntimemxBean.getInputArguments().toList
    args.filterNot(s =>
      s.contains("encoding") ||
        s.contains("classpath") ||
        s.contains("-Dmbench.") ||
        s.contains(".jar" /* bug when class path contains white spaces */ )
    )
  }

  /** JVM options that start with `-XX:`. */
  def jvmSpecialOptions: List[String] = jvmOptions.filter(_.startsWith("-XX:")).map(_.substring(4))

  /**
   * JVM options that start with `-XX:` minus the common special options configured by the
   * MBench `common.special.options` property.
   */
  def uncommonJvmSpecialOptions: List[String] =
    jvmSpecialOptions.filterNot(option => MBench.commonSpecialOptions.get.exists(option.startsWith))

  /** Hardware information. */
  object Hardware {
    /** Number of cores. */
    val cores = Host.cores.get
    /** Architecture. */
    val arch = System.getProperty("os.arch")
    /** Size of the different cache levels (unavailable unless configured in `host.properties` as `cache.i=...`). */
    val caches = {
      val l = List.newBuilder[(Int, Int)]
      var i = 1
      var p = properties.get("cache." + i, 0)
      while (p != 0) {
        l += ((i, p))
        i += 1
        p = properties.get("cache." + i, 0)
      }
      Map(l.result: _*)
    }

    /** A list of attributes describing the hardware. */
    def describe: List[String] =
      List(
        "Core(s):        " + cores
      ) ++
        List(
          "arch:           " + arch,
          "caches:         " + caches.map(_ + "KB").mkString("[", ",", "]")
        )

    /** A short string describing the hardware. */
    def shortString = Hardware.cores + "*" + Hardware.arch
  }

  /** A string describing the JVM using its vendor name, JVM name and Java runtime version. */
  val JVM = jvmVendor + " " + jvmName + " " + javaRuntimeVersion

  /** A list of strings describing the runtime. */
  def describeRuntime = List(
    "Host:        " + name + " (" + Hardware.shortString + ")",
    "OS:          " + OS,
    "JVM:         " + JVM,
    "JVM options: " + jvmOptions.mkString("[", ",", "]")
  )

  /** A list of strings describing the runtime and the hardware. */
  def describe: List[String] =
    describeRuntime ++ Hardware.describe

  /** A short string describing the runtime and the hardware. */
  def shortString: String = Hardware.shortString + ", " + javaRuntimeVersion + (
    if (Host.uncommonJvmSpecialOptions.isEmpty) ""
    else ", " + Host.uncommonJvmSpecialOptions.mkString("[", ", ", "]")
  )
}