package com.jeztek.imok;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.SmsMessage;
import android.util.Log;

public class SmsBridgeService extends Service {
	private static final String DEBUG_ID = "SmsBridgeService";

	private static final int NOTIFICATION_ID = 1;
	
	SmsReceiver mSmsReceiver = new SmsReceiver();
	boolean mIsEnabled = false;
	
	private NotificationManager mNotificationManager;
	
	// IPC calls
	private final ISmsBridge.Stub mBinder = new ISmsBridge.Stub() {

		@Override
		public void startBridge() throws RemoteException {
			IntentFilter filter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
			registerReceiver(mSmsReceiver, filter);
			showNotification("SmsBridge", "Bridge Enabled");
			mIsEnabled = true;
			Log.d(DEBUG_ID, "Bridge service started");
		}

		@Override
		public void stopBridge() throws RemoteException {
			unregisterReceiver(mSmsReceiver);
			mIsEnabled = false;
			mNotificationManager.cancel(NOTIFICATION_ID);
			Log.d(DEBUG_ID, "Bridge service stopped");
		}

		@Override
		public boolean isEnabled() throws RemoteException {
			return mIsEnabled;
		}		
	};

	@Override
	public void onCreate() {
		super.onCreate();
		mNotificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	private void showNotification(CharSequence title, CharSequence notifyText) {
		Intent notificationIntent = new Intent(getApplication(), SmsBridgeActivity.class);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		Notification notification = new Notification(android.R.drawable.sym_action_chat, notifyText, System.currentTimeMillis());
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.flags |= Notification.FLAG_NO_CLEAR;

		notification.setLatestEventInfo(getApplicationContext(), title, notifyText, contentIntent);
		mNotificationManager.notify(NOTIFICATION_ID, notification);
	}

	public class SmsReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			
			Bundle bundle = intent.getExtras();
			if (bundle != null) {
				Object[] pdus = (Object[])bundle.get("pdus");
				SmsMessage[] messages = new SmsMessage[pdus.length];
				for (int i = 0; i < messages.length; i++) {
					messages[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
					String messageText = messages[i].getMessageBody();
					if (messageText.length() > 30) {
						messageText = messageText.substring(0, 30);
					}
					showNotification("SmsBridge", messageText);
					Log.d(DEBUG_ID, "Got SMS from " + messages[i].getDisplayOriginatingAddress());
					Log.d(DEBUG_ID, "\t" + messageText);
				}
			}
		}
	}
}
