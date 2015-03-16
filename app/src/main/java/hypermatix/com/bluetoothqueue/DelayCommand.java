package hypermatix.com.bluetoothqueue;

import android.bluetooth.BluetoothGatt;

import com.movisens.smartgattlib.Characteristic;

public class DelayCommand extends BluetoothCommand {
    public void execute(BluetoothGatt gatt){
        try {
            synchronized (Thread.currentThread()) {
                Thread.currentThread().wait(500);
            }
        }catch(InterruptedException e){
            //ignore
        }

        //As an example, read from serial number characteristic
        gatt.readCharacteristic(
            gatt.getService(com.movisens.smartgattlib.Service.DEVICE_INFORMATION)
                .getCharacteristic(Characteristic.SERIAL_NUMBER_STRING));
    }
}
