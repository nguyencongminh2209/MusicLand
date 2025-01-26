package com.example.musicland2308

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.MediaPlayer
import android.media.MediaPlayer.OnSeekCompleteListener
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random


//class MusicService : android.app.Service() {
class MusicService : Service(){
    interface OnSeekChangedListener {
        fun onSeekChanged(pos: Int)
    }
    enum class RepeatMode{
        OFF, ONE, ALL
    }
    private val onSeekChangeListeners = ArrayList<OnSeekChangedListener>()
    val listSong = MutableLiveData<List<Song>>(emptyList())
    private var currentSongIndex = -1
    lateinit var mediaPlayer: MediaPlayer
    private var isSuffer = false
    private var repeatMode = RepeatMode.OFF
    val MUSIC_CHANNEL_ID = "music_channels_id"
    // Đăng ký BroadcastReceiver
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val isPlaying = intent.getBooleanExtra("isPlaying", false)
            // Xử lý thông báo
        }
    }
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, IntentFilter("MEDIA_PLAYER_STATUS"))
        //Init mediaPlayer
        mediaPlayer = MediaPlayer()
        mediaPlayer.setOnPreparedListener{
            mediaPlayer.start()
            startForeground(111, createNotification())
        }
        mediaPlayer.setOnCompletionListener {
            if(repeatMode == RepeatMode.OFF) {
                currentSongIndex = -1
            } else if (repeatMode == RepeatMode.ONE){
                playSongAtCurrentIndex()
            } else if (repeatMode == RepeatMode.ALL){
                nextSong()
            }
        }
        mediaPlayer.setOnSeekCompleteListener {  }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent != null && intent.action != null) {
            when (intent?.action) {
                "LOAD_DATA" -> /*load data */ loadAllSong()
                "ACTION_PLAY" -> /*play*/ playSong(intent)
                "ACTION_CONTINUE" -> /*continue song*/ continueSong()
                "ACTION_PAUSE" -> /*pause*/ {
                    if (mediaPlayer.isPlaying) {
                        pause()
                    }
                }
                "ACTION_RESUME" -> /*resume*/{
                    if (!mediaPlayer.isPlaying) {
                        resume()
                    }
                }
                "ACTION_NEXT" -> /*next*/ nextSong()
                "ACTION_PREV" -> /*previous*/ previousSong()
                "ACTION_SEEK" -> /*seek*/ seekSong(intent)
                "ACTION_CLOSE" -> /*close*/ closeService()
                "ACTION_SUFFER" -> /*suffer playback*/ suffer()
                "ACTION_OFF_SUFFER" -> /*suffer playback*/ offsuffer()
                "ACTION_REPEAT_ALL" -> /*repeat all*/ repeatAll()
                "ACTION_REPEAT_ONE" -> /*repeat one*/ repeatOne()
                "ACTION_OFF_ALL_REPEAT_MODE" -> offAllRepeatMode()
                else -> throw IllegalStateException("Unknow action ${intent?.action}")
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun offsuffer() {
        isSuffer = false
    }

    private fun setOnSeekListener(onSeekCompleteListener: OnSeekCompleteListener){
        mediaPlayer.setOnSeekCompleteListener { onSeekCompleteListener }
    }

    fun registerOnSeekChanged(onSeekChangedListener: OnSeekChangedListener) {
        onSeekChangeListeners.add(onSeekChangedListener)
    }

    fun unregisterOnSeekChanged(onSeekChangedListener: OnSeekChangedListener) {
        onSeekChangeListeners.remove(onSeekChangedListener)
    }

    private fun continueSong() {
        TODO("Not yet implemented")
        mediaPlayer.start()
    }

    private fun offAllRepeatMode() {
        repeatMode = RepeatMode.OFF
    }

    private fun repeatOne() {
        repeatMode = RepeatMode.ONE
    }

    private fun repeatAll() {
        repeatMode = RepeatMode.ALL
    }

    private fun suffer() {
        isSuffer = true
    }

    private fun closeService() {
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun seekSong(intent: Intent) {
        var pos = intent.getIntExtra("SEEK_POS", 0)
        pos = Math.min(pos, mediaPlayer.duration)
        mediaPlayer.seekTo(pos)
        dispatchSeekChanged()

    }

    fun dispatchSeekChanged(){
        onSeekChangeListeners.forEach {
            it.onSeekChanged(mediaPlayer.currentPosition)
        }
    }

    private fun previousSong() {

        var index = 0
        if(currentSongIndex != -1 && !isSuffer) {
            index = currentSongIndex - 1
            if(index < 0){
                index = listSong.value!!.size - 1
            }
        } else {
            if(isSuffer){
                index = randomSongIndex()
            }
        }

        currentSongIndex = index
        playSongAtCurrentIndex()
    }

    private fun nextSong() {

        var index = 0
        if(currentSongIndex != -1 && !isSuffer){
            index = currentSongIndex + 1
            if(index >= listSong.value!!.size){
                index = 0
            }
        } else if(isSuffer){
            index = randomSongIndex()

        }

        currentSongIndex = index
        playSongAtCurrentIndex()
    }

    fun randomSongIndex() : Int{
        if(listSong.value!!.isEmpty()){
            return - 1
        } else {
            return java.util.Random().nextInt(listSong.value!!.size - 1)
        }
    }

    private fun pause() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            sendMediaPlayerStatus()
        }
    }

    private fun resume() {
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
            sendMediaPlayerStatus()
        }
    }

    private fun playSongAtCurrentIndex(){
        mediaPlayer.reset()
        if (currentSongIndex == -1) return
        val songId = listSong.value!![currentSongIndex].id
        if(songId != -1){
            val trackUri = ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI, songId.toLong())
            try {
                mediaPlayer.setDataSource(this, trackUri)
                mediaPlayer.prepareAsync()  // Chuẩn bị phát nhạc
            } catch (e: Exception) {
                Log.e("MUSIC SERVICE", "Error starting data source", e)
            }
        }
    }

    private fun playSong(intent: Intent) {
       val songId = intent.getIntExtra("SONG_ID", -1)
        if(songId > -1){
            currentSongIndex = listSong.value?.indexOfFirst {
                if(it.id == songId){
                    return@indexOfFirst true
                }
                return@indexOfFirst false
            }!!
            playSongAtCurrentIndex()
        }
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel.
            val name = "Music Channel"
            val descriptionText = "Description for channel"
            val importance = NotificationManager.IMPORTANCE_LOW
            val mChannel = NotificationChannel(MUSIC_CHANNEL_ID, name, importance)
            mChannel.description = descriptionText
            // Register the channel with the system. You can't change the importance
            // or other notification behaviors after this.
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }
    }
    @TargetApi(Build.VERSION_CODES.O)
    fun createNotification() : Notification {
        val mainIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent
            .getActivity(this, 101, mainIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, MUSIC_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle(listSong.value!![currentSongIndex].title)
            .setContentText(listSong.value!![currentSongIndex].album)
            .setContentIntent(pendingIntent)
            .build()
        return notification
    }


    public inner class Binders() : Binder(){
        fun getService() : MusicService{
            return this@MusicService
        }
    }

    override fun onBind(intent: Intent?): IBinder? {

        return Binders()
    }
    suspend fun loadAllSongAsync() : List<Song>{
        return withContext(Dispatchers.IO){
            val result = mutableListOf<Song>()
            val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.AUTHOR,
                MediaStore.Audio.Media.ALBUM,

                )
            val clauses = "${MediaStore.Audio.Media.IS_MUSIC}=?"
            val clausesArgs = arrayOf("1")
            val order = "${MediaStore.Audio.Media.TITLE} ASC"
            val cursor = contentResolver.query(
                uri, projection, clauses, clausesArgs, order
            )
            cursor?.let { c ->
                val idIndex = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleIndex = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val authorIndex = c.getColumnIndexOrThrow(MediaStore.Audio.Media.AUTHOR)
                val albumIndex = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                while (c.moveToNext()){
                    val id = c.getInt(idIndex)
                    val title = c.getString(titleIndex)
                    val author = c.getString(authorIndex)
                    val album = c.getString(albumIndex)


                    val song = Song(id, title, author, album)
                    result.add(song)
                }
            }



            cursor?.close()
            return@withContext result
        }
    }


    fun loadAllSong(){
        CoroutineScope(Dispatchers.Main).launch {
            val listallSong = loadAllSongAsync()
            listSong.postValue(listallSong)

        }

    }
    private fun sendMediaPlayerStatus() {
        val intent = Intent("MEDIA_PLAYER_STATUS")
        intent.putExtra("isPlaying", mediaPlayer.isPlaying)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
}