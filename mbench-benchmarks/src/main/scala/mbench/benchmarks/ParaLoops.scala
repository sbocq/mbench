package mbench.benchmarks

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