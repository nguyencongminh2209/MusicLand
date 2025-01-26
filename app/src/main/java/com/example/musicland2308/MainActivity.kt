package com.example.musicland2308

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.provider.MediaStore.Audio
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.log


val ATLEAST_TIRAMISU = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

class MainActivity : AppCompatActivity() {
    var isPlay: Boolean = false

    lateinit var adapter: SongAdapter
    var musicService: MusicService? = null
    val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MusicService.Binders) {
                musicService = service.getService()
                musicService?.listSong?.observe(this@MainActivity) { listSong ->
                    adapter.data = listSong
                    adapter.notifyDataSetChanged()
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val btnPlayPause = findViewById<ImageButton>(R.id.btn_play_pause)
        // Khởi tạo BroadcastReceiver
        lateinit var receiver: BroadcastReceiver
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val isPlaying = intent.getBooleanExtra("isPlaying", false)
                Log.d("BroadcastReceiver", "Received isPlaying = $isPlaying")
                this@MainActivity.isPlay = isPlaying
                if (isPlaying) {
                    btnPlayPause.setImageResource(R.drawable.pause)
                } else {
                    btnPlayPause.setImageResource(R.drawable.start)
                }
            }
        }
        // Đăng ký BroadcastReceiver
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(receiver, IntentFilter("MEDIA_PLAYER_STATUS"))

        // loadAllSong()
        val recyclerView: RecyclerView = findViewById<RecyclerView>(R.id.rcv)
        val deconration =
            DividerItemDecoration(recyclerView.context, DividerItemDecoration.VERTICAL)
        recyclerView.addItemDecoration(deconration)
        adapter = SongAdapter(listener = {
            val song = it.tag as Song
            val intent = Intent(this@MainActivity, MusicService::class.java)
            intent.putExtra("SONG_ID", song.id)
            intent.action = "ACTION_PLAY"
            startService(intent)
            isPlay = true
            btnPlayPause.setImageResource(R.drawable.pause)

        })
        recyclerView.adapter = adapter
        //Next
        val btnNext = findViewById<ImageButton>(R.id.btn_next)
        btnNext.setOnClickListener {
            val intent = Intent(this@MainActivity, MusicService::class.java)
            intent.action = "ACTION_NEXT"
            startService(intent)
        }
        //Pre
        val btnPrev = findViewById<ImageButton>(R.id.btn_previous)
        btnPrev.setOnClickListener {
            val intent = Intent(this@MainActivity, MusicService::class.java)
            intent.action = "ACTION_PREV"
            startService(intent)
        }
        //Pause_Resume
        btnPlayPause.setOnClickListener {
            val intent = Intent(this@MainActivity, MusicService::class.java)
            Log.d("ClickPausePlay", "ok")
            if (isPlay) {
//                btnPlayPause.setImageResource(R.drawable.pause)
                Log.d("ClickPausePlay", "Pause")
                intent.action = "ACTION_PAUSE"

            } else {
                Log.d("ClickPausePlay", "Start")
                intent.action = "ACTION_RESUME"
            }
            isPlay = !isPlay
            startService(intent)
        }

        if (checkNeededsPermission()) {
            startMusicService()
        } else {
            requesNeedsPermission()
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    fun checkNeededsPermission(): Boolean {
        val result: Int
        if (!ATLEAST_TIRAMISU) {
            result = checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)

        } else {
            result = checkSelfPermission(android.Manifest.permission.READ_MEDIA_AUDIO)
        }
        return result == PackageManager.PERMISSION_GRANTED
    }

    fun requesNeedsPermission() {

        if (!checkNeededsPermission()) {
            val permissions: String
            if (!ATLEAST_TIRAMISU) {
                permissions = android.Manifest.permission.READ_EXTERNAL_STORAGE
            } else {
                permissions = android.Manifest.permission.READ_MEDIA_AUDIO
            }
            requestPermissions(
                arrayOf(permissions), 999
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 999) {
//            loadAllSong()
            finish()
        } else {
            startMusicService()
        }
    }

    fun startMusicService() {
        val intent = Intent(this@MainActivity, MusicService::class.java)
        intent.action = "LOAD_DATA"
        startService(intent)
        bindService(intent, serviceConnection, 0)
    }
}