package com.isoftstone.jxyz.componet;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.drinkjava2.jsqlbox.DB;
import com.github.drinkjava2.jsqlbox.DbContext;
import com.isoftstone.jxyz.service.JxyzService;
import com.isoftstone.jxyz.util.Utils;

import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.exception.ZipException;

@Component
@Slf4j
public class JxyzAmqpReceiverComponet {
	@Value("${jxyz.created_user}")
	private String created_user;

	@Value("${jxyz.minio_data_base_path}")
	private String minio_data_base_path;

	@Value("${jxyz.upzip_base_path}")
	private String upzip_base_path;

	@Value("${jxyz.default_year}")
	private String defaultYear;

	@Autowired
	public JxyzService jxyzService;

	@RabbitListener(queues = "jxyz-esb-queue")
	public void receiveJxyzEsbQueue(String amqp_message) throws SQLException {
		DbContext ctx = DbContext.getGlobalDbContext();
		JSONObject s3MessageJsb = JSONObject.parseObject(amqp_message);
		if (s3MessageJsb.containsKey("Records")) {
			JSONArray recordsJsa = s3MessageJsb.getJSONArray("Records");
			for (int i = 0; i < recordsJsa.size(); i++) {
				JSONObject recordJsb = recordsJsa.getJSONObject(i);
				JSONObject responseElementsJsb = recordJsb.getJSONObject("responseElements");
				String id = responseElementsJsb.getString("x-amz-request-id");
				JSONObject sourceJsb = recordJsb.getJSONObject("source");
				String host = sourceJsb.getString("host");
				Date event_time = recordJsb.getDate("eventTime");
				JSONObject s3Jsb = recordJsb.getJSONObject("s3");
				JSONObject bucketJsb = s3Jsb.getJSONObject("bucket");
				String bucket = bucketJsb.getString("name");
				JSONObject objectJsb = s3Jsb.getJSONObject("object");
				String object = objectJsb.getString("key");

				Date created_time = new Date();
				ctx.execute(
						"INSERT INTO esb_log(id,host,event_time,bucket,object,amqp_message,created_user,created_time)VALUES(?,?,?,?,?,?,?,?)",
						id, host, Utils.df().format(event_time), bucket, object, amqp_message, created_user,
						Utils.df().format(created_time));
				process(bucket, object, id);
			}
		}
		System.out.println("收到fanout的消息：" + amqp_message);
	}

	private void process(String bucket, String object, String id) throws SQLException {
		String sourcePath = minio_data_base_path + Utils.separator + bucket + Utils.separator + object;
		String unzipBucketPath = upzip_base_path + Utils.separator + bucket;

		File sourceFile = new File(sourcePath);

		if (!sourceFile.exists()) {
			log.error("文件路径错误,minio_data_base_path:{},bucket:{},object:{}", minio_data_base_path, bucket, object);
		}

		File bucketDir = new File(unzipBucketPath);
		if (!bucketDir.isDirectory()) {
			bucketDir.mkdir();
		}

		String unzipObjectPath = unzipBucketPath + Utils.separator + object;

		File ObjectDir = new File(unzipObjectPath);
		if (!ObjectDir.isDirectory()) {
			ObjectDir.mkdir();
		}

		log.info(">>开始解压文件：{},至：{}", sourcePath, unzipObjectPath);
		unzip(sourceFile, unzipObjectPath, id);
		log.info(">>结束解压文件：{},至：{}", sourcePath, unzipObjectPath);
		log.info(">>开始解析文件夹：{}", unzipObjectPath);
		String year = object.substring(0, 4);
		try {
			Long.parseLong(year);
		} catch (Exception e) {
			// TODO: handle exception
			year = defaultYear;
			log.error(">>解析年份失败，使用默认年份：{}", year);
		}
		parse(unzipObjectPath, id, year);
		log.info(">>结束解析文件夹：{}", unzipObjectPath);
		log.info(">>开始清除文件夹：{}", unzipObjectPath);
		Utils.delAllFile(ObjectDir);
		log.info(">>结束清除文件夹：{}", unzipObjectPath);
	}

	private void unzip(File sourceFile, String unzipObjectPath, String id) throws SQLException {
		DbContext ctx = DbContext.getGlobalDbContext();
		try {
			Date unzip_start_time = new Date();
			ctx.execute("update esb_log set unzip_start_time = ? where id = ? ", Utils.df().format(unzip_start_time), id);
			Utils.unzip(sourceFile, unzipObjectPath);
			Date unzip_end_time = new Date();
			ctx.execute("update esb_log set unzip_end_time = ? where id = ? ", Utils.df().format(unzip_end_time), id);
		} catch (ZipException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			ctx.execute("update esb_log set err_msg = ? where id = ? ", e.getMessage(), id);
		}
	}

	private void parse(String unzipObjectPath, String id, String year) throws SQLException {
		DbContext ctx = DbContext.getGlobalDbContext();
		log.info(">>开始执行扫描文件夹--{}--", unzipObjectPath);
		List<File> fileList = new ArrayList<File>();
		File esbDirectory = new File(unzipObjectPath);
		Utils.traverFile(esbDirectory, fileList);
		log.info(">>文件夹数量--{}--", fileList.size());
		List<Map<String, Object>> logMapList = ctx.qryMapList(
				"select files_path,files_name from sdi_jxyz_log where folder_name=?", DB.param(unzipObjectPath));
		log.info(">>获取往期--{}--日志", unzipObjectPath);
		log.info(">>往期日志--{}--条", unzipObjectPath);
		for (int i = 0; i < fileList.size(); i++) {
			File file = fileList.get(i);
			if (!file.getAbsolutePath().endsWith(".json")) {// 不是json文件，直接过滤
				continue;
			}

			boolean fileLogExist = false;
			for (int j = 0; j < logMapList.size(); j++) {
				Map<String, Object> logMap = logMapList.get(j);
				if (logMap.get("files_path").toString().equals(file.getAbsolutePath())) {
					fileLogExist = true;
					break;
				}
			}
			if (fileLogExist) {
				continue;
			}
			log.debug(">>文件夹--{}--,第--{}--个文件,文件路径--{}--", unzipObjectPath, i, file.getAbsolutePath());
			String jsonStr = "";
			JSONArray jsa = new JSONArray();
			try {
				jsonStr = FileUtils.readFileToString(file);
				jsa = JSONArray.parseArray(jsonStr);
				log.debug(">>文件夹--{}--,第--{}--个文件,文件路径--{}--,开始解析文件", unzipObjectPath, i, file.getAbsolutePath());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(">>文件夹--{}--,第--{}--个文件,文件路径--{}--,文件解析异常--{}--", unzipObjectPath, i, file.getAbsolutePath(),
						e.getMessage());
				e.printStackTrace();
			}
			try {
				ctx.nBatchBegin();
				log.debug(">>文件夹--{}--,第--{}--个文件,文件路径--{}--,开始解析,文件内容大小{}", unzipObjectPath, i, file.getAbsolutePath(),
						jsa.size());
				jsa.forEach(jsbObj -> {
					JSONObject jsb = (JSONObject) jsbObj;
					if (jsb.containsKey("bodys")) {
						JSONArray jsa1 = jsb.getJSONArray("bodys");
						jsa1.forEach(jsbObj1 -> {
							JSONObject jsb1 = (JSONObject) jsbObj1;
							if (jsb1.containsKey("pkp_waybill_more")) {
								JSONArray pkp_waybill_moreJsa = jsb1.getJSONArray("pkp_waybill_more");
								jxyzService.pkp_waybill_moreInsert(pkp_waybill_moreJsa, ctx, year);
								log.debug(">>解析对象--{}--", "pkp_waybill_more");
							}
							if (jsb1.containsKey("pkp_waybill_payment")) {
								JSONArray pkp_waybill_paymentJsa = jsb1.getJSONArray("pkp_waybill_payment");
								jxyzService.pkp_waybill_paymentInsert(pkp_waybill_paymentJsa, ctx, year);
								log.debug(">>解析对象--{}--", "pkp_waybill_payment");
							}
							if (jsb1.containsKey("pkp_waybill_fee")) {
								JSONArray pkp_waybill_feeJsa = jsb1.getJSONArray("pkp_waybill_fee");
								jxyzService.pkp_waybill_feeInsert(pkp_waybill_feeJsa, ctx, year);
								log.debug(">>解析对象--{}--", "pkp_waybill_fee");
							}
							if (jsb1.containsKey("pkp_waybill_biz")) {
								JSONObject pkp_waybill_bizJsb = jsb1.getJSONObject("pkp_waybill_biz");
								jxyzService.pkp_waybill_bizInsert(pkp_waybill_bizJsb, ctx, year);
								log.debug(">>解析对象--{}--", "pkp_waybill_biz");
							}
							if (jsb1.containsKey("pkp_waybill_base")) {
								JSONObject pkp_waybill_baseJsb = jsb1.getJSONObject("pkp_waybill_base");
								jxyzService.pkp_waybill_baseInsert(pkp_waybill_baseJsb, ctx, year);
								log.debug(">>解析对象--{}--", "pkp_waybill_base");
							}
							if (jsb1.containsKey("pkp_waybill_cargo")) {
								JSONArray pkp_waybill_cargoJsa = jsb1.getJSONArray("pkp_waybill_cargo");
								jxyzService.pkp_waybill_cargoInsert(pkp_waybill_cargoJsa, ctx, year);
								log.debug(">>解析对象--{}--", "pkp_waybill_cargo");
							}
							if (jsb1.containsKey("pkp_waybill_package")) {
								JSONArray pkp_waybill_packageJsa = jsb1.getJSONArray("pkp_waybill_package");
								jxyzService.pkp_waybill_packageInsert(pkp_waybill_packageJsa, ctx, year);
								log.debug(">>解析对象--{}--", "pkp_waybill_package");
							}
							if (jsb1.containsKey("pkp_waybill_bill")) {
								JSONArray pkp_waybill_billJsa = jsb1.getJSONArray("pkp_waybill_bill");
								jxyzService.pkp_waybill_billInsert(pkp_waybill_billJsa, ctx, year);
								log.debug(">>解析对象--{}--", "pkp_waybill_bill");
							}
						});
					}
				});
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
				log.error(">>执行过程错误路径--{}--", file.getAbsolutePath());
				log.error(">>执行过程错误信息--{}--", e.getMessage());
			} finally {
				// TODO: handle finally clause
				ctx.execute("INSERT INTO sdi_jxyz_log(files_path,files_name,folder_name,created_name)VALUES(?,?,?,?)",
						file.getAbsolutePath(), file.getName(), unzipObjectPath, "王小贱");
				try {
					ctx.nBatchEnd();
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
					log.error(">>执行提交错误路径--{}--", file.getAbsolutePath());
					log.error(">>执行提交错误信息--{}--", e.getMessage());
					ctx.execute(
							"INSERT INTO sdi_jxyz_log(files_path,files_name,folder_name,created_name,error_msg)VALUES(?,?,?,?,?)",
							file.getAbsolutePath(), file.getName(), unzipObjectPath, "王小贱", e.getMessage());
				}

			}

		}
		;

		log.info(">>>>>>>>结束执行>>>>>>>>>");
	}

}