package de.ph1b.audiobook.data.repo

/*//todo: fix me
class BookRepositoryTest {

  private lateinit var repo: BookRepository

  @Rule
  @JvmField
  val clearAppDbRule = ClearDbRule()

  @Before
  fun setUp() {
    val context = InstrumentationRegistry.getTargetContext()
    val helper = PersistenceModule()
      .appDb(
        builder = Room.inMemoryDatabaseBuilder(context, AppDb::class.java),
        callback = InitialRoomCallback(),
        migrations = PersistenceModule().migrations(context)
      )
      .openHelper
    val moshi = Moshi.Builder().build()
    val internalBookRegister = BookStorage(helper, moshi)
    repo = BookRepository(internalBookRegister)
  }

  @Test
  fun inOut() {
    runBlocking {
      val dummy = BookFactory.create()
      repo.addBook(dummy)
      val firstBook = repo.activeBooks.first()
      val dummyWithUpdatedId = dummy.copy(
        id = firstBook.id, content = dummy.content.copy(
          id = firstBook.id
        )
      )
      assertThat(dummyWithUpdatedId).isEqualTo(firstBook)
    }
  }
}
*/
