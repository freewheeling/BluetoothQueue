package hypermatix.com.bluetoothqueue;

import android.app.DialogFragment;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends ActionBarActivity
    implements DeviceFragment.OnDeviceSelectListener{

    Button mButtonConnect, mButtonSendCommand;
    TextView mTextResult, mTextStats;
    BluetoothQueueService mBluetoothService;
    boolean mIsBound = false;

    private final BroadcastReceiver mServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action == BluetoothQueueService.DEVICE_CONNECTED){
                mButtonConnect.setText(R.string.disconnect_device);
                mButtonSendCommand.setEnabled(true);
            }
            if(action == BluetoothQueueService.DEVICE_DISCONNECTED){
                mButtonConnect.setText(R.string.connect_device);
                mButtonSendCommand.setEnabled(false);
                mTextResult.setText(null);
                mTextStats.setText(null);
            }
            if(action == BluetoothQueueService.DEVICE_CHARACTERISTIC_READ){
                mTextResult.setText(
                        String.format("Serial No.: %s",intent.getStringExtra(BluetoothQueueService.EXTRA_VALUE)));
            }
            if(action == BluetoothQueueService.DEVICE_QUEUE_STATS){
                mTextStats.setText(intent.getStringExtra(BluetoothQueueService.EXTRA_VALUE));
            }
        }
    };

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            BluetoothQueueService.BluetoothQueueServiceLocalBinder binder =
                    (BluetoothQueueService.BluetoothQueueServiceLocalBinder) service;
            mBluetoothService = binder.getService();
            mIsBound = true;
            registerServiceReceiver();
        }

        public void onServiceDisconnected(ComponentName arg0) {
            mIsBound = false;
            unregisterServiceReceiver();
        }
    };

    private void registerServiceReceiver(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothQueueService.DEVICE_CONNECTED);
        intentFilter.addAction(BluetoothQueueService.DEVICE_DISCONNECTED);
        intentFilter.addAction(BluetoothQueueService.DEVICE_CHARACTERISTIC_READ);
        intentFilter.addAction(BluetoothQueueService.DEVICE_QUEUE_STATS);
        registerReceiver(mServiceReceiver,intentFilter);
    }

    private void unregisterServiceReceiver(){
        unregisterReceiver(mServiceReceiver);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = new Intent(this, BluetoothQueueService.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        mButtonConnect = (Button)findViewById(R.id.button_connect);
        mButtonSendCommand = (Button)findViewById(R.id.button_send);
        mButtonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleConnect();
            }
        });
        mButtonSendCommand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendQueuedCommand();
            }
        });
        mTextResult = (TextView)findViewById(R.id.text_result);
        mTextStats = (TextView)findViewById(R.id.text_stats);
    }

    private void toggleConnect(){
        if(mIsBound){
            if(mBluetoothService.isConnected()){
                mBluetoothService.disconnect();
            }else {
                //Select device to connect to
                DeviceFragment fragment = (DeviceFragment) Fragment.instantiate(this, DeviceFragment.class.getName());
                fragment.show(getFragmentManager(), DeviceFragment.TAG);
                mBluetoothService.scanForDevices(true);
            }
        }
    }

    private void sendQueuedCommand(){
        if(mIsBound){
            //Use a sample command with a delay in it to demonstarte
            BluetoothCommand command = new DelayCommand();
            mBluetoothService.queueCommand(command);
        }
    }

    //OnDeviceSelectListener
    public void onDeviceSelect(String address){
        if(mIsBound) mBluetoothService.connect(address);
    }
}