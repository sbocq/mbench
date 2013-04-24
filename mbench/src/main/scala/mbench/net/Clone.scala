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
package net

import fs.Path
import java.io.{ InputStreamReader, BufferedReader }

import mbench.benchmark._
import java.io.{ ObjectOutputStream, OutputStream, ObjectInputStream, InputStream }
import gnuplot.{ DatFileReporter, DatFile }

private[mbench] object Clone {

  val localhost = java.net.InetAddress.getByName("127.0.0.1")
  val port = 9876
  val address = new java.net.InetSocketAddress(localhost, port)

  def silence(in: InputStream): Unit = new Thread {
    override def run(): Unit = {
      val buffer = new Array[Byte](8192)
      while (in.read(buffer) >= 0) {}
    }
  }.start()

  def bridge(in: InputStream, out: OutputStream): Unit = new Thread {
    override def run(): Unit = {
      //val i = new java.io.InputStreamReader(in)
      //val o = new java.io.OutputStreamWriter(out)
      val buffer = new Array[Byte](8192)
      var length = 0
      while ({ length = in.read(buffer); length } >= 0)
        out.write(buffer, 0, length)
    }
  }.start()

  def start[R](benchmark: Benchmark[_, _, _], config: Config[_, _, _], test: Test[_, _, _], quiet: Boolean): R =
    go("mbench.net.TestClone",
      TestClone.writeBenchmark(_, benchmark, config, test),
      TestClone.readResult[R](_), quiet)

  def start[R](benchmark: Benchmark[_, _, _], config: Config[_, _, _], test: TestSeq[_, _, _], quiet: Boolean): Seq[R] =
    go("mbench.net.TestSeqClone",
      TestSeqClone.writeBenchmark(_, benchmark, config, test),
      TestSeqClone.readResults(_), quiet)

  import java.net.ServerSocket
  lazy val ssock = {
    val ssock = new ServerSocket()
    ssock.bind(Clone.address)
    ssock.setReuseAddress(true)
    ssock
  }

  def go[T](main: String, write: ObjectOutputStream => Unit, read: ObjectInputStream => T, quiet: Boolean): T = {

    val pathSep = System.getProperty("path.separator")

    val scalaLibrary = {
      val p = System.getProperty("sun.boot.class.path").split(pathSep).filter(_.contains("scala-library.jar"))
      if (p.isEmpty) "" else p.head
    }

    val javaHome = System.getProperty("java.home")
    val javaBin = (Path(javaHome) / "bin" / "java").toString

    val opts = Host.jvmOptions.filter(_.startsWith("-X"))
    val classPath = Seq(System.getProperty("java.class.path"), scalaLibrary).mkString(pathSep)

    import scala.collection.JavaConversions._

    val mbenchProps = System.getProperties.filter(_._1.startsWith("mbench.")) +
      (("mbench." + MBench.newDateDir.name, MBench.newDateDir.get.toString))

    val args = new java.util.LinkedList[String]()
    args.add(javaBin)
    args.add("-server")
    args.add("-cp")
    args.add(classPath)
    mbenchProps.foreach { case (name, value) => args.add("-D" + name + "=" + value) }
    opts.foreach(args.add(_))
    MBench.microJvmOptions.get.foreach(args.add(_))
    args.add(main)

    //println(System.getProperties.foreach(println))
    if (MBench.verbose.get)
      MBench.logout.println("Cloning VM with arguments:" + args.mkString(","))

    ssock

    val builder = new ProcessBuilder(args)
    val p = builder.start()

    if (quiet)
      // p.getInputStream().close() performance drops when closing the stream c.f. molecule-core/prime-sieve
      silence(p.getInputStream())
    else
      bridge(p.getInputStream(), System.out)

    bridge(p.getErrorStream(), System.err)

    val csock = ssock.accept()
    csock.setSoLinger(true, 0)

    try {
      val out = new ObjectOutputStream(csock.getOutputStream())
      write(out)

      val in = new ObjectInputStream(csock.getInputStream())
      val result = read(in)

      csock.close()
      result

    } catch {
      case t: Throwable => csock.close(); println(t); throw t
    }
  }
}
