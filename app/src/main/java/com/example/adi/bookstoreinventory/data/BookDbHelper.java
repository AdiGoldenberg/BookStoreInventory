package com.example.adi.bookstoreinventory.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.adi.bookstoreinventory.data.BookContract.BookEntry;


/**
 * Created by ADI on 11/07/2018.
 */

public class BookDbHelper extends SQLiteOpenHelper {

    // DB name and version
    public static final String DB_NAME = "inventory.db";
    public static final int DB_VERSION = 1;

    // SQL commands
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + BookEntry.TABLE_NAME + " ( " +
                    BookEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    BookEntry.COLUMN_BOOK_NAME + " TEXT NOT NULL, " +
                    BookEntry.COLUMN_BOOK_PRICE + " INTEGER DEFAULT 0, " +
                    BookEntry.COLUMN_BOOK_QUANTITY + " INTEGER DEFAULT 0, " +
                    BookEntry.COLUMN_BOOK_SUPPLIER_NAME + " TEXT NOT NULL, " +
                    BookEntry.COLUMN_BOOK_SUPPLIER_PHONE + " TEXT" +
                    ");";
    private static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + BookEntry.TABLE_NAME;

    // Constructor
    public BookDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    // This method creates an SQLite database
    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.e("BookDbHelper", "The SQL CREATE TABLE command is: " + SQL_CREATE_ENTRIES);
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    // This method updates the SQLite database
    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
}
