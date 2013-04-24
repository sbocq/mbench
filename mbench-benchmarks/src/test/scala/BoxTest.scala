
/**
 * A Micro benchmark test
 */
class BoxTest extends org.scalatest.FlatSpec {

  import org.scalatest._
  import BoxTest._

  def assertSimilar(testName: String, mtime: Double, ltime: Double) =
    assert((math.abs(mtime - ltime) / mtime * 100) < 20 || (mtime < ltime), // somehow this can happen in some runs
      testName + ":mbench time [" + mtime + "us] differs importantly from local time [" + ltime + "us]")

  "MBench" should "report similar results as local benchmark for sum tests" in {
    val msrs = sums.map(benchmark(cfg, _).rows.map(_.tail.head.asInstanceOf[Double] * 1000000).head)
    println("mbench sums:" + msrs)

    val lrs = Seq(timesum(timeprimsum()),
      timesum(timelibsum()),
      timesum(timeminisum()),
      timesum(timeiosum()))
    println("local sums:" + lrs)

    sums.zip(msrs.zip(lrs)).foreach { case (t, (m, l)) => assertSimilar(t.name, m, l) }
  }

  it should "report similar results as local benchmark for count tests" in {
    val mcrs =
      counts.map(benchmark(cfg, _).rows.map(_.tail.head.asInstanceOf[Double] * 1000000).head)
    println("mbench counts:" + mcrs)

    val lrs = Seq(timecount(timeprimcountnan()),
      timecount(timelibcountnan()),
      timecount(timeminicountnan()),
      timecount(timeiocountnan()))
    println("local counts:" + lrs)

    counts.zip(mcrs.zip(lrs)).foreach { case (t, (m, l)) => assertSimilar(t.name, m, l) }
  }

}

object BoxTest {

  private[this] final val array = Array.tabulate(10000) { i => if (i % 3 == 0) Double.NaN else 1.1 }

  import java.lang.Double.{ doubleToRawLongBits => d2b, longBitsToDouble => b2d }
  import java.lang.Double.{ isNaN => nan }
  abstract class MiniFun2 {
    def apply(a: Long, b: Long): Long
  }
  def minifoldD(a: Array[Double], zero: Long, mf: MiniFun2) = {
    var i = 0
    var s = zero
    while (i < a.length) {
      s = mf(s, d2b(a(i)))
      i += 1
    }
    s
  }
  def minisum(a: Array[Double]): Double = {
    val mf = new MiniFun2 { def apply(a: Long, b: Long) = d2b(b2d(a) + b2d(b)) }
    b2d(minifoldD(a, d2b(0.0), mf))
  }

  def timeminisum() = {
    val start = System.nanoTime
    val a = minisum(array)
    val time_us = (System.nanoTime - start) / 1000
    (time_us.toDouble, a)
  }

  def minicountnan(a: Array[Double]): Int = {
    val mf = new MiniFun2 {
      def apply(a: Long, b: Long) = a + (if (nan(b2d(b))) 1 else 0)
    }
    minifoldD(a, d2b(0.0), mf).toInt
  }

  def timeminicountnan() = {
    val start = System.nanoTime
    val a = minicountnan(array)
    val time_us = (System.nanoTime - start) / 1000
    (time_us.toDouble, a)
  }

  trait In[@specialized A] { def apply(a: A): Unit }
  trait Out[@specialized B] { def init(zero: B): Unit; def get: B }
  def ifoldD[@specialized A](a: Array[A], fi: In[A]): fi.type = {
    var i = 0
    while (i < a.length) {
      fi(a(i))
      i += 1
    }
    fi
  }
  def iosum(a: Array[Double]): Double = {
    class anon extends In[Double] with Out[Double] {
      private[this] var acc: Double = _
      def apply(a: Double) { acc += a }
      def init(b: Double) { acc = b }
      def get = acc
    }
    val af = new anon
    af.init(0.0)
    ifoldD(a, af).get
  }

  def timeiosum() = {
    val start = System.nanoTime
    val a = iosum(array)
    val time_us = (System.nanoTime - start) / 1000
    (time_us.toDouble, a)
  }

  def iocountnan(a: Array[Double]): Int = {
    class anon extends In[Double] with Out[Int] {
      private[this] var acc: Int = _
      def apply(a: Double) { acc += (if (nan(a)) 1 else 0) }
      def init(b: Int) { acc = b }
      def get = acc
    }
    val af = new anon
    af.init(0)
    ifoldD(a, af).get
  }
  def timeiocountnan() = {
    val start = System.nanoTime
    val a = iocountnan(array)
    val time_us = (System.nanoTime - start) / 1000
    (time_us.toDouble, a)
  }

  def libsum(a: Array[Double]): Double = a.sum
  def timelibsum() = {
    val start = System.nanoTime
    val a = libsum(array)
    val time_us = (System.nanoTime - start) / 1000
    (time_us.toDouble, a)
  }

  def libcountnan(a: Array[Double]): Int = a.count(x => nan(x))
  def timelibcountnan() = {
    val start = System.nanoTime
    val a = libcountnan(array)
    val time_us = (System.nanoTime - start) / 1000
    (time_us.toDouble, a)
  }

  def primsum(a: Array[Double]): Double = {
    var i = 0
    var s = 0.0
    while (i < a.length) { s += a(i); i += 1 }
    s
  }
  def timeprimsum() = {
    val start = System.nanoTime
    val a = primsum(array)
    val time_us = (System.nanoTime - start) / 1000
    (time_us.toDouble, a)
  }

  def primcountnan(a: Array[Double]): Int = {
    var i, s = 0
    while (i < a.length) { s += (if (nan(a(i))) 1 else 0); i += 1 }
    s
  }
  def timeprimcountnan() = {
    val start = System.nanoTime
    val a = primcountnan(array)
    val time_us = (System.nanoTime - start) / 1000
    (time_us.toDouble, a)
  }

  import mbench.benchmark._
  def timesum(sumf: => (Double, Double)): Double = {
    var i = 0
    var tmp = 0.0
    while (i < 200) {
      tmp += sumf._2
      i += 1
    }
    Measure.medianOf((1 to 5).map(_ => sumf._1))
  }
  def timecount(cntf: => (Double, Int)): Double = {
    var i = 0
    var tmp = 0
    while (i < 200) {
      tmp += cntf._2
      i += 1
    }
    Measure.medianOf((1 to 5).map(_ => cntf._1))
  }
  val input = Seq(10000)
  val ilabel = Label[Int]("Elems")
  val throughput = Column.throughput(ilabel)

  val benchmark =
    Benchmark("BoxTest", input, ilabel, TableReporter, warmups = 2, runs = 5)
      .add(throughput)

  val cfg = Config.runtime[Int, Array[Double]](
    "",
    n => Array.tabulate(n) { i => if (i % 3 == 0) Double.NaN else 1.1 },
    _ => ())

  // Ignore input i.e. elems
  def mkTest(name: String, f: Array[Double] => Unit) =
    Test.runtime[Array[Double], Int](name, (a, n) => f(a))

  val counts = Seq(
    mkTest("primcount", primcountnan),
    mkTest("libcount", libcountnan),
    mkTest("minicount", minicountnan),
    mkTest("iocount", iocountnan)
  )

  val sums = Seq(
    mkTest("primsum", primsum),
    mkTest("libsum", libsum),
    mkTest("minisum", minisum),
    mkTest("iosum", iosum)
  )

}
