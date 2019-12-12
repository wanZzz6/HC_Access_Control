package singleDoorDemo;

import sdk.HCTools;

public class AlarmListen {
	// 布防、监听事件
	public static void main(String[] args) {
		HCTools hcTool = new HCTools("192.168.1.1", "admin", "123456");
		hcTool.initTools();
		// 开启日志
		hcTool.setLogDir(null); //null：默认日志路径
		hcTool.enableLog();

		hcTool.setupAlarmChan();
		// sleep 10s ,监听门禁事件
		try {
			hcTool.openTheDoor(1);
			Thread.currentThread();
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		hcTool.cleanUp();
	}
}
