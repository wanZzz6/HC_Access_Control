import SDK.UserTools;

public class AlarmListen {
	// 布防、监听事件
	public static void main(String[] args) {
		UserTools hcTool = new UserTools("192.168.1.1", "admin", "123456");
		hcTool.initTools();

		hcTool.setupAlarmChan();
		// sleep 10s ,监听门禁事件，
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
