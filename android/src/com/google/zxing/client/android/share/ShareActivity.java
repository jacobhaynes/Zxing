/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.client.android.share;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.Intents;
import com.google.zxing.client.android.R;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Browser;
import android.provider.ContactsContract;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;



/**
 * Barcode Scanner can share data like contacts and bookmarks by displaying a QR Code on screen,
 * such that another user can scan the barcode with their phone.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ShareActivity extends Activity {

    private static final String TAG = ShareActivity.class.getSimpleName();

    private static final int PICK_BOOKMARK = 0;
    private static final int PICK_CONTACT = 1;
    private static final int PICK_APP = 2;
    
    final String[] phoneProjection = new String[] {
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
    };
    
    final String[] emailProjection = new String[] {
            ContactsContract.CommonDataKinds.Email.DATA, // use Email.ADDRESS for API-Level 11+
            ContactsContract.CommonDataKinds.Email.TYPE
    };
    
    final String[] addressProjection = new String[] {
            ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, // use Email.ADDRESS for API-Level 11+
            ContactsContract.CommonDataKinds.StructuredPostal.TYPE
    };

    private Button clipboardButton;

    SharedPreferences settings;
    String barcodeFormat;


    private final Button.OnClickListener contactListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            startActivityForResult(intent, PICK_CONTACT);
        }
    };

    private final Button.OnClickListener bookmarkListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            intent.setClassName(ShareActivity.this, BookmarkPickerActivity.class.getName());
            startActivityForResult(intent, PICK_BOOKMARK);
        }
    };

    private final Button.OnClickListener appListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            intent.setClassName(ShareActivity.this, AppPickerActivity.class.getName());
            startActivityForResult(intent, PICK_APP);
        }
    };

    private final Button.OnClickListener clipboardListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            // Should always be true, because we grey out the clipboard button in onResume() if it's empty
            if (clipboard.hasText()) {
                launchSearch(clipboard.getText().toString());
            }
        }
    };

    private final View.OnKeyListener textListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View view, int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                String text = ((TextView) view).getText().toString();
                if (text != null && text.length() > 0) {
                    launchSearch(text);
                }
                return true;
            }
            return false;
        }
    };

    private void launchSearch(String text) {
        Intent intent = new Intent(Intents.Encode.ACTION);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.putExtra(Intents.Encode.TYPE, Contents.Type.TEXT);
        intent.putExtra(Intents.Encode.DATA, text);
        intent.putExtra(Intents.Encode.FORMAT, barcodeFormat);
        startActivity(intent);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.share);

        findViewById(R.id.share_contact_button).setOnClickListener(contactListener);
        findViewById(R.id.share_bookmark_button).setOnClickListener(bookmarkListener);
        findViewById(R.id.share_app_button).setOnClickListener(appListener);
        clipboardButton = (Button) findViewById(R.id.share_clipboard_button);
        clipboardButton.setOnClickListener(clipboardListener);
        findViewById(R.id.share_text_view).setOnKeyListener(textListener);
    }

    @Override
    protected void onResume() {
        super.onResume();

        settings = getSharedPreferences("barcodeFormat",MODE_PRIVATE);
        barcodeFormat = settings.getString("barcodeFormat", BarcodeFormat.QR_CODE.toString());

        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard.hasText()) {
            clipboardButton.setEnabled(true);
            clipboardButton.setText(R.string.button_share_clipboard);
        } else {
            clipboardButton.setEnabled(false);
            clipboardButton.setText(R.string.button_clipboard_empty);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case PICK_BOOKMARK:
                case PICK_APP:
                    showTextAsBarcode(intent.getStringExtra(Browser.BookmarkColumns.URL));
                    break;
                case PICK_CONTACT:
                    // Data field is content://contacts/people/984
                    showContactAsBarcode(intent.getData());
                    break;
            }
        }
    }

    private void showTextAsBarcode(String text) {
        Log.i(TAG, "Showing text as barcode: " + text);
        if (text == null) {
            return; // Show error?
        }
        Intent intent = new Intent(Intents.Encode.ACTION);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.putExtra(Intents.Encode.TYPE, Contents.Type.TEXT);
        intent.putExtra(Intents.Encode.DATA, text);
        intent.putExtra(Intents.Encode.FORMAT, barcodeFormat);
        startActivity(intent);
    }

    /**
     * Takes a contact Uri and does the necessary database lookups to retrieve that person's info,
     * then sends an Encode intent to render it as a QR Code.
     *
     * @param contactUri A Uri of the form content://contacts/people/17
     */
    private void showContactAsBarcode(Uri contactUri) {        
       
        
        Log.i(TAG, "Showing contact URI as barcode: " + contactUri);
        if (contactUri == null) {
          return; // Show error?
        }
        ContentResolver resolver = getContentResolver();
        Cursor contactCursor;
        

        
        
        try {
          // We're seeing about six reports a week of this exception although I don't understand why.
          contactCursor = resolver.query(contactUri, null, null, null, null);
        } catch (IllegalArgumentException e) {
          return;
        }
        
        int contactId;
        
        Bundle bundle = new Bundle();
        if (contactCursor != null && contactCursor.moveToFirst()) {
          int nameColumn = contactCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
          if (nameColumn >= 0) {
            String name = contactCursor.getString(nameColumn);

            // Don't require a name to be present, this contact might be just a phone number.
            if (name != null && name.length() > 0) {
              bundle.putString(ContactsContract.Intents.Insert.NAME, massageContactData(name));
            }
            
            int contactIdColumnIndex = contactCursor.getColumnIndex(ContactsContract.Contacts._ID);
                    //contactCursor.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID);
            contactId = contactCursor.getInt(contactIdColumnIndex);

          } else {
            Log.w(TAG, "Unable to find column? " + ContactsContract.Contacts.DISPLAY_NAME);
            return;
          }
          contactCursor.close();
          /*Uri phonesUri = Uri.withAppendedPath(contactUri, Contacts.People.ContactMethods.CONTENT_DIRECTORY);
          String phonesURI = phonesUri.toString();
          String phoneURI2 = ContactsContract.CommonDataKinds.Phone.CONTENT_URI.toString();
          Cursor phonesCursor = resolver.query(phonesUri, PHONES_PROJECTION, null, null, null);*/
          
          final Cursor phoneCursor = managedQuery(
                  ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                  phoneProjection,
                  ContactsContract.Data.CONTACT_ID + "=?",
                  new String[]{String.valueOf(contactId)},
                  null);
          if (phoneCursor != null) {
            int foundPhone = 0;
            final int contactNumberColumnIndex = 
                    phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            while (phoneCursor.moveToNext()) {
              String number = phoneCursor.getString(contactNumberColumnIndex);
              if (foundPhone < Contents.PHONE_KEYS.length) {
                bundle.putString(Contents.PHONE_KEYS[foundPhone], massageContactData(number));
                foundPhone++;
              }
            }
            phoneCursor.close();
          }
          
          final Cursor emailCursor = managedQuery(
                          ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                          emailProjection,
                          ContactsContract.Data.CONTACT_ID + "=?",
                          new String[]{String.valueOf(contactId)},
                          null);

          int foundEmail = 0;
          if(emailCursor.moveToFirst()) {
              final int contactEmailColumnIndex = 
                      emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA);

              while(!emailCursor.isAfterLast()) {
                  String address = emailCursor.getString(contactEmailColumnIndex);
                  if (foundEmail < Contents.EMAIL_KEYS.length) {
                      bundle.putString(Contents.EMAIL_KEYS[foundEmail], massageContactData(address));
                      foundEmail++;
                    }
                  emailCursor.moveToNext();
              }

          }
          emailCursor.close();
         
          
          final Cursor addressCursor = managedQuery(
                  ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
                  addressProjection,
                  ContactsContract.Data.CONTACT_ID + "=?",
                  new String[]{String.valueOf(contactId)},
                  null);
          while(addressCursor.moveToNext()) {
              String address = addressCursor.getString(
                           addressCursor.getColumnIndex(ContactsContract.CommonDataKinds
                                   .StructuredPostal.FORMATTED_ADDRESS));
           
              bundle.putString(ContactsContract.Intents.Insert.POSTAL, 
                      massageContactData(address));
              break;
          }
          addressCursor.close();
          

          /*Uri methodsUri = Uri.withAppendedPath(contactUri,
              Contacts.People.ContactMethods.CONTENT_DIRECTORY);
          Cursor methodsCursor = resolver.query(methodsUri, METHODS_PROJECTION, null, null, null);
          if (methodsCursor != null) {
            int foundEmail = 0;
            boolean foundPostal = false;
            while (methodsCursor.moveToNext()) {
              int kind = methodsCursor.getInt(METHODS_KIND_COLUMN);
              String data = methodsCursor.getString(METHODS_DATA_COLUMN);
              switch (kind) {
                case Contacts.KIND_EMAIL:
                  if (foundEmail < Contents.EMAIL_KEYS.length) {
                    bundle.putString(Contents.EMAIL_KEYS[foundEmail], massageContactData(data));
                    foundEmail++;
                  }
                  break;
                case Contacts.KIND_POSTAL:
                  if (!foundPostal) {
                    bundle.putString(ContactsContract.Intents.Insert.POSTAL, massageContactData(data));
                    foundPostal = true;
                  }
                  break;
              }
            }
            methodsCursor.close();
          }*/
        
        
        /*Log.i(TAG, "Showing contact URI as barcode: " + contactUri);
        if (contactUri == null) {
            return; // Show error?
        }

        ContentResolver resolver = getContentResolver();
        Cursor contactCursor;
        try {
            // We're seeing about six reports a week of this exception although I don't understand why.
            contactCursor = resolver.query(ContactsContract.Contacts.CONTENT_URI,
                    null, null, null, null);
        } catch (IllegalArgumentException e) {
            return;
        }
        
        Bundle bundle = new Bundle();
        
        int foundPhone = 0;
        int foundEmail = 0;
        
        
        if (contactCursor != null && contactCursor.moveToFirst()) {
            if (contactCursor.getCount() > 0) {
                while (contactCursor.moveToNext()) {
                    String id = contactCursor.getString(
                            contactCursor.getColumnIndex(ContactsContract.Contacts._ID));
                    String name = contactCursor.getString(
                            contactCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                    // Don't require a name to be present, this contact might be just a phone number.
                    if (name != null && name.length() > 0) {
                        bundle.putString(ContactsContract.Intents.Insert.NAME, massageContactData(name));
                    }
                    
                    
                    //Handles Phones
                    if (Integer.parseInt(contactCursor.getString(
                            contactCursor.getColumnIndex(
                                    ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {

                        Cursor phonesCursor = resolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                PHONES_PROJECTION,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = ?",
                                new String[]{id}, null);
                        while (phonesCursor.moveToNext()) {
                            String number = phonesCursor.getString(PHONES_NUMBER_COLUMN);
                            if (foundPhone < Contents.PHONE_KEYS.length) {
                                //massageContactData(number);
                                bundle.putString(Contents.PHONE_KEYS[foundPhone]
                                        , number);
                                foundPhone++;
                            }
                        }
                        phonesCursor.close();
                    }
                    
                    //Handles Emails
                    Cursor emailCursor = resolver.query(
                            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                            new String[]{id}, null);
                    while (emailCursor.moveToNext()) {

                        // This would allow you get several email addresses
                        // if the email addresses were stored in an array
                        
                        String email = emailCursor.getString(
                                emailCursor.getColumnIndex(
                                        ContactsContract.CommonDataKinds.Email.DATA));
                        
                        if (foundEmail < 3){
                            bundle.putString(Contents.EMAIL_KEYS[foundEmail], email);
                            foundEmail++;
                        }
                        break;
                    }
                    emailCursor.close();
                    
                    
                }
            }


*/

            Intent intent = new Intent(Intents.Encode.ACTION);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            intent.putExtra(Intents.Encode.TYPE, Contents.Type.CONTACT);
            intent.putExtra(Intents.Encode.DATA, bundle);
            intent.putExtra(Intents.Encode.FORMAT, barcodeFormat);

            Log.i(TAG, "Sending bundle for encoding: " + bundle);
            startActivity(intent);
        }
    }

    private static String massageContactData(String data) {
        // For now -- make sure we don't put newlines in shared contact data. It messes up
        // any known encoding of contact data. Replace with space.
        if (data.indexOf('\n') >= 0) {
            data = data.replace("\n", " ");
        }
        if (data.indexOf('\r') >= 0) {
            data = data.replace("\r", " ");
        }
        return data;
    }

    private static final int QR_CODE = Menu.FIRST;
    private static final int PDF_417 = Menu.FIRST + 1;

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.clear();
        if (barcodeFormat.equals(BarcodeFormat.PDF_417.toString())){
            menu.add(0, QR_CODE, 0, "QR Code")
            .setIcon(android.R.drawable.radiobutton_off_background);
            menu.add(0, PDF_417, 0, "PDF417")
            .setIcon(android.R.drawable.radiobutton_on_background);
        } else {
            menu.add(0, QR_CODE, 0, "QR Code")
            .setIcon(android.R.drawable.radiobutton_on_background);
            menu.add(0, PDF_417, 0, "PDF 417")
            .setIcon(android.R.drawable.radiobutton_off_background);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case PDF_417: {
                barcodeFormat = BarcodeFormat.PDF_417.toString();
                settings.edit().putString("barcodeFormat", barcodeFormat).commit();
                break;
            }
            case QR_CODE: {
                barcodeFormat = BarcodeFormat.QR_CODE.toString();
                settings.edit().putString("barcodeFormat", barcodeFormat).commit();
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }
}
