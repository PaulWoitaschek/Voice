package de.ph1b.audiobook.features.chapterReader

import android.util.SparseArray
import de.ph1b.audiobook.misc.emptySparseArray
import java.io.File
import java.io.RandomAccessFile

/**
 * Reads chapters from the chpl atom
 *
 * @author Paul Woitaschek
 */
object ChplReader {

  fun read(file: File): SparseArray<String> {
    val raf = RandomAccessFile(file, "r")
    val atoms = raf.atoms(listOf("moov", "udta"))

    val timeScale = readTimeScale(raf, atoms)
        ?: return emptySparseArray()

    val chplAtom = atoms.findAtom("moov", "udta", "chpl")
        ?: return emptySparseArray()
    raf.seek(chplAtom.position + 8)
    raf.skipBytes(8)
    val count = raf.readByte().toUnsignedInt()

    val array = SparseArray<String>(count)
    repeat(count) {
      val duration = raf.readUInt64() / timeScale / 10
      val titleSize = raf.readByte().toInt()

      val titleBytes = ByteArray(titleSize)
      raf.read(titleBytes)
      val title = String(titleBytes)

      array.put(duration.toInt(), title)
    }
    return array
  }

  private fun readTimeScale(raf: RandomAccessFile, atoms: List<Mp4Atom>): Int? {
    val movieHeaderAtom = atoms.findAtom("moov", "mvhd")
        ?: return null
    raf.seek(movieHeaderAtom.position + 8)
    val version = raf.readByte().toInt()
    if (version != 0 && version != 1) return null

    val sizeFlags = 3
    val sizeCreationTime = if (version == 0) 4 else 8
    val sizeModificationTime = if (version == 0) 4 else 8

    raf.skipBytes(sizeFlags + sizeCreationTime + sizeModificationTime)
    return raf.readInt()
  }
}