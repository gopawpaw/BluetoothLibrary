/**
 * 文件名: BluetoothOppShareInfo.java
 * 版    权：  Copyright  LiJinHua  All Rights Reserved.
 * 描    述: 
 * 创建人: LiJinHua
 * 创建时间:  2014年5月14日
 * 
 * 修改人：LiJinHua
 * 修改时间:2014年5月14日  上午9:59:51
 * 修改内容：[修改内容]
 */
package com.gopawpaw.core.bluetooth.opp;

/**
 * 蓝牙OPP分享信息对象
 * @author LiJinHua
 * @modify 2014年5月14日 上午9:59:51
 */
public class BluetoothOppShareInfo {

	private String filePath;
	
	private String name;
    
	private String destination;
	
	private String mimetype;
    
	private int status;
	
	public BluetoothOppShareInfo(String filePath, String name, String mimetype,
			int status) {
		super();
		this.filePath = filePath;
		this.name = name;
		this.mimetype = mimetype;
		this.status = status;
	}
	
	public BluetoothOppShareInfo(String filePath, String name, String mimetype) {
		this(filePath,name,mimetype,STATUS_PENDING);
	}
	
	public BluetoothOppShareInfo(String filePath, String name) {
		this(filePath,name,"*/*",STATUS_PENDING);
	}
	
	public BluetoothOppShareInfo(String filePath) {
		this(filePath,null,"*/*",STATUS_PENDING);
	}
	
	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getMimetype() {
		return mimetype;
	}

	public void setMimetype(String mimetype) {
		this.mimetype = mimetype;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	/**
     * Returns whether the status is informational (i.e. 1xx).
     */
    public static boolean isStatusInformational(int status) {
        return (status >= 100 && status < 200);
    }

    /**
     * Returns whether the transfer is suspended. (i.e. whether the transfer
     * won't complete without some action from outside the transfer manager).
     */
    public static boolean isStatusSuspended(int status) {
        return (status == STATUS_PENDING);
    }

    /**
     * Returns whether the status is a success (i.e. 2xx).
     */
    public static boolean isStatusSuccess(int status) {
        return (status >= 200 && status < 300);
    }

    /**
     * Returns whether the status is an error (i.e. 4xx or 5xx).
     */
    public static boolean isStatusError(int status) {
        return (status >= 400 && status < 600);
    }

    /**
     * Returns whether the status is a client error (i.e. 4xx).
     */
    public static boolean isStatusClientError(int status) {
        return (status >= 400 && status < 500);
    }

    /**
     * Returns whether the status is a server error (i.e. 5xx).
     */
    public static boolean isStatusServerError(int status) {
        return (status >= 500 && status < 600);
    }

    /**
     * Returns whether the transfer has completed (either with success or
     * error).
     */
    public static boolean isStatusCompleted(int status) {
        return (status >= 200 && status < 300) || (status >= 400 && status < 600);
    }

    /**
     * This transfer hasn't stated yet
     */
    public static final int STATUS_PENDING = 190;

    /**
     * This transfer has started
     */
    public static final int STATUS_RUNNING = 192;

    /**
     * This transfer has successfully completed. Warning: there might be other
     * status values that indicate success in the future. Use isSucccess() to
     * capture the entire category.
     */
    public static final int STATUS_SUCCESS = 200;

    /**
     * This request couldn't be parsed. This is also used when processing
     * requests with unknown/unsupported URI schemes.
     */
    public static final int STATUS_BAD_REQUEST = 400;

    /**
     * This transfer is forbidden by target device.
     */
    public static final int STATUS_FORBIDDEN = 403;

    /**
     * This transfer can't be performed because the content cannot be handled.
     */
    public static final int STATUS_NOT_ACCEPTABLE = 406;

    /**
     * This transfer cannot be performed because the length cannot be determined
     * accurately. This is the code for the HTTP error "Length Required", which
     * is typically used when making requests that require a content length but
     * don't have one, and it is also used in the client when a response is
     * received whose length cannot be determined accurately (therefore making
     * it impossible to know when a transfer completes).
     */
    public static final int STATUS_LENGTH_REQUIRED = 411;

    /**
     * This transfer was interrupted and cannot be resumed. This is the code for
     * the OBEX error "Precondition Failed", and it is also used in situations
     * where the client doesn't have an ETag at all.
     */
    public static final int STATUS_PRECONDITION_FAILED = 412;

    /**
     * This transfer was canceled
     */
    public static final int STATUS_CANCELED = 490;

    /**
     * This transfer has completed with an error. Warning: there will be other
     * status values that indicate errors in the future. Use isStatusError() to
     * capture the entire category.
     */
    public static final int STATUS_UNKNOWN_ERROR = 491;

    /**
     * This transfer couldn't be completed because of a storage issue.
     * Typically, that's because the file system is missing or full.
     */
    public static final int STATUS_FILE_ERROR = 492;

    /**
     * This transfer couldn't be completed because of no sdcard.
     */
    public static final int STATUS_ERROR_NO_SDCARD = 493;

    /**
     * This transfer couldn't be completed because of sdcard full.
     */
    public static final int STATUS_ERROR_SDCARD_FULL = 494;

    /**
     * This transfer couldn't be completed because of an unspecified un-handled
     * OBEX code.
     */
    public static final int STATUS_UNHANDLED_OBEX_CODE = 495;

    /**
     * This transfer couldn't be completed because of an error receiving or
     * processing data at the OBEX level.
     */
    public static final int STATUS_OBEX_DATA_ERROR = 496;

    /**
     * This transfer couldn't be completed because of an error when establishing
     * connection.
     */
    public static final int STATUS_CONNECTION_ERROR = 497;
    
    /**
     * This transfer try again
     * connection.
     */
    public static final int STATUS_TRY_AGAIN = 499;
}
