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

package mbench.benchmarks

import scala.collection.Map
import scala.collection.mutable
import scala.collection.immutable

object Maps {

  def testAdd(map: Map[Int, Int], n: Int): Map[Int, Int] = {
    var i = 0
    var m = map
    while (i < n) {
      m += i -> i
      i += 1
    }
    m
  }

  def testDel(map: Map[Int, Int], n: Int): Map[Int, Int] = {
    var i = 0
    var m = map
    while (i < n) {
      m -= i
      i += 1
    }
    m
  }

  /* Hello static dispatch: test methods above would be extremely slow with mutable Map
   * because += must clone the map each time it is invoked. This is why we need these.
   */
  def testMAdd(map: mutable.Map[Int, Int], n: Int): mutable.Map[Int, Int] = {
    var i = 0
    var m = map
    while (i < n) {
      m += i -> i
      i += 1
    }
    m
  }

  def testMDel(map: mutable.Map[Int, Int], n: Int): mutable.Map[Int, Int] = {
    var i = 0
    var m = map
    while (i < n) {
      m -= i
      i += 1
    }
    m
  }

  import mbench.benchmark._
  import mbench.gnuplot._
  import mbench.benchmark.TestSeq.{ test, ignore }

  val MapTest = TestSeq.static[immutable.Map[Int, Int], Int]("", (map, n) =>
    for {
      m <- test("add", testAdd(map, n))
      m <- test("del", testDel(m, n))
      _ <- ignore(assert(m.isEmpty))
    } yield ()
  )

  val MMapTest = TestSeq.static[mutable.Map[Int, Int], Int]("", (map, n) =>
    for {
      m <- test("add", testMAdd(map, n))
      m <- test("del", testMDel(m, n))
      _ <- ignore(assert(m.isEmpty))
    } yield ()
  )

  val immutableMap = Config.static("immutable-map", Map.empty[Int, Int])
  val mutableMap = Config.static("mutable-map", mutable.Map.empty[Int, Int])
  val openMap = Config.static("open-map", mutable.OpenHashMap.empty[Int, Int])

  val input = Seq(1, 5, 10).map(_ * 50000)
  val elems = Label[Int]("elems")
  val throughput = Column.throughput(elems)

  val benchmark = Benchmark("maps", input, elems, warmups = 2, runs = 5).add(throughput)

  def main(args: Array[String]) = {
    val ires = benchmark.seq(immutableMap, MapTest)
    val mres = benchmark.seq(mutableMap, MMapTest)
    val ores = benchmark.seq(openMap, MMapTest)

    val plots = Gnuplot(ires ++ mres ++ ores, Plot.xticList(input: _*))
    Gnuplot.save(plots)
  }

}