package singleDoorDemo;

import sdk.HCNetSDK;
import sdk.HCTools;

// 设置门参数
public class SetDoorConfig {
	// 以下步骤都已封装在 HCTools.setDoorConfig() 中，此处只做Demo演示流程
	public static void main(String[] args) {
		HCTools hcTool = new HCTools("192.168.1.1", "admin", "admin777");
		hcTool.initTools();
		// 开启日志
		hcTool.setLogDir(null); //null：默认日志路径
		hcTool.enableLog();
// ==================================================================================
		// 先获取参数结构体，否则构造新的太麻烦
		System.out.println("门原参数");
		HCNetSDK.NET_DVR_DOOR_CFG m_struDoorCfg = hcTool.getDoorConfig();
		if(m_struDoorCfg == null) {
			return ;
		}
		hcTool.printDoorConfig(m_struDoorCfg);

		System.out.println("============= 华丽的分割线 ==============");

		// 门参数设置
		// 门名称
		String doorName = "门18"; 
		System.arraycopy(doorName.getBytes(), 0, m_struDoorCfg.byDoorName, 0, doorName.getBytes().length);
		// 超级密码，也可设置胁迫密码
		String SuperPassword = "555555"; 
		System.arraycopy(SuperPassword.getBytes(), 0, m_struDoorCfg.bySuperPassword, 0, SuperPassword.length());
		// 开门持续时间
		m_struDoorCfg.byOpenDuration = 6; 
		// --------其他参数自行设置--------

		// 要想清除密码、门名称等，设置空数组即可
//		m_struDoorCfg.bySuperPassword = new byte[8]; 
//		m_struDoorCfg.byDoorName = new byte[32];

		// 使生效
		m_struDoorCfg.write();
		// 执行
		hcTool.setDoorConfig(m_struDoorCfg);
		System.out.println("修改后参数");
		hcTool.printDoorConfig(null);
		// 退出
		hcTool.cleanUp();
	}
}
