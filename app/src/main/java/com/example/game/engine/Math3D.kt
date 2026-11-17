package com.example.game.engine

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)

    fun lengthSq(): Float = x * x + y * y + z * z
    fun length(): Float = sqrt(lengthSq())
    fun distanceTo(o: Vec3): Float = (this - o).length()
    fun flatDistanceTo(o: Vec3): Float {
        val dx = x - o.x
        val dz = z - o.z
        return sqrt(dx * dx + dz * dz)
    }

    fun normalized(): Vec3 {
        val l = length()
        return if (l > 1e-5f) Vec3(x / l, y / l, z / l) else Vec3(0f, 0f, 1f)
    }

    fun dot(o: Vec3): Float = x * o.x + y * o.y + z * o.z

    companion object {
        val ZERO = Vec3(0f, 0f, 0f)
    }
}

data class AABB(
    val minX: Float,
    val minY: Float,
    val minZ: Float,
    val maxX: Float,
    val maxY: Float,
    val maxZ: Float
) {
    val centerX: Float get() = (minX + maxX) * 0.5f
    val centerY: Float get() = (minY + maxY) * 0.5f
    val centerZ: Float get() = (minZ + maxZ) * 0.5f
    val sizeX: Float get() = maxX - minX
    val sizeY: Float get() = maxY - minY
    val sizeZ: Float get() = maxZ - minZ

    fun containsXZ(x: Float, z: Float, margin: Float = 0f): Boolean {
        return x >= minX - margin && x <= maxX + margin &&
            z >= minZ - margin && z <= maxZ + margin
    }

    fun rayIntersect(
        ox: Float, oy: Float, oz: Float,
        dx: Float, dy: Float, dz: Float,
        maxDist: Float
    ): Float? {
        var tmin = 0f
        var tmax = maxDist

        if (abs(dx) < 1e-6f) {
            if (ox < minX || ox > maxX) return null
        } else {
            val invD = 1f / dx
            var t1 = (minX - ox) * invD
            var t2 = (maxX - ox) * invD
            if (t1 > t2) { val tmp = t1; t1 = t2; t2 = tmp }
            tmin = max(tmin, t1)
            tmax = min(tmax, t2)
            if (tmin > tmax) return null
        }

        if (abs(dy) < 1e-6f) {
            if (oy < minY || oy > maxY) return null
        } else {
            val invD = 1f / dy
            var t1 = (minY - oy) * invD
            var t2 = (maxY - oy) * invD
            if (t1 > t2) { val tmp = t1; t1 = t2; t2 = tmp }
            tmin = max(tmin, t1)
            tmax = min(tmax, t2)
            if (tmin > tmax) return null
        }

        if (abs(dz) < 1e-6f) {
            if (oz < minZ || oz > maxZ) return null
        } else {
            val invD = 1f / dz
            var t1 = (minZ - oz) * invD
            var t2 = (maxZ - oz) * invD
            if (t1 > t2) { val tmp = t1; t1 = t2; t2 = tmp }
            tmin = max(tmin, t1)
            tmax = min(tmax, t2)
            if (tmin > tmax) return null
        }

        return if (tmin >= 0f && tmin <= maxDist) tmin else null
    }
}

object Math3D {
    const val DEG_TO_RAD = (Math.PI / 180.0).toFloat()
    const val RAD_TO_DEG = (180.0 / Math.PI).toFloat()

    fun wrapAngleDeg(angle: Float): Float {
        var a = angle % 360f
        if (a > 180f) a -= 360f
        if (a < -180f) a += 360f
        return a
    }

    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t.coerceIn(0f, 1f)

    fun lerpAngleDeg(a: Float, b: Float, t: Float): Float {
        val diff = wrapAngleDeg(b - a)
        return a + diff * t.coerceIn(0f, 1f)
    }

    fun forwardFromYawPitch(yawDeg: Float, pitchDeg: Float): Vec3 {
        val yr = yawDeg * DEG_TO_RAD
        val pr = pitchDeg * DEG_TO_RAD
        val cp = cos(pr)
        return Vec3(
            x = sin(yr) * cp,
            y = sin(pr),
            z = -cos(yr) * cp
        ).normalized()
    }

    fun yawPitchToward(from: Vec3, to: Vec3): Pair<Float, Float> {
        val dx = to.x - from.x
        val dy = to.y - from.y
        val dz = to.z - from.z
        val horiz = sqrt(dx * dx + dz * dz).coerceAtLeast(1e-5f)
        val yaw = atan2(dx, -dz) * RAD_TO_DEG
        val pitch = atan2(dy, horiz) * RAD_TO_DEG
        return yaw to pitch
    }

    fun resolveCylinderXZCollisions(
        posX: Float,
        posY: Float,
        posZ: Float,
        radius: Float,
        height: Float,
        colliders: List<AABB>
    ): Pair<Float, Float> {
        var x = posX
        var z = posZ
        for (box in colliders) {
            if (posY + height <= box.minY + 0.05f || posY >= box.maxY - 0.05f) continue
            val closestX = x.coerceIn(box.minX, box.maxX)
            val closestZ = z.coerceIn(box.minZ, box.maxZ)
            val dx = x - closestX
            val dz = z - closestZ
            val distSq = dx * dx + dz * dz
            if (distSq < radius * radius) {
                val dist = sqrt(distSq)
                if (dist > 1e-5f) {
                    val push = radius - dist
                    x += (dx / dist) * push
                    z += (dz / dist) * push
                } else {
                    val leftPen = abs(x - box.minX)
                    val rightPen = abs(box.maxX - x)
                    val frontPen = abs(z - box.minZ)
                    val backPen = abs(box.maxZ - z)
                    val minPen = minOf(leftPen, rightPen, frontPen, backPen)
                    when (minPen) {
                        leftPen -> x = box.minX - radius
                        rightPen -> x = box.maxX + radius
                        frontPen -> z = box.minZ - radius
                        else -> z = box.maxZ + radius
                    }
                }
            }
        }
        return x to z
    }
}
