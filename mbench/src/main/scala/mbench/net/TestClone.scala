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

private[mbench] object TestClone {

  import mbench.benchmark._
  import java.io.{ ObjectOutputStream, ObjectInputStream }
  import gnuplot.{ DatFileReporter, DatFile }

  def writeBenchmark(out: ObjectOutputStream,
    benchmark: Benchmark[_, _, _],
    config: Config[_, _, _],
    test: Test[_, _, _]) {
    out.writeObject(benchmark)
    out.writeObject(config)
    out.writeObject(test)

  }

  def readBenchmark(in: ObjectInputStream): (Benchmark[Any, Any, Any], Config[Any, Any, Any], Test[Any, Any, Any]) = {
    val b = in.readObject().asInstanceOf[Benchmark[Any, Any, Any]]
    val c = in.readObject().asInstanceOf[Config[Any, Any, Any]]
    val t = in.readObject().asInstanceOf[Test[Any, Any, Any]]
    (b, c, t)
  }

  def writeResult(out: ObjectOutputStream, result: Any) {
    out.writeObject(result)
  }

  def readResult[R](in: ObjectInputStream): R =
    in.readObject().asInstanceOf[R]

  def main(args: Array[String]) {
    import java.net.Socket
    val csock = new Socket()
    csock.setReuseAddress(true)
    csock.connect(Clone.address)

    try {
      val in = new ObjectInputStream(csock.getInputStream())
      val (b, c, t) = readBenchmark(in)

      val datFile = b.local(c, t)

      System.out.close()
      System.err.close()

      val out = new ObjectOutputStream(csock.getOutputStream())
      writeResult(out, datFile)
    } catch {
      case t => System.err.println(t)
    } finally {
      csock.close();
    }
  }
}