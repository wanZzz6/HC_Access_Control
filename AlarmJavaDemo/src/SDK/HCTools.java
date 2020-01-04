package sdk;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.tsit.callback.FMSGCallBack;
import com.tsit.callback.FMSGCallBack_V31;

public class HCTools {

	public HCNetSDK hCNetSDK = null;
	private String ipAddress;
	private String userName;

	private short port = 8000;

//	日志的等级（默认为 0）：
//	0-表示关闭日志，
//	1-表示只输出 ERROR 错误日志，
//	2-输出 ERROR 错误信息和 DEBUG 调试信息，
//	3-输出 ERROR 错误信息、DEBUG 调试信息和 INFO 普通信息等所
//	有信息
	private int logLevel = 3;
	private String logDir; // 日志文件夹， windows 默认值为"C:\\SdkLog\\"；linux 默认值"/home/sdklog/"

	private String passwd;
	private int lUserID = -1; // 用户句柄
	private int lAlarmHandle = -1;// 报警布防句柄
	private int lListenHandle = -1;// 报警监听句柄

	private FMSGCallBack fMSFCallBack = null;// 报警回调函数实现
	private FMSGCallBack_V31 fMSFCallBack_V31 = null;// 报警回调函数实现

	public void setfMSFCallBack(FMSGCallBack fMSFCallBack) {
		this.fMSFCallBack = fMSFCallBack;
	}

	public void setfMSFCallBack_V31(FMSGCallBack_V31 fMSFCallBack_V31) {
		this.fMSFCallBack_V31 = fMSFCallBack_V31;
	}

	public HCTools(String ipAddress, String userName, String passwd) {
		this.ipAddress = ipAddress;
		this.userName = userName;
		this.passwd = passwd;
	}

	public HCTools(String ipAddress, String userName, String passwd, short port) {
		this.ipAddress = ipAddress;
		this.userName = userName;
		this.passwd = passwd;
		this.port = port;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public short getPort() {
		return port;
	}

	public void setPort(short port) {
		this.port = port;
	}

	public String getPasswd() {
		return passwd;
	}

	public void setPasswd(String passwd) {
		this.passwd = passwd;
	}

	public int getUserID() {
		return lUserID;
	}

	public int getLogLevel() {
		return logLevel;
	}

	public void setLogLevel(int logLevel) {
		this.logLevel = logLevel;
	}

	public String getLogDir() {
		return logDir;
	}

	public void setLogDir(String logDir) {
		this.logDir = logDir;
	}

	// 获取错误码
	public int getErrorCode() {
		if (hCNetSDK == null) {
			System.err.println("请先初始化");
			return -999;
		}
		return hCNetSDK.NET_DVR_GetLastError();
	}

	// 获取错误信息
	public String getErrorMsg() {
		return hCNetSDK.NET_DVR_GetErrorMsg(new IntByReference(getErrorCode()));
	}

	public void printErrorInfo() {
		System.err.println("错误码：" + getErrorCode() + "，错误信息：" + getErrorMsg());
	}

	// 初始化+注册
	public void initTools() {
		hCNetSDK = HCNetSDK.INSTANCE;
		boolean initSuc = hCNetSDK.NET_DVR_Init();
		if (initSuc != true) {
			System.err.println("初始化失败！");
			printErrorInfo();
		} else {
			System.out.println("初始化成功");
//			//设置连接超时时间与重连功能
//			hCNetSDK.NET_DVR_SetConnectTime(2000, 1);
//			hCNetSDK.NET_DVR_SetReconnect(10000, true);
			userLogin(this.ipAddress, this.userName, this.passwd, this.port);
		}
		System.out.println("============= 华丽的分割线 ====  ==========");
	}

	// 默认端口8000
	private boolean userLogin(String ip, String userName, String passwd) {
		return userLogin(ip, userName, passwd, (short) 8000);
	}

	// 注册设备
	private boolean userLogin(String sDeviceIP, String sUsername, String sPassword, short port) {

		if (hCNetSDK == null) {
			initTools();
		}
		if (lUserID > -1) {
			// 先注销
			hCNetSDK.NET_DVR_Logout(lUserID);
			lUserID = -1;
		}
		HCNetSDK.NET_DVR_USER_LOGIN_INFO m_strLoginInfo = new HCNetSDK.NET_DVR_USER_LOGIN_INFO();// 设备登录信息
		HCNetSDK.NET_DVR_DEVICEINFO_V40 m_strDeviceInfo = new HCNetSDK.NET_DVR_DEVICEINFO_V40();// 设备信息

		m_strLoginInfo.sDeviceAddress = new byte[HCNetSDK.NET_DVR_DEV_ADDRESS_MAX_LEN];
		System.arraycopy(sDeviceIP.getBytes(), 0, m_strLoginInfo.sDeviceAddress, 0, sDeviceIP.length());

		m_strLoginInfo.sUserName = new byte[HCNetSDK.NET_DVR_LOGIN_USERNAME_MAX_LEN];
		System.arraycopy(sUsername.getBytes(), 0, m_strLoginInfo.sUserName, 0, sUsername.length());

		m_strLoginInfo.sPassword = new byte[HCNetSDK.NET_DVR_LOGIN_PASSWD_MAX_LEN];
		System.arraycopy(sPassword.getBytes(), 0, m_strLoginInfo.sPassword, 0, sPassword.length());

		m_strLoginInfo.wPort = port;

		m_strLoginInfo.bUseAsynLogin = false; // 是否异步登录：0- 否，1- 是
		m_strLoginInfo.write();

		lUserID = hCNetSDK.NET_DVR_Login_V40(m_strLoginInfo, m_strDeviceInfo);
		// -1: fail 0: success
		if (lUserID == -1) {
			System.err.println("注册失败");
			printErrorInfo();
			return false;
		} else {
			System.out.println("注册成功");
			return true;
		}
	}

	// 请求并返回门参数结构体
	public HCNetSDK.NET_DVR_DOOR_CFG getDoorConfig() {
		if (lUserID == -1) {
			System.err.println("请先注册");
		}
		HCNetSDK.NET_DVR_DOOR_CFG struDoorCfg = new HCNetSDK.NET_DVR_DOOR_CFG();
		struDoorCfg.write();
		// 获取指针
		Pointer pDoorCfg = struDoorCfg.getPointer();
		// 获取门参数：2108
		boolean bRet = hCNetSDK.NET_DVR_GetDVRConfig(lUserID, HCNetSDK.NET_DVR_GET_DOOR_CFG, 1, pDoorCfg,
				struDoorCfg.size(), new IntByReference());

		if (!bRet) {
			System.err.println("获取门参数失败");
			printErrorInfo();
			return null;
		}
		// 读数据
		struDoorCfg.read();
		return struDoorCfg;
	}

		// 输出门参数结构体的内容，如果传入null，先获取当前门参数信息
	public void printDoorConfig(HCNetSDK.NET_DVR_DOOR_CFG doorCfg) {
		if (lUserID == -1) {
			System.err.println("请先注册");
			return;
		}
		if (doorCfg == null) {
			doorCfg = getDoorConfig();
		}

		System.out.println("获取门参数：");
		System.out.println("门名称:" + new String(doorCfg.byDoorName));
		System.out.print("门磁类型： " + (doorCfg.byMagneticType > 0 ? "常开" : "常闭"));
		System.out.println("开门按钮类型：" + (doorCfg.byOpenButtonType > 0 ? "常开" : "常闭"));
		System.out.println("开门持续时间： " + doorCfg.byOpenDuration);
		System.out.println("是否启用闭门回锁： " + (doorCfg.byEnableDoorLock > 0 ? "是" : "否"));
		System.out.println("是否启用首卡常开功能： " + (doorCfg.byEnableLeaderCard > 0 ? "是" : "否"));
		System.out
				.println("首卡模式：" + (new String[] { "不启用首卡功能", "首卡常开模式", "首卡授权模式" })[doorCfg.byLeaderCardMode]);
		System.out.println("胁迫密码：" + new String(doorCfg.byStressPassword));
		System.out.println("超级密码：" + new String(doorCfg.bySuperPassword));
		System.out.println("解锁密码：" + new String(doorCfg.bySuperPassword));
		System.out.println("是否启用门锁输入检测：" + (doorCfg.byLockInputCheck > 0 ? "启用" : "不启用"));
		System.out.println("门锁输入类型：" + (doorCfg.byLockInputType > 0 ? "常开" : "常闭"));
		System.out.println("是否启用开门按钮：" + (doorCfg.byOpenButton > 0 ? "否" : "是"));
		System.out.println("梯控访客延迟时间：" + doorCfg.byLadderControlDelayTime + "分钟");
	}

	// 设置门参数，NET_DVR_DOOR_CFG 参数设置完记得 write()
	public void setDoorConfig(HCNetSDK.NET_DVR_DOOR_CFG m_struDoorCfg) {
		boolean bRet = false;
		bRet = hCNetSDK.NET_DVR_SetDVRConfig(lUserID, HCNetSDK.NET_DVR_SET_DOOR_CFG, 1, m_struDoorCfg.getPointer(),
				m_struDoorCfg.size());

		if (bRet) {
			System.out.println("修改成功");
		} else {
			System.err.println("修改参数失败");
			printErrorInfo();
		}

	}

	// 改变门禁状态
	public boolean changeDoorStatus(int doorIndex, int status) {
		// status：0-close，1-open，2-always open，3-always close

		boolean result = false;
		if (lUserID != -1) {
			switch (status) {
			case 1:
				result = openTheDoor(doorIndex);
				break;
			case 3:
				result = closeTheDoorForrever(doorIndex);
				break;
			case 2:
				result = openTheDoorForrever(doorIndex);
				break;
			default:
				System.err.println("未实现此功能");
			}
		} else {
			System.out.println("请先注册！");
			return false;
		}
		if (result) {
			System.out.println("执行成功！");
		} else {
			System.err.println("执行失败！");
			printErrorInfo();
		}
		return result;
	}

	public boolean openTheDoor(int doorIndex) {
		System.out.println("远程开门");
		return hCNetSDK.NET_DVR_ControlGateway(lUserID, doorIndex, 1);
	}

	public boolean closeTheDoorForrever(int doorIndex) {
		System.out.println("控制门常闭");
		return hCNetSDK.NET_DVR_ControlGateway(lUserID, doorIndex, 3);
	}

	public boolean openTheDoorForrever(int doorIndex) {
		System.out.println("控制门常开");
		return hCNetSDK.NET_DVR_ControlGateway(lUserID, doorIndex, 2);
	}

	// 设置布防，二级、实时布防
	public void setupAlarmChan() {
		if (lUserID == -1) {
			System.err.println("请先注册");
			return;
		}
		if (lAlarmHandle < 0)// 尚未布防,需要布防
		{
			if (fMSFCallBack_V31 == null) {
				fMSFCallBack_V31 = new FMSGCallBack_V31();
			}
			// 设置报警回调函数，刷卡等事件都会触发报警回调函数
			Pointer pUser = null;
			if (!hCNetSDK.NET_DVR_SetDVRMessageCallBack_V31(fMSFCallBack_V31, pUser)) {
				System.out.println("设置回调函数失败!");
				printErrorInfo();
				return;
			}
			// 设置报警布防参数，详见定义
			HCNetSDK.NET_DVR_SETUPALARM_PARAM m_strAlarmInfo = new HCNetSDK.NET_DVR_SETUPALARM_PARAM();
			m_strAlarmInfo.dwSize = m_strAlarmInfo.size();
			m_strAlarmInfo.byLevel = 1;// 布防优先级：0- 一等级（高），1- 二等级（中），2- 三等级（低）
			m_strAlarmInfo.byAlarmInfoType = 1;// 报警信息上传类型：0- 老报警信息（NET_DVR_PLATE_RESULT），1- 新报警信息(NET_ITS_PLATE_RESULT)
			m_strAlarmInfo.byDeployType = 1; // 布防类型(仅针对门禁主机、人证设备)：0-客户端布防(会断网续传)，1-实时布防(只上传实时数据)
			m_strAlarmInfo.write();
			// 报警布防
			lAlarmHandle = hCNetSDK.NET_DVR_SetupAlarmChan_V41(lUserID, m_strAlarmInfo);
			if (lAlarmHandle == -1) {
				System.err.println(ipAddress + " 布防失败");
				printErrorInfo();
			} else {
				System.out.println(ipAddress + " 布防成功");
			}
		} else {
			System.out.println("已经布防");
		}
	}

	// 撤防
	public void CloseAlarmChan() {

		if (lAlarmHandle > -1) {
			if (hCNetSDK.NET_DVR_CloseAlarmChan_V30(lAlarmHandle)) {
				System.out.println("撤防成功");
				lAlarmHandle = -1;
			} else {
				System.err.println("撤防失败");
				printErrorInfo();
			}
		}
	}

	// 启用SDK日志功能
	// SDK 限制个数默认为 10 个，可以调用接口 NET_DVR_SetSDKLocalCfg
	public void enableLog() {
		boolean ret = hCNetSDK.NET_DVR_SetLogToFile(logLevel, logDir, true);
		if (ret) {
			System.out.println("日志目录：" + (logDir == null ? "默认" : logDir));
		} else {
			System.err.println("开启日志失败");
			printErrorInfo();
		}
	}

	/*
	 * SDK 的版本号和 build 信息。2 个高字节表示版本号 ：25~32 位表示主版本号，17~24 位表示次 版本号；2 个低字节表示 build
	 * 信息。如 0x03000101：表示版本号为 3.0，build 号是 0101。
	 */
	public String getSDKBuildVersion() {
		String version = "0x" + Integer.toHexString(hCNetSDK.NET_DVR_GetSDKBuildVersion());
		return version;
	}

	// 注销
	private void LouOut() {
		if (lUserID > -1) {
			// 注销
			hCNetSDK.NET_DVR_Logout(lUserID);
			lUserID = -1;
		}
	}

	// 撤防 + 注销 + 释放SDK资源
	public void cleanUp() {
		CloseAlarmChan();
		LouOut();
		hCNetSDK.NET_DVR_Cleanup();
		System.out.println("已释放资源");
	}

	public static void main(String[] args) {
		// Demo
		HCTools hcTool = new HCTools("192.168.1.1", "admin", "123456");
		hcTool.initTools();

		hcTool.setupAlarmChan();
		// 监听 10 s
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
