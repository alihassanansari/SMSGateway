package hr.evorion.smsgateway;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.SmsManager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Properties;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;


public class GatewayIntentService extends IntentService {

    private boolean mWeTurnedMobileDataOn = false;
    private String mServer, mUsername, mPassword;
    private Object mSemaphore = new Object();
    private boolean mKeepRunning = true;
    private StopBroadcastReceiver mStopBroadcastReceiver = new StopBroadcastReceiver();
    private ConnectivityBroadcastReceiver mConnectivityBroadcastReceiver = new ConnectivityBroadcastReceiver();

    public class StopBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            GatewayIntentService.this.mKeepRunning = false;
            synchronized (mSemaphore) {
                mSemaphore.notifyAll();
            }
        }
    }

    public class ConnectivityBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Wake up the main loop
            synchronized (mSemaphore) {
                mSemaphore.notifyAll();
            }
        }
    }

    public GatewayIntentService() {
        super("GatewayIntentService");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        IntentFilter stopFilter = new IntentFilter();
        stopFilter.addAction("hr.evorion.smsgateway.STOP_GATEWAY_SERVICE");
        registerReceiver(mStopBroadcastReceiver, stopFilter);

        IntentFilter connectivityFilter = new IntentFilter();
        connectivityFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        connectivityFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        registerReceiver(mConnectivityBroadcastReceiver, connectivityFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mStopBroadcastReceiver);
        unregisterReceiver(mConnectivityBroadcastReceiver);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        int pollFrequency;

        mServer = intent.getStringExtra("server");
        mUsername = intent.getStringExtra("username");
        mPassword = intent.getStringExtra("password");
        pollFrequency   = Integer.parseInt(intent.getStringExtra("pollFrequency")) * 1000;

        while (mKeepRunning) {
            // If we're online we just call processMessageQueue directly
            // If we're offline connecting will cause it to be invoked
            if (isOnline()) {
                processMessageQueue();
                if (mWeTurnedMobileDataOn && isOnline()) {
                    setMobileData(false);
                    mWeTurnedMobileDataOn = false;
                }

                try { Thread.sleep(pollFrequency); }
                catch (InterruptedException e) { e.printStackTrace(); }
            }
            else {
                setMobileData(true);
                mWeTurnedMobileDataOn = true;

                // Now we just wait until we get connection
                synchronized (mSemaphore) {
                    try { if (!isOnline()) mSemaphore.wait(); }
                    catch (InterruptedException e) { e.printStackTrace(); }
                }
            }
        }
    }

    // Get the e-mail messages and forward them to SMS
    public synchronized void processMessageQueue() {
        try {
            // Connect to the mailbox
            Session session = Session.getDefaultInstance(System.getProperties());
            Store store = session.getStore("imap");
            store.connect(mServer, mUsername, mPassword);
            Folder folder = store.getFolder("Inbox");
            folder.open(Folder.READ_WRITE);

            // get the list of inbox messages
            Message[] messages = folder.getMessages();
            SmsManager smsManager = SmsManager.getDefault();
            for (int i = 0; i < messages.length; i++) {

                String phoneNo = messages[i].getSubject();
                if (phoneNo == null || phoneNo.isEmpty() ) phoneNo = messages[i].getAllRecipients()[0].toString().split("@")[0];
                String message = messages[i].getContent().toString();

                smsManager.sendTextMessage(phoneNo, null, message, null, null);

                messages[i].setFlag(Flags.Flag.DELETED, true);
            }

            folder.close(true);
            store.close();
        } catch (Exception e) {
            e.printStackTrace();
            new ShowMsg("Forward loop exception: " + e.toString(), getApplicationContext()).execute();
        }
    }

    public void setMobileData(boolean enabled)
    {
        try {
            ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
            Class cmClass = Class.forName(cm.getClass().getName());
            Method setMobileDataMethod = cmClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
            setMobileDataMethod.setAccessible(true);
            setMobileDataMethod.invoke(cm, enabled);
        }
        catch (Exception e) { e.printStackTrace(); }
    }

    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return (networkInfo != null) && networkInfo.isConnected();
    }

}