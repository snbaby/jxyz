package com.isoftstone.jxyz.controller;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONObject;
import com.github.drinkjava2.jsqlbox.DbContext;

@Controller
public class TestController {

	@GetMapping(value = "/post/org")
	@ResponseBody
	public JSONObject postOrg() throws SQLException {
		DbContext ctx = DbContext.getGlobalDbContext();

		Calendar start = Calendar.getInstance();
		start.set(2020, 8 - 1, 1, 0, 0, 0);
		Long startTIme = start.getTimeInMillis();

		Calendar end = Calendar.getInstance();
		end.set(2020, 10 - 1, 31, 0, 0, 0);
		Long endTime = end.getTimeInMillis();

		Long oneDay = 1000 * 60 * 60 * 24l;
		Long time = startTIme;

		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		while (time <= endTime) {
			Date today = new Date(time);
			Date tomorrow = new Date(time + oneDay);

			String sql = "insert into baby_post_org SELECT uuid() as id, t.post_org_no,t.post_org_name,t.sender_province_no,t.sender_city_no,t.sender_county_no,sum(postage_total) as postage_total,'"+df.format(today)+"' as create_date\r\n"
					+ "FROM\r\n" + "sdi_jxyz_pkp_waybill_base_2019 t\r\n" + "WHERE t.biz_occur_date >= '"
					+ df.format(today) + "'\r\n" + "AND t.biz_occur_date <= '" + df.format(tomorrow) + "'\r\n"
					+ "AND t.sender_province_no = '360000'\r\n"
					+ "GROUP BY t.post_org_no,t.post_org_name,t.sender_province_no,t.sender_city_no,t.sender_county_no;";
			ctx.execute(sql);
			time += oneDay;
		}

		JSONObject jsb = new JSONObject();
		jsb.put("a", "ok");
		return jsb;
	}
	
	@GetMapping(value = "/post/person")
	@ResponseBody
	public JSONObject postPerson() throws SQLException {
		DbContext ctx = DbContext.getGlobalDbContext();

		Calendar start = Calendar.getInstance();
		start.set(2020, 8 - 1, 1, 0, 0, 0);
		Long startTIme = start.getTimeInMillis();

		Calendar end = Calendar.getInstance();
		end.set(2020, 10 - 1, 31, 0, 0, 0);
		Long endTime = end.getTimeInMillis();

		Long oneDay = 1000 * 60 * 60 * 24l;
		Long time = startTIme;

		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		while (time <= endTime) {
			Date today = new Date(time);
			Date tomorrow = new Date(time + oneDay);

			String sql = "insert into baby_post_person SELECT \r\n" + 
					"	uuid() as id,\r\n" + 
					"    t.post_person_no,\r\n" + 
					"    t.post_person_name,\r\n" + 
					"    t.post_org_no,\r\n" + 
					"    t.post_org_name,\r\n" + 
					"    t.sender_province_no,\r\n" + 
					"    t.sender_city_no,\r\n" + 
					"    t.sender_county_no,\r\n" + 
					"    '"+df.format(today)+"' as create_date\r\n" + 
					"FROM\r\n" + 
					"    sdi_jxyz_pkp_waybill_base_2019 t\r\n" + 
					"WHERE\r\n" + 
					"    t.biz_occur_date >= '"+df.format(today)+"'\r\n" + 
					"        AND t.biz_occur_date <= '"+df.format(tomorrow)+"'\r\n" + 
					"        AND t.sender_province_no = '360000'\r\n" + 
					"GROUP BY  t.post_person_no , t.post_person_name , t.post_org_no , t.post_org_name , t.sender_province_no , t.sender_city_no , t.sender_county_no";
			ctx.execute(sql);
			time += oneDay;
		}

		JSONObject jsb = new JSONObject();
		jsb.put("a", "ok");
		return jsb;
	}
}
