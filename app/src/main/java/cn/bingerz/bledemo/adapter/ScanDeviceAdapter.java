package cn.bingerz.bledemo.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cn.bingerz.bledemo.R;
import cn.bingerz.flipble.central.CentralManager;
import cn.bingerz.flipble.central.ScanDevice;

public class ScanDeviceAdapter extends RecyclerView.Adapter<ScanDeviceAdapter.ViewHolder> {

    private List<ScanDevice> mScanDevices;

    public ScanDeviceAdapter() {
        mScanDevices = new ArrayList<>();
    }


    public void addDevice(ScanDevice device) {
        removeDevice(device);
        mScanDevices.add(device);
    }

    public void removeDevice(ScanDevice scanDevice) {
        for (int i = 0; i < mScanDevices.size(); i++) {
            ScanDevice device = mScanDevices.get(i);
            if (device.getAddress().equals(scanDevice.getAddress())) {
                mScanDevices.remove(i);
            }
        }
    }

    public void clearConnectedDevice() {
        for (int i = 0; i < mScanDevices.size(); i++) {
            ScanDevice device = mScanDevices.get(i);
            if (CentralManager.getInstance().isConnected(device.getAddress())) {
                mScanDevices.remove(i);
            }
        }
    }

    public void clearScanDevice() {
        for (int i = 0; i < mScanDevices.size(); i++) {
            ScanDevice device = mScanDevices.get(i);
            if (!CentralManager.getInstance().isConnected(device.getAddress())) {
                mScanDevices.remove(i);
            }
        }
    }

    public void clear() {
        clearConnectedDevice();
        clearScanDevice();
    }

    @Override
    public int getItemCount() {
        return mScanDevices == null ? 0 : mScanDevices.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_device, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final ScanDevice device = mScanDevices.get(position);
        if (device != null) {
            boolean isConnected = CentralManager.getInstance().isConnected(device.getAddress());
            String name = device.getName();
            String mac = device.getAddress();
            int rssi = device.getRssi();
            holder.tvName.setText(name);
            holder.tvMacAddress.setText(mac);
            holder.tvRssi.setText(String.valueOf(rssi));
            if (isConnected) {
                holder.ivBluetooth.setImageResource(R.mipmap.ic_blue_connected);
                holder.tvName.setTextColor(0xFF4a90e2);
                holder.tvMacAddress.setTextColor(0xFF4a90e2);
                holder.llIdle.setVisibility(View.GONE);
                holder.llConnected.setVisibility(View.VISIBLE);
            } else {
                holder.ivBluetooth.setImageResource(R.mipmap.ic_blue_remote);
                holder.tvName.setTextColor(0xFF000000);
                holder.tvMacAddress.setTextColor(0xFF000000);
                holder.llIdle.setVisibility(View.VISIBLE);
                holder.llConnected.setVisibility(View.GONE);
            }
        }
        holder.btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    mListener.onConnect(device);
                }
            }
        });

        holder.btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    mListener.onDisConnect(device);
                }
            }
        });

        holder.btnDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    mListener.onDetail(device);
                }
            }
        });
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivBluetooth;
        private TextView tvName;
        private TextView tvMacAddress;
        private TextView tvRssi;
        private LinearLayout llIdle;
        private LinearLayout llConnected;
        private Button btnDisconnect;
        private Button btnConnect;
        private Button btnDetail;

        public ViewHolder(View itemView) {
            super(itemView);
            ivBluetooth = itemView.findViewById(R.id.iv_bluetooth);
            tvName = itemView.findViewById(R.id.tv_name);
            tvMacAddress = itemView.findViewById(R.id.tv_mac);
            tvRssi = itemView.findViewById(R.id.tv_rssi);
            llIdle = itemView.findViewById(R.id.layout_idle);
            llConnected = itemView.findViewById(R.id.layout_connected);
            btnDisconnect = itemView.findViewById(R.id.btn_disconnect);
            btnConnect = itemView.findViewById(R.id.btn_connect);
            btnDetail = itemView.findViewById(R.id.btn_detail);
        }
    }

    public interface OnDeviceClickListener {
        void onConnect(ScanDevice device);

        void onDisConnect(ScanDevice device);

        void onDetail(ScanDevice device);
    }

    private OnDeviceClickListener mListener;

    public void setOnDeviceClickListener(OnDeviceClickListener listener) {
        this.mListener = listener;
    }

}