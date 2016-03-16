package hr.evorion.smsgateway;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class BootBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (prefs.getBoolean("gateway_switch", false)) {
            String server           = prefs.getString("email_account_server", "mail.prijevodi-online.net");
            String username         = prefs.getString("email_account_username", "mail.prijevodi-online.net");
            String password         = prefs.getString("email_account_password", "mail.prijevodi-online.net");
            String pollFrequency    = prefs.getString("gateway_service_poll_frequency", "mail.prijevodi-online.net");

            Intent gatewayIntentService = new Intent(context, GatewayIntentService.class);
            gatewayIntentService.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            gatewayIntentService.putExtra("server", server);
            gatewayIntentService.putExtra("username", username);
            gatewayIntentService.putExtra("password", password);
            gatewayIntentService.putExtra("pollFrequency", pollFrequency);
            context.startService(gatewayIntentService);
        }
    }
}