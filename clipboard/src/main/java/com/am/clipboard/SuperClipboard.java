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

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * 超级剪切板
 */
public class SuperClipboard {

    private SuperClipboard() {
        //no instance
    }

    private static ClipboardManager getClipboardManager(Context context) {
        if (Build.VERSION.SDK_INT >= 23) {
            return context.getSystemService(ClipboardManager.class);
        } else {
            return (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        }
    }

    /**
     * 获取MIME，基础类型为游标子项
     *
     * @param subtype 子类型，如：vnd.clipboard.data
     * @return 自定义MIME
     */
    public static String getMime(String subtype) {
        return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + subtype;
    }

    private static void delete(Context context, ClipData excluded) {
        final ArrayList<Uri> uris = new ArrayList<>();
        if (excluded != null) {
            final int count = excluded.getItemCount();
            if (count > 0) {
                for (int i = 0; i < count; i++) {
                    final Uri uri = excluded.getItemAt(i).getUri();
                    if (uri != null) {
                        uris.add(uri);
                    }
                }
            }
        }
        ClipboardProvider.delete(context, uris);
    }

    /**
     * 设置剪切板
     *
     * @param context Context
     * @param adapter 输出内容提供者
     * @return 设置成功时返回true
     */
    public static boolean setPrimaryClip(Context context, OutputAdapter adapter) {
        final ClipboardManager manager = getClipboardManager(context);
        if (manager == null) {
            return false;
        }
        final HashSet<String> mimeTypes = new HashSet<>();
        final ArrayList<Uri> uris = ClipboardProvider.write(context, adapter, mimeTypes);
        if (uris.isEmpty()) {
            return false;
        }
        final ClipDescription description = new ClipDescription("URI",
                mimeTypes.toArray(new String[0]));
        final ClipData data = new ClipData(description, new ClipData.Item(uris.get(0)));
        final int size = uris.size();
        for (int i = 1; i < size; i++) {
            data.addItem(new ClipData.Item(uris.get(i)));
        }
        delete(context, data);
        manager.setPrimaryClip(data);
        return true;
    }

    /**
     * 设置剪切板
     *
     * @param context  Context
     * @param mimeType MIME类型
     * @param items    子项
     * @return 设置成功时返回true
     */
    public static boolean setPrimaryClip(Context context,
                                         String mimeType, Serializable... items) {
        return items != null && items.length > 0 && setPrimaryClip(context,
                new SerializableHelper.SerializableOutputAdapter(mimeType, items));
    }

    /**
     * 设置剪切板
     *
     * @param context   Context
     * @param mimeTypes MIME类型集合
     * @param items     子项集合
     * @return 设置成功时返回true
     */
    public static boolean setPrimaryClip(Context context,
                                         String[] mimeTypes, Serializable[] items) {
        return mimeTypes != null && items != null &&
                mimeTypes.length == items.length && setPrimaryClip(context,
                new SerializableHelper.SerializableOutputAdapter(mimeTypes, items));
    }

    /**
     * 设置剪切板
     *
     * @param context  Context
     * @param mimeType MIME类型
     * @param files    文件
     * @return 设置成功时返回true
     */
    public static boolean setPrimaryClip(Context context, String mimeType, File... files) {
        return files != null && files.length > 0 && setPrimaryClip(context,
                new FileHelper.FileOutputAdapter(mimeType, files));
    }

    /**
     * 设置剪切板
     *
     * @param context   Context
     * @param mimeTypes MIME类型集合
     * @param files     文件合集
     * @return 设置成功时返回true
     */
    public static boolean setPrimaryClip(Context context,
                                         String[] mimeTypes, File[] files) {
        return mimeTypes != null && files != null &&
                mimeTypes.length == files.length && setPrimaryClip(context,
                new FileHelper.FileOutputAdapter(mimeTypes, files));
    }

    /**
     * 清空剪切板
     *
     * @param context Context
     * @return 清空成功时返回true
     */
    public static boolean clearPrimaryClip(Context context) {
        final ClipboardManager manager = getClipboardManager(context);
        if (manager == null) {
            return false;
        }
        ClipboardProvider.clear(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            manager.clearPrimaryClip();
        } else {
            manager.setPrimaryClip(ClipData.newPlainText("TEXT", ""));
        }
        return true;
    }

    /**
     * 获取剪切板数据
     *
     * @param context Context
     * @param adapter 输入内容提供者
     * @return 获取成功时返回true
     */
    public static boolean getPrimaryClip(Context context, InputAdapter adapter) {
        final ClipboardManager manager = getClipboardManager(context);
        if (manager == null || !manager.hasPrimaryClip()) {
            return false;
        }
        final ClipData data = manager.getPrimaryClip();
        if (data == null) {
            return false;
        }
        final int count = data.getItemCount();
        if (count <= 0) {
            return false;
        }
        for (int i = 0; i < count; i++) {
            final Uri uri = data.getItemAt(i).getUri();
            if (uri == null) {
                // 该情况不应该出现
                return false;
            }
            if (!ClipboardProvider.read(context, adapter, uri)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取剪切板序列化数据
     *
     * @param context Context
     * @return 序列化数据
     */
    public static <T extends Serializable> T getPrimaryClipSerializable(Context context) {
        final SerializableHelper.SerializableInputAdapter input =
                new SerializableHelper.SerializableInputAdapter();
        if (getPrimaryClip(context, input)) {
            final ArrayList<Serializable> items = input.getItems();
            if (!items.isEmpty()) {
                //noinspection unchecked
                return (T) items.get(0);
            }
        }
        return null;
    }

    /**
     * 获取剪切板序列化数据集
     *
     * @param context Context
     * @return 序列化数据集，结果可能为空
     */
    public static <T extends Serializable> List<T> getPrimaryClipSerializables(Context context) {
        final SerializableHelper.SerializableInputAdapter input =
                new SerializableHelper.SerializableInputAdapter();
        if (getPrimaryClip(context, input)) {
            final ArrayList<Serializable> items = input.getItems();
            if (!items.isEmpty()) {
                //noinspection unchecked
                return (List<T>) items;
            }
        }
        return null;
    }

    /**
     * 获取剪切板文件数据
     *
     * @param context Context
     * @param file    用于写入的文件
     * @return 获取成功时返回true
     */
    public static boolean getPrimaryClipFile(Context context, File file) {
        return getPrimaryClip(context, new FileHelper.FileInputAdapter(file));
    }

    /**
     * 获取剪切板文件集
     *
     * @param context   Context
     * @param directory 用于写入的目录
     * @return 文件集，结果可能为空
     */
    public static List<File> getPrimaryClipFiles(Context context, File directory) {
        final FileHelper.DirectoryInputAdapter input =
                new FileHelper.DirectoryInputAdapter(directory);
        if (getPrimaryClip(context, input)) {
            final ArrayList<File> items = input.getItems();
            if (!items.isEmpty()) {
                return items;
            }
        }
        return null;
    }


    /**
     * 判断剪切板是否包含该类型数据
     *
     * @param context   Context
     * @param mimeType  MIME类型
     * @param checkData 是否检查数据
     * @return 包含该类型数据时返回true
     */
    public static boolean contains(Context context, String mimeType, boolean checkData) {
        final ClipboardManager manager = getClipboardManager(context);
        if (manager == null || !manager.hasPrimaryClip()) {
            return false;
        }
        final ClipData data = manager.getPrimaryClip();
        if (data == null) {
            return false;
        }
        if (!checkData) {
            return data.getDescription().hasMimeType(mimeType);
        }
        if (!data.getDescription().hasMimeType(mimeType)) {
            return false;
        }
        final int count = data.getItemCount();
        if (count <= 0) {
            return false;
        }
        boolean success = false;
        for (int i = 0; i < count; i++) {
            final Uri uri = data.getItemAt(i).getUri();
            if (uri == null) {
                // 该情况不应该出现
                return false;
            }
            if (ClipboardProvider.check(context, mimeType, uri)) {
                success = true;
            }
        }
        return success;
    }

    /**
     * 检查剪切板
     * 清除不在剪切板内的数据
     *
     * @param context Context
     */
    public static void check(Context context) {
        final ClipboardManager manager = getClipboardManager(context);
        if (manager == null) {
            ClipboardProvider.clear(context);
            return;
        }
        if (!manager.hasPrimaryClip()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                // Android 10及以上，仅默认输入法或者应用已获取到焦点，否则无法访问剪切板。
                ClipboardProvider.clear(context);
            }
            return;
        }
        final ClipData data = manager.getPrimaryClip();
        if (data == null) {
            ClipboardProvider.clear(context);
            return;
        }
        final int count = data.getItemCount();
        if (count <= 0) {
            return;
        }
        for (int i = 0; i < count; i++) {
            final Uri uri = data.getItemAt(i).getUri();
            if (uri != null && ClipboardProvider.check(context, null, uri)) {
                continue;
            }
            ClipboardProvider.clear(context);
            return;
        }
    }

    /**
     * 输出内容提供者
     */
    public interface OutputAdapter {

        /**
         * 获取总数
         *
         * @return 总数
         */
        int getCount();

        /**
         * 获取MIME类型
         *
         * @param position 位置
         * @return MIME类型
         */
        String getMimeType(int position);

        /**
         * 写入
         *
         * @param position   位置
         * @param descriptor 文件
         * @return 是否成功
         */
        boolean write(int position, ParcelFileDescriptor descriptor);
    }

    /**
     * 输入内容提供者
     */
    public interface InputAdapter {

        /**
         * 读取
         *
         * @param mimeType   MIME类型
         * @param descriptor 文件
         * @return 读取成功时返回true
         */
        boolean read(String mimeType, ParcelFileDescriptor descriptor);
    }
}
