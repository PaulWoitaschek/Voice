package de.ph1b.audiobook.uitools;/*
 * Copyright (C) 2014 AChep@xda <artemchep@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

import android.util.Property;

/**
 * An implementation of {@link android.util.Property} to be used specifically with fields of type
 * <code>float</code>. This type-specific subclass enables performance benefit by allowing
 * calls to a {@link #set(Object, Float) set()} function that takes the primitive
 * <code>float</code> type and avoids autoboxing and other overhead associated with the
 * <code>Float</code> class.
 *
 * @param <T> The class on which the Property is declared.
 */
abstract class FloatProperty<T> extends Property<T, Float> {

    public FloatProperty() {
        super(Float.class, "property");
    }

    /**
     * A type-specific override of the {@link #set(Object, Float)} that is faster when dealing
     * with fields of type <code>float</code>.
     */
    public abstract void setValue(T object, float value);

    @Override
    final public void set(T object, Float value) {
        setValue(object, value);
    }

}