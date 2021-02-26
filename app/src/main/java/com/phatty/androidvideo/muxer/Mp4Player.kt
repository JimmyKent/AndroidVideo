package com.phatty.androidvideo.muxer

import android.view.Surface

/**
 * @author jinguochong
 * @since  2021/2/26
 */
class Mp4Player {

    private var mVideoPlayer: VideoPlayer
    private var mAudioPlayer: AudioPlayer

    init {
        mVideoPlayer = VideoPlayer()
        mAudioPlayer = AudioPlayer()
    }

    fun setSurface(surface: Surface) {
        mVideoPlayer.setSurface(surface)
    }

    fun setDataSource(path: String) {
        mVideoPlayer.setDataSource(path)
        mAudioPlayer.setDataSource(path)
    }

    fun start() {
        mVideoPlayer.start()
        mAudioPlayer.start()
    }

    fun stop() {
        mVideoPlayer.stop()
        mAudioPlayer.stop()
    }
}