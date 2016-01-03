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
 * along with Material Audiobook Player. If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

package de.ph1b.audiobook.injection;

import dagger.Module;
import dagger.Provides;
import de.ph1b.audiobook.mediaplayer.MediaPlayerController;
import de.ph1b.audiobook.model.BookAdder;
import de.ph1b.audiobook.persistence.BookChest;
import de.ph1b.audiobook.persistence.PrefsManager;
import de.ph1b.audiobook.playback.PlayStateManager;
import de.ph1b.audiobook.presenter.BookShelfBasePresenter;
import de.ph1b.audiobook.presenter.BookShelfPresenter;

/**
 * Module for providing presenters.
 *
 * @author Paul Woitaschek
 */
@Module
public class PresenterModule {

    @Provides
    BookShelfBasePresenter provideBookShelfBasePresenter(
            BookChest bookChest, BookAdder bookAdder, PrefsManager prefsManager,
            PlayStateManager playStateManager, MediaPlayerController mediaPlayerController) {
        return new BookShelfPresenter(bookChest, bookAdder, prefsManager, playStateManager, mediaPlayerController);
    }
}
