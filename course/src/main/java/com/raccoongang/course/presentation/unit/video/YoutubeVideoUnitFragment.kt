package com.raccoongang.course.presentation.unit.video

import android.os.Bundle
import android.view.OrientationEventListener
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.ui.DefaultPlayerUiController
import com.raccoongang.core.extension.computeWindowSizeClasses
import com.raccoongang.core.presentation.global.viewBinding
import com.raccoongang.core.ui.WindowSize
import com.raccoongang.core.ui.theme.NewEdxTheme
import com.raccoongang.core.ui.theme.appColors
import com.raccoongang.course.R
import com.raccoongang.course.databinding.FragmentYoutubeVideoUnitBinding
import com.raccoongang.course.presentation.CourseRouter
import com.raccoongang.course.presentation.ui.ConnectionErrorView
import com.raccoongang.course.presentation.ui.VideoRotateView
import com.raccoongang.course.presentation.ui.VideoTitle
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class YoutubeVideoUnitFragment : Fragment(R.layout.fragment_youtube_video_unit) {

    private val viewModel by viewModel<VideoUnitViewModel> {
        parametersOf(requireArguments().getString(ARG_COURSE_ID, ""))
    }
    private val router by inject<CourseRouter>()
    private val binding by viewBinding(FragmentYoutubeVideoUnitBinding::bind)

    private var windowSize: WindowSize? = null
    private var orientationListener: OrientationEventListener? = null
    private var _youTubePlayer: YouTubePlayer? = null

    private var blockId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        windowSize = computeWindowSizeClasses()
        lifecycle.addObserver(viewModel)
        viewModel.videoUrl = requireArguments().getString(ARG_VIDEO_URL, "")
        blockId = requireArguments().getString(ARG_BLOCK_ID, "")
        orientationListener = object : OrientationEventListener(requireActivity()) {
            override fun onOrientationChanged(orientation: Int) {
                if (windowSize?.isTablet != true) {
                    if (orientation in 75..300) {
                        if (!viewModel.fullscreenHandled) {
                            router.navigateToFullScreenYoutubeVideo(
                                requireActivity().supportFragmentManager,
                                viewModel.videoUrl,
                                viewModel.currentVideoTime,
                                blockId,
                                viewModel.courseId
                            )
                            viewModel.fullscreenHandled = true
                        }
                    } else {
                        viewModel.fullscreenHandled = false
                    }
                }
            }
        }

        viewModel.isVideoPaused.observe(this) {
            _youTubePlayer?.pause()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cvRotateHelper.setContent {
            NewEdxTheme {
                VideoRotateView()
            }
        }

        binding.cvVideoTitle.setContent {
            NewEdxTheme {
                VideoTitle(text = requireArguments().getString(ARG_TITLE) ?: "")
            }
        }

        binding.connectionError.setContent {
            NewEdxTheme {
                ConnectionErrorView(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.appColors.background)
                ) {
                    binding.connectionError.isVisible = !viewModel.hasInternetConnection
                }
            }
        }

        binding.connectionError.isVisible = !viewModel.hasInternetConnection

        lifecycle.addObserver(binding.youtubePlayerView)

        val options = IFramePlayerOptions.Builder()
            .controls(0)
            .rel(0)
            .build()

        val listener = object : AbstractYouTubePlayerListener() {
            override fun onStateChange(
                youTubePlayer: YouTubePlayer,
                state: PlayerConstants.PlayerState
            ) {
                super.onStateChange(youTubePlayer, state)
                if (state == PlayerConstants.PlayerState.ENDED) {
                    viewModel.markBlockCompleted(blockId)
                }
            }

            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                super.onCurrentSecond(youTubePlayer, second)
                viewModel.currentVideoTime = second.toLong()
            }

            override fun onReady(youTubePlayer: YouTubePlayer) {
                super.onReady(youTubePlayer)
                _youTubePlayer = youTubePlayer
                val defPlayerUiController = DefaultPlayerUiController(
                    binding.youtubePlayerView,
                    youTubePlayer
                )
                defPlayerUiController.setFullScreenButtonClickListener {
                    viewModel.fullscreenHandled = true
                    router.navigateToFullScreenYoutubeVideo(
                        requireActivity().supportFragmentManager,
                        viewModel.videoUrl,
                        viewModel.currentVideoTime,
                        blockId,
                        viewModel.courseId
                    )
                }
                binding.youtubePlayerView.setCustomPlayerUi(defPlayerUiController.rootView)

                val videoId = viewModel.videoUrl.split("watch?v=")[1]
                youTubePlayer.loadVideo(videoId, viewModel.currentVideoTime.toFloat())
                if (viewModel.isVideoPaused.value == true) {
                    youTubePlayer.pause()
                }
            }
        }

        binding.youtubePlayerView.initialize(listener, options)

        viewModel.isPopUpViewShow.observe(viewLifecycleOwner) {
            if (windowSize?.isTablet != true) {
                binding.cvRotateHelper.isVisible = it
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (orientationListener?.canDetectOrientation() == true) {
            orientationListener?.enable()
        }
    }

    override fun onPause() {
        super.onPause()
        orientationListener?.disable()
    }

    companion object {
        private const val ARG_VIDEO_URL = "videoUrl"
        private const val ARG_BLOCK_ID = "blockId"
        private const val ARG_COURSE_ID = "courseId"
        private const val ARG_TITLE = "blockTitle"
        fun newInstance(
            blockId: String,
            courseId: String,
            videoUrl: String,
            blockTitle: String
        ): YoutubeVideoUnitFragment {
            val fragment = YoutubeVideoUnitFragment()
            fragment.arguments = bundleOf(
                ARG_VIDEO_URL to videoUrl,
                ARG_BLOCK_ID to blockId,
                ARG_COURSE_ID to courseId,
                ARG_TITLE to blockTitle
            )
            return fragment
        }
    }
}
