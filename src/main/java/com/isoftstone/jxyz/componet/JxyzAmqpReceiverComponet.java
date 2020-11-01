package com.isoftstone.jxyz.componet;

import java.io.File;
import java.sql.SQLException;
import java.util.Date;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.drinkjava2.jsqlbox.DbContext;
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
						id, host, Utils.df.format(event_time), bucket, object, amqp_message, created_user,
						Utils.df.format(created_time));
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
		
		unzip(sourceFile, unzipObjectPath, id);
	}

	private void unzip(File sourceFile, String unzipObjectPath,String id) throws SQLException {
		DbContext ctx = DbContext.getGlobalDbContext();
		try {
			Date unzip_start_time = new Date();
			ctx.execute("update esb_log set unzip_start_time = ? where id = ? ", Utils.df.format(unzip_start_time), id);
			Utils.unzip(sourceFile, unzipObjectPath);
			Date unzip_end_time = new Date();
			ctx.execute("update esb_log set unzip_end_time = ? where id = ? ", Utils.df.format(unzip_end_time), id);
		} catch (ZipException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			ctx.execute("update esb_log set err_msg = ? where id = ? ", e.getMessage(), id);
		}
	}
}