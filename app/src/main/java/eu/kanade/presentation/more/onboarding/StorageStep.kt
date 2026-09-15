package eu.kanade.presentation.more.onboarding

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import com.hippo.unifile.UniFile
import eu.kanade.presentation.more.settings.screen.SettingsDataScreen
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.collectLatest
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.storage.service.StorageManager.Companion.directoryAccessible
import tachiyomi.domain.storage.service.StoragePreferences
import tachiyomi.i18n.MR
import tachiyomi.i18n.kmk.KMR
import tachiyomi.presentation.core.components.material.Button
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class StorageStep : OnboardingStep {

    private val storagePref = Injekt.get<StoragePreferences>().baseStorageDirectory()

    private var _isComplete by mutableStateOf(false)

    override val isComplete: Boolean
        get() = _isComplete

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val handler = LocalUriHandler.current

        // KMK -->
        val storageDir by storagePref.collectAsState()
        var locationValid by remember(storageDir) {
            mutableStateOf(directoryAccessible(context, storageDir))
        }

        var folderName by remember { mutableStateOf("Rout") }
        var isEditing by remember { mutableStateOf(false) }
        var isLocked by remember { mutableStateOf(false) }
        var showGuideDialog by remember { mutableStateOf(false) }

        val hintLauncher = rememberLauncherForActivityResult(
            contract = OpenDocumentTreeWithHint(),
        ) { uri ->
            if (uri != null) {
                Log.d("RoutDebug", "URI dipilih: $uri")
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                try {
                    context.contentResolver.takePersistableUriPermission(uri, flags)
                    Log.d("RoutDebug", "Izin permanen diberikan untuk: $uri")
                } catch (e: SecurityException) {
                    Log.e("RoutDebug", "Gagal mengambil izin permanen", e)
                    context.logcat(LogPriority.ERROR, e) { "Failed to acquire persistent folder access" }
                    context.toast(MR.strings.file_picker_uri_permission_unsupported)
                }

                UniFile.fromUri(context, uri)?.let {
                    storagePref.set("")
                    storagePref.set(it.uri.toString())
                    Log.d("RoutDebug", "Storage preference diperbarui: ${it.uri}")
                }
            } else {
                Log.d("RoutDebug", "Pemilihan folder dibatalkan oleh pengguna")
                isLocked = false
            }
        }

        if (showGuideDialog) {
            AlertDialog(
                onDismissRequest = { },
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                title = {
                    Text(
                        text = stringResource(KMR.strings.onboarding_storage_permission_dialog_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(KMR.strings.onboarding_storage_permission_dialog_desc, folderName),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp,
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        androidx.compose.material3.Button(
                            onClick = {
                                Log.d("RoutDebug", "Tombol Select Folder di dialog diklik")
                                showGuideDialog = false
                                val hintUri = "content://com.android.externalstorage.documents/document/primary:Documents%2F$folderName".toUri()
                                Log.d("RoutDebug", "Meluncurkan SAF dengan hint: $hintUri")
                                try {
                                    hintLauncher.launch(hintUri)
                                } catch (e: Exception) {
                                    Log.e("RoutDebug", "Gagal meluncurkan SAF launcher", e)
                                    context.toast(MR.strings.file_picker_error)
                                    isLocked = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            elevation = androidx.compose.material3.ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                        ) {
                            Text(
                                text = stringResource(MR.strings.onboarding_storage_action_select),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                    }
                },
                confirmButton = {},
                shape = RoundedCornerShape(28.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
        ) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.CreateNewFolder,
                        contentDescription = null,
                        modifier = Modifier.size(50.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Text(
                text = stringResource(KMR.strings.onboarding_storage_kmk_info),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Surface(
                onClick = { if (!isLocked) isEditing = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isLocked) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                ),
                shadowElevation = if (isLocked) 0.dp else 4.dp,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!isEditing) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isLocked) stringResource(KMR.strings.onboarding_storage_folder_name_label_created) else stringResource(KMR.strings.onboarding_storage_folder_name_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isLocked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                                letterSpacing = 2.sp,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = folderName,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLocked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                )
                                if (!isLocked) {
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = "Edit",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    )
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = folderName,
                            onValueChange = { folderName = it },
                            label = { Text(stringResource(KMR.strings.onboarding_storage_folder_name_hint)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            ),
                            shape = RoundedCornerShape(20.dp),
                            trailingIcon = {
                                IconButton(onClick = { if (folderName.isNotBlank()) isEditing = false }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = stringResource(MR.strings.action_ok),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            },
                        )
                    }
                }
            }

            androidx.compose.material3.Button(
                onClick = {
                    val cleanName = folderName.trim()
                    Log.d("RoutDebug", "Tombol OK diklik. Nama folder: $cleanName")
                    if (cleanName.isNotBlank()) {
                        isLocked = true
                        createFolderInDocuments(context, cleanName)
                        showGuideDialog = true
                    } else {
                        Log.d("RoutDebug", "Nama folder kosong, mengaktifkan mode edit")
                        isEditing = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                enabled = !isLocked,
                shape = RoundedCornerShape(20.dp),
                elevation = androidx.compose.material3.ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
            ) {
                Text(
                    text = stringResource(MR.strings.action_ok),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            Text(
                text = stringResource(MR.strings.onboarding_storage_help_info, stringResource(MR.strings.app_name)),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { handler.openUri(SettingsDataScreen.HELP_URL) },
            ) {
                Text(stringResource(MR.strings.onboarding_storage_help_action))
            }
        }
        // KMK <--

        LaunchedEffect(/* KMK --> */storageDir/* KMK <-- */) {
            storagePref.changes()
                .collectLatest {
                    // KMK -->
                    locationValid = directoryAccessible(context, storageDir)
                    _isComplete = locationValid
                    // KMK <--
                }
        }
    }
}

// KMK -->
private class OpenDocumentTreeWithHint : ActivityResultContracts.OpenDocumentTree() {
    override fun createIntent(context: Context, input: Uri?): Intent {
        val intent = super.createIntent(context, input)
        input?.let { intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, it) }
        return intent
    }
}

private fun createFolderInDocuments(context: Context, folderName: String) {
    // Keamanan: Bersihkan nama folder dari karakter ilegal OS
    val cleanName = folderName.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_")
    val contentUri = MediaStore.Files.getContentUri("external")

    // Pengecekan keberadaan folder
    val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
    val selectionArgs = arrayOf("${Environment.DIRECTORY_DOCUMENTS}/$cleanName%", cleanName)

    try {
        context.contentResolver.query(contentUri, arrayOf(MediaStore.MediaColumns._ID), selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                Log.d("RoutDebug", "Folder $cleanName terdeteksi sudah ada.")
                return
            }
        }
    } catch (_: Exception) { }

    try {
        // Teknik Dummy File: Membuat file .nomedia untuk memaksa sistem membuat direktori fisik
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, ".nomedia")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/$cleanName/")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(contentUri, values)
        uri?.let {
            context.contentResolver.delete(it, null, null)
            Log.d("RoutDebug", "Folder $cleanName berhasil dibuat secara fisik.")
        }
    } catch (e: Exception) {
        Log.e("RoutDebug", "Error saat menyiapkan folder secara fisik", e)
    }
}
// KMK <--
