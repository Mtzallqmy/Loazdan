package com.lughzdan.engine

import kotlinx.serialization.Serializable
import java.security.MessageDigest

@Serializable enum class DownloadState { NOT_INSTALLED, QUEUED, DOWNLOADING, PAUSED, VERIFYING, INSTALLING, INSTALLED, UPDATE_AVAILABLE, FAILED }
@Serializable data class BundledManifest(val schemaVersion: Int, val packId: String, val version: Int, val fixtureOnly: Boolean, val path: String, val bytes: Long, val sha256: String)
interface PackStorage { fun read(path: String): ByteArray }
object BundledPackVerifier {
    fun verify(manifest: BundledManifest, bytes: ByteArray, allowFixtures: Boolean) {
        require(manifest.schemaVersion == 1 && manifest.version > 0)
        require(!manifest.fixtureOnly || allowFixtures) { "Unapproved fixture content" }
        require(manifest.path.matches(Regex("data/[a-z0-9_-]+\\.jsonl")))
        require(manifest.bytes in 1..8_388_608 && bytes.size.toLong() == manifest.bytes)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        require(digest == manifest.sha256) { "Pack integrity failure" }
    }
}
