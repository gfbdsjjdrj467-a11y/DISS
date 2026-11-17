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
    val uAtlasTex: Int

    val aPos: Int
    val aNormal: Int
    val aUv: Int
    val aColor: Int
    val aMatType: Int

    init {
        val vertSrc = """
            uniform mat4 uMvpMatrix;
            uniform mat4 uModelMatrix;
            attribute vec3 aPos;
            attribute vec3 aNormal;
            attribute vec2 aUv;
            attribute vec3 aColor;
            attribute float aMatType;
            varying vec3 vWorldPos;
            varying vec3 vNormal;
            varying vec2 vUv;
            varying vec3 vColor;
            varying float vMatType;
            void main() {
                vec4 worldPos = uModelMatrix * vec4(aPos, 1.0);
                vWorldPos = worldPos.xyz;
                vNormal = normalize(mat3(uModelMatrix) * aNormal);
                vUv = aUv;
                vColor = aColor;
                vMatType = aMatType;
                gl_Position = uMvpMatrix * vec4(aPos, 1.0);
            }
        """.trimIndent()

        val fragSrc = """
            precision mediump float;
            uniform sampler2D uAtlasTex;
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
            varying vec2 vUv;
            varying vec3 vColor;
            varying float vMatType;

            void main() {
                // Emissive surfaces (visors, optic lenses, glowing sights, beacons)
                if (vMatType < -0.5) {
                    vec3 em = vColor * 1.35;
                    float distEm = length(uCameraPos - vWorldPos);
                    float fogEm = clamp(1.0 - exp(-pow(distEm * uFogDensity, 2.0)), 0.0, 0.8);
                    gl_FragColor = vec4(mix(em, uFogColor, fogEm), 1.0);
                    return;
                }

                vec3 n = normalize(vNormal);
                vec3 sun = normalize(uSunDir);
                vec3 viewDir = normalize(uCameraPos - vWorldPos);
                float ndotl = max(dot(n, sun), 0.0);
                float hemi = 0.5 + 0.5 * n.y;

                // Sample seamless 256x256 tile from 4x2 atlas
                float tile = clamp(floor(vMatType + 0.5), 0.0, 7.0);
                float col = mod(tile, 4.0);
                float row = floor(tile / 4.0);
                vec2 tileOrigin = vec2(col * 0.25, row * 0.5);
                vec2 localUv = vec2(0.004 + fract(vUv.x) * 0.242, 0.008 + fract(vUv.y) * 0.484);
                vec3 texSample = texture2D(uAtlasTex, tileOrigin + localUv).rgb;

                // Crisp architectural windows on upper urban brick walls (tile 1)
                vec3 baseAlbedo = vColor * texSample * 1.12;
                if (uDetailLevel > 0 && tile > 0.5 && tile < 1.5 && abs(n.y) < 0.5 && vWorldPos.y > 2.4) {
                    float wx = step(0.20, fract(vUv.x * 0.85)) * step(fract(vUv.x * 0.85), 0.78);
                    float wy = step(0.28, fract(vUv.y * 0.75)) * step(fract(vUv.y * 0.75), 0.78);
                    float winMask = wx * wy;
                    vec3 glass = vec3(0.16, 0.28, 0.40) + vec3(0.12, 0.14, 0.10) * step(0.55, fract(floor(vUv.x) * 0.37 + floor(vUv.y) * 0.61));
                    baseAlbedo = mix(baseAlbedo, glass, winMask * 0.82);
                }

                // Specular highlight for metallic, gunmetal, and ice surfaces
                float specPower = 18.0;
                float specStrength = 0.08;
                if (tile > 1.5 && tile < 3.5) { // Steel & gunmetal
                    specPower = 36.0;
                    specStrength = 0.42;
                } else if (tile > 5.5 && tile < 6.5) { // Snow / ice
                    specPower = 24.0;
                    specStrength = 0.24;
                }

                vec3 halfVec = normalize(sun + viewDir);
                float spec = pow(max(dot(n, halfVec), 0.0), specPower) * specStrength;

                // Subtle Fresnel rim light on guns & characters for crisp silhouettes
                float rim = pow(1.0 - max(dot(n, viewDir), 0.0), 3.0) * (tile > 2.5 && tile < 3.5 ? 0.22 : 0.06);

                vec3 lit = baseAlbedo * (uAmbientColor * (0.68 + 0.42 * hemi) + uSunColor * ndotl * 0.90)
                         + uSunColor * spec
                         + vec3(0.55, 0.72, 0.90) * rim;

                // Dynamic muzzle flash / combat light
                if (uDetailLevel > 0 && uMuzzleIntensity > 0.01) {
                    vec3 toLight = uMuzzlePos - vWorldPos;
                    float distSq = dot(toLight, toLight);
                    float atten = uMuzzleIntensity / (1.0 + distSq * 0.20);
                    lit += vec3(1.0, 0.64, 0.24) * atten * (0.5 + specStrength);
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
        uAtlasTex = GLES20.glGetUniformLocation(programId, "uAtlasTex")

        aPos = GLES20.glGetAttribLocation(programId, "aPos")
        aNormal = GLES20.glGetAttribLocation(programId, "aNormal")
        aUv = GLES20.glGetAttribLocation(programId, "aUv")
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
