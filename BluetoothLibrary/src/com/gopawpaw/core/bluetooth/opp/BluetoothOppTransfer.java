/*
 * Copyright (c) 2008-2009, Motorola, Inc.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * - Neither the name of the Motorola, Inc. nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.gopawpaw.core.bluetooth.opp;

import java.io.IOException;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import com.gopawpaw.core.bluetooth.extend.javax.obex.ObexTransport;
import com.gopawpaw.core.bluetooth.util.AndroidSDKTool;

/**
 * This class run an actual Opp transfer session (from connect target device to
 * disconnect)
 */
public class BluetoothOppTransfer{
    private static final String TAG = BluetoothOppTransfer.class.getSimpleName();

    private static final boolean D = Constants.DEBUG;

    private static final boolean V = Constants.VERBOSE;

    private static final int RFCOMM_ERROR = 10;

    private static final int RFCOMM_CONNECTED = 11;

    private static final int SOCKET_ERROR_RETRY = 13;
    
    private static final int ADD_SHARE = 14;

    private static final String SOCKET_LINK_KEY_ERROR = "Invalid exchange";

    private Context mContext;

    private BluetoothAdapter mAdapter;
    
    private BluetoothDevice mDestinationDevice;
    
    private BluetoothOppObexSession mSession;

    private BluetoothOppShareInfo mCurrentShare;

    private ObexTransport mTransport;

    private HandlerThread mHandlerThread;

    private EventHandler mSessionHandler;
    
    private BluetoothOppTransferListener mBluetoothOppTransferListener;
    
    /**
     * An interface for notifying when BluetoothOppTransfer state is changed
     */
    public interface BluetoothOppTransferListener {
    	
    	public void onConnect(int state);
    	
    	public void onDisconnect(int state);
    	
    	public void onTransferStart(BluetoothOppShareInfo share,int size);
    	
    	public void onTransferProgress(BluetoothOppShareInfo share,int progress);
    	
    	public void onShareTimeout(BluetoothOppShareInfo share);
    	
    	public void onShareFailed(BluetoothOppShareInfo share,int failReason);
    	
    	public void onShareSuccess(BluetoothOppShareInfo share);
    }
    
    public BluetoothOppTransfer(Context context,
    		BluetoothDevice destinationDevice, BluetoothOppObexSession session) {
        mContext = context;
        mDestinationDevice = destinationDevice;
        mSession = session;
        mAdapter = BluetoothAdapter.getDefaultAdapter();
    }
    
    public BluetoothOppTransfer(Context context,BluetoothDevice destinationDevice) {
        this(context,destinationDevice, null);
    }
    
    /*
     * Receives events from mConnectThread & mSession back in the main thread.
     */
    private class EventHandler extends Handler {
        public EventHandler(Looper looper) {
            super(looper);
        }
        
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SOCKET_ERROR_RETRY:
                    mConnectThread = new
                        SocketConnectThread((BluetoothDevice)msg.obj, true);
                    mConnectThread.start();
                    break;
                case RFCOMM_ERROR:
                    /*
                    * RFCOMM connect fail is for outbound share only! Mark batch
                    * failed, and all shares in batch failed
                    */
                    if (V) Log.v(TAG, "receive RFCOMM_ERROR msg");
                    mConnectThread = null;
                    markBatchFailed(mCurrentShare,BluetoothOppShareInfo.STATUS_CONNECTION_ERROR);
//                    mBatch.mStatus = Constants.BATCH_STATUS_FAILED;
                    break;
                case RFCOMM_CONNECTED:
                    /*
                    * RFCOMM connected is for outbound share only! Create
                    * BluetoothOppObexClientSession and start it
                    */
                    if (V) Log.v(TAG, "Transfer receive RFCOMM_CONNECTED msg");
                    mConnectThread = null;
                    mTransport = (ObexTransport)msg.obj;
                    startObexSession();
                    break;
                case ADD_SHARE:
                	mCurrentShare = (BluetoothOppShareInfo)msg.obj;
                	mSession.addShare(mCurrentShare);
                	break;
                case BluetoothOppObexSession.MSG_CONNECT_SUCCESS:
                	if (V) Log.v(TAG, " BluetoothOppObexSession.MSG_CONNECT_SUCCESS");
                	if(mBluetoothOppTransferListener != null){
                		mBluetoothOppTransferListener.onConnect(BluetoothOppObexSession.MSG_CONNECT_SUCCESS);
                	}
                	break;
                case BluetoothOppObexSession.MSG_ACCESS_FILE_FAIL:
                	markBatchFailed((BluetoothOppShareInfo)msg.obj, BluetoothOppShareInfo.STATUS_FILE_ERROR);
                    break;
                case BluetoothOppObexSession.MSG_TRANSFER_START:
                    if(mBluetoothOppTransferListener != null){
                		mBluetoothOppTransferListener.onTransferStart((BluetoothOppShareInfo)msg.obj, msg.arg1);
                	}
                    break;
                case BluetoothOppObexSession.MSG_TRANSFER_PROGRESS:
                    if(mBluetoothOppTransferListener != null){
                		mBluetoothOppTransferListener.onTransferProgress((BluetoothOppShareInfo)msg.obj, msg.arg1);
                	}
                    break;
                case BluetoothOppObexSession.MSG_SHARE_COMPLETE:
                    /*
                    * Put next share if available,or finish the transfer.
                    * For outbound session, call session.addShare() to send next file,
                    * or call session.stop().
                    * For inbounds session, do nothing. If there is next file to receive,it
                    * will be notified through onShareAdded()
                    */
                	BluetoothOppShareInfo info = (BluetoothOppShareInfo)msg.obj;
                    if (V) Log.v(TAG, "receive MSG_SHARE_COMPLETE for info " + info.getFilePath());
                    if(mBluetoothOppTransferListener != null){
                		mBluetoothOppTransferListener.onShareSuccess(info);
                	}
                    break;
                case BluetoothOppObexSession.MSG_SESSION_COMPLETE:
                    /*
                    * Handle session completed status Set batch status to
                    * finished
                    */
//                	BluetoothOppShareInfo info1 = (BluetoothOppShareInfo)msg.obj;
                    if (V) Log.v(TAG, "receive MSG_SESSION_COMPLETE for batch ");
                    if(mBluetoothOppTransferListener != null){
                    	mBluetoothOppTransferListener.onDisconnect(0);
                	}
                    break;
                case BluetoothOppObexSession.MSG_SESSION_ERROR:
                    /* Handle the error state of an Obex session */
                    if (V) Log.v(TAG, "receive MSG_SESSION_ERROR for batch ");
                    BluetoothOppShareInfo info2 = (BluetoothOppShareInfo)msg.obj;
                    mSession.stop();
                    if(info2 != null){
                    	markBatchFailed(info2,info2.getStatus());
                    }else{
                    	markBatchFailed();
                    }
                    break;
                case BluetoothOppObexSession.MSG_SHARE_INTERRUPTED:
                    if (V) Log.v(TAG, "receive MSG_SHARE_INTERRUPTED for batch ");
                    BluetoothOppShareInfo info3 = (BluetoothOppShareInfo)msg.obj;
                    try {
                        if (mTransport == null) {
                            Log.v(TAG, "receive MSG_SHARE_INTERRUPTED but mTransport = null");
                        } else {
                            mTransport.close();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "failed to close mTransport");
                    }
                    if (V) Log.v(TAG, "mTransport closed ");
                    if (info3 != null) {
                        markBatchFailed(info3,info3.getStatus());
                    } else {
                        markBatchFailed();
                    }
                    break;

                case BluetoothOppObexSession.MSG_CONNECT_TIMEOUT:
                    if (V) Log.v(TAG, "receive MSG_CONNECT_TIMEOUT for batch ");
                    try {
                        if (mTransport == null) {
                            Log.v(TAG, "receive MSG_SHARE_INTERRUPTED but mTransport = null");
                        } else {
                            mTransport.close();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "failed to close mTransport");
                    }
                    if (V) Log.v(TAG, "mTransport closed ");
                    if(mBluetoothOppTransferListener != null){
                		mBluetoothOppTransferListener.onShareTimeout(mCurrentShare);
                	}
                    break;
            }
        }
    }

    private void markBatchFailed(BluetoothOppShareInfo shareInfo,int failReason) {
        synchronized (this) {
            try {
                wait(1000);
            } catch (InterruptedException e) {
                if (V) Log.v(TAG, "Interrupted waiting for markBatchFailed");
            }
        }
        if(shareInfo == null){
        	shareInfo = mCurrentShare;
        }
        if(mBluetoothOppTransferListener != null){
        	if(shareInfo != null){
            	shareInfo.setStatus(failReason);
            }
    		mBluetoothOppTransferListener.onShareFailed(shareInfo,failReason);
    	}
    }

    private void markBatchFailed() {
        markBatchFailed(mCurrentShare,BluetoothOppShareInfo.STATUS_UNKNOWN_ERROR);
    }

    /*
     * NOTE
     * For outbound transfer
     * 1) Check Bluetooth status
     * 2) Start handler thread
     * 3) new a thread to connect to target device
     * 3.1) Try a few times to do SDP query for target device OPUSH channel
     * 3.2) Try a few seconds to connect to target socket
     * 4) After BluetoothSocket is connected,create an instance of RfcommTransport
     * 5) Create an instance of BluetoothOppClientSession
     * 6) Start the session and process the first share in batch
     * For inbound transfer
     * The transfer already has session and transport setup, just start it
     * 1) Check Bluetooth status
     * 2) Start handler thread
     * 3) Start the session and process the first share in batch
     */
    /**
     * Start the transfer
     */
    public void start() {
        /* check Bluetooth enable status */
        /*
         * normally it's impossible to reach here if BT is disabled. Just check
         * for safety
         */
        if (!mAdapter.isEnabled()) {
            Log.e(TAG, "Can't start transfer when Bluetooth is disabled for ");
            markBatchFailed();
            return;
        }

        if (mHandlerThread == null) {
            if (V) Log.v(TAG, "Create handler thread for batch ");
            mHandlerThread = new HandlerThread("BtOpp Transfer Handler",
                    Process.THREAD_PRIORITY_BACKGROUND);
            mHandlerThread.start();
            mSessionHandler = new EventHandler(mHandlerThread.getLooper());
            startConnectSession();
        }
    }

    /**
     * Stop the transfer
     */
    public void stop() {
        if (V) Log.v(TAG, "stop");
        if (mConnectThread != null) {
            try {
                mConnectThread.interrupt();
                if (V) Log.v(TAG, "waiting for connect thread to terminate");
            } catch (Exception e) {
                if (V) Log.v(TAG, "Interrupted waiting for connect thread to join");
            }
            mConnectThread = null;
        }
        if (mSession != null) {
            if (V) Log.v(TAG, "Stop mSession");
            mSession.stop();
        }
        if (mHandlerThread != null) {
            mHandlerThread.getLooper().quit();
            mHandlerThread = null;
        }
    }

    private void startObexSession() {
        if (V) Log.v(TAG, "Create Client session with transport " + mTransport.toString());
        mSession = new BluetoothOppObexClientSession(mContext, mTransport);
        mSession.start(mSessionHandler, 10/*mBatch.getNumShares()*/);
    }
    
    private void startConnectSession() {
        mConnectThread = new SocketConnectThread(mDestinationDevice,false);
        mConnectThread.start();
    }

    private SocketConnectThread mConnectThread;

    private class SocketConnectThread extends Thread {
        private final BluetoothDevice device;

        private long timestamp;

        private BluetoothSocket btSocket = null;

        private boolean mRetry = false;

        /* create a Rfcomm Socket */
        public SocketConnectThread(BluetoothDevice device, boolean
                retry) {
            super("Socket Connect Thread");
            this.device = device;
            mRetry = retry;
        }

        public void interrupt() {
            if (btSocket != null) {
                try {
                    btSocket.close();
                } catch (IOException e) {
                    Log.v(TAG, "Error when close socket");
                }
            }
        }

        @Override
        public void run() {
            timestamp = System.currentTimeMillis();
            /* Use BluetoothSocket to connect */
            try {
//              UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
            	UUID uuid = UUID.fromString("00001105-0000-1000-8000-00805f9b34fb");
//              UUID uuid = UUID.fromString("00001106-0000-1000-8000-00805F9B34FB");
            	if(AndroidSDKTool.getSDKBuildVersion() >= 10){
            		//2.3.3及以上的版本
            		btSocket = device.createInsecureRfcommSocketToServiceRecord(uuid/*BluetoothUuid.ObexObjectPush.getUuid()*/);
            	}else{
            		//2.3.3以下的版本
            		btSocket = device.createRfcommSocketToServiceRecord(uuid);
            	}
            } catch (IOException e1) {
                Log.e(TAG, "Rfcomm socket create error",e1);
                markConnectionFailed(btSocket);
                return;
            }
            try {
            	if (D) Log.d(TAG, "Rfcomm socket connection attempt took " +
            			(System.currentTimeMillis() - timestamp) + " ms");
                btSocket.connect();
                BluetoothOppRfcommTransport transport;
                transport = new BluetoothOppRfcommTransport(btSocket);

                if (D) Log.d(TAG, "Send transport message " + transport.toString());

                mSessionHandler.obtainMessage(RFCOMM_CONNECTED, transport).sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Rfcomm socket connect exception",e);
                // If the devices were paired before, but unpaired on the
                // remote end, it will return an error for the auth request
                // for the socket connection. Link keys will get exchanged
                // again, but we need to retry. There is no good way to
                // inform this socket asking it to retry apart from a blind
                // delayed retry.
                if (!mRetry && e.getMessage().equals(SOCKET_LINK_KEY_ERROR)) {
                    Message msg = mSessionHandler.obtainMessage(SOCKET_ERROR_RETRY,-1,-1,device);
                    mSessionHandler.sendMessageDelayed(msg, 1500);
                } else {
                    markConnectionFailed(btSocket);
                }
            }
        }

        private void markConnectionFailed(BluetoothSocket s) {
            try {
                s.close();
            } catch (IOException e) {
                if (V) Log.e(TAG, "Error when close socket");
            }
            mSessionHandler.obtainMessage(RFCOMM_ERROR).sendToTarget();
            return;
        }
    };

    public void addShare(BluetoothOppShareInfo info) {
    	if (V) Log.v(TAG, "addShare info:"+info.getStatus());
    	mSessionHandler.obtainMessage(ADD_SHARE, info).sendToTarget();
    }

	public void setBluetoothOppTransferListener(
			BluetoothOppTransferListener mBluetoothOppTransferListener) {
		this.mBluetoothOppTransferListener = mBluetoothOppTransferListener;
	}
}
