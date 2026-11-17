package com.example.game.engine

import com.example.audio.SoundEngine
import com.example.game.ai.BotBrain
import com.example.game.ai.BotEntity
import com.example.game.ai.BotShotEvent
import com.example.game.maps.CollisionWorld
import com.example.game.maps.DoorState
import com.example.game.maps.MapCatalog
import com.example.game.maps.MapDefinition
import com.example.game.maps.NavGrid
import com.example.game.maps.PickupItemState
import com.example.game.model.BotAiState
import com.example.game.model.GameModeType
import com.example.game.model.GameSettings
import com.example.game.model.HudState
import com.example.game.model.KillFeedEntry
import com.example.game.model.MatchResultSummary
import com.example.game.model.MinimapBlip
import com.example.game.model.MinimapWall
import com.example.game.model.PickupType
import com.example.game.model.Team
import com.example.game.model.WeaponCatalog
import com.example.game.model.WeaponId
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class GameSimulation(
    private val soundEngine: SoundEngine,
    private val onMatchFinished: (MatchResultSummary) -> Unit
) {
    private val lock = Any()
    private val rng = Random(System.currentTimeMillis())

    @Volatile var settings: GameSettings = GameSettings()
    @Volatile var isShowcaseMode: Boolean = true
    @Volatile var isPaused: Boolean = false

    var currentMap: MapDefinition = MapCatalog.getMap(settings.selectedMap)
        private set
    private var navGrid: NavGrid = NavGrid(currentMap.halfExtent, 1.5f, emptyList())
    private var botBrain: BotBrain = BotBrain(currentMap, navGrid)

    val doors = ArrayList<DoorState>()
    val pickups = ArrayList<PickupItemState>()
    val bots = ArrayList<BotEntity>()
    val staticColliders = ArrayList<AABB>()
    val activeColliders = ArrayList<AABB>()
    private var minimapWalls: List<MinimapWall> = emptyList()

    val particlePool = ParticlePool(220)

    // Player physical & combat state
    var playerPos = Vec3(0f, 0f, 0f)
    var playerVelY = 0f
    var playerGrounded = true
    var playerYaw = 0f
    var playerPitch = 0f
    var currentEyeHeight = 1.62f
    var currentFov = 72f

    private var health = 100
    private var maxHealth = 100
    private var armor = 50
    private var isDead = false
    private var respawnTimer = 0f

    // Two-slot weapon loadout
    private var activeSlot = 0 // 0 = primary, 1 = secondary
    private val slotWeaponIds = arrayOf(WeaponId.AR_M4A1, WeaponId.PISTOL_P9)
    private val slotMagAmmo = intArrayOf(30, 12)
    private val slotReserveAmmo = intArrayOf(150, 72)

    private var fireCooldown = 0f
    private var isReloading = false
    private var reloadTimer = 0f
    private var reloadDuration = 1.8f
    private var weaponBobPhase = 0f
    var weaponRecoilOffset = 0f
        private set
    var cameraShakeAmount = 0f
        private set
    var muzzleFlashPos = Vec3.ZERO
        private set
    var muzzleFlashIntensity = 0f
        private set

    // Input state from multi-touch HUD
    @Volatile var moveInputX = 0f
    @Volatile var moveInputY = 0f
    @Volatile var isFiringInput = false
    @Volatile var isAdsInput = false
    @Volatile var isCrouching = false
    @Volatile private var fireJustPressed = false
    @Volatile private var jumpRequested = false
    @Volatile private var reloadRequested = false
    @Volatile private var interactRequested = false
    @Volatile private var swapRequested = false

    // Feedback timers
    private var hitmarkerTimer = 0f
    private var hitmarkerHeadshot = false
    private var damageFlashTimer = 0f
    private var damageDirectionDeg = 0f
    private var footstepDistAccum = 0f
    private var pickupMsg: String? = null
    private var pickupMsgTimer = 0f

    // Match rules & telemetry
    private var matchTimeRemaining = 180f
    private var matchElapsed = 0f
    private var scoreAlpha = 0
    private var scoreBravo = 0
    private var roundNumber = 1
    private var roundTransitionTimer = 0f
    private var waveNumber = 1
    private var waveClearTimer = 0f
    private var playerKills = 0
    private var playerDeaths = 0
    private var playerHeadshots = 0
    private var shotsFired = 0
    private var shotsHit = 0
    private var matchEnded = false
    private var matchResult: MatchResultSummary? = null

    private var showcaseTime = 0f
    private var killFeedIdSeq = 1L
    private val killFeed = ArrayDeque<KillFeedEntry>(6)
    private val botShotsBuffer = ArrayList<BotShotEvent>(16)

    // Flag telling renderer to rebuild static VBOs when map changes
    @Volatile var mapGeometryDirty = true

    private val botCallsigns = listOf(
        "Viper-01", "Ghost-04", "Reaper-7", "Falcon-2", "Nomad-9", "Kodiak-5",
        "Specter-3", "Titan-8", "lynx-6", "Raptor-11", "Cobalt-12", "Apex-10"
    )

    init {
        startShowcase(settings)
    }

    fun startShowcase(newSettings: GameSettings) {
        synchronized(lock) {
            settings = newSettings
            isShowcaseMode = true
            isPaused = false
            loadMap(newSettings.selectedMap)
            bots.clear()
            // Spawn 3 patrolling showcase operators for dynamic menu diorama
            val spawns = currentMap.ffaSpawns
            for (i in 0 until minOf(3, spawns.size)) {
                val sp = spawns[i]
                bots.add(
                    BotEntity(
                        id = i,
                        callsign = botCallsigns[i],
                        team = if (i % 2 == 0) Team.ALPHA else Team.BRAVO,
                        weaponId = WeaponId.AR_M4A1,
                        position = sp.pos,
                        yawDeg = sp.yawDeg,
                        isTargetDummy = false
                    )
                )
            }
            showcaseTime = 0f
            particlePool.clear()
        }
    }

    fun startMatch(newSettings: GameSettings) {
        synchronized(lock) {
            settings = newSettings
            isShowcaseMode = false
            isPaused = false
            matchEnded = false
            matchResult = null
            killFeed.clear()

            loadMap(newSettings.selectedMap)

            // Reset player loadout
            slotWeaponIds[0] = newSettings.primaryWeapon
            slotWeaponIds[1] = newSettings.secondaryWeapon
            val pSpec = WeaponCatalog.byId(slotWeaponIds[0])
            val sSpec = WeaponCatalog.byId(slotWeaponIds[1])
            slotMagAmmo[0] = pSpec.magSize
            slotReserveAmmo[0] = pSpec.reserveAmmo
            slotMagAmmo[1] = sSpec.magSize
            slotReserveAmmo[1] = sSpec.reserveAmmo
            activeSlot = 0
            isReloading = false
            reloadTimer = 0f
            fireCooldown = 0f
            isCrouching = false
            isAdsInput = false
            isFiringInput = false

            // Reset match stats
            scoreAlpha = 0
            scoreBravo = 0
            roundNumber = 1
            roundTransitionTimer = 0f
            waveNumber = 1
            waveClearTimer = 0f
            playerKills = 0
            playerDeaths = 0
            playerHeadshots = 0
            shotsFired = 0
            shotsHit = 0
            matchElapsed = 0f
            matchTimeRemaining = when (newSettings.selectedMode) {
                GameModeType.TEAM_DEATHMATCH -> 180f
                GameModeType.FREE_FOR_ALL -> 180f
                GameModeType.ELIMINATION -> 120f
                GameModeType.SURVIVAL -> 999f
                GameModeType.PRACTICE -> 999f
            }

            spawnPlayer()
            spawnBotsForMode()
            particlePool.clear()
        }
    }

    private fun loadMap(mapId: com.example.game.model.MapId) {
        if (currentMap.id != mapId || staticColliders.isEmpty()) {
            currentMap = MapCatalog.getMap(mapId)
            mapGeometryDirty = true
        }
        doors.clear()
        for (d in currentMap.doorTemplates) {
            doors.add(DoorState(d.id, d.closedBox, d.slideDirX, d.slideDirZ, d.r, d.g, d.b))
        }
        pickups.clear()
        for (p in currentMap.pickupSpawns) {
            pickups.add(PickupItemState(p.id, p.type, p.pos, active = true, respawnTimer = 0f))
        }

        staticColliders.clear()
        val wallList = ArrayList<MinimapWall>()
        for (b in currentMap.boxes) {
            if (b.collides) staticColliders.add(b.aabb)
            if (b.showOnMinimap) {
                wallList.add(MinimapWall(b.aabb.minX, b.aabb.minZ, b.aabb.maxX, b.aabb.maxZ))
            }
        }
        for (c in currentMap.cylinders) {
            staticColliders.add(c.toColliderAABB())
        }
        minimapWalls = wallList
        rebuildActiveColliders()
        navGrid = NavGrid(currentMap.halfExtent, 1.5f, staticColliders)
        botBrain = BotBrain(currentMap, navGrid)
    }

    private fun rebuildActiveColliders() {
        activeColliders.clear()
        activeColliders.addAll(staticColliders)
        for (d in doors) {
            if (d.openAmount < 0.72f) {
                activeColliders.add(d.currentAABB())
            }
        }
    }

    private fun spawnPlayer() {
        val spawns = when (settings.selectedMode) {
            GameModeType.TEAM_DEATHMATCH, GameModeType.ELIMINATION -> currentMap.alphaSpawns
            else -> currentMap.ffaSpawns
        }
        val sp = spawns[rng.nextInt(spawns.size)]
        playerPos = sp.pos
        playerYaw = sp.yawDeg
        playerPitch = 0f
        playerVelY = 0f
        playerGrounded = true
        currentEyeHeight = 1.62f
        health = 100
        maxHealth = 100
        armor = 50
        isDead = false
        respawnTimer = 0f
        isReloading = false
    }

    private fun spawnBotsForMode() {
        bots.clear()
        val count = settings.botCount.coerceIn(2, 12)
        val enemyWeapons = listOf(WeaponId.AR_M4A1, WeaponId.SMG_MPX, WeaponId.SHOTGUN_M870, WeaponId.PISTOL_P9)

        when (settings.selectedMode) {
            GameModeType.TEAM_DEATHMATCH, GameModeType.ELIMINATION -> {
                val alphaCount = (count / 2 - 1).coerceAtLeast(1)
                val bravoCount = (count - alphaCount).coerceAtLeast(1)
                for (i in 0 until alphaCount) {
                    val sp = currentMap.alphaSpawns[(i + 1) % currentMap.alphaSpawns.size]
                    bots.add(
                        BotEntity(
                            id = i,
                            callsign = botCallsigns[i % botCallsigns.size],
                            team = Team.ALPHA,
                            weaponId = enemyWeapons[i % enemyWeapons.size],
                            position = sp.pos,
                            yawDeg = sp.yawDeg
                        )
                    )
                }
                for (j in 0 until bravoCount) {
                    val idx = alphaCount + j
                    val sp = currentMap.bravoSpawns[j % currentMap.bravoSpawns.size]
                    bots.add(
                        BotEntity(
                            id = idx,
                            callsign = botCallsigns[idx % botCallsigns.size],
                            team = Team.BRAVO,
                            weaponId = enemyWeapons[idx % enemyWeapons.size],
                            position = sp.pos,
                            yawDeg = sp.yawDeg
                        )
                    )
                }
            }

            GameModeType.FREE_FOR_ALL -> {
                for (i in 0 until count) {
                    val sp = currentMap.ffaSpawns[(i + 1) % currentMap.ffaSpawns.size]
                    bots.add(
                        BotEntity(
                            id = i,
                            callsign = botCallsigns[i % botCallsigns.size],
                            team = Team.BRAVO,
                            weaponId = enemyWeapons[i % enemyWeapons.size],
                            position = sp.pos,
                            yawDeg = sp.yawDeg
                        )
                    )
                }
            }

            GameModeType.SURVIVAL -> {
                spawnSurvivalWave(waveNumber)
            }

            GameModeType.PRACTICE -> {
                // 3 patrolling targets + 2 static range targets (non-hostile)
                for (i in 0 until 4) {
                    val sp = currentMap.bravoSpawns[i % currentMap.bravoSpawns.size]
                    bots.add(
                        BotEntity(
                            id = i,
                            callsign = "Target-${i + 1}",
                            team = Team.BRAVO,
                            weaponId = WeaponId.AR_M4A1,
                            position = sp.pos,
                            yawDeg = sp.yawDeg,
                            isTargetDummy = (i % 2 == 0)
                        )
                    )
                }
            }
        }
    }

    private fun spawnSurvivalWave(wave: Int) {
        bots.clear()
        val waveBotCount = (2 + wave).coerceAtMost(10)
        val hp = 80 + (wave - 1) * 15
        val weapons = listOf(WeaponId.PISTOL_P9, WeaponId.SMG_MPX, WeaponId.AR_M4A1, WeaponId.SHOTGUN_M870, WeaponId.LMG_M249)
        for (i in 0 until waveBotCount) {
            val sp = currentMap.bravoSpawns[i % currentMap.bravoSpawns.size]
            bots.add(
                BotEntity(
                    id = i,
                    callsign = "Wave$wave-${i + 1}",
                    team = Team.BRAVO,
                    weaponId = weapons[(i + wave) % weapons.size],
                    position = sp.pos,
                    yawDeg = sp.yawDeg,
                    health = hp,
                    maxHealth = hp
                )
            )
        }
    }

    // --- Input Triggers from Compose HUD ---
    fun addLookDelta(dxNorm: Float, dyNorm: Float) {
        synchronized(lock) {
            if (isShowcaseMode || isPaused || isDead || matchEnded) return
            val sens = if (isAdsInput) settings.cameraSensitivity * settings.adsSensitivity else settings.cameraSensitivity
            playerYaw = Math3D.wrapAngleDeg(playerYaw + dxNorm * sens * 0.22f)
            playerPitch = (playerPitch - dyNorm * sens * 0.18f).coerceIn(-82f, 82f)
        }
    }

    fun setFiring(firing: Boolean) {
        if (firing && !isFiringInput) fireJustPressed = true
        isFiringInput = firing
    }

    fun requestJump() { jumpRequested = true }
    fun requestReload() { reloadRequested = true }
    fun requestInteract() { interactRequested = true }
    fun requestSwapWeapon() { swapRequested = true }
    fun toggleCrouch() { isCrouching = !isCrouching }
    fun setAds(ads: Boolean) { isAdsInput = ads }

    // --- Main Simulation Step ---
    fun step(rawDt: Float) {
        val dt = rawDt.coerceIn(0.001f, 0.05f)
        synchronized(lock) {
            if (isShowcaseMode) {
                stepShowcase(dt)
                return
            }
            if (isPaused || matchEnded) return

            matchElapsed += dt
            if (settings.selectedMode != GameModeType.SURVIVAL && settings.selectedMode != GameModeType.PRACTICE) {
                matchTimeRemaining = (matchTimeRemaining - dt).coerceAtLeast(0f)
                if (matchTimeRemaining <= 0f) {
                    finishMatchByTimeout()
                    return
                }
            }

            updateFeedbackTimers(dt)
            updateDoors(dt)
            rebuildActiveColliders()
            updatePickups(dt)
            botBrain.updateTeamTimers(dt)

            if (isDead) {
                if (settings.selectedMode != GameModeType.ELIMINATION && settings.selectedMode != GameModeType.SURVIVAL) {
                    respawnTimer -= dt
                    if (respawnTimer <= 0f) {
                        spawnPlayer()
                    }
                }
            } else {
                updatePlayerMovement(dt)
                updatePlayerWeaponsAndCombat(dt)
            }

            updateBots(dt)
            checkModeProgression(dt)
            particlePool.update(dt, currentMap.weatherParticleType, playerEyePos(), settings.graphicsQuality.maxParticles)
        }
    }

    private fun stepShowcase(dt: Float) {
        showcaseTime += dt
        val radius = 16f
        val angle = showcaseTime * 0.18f
        playerPos = Vec3(sin(angle) * radius, 4.2f, cos(angle) * radius)
        val (yaw, pitch) = Math3D.yawPitchToward(playerEyePos(), Vec3(0f, 1.5f, 0f))
        playerYaw = yaw
        playerPitch = pitch
        for (b in bots) {
            botBrain.updateBot(
                b, dt, settings.botDifficulty, GameModeType.PRACTICE,
                Vec3(0f, -100f, 0f), false, Team.NONE, bots, activeColliders, botShotsBuffer
            )
        }
        botShotsBuffer.clear()
        particlePool.update(dt, currentMap.weatherParticleType, playerPos, settings.graphicsQuality.maxParticles)
    }

    private fun updateFeedbackTimers(dt: Float) {
        if (hitmarkerTimer > 0f) hitmarkerTimer = (hitmarkerTimer - dt).coerceAtLeast(0f)
        if (damageFlashTimer > 0f) damageFlashTimer = (damageFlashTimer - dt).coerceAtLeast(0f)
        if (muzzleFlashIntensity > 0f) muzzleFlashIntensity = (muzzleFlashIntensity - dt * 14f).coerceAtLeast(0f)
        if (weaponRecoilOffset > 0f) weaponRecoilOffset = (weaponRecoilOffset - dt * 6.5f).coerceAtLeast(0f)
        if (cameraShakeAmount > 0f) cameraShakeAmount = (cameraShakeAmount - dt * 5f).coerceAtLeast(0f)
        if (pickupMsgTimer > 0f) {
            pickupMsgTimer -= dt
            if (pickupMsgTimer <= 0f) pickupMsg = null
        }
    }

    private fun updateDoors(dt: Float) {
        val interactPressed = interactRequested
        interactRequested = false
        for (d in doors) {
            val dist = playerPos.flatDistanceTo(d.center())
            if (dist < 3.4f && (settings.autoOpenDoors || interactPressed)) {
                if (!d.isOpen) {
                    d.isOpen = true
                    d.autoCloseTimer = 5.0f
                    soundEngine.playDoor()
                }
            }
            if (d.isOpen) {
                d.openAmount = (d.openAmount + dt * 2.5f).coerceAtMost(1f)
                d.autoCloseTimer -= dt
                if (d.autoCloseTimer <= 0f && dist > 3.6f) {
                    d.isOpen = false
                    soundEngine.playDoor()
                }
            } else {
                d.openAmount = (d.openAmount - dt * 2.2f).coerceAtLeast(0f)
            }
        }
    }

    private fun updatePickups(dt: Float) {
        for (p in pickups) {
            if (!p.active) {
                p.respawnTimer -= dt
                if (p.respawnTimer <= 0f) p.active = true
                continue
            }
            if (!isDead && settings.autoPickup && playerPos.flatDistanceTo(p.pos) < 1.55f) {
                applyPickup(p)
            }
        }
    }

    private fun applyPickup(p: PickupItemState) {
        val spec = WeaponCatalog.byId(slotWeaponIds[activeSlot])
        val consumed = when (p.type) {
            PickupType.HEALTH -> if (health < maxHealth) {
                health = (health + 35).coerceAtMost(maxHealth)
                true
            } else false
            PickupType.ARMOR -> if (armor < 100) {
                armor = (armor + 35).coerceAtMost(100)
                true
            } else false
            PickupType.AMMO -> {
                slotReserveAmmo[activeSlot] = (slotReserveAmmo[activeSlot] + spec.magSize * 2).coerceAtMost(spec.reserveAmmo * 2)
                true
            }
        }
        if (consumed) {
            p.active = false
            p.respawnTimer = 16f
            pickupMsg = p.type.displayName
            pickupMsgTimer = 2.0f
            soundEngine.playPickup()
        }
    }

    private fun updatePlayerMovement(dt: Float) {
        val spec = WeaponCatalog.byId(slotWeaponIds[activeSlot])
        val effectiveAds = isAdsInput || (settings.autoAdsOnFire && isFiringInput)
        val joyMag = sqrt(moveInputX * moveInputX + moveInputY * moveInputY).coerceAtMost(1f)
        val isSprinting = settings.autoSprint && joyMag > 0.85f && moveInputY > 0.5f && !isCrouching && !effectiveAds && !isFiringInput

        val baseSpeed = when {
            isCrouching -> 3.0f
            effectiveAds -> 3.4f
            isSprinting -> 7.4f
            else -> 5.2f
        } * spec.moveSpeedMult

        val yr = playerYaw * Math3D.DEG_TO_RAD
        val fwdX = sin(yr)
        val fwdZ = -cos(yr)
        val rightX = cos(yr)
        val rightZ = sin(yr)

        val wishX = rightX * moveInputX + fwdX * moveInputY
        val wishZ = rightZ * moveInputX + fwdZ * moveInputY

        val nextX = (playerPos.x + wishX * baseSpeed * dt).coerceIn(-currentMap.halfExtent + 0.6f, currentMap.halfExtent - 0.6f)
        val nextZ = (playerPos.z + wishZ * baseSpeed * dt).coerceIn(-currentMap.halfExtent + 0.6f, currentMap.halfExtent - 0.6f)

        // Jump & Gravity
        if (jumpRequested) {
            jumpRequested = false
            if (playerGrounded) {
                playerVelY = 6.4f
                playerGrounded = false
                isCrouching = false
            }
        }
        playerVelY -= 19.0f * dt
        var nextY = playerPos.y + playerVelY * dt
        if (nextY <= 0f) {
            nextY = 0f
            playerVelY = 0f
            playerGrounded = true
        }

        val bodyHeight = if (isCrouching) 1.25f else 1.80f
        val (rx, rz) = Math3D.resolveCylinderXZCollisions(nextX, nextY, nextZ, 0.42f, bodyHeight, activeColliders)
        val movedDist = sqrt((rx - playerPos.x) * (rx - playerPos.x) + (rz - playerPos.z) * (rz - playerPos.z))
        playerPos = Vec3(rx, nextY, rz)

        // Smooth crouch eye height & ADS FOV interpolation
        val targetEye = if (isCrouching) 1.02f else 1.62f
        currentEyeHeight = Math3D.lerp(currentEyeHeight, targetEye, (dt * 12f).coerceAtMost(1f))

        val targetFov = if (effectiveAds) spec.adsFov else 72f
        currentFov = Math3D.lerp(currentFov, targetFov, (dt * 14f).coerceAtMost(1f))

        if (playerGrounded && movedDist > 0.002f) {
            weaponBobPhase += movedDist * 2.1f
            footstepDistAccum += movedDist
            val stepThreshold = if (isSprinting) 1.85f else 2.25f
            if (footstepDistAccum >= stepThreshold) {
                footstepDistAccum = 0f
                soundEngine.playFootstep(currentMap.floorSurface)
                if (isSprinting) {
                    botBrain.notifySoundEvent(playerPos, 11f, Team.ALPHA, bots)
                }
            }
        }
    }

    private fun updatePlayerWeaponsAndCombat(dt: Float) {
        if (fireCooldown > 0f) fireCooldown = (fireCooldown - dt).coerceAtLeast(0f)

        // Swap Weapon
        if (swapRequested) {
            swapRequested = false
            activeSlot = 1 - activeSlot
            isReloading = false
            reloadTimer = 0f
            soundEngine.playReload()
        }

        val spec = WeaponCatalog.byId(slotWeaponIds[activeSlot])

        // Practice mode keeps reserve topped up
        if (settings.selectedMode == GameModeType.PRACTICE) {
            slotReserveAmmo[activeSlot] = spec.reserveAmmo
        }

        // Reload logic
        if (reloadRequested) {
            reloadRequested = false
            startReloadIfPossible()
        }

        if (isReloading) {
            reloadTimer -= dt
            if (reloadTimer <= 0f) {
                isReloading = false
                val needed = spec.magSize - slotMagAmmo[activeSlot]
                val take = minOf(needed, slotReserveAmmo[activeSlot])
                slotMagAmmo[activeSlot] += take
                slotReserveAmmo[activeSlot] -= take
            }
            fireJustPressed = false
            return
        }

        // Aim Assist gentle magnetism toward visible enemy
        if (settings.aimAssistEnabled && (isFiringInput || isAdsInput || abs(moveInputX) > 0.05f)) {
            applyAimAssist(dt)
        }

        // Fire logic
        val shouldFire = if (spec.isAutomatic) isFiringInput else fireJustPressed
        fireJustPressed = false

        if (shouldFire && fireCooldown <= 0f) {
            if (slotMagAmmo[activeSlot] > 0) {
                firePlayerWeapon(spec)
            } else {
                soundEngine.playEmptyClick()
                fireCooldown = 0.25f
                if (settings.autoReload && slotReserveAmmo[activeSlot] > 0) {
                    startReloadIfPossible()
                } else if (settings.autoSwitchWeapon && slotMagAmmo[1 - activeSlot] > 0) {
                    activeSlot = 1 - activeSlot
                    soundEngine.playReload()
                }
            }
        }
    }

    private fun startReloadIfPossible() {
        val spec = WeaponCatalog.byId(slotWeaponIds[activeSlot])
        if (isReloading) return
        if (slotMagAmmo[activeSlot] >= spec.magSize) return
        if (slotReserveAmmo[activeSlot] <= 0) return
        isReloading = true
        reloadDuration = spec.reloadTimeSec
        reloadTimer = reloadDuration
        soundEngine.playReload()
    }

    private fun applyAimAssist(dt: Float) {
        val eye = playerEyePos()
        val camDir = Math3D.forwardFromYawPitch(playerYaw, playerPitch)
        var bestDot = 0.982f // ~10.8 degree cone
        var bestYaw = playerYaw
        var bestPitch = playerPitch
        var found = false

        for (b in bots) {
            if (!b.isAlive || !isEnemyOfPlayer(b)) continue
            val target = b.torsoPos()
            val toBot = (target - eye)
            val dist = toBot.length()
            if (dist > 45f) continue
            val dir = toBot.normalized()
            val d = camDir.dot(dir)
            if (d > bestDot && CollisionWorld.hasLineOfSight(eye, target, activeColliders)) {
                bestDot = d
                val (ty, tp) = Math3D.yawPitchToward(eye, target)
                bestYaw = ty
                bestPitch = tp
                found = true
            }
        }

        if (found) {
            val strength = settings.aimAssistStrength.coerceIn(0f, 1f) * dt * 6.5f
            playerYaw = Math3D.lerpAngleDeg(playerYaw, bestYaw, strength)
            playerPitch = Math3D.lerp(playerPitch, bestPitch, strength)
        }
    }

    private fun firePlayerWeapon(spec: com.example.game.model.WeaponSpec) {
        slotMagAmmo[activeSlot]--
        fireCooldown = spec.fireIntervalSec
        weaponRecoilOffset = 1.0f
        cameraShakeAmount = (spec.recoilPitchDeg * 0.14f).coerceAtMost(0.9f)
        soundEngine.playWeaponShot(spec.id, 1.0f)

        val eye = playerEyePos()
        val fwd = Math3D.forwardFromYawPitch(playerYaw, playerPitch)
        muzzleFlashPos = eye + fwd * 0.7f
        muzzleFlashIntensity = 1.6f

        botBrain.notifySoundEvent(playerPos, 28f, Team.ALPHA, bots)

        val effectiveAds = isAdsInput || (settings.autoAdsOnFire && isFiringInput)
        val spreadDeg = if (effectiveAds) spec.adsSpreadDeg else spec.hipSpreadDeg

        var anyHit = false
        var anyHeadshot = false

        for (p in 0 until spec.pellets) {
            shotsFired++
            val spreadRad = spreadDeg * Math3D.DEG_TO_RAD
            val rx = (rng.nextFloat() * 2f - 1f) * spreadRad
            val ry = (rng.nextFloat() * 2f - 1f) * spreadRad
            val rz = (rng.nextFloat() * 2f - 1f) * spreadRad
            val shotDir = Vec3(fwd.x + rx, fwd.y + ry, fwd.z + rz).normalized()

            val wallDist = CollisionWorld.raycastWalls(eye, shotDir, spec.range, activeColliders) ?: spec.range
            var bestBot: BotEntity? = null
            var bestBotDist = wallDist
            var isHead = false

            for (b in bots) {
                if (!b.isAlive || !isEnemyOfPlayer(b)) continue
                val headHit = b.headAABB().rayIntersect(eye.x, eye.y, eye.z, shotDir.x, shotDir.y, shotDir.z, bestBotDist)
                if (headHit != null && headHit < bestBotDist) {
                    bestBotDist = headHit
                    bestBot = b
                    isHead = true
                }
                val bodyHit = b.bodyAABB().rayIntersect(eye.x, eye.y, eye.z, shotDir.x, shotDir.y, shotDir.z, bestBotDist)
                if (bodyHit != null && bodyHit < bestBotDist) {
                    bestBotDist = bodyHit
                    bestBot = b
                    isHead = false
                }
            }

            val hitPoint = eye + shotDir * bestBotDist
            particlePool.spawnTracer(eye + fwd * 0.65f + Vec3(0f, -0.08f, 0f), hitPoint)

            if (bestBot != null) {
                anyHit = true
                if (isHead) anyHeadshot = true
                shotsHit++
                val dmg = if (isHead) spec.damage * 2 else spec.damage
                applyDamageToBot(bestBot, dmg, isPlayerKiller = true, killerName = "Вы", weaponName = spec.name, isHeadshot = isHead)
                particlePool.spawnBurst(hitPoint, 5, 1.0f, 0.28f, 0.15f, speed = 3.5f, size = 0.08f)
            } else if (wallDist < spec.range) {
                particlePool.spawnBurst(hitPoint, 4, 0.85f, 0.78f, 0.62f, speed = 2.8f, size = 0.07f)
            }
        }

        if (anyHit) {
            hitmarkerTimer = 0.20f
            hitmarkerHeadshot = anyHeadshot
            soundEngine.playHitmarker(anyHeadshot)
        }

        // Apply recoil kick to player camera
        playerPitch = (playerPitch + spec.recoilPitchDeg).coerceIn(-82f, 82f)
        playerYaw = Math3D.wrapAngleDeg(playerYaw + (rng.nextFloat() * 2f - 1f) * spec.recoilYawDeg)

        // Auto-reload if mag empty
        if (slotMagAmmo[activeSlot] <= 0 && settings.autoReload && slotReserveAmmo[activeSlot] > 0) {
            startReloadIfPossible()
        }
    }

    private fun updateBots(dt: Float) {
        botShotsBuffer.clear()
        for (b in bots) {
            if (!b.isAlive) {
                b.deathFallProgress = (b.deathFallProgress + dt * 3.2f).coerceAtMost(1f)
                if (settings.selectedMode == GameModeType.TEAM_DEATHMATCH ||
                    settings.selectedMode == GameModeType.FREE_FOR_ALL ||
                    settings.selectedMode == GameModeType.PRACTICE
                ) {
                    b.respawnTimer -= dt
                    if (b.respawnTimer <= 0f) {
                        val spList = when {
                            settings.selectedMode == GameModeType.FREE_FOR_ALL -> currentMap.ffaSpawns
                            b.team == Team.ALPHA -> currentMap.alphaSpawns
                            else -> currentMap.bravoSpawns
                        }
                        val sp = spList[rng.nextInt(spList.size)]
                        b.resetForSpawn(sp.pos, sp.yawDeg, b.maxHealth)
                    }
                }
                continue
            }

            botBrain.updateBot(
                bot = b,
                dt = dt,
                difficulty = settings.botDifficulty,
                mode = settings.selectedMode,
                playerPos = playerPos,
                playerAlive = !isDead,
                playerTeam = Team.ALPHA,
                allBots = bots,
                colliders = activeColliders,
                shotsOut = botShotsBuffer
            )
        }

        // Resolve bot gunfire
        for (shot in botShotsBuffer) {
            val wallDist = CollisionWorld.raycastWalls(shot.origin, shot.dir, 65f, activeColliders) ?: 65f
            var hitDist = wallDist
            var hitPlayer = false
            var hitBot: BotEntity? = null

            // Check player hit
            if (!isDead && (settings.selectedMode != GameModeType.TEAM_DEATHMATCH && settings.selectedMode != GameModeType.ELIMINATION || shot.shooter.team != Team.ALPHA)) {
                val playerBox = AABB(
                    playerPos.x - 0.38f, playerPos.y, playerPos.z - 0.38f,
                    playerPos.x + 0.38f, playerPos.y + currentEyeHeight + 0.16f, playerPos.z + 0.38f
                )
                val pHit = playerBox.rayIntersect(shot.origin.x, shot.origin.y, shot.origin.z, shot.dir.x, shot.dir.y, shot.dir.z, hitDist)
                if (pHit != null && pHit < hitDist) {
                    hitDist = pHit
                    hitPlayer = true
                }
            }

            // Check other bots hit
            for (other in bots) {
                if (other.id == shot.shooter.id || !other.isAlive) continue
                if ((settings.selectedMode == GameModeType.TEAM_DEATHMATCH || settings.selectedMode == GameModeType.ELIMINATION) && other.team == shot.shooter.team) continue
                val bHit = other.bodyAABB().rayIntersect(shot.origin.x, shot.origin.y, shot.origin.z, shot.dir.x, shot.dir.y, shot.dir.z, hitDist)
                if (bHit != null && bHit < hitDist) {
                    hitDist = bHit
                    hitPlayer = false
                    hitBot = other
                }
            }

            val endPt = shot.origin + shot.dir * hitDist
            particlePool.spawnTracer(shot.origin + shot.dir * 0.6f, endPt, 1.0f, 0.55f, 0.25f)

            val playerDist = playerPos.distanceTo(shot.origin)
            if (playerDist < 42f) {
                soundEngine.playWeaponShot(shot.shooter.weaponId, (1f - playerDist / 42f).coerceIn(0.15f, 0.7f))
            }

            if (hitPlayer) {
                applyDamageToPlayer(shot.damage, shot.shooter)
            } else if (hitBot != null) {
                val spec = WeaponCatalog.byId(shot.shooter.weaponId)
                applyDamageToBot(hitBot, shot.damage, isPlayerKiller = false, killerName = shot.shooter.callsign, weaponName = spec.name, isHeadshot = false)
                shot.shooter.kills++
            }
        }
    }

    private fun applyDamageToBot(
        bot: BotEntity,
        damage: Int,
        isPlayerKiller: Boolean,
        killerName: String,
        weaponName: String,
        isHeadshot: Boolean
    ) {
        if (!bot.isAlive) return
        bot.health = (bot.health - damage).coerceAtLeast(0)
        bot.hitFlashTimer = 0.14f

        // Alert bot to damage direction!
        if (isPlayerKiller && bot.isAlive && !bot.isTargetDummy) {
            bot.lastKnownTargetPos = playerPos
            bot.investigatePos = playerPos
            if (bot.aiState == BotAiState.PATROL || bot.aiState == BotAiState.IDLE) {
                bot.aiState = BotAiState.INVESTIGATE
                bot.stateTimer = 4f
            }
        }

        if (bot.health <= 0) {
            bot.aiState = BotAiState.DEAD
            bot.deaths++
            bot.respawnTimer = 3.2f
            if (isPlayerKiller) {
                playerKills++
                if (isHeadshot) playerHeadshots++
            }
            if (settings.selectedMode == GameModeType.TEAM_DEATHMATCH) {
                if (bot.team == Team.BRAVO) scoreAlpha++ else scoreBravo++
            } else if (settings.selectedMode == GameModeType.FREE_FOR_ALL && isPlayerKiller) {
                scoreAlpha++
            }
            pushKillFeed(killerName, bot.callsign, weaponName, isHeadshot, killerIsPlayer = isPlayerKiller, victimIsPlayer = false)
        }
    }

    private fun applyDamageToPlayer(damage: Int, attacker: BotEntity) {
        if (isDead) return
        var rem = damage
        if (armor > 0) {
            val absorbed = minOf(armor, (damage * 0.55f).toInt())
            armor -= absorbed
            rem -= absorbed
        }
        health = (health - rem).coerceAtLeast(0)
        damageFlashTimer = 0.38f
        val (attYaw, _) = Math3D.yawPitchToward(playerPos, attacker.position)
        damageDirectionDeg = Math3D.wrapAngleDeg(attYaw - playerYaw)
        cameraShakeAmount = 0.45f
        soundEngine.playHurt()

        if (health <= 0) {
            isDead = true
            playerDeaths++
            respawnTimer = 3.0f
            attacker.kills++
            if (settings.selectedMode == GameModeType.TEAM_DEATHMATCH) {
                scoreBravo++
            }
            val wName = WeaponCatalog.byId(attacker.weaponId).name
            pushKillFeed(attacker.callsign, "Вы", wName, false, killerIsPlayer = false, victimIsPlayer = true)
        }
    }

    private fun checkModeProgression(dt: Float) {
        when (settings.selectedMode) {
            GameModeType.TEAM_DEATHMATCH -> {
                if (scoreAlpha >= 30 || scoreBravo >= 30) {
                    val won = scoreAlpha >= scoreBravo
                    endMatch(won, if (won) "ПОБЕДА" else "ПОРАЖЕНИЕ", "Командный бой завершён")
                }
            }

            GameModeType.FREE_FOR_ALL -> {
                val topBotKills = bots.maxOfOrNull { it.kills } ?: 0
                if (playerKills >= 20 || topBotKills >= 20) {
                    val won = playerKills >= topBotKills
                    endMatch(won, if (won) "ПЕРВОЕ МЕСТО" else "МАТЧ ЗАВЕРШЁН", "Режим Каждый за себя")
                }
            }

            GameModeType.ELIMINATION -> {
                if (roundTransitionTimer > 0f) {
                    roundTransitionTimer -= dt
                    if (roundTransitionTimer <= 0f) {
                        roundNumber++
                        spawnPlayer()
                        for (b in bots) {
                            val spList = if (b.team == Team.ALPHA) currentMap.alphaSpawns else currentMap.bravoSpawns
                            b.resetForSpawn(spList[rng.nextInt(spList.size)].pos, 0f)
                        }
                    }
                    return
                }
                val alphaAlive = (if (!isDead) 1 else 0) + bots.count { it.team == Team.ALPHA && it.isAlive }
                val bravoAlive = bots.count { it.team == Team.BRAVO && it.isAlive }
                if (bravoAlive == 0) {
                    scoreAlpha++
                    if (scoreAlpha >= 3) {
                        endMatch(true, "ПОБЕДА ОТРЯДА", "Выиграно раундов: $scoreAlpha : $scoreBravo")
                    } else {
                        roundTransitionTimer = 2.4f
                    }
                } else if (alphaAlive == 0) {
                    scoreBravo++
                    if (scoreBravo >= 3) {
                        endMatch(false, "ПОРАЖЕНИЕ", "Счёт раундов: $scoreAlpha : $scoreBravo")
                    } else {
                        roundTransitionTimer = 2.4f
                    }
                }
            }

            GameModeType.SURVIVAL -> {
                if (isDead) {
                    endMatch(waveNumber >= 3, "ВЫЖИВАНИЕ ЗАВЕРШЕНО", "Пройдено волн: ${waveNumber - 1}")
                    return
                }
                val aliveEnemies = bots.count { it.isAlive }
                if (aliveEnemies == 0) {
                    if (waveClearTimer <= 0f) {
                        waveClearTimer = 2.6f
                        health = (health + 30).coerceAtMost(maxHealth)
                        armor = (armor + 25).coerceAtMost(100)
                        val pSpec = WeaponCatalog.byId(slotWeaponIds[0])
                        slotReserveAmmo[0] = (slotReserveAmmo[0] + pSpec.magSize * 2).coerceAtMost(pSpec.reserveAmmo * 2)
                        pickupMsg = "Волна $waveNumber пройдена! +HP/Патроны"
                        pickupMsgTimer = 2.5f
                        soundEngine.playPickup()
                    } else {
                        waveClearTimer -= dt
                        if (waveClearTimer <= 0f) {
                            waveNumber++
                            spawnSurvivalWave(waveNumber)
                        }
                    }
                }
            }

            GameModeType.PRACTICE -> Unit
        }
    }

    private fun finishMatchByTimeout() {
        when (settings.selectedMode) {
            GameModeType.TEAM_DEATHMATCH -> {
                val won = scoreAlpha >= scoreBravo
                endMatch(won, if (won) "ПОБЕДА" else "ПОРАЖЕНИЕ", "Время матча истекло")
            }
            GameModeType.FREE_FOR_ALL -> {
                val topBot = bots.maxOfOrNull { it.kills } ?: 0
                val won = playerKills >= topBot
                endMatch(won, if (won) "ПОБЕДА" else "МАТЧ ЗАВЕРШЁН", "Время вышло")
            }
            GameModeType.ELIMINATION -> {
                val won = scoreAlpha >= scoreBravo
                endMatch(won, if (won) "ПОБЕДА" else "ПОРАЖЕНИЕ", "Итог по раундам: $scoreAlpha : $scoreBravo")
            }
            else -> endMatch(true, "ТРЕНИРОВКА ЗАВЕРШЕНА", "Отличная стрельба")
        }
    }

    private fun endMatch(victory: Boolean, title: String, subtitle: String) {
        if (matchEnded) return
        matchEnded = true
        if (victory) soundEngine.playVictory() else soundEngine.playDefeat()
        val waves = if (settings.selectedMode == GameModeType.SURVIVAL) (waveNumber - 1).coerceAtLeast(0) else 0
        val summary = MatchResultSummary(
            victory = victory,
            title = title,
            subtitle = subtitle,
            mode = settings.selectedMode,
            mapId = settings.selectedMap,
            scoreAlpha = scoreAlpha,
            scoreBravo = scoreBravo,
            playerKills = playerKills,
            playerDeaths = playerDeaths,
            headshots = playerHeadshots,
            shotsFired = shotsFired,
            shotsHit = shotsHit,
            wavesCleared = waves,
            durationSec = matchElapsed.toInt().coerceAtLeast(1)
        )
        matchResult = summary
        onMatchFinished(summary)
    }

    private fun pushKillFeed(
        killer: String,
        victim: String,
        weapon: String,
        headshot: Boolean,
        killerIsPlayer: Boolean,
        victimIsPlayer: Boolean
    ) {
        if (killFeed.size >= 4) killFeed.removeFirst()
        killFeed.addLast(
            KillFeedEntry(
                id = killFeedIdSeq++,
                killerName = killer,
                victimName = victim,
                weaponName = weapon,
                isHeadshot = headshot,
                killerIsPlayer = killerIsPlayer,
                victimIsPlayer = victimIsPlayer,
                timestampMs = System.currentTimeMillis()
            )
        )
    }

    private fun isEnemyOfPlayer(bot: BotEntity): Boolean {
        return when (settings.selectedMode) {
            GameModeType.TEAM_DEATHMATCH, GameModeType.ELIMINATION -> bot.team != Team.ALPHA
            GameModeType.FREE_FOR_ALL, GameModeType.SURVIVAL, GameModeType.PRACTICE -> true
        }
    }

    fun playerEyePos(): Vec3 = Vec3(playerPos.x, playerPos.y + currentEyeHeight, playerPos.z)
    fun activeWeaponId(): WeaponId = slotWeaponIds[activeSlot]
    fun getWeaponBobPhase(): Float = weaponBobPhase
    fun isPlayerDeadForRender(): Boolean = isDead
    fun isAdsActiveForRender(): Boolean = isAdsInput || (settings.autoAdsOnFire && isFiringInput)
    fun isReloadingForRender(): Boolean = isReloading
    fun getReloadProgressForRender(): Float =
        if (isReloading && reloadDuration > 0f) (1f - reloadTimer / reloadDuration).coerceIn(0f, 1f) else 0f
    fun isSprintingForRender(): Boolean {
        val joyMag = sqrt(moveInputX * moveInputX + moveInputY * moveInputY).coerceAtMost(1f)
        return settings.autoSprint && joyMag > 0.85f && moveInputY > 0.5f && !isCrouching && !isAdsActiveForRender()
    }

    fun snapshotHud(fps: Int): HudState {
        synchronized(lock) {
            val spec = WeaponCatalog.byId(slotWeaponIds[activeSlot])
            val effectiveAds = isAdsInput || (settings.autoAdsOnFire && isFiringInput)
            val joyMag = sqrt(moveInputX * moveInputX + moveInputY * moveInputY).coerceAtMost(1f)
            val spreadPx = (if (effectiveAds) spec.adsSpreadDeg else spec.hipSpreadDeg) * 5.2f +
                joyMag * 10f + weaponRecoilOffset * 14f

            // Check nearby door prompt
            val nearDoor = doors.firstOrNull { playerPos.flatDistanceTo(it.center()) < 3.4f }
            val doorPrompt = if (nearDoor != null && !settings.autoOpenDoors) {
                if (nearDoor.isOpen) "Закрыть дверь" else "Открыть дверь"
            } else null

            // Check targeted enemy for nameplate
            var targetedName: String? = null
            var targetedHp = 0f
            val eye = playerEyePos()
            val fwd = Math3D.forwardFromYawPitch(playerYaw, playerPitch)
            for (b in bots) {
                if (!b.isAlive) continue
                val d = b.position.distanceTo(playerPos)
                if (d < 40f && isEnemyOfPlayer(b)) {
                    val dir = (b.torsoPos() - eye).normalized()
                    if (fwd.dot(dir) > 0.994f && CollisionWorld.hasLineOfSight(eye, b.torsoPos(), activeColliders)) {
                        targetedName = b.callsign
                        targetedHp = b.health.toFloat() / b.maxHealth.toFloat()
                        break
                    }
                }
            }

            // Minimap blips
            val blips = ArrayList<MinimapBlip>(bots.size + pickups.size)
            for (b in bots) {
                if (!b.isAlive) continue
                val enemy = isEnemyOfPlayer(b)
                if (!enemy || b.position.flatDistanceTo(playerPos) < 28f) {
                    blips.add(MinimapBlip(b.position.x - playerPos.x, b.position.z - playerPos.z, isEnemy = enemy))
                }
            }
            for (p in pickups) {
                if (p.active) {
                    blips.add(MinimapBlip(p.pos.x - playerPos.x, p.pos.z - playerPos.z, isEnemy = false, isPickup = true))
                }
            }

            val alphaAlive = (if (!isDead) 1 else 0) + bots.count { it.team == Team.ALPHA && it.isAlive }
            val bravoAlive = bots.count { it.team == Team.BRAVO && it.isAlive }

            return HudState(
                isActiveMatch = !isShowcaseMode,
                isPaused = isPaused,
                health = health,
                maxHealth = maxHealth,
                armor = armor,
                isDead = isDead,
                respawnSecondsLeft = respawnTimer.coerceAtLeast(0f),
                activeWeaponId = slotWeaponIds[activeSlot],
                secondaryWeaponId = slotWeaponIds[1 - activeSlot],
                ammoInMag = slotMagAmmo[activeSlot],
                ammoReserve = slotReserveAmmo[activeSlot],
                isReloading = isReloading,
                reloadProgress = if (isReloading && reloadDuration > 0f) (1f - reloadTimer / reloadDuration).coerceIn(0f, 1f) else 0f,
                isAds = effectiveAds,
                isSprinting = settings.autoSprint && joyMag > 0.85f && moveInputY > 0.5f && !isCrouching && !effectiveAds,
                isCrouching = isCrouching,
                crosshairSpreadPx = spreadPx,
                hitmarkerAlpha = (hitmarkerTimer / 0.20f).coerceIn(0f, 1f),
                hitmarkerHeadshot = hitmarkerHeadshot,
                damageFlashAlpha = (damageFlashTimer / 0.38f).coerceIn(0f, 1f),
                damageDirectionDeg = damageDirectionDeg,
                nearDoorPrompt = doorPrompt,
                pickupNotification = pickupMsg,
                playerX = playerPos.x,
                playerZ = playerPos.z,
                playerYawDeg = playerYaw,
                mapMode = settings.selectedMode,
                mapId = settings.selectedMap,
                scoreAlpha = scoreAlpha,
                scoreBravo = scoreBravo,
                playerKills = playerKills,
                playerDeaths = playerDeaths,
                roundNumber = roundNumber,
                totalRounds = 5,
                waveNumber = waveNumber,
                aliveAlpha = alphaAlive,
                aliveBravo = bravoAlive,
                matchTimeRemainingSec = matchTimeRemaining.toInt(),
                killFeed = killFeed.toList(),
                minimapBlips = blips,
                minimapWalls = minimapWalls,
                targetedBotName = targetedName,
                targetedBotHpRatio = targetedHp,
                fpsEstimate = fps,
                matchResult = matchResult
            )
        }
    }
}
