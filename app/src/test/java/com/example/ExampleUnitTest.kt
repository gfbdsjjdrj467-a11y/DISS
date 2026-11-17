package com.example

import com.example.game.engine.AABB
import com.example.game.engine.Math3D
import com.example.game.engine.Vec3
import com.example.game.maps.CollisionWorld
import com.example.game.maps.MapCatalog
import com.example.game.maps.NavGrid
import com.example.game.model.MapId
import com.example.game.model.WeaponCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun allFiveMapsHaveValidGeometrySpawnsAndPathfinding() {
        for (mapId in MapId.entries) {
            val map = MapCatalog.getMap(mapId)
            assertTrue("Map $mapId needs alpha spawns", map.alphaSpawns.size >= 4)
            assertTrue("Map $mapId needs bravo spawns", map.bravoSpawns.size >= 4)
            assertTrue("Map $mapId needs ffa spawns", map.ffaSpawns.size >= 6)
            assertTrue("Map $mapId needs waypoints", map.waypoints.size >= 8)

            val colliders = map.boxes.filter { it.collides }.map { it.aabb } +
                map.cylinders.map { it.toColliderAABB() }
            val nav = NavGrid(map.halfExtent, 1.5f, colliders)
            val path = nav.findPath(map.alphaSpawns.first().pos, map.bravoSpawns.first().pos)
            assertTrue("Pathfinder should return non-empty path on $mapId", path.isNotEmpty())
        }
    }

    @Test
    fun raycastLineOfSightBlockedByWall() {
        val wall = listOf(AABB(-2f, 0f, -1f, 2f, 3f, 1f))
        assertFalse(CollisionWorld.hasLineOfSight(Vec3(0f, 1.5f, -5f), Vec3(0f, 1.5f, 5f), wall))
        assertTrue(CollisionWorld.hasLineOfSight(Vec3(4f, 1.5f, -5f), Vec3(4f, 1.5f, 5f), wall))
    }

    @Test
    fun cylinderCollisionPreventsWalkingThroughWall() {
        val wall = listOf(AABB(-2f, 0f, -1f, 2f, 3f, 1f))
        val (rx, rz) = Math3D.resolveCylinderXZCollisions(0f, 0f, 0.85f, 0.42f, 1.8f, wall)
        assertTrue("Resolved Z must be pushed outside wall", rz >= 1.41f)
        assertEquals(0f, rx, 0.01f)
    }

    @Test
    fun allSixWeaponClassesHaveDistinctCharacteristics() {
        assertEquals(6, WeaponCatalog.allWeapons.size)
        val rpms = WeaponCatalog.allWeapons.map { it.rpm }.toSet()
        assertEquals(6, rpms.size)
        val sniper = WeaponCatalog.allWeapons.first { it.adsFov < 35f }
        assertNotNull(sniper)
        assertTrue(sniper.damage >= 100)
    }
}
