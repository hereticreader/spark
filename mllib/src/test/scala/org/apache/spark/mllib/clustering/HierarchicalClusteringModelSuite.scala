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

package org.apache.spark.mllib.clustering

import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.util.MLlibTestSparkContext
import org.scalatest.{BeforeAndAfterEach, FunSuite}

class HierarchicalClusteringModelSuite
    extends FunSuite with MLlibTestSparkContext with BeforeAndAfterEach {

  test("clustering dense vectors") {
    val app = new HierarchicalClustering().setNumClusters(5).setSeed(1)

    val localData = (1 to 100).toSeq.map { i =>
      val label = i % 5
      val vector = Vectors.dense(label, label, label)
      (label, vector)
    }
    val data = sc.parallelize(localData.map(_._2))
    val model = app.run(data)

    val clusters = model.getClusters()
    assert(clusters.isInstanceOf[Seq[ClusterTree]])
    assert(clusters.size === 5)

    val centers = model.getCenters().sortBy(_.toArray.sum)
    assert(centers.size === 5)
    assert(centers(0) === Vectors.dense(0.0, 0.0, 0.0))
    assert(centers(1) === Vectors.dense(1.0, 1.0, 1.0))
    assert(centers(2) === Vectors.dense(2.0, 2.0, 2.0))
    assert(centers(3) === Vectors.dense(3.0, 3.0, 3.0))
    assert(centers(4) === Vectors.dense(4.0, 4.0, 4.0))

    // predict with one vector
    assert(model.predict(Vectors.dense(0.0, 0.0, 0.0)) === 0)
    assert(model.predict(Vectors.dense(0.5, 0.5, 0.5)) === 0)
    assert(model.predict(Vectors.dense(1.0, 1.0, 1.0)) === 1)
    assert(model.predict(Vectors.dense(2.0, 2.0, 2.0)) === 2)
    assert(model.predict(Vectors.dense(3.0, 3.0, 3.0)) === 3)
    assert(model.predict(Vectors.dense(4.0, 4.0, 4.0)) === 4)

    // predict with a RDD
    val predicted = model.predict(data).collect()
    assert(predicted === localData.map(_._1))
  }

  test("hoge") {
    val vec = Vectors.sparse(5, Array(0, 3, 4), Array(0.0, 60.0, 80.0)).toBreeze
    val n = 60.0
    val answer1 = vec :/ n
    val answer2 = vec.toDenseVector :/ n
    println(vec)
    println(vec.size)
    println(answer1)
    println(answer2)
    println(0)
  }

  test("clustering sparse vectors") {
    val app = new HierarchicalClustering().setNumClusters(5).setSeed(1)

    val localData = (1 to 100).toSeq.map { i =>
      val label = i % 5
      val vector = Vectors.sparse(5, Seq((label, label.toDouble)))
      (label, vector)
    }
    val data = sc.parallelize(localData.map(_._2))
    val model = app.run(data)

    val clusters = model.getClusters()
    assert(clusters.isInstanceOf[Seq[ClusterTree]])
    assert(clusters.size === 5)

    val centers = model.getCenters().sortBy(_.toArray.sum)
    assert(centers.size === 5)
    assert(centers(0) === Vectors.sparse(5, Array(), Array()))
    assert(centers(1) === Vectors.sparse(5, Array(1), Array(1.0)))
    assert(centers(2) === Vectors.sparse(5, Array(2), Array(2.0)))
    assert(centers(3) === Vectors.sparse(5, Array(3), Array(3.0)))
    assert(centers(4) === Vectors.sparse(5, Array(4), Array(4.0)))

    // predict with one vector
    assert(model.predict(Vectors.sparse(5, Array(0), Array(0.0))) === 0)
    assert(model.predict(Vectors.sparse(5, Array(1), Array(1.0))) === 1)
    assert(model.predict(Vectors.sparse(5, Array(2), Array(2.0))) === 2)
    assert(model.predict(Vectors.sparse(5, Array(3), Array(3.0))) === 3)
    assert(model.predict(Vectors.sparse(5, Array(4), Array(4.0))) === 4)

    // predict with a RDD
    val predicted = model.predict(data).collect()
    assert(predicted === localData.map(_._1))
  }
}
