package com.myzxingtest.android;

import android.util.Xml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;

/**
 * IO操作工具类
 * 
 * @author yynie
 * 
 */
public final class IOUtils {
	private static final String TAG = IOUtils.class.getName();

	private static final int BUFFER_SIZE = 1024; // 流转换的缓存大小
	private static final int CONNECT_TIMEOUT = 3000; // 从网络下载文件时的连接超时时间


	public static boolean writeToFile(File file, String text) {
		return writeToFile(file, text, Xml.Encoding.UTF_8.toString(), false);
	}

	public static boolean writeToFile(File file, String text, String encoding,
									  boolean append) {
		try {
			return writeToFile(file, text.getBytes(encoding), append);
		} catch (UnsupportedEncodingException e) {
			return false;
		}
	}

	public static boolean writeToFile(File file, byte[] buffer, boolean append) {
		FileOutputStream fos = null;
		try {
			if(file.exists()){
				file.delete();
			}
			if(!file.getParentFile().exists()){
				file.getParentFile().mkdirs();
			}

			fos = new FileOutputStream(file, append);
			fos.write(buffer);
			return true;
		} catch (Exception e) {
			return false;
		} finally {
			try {
				if (fos != null) {
					fos.close();
				}
			} catch (Exception e) {
			}
		}
	}

}
