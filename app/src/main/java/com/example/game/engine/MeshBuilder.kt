package com.example.game.engine

import android.opengl.GLES20
import com.example.game.maps.MapDefinition
import com.example.game.model.WeaponCatalog
import com.example.game.model.WeaponId
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Packed mesh with stride = 10 floats:
 * [x, y, z, nx, ny, nz, r, g, b, matType]
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

        GLES20.glEnableVertexAttribArray(shader.aColor)
        GLES20.glVertexAttribPointer(shader.aColor, 3, GLES20.GL_FLOAT, false, strideBytes, 6 * 4)

        GLES20.glEnableVertexAttribArray(shader.aMatType)
        GLES20.glVertexAttribPointer(shader.aMatType, 1, GLES20.GL_FLOAT, false, strideBytes, 9 * 4)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)

        GLES20.glDisableVertexAttribArray(shader.aPos)
        GLES20.glDisableVertexAttribArray(shader.aNormal)
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
        const val STRIDE_FLOATS = 10
    }
}

class MeshBuilder {
    private val data = ArrayList<Float>(4096)

    fun addBox(
        minX: Float, minY: Float, minZ: Float,
        maxX: Float, maxY: Float, maxZ: Float,
        r: Float, g: Float, b: Float,
        matType: Int = 1
    ) {
        val m = matType.toFloat()
        // Front (+Z)
        addQuad(
            minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ,
            0f, 0f, 1f, r, g, b, m
        )
        // Back (-Z)
        addQuad(
            maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ,
            0f, 0f, -1f, r, g, b, m
        )
        // Left (-X)
        addQuad(
            minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ,
            -1f, 0f, 0f, r, g, b, m
        )
        // Right (+X)
        addQuad(
            maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ,
            1f, 0f, 0f, r, g, b, m
        )
        // Top (+Y)
        addQuad(
            minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ,
            0f, 1f, 0f, (r * 1.06f).coerceAtMost(1f), (g * 1.06f).coerceAtMost(1f), (b * 1.06f).coerceAtMost(1f), m
        )
        // Bottom (-Y)
        addQuad(
            minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ,
            0f, -1f, 0f, r * 0.75f, g * 0.75f, b * 0.75f, m
        )
    }

    fun addCenteredBox(
        cx: Float, cy: Float, cz: Float,
        sx: Float, sy: Float, sz: Float,
        r: Float, g: Float, b: Float,
        matType: Int = 2
    ) {
        addBox(
            cx - sx * 0.5f, cy - sy * 0.5f, cz - sz * 0.5f,
            cx + sx * 0.5f, cy + sy * 0.5f, cz + sz * 0.5f,
            r, g, b, matType
        )
    }

    fun addCylinder(
        cx: Float, baseY: Float, cz: Float,
        radius: Float, height: Float,
        r: Float, g: Float, b: Float,
        matType: Int = 2,
        segments: Int = 12
    ) {
        val m = matType.toFloat()
        val topY = baseY + height
        for (i in 0 until segments) {
            val a0 = (i.toFloat() / segments) * (2f * PI.toFloat())
            val a1 = ((i + 1).toFloat() / segments) * (2f * PI.toFloat())
            val x0 = cx + cos(a0) * radius
            val z0 = cz + sin(a0) * radius
            val x1 = cx + cos(a1) * radius
            val z1 = cz + sin(a1) * radius
            val nx = cos((a0 + a1) * 0.5f)
            val nz = sin((a0 + a1) * 0.5f)

            addQuad(
                x0, baseY, z0, x1, baseY, z1, x1, topY, z1, x0, topY, z0,
                nx, 0f, nz, r, g, b, m
            )
            // Top cap triangle
            addTri(cx, topY, cz, x1, topY, z1, x0, topY, z0, 0f, 1f, 0f, r * 1.05f, g * 1.05f, b * 1.05f, m)
        }
    }

    private fun addQuad(
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        nx: Float, ny: Float, nz: Float,
        r: Float, g: Float, b: Float, m: Float
    ) {
        addTri(x0, y0, z0, x1, y1, z1, x2, y2, z2, nx, ny, nz, r, g, b, m)
        addTri(x0, y0, z0, x2, y2, z2, x3, y3, z3, nx, ny, nz, r, g, b, m)
    }

    private fun addTri(
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        nx: Float, ny: Float, nz: Float,
        r: Float, g: Float, b: Float, m: Float
    ) {
        pushVertex(x0, y0, z0, nx, ny, nz, r, g, b, m)
        pushVertex(x1, y1, z1, nx, ny, nz, r, g, b, m)
        pushVertex(x2, y2, z2, nx, ny, nz, r, g, b, m)
    }

    private fun pushVertex(
        x: Float, y: Float, z: Float,
        nx: Float, ny: Float, nz: Float,
        r: Float, g: Float, b: Float, m: Float
    ) {
        data.add(x); data.add(y); data.add(z)
        data.add(nx); data.add(ny); data.add(nz)
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
        // Ground slab
        mb.addBox(-ext, -0.5f, -ext, ext, 0f, ext, fr, fg, fb, map.floorMatType)

        for (b in map.boxes) {
            mb.addBox(b.aabb.minX, b.aabb.minY, b.aabb.minZ, b.aabb.maxX, b.aabb.maxY, b.aabb.maxZ, b.r, b.g, b.b, b.matType)
        }
        for (c in map.cylinders) {
            mb.addCylinder(c.cx, c.baseY, c.cz, c.radius, c.height, c.r, c.g, c.b, c.matType, c.segments)
        }
        return mb.build()
    }

    fun buildSkyDomeBox(map: MapDefinition): GpuMesh {
        val mb = MeshBuilder()
        val s = 135f
        val (hr, hg, hb) = map.skyHorizonColor
        val (tr, tg, tb) = map.skyTopColor
        // Inside-facing large sky ceiling & horizon panels (unlit-like high ambient)
        mb.addBox(-s, -1f, -s, s, 0.01f, s, hr, hg, hb, 0)
        // High sky cap
        mb.addBox(-s, 42f, -s, s, 44f, s, tr, tg, tb, 0)
        return mb.build()
    }

    fun buildUnitBoxMesh(): GpuMesh {
        val mb = MeshBuilder()
        mb.addCenteredBox(0f, 0f, 0f, 1f, 1f, 1f, 1f, 1f, 1f, 0)
        return mb.build()
    }

    // Articulated Bot sub-meshes (origin at pivot point)
    fun buildBotTorso(isAlpha: Boolean): GpuMesh {
        val mb = MeshBuilder()
        val (tr, tg, tb) = if (isAlpha) Triple(0.16f, 0.36f, 0.68f) else Triple(0.68f, 0.18f, 0.20f)
        // Tactical armor vest + chest plate
        mb.addCenteredBox(0f, 0.28f, 0f, 0.46f, 0.56f, 0.26f, 0.20f, 0.22f, 0.25f, 2)
        mb.addCenteredBox(0f, 0.30f, 0f, 0.48f, 0.38f, 0.29f, tr, tg, tb, 2)
        // Belt & pouches
        mb.addCenteredBox(0f, 0.03f, 0f, 0.47f, 0.09f, 0.28f, 0.14f, 0.14f, 0.15f, 4)
        return mb.build()
    }

    fun buildBotHead(isAlpha: Boolean): GpuMesh {
        val mb = MeshBuilder()
        val (vr, vg, vb) = if (isAlpha) Triple(0.10f, 0.85f, 0.98f) else Triple(1.0f, 0.38f, 0.10f)
        // Helmet
        mb.addCenteredBox(0f, 0.18f, 0f, 0.30f, 0.28f, 0.32f, 0.22f, 0.24f, 0.26f, 2)
        // Glowing tactical visor (-Z is forward)
        mb.addCenteredBox(0f, 0.17f, -0.165f, 0.24f, 0.09f, 0.04f, vr, vg, vb, 0)
        return mb.build()
    }

    fun buildBotLimb(w: Float, len: Float, r: Float, g: Float, b: Float): GpuMesh {
        val mb = MeshBuilder()
        mb.addBox(-w * 0.5f, -len, -w * 0.5f, w * 0.5f, 0f, w * 0.5f, r, g, b, 1)
        return mb.build()
    }

    fun buildWeaponMesh(weaponId: WeaponId): GpuMesh {
        val spec = WeaponCatalog.byId(weaponId)
        val (br, bg, bb) = spec.bodyColor
        val (ar, ag, ab) = spec.accentColor
        val mb = MeshBuilder()

        when (weaponId) {
            WeaponId.PISTOL_P9 -> {
                // Slide & frame (-Z forward)
                mb.addCenteredBox(0f, 0.03f, -0.05f, 0.055f, 0.06f, 0.26f, br, bg, bb, 2)
                mb.addCenteredBox(0f, 0.062f, -0.05f, 0.050f, 0.015f, 0.24f, ar, ag, ab, 2)
                // Grip
                mb.addCenteredBox(0f, -0.05f, 0.04f, 0.048f, 0.13f, 0.07f, 0.12f, 0.13f, 0.14f, 4)
                // Iron sight
                mb.addCenteredBox(0f, 0.075f, -0.16f, 0.014f, 0.016f, 0.018f, 0.2f, 0.95f, 0.6f, 0)
            }

            WeaponId.AR_M4A1 -> {
                // Receiver
                mb.addCenteredBox(0f, 0.02f, 0f, 0.065f, 0.09f, 0.34f, br, bg, bb, 2)
                // Handguard + rail
                mb.addCenteredBox(0f, 0.03f, -0.28f, 0.058f, 0.068f, 0.24f, 0.14f, 0.16f, 0.18f, 2)
                // Barrel
                mb.addCenteredBox(0f, 0.035f, -0.46f, 0.026f, 0.026f, 0.18f, 0.28f, 0.30f, 0.32f, 2)
                // Stock
                mb.addCenteredBox(0f, 0.015f, 0.26f, 0.052f, 0.095f, 0.20f, 0.13f, 0.14f, 0.16f, 4)
                // Curved-style Magazine
                mb.addCenteredBox(0f, -0.10f, -0.06f, 0.042f, 0.16f, 0.075f, ar, ag, ab, 2)
                // Grip
                mb.addCenteredBox(0f, -0.07f, 0.10f, 0.045f, 0.12f, 0.06f, 0.12f, 0.13f, 0.15f, 4)
                // Holographic Optic
                mb.addCenteredBox(0f, 0.09f, -0.02f, 0.048f, 0.055f, 0.11f, 0.12f, 0.12f, 0.14f, 2)
                mb.addCenteredBox(0f, 0.095f, -0.02f, 0.028f, 0.028f, 0.02f, ar, ag, ab, 0)
            }

            WeaponId.SMG_MPX -> {
                mb.addCenteredBox(0f, 0.02f, -0.02f, 0.062f, 0.085f, 0.30f, br, bg, bb, 2)
                mb.addCenteredBox(0f, 0.025f, -0.24f, 0.048f, 0.058f, 0.16f, 0.12f, 0.14f, 0.16f, 2)
                mb.addCenteredBox(0f, 0.025f, -0.37f, 0.036f, 0.036f, 0.12f, 0.24f, 0.26f, 0.28f, 2) // suppressor look
                mb.addCenteredBox(0f, -0.11f, -0.09f, 0.036f, 0.18f, 0.048f, ar, ag, ab, 2) // stick mag
                mb.addCenteredBox(0f, -0.065f, 0.08f, 0.044f, 0.11f, 0.055f, 0.12f, 0.12f, 0.13f, 4)
                mb.addCenteredBox(0f, 0.085f, -0.02f, 0.042f, 0.042f, 0.08f, 0.15f, 0.16f, 0.18f, 2)
            }

            WeaponId.SHOTGUN_M870 -> {
                mb.addCenteredBox(0f, 0.02f, 0.02f, 0.068f, 0.085f, 0.32f, br, bg, bb, 2)
                // Long barrel & under-tube
                mb.addCenteredBox(0f, 0.045f, -0.34f, 0.032f, 0.032f, 0.44f, 0.26f, 0.28f, 0.30f, 2)
                mb.addCenteredBox(0f, 0.005f, -0.30f, 0.030f, 0.030f, 0.36f, 0.20f, 0.22f, 0.24f, 2)
                // Pump forend
                mb.addCenteredBox(0f, -0.005f, -0.25f, 0.058f, 0.062f, 0.17f, ar, ag, ab, 4)
                // Heavy Stock
                mb.addCenteredBox(0f, -0.01f, 0.28f, 0.056f, 0.11f, 0.26f, 0.32f, 0.22f, 0.14f, 4)
            }

            WeaponId.SNIPER_AWP -> {
                // Polymer Chassis
                mb.addCenteredBox(0f, 0.01f, 0f, 0.072f, 0.095f, 0.46f, br, bg, bb, 1)
                // Long fluted barrel
                mb.addCenteredBox(0f, 0.04f, -0.48f, 0.032f, 0.032f, 0.52f, 0.22f, 0.24f, 0.26f, 2)
                // Muzzle brake
                mb.addCenteredBox(0f, 0.04f, -0.76f, 0.046f, 0.046f, 0.07f, ar, ag, ab, 2)
                // Heavy Scope
                mb.addCenteredBox(0f, 0.105f, -0.06f, 0.056f, 0.056f, 0.30f, 0.12f, 0.13f, 0.14f, 2)
                mb.addCenteredBox(0f, 0.105f, -0.22f, 0.066f, 0.066f, 0.06f, ar, ag, ab, 2)
                // Stock & Mag
                mb.addCenteredBox(0f, 0.005f, 0.32f, 0.058f, 0.11f, 0.24f, br, bg, bb, 1)
                mb.addCenteredBox(0f, -0.07f, -0.04f, 0.042f, 0.09f, 0.09f, 0.14f, 0.15f, 0.16f, 2)
            }

            WeaponId.LMG_M249 -> {
                // Heavy receiver
                mb.addCenteredBox(0f, 0.03f, -0.02f, 0.088f, 0.11f, 0.42f, br, bg, bb, 2)
                // Shroud + heavy barrel
                mb.addCenteredBox(0f, 0.04f, -0.36f, 0.070f, 0.076f, 0.28f, 0.18f, 0.20f, 0.22f, 2)
                mb.addCenteredBox(0f, 0.045f, -0.56f, 0.032f, 0.032f, 0.20f, 0.26f, 0.28f, 0.30f, 2)
                // Ammo Box Drum
                mb.addCenteredBox(-0.04f, -0.085f, -0.08f, 0.14f, 0.13f, 0.13f, ar, ag, ab, 2)
                // Heavy Stock
                mb.addCenteredBox(0f, 0.01f, 0.30f, 0.062f, 0.115f, 0.22f, 0.16f, 0.17f, 0.19f, 2)
            }
        }
        return mb.build()
    }
}
