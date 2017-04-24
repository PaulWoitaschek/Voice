package de.ph1b.audiobook.features.chapterReader

import android.util.SparseArray
import de.ph1b.audiobook.misc.emptySparseArray
import java.io.EOFException
import java.io.File
import java.io.RandomAccessFile
import java.util.*

/**
 * Reads the chap atom to find associated chapters
 *
 * @author Paul Woitaschek
 */
object ChapReader {

  private val boxHeaderBuffer = ByteArray(4)

  @Synchronized
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

  private fun RandomAccessFile.atoms(toVisit: List<String>): List<Mp4Atom> {
    seek(0)
    return atoms(toVisit, null)
  }

  private fun RandomAccessFile.atoms(toVisit: List<String>, endOfAtom: Long? = null): List<Mp4Atom> {
    val atoms = ArrayList<Mp4Atom>()
    while (true) {
      if (filePointer >= length()) break
      val length = readUnsignedInt()

      val name = readBoxHeader()
      val atomPosition = filePointer - 8

      val visit = toVisit.contains(name)
      val children = if (visit) {
        atoms(toVisit, atomPosition + length)
      } else {
        skipBytes(length.toInt() - 8)
        emptyList()
      }

      atoms.add(Mp4Atom(name, atomPosition, length, children))

      if (endOfAtom != null && filePointer == endOfAtom) {
        break
      }
    }
    return atoms
  }

  private fun List<Mp4Atom>.findAtom(vararg path: String): Mp4Atom? {
    if (path.isEmpty()) return null

    forEach { root ->
      if (root.name == path.first()) {
        var match: Mp4Atom? = null
        path.forEachIndexed { index, s ->
          if (index != 0) {
            val searchIn = match ?: root
            match = searchIn.children.firstOrNull { it.name == s }
          }
        }
        if (match != null) return match
      }
    }

    return firstOrNull { it.name == path.first() }
  }

  private fun RandomAccessFile.readUnsignedInt(): Long =
      (read().toLong() and 0xFFL shl 24) or
          (read().toLong() and 0xFFL shl 16) or
          (read().toLong() and 0xFFL shl 8) or
          (read().toLong() and 0xFFL)

  private fun RandomAccessFile.readBoxHeader(): String {
    val result = read(boxHeaderBuffer)
    if (result == -1) throw EOFException("Can't read box header")
    return String(boxHeaderBuffer)
  }

  private data class Mp4Atom(val name: String, val position: Long, val length: Long, val children: List<Mp4Atom>)
}