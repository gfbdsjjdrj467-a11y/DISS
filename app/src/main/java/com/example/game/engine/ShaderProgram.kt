package com.example.game.engine

import android.opengl.GLES20

class TacticalWorldShader {
    val programId: Int
    val uMvpMatrix: Int
    val uModelMatrix: Int
    val uSunDir: Int
    val uSunColor: Int
    val uAmbientColor: Int
    val uFogColor: Int
    val uFogDensity: Int
    val uCameraPos: Int
    val uMuzzlePos: Int
    val uMuzzleIntensity: Int
    val uDetailLevel: Int
    val uTintOverride: Int

    val aPos: Int
    val aNormal: Int
    val aColor: Int
    val aMatType: Int

    init {
        val vertSrc = """
            uniform mat4 uMvpMatrix;
            uniform mat4 uModelMatrix;
            attribute vec3 aPos;
            attribute vec3 aNormal;
            attribute vec3 aColor;
            attribute float aMatType;
            varying vec3 vWorldPos;
            varying vec3 vNormal;
            varying vec3 vColor;
            varying float vMatType;
            void main() {
                vec4 worldPos = uModelMatrix * vec4(aPos, 1.0);
                vWorldPos = worldPos.xyz;
                vNormal = normalize(mat3(uModelMatrix) * aNormal);
                vColor = aColor;
                vMatType = aMatType;
                gl_Position = uMvpMatrix * vec4(aPos, 1.0);
            }
        """.trimIndent()

        val fragSrc = """
            precision mediump float;
            uniform vec3 uSunDir;
            uniform vec3 uSunColor;
            uniform vec3 uAmbientColor;
            uniform vec3 uFogColor;
            uniform float uFogDensity;
            uniform vec3 uCameraPos;
            uniform vec3 uMuzzlePos;
            uniform float uMuzzleIntensity;
            uniform int uDetailLevel;
            uniform vec4 uTintOverride;

            varying vec3 vWorldPos;
            varying vec3 vNormal;
            varying vec3 vColor;
            varying float vMatType;

            void main() {
                vec3 n = normalize(vNormal);
                float ndotl = max(dot(n, normalize(uSunDir)), 0.0);
                float hemi = 0.5 + 0.5 * n.y;
                vec3 lit = vColor * (uAmbientColor * (0.65 + 0.45 * hemi) + uSunColor * ndotl * 0.85);

                // Procedural surface micro-detail (no external texture dependencies)
                if (uDetailLevel > 0) {
                    int m = int(vMatType + 0.5);
                    vec2 uv = abs(n.y) > 0.7 ? vWorldPos.xz : (abs(n.x) > 0.7 ? vWorldPos.zy : vWorldPos.xy);
                    if (m == 1) {
                        // Concrete / stone block seams
                        float gx = abs(fract(uv.x * 0.45) - 0.5);
                        float gy = abs(fract(uv.y * 0.45) - 0.5);
                        float seam = smoothstep(0.46, 0.50, max(gx, gy));
                        lit *= (1.0 - seam * 0.18);
                    } else if (m == 2) {
                        // Ribbed metal container / industrial steel
                        float rib = sin(uv.x * 14.0) * 0.08 + sin(uv.y * 2.5) * 0.04;
                        lit *= (1.0 + rib);
                    } else if (m == 3) {
                        // Urban facade windows & brick tiers
                        float wx = step(0.22, fract(uv.x * 0.35)) * step(fract(uv.x * 0.35), 0.78);
                        float wy = step(0.32, fract(uv.y * 0.32)) * step(fract(uv.y * 0.32), 0.76);
                        if (abs(n.y) < 0.5 && vWorldPos.y > 2.2) {
                            lit = mix(lit, vec3(0.14, 0.22, 0.30), wx * wy * 0.72);
                        }
                    } else if (m == 4) {
                        // Wood planks
                        float grain = sin(uv.y * 18.0 + sin(uv.x * 3.0) * 1.5) * 0.07;
                        lit *= (1.0 + grain);
                    } else if (m == 5) {
                        // Desert sand ripples
                        float ripple = sin(uv.x * 2.4 + uv.y * 1.4) * 0.06;
                        lit *= (1.0 + ripple);
                    } else if (m == 6) {
                        // Snow / ice shimmer
                        float sparkle = sin(uv.x * 9.0) * cos(uv.y * 9.0) * 0.05;
                        lit += vec3(sparkle * 0.5);
                    }
                }

                // Dynamic muzzle flash / combat light
                if (uDetailLevel > 0 && uMuzzleIntensity > 0.01) {
                    vec3 toLight = uMuzzlePos - vWorldPos;
                    float distSq = dot(toLight, toLight);
                    float atten = uMuzzleIntensity / (1.0 + distSq * 0.22);
                    lit += vec3(1.0, 0.62, 0.22) * atten;
                }

                // Hit flash or team tint override
                if (uTintOverride.a > 0.01) {
                    lit = mix(lit, uTintOverride.rgb, uTintOverride.a);
                }

                // Distance exponential fog
                float dist = length(uCameraPos - vWorldPos);
                float fogFactor = clamp(1.0 - exp(-pow(dist * uFogDensity, 2.0)), 0.0, 1.0);
                vec3 finalCol = mix(lit, uFogColor, fogFactor);

                gl_FragColor = vec4(finalCol, 1.0);
            }
        """.trimIndent()

        programId = compileProgram(vertSrc, fragSrc)
        uMvpMatrix = GLES20.glGetUniformLocation(programId, "uMvpMatrix")
        uModelMatrix = GLES20.glGetUniformLocation(programId, "uModelMatrix")
        uSunDir = GLES20.glGetUniformLocation(programId, "uSunDir")
        uSunColor = GLES20.glGetUniformLocation(programId, "uSunColor")
        uAmbientColor = GLES20.glGetUniformLocation(programId, "uAmbientColor")
        uFogColor = GLES20.glGetUniformLocation(programId, "uFogColor")
        uFogDensity = GLES20.glGetUniformLocation(programId, "uFogDensity")
        uCameraPos = GLES20.glGetUniformLocation(programId, "uCameraPos")
        uMuzzlePos = GLES20.glGetUniformLocation(programId, "uMuzzlePos")
        uMuzzleIntensity = GLES20.glGetUniformLocation(programId, "uMuzzleIntensity")
        uDetailLevel = GLES20.glGetUniformLocation(programId, "uDetailLevel")
        uTintOverride = GLES20.glGetUniformLocation(programId, "uTintOverride")

        aPos = GLES20.glGetAttribLocation(programId, "aPos")
        aNormal = GLES20.glGetAttribLocation(programId, "aNormal")
        aColor = GLES20.glGetAttribLocation(programId, "aColor")
        aMatType = GLES20.glGetAttribLocation(programId, "aMatType")
    }
}

class UnlitParticleShader {
    val programId: Int
    val uMvpMatrix: Int
    val aPos: Int
    val aColor: Int

    init {
        val vertSrc = """
            uniform mat4 uMvpMatrix;
            attribute vec3 aPos;
            attribute vec4 aColor;
            varying vec4 vColor;
            void main() {
                vColor = aColor;
                gl_Position = uMvpMatrix * vec4(aPos, 1.0);
            }
        """.trimIndent()

        val fragSrc = """
            precision mediump float;
            varying vec4 vColor;
            void main() {
                gl_FragColor = vColor;
            }
        """.trimIndent()

        programId = compileProgram(vertSrc, fragSrc)
        uMvpMatrix = GLES20.glGetUniformLocation(programId, "uMvpMatrix")
        aPos = GLES20.glGetAttribLocation(programId, "aPos")
        aColor = GLES20.glGetAttribLocation(programId, "aColor")
    }
}

private fun compileShader(type: Int, src: String): Int {
    val id = GLES20.glCreateShader(type)
    GLES20.glShaderSource(id, src)
    GLES20.glCompileShader(id)
    val compiled = IntArray(1)
    GLES20.glGetShaderiv(id, GLES20.GL_COMPILE_STATUS, compiled, 0)
    if (compiled[0] == 0) {
        val log = GLES20.glGetShaderInfoLog(id)
        GLES20.glDeleteShader(id)
        throw RuntimeException("GLSL compile error: $log")
    }
    return id
}

private fun compileProgram(vertSrc: String, fragSrc: String): Int {
    val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertSrc)
    val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragSrc)
    val prog = GLES20.glCreateProgram()
    GLES20.glAttachShader(prog, vs)
    GLES20.glAttachShader(prog, fs)
    GLES20.glLinkProgram(prog)
    GLES20.glDeleteShader(vs)
    GLES20.glDeleteShader(fs)
    return prog
}
