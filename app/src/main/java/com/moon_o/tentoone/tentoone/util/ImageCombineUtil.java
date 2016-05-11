package com.moon_o.tentoone.tentoone.util;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by mucha on 16. 4. 27.
 */
public class ImageCombineUtil {
    private static ImageCombineUtil _instance;

    private final String BASE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pictures/TenToOne/";
    public final String MEDIA_EXTERNAL_PATH = MediaStore.Images.Media.EXTERNAL_CONTENT_URI.getPath();

    private List<String> imagePathArray = new ArrayList<>();
    private String name;
    private String combinedPath;
    private boolean executable = false;

    private ImageCombineUtil() {}

    public synchronized static ImageCombineUtil getInstance() {
        if(_instance == null)
            _instance = new ImageCombineUtil();
        return _instance;
    }

    public List<String> getImagePathArray() {
        return imagePathArray;
    }

    public void setImagePathArray(List<String> imagePathArray) {
        this.imagePathArray = imagePathArray;
    }

    public boolean isExecutable() {
        return this.executable;
    }

    public void setExecutable(boolean executable) {
        this.executable = executable;
    }

    public String getBasePath() {
        return BASE_PATH;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCombinedPath() {
        return combinedPath;
    }

    public void setCombinedPath(String combinedPath) {
        this.combinedPath = combinedPath;
    }

    public String pathCreat() {
        setName("TenToOne_"+new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date()) + ".jpg");
        imagePathArray.add(BASE_PATH + name);
//        setPath(BASE_PATH + name);
        return imagePathArray.get(imagePathArray.size()-1);
    }

    public void fileCreate(File file) {
        File folder = new File(BASE_PATH);
        if(folder != null && !folder.exists())
            folder.mkdir();

        if(file != null && !file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean fileDelete(String path) {
        try {
            File file = new File(path);
            if (file.exists())
                file.delete();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public void bitmapFileWrite(String path, Bitmap bitmap) {
        File backFile = new File(path);
        fileCreate(backFile);

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(backFile, true);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 55, out);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bitmap.recycle();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    public void bitmapCombine(String combinedPath, String combinePath, Bitmap backupBitmap, Bitmap capturedBitmap) {
        setCombinedPath(combinedPath);
        fileCreate(new File(combinePath));
        Bitmap backupCopyBitmap, capturedCopyBitmap, combineBitmap;
        backupCopyBitmap = backupBitmap.copy(Bitmap.Config.ARGB_4444, true);
        capturedCopyBitmap = capturedBitmap.copy(Bitmap.Config.ARGB_4444, true);

        int w;
        if(backupCopyBitmap.getWidth() > capturedCopyBitmap.getWidth())
            w = backupCopyBitmap.getWidth();
        else
            w = capturedCopyBitmap.getWidth();

        int h = backupCopyBitmap.getHeight() + capturedCopyBitmap.getHeight();


        combineBitmap =Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_4444);
//                createScaledBitmap(backupCopyBitmap, w, h, true);

        Canvas canvas = new Canvas(combineBitmap);
        canvas.drawBitmap(backupCopyBitmap, 0, 0, null);
        canvas.drawBitmap(capturedCopyBitmap, 0, backupCopyBitmap.getHeight(), null);

        bitmapFileWrite(combinePath, combineBitmap);

        backupCopyBitmap.recycle();
        capturedCopyBitmap.recycle();
        backupBitmap.recycle();
        capturedBitmap.recycle();
        combineBitmap.recycle();
    }

    public static Bitmap createScaledBitmap(String path, int width, int height) {

        BitmapFactory.Options option = new BitmapFactory.Options();

        option.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, option);

        if (option.outWidth < width || option.outHeight < height) {
            return BitmapFactory.decodeFile(path);
        }

        float scaleWidth = ((float) width) / option.outWidth;
        float scaleHeight = ((float) height) / option.outHeight;

        int newSize = 0;
        int oldSize = 0;
        if (scaleWidth > scaleHeight) {
            newSize = width;
            oldSize = option.outWidth;
        } else {
            newSize = height;
            oldSize = option.outHeight;
        }

        int sampleSize = 1;
        int tmpSize = oldSize;
        while (tmpSize > newSize) {
            sampleSize = sampleSize * 2;
            tmpSize = oldSize / sampleSize;
        }
        if (sampleSize != 1) {
            sampleSize = sampleSize / 2;
        }

        option.inJustDecodeBounds = false;
        option.inSampleSize = sampleSize;

        return BitmapFactory.decodeFile(path, option);
    }

    public static Bitmap resize(Bitmap bitmap, int newWidth, int newHeight) {

        if (bitmap == null) {
            return null;
        }

        int oldWidth = bitmap.getWidth();
        int oldHeight = bitmap.getHeight();

        if (oldWidth < newWidth && oldHeight < newHeight) {
            return bitmap;
        }

        float scaleWidth = ((float) newWidth) / oldWidth;
        float scaleHeight = ((float) newHeight) / oldHeight;
        float scaleFactor = Math.min(scaleWidth, scaleHeight);

        Matrix scale = new Matrix();
        scale.postScale(scaleFactor, scaleFactor);

        Bitmap resizeBitmap = Bitmap.createBitmap(bitmap, 0, 0, oldWidth, oldHeight, scale, false);
        bitmap.recycle();

        return resizeBitmap;

    }

    public boolean mediaStoreInsertImage(ContentResolver contentResolver, String imagePath, String imageName) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, imageName);
            values.put(MediaStore.Images.Media.DISPLAY_NAME, imageName);
            values.put(MediaStore.Images.Media.DESCRIPTION, "Created by TenToOne");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");
            values.put(MediaStore.Images.Media.DATA, imagePath);
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            return true;
        }
    }

    public boolean mediaStoreDeleteImage(ContentResolver contentResolver, String path) {
        try {
            String[] retCol = {MediaStore.Images.Media._ID};
            Cursor cur = contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    retCol,
                    MediaStore.MediaColumns.DATA + "='" +path+"'",
                    null,
                    null);
            if(cur.getCount() == 0)
                throw new NullPointerException("ImageData is not exist");

            cur.moveToFirst();
            int id = cur.getInt(cur.getColumnIndex(MediaStore.MediaColumns._ID));
            cur.close();

            Uri uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
            contentResolver.delete(uri, null, null);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            return true;
        }
    }

    public void deleteFileFromMediaStore(final ContentResolver contentResolver, final File file) {
        String canonicalPath;
        try {
            canonicalPath = file.getCanonicalPath();
        } catch (IOException e) {
            canonicalPath = file.getAbsolutePath();
        }
        final Uri uri = MediaStore.Files.getContentUri("external");
        final int result = contentResolver.delete(uri,
                MediaStore.Files.FileColumns.DATA + "=?", new String[] {canonicalPath});
        if (result == 0) {
            final String absolutePath = file.getAbsolutePath();
            if (!absolutePath.equals(canonicalPath)) {
                contentResolver.delete(uri,
                        MediaStore.Files.FileColumns.DATA + "=?", new String[]{absolutePath});
            }
        }
    }

}
