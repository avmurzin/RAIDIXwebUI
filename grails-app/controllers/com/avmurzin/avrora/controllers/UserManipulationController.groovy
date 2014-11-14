package com.avmurzin.avrora.controllers

import com.avmurzin.avrora.aux.ContainerType
import com.avmurzin.avrora.db.Container
import com.avmurzin.avrora.global.UserRole

import org.apache.shiro.SecurityUtils
import org.apache.shiro.crypto.hash.Sha256Hash
import com.avmurzin.avrora.global.ReturnMessage
import com.avmurzin.avrora.sec.User
import com.avmurzin.avrora.system.*
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Управление пользователями (создание, удаление, назначение прав, квот и т.п.)
 * @author Andrei Murzin (http://avmurzin.com)
 *
 */
class UserManipulationController {

	def index() {
		render(contentType: "application/json") {
			result = false
			message = "Несуществующая операция"
		}
	}


	/**
	 * Установить квоту для пользователя uuid.
	 * Доступ: ROOT
	 * @return JSON
	 */
	def set_user_quota() {
		String username = params.username
		long maxquota

		LogUi.log("изменение квоты пользователя", "${username}: ${maxquota}")

		try {
			maxquota = Long.valueOf(params.maxquota).longValue()
		} catch (Exception e) {
			//e.printStackTrace()
			maxquota = 0;
			render(contentType: "application/json") {
				result = false
				message = "Квота указана неверно"
			}
			return
		}

		def user = User.findByUsername(username)

		if (user != null) {
			user.maxQuota = maxquota
			user.save(flush: true)
			render(contentType: "application/json") {
				result = true
				message = "Квота пользователя изменена"
			}
		} else {
			render(contentType: "application/json") {
				result = false
				message = "Пользователь не существует"
			}
		}
		//		//long maxquota = params.maxquota
		//		QuotaSet quotaSet;
		//
		//		def config = new ConfigSlurper().parse(new File('ConfigSlurper/avrora.groovy').toURI().toURL())
		//		//определить тип используемой FS (по настройкам)
		//
		//		switch (config.quota.fstype) {
		//			case "zfs":
		//			//println "zfs"
		//				quotaSet = ZfsQuotaSet.getInstance()
		//				break;
		//			case "xfs":
		//			//println "xfs"
		//				quotaSet = XfsQuotaSet.getInstance()
		//				break;
		//		}
		//
		//		if (SecurityUtils.subject.isPermitted("${UserRole.ROOT.toString()}") ) {
		//
		//			ReturnMessage returnMessage = quotaSet.setUserQuota(username, maxquota)
		//			if (returnMessage.getResult()) {
		//				render(contentType: "application/json") {
		//					result = true
		//					message = ""
		//				}
		//			} else {
		//				render(contentType: "application/json") {
		//					result = false
		//					message = returnMessage.getMessage()
		//				}
		//			}
		//		} else {
		//			render(contentType: "application/json") {
		//				result = false
		//				message = "Недостаточно прав"
		//			}
		//		}

	}

	/**
	 * Добавить права (в формате Apache Shiro)  пользователю ?username=&permission=.
	 * @return
	 */
	def add_permission() {
		String username = params.username;
		//String permission = params.permission;
		UserRole role = params.role;
		def permission = ""
		switch(role) {
			case UserRole.ROOT:
			permission = "*:*"
			break;
			case UserRole.WEBUSER:
			permission = "containerManipulation:*:*"
			break;
		}

		LogUi.log("добавление прав пользователя", "${username}: ${role.toString()}")

		def user = User.findByUsername(username)
		if(user == null) {
			render(contentType: "application/json") {
				result = false
				message = "Пользователя не существует"
			}
		} else {
			user.addToPermissions(permission)
			user.save(flush: true)
			render(contentType: "application/json") {
				result = true
				message = "Права добавлены"
			}
		}

	}

	/**
	 * Удалить права (в формате Apache Shiro) пользователю ?username=&permission=
	 * @return
	 */
	def del_permission() {
		String username = params.username;
		//String permission = params.permission;
		UserRole role = params.role;
		def permission = ""
		switch(role) {
			case UserRole.ROOT:
			permission = "*:*"
			break;
			case UserRole.WEBUSER:
			permission = "containerManipulation:*:*"
			break;
		}

		LogUi.log("удаление прав пользователя", "${username}: ${role.toString()}")

		def user = User.findByUsername(username)

		if(user == null) {
			render(contentType: "application/json") {
				result = false
				message = "Пользователя не существует"
			}
		} else {
			try {
				user.removeFromPermissions(permission)
				user.save(flush: true)
				render(contentType: "application/json") {
					result = true
					message = "Права удалены"
				}
			} catch (Exception e) {
				render(contentType: "application/json") {
					result = false
					message = "Права не удалены"
				}
			}

		}

	}

	//Добавить пользователя и пароль
	//?username=&password=&password2=
	def add_user() {
		String username = params.username;
		String password = params.password;
		String password2 = params.password2;

		if (!password.equals(password2)) {
			render(contentType: "application/json") {
				result = false
				message = "Пароли не совпадают"
			}
			return
		}

		def user = User.findByUsername(username)
		if (user != null) {
			if (user.passwordHash == 'f57a34f65dfdee584d10313738e8e4beb29b5d5c8b6d8c3f6cbceffb3b36444b') {
				render(contentType: "application/json") {
					result = false
					message = "Имя пользователя совпадаем с существующим доменным именем"
				}

			} else {
				render(contentType: "application/json") {
					result = false
					message = "Пользователь уже существует"
				}
			}
		} else {
			user = user = new User(username: "${username}", passwordHash: new Sha256Hash(password).toHex())
			user.save(flush: true)

			LogUi.log("добавление пользователя", "${username}}")

			render(contentType: "application/json") {
				result = true
				message = "Локальный пользователь успешно добавлен"
			}
		}

	}

	//Удалить пользователя username
	def del_user() {
		String username = params.username;
		def user = User.findByUsername(username)
		if (user == null) {
			render(contentType: "application/json") {
				result = false
				message = "Пользователя не существует"
			}
		} else {
			if (user.passwordHash == 'f57a34f65dfdee584d10313738e8e4beb29b5d5c8b6d8c3f6cbceffb3b36444b') {
				render(contentType: "application/json") {
					result = false
					message = "Невозможно удалить доменного пользователя"
				}
			} else {
				render(contentType: "application/json") {

					LogUi.log("удаление пользователя", "${username}}")

					user.delete(flush: true)
					result = true
					message = "Пользователь успешно удален"
				}
			}
		}
	}

	def change_password() {
		String username = params.username;
		String password = params.password;
		String password2 = params.password2;

		if (!password.equals(password2)) {
			render(contentType: "application/json") {
				result = false
				message = "Пароли не совпадают"
			}
			return
		}
		
		def user = User.findByUsername(username)
		if (user != null) {
			if (user.passwordHash == 'f57a34f65dfdee584d10313738e8e4beb29b5d5c8b6d8c3f6cbceffb3b36444b') {
				render(contentType: "application/json") {
					result = false
					message = "Нельзя изменить пароль доменного пользователя"
				}

			} else {
				user.passwordHash = new Sha256Hash(password).toHex()
				user.save(flush: true)

				LogUi.log("изменение пароля пользователя", "${username}}")

				render(contentType: "application/json") {
					result = true
					message = "Пароль изменен"
				}
			}
		} else {
			render(contentType: "application/json") {
				result = false
				message = "Пользователь не существует"
			}
		}
	}

	/**
	 * Искать пользователя
	 *	//?username=
	 */
	def find_user() {
		String username = params.username;
		String type = ""
		def users = User.findAllByUsernameLike("%${username}%")
		if (users != null) {
			render (contentType: "application/json") {
				userses = array {
					for (User user : users) {
						if (user.passwordHash == 'f57a34f65dfdee584d10313738e8e4beb29b5d5c8b6d8c3f6cbceffb3b36444b') {
							type = "доменный"
						} else { type = "локальный" }
						userse username: "${user.username}", quota: "${user.maxQuota / 1024 / 1024} Мб", type: "${type}"
					}
				}
			}
		} else {
			render (contentType: "application/json") {
				result = false
			}
		}
	}

		//Получить список пользователей веб-интерфейса с правами.
		def get_localusers() {
			def users = User.findAll()
			def st = ""
			render (contentType: "application/json") {
				userses = array {
					for (User user : users) {
						for(UserRole role : [UserRole.ROOT, UserRole.WEBUSER]) {

							switch(role) {
								case UserRole.ROOT:
								st = "*:*"
								break;
								case UserRole.WEBUSER:
								st = "containerManipulation:*:*"
								break;
							}
							def perm = user.permissions.find {it == "${st}"}
							//println perm
							if (perm != null) {
								userse username: "${user.username}", role: "${role}"
							}
							perm = null;
						}
					}
				}
			}
		}

		def get_smbstatus() {

			LogUi.log("получить сесии сервера samba", "")

			def config = new ConfigSlurper().parse(new File('ConfigSlurper/avrora.groovy').toURI().toURL())
			String pred = ""
			String out = ""
			String last = ""
			List<String> inList = ExecuteCommand.executeForList("sudo ${config.smb.status}")

			for (String st : inList) {

				last = st
				if (st.contains("----------")){
					out += "<hr><br><b>${pred}</b><br>"
					pred = st
				} else {
					out += "${pred}<br>"
					pred = st
				}
			}
			out += "${last}<br>"

			out = out.replace(' ', '&nbsp;')
			render out

		}

		def kill_smb() {
			long pid
			try {
				pid = Long.valueOf(params.pid).longValue()
			} catch (Exception e) {
				//e.printStackTrace()
				pid = 0;
			}
			def config = new ConfigSlurper().parse(new File('ConfigSlurper/avrora.groovy').toURI().toURL())
			String out = ExecuteCommand.execute("sudo ${config.smb.status}").getMessage()
			if (out.contains("${pid}")) {
				ExecuteCommand.execute("sudo ${config.smb.kill} ${pid.toString()}")

				LogUi.log("остановить сессию сервера samba", "${pid}")

				render(contentType: "application/json") {
					result = true
					message = "Процесс остановлен"
				}
			} else {
				render(contentType: "application/json") {
					result = false
					message = "Процесса не существует"
				}
			}

		}

		//Получить список правил в таблице filter, цепочка INPUT
		def list_iptables() {
			int num=0;
			String color=""
			def config = new ConfigSlurper().parse(new File('ConfigSlurper/avrora.groovy').toURI().toURL())

			List<String> inList = ExecuteCommand.executeForList("sudo ${config.iptables.lists}")
			render (contentType: "application/json") {
				rules = array {
					for (String st : inList) {

						if (st.matches(".*[\\d].*\$")) {
							if (st.matches(".*conn.*\$")) {
								color = "#CC99CC"
							} else {color = "#000000"}
							++num
							rule id: "${num}", text: "${st}", color: "${color}"
						}
					}
				}
			}
		}

		//Добавить блокирующее правило в iptables
		//?ipaddress=&mask=
		def add_iptables() {
			String ipaddress = params.ipaddress;
			String mask = params.mask;
			String type = params.type;

			String command;

			LogUi.log("добавить блокирующее правило iptables", "${ipaddress}/${mask}")

			def config = new ConfigSlurper().parse(new File('ConfigSlurper/avrora.groovy').toURI().toURL())

			if (type.equals("block")) {
				command = "sudo ${config.iptables.add} ${ipaddress} ${mask}"
			}
			if (type.equals("limit")) {
				String limit = params.limit;
				command = "sudo ${config.iptables.limit} ${ipaddress} ${mask} ${limit}"
			}


			if (ExecuteCommand.execute(command).getResult()) {
				render(contentType: "application/json") {
					result = true
					message = "Правило добавлено"
				}
			} else {
				render(contentType: "application/json") {
					result = false
					message = "Не выполнено"
				}
			}

		}

		//Удалить блокирующее правило numbers в iptables
		def del_iptables() {
			String number = params.number;

			LogUi.log("удалить блокирующее правило iptables", "")

			def config = new ConfigSlurper().parse(new File('ConfigSlurper/avrora.groovy').toURI().toURL())

			if (ExecuteCommand.execute("sudo ${config.iptables.del} ${number}").getResult()) {
				render(contentType: "application/json") {
					result = true
					message = "Правило удалено"
				}
			} else {
				render(contentType: "application/json") {
					result = false
					message = "Не выполнено"
				}
			}

		}

	}
