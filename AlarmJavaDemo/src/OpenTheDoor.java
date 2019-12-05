import SDK.HCNetSDK;

public class OpenTheDoor {
	public static void main(String[] args) {
		HCNetSDK hCNetSDK = HCNetSDK.INSTANCE;
		HCNetSDK.NET_DVR_USER_LOGIN_INFO m_strLoginInfo = new HCNetSDK.NET_DVR_USER_LOGIN_INFO();// 设备登录信息
		HCNetSDK.NET_DVR_DEVICEINFO_V40 m_strDeviceInfo = new HCNetSDK.NET_DVR_DEVICEINFO_V40();// 设备信息
		String m_sDeviceIP = "192.168.1.101";// 已登录设备的IP地址
		String m_sUsername = "admin";// 设备用户名
		String m_sPassword = "123456";// 设备密码
		short port = 8000;

		int lUserID = -1;// 用户句柄

		boolean initSuc = hCNetSDK.NET_DVR_Init();
		if (initSuc != true) {
			System.out.println("初始化失败！");
		}

		m_strLoginInfo.sDeviceAddress = new byte[HCNetSDK.NET_DVR_DEV_ADDRESS_MAX_LEN];
		System.arraycopy(m_sDeviceIP.getBytes(), 0, m_strLoginInfo.sDeviceAddress, 0, m_sDeviceIP.length());

		m_strLoginInfo.sUserName = new byte[HCNetSDK.NET_DVR_LOGIN_USERNAME_MAX_LEN];
		System.arraycopy(m_sUsername.getBytes(), 0, m_strLoginInfo.sUserName, 0, m_sUsername.length());

		m_strLoginInfo.sPassword = new byte[HCNetSDK.NET_DVR_LOGIN_PASSWD_MAX_LEN];
		System.arraycopy(m_sPassword.getBytes(), 0, m_strLoginInfo.sPassword, 0, m_sPassword.length());

		m_strLoginInfo.wPort = port;

		m_strLoginInfo.bUseAsynLogin = false; // 是否异步登录：0- 否，1- 是
		m_strLoginInfo.write();

		lUserID = hCNetSDK.NET_DVR_Login_V40(m_strLoginInfo, m_strDeviceInfo);

		if (lUserID == -1) {
			System.out.println("注册失败，错误号:" + hCNetSDK.NET_DVR_GetLastError());
		} else {
			System.out.println("注册成功" + lUserID);
			Boolean bRet;
			int gatewayIndex = 1;
			int dwStaic = 1;// 命令值：0-关闭，1-打开，2-常开，3-常关

			bRet = hCNetSDK.NET_DVR_ControlGateway(lUserID, gatewayIndex, dwStaic);
			if (!bRet) {
				System.out.println("NET_DVR_ControlGateway failed: " + hCNetSDK.NET_DVR_GetLastError());
			} else {
				System.out.println("opening");
			}
		}

		if (lUserID > -1) {
			// 娉ㄩ攢
			hCNetSDK.NET_DVR_Logout(lUserID);
			lUserID = -1;
		}
		hCNetSDK.NET_DVR_Cleanup();
	}

}
