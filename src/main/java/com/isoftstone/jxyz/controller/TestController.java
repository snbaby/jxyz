package com.isoftstone.jxyz.controller;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONObject;
import com.github.drinkjava2.jsqlbox.DB;
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
					+ "FROM\r\n" + "sdi_jxyz_pkp_waybill_base_2020 t\r\n" + "WHERE t.biz_occur_date >= '"
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
					"    sdi_jxyz_pkp_waybill_base_2020 t\r\n" + 
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
	
	
	@GetMapping(value = "/post/org/c1")
	@ResponseBody
	public JSONObject postorgc1() throws SQLException {
		DbContext ctx = DbContext.getGlobalDbContext();
		
		List<Map<String, Object>> provinceListMap = ctx.qryMapList("select code,name from t_grid_m where level = 1");
		List<Map<String, Object>> cityListMap = ctx.qryMapList("select code,name from t_grid_m where level = 2");
		List<Map<String, Object>> countryListMap = ctx.qryMapList("select code,name from t_grid_m where level = 3");
		
		provinceListMap.forEach(provinceMap->{
			try {
				ctx.execute("update baby_post_org set sender_province_name = ? where sender_province_no = ?", provinceMap.get("name"),provinceMap.get("code"));
				ctx.execute("update baby_post_person set sender_province_name = ? where sender_province_no = ?", provinceMap.get("name"),provinceMap.get("code"));
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		
		cityListMap.forEach(cityMap->{
			try {
				ctx.execute("update baby_post_org set sender_city_name = ? where sender_city_no = ?", cityMap.get("name"),cityMap.get("code"));
				ctx.execute("update baby_post_person set sender_city_name = ? where sender_city_no = ?", cityMap.get("name"),cityMap.get("code"));
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		
		countryListMap.forEach(countryMap->{
			try {
				ctx.execute("update baby_post_org set sender_county_name = ? where sender_county_no = ?", countryMap.get("name"),countryMap.get("code"));
				ctx.execute("update baby_post_person set sender_county_name = ? where sender_county_no = ?", countryMap.get("name"),countryMap.get("code"));
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		
		
		JSONObject jsb = new JSONObject();
		jsb.put("a", "ok");
		return jsb;
	}
	
	@GetMapping(value = "/post/org/c2")
	@ResponseBody
	public JSONObject postorgc2() throws SQLException {
		DbContext ctx = DbContext.getGlobalDbContext();
		
		List<Map<String, Object>> postOrgMinListMap = ctx.qryMapList("SELECT post_org_no,sum(postage_total) as postage_total FROM baby_post_org group by post_org_no");
		
		System.out.println(postOrgMinListMap.size());
		
		postOrgMinListMap.forEach(postOrgMinMap->{
			try {
				
				String post_org_no = postOrgMinMap.get("post_org_no").toString();
				double postage_total = Double.parseDouble(postOrgMinMap.get("postage_total").toString());
				List<Map<String, Object>> postOrgListMap = ctx.qryMapList("SELECT * FROM baby_post_org where post_org_no = ? limit 1",DB.param(post_org_no));
				Map<String, Object> postOrgMap = postOrgListMap.get(0);
				
				String post_org_name = postOrgMap.get("post_org_name").toString();
				String sender_province_no = postOrgMap.get("sender_province_no").toString();
				String sender_province_name = postOrgMap.get("sender_province_name").toString();
				String sender_city_no = postOrgMap.get("sender_city_no").toString();
				String sender_city_name = postOrgMap.get("sender_city_name").toString();
				String sender_county_no = postOrgMap.get("sender_county_no").toString();
				String sender_county_name = postOrgMap.get("sender_county_name") == null ? "":postOrgMap.get("sender_county_name").toString();
				
				String id = UUID.randomUUID().toString();
				
				ctx.execute("INSERT INTO baby_post_org_pro(id,post_org_no,post_org_name,sender_province_no,sender_province_name,sender_city_no,sender_city_name,sender_county_no,sender_county_name,postage_total)VALUES(?,?,?,?,?,?,?,?,?,?)",id,post_org_no,post_org_name,sender_province_no,sender_province_name,sender_city_no,sender_city_name,sender_county_no,sender_county_name,postage_total);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		
		
		JSONObject jsb = new JSONObject();
		jsb.put("a", "ok");
		return jsb;
	}
	
	@GetMapping(value = "/post/person/c3")
	@ResponseBody
	public JSONObject postorgc3() throws SQLException {
		DbContext ctx = DbContext.getGlobalDbContext();
		
		List<Map<String, Object>> postPersonMinListMap = ctx.qryMapList("select post_person_no from baby_post_person group by post_person_no");
		
		postPersonMinListMap.forEach(postPersonMinMap->{
			try {
				
				String post_person_no = postPersonMinMap.get("post_person_no").toString();
				List<Map<String, Object>> postOrgListMap = ctx.qryMapList("SELECT * FROM baby_post_person where post_person_no = ? limit 1",DB.param(post_person_no));
				Map<String, Object> postPersonMap = postOrgListMap.get(0);
				
				String id = UUID.randomUUID().toString();
				String post_person_name = postPersonMap.get("post_person_name").toString();
				String post_org_no = postPersonMap.get("post_org_no").toString();
				String post_org_name = postPersonMap.get("post_org_name").toString();
				String sender_province_no = postPersonMap.get("sender_province_no").toString();
				String sender_province_name = postPersonMap.get("sender_province_name").toString();
				String sender_city_no = postPersonMap.get("sender_city_no").toString();
				String sender_city_name = postPersonMap.get("sender_city_name").toString();
				String sender_county_no = postPersonMap.get("sender_county_no").toString();
				String sender_county_name = postPersonMap.get("sender_county_name")==null?"":postPersonMap.get("sender_county_name").toString();
				
				ctx.execute("INSERT INTO baby_post_person_pro(id,post_person_no,post_person_name,post_org_no,post_org_name,sender_province_no,sender_province_name,sender_city_no,sender_city_name,sender_county_no,sender_county_name)VALUES(?,?,?,?,?,?,?,?,?,?,?)",id,post_person_no,post_person_name,post_org_no,post_org_name,sender_province_no,sender_province_name,sender_city_no,sender_city_name,sender_county_no,sender_county_name);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		
		
		JSONObject jsb = new JSONObject();
		jsb.put("a", "ok");
		return jsb;
	}
	
}
