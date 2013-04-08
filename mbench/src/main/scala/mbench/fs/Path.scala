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

package mbench.fs

class Path(val vector: Vector[String]) extends Serializable {

  def /(name: String): Path = new Path(vector :+ name)

  def /(path: Path): Path = new Path(vector ++ path.vector)

  def name: String = vector.last

  def isEmpty = vector.isEmpty

  def parent: Path = new Path(vector.init)

  def exists: Boolean = new JFile(toString).exists()

  override def toString = vector.mkString(Path.SEP)
}

object Path {

  val SEP = System.getProperty("file.separator")

  def apply(path: String): Path = {
    def paths(file: JFile, acc: Vector[String]): Vector[String] = {
      val name = if (file.getName().isEmpty()) file.getPath() else file.getName()
      val nacc = name +: acc
      val parent = file.getParentFile
      if (parent == null) nacc else paths(parent, nacc)
    }
    val file = new JFile(path)
    val vect = paths(file, Vector())
    new Path(vect)
  }

}