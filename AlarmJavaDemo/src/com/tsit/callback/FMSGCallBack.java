package com.tsit.callback;
import com.sun.jna.Pointer;

import sdk.HCNetSDK;

public class FMSGCallBack implements HCNetSDK.FMSGCallBack {
		// 报警信息回调函数

		public void invoke(int lCommand, HCNetSDK.NET_DVR_ALARMER pAlarmer, Pointer pAlarmInfo, int dwBufLen,
				Pointer pUser) {
		}
	}