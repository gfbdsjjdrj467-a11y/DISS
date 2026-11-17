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
    val uTime: Int
    val uSkinMode: Int
    val uSkinPrimary: Int
    val uSkinSecondary: Int
    val uIsSky: Int

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
            uniform float uTime;
            uniform int uSkinMode;
            uniform vec3 uSkinPrimary;
            uniform vec3 uSkinSecondary;
            uniform int uIsSky;

            varying vec3 vWorldPos;
            varying vec3 vNormal;
            varying vec2 vUv;
            varying vec3 vColor;
            varying float vMatType;

            void main() {
                // Sky dome pass with animated clouds & warm sun halo corona
                if (uIsSky == 1) {
                    if (vMatType < -1.5) {
                        gl_FragColor = vec4(vColor * 1.4, 1.0);
                        return;
                    }
                    vec3 ray = normalize(vWorldPos - uCameraPos);
                    vec3 sun = normalize(uSunDir);
                    float sunGlow = pow(max(dot(ray, sun), 0.0), 18.0) * 0.55;
                    float cloudCoordX = ray.x / max(ray.y + 0.18, 0.12) + uTime * 0.035;
                    float cloudCoordZ = ray.z / max(ray.y + 0.18, 0.12) - uTime * 0.020;
                    float cloudBand = sin(cloudCoordX * 2.2 + sin(cloudCoordZ * 1.9)) * cos(cloudCoordZ * 2.4 - uTime * 0.04);
                    float cloudMask = smoothstep(0.22, 0.78, cloudBand * 0.5 + 0.5) * smoothstep(0.03, 0.45, ray.y);
                    vec3 skyCol = vColor + uSunColor * sunGlow;
                    skyCol = mix(skyCol, mix(vec3(0.96), uSunColor, 0.25), cloudMask * 0.28);
                    gl_FragColor = vec4(skyCol, 1.0);
                    return;
                }

                // Emissive surfaces (visors, optic lenses, glowing tritium sights, beacons)
                if (vMatType < -0.5) {
                    float pulse = 1.25 + 0.18 * sin(uTime * 4.5 + vWorldPos.x * 3.0);
                    vec3 em = vColor * pulse;
                    float distEm = length(uCameraPos - vWorldPos);
                    float fogEm = clamp(1.0 - exp(-pow(distEm * uFogDensity, 2.0)), 0.0, 0.72);
                    gl_FragColor = vec4(mix(em, uFogColor, fogEm), 1.0);
                    return;
                }

                vec3 n = normalize(vNormal);
                vec3 sun = normalize(uSunDir);
                vec3 viewDir = normalize(uCameraPos - vWorldPos);

                // Sample seamless 256x256 tile from 4x2 atlas
                float tile = clamp(floor(vMatType + 0.5), 0.0, 7.0);
                float col = mod(tile, 4.0);
                float row = floor(tile / 4.0);
                vec2 tileOrigin = vec2(col * 0.25, row * 0.5);
                vec2 localUv = vec2(0.004 + fract(vUv.x) * 0.242, 0.008 + fract(vUv.y) * 0.484);
                vec3 texSample = texture2D(uAtlasTex, tileOrigin + localUv).rgb;

                // Micro-normal relief perturbation for tactile surface sharpness
                if (uDetailLevel > 0) {
                    float bumpScale = (tile > 1.5 && tile < 3.5) ? 0.09 : 0.06;
                    float bx = sin(vUv.x * 28.0 + texSample.r * 9.0) * bumpScale;
                    float bz = cos(vUv.y * 28.0 + texSample.g * 9.0) * bumpScale;
                    n = normalize(n + vec3(bx, 0.0, bz));
                }

                vec3 baseAlbedo = vColor * texSample * 1.14;
                float specPower = 18.0;
                float specStrength = 0.08;
                vec3 extraEmissive = vec3(0.0);

                // Crisp architectural windows on upper urban brick walls (tile 1)
                if (uDetailLevel > 0 && tile > 0.5 && tile < 1.5 && abs(n.y) < 0.5 && vWorldPos.y > 2.2) {
                    float wx = step(0.18, fract(vUv.x * 0.85)) * step(fract(vUv.x * 0.85), 0.78);
                    float wy = step(0.26, fract(vUv.y * 0.75)) * step(fract(vUv.y * 0.75), 0.78);
                    float winMask = wx * wy;
                    float litRoom = step(0.58, fract(floor(vUv.x) * 0.37 + floor(vUv.y) * 0.61));
                    vec3 glass = mix(vec3(0.14, 0.26, 0.40), vec3(0.45, 0.40, 0.26), litRoom);
                    baseAlbedo = mix(baseAlbedo, glass, winMask * 0.86);
                    specStrength = mix(specStrength, 0.55, winMask);
                    specPower = mix(specPower, 44.0, winMask);
                }

                // Metallic & ice specular defaults
                if (tile > 1.5 && tile < 3.5) {
                    specPower = 38.0;
                    specStrength = 0.44;
                } else if (tile > 5.5 && tile < 6.5) {
                    specPower = 26.0;
                    specStrength = 0.28;
                }

                // Procedural PBR / Animated Weapon Skin Finish Shader (applied to metal/polymer receiver & barrel tiles 2 & 3)
                if (uSkinMode > 0 && tile > 1.5 && tile < 3.5) {
                    vec2 su = vUv * 2.4 + vWorldPos.xz * 0.8;
                    if (uSkinMode == 1) {
                        // Desert Snake tiger camo
                        float pat = sin(su.x * 6.5 + sin(su.y * 4.5) * 1.6);
                        float m = smoothstep(-0.2, 0.3, pat);
                        baseAlbedo = mix(uSkinPrimary, uSkinSecondary, m) * (0.72 + texSample * 0.45);
                        specStrength = 0.22;
                    } else if (uSkinMode == 2) {
                        // Cobalt Digital pixel camo
                        vec2 cell = floor(su * 4.5);
                        float h = fract(sin(dot(cell, vec2(12.9898, 78.233))) * 43758.5453);
                        vec3 cMix = mix(uSkinPrimary, uSkinSecondary, step(0.55, h));
                        if (h > 0.86) cMix = vec3(0.86, 0.94, 1.0);
                        baseAlbedo = cMix * (0.78 + texSample * 0.38);
                        specPower = 48.0;
                        specStrength = 0.58;
                    } else if (uSkinMode == 3) {
                        // Crimson Viper carbon + racing stripes
                        float stripe = smoothstep(0.68, 0.78, fract((su.x + su.y) * 1.35));
                        baseAlbedo = mix(uSkinPrimary * (0.85 + texSample * 0.35), uSkinSecondary, stripe);
                        specPower = 52.0;
                        specStrength = 0.62;
                    } else if (uSkinMode == 4) {
                        // Arctic SpecOps white ceramic + ice accents
                        float hexPat = step(0.64, fract(su.x * 2.1) * 0.5 + fract(su.y * 2.1) * 0.5);
                        baseAlbedo = mix(uSkinPrimary, uSkinSecondary, hexPat) * (0.86 + texSample * 0.26);
                        specPower = 46.0;
                        specStrength = 0.54;
                    } else if (uSkinMode == 5) {
                        // Neon Cyber animated pulse veins
                        float gridX = smoothstep(0.86, 0.98, fract(su.x * 2.5));
                        float gridY = smoothstep(0.86, 0.98, fract(su.y * 2.5));
                        float vein = max(gridX, gridY);
                        float wave = 0.65 + 0.35 * sin(uTime * 4.0 + su.x * 3.0);
                        baseAlbedo = uSkinPrimary * (0.80 + texSample * 0.35);
                        extraEmissive = uSkinSecondary * vein * wave * 1.15;
                        specPower = 56.0;
                        specStrength = 0.65;
                    } else if (uSkinMode == 6) {
                        // Gold Dynasty 24K mirror gold + dark filigree
                        float engrave = smoothstep(0.75, 0.88, sin(su.x * 11.0) * cos(su.y * 11.0));
                        baseAlbedo = mix(uSkinPrimary, uSkinSecondary, engrave * 0.45) * (0.82 + texSample * 0.34);
                        specPower = 64.0;
                        specStrength = 0.85;
                    } else if (uSkinMode == 7) {
                        // Legend Void obsidian + animated molten lava veins
                        float nFlow = sin(su.x * 4.5 + uTime * 1.4) * cos(su.y * 4.2 - uTime * 1.1);
                        float crack = smoothstep(0.58, 0.84, abs(nFlow));
                        baseAlbedo = uSkinPrimary * (0.76 + texSample * 0.38);
                        extraEmissive = uSkinSecondary * (1.0 - crack) * (0.95 + 0.35 * sin(uTime * 3.5));
                        specPower = 60.0;
                        specStrength = 0.78;
                    }
                }

                float ndotl = max(dot(n, sun), 0.0);
                float hemi = 0.5 + 0.5 * n.y;

                // Subtle ground contact ambient occlusion on vertical world walls
                float contactAo = 1.0;
                if (uSkinMode == 0 && abs(vNormal.y) < 0.4 && vWorldPos.y < 0.6) {
                    contactAo = mix(0.76, 1.0, clamp(vWorldPos.y / 0.6, 0.0, 1.0));
                }

                vec3 halfVec = normalize(sun + viewDir);
                float spec = pow(max(dot(n, halfVec), 0.0), specPower) * specStrength;

                // Fresnel rim highlight for gun silhouettes & armored bots
                float rimBoost = (tile > 1.5 && tile < 3.5) ? 0.26 : 0.07;
                float rim = pow(1.0 - max(dot(n, viewDir), 0.0), 3.0) * rimBoost;
                vec3 rimCol = (uSkinMode > 0) ? mix(vec3(0.65, 0.80, 1.0), uSkinSecondary, 0.45) : vec3(0.55, 0.72, 0.90);

                vec3 lit = baseAlbedo * contactAo * (uAmbientColor * (0.68 + 0.44 * hemi) + uSunColor * ndotl * 0.92)
                         + uSunColor * spec
                         + rimCol * rim
                         + extraEmissive;

                // Dynamic muzzle flash / combat light
                if (uDetailLevel > 0 && uMuzzleIntensity > 0.01) {
                    vec3 toLight = uMuzzlePos - vWorldPos;
                    float distSq = dot(toLight, toLight);
                    float atten = uMuzzleIntensity / (1.0 + distSq * 0.18);
                    lit += vec3(1.0, 0.64, 0.24) * atten * (0.55 + specStrength);
                }

                // Hit flash or team tint override
                if (uTintOverride.a > 0.01) {
                    lit = mix(lit, uTintOverride.rgb, uTintOverride.a);
                }

                // Gentle filmic tone curve for crisp highlights without blown-out whites
                lit = lit / (lit + vec3(0.78)) * 1.48;

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
        uTime = GLES20.glGetUniformLocation(programId, "uTime")
        uSkinMode = GLES20.glGetUniformLocation(programId, "uSkinMode")
        uSkinPrimary = GLES20.glGetUniformLocation(programId, "uSkinPrimary")
        uSkinSecondary = GLES20.glGetUniformLocation(programId, "uSkinSecondary")
        uIsSky = GLES20.glGetUniformLocation(programId, "uIsSky")

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
