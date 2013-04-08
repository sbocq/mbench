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
 * Property type that can hold multiple values formatted
 * as comma separated values (e.g. 1,2,3).
 */
class Many[+T](val seq: Seq[T]) {
  override def toString = seq.mkString(",")
}

object Many {
  def empty[T] = Many(Seq.empty)
  def apply[T](t: T, ts: T*): Many[T] = new Many(t +: ts)

  implicit def manyToSeq[T](many: Many[T]): Seq[T] = many.seq

}