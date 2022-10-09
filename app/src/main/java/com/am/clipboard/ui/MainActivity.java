package com.am.clipboard.ui;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.am.appcompat.app.AppCompatActivity;
import com.am.clipboard.SuperClipboard;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Serializable;
import java.util.Random;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private final ClipboardBean mData = ClipboardBean.test();
    private File mFile;

    public MainActivity() {
        super(R.layout.activity_main);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFile = new File(getExternalCacheDir(), UUID.randomUUID().toString());
        writeString(mFile, "This is file content, created at " + System.currentTimeMillis());

        SuperClipboard.check(this);
        findViewById(R.id.clipboard_btn_copy_data).setOnClickListener(v -> copyData());
        findViewById(R.id.clipboard_btn_paste_data).setOnClickListener(v -> pasteData());
        findViewById(R.id.clipboard_btn_copy_file).setOnClickListener(v -> copyFile());
        findViewById(R.id.clipboard_btn_paste_file).setOnClickListener(v -> pasteFile());
        this.<TextView>findViewById(R.id.clipboard_tv_target_data).setText(mData.toString());
        this.<TextView>findViewById(R.id.clipboard_tv_target_file).setText(readString(mFile));
    }

    private void copyData() {
        if (SuperClipboard.setPrimaryClip(this,
                SuperClipboard.getMime("vnd.projectx.data"), mData)) {
            Toast.makeText(this, R.string.clipboard_info,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void pasteData() {
        final ClipboardBean result = SuperClipboard.getPrimaryClipSerializable(this);
        if (result != null) {
            this.<TextView>findViewById(R.id.clipboard_tv_result_data).setText(result.toString());
        }
    }

    private void copyFile() {
        if (SuperClipboard.setPrimaryClip(this,
                SuperClipboard.getMime("vnd.projectx.file"), mFile)) {
            Toast.makeText(this, R.string.clipboard_info,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void pasteFile() {
        final File temp = new File(getExternalCacheDir(), UUID.randomUUID().toString());
        if (SuperClipboard.getPrimaryClipFile(this, temp)) {
            this.<TextView>findViewById(R.id.clipboard_tv_result_file).setText(readString(temp));
            //noinspection ResultOfMethodCallIgnored
            temp.delete();
        }
    }

    private static class ClipboardBean implements Serializable {
        private final byte mByte;
        private final short mShort;
        private final int mInt;
        private final long mLong;
        private final float mFloat;
        private final double mDouble;
        private final boolean mBoolean;
        private final char mChar;
        private final String mString;

        private ClipboardBean(byte mByte, short mShort, int mInt, long mLong, float mFloat,
                              double mDouble, boolean mBoolean, char mChar, String mString) {
            this.mByte = mByte;
            this.mShort = mShort;
            this.mInt = mInt;
            this.mLong = mLong;
            this.mFloat = mFloat;
            this.mDouble = mDouble;
            this.mBoolean = mBoolean;
            this.mChar = mChar;
            this.mString = mString;
        }

        static ClipboardBean test() {
            final Random random = new Random();
            final int v = random.nextInt(250);
            return new ClipboardBean((byte) (127 - v), (short) (32767 - v), Integer.MAX_VALUE / v,
                    Long.MAX_VALUE / v, Float.MAX_VALUE / v,
                    Double.MAX_VALUE / v, true, Character.MAX_VALUE,
                    "Test:" + v);
        }

        @NonNull
        @Override
        public String toString() {
            return "ClipboardBean{" +
                    "byte=" + mByte +
                    ", short=" + mShort +
                    ", int=" + mInt +
                    ", long=" + mLong +
                    ", float=" + mFloat +
                    ", double=" + mDouble +
                    ", boolean=" + mBoolean +
                    ", char=" + mChar +
                    ", string='" + mString + '\'' +
                    '}';
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    private static boolean writeString(File file, String content) {
        if (content == null)
            return file.delete();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(content);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String readString(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return null;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            final StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
}