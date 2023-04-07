@file:OptIn(ExperimentalComposeUiApi::class)

package com.raccoongang.profile.presentation.edit

import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Report
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import coil.compose.rememberAsyncImagePainter
import com.raccoongang.core.AppDataConstants.DEFAULT_MIME_TYPE
import com.raccoongang.core.R
import com.raccoongang.core.UIMessage
import com.raccoongang.core.domain.model.Account
import com.raccoongang.core.domain.model.LanguageProficiency
import com.raccoongang.core.domain.model.ProfileImage
import com.raccoongang.core.domain.model.RegistrationField
import com.raccoongang.core.extension.getFileName
import com.raccoongang.core.extension.parcelable
import com.raccoongang.core.ui.*
import com.raccoongang.core.ui.theme.*
import com.raccoongang.core.utils.LocaleUtils
import com.raccoongang.profile.presentation.ProfileRouter
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import com.raccoongang.profile.R as profileR

private const val BIO_TEXT_FIELD_LIMIT = 300

class EditProfileFragment : Fragment() {

    private val viewModel by viewModel<EditProfileViewModel> {
        parametersOf(requireArguments().parcelable<Account>(ARG_ACCOUNT)!!)
    }

    private val router by inject<ProfileRouter>()

    private val registerForActivityResult =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                viewModel.setImageUri(cropImage(it))
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callback = requireActivity()
            .onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (viewModel.profileDataChanged) {
                        viewModel.setShowLeaveDialog(true)
                    } else {
                        requireActivity().supportFragmentManager.popBackStack()
                    }
                }
            })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            NewEdxTheme {
                val windowSize = rememberWindowSize()

                val uiState by viewModel.uiState.observeAsState(
                    EditProfileUIState(
                        viewModel.account,
                        isLimited = viewModel.isLimitedProfile
                    )
                )
                val uiMessage by viewModel.uiMessage.observeAsState()
                val selectedImageUri by viewModel.selectedImageUri.observeAsState()
                val isImageDeleted by viewModel.deleteImage.observeAsState(false)
                val leaveDialog by viewModel.showLeaveDialog.observeAsState(false)

                EditProfileScreen(
                    windowSize = windowSize,
                    uiState = uiState,
                    uiMessage = uiMessage,
                    selectedImageUri = selectedImageUri,
                    isImageDeleted = isImageDeleted,
                    leaveDialog = leaveDialog,
                    onBackClick = {
                        if (it) {
                            viewModel.setShowLeaveDialog(true)
                        } else {
                            viewModel.setShowLeaveDialog(false)
                            requireActivity().supportFragmentManager.popBackStackImmediate()
                        }
                    },
                    onSaveClick = { fields ->
                        if (selectedImageUri == null) {
                            viewModel.updateAccount(fields)
                        } else {
                            selectedImageUri?.let {
                                requireContext().contentResolver.openInputStream(it)
                                    .use { stream ->
                                        val file = File(
                                            requireContext().cacheDir,
                                            requireContext().contentResolver.getFileName(it)
                                        )
                                        val mimeType = requireContext().contentResolver.getType(it)
                                            ?: DEFAULT_MIME_TYPE
                                        stream?.copyTo(FileOutputStream(file))
                                        viewModel.updateAccountAndImage(fields, file, mimeType)
                                    }
                            }
                        }
                    },
                    onDataChanged = {
                        viewModel.profileDataChanged = it
                    },
                    onKeepEdit = {
                        viewModel.setShowLeaveDialog(false)
                    },
                    onDeleteClick = {
                        router.navigateToDeleteAccount(
                            requireActivity().supportFragmentManager
                        )
                    },
                    onSelectImageClick = {
                        registerForActivityResult.launch("image/*")
                    },
                    onDeleteImageClick = {
                        viewModel.deleteImage()
                    },
                    onLimitedProfileChange = {
                        viewModel.isLimitedProfile = it
                    }
                )
            }
        }
    }

    private fun cropImage(uri: Uri): Uri {
        val matrix = Matrix()
        matrix.postRotate(getImageOrientation(uri).toFloat())
        val originalBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(
                ImageDecoder.createSource(
                    requireContext().contentResolver,
                    uri
                )
            )
        } else {
            MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
        }
        val rotatedBitmap = Bitmap.createBitmap(
            originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true
        )
        val newFile = File.createTempFile(
            "Image_${System.currentTimeMillis()}", ".jpg",
            requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        )

        val ratio: Float = rotatedBitmap.width.toFloat() / 500
        val newBitmap = Bitmap.createScaledBitmap(
            rotatedBitmap,
            500,
            (rotatedBitmap.height.toFloat() / ratio).toInt(),
            false
        )
        val bos = ByteArrayOutputStream()
        newBitmap.compress(Bitmap.CompressFormat.JPEG, 90, bos)
        val bitmapData = bos.toByteArray()

        val fos = FileOutputStream(newFile)
        fos.write(bitmapData)
        fos.flush()
        fos.close()
        //TODO: get applicationId instead of packageName
        return FileProvider.getUriForFile(
            requireContext(), requireContext().packageName + ".fileprovider",
            newFile
        )!!
    }

    private fun getImageOrientation(uri: Uri): Int {
        var rotation = 0
        val exif = ExifInterface(requireActivity().contentResolver.openInputStream(uri)!!)
        when (exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )) {
            ExifInterface.ORIENTATION_ROTATE_270 -> rotation = 270
            ExifInterface.ORIENTATION_ROTATE_180 -> rotation = 180
            ExifInterface.ORIENTATION_ROTATE_90 -> rotation = 90
        }
        return rotation
    }


    companion object {
        private const val ARG_ACCOUNT = "argAccount"
        fun newInstance(account: Account): EditProfileFragment {
            val fragment = EditProfileFragment()
            fragment.arguments = bundleOf(ARG_ACCOUNT to account)
            return fragment
        }
    }

}

@OptIn(
    ExperimentalMaterialApi::class,
    ExperimentalComposeUiApi::class
)
@Composable
private fun EditProfileScreen(
    windowSize: WindowSize,
    uiState: EditProfileUIState,
    uiMessage: UIMessage?,
    selectedImageUri: Uri?,
    isImageDeleted: Boolean,
    leaveDialog: Boolean,
    onKeepEdit: () -> Unit,
    onDataChanged: (Boolean) -> Unit,
    onLimitedProfileChange: (Boolean) -> Unit,
    onBackClick: (Boolean) -> Unit,
    onSaveClick: (Map<String, Any?>) -> Unit,
    onDeleteClick: () -> Unit,
    onSelectImageClick: () -> Unit,
    onDeleteImageClick: () -> Unit,
) {
    val scaffoldState = rememberScaffoldState()
    val coroutine = rememberCoroutineScope()
    val configuration = LocalConfiguration.current

    val bottomSheetScaffoldState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden
    )
    val keyboardController = LocalSoftwareKeyboardController.current

    var expandedList by rememberSaveable {
        mutableStateOf(emptyList<RegistrationField.Option>())
    }
    var openWarningMessageDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val mapFields = rememberSaveableMap {
        mutableStateMapOf(
            Pair(
                YEAR_OF_BIRTH,
                if (uiState.account.yearOfBirth != null) uiState.account.yearOfBirth.toString() else ""
            ),
            Pair(LANGUAGE, uiState.account.languageProficiencies),
            Pair(COUNTRY, uiState.account.country),
            Pair(BIO, uiState.account.bio),
            Pair(ACCOUNT_PRIVACY, uiState.account.accountPrivacy.name.lowercase())
        )
    }

    val saveButtonEnabled = !(uiState.account.yearOfBirth.toString() == mapFields[YEAR_OF_BIRTH]
            && uiState.account.languageProficiencies == mapFields[LANGUAGE]
            && uiState.account.country == mapFields[COUNTRY]
            && uiState.account.bio == mapFields[BIO]
            && selectedImageUri == null
            && !isImageDeleted
            && uiState.isLimited == uiState.account.isLimited())
    onDataChanged(saveButtonEnabled)

    val serverFieldName = rememberSaveable {
        mutableStateOf("")
    }

    val imageRes: Any = if (!isImageDeleted) {
        selectedImageUri?.toString() ?: uiState.account.profileImage.imageUrlFull
    } else {
        R.drawable.core_ic_default_profile_picture
    }

    val modalListState = rememberLazyListState()
    var isOpenChangeImageDialogState by rememberSaveable {
        mutableStateOf(false)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        scaffoldState = scaffoldState
    ) { paddingValues ->

        val contentWidth by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier.widthIn(Dp.Unspecified, 420.dp),
                    compact = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )
            )
        }

        val topBarWidth by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier.widthIn(Dp.Unspecified, 560.dp),
                    compact = Modifier
                        .fillMaxWidth()
                )
            )
        }

        val popUpModifier by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier
                        .widthIn(Dp.Unspecified, 560.dp)
                        .padding(bottom = 86.dp),
                    compact = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 60.dp)
                        .padding(horizontal = 24.dp)
                )
            )
        }

        val bottomSheetWeight by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) 0.8f else 0.6f,
                    compact = 1f
                )
            )
        }

        ModalBottomSheetLayout(
            modifier = Modifier
                .noRippleClickable {
                    if (bottomSheetScaffoldState.isVisible) {
                        coroutine.launch {
                            bottomSheetScaffoldState.hide()
                        }
                    }
                },
            sheetShape = BottomSheetShape(
                width = configuration.screenWidthDp.px,
                height = configuration.screenHeightDp.px,
                weight = bottomSheetWeight
            ),
            sheetState = bottomSheetScaffoldState,
            scrimColor = Color.Black.copy(alpha = 0.4f),
            sheetBackgroundColor = MaterialTheme.appColors.background,
            sheetContent = {
                SheetContent(
                    expandedList = expandedList,
                    listState = modalListState,
                    onItemClick = { item ->
                        if (serverFieldName.value == LANGUAGE) {
                            mapFields[serverFieldName.value] =
                                listOf(LanguageProficiency(item.value))
                        } else {
                            mapFields[serverFieldName.value] = item.value
                        }
                        coroutine.launch {
                            bottomSheetScaffoldState.hide()
                            modalListState.scrollToItem(0)
                        }
                    })
            }) {

            HandleUIMessage(uiMessage = uiMessage, scaffoldState = scaffoldState)

            if (isOpenChangeImageDialogState && uiState.account.isOlderThanMinAge()) {
                ChangeImageDialog(
                    onSelectFromGalleryClick = {
                        isOpenChangeImageDialogState = false
                        onSelectImageClick()
                    },
                    onRemoveImageClick = {
                        onDeleteImageClick()
                        isOpenChangeImageDialogState = false
                    },
                    onCancelClick = {
                        isOpenChangeImageDialogState = false
                    }
                )
            } else {
                isOpenChangeImageDialogState = false
            }

            if (leaveDialog) {
                LeaveProfile(
                    onDismissRequest = {
                        onKeepEdit()
                    },
                    onLeaveClick = {
                        onBackClick(false)
                    }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues)
                    .statusBarsInset(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = topBarWidth,
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth(),
                        text = stringResource(id = profileR.string.profile_edit_profile),
                        color = MaterialTheme.appColors.textPrimary,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.appTypography.titleMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        BackBtn(modifier = Modifier.padding(end = 16.dp)) {
                            onBackClick(saveButtonEnabled)
                        }
                        if (saveButtonEnabled) {
                            IconText(
                                modifier = Modifier
                                    .height(48.dp)
                                    .padding(end = 24.dp),
                                text = stringResource(id = profileR.string.profile_done),
                                icon = Icons.Filled.Done,
                                color = MaterialTheme.appColors.primary,
                                textStyle = MaterialTheme.appTypography.labelLarge,
                                onClick = {
                                    onSaveClick(mapFields.toMap())
                                }
                            )

                        }
                    }
                }

                Surface(
                    modifier = Modifier,
                    color = MaterialTheme.appColors.background,
                    shape = MaterialTheme.appShapes.screenBackgroundShape
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Column(
                            Modifier
                                .fillMaxHeight()
                                .then(contentWidth)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(if (uiState.isLimited) profileR.string.profile_limited_profile else profileR.string.profile_full_profile),
                                color = MaterialTheme.appColors.textSecondary,
                                style = MaterialTheme.appTypography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        model = imageRes,
                                        placeholder = painterResource(id = R.drawable.core_ic_default_profile_picture),
                                        error = painterResource(id = R.drawable.core_ic_default_profile_picture)
                                    ),
                                    contentScale = ContentScale.Crop,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .border(
                                            2.dp,
                                            MaterialTheme.appColors.onSurface,
                                            CircleShape
                                        )
                                        .padding(2.dp)
                                        .size(100.dp)
                                        .clip(CircleShape)

                                        .noRippleClickable {
                                            isOpenChangeImageDialogState = true
                                            if (!uiState.account.isOlderThanMinAge()) {
                                                openWarningMessageDialog = true
                                            }
                                        }
                                )
                                Icon(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.appColors.primary)
                                        .padding(5.dp),
                                    painter = painterResource(id = profileR.drawable.profile_ic_edit_image),
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = uiState.account.name,
                                style = MaterialTheme.appTypography.headlineSmall,
                                color = MaterialTheme.appColors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                modifier = Modifier.clickable {
                                    if (!LocaleUtils.isProfileLimited(mapFields[YEAR_OF_BIRTH].toString())) {
                                        val privacy = if (uiState.isLimited) {
                                            Account.Privacy.ALL_USERS
                                        } else {
                                            Account.Privacy.PRIVATE
                                        }
                                        mapFields[ACCOUNT_PRIVACY] = privacy
                                        onLimitedProfileChange(!uiState.isLimited)
                                    } else {
                                        openWarningMessageDialog = true
                                    }
                                },
                                text = stringResource(if (uiState.isLimited) profileR.string.profile_switch_to_full else profileR.string.profile_switch_to_limited),
                                color = MaterialTheme.appColors.textAccent,
                                style = MaterialTheme.appTypography.labelLarge
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            ProfileFields(
                                disabled = uiState.isLimited,
                                onFieldClick = { it ->
                                    if (it == YEAR_OF_BIRTH) {
                                        serverFieldName.value = YEAR_OF_BIRTH
                                        expandedList =
                                            LocaleUtils.getBirthYearsRange()
                                    } else if (it == COUNTRY) {
                                        serverFieldName.value = COUNTRY
                                        expandedList =
                                            LocaleUtils.getCountries()
                                    } else if (it == LANGUAGE) {
                                        serverFieldName.value = LANGUAGE
                                        expandedList = LocaleUtils.getLanguages()
                                    }
                                    keyboardController?.hide()
                                    coroutine.launch {
                                        val index = expandedList.indexOfFirst { option ->
                                            if (serverFieldName.value == LANGUAGE) {
                                                option.value == (mapFields[serverFieldName.value] as List<LanguageProficiency>).getOrNull(
                                                    0
                                                )?.code
                                            } else {
                                                option.value == mapFields[serverFieldName.value]
                                            }
                                        }
                                        modalListState.scrollToItem(if (index > 0) index else 0)
                                        if (bottomSheetScaffoldState.isVisible) {
                                            bottomSheetScaffoldState.hide()
                                        } else {
                                            bottomSheetScaffoldState.show()
                                        }
                                    }
                                },
                                onValueChanged = {
                                    mapFields[BIO] = it
                                },
                                mapFields = mapFields,
                                onDoneClick = { onSaveClick(mapFields.toMap()) }
                            )
                            Spacer(Modifier.height(40.dp))
                            IconText(
                                text = stringResource(id = com.raccoongang.profile.R.string.profile_delete_profile),
                                painter = painterResource(id = profileR.drawable.profile_ic_trash),
                                textStyle = MaterialTheme.appTypography.labelLarge,
                                color = MaterialTheme.appColors.error,
                                onClick = {
                                    onDeleteClick()
                                })
                            Spacer(Modifier.height(52.dp))
                        }
                        if (openWarningMessageDialog) {
                            LimitedProfileDialog(
                                modifier = popUpModifier
                            ) {
                                openWarningMessageDialog = false
                            }
                        }
                    }
                }

            }
        }
    }
}

@Composable
private fun LimitedProfileDialog(
    modifier: Modifier,
    onCloseClick: () -> Unit
) {
    Column(
        modifier
            .shadow(
                2.dp,
                MaterialTheme.appShapes.material.medium
            )
            .background(
                MaterialTheme.appColors.warning,
                MaterialTheme.appShapes.material.medium
            )
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Report,
                    contentDescription = null,
                    tint = MaterialTheme.appColors.textDark
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(id = profileR.string.profile_oh_sorry),
                    color = MaterialTheme.appColors.textDark,
                    style = MaterialTheme.appTypography.titleMedium
                )
                Icon(
                    modifier = Modifier.clickable { onCloseClick() },
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = MaterialTheme.appColors.textDark
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = profileR.string.profile_must_be_over),
                color = MaterialTheme.appColors.textDark,
                style = MaterialTheme.appTypography.bodyMedium
            )
        }
    }
}

@Composable
private fun ChangeImageDialog(
    onSelectFromGalleryClick: () -> Unit,
    onRemoveImageClick: () -> Unit,
    onCancelClick: () -> Unit,
) {
    Dialog(onDismissRequest = {
        onCancelClick()
    }) {
        val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
        dialogWindowProvider.window.setGravity(Gravity.BOTTOM)
        Box(
            Modifier.padding(bottom = 24.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.appColors.cardViewBackground,
                        MaterialTheme.appShapes.cardShape
                    )
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(4.dp))
                Divider(
                    modifier = Modifier
                        .width(32.dp)
                        .background(
                            MaterialTheme.appColors.bottomSheetToggle,
                            MaterialTheme.appShapes.material.small
                        )
                        .clip(MaterialTheme.appShapes.material.small),
                    thickness = 4.dp
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(id = profileR.string.profile_change_image),
                    style = MaterialTheme.appTypography.titleLarge,
                    color = MaterialTheme.appColors.textPrimary
                )
                Spacer(Modifier.height(20.dp))
                NewEdxButton(
                    text = stringResource(id = profileR.string.profile_select_from_gallery),
                    onClick = onSelectFromGalleryClick,
                    content = {
                        IconText(
                            text = stringResource(id = profileR.string.profile_select_from_gallery),
                            painter = painterResource(id = profileR.drawable.profile_ic_gallery),
                            color = Color.White,
                            textStyle = MaterialTheme.appTypography.labelLarge
                        )
                    }
                )
                Spacer(Modifier.height(16.dp))
                NewEdxOutlinedButton(
                    borderColor = MaterialTheme.appColors.error,
                    textColor = MaterialTheme.appColors.textPrimary,
                    text = stringResource(id = profileR.string.profile_remove_photo),
                    onClick = onRemoveImageClick,
                    content = {
                        IconText(
                            text = stringResource(id = profileR.string.profile_remove_photo),
                            painter = painterResource(id = profileR.drawable.profile_ic_remove_image),
                            color = MaterialTheme.appColors.error,
                            textStyle = MaterialTheme.appTypography.labelLarge
                        )
                    }
                )
                Spacer(Modifier.height(40.dp))
                NewEdxOutlinedButton(
                    borderColor = MaterialTheme.appColors.textPrimaryVariant,
                    textColor = MaterialTheme.appColors.textPrimary,
                    text = stringResource(id = R.string.core_cancel),
                    onClick = onCancelClick
                )
                Spacer(Modifier.height(20.dp))
            }
        }

    }
}

@Composable
private fun ProfileFields(
    disabled: Boolean,
    mapFields: MutableMap<String, Any?>,
    onFieldClick: (String) -> Unit,
    onValueChanged: (String) -> Unit,
    onDoneClick: () -> Unit
) {
    val languageProficiency = (mapFields[LANGUAGE] as List<LanguageProficiency>)
    val lang = if (languageProficiency.isNotEmpty()) {
        LocaleUtils.getLanguageByLanguageCode(languageProficiency[0].code)
    } else ""
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        SelectableField(
            name = stringResource(id = profileR.string.profile_year),
            initialValue = mapFields[YEAR_OF_BIRTH].toString(),
            onClick = {
                onFieldClick(YEAR_OF_BIRTH)
            }
        )
        if (!disabled) {
            SelectableField(
                name = stringResource(id = profileR.string.profile_location),
                initialValue = LocaleUtils.getCountryByCountryCode(mapFields[COUNTRY].toString()),
                onClick = {
                    onFieldClick(COUNTRY)
                }
            )
            SelectableField(
                name = stringResource(id = profileR.string.profile_spoken_language),
                initialValue = lang,
                onClick = {
                    onFieldClick(LANGUAGE)
                }
            )
            InputEditField(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp),
                name = stringResource(id = profileR.string.profile_about_me),
                initialValue = mapFields[BIO].toString(),
                onValueChanged = {
                    onValueChanged(it.take(BIO_TEXT_FIELD_LIMIT))
                },
                onDoneClick = onDoneClick
            )
        }
    }
}

@Composable
private fun SelectableField(
    name: String,
    initialValue: String?,
    disabled: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = if (disabled) {
        TextFieldDefaults.outlinedTextFieldColors(
            unfocusedBorderColor = MaterialTheme.appColors.textFieldBorder,
            backgroundColor = MaterialTheme.appColors.textFieldBackground
        )
    } else {
        TextFieldDefaults.outlinedTextFieldColors(
            unfocusedBorderColor = MaterialTheme.appColors.textFieldBorder,
            disabledBorderColor = MaterialTheme.appColors.textFieldBorder,
            disabledTextColor = MaterialTheme.appColors.textPrimary,
            backgroundColor = MaterialTheme.appColors.textFieldBackground,
            disabledPlaceholderColor = MaterialTheme.appColors.textFieldHint
        )
    }
    Column() {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = name,
            style = MaterialTheme.appTypography.labelLarge,
            color = MaterialTheme.appColors.textPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            readOnly = true,
            enabled = false,
            value = initialValue ?: "",
            colors = colors,
            shape = MaterialTheme.appShapes.textFieldShape,
            textStyle = MaterialTheme.appTypography.bodyMedium,
            onValueChange = { },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.appColors.textPrimaryVariant
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .noRippleClickable {
                    onClick()
                },
            placeholder = {
                Text(
                    text = name,
                    color = MaterialTheme.appColors.textFieldHint,
                    style = MaterialTheme.appTypography.bodyMedium
                )
            }
        )
    }
}

@Composable
private fun InputEditField(
    modifier: Modifier,
    name: String,
    initialValue: String,
    disabled: Boolean = false,
    onValueChanged: (String) -> Unit,
    onDoneClick: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val keyboardType = KeyboardType.Text

    Column {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = name,
            style = MaterialTheme.appTypography.labelLarge,
            color = MaterialTheme.appColors.textPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            enabled = !disabled,
            value = initialValue,
            onValueChange = {
                onValueChanged(it)
            },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                unfocusedBorderColor = MaterialTheme.appColors.textFieldBorder,
                backgroundColor = MaterialTheme.appColors.textFieldBackground
            ),
            shape = MaterialTheme.appShapes.textFieldShape,
            placeholder = {
                Text(
                    text = name,
                    color = MaterialTheme.appColors.textFieldHint,
                    style = MaterialTheme.appTypography.bodyMedium
                )
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = keyboardType,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions {
                keyboardController?.hide()
                focusManager.clearFocus()
                onDoneClick()
            },
            textStyle = MaterialTheme.appTypography.bodyMedium,
            modifier = modifier
        )
    }
}

@Composable
private fun LeaveProfile(
    onDismissRequest: () -> Unit,
    onLeaveClick: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        content = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.appColors.background,
                        MaterialTheme.appShapes.cardShape
                    )
                    .clip(MaterialTheme.appShapes.cardShape)
                    .border(
                        1.dp,
                        MaterialTheme.appColors.cardViewBorder,
                        MaterialTheme.appShapes.cardShape
                    )
                    .padding(horizontal = 40.dp)
                    .padding(top = 48.dp, bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    modifier = Modifier
                        .size(100.dp),
                    painter = painterResource(com.raccoongang.profile.R.drawable.profile_ic_save),
                    contentDescription = null
                )
                Spacer(Modifier.size(48.dp))
                Text(
                    text = stringResource(id = com.raccoongang.profile.R.string.profile_leave_profile),
                    color = MaterialTheme.appColors.textPrimary,
                    style = MaterialTheme.appTypography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.size(12.dp))
                Text(
                    text = stringResource(id = com.raccoongang.profile.R.string.profile_changes_you_made),
                    color = MaterialTheme.appColors.textPrimary,
                    style = MaterialTheme.appTypography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.size(40.dp))
                NewEdxButton(
                    text = stringResource(id = com.raccoongang.profile.R.string.profile_leave),
                    onClick = onLeaveClick,
                    backgroundColor = MaterialTheme.appColors.warning,
                    content = {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(id = com.raccoongang.profile.R.string.profile_leave),
                            color = MaterialTheme.appColors.textDark,
                            style = MaterialTheme.appTypography.labelLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                )
                Spacer(Modifier.height(24.dp))
                NewEdxOutlinedButton(
                    borderColor = MaterialTheme.appColors.textPrimary,
                    textColor = MaterialTheme.appColors.textPrimary,
                    text = stringResource(id = com.raccoongang.profile.R.string.profile_keep_editing),
                    onClick = onDismissRequest
                )
            }
        })
}


@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "NEXUS_5_Light", device = Devices.NEXUS_5, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_5_Dark", device = Devices.NEXUS_5, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun EditProfileScreenPreview() {
    NewEdxTheme {
        EditProfileScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = EditProfileUIState(account = mockAccount, isUpdating = false, false),
            selectedImageUri = null,
            uiMessage = null,
            isImageDeleted = true,
            leaveDialog = false,
            onBackClick = {},
            onSaveClick = {},
            onDeleteClick = {},
            onSelectImageClick = {},
            onDeleteImageClick = {},
            onDataChanged = {},
            onKeepEdit = {},
            onLimitedProfileChange = {}
        )
    }
}

@Preview(name = "NEXUS_9_Light", device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_9_Dark", device = Devices.NEXUS_9, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun EditProfileScreenTabletPreview() {
    NewEdxTheme {
        EditProfileScreen(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiState = EditProfileUIState(account = mockAccount, isUpdating = false, false),
            selectedImageUri = null,
            uiMessage = null,
            isImageDeleted = true,
            leaveDialog = false,
            onBackClick = {},
            onSaveClick = {},
            onDeleteClick = {},
            onSelectImageClick = {},
            onDeleteImageClick = {},
            onDataChanged = {},
            onKeepEdit = {},
            onLimitedProfileChange = {}
        )
    }
}


private val mockAccount = Account(
    username = "thom84",
    bio = "designer",
    requiresParentalConsent = true,
    name = "Thomas",
    country = "Ukraine",
    isActive = true,
    profileImage = ProfileImage("", "", "", "", false),
    yearOfBirth = 2000,
    levelOfEducation = "Bachelor",
    goals = "130",
    languageProficiencies = emptyList(),
    gender = "male",
    mailingAddress = "",
    "",
    null,
    accountPrivacy = Account.Privacy.ALL_USERS
)
