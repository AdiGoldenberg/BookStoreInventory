package com.example.adi.bookstoreinventory;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.adi.bookstoreinventory.data.BookContract.BookEntry;

/**
 * Displays list of books that were entered and stored in the app.
 */
public class InventoryActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    // Create a global variable for the loader ID
    private static final int BOOK_LOADER = 0;
    // Create a global variable for the PetCursorAdapter
    private BookCursorAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        // Setup FAB to open EditorActivity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_add);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(InventoryActivity.this, EditorActivity.class);
                startActivity(intent);
            }
        });

        // Get the ListView object
        ListView listView = (ListView) findViewById(R.id.list);

        // Find and set empty view on the ListView, so that it only shows when the list has 0 items.
        View emptyView = findViewById(R.id.empty_list_text_view);
        listView.setEmptyView(emptyView);

        // Create an empty object of PetCursorAdapter
        mAdapter = new BookCursorAdapter(this, null);
        // Associate the ListView and the adapter
        listView.setAdapter(mAdapter);

        // Create an onItemClickListener for the listView
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Intent intent = new Intent(
                        Intent.ACTION_EDIT, Uri.withAppendedPath(BookEntry.CONTENT_URI, String.valueOf(id)),
                        InventoryActivity.this, EditorActivity.class);
                startActivity(intent);
            }
        });

        // Initialize the CursorLoader
        getSupportLoaderManager().initLoader(BOOK_LOADER, null, this);
    }

    // This method inserts a dummy book
    private void insertBook() {
        /// Create a ContentValues object with the dummy data
        ContentValues values = new ContentValues();
        values.put(BookEntry.COLUMN_BOOK_NAME, "Harry Potter and the goblet of fire");
        values.put(BookEntry.COLUMN_BOOK_PRICE, 100);
        values.put(BookEntry.COLUMN_BOOK_QUANTITY, 2);
        values.put(BookEntry.COLUMN_BOOK_SUPPLIER_NAME, "Supplier 1");
        values.put(BookEntry.COLUMN_BOOK_SUPPLIER_PHONE, "972-3-1234567");

        // Insert the dummy data to the database
        Uri uri = getContentResolver().insert(BookEntry.CONTENT_URI, values);
        // Show a toast message
        String message;
        if (uri == null) {
            message = getResources().getString(R.string.error_adding_book_toast).toString();
        } else {
            message = getResources().getString(R.string.book_added_toast).toString();
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_catalog.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_inventory, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Insert dummy data" menu option
            case R.id.action_insert_dummy_data:
                insertBook();
                return true;
            // Respond to a click on the "Delete all entries" menu option
            case R.id.action_delete_all_entries:
                // Popup a dialog verifing the user wants to proceed deleting all books
                showDeleteConfirmationDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Create a Dialog to confirm deletion of the current book from the database
    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_all_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the book.
                deleteAllBooks();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the book.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the book in the database.
     */
    private void deleteAllBooks() {
        int rowsDeleted = getContentResolver().delete(BookEntry.CONTENT_URI, null, null);
        // Update the message for the Toast
        String message;
        if (rowsDeleted == 0) {
            message = getResources().getString(R.string.inventory_delete_books_failed).toString();
        } else {
            message = getResources().getString(R.string.inventory_delete_books_successful).toString();
        }
        // Show toast
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        // Create and return a CursorLoader with the CONTENT_URI (containing _ID, NAME, PRICE and QUANTITY)
        return new CursorLoader(this, BookEntry.CONTENT_URI,
                new String[]{BookEntry._ID, BookEntry.COLUMN_BOOK_NAME, BookEntry.COLUMN_BOOK_SUPPLIER_NAME,
                        BookEntry.COLUMN_BOOK_SUPPLIER_PHONE, BookEntry.COLUMN_BOOK_PRICE,
                        BookEntry.COLUMN_BOOK_QUANTITY}, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader loader, Cursor data) {
        // Update the BookCursorAdaptetr
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader loader) {
        // Update the BookCursorAdaptetr to have a cleared Cursor
        mAdapter.swapCursor(null);
    }
}
