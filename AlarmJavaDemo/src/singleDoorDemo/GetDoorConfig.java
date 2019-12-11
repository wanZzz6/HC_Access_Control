package singleDoorDemo;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import sdk.HCNetSDK;
import sdk.HCTools;

// 获取门参数
public class GetDoorConfig {
	// 以下步骤都已封装在 HCTools.printDoorConfig() 中，此处只做Demo演示流程
	public static void main(String[] args) {
		HCTools hcTool = new HCTools("192.168.1.1", "admin", "admin777");
		hcTool.initTools();
		// 门参数结构体
		HCNetSDK.NET_DVR_DOOR_CFG struDoorCfg = new HCNetSDK.NET_DVR_DOOR_CFG();
		// 获取指针
		Pointer pDoorCfg = struDoorCfg.getPointer();
		// 获取门参数
		boolean bRet = hcTool.hCNetSDK.NET_DVR_GetDVRConfig(hcTool.getUserID(), HCNetSDK.NET_DVR_GET_DOOR_CFG, 1,
				pDoorCfg, struDoorCfg.size(), new IntByReference());
		// 读数据
		struDoorCfg.read();
		if (bRet) {
			// 读数据
//			struDoorCfg.read();
			System.out.println("获取门参数：");
			System.out.println("门名称:" + new String(struDoorCfg.byDoorName));
			System.out.print("门磁类型： " + (struDoorCfg.byMagneticType > 0 ? "常开" : "常闭"));
			System.out.println("开门按钮类型：" + (struDoorCfg.byOpenButtonType > 0 ? "常开" : "常闭"));
			System.out.println("开门持续时间： " + struDoorCfg.byOpenDuration);
			System.out.println("是否启用闭门回锁： " + (struDoorCfg.byEnableDoorLock > 0 ? "是" : "否"));
			System.out.println("是否启用首卡常开功能： " + (struDoorCfg.byEnableLeaderCard > 0 ? "是" : "否"));
			System.out
					.println("首卡模式：" + (new String[] { "不启用首卡功能", "首卡常开模式", "首卡授权模式" })[struDoorCfg.byLeaderCardMode]);
			System.out.println("胁迫密码：" + new String(struDoorCfg.byStressPassword));
			System.out.println("超级密码：" + new String(struDoorCfg.bySuperPassword));
			System.out.println("解锁密码：" + new String(struDoorCfg.bySuperPassword));
			System.out.println("是否启用门锁输入检测：" + (struDoorCfg.byLockInputCheck > 0 ? "启用" : "不启用"));
			System.out.println("门锁输入类型：" + (struDoorCfg.byLockInputType > 0 ? "常开" : "常闭"));
			System.out.println("是否启用开门按钮：" + (struDoorCfg.byOpenButton > 0 ? "否" : "是"));
			System.out.println("梯控访客延迟时间：" + struDoorCfg.byLadderControlDelayTime + "分钟");
		} else {
			System.out.println("获取门参数失败，错误码：" + hcTool.getErrorCode());
		}
		hcTool.cleanUp();
	}
}
