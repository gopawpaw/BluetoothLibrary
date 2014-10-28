package com.gopawpaw.core.bluetooth.share;

import com.gopawpaw.core.bluetooth.opp.BluetoothOppShareInfo;

/**
 * OPP 对象分享回调接口
 * @author LiJinHua
 * @modify 2014年5月13日 上午10:44:41
 */
public interface OppObexShareCallback {
	
	enum StatusType{
		onConnect,
		onDisconnect,
		onTransferStart,
		onTransferProgress,
		onShareTimeout,
		onShareFailed,
		onShareSuccess
	}
	
	public void onOppObexShareStatus(StatusType type,int value,BluetoothOppShareInfo shareInfo);
}
