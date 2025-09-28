package org.openedx.course.presentation.unit.video

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.YouTubePlayerTracker
import com.pierfrancescosoffritti.androidyoutubeplayer.core.ui.DefaultPlayerUiController
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.presentation.dialog.appreview.AppReviewManager
import org.openedx.core.presentation.global.viewBinding
import org.openedx.course.R
import org.openedx.course.databinding.FragmentYoutubeVideoFullScreenBinding
import org.openedx.course.presentation.CourseAnalyticsKey
import org.openedx.course.presentation.unit.video.YoutubeVideoUnitFragment.Companion.RATE_DIALOG_THRESHOLD
import org.openedx.course.presentation.unit.video.YoutubeVideoUnitFragment.Companion.VIDEO_COMPLETION_THRESHOLD
import org.openedx.foundation.extension.requestApplyInsetsWhenAttached

class YoutubeVideoFullScreenFragment : Fragment(R.layout.fragment_youtube_video_full_screen) {

    private val binding by viewBinding(FragmentYoutubeVideoFullScreenBinding::bind)
    private val viewModel by viewModel<VideoViewModel> {
        parametersOf(requireArguments().getString(ARG_COURSE_ID, ""))
    }
    private val appReviewManager by inject<AppReviewManager> { parametersOf(requireActivity()) }

    private var blockId = ""

    private val youtubeTrackerListener = YouTubePlayerTracker()

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

            val statusBarParams = binding.youtubePlayerView.layoutParams as FrameLayout.LayoutParams
            statusBarParams.topMargin = insetsCompat.top
            statusBarParams.bottomMargin = insetsCompat.bottom
            statusBarParams.marginStart = insetsCompat.left
            statusBarParams.marginEnd = insetsCompat.right
            binding.youtubePlayerView.layoutParams = statusBarParams
            insets
        }
        binding.root.requestApplyInsetsWhenAttached()

        lifecycle.addObserver(binding.youtubePlayerView)
        val options = IFramePlayerOptions.Builder()
            .controls(0)
            .rel(0)
            .build()

        binding.youtubePlayerView.initialize(
            object : AbstractYouTubePlayerListener() {
                var isMarkBlockCompletedCalled = false

                override fun onStateChange(
                    youTubePlayer: YouTubePlayer,
                    state: PlayerConstants.PlayerState,
                ) {
                    super.onStateChange(youTubePlayer, state)
                    if (state == PlayerConstants.PlayerState.ENDED) {
                        viewModel.markBlockCompleted(blockId, CourseAnalyticsKey.YOUTUBE.key)
                    }
                    viewModel.isPlaying = when (state) {
                        PlayerConstants.PlayerState.PLAYING -> true
                        PlayerConstants.PlayerState.PAUSED -> false
                        else -> return
                    }
                }

                override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                    super.onCurrentSecond(youTubePlayer, second)
                    viewModel.currentVideoTime = (second * 1000f).toLong()
                    val completePercentage = second / youtubeTrackerListener.videoDuration
                    if (completePercentage >= VIDEO_COMPLETION_THRESHOLD && !isMarkBlockCompletedCalled) {
                        viewModel.markBlockCompleted(blockId, CourseAnalyticsKey.YOUTUBE.key)
                        isMarkBlockCompletedCalled = true
                    }
                    if (completePercentage >= RATE_DIALOG_THRESHOLD && !appReviewManager.isDialogShowed) {
                        if (!appReviewManager.isDialogShowed) {
                            appReviewManager.tryToOpenRateDialog()
                        }
                    }
                }

                override fun onReady(youTubePlayer: YouTubePlayer) {
                    super.onReady(youTubePlayer)
                    binding.youtubePlayerView.isVisible = true
                    val defPlayerUiController =
                        DefaultPlayerUiController(binding.youtubePlayerView, youTubePlayer)
                    defPlayerUiController.setFullScreenButtonClickListener {
                        parentFragmentManager.popBackStack()
                    }

                    binding.youtubePlayerView.setCustomPlayerUi(defPlayerUiController.rootView)

                    val videoId = viewModel.videoUrl.split("watch?v=")[1]
                    if (viewModel.isPlaying == true) {
                        youTubePlayer.loadVideo(videoId, viewModel.currentVideoTime.toFloat() / 1000)
                    } else {
                        youTubePlayer.cueVideo(videoId, viewModel.currentVideoTime.toFloat() / 1000)
                    }
                    youTubePlayer.addListener(youtubeTrackerListener)
                }

                override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                    viewModel.duration = (duration * 1000).toLong()
                    super.onVideoDuration(youTubePlayer, duration)
                }
            },
            options
        )
    }

    override fun onDestroyView() {
        viewModel.sendTime()
        super.onDestroyView()
    }

    companion object {
        private const val ARG_BLOCK_VIDEO_URL = "blockVideoUrl"
        private const val ARG_VIDEO_TIME = "videoTime"
        private const val ARG_BLOCK_ID = "blockID"
        private const val ARG_COURSE_ID = "courseId"
        private const val ARG_IS_PLAYING = "isPlaying"

        fun newInstance(
            videoUrl: String,
            videoTime: Long,
            blockId: String,
            courseId: String,
            isPlaying: Boolean,
        ): YoutubeVideoFullScreenFragment {
            val fragment = YoutubeVideoFullScreenFragment()
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
