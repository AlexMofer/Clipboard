/*
 * Copyright (C) 2021 AlexMofer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.am.clipboard;

import android.os.ParcelFileDescriptor;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * 序列化数据辅助器
 */
class SerializableHelper {

    private SerializableHelper() {
        //no instance
    }

    public static class SerializableOutputAdapter implements SuperClipboard.OutputAdapter {

        private final String mMimeType;
        private final String[] mMimeTypes;
        private final Serializable[] mItems;

        public SerializableOutputAdapter(String mimeType, Serializable... items) {
            mMimeType = mimeType;
            mMimeTypes = null;
            mItems = items;
        }

        public SerializableOutputAdapter(String[] mimeTypes, Serializable[] items) {
            mMimeType = null;
            mMimeTypes = mimeTypes;
            mItems = items;
        }

        @Override
        public int getCount() {
            return mItems.length;
        }

        @Override
        public String getMimeType(int position) {
            //noinspection ConstantConditions
            return mMimeType != null ? mMimeType : mMimeTypes[position];
        }

        @Override
        public boolean write(int position, ParcelFileDescriptor descriptor) {
            final Serializable item = mItems[position];
            try (final ObjectOutputStream output = new ObjectOutputStream(
                    new ParcelFileDescriptor.AutoCloseOutputStream(descriptor))) {
                output.writeObject(item);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    public static class SerializableInputAdapter implements SuperClipboard.InputAdapter {

        private final ArrayList<Serializable> mItems = new ArrayList<>();

        @Override
        public boolean read(String mimeType, ParcelFileDescriptor descriptor) {
            try (final ObjectInputStream input = new ObjectInputStream(
                    new ParcelFileDescriptor.AutoCloseInputStream(descriptor))) {
                mItems.add((Serializable) input.readObject());
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        public ArrayList<Serializable> getItems() {
            return mItems;
        }
    }
}
