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

package mbench.gnuplot

object Utils {

  private[gnuplot] def filterOutIdeal(datFiles: Seq[DatFile]): Seq[DatFile] =
    datFiles.filterNot(_.testName.startsWith("ideal"))

  private[gnuplot] def isUnique(datFiles: Seq[DatFile], f: DatFile => String): Option[String] =
    if (datFiles.map(f).distinct.length == 1)
      Some(f(datFiles.head))
    else
      None

  private[gnuplot] def exists(datFiles: Seq[DatFile], f: DatFile => String): Boolean =
    datFiles.exists(f(_).length > 0)

}