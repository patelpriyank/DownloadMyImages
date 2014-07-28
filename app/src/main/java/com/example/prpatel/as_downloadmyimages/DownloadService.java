package com.example.prpatel.as_downloadmyimages;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Base64;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.logging.Handler;

public class DownloadService extends Service {

    private volatile ServiceHandler serviceHandler;
    private volatile Looper bgThreadLooper;

    public DownloadService() {
    }

    public void onCreate()
    {
        Log.d(getClass().getName(), "DownloadService.onCreate()");

        super.onCreate();

        HandlerThread bgThread = new HandlerThread("DownloadService");
        bgThread.start();
        Log.d(getClass().getName(), "HandlerThread.start()");

        bgThreadLooper = bgThread.getLooper();
        serviceHandler = new ServiceHandler(bgThreadLooper);
        Log.d(getClass().getName(), "ServiceHandler(Looper)");
    }

    //Called by the system every time a client explicitly starts the service by calling startService(Intent),
    //providing the arguments it supplied and a unique integer token representing the start request.
    // Do not call this method directly.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(getClass().getName(), "Downloadservice.onStartCommand()");

        //1. download image - send message to serviceHandler
        Message downloadMsg = serviceHandler.MakeDownloadMessage(intent, startId);
        serviceHandler.sendMessage(downloadMsg);
        Log.d(getClass().getName(), "ServiceHandler.sendMessage");

        // Don't restart the DownloadService automatically if its
        // process is killed while it's running.
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy()
    {
        bgThreadLooper.quit();
    }

    /**
     * This hook method is a no-op since we're a Start Service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    /**
     * Factory method to make the desired Intent.
     */
    public static Intent MakeIntent(Context context, Uri url, android.os.Handler downloadHandler)
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

    public static String ExtractDownloadedImagePath(Message msg)
    {
        // Extract the data from Message, which is in the form
        // of a Bundle that can be passed across processes.
        Bundle data = msg.getData();

        //Extract image path name from message
        String imagePath = data.getString("DOWNLOADED_IMAGE_PATH");

        Log.d("DownloadService", "DownloadActivity.ExtractDownloadedImagePath from imagePath " + imagePath);

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
            Log.d(getClass().getName(), "ServiceHandler.handleMessage");
            downloadAndReply((Intent) msg.obj);

            // Stop the Service using the startId, so it doesn't stop
            // in the middle of handling another download request.
            stopSelf(msg.arg1);
        }

        //spply startId to stopSelf(startId) StartId is helpful to keep track of currently running service requests and prevents service from terminating immaturely.
        public Message MakeDownloadMessage(Intent intent, int startId)
        {
            //the best way to get one of these is to call Message.obtain() or one of the Handler.obtainMessage() methods, which will pull them from a pool of recycled objects.
            Message msg = this.obtainMessage();
            msg.obj = intent;
            msg.arg1 = startId;

            return msg;
        }

        private void downloadAndReply(Intent intent) {

            Log.d(getClass().getName(), "ServiceHandler.downloadAndReply");
            //1. download requested image and store locally
            String imagePath = downloadImage(DownloadService.this, intent.getData().toString());

            Log.d(getClass().getName(), "Downloaded image at " + imagePath);
            //2. reply message back to component (DownloadActivity) with image url
            replyToComponent(imagePath, intent);
        }

        private String downloadImage(Context downloadServiceClassContext, String url) {
            try {
                final File file = getTemporaryFile(downloadServiceClassContext, url);
                Log.d(getClass().getName(), "    downloading to " + file);

                //1. download image
                final InputStream in = (InputStream)new URL(url).getContent();

                //2. store image locally
                final OutputStream out = new FileOutputStream(file);

                copy(in, out);
                in.close();
                out.close();
                return file.getAbsolutePath();
            } catch (Exception e) {
                Log.e(getClass().getName(),
                        "Exception while downloading. Returning null.");
                Log.e(getClass().getName(),
                        e.toString());
                e.printStackTrace();
                return null;
            }
        }

        private void replyToComponent(String downloadedImageUrl, Intent intent)
        {
            Log.d(getClass().getName(), "ServiceHandler.replyToComponent with imageurl " + downloadedImageUrl);
            Messenger componentMessenger = (Messenger)intent.getExtras().get("MESSENGER");

            Message message = Message.obtain();
            // Return the result to indicate whether the download
            // succeeded or failed.
            message.arg1 = downloadedImageUrl == null ? Activity.RESULT_CANCELED : Activity.RESULT_OK;

            Bundle data = new Bundle();

            // Pathname for the downloaded image.
            data.putString("DOWNLOADED_IMAGE_PATH", downloadedImageUrl);
            message.setData(data);

            try {
                // Send pathname to back to the DownloadActivity.
                componentMessenger.send(message);
            } catch (RemoteException e) {
                Log.e(getClass().getName(), "Exception while sending.", e);
            }
        }

        /**
         * Create a file to store the result of a download.
         *
         * @param context
         * @param url
         * @return
         * @throws IOException
         */
        private File getTemporaryFile(final Context context,
                                      final String url) throws IOException {
            return context.getFileStreamPath(Base64.encodeToString(url.getBytes(),
                    Base64.NO_WRAP)
                    + System.currentTimeMillis());
        }

        /**
         * Copy the contents of an InputStream into an OutputStream.
         *
         * @param in
         * @param out
         * @return
         * @throws IOException
         */
        private int copy(final InputStream in,
                         final OutputStream out) throws IOException {
            final int BUFFER_LENGTH = 1024;
            final byte[] buffer = new byte[BUFFER_LENGTH];
            int totalRead = 0;
            int read = 0;

            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                totalRead += read;
            }

            return totalRead;
        }
    }


}
