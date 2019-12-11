package com.tsit.callback;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.jna.Pointer;
import com.tsit.mqtt.MyMqttClient;

import sdk.HCNetSDK;


//文档6.5.1
public class FMSGCallBack_V31 implements HCNetSDK.FMSGCallBack_V31 {

	// 门别名
	public String doorName = "";
	// 对应的thingsboard物联网平台设备名
	public String tbDeviceName = "";

	private static final String GATEWAY_TELEMETRY_TOPIC = "v1/gateway/telemetry";
	// DCL单例模式
	private static MyMqttClient mqttClient = MyMqttClient.getInstance();

	public String getDoorName() {
		return doorName;
	}

	public void setDoorName(String doorName) {
		this.doorName = doorName;
	}

	public String getTbDeviceName() {
		return tbDeviceName;
	}

	public void setTbDeviceName(String tbDeviceName) {
		this.tbDeviceName = tbDeviceName;
	}

	public FMSGCallBack_V31() {
	}

	public FMSGCallBack_V31(String doorName) {
		this.doorName = doorName;
	}

	public FMSGCallBack_V31(String doorName, String tbDeviceName) {
		this.doorName = doorName;
		this.tbDeviceName = tbDeviceName;
	}

	// 报警信息回调函数
//		lCommand  上传的消息类型，详见表 6.6
//		pAlarmer  报警设备信息，详见 NET_DVR_ALARMER
//		pAlarmInfo 报警信息，详见表 6.7
//		dwBufLen  报警信息缓存大小
//		pUser    用户数据
//	第一个参数（lCommand）和第三个参数（pAlarmInfo）是密切关联的，其
//	关系如表 6.7 .所示。
	public boolean invoke(int lCommand, HCNetSDK.NET_DVR_ALARMER pAlarmer, Pointer pAlarmInfo, int dwBufLen,
			Pointer pUser) {

		String sAlarmType = new String();
		sAlarmType = new String("lCommand=") + lCommand;

		// 报警设备信息
//		System.out.println("设备序列号" + new String(pAlarmer.sSerialNumber));
//		System.out.println("设备名字" + new String(pAlarmer.sDeviceName));
//		System.out.println("设备IP地址" + new String(pAlarmer.sDeviceIP));

//		System.out.println("报警主动上传时的socket IP地址" + new String(pAlarmer.sSocketIP));
//		System.out.println(pAlarmInfo);
//		System.out.println(dwBufLen);
//		System.out.println(pUser);

		// 报警主类型
		int majorAlarmType;
		// 报警次类型
		int minorAlarmType;
		// 报警时间戳
		long timestamp;
		// mqtt 消息
		JsonObject mqttvalue = new JsonObject();
		mqttvalue.addProperty("AlarmNum", lCommand);
		mqttvalue.addProperty("ip", new String(pAlarmer.sDeviceIP).trim());

		switch (lCommand) {
		// 门禁主机报警信息
		case HCNetSDK.COMM_ALARM_ACS:

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

			mqttvalue.addProperty("majorAlarmType", majorAlarmType);
			mqttvalue.addProperty("minorAlarmType", minorAlarmType);
			mqttvalue.addProperty("timestamp", timestamp);
			mqttvalue.addProperty("CardNumber", CardNumber);
			mqttvalue.addProperty("CardType", CardType);
			mqttvalue.addProperty("EmployeeNo", EmployeeNo);
			mqttvalue.addProperty("ReportChannel", ReportChannel);
			mqttvalue.addProperty("byType", byType);
			mqttvalue.addProperty("byTimeType", byTimeType);

			sAlarmType = "门禁主机报警信息: " + sAlarmType + "，时间：" + timestamp / 1000 + "，卡号：" + CardNumber + "，卡类型："
					+ CardType + "，工号：" + EmployeeNo + "，报警主类型：" + majorAlarmType + "，报警次类型：" + minorAlarmType
					+ "，上传通道：" + ReportChannel + "，防区类型：" + byType;
			System.out.println(sAlarmType);
			// 发送mqtt消息， QoS=0
			sendMqtt(mqttvalue);

			break;
		// 门禁身份证刷卡信息
		case HCNetSDK.COMM_ID_INFO_ALARM:
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

			mqttvalue.addProperty("IDCardName", IDCardName);
			mqttvalue.addProperty("IDCardNumber", IDCardNumber);
			mqttvalue.addProperty("IDCardType", IDCardType);
			mqttvalue.addProperty("majorAlarmType", majorAlarmType);
			mqttvalue.addProperty("minorAlarmType", minorAlarmType);
			mqttvalue.addProperty("timestamp", timestamp);

			sAlarmType = "门禁身份证刷卡信息:" + sAlarmType + "，时间：" + timestamp + "，身份证号码：" + IDCardNumber + "，姓名：" + IDCardName
					+ "，卡类型：" + IDCardType + "，报警主类型：" + majorAlarmType + "，报警次类型：" + minorAlarmType;

			System.out.println(sAlarmType);

			sendMqtt(mqttvalue);
			break;

		case HCNetSDK.COMM_PASSNUM_INFO_ALARM:
			System.out.println("门禁通行人数信息");
			break;
		default:
			System.out.println("???");
		}
		return true;
	}

	public void sendMqtt(JsonObject mqttValue) {
		if (null != mqttValue && !mqttValue.isJsonNull()) {
			// 构造mqtt数据包
			String ts = String.valueOf(System.currentTimeMillis());
			JsonObject mqJson = new JsonObject();
			JsonObject innerJson = new JsonObject();
			JsonArray ja = new JsonArray();

			innerJson.addProperty("ts", ts);

			// 如果有tb名则当作tb上的设备名（一设备挂载多门），否则将门的别名当作tb设备名（一设备一门）
			if (tbDeviceName != null && tbDeviceName != "") {
				mqttValue.addProperty("doorName", doorName);
				innerJson.add("values", mqttValue);
				ja.add(innerJson);
				mqJson.add(tbDeviceName, ja);
				mqttClient.publishMessage(GATEWAY_TELEMETRY_TOPIC, mqJson.toString(), 0);
			} else if (doorName != null && doorName != "") {
				innerJson.add("values", mqttValue);
				ja.add(innerJson);
				mqJson.add(doorName, ja);
				mqttClient.publishMessage(GATEWAY_TELEMETRY_TOPIC, mqJson.toString(), 0);
			} else {
				System.out.println("未设置设备名");
			}
		}
	}
}