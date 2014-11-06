package com.avmurzin.avrora.controllers

import com.avmurzin.avrora.ui.UiContainerTree
import com.avmurzin.avrora.aux.ContainerType
import com.avmurzin.avrora.db.Container
import com.avmurzin.avrora.db.SambaLog
import com.avmurzin.avrora.global.ShareType
import com.avmurzin.avrora.global.UserRole

import org.apache.naming.resources.BaseDirContext;
import org.apache.shiro.SecurityUtils

import com.avmurzin.avrora.sec.User
import com.avmurzin.avrora.system.*

/**
 * Управление контейнерами (получение дерева, добавление/удаление/
 * редактирование контейнера и т.п.).
 * @author Andrei Murzin (http://avmurzin.com)
 *
 */
class ContainerManipulationController {

	def index() {
		//TODO: редиректить на процедуру создания базового интерфейса.
		redirect(url: "/index.html")
	}

	/**
	 * Получить древовидную структуру контейнеров (создается и хранится
	 * в singleton-е UiContainerTree).
	 * @return JSON - данные для создания дерева id, value
	 */
	def get_tree() {
		if (SecurityUtils.subject.isPermitted("containerManipulation:get_tree")) {
			UiContainerTree tree1 = UiContainerTree.getInstance();
			tree1.refreshTree();
			render(contentType: "application/json") {
				tree1.getTree();
			}
		} else {
			render(contentType: "application/json") {
				result = false
				message = "Недостаточно прав"
			}
		}
	}

	/**
	 * Создать новый контейнер.
	 * Текущий пользователь получает право OWNER на созданный контейнер
	 * Пользователи родительского никаких прав на дочерние не получают.
	 * +Доступ: ${parentuuid}:OWNER || ${parentuuid}:ADMIN
	 * @return JSON - UUID идентификатор созданного контейнера.
	 */
	def new_container() {
		String parentuuid = params.parentuuid;
		String name = params.name;
		String description = params.description;

		if (description.equals('')) {
			description = "без описания"
		}

		if (name.equals('')) {
			name = "без_имени"
		}

		if (SecurityUtils.subject.isPermitted("${parentuuid}:${UserRole.OWNER.toString()}") ||
		SecurityUtils.subject.isPermitted("${parentuuid}:${UserRole.ADMIN.toString()}")) {
			UiContainerTree tree1 = UiContainerTree.getInstance();

			Container container = tree1.getNewContainer(parentuuid, name, description, SecurityUtils.subject.getPrincipal().toString())
			if (container == null) {
				render(contentType: "application/json") {
					result = false
					message = "Невозможно создать контейнер в данном месте"
				}
			} else {
				render(contentType: "application/json") {
					result = true
					uuid = container.uuid.toString()
					value = container.name
					image = container.type.toString()
				}
			}
		} else {
			render(contentType: "application/json") {
				result = false
				message = "Недостаточно прав"
			}
		}
	}


	/**
	 * Удалить контейнер uuid. Одновременно удаляются пользователи контейнера
	 * и их права на удаленный контейнер.
	 * +Доступ: ${uuid}:OWNER 
	 * @return JSON result = true|false и при успехе uuid удаленного контейнера.
	 */
	def del_container() {
		//TODO: при удалении контейнера следует освобождать квоту
		String cuuid = params.uuid;
		if (SecurityUtils.subject.isPermitted("${cuuid}:${UserRole.OWNER.toString()}")) {
			UiContainerTree tree1 = UiContainerTree.getInstance();
			if (tree1.delContainer(UUID.fromString(cuuid))) {
				render(contentType: "application/json") {
					result = true
					uuid = cuuid
				}
			} else {
				render(contentType: "application/json") {
					result = false
					message = "Контейнер содержит вложенные элементы"
				}
			}
		} else {
			render(contentType: "application/json") {
				result = false
				message = "Недостаточно прав"
			}
		}
	}

	/**
	 * Получить свойства контейнера.
	 * @return
	 */
	def container_properties() {
		String cuuid = params.uuid;

		def folder = Container.findByUuid(UUID.fromString(cuuid));

		render(contentType: "application/json") {
			uuid = folder.uuid.toString()
			name = folder.name
			type = folder.type.toString()
			description = folder.description
			maxquota = folder.maxQuota
			freequota = folder.freeQuota
		}
	}

	/**
	 * Изменить свойства контейнера uuid.
	 * ?name=&description=&maxquota=
	 * +Доступ: ${uuid}:OWNER
	 * @return
	 */
	def change_container_properties() {
		String cuuid = params.uuid;
		String cname = params.name;
		String cdescription = params.description;
		long cmaxquota = Long.valueOf(params.maxquota).longValue();

		if (cdescription.equals('')) {
			cdescription = "без описания"
		}

		if (cname.equals('')) {
			cname = "без_имени"
		}

		if (SecurityUtils.subject.isPermitted("${cuuid}:${UserRole.OWNER.toString()}")) {
			UiContainerTree tree1 = UiContainerTree.getInstance();
			Container container = tree1.changeContainer(UUID.fromString(cuuid),
					cname, cdescription, cmaxquota)
			if (container != null) {
				render(contentType: "application/json") {
					uuid = container.uuid.toString()
					name = container.name
					type = container.type.toString()
					description = container.description
					maxquota = container.maxQuota
					freequota = container.freeQuota
				}

			}
		} else {
			render(contentType: "application/json") {
				result = false
				message = "Недостаточно прав"
			}
		}
	}

	/**
	 * Добавить пользователя контейнера uuid с указанием его роли
	 * ?username=&role=OWNER|ADMIN|MANAGER|ROUSER|RWUSER
	 * права добавляются в виде "${uuid}:${UserRole}"
	 * +Доступ: ${uuid}:OWNER
	 * @return
	 */
	def add_container_user() {
		String cuuid = params.uuid;
		String username = params.username;
		UserRole role = params.role;
		UiContainerTree tree1 = UiContainerTree.getInstance();

		if (SecurityUtils.subject.isPermitted("${cuuid}:${UserRole.OWNER.toString()}")) {
			def container = Container.findByUuid(UUID.fromString(cuuid))
			def user = User.findByUsername(username)
			if ((container != null) && (user != null)) {
				container.addToUsers(user)
				container.save(flush: true)
				user.addToPermissions("${cuuid}:${role.toString()}")
				user.save(flush: true)
				tree1.refreshShareConfig(container)
				render(contentType: "application/json") {
					result = true
					uuid = container.uuid.toString()
					//message = "Недостаточно прав"
				}

			} else {
				render(contentType: "application/json") {
					result = false
					message = "Пользователя или контейнера не существует"
				}
			}
		} else {
			render(contentType: "application/json") {
				result = false
				message = "Недостаточно прав"
			}
		}
	}

	/**
	 * Удалить пользователя контейнера uuid с удалением всех(!) его прав на контейнер.
	 * ?username=
	 * Удаляются все права из user_permissions, содержащие uuid
	 * +Доступ: ${uuid}:OWNER
	 * @return
	 */
	def del_container_user() {
		String uuid = params.uuid;
		String username = params.username;
		UiContainerTree tree1 = UiContainerTree.getInstance();
		//TODO: если удалить себя и списка пользователей, то оппа...
		if (SecurityUtils.subject.isPermitted("${uuid}:${UserRole.OWNER.toString()}")) {
			def container = Container.findByUuid(UUID.fromString(uuid))
			def user = User.findByUsername(username)
			if ((container != null) && (user != null)) {
				container.removeFromUsers(user)
				container.save(flush: true)
				Collection perms = new ArrayList<String>()
				perms = user.permissions.findAll {it.contains(uuid)}
				for (String perm : perms) {
					//println(perm)
					user.removeFromPermissions(perm)
					user.save(flush: true)
				}
				tree1.refreshShareConfig(container)
				render(contentType: "application/json") {
					result = true
					//message = "Недостаточно прав"
				}
			} else {
				render(contentType: "application/json") {
					result = false
					message = "Контейнер или пользователь не существуют"
				}
			}
		} else {
			render(contentType: "application/json") {
				result = false
				message = "Недостаточно прав"
			}
		}
	}

	/**
	 * Удалить указанное право пользователя контейнера uuid.
	 * ?username=&role=OWNER|ADMIN|MANAGER|ROUSER|RWUSER
	 * Удаляются все права из user_permissions, содержащие uuid
	 * +Доступ: ${uuid}:OWNER
	 * @return
	 */
	def del_container_user_role() {
		String uuid = params.uuid;
		String username = params.username;
		UserRole role = params.role;
		UiContainerTree tree1 = UiContainerTree.getInstance();
		//TODO: если удалить себя и списка пользователей, то оппа...
		if (SecurityUtils.subject.isPermitted("${uuid}:${UserRole.OWNER.toString()}")) {
			def container = Container.findByUuid(UUID.fromString(uuid))
			def user = User.findByUsername(username)
			if ((container != null) && (user != null)) {
				//container.removeFromUsers(user)
				//container.save(flush: true)
				Collection perms = new ArrayList<String>()
				perms = user.permissions.findAll {it == "${uuid}:${role.toString()}"}
				for (String perm : perms) {
					//println(perm)
					user.removeFromPermissions(perm)
					user.save(flush: true)
				}
				tree1.refreshShareConfig(container)
				render(contentType: "application/json") {
					result = true
					//message = "Недостаточно прав"
				}
			} else {
				render(contentType: "application/json") {
					result = false
					message = "Контейнер или пользователь не существуют"
				}
			}
		} else {
			render(contentType: "application/json") {
				result = false
				message = "Недостаточно прав"
			}
		}
	}

	/**
	 * Получить список всех пользователей контейнера uuid и их ролей.
	 * @return - JSON вида {"user1":"OWNER","user2":"MANAGER"}
	 */
	//	def get_container_users() {
	//		String uuid = params.uuid;
	//
	//		def container = Container.findByUuid(UUID.fromString(uuid))
	//		def out = [:]
	//		if (container != null) {
	//			def users = container.users.findAll()
	//			//println users
	//			for (User user : users) {
	//				for(UserRole role : UserRole.values()) {
	//
	//					def perm = user.permissions.find {it == "${uuid}:${role.toString()}"}
	//					println perm
	//					if (perm != null) {
	//						out.put("${user.username}", "${role}")
	//					}
	//					perm = null;
	//				}
	//			}
	//		}
	//		def keyset = out.keySet()
	//		render (contentType: "application/json") {
	//			//keyset
	//
	//
	//			userses = array {
	//			for (k in keyset) {
	//				userse username: k, role: out.get(k)
	//			}
	//			}
	//			//out
	//		}
	//	}
	def get_container_users() {
		String uuid = params.uuid;

		def container = Container.findByUuid(UUID.fromString(uuid))
		def out = [:]
		if (container != null) {
			def users = container.users.findAll()
			render (contentType: "application/json") {
				userses = array {
					for (User user : users) {
						for(UserRole role : UserRole.values()) {

							def perm = user.permissions.find {it == "${uuid}:${role.toString()}"}
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
	}

	/**
	 * Создать сетевой ресурс (контейнер типа SHARE_*) внутри parentuuid
	 * ?name=&description=&sharetype=SMB|FTP..
	 * Текущий пользователь получает право OWNER на созданный контейнер
	 * Пользователи родительского никаких прав на дочерние не получают.
	 * +Доступ: ${parentuuid}:OWNER || ${parentuuid}:ADMIN || ${parentuuid}:MANAGER
	 * @return JSON - UUID идентификатор созданного контейнера.
	 */
	def share() {
		String parentuuid = params.parentuuid;
		String cname = params.name;
		String description = params.description;
		ContainerType ctype = params.sharetype;

		if (description.equals('')) {
			description = "без описания"
		}

		if (cname.equals('')) {
			cname = "без_имени"
		}

		if (SecurityUtils.subject.isPermitted("${parentuuid}:${UserRole.OWNER.toString()}") ||
		SecurityUtils.subject.isPermitted("${parentuuid}:${UserRole.ADMIN.toString()}") ||
		SecurityUtils.subject.isPermitted("${parentuuid}:${UserRole.MANAGER.toString()}"))
		{
			UiContainerTree tree1 = UiContainerTree.getInstance();

			Container container = tree1.getNewShare(parentuuid, cname,
					description, SecurityUtils.subject.getPrincipal().toString(),
					ctype)

			if (container != null) {
				render(contentType: "application/json") {
					uuid = container.uuid.toString()
					name = container.name
					type = container.type.toString()
					description = container.description
					maxquota = container.maxQuota
					freequota = container.freeQuota
				}
			} else {
				render(contentType: "application/json") {
					result = false
					message = "Невозможно создать ресурс. Обратитесь к администратору"
				}
			}


		} else {
			render(contentType: "application/json") {
				result = false
				message = "Недостаточно прав"
			}
		}
	}


	/**
	 * Временное закрытие шары uuid (удаляется конфиг, но не удаляются данные)
	 * Тип шары определяется по типу контейнера (SHARE_*).
	 * +Доступ: ${parentuuid}:OWNER || ${parentuuid}:ADMIN || ${parentuuid}:MANAGER
	 * @return JSON - закрытый контейнер
	 */
	def close_share() {
		String cuuid = params.uuid;
		UiContainerTree tree1 = UiContainerTree.getInstance();
		def container = Container.findByUuid(UUID.fromString(cuuid))
		if (SecurityUtils.subject.isPermitted("${cuuid}:${UserRole.OWNER.toString()}") ||
		SecurityUtils.subject.isPermitted("${cuuid}:${UserRole.ADMIN.toString()}") ||
		SecurityUtils.subject.isPermitted("${cuuid}:${UserRole.MANAGER.toString()}"))
		{
			if (tree1.closeShare(UUID.fromString(cuuid)) && (container != null)) {
				render(contentType: "application/json") {
					result = true
					uuid = container.uuid.toString()
					value = container.name
					image = container.type.toString()
				}
			} else {
				render(contentType: "application/json") {
					result = false
					message = "Невозможно закрыть ресурс. Обратитесь к администратору"
				}
			}
		} else {
			render(contentType: "application/json") {
				result = false
				message = "Недостаточно прав"
			}
		}
	}

	/**
	 * Открыть ранее созданную шару.
	 * Тип шары определяется по типу контейнера (SHARE_*).
	 * +Доступ: ${parentuuid}:OWNER || ${parentuuid}:ADMIN || ${parentuuid}:MANAGER
	 * @return JSON - открытый контейнер
	 */
	def open_share() {
		String cuuid = params.uuid;

		if (SecurityUtils.subject.isPermitted("${cuuid}:${UserRole.OWNER.toString()}") ||
		SecurityUtils.subject.isPermitted("${cuuid}:${UserRole.ADMIN.toString()}") ||
		SecurityUtils.subject.isPermitted("${cuuid}:${UserRole.MANAGER.toString()}"))
		{
			UiContainerTree tree1 = UiContainerTree.getInstance();

			//			Container container = tree1.getNewShare(parentuuid, cname,
			//					description, SecurityUtils.subject.getPrincipal().toString(),
			//					ctype)


			def container = Container.findByUuid(UUID.fromString(cuuid))

			def res = tree1.openShare(UUID.fromString(cuuid))

			if ((container != null) && res) {
				render(contentType: "application/json") {
					result = true
					uuid = container.uuid.toString()
					value = container.name
					image = container.type.toString()
				}
			} else {
				render(contentType: "application/json") {
					result = false
					message = "Невозможно создать ресурс. Обратитесь к администратору"
				}
			}


		} else {
			render(contentType: "application/json") {
				result = false
				message = "Недостаточно прав"
			}
		}
	}

	/**
	 * Получить список файлов и каталогов. Ограничение - имя должно включать
	 * имя каталога config.smb.sharefolder
	 * ?dir=
	 * +Доступ: containerManipulation:*:*
	 * @return
	 */
	def get_filelist() {
		String folder = params.dir;
//		if (SecurityUtils.subject.isPermitted("${cuuid}:${UserRole.OWNER.toString()}") ||
//		SecurityUtils.subject.isPermitted("${cuuid}:${UserRole.ADMIN.toString()}") ||
//		SecurityUtils.subject.isPermitted("${cuuid}:${UserRole.MANAGER.toString()}"))
//		{
		def config = new ConfigSlurper().parse(new File('ConfigSlurper/avrora.groovy').toURI().toURL())
		if (!folder.contains("${config.smb.sharefolder}") || folder == "${config.smb.sharefolder}") {
			render(contentType: "application/json") {
				result = false
				message = "Недопустимое имя каталога"
				}
			} else {
			def baseDir = new File(folder)
			def files = []
			def type = ""
			if (baseDir.isDirectory()) {
			baseDir.eachFileMatch(~/^.*$/) { files << it }
			
			render(contentType: "application/json") {
				items = array {
					item name: "..", type: "directory", path: "${baseDir.getParentFile().getAbsolutePath()}"
					for (File file : files) {
						if (file.isDirectory()) {
							type = "directory"
						} else {
							type = "file"
						}
						item name: "${file.name}", type: "${type}", path: "${file.absolutePath}"
					}
				}
			}
			} else {
			render(contentType: "application/json") {
				result = false
				message = "Объект не является каталогом"
			}
			}
			}
//		} else {
//			render(contentType: "application/json") {
//				result = false
//				message = "Недостаточно прав"
//			}
//		}
	}
	
	def get_sharefilelist() {
		String cuuid = params.uuid;
		if (SecurityUtils.subject.isPermitted("${cuuid}:${UserRole.OWNER.toString()}") ||
		SecurityUtils.subject.isPermitted("${cuuid}:${UserRole.ADMIN.toString()}") ||
		SecurityUtils.subject.isPermitted("${cuuid}:${UserRole.MANAGER.toString()}"))
		{
			def sharepath = Container.findByUuid(UUID.fromString(cuuid)).sharepath
			params.dir = sharepath
			redirect(action: "get_filelist", params: params)
		} else {
			render(contentType: "application/json") {
				result = false
				message = "Недостаточно прав"
			}
		}
	}
	
	/**
	 * Получить данные из лога sambalog.
	 * ?ipAddress=&username=&filePath=
	 * Доступ: ROOT
	 * @return
	 */
	def get_sambalog() {
		//TODO: добавить ограничение прав.
		String ipAddress = params.ipAddress;
		String username = params.username;
		String filePath = params.filePath;
		Calendar calendar = new GregorianCalendar()
		def locale =  new Locale("ru", "RU")
		def records = SambaLog.findAllByIpAddressLikeAndUsernameLikeAndFilePathLike("%${ipAddress}%", "%${username}%", "%${filePath}%");
			render(contentType: "application/json") {
				items = array {
					for (SambaLog record : records) {
						calendar.setTimeInMillis(record.timestamp * 1000)
						calendar.computeFields()
						item username: "${record.username}", ipAddress: "${record.ipAddress}", filePath: "${record.filePath}", 
						operation: "${record.operation}", status: "${record.status}",
						date: "${calendar.get(Calendar.HOUR_OF_DAY)}:${calendar.get(Calendar.MINUTE)} ${calendar.get(Calendar.DAY_OF_MONTH)}/${calendar.get(Calendar.MONTH)}/${calendar.get(Calendar.YEAR)}",
						timezone: "${calendar.getTimeZone().getDisplayName()}"
					} //year: "${calendar.getDisplayName(Calendar.YEAR,Calendar.LONG, locale)}",
				}
			}
		

	}


	/**
	 * Добавить права (в формате Apache Shiro)  пользователю ?username=&permission=.
	 * @return
	 */
	def add_permission() {
		String username = params.username;
		String permission = params.permission;

		def user = User.findByUsername(username)
		user.addToPermissions(permission)
		user.save(flush: true)
		render(contentType: "application/json") {
			user
		}
	}

	/**
	 * Удалить права (в формате Apache Shiro) пользователю ?username=&permission=
	 * @return
	 */
	def del_permission() {
		String username = params.username;
		String permission = params.permission;

		def user = User.findByUsername(username)
		user.removeFromPermissions(permission)
		user.save(flush: true)
	}


	/**
	 * Получить текущее имя пользователя.
	 * @return
	 */
	def get_username() {
		render(contentType: "application/json") {
			username = SecurityUtils.subject.getPrincipal().toString()
		}

	}
	def experimental() {
		String uuid = params.uuid;
		//String name = params.name;
		//
		//		if (SecurityUtils.subject.isPermitted("${uuid}:admin")) {
		//			render(contentType: "application/json") {
		//				message = "Permitted"
		//			}
		//		} else {
		//			render(contentType: "application/json") {
		//				message = "Not enough permissions"
		//			}
		//		}
		//SmbShareControl done = new SmbShareControl()
		//done.closeShare(uuid)
	}

}
