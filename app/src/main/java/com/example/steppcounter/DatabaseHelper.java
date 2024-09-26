package com.example.steppcounter;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    static final String DATABASE_NAME = "Byte_Me_DB";
    static final int DATABASE_VERSION = 1;

    //creating column and table names for the Activity table in DB
    static final String DB_STEP_TABLE = "ACTIVITY";
    static final String DATE = "DATE";
    static final String STEP_ID = "STEP_ID";
    static final String USER_ID = "USER_ID";
    static final String STEP_COUNT = "STEP_COUNT";
    static final String DISTANCE_STEPPED = "DISTANCE_STEPPED";

    private static final String CREATE_DB_QUERY = "CREATE TABLE " + DB_STEP_TABLE +" ( "
            + USER_ID + " INTEGER, "
            + STEP_ID + " INTEGER, "
            + DATE + " TEXT, "
            + STEP_COUNT + " INTEGER, "
            + DISTANCE_STEPPED + " REAL, "
            + "PRIMARY KEY (" + USER_ID + ", " + DATE +")"
            + ");";

    public DatabaseHelper( Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_DB_QUERY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS " + DB_STEP_TABLE);

    }
}
