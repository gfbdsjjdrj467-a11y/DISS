package com.example.game.ai

import com.example.game.engine.AABB
import com.example.game.engine.Vec3
import com.example.game.model.BotAiState
import com.example.game.model.Team
import com.example.game.model.WeaponCatalog
import com.example.game.model.WeaponId

class BotEntity(
    val id: Int,
    val callsign: String,
    var team: Team,
    var weaponId: WeaponId,
    var position: Vec3,
    var yawDeg: Float,
    var health: Int = 100,
    val maxHealth: Int = 100,
    var isTargetDummy: Boolean = false
) {
    var pitchDeg: Float = 0f
    var velocity: Vec3 = Vec3.ZERO
    var ammoInMag: Int = WeaponCatalog.byId(weaponId).magSize
    var reloadTimer: Float = 0f
    var fireCooldown: Float = 0f

    var aiState: BotAiState = BotAiState.PATROL
    var stateTimer: Float = 0f
    var reactionTimer: Float = 0f
    var strafeDir: Float = 1f
    var strafeTimer: Float = 1.2f

    var targetEntityId: Int? = null // -1 = human player, >= 0 = another bot id
    var lastKnownTargetPos: Vec3? = null
    var investigatePos: Vec3? = null
    var path: List<Vec3> = emptyList()
    var pathIndex: Int = 0
    var pathRecalcTimer: Float = 0f

    var respawnTimer: Float = 0f
    var walkCycle: Float = 0f
    var hitFlashTimer: Float = 0f
    var deathFallProgress: Float = 0f
    var recoilKick: Float = 0f
    var muzzleFlashTimer: Float = 0f

    var kills: Int = 0
    var deaths: Int = 0

    val isAlive: Boolean get() = aiState != BotAiState.DEAD && health > 0

    fun eyePos(): Vec3 = Vec3(position.x, position.y + 1.56f, position.z)
    fun torsoPos(): Vec3 = Vec3(position.x, position.y + 1.12f, position.z)
    fun headPos(): Vec3 = Vec3(position.x, position.y + 1.58f, position.z)

    fun bodyAABB(): AABB = AABB(
        position.x - 0.38f, position.y, position.z - 0.38f,
        position.x + 0.38f, position.y + 1.42f, position.z + 0.38f
    )

    fun headAABB(): AABB = AABB(
        position.x - 0.22f, position.y + 1.42f, position.z - 0.22f,
        position.x + 0.22f, position.y + 1.82f, position.z + 0.22f
    )

    fun resetForSpawn(pos: Vec3, yaw: Float, newMaxHp: Int = 100) {
        position = pos
        yawDeg = yaw
        pitchDeg = 0f
        health = newMaxHp
        ammoInMag = WeaponCatalog.byId(weaponId).magSize
        reloadTimer = 0f
        fireCooldown = 0.5f
        aiState = if (isTargetDummy) BotAiState.IDLE else BotAiState.PATROL
        stateTimer = 0f
        reactionTimer = 0f
        targetEntityId = null
        lastKnownTargetPos = null
        investigatePos = null
        path = emptyList()
        pathIndex = 0
        respawnTimer = 0f
        hitFlashTimer = 0f
        deathFallProgress = 0f
        muzzleFlashTimer = 0f
    }
}
