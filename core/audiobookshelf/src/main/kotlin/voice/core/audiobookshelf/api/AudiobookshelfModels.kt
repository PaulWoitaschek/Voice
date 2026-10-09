package voice.core.audiobookshelf.api

import kotlinx.serialization.Serializable

@Serializable
internal data class StatusResponse(
  val app: String? = null,
  val serverVersion: String? = null,
  val isInit: Boolean = true,
  val authMethods: List<String> = emptyList(),
)

@Serializable
internal data class LoginRequest(
  val username: String,
  val password: String,
)

@Serializable
internal data class LoginResponse(val user: AbsUser)

@Serializable
internal data class AbsUser(
  val id: String,
  val username: String,
  val accessToken: String? = null,
  val refreshToken: String? = null,
  val mediaProgress: List<AbsMediaProgress> = emptyList(),
  val bookmarks: List<AbsBookmark> = emptyList(),
)

@Serializable
internal data class LibrariesResponse(val libraries: List<AbsLibrary>)

@Serializable
internal data class AbsLibrary(
  val id: String,
  val name: String,
  val mediaType: String,
)

@Serializable
internal data class LibraryItemsResponse(
  val results: List<AbsLibraryItem> = emptyList(),
  val total: Int = 0,
)

@Serializable
internal data class BatchGetRequest(val libraryItemIds: List<String>)

@Serializable
internal data class BatchGetResponse(val libraryItems: List<AbsLibraryItem>)

@Serializable
internal data class AbsLibraryItem(
  val id: String,
  val libraryId: String,
  val mediaType: String,
  val addedAt: Long = 0,
  val updatedAt: Long = 0,
  val isMissing: Boolean = false,
  val isInvalid: Boolean = false,
  val media: AbsBookMedia,
)

@Serializable
internal data class AbsBookMedia(
  val metadata: AbsBookMetadata,
  val coverPath: String? = null,
  val duration: Double = 0.0,
  val numTracks: Int = 0,
  val tracks: List<AbsTrack>? = null,
  val chapters: List<AbsChapter>? = null,
)

@Serializable
internal data class AbsBookMetadata(
  val title: String? = null,
  val authorName: String? = null,
  val narratorName: String? = null,
  val seriesName: String? = null,
  val series: List<AbsSeries>? = null,
  val genres: List<String> = emptyList(),
)

@Serializable
internal data class AbsSeries(
  val name: String,
  val sequence: String? = null,
)

@Serializable
internal data class AbsTrack(
  val index: Int,
  val ino: String,
  val startOffset: Double,
  val duration: Double,
  val title: String? = null,
  val mimeType: String? = null,
  val metadata: AbsFileMetadata? = null,
)

@Serializable
internal data class AbsFileMetadata(
  val filename: String? = null,
  val size: Long = 0,
)

@Serializable
internal data class AbsChapter(
  val start: Double,
  val end: Double,
  val title: String? = null,
)

@Serializable
internal data class AbsMediaProgress(
  val libraryItemId: String? = null,
  val episodeId: String? = null,
  val duration: Double = 0.0,
  val currentTime: Double = 0.0,
  val isFinished: Boolean = false,
  val lastUpdate: Long = 0,
)

@Serializable
internal data class ProgressUpdate(
  val currentTime: Double,
  val duration: Double,
  val progress: Double,
  /**
   * Only ever true: told that a finished book isn't finished, the server starts it over. Without the flag it takes
   * the time and un-finishes the book by itself.
   */
  val isFinished: Boolean? = null,
)

@Serializable
internal data class AbsBookmark(
  val libraryItemId: String,
  val title: String? = null,
  val time: Double,
  val createdAt: Long = 0,
)

@Serializable
internal data class BookmarkRequest(
  val time: Double,
  val title: String,
)

@Serializable
internal data class StartSessionRequest(
  val deviceInfo: DeviceInfo,
  val mediaPlayer: String,
  val forceDirectPlay: Boolean,
  val forceTranscode: Boolean,
  val supportedMimeTypes: List<String>,
)

@Serializable
internal data class DeviceInfo(
  val clientName: String,
  val clientVersion: String,
  val manufacturer: String,
  val model: String,
  val sdkVersion: Int,
  val deviceId: String,
)

@Serializable
internal data class PlaybackSessionResponse(val id: String)

@Serializable
internal data class SessionSyncRequest(
  val currentTime: Double,
  val timeListened: Double,
  val duration: Double,
)
