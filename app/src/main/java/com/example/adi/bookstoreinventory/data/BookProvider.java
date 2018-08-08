package com.example.adi.bookstoreinventory.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.example.adi.bookstoreinventory.data.BookContract.BookEntry;

/**
 * Created by ADI on 04/08/2018.
 */

public class BookProvider extends ContentProvider {
    /**
     * Tag for the log messages
     */
    public static final String LOG_TAG = BookProvider.class.getSimpleName();
    // Create public codes for input errors in insert (Strings) and in edit (ints) mode
    // The content of the strings is irrelevant as long as it's not identical to BookEntry.CONTENT_URI
    // The values of the ints is irrelevant as long as they're negative
    public static final String INSERT_NAME_ERROR = "name error";
    public static final String INSERT_SUPPLIER_ERROR = "supplier error";
    public static final String INSERT_PHONE_ERROR = "phone error";
    public static final String INSERT_PRICE_ERROR = "price error";
    public static final String INSERT_QUANTITY_ERROR = "quantity error";
    public static final int UPDATE_NAME_ERROR = -1;
    public static final int UPDATE_SUPPLIER_ERROR = -2;
    public static final int UPDATE_PHONE_ERROR = -3;
    public static final int UPDATE_PRICE_ERROR = -4;
    public static final int UPDATE_QUANTITY_ERROR = -5;
    /**
     * URI matcher code for the content URI for the books table
     */
    private static final int BOOKS = 100;
    /**
     * URI matcher code for the content URI for a single book in the books table
     */
    private static final int BOOK_ID = 101;
    /**
     * UriMatcher object to match a content URI to a corresponding code.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static {
        sUriMatcher.addURI(BookContract.CONTENT_AUTHORITY, BookEntry.TABLE_NAME, BOOKS);
        sUriMatcher.addURI(BookContract.CONTENT_AUTHORITY, BookEntry.TABLE_NAME + "/#", BOOK_ID);
    }

    // Create a database helper object
    private BookDbHelper mDbHelper;

    /**
     * Initialize the provider and the database helper object.
     */
    @Override
    public boolean onCreate() {
        // Initialize a BookDbHelper object to gain access to the books database.
        mDbHelper = new BookDbHelper(getContext());
        return true;
    }

    /**
     * Perform the query for the given URI. Use the given projection, selection, selection arguments, and sort order.
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Get readable database
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        // This cursor will hold the result of the query
        Cursor cursor;

        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match) {
            case BOOKS:
                // For the BOOKS code, query the books table directly with the given
                // projection, selection, selection arguments, and sort order. The cursor
                // could contain multiple rows of the books table.
                cursor = database.query(BookEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case BOOK_ID:
                // For the BOOK_ID code, extract out the ID from the URI.
                selection = BookEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};

                // Perform a query on the books table where the _id equals the given rowID to return a
                // Cursor containing that row of the table.
                cursor = database.query(BookEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }
        // Listen to data changes and update the cursor when it happens
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    /**
     * Insert new data into the provider with the given ContentValues.
     */
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case BOOKS:
                return insertBook(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    /**
     * Insert a book into the database with the given content values. Return the new content URI
     * for that specific row in the database.
     */
    private Uri insertBook(Uri uri, ContentValues values) {
        // Verifying the values are legal
        try {
            insertSanityCheck(values);      // Throws IllegalArgumentException with indicating message if there's an invalid input

            // The next lines will only get executed in case no IllegalArgumentException came up in the sanity check
            // Get the database
            SQLiteDatabase database = mDbHelper.getWritableDatabase();

            // Insert a new book into the books database table with the given ContentValues
            long id = database.insert(BookEntry.TABLE_NAME, null, values);

            //  If the ID is -1, then the insertion failed. Log an error and return null.
            if (id == -1) {
                Log.e(LOG_TAG, "Failed to insert row for " + uri);
                return null;
            } else {
                // Notify a change has occured
                getContext().getContentResolver().notifyChange(uri, null);
                // Once we know the ID of the new row in the table,
                // return the new URI with the ID appended to the end of it
                return ContentUris.withAppendedId(uri, id);
            }
        } catch (IllegalArgumentException ex) {
            // If the book's name, supplier, price or quantity is invalid return an indicative value
            switch (ex.getMessage()) {
                case INSERT_NAME_ERROR:
                    return Uri.parse(INSERT_NAME_ERROR);
                case INSERT_SUPPLIER_ERROR:
                    return Uri.parse(INSERT_SUPPLIER_ERROR);
                case INSERT_PHONE_ERROR:
                    return Uri.parse(INSERT_PHONE_ERROR);
                case INSERT_PRICE_ERROR:
                    return Uri.parse(INSERT_PRICE_ERROR);
                case INSERT_QUANTITY_ERROR:
                    return Uri.parse(INSERT_QUANTITY_ERROR);
                default:
                    return null;
            }
        }
    }

    /**
     * Updates the data at the given selection and selection arguments, with the new ContentValues.
     */
    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case BOOKS:
                return updateBook(uri, contentValues, selection, selectionArgs);
            case BOOK_ID:
                // For the BOOK_ID code, extract out the ID from the URI,
                // so we know which row to update. Selection will be "_id=?" and selection
                // arguments will be a String array containing the actual ID.
                selection = BookEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updateBook(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    /**
     * Update books in the database with the given content values. Apply the changes to the rows
     * specified in the selection and selection arguments (which could be 0 or 1 or more books).
     * Return the number of rows that were successfully updated.
     */
    private int updateBook(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // If the values are empty (nothing to update) just return 0
        if (values.size() == 0) {
            return 0;
        }
        // Verifying the values are legal
        try {
            updateSanityCheck(values);  // Throws IllegalArgumentException with indicating message if there's an invalid input

            // Get the database
            SQLiteDatabase database = mDbHelper.getWritableDatabase();

            // Update the selected books in the books database table with the given ContentValues
            int rowsUpdated = database.update(BookEntry.TABLE_NAME, values, selection, selectionArgs);

            // If a change has occurred mae a notification of it
            if (rowsUpdated != 0) {
                getContext().getContentResolver().notifyChange(uri, null);
            }

            // Return the number of rows that were affected
            return rowsUpdated;
        } catch (IllegalArgumentException ex) {
            // If the book's name, supplier, price or quantity is invalid return an indicative value
            switch (ex.getMessage()) {
                case INSERT_NAME_ERROR:
                    return UPDATE_NAME_ERROR;
                case INSERT_SUPPLIER_ERROR:
                    return UPDATE_SUPPLIER_ERROR;
                case INSERT_PHONE_ERROR:
                    return UPDATE_PHONE_ERROR;
                case INSERT_PRICE_ERROR:
                    return UPDATE_PRICE_ERROR;
                case INSERT_QUANTITY_ERROR:
                    return UPDATE_QUANTITY_ERROR;
                default:
                    return 0;
            }
        }
    }

    /**
     * Sanity check - verifying the values are all present and legal
     **/
    public void insertSanityCheck(ContentValues values) throws IllegalArgumentException {
        // Check that the name and is not null
        String name = values.getAsString(BookEntry.COLUMN_BOOK_NAME);
        if (name == null || TextUtils.isEmpty(name)) {
            throw new IllegalArgumentException(INSERT_NAME_ERROR);
        }
        // Check that the supplier is not null
        String supplier = values.getAsString(BookEntry.COLUMN_BOOK_SUPPLIER_NAME);
        if (supplier == null || TextUtils.isEmpty(supplier)) {
            throw new IllegalArgumentException(INSERT_SUPPLIER_ERROR);
        }
        // Check that the phone is not null
        String phone = values.getAsString(BookEntry.COLUMN_BOOK_SUPPLIER_PHONE);
        if (phone == null || TextUtils.isEmpty(phone)) {
            throw new IllegalArgumentException(INSERT_PHONE_ERROR);
        }
        // Check that the price isn't a negative number (though it can be null)
        Integer price = values.getAsInteger(BookEntry.COLUMN_BOOK_PRICE);
        if (price != null && price < 0) {
            throw new IllegalArgumentException(INSERT_PRICE_ERROR);
        }
        // Check that the quantity isn't a negative number (though it can be null)
        Integer quantity = values.getAsInteger(BookEntry.COLUMN_BOOK_QUANTITY);
        if (quantity != null && quantity < 0) {
            throw new IllegalArgumentException(INSERT_QUANTITY_ERROR);
        }
    }

    /**
     * Sanity check - verifying the values are all legal if present
     **/
    public void updateSanityCheck(ContentValues values) throws IllegalArgumentException {
        // Check that the name and is not null
        if (values.containsKey(BookEntry.COLUMN_BOOK_NAME)) {
            String name = values.getAsString(BookEntry.COLUMN_BOOK_NAME);
            if (name == null) {
                throw new IllegalArgumentException(INSERT_NAME_ERROR);
            }
        }
        // Check that the supplier is not null
        if (values.containsKey(BookEntry.COLUMN_BOOK_SUPPLIER_NAME)) {
            String supplier = values.getAsString(BookEntry.COLUMN_BOOK_SUPPLIER_NAME);
            if (supplier == null) {
                throw new IllegalArgumentException(INSERT_SUPPLIER_ERROR);
            }
        }
        // Check that the supplier's phone is not null
        if (values.containsKey(BookEntry.COLUMN_BOOK_SUPPLIER_PHONE)) {
            String phone = values.getAsString(BookEntry.COLUMN_BOOK_SUPPLIER_PHONE);
            if (phone == null) {
                throw new IllegalArgumentException(INSERT_PHONE_ERROR);
            }
        }
        // Check that the price isn't a negative number (though it can be null)
        if (values.containsKey(BookEntry.COLUMN_BOOK_PRICE)) {
            Integer price = values.getAsInteger(BookEntry.COLUMN_BOOK_PRICE);
            if (price == null || price < 0) {
                throw new IllegalArgumentException(INSERT_PRICE_ERROR);
            }
        }
        // Check that the quantity isn't a negative number (though it can be null)
        if (values.containsKey(BookEntry.COLUMN_BOOK_QUANTITY)) {
            Integer quantity = values.getAsInteger(BookEntry.COLUMN_BOOK_QUANTITY);
            if (quantity != null && quantity < 0) {
                throw new IllegalArgumentException(INSERT_QUANTITY_ERROR);
            }
        }
    }

    /**
     * Delete the data at the given selection and selection arguments.
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Get writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Create a variable for the result
        int rowsDeleted = 0;
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case BOOKS:
                // Delete all rows that match the selection and selection args
                rowsDeleted = database.delete(BookEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case BOOK_ID:
                // Delete a single row given by the ID in the URI
                selection = BookEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = database.delete(BookEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        // If any rows were deleted, make a notifcation of it
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of row deletes
        return rowsDeleted;
    }

    /**
     * Returns the MIME type of data for the content URI.
     */
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case BOOKS:
                return BookEntry.CONTENT_LIST_TYPE;
            case BOOK_ID:
                return BookEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }
}
