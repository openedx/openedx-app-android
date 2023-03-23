package com.raccoongang.course.presentation.unit.video

import android.graphics.Point
import android.os.Bundle
import android.view.OrientationEventListener
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.raccoongang.core.extension.computeWindowSizeClasses
import com.raccoongang.core.extension.dpToPixel
import com.raccoongang.core.presentation.global.viewBinding
import com.raccoongang.core.ui.WindowSize
import com.raccoongang.core.ui.theme.NewEdxTheme
import com.raccoongang.core.ui.theme.appColors
import com.raccoongang.course.R
import com.raccoongang.course.databinding.FragmentVideoUnitBinding
import com.raccoongang.course.presentation.CourseRouter
import com.raccoongang.course.presentation.ui.ConnectionErrorView
import com.raccoongang.course.presentation.ui.VideoRotateView
import com.raccoongang.course.presentation.ui.VideoTitle
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.roundToInt

class VideoUnitFragment : Fragment(R.layout.fragment_video_unit) {

    private val binding by viewBinding(FragmentVideoUnitBinding::bind)
    private val viewModel by viewModel<VideoUnitViewModel> {
        parametersOf(requireArguments().getString(ARG_COURSE_ID, ""))
    }
    private val router by inject<CourseRouter>()

    private var exoPlayer: ExoPlayer? = null
    private var windowSize: WindowSize? = null

    private var orientationListener: OrientationEventListener? = null
    private var blockId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        windowSize = computeWindowSizeClasses()
        lifecycle.addObserver(viewModel)
        viewModel.videoUrl = requireArguments().getString(ARG_VIDEO_URL, "")
        viewModel.isDownloaded = requireArguments().getBoolean(ARG_DOWNLOADED)
        blockId = requireArguments().getString(ARG_BLOCK_ID, "")
        orientationListener = object : OrientationEventListener(requireActivity()) {
            override fun onOrientationChanged(orientation: Int) {
                if (windowSize?.isTablet != true) {
                    if (orientation in 75..300) {
                        if (!viewModel.fullscreenHandled) {
                            router.navigateToFullScreenVideo(
                                requireActivity().supportFragmentManager,
                                viewModel.videoUrl,
                                exoPlayer?.currentPosition ?: viewModel.currentVideoTime,
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
            exoPlayer?.pause()
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
                    binding.connectionError.isVisible =
                        !viewModel.hasInternetConnection && !viewModel.isDownloaded
                }
            }
        }

        binding.connectionError.isVisible =
            !viewModel.hasInternetConnection && !viewModel.isDownloaded
        val display = requireActivity().windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val width = size.x - requireContext().dpToPixel(32)
        val minHeight = requireContext().dpToPixel(194).roundToInt()
        val height = (width / 16f * 9f).roundToInt()
        val layoutParams = binding.playerView.layoutParams as FrameLayout.LayoutParams
        layoutParams.height = if (windowSize?.isTablet == true) {
            requireContext().dpToPixel(320).roundToInt()
        } else if (height < minHeight) {
            minHeight
        } else {
            height
        }
        binding.playerView.layoutParams = layoutParams

        viewModel.isUpdated.observe(viewLifecycleOwner) { isUpdated ->
            if (isUpdated) {
                initPlayer()
            }
        }

        viewModel.isPopUpViewShow.observe(viewLifecycleOwner) {
            if (windowSize?.isTablet != true) {
                binding.cvRotateHelper.isVisible = it
            }
        }
    }

    private fun initPlayer() {
        with(binding) {
            if (exoPlayer == null) {
                exoPlayer = ExoPlayer.Builder(requireContext())
                    .build()
            }
            playerView.player = exoPlayer
            playerView.setShowNextButton(false)
            playerView.setShowPreviousButton(false)
            val mediaItem = MediaItem.fromUri(viewModel.videoUrl)
            exoPlayer?.setMediaItem(mediaItem, viewModel.currentVideoTime)
            exoPlayer?.prepare()
            exoPlayer?.playWhenReady = !(viewModel.isVideoPaused.value ?: false)

            playerView.setFullscreenButtonClickListener { isFullScreen ->
                router.navigateToFullScreenVideo(
                    requireActivity().supportFragmentManager,
                    viewModel.videoUrl,
                    exoPlayer?.currentPosition ?: 0L,
                    blockId,
                    viewModel.courseId
                )
                viewModel.fullscreenHandled = true
            }

            exoPlayer?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    if (playbackState == Player.STATE_ENDED) {
                        viewModel.markBlockCompleted(blockId)
                    }
                }
            })
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
        exoPlayer?.pause()
        orientationListener?.disable()
    }

    override fun onDestroyView() {
        exoPlayer?.release()
        exoPlayer = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_BLOCK_ID = "blockId"
        private const val ARG_VIDEO_URL = "videoUrl"
        private const val ARG_COURSE_ID = "courseId"
        private const val ARG_TITLE = "title"
        private const val ARG_DOWNLOADED = "isDownloaded"
        fun newInstance(
            blockId: String,
            courseId: String,
            videoUrl: String,
            title: String,
            isDownloaded: Boolean
        ): VideoUnitFragment {
            val fragment = VideoUnitFragment()
            fragment.arguments = bundleOf(
                ARG_BLOCK_ID to blockId,
                ARG_COURSE_ID to courseId,
                ARG_VIDEO_URL to videoUrl,
                ARG_TITLE to title,
                ARG_DOWNLOADED to isDownloaded
            )
            return fragment
        }
    }
}