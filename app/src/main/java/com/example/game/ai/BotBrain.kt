package com.example.game.ai

import com.example.game.engine.AABB
import com.example.game.engine.Math3D
import com.example.game.engine.Vec3
import com.example.game.maps.CollisionWorld
import com.example.game.maps.MapDefinition
import com.example.game.maps.NavGrid
import com.example.game.model.BotAiState
import com.example.game.model.BotDifficulty
import com.example.game.model.GameModeType
import com.example.game.model.Team
import com.example.game.model.WeaponCatalog
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class BotShotEvent(
    val shooter: BotEntity,
    val origin: Vec3,
    val dir: Vec3,
    val targetId: Int?,
    val isHitOnTarget: Boolean,
    val damage: Int
)

class BotBrain(
    private val map: MapDefinition,
    private val navGrid: NavGrid
) {
    private val rng = Random(System.currentTimeMillis())

    // Shared team callout for coordinated squad tactics
    private var alphaCalloutPos: Vec3? = null
    private var alphaCalloutTimer: Float = 0f
    private var bravoCalloutPos: Vec3? = null
    private var bravoCalloutTimer: Float = 0f

    fun updateTeamTimers(dt: Float) {
        if (alphaCalloutTimer > 0f) {
            alphaCalloutTimer -= dt
            if (alphaCalloutTimer <= 0f) alphaCalloutPos = null
        }
        if (bravoCalloutTimer > 0f) {
            bravoCalloutTimer -= dt
            if (bravoCalloutTimer <= 0f) bravoCalloutPos = null
        }
    }

    fun notifySoundEvent(sourcePos: Vec3, radius: Float, sourceTeam: Team, bots: List<BotEntity>) {
        for (bot in bots) {
            if (!bot.isAlive || bot.isTargetDummy) continue
            if (sourceTeam != Team.NONE && bot.team == sourceTeam) continue
            val dist = bot.position.flatDistanceTo(sourcePos)
            if (dist <= radius && (bot.aiState == BotAiState.IDLE || bot.aiState == BotAiState.PATROL || bot.aiState == BotAiState.SEARCH)) {
                bot.investigatePos = sourcePos
                bot.aiState = BotAiState.INVESTIGATE
                bot.stateTimer = 5.0f
                bot.pathRecalcTimer = 0f
            }
        }
    }

    fun updateBot(
        bot: BotEntity,
        dt: Float,
        difficulty: BotDifficulty,
        mode: GameModeType,
        playerPos: Vec3,
        playerAlive: Boolean,
        playerTeam: Team,
        allBots: List<BotEntity>,
        colliders: List<AABB>,
        shotsOut: MutableList<BotShotEvent>
    ) {
        if (bot.hitFlashTimer > 0f) bot.hitFlashTimer = (bot.hitFlashTimer - dt).coerceAtLeast(0f)
        if (bot.muzzleFlashTimer > 0f) bot.muzzleFlashTimer = (bot.muzzleFlashTimer - dt).coerceAtLeast(0f)
        if (bot.recoilKick > 0f) bot.recoilKick = (bot.recoilKick - dt * 5f).coerceAtLeast(0f)
        if (bot.fireCooldown > 0f) bot.fireCooldown = (bot.fireCooldown - dt).coerceAtLeast(0f)

        if (!bot.isAlive) {
            bot.aiState = BotAiState.DEAD
            bot.deathFallProgress = (bot.deathFallProgress + dt * 3.2f).coerceAtMost(1f)
            return
        }

        if (bot.isTargetDummy) {
            // Practice targets gently sway or patrol horizontally without attacking
            return
        }

        val weapon = WeaponCatalog.byId(bot.weaponId)

        // Handle Reloading state
        if (bot.aiState == BotAiState.RELOAD) {
            bot.reloadTimer -= dt
            if (bot.reloadTimer <= 0f) {
                bot.ammoInMag = weapon.magSize
                bot.aiState = if (bot.lastKnownTargetPos != null) BotAiState.CHASE else BotAiState.PATROL
            }
            return
        }

        // Low ammo triggers Reload or Retreat+Reload
        if (bot.ammoInMag <= 0) {
            bot.aiState = BotAiState.RELOAD
            bot.reloadTimer = weapon.reloadTimeSec
            return
        }

        // 1. Visual Target Acquisition (true line-of-sight & FOV check)
        val (visibleTargetId, visibleTargetPos, visibleDist) = findVisibleEnemy(
            bot, mode, playerPos, playerAlive, playerTeam, allBots, colliders
        )

        if (visibleTargetId != null && visibleTargetPos != null) {
            if (bot.targetEntityId != visibleTargetId) {
                bot.reactionTimer = difficulty.reactionDelaySec
            }
            bot.targetEntityId = visibleTargetId
            bot.lastKnownTargetPos = visibleTargetPos

            // Share tactical callout with squadmates in team modes & Expert/Hard
            if (mode == GameModeType.TEAM_DEATHMATCH || mode == GameModeType.ELIMINATION || mode == GameModeType.RANKED_2V2) {
                if (bot.team == Team.ALPHA) {
                    alphaCalloutPos = visibleTargetPos
                    alphaCalloutTimer = 4.5f
                } else if (bot.team == Team.BRAVO) {
                    bravoCalloutPos = visibleTargetPos
                    bravoCalloutTimer = 4.5f
                }
            }

            // Decide tactical combat state based on HP, distance, difficulty
            val hpRatio = bot.health.toFloat() / bot.maxHealth.toFloat()
            if (hpRatio < difficulty.coverThresholdHp && bot.aiState != BotAiState.RETREAT && bot.aiState != BotAiState.TAKE_COVER) {
                val cover = findBestCoverNode(bot.position, visibleTargetPos, colliders)
                if (cover != null) {
                    bot.investigatePos = cover
                    bot.aiState = BotAiState.TAKE_COVER
                    bot.stateTimer = 3.5f
                    bot.pathRecalcTimer = 0f
                } else {
                    bot.aiState = BotAiState.RETREAT
                    bot.stateTimer = 2.5f
                }
            } else if (bot.aiState != BotAiState.TAKE_COVER && bot.aiState != BotAiState.RETREAT) {
                bot.aiState = if (visibleDist <= weapon.range * 0.72f) BotAiState.ATTACK else BotAiState.CHASE
            }
        } else {
            // Target lost from sight!
            if (bot.aiState == BotAiState.ATTACK) {
                bot.aiState = BotAiState.SEARCH
                bot.stateTimer = 4.5f
                bot.pathRecalcTimer = 0f
            } else if (bot.aiState == BotAiState.PATROL && difficulty == BotDifficulty.EXPERT) {
                // Squad callout response
                val callout = if (bot.team == Team.ALPHA) alphaCalloutPos else if (bot.team == Team.BRAVO) bravoCalloutPos else null
                if (callout != null && bot.position.flatDistanceTo(callout) > 5f) {
                    bot.investigatePos = callout
                    bot.aiState = BotAiState.INVESTIGATE
                    bot.stateTimer = 4.0f
                    bot.pathRecalcTimer = 0f
                }
            }
        }

        // 2. Execute FSM State Behavior
        bot.stateTimer -= dt
        bot.pathRecalcTimer -= dt
        if (bot.reactionTimer > 0f) bot.reactionTimer -= dt

        val baseSpeed = 3.85f * difficulty.moveSpeedMult * weapon.moveSpeedMult

        when (bot.aiState) {
            BotAiState.IDLE -> {
                if (bot.stateTimer <= 0f) {
                    bot.aiState = BotAiState.PATROL
                    pickNewPatrolTarget(bot)
                }
            }

            BotAiState.PATROL -> {
                val dest = bot.investigatePos ?: pickNewPatrolTarget(bot)
                followPathToward(bot, dest, baseSpeed * 0.80f, dt, colliders)
                if (bot.position.flatDistanceTo(dest) < 1.8f || bot.stateTimer <= -6f) {
                    bot.aiState = BotAiState.IDLE
                    bot.stateTimer = rng.nextFloat() * 1.4f + 0.5f
                    bot.investigatePos = null
                }
            }

            BotAiState.INVESTIGATE -> {
                val dest = bot.investigatePos
                if (dest == null || bot.stateTimer <= 0f) {
                    bot.aiState = BotAiState.PATROL
                    bot.investigatePos = null
                } else {
                    followPathToward(bot, dest, baseSpeed * 0.92f, dt, colliders)
                    if (bot.position.flatDistanceTo(dest) < 2.2f) {
                        bot.aiState = BotAiState.SEARCH
                        bot.stateTimer = 3.0f
                    }
                }
            }

            BotAiState.CHASE -> {
                val targetPos = visibleTargetPos ?: bot.lastKnownTargetPos
                if (targetPos == null) {
                    bot.aiState = BotAiState.PATROL
                } else {
                    followPathToward(bot, targetPos, baseSpeed * 1.08f, dt, colliders)
                    faceToward(bot, targetPos, dt * 8f)
                    if (visibleTargetPos != null && bot.reactionTimer <= 0f && bot.fireCooldown <= 0f && rng.nextFloat() < 0.35f) {
                        attemptFire(bot, visibleTargetId, visibleTargetPos, difficulty, shotsOut)
                    }
                }
            }

            BotAiState.ATTACK -> {
                val targetPos = visibleTargetPos
                if (targetPos == null) {
                    bot.aiState = BotAiState.SEARCH
                    bot.stateTimer = 4.0f
                } else {
                    faceToward(bot, targetPos, dt * 11f)
                    // Tactical strafing in combat
                    bot.strafeTimer -= dt
                    if (bot.strafeTimer <= 0f) {
                        bot.strafeDir = if (rng.nextBoolean()) 1f else -1f
                        bot.strafeTimer = 0.8f + rng.nextFloat() * 1.2f
                    }
                    val yawRad = bot.yawDeg * Math3D.DEG_TO_RAD
                    val rightX = cos(yawRad)
                    val rightZ = sin(yawRad)
                    val fwdX = sin(yawRad)
                    val fwdZ = -cos(yawRad)
                    val desiredRange = if (weapon.pellets > 1) 7f else 14f
                    val radialMove = when {
                        visibleDist > desiredRange + 3f -> 0.55f
                        visibleDist < desiredRange - 4f -> -0.55f
                        else -> 0f
                    }
                    val strafeScale = if (difficulty == BotDifficulty.EASY) 0.3f else 0.75f
                    val moveX = rightX * bot.strafeDir * strafeScale + fwdX * radialMove
                    val moveZ = rightZ * bot.strafeDir * strafeScale + fwdZ * radialMove
                    moveWithCollision(bot, moveX, moveZ, baseSpeed * 0.72f, dt, colliders)

                    if (bot.reactionTimer <= 0f && bot.fireCooldown <= 0f) {
                        attemptFire(bot, visibleTargetId, targetPos, difficulty, shotsOut)
                    }
                }
            }

            BotAiState.TAKE_COVER -> {
                val coverPos = bot.investigatePos
                if (coverPos == null || bot.stateTimer <= 0f) {
                    bot.aiState = if (bot.ammoInMag < weapon.magSize * 0.5f) {
                        bot.reloadTimer = weapon.reloadTimeSec
                        BotAiState.RELOAD
                    } else {
                        BotAiState.SEARCH
                    }
                    bot.stateTimer = 3.0f
                } else {
                    followPathToward(bot, coverPos, baseSpeed * 1.1f, dt, colliders)
                    if (bot.position.flatDistanceTo(coverPos) < 1.3f) {
                        if (bot.ammoInMag < weapon.magSize) {
                            bot.aiState = BotAiState.RELOAD
                            bot.reloadTimer = weapon.reloadTimeSec
                        } else if (visibleTargetPos != null && bot.reactionTimer <= 0f && bot.fireCooldown <= 0f) {
                            faceToward(bot, visibleTargetPos, dt * 10f)
                            attemptFire(bot, visibleTargetId, visibleTargetPos, difficulty, shotsOut)
                        }
                    }
                }
            }

            BotAiState.RETREAT -> {
                val threat = visibleTargetPos ?: bot.lastKnownTargetPos
                if (threat == null || bot.stateTimer <= 0f) {
                    bot.aiState = BotAiState.PATROL
                } else {
                    val away = (bot.position - threat).normalized()
                    moveWithCollision(bot, away.x, away.z, baseSpeed * 1.05f, dt, colliders)
                    faceToward(bot, threat, dt * 7f)
                    if (visibleTargetPos != null && bot.fireCooldown <= 0f && rng.nextFloat() < 0.4f) {
                        attemptFire(bot, visibleTargetId, visibleTargetPos, difficulty, shotsOut)
                    }
                }
            }

            BotAiState.SEARCH -> {
                val lastPos = bot.lastKnownTargetPos ?: bot.investigatePos
                if (lastPos == null || bot.stateTimer <= 0f) {
                    bot.lastKnownTargetPos = null
                    bot.targetEntityId = null
                    bot.aiState = BotAiState.PATROL
                    pickNewPatrolTarget(bot)
                } else {
                    followPathToward(bot, lastPos, baseSpeed * 0.9f, dt, colliders)
                    if (bot.position.flatDistanceTo(lastPos) < 1.8f) {
                        // Look around while searching
                        bot.yawDeg = Math3D.wrapAngleDeg(bot.yawDeg + dt * 95f)
                    }
                }
            }

            BotAiState.RELOAD, BotAiState.DEAD -> Unit
        }
    }

    private fun findVisibleEnemy(
        bot: BotEntity,
        mode: GameModeType,
        playerPos: Vec3,
        playerAlive: Boolean,
        playerTeam: Team,
        allBots: List<BotEntity>,
        colliders: List<AABB>
    ): Triple<Int?, Vec3?, Float> {
        val eye = bot.eyePos()
        val maxSight = 48f
        var bestId: Int? = null
        var bestPos: Vec3? = null
        var bestDist = maxSight

        // Check human player
        val isPlayerEnemy = playerAlive && when (mode) {
            GameModeType.FREE_FOR_ALL, GameModeType.SURVIVAL -> true
            GameModeType.TEAM_DEATHMATCH, GameModeType.ELIMINATION, GameModeType.RANKED_2V2 -> bot.team != playerTeam
            GameModeType.PRACTICE -> false
        }

        if (isPlayerEnemy) {
            val targetTorso = Vec3(playerPos.x, playerPos.y + 1.25f, playerPos.z)
            val d = eye.distanceTo(targetTorso)
            if (d < bestDist && isWithinSightCone(bot, targetTorso, d) && CollisionWorld.hasLineOfSight(eye, targetTorso, colliders)) {
                bestId = -1
                bestPos = targetTorso
                bestDist = d
            }
        }

        // Check other bots (for TDM / Elimination / Ranked 2v2 / FFA)
        if (mode == GameModeType.TEAM_DEATHMATCH || mode == GameModeType.ELIMINATION || mode == GameModeType.RANKED_2V2 || mode == GameModeType.FREE_FOR_ALL) {
            for (other in allBots) {
                if (other.id == bot.id || !other.isAlive) continue
                val enemy = if (mode == GameModeType.FREE_FOR_ALL) true else other.team != bot.team
                if (!enemy) continue
                val targetTorso = other.torsoPos()
                val d = eye.distanceTo(targetTorso)
                if (d < bestDist && isWithinSightCone(bot, targetTorso, d) && CollisionWorld.hasLineOfSight(eye, targetTorso, colliders)) {
                    bestId = other.id
                    bestPos = targetTorso
                    bestDist = d
                }
            }
        }

        return Triple(bestId, bestPos, bestDist)
    }

    private fun isWithinSightCone(bot: BotEntity, target: Vec3, dist: Float): Boolean {
        if (dist < 6.5f) return true // close peripheral awareness
        val (targetYaw, _) = Math3D.yawPitchToward(bot.eyePos(), target)
        val diff = abs(Math3D.wrapAngleDeg(targetYaw - bot.yawDeg))
        return diff <= 78f
    }

    private fun attemptFire(
        bot: BotEntity,
        targetId: Int?,
        targetPos: Vec3,
        difficulty: BotDifficulty,
        shotsOut: MutableList<BotShotEvent>
    ) {
        if (bot.ammoInMag <= 0) return
        val weapon = WeaponCatalog.byId(bot.weaponId)
        bot.ammoInMag--
        bot.fireCooldown = weapon.fireIntervalSec * (if (difficulty == BotDifficulty.EASY) 2.2f else if (difficulty == BotDifficulty.NORMAL) 1.55f else 1.2f)
        bot.muzzleFlashTimer = 0.06f
        bot.recoilKick = 1f

        val eye = bot.eyePos()
        val baseDir = (targetPos - eye).normalized()
        // Add difficulty-based angular jitter
        val jitterRad = difficulty.aimJitterDeg * Math3D.DEG_TO_RAD
        val jx = (rng.nextFloat() * 2f - 1f) * jitterRad
        val jy = (rng.nextFloat() * 2f - 1f) * jitterRad * 0.7f
        val jz = (rng.nextFloat() * 2f - 1f) * jitterRad
        val shotDir = Vec3(baseDir.x + jx, baseDir.y + jy, baseDir.z + jz).normalized()

        shotsOut.add(
            BotShotEvent(
                shooter = bot,
                origin = eye,
                dir = shotDir,
                targetId = targetId,
                isHitOnTarget = false,
                damage = (weapon.damage * if (difficulty == BotDifficulty.EASY) 0.55f else if (difficulty == BotDifficulty.NORMAL) 0.75f else 0.92f).toInt().coerceAtLeast(6)
            )
        )
    }

    private fun pickNewPatrolTarget(bot: BotEntity): Vec3 {
        val candidate = if (map.waypoints.isNotEmpty()) {
            map.waypoints[rng.nextInt(map.waypoints.size)]
        } else Vec3.ZERO
        bot.investigatePos = candidate
        bot.pathRecalcTimer = 0f
        return candidate
    }

    private fun findBestCoverNode(botPos: Vec3, threatPos: Vec3, colliders: List<AABB>): Vec3? {
        var best: Vec3? = null
        var bestScore = Float.MAX_VALUE
        for (node in map.coverNodes) {
            val d = botPos.flatDistanceTo(node)
            if (d > 22f) continue
            val nodeEye = Vec3(node.x, 1.35f, node.z)
            val blocked = !CollisionWorld.hasLineOfSight(nodeEye, threatPos, colliders)
            val score = d - (if (blocked) 12f else 0f)
            if (score < bestScore) {
                bestScore = score
                best = node
            }
        }
        return best
    }

    private fun followPathToward(
        bot: BotEntity,
        dest: Vec3,
        speed: Float,
        dt: Float,
        colliders: List<AABB>
    ) {
        if (bot.pathRecalcTimer <= 0f || bot.path.isEmpty()) {
            bot.path = navGrid.findPath(bot.position, dest)
            bot.pathIndex = 0
            bot.pathRecalcTimer = 0.75f
        }

        val waypoint = if (bot.pathIndex < bot.path.size) bot.path[bot.pathIndex] else dest
        val dx = waypoint.x - bot.position.x
        val dz = waypoint.z - bot.position.z
        val dist = kotlin.math.sqrt(dx * dx + dz * dz)
        if (dist < 0.85f && bot.pathIndex < bot.path.size - 1) {
            bot.pathIndex++
        }
        if (dist > 0.1f) {
            val nx = dx / dist
            val nz = dz / dist
            moveWithCollision(bot, nx, nz, speed, dt, colliders)
            if (bot.aiState != BotAiState.ATTACK && bot.aiState != BotAiState.RETREAT) {
                val targetYaw = kotlin.math.atan2(nx, -nz) * Math3D.RAD_TO_DEG
                bot.yawDeg = Math3D.lerpAngleDeg(bot.yawDeg, targetYaw, (dt * 7f).coerceAtMost(1f))
            }
        }
    }

    private fun faceToward(bot: BotEntity, target: Vec3, lerpAmount: Float) {
        val (yaw, pitch) = Math3D.yawPitchToward(bot.eyePos(), target)
        bot.yawDeg = Math3D.lerpAngleDeg(bot.yawDeg, yaw, lerpAmount.coerceAtMost(1f))
        bot.pitchDeg = Math3D.lerp(bot.pitchDeg, pitch.coerceIn(-40f, 40f), lerpAmount.coerceAtMost(1f))
    }

    private fun moveWithCollision(
        bot: BotEntity,
        dirX: Float,
        dirZ: Float,
        speed: Float,
        dt: Float,
        colliders: List<AABB>
    ) {
        val len = kotlin.math.sqrt(dirX * dirX + dirZ * dirZ)
        if (len < 1e-4f) return
        val nx = (dirX / len) * speed * dt
        val nz = (dirZ / len) * speed * dt
        val nextX = (bot.position.x + nx).coerceIn(-map.halfExtent + 0.6f, map.halfExtent - 0.6f)
        val nextZ = (bot.position.z + nz).coerceIn(-map.halfExtent + 0.6f, map.halfExtent - 0.6f)
        val (resolvedX, resolvedZ) = Math3D.resolveCylinderXZCollisions(
            nextX, bot.position.y, nextZ, 0.42f, 1.8f, colliders
        )
        val moved = kotlin.math.sqrt(
            (resolvedX - bot.position.x) * (resolvedX - bot.position.x) +
                (resolvedZ - bot.position.z) * (resolvedZ - bot.position.z)
        )
        bot.position = Vec3(resolvedX, bot.position.y, resolvedZ)
        bot.walkCycle += moved * 2.8f
    }
}
