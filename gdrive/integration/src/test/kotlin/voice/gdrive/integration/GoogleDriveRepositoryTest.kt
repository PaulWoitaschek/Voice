package voice.gdrive.integration

import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GoogleDriveRepositoryTest {

  private lateinit var mockDriveService: Drive
  private lateinit var repository: GoogleDriveRepository

  @Before
  fun setUp() {
    mockDriveService = mockk()
    repository = GoogleDriveRepository(mockDriveService)
  }

  @Test
  fun `getFolders returns list of folders`() = runBlocking {
    // Arrange
    val expectedFileList = FileList().setFiles(
      listOf(
        File().setName("Folder1").setId("id1").setMimeType("application/vnd.google-apps.folder"),
        File().setName("Folder2").setId("id2").setMimeType("application/vnd.google-apps.folder"),
      ),
    )
    val mockRequest = mockk<Drive.Files.List>()
    coEvery { mockDriveService.files() } returns mockk {
      coEvery { list() } returns mockRequest
    }
    coEvery { mockRequest.setQ(any()) } returns mockRequest
    coEvery { mockRequest.setFields(any()) } returns mockRequest
    coEvery { mockRequest.execute() } returns expectedFileList

    // Act
    val result = repository.getFolders("root")

    // Assert
    assertEquals(2, result.files.size)
    assertEquals("Folder1", result.files[0].name)
    assertEquals("application/vnd.google-apps.folder", result.files[0].mimeType)
  }

  @Test
  fun `getAudioFiles returns list of audio files`() = runBlocking {
    // Arrange
    val expectedFileList = FileList().setFiles(
      listOf(
        File().setName("Audio1.mp3").setId("id1").setMimeType("audio/mpeg"),
        File().setName("Audio2.wav").setId("id2").setMimeType("audio/wav"),
      ),
    )
    val mockRequest = mockk<Drive.Files.List>()
    coEvery { mockDriveService.files() } returns mockk {
      coEvery { list() } returns mockRequest
    }
    coEvery { mockRequest.setQ(any()) } returns mockRequest
    coEvery { mockRequest.setFields(any()) } returns mockRequest
    coEvery { mockRequest.execute() } returns expectedFileList

    // Act
    val result = repository.getAudioFiles("root")

    // Assert
    assertEquals(2, result.files.size)
    assertEquals("Audio1.mp3", result.files[0].name)
    assertEquals("audio/mpeg", result.files[0].mimeType)
  }

  @Test
  fun `getFileUri returns webContentLink if available`() = runBlocking {
    // Arrange
    val fileId = "testFileId"
    val expectedWebContentLink = "http://example.com/download/file"
    val mockFile = File().setWebContentLink(expectedWebContentLink).setWebViewLink("http://example.com/view/file")
    val mockGetRequest = mockk<Drive.Files.Get>()
    coEvery { mockDriveService.files() } returns mockk {
      coEvery { get(fileId) } returns mockGetRequest
    }
    coEvery { mockGetRequest.setFields("webContentLink, webViewLink") } returns mockGetRequest
    coEvery { mockGetRequest.execute() } returns mockFile

    // Act
    val result = repository.getFileUri(fileId)

    // Assert
    assertEquals(expectedWebContentLink, result)
  }

  @Test
  fun `getFileUri returns webViewLink if webContentLink is null`() = runBlocking {
    // Arrange
    val fileId = "testFileId"
    val expectedWebViewLink = "http://example.com/view/file"
    val mockFile = File().setWebViewLink(expectedWebViewLink) // webContentLink is null
    val mockGetRequest = mockk<Drive.Files.Get>()
    coEvery { mockDriveService.files() } returns mockk {
      coEvery { get(fileId) } returns mockGetRequest
    }
    coEvery { mockGetRequest.setFields("webContentLink, webViewLink") } returns mockGetRequest
    coEvery { mockGetRequest.execute() } returns mockFile

    // Act
    val result = repository.getFileUri(fileId)

    // Assert
    assertEquals(expectedWebViewLink, result)
  }

  @Test
  fun `getFolders handles empty result`() = runBlocking {
    // Arrange
    val expectedFileList = FileList().setFiles(emptyList())
    val mockRequest = mockk<Drive.Files.List>()
    coEvery { mockDriveService.files() } returns mockk {
      coEvery { list() } returns mockRequest
    }
    coEvery { mockRequest.setQ(any()) } returns mockRequest
    coEvery { mockRequest.setFields(any()) } returns mockRequest
    coEvery { mockRequest.execute() } returns expectedFileList

    // Act
    val result = repository.getFolders("root")

    // Assert
    assertTrue(result.files.isEmpty())
  }

  @Test
  fun `getAudioFiles handles empty result`() = runBlocking {
    // Arrange
    val expectedFileList = FileList().setFiles(emptyList())
    val mockRequest = mockk<Drive.Files.List>()
    coEvery { mockDriveService.files() } returns mockk {
      coEvery { list() } returns mockRequest
    }
    coEvery { mockRequest.setQ(any()) } returns mockRequest
    coEvery { mockRequest.setFields(any()) } returns mockRequest
    coEvery { mockRequest.execute() } returns expectedFileList

    // Act
    val result = repository.getAudioFiles("root")

    // Assert
    assertTrue(result.files.isEmpty())
  }
}
