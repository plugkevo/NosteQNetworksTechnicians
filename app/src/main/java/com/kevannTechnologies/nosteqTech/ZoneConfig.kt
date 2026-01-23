package com.kevannTechnologies.nosteqTech



/**
 * Zone configuration for technician service area mapping.
 * Each zone contains multiple towns, and each town has ONU naming patterns.
 */
object ZoneConfig {

    /**
     * Zone A Towns and their ONU patterns
     */
    private val zoneATowns = listOf(
        // BANANA - ONUs with BNN_ prefix or containing BANANA
        TownPattern(
            townName = "BANANA",
            patterns = listOf(
                "BNN_",
                "HIGHWAY HOMES",
                "NJORO",
                "P.C.E.A_THIMBIGUA",
                "TRINITY BANANA",
                "WANDUI KIAMBAA"
            )
        ),
        // KARURA
        TownPattern(
            townName = "KARURA",
            patterns = listOf("KARURA")
        ),
        // REDHILL
        TownPattern(
            townName = "REDHILL",
            patterns = listOf(
                "REDHILL",
                "KIANJOGU",
                "GATWIKIRA NJIKU",
                "NDENDERU JUNCTION"
            )
        ),

        // KARURI
        TownPattern(
            townName = "KARURI",
            patterns = listOf("KARURI")
        ),
        // MUCHATHA
        TownPattern(
            townName = "MUCHATHA",
            patterns = listOf(
                "MUCHATHA",
                "KIRIRU",
                "YAMOGO"
            )
        ),
        // RUAKA
        TownPattern(
            townName = "RUAKA",
            patterns = listOf(
                "RUAKA",
                "DAVANA",
                "DIGRO"
            )
        )
    )


    /**
    * Zone B Towns and their ONU patterns
    */
    private val zoneBTowns = listOf(
        // TURITU
        TownPattern(
            townName = "TURITU",
            patterns = listOf(
                "TURITU"
            )
        ),
        // KANUNGA
        TownPattern(
            townName = "KANUNGA",
            patterns = listOf(
                "KANUNGA"
            )
        ),
        // KASPHAT
        TownPattern(
            townName = "KASPHAT",
            patterns = listOf(
                "KASPHAT"
            )
        ),
        // GATHANGA
        TownPattern(
            townName = "GATHANGA",
            patterns = listOf(
                "GATHANGA",
                "MAYUYU"
            )
        ),
        // WAGUTHU
        TownPattern(
            townName = "WAGUTHU",
            patterns = listOf(
                "WAGUTHU",
                "WANYORI"
            )
        ),
        // KIAMBAA
        TownPattern(
            townName = "KIAMBAA",
            patterns = listOf(
                "KIAMBAA",
                "K-SENIOR"
            )
        )
    )



    /**
     * Zone C Towns and their ONU patterns
     */
    private val zoneCTowns = listOf(
        // NAZARETH
        TownPattern(
            townName = "NAZARETH",
            patterns = listOf(
                "NAZARETH",
                "CLARENCE"
            )
        ),
        // KAWAIDA
        TownPattern(
            townName = "KAWAIDA",
            patterns = listOf(
                "KWD_"
            )
        ),
        // RAINI
        TownPattern(
            townName = "RAINI",
            patterns = listOf(
                "RAINI",
                "BRICKHOUSE",
                "COUNTY MOTEL",
                "NDUOTA",
                "NJIKU RAINI",
                "RUDI",
                "RUBIS"
            )
        ),
        // NJIKU
        TownPattern(
            townName = "NJIKU",
            patterns = listOf(
                "NJIKU",
                "HOMEX"
            )
        ),
        // MUTHURWA
        TownPattern(
            townName = "MUTHURWA",
            patterns = listOf(
                "MUTHURWA"
            )
        )
    )
    private val zoneDTowns = listOf<TownPattern>()

    /**
     * Zone E Towns - To be configured
     */
    private val zoneETowns = listOf<TownPattern>()

    /**
     * Map of zone names to their town patterns
     */
    private val zoneMap = mapOf(
        "ZONE A" to zoneATowns,
        "ZONE B" to zoneBTowns,
        "ZONE C" to zoneCTowns,
        "ZONE D" to zoneDTowns,
        "ZONE E" to zoneETowns,
        // Alternative naming formats
        "A" to zoneATowns,
        "B" to zoneBTowns,
        "C" to zoneCTowns,
        "D" to zoneDTowns,
        "E" to zoneETowns
    )

    /**
     * Check if an ONU belongs to a specific zone based on its zoneName
     */
    fun isOnuInZone(onuZoneName: String, technicianServiceArea: String): Boolean {
        val normalizedServiceArea = technicianServiceArea.uppercase().trim()
        val townPatterns = zoneMap[normalizedServiceArea] ?: return false

        if (townPatterns.isEmpty()) return false

        val normalizedOnuZone = onuZoneName.uppercase().trim()

        // Check if ONU zone matches any pattern in any town of the zone
        return townPatterns.any { town ->
            town.patterns.any { pattern ->
                normalizedOnuZone.startsWith(pattern.uppercase()) ||
                        normalizedOnuZone.contains(pattern.uppercase())
            }
        }
    }

    /**
     * Get all patterns for a zone (useful for debugging)
     */
    fun getPatternsForZone(serviceArea: String): List<String> {
        val normalizedServiceArea = serviceArea.uppercase().trim()
        val townPatterns = zoneMap[normalizedServiceArea] ?: return emptyList()
        return townPatterns.flatMap { it.patterns }
    }

    /**
     * Get all zone names
     */
    fun getAllZones(): List<String> = listOf("ZONE A", "ZONE B", "ZONE C", "ZONE D", "ZONE E")
}

/**
 * Data class representing a town and its ONU naming patterns
 */
data class TownPattern(
    val townName: String,
    val patterns: List<String>
)