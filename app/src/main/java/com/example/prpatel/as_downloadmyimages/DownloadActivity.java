package com.example.prpatel.as_downloadmyimages;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.lang.ref.WeakReference;


public class DownloadActivity extends Activity {

    Handler downloadHandler = null;
    EditText mUrlEditText = null;
    private ProgressDialog mProgressDialog;
    ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_download);

        mUrlEditText = (EditText) findViewById(R.id.mUrlEditText);

        //handler to handle communication back from Backend download service
        downloadHandler = new DownloadHandler(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.download, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void downloadImage_ButtonClick(View view)
    {
        //1. Make intent
        String imageUrl = getImageUrlFromTextBox();
        Intent downloadIntent = DownloadService.MakeIntent(this, Uri.parse(imageUrl), downloadHandler);

        ShowProgressDialog("Downloading image...");

        //2. Start service
        startService(downloadIntent);
    }

    //this method extracts User supplied image url from EditBox
    private String getImageUrlFromTextBox() {
        return mUrlEditText.getText().toString();
    }

    private void displayImage(Bitmap image)
    {
        //set downloaded image to ImageView
        if (mImageView == null)
            showToastDialog("Problem with Application,"
                    + " please contact the Developer.");
        else if (image != null)
            mImageView.setImageBitmap(image);
        else
            showToastDialog("image is corrupted,"
                    + " please check the requested URL.");
    }

    public void ShowProgressDialog(String message)
    {
        mProgressDialog = ProgressDialog.show(this, "Download Image", message);
    }
    public void DismissDialog()
    {
        if(mProgressDialog != null)
            mProgressDialog.dismiss();
    }
    /**
     * Show a toast, notifying a user of an error when retrieving a
     * bitmap.
     */
    private void showToastDialog(String message)
    {
        Toast.makeText(this,message,Toast.LENGTH_LONG);
    }
    /**
     * Hide the keyboard after a user has finished typing the url.
     */
    private void hideKeyboard() {
        InputMethodManager mgr =
                (InputMethodManager) getSystemService
                        (Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(mUrlEditText.getWindowToken(),
                0);
    }

    private class DownloadHandler extends Handler{

        private WeakReference<DownloadActivity> mActivity;

        public DownloadHandler(DownloadActivity downloadActivity) {
            mActivity = new WeakReference<DownloadActivity>(downloadActivity);
        }

        //handles response back from DownloadService's ServiceHandler
        public void handleMessage(Message svcReplyMsg)
        {
            DownloadActivity activity = mActivity.get();

            //if by this time, DownloadActivity was destroyed, then do nothing and just return.
            if(activity == null) return;

           //1. Extract downloaded image location Path from response message
            String downloadedImageUrl = DownloadService.ExtractDownloadedImagePath(svcReplyMsg);

            //2. Display image on UI
            if(downloadedImageUrl == null)
                activity.ShowProgressDialog("Download failed");

            activity.DismissDialog();

            activity.displayImage(BitmapFactory.decodeFile(downloadedImageUrl));
        }
    }
}
