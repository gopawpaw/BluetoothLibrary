/**
 * 文件名: AndroidSDKTool.java
 * 版    权：  Copyright  LiJinHua  All Rights Reserved.
 * 描    述: 
 * 创建人: LiJinHua
 * 创建时间:  2014年5月12日
 * 
 * 修改人：LiJinHua
 * 修改时间:2014年5月12日  下午5:09:23
 * 修改内容：[修改内容]
 */
package com.gopawpaw.core.bluetooth.util;

import android.os.Build;

/**
 * @author LiJinHua
 * @modify 2014年5月12日 下午5:09:23
 */
public class AndroidSDKTool {
	
	public static int getSDKBuildVersion(){
		String device = android.os.Build.DEVICE;
		int sdk = Build.VERSION.SDK_INT;
		
		if("enterprise_U950".equals(device) && 15 == sdk){
			//中兴U950手机的4.0.3版本其实是2.2的内核。即SDK=8
			return 8;
		}
		return Build.VERSION.SDK_INT;
	}
}
