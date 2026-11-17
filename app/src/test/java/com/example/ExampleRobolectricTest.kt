package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.SoundEngine
import com.example.game.engine.GameSimulation
import com.example.game.model.GameModeType
import com.example.game.model.GameSettings
import com.example.game.model.Team
import com.example.game.model.WeaponId
import com.example.game.model.WeaponSkin
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Strike Arena", appName)
  }

  @Test
  fun `ranked 2v2 spawns one alpha wingman two bravo rivals and applies active weapon skin`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val soundEngine = SoundEngine(context)
    val sim = GameSimulation(soundEngine) {}
    val settings = GameSettings(
      selectedMode = GameModeType.RANKED_2V2,
      primaryWeapon = WeaponId.AR_M4A1,
      weaponSkins = mapOf(WeaponId.AR_M4A1 to WeaponSkin.GOLD_DYNASTY)
    )
    sim.currentPlayerRp = 2400
    sim.startMatch(settings)

    val alphaBots = sim.bots.count { it.team == Team.ALPHA }
    val bravoBots = sim.bots.count { it.team == Team.BRAVO }
    assertEquals(1, alphaBots)
    assertEquals(2, bravoBots)

    val hud = sim.snapshotHud(60)
    assertEquals(GameModeType.RANKED_2V2, hud.mapMode)
    assertEquals(5, hud.totalRounds)
    assertEquals(2, hud.aliveAlpha)
    assertEquals(2, hud.aliveBravo)
    assertEquals(WeaponSkin.GOLD_DYNASTY, hud.activeWeaponSkin)
  }
}
