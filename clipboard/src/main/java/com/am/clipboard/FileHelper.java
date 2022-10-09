package com.am.clipboard;

import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

/**
 * 文件辅助
 * Created by Alex on 2022/10/9.
 */
class FileHelper {

    private FileHelper() {
        //no instance
    }

    private static void copy(InputStream input, OutputStream output) throws IOException {
        final byte[] buffer = new byte[1024];
        int count;
        while ((count = input.read(buffer)) != -1) {
            if (count == 0) {
                count = input.read();
                if (count < 0)
                    break;
                output.write(count);
                continue;
            }
            output.write(buffer, 0, count);
            output.flush();
        }
    }

    private static boolean copyFile(File source, OutputStream target) {
        try (final FileInputStream input = new FileInputStream(source)) {
            copy(input, target);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean copyFile(InputStream source, File target) {
        try (final FileOutputStream output = new FileOutputStream(target)) {
            copy(source, output);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * 删除文件或文件夹
     *
     * @param file 文件或文件夹
     * @return 删除成功时返回true
     */
    public static boolean delete(File file) {
        if (file == null) {
            return true;
        }
        if (!file.exists()) {
            return true;
        }
        if (file.isFile()) {
            return file.delete();
        }
        if (file.isDirectory()) {
            final File[] children = file.listFiles();
            if (children == null || children.length <= 0) {
                return file.delete();
            }
            for (File child : children) {
                if (!delete(child)) {
                    return false;
                }
            }
            return file.delete();
        }
        return false;
    }

    /**
     * 清空文件夹
     *
     * @param dir 文件夹
     * @return 删除的子项个数
     */
    public static int clearDirectory(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return 0;
        }
        final File[] children = dir.listFiles();
        if (children == null || children.length <= 0) {
            return 0;
        }
        int count = 0;
        for (File child : children) {
            if (delete(child)) {
                count++;
            }
        }
        return count;
    }

    public static class FileOutputAdapter implements SuperClipboard.OutputAdapter {

        private final String mMimeType;
        private final String[] mMimeTypes;
        private final File[] mItems;

        public FileOutputAdapter(String mimeType, File... files) {
            mMimeType = mimeType;
            mMimeTypes = null;
            mItems = files;
        }

        public FileOutputAdapter(String[] mimeTypes, File[] items) {
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
            final File item = mItems[position];
            try (final OutputStream output =
                         new ParcelFileDescriptor.AutoCloseOutputStream(descriptor)) {
                return copyFile(item, output);
            } catch (Exception e) {
                return false;
            }
        }
    }

    public static class FileInputAdapter implements SuperClipboard.InputAdapter {

        private final File mFile;

        public FileInputAdapter(File file) {
            mFile = file;
        }

        @Override
        public boolean read(String mimeType, ParcelFileDescriptor descriptor) {
            try (final InputStream input =
                         new ParcelFileDescriptor.AutoCloseInputStream(descriptor)) {
                if (mFile != null) {
                    return copyFile(input, mFile);
                } else {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        }
    }

    public static class DirectoryInputAdapter implements SuperClipboard.InputAdapter {

        private final File mDirectory;
        private final ArrayList<File> mItems = new ArrayList<>();

        public DirectoryInputAdapter(File directory) {
            mDirectory = directory;
        }

        @Override
        public boolean read(String mimeType, ParcelFileDescriptor descriptor) {
            try (final InputStream input =
                         new ParcelFileDescriptor.AutoCloseInputStream(descriptor)) {
                final File file = new File(mDirectory, UUID.randomUUID().toString());
                if (copyFile(input, file)) {
                    mItems.add(file);
                    return true;
                }
                //noinspection ResultOfMethodCallIgnored
                file.delete();
                return false;
            } catch (Exception e) {
                return false;
            }
        }

        public ArrayList<File> getItems() {
            return mItems;
        }
    }
}
