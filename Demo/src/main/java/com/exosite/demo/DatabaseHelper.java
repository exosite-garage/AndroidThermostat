package com.exosite.demo;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    static String DatabaseName = "ExositeDemoDB";
    static String DEVICES = "DeviceTable";
    static String colRID = "_id";
    static String colName = "Name";
    static String colCIK = "CIK";
    static String colPortalRID = "PortalRID";
    static String colPortalName = "PortalName";
    static String colPortalCIK = "PortalCIK";

    static String CreateString = "CREATE TABLE IF NOT EXISTS "
            + DEVICES + "(" + colRID + " VARCHAR, " + colName + " VARCHAR, " + colCIK + " VARCHAR, "
            + colPortalRID + " VARCHAR, " + colPortalName + " VARCHAR, " + colPortalCIK + " VARCHAR);";

    public DatabaseHelper(Context context) {
        super(context, DatabaseName, null, 1);
    }

    public void RecreateTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE " + DEVICES + ";");
        db.execSQL(CreateString);
    }

    public void RecreateTable() {
        SQLiteDatabase db = this.getWritableDatabase();
        this.RecreateTable(db);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CreateString);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        RecreateTable(db);
    }

    public void Clear() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(DEVICES, null, null);
    }

    public void InsertDevice(String rid, String name, String cik, String portalRID, String portalName, String portalCIK) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(colRID, rid);
        cv.put(colName, name);
        cv.put(colCIK, cik);
        cv.put(colPortalRID, portalRID);
        cv.put(colPortalName, portalName);
        cv.put(colPortalCIK, portalCIK);
        db.insert(DEVICES, null, cv);
    }

    public Cursor GetAllData()
    {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + DEVICES + ";", null);
        return c;
    }

    public void DeleteTable()
    {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE " + DEVICES + ";");
    }
}
