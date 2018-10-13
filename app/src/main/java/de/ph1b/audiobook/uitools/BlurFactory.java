package de.ph1b.audiobook.uitools;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Copyright (C) 2018  Felix Nüsse
 * Created on 13.10.18 - 16:25
 *
 * Edited by: Felix Nüsse felix.nuesse(at)t-online.de
 *
 *
 * This program is released under the GPLv3 license
 *
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 */

//https://www.ssaurel.com/blog/create-a-blur-effect-on-android-with-renderscript/
public class BlurFactory {

    private static final float BITMAP_SCALE = 0.6f;
    private static final float BLUR_RADIUS = 15f;

    public static File blurBitmap(File file, Context context) {

      String filePath = file.getPath();
      Bitmap image = BitmapFactory.decodeFile(filePath);

      int width = Math.round(image.getWidth() * BITMAP_SCALE);
      int height = Math.round(image.getHeight() * BITMAP_SCALE);

      Bitmap inputBitmap = Bitmap.createScaledBitmap(image, width, height, false);
      Bitmap outputBitmap = Bitmap.createBitmap(inputBitmap);

      RenderScript rs = RenderScript.create(context);

      ScriptIntrinsicBlur intrinsicBlur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
      Allocation tmpIn = Allocation.createFromBitmap(rs, inputBitmap);
      Allocation tmpOut = Allocation.createFromBitmap(rs, outputBitmap);

      intrinsicBlur.setRadius(BLUR_RADIUS);
      intrinsicBlur.setInput(tmpIn);
      intrinsicBlur.forEach(tmpOut);
      tmpOut.copyTo(outputBitmap);

      return bitmapToFile(outputBitmap, context);
    }


  private static File bitmapToFile(Bitmap b, Context context) {
    File filesDir = context.getFilesDir();
    File f = new File(filesDir,"cover.png");

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    b.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
    byte[] bitmapdata = bos.toByteArray();

    try {
      FileOutputStream fos = new FileOutputStream(f);
      fos.write(bitmapdata);
      fos.flush();
      fos.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return f;
  }



}
