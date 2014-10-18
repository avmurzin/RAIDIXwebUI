package com.avmurzin.avrora.controllers

import com.avmurzin.avrora.aux.ContainerType
import com.avmurzin.avrora.db.Container
import com.avmurzin.avrora.global.UserRole

import org.apache.shiro.SecurityUtils
import com.avmurzin.avrora.global.ReturnMessage
import com.avmurzin.avrora.sec.User
import com.avmurzin.avrora.system.*

/**
 * Управление пользователями (создание, удаление, назначение прав, квот и т.п.)
 * @author Andrei Murzin (http://avmurzin.com)
 *
 */
class UserManipulationController {

	def index() { }

	/**
	 * Установить квоту для пользователя uuid.
	 * Доступ: ROOT
	 * @return JSON
	 */
	def set_user_quota() {
		String username = params.username
		long maxquota
		try {
			maxquota = Long.valueOf(params.maxquota).longValue()
		} catch (Exception e) {
			//e.printStackTrace()
			maxquota = 0;
		}

		//long maxquota = params.maxquota
		QuotaSet quotaSet;

		def config = new ConfigSlurper().parse(new File('ConfigSlurper/avrora.groovy').toURI().toURL())
		//определить тип используемой FS (по настройкам)

		switch (config.quota.fstype) {
			case "zfs":
				//println "zfs"
				quotaSet = ZfsQuotaSet.getInstance()
				break;
			case "xfs":
				//println "xfs"
				quotaSet = XfsQuotaSet.getInstance()
				break;
		}

		if (SecurityUtils.subject.isPermitted("${UserRole.ROOT.toString()}") ) {

			ReturnMessage returnMessage = quotaSet.setUserQuota(username, maxquota)
			if (returnMessage.getResult()) {
				render(contentType: "application/json") {
					result = true
					message = ""
				}
			} else {
				render(contentType: "application/json") {
					result = false
					message = returnMessage.getMessage()
				}
			}
		} else {
			render(contentType: "application/json") {
				result = false
				message = "Недостаточно прав"
			}
		}

	}
}
