package cn.bingerz.bledemo;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.test.espresso.IdlingResource;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cn.bingerz.bledemo.adapter.ScanDeviceAdapter;
import cn.bingerz.bledemo.comm.ObserverManager;
import cn.bingerz.bledemo.operation.OperationActivity;
import cn.bingerz.bledemo.util.EspressoIdlingResource;
import cn.bingerz.flipble.central.CentralManager;
import cn.bingerz.flipble.central.ScanDevice;
import cn.bingerz.flipble.exception.BLEException;
import cn.bingerz.flipble.peripheral.Peripheral;
import cn.bingerz.flipble.peripheral.callback.ConnectStateCallback;
import cn.bingerz.flipble.peripheral.callback.MtuChangedCallback;
import cn.bingerz.flipble.peripheral.callback.RssiCallback;
import cn.bingerz.flipble.central.callback.ScanCallback;
import cn.bingerz.flipble.central.ScanRuleConfig;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_CODE_OPEN_GPS = 1;
    private static final int REQUEST_CODE_PERMISSION_LOCATION = 2;

    private LinearLayout llSetting;
    private TextView tvSetting;
    private Button btnScan;
    private EditText etName, etMac, etUUID;
    private Switch swAuto;
    private ImageView ivLoading;

    private Animation operatingAnim;
    private ScanDeviceAdapter mScanDeviceAdapter;
    private ProgressDialog progressDialog;

    private String DEFAULT_SERVICE_UUID = "00001803-0000-1000-8000-00805f9b34fb";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        initView();

        CentralManager.getInstance().init(getApplication());

        CentralManager.getInstance().enableLog(true).setMaxConnectCount(7).setOperateTimeout(5000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        showConnectedDevice();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CentralManager.getInstance().disconnectAllDevice();
        CentralManager.getInstance().destroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_scan:
                if (btnScan.getText().equals(getString(R.string.start_scan))) {
                    checkPermissions();
                } else if (btnScan.getText().equals(getString(R.string.stop_scan))) {
                    stopScan();
                }
                break;

            case R.id.txt_setting:
                if (llSetting.getVisibility() == View.VISIBLE) {
                    llSetting.setVisibility(View.GONE);
                    tvSetting.setText(getString(R.string.expand_search_settings));
                } else {
                    llSetting.setVisibility(View.VISIBLE);
                    tvSetting.setText(getString(R.string.retrieve_search_settings));
                }
                break;
        }
    }

    private void initView() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        btnScan = findViewById(R.id.btn_scan);
        btnScan.setText(getString(R.string.start_scan));
        btnScan.setOnClickListener(this);

        etName = findViewById(R.id.et_name);
        etMac = findViewById(R.id.et_mac);
        etUUID = findViewById(R.id.et_uuid);
        swAuto = findViewById(R.id.sw_auto);

        llSetting = findViewById(R.id.layout_setting);
        tvSetting = findViewById(R.id.txt_setting);
        tvSetting.setOnClickListener(this);
        llSetting.setVisibility(View.GONE);
        tvSetting.setText(getString(R.string.expand_search_settings));

        ivLoading = findViewById(R.id.img_loading);
        operatingAnim = AnimationUtils.loadAnimation(this, R.anim.rotate);
        operatingAnim.setInterpolator(new LinearInterpolator());
        progressDialog = new ProgressDialog(this);

        mScanDeviceAdapter = new ScanDeviceAdapter();
        mScanDeviceAdapter.setOnDeviceClickListener(new ScanDeviceAdapter.OnDeviceClickListener() {
            @Override
            public void onConnect(ScanDevice device) {
                if (!CentralManager.getInstance().isConnected(device.getAddress())) {
                    EspressoIdlingResource.increment();
                    stopScan();
                    Peripheral peripheral = new Peripheral(device);
                    connect(peripheral);
                }
            }

            @Override
            public void onDisConnect(ScanDevice device) {
                if (CentralManager.getInstance().isConnected(device.getAddress())) {
                    Peripheral peripheral = CentralManager.getInstance().getPeripheral(device.getAddress());
                    if (peripheral != null) {
                        peripheral.disconnect();
                    }
                }
            }

            @Override
            public void onDetail(ScanDevice device) {
                if (CentralManager.getInstance().isConnected(device.getAddress())) {
                    Intent intent = new Intent(MainActivity.this, OperationActivity.class);
                    intent.putExtra(OperationActivity.KEY_DATA, device.getAddress());
                    startActivity(intent);
                }
            }
        });
        RecyclerView mRecyclerView = findViewById(R.id.rv_list);
        mRecyclerView.setHasFixedSize(true);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mScanDeviceAdapter);
    }

    private void showConnectedDevice() {
        List<Peripheral> deviceList = CentralManager.getInstance().getAllConnectedDevice();
        mScanDeviceAdapter.clearConnectedDevice();
        for (Peripheral peripheral : deviceList) {
            mScanDeviceAdapter.addDevice(peripheral.getDevice());
        }
        mScanDeviceAdapter.notifyDataSetChanged();
    }

    private void setScanRule() {
        String[] uuids;
        String str_uuid = etUUID.getText().toString();
        if (TextUtils.isEmpty(str_uuid)) {
            uuids = null;
        } else {
            uuids = str_uuid.split(",");
        }
        UUID[] serviceUuids = new UUID[]{ UUID.fromString(DEFAULT_SERVICE_UUID)};
        if (uuids != null && uuids.length > 0) {
            serviceUuids = new UUID[uuids.length];
            for (int i = 0; i < uuids.length; i++) {
                serviceUuids[i] = UUID.fromString(uuids[i]);
            }
        }

        String[] names;
        String str_name = etName.getText().toString();
        if (TextUtils.isEmpty(str_name)) {
            names = null;
        } else {
            names = str_name.split(",");
        }

        String mac = etMac.getText().toString();

        boolean isAutoConnect = swAuto.isChecked();

        ScanRuleConfig scanRuleConfig = new ScanRuleConfig.Builder()
                .setServiceUuids(serviceUuids)      // 只扫描指定的服务的设备，可选
                .setDeviceName(true, names)   // 只扫描指定广播名的设备，可选
                .setDeviceMac(mac)                  // 只扫描指定mac的设备，可选
                .setScanTimeOut(6000)              // 扫描超时时间，可选，默认10秒
                .build();
        CentralManager.getInstance().initScanRule(scanRuleConfig);
    }

    private void startScan() {
        CentralManager.getInstance().scan(new ScanCallback() {
            @Override
            public void onScanStarted(boolean success) {
                mScanDeviceAdapter.clearScanDevice();
                mScanDeviceAdapter.notifyDataSetChanged();
                ivLoading.startAnimation(operatingAnim);
                ivLoading.setVisibility(View.VISIBLE);
                btnScan.setText(getString(R.string.stop_scan));
            }

            @Override
            public void onScanning(ScanDevice device) {
                if (device.getRssi() > -65) {
                    mScanDeviceAdapter.addDevice(device);
                    mScanDeviceAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onScanFinished(List<ScanDevice> scanResultList) {
                ivLoading.clearAnimation();
                ivLoading.setVisibility(View.INVISIBLE);
                btnScan.setText(getString(R.string.start_scan));
            }
        });
    }

    private void stopScan() {
        if (CentralManager.getInstance().isScanning()) {
            CentralManager.getInstance().cancelScan();
        }
    }

    private void connect(Peripheral peripheral) {
        peripheral.connect(false, new ConnectStateCallback() {
            @Override
            public void onStartConnect() {
                progressDialog.show();
            }

            @Override
            public void onConnectFail(BLEException exception) {
                EspressoIdlingResource.decrement();
                ivLoading.clearAnimation();
                ivLoading.setVisibility(View.INVISIBLE);
                btnScan.setText(getString(R.string.start_scan));
                progressDialog.dismiss();
                Toast.makeText(MainActivity.this, getString(R.string.connect_fail), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onConnectSuccess(Peripheral peripheral, int status) {
                EspressoIdlingResource.decrement();
                progressDialog.dismiss();
                mScanDeviceAdapter.addDevice(peripheral.getDevice());
                mScanDeviceAdapter.notifyDataSetChanged();

                readRssi(peripheral);
                setMtu(peripheral, 23);
            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, Peripheral peripheral, int status) {
                progressDialog.dismiss();

                mScanDeviceAdapter.removeDevice(peripheral.getDevice());
                mScanDeviceAdapter.notifyDataSetChanged();

                if (!isActiveDisConnected) {
                    Toast.makeText(MainActivity.this, getString(R.string.disconnected), Toast.LENGTH_LONG).show();
                    ObserverManager.getInstance().notifyObserver(peripheral);
                }
            }
        });
    }

    private void readRssi(Peripheral peripheral) {
        peripheral.readRssi(new RssiCallback() {
            @Override
            public void onRssiFailure(BLEException exception) {
                Log.i(TAG, "onRssiFailure" + exception.toString());
            }

            @Override
            public void onRssiSuccess(int rssi) {
                Log.i(TAG, "onRssiSuccess: " + rssi);
            }
        });
    }

    private void setMtu(Peripheral peripheral, int mtu) {
        peripheral.setMtu(mtu, new MtuChangedCallback() {
            @Override
            public void onSetMTUFailure(BLEException exception) {
                Log.i(TAG, "onsetMTUFailure" + exception.toString());
            }

            @Override
            public void onMtuChanged(int mtu) {
                Log.i(TAG, "onMtuChanged: " + mtu);
            }
        });
    }

    @Override
    public final void onRequestPermissionsResult(int requestCode,
                                                 @NonNull String[] permissions,
                                                 @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_PERMISSION_LOCATION:
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            onPermissionGranted(permissions[i]);
                        }
                    }
                }
                break;
        }
    }

    private void checkPermissions() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, getString(R.string.please_open_blue), Toast.LENGTH_LONG).show();
            return;
        }

        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
        List<String> permissionDeniedList = new ArrayList<>();
        for (String permission : permissions) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, permission);
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted(permission);
            } else {
                permissionDeniedList.add(permission);
            }
        }
        if (!permissionDeniedList.isEmpty()) {
            String[] deniedPermissions = permissionDeniedList.toArray(new String[permissionDeniedList.size()]);
            ActivityCompat.requestPermissions(this, deniedPermissions, REQUEST_CODE_PERMISSION_LOCATION);
        }
    }

    private void onPermissionGranted(String permission) {
        switch (permission) {
            case Manifest.permission.ACCESS_FINE_LOCATION:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !checkGPSIsOpen()) {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.notifyTitle)
                            .setMessage(R.string.gpsNotifyMsg)
                            .setNegativeButton(R.string.cancel,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            finish();
                                        }
                                    })
                            .setPositiveButton(R.string.setting,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                            startActivityForResult(intent, REQUEST_CODE_OPEN_GPS);
                                        }
                                    })

                            .setCancelable(false)
                            .show();
                } else {
                    setScanRule();
                    startScan();
                }
                break;
        }
    }

    private boolean checkGPSIsOpen() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null)
            return false;
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OPEN_GPS) {
            if (checkGPSIsOpen()) {
                setScanRule();
                startScan();
            }
        }
    }

    @VisibleForTesting
    public IdlingResource getCountingIdlingResource() {
        return EspressoIdlingResource.getIdlingResource();
    }
}
