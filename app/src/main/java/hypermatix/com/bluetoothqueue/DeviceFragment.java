package hypermatix.com.bluetoothqueue;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class DeviceFragment extends DialogFragment
{
    public final static String TAG = DeviceFragment.class.getSimpleName();

    private static AlertDialog.Builder builder;
    DeviceAdapter mDeviceAdapter;
    OnDeviceSelectListener mDeviceSelectListener;

    private final BroadcastReceiver mDeviceScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String address = intent.getStringExtra(DeviceScanCallback.EXTRA_ADDRESS);
            String name = intent.getStringExtra(DeviceScanCallback.EXTRA_NAME);
            mDeviceAdapter.addDevice(address,name);
        }
    };

    public interface OnDeviceSelectListener{
        void onDeviceSelect(String address);
    }

    public DeviceFragment(){}

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume()
    {
        //Register to receive device scan results
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DeviceScanCallback.DEVICE_SCAN_RESULT);
        getActivity().registerReceiver(mDeviceScanReceiver,intentFilter);
        super.onResume();
    }

    @Override
    public void onPause()
    {
        getActivity().unregisterReceiver(mDeviceScanReceiver);
        super.onPause();
    }

    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);
        mDeviceSelectListener = (OnDeviceSelectListener)activity;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        // Initializes list view adapter.
        mDeviceAdapter = new DeviceAdapter();

        builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.select_device);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View content = inflater.inflate(R.layout.fragment_device, null, false);
        ListView deviceList  = (ListView) content.findViewById(R.id.list);
        deviceList.setAdapter(mDeviceAdapter);
        deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                if(mDeviceSelectListener != null)
                    mDeviceSelectListener.onDeviceSelect((String)view.getTag());
                getDialog().dismiss();
            }
        });

        builder.setView(deviceList);
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                //Standard dismiss
            }
        });
        return builder.create();
    }

    private class DeviceAdapter extends BaseAdapter
    {
        private ArrayList<String> mDeviceAddress = new ArrayList<String>();
        private ArrayList<String> mDeviceName = new ArrayList<String>();
        private LayoutInflater mInflator;

        public DeviceAdapter()
        {
            super();
            mInflator = getActivity().getLayoutInflater();
        }

        public void addDevice(String address, String name)
        {
            if(!mDeviceAddress.contains(address))
            {
                mDeviceAddress.add(address);
                mDeviceName.add(name);
                notifyDataSetChanged();
            }
        }

        @Override
        public int getCount()
        {
            return mDeviceAddress.size();
        }

        @Override
        public Object getItem(int i)
        {
            return mDeviceAddress.get(i);
        }

        @Override
        public long getItemId(int i)
        {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup)
        {
            if(view == null)
            {
                view = mInflator.inflate(android.R.layout.simple_list_item_1, null);
            }

            String address = mDeviceAddress.get(i);
            String name = mDeviceName.get(i);
            ((TextView)view.findViewById(android.R.id.text1)).setText(name);
            view.setTag(address);

            return view;
        }
    }
}
