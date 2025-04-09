package com.example.android_read_contacts;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_READ_CONTACTS_PERMISSION = 100;
    private ListView contactsListView;
    private Cursor cursor;
    private SimpleCursorAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        contactsListView = findViewById(R.id.contactsListView);

        // Check and request permission
        if (checkPermission()) {
            loadContacts();
        }
    }

    private boolean checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    REQUEST_READ_CONTACTS_PERMISSION);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_CONTACTS_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                loadContacts();
            } else {
                // Permission denied
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadContacts() {
        // Background thread for querying
        new Thread(() -> {
            try {
                ContentResolver contentResolver = getContentResolver();
                cursor = contentResolver.query(
                        ContactsContract.Contacts.CONTENT_URI,
                        null,
                        null,
                        null,
                        null);

                if (cursor != null && cursor.getCount() > 0) {
                    // UI thread to update the ListView
                    new Handler(Looper.getMainLooper()).post(() -> {
                        // Map data from the cursor to the layout
                        String[] from = {
                                ContactsContract.Contacts.DISPLAY_NAME,
                        };
                        int[] to = {android.R.id.text1};

                        // Use a SimpleCursorAdapter to display data in the ListView
                        adapter = new SimpleCursorAdapter(
                                this,
                                android.R.layout.simple_list_item_1,
                                cursor,
                                from,
                                to,
                                0); // Use 0 as flags
                        contactsListView.setAdapter(adapter);
                    });

                    // Log the contact names and phone numbers
                    while (cursor.moveToNext()) {
                        String id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                        String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
                        String phoneNumbers = getPhoneNumbers(contentResolver, id);
                        Log.d("Contacts", "Name: " + name + ", Phone Numbers: " + phoneNumbers);
                    }
                } else {
                    Log.d("Contacts", "No contacts found");
                    // Show a Toast on the UI thread
                    new Handler(Looper.getMainLooper()).post(() ->
                            Toast.makeText(this, "No contacts found.", Toast.LENGTH_LONG).show()
                    );
                }
            } catch (Exception e) {
                Log.e("Contacts", "Error loading contacts", e);
                // Show a Toast on the UI thread
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(this, "Error loading contacts.", Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    // Helper method to get phone numbers for a contact
    private String getPhoneNumbers(ContentResolver contentResolver, String contactId) {
        StringBuilder phoneNumbers = new StringBuilder();
        Cursor phoneCursor = null;
        try {
            phoneCursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                    new String[]{contactId},
                    null);
            if (phoneCursor != null && phoneCursor.getCount() > 0) {
                while (phoneCursor.moveToNext()) {
                    String phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    phoneNumbers.append(phoneNumber).append(", ");
                }
            }
        } finally {
            if (phoneCursor != null) {
                phoneCursor.close();
            }
        }
        // Remove the last comma and space if present
        if (phoneNumbers.length() > 2) {
            phoneNumbers.delete(phoneNumbers.length() - 2, phoneNumbers.length());
        }
        return phoneNumbers.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cursor != null) {
            // Close the cursor here
            cursor.close();
        }
    }
}