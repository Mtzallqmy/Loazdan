package com.lughzdan.content

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlinx.serialization.decodeFromString
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.erdtman.jcs.JsonCanonicalizer
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.time.OffsetDateTime
import java.util.Base64
import java.util.UUID

@Serializable data class Chunk(val sha256: String, val bytes: Long)
@Serializable data class ManifestFile(val path:String,val kind:String,val sha256:String,val bytes:Long,val chunks:List<Chunk>)
@Serializable data class Dependency(val packId:String,val version:Int)
@Serializable data class Asset(val assetId:String,val path:String,val sourceReferenceId:String,val altText:String)
@Serializable data class SourceReference(val referenceId:String,val sourceId:String,val sourceName:String,val author:String,val locator:String,val sourceVersion:String,val licenseCode:String,val attribution:String,val noticePath:String)
@Serializable data class SignedManifest(val schemaVersion:Int,val packId:String,val version:Int,val minAppVersionCode:Int,val minEngineVersion:Int,val locale:String,val publishedAt:String,val files:List<ManifestFile>,val downloadBytes:Long,val installedBytes:Long,val dependencies:List<Dependency>,val sourceRefs:List<String>,val keyId:String,val signature:String,val assets:List<Asset>,val references:List<SourceReference>)

/** Caller must run verification and all filesystem/network methods on a worker thread. */
class ManifestVerifier(trustedKeys:Map<String,ByteArray>) {
 private val keys=trustedKeys.mapValues { (_,v)->v.copyOf().also { require(it.size==32) } }
 fun verify(input:InputStream,appVersion:Int,engineVersion:Int,highWaterMark:Int=0):SignedManifest {
  val bytes=input.readNBytesCompat(16*1024*1024)
  val text=Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
  // The canonicalizer's parser rejects duplicate keys, invalid escapes and lone surrogates.
  JsonCanonicalizer(text)
  val obj=Json.parseToJsonElement(text).jsonObject
  fun checkNumbers(e:JsonElement) { when(e) { is JsonObject->e.values.forEach(::checkNumbers); is JsonArray->e.forEach(::checkNumbers); is JsonPrimitive->if(!e.isString && e.content !in listOf("true","false","null")) { require(e.content.matches(Regex("-?(0|[1-9][0-9]*)"))) { "Integer required" }; require(e.content.toLong() in -9007199254740991L..9007199254740991L) }; else->Unit } }
  checkNumbers(obj)
  val m=Json.decodeFromString<SignedManifest>(text)
  ManifestPolicy.validate(m,appVersion,engineVersion,highWaterMark)
  val raw=keys[m.keyId] ?: error("Unknown signing key")
  require(m.signature.matches(Regex("[A-Za-z0-9_-]{86}")))
  val signature=Base64.getUrlDecoder().decode(m.signature)
  require(Base64.getUrlEncoder().withoutPadding().encodeToString(signature)==m.signature)
  val payload=JsonCanonicalizer(JsonObject(obj.filterKeys { it!="signature" }).toString()).encodedUTF8
  val verifier=Ed25519Signer(); verifier.init(false,Ed25519PublicKeyParameters(raw,0)); verifier.update(payload,0,payload.size)
  require(verifier.verifySignature(signature)) { "Invalid signature" }
  return m
 }
}

internal fun InputStream.readNBytesCompat(limit:Int):ByteArray {
 val out=java.io.ByteArrayOutputStream(); val buf=ByteArray(8192)
 while(true) { val n=read(buf); if(n<0)break; require(out.size().toLong()+n<=limit) { "Input exceeds limit" }; out.write(buf,0,n) }; return out.toByteArray()
}
object ManifestPolicy {
 private val hash=Regex("[a-f0-9]{64}")
 fun safePath(path:String):Boolean = path.length<=160 && path.matches(Regex("(data|media|rights)/[A-Za-z0-9_/-]+\\.(jsonl|webp|png|jpg|ogg|txt)")) && path.split('/').none { it.isEmpty() || it==".." || it=="." }
 private fun uuid(s:String) { require(UUID.fromString(s).toString()==s.lowercase()) }
 fun validate(m:SignedManifest,appVersion:Int,engineVersion:Int,highWaterMark:Int=0) {
  require(m.schemaVersion==1 && m.locale=="ar"); uuid(m.packId)
  require(m.version>0 && m.version>=highWaterMark && m.minAppVersionCode in 1..appVersion && m.minEngineVersion in 1..engineVersion)
  OffsetDateTime.parse(m.publishedAt); require(m.keyId.matches(Regex("[A-Za-z0-9_-]{1,64}")))
  require(m.files.size in 1..100000 && m.files.map {it.path}.distinct().size==m.files.size)
  val chunks=linkedMapOf<String,Long>()
  m.files.forEach { f->
   require(safePath(f.path) && hash.matches(f.sha256) && f.bytes in 1..9007199254740991L && f.chunks.size in 1..100000)
   require(when(f.kind) { "puzzles"->f.path.startsWith("data/") && f.path.endsWith(".jsonl") && f.bytes<=8388608; "rights"->f.path.startsWith("rights/") && f.path.endsWith(".txt"); "media"->f.path.startsWith("media/") && !f.path.endsWith(".jsonl") && !f.path.endsWith(".txt"); else->false })
   f.chunks.forEach { c-> require(hash.matches(c.sha256) && c.bytes in 1..8388608); require(chunks.put(c.sha256,c.bytes)?.let {it==c.bytes}!=false) }
   require(sum(f.chunks.map {it.bytes})==f.bytes)
  }
  require(sum(chunks.values)==m.downloadBytes && sum(m.files.map {it.bytes})==m.installedBytes)
  require(m.dependencies.size<=20 && m.dependencies.map {it.packId}.distinct().size==m.dependencies.size)
  m.dependencies.forEach {uuid(it.packId); require(it.version>0 && it.packId!=m.packId)}
  require(m.sourceRefs.size in 1..200 && m.sourceRefs.distinct().size==m.sourceRefs.size); m.sourceRefs.forEach(::uuid)
  require(m.references.size<=100000 && m.assets.size<=100000)
  require(m.references.map {it.referenceId}.distinct().size==m.references.size && m.assets.map {it.assetId}.distinct().size==m.assets.size && m.assets.map {it.path}.distinct().size==m.assets.size)
  m.references.forEach { r-> uuid(r.referenceId); uuid(r.sourceId); listOf(r.sourceName,r.author,r.locator,r.sourceVersion,r.licenseCode,r.attribution,r.noticePath).forEach { require(it.length in 1..2000) }; require(m.files.any {it.path==r.noticePath && it.kind=="rights"}) }
  m.assets.forEach { a-> uuid(a.assetId); uuid(a.sourceReferenceId); require(a.altText.length in 1..2000 && m.files.any {it.path==a.path && it.kind=="media"}) }
 }
 fun validateDependencies(m:SignedManifest,installed:Map<Dependency,SignedManifest>) {
  val visited=mutableSetOf<Dependency>(); val visiting=mutableSetOf<String>(); val versions=mutableMapOf<String,Int>(); val refs=mutableMapOf<String,SourceReference>(); val assets=mutableMapOf<String,Pair<Asset,String>>()
  fun visit(node:SignedManifest) {
   require(visiting.size<256) { "Dependency depth limit" }
   val identity=Dependency(node.packId,node.version)
   require(node.packId !in visiting) { "Dependency cycle" }
   if(identity in visited)return
   require(visiting.add(node.packId)) { "Dependency cycle" }; require(versions.put(node.packId,node.version)?.let {it==node.version}!=false) { "Version conflict" }
   node.dependencies.forEach { d-> val dependency=installed[d] ?: error("Missing dependency"); require(dependency.packId==d.packId && dependency.version==d.version); visit(dependency) }
   node.references.forEach { r-> require(refs.put(r.referenceId,r)?.let {it==r}!=false) { "Reference conflict" } }
   node.assets.forEach { a-> val v=a to node.files.first {it.path==a.path}.sha256; require(assets.put(a.assetId,v)?.let {it==v}!=false) { "Asset conflict" } }
   visiting.remove(node.packId);visited.add(identity)
  }
  visit(m)
  (installed.values.filter {versions[it.packId]==it.version}+m).forEach { node-> require(node.sourceRefs.all {it in refs}); require(node.assets.all {it.sourceReferenceId in refs}) }
 }
 fun sum(values:Collection<Long>):Long=values.fold(0L) {a,b->Math.addExact(a,b)}
 fun requiredStorage(m:SignedManifest,verifiedHashes:Set<String>,reserve:Long):Long {
  require(reserve>=0); val missing=m.files.flatMap {it.chunks}.distinctBy {it.sha256}.filter {it.sha256 !in verifiedHashes}
  return Math.addExact(Math.addExact(sum(missing.map {it.bytes}),m.installedBytes),reserve)
 }
}
