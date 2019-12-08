package com.tsit.mqtt;

import java.io.UnsupportedEncodingException;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import com.tsit.callback.MqttRecieveCallback;

public class MyMqttClient {

	private String clientId = "";
	private String Host;
	private String UserName;
	private char[] passwd;

	public MqttClient mqttClient = null;
	private MemoryPersistence memoryPersistence = null;
	private MqttConnectOptions mqttConnectOptions = null;

	// 将自身实例化对象设置为一个属性，并用static修饰
	private static MyMqttClient instance;

	// 构造方法私有化
	private MyMqttClient(String host, String userName, String clientId) {
		Host = host;
		this.UserName = userName;
		this.clientId = clientId;
		initMqttClient(clientId);
	}

	private MyMqttClient() {
	}

	// 静态方法返回该实例
	public static MyMqttClient getInstance(String host, String userName, String clientId) {
		// 第一次检查instance是否被实例化出来，如果没有进入if块,
		if (instance == null) {
			synchronized (MyMqttClient.class) {
				// 某个线程取得了类锁，实例化对象前第二次检查instance是否已经被实例化出来，如果没有，才最终实例出对象
				if (instance == null) {
					instance = new MyMqttClient(host, userName, clientId);
				}
			}
		}
		return instance;
	}

	public static MyMqttClient getInstance() {
		// 第一次检查instance是否被实例化出来，如果没有进入if块,
		if (instance == null) {
			synchronized (MyMqttClient.class) {
				// 某个线程取得了类锁，实例化对象前第二次检查instance是否已经被实例化出来，如果没有，才最终实例出对象
				if (instance == null) {
					instance = new MyMqttClient();
				}
			}
		}
		return instance;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getHost() {
		return Host;
	}

	public void setHost(String host) {
		Host = host;
	}

	public String getUserName() {
		return UserName;
	}

	public void setUserName(String userName) {
		UserName = userName;
	}

	public char[] getPasswd() {
		return passwd;
	}

	public void setPasswd(char[] passwd) {
		this.passwd = passwd;
	}

	public void initMqttClient(String clientId) {
		// 初始化连接设置对象
		mqttConnectOptions = new MqttConnectOptions();
		// 初始化MqttClient
		if (null != mqttConnectOptions) {
			// true可以安全地使用内存持久性作为客户端断开连接时清除的所有状态
			mqttConnectOptions.setCleanSession(true);
			// 设置连接超时
			mqttConnectOptions.setConnectionTimeout(30);
			// 设置持久化方式
			memoryPersistence = new MemoryPersistence();
			// 设置用户名token
			mqttConnectOptions.setUserName(UserName);
//			mqttConnectOptions.setPassword(passwd);
			if (null != memoryPersistence && null != clientId) {
				try {
					mqttClient = new MqttClient(Host, clientId, memoryPersistence);
				} catch (MqttException e) {
					e.printStackTrace();
				}
			} else {
			}
		} else {
			System.out.println("mqttConnectOptions对象为空");
		}

		// 设置连接和回调
		if (null != mqttClient) {
			if (!mqttClient.isConnected()) {

				// 创建回调函数对象
				MqttRecieveCallback mqttReceriveCallback = new MqttRecieveCallback();
				//// 客户端添加回调函数
				mqttClient.setCallback(mqttReceriveCallback);
				// 创建连接
				try {
					System.out.println("Connecting to broker: " + Host);
					mqttClient.connect(mqttConnectOptions);
				} catch (MqttException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			System.out.println("mqttClient isConnected: " + mqttClient.isConnected());
		} else {
			System.out.println("mqttClient为空");
		}
	}

	// 关闭连接
	public void closeConnect() {
		// 关闭存储方式
		if (null != memoryPersistence) {
			try {
				memoryPersistence.close();
			} catch (MqttPersistenceException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("memoryPersistence is null");
		}

		// 关闭连接
		if (null != mqttClient) {
			if (mqttClient.isConnected()) {
				try {
					mqttClient.disconnect();
					mqttClient.close();
				} catch (MqttException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				System.out.println("mqttClient is not connect");
			}
		} else {
			System.out.println("mqttClient is null");
		}
	}

	// 发布消息
	public void publishMessage(String pubTopic, String message, int qos) {
		if (null != mqttClient && mqttClient.isConnected()) {
			System.out.println("发布消息 : " + message);
			MqttMessage mqttMessage = new MqttMessage();
			mqttMessage.setQos(qos);
			try {
				mqttMessage.setPayload(message.getBytes("utf-8"));
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}

			MqttTopic topic = mqttClient.getTopic(pubTopic);

			if (null != topic) {
				try {
					MqttDeliveryToken publish = topic.publish(mqttMessage);
					if (!publish.isComplete()) {
						System.out.println("消息发布成功");
					}
				} catch (MqttException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		} else {
			reConnect();
		}

	}

	// 重新连接
	public void reConnect() {
		if (null != mqttClient) {
			if (!mqttClient.isConnected()) {
				if (null != mqttConnectOptions) {
					try {
						mqttClient.connect(mqttConnectOptions);
					} catch (MqttException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					System.out.println("mqttConnectOptions is null");
				}
			} else {
				System.out.println("mqttClient is null or connect");
			}
		} else {
			initMqttClient(clientId);
		}

	}

	// 订阅主题
	public void subTopic(String topic) {
		if (null != mqttClient && mqttClient.isConnected()) {
			try {
				mqttClient.subscribe(topic, 1);
			} catch (MqttException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			System.out.println("mqttClient is error");
		}
	}

	// 清空主题
	public void cleanTopic(String topic) {
		if (null != mqttClient && !mqttClient.isConnected()) {
			try {
				mqttClient.unsubscribe(topic);
			} catch (MqttException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			System.out.println("mqttClient is error");
		}
	}

	public static void main(String[] args) {
		String GATEWAY_TELEMETRY_TOPIC = "v1/gateway/telemetry";
		// 测试mqtt 发送
		String Host = "tcp://111.11.1.24:31883";
		String UserName = "ooYKWsZZXuGqDWkRYCPZ";
		String clientId = "tsit";

		MyMqttClient mClient = new MyMqttClient(Host, UserName, clientId);
		mClient.initMqttClient("test");
		String data = "{\"wan\": [{\"ts\": 1574428621855, \"values\": {\"204\":123}}]}";
		mClient.publishMessage(GATEWAY_TELEMETRY_TOPIC, data, 0);
	}
}
