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

// There is propably a better representation...

abstract class Folder extends FsObject {

  def relativePathTo(obj: FsObject): String = {
    val dst = obj.path.vector
    val src = path.vector
    val z = (src zip dst) takeWhile { case (s, d) => s == d } length
    val r = src.drop(z).map(_ => "..")
    new Path(r ++ dst.drop(z)).toString
  }

  def name: String = path.name

  def newFile(name: String): File =
    File(this, name);

  def /(name: String): Folder =
    newFolder(name)

  def /(path: Path): Folder =
    newFolder(path)

  def newFolder(path: String): Folder =
    newFolder(Path(path))

  def newFolder(path: Path): Folder

}

case class RootFolder(rootPath: Path) extends Folder {
  def path = rootPath
  def newFolder(path: Path): Folder = {
    Fs.mkDirs(rootPath / path)
    SubFolder(this, path)
  }
}

case class SubFolder(root: RootFolder, val subPath: Path) extends Folder {
  def path = root.path / subPath
  def newFolder(path: Path): Folder = {
    Fs.mkDirs(this.path / path)
    SubFolder(root, subPath / path)
  }
}

object Folder {

  import java.util.Date
  import java.text.DateFormat
  import java.text.SimpleDateFormat

  def apply(path: String): RootFolder =
    apply(Path(path))

  def apply(path: Path): RootFolder = {
    Fs.mkDirs(path)
    RootFolder(path)
  }

}
