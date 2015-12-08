package hr.evorion.smsgateway;

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;

import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;

/**
 * Created by Vlatko Šurlan on 11/30/15.
 */
public class GatewayService extends IntentService {


    @Override
    protected void onHandleIntent(Intent intent) {
        // mail server connection parameters
        String server = intent.getStringExtra("server");
        String username = intent.getStringExtra("username");
        String password = intent.getStringExtra("password");

        Log.d(getClass().getName(), server);
        Log.d(getClass().getName(), username);
        Log.d(getClass().getName(), password);

        try {
            // connect to my pop3 inbox
            Properties properties = System.getProperties();
            Session session = Session.getDefaultInstance(properties);
            Store store = session.getStore("pop3");
            store.connect(server, username, password);
            Folder inbox = store.getFolder("Inbox");
            inbox.open(Folder.READ_WRITE);

            // get the list of inbox messages
            Message[] messages = inbox.getMessages();

            if (messages.length == 0) System.out.println("No messages found.");

            for (int i = 0; i < messages.length; i++) {
                // stop after listing ten messages
                if (i > 10) {
                    System.exit(0);
                    inbox.close(true);
                    store.close();
                }

                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(
                        messages[i].getAllRecipients()[0].toString().replaceFirst("@.*", ""),
                        null,
                        String.valueOf(messages[i].getContent()),
                        null,
                        null
                );
            }

            inbox.close(true);
            store.close();
        } catch (Exception e) {
            Log.d(getClass().getName(), e.getMessage());
        }

    }

    public GatewayService() {
        super("GatewayService");
    }

}
