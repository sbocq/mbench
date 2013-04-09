# mbench

A microbenchmarking library for the JVM with a Gnuplot backend suitable for comparison and scalability benchmarks.

Features:

- Benchmarks can be launched directly from a main class independently from any specific build environment.
- Each benchmarks is executed in isolation of each other in a fresh clone of the master JVM and automatically inherits its settings (e.g. class paths, system settings, command line options, etc.).
- Benchmarks can evaluate multiple tests for multiple inputs and in various configurations, which makes the library suitable for running scalability and comparison benchmarks.
- The Gnuplot backend that saves the median execution time and the coefficient of variation for each input of the test in a `.dat` files.
- The Gnuplot backend can use the `.dat` files to aggregate the results of multiple benchmarks in a single plot that help visualize how different tests compare with each other.
- End users may specify custom columns that compute additional information in real-time (e.g. speedup or throughput).

See the [current API documentation](http://sbocq.github.io/mbench) for more information.

## Examples

The examples below can be found in the `mbench-benchmarks` project and launched from your favorite IDE or from `sbt` using the `run` command. All the benchmarks shown in this section where run on a 24-cores (2200MHz each) Opteron server with the following JVM flags: `-server` `-XX:+UseNUMA` `-XX:+UseCondCardMark` `-Xss1M` `-XX:MaxPermSize=128m` `-XX:+UseParallelGC` `-XX:+DoEscapeAnalysis` `-Xms1024m` `-Xmx1024m`.

## Loops

The following benchmark compares the performance of several loops for different amount of iterations.

```scala
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
```

The following plots will be automatically generated. If we look at the time plot, as expected, we see that the time increases with the number of cycles.

![loops%time.plt](https://raw.github.com/sbocq/mbench/master/gallery/loops/loops%time.png)

However, this does not tell us if the time increases linearly with the number of cycles. This is why generating a second plot of the 
throughput against the number of cycles is interesting. According to the figure below, we can see that loops are still optimized beyond 15 millions cycles.

![loops%throughput.plt](https://raw.github.com/sbocq/mbench/master/gallery/loops/loops%throughput.png)

## Parallel Loops

The benchmark below measures how well executing these loops in parallel scales with the number of threads on our 24-cores server.

```scala

object ParaLoops {

  import mbench.benchmark._
  import mbench.gnuplot._

  import java.util.concurrent.{ ExecutorService, Executors }

  /* The benchmark will be configured using an executor as runtime configuration 
   * and a number of loop cycles as static configuration. 
   */
  def runtimeConfig(executorName: String, mkExecutor: Int => ExecutorService) =
    Config.runtime[Int, ExecutorService](executorName, mkExecutor, _.shutdown())

  val threadPool = runtimeConfig("thread-pool", Executors.newFixedThreadPool)

  val cycles = Config.static(10000000)

  import mbench.Host.Hardware.cores

  val threads = ((1 to 3) ++ (4 to cores by (if (cores <= 8) 2 else 4)) ++ (Seq(2, 4) map (_ + cores))).distinct

  val ilabel = Label[Int]("threads")

  /* This column uses the cycles in the static configuration to compute 
   * the throughput in cycles per second. 
   */
  val throughput = Column.withConfig[Int, Int, Double]("throughput", "cycles".perSeconds)(
    (threads, cycles, time) => threads * (cycles / time)
  )

  val speedup = throughput.speedupHigherIsBetter

  val benchmark = Benchmark("para-loops", threads, ilabel, warmups = 5, runs = 7)
    .add(throughput).add(speedup)

  def main(args: Array[String]) = {

    /* The tests and the benchmark must use the same setup, which specifies 
     * the executor and the number of cycles to execute.
     */

    def mkTest(loop: Int => Unit)(executor: ExecutorService, cycles: Int, threads: Int) = {
      (1 to threads)
        .map(_ => executor.submit(new Runnable { def run() = loop(cycles) }))
        .foreach(_.get())
    }

    val testWhile = Test("while", mkTest(Loops.testWhile))
    val testWhileNew = Test("while-new", mkTest(Loops.testWhileNew))
    val testFor = Test("for", mkTest(Loops.testFor))

    val idealTimes = benchmark.ideal("speedup", 1 /* cycles don't matter */ , threads => if (threads <= cores) 1 else (threads.toDouble / cores))

    val tests = Seq(testWhile, testWhileNew, testFor)

    val dats = tests.map(benchmark(threadPool and cycles, _))

    val settings = Seq(Plot.xtics(1))
    Gnuplot.save(Gnuplot(dats, settings, throughput))
    Gnuplot.save(Gnuplot(dats :+ idealTimes, settings, speedup.label))

  }

}
```

If we loop at the throughput plot below, we see again that the while-loop wins over Scala's for-comprehension in scala 2.9.3 (contributions to migrate the build to Scala 2.10 are welcome) and that the loop that does excessive boxing is again the worst performer of the three. 

![para-loops%throughput.plt](https://raw.github.com/sbocq/mbench/master/gallery/para-loops/para-loops_thread-pool%throughput.png)

But how well do they scale on multicore hardware with the number of threads? According to the speedup plot shown below, both the while-loop and the for-comprehension come close to the ideal parallel speedup and that it is not worth going beyond 24 threads. The version that does boxing creates so much garbage that it cannot be eliminated efficiently by the JVM above 4 threads (question: can this be improved by tuning the JVM?).

![para-loops%speedup.plt](https://raw.github.com/sbocq/mbench/master/gallery/para-loops/para-loops_thread-pool%speedup.png)

## Maps

The benchmark below illustrates how to benchmark a scenario that performs side-effects that must be executed in a predefined sequence. In this scenario, we measure how fast we can add elements to a hash map and then remove them. At the same time, we compare the performance of immutable and mutable hash maps.

```scala
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

    val plots = Gnuplot(ires ++ mres ++ ores)
    Gnuplot.save(plots)
  }

}
```

The time plot shows below tells us that, as expected, the time increases with the number of elements and that the open hash map is 
the fastest of the three for `add` and `remove` operations. 

![maps%time.plt](https://raw.github.com/sbocq/mbench/master/gallery/maps/maps%time.png)

If we look now at how the throughput scales with the number of elements, we see that the open hash map is not only the fastest of the pack but 
that its operations can still be optimized as the number of elements increases, like the while-loop shown in our first example.

![maps%throughput.plt](https://raw.github.com/sbocq/mbench/master/gallery/maps/maps%throughput.png)
