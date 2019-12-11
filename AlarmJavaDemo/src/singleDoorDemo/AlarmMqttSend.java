package singleDoorDemo;

import com.tsit.callback.FMSGCallBack_V31;
import com.tsit.mqtt.MyMqttClient;

import sdk.HCTools;

public class AlarmMqttSend {
	// demo 单个门事件监听并通过Mqtt发送到物联网平台
	public static void main(String[] args) {
		// 创建mqtt客户端
		String Host = "tcp://localhost:31883";
		String UserName = "8z7a0K9DmhQWuBq5JIGw";
		String clientId = "tsit";
		MyMqttClient mqttClient = MyMqttClient.getInstance(Host, UserName, clientId);
		// 回调函数
		FMSGCallBack_V31 cb = new FMSGCallBack_V31("门1", "test");

		HCTools hctools = new HCTools("192.168.1.1", "admin", "admin777");
		// 设置回调函数
		hctools.setfMSFCallBack_V31(cb);
		// 初始化
		hctools.initTools();
		// 布防
		hctools.setupAlarmChan();
		// 自定义等待过程，比如等待 10 s，中间进行一次远程开门
		try {
			hctools.openTheDoor(1);
			Thread.currentThread();
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}finally {
			hctools.cleanUp();
		}
	}

}
