package com.phatty.androidvideo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.media.*
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.phatty.androidvideo.audio.GlobalConfig.AUDIO_FORMAT
import com.phatty.androidvideo.audio.GlobalConfig.CHANNEL_CONFIG
import com.phatty.androidvideo.audio.GlobalConfig.SAMPLE_RATE_INHZ
import com.phatty.androidvideo.audio.PcmToWavUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*


class MainActivity : AppCompatActivity() {
    private lateinit var mImageView: ImageView
    private lateinit var mSurfaceView: SurfaceView
    private lateinit var mCustomView: CustomShowPicture


    /**
     * 需要申请的运行时权限
     */
    private val permissions: Array<String> = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    /**
     * 被用户拒绝的权限列表
     */
    private val mPermissionList: ArrayList<String> = ArrayList()
    private val MY_PERMISSIONS_REQUEST = 1001

    private var isRecording = false
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private lateinit var audioData: ByteArray
    private var fileInputStream: FileInputStream? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mImageView = findViewById(R.id.iv)
        mSurfaceView = findViewById(R.id.surface_view)
        mCustomView = findViewById(R.id.custom_view)

        mImageView.setBackgroundResource(R.drawable.ic_launcher)

        mSurfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
                val paint = Paint()
                paint.isAntiAlias = true
                paint.style = Paint.Style.STROKE
                val bitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_launcher)
                val canvas: Canvas = surfaceHolder.lockCanvas() // 先锁定当前surfaceView的画布
                canvas.drawBitmap(bitmap, 0f, 0f, paint) // 执行绘制操作
                surfaceHolder.unlockCanvasAndPost(canvas) // 解除锁定并显示在界面上
            }

            override fun surfaceChanged(
                surfaceHolder: SurfaceHolder,
                i: Int,
                i1: Int,
                i2: Int
            ) {
            }

            override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {}
        })

        checkPermissions()

        val ctrl: Button = findViewById(R.id.btn_control)
        ctrl.setOnClickListener {
            if (ctrl.text == getString(R.string.start_record)) {
                ctrl.text = getString(R.string.stop_record)
                startRecord()
            } else {
                ctrl.text = getString(R.string.start_record)
                stopRecord()
            }
        }

        val convert: Button = findViewById(R.id.btn_convert)
        convert.setOnClickListener {
            val pcmToWavUtil =
                PcmToWavUtil(SAMPLE_RATE_INHZ, CHANNEL_CONFIG, AUDIO_FORMAT)
            val pcmFile = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "test.pcm")
            val wavFile = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "test.wav")
            if (!wavFile.mkdirs()) {
                Log.e("MainActivity", "wavFile Directory not created")
            }
            if (wavFile.exists()) {
                wavFile.delete()
            }
            pcmToWavUtil.pcmToWav(pcmFile.absolutePath, wavFile.absolutePath)
        }

        val play: Button = findViewById(R.id.btn_play)
        play.setOnClickListener {
            val string = play.text.toString()
            if (string == getString(R.string.start_play)) {
                play.text = getString(R.string.stop_play)
                playInModeStream()
                // playInModeStatic()
            } else {
                play.text = getString(R.string.start_play)
                stopPlay()
            }
        }

        val preview: Button = findViewById(R.id.btn_to_preview)
        preview.setOnClickListener {
            startActivity(Intent(this, PreviewActivity::class.java))
        }


    }

    private fun startRecord() {
        val minBufferSize =
            AudioRecord.getMinBufferSize(SAMPLE_RATE_INHZ, CHANNEL_CONFIG, AUDIO_FORMAT)
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC, SAMPLE_RATE_INHZ,
            CHANNEL_CONFIG, AUDIO_FORMAT, minBufferSize
        )
        val data = ByteArray(minBufferSize)
        val file =
            File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "test.pcm")
        Log.i("MainActivity", "file path:" + file.absolutePath)
        if (!file.mkdirs()) {
            Log.e("MainActivity", "Directory not created")
        }
        if (file.exists()) {
            file.delete()
        }
        audioRecord?.startRecording()
        isRecording = true

        // pcm数据无法直接播放，保存为WAV格式。
        Thread(Runnable {
            var os: FileOutputStream? = null
            try {
                os = FileOutputStream(file)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
            if (null != os) {
                while (isRecording) {
                    Log.i("MainActivity", "write file gap time: " + System.currentTimeMillis())
                    val read: Int = audioRecord!!.read(data, 0, minBufferSize)
                    // 如果读取音频数据没有出现错误，就将数据写入到文件
                    if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                        try {
                            os.write(data)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
                try {
                    Log.i("MainActivity", "run: close file output stream !")
                    os.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }).start()
    }


    private fun stopRecord() {
        isRecording = false
        // 释放资源
        if (null != audioRecord) {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            //recordingThread = null;
        }
    }

    /**
     * 播放，使用stream模式
     */
    private fun playInModeStream() {
        /*
        * SAMPLE_RATE_INHZ 对应pcm音频的采样率
        * channelConfig 对应pcm音频的声道
        * AUDIO_FORMAT 对应pcm音频的格式
        * */
        val channelConfig: Int = AudioFormat.CHANNEL_OUT_MONO
        val minBufferSize =
            AudioTrack.getMinBufferSize(SAMPLE_RATE_INHZ, channelConfig, AUDIO_FORMAT)
        audioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            AudioFormat.Builder().setSampleRate(SAMPLE_RATE_INHZ)
                .setEncoding(AUDIO_FORMAT)
                .setChannelMask(channelConfig)
                .build(),
            minBufferSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        audioTrack!!.play()
        val file =
            File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "test.pcm")
        try {
            fileInputStream = FileInputStream(file)
            Thread(Runnable {
                try {
                    val tempBuffer = ByteArray(minBufferSize)
                    while (fileInputStream!!.available() > 0) {
                        val readCount = fileInputStream!!.read(tempBuffer)
                        if (readCount == AudioTrack.ERROR_INVALID_OPERATION ||
                            readCount == AudioTrack.ERROR_BAD_VALUE
                        ) {
                            continue
                        }
                        if (readCount != 0 && readCount != -1) {
                            audioTrack!!.write(tempBuffer, 0, readCount)
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }).start()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * 播放，使用static模式
     */
    private fun playInModeStatic() {
        // static模式，需要将音频数据一次性write到AudioTrack的内部缓冲区
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            try {
                val inputStream = resources.openRawResource(R.raw.ding)
                try {
                    val out = ByteArrayOutputStream()
                    var b: Int
                    while (inputStream.read().also { b = it } != -1) {
                        out.write(b)
                    }
                    Log.d("MainActivity", "Got the data")
                    audioData = out.toByteArray()
                } finally {
                    inputStream.close()
                }
            } catch (e: IOException) {
                Log.wtf("MainActivity", "Failed to read", e)
            }

            withContext(Dispatchers.Main) {
                Log.i(
                    "MainActivity",
                    "Creating track...audioData.length = " + audioData.size
                )

                // R.raw.ding铃声文件的相关属性为 22050Hz, 8-bit, Mono
                audioTrack = AudioTrack(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                    AudioFormat.Builder().setSampleRate(22050)
                        .setEncoding(AudioFormat.ENCODING_PCM_8BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                    audioData.size,
                    AudioTrack.MODE_STATIC,
                    AudioManager.AUDIO_SESSION_ID_GENERATE
                )
                Log.d("MainActivity", "Writing audio data...")
                audioTrack!!.write(audioData, 0, audioData.size)
                Log.d("MainActivity", "Starting playback")
                audioTrack!!.play()
                Log.d("MainActivity", "Playing")
            }
        }


    }


    /**
     * 停止播放
     */
    private fun stopPlay() {
        if (audioTrack != null) {
            Log.d("MainActivity", "Stopping")
            audioTrack!!.stop()
            Log.d("MainActivity", "Releasing")
            audioTrack!!.release()
            Log.d("MainActivity", "Nulling")
        }
    }

    private fun checkPermissions() {
        // Marshmallow开始才用申请运行时权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (i in permissions.indices) {
                if (ContextCompat.checkSelfPermission(this, permissions[i]) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    mPermissionList.add(permissions[i])
                }
            }
            if (mPermissionList.isNotEmpty()) {
                val permissions: Array<String> =
                    mPermissionList.toArray(arrayOfNulls<String>(mPermissionList.size))
                ActivityCompat.requestPermissions(this, permissions, MY_PERMISSIONS_REQUEST)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MY_PERMISSIONS_REQUEST) {
            for (i in grantResults) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Log.i("MainActivity", permissions[i] + " 权限被用户禁止！")
                }
            }
            // 运行时权限的申请不是本demo的重点，所以不再做更多的处理，请同意权限申请。
        }
    }

}