package hypermatix.com.bluetoothqueue;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.movisens.smartgattlib.Characteristic;

import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class BluetoothQueueService extends Service {

    //Broadcast Intent Constants
    public final static String DEVICE_CONNECTED = "com.hypermatix.bluetooth.DEVICE_CONNECTED";
    public final static String DEVICE_DISCONNECTED = "com.hypermatix.bluetooth.DEVICE_DISCONNECTED";
    public final static String DEVICE_CHARACTERISTIC_READ = "com.hypermatix.bluetooth.DEVICE_CHARACTERISTIC_READ";
    public final static String DEVICE_QUEUE_STATS = "com.hypermatix.bluetooth.DEVICE_QUEUE_STATS";
    public final static String EXTRA_VALUE = "com.hypermatix.bluetooth.EXTRA_VALUE";

    Handler mHandler = new Handler();
    DeviceScanCallback mScanCallback;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothGatt mBluetoothGatt;
    LinkedList<BluetoothCommand> mCommandQueue = new LinkedList<BluetoothCommand>();
    //Command Operation executor - will only run one at a time
    Executor mCommandExecutor = Executors.newSingleThreadExecutor();
    //Semaphore lock to coordinate command executions, to ensure only one is
    //currently started and waiting on a response.
    Semaphore mCommandLock = new Semaphore(1,true);

    private final IBinder mBluetoothQueueServiceLocalBinder = new BluetoothQueueServiceLocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        initialize();
        return mBluetoothQueueServiceLocalBinder; //Locally-bound only
    }

    public class BluetoothQueueServiceLocalBinder extends Binder {
        BluetoothQueueService getService() {
            return BluetoothQueueService.this;
        }
    }

    private void initialize(){
        if(mBluetoothAdapter == null){
            //Initialize Bluetooth
            BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }
    }

    @Override
    public void onDestroy(){
        disconnect();
        if(mBluetoothGatt != null) mBluetoothGatt.close();
        super.onDestroy();
    }


    //Function to scan for advertising BLE devices to connect to
    public boolean scanForDevices(boolean on){
        if(on){
            //Turn scanning on
            if(!isScanningDevices()){
                mScanCallback = new DeviceScanCallback(this);
                //Look for Devices with a standard DEVICE_INFORMATION service
                mBluetoothAdapter.startLeScan(new UUID[] {com.movisens.smartgattlib.Service.DEVICE_INFORMATION},
                        mScanCallback);
                //Ensure we won't Scan forever (save battery)
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(isScanningDevices())
                            scanForDevices(false);
                    }
                }, 30000);

            }
        }else{
            //Turn scanning off
            if(isScanningDevices()){
                mBluetoothAdapter.stopLeScan(mScanCallback);
                mScanCallback = null;
            }
        }
        return true;
    }

    public boolean isScanningDevices(){
        return mScanCallback != null;
    }

    public void connect(String address){
        mBluetoothGatt = mBluetoothAdapter.getRemoteDevice(address).connectGatt(this,false,mGattCallback);
    }

    public void disconnect(){
        if(isConnected()) mBluetoothGatt.disconnect();
    }

    public boolean isConnected(){
        return mBluetoothGatt != null;
    }

    public void queueCommand(BluetoothCommand command){
        synchronized (mCommandQueue) {
            mCommandQueue.add(command);  //Add to end of stack
            sendBroadcast(DEVICE_QUEUE_STATS,
                    String.format("Commands in queue: %d",mCommandQueue.size()));
            //Schedule a new runnable to process that command (one command at a time executed only)
            ExecuteCommandRunnable runnable = new ExecuteCommandRunnable(command);
            mCommandExecutor.execute(runnable);
        }
    }

    //Remove the current command from the queue, and release the lock
    //signalling the next queued command (if any) that it can start
    protected void dequeueCommand(){
        mCommandQueue.pop();
        sendBroadcast(DEVICE_QUEUE_STATS,
                String.format("Commands in queue: %d",mCommandQueue.size()));
        mCommandLock.release();
    }

    private void sendBroadcast(String intentAction){
        sendBroadcast(new Intent(intentAction));
    }

    private void sendBroadcast(String intentAction, String value){
        Intent intent = new Intent(intentAction);
        intent.putExtra(EXTRA_VALUE,value);
        sendBroadcast(intent);
    }

    //Runnable to execute a command from the queue
    class ExecuteCommandRunnable implements Runnable{

        BluetoothCommand mCommand;

        public ExecuteCommandRunnable(BluetoothCommand command) {
            mCommand = command;
        }

        @Override
        public void run() {
            //Acquire semaphore lock to ensure no other operations can run until this one completed
            mCommandLock.acquireUninterruptibly();
            //Tell the command to start itself.
            mCommand.execute(mBluetoothGatt);
        }
    };


    //The main callback to handle bluetooth gatt client notifications
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if(newState == BluetoothGatt.STATE_CONNECTED){
                if(status == BluetoothGatt.GATT_SUCCESS) {
                    mBluetoothGatt.discoverServices();
                }else{
                    mBluetoothGatt.close();
                    mBluetoothGatt = null;
                }
            }
            if(newState == BluetoothGatt.STATE_DISCONNECTED){
                mBluetoothGatt.close();
                mBluetoothGatt = null;
                sendBroadcast(DEVICE_DISCONNECTED);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            sendBroadcast(DEVICE_CONNECTED);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if(characteristic.getUuid().equals(Characteristic.SERIAL_NUMBER_STRING)){
                sendBroadcast(DEVICE_CHARACTERISTIC_READ,characteristic.getStringValue(0));
                dequeueCommand();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }
    };
}
