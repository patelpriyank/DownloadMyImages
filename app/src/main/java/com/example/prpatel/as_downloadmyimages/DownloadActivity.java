package com.example.prpatel.as_downloadmyimages;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.lang.ref.WeakReference;


public class DownloadActivity extends Activity {

    Handler downloadHandler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

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
        Intent downloadIntent = DownloadService.makeIntent(this, Uri.parse(imageUrl), downloadHandler);

        //2. Start service
        startService(downloadIntent);
    }

    //this method extracts User supplied image url from EditBox
    private String getImageUrlFromTextBox() {
        return null;
    }

    private void displayImage(Bitmap image)
    {
        //set downloaded image to ImageView
    }

    private class DownloadHandler extends Handler{

        private WeakReference<DownloadActivity> mActivity;

        public DownloadHandler(DownloadActivity downloadActivity) {
            mActivity = new WeakReference<DownloadActivity>(downloadActivity);
        }

        //handles response back from DownloadService's ServiceHandler
        public void handleMessage(Message msg)
        {
            DownloadActivity activity = mActivity.get();

            //if by this time, DownloadActivity was destroyed, then do nothing and just return.
            if(activity == null) return;

           //1. Extract downloaded image location Path from response message
            String downloadedImageUrl = DownloadService.get


            //2. Display image on UI
        }
    }
}
