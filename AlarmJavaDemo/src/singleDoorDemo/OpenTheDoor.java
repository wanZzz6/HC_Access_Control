package singleDoorDemo;
import sdk.HCTools;

public class OpenTheDoor {
	public static void main(String[] args) {

		HCTools hcTool = new HCTools("192.168.1.1", "admin", "123456");
		// 初始化+注册
		hcTool.initTools();
		// 开启日志
		hcTool.setLogDir(null); //null：默认日志路径
		hcTool.enableLog();
		
		Thread.currentThread();
		// 延迟开门
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// 远程开门
		hcTool.openTheDoor(1);
		// 注销 +释放资源
		hcTool.cleanUp();
	}
}