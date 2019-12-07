import SDK.UserTools;

public class OpenTheDoor {
	public static void main(String[] args) {

		UserTools hcTool = new UserTools("192.168.1.1", "admin", "123");
		// 初始化+注册
		hcTool.initTools();
		// 远程开门
		hcTool.openTheDoor(1);
		// 注销 +释放资源
		hcTool.cleanUp();
	}
}