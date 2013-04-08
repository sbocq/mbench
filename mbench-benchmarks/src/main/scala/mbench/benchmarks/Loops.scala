package mbench.benchmarks

object Loops {

  def testWhile(cycles: Int): Unit = {
    var ary = new Array[Int](16)
    var i = 0
    while (i < cycles) {
      ary(i & 0xF) = i
      i += 1
    }
    println(ary(0) + "," + ary(1))
  }

  def testFor(cycles: Int): Unit = {
    var ary = new Array[Int](16)
    for (i <- 0 until cycles) {
      ary(i & 0xF) = i
    }
    println(ary(0) + "," + ary(1))
  }

  def testWhileNew(cycles: Int): Unit = {
    case class Box(i: Int)
    var ary = new Array[Box](16)
    var i = 0
    while (i < cycles) {
      ary(i & 0xF) = Box(i)
      i += 1
    }
    println(ary(0) + "," + ary(1))
  }

  import mbench.benchmark._
  import mbench.gnuplot._

  val input = (1 to 3) map (_ * 5000000)
  val cycles = Label[Int]("cycles")
  val throughput = Column.throughput(cycles)

  val benchmark = Benchmark("loops", input, cycles, warmups = 2, runs = 5).add(throughput)

  def main(args: Array[String]) = {

    val tests = Seq(
      Test.input("while", testWhile),
      Test.input("for", testFor),
      Test.input("while-new", testWhileNew)
    )

    val datFiles = tests.map(benchmark(_))

    val plots = Gnuplot(datFiles)
    Gnuplot.save(plots)

  }
}