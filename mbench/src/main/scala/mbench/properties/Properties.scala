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

import java.util.{ Properties => JProps }

/** Class representing application properties. 
 * 
 * An instance of this class looks up the value of a property either in system properties passed on the 
 * command line or in its associated property file if it is not found. If ultimately the file does 
 * not exist or an entry for that property is missing in this file, then it falls back on the 
 * default value specified when the property was declared (see [[mbench.properties.Property]]).
 * 
 * Not that the name of system properties passed on the command line must be prefixed by the name of 
 * the file associated to a Properties object. For example, if a integer property `foo` is associated to a 
 * properties file `bar.properties`, then the value of `foo` can be passed on the command line using 
 * the syntax `-Dbar.foo=4`.
 * 
 * Properties are created using the factory methods declared in the companion object of this class.
 */
class Properties private (fileName: String, props: JProps) {

  private lazy val prefix = {
    val index = fileName.indexOf(".properties")
    if (index > 0)
      fileName.substring(0, index) + "."
    else ""
  }

  /** Get the value of a property.
   * 
   * @param p the property.
   * @return its value retrieved either from system properties, a property file or its default value 
   *         depending on where it is found first.
   */
  def get[T](p: Property[T]): T = {
    if (!prefix.isEmpty) {
      val v = System.getProperty(prefix + p.name)
      if (v != null)
        p.valueFromString(v) match {
          case Left(err) => System.err.println(fileName + ":" + err)
          case Right(value) => return value
        }
    }

    val s = props.getProperty(p.name)
    if (s == null)
      return p.defaultValue

    p.valueFromString(s).fold(err => { ; p.defaultValue }, identity)
  }

  /** Get the value of a property.
   * 
   * @param name the property name.
   * @param defaultValue the default value of the property.
   * @return its value retrieved either from system properties, a property file or its default value.
   */
  def get[T: ClassTag](name: String, defaultValue: T): T =
    get(Property[T](name, null, defaultValue))
}

/** Factory object for properties. 
 */
object Properties {

   /** Load a property file. 
    * 
    * If the file does not exists, the method returns an empty property object as if the file 
    * existed but did not declare any property.
    *   
    * @param fileName the file name in which to search for a property in case it is not specified 
    *                 as system properties. 
    */
  def load(fileName: String): Properties = {
    val alternateFileName = System.getProperty(fileName)
    val file = if (alternateFileName == null) new java.io.File(fileName) else {
      val file = new java.io.File(alternateFileName)
      if (file.exists()) file
      else {
        System.err.println(alternateFileName + " not found, reverting to " + fileName)
        new java.io.File(fileName)
      }
    }
    new Properties(file.getName(), load(file))
  }

  private[this] def load(file: java.io.File): JProps = {
    val props = new JProps()
    if (file.exists) {
      val in = new java.io.FileInputStream(file);
      props.load(in)
      in.close()
    }
    props
  }

}