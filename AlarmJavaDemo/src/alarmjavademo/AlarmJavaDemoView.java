package alarmjavademo;

import com.sun.jna.Pointer;
import javax.swing.JOptionPane;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import java.text.DateFormat;
import java.util.Date;
import java.text.SimpleDateFormat;
import javax.swing.table.DefaultTableModel;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import SDK.HCNetSDK;
/**
 * The application's main frame.
 */
public class AlarmJavaDemoView extends FrameView {
    static HCNetSDK hCNetSDK = HCNetSDK.INSTANCE;
    HCNetSDK.NET_DVR_USER_LOGIN_INFO m_strLoginInfo = new HCNetSDK.NET_DVR_USER_LOGIN_INFO();//设备登录信息
    HCNetSDK.NET_DVR_DEVICEINFO_V40 m_strDeviceInfo = new HCNetSDK.NET_DVR_DEVICEINFO_V40();//设备信息
    String m_sDeviceIP;//已登录设备的IP地址
    String m_sUsername;//设备用户名
    String m_sPassword;//设备密码

    int lUserID;//用户句柄
    int lAlarmHandle;//报警布防句柄
    int lListenHandle;//报警监听句柄

    FMSGCallBack fMSFCallBack;//报警回调函数实现
    FMSGCallBack_V31 fMSFCallBack_V31;//报警回调函数实现

    FGPSDataCallback fGpsCallBack;//GPS信息查询回调函数实现

    public AlarmJavaDemoView(SingleFrameApplication app) {
        super(app);

        initComponents();

        lUserID = -1;
        lAlarmHandle = -1;
        lListenHandle = -1;
        fMSFCallBack = null;
        fMSFCallBack_V31 = null;
        fGpsCallBack = null;

        // status bar initialization - message timeout, idle icon and busy animation, etc
        ResourceMap resourceMap = getResourceMap();
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new Timer(messageTimeout, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                statusMessageLabel.setText("");
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
            }
        });
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        statusAnimationLabel.setIcon(idleIcon);

        boolean initSuc = hCNetSDK.NET_DVR_Init();
        if (initSuc != true)
        {
                 JOptionPane.showMessageDialog(null, "初始化失败");
        }

         HCNetSDK.NET_DVR_LOCAL_GENERAL_CFG struGeneralCfg = new HCNetSDK.NET_DVR_LOCAL_GENERAL_CFG();
        struGeneralCfg.byAlarmJsonPictureSeparate =1; //控制JSON透传报警数据和图片是否分离，0-不分离，1-分离（分离后走COMM_ISAPI_ALARM回调返回）
        struGeneralCfg.write();

        if(!hCNetSDK.NET_DVR_SetSDKLocalCfg(17, struGeneralCfg.getPointer()))
        {
              JOptionPane.showMessageDialog(null, "NET_DVR_SetSDKLocalCfg失败");
        }
    }

    public class FGPSDataCallback implements HCNetSDK.fGPSDataCallback
    {
        public void invoke(int nHandle, int dwState, Pointer lpBuffer, int dwBufLen, Pointer pUser)
        {
        }
    }

    public void AlarmDataHandle(int lCommand, HCNetSDK.NET_DVR_ALARMER pAlarmer, Pointer pAlarmInfo, int dwBufLen, Pointer pUser)
    {
        try {
            String sAlarmType = new String();
            DefaultTableModel alarmTableModel = ((DefaultTableModel) jTableAlarm.getModel());//获取表格模型
            String[] newRow = new String[3];
            //报警时间
            Date today = new Date();
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            String[] sIP = new String[2];

            sAlarmType = new String("lCommand=") + lCommand;
            //lCommand是传的报警类型
            switch (lCommand)
            {
                case HCNetSDK.COMM_ALARM_V40:
                    HCNetSDK.NET_DVR_ALARMINFO_V40 struAlarmInfoV40 = new HCNetSDK.NET_DVR_ALARMINFO_V40();
                    struAlarmInfoV40.write();
                    Pointer pInfoV40 = struAlarmInfoV40.getPointer();
                    pInfoV40.write(0, pAlarmInfo.getByteArray(0, struAlarmInfoV40.size()), 0, struAlarmInfoV40.size());
                    struAlarmInfoV40.read();

                    switch (struAlarmInfoV40.struAlarmFixedHeader.dwAlarmType)
                    {
                        case 0:
                            struAlarmInfoV40.struAlarmFixedHeader.ustruAlarm.setType(HCNetSDK.struIOAlarm.class);
                            struAlarmInfoV40.read();
                            sAlarmType = sAlarmType + new String("：信号量报警") + "，"+ "报警输入口：" + struAlarmInfoV40.struAlarmFixedHeader.ustruAlarm.struioAlarm.dwAlarmInputNo;
                            break;
                        case 1:
                            sAlarmType = sAlarmType + new String("：硬盘满");
                            break;
                        case 2:
                            sAlarmType = sAlarmType + new String("：信号丢失");
                            break;
                        case 3:
                            struAlarmInfoV40.struAlarmFixedHeader.ustruAlarm.setType(HCNetSDK.struAlarmChannel.class);
                            struAlarmInfoV40.read();
                            int iChanNum = struAlarmInfoV40.struAlarmFixedHeader.ustruAlarm.sstrualarmChannel.dwAlarmChanNum;
                            sAlarmType = sAlarmType + new String("：移动侦测") + "，"+ "报警通道个数：" + iChanNum + "，"+ "报警通道号：";

                            for (int i=0; i<iChanNum; i++)
                            {
                                byte[] byChannel = struAlarmInfoV40.pAlarmData.getByteArray(i*4, 4);

                                int iChanneNo = 0;
                                for(int j=0;j<4;j++)
                                {
                                    int ioffset = j*8;
                                    int iByte = byChannel[j]&0xff;
                                    iChanneNo = iChanneNo + (iByte << ioffset);
                                }

                                 sAlarmType= sAlarmType + "+ch["+ iChanneNo +"]";
                            }

                            break;
                        case 4:
                            sAlarmType = sAlarmType + new String("：硬盘未格式化");
                            break;
                        case 5:
                            sAlarmType = sAlarmType + new String("：读写硬盘出错");
                            break;
                        case 6:
                            sAlarmType = sAlarmType + new String("：遮挡报警");
                            break;
                        case 7:
                            sAlarmType = sAlarmType + new String("：制式不匹配");
                            break;
                        case 8:
                            sAlarmType = sAlarmType + new String("：非法访问");
                            break;
                    }

                    newRow[0] = dateFormat.format(today);
                    //报警类型
                    newRow[1] = sAlarmType;
                    //报警设备IP地址
                    sIP = new String(pAlarmer.sDeviceIP).split("\0", 2);
                    newRow[2] = sIP[0];
                    alarmTableModel.insertRow(0, newRow);
                    break;
                case HCNetSDK.COMM_ALARM_V30:
                    HCNetSDK.NET_DVR_ALARMINFO_V30 strAlarmInfoV30 = new HCNetSDK.NET_DVR_ALARMINFO_V30();
                    strAlarmInfoV30.write();
                    Pointer pInfoV30 = strAlarmInfoV30.getPointer();
                    pInfoV30.write(0, pAlarmInfo.getByteArray(0, strAlarmInfoV30.size()), 0, strAlarmInfoV30.size());
                    strAlarmInfoV30.read();
                    switch (strAlarmInfoV30.dwAlarmType)
                    {
                        case 0:
                            sAlarmType = sAlarmType + new String("：信号量报警") + "，"+ "报警输入口：" + (strAlarmInfoV30.dwAlarmInputNumber+1);
                            break;
                        case 1:
                            sAlarmType = sAlarmType + new String("：硬盘满");
                            break;
                        case 2:
                            sAlarmType = sAlarmType + new String("：信号丢失");
                            break;
                        case 3:
                            sAlarmType = sAlarmType + new String("：移动侦测") + "，"+ "报警通道：";
                             for (int i=0; i<64; i++)
                             {
                                if (strAlarmInfoV30.byChannel[i] == 1)
                                {
                                   sAlarmType=sAlarmType + "ch"+(i+1)+" ";
                               }
                            }
                            break;
                        case 4:
                            sAlarmType = sAlarmType + new String("：硬盘未格式化");
                            break;
                        case 5:
                            sAlarmType = sAlarmType + new String("：读写硬盘出错");
                            break;
                        case 6:
                            sAlarmType = sAlarmType + new String("：遮挡报警");
                            break;
                        case 7:
                            sAlarmType = sAlarmType + new String("：制式不匹配");
                            break;
                        case 8:
                            sAlarmType = sAlarmType + new String("：非法访问");
                            break;
                    }
                    newRow[0] = dateFormat.format(today);
                    //报警类型
                    newRow[1] = sAlarmType;
                    //报警设备IP地址
                    sIP = new String(pAlarmer.sDeviceIP).split("\0", 2);
                    newRow[2] = sIP[0];
                    alarmTableModel.insertRow(0, newRow);
                    break;
                case HCNetSDK.COMM_ALARM_RULE:
                    HCNetSDK.NET_VCA_RULE_ALARM strVcaAlarm = new HCNetSDK.NET_VCA_RULE_ALARM();
                    strVcaAlarm.write();
                    Pointer pVcaInfo = strVcaAlarm.getPointer();
                    pVcaInfo.write(0, pAlarmInfo.getByteArray(0, strVcaAlarm.size()), 0, strVcaAlarm.size());
                    strVcaAlarm.read();

                    switch (strVcaAlarm.struRuleInfo.wEventTypeEx)
                    {
                        case 1:
                            sAlarmType = sAlarmType + new String("：穿越警戒面") + "，" +
                                    "_wPort:" + strVcaAlarm.struDevInfo.wPort +
                                    "_byChannel:" + strVcaAlarm.struDevInfo.byChannel +
                                    "_byIvmsChannel:" +  strVcaAlarm.struDevInfo.byIvmsChannel +
                                    "_Dev IP：" + new String(strVcaAlarm.struDevInfo.struDevIP.sIpV4);
                            break;
                        case 2:
                            sAlarmType = sAlarmType + new String("：目标进入区域") + "，" +
                                    "_wPort:" + strVcaAlarm.struDevInfo.wPort +
                                    "_byChannel:" + strVcaAlarm.struDevInfo.byChannel +
                                    "_byIvmsChannel:" +  strVcaAlarm.struDevInfo.byIvmsChannel +
                                    "_Dev IP：" + new String(strVcaAlarm.struDevInfo.struDevIP.sIpV4);
                            break;
                        case 3:
                            sAlarmType = sAlarmType + new String("：目标离开区域") + "，" +
                                    "_wPort:" + strVcaAlarm.struDevInfo.wPort +
                                    "_byChannel:" + strVcaAlarm.struDevInfo.byChannel +
                                    "_byIvmsChannel:" +  strVcaAlarm.struDevInfo.byIvmsChannel +
                                    "_Dev IP：" + new String(strVcaAlarm.struDevInfo.struDevIP.sIpV4);
                            break;
                        default:
                            sAlarmType = sAlarmType + new String("：其他行为分析报警，事件类型：")
                                    + strVcaAlarm.struRuleInfo.wEventTypeEx +
                                    "_wPort:" + strVcaAlarm.struDevInfo.wPort +
                                    "_byChannel:" + strVcaAlarm.struDevInfo.byChannel +
                                    "_byIvmsChannel:" +  strVcaAlarm.struDevInfo.byIvmsChannel +
                                    "_Dev IP：" + new String(strVcaAlarm.struDevInfo.struDevIP.sIpV4);
                            break;
                    }
                    newRow[0] = dateFormat.format(today);
                    //报警类型
                    newRow[1] = sAlarmType;
                    //报警设备IP地址
                    sIP = new String(pAlarmer.sDeviceIP).split("\0", 2);
                    newRow[2] = sIP[0];
                    alarmTableModel.insertRow(0, newRow);

                    if(strVcaAlarm.dwPicDataLen>0)
                    {
                        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
                        String newName = sf.format(new Date());
                        FileOutputStream fout;
                        try {
                            fout = new FileOutputStream(".\\pic\\"+ new String(pAlarmer.sDeviceIP).trim()
                                    + "wEventTypeEx[" + strVcaAlarm.struRuleInfo.wEventTypeEx + "]_"+ newName +"_vca.jpg");
                            //将字节写入文件
                            long offset = 0;
                            ByteBuffer buffers = strVcaAlarm.pImage.getByteBuffer(offset, strVcaAlarm.dwPicDataLen);
                            byte [] bytes = new byte[strVcaAlarm.dwPicDataLen];
                            buffers.rewind();
                            buffers.get(bytes);
                            fout.write(bytes);
                            fout.close();
                        }catch (FileNotFoundException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    break;
                case HCNetSDK.COMM_UPLOAD_PLATE_RESULT:
                    HCNetSDK.NET_DVR_PLATE_RESULT strPlateResult = new HCNetSDK.NET_DVR_PLATE_RESULT();
                    strPlateResult.write();
                    Pointer pPlateInfo = strPlateResult.getPointer();
                    pPlateInfo.write(0, pAlarmInfo.getByteArray(0, strPlateResult.size()), 0, strPlateResult.size());
                    strPlateResult.read();
                    try {
                        String srt3=new String(strPlateResult.struPlateInfo.sLicense,"GBK");
                        sAlarmType = sAlarmType + "：交通抓拍上传，车牌："+ srt3;
                    }
                    catch (UnsupportedEncodingException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    
                    newRow[0] = dateFormat.format(today);
                    //报警类型
                    newRow[1] = sAlarmType;
                    //报警设备IP地址
                    sIP = new String(pAlarmer.sDeviceIP).split("\0", 2);
                    newRow[2] = sIP[0];
                    alarmTableModel.insertRow(0, newRow);

                    if(strPlateResult.dwPicLen>0)
                    {
                        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
                        String newName = sf.format(new Date());
                        FileOutputStream fout;
                        try {
                            fout = new FileOutputStream(".\\pic\\"+ new String(pAlarmer.sDeviceIP).trim() + "_"
                                    + newName+"_plateResult.jpg");
                            //将字节写入文件
                            long offset = 0;
                            ByteBuffer buffers = strPlateResult.pBuffer1.getByteBuffer(offset, strPlateResult.dwPicLen);
                            byte [] bytes = new byte[strPlateResult.dwPicLen];
                            buffers.rewind();
                            buffers.get(bytes);
                            fout.write(bytes);
                            fout.close();
                        } catch (FileNotFoundException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    break;
                case HCNetSDK.COMM_ITS_PLATE_RESULT:
                    HCNetSDK.NET_ITS_PLATE_RESULT strItsPlateResult = new HCNetSDK.NET_ITS_PLATE_RESULT();
                    strItsPlateResult.write();
                    Pointer pItsPlateInfo = strItsPlateResult.getPointer();
                    pItsPlateInfo.write(0, pAlarmInfo.getByteArray(0, strItsPlateResult.size()), 0, strItsPlateResult.size());
                    strItsPlateResult.read();
                    try {
                        String srt3=new String(strItsPlateResult.struPlateInfo.sLicense,"GBK");
                        sAlarmType = sAlarmType + ",车辆类型："+strItsPlateResult.byVehicleType + ",交通抓拍上传，车牌："+ srt3;
                    }
                    catch (UnsupportedEncodingException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    
                    newRow[0] = dateFormat.format(today);
                    //报警类型
                    newRow[1] = sAlarmType;
                    //报警设备IP地址
                    sIP = new String(pAlarmer.sDeviceIP).split("\0", 2);
                    newRow[2] = sIP[0];
                    alarmTableModel.insertRow(0, newRow);

                    for(int i=0;i<strItsPlateResult.dwPicNum;i++)
                    {
                        if(strItsPlateResult.struPicInfo[i].dwDataLen>0)
                        {
                            SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
                            String newName = sf.format(new Date());
                            FileOutputStream fout;
                            try {
                                String filename = ".\\pic\\"+ new String(pAlarmer.sDeviceIP).trim() + "_"
                                        + newName+"_type["+strItsPlateResult.struPicInfo[i].byType+"]_ItsPlate.jpg";
                                fout = new FileOutputStream(filename);
                                //将字节写入文件
                                long offset = 0;
                                ByteBuffer buffers = strItsPlateResult.struPicInfo[i].pBuffer.getByteBuffer(offset, strItsPlateResult.struPicInfo[i].dwDataLen);
                                byte [] bytes = new byte[strItsPlateResult.struPicInfo[i].dwDataLen];
                                buffers.rewind();
                                buffers.get(bytes);
                                fout.write(bytes);
                                fout.close();
                            } catch (FileNotFoundException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    }
                    break;
                case HCNetSDK.COMM_ALARM_PDC:
                    HCNetSDK.NET_DVR_PDC_ALRAM_INFO strPDCResult = new HCNetSDK.NET_DVR_PDC_ALRAM_INFO();
                    strPDCResult.write();
                    Pointer pPDCInfo = strPDCResult.getPointer();
                    pPDCInfo.write(0, pAlarmInfo.getByteArray(0, strPDCResult.size()), 0, strPDCResult.size());
                    strPDCResult.read();

                    if(strPDCResult.byMode == 0)
                    {
                        strPDCResult.uStatModeParam.setType(HCNetSDK.NET_DVR_STATFRAME.class);
                        sAlarmType = sAlarmType + "：客流量统计，进入人数："+ strPDCResult.dwEnterNum + "，离开人数：" + strPDCResult.dwLeaveNum +
                                ", byMode:" + strPDCResult.byMode + ", dwRelativeTime:" + strPDCResult.uStatModeParam.struStatFrame.dwRelativeTime +
                                ", dwAbsTime:" + strPDCResult.uStatModeParam.struStatFrame.dwAbsTime;
                    }
                    if(strPDCResult.byMode == 1)
                    {
                        strPDCResult.uStatModeParam.setType(HCNetSDK.NET_DVR_STATTIME.class);
                        String strtmStart = "" + String.format("%04d", strPDCResult.uStatModeParam.struStatTime.tmStart.dwYear) +
                                String.format("%02d", strPDCResult.uStatModeParam.struStatTime.tmStart.dwMonth) +
                                String.format("%02d", strPDCResult.uStatModeParam.struStatTime.tmStart.dwDay) +
                                String.format("%02d", strPDCResult.uStatModeParam.struStatTime.tmStart.dwHour) +
                                String.format("%02d", strPDCResult.uStatModeParam.struStatTime.tmStart.dwMinute) +
                                String.format("%02d", strPDCResult.uStatModeParam.struStatTime.tmStart.dwSecond);
                        String strtmEnd = "" + String.format("%04d", strPDCResult.uStatModeParam.struStatTime.tmEnd.dwYear) +
                                String.format("%02d", strPDCResult.uStatModeParam.struStatTime.tmEnd.dwMonth) +
                                String.format("%02d", strPDCResult.uStatModeParam.struStatTime.tmEnd.dwDay) +
                                String.format("%02d", strPDCResult.uStatModeParam.struStatTime.tmEnd.dwHour) +
                                String.format("%02d", strPDCResult.uStatModeParam.struStatTime.tmEnd.dwMinute) +
                                String.format("%02d", strPDCResult.uStatModeParam.struStatTime.tmEnd.dwSecond);
                        sAlarmType = sAlarmType + "：客流量统计，进入人数："+ strPDCResult.dwEnterNum + "，离开人数：" + strPDCResult.dwLeaveNum +
                                ", byMode:" + strPDCResult.byMode + ", tmStart:" + strtmStart + ",tmEnd :" + strtmEnd;
                    }

                    newRow[0] = dateFormat.format(today);
                    //报警类型
                    newRow[1] = sAlarmType;
                    //报警设备IP地址
                    sIP = new String(strPDCResult.struDevInfo.struDevIP.sIpV4).split("\0", 2);
                    newRow[2] = sIP[0];
                    alarmTableModel.insertRow(0, newRow);
                    break;

                case HCNetSDK.COMM_ITS_PARK_VEHICLE:
                    HCNetSDK.NET_ITS_PARK_VEHICLE strItsParkVehicle = new HCNetSDK.NET_ITS_PARK_VEHICLE();
                    strItsParkVehicle.write();
                    Pointer pItsParkVehicle = strItsParkVehicle.getPointer();
                    pItsParkVehicle.write(0, pAlarmInfo.getByteArray(0, strItsParkVehicle.size()), 0, strItsParkVehicle.size());
                    strItsParkVehicle.read();
                    try {
                        String srtParkingNo=new String(strItsParkVehicle.byParkingNo).trim(); //车位编号
                        String srtPlate=new String(strItsParkVehicle.struPlateInfo.sLicense,"GBK").trim(); //车牌号码
                        sAlarmType = sAlarmType + ",停产场数据,车位编号："+ srtParkingNo + ",车位状态："
                                + strItsParkVehicle.byLocationStatus+ ",车牌："+ srtPlate;
                    }
                    catch (UnsupportedEncodingException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    
                    newRow[0] = dateFormat.format(today);
                    //报警类型
                    newRow[1] = sAlarmType;
                    //报警设备IP地址
                    sIP = new String(pAlarmer.sDeviceIP).split("\0", 2);
                    newRow[2] = sIP[0];
                    alarmTableModel.insertRow(0, newRow);

                    for(int i=0;i<strItsParkVehicle.dwPicNum;i++)
                    {
                        if(strItsParkVehicle.struPicInfo[i].dwDataLen>0)
                        {
                            SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
                            String newName = sf.format(new Date());
                            FileOutputStream fout;
                            try {
                                String filename = ".\\pic\\"+ new String(pAlarmer.sDeviceIP).trim() + "_"
                                        + newName+"_type["+strItsParkVehicle.struPicInfo[i].byType+"]_ParkVehicle.jpg";
                                fout = new FileOutputStream(filename);
                                //将字节写入文件
                                long offset = 0;
                                ByteBuffer buffers = strItsParkVehicle.struPicInfo[i].pBuffer.getByteBuffer(offset, strItsParkVehicle.struPicInfo[i].dwDataLen);
                                byte [] bytes = new byte[strItsParkVehicle.struPicInfo[i].dwDataLen];
                                buffers.rewind();
                                buffers.get(bytes);
                                fout.write(bytes);
                                fout.close();
                            } catch (FileNotFoundException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    }
                    break;
                case HCNetSDK.COMM_ALARM_TFS:
                    HCNetSDK.NET_DVR_TFS_ALARM strTFSAlarmInfo = new HCNetSDK.NET_DVR_TFS_ALARM();
                    strTFSAlarmInfo.write();
                    Pointer pTFSInfo = strTFSAlarmInfo.getPointer();
                    pTFSInfo.write(0, pAlarmInfo.getByteArray(0, strTFSAlarmInfo.size()), 0, strTFSAlarmInfo.size());
                    strTFSAlarmInfo.read();

                    try {
                        String srtPlate=new String(strTFSAlarmInfo.struPlateInfo.sLicense,"GBK").trim(); //车牌号码
                        sAlarmType = sAlarmType + "：交通取证报警信息，违章类型："+ strTFSAlarmInfo.dwIllegalType + "，车牌号码：" + srtPlate
                                + "，车辆出入状态：" + strTFSAlarmInfo.struAIDInfo.byVehicleEnterState;
                    }
                    catch (UnsupportedEncodingException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    
                    newRow[0] = dateFormat.format(today);
                    //报警类型
                    newRow[1] = sAlarmType;
                    //报警设备IP地址
                    sIP = new String(strTFSAlarmInfo.struDevInfo.struDevIP.sIpV4).split("\0", 2);
                    newRow[2] = sIP[0];
                    alarmTableModel.insertRow(0, newRow);
                    break;
                case HCNetSDK.COMM_ALARM_AID_V41:
                    HCNetSDK.NET_DVR_AID_ALARM_V41 struAIDAlarmInfo = new HCNetSDK.NET_DVR_AID_ALARM_V41();
                    struAIDAlarmInfo.write();
                    Pointer pAIDInfo = struAIDAlarmInfo.getPointer();
                    pAIDInfo.write(0, pAlarmInfo.getByteArray(0, struAIDAlarmInfo.size()), 0, struAIDAlarmInfo.size());
                    struAIDAlarmInfo.read();
                    sAlarmType = sAlarmType + "：交通事件报警信息，交通事件类型："+ struAIDAlarmInfo.struAIDInfo.dwAIDType + "，规则ID："
                            + struAIDAlarmInfo.struAIDInfo.byRuleID + "，车辆出入状态：" + struAIDAlarmInfo.struAIDInfo.byVehicleEnterState;

                    newRow[0] = dateFormat.format(today);
                    //报警类型
                    newRow[1] = sAlarmType;
                    //报警设备IP地址
                    sIP = new String(struAIDAlarmInfo.struDevInfo.struDevIP.sIpV4).split("\0", 2);
                    newRow[2] = sIP[0];
                    alarmTableModel.insertRow(0, newRow);
                    break;
                case HCNetSDK.COMM_ALARM_TPS_V41:
                    HCNetSDK.NET_DVR_TPS_ALARM_V41 struTPSAlarmInfo = new HCNetSDK.NET_DVR_TPS_ALARM_V41();
                    struTPSAlarmInfo.write();
                    Pointer pTPSInfo = struTPSAlarmInfo.getPointer();
                    pTPSInfo.write(0, pAlarmInfo.getByteArray(0, struTPSAlarmInfo.size()), 0, struTPSAlarmInfo.size());
                    struTPSAlarmInfo.read();

                    sAlarmType = sAlarmType + "：交通统计报警信息，绝对时标："+ struTPSAlarmInfo.dwAbsTime
                            + "，能见度:" + struTPSAlarmInfo.struDevInfo.byIvmsChannel
                            + "，车道1交通状态:" + struTPSAlarmInfo.struTPSInfo.struLaneParam[0].byTrafficState 
                            + "，监测点编号：" + new String(struTPSAlarmInfo.byMonitoringSiteID).trim()
                            + "，设备编号：" + new String(struTPSAlarmInfo.byDeviceID ).trim()
                            + "，开始统计时间：" + struTPSAlarmInfo.dwStartTime
                            + "，结束统计时间：" + struTPSAlarmInfo.dwStopTime;

                    newRow[0] = dateFormat.format(today);
                    //报警类型
                    newRow[1] = sAlarmType;
                    //报警设备IP地址
                    sIP = new String(struTPSAlarmInfo.struDevInfo.struDevIP.sIpV4).split("\0", 2);
                    newRow[2] = sIP[0];
                    alarmTableModel.insertRow(0, newRow);
                    break;
                case HCNetSDK.COMM_UPLOAD_FACESNAP_RESULT:
                    //实时人脸抓拍上传
                    HCNetSDK.NET_VCA_FACESNAP_RESULT strFaceSnapInfo = new HCNetSDK.NET_VCA_FACESNAP_RESULT();
                    strFaceSnapInfo.write();
                    Pointer pFaceSnapInfo = strFaceSnapInfo.getPointer();
                    pFaceSnapInfo.write(0, pAlarmInfo.getByteArray(0, strFaceSnapInfo.size()), 0, strFaceSnapInfo.size());
                    strFaceSnapInfo.read();
                    sAlarmType = sAlarmType + "：人脸抓拍上传，人脸评分：" + strFaceSnapInfo.dwFaceScore + "，年龄段：" + strFaceSnapInfo.struFeature.byAgeGroup + "，性别：" + strFaceSnapInfo.struFeature.bySex;
                    newRow[0] = dateFormat.format(today);
                    //报警类型
                    newRow[1] = sAlarmType;
                    //报警设备IP地址
                    sIP = new String(strFaceSnapInfo.struDevInfo.struDevIP.sIpV4).split("\0", 2);
                    newRow[2] = sIP[0];
                    alarmTableModel.insertRow(0, newRow);
                    SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss"); //设置日期格式
                    String time = df.format(new Date()); // new Date()为获取当前系统时间
                    //人脸图片写文件
                    try {
                        FileOutputStream small = new FileOutputStream(System.getProperty("user.dir") + "\\pic\\" + time + "small.jpg");
                        FileOutputStream big = new FileOutputStream(System.getProperty("user.dir") + "\\pic\\" + time + "big.jpg");
                        
                        if(strFaceSnapInfo.dwFacePicLen > 0)
                        {
                            try {
                                small.write(strFaceSnapInfo.pBuffer1.getByteArray(0, strFaceSnapInfo.dwFacePicLen), 0, strFaceSnapInfo.dwFacePicLen);
                                small.close();
                            } catch (IOException ex) {
                                Logger.getLogger(AlarmJavaDemoView.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            
                        }
                        if(strFaceSnapInfo.dwFacePicLen > 0)
                        {
                            try {
                                big.write(strFaceSnapInfo.pBuffer2.getByteArray(0, strFaceSnapInfo.dwBackgroundPicLen), 0, strFaceSnapInfo.dwBackgroundPicLen);
                                big.close();
                            } catch (IOException ex) {
                                Logger.getLogger(AlarmJavaDemoView.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    } catch (FileNotFoundException ex) {
                        Logger.getLogger(AlarmJavaDemoView.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    break;
                case HCNetSDK.COMM_SNAP_MATCH_ALARM:
                    //人脸黑名单比对报警
                    HCNetSDK.NET_VCA_FACESNAP_MATCH_ALARM strFaceSnapMatch = new HCNetSDK.NET_VCA_FACESNAP_MATCH_ALARM();
                    strFaceSnapMatch.write();
                    Pointer pFaceSnapMatch = strFaceSnapMatch.getPointer();
                    pFaceSnapMatch.write(0, pAlarmInfo.getByteArray(0, strFaceSnapMatch.size()), 0, strFaceSnapMatch.size());
                    strFaceSnapMatch.read();
                    
                    if ((strFaceSnapMatch.dwSnapPicLen > 0) && (strFaceSnapMatch.byPicTransType == 0)) {
                        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
                        String newName = sf.format(new Date());
                        FileOutputStream fout;
                        try {
                            String filename = System.getProperty("user.dir") + "\\pic\\" + newName + "_pSnapPicBuffer" + ".jpg";
                            fout = new FileOutputStream(filename);
                            //将字节写入文件
                            long offset = 0;
                            ByteBuffer buffers = strFaceSnapMatch.pSnapPicBuffer.getByteBuffer(offset, strFaceSnapMatch.dwSnapPicLen);
                            byte[] bytes = new byte[strFaceSnapMatch.dwSnapPicLen];
                            buffers.rewind();
                            buffers.get(bytes);
                            fout.write(bytes);
                            fout.close();
                        } catch (FileNotFoundException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    if ((strFaceSnapMatch.struSnapInfo.dwSnapFacePicLen > 0) && (strFaceSnapMatch.byPicTransType == 0)) {
                        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
                        String newName = sf.format(new Date());
                        FileOutputStream fout;
                        try {
                            String filename = System.getProperty("user.dir") + "\\pic\\" + newName + "_struSnapInfo_pBuffer1" + ".jpg";
                            fout = new FileOutputStream(filename);
                            //将字节写入文件
                            long offset = 0;
                            ByteBuffer buffers = strFaceSnapMatch.struSnapInfo.pBuffer1.getByteBuffer(offset, strFaceSnapMatch.struSnapInfo.dwSnapFacePicLen);
                            byte[] bytes = new byte[strFaceSnapMatch.struSnapInfo.dwSnapFacePicLen];
                            buffers.rewind();
                            buffers.get(bytes);
                            fout.write(bytes);
                            fout.close();
                        } catch (FileNotFoundException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    if ((strFaceSnapMatch.struBlackListInfo.dwBlackListPicLen > 0) && (strFaceSnapMatch.byPicTransType == 0)) {
                        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
                        String newName = sf.format(new Date());
                        FileOutputStream fout;
                        try {
                            String filename = System.getProperty("user.dir") + "\\pic\\" + newName + "_fSimilarity_" + strFaceSnapMatch.fSimilarity + "_struBlackListInfo_pBuffer1" + ".jpg";
                            fout = new FileOutputStream(filename);
                            //将字节写入文件
                            long offset = 0;
                            ByteBuffer buffers = strFaceSnapMatch.struBlackListInfo.pBuffer1.getByteBuffer(offset, strFaceSnapMatch.struBlackListInfo.dwBlackListPicLen);
                            byte[] bytes = new byte[strFaceSnapMatch.struBlackListInfo.dwBlackListPicLen];
                            buffers.rewind();
                            buffers.get(bytes);
                            fout.write(bytes);
                            fout.close();
                        } catch (FileNotFoundException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    
                    sAlarmType = sAlarmType + "：人脸黑名单比对报警，相识度：" + strFaceSnapMatch.fSimilarity + "，黑名单姓名：" + new String(strFaceSnapMatch.struBlackListInfo.struBlackListInfo.struAttribute.byName, "GBK").trim() + "，\n黑名单证件信息：" + new String(strFaceSnapMatch.struBlackListInfo.struBlackListInfo.struAttribute.byCertificateNumber).trim();
                    
                    //获取人脸库ID
                    byte[] FDIDbytes;
                    if ((strFaceSnapMatch.struBlackListInfo.dwFDIDLen > 0) && (strFaceSnapMatch.struBlackListInfo.pFDID != null)) {
                        ByteBuffer FDIDbuffers = strFaceSnapMatch.struBlackListInfo.pFDID.getByteBuffer(0, strFaceSnapMatch.struBlackListInfo.dwFDIDLen);
                        FDIDbytes = new byte[strFaceSnapMatch.struBlackListInfo.dwFDIDLen];
                        FDIDbuffers.rewind();
                        FDIDbuffers.get(FDIDbytes);
                        sAlarmType = sAlarmType + "，人脸库ID:" + new String(FDIDbytes).trim();
                    }
                    //获取人脸图片ID
                    byte[] PIDbytes;
                    if ((strFaceSnapMatch.struBlackListInfo.dwPIDLen > 0) && (strFaceSnapMatch.struBlackListInfo.pPID != null)) {
                        ByteBuffer PIDbuffers = strFaceSnapMatch.struBlackListInfo.pPID.getByteBuffer(0, strFaceSnapMatch.struBlackListInfo.dwPIDLen);
                        PIDbytes = new byte[strFaceSnapMatch.struBlackListInfo.dwPIDLen];
                        PIDbuffers.rewind();
                        PIDbuffers.get(PIDbytes);
                        sAlarmType = sAlarmType + "，人脸图片ID:" + new String(PIDbytes).trim();
                    }
                    newRow[0] = dateFormat.format(today);
                    //报警类型
                    newRow[1] = sAlarmType;
                    //报警设备IP地址
                    sIP = new String(pAlarmer.sDeviceIP).split("\0", 2);
                    newRow[2] = sIP[0];
                    alarmTableModel.insertRow(0, newRow);
                    break;
                case HCNetSDK.COMM_ALARM_ACS: //门禁主机报警信息
                    HCNetSDK.NET_DVR_ACS_ALARM_INFO strACSInfo = new HCNetSDK.NET_DVR_ACS_ALARM_INFO();
                    strACSInfo.write();
                    Pointer pACSInfo = strACSInfo.getPointer();
                    pACSInfo.write(0, pAlarmInfo.getByteArray(0, strACSInfo.size()), 0, strACSInfo.size());
                    strACSInfo.read();

                    sAlarmType = sAlarmType + "：门禁主机报警信息，卡号："+  new String(strACSInfo.struAcsEventInfo.byCardNo).trim() + "，卡类型：" +
                            strACSInfo.struAcsEventInfo.byCardType + "，报警主类型：" + strACSInfo.dwMajor + "，报警次类型：" + strACSInfo.dwMinor;

                    newRow[0] = dateFormat.format(today);
                    //报警类型
                    newRow[1] = sAlarmType;
                    //报警设备IP地址
                    sIP = new String(pAlarmer.sDeviceIP).split("\0", 2);
                    newRow[2] = sIP[0];
                    alarmTableModel.insertRow(0, newRow);

                    if(strACSInfo.dwPicDataLen>0)
                    {
                        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
                        String newName = sf.format(new Date());
                        FileOutputStream fout;
                        try {
                            String filename = ".\\pic\\"+ new String(pAlarmer.sDeviceIP).trim() +
                                    "_byCardNo["+ new String(strACSInfo.struAcsEventInfo.byCardNo).trim() +
                                    "_"+ newName + "_Acs.jpg";
                            fout = new FileOutputStream(filename);
                            //将字节写入文件
                            long offset = 0;
                            ByteBuffer buffers = strACSInfo.pPicData.getByteBuffer(offset, strACSInfo.dwPicDataLen);
                            byte [] bytes = new byte[strACSInfo.dwPicDataLen];
                            buffers.rewind();
                            buffers.get(bytes);
                            fout.write(bytes);
                            fout.close();
                        } catch (FileNotFoundException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    break;
                case HCNetSDK.COMM_ID_INFO_ALARM: //身份证信息
                    HCNetSDK.NET_DVR_ID_CARD_INFO_ALARM strIDCardInfo = new HCNetSDK.NET_DVR_ID_CARD_INFO_ALARM();
                    strIDCardInfo.write();
                    Pointer pIDCardInfo = strIDCardInfo.getPointer();
                    pIDCardInfo.write(0, pAlarmInfo.getByteArray(0, strIDCardInfo.size()), 0, strIDCardInfo.size());
                    strIDCardInfo.read();

                    sAlarmType = sAlarmType + "：门禁身份证刷卡信息，身份证号码："+  new String(strIDCardInfo.struIDCardCfg.byIDNum).trim() + "，姓名：" +
                            new String(strIDCardInfo.struIDCardCfg.byName).trim() + "，报警主类型：" + strIDCardInfo.dwMajor + "，报警次类型：" + strIDCardInfo.dwMinor;

                    newRow[0] = dateFormat.format(today);
                    //报警类型
                    newRow[1] = sAlarmType;
                    //报警设备IP地址
                    sIP = new String(pAlarmer.sDeviceIP).split("\0", 2);
                    newRow[2] = sIP[0];
                    alarmTableModel.insertRow(0, newRow);
                    
                    //身份证图片
                    if(strIDCardInfo.dwPicDataLen>0)
                    {
                        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
                        String newName = sf.format(new Date());
                        FileOutputStream fout;
                        try {
                            String filename = ".\\pic\\"+ new String(pAlarmer.sDeviceIP).trim() +
                                    "_byCardNo["+ new String(strIDCardInfo.struIDCardCfg.byIDNum ).trim() +
                                    "_"+ newName + "_IDInfoPic.jpg";
                            fout = new FileOutputStream(filename);
                            //将字节写入文件
                            long offset = 0;
                            ByteBuffer buffers = strIDCardInfo.pPicData.getByteBuffer(offset, strIDCardInfo.dwPicDataLen);
                            byte [] bytes = new byte[strIDCardInfo.dwPicDataLen];
                            buffers.rewind();
                            buffers.get(bytes);
                            fout.write(bytes);
                            fout.close();
                        } catch (FileNotFoundException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    
                    //抓拍图片
                    if(strIDCardInfo.dwCapturePicDataLen >0)
                    {
                        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
                        String newName = sf.format(new Date());
                        FileOutputStream fout;
                        try {
                            String filename = ".\\pic\\"+ new String(pAlarmer.sDeviceIP).trim() +
                                    "_byCardNo["+ new String(strIDCardInfo.struIDCardCfg.byIDNum ).trim() +
                                    "_"+ newName + "_IDInfoCapturePic.jpg";
                            fout = new FileOutputStream(filename);
                            //将字节写入文件
                            long offset = 0;
                            ByteBuffer buffers = strIDCardInfo.pCapturePicData.getByteBuffer(offset, strIDCardInfo.dwCapturePicDataLen);
                            byte [] bytes = new byte[strIDCardInfo.dwCapturePicDataLen];
                            buffers.rewind();
                            buffers.get(bytes);
                            fout.write(bytes);
                            fout.close();
                        } catch (FileNotFoundException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    break;
                case HCNetSDK.COMM_ISAPI_ALARM: //ISAPI协议报警信息
                    HCNetSDK.NET_DVR_ALARM_ISAPI_INFO struEventISAPI = new HCNetSDK.NET_DVR_ALARM_ISAPI_INFO();
                    struEventISAPI.write();
                    Pointer pEventISAPI = struEventISAPI.getPointer();
                    pEventISAPI.write(0, pAlarmInfo.getByteArray(0, struEventISAPI.size()), 0, struEventISAPI.size());
                    struEventISAPI.read();

                    sAlarmType = sAlarmType + "：ISAPI协议报警信息, 数据格式:" + struEventISAPI.byDataType +
                            ", 图片个数:" + struEventISAPI.byPicturesNumber;

                    newRow[0] = dateFormat.format(today);
                    //报警类型
                    newRow[1] = sAlarmType;
                    //报警设备IP地址
                    sIP = new String(pAlarmer.sDeviceIP).split("\0", 2);
                    newRow[2] = sIP[0];
                    alarmTableModel.insertRow(0, newRow);

                    SimpleDateFormat sf1 = new SimpleDateFormat("yyyyMMddHHmmss");
                    String curTime = sf1.format(new Date());
                    FileOutputStream foutdata;
                    try {
                        String jsonfilename = ".\\pic\\" + new String(pAlarmer.sDeviceIP).trim() + curTime +"_ISAPI_Alarm_" + ".json";
                        foutdata = new FileOutputStream(jsonfilename);
                        //将字节写入文件
                        ByteBuffer jsonbuffers = struEventISAPI.pAlarmData.getByteBuffer(0, struEventISAPI.dwAlarmDataLen);
                        byte [] jsonbytes = new byte[struEventISAPI.dwAlarmDataLen];
                        jsonbuffers.rewind();
                        jsonbuffers.get(jsonbytes);
                        foutdata.write(jsonbytes);
                        foutdata.close();
                    } catch (FileNotFoundException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                    } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                    }

                    for(int i=0;i<struEventISAPI.byPicturesNumber;i++)
                    {
                        HCNetSDK.NET_DVR_ALARM_ISAPI_PICDATA struPicData = new HCNetSDK.NET_DVR_ALARM_ISAPI_PICDATA();
                        struPicData.write();
                        Pointer pPicData = struPicData.getPointer();
                        pPicData.write(0, struEventISAPI.pPicPackData.getByteArray(i*struPicData.size(), struPicData.size()), 0, struPicData.size());
                        struPicData.read();

                        FileOutputStream fout;
                        try {
                            String filename = ".\\pic\\" + new String(pAlarmer.sDeviceIP).trim() + curTime +
                                    "_ISAPIPic_"+ i + "_" + new String(struPicData.szFilename).trim() +".jpg";
                            fout = new FileOutputStream(filename);
                            //将字节写入文件
                            long offset = 0;
                            ByteBuffer buffers = struPicData.pPicData.getByteBuffer(offset, struPicData.dwPicLen);
                            byte [] bytes = new byte[struPicData.dwPicLen];
                            buffers.rewind();
                            buffers.get(bytes);
                            fout.write(bytes);
                            fout.close();
                        } catch (FileNotFoundException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    break;
                default:
                    newRow[0] = dateFormat.format(today);
                    //报警类型
                    newRow[1] = sAlarmType;
                    //报警设备IP地址
                    sIP = new String(pAlarmer.sDeviceIP).split("\0", 2);
                    newRow[2] = sIP[0];
                    alarmTableModel.insertRow(0, newRow);
                    break;
            }
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(AlarmJavaDemoView.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public class FMSGCallBack_V31 implements HCNetSDK.FMSGCallBack_V31
    {
        //报警信息回调函数

        public boolean invoke(int lCommand, HCNetSDK.NET_DVR_ALARMER pAlarmer, Pointer pAlarmInfo, int dwBufLen, Pointer pUser)
        {
            AlarmDataHandle(lCommand, pAlarmer, pAlarmInfo, dwBufLen, pUser);
            return true;
        }
    }

    public class FMSGCallBack implements HCNetSDK.FMSGCallBack
    {
        //报警信息回调函数

        public void invoke(int lCommand, HCNetSDK.NET_DVR_ALARMER pAlarmer, Pointer pAlarmInfo, int dwBufLen, Pointer pUser)
        {
            AlarmDataHandle(lCommand, pAlarmer, pAlarmInfo, dwBufLen, pUser);
        }
     }

    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = AlarmJavaDemoApp.getApplication().getMainFrame();
            aboutBox = new AlarmJavaDemoAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        AlarmJavaDemoApp.getApplication().show(aboutBox);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        jTextFieldIPAddress = new javax.swing.JTextField();
        jLabelIPAddress = new javax.swing.JLabel();
        jLabelUserName = new javax.swing.JLabel();
        jTextFieldUserName = new javax.swing.JTextField();
        jPasswordFieldPassword = new javax.swing.JPasswordField();
        jLabelPassWord = new javax.swing.JLabel();
        jLabelPortNumber = new javax.swing.JLabel();
        jTextFieldPortNumber = new javax.swing.JTextField();
        jButtonLogin = new javax.swing.JButton();
        jLogOut = new javax.swing.JButton();
        jBtnAlarm = new javax.swing.JButton();
        jBtnCloseAlarm = new javax.swing.JButton();
        jScrollPanelAlarmList = new javax.swing.JScrollPane();
        jTableAlarm = new javax.swing.JTable();
        jButtonListen = new javax.swing.JButton();
        jTextFieldListenIP = new javax.swing.JTextField();
        jLabelIPAddress1 = new javax.swing.JLabel();
        jTextFieldListenPort = new javax.swing.JTextField();
        jLabelPortNumber1 = new javax.swing.JLabel();
        jButtonStopListen = new javax.swing.JButton();
        jButtonTest = new javax.swing.JButton();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();

        mainPanel.setName("mainPanel"); // NOI18N

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(alarmjavademo.AlarmJavaDemoApp.class).getContext().getResourceMap(AlarmJavaDemoView.class);
        jTextFieldIPAddress.setText(resourceMap.getString("jTextFieldIPAddress.text")); // NOI18N
        jTextFieldIPAddress.setName("jTextFieldIPAddress"); // NOI18N

        jLabelIPAddress.setText(resourceMap.getString("jLabelIPAddress.text")); // NOI18N
        jLabelIPAddress.setName("jLabelIPAddress"); // NOI18N

        jLabelUserName.setText(resourceMap.getString("jLabelUserName.text")); // NOI18N
        jLabelUserName.setName("jLabelUserName"); // NOI18N

        jTextFieldUserName.setText(resourceMap.getString("jTextFieldUserName.text")); // NOI18N
        jTextFieldUserName.setName("jTextFieldUserName"); // NOI18N

        jPasswordFieldPassword.setText(resourceMap.getString("jPasswordFieldPassword.text")); // NOI18N
        jPasswordFieldPassword.setName("jPasswordFieldPassword"); // NOI18N

        jLabelPassWord.setText(resourceMap.getString("jLabelPassWord.text")); // NOI18N
        jLabelPassWord.setName("jLabelPassWord"); // NOI18N

        jLabelPortNumber.setText(resourceMap.getString("jLabelPortNumber.text")); // NOI18N
        jLabelPortNumber.setName("jLabelPortNumber"); // NOI18N

        jTextFieldPortNumber.setText(resourceMap.getString("jTextFieldPortNumber.text")); // NOI18N
        jTextFieldPortNumber.setName("jTextFieldPortNumber"); // NOI18N

        jButtonLogin.setText(resourceMap.getString("jButtonLogin.text")); // NOI18N
        jButtonLogin.setName("jButtonLogin"); // NOI18N
        jButtonLogin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonLoginActionPerformed(evt);
            }
        });

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(alarmjavademo.AlarmJavaDemoApp.class).getContext().getActionMap(AlarmJavaDemoView.class, this);
        jLogOut.setAction(actionMap.get("Logout")); // NOI18N
        jLogOut.setText(resourceMap.getString("jLogOut.text")); // NOI18N
        jLogOut.setName("jLogOut"); // NOI18N

        jBtnAlarm.setAction(actionMap.get("SetupAlarmChan")); // NOI18N
        jBtnAlarm.setText(resourceMap.getString("jBtnAlarm.text")); // NOI18N
        jBtnAlarm.setName("jBtnAlarm"); // NOI18N

        jBtnCloseAlarm.setAction(actionMap.get("CloseAlarmChan")); // NOI18N
        jBtnCloseAlarm.setText(resourceMap.getString("jBtnCloseAlarm.text")); // NOI18N
        jBtnCloseAlarm.setName("jBtnCloseAlarm"); // NOI18N

        jScrollPanelAlarmList.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jScrollPanelAlarmList.setName("jScrollPanelAlarmList"); // NOI18N

        jTableAlarm.setModel(initialTableModel());
        jTableAlarm.setName("jTableAlarm"); // NOI18N
        jScrollPanelAlarmList.setViewportView(jTableAlarm);

        jButtonListen.setAction(actionMap.get("StartAlarmListen")); // NOI18N
        jButtonListen.setText(resourceMap.getString("jButtonListen.text")); // NOI18N
        jButtonListen.setName("jButtonListen"); // NOI18N

        jTextFieldListenIP.setText(resourceMap.getString("jTextFieldListenIP.text")); // NOI18N
        jTextFieldListenIP.setName("jTextFieldListenIP"); // NOI18N

        jLabelIPAddress1.setText(resourceMap.getString("jLabelIPAddress1.text")); // NOI18N
        jLabelIPAddress1.setName("jLabelIPAddress1"); // NOI18N

        jTextFieldListenPort.setText(resourceMap.getString("jTextFieldListenPort.text")); // NOI18N
        jTextFieldListenPort.setName("jTextFieldListenPort"); // NOI18N

        jLabelPortNumber1.setText(resourceMap.getString("jLabelPortNumber1.text")); // NOI18N
        jLabelPortNumber1.setName("jLabelPortNumber1"); // NOI18N

        jButtonStopListen.setAction(actionMap.get("StopAlarmListen")); // NOI18N
        jButtonStopListen.setText(resourceMap.getString("jButtonStopListen.text")); // NOI18N
        jButtonStopListen.setName("jButtonStopListen"); // NOI18N

        jButtonTest.setAction(actionMap.get("OneTest")); // NOI18N
        jButtonTest.setText(resourceMap.getString("jButtonTest.text")); // NOI18N
        jButtonTest.setActionCommand(resourceMap.getString("jButtonTest.actionCommand")); // NOI18N
        jButtonTest.setName("jButtonTest"); // NOI18N

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPanelAlarmList, javax.swing.GroupLayout.DEFAULT_SIZE, 933, Short.MAX_VALUE)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, mainPanelLayout.createSequentialGroup()
                                .addComponent(jLabelPortNumber)
                                .addGap(26, 26, 26)
                                .addComponent(jTextFieldPortNumber))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, mainPanelLayout.createSequentialGroup()
                                .addComponent(jLabelIPAddress)
                                .addGap(14, 14, 14)
                                .addComponent(jTextFieldIPAddress, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(31, 31, 31)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabelUserName)
                            .addComponent(jLabelPassWord))
                        .addGap(14, 14, 14)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jPasswordFieldPassword)
                            .addComponent(jTextFieldUserName, javax.swing.GroupLayout.DEFAULT_SIZE, 96, Short.MAX_VALUE))
                        .addGap(48, 48, 48)
                        .addComponent(jButtonLogin, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(30, 30, 30)
                        .addComponent(jLogOut, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(43, 43, 43)
                        .addComponent(jButtonTest)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 248, Short.MAX_VALUE))
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addComponent(jBtnAlarm, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jBtnCloseAlarm, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(54, 54, 54)
                        .addComponent(jLabelIPAddress1)
                        .addGap(12, 12, 12)
                        .addComponent(jTextFieldListenIP, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabelPortNumber1)
                        .addGap(10, 10, 10)
                        .addComponent(jTextFieldListenPort, javax.swing.GroupLayout.PREFERRED_SIZE, 113, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(33, 33, 33)
                        .addComponent(jButtonListen)
                        .addGap(18, 18, 18)
                        .addComponent(jButtonStopListen)))
                .addContainerGap())
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(mainPanelLayout.createSequentialGroup()
                                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabelIPAddress)
                                    .addComponent(jTextFieldIPAddress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabelPortNumber)
                                    .addComponent(jTextFieldPortNumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(mainPanelLayout.createSequentialGroup()
                                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabelUserName)
                                    .addComponent(jTextFieldUserName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabelPassWord)
                                    .addComponent(jPasswordFieldPassword, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addGap(22, 22, 22)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButtonLogin)
                            .addComponent(jLogOut)
                            .addComponent(jButtonTest))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPanelAlarmList, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jBtnAlarm)
                        .addComponent(jBtnCloseAlarm))
                    .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabelIPAddress1)
                        .addComponent(jTextFieldListenIP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabelPortNumber1)
                        .addComponent(jTextFieldListenPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jButtonListen)
                        .addComponent(jButtonStopListen)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jBtnAlarm.getAccessibleContext().setAccessibleName(resourceMap.getString("jBtnAlarm.AccessibleContext.accessibleName")); // NOI18N

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        exitMenuItem.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                exitMenuItemMouseClicked(evt);
            }
        });
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        statusPanel.setName("statusPanel"); // NOI18N

        statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

        statusMessageLabel.setName("statusMessageLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusPanelSeparator, javax.swing.GroupLayout.DEFAULT_SIZE, 943, Short.MAX_VALUE)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusMessageLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 923, Short.MAX_VALUE)
                .addComponent(statusAnimationLabel)
                .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addComponent(statusPanelSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(statusMessageLabel)
                    .addComponent(statusAnimationLabel))
                .addGap(3, 3, 3))
        );

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonLoginActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonLoginActionPerformed

        //注册之前先注销已注册的用户,预览情况下不可注销     
        if (lUserID > -1) {
            //先注销
            hCNetSDK.NET_DVR_Logout(lUserID);
            lUserID = -1;
        }

        //注册
        m_sDeviceIP = jTextFieldIPAddress.getText();//设备ip地址  
        m_strLoginInfo.sDeviceAddress = new byte[HCNetSDK.NET_DVR_DEV_ADDRESS_MAX_LEN];
        System.arraycopy(m_sDeviceIP.getBytes(), 0, m_strLoginInfo.sDeviceAddress, 0, m_sDeviceIP.length());
        
        m_sUsername = jTextFieldUserName.getText();//设备用户名
        m_strLoginInfo.sUserName = new byte[HCNetSDK.NET_DVR_LOGIN_USERNAME_MAX_LEN];
        System.arraycopy(m_sUsername.getBytes(), 0, m_strLoginInfo.sUserName, 0, m_sUsername.length());
        
        m_sPassword = new String(jPasswordFieldPassword.getPassword());//设备密码
        m_strLoginInfo.sPassword = new byte[HCNetSDK.NET_DVR_LOGIN_PASSWD_MAX_LEN];
        System.arraycopy(m_sPassword.getBytes(), 0, m_strLoginInfo.sPassword, 0, m_sPassword.length());
        
        m_strLoginInfo.wPort = (short)Integer.parseInt(jTextFieldPortNumber.getText());
        
        m_strLoginInfo.bUseAsynLogin = false; //是否异步登录：0- 否，1- 是
        
        m_strLoginInfo.write();
        lUserID = hCNetSDK.NET_DVR_Login_V40(m_strLoginInfo, m_strDeviceInfo);

        if (lUserID == -1) {
            JOptionPane.showMessageDialog(null, "注册失败，错误号:" +  hCNetSDK.NET_DVR_GetLastError());
        } else {
            JOptionPane.showMessageDialog(null, "注册成功");
        }
}//GEN-LAST:event_jButtonLoginActionPerformed

    private void exitMenuItemMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exitMenuItemMouseClicked
        // TODO add your handling code here:
        if (lAlarmHandle > -1)
        {
            hCNetSDK.NET_DVR_CloseAlarmChan_V30(lAlarmHandle);
            lAlarmHandle = -1;
        }
        if (lUserID > -1) {
            //先注销
            hCNetSDK.NET_DVR_Logout(lUserID);
            lUserID = -1;
        }
        hCNetSDK.NET_DVR_Cleanup();
    }//GEN-LAST:event_exitMenuItemMouseClicked

    @Action
    public void Logout() {
        //报警撤防
        if (lAlarmHandle > -1)
        {
            if(!hCNetSDK.NET_DVR_CloseAlarmChan_V30(lAlarmHandle))
            {
                 JOptionPane.showMessageDialog(null, "撤防失败");
            }
            else
            {
                lAlarmHandle = -1;
            }
        }

        //注销
        if (lUserID > -1) {            
            if(hCNetSDK.NET_DVR_Logout(lUserID))
            {
                JOptionPane.showMessageDialog(null, "注销成功");
                lUserID = -1;
            }
        }
    }

    @Action
    public void SetupAlarmChan() {
        if (lUserID == -1)
        {
            JOptionPane.showMessageDialog(null, "请先注册");
            return;
        }
         if (lAlarmHandle < 0)//尚未布防,需要布防
         {
                if (fMSFCallBack_V31 == null)
                {
                    fMSFCallBack_V31 = new FMSGCallBack_V31();
                    Pointer pUser = null;
                    if (!hCNetSDK.NET_DVR_SetDVRMessageCallBack_V31(fMSFCallBack_V31, pUser))
                    {
                        System.out.println("设置回调函数失败!");
                    }
                }
                HCNetSDK.NET_DVR_SETUPALARM_PARAM m_strAlarmInfo = new HCNetSDK.NET_DVR_SETUPALARM_PARAM();
                m_strAlarmInfo.dwSize=m_strAlarmInfo.size();
                m_strAlarmInfo.byLevel=1;//智能交通布防优先级：0- 一等级（高），1- 二等级（中），2- 三等级（低）
                m_strAlarmInfo.byAlarmInfoType=1;//智能交通报警信息上传类型：0- 老报警信息（NET_DVR_PLATE_RESULT），1- 新报警信息(NET_ITS_PLATE_RESULT)
                m_strAlarmInfo.byDeployType =1; //布防类型(仅针对门禁主机、人证设备)：0-客户端布防(会断网续传)，1-实时布防(只上传实时数据)
                m_strAlarmInfo.write();
                lAlarmHandle = hCNetSDK.NET_DVR_SetupAlarmChan_V41(lUserID, m_strAlarmInfo);
                if (lAlarmHandle == -1)
                {
                    JOptionPane.showMessageDialog(null, "布防失败，错误号:" +  hCNetSDK.NET_DVR_GetLastError());
                }
                else
                {
                    JOptionPane.showMessageDialog(null, "布防成功");
                }
          }
    }

    @Action
    public void CloseAlarmChan() {
        //报警撤防
        if (lAlarmHandle > -1)
        {
            if(hCNetSDK.NET_DVR_CloseAlarmChan_V30(lAlarmHandle))
            {
                JOptionPane.showMessageDialog(null, "撤防成功");
                lAlarmHandle = -1;
            }
        }
    }

     /*************************************************
    函数:      initialTableModel
    函数描述:	初始化报警信息列表,写入列名称
     *************************************************/
    public DefaultTableModel initialTableModel()
    {
        String tabeTile[];
        tabeTile = new String[]{"时间", "报警信息", "设备信息" };
        DefaultTableModel alarmTableModel = new DefaultTableModel(tabeTile, 0);
        return alarmTableModel;
    }

    @Action
    public void StartAlarmListen() {
        String m_sListenIP = jTextFieldListenIP.getText();//设备ip地址
        int iListenPort = Integer.parseInt(jTextFieldListenPort.getText());
        Pointer pUser = null;

        if (fMSFCallBack == null)
        {
             fMSFCallBack = new FMSGCallBack();
        }
        lListenHandle = hCNetSDK.NET_DVR_StartListen_V30(m_sListenIP, (short)iListenPort,fMSFCallBack, pUser);
        if(lListenHandle < 0)
        {
            JOptionPane.showMessageDialog(null, "启动监听失败，错误号:" +  hCNetSDK.NET_DVR_GetLastError());
        }
        else
        {
             JOptionPane.showMessageDialog(null, "启动监听成功");
        }
    }

    @Action
    public void StopAlarmListen() {
        if(lListenHandle < 0)
        {
            return;
        }

        if(!hCNetSDK.NET_DVR_StopListen_V30(lListenHandle))
        {
            JOptionPane.showMessageDialog(null, "停止监听失败");
        }
        else
        {
             JOptionPane.showMessageDialog(null, "停止监听成功");
        }
    }

    @Action
    public void OneTest() {

        HCNetSDK.NET_DVR_SNAPCFG struSnapCfg = new HCNetSDK.NET_DVR_SNAPCFG();
        struSnapCfg.dwSize=struSnapCfg.size();
        struSnapCfg.bySnapTimes =1;
        struSnapCfg.wSnapWaitTime =1000;
        struSnapCfg.write();

        if (false == hCNetSDK.NET_DVR_ContinuousShoot(lUserID, struSnapCfg))
	{
            int iErr = hCNetSDK.NET_DVR_GetLastError();
            JOptionPane.showMessageDialog(null, "网络触发失败，错误号：" + iErr);
            return;
        }

        /*
        HCNetSDK.NET_DVR_CHANNEL_GROUP mstruChanGroup = new HCNetSDK.NET_DVR_CHANNEL_GROUP();
        mstruChanGroup.dwSize = mstruChanGroup.size();
        mstruChanGroup.dwChannel = 1;

        HCNetSDK.NET_VCA_TRAVERSE_PLANE_DETECTION mstruTraverseCfg = new HCNetSDK.NET_VCA_TRAVERSE_PLANE_DETECTION();	

	IntByReference pInt = new IntByReference(0);
        Pointer lpStatusList = pInt.getPointer();

	mstruChanGroup.write();
	mstruTraverseCfg.write();

        Pointer lpCond = mstruChanGroup.getPointer();
        Pointer lpInbuferCfg = mstruTraverseCfg.getPointer();
        
	if (false == hCNetSDK.NET_DVR_GetDeviceConfig(lUserID, HCNetSDK.NET_DVR_GET_TRAVERSE_PLANE_DETECTION, 1, lpCond, mstruChanGroup.size(), lpStatusList, lpInbuferCfg, mstruTraverseCfg.size()))
	{
            int iErr = hCNetSDK.NET_DVR_GetLastError();
            return;
        }
        mstruTraverseCfg.read();
        int dwMaxRelRecordChanNum = mstruTraverseCfg.dwMaxRelRecordChanNum;
         * */
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jBtnAlarm;
    private javax.swing.JButton jBtnCloseAlarm;
    private javax.swing.JButton jButtonListen;
    private javax.swing.JButton jButtonLogin;
    private javax.swing.JButton jButtonStopListen;
    private javax.swing.JButton jButtonTest;
    private javax.swing.JLabel jLabelIPAddress;
    private javax.swing.JLabel jLabelIPAddress1;
    private javax.swing.JLabel jLabelPassWord;
    private javax.swing.JLabel jLabelPortNumber;
    private javax.swing.JLabel jLabelPortNumber1;
    private javax.swing.JLabel jLabelUserName;
    private javax.swing.JButton jLogOut;
    private javax.swing.JPasswordField jPasswordFieldPassword;
    private javax.swing.JScrollPane jScrollPanelAlarmList;
    private javax.swing.JTable jTableAlarm;
    private javax.swing.JTextField jTextFieldIPAddress;
    private javax.swing.JTextField jTextFieldListenIP;
    private javax.swing.JTextField jTextFieldListenPort;
    private javax.swing.JTextField jTextFieldPortNumber;
    private javax.swing.JTextField jTextFieldUserName;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    // End of variables declaration//GEN-END:variables

    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;

    private JDialog aboutBox;
}


