package com.tsit.utils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class JsonUtils {
	
	// 读json 文件 为 JsonElement
	public static JsonElement readJsonElement(String jsonPath) {
		JsonElement object = null;
		try {
			JsonParser parser = new JsonParser(); // 创建JSON解析器
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(jsonPath), "UTF-8"), 1024); // 设置缓冲区,编码
			object = (JsonElement) parser.parse(in); // 创建JsonObject对象

			System.out.println("==================READ JSON END===================");

		} catch (JsonIOException e) {
			e.printStackTrace();
		} catch (JsonSyntaxException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			System.out.println("FileNotFoundException" + jsonPath);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return object;
	}

	// 读json 文件 为 JsonArray
	public static JsonArray readJsonArray(String jsonPath) {
		return readJsonElement(jsonPath).getAsJsonArray();
	}

	// 读json 文件 为 JsonObject
	public static JsonObject readJsonObject(String jsonPath) {
		return readJsonElement(jsonPath).getAsJsonObject();
	}

}
