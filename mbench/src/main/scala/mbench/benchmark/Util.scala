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

package mbench.benchmark

object Util {

  def commonPrefix(s1: String, s2: String): String =
    s1.zip(s2).takeWhile(Function.tupled(_ == _)).map(_._1).mkString

  def commonPrefix(ss: Seq[String]): String =
    if (ss.length == 1) ""
    else {
      val s1 = ss.head
      val s2 = ss.tail.head
      ss.tail.tail.foldLeft(commonPrefix(s1, s2))((p, s) => commonPrefix(p, s))
    }

}