package SDK;

import com.sun.jna.Pointer;

//文档6.5.1
public class FMSGCallBack_V31 implements HCNetSDK.FMSGCallBack_V31 {
	// 报警信息回调函数
//		lCommand  上传的消息类型，详见表 6.
//		pAlarmer  报警设备信息，详见 NET_DVR_ALARMER
//		pAlarmInfo 报警信息，详见表 6.
//		dwBufLen  报警信息缓存大小
//		pUser    用户数据
	public boolean invoke(int lCommand, HCNetSDK.NET_DVR_ALARMER pAlarmer, Pointer pAlarmInfo, int dwBufLen,
			Pointer pUser) {

		System.out.println(lCommand);
		String sAlarmType = new String();
		sAlarmType = new String("lCommand=") + lCommand;

		// 报警设备信息
		System.out.println("设备序列号" + new String(pAlarmer.sSerialNumber));
		System.out.println("设备名字" + new String(pAlarmer.sDeviceName));
		System.out.println("设备IP地址" + new String(pAlarmer.sDeviceIP));
//		System.out.println("报警主动上传时的socket IP地址" + new String(pAlarmer.sSocketIP));

		// 报警主类型
		int majorAlarmType;
		// 报警次类型
		int minorAlarmType;
		// 报警时间戳
		long timestamp;

		switch (lCommand) {

		case HCNetSDK.COMM_ALARM_ACS:
			System.out.println("门禁主机报警信息");

			HCNetSDK.NET_DVR_ACS_ALARM_INFO strACSInfo = new HCNetSDK.NET_DVR_ACS_ALARM_INFO();
			strACSInfo.write();

			Pointer pACSInfo = strACSInfo.getPointer();
			pACSInfo.write(0, pAlarmInfo.getByteArray(0, strACSInfo.size()), 0, strACSInfo.size());
			strACSInfo.read();

			majorAlarmType = strACSInfo.dwMajor;
			minorAlarmType = strACSInfo.dwMinor;
			timestamp = strACSInfo.struTime.toMillis();

			String CardNumber = new String(strACSInfo.struAcsEventInfo.byCardNo).trim();
			byte CardType = strACSInfo.struAcsEventInfo.byCardType;
			int EmployeeNo = strACSInfo.struAcsEventInfo.dwEmployeeNo; // 工号
			byte ReportChannel = strACSInfo.struAcsEventInfo.byReportChannel; // 上传通道
			byte byType = strACSInfo.struAcsEventInfo.byType;

			byte byTimeType = strACSInfo.byTimeType; // 时间类型

			sAlarmType = sAlarmType + "时间" + timestamp + "：门禁主机报警信息，卡号：" + CardNumber + "，卡类型：" + CardType + "工号"
					+ EmployeeNo + "，报警主类型：" + majorAlarmType + "，报警次类型：" + minorAlarmType + "上传通道" + ReportChannel
					+ "防区类型" + byType;
			System.out.println(sAlarmType);
			break;

		case HCNetSDK.COMM_ID_INFO_ALARM:
			System.out.println("门禁身份证刷卡信息");
			HCNetSDK.NET_DVR_ID_CARD_INFO_ALARM strIDCardInfo = new HCNetSDK.NET_DVR_ID_CARD_INFO_ALARM();
			strIDCardInfo.write();

			Pointer pIDCardInfo = strIDCardInfo.getPointer();
			pIDCardInfo.write(0, pAlarmInfo.getByteArray(0, strIDCardInfo.size()), 0, strIDCardInfo.size());
			strIDCardInfo.read();
			// 身份证信息，更多信息见定义
			String IDCardName = new String(strIDCardInfo.struIDCardCfg.byName).trim();
			String IDCardNumber = new String(strIDCardInfo.struIDCardCfg.byIDNum).trim();
			int IDCardType = strIDCardInfo.byCardType;
			majorAlarmType = strIDCardInfo.dwMajor;
			minorAlarmType = strIDCardInfo.dwMinor;
			// 刷卡时间

			timestamp = strIDCardInfo.struSwipeTime.toMillis();
			sAlarmType = sAlarmType + timestamp + "：门禁身份证刷卡信息，身份证号码：" + IDCardNumber + "，姓名：" + IDCardName + "卡类型"
					+ IDCardType + "，报警主类型：" + majorAlarmType + "，报警次类型：" + minorAlarmType;

			System.out.println(sAlarmType);
			break;

		case HCNetSDK.COMM_PASSNUM_INFO_ALARM:
			System.out.println("门禁通行人数信息");
			break;
		default:
			System.out.println("???");
		}

//		System.out.println(pAlarmInfo);
//		System.out.println(dwBufLen);
//		System.out.println(pUser);

		return true;
	}
}