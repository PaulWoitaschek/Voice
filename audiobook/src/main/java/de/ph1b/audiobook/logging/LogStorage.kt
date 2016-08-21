package de.ph1b.audiobook.logging

import de.ph1b.audiobook.logging.LogStorage.AMOUNT_OF_ENTRIES
import java.text.SimpleDateFormat
import java.util.*
import java.util.Date as DateWithTime

/** Storage for logs. Stores up to [AMOUNT_OF_ENTRIES] logs for each day. After that removes the first */
object LogStorage {

    private const val AMOUNT_OF_ENTRIES = 1000

    private val logs = ArrayList<String>(AMOUNT_OF_ENTRIES)
    private val dateField = java.util.Date()
    private val format = SimpleDateFormat("HH:mm:ss")

    fun put(message: String) {
        // remove items if there are too many
        if (logs.size > AMOUNT_OF_ENTRIES) {
            logs.removeAt(0)
        }
        // add a timestamp
        dateField.time = System.currentTimeMillis()
        val stampMessage = "${format.format(dateField)}\t$message"
        logs.add(stampMessage)
    }

    fun get(): List<String> = logs
}