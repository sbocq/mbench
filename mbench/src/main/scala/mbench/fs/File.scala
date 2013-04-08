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

case class File(folder: Folder, name: String) extends FsObject {

  def path: Path = folder.path / name

  def jfile: JFile = {
    new JFile(path.toString)
  }

  def nameParts: (String, String) = name.splitAt(name.lastIndexOf('.'))
  def firstName: String = nameParts._1
  def lastName: String = nameParts._2
  def extension: String = lastName

  def append(content: String): Unit = {
    import java.io.{ FileWriter, PrintWriter }
    val out = new PrintWriter(new FileWriter(jfile, true))
    out.println(content)
    out.close
  }

  def outStream: java.io.FileOutputStream =
    new java.io.FileOutputStream(jfile)

  def bufferedWriter: java.io.BufferedWriter =
    new java.io.BufferedWriter(new java.io.OutputStreamWriter(outStream)) //WTF

  def printWriter(autoFlush: Boolean): java.io.PrintWriter =
    new java.io.PrintWriter(bufferedWriter, autoFlush)

  def inStream: java.io.FileInputStream =
    new java.io.FileInputStream(jfile)

  def bufferedReader: java.io.BufferedReader =
    new java.io.BufferedReader(new java.io.InputStreamReader(inStream))

}

object File {

  implicit def toJFile(file: File): JFile = file.jfile

}
