package de.ph1b.audiobook.chapterreader.mp4

import java.io.File
import java.io.RandomAccessFile
import java.util.ArrayList

/**
 * Reads the chap atom to find associated chapters
 */
internal object ChapReader {

  fun read(file: File): Map<Int, String> = RandomAccessFile(file, "r").use { raf ->
    val atoms = raf.atoms(listOf("moov", "trak", "tref", "mdia", "minf", "stbl"))

    val chapterTrackId = findChapterTrackId(raf, atoms)
        ?: return emptyMap()
    val chapterTrackAtom = findChapterTrackAtom(raf, atoms, chapterTrackId)
        ?: return emptyMap()
    val timeScale = readTimeScale(raf, chapterTrackAtom)
        ?: return emptyMap()
    val names = readNames(raf, atoms, chapterTrackId)
    val durations = readDurations(raf, chapterTrackAtom, timeScale)

    if (names.size != durations.size || names.isEmpty())
      return emptyMap()

    val map = HashMap<Int, String>(names.size)
    var position = 0L
    names.forEachIndexed { index, name ->
      map.put(position.toInt(), name)
      position += durations[index]
    }
    map
  }

  private fun findChapterTrackAtom(raf: RandomAccessFile, atoms: List<Mp4Atom>, chapterTrackId: Int): Mp4Atom? {
    val trackAtoms = atoms.firstOrNull { it.name == "moov" }
        ?.children?.filter { it.name == "trak" }
        ?: return null

    return trackAtoms.firstOrNull {
      val tkhd = it.children.firstOrNull { it.name == "tkhd" }
      if (tkhd == null) false
      else {
        // track id at byte 20:
        // https://developer.apple.com/library/content/documentation/QuickTime/QTFF/QTFFChap2/qtff2.html
        raf.seek(tkhd.position + 8)
        val version = raf.readByte().toInt()
        if (version == 0 || version == 1) {
          val flagsSize = 3
          val creationTimeSize = if (version == 0) 4 else 8
          val modificationTimeSize = if (version == 0) 4 else 8
          raf.skipBytes(flagsSize + creationTimeSize + modificationTimeSize)
          val thisTrackId = raf.readInt()
          thisTrackId == chapterTrackId
        } else false
      }
    }
  }

  private fun findChapterTrackId(raf: RandomAccessFile, atoms: List<Mp4Atom>): Int? {
    val chapAtom = atoms.findAtom("moov", "trak", "tref", "chap")
        ?: return null

    raf.seek(chapAtom.position + 8)
    return raf.readInt()
  }

  private fun readTimeScale(raf: RandomAccessFile, chapterTrakAtom: Mp4Atom): Int? {
    val mdhdAtom = chapterTrakAtom.children.firstOrNull { it.name == "mdia" }
        ?.children?.firstOrNull { it.name == "mdhd" }
        ?: return null
    raf.seek(mdhdAtom.position + 8)
    val version = raf.readByte().toInt()
    if (version != 0 && version != 1)
      return null
    val flagsSize = 3
    val creationTimeSize = if (version == 0) 4 else 8
    val modificationTimeSize = if (version == 0) 4 else 8
    raf.skipBytes(flagsSize + creationTimeSize + modificationTimeSize)
    return raf.readInt()
  }

  private fun readNames(raf: RandomAccessFile, atoms: List<Mp4Atom>, chapterTrackId: Int): List<String> {
    val stco = atoms.firstOrNull { it.name == "moov" }?.children
        ?.filter { it.name == "trak" }
        ?.getOrNull(chapterTrackId - 1)?.children
        ?.firstOrNull { it.name == "mdia" }?.children
        ?.firstOrNull { it.name == "minf" }?.children
        ?.firstOrNull { it.name == "stbl" }?.children
        ?.firstOrNull { it.name == "stco" }
        ?: return emptyList()

    raf.seek(stco.position + 8)
    val version = raf.readByte().toInt()
    if (version != 0) {
      return emptyList()
    }
    raf.skipBytes(3)
    val entryCount = raf.readUnsignedInt().toInt()
    val chunkOffsets = ArrayList<Long>(entryCount)
    repeat(entryCount) {
      chunkOffsets.add(raf.readUnsignedInt())
    }

    return chunkOffsets.map {
      raf.seek(it)
      val textLength = raf.readShort().toInt()
      val textBytes = ByteArray(textLength)
      raf.read(textBytes)
      String(textBytes)
    }
  }

  private fun readDurations(raf: RandomAccessFile, chapterTrakAtom: Mp4Atom, timeScale: Int): List<Long> {
    val stts = chapterTrakAtom.children.findAtom("mdia", "minf", "stbl", "stts")
        ?: return emptyList()
    raf.seek(stts.position + 8)
    val version = raf.readByte().toInt()
    if (version != 0)
      return emptyList()
    raf.skipBytes(3) // flags
    val numberOfEntries = raf.readInt()

    val durations = ArrayList<Long>(numberOfEntries)
    repeat(numberOfEntries) {
      val count = raf.readUnsignedInt()
      val delta = raf.readUnsignedInt()
      durations.add(count * 1000 / timeScale * delta)
    }
    return durations
  }
}
