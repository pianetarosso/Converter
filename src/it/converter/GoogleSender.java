package it.converter;

import it.converter.classes.R;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.acra.sender.ReportSenderException;
import org.acra.util.HttpUtils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;



public class GoogleSender {
    private Uri mFormUri = null;
    private final String LOG_TAG = "send_measures";
    
    public GoogleSender(Context context) {
    	String formKey = context.getString(R.string.formKey);
        mFormUri = Uri.parse("https://spreadsheets.google.com/formResponse?formkey=" + formKey + "&amp;ifq");
    }

    public void send(ArrayList<Measures> m) throws ReportSenderException {
        Map<String, String> formParams = remap(m);
        // values observed in the GoogleDocs original html form
        formParams.put("pageNumber", "0");
        formParams.put("backupCache", "");
        formParams.put("submit", "Envoyer");

        try {
            final URL reportUrl = new URL(mFormUri.toString());
            Log.d(LOG_TAG, "Connect to " + reportUrl);
            HttpUtils.doPost(formParams, reportUrl, null, null);
        } catch (IOException e) {
            throw new ReportSenderException("Error while sending report to Google Form.", e);
        }

    }

    private Map<String, String> remap(ArrayList<Measures> am) {
        Map<String, String> result = new HashMap<String, String>();

        for (Measures m : am) {
        	
        	result.put("entry." + 0 + ".single", m.getId()); // id
            result.put("entry." + 1 + ".single", m.getSymbol()); // simbolo
            result.put("entry." + 2 + ".single", m.getUnita_riferimento()); // riferimento
            result.put("entry." + 3 + ".single", m.getFrom()); // formula FROM
            result.put("entry." + 4 + ".single", m.getTo()); // formula TO
     
        }

        return result;
    }

}
