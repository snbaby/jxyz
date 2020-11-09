package com.isoftstone.jxyz.util;

import java.io.File;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.github.drinkjava2.jdbpro.SqlItem;
import com.github.drinkjava2.jsqlbox.DB;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

public class Utils {
	public static final String separator = File.separator;

	public static DateFormat df() {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	}

	public static String translateDateStr(Object obj) {
		if (obj == null) {
			return null;
		}

		try {
			return df().format(df().parse(obj.toString()));
		} catch (Exception e) {
			// TODO: handle exception
			return null;
		}
	}

	public static void unzip(File zipFile, String dest) throws ZipException {
		ZipFile zFile = new ZipFile(zipFile); // 首先创建ZipFile指向磁盘上的.zip文件
		zFile.setCharset(Charset.forName("GBK")); // 设置文件名编码，在GBK系统中需要设置
		if (!zFile.isValidZipFile()) { // 验证.zip文件是否合法，包括文件是否存在、是否为zip文件、是否被损坏等
			throw new ZipException("压缩文件不合法,可能被损坏.");
		}
		File destDir = new File(dest); // 解压目录
		if (destDir.isDirectory() && !destDir.exists()) {
			destDir.mkdir();
		}
		zFile.extractAll(dest); // 将文件抽出到解压目录(解压)
	}

	public static void consStr(List<SqlItem> sqlItemList, JSONObject jsb, String name) {
		if (jsb == null) {
			return;
		}
		Object obj = jsb.get(name);
		if (obj == null) {
			return;
		}

		if (StringUtils.isBlank(obj.toString())) {
			return;
		}

		try {
			sqlItemList.add(DB.notNull(name + ",", obj.toString()));
		} catch (Exception e) {
			// TODO: handle exception
			// 异常直接丢弃，不存入数据库
		}
	}

	public static void consInteger(List<SqlItem> sqlItemList, JSONObject jsb, String name) {
		if (jsb == null) {
			return;
		}
		Object obj = jsb.get(name);
		if (obj == null) {
			return;
		}

		try {
			if (StringUtils.isNotBlank(obj.toString())) {
				sqlItemList.add(DB.notNull(name + ",", Long.parseLong(obj.toString())));
			}
		} catch (Exception e) {
			// TODO: handle exception
			// 异常直接丢弃，不存入数据库
		}
	}

	public static void consDecimal(List<SqlItem> sqlItemList, JSONObject jsb, String name) {
		if (jsb == null) {
			return;
		}
		Object obj = jsb.get(name);
		if (obj == null) {
			return;
		}
		try {
			sqlItemList.add(DB.notNull(name + ",", Double.parseDouble(obj.toString())));
		} catch (Exception e) {
			// TODO: handle exception
			// 异常直接丢弃，不存入数据库
		}
	}

	public static void consDate(List<SqlItem> sqlItemList, JSONObject jsb, String name) {
		if (jsb == null) {
			return;
		}
		Object obj = jsb.get(name);
		if (obj == null) {
			return;
		}
		try {
			sqlItemList.add(DB.notNull(name + ",", df().format(df().parse(obj.toString()))));
		} catch (Exception e) {
			// TODO: handle exception
			// 异常直接丢弃，不存入数据库
		}
	}

	public static void traverFile(File file, List<File> fileList) {
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			if(files !=null) {
				for (File tempFile : files) {
					traverFile(tempFile, fileList);
				}
			}
		} else {
			fileList.add(file);
		}
	}

	/**
	 * 删除文件或文件夹
	 * 
	 * @param directory
	 */
	public static void delAllFile(File directory) {
		if (!directory.isDirectory()) {
			directory.delete();
		} else {
			File[] files = directory.listFiles();

			// 空文件夹
			if (files == null || files.length == 0) {
				directory.delete();
				return;
			}

			// 删除子文件夹和子文件
			for (File file : files) {
				if (file.isDirectory()) {
					delAllFile(file);
				} else {
					file.delete();
				}
			}

			// 删除文件夹本身
			directory.delete();
		}
	}

}
