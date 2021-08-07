package com.ydd.zhichat.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.ydd.zhichat.MyApplication;
import com.ydd.zhichat.R;
import com.ydd.zhichat.bean.Prefix;
import com.ydd.zhichat.util.LocaleHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Administrator on 2017/4/12 0012.
 * 与ios统一，使用数据库进行国际化
 */

public class InternationalizationHelper {

    public static final String DB_NAME = "constant.db"; //保存的数据库文件名
    private static InternationalizationHelper helper = new InternationalizationHelper();
    private final int BUFFER_SIZE = 400000;
    /**
     * 缓存打开的数据库，避免每次都打开，因此不能用完就close关闭，
     * 打开大约5到20毫秒，
     */
    private SQLiteDatabase db;

    private InternationalizationHelper() {
    }

    public static InternationalizationHelper getInternationalizationHelper() {
        return helper;
    }

    /**
     * 国际化
     *
     * @param ios
     * @return
     */
    public static String getString(String ios) {
        SQLiteDatabase db = helper.openDatabase();
        if (db != null) {
            String table = "lang";
            String[] columns = new String[]{"zh", "en", "big5"};
            String selection = "ios=?";
            String[] selectionArgs = new String[]{ios};
            // 只查一条数据时limit 1更快一点，
            Cursor cursor = db.query(table, columns, selection, selectionArgs, null, null, null, "1");

            String ms = LocaleHelper.getPersistedData(MyApplication.getContext(), Locale.getDefault().getLanguage());
            String language = " ";

            // 仅有一条记录时使用moveToFirst比moveToNext快，
            if (cursor.moveToFirst()) {
                if (ms.equals("zh")) {
                    language = cursor.getString(cursor.getColumnIndex("zh"));
                } else if (ms.equals("HK") || ms.equals("TW")) {
                    language = cursor.getString(cursor.getColumnIndex("big5"));
                } else {
                    language = cursor.getString(cursor.getColumnIndex("en"));
                }
            }
            cursor.close();
            return language;
        }
        return null;
    }

    /*
    查询SMS_country,返回所有数据
     */
    public static List<Prefix> getPrefixList() {
        List<Prefix> prefixList = new ArrayList<>();
        SQLiteDatabase db = helper.openDatabase();
        if (db != null) {
            String table = "SMS_country";
            Cursor cursor = db.query(table, null, null, null, null, null, null, null);

            while (cursor.moveToNext()) {
                Prefix preFix = new Prefix();
                String country = cursor.getString(cursor.getColumnIndex("country"));
                String enName = cursor.getString(cursor.getColumnIndex("enName"));
                int prefix = cursor.getInt(cursor.getColumnIndex("prefix"));
                preFix.setCountry(country);
                preFix.setEnName(enName);
                preFix.setPrefix(prefix);
                prefixList.add(preFix);
            }
            cursor.close();
        }
        return prefixList;
    }

    /*
    查询SMS_country,返回所有查询数据
     */
    public static List<Prefix> getSearchPrefix(String Selection) {
        List<Prefix> prefixList = new ArrayList<>();
        SQLiteDatabase db = helper.openDatabase();
        if (db != null) {
            if (Locale.getDefault().getLanguage().equals("zh")) {
                String table = "SMS_country";
                String selection = "country like ?";
                Selection = "%" + Selection + "%";
                String[] selectionArgs = new String[]{Selection};
                Cursor cursor = db.query(table, null, selection, selectionArgs, null, null, null, null);

                while (cursor.moveToNext()) {
                    Prefix preFix = new Prefix();
                    String country = cursor.getString(cursor.getColumnIndex("country"));
                    String enName = cursor.getString(cursor.getColumnIndex("enName"));
                    int prefix = cursor.getInt(cursor.getColumnIndex("prefix"));
                    preFix.setCountry(country);
                    preFix.setEnName(enName);
                    preFix.setPrefix(prefix);
                    prefixList.add(preFix);
                }
                cursor.close();
            } else if (Locale.getDefault().getLanguage().equals("en")) {
                String table = "SMS_country";
                String selection = "enName like ?";
                Selection = "%" + Selection + "%";
                String[] selectionArgs = new String[]{Selection};
                Cursor cursor = db.query(table, null, selection, selectionArgs, null, null, null, null);

                while (cursor.moveToNext()) {
                    Prefix preFix = new Prefix();
                    String country = cursor.getString(cursor.getColumnIndex("country"));
                    String enName = cursor.getString(cursor.getColumnIndex("enName"));
                    int prefix = cursor.getInt(cursor.getColumnIndex("prefix"));
                    preFix.setCountry(country);
                    preFix.setEnName(enName);
                    preFix.setPrefix(prefix);
                    prefixList.add(preFix);
                }
                cursor.close();
            } else {
                // 其他国家默认使用英文
                String table = "SMS_country";
                String selection = "enName like ?";
                Selection = "%" + Selection + "%";
                String[] selectionArgs = new String[]{Selection};
                Cursor cursor = db.query(table, null, selection, selectionArgs, null, null, null, null);

                while (cursor.moveToNext()) {
                    Prefix preFix = new Prefix();
                    String country = cursor.getString(cursor.getColumnIndex("country"));
                    String enName = cursor.getString(cursor.getColumnIndex("enName"));
                    int prefix = cursor.getInt(cursor.getColumnIndex("prefix"));
                    preFix.setCountry(country);
                    preFix.setEnName(enName);
                    preFix.setPrefix(prefix);
                    prefixList.add(preFix);
                }
                cursor.close();
            }

        }
        return prefixList;
    }

    private SQLiteDatabase openDatabase() {
        if (db != null) {
            return db;
        }
        synchronized (this) {
            if (db != null) {
                return db;
            }
            try {
                File dbfile = MyApplication.getContext().getDatabasePath(DB_NAME);
                if (!(dbfile.exists())) {
                    //判断数据库文件是否存在，若不存在则执行导入，否则直接打开数据库
                    InputStream is = MyApplication.getContext().getResources().openRawResource(
                            R.raw.constant); //欲导入的数据库

                    FileOutputStream fos = new FileOutputStream(dbfile);
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int count = 0;
                    while ((count = is.read(buffer)) > 0) {
                        fos.write(buffer, 0, count);
                    }
                    fos.close();
                    is.close();
                }

                db = SQLiteDatabase.openOrCreateDatabase(dbfile, null);
                return db;

            } catch (FileNotFoundException e) {
                Log.e("Database", "File not found");
                e.printStackTrace();
            } catch (IOException e) {
                Log.e("Database", "IO exception");
                e.printStackTrace();
            }
        }
        return null;
    }
}

/*
package com.client.yanchat.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Environment;
import android.util.Log;

import com.client.yanchat.MyApplication;
import com.client.yanchat.R;
import com.client.yanchat.bean.Prefix;
import com.client.yanchat.util.LocaleHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

*/
/**
 * Created by Administrator on 2017/4/12 0012.
 * 与ios统一，使用数据库进行国际化
 * <p>
 * 国际化
 *
 * @param ios
 * @return 国际化
 * @param ios
 * @return 国际化
 * @param ios
 * @return 国际化
 * @param ios
 * @return 国际化
 * @param ios
 * @return 国际化
 * @param ios
 * @return 国际化
 * @param ios
 * @return 国际化
 * @param ios
 * @return
 *//*


public class InternationalizationHelper {

    private static final String DB_NAME = "constant.db"; // 保存的数据库文件名

    private InternationalizationHelper() {
    }

    private static InternationalizationHelper helper = new InternationalizationHelper();

    public static InternationalizationHelper getInternationalizationHelper() {
        return helper;
    }

    */
/**
 * 国际化
 *
 * @param ios
 * @return
 *//*

    public static String getString(String ios) {
        SQLiteDatabase db = helper.openDatabase();
        if (db != null) {
            String table = "lang";
            String[] columns = new String[]{"zh", "en", "big5"};
            String selection = "ios=?";
            String[] selectionArgs = new String[]{ios};

            Cursor cursor;
            try {
                cursor = db.query(table, columns, selection, selectionArgs, null, null, null, null);
            } catch (SQLiteException e) {
                return "数据库异常";
            }

            String ms = LocaleHelper.getPersistedData(MyApplication.getContext(), Locale.getDefault().getLanguage());
            String language = " ";

            if (cursor.moveToNext()) {
                if (ms.equals("zh")) {
                    language = cursor.getString(cursor.getColumnIndex("zh"));
                } else if (ms.equals("HK") || ms.equals("TW")) {
                    language = cursor.getString(cursor.getColumnIndex("big5"));
                } else {
                    language = cursor.getString(cursor.getColumnIndex("en"));
                }
            }
            cursor.close();
            db.close();
            return language;
        }
        return "db为空";
    }

    */
/*
    查询SMS_country,返回所有数据
     *//*

    public static List<Prefix> getPrefixList() {
        List<Prefix> prefixList = new ArrayList<>();
        SQLiteDatabase db = helper.openDatabase();
        if (db != null) {
            String table = "SMS_country";
            Cursor cursor = db.query(table, null, null, null, null, null, null, null);

            while (cursor.moveToNext()) {
                Prefix preFix = new Prefix();
                String country = cursor.getString(cursor.getColumnIndex("country"));
                String enName = cursor.getString(cursor.getColumnIndex("enName"));
                int prefix = cursor.getInt(cursor.getColumnIndex("prefix"));
                preFix.setCountry(country);
                preFix.setEnName(enName);
                preFix.setPrefix(prefix);
                prefixList.add(preFix);
            }
            cursor.close();
            db.close();
        }
        return prefixList;
    }

    */
/*
    查询SMS_country,返回所有查询数据
     *//*

    public static List<Prefix> getSearchPrefix(String Selection) {
        List<Prefix> prefixList = new ArrayList<>();
        SQLiteDatabase db = helper.openDatabase();
        if (db != null) {
            if (Locale.getDefault().getLanguage().equals("zh")) {
                String table = "SMS_country";
                String selection = "country like ?";
                Selection = "%" + Selection + "%";
                String[] selectionArgs = new String[]{Selection};
                Cursor cursor = db.query(table, null, selection, selectionArgs, null, null, null, null);

                while (cursor.moveToNext()) {
                    Prefix preFix = new Prefix();
                    String country = cursor.getString(cursor.getColumnIndex("country"));
                    String enName = cursor.getString(cursor.getColumnIndex("enName"));
                    int prefix = cursor.getInt(cursor.getColumnIndex("prefix"));
                    preFix.setCountry(country);
                    preFix.setEnName(enName);
                    preFix.setPrefix(prefix);
                    prefixList.add(preFix);
                }
                cursor.close();
                db.close();
            } else if (Locale.getDefault().getLanguage().equals("en")) {
                String table = "SMS_country";
                String selection = "enName like ?";
                Selection = "%" + Selection + "%";
                String[] selectionArgs = new String[]{Selection};
                Cursor cursor = db.query(table, null, selection, selectionArgs, null, null, null, null);

                while (cursor.moveToNext()) {
                    Prefix preFix = new Prefix();
                    String country = cursor.getString(cursor.getColumnIndex("country"));
                    String enName = cursor.getString(cursor.getColumnIndex("enName"));
                    int prefix = cursor.getInt(cursor.getColumnIndex("prefix"));
                    preFix.setCountry(country);
                    preFix.setEnName(enName);
                    preFix.setPrefix(prefix);
                    prefixList.add(preFix);
                }
                cursor.close();
                db.close();
            } else {
                // 其他国家默认使用英文
                String table = "SMS_country";
                String selection = "enName like ?";
                Selection = "%" + Selection + "%";
                String[] selectionArgs = new String[]{Selection};
                Cursor cursor = db.query(table, null, selection, selectionArgs, null, null, null, null);

                while (cursor.moveToNext()) {
                    Prefix preFix = new Prefix();
                    String country = cursor.getString(cursor.getColumnIndex("country"));
                    String enName = cursor.getString(cursor.getColumnIndex("enName"));
                    int prefix = cursor.getInt(cursor.getColumnIndex("prefix"));
                    preFix.setCountry(country);
                    preFix.setEnName(enName);
                    preFix.setPrefix(prefix);
                    prefixList.add(preFix);
                }
                cursor.close();
                db.close();
            }

        }
        return prefixList;
    }

    private SQLiteDatabase openDatabase() {
        String dbPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/databases/" + DB_NAME;
        try {
            if (!new File(dbPath).exists()) {
                boolean flag = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/databases/").mkdirs();
                boolean newFile = new File(dbPath).createNewFile();

                FileOutputStream out = new FileOutputStream(dbPath);
                InputStream in = MyApplication.getContext().getResources().openRawResource(R.raw.constant);

                byte[] buffer = new byte[1024];
                int readBytes = 0;
                while ((readBytes = in.read(buffer)) > 0) {
                    out.write(buffer, 0, readBytes);
                }
                in.close();
                out.close();
            }
        } catch (FileNotFoundException e) {
            Log.e("database", "File not found");
            e.printStackTrace();
        } catch (IOException e) {
            Log.e("database", "IO exception");
            e.printStackTrace();
        }
        return SQLiteDatabase.openOrCreateDatabase(dbPath, null);
    }
}
*/
