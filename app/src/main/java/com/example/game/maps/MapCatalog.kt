package com.example.game.maps

import com.example.game.engine.AABB
import com.example.game.engine.Vec3
import com.example.game.model.MapId
import com.example.game.model.PickupType
import com.example.game.model.SurfaceType

object MapCatalog {

    fun getMap(id: MapId): MapDefinition = when (id) {
        MapId.DESERT_BASE -> buildDesertBase()
        MapId.URBAN_CITY -> buildUrbanCity()
        MapId.MILITARY_FACILITY -> buildMilitaryFacility()
        MapId.SNOW_VILLAGE -> buildSnowVillage()
        MapId.INDUSTRIAL_ZONE -> buildIndustrialZone()
    }

    private fun box(
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        r: Float, g: Float, b: Float,
        mat: Int = 1,
        collides: Boolean = true,
        minimap: Boolean = true
    ) = MapBox(
        AABB(minOf(x1, x2), minOf(y1, y2), minOf(z1, z2), maxOf(x1, x2), maxOf(y1, y2), maxOf(z1, z2)),
        r, g, b, mat, collides, minimap
    )

    private fun addPerimeter(list: MutableList<MapBox>, half: Float, height: Float, r: Float, g: Float, b: Float, mat: Int) {
        val t = 1.2f
        list += box(-half - t, 0f, -half - t, half + t, height, -half, r, g, b, mat)
        list += box(-half - t, 0f, half, half + t, height, half + t, r, g, b, mat)
        list += box(-half - t, 0f, -half, -half, height, half, r, g, b, mat)
        list += box(half, 0f, -half, half + t, height, half, r, g, b, mat)
    }

    // 1. DESERT BASE — Sunlit military outpost with two hangars, stacked containers, watchtower & sandbag bunkers
    private fun buildDesertBase(): MapDefinition {
        val half = 30f
        val boxes = mutableListOf<MapBox>()
        addPerimeter(boxes, half, 5.2f, 0.72f, 0.58f, 0.40f, 5)

        // North Hangar (open east/west ends + roof)
        boxes += box(-16f, 0f, -22f, -4f, 4.4f, -21f, 0.55f, 0.52f, 0.46f, 2)
        boxes += box(-16f, 0f, -13f, -4f, 4.4f, -12f, 0.55f, 0.52f, 0.46f, 2)
        boxes += box(-16.4f, 4.4f, -22.4f, -3.6f, 5.0f, -11.6f, 0.42f, 0.44f, 0.46f, 2, collides = true, minimap = false)

        // South Hangar
        boxes += box(4f, 0f, 12f, 16f, 4.4f, 13f, 0.55f, 0.52f, 0.46f, 2)
        boxes += box(4f, 0f, 21f, 16f, 4.4f, 22f, 0.55f, 0.52f, 0.46f, 2)
        boxes += box(3.6f, 4.4f, 11.6f, 16.4f, 5.0f, 22.4f, 0.42f, 0.44f, 0.46f, 2, collides = true, minimap = false)

        // Cargo Containers (olive, rust, blue)
        boxes += box(-6f, 0f, -3f, -1.5f, 2.7f, 4.5f, 0.25f, 0.40f, 0.28f, 2)
        boxes += box(2.5f, 0f, -6f, 8.5f, 2.7f, -2.5f, 0.68f, 0.28f, 0.19f, 2)
        boxes += box(3.2f, 2.7f, -5.6f, 7.8f, 5.2f, -2.9f, 0.22f, 0.38f, 0.56f, 2)
        boxes += box(-18f, 0f, 6f, -12f, 2.7f, 9.5f, 0.72f, 0.52f, 0.20f, 2)
        boxes += box(14f, 0f, -14f, 18.5f, 2.7f, -7f, 0.28f, 0.42f, 0.30f, 2)

        // Central Command Bunker with doorway
        boxes += box(-1.5f, 0f, 7f, 6f, 3.3f, 8.2f, 0.64f, 0.56f, 0.44f, 1)
        boxes += box(12f, 0f, -1f, 19f, 3.2f, 0.4f, 0.64f, 0.56f, 0.44f, 1)

        // Sandbag Cover Lines
        boxes += box(-10f, 0f, -5f, -9f, 1.15f, 1f, 0.76f, 0.65f, 0.44f, 5)
        boxes += box(8f, 0f, 2f, 11f, 1.15f, 3.1f, 0.76f, 0.65f, 0.44f, 5)
        boxes += box(-3f, 0f, -13f, 2f, 1.15f, -11.8f, 0.76f, 0.65f, 0.44f, 5)

        // Watchtower corner pillars + platform
        boxes += box(-24f, 0f, 16f, -22.5f, 4.8f, 17.5f, 0.48f, 0.42f, 0.34f, 4)
        boxes += box(-18.5f, 0f, 16f, -17f, 4.8f, 17.5f, 0.48f, 0.42f, 0.34f, 4)
        boxes += box(-24f, 0f, 21f, -22.5f, 4.8f, 22.5f, 0.48f, 0.42f, 0.34f, 4)
        boxes += box(-18.5f, 0f, 21f, -17f, 4.8f, 22.5f, 0.48f, 0.42f, 0.34f, 4)
        boxes += box(-24.4f, 4.4f, 15.6f, -16.6f, 4.9f, 22.9f, 0.52f, 0.44f, 0.34f, 4, minimap = false)

        val cylinders = listOf(
            MapCylinder(-22f, 0f, -18f, 1.8f, 3.6f, 0.68f, 0.60f, 0.48f, 2),
            MapCylinder(-22f, 0f, -12f, 1.8f, 3.6f, 0.68f, 0.60f, 0.48f, 2),
            MapCylinder(22f, 0f, 5f, 1.4f, 2.8f, 0.72f, 0.34f, 0.20f, 2)
        )

        val doors = listOf(
            DoorState("db_gate_1", AABB(6.2f, 0f, 7.1f, 9.8f, 2.9f, 7.9f), 1f, 0f, 0.38f, 0.42f, 0.45f)
        )

        val pickups = listOf(
            PickupItemState(1, PickupType.HEALTH, Vec3(0f, 0.45f, 0f)),
            PickupItemState(2, PickupType.AMMO, Vec3(-10f, 0.45f, -17.5f)),
            PickupItemState(3, PickupType.ARMOR, Vec3(10f, 0.45f, 17f)),
            PickupItemState(4, PickupType.HEALTH, Vec3(-15f, 0.45f, 12f)),
            PickupItemState(5, PickupType.AMMO, Vec3(16f, 0.45f, -10f))
        )

        return MapDefinition(
            id = MapId.DESERT_BASE,
            halfExtent = half,
            floorSurface = SurfaceType.SAND,
            floorColor = Triple(0.74f, 0.62f, 0.42f),
            floorMatType = 5,
            skyTopColor = Triple(0.32f, 0.58f, 0.85f),
            skyHorizonColor = Triple(0.92f, 0.78f, 0.56f),
            fogColor = Triple(0.86f, 0.73f, 0.54f),
            fogDensity = 0.011f,
            sunDirection = Vec3(0.6f, 0.75f, 0.3f).normalized(),
            sunColor = Triple(1.0f, 0.94f, 0.82f),
            ambientColor = Triple(0.42f, 0.38f, 0.32f),
            weatherParticleType = 1,
            boxes = boxes,
            cylinders = cylinders,
            doorTemplates = doors,
            pickupSpawns = pickups,
            coverNodes = listOf(
                Vec3(-8.2f, 0f, -2f), Vec3(9.5f, 0f, 0.5f), Vec3(-0.5f, 0f, -10.5f),
                Vec3(-15f, 0f, 4.5f), Vec3(12f, 0f, -8f), Vec3(1.5f, 0f, 9.5f)
            ),
            waypoints = listOf(
                Vec3(-20f, 0f, -24f), Vec3(0f, 0f, -24f), Vec3(20f, 0f, -22f),
                Vec3(-10f, 0f, -17.5f), Vec3(10f, 0f, -10f), Vec3(0f, 0f, 0f),
                Vec3(-16f, 0f, 0f), Vec3(16f, 0f, 4f), Vec3(-10f, 0f, 12f),
                Vec3(10f, 0f, 17f), Vec3(-22f, 0f, 24f), Vec3(22f, 0f, 24f)
            ),
            alphaSpawns = listOf(
                SpawnPoint(Vec3(-24f, 0f, -24f), 135f),
                SpawnPoint(Vec3(-18f, 0f, -25f), 145f),
                SpawnPoint(Vec3(-25f, 0f, -17f), 120f),
                SpawnPoint(Vec3(-12f, 0f, -25f), 160f)
            ),
            bravoSpawns = listOf(
                SpawnPoint(Vec3(24f, 0f, 24f), -45f),
                SpawnPoint(Vec3(18f, 0f, 25f), -35f),
                SpawnPoint(Vec3(25f, 0f, 17f), -60f),
                SpawnPoint(Vec3(12f, 0f, 25f), -20f)
            ),
            ffaSpawns = listOf(
                SpawnPoint(Vec3(-24f, 0f, -24f), 135f),
                SpawnPoint(Vec3(24f, 0f, 24f), -45f),
                SpawnPoint(Vec3(24f, 0f, -24f), -135f),
                SpawnPoint(Vec3(-24f, 0f, 24f), 45f),
                SpawnPoint(Vec3(0f, 0f, -25f), 180f),
                SpawnPoint(Vec3(0f, 0f, 25f), 0f)
            )
        )
    }

    // 2. URBAN CITY — Multi-story blocks, alleys, corner shopfronts, barriers & parking lot
    private fun buildUrbanCity(): MapDefinition {
        val half = 32f
        val boxes = mutableListOf<MapBox>()
        addPerimeter(boxes, half, 7.5f, 0.34f, 0.37f, 0.42f, 3)

        // 4 Multi-story City Buildings creating central crossroads & side alleys (mat 1 = brick + windows)
        boxes += box(-24f, 0f, -24f, -10f, 9.5f, -10f, 0.56f, 0.44f, 0.38f, 1)
        boxes += box(10f, 0f, -24f, 24f, 11.0f, -11f, 0.44f, 0.48f, 0.56f, 1)
        boxes += box(-24f, 0f, 10f, -11f, 10.2f, 24f, 0.46f, 0.50f, 0.52f, 1)
        boxes += box(11f, 0f, 11f, 24f, 8.8f, 24f, 0.58f, 0.48f, 0.42f, 1)

        // Road lane markings (non-colliding thin floor strips)
        for (i in -3..3) {
            boxes += box(-0.35f, 0.004f, i * 8f - 1.8f, 0.35f, 0.015f, i * 8f + 1.8f, 0.88f, 0.84f, 0.62f, 0, collides = false, minimap = false)
            boxes += box(i * 8f - 1.8f, 0.004f, -0.35f, i * 8f + 1.8f, 0.015f, 0.35f, 0.88f, 0.84f, 0.62f, 0, collides = false, minimap = false)
        }

        // Shopfront awnings (visual overhangs)
        boxes += box(-10f, 3.0f, -22f, -8.5f, 3.3f, -12f, 0.72f, 0.24f, 0.22f, 2, collides = false, minimap = false)
        boxes += box(8.5f, 3.0f, 12f, 10f, 3.3f, 22f, 0.20f, 0.50f, 0.68f, 2, collides = false, minimap = false)

        // Concrete Jersey Barriers & Kiosks along Main Avenue
        boxes += box(-3.5f, 0f, -4f, -1.5f, 1.35f, 2f, 0.52f, 0.54f, 0.56f, 1)
        boxes += box(2f, 0f, -1.5f, 4.5f, 1.35f, 4.5f, 0.52f, 0.54f, 0.56f, 1)
        boxes += box(-5f, 0f, 13f, 3f, 1.25f, 14.3f, 0.55f, 0.55f, 0.50f, 1)
        boxes += box(-3f, 0f, -15f, 4f, 1.25f, -13.7f, 0.55f, 0.55f, 0.50f, 1)

        // Parked Tactical Vehicles / Vans in Parking Lot & Streets
        boxes += box(-22f, 0f, -3f, -16.5f, 1.85f, -0.2f, 0.24f, 0.32f, 0.44f, 2)
        boxes += box(15f, 0f, 2f, 21f, 1.95f, 5f, 0.58f, 0.58f, 0.60f, 2)
        boxes += box(15f, 0f, -5f, 20.5f, 1.75f, -2.5f, 0.62f, 0.24f, 0.22f, 2)

        // Central Metro Monument / Fountain Base
        boxes += box(-1.2f, 0f, -1.2f, 1.2f, 2.6f, 1.2f, 0.42f, 0.45f, 0.49f, 1)

        val cylinders = listOf(
            MapCylinder(-8.5f, 0f, -8.5f, 0.45f, 5.5f, 0.28f, 0.30f, 0.34f, 2),
            MapCylinder(8.5f, 0f, 8.5f, 0.45f, 5.5f, 0.28f, 0.30f, 0.34f, 2)
        )

        val doors = listOf(
            DoorState("uc_alley_gate", AABB(-27.8f, 0f, -1.8f, -24.2f, 3.0f, -1.0f), 1f, 0f, 0.30f, 0.35f, 0.40f)
        )

        val pickups = listOf(
            PickupItemState(1, PickupType.HEALTH, Vec3(0f, 0.45f, 6f)),
            PickupItemState(2, PickupType.AMMO, Vec3(-18f, 0.45f, 3f)),
            PickupItemState(3, PickupType.ARMOR, Vec3(18f, 0.45f, -7f)),
            PickupItemState(4, PickupType.HEALTH, Vec3(0f, 0.45f, -18f)),
            PickupItemState(5, PickupType.AMMO, Vec3(6f, 0.45f, 18f))
        )

        return MapDefinition(
            id = MapId.URBAN_CITY,
            halfExtent = half,
            floorSurface = SurfaceType.CONCRETE,
            floorColor = Triple(0.24f, 0.26f, 0.29f),
            floorMatType = 0,
            skyTopColor = Triple(0.16f, 0.26f, 0.42f),
            skyHorizonColor = Triple(0.48f, 0.56f, 0.66f),
            fogColor = Triple(0.45f, 0.52f, 0.61f),
            fogDensity = 0.012f,
            sunDirection = Vec3(-0.45f, 0.80f, 0.40f).normalized(),
            sunColor = Triple(0.88f, 0.92f, 1.0f),
            ambientColor = Triple(0.36f, 0.39f, 0.44f),
            weatherParticleType = 0,
            boxes = boxes,
            cylinders = cylinders,
            doorTemplates = doors,
            pickupSpawns = pickups,
            coverNodes = listOf(
                Vec3(-5f, 0f, -1f), Vec3(5.5f, 0f, 1f), Vec3(-15f, 0f, 2f),
                Vec3(14f, 0f, -1f), Vec3(-1f, 0f, -11f), Vec3(1f, 0f, 11f)
            ),
            waypoints = listOf(
                Vec3(-28f, 0f, -28f), Vec3(0f, 0f, -28f), Vec3(28f, 0f, -28f),
                Vec3(-28f, 0f, 0f), Vec3(-6f, 0f, 0f), Vec3(6f, 0f, 0f), Vec3(28f, 0f, 0f),
                Vec3(0f, 0f, -10f), Vec3(0f, 0f, 10f),
                Vec3(-28f, 0f, 28f), Vec3(0f, 0f, 28f), Vec3(28f, 0f, 28f)
            ),
            alphaSpawns = listOf(
                SpawnPoint(Vec3(-4f, 0f, -28f), 180f),
                SpawnPoint(Vec3(4f, 0f, -28f), 180f),
                SpawnPoint(Vec3(-28f, 0f, -26f), 120f),
                SpawnPoint(Vec3(28f, 0f, -26f), -120f)
            ),
            bravoSpawns = listOf(
                SpawnPoint(Vec3(-4f, 0f, 28f), 0f),
                SpawnPoint(Vec3(4f, 0f, 28f), 0f),
                SpawnPoint(Vec3(-28f, 0f, 26f), 60f),
                SpawnPoint(Vec3(28f, 0f, 26f), -60f)
            ),
            ffaSpawns = listOf(
                SpawnPoint(Vec3(0f, 0f, -28f), 180f),
                SpawnPoint(Vec3(0f, 0f, 28f), 0f),
                SpawnPoint(Vec3(-28f, 0f, 0f), 90f),
                SpawnPoint(Vec3(28f, 0f, 0f), -90f),
                SpawnPoint(Vec3(-28f, 0f, -28f), 135f),
                SpawnPoint(Vec3(28f, 0f, 28f), -45f)
            )
        )
    }

    // 3. MILITARY FACILITY — Barracks, Armory Warehouse, Corridors, Training Obstacles & Blast Doors
    private fun buildMilitaryFacility(): MapDefinition {
        val half = 29f
        val boxes = mutableListOf<MapBox>()
        addPerimeter(boxes, half, 5.5f, 0.30f, 0.34f, 0.31f, 1)

        // Central Corridor Walls (leaving central doorways)
        boxes += box(-18f, 0f, -6f, -3.5f, 4.2f, -4.8f, 0.42f, 0.46f, 0.44f, 0)
        boxes += box(3.5f, 0f, -6f, 18f, 4.2f, -4.8f, 0.42f, 0.46f, 0.44f, 0)
        boxes += box(-18f, 0f, 4.8f, -3.5f, 4.2f, 6f, 0.42f, 0.46f, 0.44f, 0)
        boxes += box(3.5f, 0f, 4.8f, 18f, 4.2f, 6f, 0.42f, 0.46f, 0.44f, 0)

        // Barracks Partition Walls (North Wing)
        boxes += box(-9f, 0f, -22f, -7.8f, 3.8f, -11f, 0.38f, 0.42f, 0.40f, 1)
        boxes += box(7.8f, 0f, -22f, 9f, 3.8f, -11f, 0.38f, 0.42f, 0.40f, 1)
        // Barracks Bunks / Lockers
        boxes += box(-20f, 0f, -20f, -15f, 1.8f, -17.5f, 0.28f, 0.36f, 0.28f, 2)
        boxes += box(15f, 0f, -20f, 20f, 1.8f, -17.5f, 0.28f, 0.36f, 0.28f, 2)

        // Training Yard Cover Walls (South Wing)
        boxes += box(-14f, 0f, 13f, -8f, 1.9f, 14.5f, 0.48f, 0.42f, 0.28f, 4)
        boxes += box(8f, 0f, 13f, 14f, 1.9f, 14.5f, 0.48f, 0.42f, 0.28f, 4)
        boxes += box(-3f, 0f, 18f, 3f, 2.2f, 20.5f, 0.34f, 0.38f, 0.42f, 2)
        boxes += box(-21f, 0f, 15f, -18f, 2.4f, 21f, 0.30f, 0.38f, 0.32f, 2)
        boxes += box(18f, 0f, 15f, 21f, 2.4f, 21f, 0.30f, 0.38f, 0.32f, 2)

        // Armory Crates in Central Bay
        boxes += box(-14f, 0f, -1.5f, -10.5f, 1.6f, 1.5f, 0.34f, 0.42f, 0.26f, 4)
        boxes += box(10.5f, 0f, -1.5f, 14f, 1.6f, 1.5f, 0.34f, 0.42f, 0.26f, 4)

        val cylinders = listOf(
            MapCylinder(0f, 0f, 0f, 1.3f, 4.2f, 0.32f, 0.36f, 0.40f, 2),
            MapCylinder(-24f, 0f, 0f, 1.1f, 3.2f, 0.45f, 0.48f, 0.50f, 2),
            MapCylinder(24f, 0f, 0f, 1.1f, 3.2f, 0.45f, 0.48f, 0.50f, 2)
        )

        val doors = listOf(
            DoorState("mf_blast_north", AABB(-3.4f, 0f, -5.8f, 3.4f, 3.4f, -5.0f), 1f, 0f, 0.65f, 0.42f, 0.16f),
            DoorState("mf_blast_south", AABB(-3.4f, 0f, 5.0f, 3.4f, 3.4f, 5.8f), -1f, 0f, 0.65f, 0.42f, 0.16f)
        )

        val pickups = listOf(
            PickupItemState(1, PickupType.ARMOR, Vec3(0f, 0.45f, -2.5f)),
            PickupItemState(2, PickupType.HEALTH, Vec3(-14f, 0.45f, -14f)),
            PickupItemState(3, PickupType.AMMO, Vec3(14f, 0.45f, -14f)),
            PickupItemState(4, PickupType.HEALTH, Vec3(-12f, 0.45f, 17f)),
            PickupItemState(5, PickupType.AMMO, Vec3(12f, 0.45f, 17f))
        )

        return MapDefinition(
            id = MapId.MILITARY_FACILITY,
            halfExtent = half,
            floorSurface = SurfaceType.METAL,
            floorColor = Triple(0.28f, 0.31f, 0.33f),
            floorMatType = 0,
            skyTopColor = Triple(0.14f, 0.19f, 0.24f),
            skyHorizonColor = Triple(0.32f, 0.38f, 0.42f),
            fogColor = Triple(0.26f, 0.31f, 0.35f),
            fogDensity = 0.013f,
            sunDirection = Vec3(0.3f, 0.85f, -0.35f).normalized(),
            sunColor = Triple(0.85f, 0.95f, 0.92f),
            ambientColor = Triple(0.38f, 0.42f, 0.40f),
            weatherParticleType = 0,
            boxes = boxes,
            cylinders = cylinders,
            doorTemplates = doors,
            pickupSpawns = pickups,
            coverNodes = listOf(
                Vec3(-8f, 0f, -2f), Vec3(8f, 0f, 2f), Vec3(-11f, 0f, 11f),
                Vec3(11f, 0f, 11f), Vec3(-4f, 0f, -15f), Vec3(4f, 0f, -15f)
            ),
            waypoints = listOf(
                Vec3(-22f, 0f, -24f), Vec3(0f, 0f, -24f), Vec3(22f, 0f, -24f),
                Vec3(-22f, 0f, -10f), Vec3(0f, 0f, -10f), Vec3(22f, 0f, -10f),
                Vec3(-22f, 0f, 0f), Vec3(-5f, 0f, 0f), Vec3(5f, 0f, 0f), Vec3(22f, 0f, 0f),
                Vec3(-22f, 0f, 11f), Vec3(0f, 0f, 11f), Vec3(22f, 0f, 11f),
                Vec3(-22f, 0f, 24f), Vec3(0f, 0f, 24f), Vec3(22f, 0f, 24f)
            ),
            alphaSpawns = listOf(
                SpawnPoint(Vec3(-16f, 0f, -24f), 180f),
                SpawnPoint(Vec3(0f, 0f, -25f), 180f),
                SpawnPoint(Vec3(16f, 0f, -24f), 180f),
                SpawnPoint(Vec3(-24f, 0f, -14f), 135f)
            ),
            bravoSpawns = listOf(
                SpawnPoint(Vec3(-16f, 0f, 24f), 0f),
                SpawnPoint(Vec3(0f, 0f, 25f), 0f),
                SpawnPoint(Vec3(16f, 0f, 24f), 0f),
                SpawnPoint(Vec3(24f, 0f, 14f), -45f)
            ),
            ffaSpawns = listOf(
                SpawnPoint(Vec3(0f, 0f, -25f), 180f),
                SpawnPoint(Vec3(0f, 0f, 25f), 0f),
                SpawnPoint(Vec3(-24f, 0f, 0f), 90f),
                SpawnPoint(Vec3(24f, 0f, 0f), -90f),
                SpawnPoint(Vec3(-22f, 0f, -22f), 135f),
                SpawnPoint(Vec3(22f, 0f, 22f), -45f)
            )
        )
    }

    // 4. SNOW VILLAGE — Alpine timber cabins, frozen pond, snowdrifts, pine trees & snowfall
    private fun buildSnowVillage(): MapDefinition {
        val half = 31f
        val boxes = mutableListOf<MapBox>()
        addPerimeter(boxes, half, 4.8f, 0.56f, 0.62f, 0.68f, 6)

        // 4 Timber Cabins with snowy roofs
        val cabins = listOf(
            Triple(-16f, -15f, 7f),
            Triple(14f, -16f, 7f),
            Triple(-17f, 13f, 7.5f),
            Triple(13f, 14f, 7f)
        )
        for ((cx, cz, size) in cabins) {
            val h = size * 0.5f
            boxes += box(cx - h, 0f, cz - h, cx + h, 3.2f, cz + h, 0.42f, 0.28f, 0.18f, 4)
            // Snowy stepped roof
            boxes += box(cx - h - 0.5f, 3.2f, cz - h - 0.5f, cx + h + 0.5f, 4.0f, cz + h + 0.5f, 0.88f, 0.92f, 0.97f, 6, minimap = false)
            boxes += box(cx - h * 0.6f, 4.0f, cz - h * 0.6f, cx + h * 0.6f, 4.8f, cz + h * 0.6f, 0.92f, 0.95f, 0.99f, 6, minimap = false)
        }

        // Frozen Pond (walkable glossy ice patch at center)
        boxes += box(-5.5f, -0.05f, -5.5f, 5.5f, 0.04f, 5.5f, 0.60f, 0.82f, 0.95f, 6, collides = false, minimap = false)

        // Stone & Snow Cover Walls around village square
        boxes += box(-8f, 0f, -2f, -6.8f, 1.35f, 3f, 0.56f, 0.60f, 0.66f, 0)
        boxes += box(6.8f, 0f, -3f, 8f, 1.35f, 2f, 0.56f, 0.60f, 0.66f, 0)
        boxes += box(-3f, 0f, -9f, 3f, 1.25f, -7.8f, 0.84f, 0.89f, 0.95f, 6)
        boxes += box(-3f, 0f, 7.8f, 3f, 1.25f, 9f, 0.84f, 0.89f, 0.95f, 6)

        // Firewood Stacks
        boxes += box(0f, 0f, -22f, 4f, 1.5f, -20f, 0.45f, 0.30f, 0.18f, 4)
        boxes += box(-4f, 0f, 20f, 0f, 1.5f, 22f, 0.45f, 0.30f, 0.18f, 4)

        // Pine tree trunks + snow-dusted foliage cylinders
        val cylinders = listOf(
            MapCylinder(-24f, 0f, 0f, 0.55f, 2.2f, 0.36f, 0.22f, 0.14f, 4),
            MapCylinder(-24f, 2.2f, 0f, 2.1f, 3.6f, 0.18f, 0.42f, 0.26f, 6),
            MapCylinder(24f, 0f, 0f, 0.55f, 2.2f, 0.36f, 0.22f, 0.14f, 4),
            MapCylinder(24f, 2.2f, 0f, 2.1f, 3.6f, 0.18f, 0.42f, 0.26f, 6),
            MapCylinder(0f, 0f, -15f, 0.5f, 2.0f, 0.36f, 0.22f, 0.14f, 4),
            MapCylinder(0f, 2.0f, -15f, 1.8f, 3.2f, 0.18f, 0.42f, 0.26f, 6)
        )

        val pickups = listOf(
            PickupItemState(1, PickupType.HEALTH, Vec3(0f, 0.45f, 0f)),
            PickupItemState(2, PickupType.AMMO, Vec3(-10f, 0.45f, -8f)),
            PickupItemState(3, PickupType.ARMOR, Vec3(10f, 0.45f, 8f)),
            PickupItemState(4, PickupType.HEALTH, Vec3(-20f, 0.45f, 0f)),
            PickupItemState(5, PickupType.AMMO, Vec3(20f, 0.45f, 0f))
        )

        return MapDefinition(
            id = MapId.SNOW_VILLAGE,
            halfExtent = half,
            floorSurface = SurfaceType.SNOW,
            floorColor = Triple(0.84f, 0.89f, 0.94f),
            floorMatType = 6,
            skyTopColor = Triple(0.45f, 0.62f, 0.78f),
            skyHorizonColor = Triple(0.82f, 0.88f, 0.94f),
            fogColor = Triple(0.78f, 0.85f, 0.92f),
            fogDensity = 0.015f,
            sunDirection = Vec3(0.5f, 0.70f, -0.5f).normalized(),
            sunColor = Triple(0.95f, 0.98f, 1.0f),
            ambientColor = Triple(0.48f, 0.52f, 0.58f),
            weatherParticleType = 2,
            boxes = boxes,
            cylinders = cylinders,
            doorTemplates = emptyList(),
            pickupSpawns = pickups,
            coverNodes = listOf(
                Vec3(-6f, 0f, 0f), Vec3(6f, 0f, 0f), Vec3(0f, 0f, -7f),
                Vec3(0f, 0f, 7f), Vec3(-11f, 0f, -11f), Vec3(10f, 0f, 10f)
            ),
            waypoints = listOf(
                Vec3(-25f, 0f, -25f), Vec3(0f, 0f, -25f), Vec3(25f, 0f, -25f),
                Vec3(-20f, 0f, 0f), Vec3(0f, 0f, 0f), Vec3(20f, 0f, 0f),
                Vec3(-8f, 0f, -10f), Vec3(8f, 0f, -10f),
                Vec3(-8f, 0f, 10f), Vec3(8f, 0f, 10f),
                Vec3(-25f, 0f, 25f), Vec3(0f, 0f, 25f), Vec3(25f, 0f, 25f)
            ),
            alphaSpawns = listOf(
                SpawnPoint(Vec3(-25f, 0f, -24f), 135f),
                SpawnPoint(Vec3(-10f, 0f, -26f), 165f),
                SpawnPoint(Vec3(10f, 0f, -26f), -165f),
                SpawnPoint(Vec3(25f, 0f, -24f), -135f)
            ),
            bravoSpawns = listOf(
                SpawnPoint(Vec3(-25f, 0f, 24f), 45f),
                SpawnPoint(Vec3(-10f, 0f, 26f), 15f),
                SpawnPoint(Vec3(10f, 0f, 26f), -15f),
                SpawnPoint(Vec3(25f, 0f, 24f), -45f)
            ),
            ffaSpawns = listOf(
                SpawnPoint(Vec3(-25f, 0f, -25f), 135f),
                SpawnPoint(Vec3(25f, 0f, 25f), -45f),
                SpawnPoint(Vec3(-25f, 0f, 25f), 45f),
                SpawnPoint(Vec3(25f, 0f, -25f), -135f),
                SpawnPoint(Vec3(-26f, 0f, 0f), 90f),
                SpawnPoint(Vec3(26f, 0f, 0f), -90f)
            )
        )
    }

    // 5. INDUSTRIAL ZONE — Factory halls, silos, steel gantry bridges, shipping containers & embers
    private fun buildIndustrialZone(): MapDefinition {
        val half = 31f
        val boxes = mutableListOf<MapBox>()
        addPerimeter(boxes, half, 6.2f, 0.28f, 0.30f, 0.33f, 2)

        // West Refinery Hall
        boxes += box(-22f, 0f, -14f, -20.5f, 6.5f, 14f, 0.38f, 0.33f, 0.28f, 2)
        boxes += box(-22f, 0f, -14f, -12f, 6.5f, -12.5f, 0.38f, 0.33f, 0.28f, 2)
        boxes += box(-22f, 0f, 12.5f, -12f, 6.5f, 14f, 0.38f, 0.33f, 0.28f, 2)
        boxes += box(-22.5f, 6.5f, -14.5f, -11.5f, 7.2f, 14.5f, 0.26f, 0.28f, 0.32f, 2, minimap = false)

        // East Steel Storage Bays
        boxes += box(12f, 0f, -18f, 21f, 3.2f, -13f, 0.62f, 0.32f, 0.16f, 2)
        boxes += box(12f, 0f, 13f, 21f, 3.2f, 18f, 0.22f, 0.42f, 0.56f, 2)
        boxes += box(16f, 0f, -4f, 22f, 2.8f, 4f, 0.42f, 0.44f, 0.46f, 2)

        // Overhead Steel Catwalk Span (visual industrial frame + support pillars)
        boxes += box(-12f, 4.2f, -1.8f, 12f, 4.6f, 1.8f, 0.72f, 0.46f, 0.14f, 2, collides = false, minimap = false)
        boxes += box(-6f, 0f, -1.8f, -5.0f, 4.2f, -0.8f, 0.32f, 0.34f, 0.36f, 2)
        boxes += box(-6f, 0f, 0.8f, -5.0f, 4.2f, 1.8f, 0.32f, 0.34f, 0.36f, 2)
        boxes += box(5.0f, 0f, -1.8f, 6.0f, 4.2f, -0.8f, 0.32f, 0.34f, 0.36f, 2)
        boxes += box(5.0f, 0f, 0.8f, 6.0f, 4.2f, 1.8f, 0.32f, 0.34f, 0.36f, 2)

        // Heavy Industrial Machinery & Crates
        boxes += box(-2f, 0f, -11f, 3f, 2.2f, -8f, 0.34f, 0.38f, 0.42f, 2)
        boxes += box(-3f, 0f, 8f, 2f, 2.2f, 11f, 0.34f, 0.38f, 0.42f, 2)
        boxes += box(-9f, 0f, 20f, -2f, 1.6f, 22f, 0.56f, 0.42f, 0.22f, 4)
        boxes += box(2f, 0f, -22f, 9f, 1.6f, -20f, 0.56f, 0.42f, 0.22f, 4)

        // Industrial Silos
        val cylinders = listOf(
            MapCylinder(-2f, 0f, -21f, 2.4f, 7.5f, 0.52f, 0.55f, 0.58f, 2),
            MapCylinder(4f, 0f, 21f, 2.4f, 7.5f, 0.52f, 0.55f, 0.58f, 2),
            MapCylinder(10f, 0f, -7f, 1.6f, 5.2f, 0.66f, 0.36f, 0.16f, 2),
            MapCylinder(-10f, 0f, 7f, 1.6f, 5.2f, 0.66f, 0.36f, 0.16f, 2)
        )

        val doors = listOf(
            DoorState("iz_bay_door", AABB(11.8f, 0f, -4f, 12.6f, 3.2f, 0f), 0f, 1f, 0.72f, 0.44f, 0.14f)
        )

        val pickups = listOf(
            PickupItemState(1, PickupType.ARMOR, Vec3(0f, 0.45f, 0f)),
            PickupItemState(2, PickupType.HEALTH, Vec3(-16f, 0.45f, 0f)),
            PickupItemState(3, PickupType.AMMO, Vec3(14f, 0.45f, 0f)),
            PickupItemState(4, PickupType.HEALTH, Vec3(0f, 0.45f, -15f)),
            PickupItemState(5, PickupType.AMMO, Vec3(0f, 0.45f, 15f))
        )

        return MapDefinition(
            id = MapId.INDUSTRIAL_ZONE,
            halfExtent = half,
            floorSurface = SurfaceType.METAL,
            floorColor = Triple(0.22f, 0.23f, 0.25f),
            floorMatType = 2,
            skyTopColor = Triple(0.18f, 0.16f, 0.22f),
            skyHorizonColor = Triple(0.48f, 0.36f, 0.28f),
            fogColor = Triple(0.38f, 0.32f, 0.29f),
            fogDensity = 0.012f,
            sunDirection = Vec3(-0.6f, 0.55f, -0.45f).normalized(),
            sunColor = Triple(1.0f, 0.76f, 0.54f),
            ambientColor = Triple(0.36f, 0.32f, 0.30f),
            weatherParticleType = 3,
            boxes = boxes,
            cylinders = cylinders,
            doorTemplates = doors,
            pickupSpawns = pickups,
            coverNodes = listOf(
                Vec3(-4f, 0f, -7f), Vec3(4f, 0f, 7f), Vec3(-14f, 0f, -6f),
                Vec3(13f, 0f, 8f), Vec3(8f, 0f, -4f), Vec3(-8f, 0f, 4f)
            ),
            waypoints = listOf(
                Vec3(-25f, 0f, -25f), Vec3(0f, 0f, -25f), Vec3(25f, 0f, -25f),
                Vec3(-16f, 0f, -8f), Vec3(0f, 0f, -5f), Vec3(13f, 0f, -8f),
                Vec3(-16f, 0f, 8f), Vec3(0f, 0f, 5f), Vec3(13f, 0f, 8f),
                Vec3(-25f, 0f, 25f), Vec3(0f, 0f, 25f), Vec3(25f, 0f, 25f)
            ),
            alphaSpawns = listOf(
                SpawnPoint(Vec3(-25f, 0f, -24f), 135f),
                SpawnPoint(Vec3(-12f, 0f, -25f), 170f),
                SpawnPoint(Vec3(12f, 0f, -25f), -170f),
                SpawnPoint(Vec3(25f, 0f, -24f), -135f)
            ),
            bravoSpawns = listOf(
                SpawnPoint(Vec3(-25f, 0f, 24f), 45f),
                SpawnPoint(Vec3(-12f, 0f, 25f), 10f),
                SpawnPoint(Vec3(12f, 0f, 25f), -10f),
                SpawnPoint(Vec3(25f, 0f, 24f), -45f)
            ),
            ffaSpawns = listOf(
                SpawnPoint(Vec3(-25f, 0f, -25f), 135f),
                SpawnPoint(Vec3(25f, 0f, 25f), -45f),
                SpawnPoint(Vec3(-25f, 0f, 25f), 45f),
                SpawnPoint(Vec3(25f, 0f, -25f), -135f),
                SpawnPoint(Vec3(-26f, 0f, 0f), 90f),
                SpawnPoint(Vec3(26f, 0f, 0f), -90f)
            )
        )
    }
}
