package SDK;

import javax.swing.JOptionPane;

import com.sun.jna.Pointer;

public class UserTools {

	private HCNetSDK hCNetSDK = null;
	private String ipAddress;
	private String userName = "admin";
	private short port = 8000;

	private String passwd;
	private int lUserID = -1; // 用户句柄
	private int lAlarmHandle = -1;// 报警布防句柄
	private int lListenHandle = -1;// 报警监听句柄

	private FMSGCallBack fMSFCallBack = null;// 报警回调函数实现
	private FMSGCallBack_V31 fMSFCallBack_V31 = null;// 报警回调函数实现

	public UserTools(String ipAddress, String userName, String passwd) {
		this.ipAddress = ipAddress;
		this.userName = userName;
		this.passwd = passwd;
	}

	public UserTools(String ipAddress, String userName, String passwd, short port) {
		this.ipAddress = ipAddress;
		this.userName = userName;
		this.passwd = passwd;
		this.port = port;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public String getUserName() {
		return userName;
	}

//	public String getPasswd() {
//		return passwd;
//	}

	public void initTools() {
		// 初始化+注册
		hCNetSDK = HCNetSDK.INSTANCE;
		boolean initSuc = hCNetSDK.NET_DVR_Init();
		if (initSuc != true) {
			System.out.println("初始化失败！错误码：" + getErrorCode());
		} else {
			System.out.println("初始化成功");
//			// 断线重连
//			hCNetSDK.NET_DVR_SetConnectTime(2000, 1);
//			hCNetSDK.NET_DVR_SetReconnect(10000, true);
		}
		userLogin(this.ipAddress, this.userName, this.passwd, this.port);
		System.out.println("============= 华丽的分割线 ==============");
	}

	public int getErrorCode() {
		// 获取错误码
		if (hCNetSDK == null) {
			System.out.println("请先初始化");
			return -999;
		}
		return hCNetSDK.NET_DVR_GetLastError();
	}

	public int getUserID() {
		return lUserID;
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
			System.out.println("注册失败，错误号:");
			return false;
		} else {
			System.out.println("注册成功");
			return true;
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
			default:
				System.out.println("未实现此功能");
			}
		} else {
			System.out.println("请先注册！");
			return false;
		}
		if (result) {
			System.out.println("执行成功！");
		} else {
			System.out.println("执行失败！");
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
				System.out.println("布防失败，错误号:" + getErrorCode());
			} else {
				System.out.println("布防成功");
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
				System.out.println("撤防失败");
			}
		}
	}

	// 注销
	public void LouOut() {
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
	}

	public static void main(String[] args) {
		UserTools hcTool = new UserTools("192.168.1.1", "admin", "1213456");
		hcTool.initTools();

		hcTool.setupAlarmChan();
		// sleep
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
