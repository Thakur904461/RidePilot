package com.example

import com.example.model.RidePlatform
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleRobolectricTest {

  @Test
  fun testPlatformCatalogIntegrity() {
    val platforms = RidePlatform.entries
    assertEquals(5, platforms.size)
    assertEquals("Ola Driver", RidePlatform.OLA.displayName)
    assertEquals("Uber Driver", RidePlatform.UBER.displayName)
    assertEquals("Rapido Captain", RidePlatform.RAPIDO.displayName)
    assertEquals("Porter Partner", RidePlatform.PORTER.displayName)
  }
}
