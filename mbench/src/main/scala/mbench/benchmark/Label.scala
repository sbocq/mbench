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
package benchmark

/**
 * The label of a column.
 *
 *  @tparam T The type of values denoted by this label.
 *
 */
abstract class Label[+T] extends Serializable {

  /**
   * Label name.
   */
  def name: String

  /**
   * Label unit.
   */
  def unit: String

  /**
   * Capitalize the name of this label.
   *
   * @return a label with its name in capital
   */
  def capitalize: Label[T]

  /**
   * Format prettily the value of a label.
   *
   * @return the value prettily formatted
   */
  def format(value: Any): String =
    name + "=" + Label.format(value) + (if (unit.length > 2) " " else "") + unit

  /**
   * Create a unit string that represents the quotient between
   * the unit of this label and the unit of another label.
   *
   * @return the unit string
   */
  def per(other: Label[_]): String = (if (unit.isEmpty) name.toLowerCase else unit) + "/" + other.unit

  /**
   * Create a unit string that represents the quantity represented by this label
   * per second.
   *
   * @return the unit string
   */
  def perSeconds: String = per(Label.time)

  /**
   * Decode a string value associated to this label into the type
   * denoted by this label.
   *
   * @param s the string to decode.
   * @return the value whose type corresponds to the type of the values denoted by this label
   */
  def valueOf(s: String): T

  override def toString =
    if (unit.isEmpty) name
    else name + "[" + unit + "]"

}

object Label {

  private case class LabelImpl[T](val name: String, val unit: String = "")(implicit val classTag: ClassTag[T]) extends Label[T] {
    def capitalize: Label[T] = new LabelImpl(name.capitalize, unit)
    def valueOf(s: String): T = Label.valueOf(s)(classTag)
  }

  /**
   * Create a new label.
   *
   * @param name the name of the label.
   * @param unit the unit of the label/
   * @return a new label
   */
  def apply[T <: AnyVal: ClassTag](name: String, unit: String = ""): Label[T] = new LabelImpl(name, unit)

  // Some standard labels

  /**
   * Time label
   */
  val time = Label[Double]("time", "s")

  /**
   * Coefficient of variation label
   */
  val cvar = Label[Double]("cvar", "%")

  /**
   * Format values associated to some labels.
   *
   * @param labels the label definitions
   * @param values the values associated to each label
   * @return a sequence of values formatted according to the definition of their associated label.
   */
  //def format(labels:Vector[Label[_]], values:Vector[Any]):Seq[String] = 
  //  (labels zip values).map{case (l, v) => l.format(v)}

  /**
   * Find the index of a label in a vector of label.
   *
   * @param name the label.
   * @param labels a vector of labels.
   * @return the index of the label in the vector of labels
   */
  def indexOf(label: Label[_], labels: Vector[Label[_]]): Int = {
    val n = label.name.toLowerCase
    labels.indexWhere(_.name.toLowerCase == n)
  }

  /**
   * Find the label identified by its name in a vector of labels.
   *
   * @param name the name of the label.
   * @param labels a vector of labels.
   * @return the label with the same name (case insensitive)
   */
  def labelOf(name: String, labels: Vector[Label[_]]): Label[_] = {
    val n = name.toLowerCase
    labels.find(_.name.toLowerCase() == n) match {
      case Some(l) => l
      case _ => sys.error("label " + name + " not found in " + labels)
    }
  }

  /**
   * Locate a label inside a vector of label and return its value
   * from a corresponding vector of values.
   */
  def locate[A](label: Label[_], labels: Vector[Label[_]], values: Vector[Any]): A =
    values(indexOf(label, labels)).asInstanceOf[A]

  private val timeUnits = Seq("s", "ms", "us")
  private val memUnits = Seq("B", "kB", "KB", "MB")

  private val doubleFmt = new java.text.DecimalFormat("#.000")
  private val floatFmt = doubleFmt

  /**
   * Format a primitive value to a string
   *
   *  @param value the primitive value to format.
   *  @return a string
   */
  def format(value: Any): String = value match {
    case d: Double => doubleFmt format d
    case f: Float => floatFmt format f
    case _ => value.toString
  }

  private def valueOf[T](string: String)(implicit tag: ClassTag[T]): T = {
    val v = tag.toString match {
      case "Boolean" => string.toBoolean
      case "Byte" => string.toByte
      case "Char" => string(0)
      case "Double" => string.toDouble
      case "Float" => string.toFloat
      case "Int" => string.toInt
      case "Long" => string.toLong
      case "Short" => string.toShort
      case _ => sys.error("unknown label type: " + tag)
    }
    v.asInstanceOf[T]
  }

}
