package com.lughzdan.engine
import org.junit.Test
import java.security.MessageDigest
class PackTest {
 private val bytes="fixture".toByteArray()
 private val manifest=BundledManifest(1,"fixture",1,true,"data/fixture.jsonl",bytes.size.toLong(),MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)})
 @Test fun intactDebugPackAccepted(){BundledPackVerifier.verify(manifest,bytes,true)}
 @Test(expected=IllegalArgumentException::class) fun corruptionRejected(){BundledPackVerifier.verify(manifest,"fixturf".toByteArray(),true)}
 @Test(expected=IllegalArgumentException::class) fun incompleteRejected(){BundledPackVerifier.verify(manifest,bytes.dropLast(1).toByteArray(),true)}
 @Test(expected=IllegalArgumentException::class) fun productionFixtureRejected(){BundledPackVerifier.verify(manifest,bytes,false)}
 @Test(expected=IllegalArgumentException::class) fun traversalRejected(){BundledPackVerifier.verify(manifest.copy(path="data/../bad.jsonl"),bytes,true)}
}
