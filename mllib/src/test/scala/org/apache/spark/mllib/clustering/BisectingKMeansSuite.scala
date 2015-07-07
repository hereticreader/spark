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

import breeze.linalg.{Vector => BV, norm => breezeNorm}

import org.apache.spark.SparkFunSuite
import org.apache.spark.mllib.linalg.{Vector, Vectors}
import org.apache.spark.mllib.util.MLlibTestSparkContext
import org.apache.spark.mllib.util.TestingUtils._


class BisectingKMeansSuite extends SparkFunSuite with MLlibTestSparkContext {

  test("the root index is equal to 1") {
    assert(BisectingKMeans.ROOT_INDEX_KEY === 1)
  }

  test("findClosestCenter") {
    val metric = (bv1: BV[Double], bv2: BV[Double]) => breezeNorm(bv1 - bv2, 2.0)
    val centers = Seq(
      Vectors.sparse(5, Array(0, 1, 2), Array(0.0, 1.0, 2.0)).toBreeze,
      Vectors.sparse(5, Array(1, 2, 3), Array(1.0, 2.0, 3.0)).toBreeze,
      Vectors.sparse(5, Array(2, 3, 4), Array(2.0, 3.0, 4.0)).toBreeze
    )

    for (i <- 0 to (centers.size - 1)) {
      val point = centers(i)
      val closestIndex = BisectingKMeans.findClosestCenter(metric)(centers)(point)
      assert(closestIndex === i)
    }
  }

  test("run") {
    val algo = new BisectingKMeans().setNumClusters(123)
    val localSeed: Seq[Vector] = (0 to 999).map(i => Vectors.dense(i.toDouble, i.toDouble)).toSeq
    val data = sc.parallelize(localSeed, 2)
    val model = algo.run(data)
    assert(model.getClusters.length == 123)
    assert(model.node.getHeight ~== 702.8641 absTol 10E-4)

    // check the relations between a parent cluster and its children
    assert(model.node.getParent === None)
    assert(model.node.getChildren.head.getParent.get === model.node)
    assert(model.node.getChildren.apply(1).getParent.get === model.node)
    assert(model.getClusters.forall(_.getParent != None))
  }

  test("run with too many cluster size than the records") {
    val algo = new BisectingKMeans().setNumClusters(123)
    val localSeed: Seq[Vector] = (0 to 99).map(i => Vectors.dense(i.toDouble, i.toDouble)).toSeq
    val data = sc.parallelize(localSeed, 2)
    val model = algo.run(data)
    assert(model.getClusters.length == 100)
    assert(model.node.getHeight ~== 72.12489 absTol 10E-4)
  }

  test("initializeData") {
    val algo = new BisectingKMeans
    val localSeed: Seq[Vector] = (0 to 99).map(i => Vectors.dense(i.toDouble, i.toDouble)).toSeq
    val seed = sc.parallelize(localSeed)
    val data = algo.initData(seed)
    assert(data.map(_._1).collect().distinct === Array(1))
  }

  test("get center stats") {
    val algo = new BisectingKMeans
    val localSeed: Seq[Vector] = (0 to 99).map(i => Vectors.dense(i.toDouble, i.toDouble)).toSeq
    val seed = sc.parallelize(localSeed)
    val data = algo.initData(seed)

    val clusters = algo.summarize(data)
    assert(clusters.size === 1)
    assert(clusters(1).center === BV[Double](49.5, 49.5))
    assert(clusters(1).rows === 100)

    val data2 = seed.map(v => ((v.apply(0) / 25).toLong + 1L, v.toBreeze))
    val clusters2 = algo.summarize(data2)
    assert(clusters2.size === 4)
    assert(clusters2(1).center === BV[Double](12.0, 12.0))
    assert(clusters2(1).rows === 25)
    assert(clusters2(2).center === BV[Double](37.0, 37.0))
    assert(clusters2(2).rows === 25)
    assert(clusters2(3).center === BV[Double](62.0, 62.0))
    assert(clusters2(3).rows === 25)
    assert(clusters2(4).center === BV[Double](87.0, 87.0))
    assert(clusters2(4).rows === 25)
  }

  test("getChildrenCenter") {
    val algo = new BisectingKMeans
    val centers = Map(
      2L -> Vectors.dense(1.0, 1.0).toBreeze,
      3L -> Vectors.dense(2.0, 2.0).toBreeze
    )
    val initNextCenters = algo.initChildrenCenter(centers)
    assert(initNextCenters.size === 4)
    assert(initNextCenters.keySet === Set(4, 5, 6, 7))
  }

  test("should divide clusters") {
    val algo = new BisectingKMeans
    val seed = (0 to 99).map(i => ((i / 50) + 2L, Vectors.dense(i, i).toBreeze))
    val data = sc.parallelize(seed)
    val clusters = algo.summarize(data)
    val newClusters = algo.getDividedClusterStats(data, clusters)

    assert(newClusters.size === 4)
    assert(newClusters(4).center === BV[Double](12.0, 12.0))
    assert(newClusters(4).rows === 25)
    assert(newClusters(5).center === BV[Double](37.0, 37.0))
    assert(newClusters(5).rows === 25)
    assert(newClusters(6).center === BV[Double](62.0, 62.0))
    assert(newClusters(6).rows === 25)
    assert(newClusters(7).center === BV[Double](87.0, 87.0))
    assert(newClusters(7).rows === 25)
  }

  test("should assign each data to new clusters") {
    val algo = new BisectingKMeans
    val seed = Seq(
      (2L, BV[Double](0.0, 0.0)), (2L, BV[Double](1.0, 1.0)), (2L, BV[Double](2.0, 2.0)),
      (2L, BV[Double](3.0, 3.0)), (2L, BV[Double](4.0, 4.0)), (2L, BV[Double](5.0, 5.0)),
      (3L, BV[Double](6.0, 6.0)), (3L, BV[Double](7.0, 7.0)), (3L, BV[Double](8.0, 8.0)),
      (3L, BV[Double](9.0, 9.0)), (3L, BV[Double](10.0, 10.0)), (3L, BV[Double](11.0, 11.0))
    )
    val newClusterStats = Map(
      4L -> new ClusterNodeStat(3, BV[Double](1.0, 1.0) :* 3.0, BV[Double](1.0, 1.0)),
      5L -> new ClusterNodeStat(3, BV[Double](4.0, 4.0) :* 3.0, BV[Double](1.0, 1.0)),
      6L -> new ClusterNodeStat(3, BV[Double](7.0, 7.0) :* 3.0, BV[Double](1.0, 1.0)),
      7L -> new ClusterNodeStat(3, BV[Double](10.0, 10.0) :* 3.0, BV[Double](1.0, 1.0))
    )
    val data = sc.parallelize(seed)
    val result = algo.updateClusterIndex(data, newClusterStats).collect().toSeq

    val expected = Seq(
      (4L, BV[Double](0.0, 0.0)), (4L, BV[Double](1.0, 1.0)), (4L, BV[Double](2.0, 2.0)),
      (5L, BV[Double](3.0, 3.0)), (5L, BV[Double](4.0, 4.0)), (5L, BV[Double](5.0, 5.0)),
      (6L, BV[Double](6.0, 6.0)), (6L, BV[Double](7.0, 7.0)), (6L, BV[Double](8.0, 8.0)),
      (7L, BV[Double](9.0, 9.0)), (7L, BV[Double](10.0, 10.0)), (7L, BV[Double](11.0, 11.0))
    )
    assert(result === expected)
  }

  test("setNumClusters") {
    val algo = new BisectingKMeans()
    assert(algo.getNumClusters == 20)
    algo.setNumClusters(1000)
    assert(algo.getNumClusters == 1000)
  }

  test("setSubIterations") {
    val algo = new BisectingKMeans()
    assert(algo.getMaxIterations == 20)
    algo.setMaxIterations(15)
    assert(algo.getMaxIterations == 15)
  }

  test("setNumRetries") {
    val algo = new BisectingKMeans()
    assert(algo.getMaxRetries == 10)
    algo.setMaxRetries(15)
    assert(algo.getMaxRetries == 15)
  }

  test("setSeed") {
    val algo = new BisectingKMeans()
    assert(algo.getSeed == 1)
    algo.setSeed(987)
    assert(algo.getSeed == 987)
  }
}
