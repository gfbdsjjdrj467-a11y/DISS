package com.example.game.engine

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView
import com.example.game.model.WeaponId
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class WeaponPreviewRenderer : GLSurfaceView.Renderer {
    private lateinit var shader: TacticalWorldShader
    private val meshes = HashMap<WeaponId, GpuMesh>()

    @Volatile var activeWeapon: WeaponId = WeaponId.AR_M4A1
    @Volatile var userYawOffset: Float = 0f
    @Volatile var userPitchOffset: Float = 12f

    private val proj = FloatArray(16)
    private val view = FloatArray(16)
    private val model = FloatArray(16)
    private val vp = FloatArray(16)
    private val mvp = FloatArray(16)
    private var autoYaw = 0f
    private var lastNs = System.nanoTime()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        shader = TacticalWorldShader()
        meshes.clear()
        for (id in WeaponId.entries) {
            meshes[id] = TacticalMeshes.buildWeaponMesh(id)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        val w = width.coerceAtLeast(1)
        val h = height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, w, h)
        Matrix.perspectiveM(proj, 0, 42f, w.toFloat() / h.toFloat(), 0.05f, 20f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = ((now - lastNs) / 1_000_000_000.0).toFloat().coerceIn(0.001f, 0.05f)
        lastNs = now
        autoYaw = (autoYaw + dt * 26f) % 360f

        GLES20.glClearColor(0.07f, 0.10f, 0.14f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        Matrix.setLookAtM(view, 0, 0f, 0.14f, 1.05f, 0f, 0.02f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(vp, 0, proj, 0, view, 0)

        GLES20.glUseProgram(shader.programId)
        GLES20.glUniform3f(shader.uSunDir, 0.5f, 0.8f, 0.6f)
        GLES20.glUniform3f(shader.uSunColor, 1.0f, 0.96f, 0.90f)
        GLES20.glUniform3f(shader.uAmbientColor, 0.45f, 0.50f, 0.58f)
        GLES20.glUniform3f(shader.uFogColor, 0.07f, 0.10f, 0.14f)
        GLES20.glUniform1f(shader.uFogDensity, 0f)
        GLES20.glUniform3f(shader.uCameraPos, 0f, 0.14f, 1.05f)
        GLES20.glUniform1f(shader.uMuzzleIntensity, 0f)
        GLES20.glUniform1i(shader.uDetailLevel, 2)
        GLES20.glUniform4f(shader.uTintOverride, 0f, 0f, 0f, 0f)

        Matrix.setIdentityM(model, 0)
        Matrix.rotateM(model, 0, userPitchOffset.coerceIn(-45f, 45f), 1f, 0f, 0f)
        Matrix.rotateM(model, 0, 65f + autoYaw + userYawOffset, 0f, 1f, 0f)
        val scale = if (activeWeapon == WeaponId.SNIPER_AWP) 0.72f else if (activeWeapon == WeaponId.PISTOL_P9) 1.55f else 1.05f
        Matrix.scaleM(model, 0, scale, scale, scale)

        Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)
        GLES20.glUniformMatrix4fv(shader.uMvpMatrix, 1, false, mvp, 0)
        GLES20.glUniformMatrix4fv(shader.uModelMatrix, 1, false, model, 0)

        meshes[activeWeapon]?.draw(shader)
    }
}

@Composable
fun WeaponPreviewGLView(
    weaponId: WeaponId,
    modifier: Modifier = Modifier
) {
    val renderer = remember { WeaponPreviewRenderer() }
    var dragYaw by remember { mutableFloatStateOf(0f) }
    var dragPitch by remember { mutableFloatStateOf(12f) }

    LaunchedEffect(weaponId) {
        renderer.activeWeapon = weaponId
    }
    LaunchedEffect(dragYaw, dragPitch) {
        renderer.userYawOffset = dragYaw
        renderer.userPitchOffset = dragPitch
    }

    var glViewRef: GLSurfaceView? = null
    DisposableEffect(Unit) {
        onDispose { glViewRef?.onPause() }
    }

    AndroidView(
        factory = { ctx: Context ->
            GLSurfaceView(ctx).apply {
                setEGLContextClientVersion(2)
                setRenderer(renderer)
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                glViewRef = this
            }
        },
        modifier = modifier.pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                dragYaw += dragAmount.x * 0.45f
                dragPitch = (dragPitch + dragAmount.y * 0.35f).coerceIn(-40f, 40f)
            }
        }
    )
}
