package org.openedx.discovery.presentation.detail

import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import org.koin.android.ext.android.inject
import org.openedx.core.ui.OpenEdXButton
import org.openedx.core.ui.OpenEdXOutlinedButton
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.discovery.presentation.DiscoveryRouter
import org.openedx.foundation.extension.setWidthPercent
import org.openedx.core.R as coreR

class AuthorizationDialogFragment : DialogFragment() {

    private val router: DiscoveryRouter by inject()

    override fun onResume() {
        super.onResume()
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setWidthPercent(percentage = LANDSCAPE_WIDTH_PERCENT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val courseId = requireArguments().getString(ARG_COURSE_ID) ?: ""
                AuthorizationDialogView(
                    onRegisterButtonClick = {
                        router.navigateToSignUp(requireActivity().supportFragmentManager, courseId)
                        dismiss()
                    },
                    onSignInButtonClick = {
                        router.navigateToSignIn(
                            requireActivity().supportFragmentManager,
                            courseId,
                            null
                        )
                        dismiss()
                    },
                    onCancelButtonClick = {
                        dismiss()
                    }
                )
            }
        }
    }

    companion object {
        private const val ARG_COURSE_ID = "arg_course_id"
        private const val LANDSCAPE_WIDTH_PERCENT = 66
        fun newInstance(
            courseId: String,
        ): AuthorizationDialogFragment {
            val dialog = AuthorizationDialogFragment()
            dialog.arguments = bundleOf(
                ARG_COURSE_ID to courseId,
            )
            return dialog
        }
    }
}

@Composable
private fun AuthorizationDialogView(
    onRegisterButtonClick: () -> Unit,
    onSignInButtonClick: () -> Unit,
    onCancelButtonClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
        AuthorizationDialogPortraitView(
            onRegisterButtonClick = onRegisterButtonClick,
            onSignInButtonClick = onSignInButtonClick,
            onCancelButtonClick = onCancelButtonClick
        )
    } else {
        AuthorizationDialogLandscapeView(
            onRegisterButtonClick = onRegisterButtonClick,
            onSignInButtonClick = onSignInButtonClick,
            onCancelButtonClick = onCancelButtonClick
        )
    }
}

@Composable
private fun AuthorizationDialogPortraitView(
    onRegisterButtonClick: () -> Unit,
    onSignInButtonClick: () -> Unit,
    onCancelButtonClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(fraction = 0.95f)
            .clip(MaterialTheme.appShapes.courseImageShape),
        backgroundColor = MaterialTheme.appColors.background,
        shape = MaterialTheme.appShapes.courseImageShape
    ) {
        Column(
            modifier = Modifier.padding(30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.CenterEnd,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    modifier = Modifier.size(24.dp),
                    onClick = onCancelButtonClick
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(id = coreR.string.core_cancel),
                        tint = MaterialTheme.appColors.primary
                    )
                }
            }
            Icon(
                modifier = Modifier
                    .width(76.dp)
                    .height(72.dp),
                imageVector = Icons.AutoMirrored.Filled.Login,
                contentDescription = null,
                tint = MaterialTheme.appColors.onBackground
            )
            Spacer(Modifier.height(36.dp))
            Text(
                text = stringResource(id = coreR.string.core_authorization),
                color = MaterialTheme.appColors.textPrimary,
                style = MaterialTheme.appTypography.titleLarge
            )
            Spacer(Modifier.height(8.dp))
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = coreR.string.core_authorization_request),
                color = MaterialTheme.appColors.textFieldText,
                style = MaterialTheme.appTypography.titleSmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(42.dp))
            Row {
                OpenEdXOutlinedButton(
                    modifier = Modifier.weight(1f),
                    borderColor = MaterialTheme.appColors.primaryButtonBackground,
                    textColor = MaterialTheme.appColors.primaryButtonBackground,
                    text = stringResource(id = coreR.string.core_sign_in),
                    onClick = onSignInButtonClick
                )
                Spacer(Modifier.width(16.dp))
                OpenEdXButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(id = coreR.string.core_register),
                    onClick = onRegisterButtonClick
                )
            }
        }
    }
}

@Composable
private fun AuthorizationDialogLandscapeView(
    onRegisterButtonClick: () -> Unit,
    onSignInButtonClick: () -> Unit,
    onCancelButtonClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.appShapes.courseImageShape),
        backgroundColor = MaterialTheme.appColors.background,
        shape = MaterialTheme.appShapes.courseImageShape
    ) {
        Column(
            modifier = Modifier.padding(38.dp)
        ) {
            Box(
                contentAlignment = Alignment.CenterEnd,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                IconButton(
                    modifier = Modifier.size(24.dp),
                    onClick = onCancelButtonClick
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(id = coreR.string.core_cancel),
                        tint = MaterialTheme.appColors.primary
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        modifier = Modifier
                            .width(76.dp)
                            .height(72.dp),
                        imageVector = Icons.AutoMirrored.Filled.Login,
                        contentDescription = null,
                        tint = MaterialTheme.appColors.onBackground
                    )
                    Spacer(Modifier.height(36.dp))
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = coreR.string.core_authorization),
                        color = MaterialTheme.appColors.textPrimary,
                        style = MaterialTheme.appTypography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = coreR.string.core_authorization_request),
                        color = MaterialTheme.appColors.textFieldText,
                        style = MaterialTheme.appTypography.titleSmall,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.width(42.dp))
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OpenEdXOutlinedButton(
                        borderColor = MaterialTheme.appColors.primaryButtonBackground,
                        textColor = MaterialTheme.appColors.primaryButtonBackground,
                        text = stringResource(id = coreR.string.core_sign_in),
                        onClick = onSignInButtonClick,
                    )
                    Spacer(Modifier.height(16.dp))
                    OpenEdXButton(
                        text = stringResource(id = coreR.string.core_register),
                        onClick = onRegisterButtonClick
                    )
                }
            }
        }
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AuthorizationDialogPortraitViewPreview() {
    OpenEdXTheme {
        AuthorizationDialogPortraitView(
            onSignInButtonClick = {},
            onRegisterButtonClick = {},
            onCancelButtonClick = {}
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AuthorizationDialogLandscapeViewPreview() {
    OpenEdXTheme {
        AuthorizationDialogLandscapeView(
            onSignInButtonClick = {},
            onRegisterButtonClick = {},
            onCancelButtonClick = {}
        )
    }
}
