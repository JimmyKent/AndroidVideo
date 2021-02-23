package com.phatty.androidvideo.audio

import android.media.AudioFormat
import android.media.AudioRecord
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException


// PCM(Pulse Code Modulation，脉冲编码调制)音频数据是未经压缩的音频采样数据裸流，它是由模拟信号经过采样、量化、编码转换成的标准数字音频数据。
// https://www.jianshu.com/p/fd43c1c82945 PCM音频数据
/**
 * 将pcm音频文件转换为wav音频文件
 */
class PcmToWavUtil constructor(
    /**
     * 采样率
     */
    private val mSampleRate: Int,
    /**
     * 声道数
     */
    private val mChannel: Int, encoding: Int

) {
    /**
     * 缓存的音频大小
     */
    private val mBufferSize: Int

    /**
     * @param sampleRate sample rate、采样率
     * @param channel channel、声道
     * @param encoding Audio data format、音频格式
     */
    init {
        mBufferSize = AudioRecord.getMinBufferSize(mSampleRate, mChannel, encoding)
    }

    /**
     * pcm文件转wav文件
     *
     * @param inFilename 源文件路径
     * @param outFilename 目标文件路径
     */
    fun pcmToWav(inFilename: String?, outFilename: String?) {
        val fis: FileInputStream
        val out: FileOutputStream
        val totalAudioLen: Long
        val totalDataLen: Long
        val longSampleRate = mSampleRate.toLong()
        val channels = if (mChannel == AudioFormat.CHANNEL_IN_MONO) 1 else 2
        val byteRate = 16 * mSampleRate * channels / 8.toLong()
        val data = ByteArray(mBufferSize)
        try {
            fis = FileInputStream(inFilename)
            out = FileOutputStream(outFilename)
            totalAudioLen = fis.channel.size()
            totalDataLen = totalAudioLen + 36
            writeWaveFileHeader(
                out, totalAudioLen, totalDataLen,
                longSampleRate, channels, byteRate
            )
            while (fis.read(data) != -1) {
                out.write(data)
            }
            fis.close()
            out.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * 加入wav文件头
     */
    @Throws(IOException::class)
    private fun writeWaveFileHeader(
        out: FileOutputStream, totalAudioLen: Long,
        totalDataLen: Long, longSampleRate: Long, channels: Int, byteRate: Long
    ) {
        val header = ByteArray(44)
        // RIFF/WAVE header
        header[0] = 'R'.toByte()
        header[1] = 'I'.toByte()
        header[2] = 'F'.toByte()
        header[3] = 'F'.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = (totalDataLen shr 8 and 0xff).toByte()
        header[6] = (totalDataLen shr 16 and 0xff).toByte()
        header[7] = (totalDataLen shr 24 and 0xff).toByte()
        //WAVE
        header[8] = 'W'.toByte()
        header[9] = 'A'.toByte()
        header[10] = 'V'.toByte()
        header[11] = 'E'.toByte()
        // 'fmt ' chunk
        header[12] = 'f'.toByte()
        header[13] = 'm'.toByte()
        header[14] = 't'.toByte()
        header[15] = ' '.toByte()
        // 4 bytes: size of 'fmt ' chunk
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        // format = 1
        header[20] = 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (longSampleRate and 0xff).toByte()
        header[25] = (longSampleRate shr 8 and 0xff).toByte()
        header[26] = (longSampleRate shr 16 and 0xff).toByte()
        header[27] = (longSampleRate shr 24 and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = (byteRate shr 8 and 0xff).toByte()
        header[30] = (byteRate shr 16 and 0xff).toByte()
        header[31] = (byteRate shr 24 and 0xff).toByte()
        // block align
        header[32] = (2 * 16 / 8).toByte()
        header[33] = 0
        // bits per sample
        header[34] = 16
        header[35] = 0
        //data
        header[36] = 'd'.toByte()
        header[37] = 'a'.toByte()
        header[38] = 't'.toByte()
        header[39] = 'a'.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = (totalAudioLen shr 8 and 0xff).toByte()
        header[42] = (totalAudioLen shr 16 and 0xff).toByte()
        header[43] = (totalAudioLen shr 24 and 0xff).toByte()
        out.write(header, 0, 44)
    }


}


object GlobalConfig {
    /**
     * 采样率，现在能够保证在所有设备上使用的采样率是44100Hz, 但是其他的采样率（22050, 16000, 11025）在一些设备上也可以使用。
     */
    const val SAMPLE_RATE_INHZ = 44100

    /**
     * 声道数。CHANNEL_IN_MONO and CHANNEL_IN_STEREO. 其中CHANNEL_IN_MONO是可以保证在所有设备能够使用的。
     */
    const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO

    /**
     * 返回的音频数据的格式。 ENCODING_PCM_8BIT, ENCODING_PCM_16BIT, and ENCODING_PCM_FLOAT.
     */
    const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
}