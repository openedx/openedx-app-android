package org.openedx.course.presentation.unit.video

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import org.openedx.core.extension.requestApplyInsetsWhenAttached
import org.openedx.core.presentation.global.WindowSizeHolder
import org.openedx.core.presentation.global.viewBinding
import org.openedx.course.R
import org.openedx.course.databinding.FragmentVideoFullScreenBinding
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class VideoFullScreenFragment : Fragment(R.layout.fragment_video_full_screen) {

    private val binding by viewBinding(FragmentVideoFullScreenBinding::bind)
    private val viewModel by viewModel<VideoViewModel> {
        parametersOf(requireArguments().getString(ARG_COURSE_ID, ""))
    }

    private var exoPlayer: ExoPlayer? = null
    private var blockId = ""
    private var isTabletDevice = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.videoUrl = requireArguments().getString(ARG_BLOCK_VIDEO_URL, "")
        blockId = requireArguments().getString(ARG_BLOCK_ID, "")
        if (viewModel.currentVideoTime == 0L) {
            viewModel.currentVideoTime = requireArguments().getLong(ARG_VIDEO_TIME, 0)
        }
        setOrientationBasedOnDeviceType()
    }

    private fun setOrientationBasedOnDeviceType() {
        val windowSize = (requireActivity() as WindowSizeHolder).windowSize
        isTabletDevice = windowSize.isTablet
        if (!isTabletDevice) {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
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
            exoPlayer?.setMediaItem(mediaItem, viewModel.currentVideoTime.toLong())
            exoPlayer?.prepare()
            exoPlayer?.playWhenReady = false

            playerView.setFullscreenButtonClickListener { isFullScreen ->
                requireActivity().supportFragmentManager.popBackStackImmediate()
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

    private fun releasePlayer() {
        exoPlayer?.stop()
        exoPlayer?.release()
        exoPlayer = null
    }


    override fun onPause() {
        super.onPause()
        if (exoPlayer?.isPlaying == true) {
            exoPlayer?.pause()
            exoPlayer?.playWhenReady = false
        }
    }

    override fun onDestroyView() {
        viewModel.currentVideoTime = exoPlayer?.currentPosition ?: C.TIME_UNSET
        viewModel.sendTime()
        super.onDestroyView()
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onDestroy() {
        if (!isTabletDevice) {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        releasePlayer()
        super.onDestroy()
    }

    companion object {
        private const val ARG_BLOCK_VIDEO_URL = "blockVideoUrl"
        private const val ARG_VIDEO_TIME = "videoTime"
        private const val ARG_BLOCK_ID = "blockId"
        private const val ARG_COURSE_ID = "courseId"

        fun newInstance(
            videoUrl: String,
            videoTime: Long,
            blockId: String,
            courseId: String
        ): VideoFullScreenFragment {
            val fragment = VideoFullScreenFragment()
            fragment.arguments = bundleOf(
                ARG_BLOCK_VIDEO_URL to videoUrl,
                ARG_VIDEO_TIME to videoTime,
                ARG_BLOCK_ID to blockId,
                ARG_COURSE_ID to courseId
            )
            return fragment
        }
    }

}