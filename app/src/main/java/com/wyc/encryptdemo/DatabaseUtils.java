package com.wyc.encryptdemo;

import android.content.Context;

import net.sqlcipher.database.SQLiteDatabase;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 作者： wyc
 * <p>
 * 创建时间： 2021/9/16 16:17
 * <p>
 * 文件名字： com.wyc.database.encrypt.utils
 * <p>
 * 类的介绍：
 */
public class DatabaseUtils {

    private static final String TAG = "DatabaseUtils";

    private DatabaseUtils() {
        List list = new ArrayList();
    }

    /**
     * 使用 sqlcipher 加密数据库
     *
     * @param context 上下文
     * @param dbName  要加密的数据库名称
     * @param key     密码
     */
    public static void encryptDatabase(Context context, String dbName, String key) {
        if (!isValidSQLite(context, dbName)) {
            Log.w(TAG, "database is encrypted");
            return;
        }
        try {
            File originalFile = context.getDatabasePath(dbName);
            if (originalFile == null || !originalFile.exists()) {
                throw new RuntimeException(dbName + "does not exist！");
            }
            Log.d(TAG, "[encrypt] origin file path = " + originalFile.getAbsolutePath());
            //打开要加密的数据库
            SQLiteDatabase database = SQLiteDatabase.openDatabase(
                    originalFile.getAbsolutePath(),
                    "",
                    null,
                    SQLiteDatabase.OPEN_READWRITE);

            //新建加密后的数据库临时文件
            File newTempFile = File.createTempFile("temp_sqlcipher_", "_encrypt", context.getCacheDir());
            Log.d(TAG, "[encrypt] newFile path = " + newTempFile.getAbsolutePath());
            String tempName = "encrypt_database";
            //连接到加密后的数据库，并设置密码
            database.rawExecSQL(String.format("ATTACH DATABASE '%s' AS %s KEY '%s';", newTempFile.getAbsolutePath(), tempName, key));
            //输出要加密的数据库表和数据到加密后的数据库文件中
            database.rawExecSQL(String.format("SELECT sqlcipher_export('%s');", tempName));
            //断开同加密后的数据库的连接
            database.rawExecSQL(String.format("DETACH DATABASE %s;", tempName));

            //打开加密后的数据库，测试数据库是否加密成功
            database = SQLiteDatabase.openDatabase(newTempFile.getAbsolutePath(), key, null, SQLiteDatabase.OPEN_READWRITE);
            Log.d(TAG, "[encrypt] dbVersion = " + database.getVersion());
            database.setVersion(database.getVersion());
            //关闭数据库
            database.close();

            boolean deleteResult = originalFile.delete();
            Log.d(TAG, "[encrypt] delete origin database : " + deleteResult);
            boolean renameResult = newTempFile.renameTo(originalFile);
            Log.d(TAG, "[encrypt] rename new database : " + renameResult);
        } catch (Exception e) {
            Log.e(TAG, "Exception", e);
        }
    }


    /**
     * 使用 sqlcipher 解密数据库
     *
     * @param context 上下文
     * @param dbName  数据库名字
     * @param key     密码
     */
    public static void decryptDatabase(Context context, String dbName, String key) {
        if (isValidSQLite(context, dbName)) {
            Log.w(TAG, "database is not encrypted");
            return;
        }
        try {
            File originalFile = context.getDatabasePath(dbName);
            if (originalFile == null || !originalFile.exists()) {
                throw new RuntimeException(dbName + "does not exist！");
            }
            Log.d(TAG, "[decrypt] origin file path = " + originalFile.getAbsolutePath());
            SQLiteDatabase database = SQLiteDatabase.openDatabase(
                    originalFile.getAbsolutePath(),
                    key,
                    null,
                    SQLiteDatabase.OPEN_READWRITE);

            //新建解密后的数据库临时文件
            File newTempFile = File.createTempFile("temp_sqlcipher_", "_decrypt", context.getCacheDir());
            Log.d(TAG, "[decrypt] newFile path = " + newTempFile.getAbsolutePath());
            String tempName = "decrypt_database";
            //连接到解密后的数据库，并设置密码为空
            database.rawExecSQL(String.format("ATTACH DATABASE '%s' AS %s KEY '%s';", newTempFile.getAbsolutePath(), tempName, ""));
            //输出要加密的数据库表和数据到加密后的数据库文件中
            database.rawExecSQL(String.format("SELECT sqlcipher_export('%s');", tempName));
            //断开同加密后的数据库的连接
            database.rawExecSQL(String.format("DETACH DATABASE %s;", tempName));

            database = SQLiteDatabase.openDatabase(
                    newTempFile.getAbsolutePath(),
                    "",
                    null,
                    SQLiteDatabase.OPEN_READWRITE);
            Log.d(TAG, "[decrypt] dbVersion = " + database.getVersion());
            database.setVersion(database.getVersion());
            database.close();

            boolean deleteResult = originalFile.delete();
            Log.d(TAG, "[decrypt] delete origin database : " + deleteResult);
            boolean renameResult = newTempFile.renameTo(originalFile);
            Log.d(TAG, "[decrypt] rename new database : " + renameResult);
        } catch (Exception e) {
            Log.e(TAG, "Exception", e);
        }
    }


    /**
     * 检查文件是否是有效的 SQLite 数据库
     * <p>
     * SQLite 数据库文件包含一个提供一些有用信息的标题
     * 每个SQlite数据库文件始终在其前16个字节中包含以下值: SQLite format 3\u0000
     *
     * @param dbName
     * @return true 正常数据库  false  非正常数据库
     */
    private static boolean isValidSQLite(Context context, String dbName) {
        File file = context.getDatabasePath(dbName);
        if (!file.exists() || !file.canRead()) {
            return false;
        }
        try {
            FileReader fr = new FileReader(file);
            char[] buffer = new char[16];
            fr.read(buffer, 0, 16);
            String str = String.valueOf(buffer);
            fr.close();
            return str.equals("SQLite format 3\u0000");
        } catch (Exception e) {
            Log.e(TAG, "Exception", e);
            return false;
        }
    }

}
