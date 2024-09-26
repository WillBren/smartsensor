package com.example.steppcounter;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import java.sql.SQLDataException;

public class DatabaseManager {
    private DatabaseHelper dbhelper;
    private Context context;
    private SQLiteDatabase database;

    public DatabaseManager(Context ctx) {
        context = ctx;
    }

    public DatabaseManager open() throws SQLDataException {
        dbhelper = new DatabaseHelper(context);
        database = dbhelper.getWritableDatabase();
        return this;
    }


}
