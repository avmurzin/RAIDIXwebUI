package com.avmurzin.avrora.system

import com.avmurzin.avrora.db.WebUiLog
import org.apache.shiro.SecurityUtils

class LogUi {
	public static log(String message, String description) {
		String ipAddress = SecurityUtils.getSubject().getSession().getHost()
		if (ipAddress.equals("")) {
			ipAddress = ""
		}
		String username = SecurityUtils.subject.getPrincipal().toString()
		Calendar calendar = new GregorianCalendar()
		long timestamp = calendar.getTimeInMillis() / 1000

		def log = new WebUiLog()
		log.username = username
		log.ipAddress = ipAddress
		log.operation = message
		log.timestamp = timestamp
		log.description = description
		log.save(flush: true)
	}
}
