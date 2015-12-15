/*
 * This file is part of Material Audiobook Player.
 *
 * Material Audiobook Player is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * Material Audiobook Player is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

package de.ph1b.audiobook.utils

import de.ph1b.audiobook.model.Book
import de.ph1b.audiobook.persistence.BookChest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simple helper that provides blocking access to [BookChest]

 * @author Paul Woitaschek
 */
@Singleton
class BookVendor
@Inject
constructor(private val bookChest: BookChest) {

    fun byId(id: Long): Book? {
        return bookChest.activeBooks
                .toBlocking()
                .singleOrDefault(null, { it.id == id })
    }

    fun all(): List<Book> {
        return bookChest.activeBooks.toList().toBlocking().first()
    }
}
