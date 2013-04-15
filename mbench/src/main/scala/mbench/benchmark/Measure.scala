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

package mbench.benchmark

/**
 * Class that captures the median of the execution times and the coefficient
 * of variation of a test over several runs for a given input of a benchmark.
 *
 * @param time The median of the execution times (in seconds).
 * @param cvar The coefficient of variation (in %).
 */
case class Measure(
    time: Double, // Duration of benchmark
    cvar: Double // Coefficient of variation of time
    ) {

  override def toString =
    (Measure.labels zip Seq(time, cvar)).map { case (l, v) => l.format(v) }.mkString(", ")
}

object Measure {

  private[mbench] val labels = Vector(Label.time, Label.cvar)

  /**
   * Measure the elapsed time in seconds to evaluate a thunk of code.
   *
   * @param thunk the thunk to evaluate.
   * @return the time and the result of the thunk
   */
  def time[A](thunk: => A): (Double, A) = {
    collectGarbage()
    val start = System.nanoTime
    val a = thunk
    val time_us = (System.nanoTime - start) / 1000
    (time_us.toDouble / 1000000, a)
  }

  /**
   * After every test run, we collect garbage by calling System.gc()
   * and sleeping for a short while to make sure that the garbage
   * collector has had a chance to collect objects.
   */
  private def collectGarbage() {
    for (i <- 0 to 2) {
      System.gc()
      try {
        Thread.sleep(10)
      } catch {
        case e: InterruptedException =>
          Thread.currentThread().interrupt()
          return
      }
    }
  }

  /**
   * Compute the median of a sequence of numeric values.
   *
   * @param ns the sequence of numeric values
   * @return the median
   */
  def medianOf[A: Numeric](ns: Seq[A]): Double = {
    val n = implicitly[Numeric[A]]
    val ds = ns.map(n.toDouble)

    val runs = ds.length
    val midp = (runs - 1) / 2
    val ots = ds.sorted
    if (runs % 2 == 0)
      (ots(midp) + ots(midp + 1)) / 2
    else
      ots(midp)
  }

  /**
   * Compute the mean of a sequence of numeric values.
   *
   * @param ns the sequence of numeric values
   * @return the mean
   */
  def meanOf[A: Numeric](ns: Seq[A]): Double = {
    val n = implicitly[Numeric[A]]
    val ds = ns.map(n.toDouble)

    ds.sum / ds.length
  }

  /**
   * Compute the sample standard deviation of a sequence of numeric values.
   *
   * @param ns the sequence of numeric values
   * @param mean the mean of the sequence
   * @return the sample standard deviation
   */
  def sampleStdDevOf[A: Numeric](ns: Seq[A], mean: Double): Double = {
    val n = implicitly[Numeric[A]]
    val ds = ns.map(n.toDouble)

    def square(x: Double) = x * x

    val runs = ds.length
    math.sqrt(ds.map(t => square(t - mean)).sum / (runs - 1))
  }

  /**
   * Compute the sample standard deviation of a sequence of numeric values.
   *
   * @param ns the sequence of numeric values
   * @return the sample standard deviation
   */
  def sampleStdDevOf[A: Numeric](ts: Seq[A]): Double = {
    val mean = meanOf(ts)
    sampleStdDevOf(ts, mean)
  }

  /**
   * Compute the coefficient of variation (in %) of a sequence of numeric values.
   *
   * @param ns the sequence of numeric values
   * @return   the coefficient of variation
   */
  def variationOf[A: Numeric](ns: Seq[A]): Double = {
    if (ns.length <= 1) return 0

    val mean = meanOf(ns)
    val sd = sampleStdDevOf(ns, mean)
    if (mean == 0)
      if (ns.sum == 0) 0 else 100
    else
      sd / mean * 100
  }

  /**
   * Create a measure object from a sequence of time measurements.
   */
  def apply(times: Seq[Double]): Measure = {
    val time = medianOf(times)
    val cvar = variationOf(times)
    Measure(time, cvar)
  }

  def sum(ms: Seq[Measure]): Measure = {
    val time = ms.map(_.time).sum
    val cvar = meanOf(ms.map(_.cvar))
    Measure(time, cvar)
  }
}
