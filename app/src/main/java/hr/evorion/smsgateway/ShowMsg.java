package hr.evorion.smsgateway;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

class ShowMsg extends AsyncTask<Void, Void, String> {
    String data;
    Context context;

    ShowMsg(String data, Context context)
    {
        this.data = data;
        this.context = context;
    }

    @Override
    protected String doInBackground(Void... arg0) { return ""; }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        Toast.makeText(context, this.data, Toast.LENGTH_LONG).show();
    }
}
