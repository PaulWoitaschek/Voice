package voice.gdrive.integration

import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GoogleDriveRepository @Inject constructor(private val driveService: Drive) {

  /**
   * Fetches a list of folders within a specified Google Drive folder.
   *
   * @param folderId The ID of the parent folder. Defaults to "root" to list folders in the root directory.
   * @return A [FileList] object containing the folders.
   * @throws java.io.IOException If an I/O error occurs during the API request.
   */
  suspend fun getFolders(folderId: String? = "root"): FileList = withContext(Dispatchers.IO) {
    driveService.files().list()
      .setQ("mimeType='application/vnd.google-apps.folder' and '${folderId ?: "root"}' in parents and trashed=false")
      .setFields("nextPageToken, files(id, name)")
      .execute()
  }

  /**
   * Fetches a list of audio files (MP3 or WAV) within a specified Google Drive folder.
   *
   * @param folderId The ID of the parent folder. Defaults to "root" to list files in the root directory.
   * @return A [FileList] object containing the audio files.
   * @throws java.io.IOException If an I/O error occurs during the API request.
   */
  suspend fun getAudioFiles(folderId: String? = "root"): FileList = withContext(Dispatchers.IO) {
    driveService.files().list()
      .setQ("(mimeType='audio/mpeg' or mimeType='audio/wav') and '${folderId ?: "root"}' in parents and trashed=false")
      .setFields("nextPageToken, files(id, name, size, mimeType)")
      .execute()
  }

  suspend fun downloadFile(
    fileId: String,
    destinationPath: java.io.File,
  ): Unit = withContext(Dispatchers.IO) {
    val outputStream = java.io.FileOutputStream(destinationPath)
    driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
    outputStream.flush()
    outputStream.close()
  }

  // This is a placeholder for actual file upload logic
  suspend fun uploadFile(
    name: String,
    filePath: String,
    mimeType: String,
  ): File = withContext(Dispatchers.IO) {
    val fileMetadata = File().setName(name)
    val mediaContent = FileContent(mimeType, java.io.File(filePath))
    driveService.files().create(fileMetadata, mediaContent).setFields("id").execute()
  }

  /**
   * Retrieves a direct download URI for a given Google Drive file ID.
   * Prefers `webContentLink` if available, otherwise falls back to `webViewLink`.
   * Note: For robust streaming, ensure the link provides direct media access and
   * handles authentication correctly. `webContentLink` is generally better for this.
   *
   * @param fileId The ID of the file in Google Drive.
   * @return A string URI for the file. Returns an empty string if no link is found.
   * @throws java.io.IOException If an I/O error occurs during the API request.
   */
  suspend fun getFileUri(fileId: String): String = withContext(Dispatchers.IO) {
    // This is a simplified representation. For actual streaming,
    // you might need to use the file's webContentLink or a temporary download link.
    // Proper handling of authentication for the URI is also crucial.
    val file = driveService.files().get(fileId).setFields("webContentLink, webViewLink").execute()
    // Prefer webContentLink for direct download, webViewLink for opening in browser
    return@withContext file.webContentLink ?: file.webViewLink ?: ""
  }
}
