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
package properties

/**
 * Class that represents a property. 
 * 
 * The types supported are:
 * Boolean, Byte, Char, Double, Float, Int, Long, Short, String, [[mbench.fs.Path]], [[mbench.properties.Many]].
 * 
 * @param name         the name of the property.
 * @param properties   the file name to which this property is associated.
 * @param defaultValue the default value of this property.
 * 
 */
case class Property[T](name: String, properties: Properties, defaultValue: T)(implicit val tag: ClassTag[T]) {

  /** Return the value of this property by looking up its associated properties object 
   * (see [[mbench.properties.Properties]]).
   * 
   * @return the value of this property.
   */
  lazy val get = properties.get(this)

  def valueFromString(value: String): Either[String, T] =
    valueOf(value, tag.toString).asInstanceOf[Either[String, T]]

  override def toString = name

  private[this] def valueOf(string: String, typ: String): Either[String, Any] = try {
    Right(
      typ match {
        case "Boolean" => string.toBoolean
        case "Byte" => string.toByte
        case "Char" => string(0)
        case "Double" => string.toDouble
        case "Float" => string.toFloat
        case "Int" => string.toInt
        case "Long" => string.toLong
        case "Short" => string.toShort
        case "java.lang.String" => string
        case "mbench.fs.Path" => mbench.fs.Path(string)
        case Property.ManyP(t) => // expect CSV
          val seq = string.split(",").map(s => valueOf(s.trim, t)).toSeq

          val err = seq.collect { case Left(e) => e }
          if (err.isEmpty)
            new Many(seq.map { case Right(v) => v })
          else
            Left(err.mkString)
        case _ => Left("type not supported: " + typ + " for property " + name + ":" + string)
      }
    )
  } catch { case t => castError(typ, name, string, t) }

  private[this] def castError(expectedType: String, name: String, value: String, t: Throwable): Left[String, Any] =
    Left("Property " + name + ": cannot convert " + value + " to " + expectedType + " - cause:" + t)

}

object Property {
  private val ManyP = """mbench.properties.Many\[(.+)\]""".r
}