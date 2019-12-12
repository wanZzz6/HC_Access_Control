# HC_Access_Control

Windows64环境下海康门禁 SDK 的开发和使用

先修改 HCNetSDK.java 中 dll文件所在的绝对路径（两处）

---

package singleDoorDemo:

- alarmjavademo 官方SDK报警布防监听demo 

- OpenTheDoor 远程开门

- getDoorConfig  获取门参数（密码、名称等）

- setDoorConfig  设置门参数（密码、名称等）

- AlarmListen  布防监听门禁事件

- AlarmMqttSend 门禁事件通过Mqtt 发送到IoT平台
