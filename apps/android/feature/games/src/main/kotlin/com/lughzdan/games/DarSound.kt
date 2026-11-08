package com.lughzdan.games
import android.content.Context
import android.media.MediaPlayer
import android.media.SoundPool
import android.media.AudioAttributes

internal class DarSound(private val context:Context) {
    private val sounds=SoundPool.Builder().setMaxStreams(2).setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build()).build()
    private val ready=java.util.Collections.synchronizedSet(mutableSetOf<Int>())
    private val cues:List<Int>
    private var background:MediaPlayer?=null
    init {
        sounds.setOnLoadCompleteListener { _,id,status->if(status==0)ready.add(id) }
        cues=listOf(R.raw.dar_paper,R.raw.dar_pin,R.raw.dar_reveal).map{sounds.load(context,it,1)}
    }
    fun play(index:Int) {val cue=cues[index];if(cue in ready)sounds.play(cue,.65f,.65f,0,0,1f)}
    fun room(enabled:Boolean,speaking:Boolean){
        if(!enabled){background?.release();background=null;return}
        if(background==null)background=MediaPlayer.create(context,R.raw.dar_room)?.apply{isLooping=true;setVolume(.14f,.14f);start()}
        val volume=if(speaking).02f else .14f
        background?.setVolume(volume,volume)
    }
    fun close(){background?.release();background=null;sounds.release()}
}
