package de.ph1b.audiobook.data.di

import de.ph1b.audiobook.data.repo.internals.Converters

interface DataComponent {

  fun inject(converters: Converters)
}
