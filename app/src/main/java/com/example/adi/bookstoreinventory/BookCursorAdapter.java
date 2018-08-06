package com.example.adi.bookstoreinventory;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.adi.bookstoreinventory.data.BookContract.BookEntry;

/**
 * Created by ADI on 02/08/2018.
 */

public class BookCursorAdapter extends CursorAdapter {
    /**
     * Constructs a new {@link BookCursorAdapter}.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    public BookCursorAdapter(final Context context, Cursor c) {
        super(context, c, 0 /* flags */);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // Return the list item view
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    /**
     * This method binds the book data (in the current row pointed to by cursor) to the given
     * list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(final View view, final Context context, Cursor cursor) {
        // Extract properties from the cursor
        int id = cursor.getInt(cursor.getColumnIndex(BookEntry._ID));
        String name = cursor.getString(cursor.getColumnIndex(BookEntry.COLUMN_BOOK_NAME));
        int price = cursor.getInt(cursor.getColumnIndex(BookEntry.COLUMN_BOOK_PRICE));
        int quantity = cursor.getInt(cursor.getColumnIndex(BookEntry.COLUMN_BOOK_QUANTITY));

        // Get the views from the list_item and update the name, price and mQuantity
        TextView nameView = (TextView) view.findViewById(R.id.name);
        nameView.setText(name);
        TextView priceView = (TextView) view.findViewById(R.id.price);
        priceView.setText(context.getResources().getString(R.string.price_text) + ": " + price +
                context.getResources().getString(R.string.unit_book_price));
        TextView quantityView = (TextView) view.findViewById(R.id.quantity);
        quantityView.setText(context.getResources().getString(R.string.quantity_text) + ": " + quantity);

        // Set functionality for the sold button
        Button soldButton = (Button) view.findViewById(R.id.sold);
        soldButton.setOnClickListener(createSoldListener(context, id, quantity));
    }

    // Create a helper method that makes an OnClickListener for the sold item
    private View.OnClickListener createSoldListener(final Context context, final int id, final int quantity) {
        // Create the OnClickListener
        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create a variable for the Toast message and set it to zero quantity as default
                String message = context.getResources().getString(R.string.msg_no_more_books);
                // Define book Uri
                Uri bookUri = Uri.withAppendedPath(BookEntry.CONTENT_URI, String.valueOf(id));
                Log.e(" Test: ", "URI is " + bookUri);
                // If the quantity is greater than zero, decrement it by 1
                if (quantity > 0) {
                    int currentQuantity = quantity - 1;
                    // Update the mQuantity in the database
                    ContentValues values = new ContentValues();
                    values.put(BookEntry.COLUMN_BOOK_QUANTITY, currentQuantity);
                    int rowsUpdated = context.getContentResolver().update(
                            bookUri, values, null, null);
                    // Update the message for the Toast
                    if (rowsUpdated == 0) {
                        message = context.getResources().getString(R.string.msg_error_updating_quantity).toString();
                    } else {
                        message = context.getResources().getString(R.string.msg_quantity_successfully_updated).toString();
                    }
                }
                // Show update in a Toast
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        };
        return onClickListener;
    }
}
