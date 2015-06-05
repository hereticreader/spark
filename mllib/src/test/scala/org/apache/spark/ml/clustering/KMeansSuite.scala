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

package org.apache.spark.ml.clustering

import org.apache.spark.SparkFunSuite
import org.apache.spark.mllib.clustering.KMeans
import org.apache.spark.mllib.linalg.{Vector, Vectors}
import org.apache.spark.mllib.util.MLlibTestSparkContext
import org.apache.spark.sql.DataFrame

private[clustering] case class TestRow(features: Vector)

class KMeansSuite extends SparkFunSuite with MLlibTestSparkContext {

  val k = 5
  @transient var dataset: DataFrame = _

  override def beforeAll(): Unit = {
    super.beforeAll()

    dataset = generateKMeansDataset(1000, 10, k).cache()
  }

  def generateKMeansDataset(rows: Int, dim: Int, k: Int): DataFrame = {
    val rdd = sc.parallelize(1 to rows, 2)
        .map(i => Vectors.dense(Array.fill(dim)((i % k).toDouble)))
        .map(p => new TestRow(p))
    sqlContext.createDataFrame(rdd)
  }

  test("default parameters") {
    val kmeans = new KMeans()

    assert(kmeans.getK === 2)
    assert(kmeans.getFeaturesCol === "features")
    assert(kmeans.getMaxIter === 20)
    assert(kmeans.getRuns === 1)
    assert(kmeans.getInitializationMode === KMeans.K_MEANS_PARALLEL)
    assert(kmeans.getInitializationSteps === 5)
    assert(kmeans.getEpsilon === 1e-4)
  }

  test("fit & transform") {
    val kmeans = new KMeans().setK(k)
    val model = kmeans.fit(dataset)
    assert(model.clusterCenters.length === k)

    val transformed = model.transform(dataset)
    assert(transformed.columns === Array("features", "prediction"))
    val clusters = transformed.select("prediction")
        .map(row => row.apply(0)).distinct().collect().toSet
    assert(clusters === Set(0, 1, 2, 3, 4))
  }
}
