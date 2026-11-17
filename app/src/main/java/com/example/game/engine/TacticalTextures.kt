package com.example.game.engine

import android.graphics.Bitmap
import android.graphics.Color
import android.opengl.GLES20
import android.opengl.GLUtils
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Generates a seamless 1024x512 (4x2 tiles of 256x256) procedural PBR-inspired texture atlas
 * at OpenGL initialization so every surface has rich detail with zero external asset bloat.
 *
 * Tile layout (col, row) -> matType index:
 * 0 (0,0): Tactical Concrete / Asphalt
 * 1 (1,0): Urban Brick / Window Facade
 * 2 (2,0): Ribbed Steel / Shipping Container
 * 3 (3,0): Anodized Gunmetal / Carbon Grip
 * 4 (0,1): Timber Wood Planks
 * 5 (1,1): Desert Sand / Woven Sandbags
 * 6 (2,1): Alpine Snow / Frosted Ice
 * 7 (3,1): Tactical Kevlar Camo
 */
class TacticalTextureAtlas {
    var textureId: Int = 0
        private set

    fun createAndUpload(): Int {
        val tileSize = 256
        val width = tileSize * 4
        val height = tileSize * 2
        val pixels = IntArray(width * height)

        for (ty in 0 until 2) {
            for (tx in 0 until 4) {
                val tileIdx = ty * 4 + tx
                val ox = tx * tileSize
                val oy = ty * tileSize
                for (y in 0 until tileSize) {
                    for (x in 0 until tileSize) {
                        val u = x.toFloat() / tileSize
                        val v = y.toFloat() / tileSize
                        val c = evalTilePixel(tileIdx, u, v, x, y)
                        pixels[(oy + y) * width + (ox + x)] = c
                    }
                }
            }
        }

        val bmp = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        textureId = ids[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
        bmp.recycle()
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        return textureId
    }

    fun delete() {
        if (textureId != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
            textureId = 0
        }
    }

    private fun evalTilePixel(tile: Int, u: Float, v: Float, px: Int, py: Int): Int {
        val n1 = hashNoise(px, py)
        val n2 = smoothNoise(u * 16f, v * 16f, 16)
        val n3 = smoothNoise(u * 6f, v * 6f, 6)

        val lum: Float
        var rTint = 1f
        var gTint = 1f
        var bTint = 1f

        when (tile) {
            // 0: Concrete / Asphalt with expansion panel seams & aggregate grit
            0 -> {
                val seamX = if (px < 4 || px > 251 || abs(px - 128) < 2) 0.72f else 1f
                val seamY = if (py < 4 || py > 251 || abs(py - 128) < 2) 0.72f else 1f
                val bolt = if ((px % 128 in 10..14) && (py % 128 in 10..14)) 0.78f else 1f
                lum = (0.78f + n1 * 0.12f + n2 * 0.14f - n3 * 0.08f) * seamX * seamY * bolt
            }

            // 1: Brick / Urban Masonry with mortar seams
            1 -> {
                val row = (v * 8f).toInt()
                val rowFrac = (v * 8f) - row
                val offset = if (row % 2 == 1) 0.5f else 0f
                val colFrac = ((u * 4f + offset) % 1f)
                val isMortar = rowFrac < 0.11f || colFrac < 0.06f
                val brickVar = hashNoise(row * 17 + ((u * 4f + offset).toInt()), row)
                if (isMortar) {
                    lum = 0.62f + n1 * 0.06f
                    rTint = 0.94f; gTint = 0.94f; bTint = 0.96f
                } else {
                    lum = 0.80f + brickVar * 0.18f + n2 * 0.08f
                }
            }

            // 2: Corrugated Military Steel / Container Ribs + rivets
            2 -> {
                val rib = sin(u * Math.PI.toFloat() * 24f) * 0.16f
                val edgeFrame = if (py < 10 || py > 245 || px < 8 || px > 247) 0.78f else 1f
                val scratch = if (n2 > 0.78f && n1 > 0.65f) 1.16f else 1f
                lum = (0.84f + rib + n1 * 0.05f) * edgeFrame * scratch
            }

            // 3: Anodized Gunmetal / Carbon Fiber Tactical Weave
            3 -> {
                val check = ((px / 4) + (py / 4)) % 2
                val weave = if (check == 0) 0.92f else 0.78f
                val brushed = sin(u * 40f + n1 * 0.5f) * 0.04f
                lum = (weave + brushed + n1 * 0.06f).coerceIn(0.55f, 1.05f)
            }

            // 4: Timber Wood Planks with grain
            4 -> {
                val plankIdx = (v * 4f).toInt()
                val plankFrac = (v * 4f) - plankIdx
                val seam = if (plankFrac < 0.05f || (px % 256 < 3 && plankIdx % 2 == 0) || (abs(px - 128) < 2 && plankIdx % 2 == 1)) 0.58f else 1f
                val grain = sin((v * 28f + smoothNoise(u * 4f, v * 20f, 20) * 3.5f) * 3.14f) * 0.11f
                val plankShade = 0.85f + hashNoise(plankIdx, 7) * 0.15f
                lum = (plankShade + grain + n1 * 0.04f) * seam
                rTint = 1.04f; gTint = 0.97f; bTint = 0.90f
            }

            // 5: Desert Sand Ripples & Granular Grit
            5 -> {
                val ripple = sin((u * 6f + v * 3f + n3 * 1.4f) * Math.PI.toFloat() * 2f) * 0.09f
                lum = 0.84f + ripple + n1 * 0.10f + n2 * 0.06f
                rTint = 1.03f; gTint = 0.99f; bTint = 0.92f
            }

            // 6: Alpine Snow & Frosted Ice Crystals
            6 -> {
                val sparkle = if (n1 > 0.93f) 1.12f else 1f
                val drift = sin((u * 4f - v * 4f + n3) * Math.PI.toFloat() * 2f) * 0.06f
                lum = (0.88f + drift + n2 * 0.09f) * sparkle
                rTint = 0.95f; gTint = 0.99f; bTint = 1.05f
            }

            // 7: Digital / Woodland Tactical Kevlar Camo
            else -> {
                val block = smoothNoise((px / 14).toFloat(), (py / 14).toFloat(), 18)
                val fineFabric = (((px % 3) == 0) xor ((py % 3) == 0))
                val fabricShade = if (fineFabric) 0.94f else 1.02f
                lum = when {
                    block < 0.32f -> 0.58f * fabricShade
                    block < 0.65f -> 0.82f * fabricShade
                    else -> 1.02f * fabricShade
                } + n1 * 0.04f
            }
        }

        val r = (lum * rTint * 255f).toInt().coerceIn(0, 255)
        val g = (lum * gTint * 255f).toInt().coerceIn(0, 255)
        val b = (lum * bTint * 255f).toInt().coerceIn(0, 255)
        return Color.argb(255, r, g, b)
    }

    private fun hashNoise(x: Int, y: Int): Float {
        var h = x * 374761393 + y * 668265263
        h = (h xor (h ushr 13)) * 1274126177
        h = h xor (h ushr 16)
        return ((h and 0xFFFF).toFloat() / 65535f)
    }

    private fun smoothNoise(x: Float, y: Float, period: Int): Float {
        val ix = kotlin.math.floor(x).toInt()
        val iy = kotlin.math.floor(y).toInt()
        val fx = x - ix
        val fy = y - iy
        val sx = fx * fx * (3f - 2f * fx)
        val sy = fy * fy * (3f - 2f * fy)

        val x0 = ((ix % period) + period) % period
        val y0 = ((iy % period) + period) % period
        val x1 = (x0 + 1) % period
        val y1 = (y0 + 1) % period

        val v00 = hashNoise(x0, y0)
        val v10 = hashNoise(x1, y0)
        val v01 = hashNoise(x0, y1)
        val v11 = hashNoise(x1, y1)

        val nx0 = v00 + (v10 - v00) * sx
        val nx1 = v01 + (v11 - v01) * sx
        return nx0 + (nx1 - nx0) * sy
    }
}
