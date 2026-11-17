package com.example.game.engine

import android.opengl.GLES20
import com.example.game.maps.MapDefinition
import com.example.game.model.PickupType
import com.example.game.model.WeaponCatalog
import com.example.game.model.WeaponId
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Packed mesh with stride = 12 floats:
 * [x, y, z, nx, ny, nz, u, v, r, g, b, matType]
 * matType: 0..7 = atlas texture tile; -1.0 = emissive (unlit glowing visor/optic/sight)
 */
class GpuMesh(val vertices: FloatArray) {
    val vertexCount: Int = vertices.size / STRIDE_FLOATS
    private var vboId: Int = 0
    private val buffer: FloatBuffer = ByteBuffer
        .allocateDirect(vertices.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(vertices)
            position(0)
        }

    fun uploadIfNeeded() {
        if (vboId == 0 && vertexCount > 0) {
            val ids = IntArray(1)
            GLES20.glGenBuffers(1, ids, 0)
            vboId = ids[0]
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId)
            buffer.position(0)
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertices.size * 4, buffer, GLES20.GL_STATIC_DRAW)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        }
    }

    fun draw(shader: TacticalWorldShader) {
        if (vertexCount == 0) return
        uploadIfNeeded()
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId)

        val strideBytes = STRIDE_FLOATS * 4
        GLES20.glEnableVertexAttribArray(shader.aPos)
        GLES20.glVertexAttribPointer(shader.aPos, 3, GLES20.GL_FLOAT, false, strideBytes, 0)

        GLES20.glEnableVertexAttribArray(shader.aNormal)
        GLES20.glVertexAttribPointer(shader.aNormal, 3, GLES20.GL_FLOAT, false, strideBytes, 3 * 4)

        GLES20.glEnableVertexAttribArray(shader.aUv)
        GLES20.glVertexAttribPointer(shader.aUv, 2, GLES20.GL_FLOAT, false, strideBytes, 6 * 4)

        GLES20.glEnableVertexAttribArray(shader.aColor)
        GLES20.glVertexAttribPointer(shader.aColor, 3, GLES20.GL_FLOAT, false, strideBytes, 8 * 4)

        GLES20.glEnableVertexAttribArray(shader.aMatType)
        GLES20.glVertexAttribPointer(shader.aMatType, 1, GLES20.GL_FLOAT, false, strideBytes, 11 * 4)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)

        GLES20.glDisableVertexAttribArray(shader.aPos)
        GLES20.glDisableVertexAttribArray(shader.aNormal)
        GLES20.glDisableVertexAttribArray(shader.aUv)
        GLES20.glDisableVertexAttribArray(shader.aColor)
        GLES20.glDisableVertexAttribArray(shader.aMatType)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    fun delete() {
        if (vboId != 0) {
            GLES20.glDeleteBuffers(1, intArrayOf(vboId), 0)
            vboId = 0
        }
    }

    companion object {
        const val STRIDE_FLOATS = 12
    }
}

class MeshBuilder {
    private val data = ArrayList<Float>(8192)

    fun addBox(
        minX: Float, minY: Float, minZ: Float,
        maxX: Float, maxY: Float, maxZ: Float,
        r: Float, g: Float, b: Float,
        matType: Int = 1,
        uvScale: Float = 0.45f
    ) {
        val m = matType.toFloat()
        val sx = (maxX - minX) * uvScale
        val sy = (maxY - minY) * uvScale
        val sz = (maxZ - minZ) * uvScale

        // Front (+Z)
        addQuadUv(
            minX, minY, maxZ, 0f, 0f,
            maxX, minY, maxZ, sx, 0f,
            maxX, maxY, maxZ, sx, sy,
            minX, maxY, maxZ, 0f, sy,
            0f, 0f, 1f, r, g, b, m
        )
        // Back (-Z)
        addQuadUv(
            maxX, minY, minZ, 0f, 0f,
            minX, minY, minZ, sx, 0f,
            minX, maxY, minZ, sx, sy,
            maxX, maxY, minZ, 0f, sy,
            0f, 0f, -1f, r, g, b, m
        )
        // Left (-X)
        addQuadUv(
            minX, minY, minZ, 0f, 0f,
            minX, minY, maxZ, sz, 0f,
            minX, maxY, maxZ, sz, sy,
            minX, maxY, minZ, 0f, sy,
            -1f, 0f, 0f, r, g, b, m
        )
        // Right (+X)
        addQuadUv(
            maxX, minY, maxZ, 0f, 0f,
            maxX, minY, minZ, sz, 0f,
            maxX, maxY, minZ, sz, sy,
            maxX, maxY, maxZ, 0f, sy,
            1f, 0f, 0f, r, g, b, m
        )
        // Top (+Y)
        addQuadUv(
            minX, maxY, maxZ, 0f, 0f,
            maxX, maxY, maxZ, sx, 0f,
            maxX, maxY, minZ, sx, sz,
            minX, maxY, minZ, 0f, sz,
            0f, 1f, 0f, (r * 1.06f).coerceAtMost(1f), (g * 1.06f).coerceAtMost(1f), (b * 1.06f).coerceAtMost(1f), m
        )
        // Bottom (-Y)
        addQuadUv(
            minX, minY, minZ, 0f, 0f,
            maxX, minY, minZ, sx, 0f,
            maxX, minY, maxZ, sx, sz,
            minX, minY, maxZ, 0f, sz,
            0f, -1f, 0f, r * 0.72f, g * 0.72f, b * 0.72f, m
        )
    }

    fun addCenteredBox(
        cx: Float, cy: Float, cz: Float,
        sx: Float, sy: Float, sz: Float,
        r: Float, g: Float, b: Float,
        matType: Int = 3,
        uvScale: Float = 4.0f
    ) {
        addBox(
            cx - sx * 0.5f, cy - sy * 0.5f, cz - sz * 0.5f,
            cx + sx * 0.5f, cy + sy * 0.5f, cz + sz * 0.5f,
            r, g, b, matType, uvScale
        )
    }

    /**
     * Chamfered / beveled box along the Z-axis (eliminates harsh blocky square silhouettes on receivers & slides).
     */
    fun addChamferedBox(
        cx: Float, cy: Float, cz: Float,
        sx: Float, sy: Float, sz: Float,
        bevel: Float,
        r: Float, g: Float, b: Float,
        matType: Int = 3
    ) {
        val bClamp = bevel.coerceAtMost(minOf(sx, sy) * 0.38f)
        // Main inner core
        addCenteredBox(cx, cy, cz, sx - bClamp * 2f, sy, sz, r, g, b, matType, 4.5f)
        // Side cheeks
        addCenteredBox(cx, cy, cz, sx, sy - bClamp * 2f, sz, r * 0.96f, g * 0.96f, b * 0.96f, matType, 4.5f)
        // 45-degree chamfer strips along top/bottom edges
        val x0 = cx - sx * 0.5f; val x1 = cx - sx * 0.5f + bClamp
        val x2 = cx + sx * 0.5f - bClamp; val x3 = cx + sx * 0.5f
        val y0 = cy - sy * 0.5f; val y1 = cy - sy * 0.5f + bClamp
        val y2 = cy + sy * 0.5f - bClamp; val y3 = cy + sy * 0.5f
        val zMin = cz - sz * 0.5f; val zMax = cz + sz * 0.5f
        val m = matType.toFloat()
        val s = 0.707f
        // Top-left bevel
        addQuadUv(x0, y2, zMax, 0f, 0f, x1, y3, zMax, 0.2f, 0f, x1, y3, zMin, 0.2f, 1f, x0, y2, zMin, 0f, 1f, -s, s, 0f, r, g, b, m)
        // Top-right bevel
        addQuadUv(x2, y3, zMax, 0f, 0f, x3, y2, zMax, 0.2f, 0f, x3, y2, zMin, 0.2f, 1f, x2, y3, zMin, 0f, 1f, s, s, 0f, r, g, b, m)
        // Bottom-left bevel
        addQuadUv(x1, y0, zMax, 0f, 0f, x0, y1, zMax, 0.2f, 0f, x0, y1, zMin, 0.2f, 1f, x1, y0, zMin, 0f, 1f, -s, -s, 0f, r * 0.9f, g * 0.9f, b * 0.9f, m)
        // Bottom-right bevel
        addQuadUv(x3, y1, zMax, 0f, 0f, x2, y0, zMax, 0.2f, 0f, x2, y0, zMin, 0.2f, 1f, x3, y1, zMin, 0f, 1f, s, -s, 0f, r * 0.9f, g * 0.9f, b * 0.9f, m)
    }

    /**
     * Arbitrary 3D tapered cylinder / tube from p1 to p2 (used for round barrels, suppressors,
     * scopes, angled grips, tubular stocks, grenade tubes, bipods, and limbs).
     */
    fun addTube(
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        r1: Float, r2: Float,
        r: Float, g: Float, b: Float,
        matType: Int = 3,
        segments: Int = 12,
        capEnds: Boolean = true
    ) {
        val dx = x2 - x1; val dy = y2 - y1; val dz = z2 - z1
        val len = sqrt(dx * dx + dy * dy + dz * dz)
        if (len < 1e-5f) return
        val ax = dx / len; val ay = dy / len; val az = dz / len

        // Orthonormal basis (uVec, vVec) perpendicular to axis
        val refX = if (abs(ay) < 0.9f) 0f else 1f
        val refY = if (abs(ay) < 0.9f) 1f else 0f
        val refZ = 0f
        var ux = ay * refZ - az * refY
        var uy = az * refX - ax * refZ
        var uz = ax * refY - ay * refX
        val uLen = sqrt(ux * ux + uy * uy + uz * uz).coerceAtLeast(1e-5f)
        ux /= uLen; uy /= uLen; uz /= uLen

        val vx = ay * uz - az * uy
        val vy = az * ux - ax * uz
        val vz = ax * uy - ay * ux

        val m = matType.toFloat()
        val uvLen = len * 4f

        for (i in 0 until segments) {
            val a0 = (i.toFloat() / segments) * (2f * PI.toFloat())
            val a1 = ((i + 1).toFloat() / segments) * (2f * PI.toFloat())
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)

            val n0x = ux * c0 + vx * s0; val n0y = uy * c0 + vy * s0; val n0z = uz * c0 + vz * s0
            val n1x = ux * c1 + vx * s1; val n1y = uy * c1 + vy * s1; val n1z = uz * c1 + vz * s1

            val p10x = x1 + n0x * r1; val p10y = y1 + n0y * r1; val p10z = z1 + n0z * r1
            val p11x = x1 + n1x * r1; val p11y = y1 + n1y * r1; val p11z = z1 + n1z * r1
            val p20x = x2 + n0x * r2; val p20y = y2 + n0y * r2; val p20z = z2 + n0z * r2
            val p21x = x2 + n1x * r2; val p21y = y2 + n1y * r2; val p21z = z2 + n1z * r2

            val u0 = i.toFloat() / segments * 2f
            val u1 = (i + 1).toFloat() / segments * 2f

            // Two triangles with smooth outward normals
            pushVertex(p10x, p10y, p10z, n0x, n0y, n0z, u0, 0f, r, g, b, m)
            pushVertex(p11x, p11y, p11z, n1x, n1y, n1z, u1, 0f, r, g, b, m)
            pushVertex(p21x, p21y, p21z, n1x, n1y, n1z, u1, uvLen, r, g, b, m)

            pushVertex(p10x, p10y, p10z, n0x, n0y, n0z, u0, 0f, r, g, b, m)
            pushVertex(p21x, p21y, p21z, n1x, n1y, n1z, u1, uvLen, r, g, b, m)
            pushVertex(p20x, p20y, p20z, n0x, n0y, n0z, u0, uvLen, r, g, b, m)

            if (capEnds && r1 > 1e-4f) {
                addTriUv(
                    x1, y1, z1, 0.5f, 0.5f,
                    p11x, p11y, p11z, 0.5f + c1 * 0.5f, 0.5f + s1 * 0.5f,
                    p10x, p10y, p10z, 0.5f + c0 * 0.5f, 0.5f + s0 * 0.5f,
                    -ax, -ay, -az, r * 0.85f, g * 0.85f, b * 0.85f, m
                )
            }
            if (capEnds && r2 > 1e-4f) {
                addTriUv(
                    x2, y2, z2, 0.5f, 0.5f,
                    p20x, p20y, p20z, 0.5f + c0 * 0.5f, 0.5f + s0 * 0.5f,
                    p21x, p21y, p21z, 0.5f + c1 * 0.5f, 0.5f + s1 * 0.5f,
                    ax, ay, az, r * 0.92f, g * 0.92f, b * 0.92f, m
                )
            }
        }
    }

    /**
     * Slanted rectangular bar from p1 to p2 (for angled pistol/rifle grips, curved magazines, bipods, gable roofs).
     */
    fun addOrientedBar(
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        widthX: Float, depthZ: Float,
        r: Float, g: Float, b: Float,
        matType: Int = 3
    ) {
        val hw = widthX * 0.5f
        val hd = depthZ * 0.5f
        val m = matType.toFloat()
        // Approximate front/back/left/right quads between bottom ring (y1, z1) and top ring (y2, z2)
        val dx = x2 - x1; val dy = y2 - y1; val dz = z2 - z1
        val nLen = sqrt(dy * dy + dz * dz).coerceAtLeast(1e-4f)
        val ny = -dz / nLen; val nz = dy / nLen

        // Front (+Z face)
        addQuadUv(
            x1 - hw, y1, z1 + hd, 0f, 0f,
            x1 + hw, y1, z1 + hd, 1f, 0f,
            x2 + hw, y2, z2 + hd, 1f, 1f,
            x2 - hw, y2, z2 + hd, 0f, 1f,
            0f, ny, nz, r, g, b, m
        )
        // Back (-Z face)
        addQuadUv(
            x1 + hw, y1, z1 - hd, 0f, 0f,
            x1 - hw, y1, z1 - hd, 1f, 0f,
            x2 - hw, y2, z2 - hd, 1f, 1f,
            x2 + hw, y2, z2 - hd, 0f, 1f,
            0f, -ny, -nz, r * 0.92f, g * 0.92f, b * 0.92f, m
        )
        // Left (-X)
        addQuadUv(
            x1 - hw, y1, z1 - hd, 0f, 0f,
            x1 - hw, y1, z1 + hd, 1f, 0f,
            x2 - hw, y2, z2 + hd, 1f, 1f,
            x2 - hw, y2, z2 - hd, 0f, 1f,
            -1f, 0f, 0f, r * 0.96f, g * 0.96f, b * 0.96f, m
        )
        // Right (+X)
        addQuadUv(
            x1 + hw, y1, z1 + hd, 0f, 0f,
            x1 + hw, y1, z1 - hd, 1f, 0f,
            x2 + hw, y2, z2 - hd, 1f, 1f,
            x2 + hw, y2, z2 + hd, 0f, 1f,
            1f, 0f, 0f, r * 0.96f, g * 0.96f, b * 0.96f, m
        )
        // End caps
        addQuadUv(
            x1 - hw, y1, z1 - hd, 0f, 0f,
            x1 + hw, y1, z1 - hd, 1f, 0f,
            x1 + hw, y1, z1 + hd, 1f, 1f,
            x1 - hw, y1, z1 + hd, 0f, 1f,
            0f, -1f, 0f, r * 0.8f, g * 0.8f, b * 0.8f, m
        )
        addQuadUv(
            x2 - hw, y2, z2 + hd, 0f, 0f,
            x2 + hw, y2, z2 + hd, 1f, 0f,
            x2 + hw, y2, z2 - hd, 1f, 1f,
            x2 - hw, y2, z2 - hd, 0f, 1f,
            0f, 1f, 0f, r, g, b, m
        )
    }

    fun addCylinder(
        cx: Float, baseY: Float, cz: Float,
        radius: Float, height: Float,
        r: Float, g: Float, b: Float,
        matType: Int = 2,
        segments: Int = 14
    ) {
        addTube(cx, baseY, cz, cx, baseY + height, cz, radius, radius, r, g, b, matType, segments, true)
    }

    fun addCone(
        cx: Float, baseY: Float, cz: Float,
        radius: Float, height: Float,
        r: Float, g: Float, b: Float,
        matType: Int = 6,
        segments: Int = 12
    ) {
        addTube(cx, baseY, cz, cx, baseY + height, cz, radius, 0.01f, r, g, b, matType, segments, true)
    }

    fun addQuadUv(
        x0: Float, y0: Float, z0: Float, u0: Float, v0: Float,
        x1: Float, y1: Float, z1: Float, u1: Float, v1: Float,
        x2: Float, y2: Float, z2: Float, u2: Float, v2: Float,
        x3: Float, y3: Float, z3: Float, u3: Float, v3: Float,
        nx: Float, ny: Float, nz: Float,
        r: Float, g: Float, b: Float, m: Float
    ) {
        addTriUv(x0, y0, z0, u0, v0, x1, y1, z1, u1, v1, x2, y2, z2, u2, v2, nx, ny, nz, r, g, b, m)
        addTriUv(x0, y0, z0, u0, v0, x2, y2, z2, u2, v2, x3, y3, z3, u3, v3, nx, ny, nz, r, g, b, m)
    }

    fun addTriUv(
        x0: Float, y0: Float, z0: Float, u0: Float, v0: Float,
        x1: Float, y1: Float, z1: Float, u1: Float, v1: Float,
        x2: Float, y2: Float, z2: Float, u2: Float, v2: Float,
        nx: Float, ny: Float, nz: Float,
        r: Float, g: Float, b: Float, m: Float
    ) {
        pushVertex(x0, y0, z0, nx, ny, nz, u0, v0, r, g, b, m)
        pushVertex(x1, y1, z1, nx, ny, nz, u1, v1, r, g, b, m)
        pushVertex(x2, y2, z2, nx, ny, nz, u2, v2, r, g, b, m)
    }

    private fun pushVertex(
        x: Float, y: Float, z: Float,
        nx: Float, ny: Float, nz: Float,
        u: Float, v: Float,
        r: Float, g: Float, b: Float, m: Float
    ) {
        data.add(x); data.add(y); data.add(z)
        data.add(nx); data.add(ny); data.add(nz)
        data.add(u); data.add(v)
        data.add(r); data.add(g); data.add(b)
        data.add(m)
    }

    fun build(): GpuMesh = GpuMesh(data.toFloatArray())
}

object TacticalMeshes {

    fun buildMapMesh(map: MapDefinition): GpuMesh {
        val mb = MeshBuilder()
        val ext = map.halfExtent + 2f
        val (fr, fg, fb) = map.floorColor
        // Textured Ground Slab
        mb.addBox(-ext, -0.5f, -ext, ext, 0f, ext, fr, fg, fb, map.floorMatType, uvScale = 0.25f)

        // Subtle tactical arena grid/border trim along perimeter
        mb.addBox(-map.halfExtent, 0.005f, -map.halfExtent, map.halfExtent, 0.02f, -map.halfExtent + 0.45f, 0.90f, 0.55f, 0.15f, 2, uvScale = 0.5f)
        mb.addBox(-map.halfExtent, 0.005f, map.halfExtent - 0.45f, map.halfExtent, 0.02f, map.halfExtent, 0.90f, 0.55f, 0.15f, 2, uvScale = 0.5f)

        for (b in map.boxes) {
            mb.addBox(b.aabb.minX, b.aabb.minY, b.aabb.minZ, b.aabb.maxX, b.aabb.maxY, b.aabb.maxZ, b.r, b.g, b.b, b.matType, uvScale = 0.42f)
        }
        for (c in map.cylinders) {
            mb.addCylinder(c.cx, c.baseY, c.cz, c.radius, c.height, c.r, c.g, c.b, c.matType, c.segments)
        }
        return mb.build()
    }

    /**
     * Smooth gradient sky hemisphere + glowing sun disc (emissive matType = -1).
     */
    fun buildSkyDome(map: MapDefinition): GpuMesh {
        val mb = MeshBuilder()
        val radius = 120f
        val rings = 8
        val slices = 18
        val (tr, tg, tb) = map.skyTopColor
        val (hr, hg, hb) = map.skyHorizonColor

        for (i in 0 until rings) {
            val phi0 = (i.toFloat() / rings) * (PI.toFloat() * 0.5f)
            val phi1 = ((i + 1).toFloat() / rings) * (PI.toFloat() * 0.5f)
            val y0 = sin(phi0) * radius
            val y1 = sin(phi1) * radius
            val rad0 = cos(phi0) * radius
            val rad1 = cos(phi1) * radius

            val t0 = (i.toFloat() / rings)
            val t1 = ((i + 1).toFloat() / rings)
            val r0 = hr + (tr - hr) * t0; val g0 = hg + (tg - hg) * t0; val b0 = hb + (tb - hb) * t0
            val r1 = hr + (tr - hr) * t1; val g1 = hg + (tg - hg) * t1; val b1 = hb + (tb - hb) * t1

            for (j in 0 until slices) {
                val th0 = (j.toFloat() / slices) * (2f * PI.toFloat())
                val th1 = ((j + 1).toFloat() / slices) * (2f * PI.toFloat())
                val x00 = cos(th0) * rad0; val z00 = sin(th0) * rad0
                val x01 = cos(th1) * rad0; val z01 = sin(th1) * rad0
                val x10 = cos(th0) * rad1; val z10 = sin(th0) * rad1
                val x11 = cos(th1) * rad1; val z11 = sin(th1) * rad1

                // Inside-facing triangles (visible from center)
                mb.addTriUv(x00, y0, z00, 0f, 0f, x10, y1, z10, 0f, 1f, x01, y0, z01, 1f, 0f, 0f, 1f, 0f, r0, g0, b0, -1f)
                mb.addTriUv(x01, y0, z01, 1f, 0f, x10, y1, z10, 0f, 1f, x11, y1, z11, 1f, 1f, 0f, 1f, 0f, r1, g1, b1, -1f)
            }
        }

        // Glowing Sun disc positioned along sunDirection
        val sunP = map.sunDirection * 96f
        val (sr, sg, sb) = map.sunColor
        mb.addTube(
            sunP.x, sunP.y, sunP.z,
            sunP.x - map.sunDirection.x * 1.2f, sunP.y - map.sunDirection.y * 1.2f, sunP.z - map.sunDirection.z * 1.2f,
            6.5f, 6.5f, sr, sg, sb * 0.9f, -1, 16, true
        )
        return mb.build()
    }

    fun buildUnitBoxMesh(): GpuMesh {
        val mb = MeshBuilder()
        mb.addCenteredBox(0f, 0f, 0f, 1f, 1f, 1f, 1f, 1f, 1f, 2, 1.0f)
        return mb.build()
    }

    fun buildPickupMesh(type: PickupType): GpuMesh {
        val mb = MeshBuilder()
        when (type) {
            PickupType.HEALTH -> {
                mb.addChamferedBox(0f, 0f, 0f, 0.52f, 0.34f, 0.38f, 0.05f, 0.92f, 0.94f, 0.96f, 2)
                mb.addCenteredBox(0f, 0.18f, 0f, 0.24f, 0.03f, 0.08f, 0.10f, 0.95f, 0.45f, -1)
                mb.addCenteredBox(0f, 0.18f, 0f, 0.08f, 0.03f, 0.24f, 0.10f, 0.95f, 0.45f, -1)
            }
            PickupType.AMMO -> {
                mb.addChamferedBox(0f, 0f, 0f, 0.54f, 0.30f, 0.36f, 0.04f, 0.24f, 0.34f, 0.22f, 2)
                mb.addCenteredBox(0f, 0.04f, 0f, 0.56f, 0.06f, 0.38f, 0.96f, 0.72f, 0.16f, 2)
                mb.addTube(-0.14f, 0.16f, -0.06f, -0.14f, 0.16f, 0.06f, 0.025f, 0.025f, 0.95f, 0.78f, 0.22f, 2, 8)
                mb.addTube(0.0f, 0.16f, -0.06f, 0.0f, 0.16f, 0.06f, 0.025f, 0.025f, 0.95f, 0.78f, 0.22f, 2, 8)
                mb.addTube(0.14f, 0.16f, -0.06f, 0.14f, 0.16f, 0.06f, 0.025f, 0.025f, 0.95f, 0.78f, 0.22f, 2, 8)
            }
            PickupType.ARMOR -> {
                mb.addChamferedBox(0f, 0f, 0f, 0.48f, 0.42f, 0.28f, 0.06f, 0.16f, 0.42f, 0.68f, 7)
                mb.addCenteredBox(0f, 0.02f, 0f, 0.34f, 0.24f, 0.31f, 0.12f, 0.85f, 0.98f, -1)
            }
        }
        return mb.build()
    }

    // Detailed Articulated Bot Sub-Meshes
    fun buildBotTorso(isAlpha: Boolean): GpuMesh {
        val mb = MeshBuilder()
        val (tr, tg, tb) = if (isAlpha) Triple(0.18f, 0.44f, 0.82f) else Triple(0.80f, 0.22f, 0.22f)
        // Undershirt / torso core (Camo)
        mb.addChamferedBox(0f, 0.28f, 0f, 0.44f, 0.56f, 0.25f, 0.05f, 0.26f, 0.29f, 0.31f, 7)
        // Plate carrier Kevlar vest
        mb.addChamferedBox(0f, 0.31f, 0f, 0.48f, 0.38f, 0.30f, 0.05f, tr, tg, tb, 7)
        // Chest mag pouches (front -Z)
        mb.addCenteredBox(-0.11f, 0.22f, -0.16f, 0.09f, 0.14f, 0.06f, 0.18f, 0.20f, 0.22f, 7)
        mb.addCenteredBox(0.0f, 0.22f, -0.16f, 0.09f, 0.14f, 0.06f, 0.18f, 0.20f, 0.22f, 7)
        mb.addCenteredBox(0.11f, 0.22f, -0.16f, 0.09f, 0.14f, 0.06f, 0.18f, 0.20f, 0.22f, 7)
        // Tactical radio & backpack on back (+Z)
        mb.addChamferedBox(0f, 0.32f, 0.18f, 0.30f, 0.34f, 0.14f, 0.04f, 0.20f, 0.22f, 0.24f, 7)
        mb.addTube(0.11f, 0.46f, 0.20f, 0.11f, 0.66f, 0.22f, 0.014f, 0.010f, 0.15f, 0.15f, 0.16f, 3, 6)
        // Shoulder pauldrons
        mb.addChamferedBox(-0.26f, 0.45f, 0f, 0.14f, 0.14f, 0.22f, 0.03f, tr * 0.85f, tg * 0.85f, tb * 0.85f, 3)
        mb.addChamferedBox(0.26f, 0.45f, 0f, 0.14f, 0.14f, 0.22f, 0.03f, tr * 0.85f, tg * 0.85f, tb * 0.85f, 3)
        // Utility belt
        mb.addCenteredBox(0f, 0.04f, 0f, 0.47f, 0.09f, 0.28f, 0.14f, 0.14f, 0.15f, 3)
        return mb.build()
    }

    fun buildBotHead(isAlpha: Boolean): GpuMesh {
        val mb = MeshBuilder()
        val (vr, vg, vb) = if (isAlpha) Triple(0.12f, 0.88f, 1.0f) else Triple(1.0f, 0.36f, 0.10f)
        // Cylindrical neck
        mb.addTube(0f, -0.05f, 0f, 0f, 0.08f, 0f, 0.08f, 0.075f, 0.20f, 0.22f, 0.24f, 7, 10)
        // Rounded/chamfered helmet shell
        mb.addChamferedBox(0f, 0.18f, 0.01f, 0.31f, 0.26f, 0.33f, 0.07f, 0.24f, 0.27f, 0.28f, 3)
        // Lower face mask / respirator
        mb.addChamferedBox(0f, 0.08f, -0.06f, 0.23f, 0.12f, 0.20f, 0.04f, 0.14f, 0.16f, 0.18f, 3)
        // Glowing tactical visor (-Z forward)
        mb.addCenteredBox(0f, 0.19f, -0.158f, 0.25f, 0.075f, 0.035f, vr, vg, vb, -1)
        // NVG forehead mount bracket
        mb.addCenteredBox(0f, 0.29f, -0.16f, 0.09f, 0.05f, 0.05f, 0.18f, 0.20f, 0.22f, 3)
        return mb.build()
    }

    fun buildBotArm(isAlpha: Boolean): GpuMesh {
        val mb = MeshBuilder()
        val (ar, ag, ab) = if (isAlpha) Triple(0.22f, 0.36f, 0.56f) else Triple(0.46f, 0.22f, 0.22f)
        // Upper sleeve + forearm + tactical glove
        mb.addTube(0f, 0f, 0f, 0f, -0.25f, 0f, 0.072f, 0.064f, ar, ag, ab, 7, 10)
        mb.addTube(0f, -0.25f, 0f, 0f, -0.44f, 0f, 0.062f, 0.054f, 0.24f, 0.26f, 0.28f, 7, 10)
        mb.addChamferedBox(0f, -0.49f, 0f, 0.10f, 0.11f, 0.11f, 0.02f, 0.14f, 0.15f, 0.16f, 3)
        return mb.build()
    }

    fun buildBotLeg(): GpuMesh {
        val mb = MeshBuilder()
        // Thigh, kneepad, shin, combat boot
        mb.addTube(0f, 0f, 0f, 0f, -0.36f, 0f, 0.095f, 0.082f, 0.20f, 0.22f, 0.25f, 7, 10)
        mb.addChamferedBox(0f, -0.37f, -0.05f, 0.13f, 0.10f, 0.11f, 0.02f, 0.14f, 0.15f, 0.17f, 3)
        mb.addTube(0f, -0.36f, 0f, 0f, -0.64f, 0f, 0.080f, 0.068f, 0.18f, 0.20f, 0.22f, 7, 10)
        mb.addChamferedBox(0f, -0.69f, -0.03f, 0.14f, 0.10f, 0.23f, 0.025f, 0.11f, 0.11f, 0.12f, 3)
        return mb.build()
    }

    /**
     * First-Person Armored Tactical Forearms & Gloves attached relative to weapon space.
     */
    fun buildPlayerViewArms(): GpuMesh {
        val mb = MeshBuilder()
        // Right armored forearm & gloved hand gripping pistol grip
        mb.addTube(0.11f, -0.22f, 0.34f, 0.02f, -0.09f, 0.11f, 0.052f, 0.044f, 0.20f, 0.24f, 0.28f, 7, 10)
        mb.addChamferedBox(0.0f, -0.07f, 0.08f, 0.072f, 0.085f, 0.082f, 0.015f, 0.14f, 0.15f, 0.16f, 3)
        // Left armored forearm & gloved support hand cradling forend
        mb.addTube(-0.18f, -0.20f, 0.20f, -0.03f, -0.03f, -0.18f, 0.050f, 0.042f, 0.20f, 0.24f, 0.28f, 7, 10)
        mb.addChamferedBox(-0.01f, -0.02f, -0.20f, 0.072f, 0.065f, 0.095f, 0.015f, 0.14f, 0.15f, 0.16f, 3)
        return mb.build()
    }

    /**
     * Detailed, non-blocky 3D Weapon Models using tapered cylinders, chamfered receivers,
     * angled ergonomic grips, curved multi-segment magazines, Picatinny rails, scopes & glowing optics.
     */
    fun buildWeaponMesh(weaponId: WeaponId): GpuMesh {
        val spec = WeaponCatalog.byId(weaponId)
        val (br, bg, bb) = spec.bodyColor
        val (ar, ag, ab) = spec.accentColor
        val darkSteel = Triple(0.13f, 0.14f, 0.16f)
        val chromeSteel = Triple(0.45f, 0.48f, 0.52f)
        val mb = MeshBuilder()

        when (weaponId) {
            WeaponId.PISTOL_P9 -> {
                // Chamfered upper slide (-Z forward)
                mb.addChamferedBox(0f, 0.042f, -0.06f, 0.050f, 0.050f, 0.27f, 0.012f, br, bg, bb, 3)
                // Slide serrations (tactical grip ridges)
                for (i in 0..4) {
                    mb.addCenteredBox(0f, 0.042f, 0.02f + i * 0.012f, 0.053f, 0.036f, 0.005f, darkSteel.first, darkSteel.second, darkSteel.third, 3)
                }
                // Lower polymer frame + dust cover
                mb.addChamferedBox(0f, 0.008f, -0.05f, 0.044f, 0.028f, 0.23f, 0.008f, 0.14f, 0.15f, 0.17f, 3)
                // Cylindrical threaded barrel + muzzle compensator
                mb.addTube(0f, 0.040f, -0.18f, 0f, 0.040f, -0.24f, 0.014f, 0.014f, chromeSteel.first, chromeSteel.second, chromeSteel.third, 2, 12)
                mb.addTube(0f, 0.040f, -0.235f, 0f, 0.040f, -0.29f, 0.019f, 0.018f, ar, ag, ab, 3, 12)
                // Under-barrel tactical laser/light module
                mb.addTube(0f, -0.014f, -0.09f, 0f, -0.014f, -0.18f, 0.015f, 0.015f, darkSteel.first, darkSteel.second, darkSteel.third, 3, 10)
                mb.addTube(0f, -0.014f, -0.18f, 0f, -0.014f, -0.184f, 0.011f, 0.011f, 0.2f, 0.9f, 1.0f, -1, 10)
                // Angled ergonomic pistol grip
                mb.addOrientedBar(0f, -0.115f, 0.072f, 0f, -0.002f, 0.036f, 0.042f, 0.064f, 0.12f, 0.13f, 0.15f, 7)
                // Magazine baseplate
                mb.addChamferedBox(0f, -0.122f, 0.075f, 0.045f, 0.018f, 0.068f, 0.004f, ar, ag, ab, 3)
                // Trigger guard & trigger
                mb.addCenteredBox(0f, -0.036f, -0.028f, 0.016f, 0.008f, 0.062f, darkSteel.first, darkSteel.second, darkSteel.third, 3)
                mb.addCenteredBox(0f, -0.018f, -0.056f, 0.016f, 0.036f, 0.008f, darkSteel.first, darkSteel.second, darkSteel.third, 3)
                // Glowing 3-dot tritium iron sights
                mb.addCenteredBox(0f, 0.074f, -0.18f, 0.012f, 0.015f, 0.014f, 0.15f, 1.0f, 0.55f, -1)
                mb.addCenteredBox(-0.014f, 0.073f, 0.06f, 0.008f, 0.013f, 0.012f, 0.15f, 1.0f, 0.55f, -1)
                mb.addCenteredBox(0.014f, 0.073f, 0.06f, 0.008f, 0.013f, 0.012f, 0.15f, 1.0f, 0.55f, -1)
            }

            WeaponId.AR_M4A1 -> {
                // Chamfered upper & lower receiver
                mb.addChamferedBox(0f, 0.025f, 0.0f, 0.062f, 0.086f, 0.33f, 0.014f, br, bg, bb, 3)
                // Picatinny top rail teeth
                for (i in 0..10) {
                    mb.addCenteredBox(0f, 0.073f, -0.26f + i * 0.036f, 0.034f, 0.012f, 0.018f, darkSteel.first, darkSteel.second, darkSteel.third, 3)
                }
                // Cylindrical tactical quad-rail handguard
                mb.addTube(0f, 0.032f, -0.16f, 0f, 0.032f, -0.43f, 0.033f, 0.031f, 0.14f, 0.16f, 0.19f, 3, 8)
                // Fluted steel barrel + gas block + 3-prong flash hider
                mb.addTube(0f, 0.034f, -0.42f, 0f, 0.034f, -0.60f, 0.015f, 0.014f, chromeSteel.first, chromeSteel.second, chromeSteel.third, 2, 12)
                mb.addTube(0f, 0.034f, -0.59f, 0f, 0.034f, -0.65f, 0.020f, 0.022f, darkSteel.first, darkSteel.second, darkSteel.third, 3, 10)
                // Angled 3-segment curved STANAG magazine
                mb.addOrientedBar(0f, -0.085f, -0.072f, 0f, -0.010f, -0.058f, 0.038f, 0.068f, ar, ag, ab, 3)
                mb.addOrientedBar(0f, -0.155f, -0.098f, 0f, -0.082f, -0.072f, 0.037f, 0.066f, ar * 0.9f, ag * 0.9f, ab * 0.9f, 3)
                // Angled pistol grip
                mb.addOrientedBar(0f, -0.105f, 0.125f, 0f, -0.012f, 0.085f, 0.040f, 0.056f, 0.12f, 0.13f, 0.15f, 7)
                // Angled tactical foregrip
                mb.addTube(0f, -0.002f, -0.28f, 0f, -0.085f, -0.26f, 0.018f, 0.016f, 0.14f, 0.15f, 0.17f, 7, 10)
                // Tubular buffer tube + telescoping angled stock
                mb.addTube(0f, 0.032f, 0.16f, 0f, 0.032f, 0.39f, 0.022f, 0.022f, darkSteel.first, darkSteel.second, darkSteel.third, 3, 12)
                mb.addOrientedBar(0f, -0.045f, 0.37f, 0f, 0.038f, 0.25f, 0.044f, 0.11f, 0.13f, 0.14f, 0.16f, 3)
                // Cylindrical Holographic Optic + glowing reticle lens
                mb.addTube(0f, 0.102f, 0.02f, 0f, 0.102f, -0.08f, 0.028f, 0.030f, darkSteel.first, darkSteel.second, darkSteel.third, 3, 12)
                mb.addTube(0f, 0.102f, -0.02f, 0f, 0.102f, -0.03f, 0.018f, 0.018f, ar, ag, ab, -1, 12)
            }

            WeaponId.SMG_MPX -> {
                // Compact beveled receiver
                mb.addChamferedBox(0f, 0.024f, -0.02f, 0.056f, 0.078f, 0.28f, 0.014f, br, bg, bb, 3)
                // Cylindrical M-LOK shroud + integral suppressor with heat rings
                mb.addTube(0f, 0.028f, -0.15f, 0f, 0.028f, -0.32f, 0.028f, 0.028f, darkSteel.first, darkSteel.second, darkSteel.third, 3, 10)
                mb.addTube(0f, 0.028f, -0.30f, 0f, 0.028f, -0.49f, 0.022f, 0.022f, 0.22f, 0.24f, 0.26f, 2, 14)
                mb.addTube(0f, 0.028f, -0.39f, 0f, 0.028f, -0.405f, 0.025f, 0.025f, ar, ag, ab, 3, 14)
                // Long slanted stick magazine
                mb.addOrientedBar(0f, -0.165f, -0.125f, 0f, -0.010f, -0.082f, 0.032f, 0.046f, ar, ag, ab, 3)
                // Angled grip & stub foregrip
                mb.addOrientedBar(0f, -0.095f, 0.105f, 0f, -0.010f, 0.072f, 0.038f, 0.052f, 0.12f, 0.13f, 0.15f, 7)
                mb.addTube(0f, -0.002f, -0.24f, 0f, -0.075f, -0.23f, 0.016f, 0.014f, 0.15f, 0.16f, 0.18f, 7, 10)
                // Folding twin-tube wire stock
                mb.addTube(-0.018f, 0.036f, 0.12f, -0.018f, 0.030f, 0.32f, 0.008f, 0.008f, chromeSteel.first, chromeSteel.second, chromeSteel.third, 2, 8)
                mb.addTube(0.018f, 0.036f, 0.12f, 0.018f, 0.030f, 0.32f, 0.008f, 0.008f, chromeSteel.first, chromeSteel.second, chromeSteel.third, 2, 8)
                mb.addChamferedBox(0f, 0.005f, 0.33f, 0.044f, 0.095f, 0.028f, 0.008f, darkSteel.first, darkSteel.second, darkSteel.third, 3)
                // Micro red-dot cylinder
                mb.addTube(0f, 0.086f, 0.0f, 0f, 0.086f, -0.07f, 0.022f, 0.022f, darkSteel.first, darkSteel.second, darkSteel.third, 3, 12)
                mb.addTube(0f, 0.086f, -0.03f, 0f, 0.086f, -0.04f, 0.015f, 0.015f, ar, ag, ab, -1, 12)
            }

            WeaponId.SHOTGUN_M870 -> {
                // Rounded heavy receiver
                mb.addChamferedBox(0f, 0.024f, 0.02f, 0.062f, 0.080f, 0.30f, 0.018f, br, bg, bb, 3)
                // Long cylindrical steel barrel + under-barrel magazine tube
                mb.addTube(0f, 0.048f, -0.12f, 0f, 0.048f, -0.62f, 0.018f, 0.016f, chromeSteel.first, chromeSteel.second, chromeSteel.third, 2, 14)
                mb.addTube(0f, 0.006f, -0.12f, 0f, 0.006f, -0.54f, 0.017f, 0.017f, darkSteel.first, darkSteel.second, darkSteel.third, 2, 14)
                // Barrel clamp ring
                mb.addCenteredBox(0f, 0.027f, -0.50f, 0.040f, 0.066f, 0.026f, darkSteel.first, darkSteel.second, darkSteel.third, 3)
                // Ribbed cylindrical pump forend
                mb.addTube(0f, 0.002f, -0.20f, 0f, 0.002f, -0.39f, 0.032f, 0.030f, ar, ag, ab, 4, 12)
                // Angled timber/composite stock with curved buttpad
                mb.addOrientedBar(0f, -0.035f, 0.38f, 0f, 0.015f, 0.16f, 0.048f, 0.088f, 0.38f, 0.24f, 0.14f, 4)
                mb.addChamferedBox(0f, -0.040f, 0.41f, 0.050f, 0.115f, 0.032f, 0.010f, 0.12f, 0.12f, 0.13f, 3)
                // Side saddle 4 shotgun shells!
                for (i in 0..3) {
                    mb.addTube(-0.035f, 0.002f, -0.05f + i * 0.032f, -0.035f, 0.052f, -0.05f + i * 0.032f, 0.011f, 0.011f, 0.88f, 0.16f, 0.14f, 3, 8)
                }
                // Glowing front fiber sight
                mb.addCenteredBox(0f, 0.070f, -0.60f, 0.010f, 0.014f, 0.016f, 1.0f, 0.45f, 0.10f, -1)
            }

            WeaponId.SNIPER_AWP -> {
                // Sculpted thumbhole olive chassis
                mb.addChamferedBox(0f, 0.014f, 0.02f, 0.068f, 0.088f, 0.44f, 0.016f, br, bg, bb, 3)
                // Heavy cylindrical free-float barrel + slotted tank muzzle brake
                mb.addTube(0f, 0.042f, -0.18f, 0f, 0.042f, -0.78f, 0.022f, 0.017f, darkSteel.first, darkSteel.second, darkSteel.third, 2, 14)
                mb.addChamferedBox(0f, 0.042f, -0.82f, 0.046f, 0.040f, 0.085f, 0.008f, ar, ag, ab, 3)
                // Multi-stage cylindrical telescopic sniper scope!
                mb.addTube(0f, 0.108f, 0.10f, 0f, 0.108f, 0.02f, 0.028f, 0.022f, darkSteel.first, darkSteel.second, darkSteel.third, 3, 14) // eyepiece
                mb.addTube(0f, 0.108f, 0.02f, 0f, 0.108f, -0.16f, 0.020f, 0.020f, 0.18f, 0.20f, 0.22f, 2, 14) // main tube
                mb.addTube(0f, 0.108f, -0.16f, 0f, 0.108f, -0.26f, 0.020f, 0.036f, darkSteel.first, darkSteel.second, darkSteel.third, 3, 16) // flared objective bell
                // Glowing cyan scope lens + turret knobs
                mb.addTube(0f, 0.108f, -0.258f, 0f, 0.108f, -0.262f, 0.028f, 0.028f, 0.10f, 0.85f, 0.98f, -1, 14)
                mb.addCylinder(0f, 0.125f, -0.07f, 0.014f, 0.022f, ar, ag, ab, 3, 10)
                // Scope rings
                mb.addCenteredBox(0f, 0.082f, -0.01f, 0.044f, 0.050f, 0.024f, darkSteel.first, darkSteel.second, darkSteel.third, 3)
                mb.addCenteredBox(0f, 0.082f, -0.13f, 0.044f, 0.050f, 0.024f, darkSteel.first, darkSteel.second, darkSteel.third, 3)
                // Folded cylindrical bipod legs under forend
                mb.addTube(-0.028f, -0.018f, -0.20f, -0.036f, -0.028f, -0.42f, 0.010f, 0.012f, chromeSteel.first, chromeSteel.second, chromeSteel.third, 2, 8)
                mb.addTube(0.028f, -0.018f, -0.20f, 0.036f, -0.028f, -0.42f, 0.010f, 0.012f, chromeSteel.first, chromeSteel.second, chromeSteel.third, 2, 8)
                // Angled thumbhole stock + adjustable cheek riser
                mb.addOrientedBar(0f, -0.030f, 0.42f, 0f, 0.014f, 0.22f, 0.050f, 0.092f, br, bg, bb, 3)
                mb.addChamferedBox(0f, 0.048f, 0.34f, 0.044f, 0.032f, 0.12f, 0.008f, 0.14f, 0.15f, 0.16f, 7)
                // Precision box magazine
                mb.addChamferedBox(0f, -0.062f, -0.04f, 0.040f, 0.078f, 0.095f, 0.008f, darkSteel.first, darkSteel.second, darkSteel.third, 3)
            }

            WeaponId.LMG_M249 -> {
                // Heavy machined receiver + hinged feed cover
                mb.addChamferedBox(0f, 0.032f, -0.02f, 0.082f, 0.096f, 0.40f, 0.016f, br, bg, bb, 3)
                mb.addChamferedBox(0f, 0.090f, -0.04f, 0.068f, 0.024f, 0.26f, 0.008f, darkSteel.first, darkSteel.second, darkSteel.third, 2)
                // Perforated cylindrical barrel shroud + heavy fluted barrel + flash hider
                mb.addTube(0f, 0.042f, -0.20f, 0f, 0.042f, -0.48f, 0.038f, 0.035f, 0.16f, 0.18f, 0.20f, 2, 12)
                mb.addTube(0f, 0.045f, -0.46f, 0f, 0.045f, -0.68f, 0.018f, 0.017f, chromeSteel.first, chromeSteel.second, chromeSteel.third, 2, 12)
                mb.addTube(0f, 0.045f, -0.66f, 0f, 0.045f, -0.73f, 0.024f, 0.024f, darkSteel.first, darkSteel.second, darkSteel.third, 3, 10)
                // Large cylindrical ammo drum + visible brass cartridge belt!
                mb.addTube(-0.045f, -0.078f, -0.14f, -0.045f, -0.078f, -0.01f, 0.068f, 0.068f, ar, ag, ab, 2, 16)
                for (i in 0..4) {
                    mb.addTube(-0.062f, -0.02f + i * 0.014f, -0.07f, -0.025f, -0.01f + i * 0.014f, -0.07f, 0.006f, 0.003f, 0.96f, 0.78f, 0.24f, 2, 6)
                }
                // Angled grip & heavy tubular para stock
                mb.addOrientedBar(0f, -0.095f, 0.14f, 0f, -0.008f, 0.10f, 0.044f, 0.060f, 0.12f, 0.13f, 0.15f, 7)
                mb.addTube(0f, 0.042f, 0.18f, 0f, 0.032f, 0.42f, 0.020f, 0.020f, chromeSteel.first, chromeSteel.second, chromeSteel.third, 2, 10)
                mb.addTube(0f, -0.020f, 0.18f, 0f, -0.030f, 0.42f, 0.014f, 0.014f, chromeSteel.first, chromeSteel.second, chromeSteel.third, 2, 10)
                mb.addChamferedBox(0f, 0.002f, 0.43f, 0.056f, 0.115f, 0.034f, 0.010f, darkSteel.first, darkSteel.second, darkSteel.third, 3)
                // Folded bipod legs & carrying handle
                mb.addTube(-0.036f, -0.01f, -0.26f, -0.042f, -0.02f, -0.47f, 0.012f, 0.012f, darkSteel.first, darkSteel.second, darkSteel.third, 2, 8)
                mb.addTube(0.036f, -0.01f, -0.26f, 0.042f, -0.02f, -0.47f, 0.012f, 0.012f, darkSteel.first, darkSteel.second, darkSteel.third, 2, 8)
                mb.addCenteredBox(0f, 0.12f, -0.20f, 0.018f, 0.018f, 0.09f, ar, ag, ab, 3)
            }
        }
        return mb.build()
    }
}
