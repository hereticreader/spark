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

import breeze.linalg.{DenseVector => BDV, Vector => BV, norm => breezeNorm}
import org.apache.spark.api.java.JavaRDD
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.rdd.RDD

/**
 * this class is used for the model of the hierarchical clustering
 *
 * @param clusterTree a cluster as a tree node
 * @param trainTime the milliseconds for executing a training
 * @param predictTime the milliseconds for executing a prediction
 * @param isTrained if the model has been trained, the flag is true
 * @param clusters the clusters as the result of the training
 */
class HierarchicalClusteringModel private (
  val clusterTree: ClusterTree,
  var trainTime: Int,
  var predictTime: Int,
  var isTrained: Boolean,
  private var clusters: Option[Array[ClusterTree]]) extends Serializable {

  def this(clusterTree: ClusterTree) = this(clusterTree, 0, 0, false, None)

  def getClusters(): Array[ClusterTree] = {
    if (clusters == None) {
      val clusters = this.clusterTree.toSeq().filter(_.isLeaf())
          .sortWith { case (a, b) =>
        a.getDepth() < b.getDepth() &&
            breezeNorm(a.center.toBreeze, 2) < breezeNorm(b.center.toBreeze, 2)
      }.toArray
      this.clusters = Some(clusters)
    }
    this.clusters.get
  }

  def getCenters(): Array[Vector] = getClusters().map(_.center)

  /**
   * Predicts the closest cluster of each point
   * @param data the data to predict
   * @return predicted data
   */
  def predict(data: RDD[Vector]): RDD[(Int, Vector)] = {
    val startTime = System.currentTimeMillis() // to measure the execution time

    val centers = getClusters().map(_.center.toBreeze)
    val finder = new EuclideanClosestCenterFinder(centers)
    data.sparkContext.broadcast(centers)
    data.sparkContext.broadcast(finder)
    val predicted = data.map { point =>
      val closestIdx = finder(point.toBreeze)
      (closestIdx, point)
    }
    this.predictTime = (System.currentTimeMillis() - startTime).toInt
    predicted
  }

  /** Maps given points to their cluster indices. */
  def predict(points: JavaRDD[Vector]): JavaRDD[java.lang.Integer] =
    predict(points.rdd).map(_._1).toJavaRDD().asInstanceOf[JavaRDD[java.lang.Integer]]
}
