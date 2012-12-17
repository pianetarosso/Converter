package it.converter;

import it.converter.classes.R;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;

// id dei docs su google per la segnalazione
@ReportsCrashes(formKey = "dDVDcHRTQ28yMVJGTU9DclBncmZVRUE6MQ#gid=0", 
				mode = ReportingInteractionMode.TOAST,
				forceCloseDialogAfterToast = false, // optional, default false
				resToastText = R.string.crash_toast_text)

public class MyActivity extends Application {

	@Override
	public void onCreate() {
		// avvio il monitoraggio
        ACRA.init(this);
		super.onCreate();
	}

}
