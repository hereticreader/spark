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

package org.apache.spark.mllib.linalg.distance

import breeze.linalg.{DenseVector => DBV, Vector => BV}
import org.apache.spark.mllib.linalg.{Matrices, Matrix, Vectors}
import org.scalatest.{FunSuite, Matchers}

private[distance]
trait GeneralDistanceMetricSuite extends FunSuite with Matchers {
  def distanceFactory: DistanceMetric

  test("ditances are required to satisfy the conditions for distance function") {
    val vectors = Array(
      Vectors.dense(1, 1, 1, 1, 1, 1).toBreeze,
      Vectors.dense(2, 2, 2, 2, 2, 2).toBreeze,
      Vectors.dense(6, 6, 6, 6, 6, 6).toBreeze,
      Vectors.dense(-1, -1, -1, -1, -1, -1).toBreeze,
      Vectors.dense(0, 0, 0, 0, 0, 0).toBreeze,
      Vectors.dense(0.1, 0.1, -0.1, 0.1, 0.1, 0.1).toBreeze,
      Vectors.dense(-0.9, 0.8, 0.7, -0.6, 0.5, -0.4).toBreeze
    )

    val distanceMatrix = GeneralDistanceMetricSuite.calcDistanceMatrix(distanceFactory, vectors)

    // non-negative
    assert(NonNegativeValidator(distanceMatrix), "not non-negative")

    // identity of indiscernibles
    assert(IdentityOfIndiscerniblesValidator(distanceMatrix), "not identity of indiscernibles")

    // symmetry
    assert(SymmetryValidator(distanceMatrix), "not symmetry")

    //  triangle inequality
    assert(TriangleInequalityValidator(distanceMatrix), "not triangle inequality")
  }
}

private[distance]
object GeneralDistanceMetricSuite {

  val EPSILON = 0.00000001

  def calcDistanceMatrix(distanceMeasure: DistanceMeasure, vectors: Array[BV[Double]]): Matrix = {
    val denseMatrixElements = for (v1 <- vectors; v2 <- vectors) yield {
      distanceMeasure(v2, v1)
    }
    Matrices.dense(vectors.size, vectors.size, denseMatrixElements)
  }

  def isNearlyEqual(value: Double, expected: Double): Boolean = {
    Math.abs(value - expected) < EPSILON
  }
}
