package com.gopawpaw.core.bluetooth.share;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.gopawpaw.core.bluetooth.opp.BluetoothOppShareInfo;
import com.gopawpaw.core.bluetooth.opp.BluetoothOppTransfer;
import com.gopawpaw.core.bluetooth.opp.BluetoothOppTransfer.BluetoothOppTransferListener;
import com.gopawpaw.core.bluetooth.share.OppObexShareCallback.StatusType;

/**
 * 蓝牙OPP 对象传输分享接口
 * @author LiJinHua
 * @modify 2014年5月13日 上午9:53:57
 */
public interface BluetoothOppObexShare {
	
	void shareFile(BluetoothOppShareInfo shareInfo);
	
	BluetoothOppObexShare connect();
	
	void disconnect();
	
	public static class Factory {
		
		public static BluetoothOppObexShare create(Context context,BluetoothDevice device,OppObexShareCallback callback) {
			if(context == null || device == null){
				return null;
			}
			return  new BluetoothOppObexShareImpl(context,device,callback);
		}
		
		static class BluetoothOppObexShareImpl implements BluetoothOppObexShare,BluetoothOppTransferListener{
			private Context context;
			private BluetoothDevice device;
			private OppObexShareCallback callback;
			private BluetoothOppTransfer btOppT;
			public BluetoothOppObexShareImpl(Context context,
					BluetoothDevice device, OppObexShareCallback callback) {
				super();
				this.context = context;
				this.device = device;
				this.callback = callback;
			}
			
			@Override
			public BluetoothOppObexShare connect() {
				BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
				adapter.cancelDiscovery();
				if(btOppT == null){
					btOppT = new BluetoothOppTransfer(context,device);
					btOppT.setBluetoothOppTransferListener(this);
					btOppT.start();
				}
				return this;
			}
			
			@Override
			public void disconnect() {
				if(btOppT != null){
					btOppT.stop();
					btOppT = null;
				}
			}
			
			@Override
			public void shareFile(BluetoothOppShareInfo shareInfo) {
				if(btOppT == null){
					connect();
				}
				btOppT.addShare(shareInfo);
			}
			
			@Override
			public void onShareTimeout(BluetoothOppShareInfo share) {
				if(callback != null){
					callback.onOppObexShareStatus(StatusType.onShareTimeout, 0, share);
				}
			}

			@Override
			public void onShareFailed(BluetoothOppShareInfo share,
					int failReason) {
				if(callback != null){
					callback.onOppObexShareStatus(StatusType.onShareFailed, failReason, share);
				}
			}

			@Override
			public void onConnect(int state) {
				if(callback != null){
					callback.onOppObexShareStatus(StatusType.onConnect, state, null);
				}
			}

			@Override
			public void onDisconnect(int state) {
				disconnect();
				if(callback != null){
					callback.onOppObexShareStatus(StatusType.onDisconnect, state, null);
				}
			}

			@Override
			public void onTransferStart(BluetoothOppShareInfo share, int size) {
				if(callback != null){
					callback.onOppObexShareStatus(StatusType.onTransferStart, size, share);
				}
			}

			@Override
			public void onTransferProgress(BluetoothOppShareInfo share,
					int progress) {
				if(callback != null){
					callback.onOppObexShareStatus(StatusType.onTransferProgress, progress, share);
				}
			}

			@Override
			public void onShareSuccess(BluetoothOppShareInfo share) {
				if(callback != null){
					callback.onOppObexShareStatus(StatusType.onShareSuccess, 0, share);
				}
			}
		}
	}
}
