package org.openedx.course.presentation.unit.video

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.media3.cast.CastPlayer
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.google.android.gms.cast.framework.CastContext
import org.openedx.core.module.TranscriptManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.course.data.repository.CourseRepository
import java.util.concurrent.Executors

class EncodedVideoUnitViewModel(
    courseId: String,
    val blockId: String,
    courseRepository: CourseRepository,
    notifier: CourseNotifier,
    networkConnection: NetworkConnection,
    transcriptManager: TranscriptManager,
    private val context: Context,
) : VideoUnitViewModel(
    courseId,
    courseRepository,
    notifier,
    networkConnection,
    transcriptManager
) {

    var exoPlayer: ExoPlayer? = null
        private set
    var castPlayer: CastPlayer? = null
        private set
    private var castContext: CastContext? = null

    var isCastActive = false

    var isPlayerSetUp = false

    private val exoPlayerListener = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            super.onPlayWhenReadyChanged(playWhenReady, reason)
            isPlaying = playWhenReady
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            if (playbackState == Player.STATE_ENDED) {
                markBlockCompleted(blockId)
            }
        }
    }

    @androidx.media3.common.util.UnstableApi
    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)

        if (exoPlayer != null) {
            return
        }

        exoPlayer = ExoPlayer.Builder(context)
            .build()

        val executor = Executors.newSingleThreadExecutor()
        castContext = CastContext.getSharedInstance(context, executor).result
        castContext?.let {
            castPlayer = CastPlayer(it)
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        exoPlayer?.addListener(exoPlayerListener)
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        exoPlayer?.removeListener(exoPlayerListener)
        exoPlayer?.pause()
    }

    fun getActivePlayer(): Player? {
        return if (isCastActive) {
            castPlayer
        } else {
            exoPlayer
        }
    }

    @androidx.media3.common.util.UnstableApi
    fun releasePlayers() {
        exoPlayer?.release()
        castPlayer?.release()
        exoPlayer = null
        castPlayer = null
    }
}