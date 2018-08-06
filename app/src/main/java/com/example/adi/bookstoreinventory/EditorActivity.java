package com.example.adi.bookstoreinventory;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.adi.bookstoreinventory.data.BookContract.BookEntry;
import com.example.adi.bookstoreinventory.data.BookProvider;

public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    // Create a global variable for the loader ID
    private static final int BOOK_LOADER = 0;
    /**
     * EditText field to enter the book's name
     */
    private EditText mNameEditText;
    /**
     * EditText field to enter the book's supplier
     */
    private EditText mSupplierEditText;
    /**
     * EditText field to enter the book's supplier phone
     */
    private EditText mPhoneEditText;
    /**
     * EditText field to enter the book's price
     */
    private EditText mPriceEditText;
    /**
     * TextView field showing the book's quantity
     */
    private TextView mQuantityTextView;
    /**
     * Button to decrease the book's quantity
     */
    private Button mMinusButton;
    /**
     * Button to increase the book's quantity
     */
    private Button mPlusButton;
    // Create a global variable holding the uri of the specific book if in edit mode
    // (null is in inset mode)
    private Uri mCurrentBookUri;

    // Create a variable to report if any view was touched
    private boolean mBookHasChanged = false;

    // Create an OnTouchListener that changes the mBookHasChanged to be true if the view was touched
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mBookHasChanged = true;
            return false;
        }
    };

    // Create a global variable to hold the current mode of the activity (Edit/Add)
    private String mMode;

    // Create a global variable to hold the current quantity of the book
    private int mQuantityInt;

    /**
     * Create a global variable to tell if the activity should finish after "save" click or not
     * Set the mFinishActivity to true as default
     * (unless the input needs changing, the activity should terminate after a "save" click)
     **/
    private boolean mFinishActivity = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_book_name);
        mSupplierEditText = (EditText) findViewById(R.id.edit_book_supplier_name);
        mPhoneEditText = (EditText) findViewById(R.id.edit_book_supplier_phone);
        mPriceEditText = (EditText) findViewById(R.id.edit_book_price);
        mQuantityTextView = (TextView) findViewById(R.id.edit_book_quantity);
        mMinusButton = (Button) findViewById(R.id.minus_quantity);
        mPlusButton = (Button) findViewById(R.id.plus_quantity);
        FloatingActionButton deleteFab = (FloatingActionButton) findViewById(R.id.fab_delete);

        // Find what intent opened the activity
        String action = getIntent().getAction();
        if (action == Intent.ACTION_EDIT) {
            // Change the title of the activity
            setTitle(getResources().getString(R.string.editor_activity_title_edit_book));
            // Inactivate the OptionsMenu (there is nothing to delete)
            invalidateOptionsMenu();
            // Get the specific book uri associated with the intent opening the activity
            mCurrentBookUri = getIntent().getData();
            // Initialize the CursorLoader
            getLoaderManager().initLoader(BOOK_LOADER, null, this);
            // Update mMode
            mMode = action;

            // Setup FAB to delete the entry
            deleteFab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Open the Delete confirmation dialog
                    showDeleteConfirmationDialog();
                }
            });
        } else {
            // Change the title of the activity
            setTitle(getResources().getString(R.string.editor_activity_title_new_book));
            // Set the mCurrentPetUri to be null
            mCurrentBookUri = null;
            // Update mMode
            mMode = "add";
            // Make the deleting FAB to pop a toast
            deleteFab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Popup a toast saying there is nothing to delete
                    Toast.makeText(EditorActivity.this,
                            getResources().getString(R.string.msg_nothing_to_delete), Toast.LENGTH_LONG).show();
                }
            });
            // Set the mQuantityInt to be zero
            mQuantityInt = 0;
        }

        // Set the OnTouchListener on the views
        mNameEditText.setOnTouchListener(mTouchListener);
        mSupplierEditText.setOnTouchListener(mTouchListener);
        mPhoneEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mMinusButton.setOnTouchListener(mTouchListener);
        mPlusButton.setOnTouchListener(mTouchListener);

        // Setup FAB to call supplier
        FloatingActionButton contactFab = (FloatingActionButton) findViewById(R.id.fab_call_supplier);
        contactFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // If there is a phone appearing in the PhoneEditText, send intent to dial that number
                String currentPhone = mPhoneEditText.getText().toString().trim();
                if (!TextUtils.isEmpty(currentPhone)) {
                    Intent intent = new Intent(Intent.ACTION_DIAL,
                            Uri.parse("tel:" + mPhoneEditText.getText().toString().trim()));
                    startActivity(intent);
                } else { // If there is nothing in the PhoneEditText, show a toast message
                    Toast.makeText(EditorActivity.this,
                            getResources().getString(R.string.msg_no_phone_number).toString(),
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save book to database
                savePet();
                // Exit the editor activity unless there was an invalid name, supplier, price or quantity
                if (mFinishActivity) {
                    finish();
                }
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the book hasn't changed, continue with navigating up to parent activity
                // which is the {@link InventoryActivity}.
                if (!mBookHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void savePet() {
        // Get the book values entered by the user
        String nameString = mNameEditText.getText().toString().trim();
        String supplierString = mSupplierEditText.getText().toString().trim();
        String phoneString = mPhoneEditText.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();

        // Convert the price string into an int (and set it to 0 if nothing was entered)
        int priceInt = 0;
        if (!TextUtils.isEmpty(priceString)) {
            priceInt = Integer.parseInt(priceString);
        }

        // Create a variable to hold the message that should appear in the Toast
        String message = "";

        // Skip saving the book if all of the values are blank
        boolean emptyBook = TextUtils.isEmpty(nameString) &&
                TextUtils.isEmpty(supplierString) &&
                TextUtils.isEmpty(phoneString) &&
                priceInt == 0 &&
                mQuantityInt == 0;
        if (emptyBook) {
            message = getResources().getString(R.string.book_not_saved_toast);
        } else {    // If the user entered any values
            // Insert the values to a ContentValues object
            ContentValues values = new ContentValues();
            values.put(BookEntry.COLUMN_BOOK_NAME, nameString);
            values.put(BookEntry.COLUMN_BOOK_SUPPLIER_NAME, supplierString);
            values.put(BookEntry.COLUMN_BOOK_SUPPLIER_PHONE, phoneString);
            values.put(BookEntry.COLUMN_BOOK_PRICE, priceInt);
            values.put(BookEntry.COLUMN_BOOK_QUANTITY, mQuantityInt);

            // Find what intent opened the activity
            switch (mMode) {
                case Intent.ACTION_EDIT:    // The activity is in "edit book" mode
                    // Insert the ContentValues object into the books table and return the newRowId
                    int rowsUpdated = getContentResolver().update(mCurrentBookUri, values, null, null);
                    // Update the message for the Toast
                    switch (rowsUpdated) {
                        // If no book was updated for an unknown reason update the toast message and finish
                        case 0:
                            message = getResources().getString(R.string.error_updating_book_toast).toString();
                            mFinishActivity = true;
                            break;
                        // If the book's name, supplier, price or quantity are invalid,
                        // set an appropriate message and don't finish the activity
                        case BookProvider.UPDATE_NAME_ERROR:
                            message = getResources().getString(R.string.input_name_error);
                            mFinishActivity = false;
                            break;
                        case BookProvider.UPDATE_SUPPLIER_ERROR:
                            message = getResources().getString(R.string.input_supplier_error);
                            mFinishActivity = false;
                            break;
                        case BookProvider.UPDATE_PRICE_ERROR:
                            message = getResources().getString(R.string.input_price_error);
                            mFinishActivity = false;
                            break;
                        case BookProvider.UPDATE_QUANTITY_ERROR:
                            message = getResources().getString(R.string.input_quantity_error);
                            mFinishActivity = false;
                            break;
                        // If the rowsAdded isn't 0 or any of the 4 input error negative ints, the insertion
                        // was successful so set the appropriate message and finish the activity
                        default:
                            message = getResources().getString(R.string.book_updated_toast).toString();
                            mFinishActivity = true;
                    }
                    break;
                case "add":    // If the mMode is "add" the activity should be in "add new book" mode
                    // Insert the ContentValues object into the books table and return the newRowId
                    Uri uri = getContentResolver().insert(BookEntry.CONTENT_URI, values);
                    // Update the message for the Toast
                    if (uri == null) {  // If the uri is null there was an unfamiliar error inserting the book
                        message = getResources().getString(R.string.error_adding_book_toast).toString();
                        mFinishActivity = true;
                    } else {
                        switch (String.valueOf(uri)) {
                            // If the book's name, supplier, price or quantity are invalid,
                            // set an appropriate message and don't finish the activity
                            case BookProvider.INSERT_NAME_ERROR:
                                message = getResources().getString(R.string.input_name_error);
                                mFinishActivity = false;
                                break;
                            case BookProvider.INSERT_SUPPLIER_ERROR:
                                message = getResources().getString(R.string.input_supplier_error);
                                mFinishActivity = false;
                                break;
                            case BookProvider.INSERT_PRICE_ERROR:
                                message = getResources().getString(R.string.input_price_error);
                                mFinishActivity = false;
                                break;
                            case BookProvider.INSERT_QUANTITY_ERROR:
                                message = getResources().getString(R.string.input_quantity_error);
                                mFinishActivity = false;
                                break;
                            // If the uri isn't null or any of the 4 input error strings, the insertion
                            // was successful so set the appropriate message and finish the activity
                            default:
                                message = getResources().getString(R.string.book_added_toast).toString();
                                mFinishActivity = true;
                        }
                    }
            }
        }
        // Show toast message
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    // A method for creating a “Discard changes” dialog
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
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

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Create and return a CursorLoader with the CONTENT_URI of a given book
        return new CursorLoader(this, mCurrentBookUri,
                new String[]{BookEntry._ID,
                        BookEntry.COLUMN_BOOK_NAME,
                        BookEntry.COLUMN_BOOK_SUPPLIER_NAME,
                        BookEntry.COLUMN_BOOK_SUPPLIER_PHONE,
                        BookEntry.COLUMN_BOOK_PRICE,
                        BookEntry.COLUMN_BOOK_QUANTITY},
                null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Move to the first and only row of the cursor
        if (cursor.moveToFirst()) {
            // Extract properties from the cursor
            String name = cursor.getString(cursor.getColumnIndex(BookEntry.COLUMN_BOOK_NAME));
            String supplier = cursor.getString(cursor.getColumnIndex(BookEntry.COLUMN_BOOK_SUPPLIER_NAME));
            String phone = cursor.getString(cursor.getColumnIndex(BookEntry.COLUMN_BOOK_SUPPLIER_PHONE));
            int price = cursor.getInt(cursor.getColumnIndex(BookEntry.COLUMN_BOOK_PRICE));
            mQuantityInt = cursor.getInt(cursor.getColumnIndex(BookEntry.COLUMN_BOOK_QUANTITY));
            // Update the values of the EditTexts and the TextViews
            mNameEditText.setText(name);
            mSupplierEditText.setText(supplier);
            mPhoneEditText.setText(phone);
            mPriceEditText.setText(Integer.toString(price));
            mQuantityTextView.setText(Integer.toString(mQuantityInt));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Clear the EditText fields
        mNameEditText.clearComposingText();
        mSupplierEditText.clearComposingText();
        mPhoneEditText.clearComposingText();
        mPriceEditText.clearComposingText();
        mQuantityTextView.clearComposingText();
    }

    // If the user clicks the back button without saving data they changed, popup the UnsavedChangesDialog
    @Override
    public void onBackPressed() {
        // If the book hasn't changed, continue with handling back button press
        if (!mBookHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    // Create a Dialog to confirm deletion of the current book from the database
    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the book.
                deleteBook();
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
    private void deleteBook() {
        if (mMode == Intent.ACTION_EDIT) { // If the activity is in "edit book" mode and not in "add a book" mode
            int rowsDeleted = getContentResolver().delete(mCurrentBookUri, null, null);
            // Update the message for the Toast
            String message;
            if (rowsDeleted == 0) {
                message = getResources().getString(R.string.editor_delete_book_failed).toString();
            } else {
                message = getResources().getString(R.string.editor_delete_book_successful).toString();
            }
            // Show toast
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            // Exit the editor activity
            finish();
        }
    }

    // Set functionality for the minus button
    public void minusButton(View view) {
        // If the current quantity is greater than 0
        if (mQuantityInt > 0) {
            // Decrease the quantity variable by 1
            mQuantityInt = mQuantityInt - 1;
            // Update the quantity TextView
            mQuantityTextView.setText(String.valueOf(mQuantityInt));
        } else {    // If the current quantity is zero, do nothing other than poping up a toast alerting this
            Toast.makeText(this, getResources().getString(R.string.msg_no_more_books).toString(), Toast.LENGTH_LONG).show();
        }
    }

    // Set functionality for the plus button
    public void plusButton(View view) {
        // Popup a dialog prompting the user to enter the amount of copies of the book to add to inventory
        showPlusQuantityDialog();
    }

    // Create a Dialog to set the amount of books to add
    private void showPlusQuantityDialog() {
        // Create an EditText for the user to set the amount of books to add
        final EditText quantityEditText = new EditText(this);
        quantityEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
        // Set the default amount to add as 10
        quantityEditText.setText("10");
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.increase_quantity_dialog_msg);
        builder.setView(quantityEditText);
        builder.setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "add" button, so get the amount of books entered to the editText
                int booksToAdd = Integer.valueOf(quantityEditText.getText().toString());
                // Verify the new quantity isn't negative
                if (mQuantityInt + booksToAdd < 0) {
                    Toast.makeText(EditorActivity.this, getResources().getString(R.string.msg_no_more_books).toString(),
                            Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                } else {
                    // Update the mQuantityInt
                    mQuantityInt = mQuantityInt + booksToAdd;
                    // Update the quantity TextView
                    mQuantityTextView.setText(String.valueOf(mQuantityInt));
                }
            }
        });
        builder.setNegativeButton(R.string.none, new DialogInterface.OnClickListener() {
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
}
