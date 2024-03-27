package org.openedx.course.presentation.unit.video

import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.media3.cast.SessionAvailabilityListener
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.window.layout.WindowMetricsCalculator
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.extension.computeWindowSizeClasses
import org.openedx.core.extension.dpToPixel
import org.openedx.core.extension.objectToString
import org.openedx.core.extension.stringToObject
import org.openedx.core.presentation.dialog.appreview.AppReviewManager
import org.openedx.core.presentation.dialog.selectorbottomsheet.SelectBottomDialogFragment
import org.openedx.core.presentation.global.viewBinding
import org.openedx.core.ui.ConnectionErrorView
import org.openedx.core.ui.WindowSize
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.utils.LocaleUtils
import org.openedx.course.R
import org.openedx.course.databinding.FragmentVideoUnitBinding
import org.openedx.course.presentation.CourseAnalyticsEvent
import org.openedx.course.presentation.CourseAnalyticsKey
import org.openedx.course.presentation.CourseRouter
import org.openedx.course.presentation.ui.VideoSubtitles
import org.openedx.course.presentation.ui.VideoTitle
import kotlin.math.roundToInt

class VideoUnitFragment : Fragment(R.layout.fragment_video_unit) {

    private val binding by viewBinding(FragmentVideoUnitBinding::bind)
    private val viewModel by viewModel<EncodedVideoUnitViewModel> {
        parametersOf(
            requireArguments().getString(ARG_COURSE_ID, ""),
            requireArguments().getString(ARG_BLOCK_ID, ""),
        )
    }
    private val router by inject<CourseRouter>()
    private val appReviewManager by inject<AppReviewManager> { parametersOf(requireActivity()) }

    private var windowSize: WindowSize? = null

    private val handler = Handler(Looper.getMainLooper())
    private var videoTimeRunnable: Runnable = object : Runnable {
        override fun run() {
            viewModel.getActivePlayer()?.let {
                if (it.isPlaying) {
                    viewModel.setCurrentVideoTime(it.currentPosition)
                }
                val completePercentage = it.currentPosition.toDouble() / it.duration.toDouble()
                if (completePercentage >= 0.8f) {
                    viewModel.markBlockCompleted(viewModel.blockId, CourseAnalyticsKey.NATIVE.key)
                }
            }
            handler.postDelayed(this, 200)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        windowSize = computeWindowSizeClasses()
        lifecycle.addObserver(viewModel)
        handler.post(videoTimeRunnable)
        requireArguments().apply {
            viewModel.videoUrl = getString(ARG_VIDEO_URL, "")
            viewModel.transcripts = stringToObject<Map<String, String>>(
                getString(ARG_TRANSCRIPT_URL, "")
            ) ?: emptyMap()
            viewModel.isDownloaded = getBoolean(ARG_DOWNLOADED)
        }
        viewModel.downloadSubtitles()
        handler.removeCallbacks(videoTimeRunnable)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cvVideoTitle?.setContent {
            OpenEdXTheme {
                VideoTitle(text = requireArguments().getString(ARG_TITLE) ?: "")
            }
        }

        binding.connectionError.setContent {
            OpenEdXTheme {
                ConnectionErrorView(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.appColors.background)
                ) {
                    binding.connectionError.isVisible =
                        !viewModel.hasInternetConnection && !viewModel.isDownloaded
                }
            }
        }

        binding.subtitles.setContent {
            OpenEdXTheme {
                val state = rememberLazyListState()
                val currentIndex by viewModel.currentIndex.collectAsState(0)
                val transcriptObject by viewModel.transcriptObject.observeAsState()
                VideoSubtitles(
                    listState = state,
                    timedTextObject = transcriptObject,
                    subtitleLanguage = LocaleUtils.getDisplayLanguage(viewModel.transcriptLanguage),
                    showSubtitleLanguage = viewModel.transcripts.size > 1,
                    currentIndex = currentIndex,
                    onTranscriptClick = {
                        viewModel.getActivePlayer()?.apply {
                            seekTo(it.start.mseconds.toLong())
                            play()
                        }
                    },
                    onSettingsClick = {
                        viewModel.getActivePlayer()?.pause()
                        val dialog = SelectBottomDialogFragment.newInstance(
                            LocaleUtils.getLanguages(viewModel.transcripts.keys.toList())
                        )
                        dialog.show(
                            requireActivity().supportFragmentManager,
                            SelectBottomDialogFragment::class.simpleName
                        )
                    }
                )
            }
        }

        binding.connectionError.isVisible =
            !viewModel.hasInternetConnection && !viewModel.isDownloaded

        val orientation = resources.configuration.orientation
        val windowMetrics =
            WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(requireActivity())
        val currentBounds = windowMetrics.bounds
        val layoutParams = binding.playerView.layoutParams as FrameLayout.LayoutParams
        if (orientation == Configuration.ORIENTATION_PORTRAIT || windowSize?.isTablet == true) {
            val width = currentBounds.width() - requireContext().dpToPixel(32)
            val minHeight = requireContext().dpToPixel(194).roundToInt()
            val height = (width / 16f * 9f).roundToInt()
            layoutParams.height = if (windowSize?.isTablet == true) {
                requireContext().dpToPixel(320).roundToInt()
            } else if (height < minHeight) {
                minHeight
            } else {
                height
            }
        }

        binding.playerView.layoutParams = layoutParams

        viewModel.isUpdated.observe(viewLifecycleOwner) { isUpdated ->
            if (isUpdated) {
                initPlayer()
            }
        }

        viewModel.isVideoEnded.observe(viewLifecycleOwner) { isVideoEnded ->
            if (isVideoEnded && !appReviewManager.isDialogShowed) {
                appReviewManager.tryToOpenRateDialog()
            }
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun initPlayer() {
        with(binding) {
            playerView.player = viewModel.getActivePlayer()
            playerView.setShowNextButton(false)
            playerView.setShowPreviousButton(false)
            showVideoControllerIndefinitely(false)

            val movieMetadata = MediaMetadata.Builder()
                .setMediaType(MediaMetadata.MEDIA_TYPE_MOVIE)
                .build()
            val mediaItem = MediaItem.Builder().setMediaMetadata(movieMetadata)
                .setUri(viewModel.videoUrl)
                .setMimeType("video/*")
                .build()

            if (!viewModel.isPlayerSetUp) {
                setPlayerMedia(mediaItem)
                viewModel.getActivePlayer()?.prepare()
                viewModel.getActivePlayer()?.playWhenReady = viewModel.isPlaying
                viewModel.isPlayerSetUp = true
            }

            viewModel.castPlayer?.setSessionAvailabilityListener(
                object : SessionAvailabilityListener {
                    override fun onCastSessionAvailable() {
                        viewModel.logCastConnection(CourseAnalyticsEvent.CAST_CONNECTED)
                        viewModel.isCastActive = true
                        viewModel.exoPlayer?.pause()
                        playerView.player = viewModel.castPlayer
                        viewModel.castPlayer?.setMediaItem(
                            mediaItem,
                            viewModel.exoPlayer?.currentPosition ?: 0L
                        )
                        viewModel.castPlayer?.playWhenReady = false
                        showVideoControllerIndefinitely(true)
                    }

                    override fun onCastSessionUnavailable() {
                        viewModel.logCastConnection(CourseAnalyticsEvent.CAST_DISCONNECTED)
                        viewModel.isCastActive = false
                        playerView.player = viewModel.exoPlayer
                        viewModel.exoPlayer?.seekTo(viewModel.castPlayer?.currentPosition ?: 0L)
                        viewModel.castPlayer?.stop()
                        viewModel.exoPlayer?.play()
                        showVideoControllerIndefinitely(false)
                    }
                }
            )

            playerView.setFullscreenButtonClickListener {
                if (viewModel.isCastActive)
                    return@setFullscreenButtonClickListener

                router.navigateToFullScreenVideo(
                    requireActivity().supportFragmentManager,
                    viewModel.videoUrl,
                    viewModel.exoPlayer?.currentPosition ?: 0L,
                    viewModel.blockId,
                    viewModel.courseId,
                    viewModel.isPlaying
                )
            }
        }
    }

    @UnstableApi
    override fun onDestroy() {
        if (!requireActivity().isChangingConfigurations) {
            viewModel.releasePlayers()
            viewModel.isPlayerSetUp = false
        }
        handler.removeCallbacks(videoTimeRunnable)
        super.onDestroy()
    }

    @UnstableApi
    private fun showVideoControllerIndefinitely(show: Boolean) {
        if (show) {
            binding.playerView.controllerAutoShow = false
            binding.playerView.controllerShowTimeoutMs = 0
            binding.playerView.showController()
        } else {
            binding.playerView.controllerAutoShow = true
            binding.playerView.controllerShowTimeoutMs = 2000
        }
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun setPlayerMedia(mediaItem: MediaItem) {
        if (viewModel.videoUrl.endsWith(".m3u8")) {
            val factory = DefaultDataSource.Factory(requireContext())
            val mediaSource: HlsMediaSource =
                HlsMediaSource.Factory(factory).createMediaSource(mediaItem)
            viewModel.exoPlayer?.setMediaSource(mediaSource, viewModel.getCurrentVideoTime())
        } else {
            viewModel.getActivePlayer()?.setMediaItem(
                mediaItem,
                viewModel.getCurrentVideoTime()
            )
        }
    }

    companion object {
        private const val ARG_BLOCK_ID = "blockId"
        private const val ARG_VIDEO_URL = "videoUrl"
        private const val ARG_TRANSCRIPT_URL = "transcriptUrl"
        private const val ARG_COURSE_ID = "courseId"
        private const val ARG_TITLE = "title"
        private const val ARG_DOWNLOADED = "isDownloaded"

        fun newInstance(
            blockId: String,
            courseId: String,
            videoUrl: String,
            transcriptsUrl: Map<String, String>,
            title: String,
            isDownloaded: Boolean,
        ): VideoUnitFragment {
            val fragment = VideoUnitFragment()
            fragment.arguments = bundleOf(
                ARG_BLOCK_ID to blockId,
                ARG_COURSE_ID to courseId,
                ARG_VIDEO_URL to videoUrl,
                ARG_TRANSCRIPT_URL to objectToString(transcriptsUrl),
                ARG_TITLE to title,
                ARG_DOWNLOADED to isDownloaded
            )
            return fragment
        }
    }
}
