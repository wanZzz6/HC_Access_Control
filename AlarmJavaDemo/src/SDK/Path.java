package sdk;

public class Path {
	public static String PATH = "";
	static {
		String pathString = HCNetSDK.class.getResource("/").getPath();
		int index = pathString.indexOf("AlarmJavaDemo");
		PATH = pathString.substring(0, index + 14) + "dll/HCNetSDK.dll";

	}

	public static void main(String[] args) {
		System.out.println(PATH);
	}
}
