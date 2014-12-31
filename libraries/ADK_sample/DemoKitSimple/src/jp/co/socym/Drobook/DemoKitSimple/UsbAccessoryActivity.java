package jp.co.socym.Drobook.DemoKitSimple;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

//アプリ非起動時に、USBが接続されると自動的に立ち上がるアクティビティ (1)
public final class UsbAccessoryActivity extends Activity {

	static final String TAG = "Socym.DemoKitSimple";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG,"[UsbAccessoryActivity] onCreate");

		//DemoKitActivityを実行する (2)
		Intent intent = new Intent(this, DemoKitActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_CLEAR_TOP);
		try {
			startActivity(intent);
		} catch (ActivityNotFoundException e) {
			Log.e(TAG, "unable to start DemoKit activity", e);
		}
		finish();
	}
}
