package com.isoftstone.jxyz.controller;

import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.github.drinkjava2.jsqlbox.DB;
import com.github.drinkjava2.jsqlbox.DbContext;
import com.isoftstone.jxyz.util.Utils;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("api")
@Slf4j
public class JxyzController {
	@Value("${jxyz.default_year}")
	private String defaultYear;

	@PostMapping(value = "receive/trajectory")
	@ResponseBody
	public ResponseEntity<JSONObject> trajectory(@RequestBody JSONObject jsb) {
		DbContext ctx = DbContext.getGlobalDbContext();
		log.debug("轨迹数据：导入数据：{}", jsb.toString());
		log.info("轨迹数据：开始导入数据");

		ctx.exe("INSERT INTO sdi_jxyz_pkp_trace_message_" + defaultYear + "(",
				DB.notNull("trace_no,", jsb.getString("traceNo")),
				DB.notNull("op_time,", Utils.translateDateStr(jsb.get("opTime"))),
				DB.notNull("op_code,", jsb.getString("opCode")), DB.notNull("op_name,", jsb.getString("opName")),
				DB.notNull("op_desc,", jsb.getString("opDesc")),
				DB.notNull("op_org_prov_name,", jsb.getString("opOrgProvName")),
				DB.notNull("op_org_city,", jsb.getString("oporgcity")),
				DB.notNull("op_org_code,", jsb.getString("oporgcode")),
				DB.notNull("op_org_name,", jsb.getString("oporgname")),
				DB.notNull("op_erator_no,", jsb.getString("operatorno")),
				DB.notNull("op_erator_name,", jsb.getString("operatorname")),
				DB.notNull("created_date,", Utils.df().format(new Date())), DB.notNull("created_by", "王小贱"), ")",
				DB.valuesQuestions());

		log.info("轨迹数据：结束导入数据");

		JSONObject resJsb = new JSONObject();
		resJsb.put("code", 0);
		resJsb.put("msg", "success");
		resJsb.put("data", "");
		return new ResponseEntity<JSONObject>(resJsb, HttpStatus.OK);
	}
}
