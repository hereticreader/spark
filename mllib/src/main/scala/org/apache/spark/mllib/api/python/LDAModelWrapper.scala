/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.spark.mllib.api.python

import scala.collection.JavaConverters

import org.apache.spark.SparkContext
import org.apache.spark.mllib.clustering.LDAModel
import org.apache.spark.mllib.linalg.Matrix

/**
 * Wrapper around LDAModel to provide helper methods in Python
 */
private[python] class LDAModelWrapper(model: LDAModel) {

  def topicsMatrix(): Matrix = model.topicsMatrix

  def vocabSize(): Int = model.vocabSize

  def describeTopics(): java.util.List[Array[Any]] = describeTopics(this.model.vocabSize)

  def describeTopics(maxTermsPerTopic: Int): java.util.List[Array[Any]] = {

    val seq = model.describeTopics(maxTermsPerTopic).map { case (terms, termWeights) =>
        Array.empty[Any] ++ terms ++ termWeights
      }.toSeq
    JavaConverters.seqAsJavaListConverter(seq).asJava
  }

  def save(sc: SparkContext, path: String): Unit = model.save(sc, path)
}
