package hypermatix.com.bluetoothqueue;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;

public class DeviceScanCallback implements BluetoothAdapter.LeScanCallback {

    //Broadcast Intent Constants
    public final static String DEVICE_SCAN_RESULT = "com.hypermatix.bluetooth.DEVICE_SCAN_RESULT";
    public final static String EXTRA_ADDRESS = "com.hypermatix.bluetooth.EXTRA_ADDRESS";
    public final static String EXTRA_NAME = "com.hypermatix.bluetooth.EXTRA_NAME";

    ArrayList<String> mDiscoveredDevices = new ArrayList<String>();
    Context mContext;

    public DeviceScanCallback(Context context){
        mContext = context;
    }

    @Override
    public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord)
    {
        if(!mDiscoveredDevices.contains(device.getAddress())) {
            mDiscoveredDevices.add(device.getAddress());
            //Send a broadcast to main part of the app to alert it to new found device
            Intent broadcast = new Intent(DEVICE_SCAN_RESULT);
            broadcast.putExtra(EXTRA_ADDRESS, device.getAddress());
            broadcast.putExtra(EXTRA_NAME, device.getName());
            mContext.sendBroadcast(broadcast);
        }
    }
}
