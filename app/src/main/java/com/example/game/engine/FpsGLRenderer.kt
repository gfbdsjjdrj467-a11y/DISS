package com.example.game.engine

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.game.ai.BotEntity
import com.example.game.model.HudState
import com.example.game.model.PickupType
import com.example.game.model.Team
import com.example.game.model.WeaponId
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

class FpsGLRenderer(
    private val sim: GameSimulation,
    private val onHudUpdate: (HudState) -> Unit
) : GLSurfaceView.Renderer {

    private lateinit var worldShader: TacticalWorldShader
    private lateinit var particleShader: UnlitParticleShader
    private val textureAtlas = TacticalTextureAtlas()

    private var mapMesh: GpuMesh? = null
    private var skyMesh: GpuMesh? = null
    private var unitBoxMesh: GpuMesh? = null
    private var playerArmsMesh: GpuMesh? = null

    private lateinit var alphaTorso: GpuMesh
    private lateinit var bravoTorso: GpuMesh
    private lateinit var alphaHead: GpuMesh
    private lateinit var bravoHead: GpuMesh
    private lateinit var alphaArm: GpuMesh
    private lateinit var bravoArm: GpuMesh
    private lateinit var botLeg: GpuMesh

    private val weaponMeshes = HashMap<WeaponId, GpuMesh>()
    private val pickupMeshes = HashMap<PickupType, GpuMesh>()

    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private var surfaceWidth = 1280
    private var surfaceHeight = 720
    private var lastTimeNs = System.nanoTime()
    private var hudAccumSec = 0f
    private var fpsAccumSec = 0f
    private var fpsFrames = 0
    private var currentFps = 60

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)

        worldShader = TacticalWorldShader()
        particleShader = UnlitParticleShader()
        textureAtlas.createAndUpload()

        unitBoxMesh = TacticalMeshes.buildUnitBoxMesh()
        playerArmsMesh = TacticalMeshes.buildPlayerViewArms()

        alphaTorso = TacticalMeshes.buildBotTorso(isAlpha = true)
        bravoTorso = TacticalMeshes.buildBotTorso(isAlpha = false)
        alphaHead = TacticalMeshes.buildBotHead(isAlpha = true)
        bravoHead = TacticalMeshes.buildBotHead(isAlpha = false)
        alphaArm = TacticalMeshes.buildBotArm(isAlpha = true)
        bravoArm = TacticalMeshes.buildBotArm(isAlpha = false)
        botLeg = TacticalMeshes.buildBotLeg()

        weaponMeshes.clear()
        for (wId in WeaponId.entries) {
            weaponMeshes[wId] = TacticalMeshes.buildWeaponMesh(wId)
        }
        pickupMeshes.clear()
        for (pType in PickupType.entries) {
            pickupMeshes[pType] = TacticalMeshes.buildPickupMesh(pType)
        }
        sim.mapGeometryDirty = true
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        surfaceWidth = width.coerceAtLeast(1)
        surfaceHeight = height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = ((now - lastTimeNs) / 1_000_000_000.0).toFloat().coerceIn(0.001f, 0.05f)
        lastTimeNs = now

        val limit = sim.settings.fpsLimit
        if (limit == 30 && dt < 0.028f) {
            try { Thread.sleep(((0.031f - dt) * 1000f).toLong().coerceIn(1L, 25L)) } catch (_: InterruptedException) {}
        }

        sim.step(dt)

        if (sim.mapGeometryDirty) {
            mapMesh?.delete()
            skyMesh?.delete()
            mapMesh = TacticalMeshes.buildMapMesh(sim.currentMap)
            skyMesh = TacticalMeshes.buildSkyDome(sim.currentMap)
            sim.mapGeometryDirty = false
        }

        val map = sim.currentMap
        val (sr, sg, sb) = map.skyHorizonColor
        GLES20.glClearColor(sr, sg, sb, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        val aspect = surfaceWidth.toFloat() / surfaceHeight.toFloat()
        val fov = if (sim.isShowcaseMode) 62f else sim.currentFov
        val farPlane = sim.settings.graphicsQuality.drawDistance
        Matrix.perspectiveM(projMatrix, 0, fov, aspect, 0.05f, farPlane + 65f)

        val eye = sim.playerEyePos()
        val shake = sim.cameraShakeAmount
        val shakeY = if (shake > 0.01f) sin(now * 0.00008f) * shake * 0.03f else 0f
        val fwd = Math3D.forwardFromYawPitch(sim.playerYaw, sim.playerPitch)
        Matrix.setLookAtM(
            viewMatrix, 0,
            eye.x, eye.y + shakeY, eye.z,
            eye.x + fwd.x, eye.y + fwd.y + shakeY, eye.z + fwd.z,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        GLES20.glUseProgram(worldShader.programId)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureAtlas.textureId)
        GLES20.glUniform1i(worldShader.uAtlasTex, 0)

        GLES20.glUniform3f(worldShader.uSunDir, map.sunDirection.x, map.sunDirection.y, map.sunDirection.z)
        GLES20.glUniform3f(worldShader.uSunColor, map.sunColor.first, map.sunColor.second, map.sunColor.third)
        GLES20.glUniform3f(worldShader.uAmbientColor, map.ambientColor.first, map.ambientColor.second, map.ambientColor.third)
        GLES20.glUniform3f(worldShader.uFogColor, map.fogColor.first, map.fogColor.second, map.fogColor.third)
        GLES20.glUniform1f(worldShader.uFogDensity, map.fogDensity)
        GLES20.glUniform3f(worldShader.uCameraPos, eye.x, eye.y, eye.z)

        val dynLight = if (sim.settings.graphicsQuality.dynamicLightsEnabled) sim.muzzleFlashIntensity else 0f
        GLES20.glUniform3f(worldShader.uMuzzlePos, sim.muzzleFlashPos.x, sim.muzzleFlashPos.y, sim.muzzleFlashPos.z)
        GLES20.glUniform1f(worldShader.uMuzzleIntensity, dynLight)
        GLES20.glUniform1i(worldShader.uDetailLevel, sim.settings.graphicsQuality.surfaceDetailLevel)
        GLES20.glUniform4f(worldShader.uTintOverride, 0f, 0f, 0f, 0f)

        // Draw Gradient Sky Dome + Sun Disc behind world
        GLES20.glDepthMask(false)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glUniform1f(worldShader.uFogDensity, 0f)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, eye.x, -4f, eye.z)
        drawMeshWithModel(skyMesh)
        GLES20.glUniform1f(worldShader.uFogDensity, map.fogDensity)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glDepthMask(true)

        // Draw Textured Static Map
        Matrix.setIdentityM(modelMatrix, 0)
        drawMeshWithModel(mapMesh)

        // Draw Interactive Blast Doors
        for (d in sim.doors) {
            val box = d.currentAABB()
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, box.centerX, box.centerY, box.centerZ)
            Matrix.scaleM(modelMatrix, 0, box.sizeX, box.sizeY, box.sizeZ)
            GLES20.glUniform4f(worldShader.uTintOverride, d.r, d.g, d.b, 0.72f)
            drawMeshWithModel(unitBoxMesh)
        }

        // Draw Detailed Rotating 3D Pickups
        GLES20.glUniform4f(worldShader.uTintOverride, 0f, 0f, 0f, 0f)
        val timeSec = (now / 1_000_000_000.0).toFloat()
        for (p in sim.pickups) {
            if (!p.active) continue
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, p.pos.x, p.pos.y + sin(timeSec * 3.2f + p.id) * 0.12f, p.pos.z)
            Matrix.rotateM(modelMatrix, 0, timeSec * 72f + p.id * 45f, 0f, 1f, 0f)
            drawMeshWithModel(pickupMeshes[p.type])
        }

        // Draw Articulated Bots (LOD)
        for (b in sim.bots) {
            val dist = b.position.distanceTo(eye)
            if (dist > farPlane) continue
            drawBot(b, dist)
        }

        // Draw Particles & Bullet Tracers
        val camRight = Vec3(viewMatrix[0], viewMatrix[4], viewMatrix[8]).normalized()
        val camUp = Vec3(viewMatrix[1], viewMatrix[5], viewMatrix[9]).normalized()
        sim.particlePool.render(particleShader, vpMatrix, camRight, camUp)

        // Draw First-Person Tactical Arms + Detailed Weapon Viewmodel
        if (!sim.isShowcaseMode && sim.isActiveWeaponVisible()) {
            drawFirstPersonWeapon(aspect)
        }

        fpsFrames++
        fpsAccumSec += dt
        if (fpsAccumSec >= 0.5f) {
            currentFps = (fpsFrames / fpsAccumSec).toInt().coerceIn(1, 120)
            fpsFrames = 0
            fpsAccumSec = 0f
        }
        hudAccumSec += dt
        if (hudAccumSec >= 0.033f) {
            hudAccumSec = 0f
            onHudUpdate(sim.snapshotHud(currentFps))
        }
    }

    private fun GameSimulation.isActiveWeaponVisible(): Boolean {
        return !isPlayerDeadForRender() && !(isAdsActiveForRender() && activeWeaponId() == WeaponId.SNIPER_AWP)
    }

    private fun drawBot(b: BotEntity, dist: Float) {
        val isAlpha = b.team == Team.ALPHA
        val flash = (b.hitFlashTimer / 0.14f).coerceIn(0f, 1f)
        if (flash > 0.01f) {
            GLES20.glUniform4f(worldShader.uTintOverride, 1f, 0.25f, 0.2f, flash * 0.75f)
        } else {
            GLES20.glUniform4f(worldShader.uTintOverride, 0f, 0f, 0f, 0f)
        }

        val fallAngle = b.deathFallProgress * 88f
        val swing = if (b.isAlive) sin(b.walkCycle) * 28f else 0f

        if (sim.settings.shadowQuality > 0 && dist < 45f && b.isAlive) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, b.position.x, 0.02f, b.position.z)
            Matrix.scaleM(modelMatrix, 0, 0.78f, 0.02f, 0.78f)
            GLES20.glUniform4f(worldShader.uTintOverride, 0.04f, 0.05f, 0.06f, 0.85f)
            drawMeshWithModel(unitBoxMesh)
            if (flash > 0.01f) {
                GLES20.glUniform4f(worldShader.uTintOverride, 1f, 0.25f, 0.2f, flash * 0.75f)
            } else {
                GLES20.glUniform4f(worldShader.uTintOverride, 0f, 0f, 0f, 0f)
            }
        }

        val baseMatrix = FloatArray(16)
        Matrix.setIdentityM(baseMatrix, 0)
        Matrix.translateM(baseMatrix, 0, b.position.x, b.position.y, b.position.z)
        Matrix.rotateM(baseMatrix, 0, -b.yawDeg, 0f, 1f, 0f)
        if (fallAngle > 0.1f) {
            Matrix.rotateM(baseMatrix, 0, fallAngle, 1f, 0f, 0f)
        }

        // Legs
        for (side in intArrayOf(-1, 1)) {
            System.arraycopy(baseMatrix, 0, modelMatrix, 0, 16)
            Matrix.translateM(modelMatrix, 0, side * 0.13f, 0.72f, 0f)
            Matrix.rotateM(modelMatrix, 0, swing * side, 1f, 0f, 0f)
            drawMeshWithModel(botLeg)
        }

        // Torso
        System.arraycopy(baseMatrix, 0, modelMatrix, 0, 16)
        Matrix.translateM(modelMatrix, 0, 0f, 0.72f, 0f)
        drawMeshWithModel(if (isAlpha) alphaTorso else bravoTorso)

        // Head
        System.arraycopy(baseMatrix, 0, modelMatrix, 0, 16)
        Matrix.translateM(modelMatrix, 0, 0f, 1.32f, 0f)
        Matrix.rotateM(modelMatrix, 0, b.pitchDeg * 0.4f, 1f, 0f, 0f)
        drawMeshWithModel(if (isAlpha) alphaHead else bravoHead)

        // Arms & Held Weapon
        val armMesh = if (isAlpha) alphaArm else bravoArm
        System.arraycopy(baseMatrix, 0, modelMatrix, 0, 16)
        Matrix.translateM(modelMatrix, 0, -0.28f, 1.24f, 0f)
        Matrix.rotateM(modelMatrix, 0, -swing * 0.55f, 1f, 0f, 0f)
        drawMeshWithModel(armMesh)

        System.arraycopy(baseMatrix, 0, modelMatrix, 0, 16)
        Matrix.translateM(modelMatrix, 0, 0.28f, 1.24f, 0f)
        Matrix.rotateM(modelMatrix, 0, 62f + b.recoilKick * 8f, 1f, 0f, 0f)
        drawMeshWithModel(armMesh)

        System.arraycopy(baseMatrix, 0, modelMatrix, 0, 16)
        Matrix.translateM(modelMatrix, 0, 0.24f, 1.15f, -0.35f + b.recoilKick * 0.05f)
        Matrix.rotateM(modelMatrix, 0, b.pitchDeg * 0.6f, 1f, 0f, 0f)
        Matrix.scaleM(modelMatrix, 0, 0.85f, 0.85f, 0.85f)
        drawMeshWithModel(weaponMeshes[b.weaponId])

        GLES20.glUniform4f(worldShader.uTintOverride, 0f, 0f, 0f, 0f)
    }

    private fun drawFirstPersonWeapon(aspect: Float) {
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT)
        val viewProjWeapon = FloatArray(16)
        Matrix.perspectiveM(viewProjWeapon, 0, 56f, aspect, 0.03f, 5f)

        val isAds = sim.isAdsActiveForRender()
        val isReloading = sim.isReloadingForRender()
        val reloadProgress = sim.getReloadProgressForRender()
        val isSprinting = sim.isSprintingForRender()
        val adsBlend = if (isAds) 1f else 0f
        val reloadDrop = if (isReloading) sin(reloadProgress * Math.PI.toFloat()) * 0.22f else 0f
        val bobPhase = sim.getWeaponBobPhase()
        val bobAmp = if (isAds) 0.0035f else 0.014f
        val bobX = cos(bobPhase) * bobAmp
        val bobY = sin(bobPhase * 2f) * bobAmp
        val recoilZ = sim.weaponRecoilOffset * 0.065f
        val recoilPitch = sim.weaponRecoilOffset * 7.5f

        val offsetX = Math3D.lerp(0.22f, 0.0f, adsBlend) + bobX
        val offsetY = Math3D.lerp(-0.20f, -0.140f, adsBlend) - reloadDrop + bobY
        val offsetZ = Math3D.lerp(-0.46f, -0.35f, adsBlend) + recoilZ

        GLES20.glUseProgram(worldShader.programId)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureAtlas.textureId)
        GLES20.glUniform1i(worldShader.uAtlasTex, 0)
        GLES20.glUniform3f(worldShader.uCameraPos, 0f, 0f, 0f)
        GLES20.glUniform1f(worldShader.uFogDensity, 0f)
        GLES20.glUniform4f(worldShader.uTintOverride, 0f, 0f, 0f, 0f)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, offsetX, offsetY, offsetZ)
        Matrix.rotateM(modelMatrix, 0, recoilPitch - reloadDrop * 85f, 1f, 0f, 0f)
        if (isSprinting) {
            Matrix.rotateM(modelMatrix, 0, -24f, 0f, 1f, 0f)
            Matrix.rotateM(modelMatrix, 0, -14f, 1f, 0f, 0f)
        }

        Matrix.multiplyMM(mvpMatrix, 0, viewProjWeapon, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(worldShader.uMvpMatrix, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(worldShader.uModelMatrix, 1, false, modelMatrix, 0)

        weaponMeshes[sim.activeWeaponId()]?.draw(worldShader)
        playerArmsMesh?.draw(worldShader)
    }

    private fun drawMeshWithModel(mesh: GpuMesh?) {
        if (mesh == null) return
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(worldShader.uMvpMatrix, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(worldShader.uModelMatrix, 1, false, modelMatrix, 0)
        mesh.draw(worldShader)
    }
}

class FpsGLView(
    context: Context,
    sim: GameSimulation,
    onHudUpdate: (HudState) -> Unit
) : GLSurfaceView(context) {
    init {
        setEGLContextClientVersion(2)
        preserveEGLContextOnPause = true
        setRenderer(FpsGLRenderer(sim, onHudUpdate))
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}
