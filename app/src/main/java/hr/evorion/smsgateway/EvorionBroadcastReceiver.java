package hr.evorion.smsgateway;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.util.Log;

import hr.evorion.smsgateway.GatewayService;

public class EvorionBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent startServiceIntent = new Intent(context, GatewayService.class);
        //startServiceIntent.putExtra("server", ((EditTextPreference) findPreference("email_account_server")).getText());
        //startServiceIntent.putExtra("username", ((EditTextPreference) findPreference("email_account_username")).getText());
        //startServiceIntent.putExtra("password", ((EditTextPreference) findPreference("email_account_password")).getText());
        String server = context.getSharedPreferences("email_account_server", Context.MODE_PRIVATE).toString();
        Log.d(getClass().getName(), server);
        context.startService(startServiceIntent);
    }
}