package com.example.game.engine

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.random.Random

/**
 * Zero-allocation Object Pool for 3D particles, bullet tracers, hit sparks, and ambient weather.
 */
class ParticlePool(private val maxParticles: Int = 220) {

    private class Particle {
        var active = false
        var x = 0f; var y = 0f; var z = 0f
        var vx = 0f; var vy = 0f; var vz = 0f
        var endX = 0f; var endY = 0f; var endZ = 0f
        var isTracer = false
        var life = 0f
        var maxLife = 0.2f
        var size = 0.08f
        var r = 1f; var g = 0.7f; var b = 0.2f
    }

    private val pool = Array(maxParticles) { Particle() }
    private val rng = Random(42)

    // Pre-allocated vertex buffer: up to maxParticles * 6 verts * 7 floats (x,y,z, r,g,b,a)
    private val vertexData = FloatArray(maxParticles * 6 * 7)
    private val vertexBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(vertexData.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()

    fun clear() {
        for (p in pool) p.active = false
    }

    fun spawnTracer(start: Vec3, end: Vec3, r: Float = 1f, g: Float = 0.85f, b: Float = 0.35f) {
        val p = acquire() ?: return
        p.active = true
        p.isTracer = true
        p.x = start.x; p.y = start.y; p.z = start.z
        p.endX = end.x; p.endY = end.y; p.endZ = end.z
        p.life = 0.09f
        p.maxLife = 0.09f
        p.r = r; p.g = g; p.b = b
        p.size = 0.03f
    }

    fun spawnBurst(pos: Vec3, count: Int, r: Float, g: Float, b: Float, speed: Float = 4.5f, size: Float = 0.09f) {
        for (i in 0 until count) {
            val p = acquire() ?: return
            p.active = true
            p.isTracer = false
            p.x = pos.x; p.y = pos.y; p.z = pos.z
            p.vx = (rng.nextFloat() * 2f - 1f) * speed
            p.vy = rng.nextFloat() * speed * 0.9f + 0.5f
            p.vz = (rng.nextFloat() * 2f - 1f) * speed
            p.life = 0.22f + rng.nextFloat() * 0.24f
            p.maxLife = p.life
            p.size = size
            p.r = r; p.g = g; p.b = b
        }
    }

    fun spawnAmbientWeather(center: Vec3, weatherType: Int) {
        if (weatherType == 0) return
        val p = acquire() ?: return
        p.active = true
        p.isTracer = false
        p.x = center.x + (rng.nextFloat() * 2f - 1f) * 24f
        p.z = center.z + (rng.nextFloat() * 2f - 1f) * 24f
        when (weatherType) {
            1 -> { // Desert dust
                p.y = 0.4f + rng.nextFloat() * 4.5f
                p.vx = 2.4f + rng.nextFloat() * 1.5f
                p.vy = (rng.nextFloat() - 0.4f) * 0.4f
                p.vz = 0.8f
                p.life = 1.8f
                p.size = 0.06f
                p.r = 0.88f; p.g = 0.74f; p.b = 0.52f
            }
            2 -> { // Snow flakes
                p.y = 9f + rng.nextFloat() * 4f
                p.vx = (rng.nextFloat() - 0.5f) * 1.1f
                p.vy = -3.2f - rng.nextFloat() * 1.4f
                p.vz = (rng.nextFloat() - 0.5f) * 1.1f
                p.life = 2.8f
                p.size = 0.075f
                p.r = 0.94f; p.g = 0.97f; p.b = 1.0f
            }
            else -> { // Industrial embers
                p.y = 0.3f + rng.nextFloat() * 3f
                p.vx = (rng.nextFloat() - 0.5f) * 1.2f
                p.vy = 1.6f + rng.nextFloat() * 1.4f
                p.vz = (rng.nextFloat() - 0.5f) * 1.2f
                p.life = 1.6f
                p.size = 0.055f
                p.r = 1.0f; p.g = 0.48f; p.b = 0.12f
            }
        }
        p.maxLife = p.life
    }

    fun update(dt: Float, weatherType: Int, cameraPos: Vec3, activeLimit: Int) {
        var activeCount = 0
        for (p in pool) {
            if (!p.active) continue
            p.life -= dt
            if (p.life <= 0f) {
                p.active = false
                continue
            }
            activeCount++
            if (!p.isTracer) {
                p.x += p.vx * dt
                p.y += p.vy * dt
                p.z += p.vz * dt
                if (weatherType == 0) {
                    p.vy -= 9.5f * dt
                }
            }
        }
        if (weatherType != 0 && activeCount < (activeLimit / 2).coerceAtMost(70)) {
            spawnAmbientWeather(cameraPos, weatherType)
        }
    }

    fun render(shader: UnlitParticleShader, mvpMatrix: FloatArray, camRight: Vec3, camUp: Vec3) {
        var floatCount = 0
        val maxFloats = vertexData.size

        for (p in pool) {
            if (!p.active) continue
            val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
            if (p.isTracer) {
                if (floatCount + 6 * 7 > maxFloats) break
                val s = p.size
                val ox = camUp.x * s; val oy = camUp.y * s; val oz = camUp.z * s
                floatCount = pushQuad(
                    floatCount,
                    p.x - ox, p.y - oy, p.z - oz,
                    p.x + ox, p.y + oy, p.z + oz,
                    p.endX + ox, p.endY + oy, p.endZ + oz,
                    p.endX - ox, p.endY - oy, p.endZ - oz,
                    p.r, p.g, p.b, alpha
                )
            } else {
                if (floatCount + 6 * 7 > maxFloats) break
                val s = p.size * (0.5f + 0.5f * alpha)
                val rx = camRight.x * s; val ry = camRight.y * s; val rz = camRight.z * s
                val ux = camUp.x * s; val uy = camUp.y * s; val uz = camUp.z * s
                floatCount = pushQuad(
                    floatCount,
                    p.x - rx - ux, p.y - ry - uy, p.z - rz - uz,
                    p.x + rx - ux, p.y + ry - uy, p.z + rz - uz,
                    p.x + rx + ux, p.y + ry + uy, p.z + rz + uz,
                    p.x - rx + ux, p.y - ry + uy, p.z - rz + uz,
                    p.r, p.g, p.b, alpha
                )
            }
        }

        val vertCount = floatCount / 7
        if (vertCount == 0) return

        vertexBuffer.position(0)
        vertexBuffer.put(vertexData, 0, floatCount)
        vertexBuffer.position(0)

        GLES20.glUseProgram(shader.programId)
        GLES20.glUniformMatrix4fv(shader.uMvpMatrix, 1, false, mvpMatrix, 0)

        val stride = 7 * 4
        GLES20.glEnableVertexAttribArray(shader.aPos)
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(shader.aPos, 3, GLES20.GL_FLOAT, false, stride, vertexBuffer)

        GLES20.glEnableVertexAttribArray(shader.aColor)
        vertexBuffer.position(3)
        GLES20.glVertexAttribPointer(shader.aColor, 4, GLES20.GL_FLOAT, false, stride, vertexBuffer)

        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertCount)
        GLES20.glDisable(GLES20.GL_BLEND)

        GLES20.glDisableVertexAttribArray(shader.aPos)
        GLES20.glDisableVertexAttribArray(shader.aColor)
    }

    private fun pushQuad(
        offset: Int,
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        r: Float, g: Float, b: Float, a: Float
    ): Int {
        var idx = offset
        fun put(x: Float, y: Float, z: Float) {
            vertexData[idx++] = x; vertexData[idx++] = y; vertexData[idx++] = z
            vertexData[idx++] = r; vertexData[idx++] = g; vertexData[idx++] = b; vertexData[idx++] = a
        }
        put(x0, y0, z0); put(x1, y1, z1); put(x2, y2, z2)
        put(x0, y0, z0); put(x2, y2, z2); put(x3, y3, z3)
        return idx
    }

    private fun acquire(): Particle? {
        for (p in pool) {
            if (!p.active) return p
        }
        return null
    }
}
