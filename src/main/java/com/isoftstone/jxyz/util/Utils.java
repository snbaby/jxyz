package com.isoftstone.jxyz.util;

import java.io.File;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

public class Utils {
	public static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public static final String separator = File.separator;

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
}
