package com.jeztek.imok;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class SmsBridgeActivity extends Activity {
	private static final String DEBUG_ID = "SmsBridgeActivity";
	
	private static final int BRIDGE_MENU_ID = Menu.FIRST;

	private boolean mIsBridgeEnabled = false;
	private ISmsBridge mService;
	private boolean mServiceBound = false;
	
	private MenuItem mBridgeMenuItem;
	
	private ServiceConnection mServiceConn = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = ISmsBridge.Stub.asInterface(service);
			mServiceBound = true;
			
			try {
				mIsBridgeEnabled = mService.isEnabled();
			}
			catch (RemoteException e) {
				Log.e(DEBUG_ID, "Error getting bridge status on bind");
			}
			Log.d(DEBUG_ID, "Bound to bridge service");
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
			mServiceBound = false;
			Log.d(DEBUG_ID, "Disconnected from bridge service");
		}
	};
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        startService(new Intent(this, SmsBridgeService.class));
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	if (mServiceBound)
    		Log.d(DEBUG_ID, "Unbinding from bridge service");
    		unbindService(mServiceConn);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
		Log.d(DEBUG_ID, "Binding to bridge service");
    	mServiceBound = bindService(new Intent(this, SmsBridgeService.class), mServiceConn, Context.BIND_AUTO_CREATE);
    	if (!mServiceBound) {
    		Log.e(DEBUG_ID, "Error binding to service");
    	}
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
    	
    	mBridgeMenuItem = menu.add(0, BRIDGE_MENU_ID, 0, R.string.main_menu_startbridge);
    	return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	super.onPrepareOptionsMenu(menu);
    	
    	if (mIsBridgeEnabled) {
    		mBridgeMenuItem.setTitle(R.string.main_menu_stopbridge);
    		mBridgeMenuItem.setIcon(android.R.drawable.ic_menu_delete);
    	}
    	else {
    		mBridgeMenuItem.setTitle(R.string.main_menu_startbridge);
    		mBridgeMenuItem.setIcon(android.R.drawable.ic_menu_add);
    	}
    	return true;
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
    	switch (item.getItemId()) {
    	case BRIDGE_MENU_ID:
    		if (mIsBridgeEnabled)
    			stopBridge();
    		else
    			startBridge();
    		break;
    	}
    	return super.onMenuItemSelected(featureId, item);
    }
    
    private void startBridge() {
    	if (!mServiceBound) {
    		Log.e(DEBUG_ID, "Service not bound while trying to start bridge");
    		return;
    	}
    	try {
    		if (!mIsBridgeEnabled) {
    			mService.startBridge();
    			mIsBridgeEnabled = true;
    		}    		
    	}
    	catch (RemoteException e) {
    		Log.e(DEBUG_ID, "Error while starting bridge");
    	}
    }
    
    private void stopBridge() {
    	if (!mServiceBound) {
    		Log.e(DEBUG_ID, "Service not bound while trying to stop bridge");
    		return;
    	}
    	try {
    		if (mIsBridgeEnabled) {
    			mService.stopBridge();
    			mIsBridgeEnabled = false;
    		}
    	}
    	catch (RemoteException e) {
    		Log.e(DEBUG_ID, "Error while stopping bridge");
    	}
    }
}