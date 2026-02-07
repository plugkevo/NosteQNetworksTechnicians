package com.kevannTechnologies.nosteqTech.data.api


// Data class for memoizing count calculations
data class CountData(
    val online: Int = 0,
    val los: Int = 0,
    val offline: Int = 0,
    val powerFail: Int = 0
)