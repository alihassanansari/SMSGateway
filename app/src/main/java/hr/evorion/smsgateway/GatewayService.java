package hr.evorion.smsgateway;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Properties;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;

/**
 * Created by Vlatko Šurlan on 11/30/15.
 */
public class GatewayService extends Service {

    public static Thread thread;
    int flag=0;
    public int timer = 10000;
    String server, username, password;
    Boolean  mobileDataStatus;
    ArrayList<Long> PUID = new ArrayList<Long>();

    class GatewayServiceThread implements Runnable {
        GatewayServiceThread() {}

        public void run() {
            while (true) {
                if(flag == 1) break;
                else GatewayService.this.runSendLog();
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //this._context = getApplicationContext();

       // mobileDataStatus = checkMobileDataStatus();

       // if (!isWifiConnected(getApplicationContext()) && checkMobileDataStatus() == false) {
       //     new ShowMsg(enableData()).execute();
       // }
        flag = 0;

        thread = new Thread(new GatewayServiceThread());
        thread.start();
        Toast.makeText(getApplicationContext(), "started", Toast.LENGTH_LONG).show();
        //this.logDatabase = new LogDatabase(this._context);
    }

    public synchronized void runSendLog() {

        mobileDataStatus = checkMobileDataStatus();

        if (!isWifiConnected(getApplicationContext()) && checkMobileDataStatus() == false) {
            new ShowMsg(enableData()).execute();
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        do_the_do();

        try {
            Thread.sleep(timer);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    public synchronized void do_the_do()
    {
        try {
            // connect to my pop3 inbox
            Properties properties = System.getProperties();
            Session session = Session.getDefaultInstance(properties);
            final Store store = session.getStore("imap");
            store.connect(server, username, password);
            final Folder inbox = store.getFolder("Inbox");
            UIDFolder ufolder = (UIDFolder) inbox;
            inbox.open(Folder.READ_WRITE);

            // get the list of inbox messages
            final Message[] messages = ufolder.getMessagesByUID(1, UIDFolder.LASTUID);

            if (messages.length == 0) {
                new ShowMsg("No messages found.").execute();

                // The disableData must be duplicated here and in the loop below because the second
                // disableData check is in a different thread that is waiting for the delivery confirmation.
                if (mobileDataStatus == false)
                    new ShowMsg(disableData()).execute();
            }
            else {
                //Log.d("Log \t",String.valueOf(messages.length));
                for (int i = 0; i < messages.length; i++) {

                    //final Message mail = messages[i];
                    final String phoneNo = messages[i].getAllRecipients()[0].toString().split("@")[0];
                    final String msg = messages[i].getContent().toString();
                    final long UID = ufolder.getUID(messages[i]);

                    if (PUID.contains(UID)) {
                        new ShowMsg("again...").execute();
                        continue;
                    }

                    Thread thread1 = new Thread(new Runnable() {
                        @Override
                        public void run() {

                            final String DELIVERED = "delivered" + UID;

                            Intent deliveryIntent = new Intent(DELIVERED);

                            deliveryIntent.putExtra("phone", String.valueOf(UID));
                            final PendingIntent deliverPI = PendingIntent.getBroadcast(
                                    getApplicationContext(), 0, deliveryIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT);

                            registerReceiver(new BroadcastReceiver() {

                                @Override
                                public void onReceive(Context context, final Intent intent) {
                                    Thread thread2 = new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                // connect to my pop3 inbox
                                                Properties properties = System.getProperties();
                                                Session session = Session.getDefaultInstance(properties);
                                                Store store = session.getStore("imap");
                                                store.connect(server, username, password);
                                                Folder inbox = store.getFolder("Inbox");
                                                UIDFolder ufolder = (UIDFolder) inbox;
                                                inbox.open(Folder.READ_WRITE);

                                                long index = Long.parseLong(intent.getStringExtra("phone").trim());
                                                Message msg = ufolder.getMessageByUID(index);
                                                msg.setFlag(Flags.Flag.DELETED, true);
                                                PUID.remove(index);

                                                new ShowMsg(msg.getAllRecipients()[0].toString() + " deleted..").execute();
                                                System.gc();

                                                if (PUID.size() == 0) {
                                                    inbox.close(true);
                                                    store.close();

                                                    if (mobileDataStatus == false)
                                                        new ShowMsg(disableData()).execute();
                                                }

                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                new ShowMsg("Exception: " + e.toString()).execute();
                                            }

                                        }

                                    });

                                    thread2.start();

                                    //new ShowMsg("Deliverd To :"+intent.getStringExtra("phone")).execute();
                                }

                            }, new IntentFilter(DELIVERED));

                            SmsManager smsManager = SmsManager.getDefault();
                            smsManager.sendTextMessage(
                                    phoneNo,
                                    null,
                                    msg,
                                    null,
                                    deliverPI
                            );

                            PUID.add(UID);

                            new ShowMsg(phoneNo + " --- " + msg + " UID: " + UID).execute();

                        }

                    });

                    thread1.start();

                    //Log.d("Log \t",messages[i].getFrom()[0].toString());//+" --- "+messages[i].getContent());
                    //messages[i].setFlag(Flags.Flag.DELETED, true);
                    //Log.d("Log \t", messages[i].getAllRecipients()[0].toString()+" : "+String.valueOf(messages[i].getContent().toString()));
                    //  break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            new ShowMsg("Exceptional :"+e.toString()).execute();
        }
    }


    public Boolean checkMobileDataStatus()
    {
        boolean mobileDataEnabled = false; // Assume disabled
        ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            Class cmClass = Class.forName(cm.getClass().getName());
            Method method = cmClass.getDeclaredMethod("getMobileDataEnabled");
            method.setAccessible(true); // Make the method callable
            // get the setting for "mobile data"
            mobileDataEnabled = (Boolean)method.invoke(cm);
        } catch (Exception e) {
            // Some problem accessible private API
            // TODO do whatever error handling you want here
        }
        return mobileDataEnabled;
    }

    public String enableData()
    {
        ConnectivityManager dataManager;
        dataManager  = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        Method dataMtd = null;
        try {
            dataMtd = ConnectivityManager.class.getDeclaredMethod("setMobileDataEnabled", boolean.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return e.toString();
        }
        dataMtd.setAccessible(true);
        try {
            dataMtd.invoke(dataManager, true);        // True - to enable data connectivity .
            return "data enabled.";
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return e.toString();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            return e.toString();
        }
    }


    public String disableData()
    {
        ConnectivityManager dataManager;
        dataManager  = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        Method dataMtd = null;
        try {
            dataMtd = ConnectivityManager.class.getDeclaredMethod("setMobileDataEnabled", boolean.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return e.toString();
        }
        dataMtd.setAccessible(true);
        try {
            dataMtd.invoke(dataManager, false);        //True - to enable data connectivity .
            return "data disabled.";
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return e.toString();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            return e.toString();
        }
    }


    public static boolean isWifiConnected(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiInfo =
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        boolean wifiConnected = wifiInfo.getState() == NetworkInfo.State.CONNECTED;

        return wifiConnected;
    }

    private class ShowMsg extends AsyncTask<Void, Void, String> {
        String data;

        ShowMsg(String data)
        {
            this.data = data;
        }

        @Override
        protected String doInBackground(Void... arg0) { return ""; }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Toast.makeText(getApplicationContext(), this.data, Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, startId, startId);
        if(intent != null){
            this.server = intent.getStringExtra("server");
            this.username = intent.getStringExtra("username");
            this.password = intent.getStringExtra("password");
            this.timer = Integer.parseInt(intent.getStringExtra("poll"))*1000;
        }
        //Toast.makeText(getApplicationContext(), this.server+":"+this.username+":"+this.password+":"+this.timer, Toast.LENGTH_LONG).show();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //GatewayServiceThread.interrupt();
        System.gc();
        flag = 1;
        //if(mobileDataStatus == false)
          //  new ShowMsg(disableData()).execute();

        Toast.makeText(getApplicationContext(), "killed..", Toast.LENGTH_SHORT).show();
    }

}