package com.am.clipboard;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.AbstractCursor;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ClipboardProvider extends ContentProvider {

    private static final String AUTHORITY = "com.am.clipboard.clipboardprovider";
    private static final Uri URI_BASE = Uri.parse(ContentResolver.SCHEME_CONTENT + "://" + AUTHORITY);
    private static final String PATH_ITEM = "item";
    private static final String PATH_CLEAR = "clear";
    private static final String PATH_DELETE = "delete";
    private static final String PATH_CHECK = "check";
    private static final String MODE_WRITE = "w";
    private static final String MODE_READ = "r";
    private static final int CODE_ITEM = 1;
    private static final int CODE_CLEAR = 2;
    private static final int CODE_DELETE = 3;
    private static final int CODE_CHECK = 4;
    private final UriMatcher mMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private File mDirectory;// 剪切板文件夹

    private static Uri getUri(String pathSegment) {
        return Uri.withAppendedPath(URI_BASE, pathSegment);
    }

    static void delete(Context context, List<Uri> uris) {
        final ArrayList<String> names = new ArrayList<>();
        for (Uri uri : uris) {
            final List<String> segments = uri.getPathSegments();
            if (segments == null || segments.size() != 3 || !PATH_ITEM.equals(segments.get(0))) {
                continue;
            }
            final String name = segments.get(2);
            if (TextUtils.isEmpty(name)) {
                continue;
            }
            names.add(name);
        }
        context.getContentResolver().delete(getUri(PATH_DELETE), null,
                names.toArray(new String[0]));
    }

    static ArrayList<Uri> write(Context context, SuperClipboard.OutputAdapter adapter,
                                Set<String> mimeTypes) {
        if (context == null || adapter == null || mimeTypes == null) {
            return new ArrayList<>();
        }
        final ContentResolver resolver = context.getContentResolver();
        final int count = adapter.getCount();
        if (count <= 0) {
            return new ArrayList<>();
        }
        final ArrayList<Uri> uris = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final String mimeType = adapter.getMimeType(i);
            if (TextUtils.isEmpty(mimeType)) {
                mimeTypes.clear();
                return new ArrayList<>();
            }
            final String name = UUID.randomUUID().toString();
            final Uri uri = getUri(PATH_ITEM + "/" + Uri.encode(mimeType) + "/" + name);
            boolean success = false;
            try (final ParcelFileDescriptor descriptor =
                         resolver.openFileDescriptor(uri, MODE_WRITE)) {
                success = adapter.write(i, descriptor);
            } catch (Exception e) {
                // do nothing
                e.printStackTrace();
            }
            if (success) {
                mimeTypes.add(mimeType);
                uris.add(uri);
            } else {
                mimeTypes.clear();
                return new ArrayList<>();
            }
        }
        return uris;
    }

    static void clear(Context context) {
        context.getContentResolver().delete(getUri(PATH_CLEAR), null, null);
    }

    static boolean read(Context context, SuperClipboard.InputAdapter adapter, Uri uri) {
        final List<String> segments = uri.getPathSegments();
        if (segments == null || segments.size() != 3 || !PATH_ITEM.equals(segments.get(0))) {
            return false;
        }
        final String mimeType = segments.get(1);
        if (TextUtils.isEmpty(mimeType)) {
            return false;
        }
        try (final ParcelFileDescriptor descriptor =
                     context.getContentResolver().openFileDescriptor(uri, MODE_READ)) {
            return adapter.read(mimeType, descriptor);
        } catch (Exception e) {
            return false;
        }
    }

    static boolean check(Context context, String mimeType, Uri uri) {
        final List<String> segments = uri.getPathSegments();
        if (segments == null || segments.size() != 3 || !PATH_ITEM.equals(segments.get(0))) {
            return false;
        }
        if (mimeType != null &&
                !TextUtils.equals(mimeType, Uri.decode(segments.get(1)))) {
            return false;
        }
        final Uri check = getUri(PATH_CHECK + "/" + segments.get(2));
        try (final Cursor cursor = context.getContentResolver().query(check,
                null, null, null, null)) {
            if (cursor != null) {
                boolean result = false;
                if (cursor.moveToFirst()) {
                    final byte[] blob = cursor.getBlob(0);
                    result = blob != null && blob.length == 1 && blob[0] != 0;
                }
                return result;
            }
        }
        return false;
    }

    @Override
    public boolean onCreate() {
        final String authority = AUTHORITY;
        mMatcher.addURI(authority, PATH_ITEM + "/*/*", CODE_ITEM);
        mMatcher.addURI(authority, PATH_CLEAR, CODE_CLEAR);
        mMatcher.addURI(authority, PATH_DELETE, CODE_DELETE);
        mMatcher.addURI(authority, PATH_CHECK + "/*", CODE_CHECK);
        mDirectory = getContext().getExternalFilesDir("SuperClipboard");
        if (mDirectory == null) {
            mDirectory = new File(getContext().getFilesDir(), "SuperClipboard");
        }
        //noinspection ResultOfMethodCallIgnored
        mDirectory.mkdirs();
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        if (mMatcher.match(uri) != CODE_CHECK) {
            return null;
        }
        final List<String> segments = uri.getPathSegments();
        if (segments == null || segments.size() != 2 || !PATH_CHECK.equals(segments.get(0))) {
            return null;
        }
        final String name = segments.get(1);
        return new ClipboardCursor(mDirectory != null && new File(mDirectory, name).exists());
    }

    @Override
    public String getType(Uri uri) {
        if (mMatcher.match(uri) != CODE_ITEM) {
            return null;
        }
        final List<String> segments = uri.getPathSegments();
        if (segments == null || segments.size() != 3 || !PATH_ITEM.equals(segments.get(0))) {
            return null;
        }
        return Uri.decode(segments.get(1));
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (mDirectory == null) {
            return 0;
        }
        if (mMatcher.match(uri) == CODE_CLEAR) {
            return clear();
        }
        if (mMatcher.match(uri) == CODE_DELETE) {
            final HashSet<String> names = new HashSet<>(Arrays.asList(selectionArgs));
            final File[] children = mDirectory.listFiles((dir, name) -> !names.contains(name));
            int count = 0;
            if (children != null) {
                for (File child : children) {
                    if (FileHelper.delete(child)) {
                        count++;
                    }
                }
            }
            return count;
        }
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (mMatcher.match(uri) != CODE_ITEM) {
            return super.openFile(uri, mode);
        }
        if (mDirectory == null) {
            throw new FileNotFoundException("Cannot get directory.");
        }
        final List<String> segments = uri.getPathSegments();
        if (segments == null || segments.size() != 3 || !PATH_ITEM.equals(segments.get(0))) {
            throw new FileNotFoundException("Uri error at " + uri);
        }
        final String name = segments.get(2);
        if (TextUtils.isEmpty(name)) {
            throw new FileNotFoundException("Uri error at " + uri);
        }
        final File file = new File(mDirectory, name);
        if (MODE_WRITE.equals(mode)) {
            // 写入
            return ParcelFileDescriptor.open(file,
                    ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_CREATE);
        } else if (MODE_READ.equals(mode)) {
            // 读取
            if (file.exists()) {
                return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            }
            return null;
        } else {
            throw new FileNotFoundException("Mode error at " + uri);
        }
    }

    private int clear() {
        return FileHelper.clearDirectory(mDirectory);
    }

    private static class ClipboardCursor extends AbstractCursor {

        private static final String NAME_DATA = "data";
        private final byte[] mBlob;

        ClipboardCursor(boolean value) {
            mBlob = value ? new byte[]{1} : new byte[]{0};
        }

        @Override
        public int getCount() {
            return 1;
        }

        @Override
        public String[] getColumnNames() {
            return new String[]{NAME_DATA};
        }

        @Override
        public byte[] getBlob(int column) {
            return column == 0 ? mBlob : null;
        }

        @Override
        public String getString(int column) {
            throw new UnsupportedOperationException("getString is not supported");
        }

        @Override
        public short getShort(int column) {
            throw new UnsupportedOperationException("getShort is not supported");
        }

        @Override
        public int getInt(int column) {
            throw new UnsupportedOperationException("getInt is not supported");
        }

        @Override
        public long getLong(int column) {
            throw new UnsupportedOperationException("getLong is not supported");
        }

        @Override
        public float getFloat(int column) {
            throw new UnsupportedOperationException("getFloat is not supported");
        }

        @Override
        public double getDouble(int column) {
            throw new UnsupportedOperationException("getDouble is not supported");
        }

        @Override
        public boolean isNull(int column) {
            return column != 0;
        }
    }
}
