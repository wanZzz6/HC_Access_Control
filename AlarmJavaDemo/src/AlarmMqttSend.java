import java.util.ArrayList;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tsit.callback.FMSGCallBack_V31;
import com.tsit.mqtt.MyMqttClient;
import com.tsit.utils.JsonUtils;

import sdk.HCTools;

public class AlarmMqttSend {

	// 设备信息 json
	private JsonArray deviceNameMap;
	String clientId = "tsit";
	String Host = "tcp://11.11.11.11:31883";
	String UserName = "8z7a0K9DmhQWuBq5JIGw";
	// 测试地址

	// DCL单例模式，mqtt客户端实例，每个门禁的回调函数中共用发送数据
	public MyMqttClient mqttClient;

	// 加载配置文件
	private void loadDeviceName() {
		// 从文件加载
		this.deviceNameMap = JsonUtils.readJsonArray(
				"C:\\Users\\13438\\Desktop\\git_clone\\HC_Access_Control\\AlarmJavaDemo\\src\\config.json");
		if (null == this.deviceNameMap) {
			System.out.println("加载门禁设备信息失败");
		}
	}

	// 监听配置信息里的所有门禁设备并通过Mqtt发送到物联网平台
	public void lunchAlarmListen() {
		mqttClient = MyMqttClient.getInstance(Host, UserName, clientId);
		loadDeviceName();
		ArrayList<HCTools> alldeviceArray = new ArrayList<HCTools>();

		for (JsonElement jsonElement : deviceNameMap) {
			String area = jsonElement.getAsJsonObject().get("area").getAsString().toString();
			JsonArray doorList = (JsonArray) jsonElement.getAsJsonObject().get("device");
			for (JsonElement doorInfoElement : doorList) {
				JsonObject doorInfoObject = doorInfoElement.getAsJsonObject();

				FMSGCallBack_V31 cb = new FMSGCallBack_V31(doorInfoObject.get("name").getAsString().toString(), area);
				HCTools hctools = new HCTools(doorInfoObject.get("ipAddress").getAsString().toString(),
						doorInfoObject.get("userName").getAsString().toString(),
						doorInfoObject.get("passwd").getAsString().toString());

				// 初始化
				hctools.initTools();
				// 设置回调函数
				hctools.setfMSFCallBack_V31(cb);
				// 布防
				hctools.setupAlarmChan();
				alldeviceArray.add(hctools);
			}
		}
		try {
//			hctools.openTheDoor(1);
			Thread.currentThread();
			Thread.sleep(20000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			for (HCTools hcTools2 : alldeviceArray) {
				hcTools2.cleanUp();
			}

		}

	}

	public static void main(String[] args) {
		AlarmMqttSend as = new AlarmMqttSend();
		as.lunchAlarmListen();

	}

}
