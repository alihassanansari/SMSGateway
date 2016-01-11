package hr.evorion.smsgateway;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import hr.evorion.smsgateway.GatewayService;

public class EvorionBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        //Toast.makeText(context, "rebooted..", Toast.LENGTH_LONG).show();
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context);
        Boolean flag = prefs.getBoolean("gateway_switch",false);
        if(flag) {
            String server = prefs.getString("email_account_server", "mail.prijevodi-online.net");
            String username = prefs.getString("email_account_username", "mail.prijevodi-online.net");
            String password = prefs.getString("email_account_password", "mail.prijevodi-online.net");
            String poll = prefs.getString("gateway_service_poll_frequency", "mail.prijevodi-online.net");

            Intent startServiceIntent = new Intent(context, GatewayService.class);
            startServiceIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            startServiceIntent.putExtra("server", server);
            startServiceIntent.putExtra("username", username);
            startServiceIntent.putExtra("password", password);
            startServiceIntent.putExtra("poll", poll);
            context.startService(startServiceIntent);
        }
        else {
            Toast.makeText(context, "switch off..", Toast.LENGTH_LONG).show();
        }
    }
}