package com.github.dgraciac.euclideantsp

import com.github.dgraciac.euclideantsp.jts.arrayOfPoints
import com.github.dgraciac.euclideantsp.jts.createPolygon
import com.github.dgraciac.euclideantsp.jts.lengthAfterInsertBetweenPairOfPoints
import com.github.dgraciac.euclideantsp.jts.listOfPoints
import com.github.dgraciac.euclideantsp.shared.Euclidean2DTSPInstance
import com.github.dgraciac.euclideantsp.shared.Euclidean2DTSPSolver
import com.github.dgraciac.euclideantsp.shared.Tour
import org.locationtech.jts.algorithm.ConvexHull
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon

class SolverA : Euclidean2DTSPSolver {
    override fun compute(instance: Euclidean2DTSPInstance): Tour {
        require(instance.points.size >= 3)

        if (instance.points.size == 3) return Tour(instance.points)

        val unconnectedPoints: MutableSet<Point> = instance.points.toJTSPoints().toMutableSet()

        val centroid: Point = centroid(instance)

        val unconnectedPointsSortedByDistanceToCentroid: List<Point> =
            unconnectedPoints.sortedBy { it.distance(centroid) }

        val polygon: Polygon = createPolygon(unconnectedPointsSortedByDistanceToCentroid.take(3))

        unconnectedPoints.removeAll(polygon.listOfPoints())
            .let { removed -> if (!removed) throw RuntimeException("Points not removed") }

        val connectedPoints: ArrayList<Point> = arrayListOf(*polygon.arrayOfPoints())
        connectedPoints.removeAt(connectedPoints.size - 1)

        while (unconnectedPoints.isNotEmpty()) {
            val bestInsertion: Pair<Point, Pair<Point, Point>> =
                findBestInsertion(unconnectedPoints, connectedPoints)

            connectedPoints.add(connectedPoints.indexOf(bestInsertion.second.second), bestInsertion.first)
            unconnectedPoints.remove(bestInsertion.first)
        }

        return Tour(points = connectedPoints.map { com.github.dgraciac.euclideantsp.shared.Point(it.x, it.y) })
    }

    private fun centroid(instance: Euclidean2DTSPInstance): Point =
        ConvexHull(instance.points.map { it.toCoordinate() }.toTypedArray(), GeometryFactory()).convexHull.centroid

    private fun findBestInsertion(
        unconnectedPoints: MutableSet<Point>,
        connectedPoints: ArrayList<Point>
    ): Pair<Point, Pair<Point, Point>> {

        val bestUnconnected: Point = unconnectedPoints.minBy { unconnectedPoint: Point ->
            val pairs: MutableList<Pair<Point, Point>> = connectedPoints
                .zipWithNext()
                .toMutableList()
                .also { it.add(Pair(connectedPoints.last(), connectedPoints.first())) }
            pairs.minBy { pair: Pair<Point, Point> ->
                lengthAfterInsertBetweenPairOfPoints(pair, unconnectedPoint)
            }?.let {
                lengthAfterInsertBetweenPairOfPoints(it, unconnectedPoint)
            } ?: throw RuntimeException("Null Pair")
        } ?: throw RuntimeException("Null Best Unconnected")

        val pairs: MutableList<Pair<Point, Point>> = connectedPoints
            .zipWithNext()
            .toMutableList()
            .also { it.add(Pair(connectedPoints.last(), connectedPoints.first())) }

        val bestPair: Pair<Point, Point> = pairs.minBy { pair: Pair<Point, Point> ->
            lengthAfterInsertBetweenPairOfPoints(pair, bestUnconnected)
        } ?: throw RuntimeException("Null best pair")

        return Pair(bestUnconnected, bestPair)
    }
}
