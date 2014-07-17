package com.example.prpatel.as_downloadmyimages;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;

import java.util.logging.Handler;

public class DownloadService extends Service {
    public DownloadService() {
    }

    public void onCreate()
    {
        super.onCreate();

    }

    /**
     * Factory method to make the desired Intent.
     */
    public static Intent makeIntent(Context context, Uri url, android.os.Handler downloadHandler)
    {
        //Make intent with
        // 1. context of component (UI Activity) who is planning to call this service
        // 2. Class this intent is associated with
        // 3. Image URL to download image from
        // 4. Messenger associated with handler of component (DownloadActivity) so that service can post result back to component
        Intent downloadIntent = new Intent(context, DownloadService.class);
        downloadIntent.setData(url);
        downloadIntent.putExtra("MESSENGER", new Messenger(downloadHandler));
        return downloadIntent;
    }

    public static String extractDownloadedImagePath(Message msg)
    {
        // Extract the data from Message, which is in the form
        // of a Bundle that can be passed across processes.
        Bundle data = msg.getData();

        //Extract image path name from message
        String imagePath = data.getString("DOWNLOADED_IMAGE_PATH");

        //check if download was successful
        if(msg.arg1 != Activity.RESULT_OK || imagePath == null)
        {
            return null;
        }

        return imagePath;
    }

    private final class ServiceHandler extends android.os.Handler
    {
        /**
         * Class constructor initializes the Looper.
         *
         * @param looper
         *            The Looper that we borrow from DownloadService's HandlerThread.
         */
        public ServiceHandler(Looper looper)
        {
            super(looper);
        }

        public void handleMessage(Message msg)
        {
            //1. download requested image
            //2. store image locally
            //3. reply message back to component (DownloadActivity) with image url
            downloadAndReplyMessage((Intent)msg.obj);

        }

        private void downloadAndReplyMessage(Intent intent) {
            String imagePath = downloadImage(DownloadService.this, intent.getData().toString());
        }

        private String downloadImage(Context downloadServiceClass, String s) {
        }

    }

    /**
     * This hook method is a no-op since we're a Start Service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

}
