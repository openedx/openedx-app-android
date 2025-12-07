package org.openedx.course.presentation.unit.video

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.pierfrancescosoffritti.androidyoutubeplayer.core.customui.DefaultPlayerUiController
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.YouTubePlayerTracker
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.presentation.dialog.appreview.AppReviewManager
import org.openedx.core.presentation.dialog.selectorbottomsheet.SelectBottomDialogFragment
import org.openedx.core.ui.ConnectionErrorView
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.utils.LocaleUtils
import org.openedx.course.R
import org.openedx.course.databinding.FragmentYoutubeVideoUnitBinding
import org.openedx.course.presentation.CourseAnalyticsKey
import org.openedx.course.presentation.CourseRouter
import org.openedx.course.presentation.ui.VideoSubtitles
import org.openedx.course.presentation.ui.VideoTitle
import org.openedx.foundation.extension.computeWindowSizeClasses
import org.openedx.foundation.extension.objectToString
import org.openedx.foundation.extension.stringToObject
import org.openedx.foundation.presentation.WindowSize

class YoutubeVideoUnitFragment : Fragment(R.layout.fragment_youtube_video_unit) {

    private val viewModel by viewModel<VideoUnitViewModel> {
        parametersOf(
            requireArguments().getString(ARG_COURSE_ID, ""),
            requireArguments().getString(ARG_VIDEO_URL, ""),
            requireArguments().getString(ARG_BLOCK_ID, ""),
        )
    }
    private val router by inject<CourseRouter>()
    private val appReviewManager by inject<AppReviewManager> { parametersOf(requireActivity()) }

    private var _binding: FragmentYoutubeVideoUnitBinding? = null
    private val binding get() = _binding!!

    private var windowSize: WindowSize? = null
    private var _youTubePlayer: YouTubePlayer? = null

    private var blockId = ""

    private var isPlayerInitialized = false

    private val youtubeTrackerListener = YouTubePlayerTracker()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        windowSize = computeWindowSizeClasses()
        lifecycle.addObserver(viewModel)
        requireArguments().apply {
            viewModel.transcripts = stringToObject<Map<String, String>>(
                getString(ARG_TRANSCRIPT_URL, "")
            ) ?: emptyMap()
            blockId = getString(ARG_BLOCK_ID, "")
        }
        viewModel.downloadSubtitles()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentYoutubeVideoUnitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.isPlaying) {
            _youTubePlayer?.play()
        }
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
                ConnectionErrorView {
                    binding.connectionError.isVisible = !viewModel.hasInternetConnection
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
                        _youTubePlayer?.apply {
                            seekTo(it.start.mseconds / 1000f)
                            play()
                        }
                    },
                    onSettingsClick = {
                        _youTubePlayer?.pause()
                        val dialog =
                            SelectBottomDialogFragment.newInstance(
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

        binding.connectionError.isVisible = !viewModel.hasInternetConnection

        lifecycle.addObserver(binding.youtubePlayerView)

        val options = IFramePlayerOptions.Builder(requireContext())
            .controls(0)
            .rel(0)
            .build()

        val listener = object : AbstractYouTubePlayerListener() {
            var isMarkBlockCompletedCalled = false

            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                super.onCurrentSecond(youTubePlayer, second)
                viewModel.setCurrentVideoTime((second * 1000f).toLong())
                val completePercentage = second / youtubeTrackerListener.videoDuration
                if (completePercentage >= VIDEO_COMPLETION_THRESHOLD && !isMarkBlockCompletedCalled) {
                    viewModel.markBlockCompleted(blockId, CourseAnalyticsKey.YOUTUBE.key)
                    isMarkBlockCompletedCalled = true
                }
                if (completePercentage >= RATE_DIALOG_THRESHOLD && !appReviewManager.isDialogShowed) {
                    appReviewManager.tryToOpenRateDialog()
                }
            }

            override fun onStateChange(
                youTubePlayer: YouTubePlayer,
                state: PlayerConstants.PlayerState,
            ) {
                super.onStateChange(youTubePlayer, state)
                viewModel.isPlaying = when (state) {
                    PlayerConstants.PlayerState.PLAYING -> true
                    PlayerConstants.PlayerState.PAUSED -> false
                    else -> return
                }
                viewModel.logPlayPauseEvent(
                    viewModel.videoUrl,
                    viewModel.isPlaying,
                    viewModel.getCurrentVideoTime(),
                    CourseAnalyticsKey.YOUTUBE.key
                )
            }

            override fun onReady(youTubePlayer: YouTubePlayer) {
                super.onReady(youTubePlayer)
                _youTubePlayer = youTubePlayer
                if (_binding != null) {
                    val defPlayerUiController = DefaultPlayerUiController(
                        binding.youtubePlayerView,
                        youTubePlayer
                    )
                    defPlayerUiController.setFullscreenButtonClickListener {
                        router.navigateToFullScreenYoutubeVideo(
                            requireActivity().supportFragmentManager,
                            viewModel.videoUrl,
                            viewModel.getCurrentVideoTime(),
                            blockId,
                            viewModel.courseId,
                            viewModel.isPlaying
                        )
                    }
                    binding.youtubePlayerView.setCustomPlayerUi(defPlayerUiController.rootView)
                }

                viewModel.videoUrl.split("watch?v=").getOrNull(1)?.let { videoId ->
                    if (viewModel.isPlaying && isResumed) {
                        youTubePlayer.loadVideo(
                            videoId,
                            viewModel.getCurrentVideoTime().toFloat() / 1000
                        )
                    } else {
                        youTubePlayer.cueVideo(
                            videoId,
                            viewModel.getCurrentVideoTime().toFloat() / 1000
                        )
                    }
                }
                youTubePlayer.addListener(youtubeTrackerListener)
                viewModel.logLoadedCompletedEvent(
                    viewModel.videoUrl,
                    true,
                    viewModel.getCurrentVideoTime(),
                    CourseAnalyticsKey.YOUTUBE.key
                )
            }

            override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                viewModel.duration = (duration * 1000).toLong()
                super.onVideoDuration(youTubePlayer, duration)
            }
        }

        if (!isPlayerInitialized) {
            binding.youtubePlayerView.initialize(listener, options)
            isPlayerInitialized = true
        }
    }

    override fun onPause() {
        super.onPause()
        _youTubePlayer?.pause()
    }

    override fun onDestroyView() {
        isPlayerInitialized = false
        _youTubePlayer = null
        super.onDestroyView()
        _binding = null
    }

    companion object {

        private const val ARG_VIDEO_URL = "videoUrl"
        private const val ARG_TRANSCRIPT_URL = "transcriptUrl"
        private const val ARG_BLOCK_ID = "blockId"
        private const val ARG_COURSE_ID = "courseId"
        private const val ARG_TITLE = "blockTitle"

        const val VIDEO_COMPLETION_THRESHOLD = 0.8f
        const val RATE_DIALOG_THRESHOLD = 0.99f

        fun newInstance(
            blockId: String,
            courseId: String,
            videoUrl: String,
            transcriptsUrl: Map<String, String>,
            blockTitle: String,
        ): YoutubeVideoUnitFragment {
            val fragment = YoutubeVideoUnitFragment()
            fragment.arguments = bundleOf(
                ARG_VIDEO_URL to videoUrl,
                ARG_TRANSCRIPT_URL to objectToString(transcriptsUrl),
                ARG_BLOCK_ID to blockId,
                ARG_COURSE_ID to courseId,
                ARG_TITLE to blockTitle
            )
            return fragment
        }
    }
}
