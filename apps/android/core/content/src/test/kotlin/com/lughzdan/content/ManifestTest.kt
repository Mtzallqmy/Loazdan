package com.lughzdan.content

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.erdtman.jcs.JsonCanonicalizer
import org.junit.Assert.*
import org.junit.Test
import java.util.Base64

class ManifestTest {
 // Deterministic test-only key; never bundled by production.
 private val privateKey=Ed25519PrivateKeyParameters(ByteArray(32){it.toByte()},0)
 private val id="5637c78f-fe7e-528c-9763-9450426f7f18"
 private val ref="5637c78f-fe7e-528c-9763-9450426f7f19"
 private val hash="0".repeat(64)
 private fun manifest()=SignedManifest(1,id,2,1,1,"ar","2026-10-26T00:00:00Z",listOf(ManifestFile("rights/source.txt","rights",hash,1,listOf(Chunk(hash,1)))),1,1,emptyList(),listOf(ref),"test","",emptyList(),listOf(SourceReference(ref,id,"Source","Author","1","1","CC0","Attribution","rights/source.txt")))
 private fun signed():String {
  val obj=Json.parseToJsonElement(Json.encodeToString(manifest())).jsonObject
  val payload=JsonCanonicalizer(JsonObject(obj.filterKeys{it!="signature"}).toString()).encodedUTF8
  val signer=Ed25519Signer();signer.init(true,privateKey);signer.update(payload,0,payload.size)
  return JsonObject(obj+ ("signature" to JsonPrimitive(Base64.getUrlEncoder().withoutPadding().encodeToString(signer.generateSignature())))).toString()
 }
 private fun verify(s:String,high:Int=0)=ManifestVerifier(mapOf("test" to privateKey.generatePublicKey().encoded)).verify(s.byteInputStream(),1,1,high)
 private fun rejects(action:()->Unit){try{action();fail("Expected rejection")}catch(e:IllegalArgumentException){}catch(e:IllegalStateException){}}
 @Test fun verifiesRealEd25519JcsAndRejectsTampering(){val s=signed();assertEquals(2,verify(s).version);assertEquals(2,verify(s.replace(",",",\n")).version);rejects{verify(s.replace("Attribution","Changed"))};rejects{verify(s,3)}}
 @Test fun rejectsUnknownKeysDuplicateKeysUnsafeNumbersAndMalformedSignature(){val s=signed();rejects{verify(s.replace("\"test\"","\"unknown\""))};rejects{verify(s.replaceFirst("{","{\"schemaVersion\":1,"))};rejects{verify(s.replace("\"downloadBytes\":1","\"downloadBytes\":9007199254740992"))};rejects{verify(s.replace("\"signature\":\"","\"signature\":\"="))}}
 @Test fun rejectsTraversalAndDependencyCycles(){assertFalse(ManifestPolicy.safePath("data/../escape.jsonl"));assertFalse(ManifestPolicy.safePath("data\\escape.jsonl"));assertFalse(ManifestPolicy.safePath("/data/p.jsonl"));val m=manifest().copy(dependencies=listOf(Dependency(id,2)));rejects{ManifestPolicy.validateDependencies(m,mapOf(Dependency(id,2) to m))}}
 @Test fun uniqueChunkStorageIncludesAssemblyAndReserve(){assertEquals(11L,ManifestPolicy.requiredStorage(manifest(),emptySet(),9));assertEquals(10L,ManifestPolicy.requiredStorage(manifest(),setOf(hash),9))}
}
