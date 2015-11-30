package hr.evorion.smsgateway;

import android.os.AsyncTask;
import android.util.Log;

import java.util.Properties;

import javax.mail.*;

/**
 * Created by Vlatko Šurlan on 11/30/15.
 */
public class GatewayEngine extends AsyncTask<String[], Integer, Long> {

    protected Long doInBackground(String[]... gatewayEngineSettings) {
        Log.d(getClass().getName(), gatewayEngineSettings[0][0]);
        Log.d(getClass().getName(), gatewayEngineSettings[0][1]);
        Log.d(getClass().getName(), gatewayEngineSettings[0][2]);

        try {
            //create properties field
            Properties properties = new Properties();

            properties.put("mail.pop3.host", gatewayEngineSettings[0][0]);
            properties.put("mail.pop3.port", "110");
            properties.put("mail.pop3.starttls.enable", "false");
            Session emailSession = Session.getDefaultInstance(properties);

            //create the POP3 store object and connect with the pop server
            Store store = emailSession.getStore("pop3s");

            store.connect(
                    gatewayEngineSettings[0][0],
                    gatewayEngineSettings[0][1],
                    gatewayEngineSettings[0][2]
            );

            //create the folder object and open it
            Folder emailFolder = store.getFolder("INBOX");
            emailFolder.open(Folder.READ_ONLY);

            // retrieve the messages from the folder in an array and print it
            Message[] messages = emailFolder.getMessages();
            System.out.println("messages.length---" + messages.length);

            for (int i = 0, n = messages.length; i < n; i++) {
                Message message = messages[i];
                System.out.println("---------------------------------");
                System.out.println("Email Number " + (i + 1));
                System.out.println("Subject: " + message.getSubject());
                System.out.println("From: " + message.getFrom()[0]);
                System.out.println("Text: " + message.getContent().toString());

            }

            //close the store and folder objects
            emailFolder.close(false);
            store.close();

        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return (long)0;
    }

}
