package de.ph1b.audiobook.features.chapterReader

import android.util.SparseArray
import de.ph1b.audiobook.misc.emptySparseArray
import java.io.File
import java.io.RandomAccessFile
import java.util.*

/**
 * Reads the chap atom to find associated chapters
 *
 * @author Paul Woitaschek
 */
object ChapReader {

  fun read(file: File): SparseArray<String> {
    val raf = RandomAccessFile(file, "r")

    val atoms = raf.atoms(listOf("moov", "trak", "tref", "mdia", "minf", "stbl"))

    val chapterTrackId = findChapterTrackId(raf, atoms)
        ?: return emptySparseArray()
    val chapterTrackAtom = findChapterTrackAtom(raf, atoms, chapterTrackId)
        ?: return emptySparseArray()
    val timeScale = readTimeScale(raf, chapterTrackAtom)
        ?: return emptySparseArray()
    val names = readNames(raf, atoms, chapterTrackId)
    val durations = readDurations(raf, chapterTrackAtom, timeScale)

    if (names.size != durations.size || names.isEmpty())
      return emptySparseArray()

    val array = SparseArray<String>(names.size)
    var position = 0L
    names.forEachIndexed { index, name ->
      array.put(position.toInt(), name)
      position += durations[index]
    }
    return array
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
    val mdatAtom = atoms.filter { it.name == "mdat" }
        .getOrNull(chapterTrackId - 1)
        ?: return emptyList()

    raf.seek(mdatAtom.position + 8)

    val names = ArrayList<String>()
    while (raf.filePointer < mdatAtom.position + mdatAtom.length) {
      val textLength = raf.readShort().toInt()
      val textBytes = ByteArray(textLength)
      raf.read(textBytes)
      val name = String(textBytes)
      names.add(name)
      raf.skipBytes(12)
    }
    return names
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
