package org.openedx.course.presentation.unit.video

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.core.os.bundleOf
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.Clock
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.extractor.DefaultExtractorsFactory
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.domain.model.VideoQuality
import org.openedx.core.presentation.dialog.appreview.AppReviewManager
import org.openedx.core.presentation.global.viewBinding
import org.openedx.course.R
import org.openedx.course.databinding.FragmentVideoFullScreenBinding
import org.openedx.course.presentation.CourseAnalyticsKey
import org.openedx.foundation.extension.requestApplyInsetsWhenAttached

class VideoFullScreenFragment : Fragment(R.layout.fragment_video_full_screen) {

    private val binding by viewBinding(FragmentVideoFullScreenBinding::bind)
    private val viewModel by viewModel<VideoViewModel> {
        parametersOf(requireArguments().getString(ARG_COURSE_ID, ""))
    }
    private val appReviewManager by inject<AppReviewManager> { parametersOf(requireActivity()) }

    private var exoPlayer: ExoPlayer? = null
    private var blockId = ""
    private val exoPlayerListener = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            super.onPlayWhenReadyChanged(playWhenReady, reason)
            viewModel.isPlaying = playWhenReady
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            if (playbackState == Player.STATE_ENDED) {
                if (!appReviewManager.isDialogShowed) {
                    appReviewManager.tryToOpenRateDialog()
                }
                viewModel.markBlockCompleted(blockId, CourseAnalyticsKey.NATIVE.key)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.videoUrl = requireArguments().getString(ARG_BLOCK_VIDEO_URL, "")
        blockId = requireArguments().getString(ARG_BLOCK_ID, "")
        if (viewModel.currentVideoTime == 0L) {
            viewModel.currentVideoTime = requireArguments().getLong(ARG_VIDEO_TIME, 0)
        }
        if (viewModel.isPlaying == null) {
            viewModel.isPlaying = requireArguments().getBoolean(ARG_IS_PLAYING)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.setOnApplyWindowInsetsListener { _, insets ->
            val insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(insets)
                .getInsets(WindowInsetsCompat.Type.systemBars())

            val statusBarParams = binding.playerView.layoutParams as FrameLayout.LayoutParams
            statusBarParams.topMargin = insetsCompat.top
            statusBarParams.bottomMargin = insetsCompat.bottom
            statusBarParams.marginStart = insetsCompat.left
            statusBarParams.marginEnd = insetsCompat.right
            binding.playerView.layoutParams = statusBarParams
            insets
        }
        binding.root.requestApplyInsetsWhenAttached()
        initPlayer()
    }

    @OptIn(UnstableApi::class)
    private fun initPlayer() {
        if (exoPlayer == null) {
            exoPlayer = buildExoPlayer()
        }
        setupPlayerView()
        setupMediaItem()
        setupPlayerListeners()
    }

    @OptIn(UnstableApi::class)
    private fun buildExoPlayer(): ExoPlayer {
        val videoQuality = viewModel.getVideoQuality()
        val trackSelector = DefaultTrackSelector(requireContext(), AdaptiveTrackSelection.Factory())
        trackSelector.parameters = DefaultTrackSelector.Parameters.Builder(requireContext()).apply {
            if (videoQuality != VideoQuality.AUTO) {
                setMaxVideoSize(videoQuality.width, videoQuality.height)
                setViewportSize(videoQuality.width, videoQuality.height, false)
            }
        }.build()

        return ExoPlayer.Builder(
            requireContext(),
            DefaultRenderersFactory(requireContext()),
            DefaultMediaSourceFactory(requireContext(), DefaultExtractorsFactory()),
            trackSelector,
            DefaultLoadControl(),
            DefaultBandwidthMeter.getSingletonInstance(requireContext()),
            DefaultAnalyticsCollector(Clock.DEFAULT)
        ).build()
    }

    @OptIn(UnstableApi::class)
    private fun setupPlayerView() {
        with(binding.playerView) {
            player = exoPlayer
            setShowNextButton(false)
            setShowPreviousButton(false)
            setFullscreenButtonClickListener {
                requireActivity().supportFragmentManager.popBackStackImmediate()
            }
        }
    }

    private fun setupMediaItem() {
        val mediaItem = MediaItem.fromUri(viewModel.videoUrl)
        setPlayerMedia(mediaItem)
        exoPlayer?.prepare()
        exoPlayer?.playWhenReady = viewModel.isPlaying ?: false
    }

    private fun setupPlayerListeners() {
        exoPlayer?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                viewModel.logPlayPauseEvent(
                    viewModel.videoUrl,
                    isPlaying,
                    viewModel.currentVideoTime,
                    CourseAnalyticsKey.NATIVE.key
                )
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (playbackState == Player.STATE_ENDED) {
                    viewModel.markBlockCompleted(blockId, CourseAnalyticsKey.NATIVE.key)
                }
            }

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                super.onPlaybackParametersChanged(playbackParameters)
                viewModel.logVideoSpeedEvent(
                    viewModel.videoUrl,
                    playbackParameters.speed,
                    viewModel.currentVideoTime,
                    CourseAnalyticsKey.NATIVE.key
                )
            }
        })
    }

    @OptIn(UnstableApi::class)
    private fun setPlayerMedia(mediaItem: MediaItem) {
        if (viewModel.videoUrl.endsWith(".m3u8")) {
            val factory = DefaultDataSource.Factory(requireContext())
            val mediaSource: HlsMediaSource =
                HlsMediaSource.Factory(factory).createMediaSource(mediaItem)
            exoPlayer?.setMediaSource(mediaSource, viewModel.currentVideoTime)
        } else {
            exoPlayer?.setMediaItem(
                mediaItem,
                viewModel.currentVideoTime
            )
        }
    }

    private fun releasePlayer() {
        exoPlayer?.stop()
        exoPlayer?.release()
        exoPlayer = null
    }

    override fun onPause() {
        super.onPause()
        exoPlayer?.removeListener(exoPlayerListener)
        exoPlayer?.pause()
    }

    override fun onDestroyView() {
        viewModel.currentVideoTime = exoPlayer?.currentPosition ?: C.TIME_UNSET
        viewModel.duration = exoPlayer?.duration ?: 0L
        viewModel.sendTime()
        super.onDestroyView()
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onDestroy() {
        releasePlayer()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        exoPlayer?.addListener(exoPlayerListener)
    }

    companion object {
        private const val ARG_BLOCK_VIDEO_URL = "blockVideoUrl"
        private const val ARG_VIDEO_TIME = "videoTime"
        private const val ARG_BLOCK_ID = "blockId"
        private const val ARG_COURSE_ID = "courseId"
        private const val ARG_IS_PLAYING = "isPlaying"

        fun newInstance(
            videoUrl: String,
            videoTime: Long,
            blockId: String,
            courseId: String,
            isPlaying: Boolean,
        ): VideoFullScreenFragment {
            val fragment = VideoFullScreenFragment()
            fragment.arguments = bundleOf(
                ARG_BLOCK_VIDEO_URL to videoUrl,
                ARG_VIDEO_TIME to videoTime,
                ARG_BLOCK_ID to blockId,
                ARG_COURSE_ID to courseId,
                ARG_IS_PLAYING to isPlaying
            )
            return fragment
        }
    }
}
