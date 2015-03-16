package hypermatix.com.bluetoothqueue;

import android.bluetooth.BluetoothGatt;

import com.movisens.smartgattlib.Characteristic;

//You could subclass this to implement different, custom, or more complex
//types of bluetooth commands (e.g. a compound command involving multiple writes, etc.)
public abstract class BluetoothCommand {
    public void execute(BluetoothGatt gatt){}
}
