package voice.gdrive.integration.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import voice.gdrive.integration.R

/**
 * A Composable screen that handles Google Drive sign-in and displays content from Google Drive.
 *
 * This screen allows users to sign in to their Google Account to authorize access to Google Drive.
 * Once signed in, it's intended to display folders and audio files from their Drive,
 * allowing for navigation and selection.
 *
 * TODO:
 * - Inject a ViewModel to manage state and business logic (fetching files, handling errors).
 * - Implement actual folder navigation and file display logic using data from the ViewModel.
 * - Show detailed error messages to the user when sign-in or data fetching fails.
 * - Add loading indicators while data is being fetched.
 */
@Composable
fun GoogleDriveScreen(
  modifier: Modifier = Modifier,
  // viewModel: GoogleDriveViewModel // Inject ViewModel later
) {
  val context = LocalContext.current
  var isSignedIn by remember { mutableStateOf(GoogleSignIn.getLastSignedInAccount(context) != null) }
  // In a real app, you'd get this from a ViewModel that interacts with GoogleDriveRepository
  // var filesAndFolders by remember { mutableStateOf<List<Any>>(emptyList()) }
  // var error by remember { mutableStateOf<String?>(null) }

  // Configure sign-in to request the Drive API scope.
  val gso = remember {
    GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
      .requestScopes(Scope(DriveScopes.DRIVE_READONLY)) // Request read-only access
      .requestEmail()
      .build()
  }
  val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

  val signInLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult(),
  ) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
      try {
        val account = task.getResult(ApiException::class.java)
        isSignedIn = true
        // TODO: Use account to initialize Drive service in ViewModel/Repository
        // viewModel.onSignInSuccess(account)
        // error = null // Clear previous errors on successful sign-in
      } catch (e: ApiException) {
        // error = context.getString(R.string.gdrive_sign_in_failed, e.statusCode.toString())
        isSignedIn = false
      }
    } else {
      // error = context.getString(R.string.gdrive_sign_in_cancelled)
      isSignedIn = false
    }
  }

  Column(modifier = modifier.fillMaxSize()) {
    // if (error != null) {
    //     Text(text = error!!, color = MaterialTheme.colorScheme.error)
    // }
    if (isSignedIn) {
      Text(context.getString(R.string.gdrive_signed_in))
      // TODO: Implement folder navigation and file display here
      // This would involve calling methods on the ViewModel which in turn call the Repository
      // For example:
      // LaunchedEffect(Unit) {
      //     viewModel.loadDirectoryContents("root") // or current folder ID
      // }
      // if (error != null) {
      //     Text("Error: $error")
      // } else {
      //     LazyColumn {
      //         items(filesAndFolders) { item ->
      //             // Display item (folder or file)
      //         }
      //     }
      // }
    } else {
      Text(context.getString(R.string.gdrive_not_signed_in))
      Button(onClick = { signInLauncher.launch(googleSignInClient.signInIntent) }) {
        Text(context.getString(R.string.gdrive_sign_in_button))
      }
    }
  }
}

@Preview
@Composable
private fun GoogleDriveScreenPreview() {
  // Mocking the signed-in state for preview
  GoogleDriveScreen()
}
