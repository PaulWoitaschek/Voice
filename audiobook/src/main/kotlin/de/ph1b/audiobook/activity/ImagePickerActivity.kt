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

package de.ph1b.audiobook.activity

import android.os.Bundle
import android.support.v7.widget.Toolbar
import butterknife.bindView
import de.ph1b.audiobook.R
import de.ph1b.audiobook.fragment.ImagePickerFragment
import java.io.Serializable
import de.ph1b.audiobook.fragment.ImagePickerFragment.Companion.Args as FragmentArgs

/**
 * Created by ph1b on 13/02/16.
 */
class ImagePickerActivity : BaseActivity(), ImagePickerFragment.Callback {

    private val FM_IMAGE_PICKER = "imagePickerFragment"
    private val toolBar: Toolbar by bindView(R.id.toolbar)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = intent.getSerializableExtra(NI) as Initializer

        setContentView(R.layout.activity_image_picker)
        setSupportActionBar(toolBar)
        if (savedInstanceState == null) {
            val pickerArgs = FragmentArgs(args.bookId)
            supportFragmentManager.beginTransaction()
                    .add(R.id.fragmentContainer, ImagePickerFragment.newInstance(pickerArgs), FM_IMAGE_PICKER)
                    .commit()
        }
    }

    override fun onBackPressed() {
        val imagePickerFragment = supportFragmentManager.findFragmentByTag(FM_IMAGE_PICKER) as ImagePickerFragment

        val backHandled = imagePickerFragment.backPressed()
        if (!backHandled) super.onBackPressed()
    }

    override fun editDone() {
        supportFinishAfterTransition()
    }

    companion object {
        private val NI = "ni"

        fun arguments(initializer: Initializer) = Bundle().apply {
            putSerializable(NI, initializer)
        }

        data class Initializer(val bookId: Long) : Serializable
    }
}
