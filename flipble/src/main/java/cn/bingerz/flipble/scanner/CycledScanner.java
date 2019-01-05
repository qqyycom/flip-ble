package cn.bingerz.flipble.scanner;

import android.annotation.TargetApi;
import android.os.Build;

import cn.bingerz.flipble.central.CentralManager;
import cn.bingerz.flipble.scanner.callback.ScanCallback;


@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class CycledScanner extends Scanner {

    private long scanDuration = CentralManager.DEFAULT_FOREGROUND_SCAN_DURATION;
    private long scanInterval = CentralManager.DEFAULT_FOREGROUND_SCAN_INTERVAL;

    @Override
    public void initConfig(ScanRuleConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("ScanRuleConfig is null.");
        }
        if (config.getScanDuration() > 0) {
            setDuration(config.getScanDuration());
        }
        if (config.getScanInterval() > 0) {
            setInterval(config.getScanInterval());
        }
        initLeScanner(config);
    }

    private void setDuration(long duration) {
        this.scanDuration = duration;
    }

    private long getDuration() {
        return scanDuration;
    }

    private void setInterval(long interval) {
        this.scanInterval = interval;
    }

    private long getInterval() {
        return scanInterval;
    }

    @Override
    public void startScan(final ScanCallback callback) {
        mScanCallback = callback;
        startLeScan();
        mScanState = ScanState.STATE_SCANNING;
    }

    @Override
    public void stopScan() {
        stopLeScan();
        destroy();
        mScanCallback = null;
        mScanState = ScanState.STATE_IDLE;
    }

    private void delayScheduleStartScan() {
        removeHandlerMsg();
        if (mHandler != null) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startLeScan();
                }
            }, getInterval());
        }
    }

    private void delayScheduleStopScan() {
        removeHandlerMsg();
        if (mHandler != null) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopLeScan();
                }
            }, getDuration());
        }
    }

    @Override
    protected void notifyScanStarted() {
        delayScheduleStopScan();
        if (mScannerPresenter != null) {
            mScannerPresenter.notifyScanStarted();
        }
    }

    @Override
    protected void notifyScanStopped() {
        delayScheduleStartScan();
        if (mScannerPresenter != null) {
            mScannerPresenter.notifyScanStopped();
        }
    }
}
