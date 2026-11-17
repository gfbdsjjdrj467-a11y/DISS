package com.example.game.maps

import com.example.game.engine.AABB
import com.example.game.engine.Vec3
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sqrt

class NavGrid(
    private val halfExtent: Float,
    private val cellSize: Float,
    colliders: List<AABB>
) {
    private val gridDim: Int = ((halfExtent * 2f) / cellSize).toInt().coerceAtLeast(8)
    private val blocked: BooleanArray = BooleanArray(gridDim * gridDim)

    init {
        val margin = 0.52f
        for (gz in 0 until gridDim) {
            for (gx in 0 until gridDim) {
                val wx = cellToWorldX(gx)
                val wz = cellToWorldZ(gz)
                var isBlocked = abs(wx) > halfExtent - 0.6f || abs(wz) > halfExtent - 0.6f
                if (!isBlocked) {
                    for (box in colliders) {
                        if (box.minY <= 1.5f && box.maxY >= 0.4f && box.containsXZ(wx, wz, margin)) {
                            isBlocked = true
                            break
                        }
                    }
                }
                blocked[gz * gridDim + gx] = isBlocked
            }
        }
    }

    private fun worldToCellX(x: Float): Int =
        floor((x + halfExtent) / cellSize).toInt().coerceIn(0, gridDim - 1)

    private fun worldToCellZ(z: Float): Int =
        floor((z + halfExtent) / cellSize).toInt().coerceIn(0, gridDim - 1)

    private fun cellToWorldX(gx: Int): Float = -halfExtent + (gx + 0.5f) * cellSize
    private fun cellToWorldZ(gz: Int): Float = -halfExtent + (gz + 0.5f) * cellSize

    private fun isWalkable(gx: Int, gz: Int): Boolean {
        if (gx < 0 || gz < 0 || gx >= gridDim || gz >= gridDim) return false
        return !blocked[gz * gridDim + gx]
    }

    private fun findNearestWalkable(gx: Int, gz: Int): Pair<Int, Int> {
        if (isWalkable(gx, gz)) return gx to gz
        for (r in 1..6) {
            for (dz in -r..r) {
                for (dx in -r..r) {
                    if (isWalkable(gx + dx, gz + dz)) return (gx + dx) to (gz + dz)
                }
            }
        }
        return gx to gz
    }

    fun findPath(start: Vec3, goal: Vec3): List<Vec3> {
        val (sx, sz) = findNearestWalkable(worldToCellX(start.x), worldToCellZ(start.z))
        val (tx, tz) = findNearestWalkable(worldToCellX(goal.x), worldToCellZ(goal.z))
        if (sx == tx && sz == tz) return listOf(goal)

        val total = gridDim * gridDim
        val gScore = FloatArray(total) { Float.POSITIVE_INFINITY }
        val fScore = FloatArray(total) { Float.POSITIVE_INFINITY }
        val cameFrom = IntArray(total) { -1 }
        val closed = BooleanArray(total)
        val openList = ArrayList<Int>(64)

        val startIdx = sz * gridDim + sx
        val goalIdx = tz * gridDim + tx
        gScore[startIdx] = 0f
        fScore[startIdx] = heuristic(sx, sz, tx, tz)
        openList.add(startIdx)

        val dirs = arrayOf(
            intArrayOf(1, 0, 10), intArrayOf(-1, 0, 10),
            intArrayOf(0, 1, 10), intArrayOf(0, -1, 10),
            intArrayOf(1, 1, 14), intArrayOf(-1, 1, 14),
            intArrayOf(1, -1, 14), intArrayOf(-1, -1, 14)
        )

        var iterations = 0
        while (openList.isNotEmpty() && iterations < 450) {
            iterations++
            var bestPos = 0
            var bestF = fScore[openList[0]]
            for (i in 1 until openList.size) {
                val idx = openList[i]
                if (fScore[idx] < bestF) {
                    bestF = fScore[idx]
                    bestPos = i
                }
            }
            val current = openList.removeAt(bestPos)
            if (current == goalIdx) {
                return reconstruct(cameFrom, current, goal)
            }
            closed[current] = true
            val cx = current % gridDim
            val cz = current / gridDim

            for (d in dirs) {
                val nx = cx + d[0]
                val nz = cz + d[1]
                if (!isWalkable(nx, nz)) continue
                if (d[0] != 0 && d[1] != 0) {
                    if (!isWalkable(cx + d[0], cz) || !isWalkable(cx, cz + d[1])) continue
                }
                val nIdx = nz * gridDim + nx
                if (closed[nIdx]) continue
                val tentativeG = gScore[current] + d[2] * 0.1f
                if (tentativeG < gScore[nIdx]) {
                    cameFrom[nIdx] = current
                    gScore[nIdx] = tentativeG
                    fScore[nIdx] = tentativeG + heuristic(nx, nz, tx, tz)
                    if (!openList.contains(nIdx)) openList.add(nIdx)
                }
            }
        }
        return listOf(goal)
    }

    private fun heuristic(x1: Int, z1: Int, x2: Int, z2: Int): Float {
        val dx = (x2 - x1).toFloat()
        val dz = (z2 - z1).toFloat()
        return sqrt(dx * dx + dz * dz)
    }

    private fun reconstruct(cameFrom: IntArray, endIdx: Int, goal: Vec3): List<Vec3> {
        val pts = ArrayList<Vec3>()
        var curr = endIdx
        while (curr != -1) {
            val gx = curr % gridDim
            val gz = curr / gridDim
            pts.add(Vec3(cellToWorldX(gx), 0f, cellToWorldZ(gz)))
            curr = cameFrom[curr]
        }
        pts.reverse()
        if (pts.isNotEmpty()) pts.removeAt(0)
        if (pts.size > 1) {
            pts[pts.lastIndex] = Vec3(goal.x, 0f, goal.z)
        } else if (pts.isEmpty()) {
            pts.add(Vec3(goal.x, 0f, goal.z))
        }
        return pts
    }
}

object CollisionWorld {
    fun raycastWalls(
        origin: Vec3,
        dir: Vec3,
        maxDist: Float,
        colliders: List<AABB>
    ): Float? {
        var closest: Float? = null
        for (box in colliders) {
            val hit = box.rayIntersect(origin.x, origin.y, origin.z, dir.x, dir.y, dir.z, maxDist)
            if (hit != null && (closest == null || hit < closest)) {
                closest = hit
            }
        }
        return closest
    }

    fun hasLineOfSight(from: Vec3, to: Vec3, colliders: List<AABB>): Boolean {
        val diff = to - from
        val dist = diff.length()
        if (dist < 0.15f) return true
        val dir = diff.normalized()
        val wallDist = raycastWalls(from, dir, dist - 0.1f, colliders)
        return wallDist == null
    }
}
