package com.example.game.maps

import com.example.game.engine.AABB
import com.example.game.engine.Vec3
import com.example.game.model.MapId
import com.example.game.model.PickupType
import com.example.game.model.SurfaceType

/**
 * Material pattern codes interpreted by the GLSL fragment shader for procedural surface detail.
 * 0 = smooth/ground, 1 = concrete/stone blocks, 2 = metal ribbed/container,
 * 3 = brick/urban facade, 4 = wood planks, 5 = sand/sandbags, 6 = snow/ice
 */
data class MapBox(
    val aabb: AABB,
    val r: Float,
    val g: Float,
    val b: Float,
    val matType: Int = 1,
    val collides: Boolean = true,
    val showOnMinimap: Boolean = true
)

data class MapCylinder(
    val cx: Float,
    val baseY: Float,
    val cz: Float,
    val radius: Float,
    val height: Float,
    val r: Float,
    val g: Float,
    val b: Float,
    val matType: Int = 2,
    val segments: Int = 12
) {
    fun toColliderAABB(): AABB = AABB(
        cx - radius, baseY, cz - radius,
        cx + radius, baseY + height, cz + radius
    )
}

data class MapCone(
    val cx: Float,
    val baseY: Float,
    val cz: Float,
    val radius: Float,
    val height: Float,
    val r: Float,
    val g: Float,
    val b: Float,
    val matType: Int = 6,
    val segments: Int = 12
)

class DoorState(
    val id: String,
    val closedBox: AABB,
    val slideDirX: Float,
    val slideDirZ: Float,
    val r: Float,
    val g: Float,
    val b: Float,
    var openAmount: Float = 0f, // 0 = closed, 1 = fully open
    var isOpen: Boolean = false,
    var autoCloseTimer: Float = 0f
) {
    fun currentAABB(): AABB {
        val shiftX = slideDirX * (closedBox.sizeX * 0.92f) * openAmount
        val shiftZ = slideDirZ * (closedBox.sizeZ * 0.92f) * openAmount
        return AABB(
            closedBox.minX + shiftX, closedBox.minY, closedBox.minZ + shiftZ,
            closedBox.maxX + shiftX, closedBox.maxY, closedBox.maxZ + shiftZ
        )
    }

    fun center(): Vec3 = Vec3(closedBox.centerX, closedBox.centerY, closedBox.centerZ)
}

class PickupItemState(
    val id: Int,
    val type: PickupType,
    val pos: Vec3,
    var active: Boolean = true,
    var respawnTimer: Float = 0f
)

data class SpawnPoint(
    val pos: Vec3,
    val yawDeg: Float
)

data class MapDefinition(
    val id: MapId,
    val halfExtent: Float,
    val floorSurface: SurfaceType,
    val floorColor: Triple<Float, Float, Float>,
    val floorMatType: Int,
    val skyTopColor: Triple<Float, Float, Float>,
    val skyHorizonColor: Triple<Float, Float, Float>,
    val fogColor: Triple<Float, Float, Float>,
    val fogDensity: Float,
    val sunDirection: Vec3,
    val sunColor: Triple<Float, Float, Float>,
    val ambientColor: Triple<Float, Float, Float>,
    val weatherParticleType: Int, // 0 = none, 1 = desert dust, 2 = snow flakes, 3 = industrial embers
    val boxes: List<MapBox>,
    val cylinders: List<MapCylinder>,
    val cones: List<MapCone> = emptyList(),
    val doorTemplates: List<DoorState>,
    val pickupSpawns: List<PickupItemState>,
    val coverNodes: List<Vec3>,
    val waypoints: List<Vec3>,
    val alphaSpawns: List<SpawnPoint>,
    val bravoSpawns: List<SpawnPoint>,
    val ffaSpawns: List<SpawnPoint>
)
