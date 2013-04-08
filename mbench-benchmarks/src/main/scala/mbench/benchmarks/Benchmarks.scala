package mbench.benchmarks

import mbench.benchmark._
import mbench.properties._

object Benchmarks {

  val properties = Properties.load("benchmarks.properties")

  val loops = "loops"
  val maps = "maps"
  val paraLoops = "para-loops"

  /**
   * The benchmarks to include.
   * (default: all)
   */
  val benchmarksInclude = Property("benchmarks.include", properties, Many(
    loops,
    maps,
    paraLoops
  ))

  /**
   * The benchmarks to exclude.
   * (default: none)
   */
  val benchmarksExclude = Property("benchmarks.exclude", properties, Many.empty[String])

  val benchmarks = benchmarksInclude.get.filterNot(benchmarksExclude.get.contains)

  def run(benchmark: String): Unit = {
    val name = "mbench.benchmarks." + benchmark.split("-").map(_.capitalize).mkString("") + "$"
    val clazz = java.lang.Class.forName(name)
    val obj = clazz.getField("MODULE$").get(null)
    val main = clazz.getMethod("main", classOf[Array[String]])
    val params: Array[String] = null
    main.invoke(obj, params)
  }

  def main(args: Array[String]) {
    benchmarks foreach run
  }

}