/**
 * 
 */
package com.gopawpaw.core.bluetooth.scanner;

import android.bluetooth.BluetoothDevice;

/**
 * 蓝牙扫描回调接口
 * @author LiJinHua
 * @modify 2014年5月14日 下午2:39:52
 */
public interface ScannerCallback {
	
	void onFoundDevice(BluetoothDevice device,short rssi);
	
	void onScanFinished();
}
