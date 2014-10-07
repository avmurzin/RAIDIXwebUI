package com.avmurzin.avrora.controllers

import com.avmurzin.avrora.ui.UiContainerTree
import com.avmurzin.avrora.aux.ContainerType
import com.avmurzin.avrora.db.Container
import com.avmurzin.avrora.global.ShareType
import com.avmurzin.avrora.global.UserRole

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

		if (SecurityUtils.subject.isPermitted("${parentuuid}:${UserRole.OWNER.toString()}") ||
		SecurityUtils.subject.isPermitted("${parentuuid}:${UserRole.ADMIN.toString()}")) {
			UiContainerTree tree1 = UiContainerTree.getInstance();

			Container container = tree1.getNewContainer(parentuuid, name, description, SecurityUtils.subject.getPrincipal().toString())

			render(contentType: "application/json") {
				result = true
				uuid = container.uuid.toString()
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
	 * ?username=&description=&maxquota=
	 * +Доступ: ${uuid}:OWNER
	 * @return
	 */
	def change_container_properties() {
		String cuuid = params.uuid;
		String cname = params.name;
		String cdescription = params.description;
		long cmaxquota = Long.valueOf(params.maxquota).longValue();

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
		String uuid = params.uuid;
		String username = params.username;
		UserRole role = params.role;

		if (SecurityUtils.subject.isPermitted("${uuid}:${UserRole.OWNER.toString()}")) {
			def container = Container.findByUuid(UUID.fromString(uuid))
			def user = User.findByUsername(username)
			if ((container != null) && (user != null)) {
				container.addToUsers(user)
				container.save(flush: true)
				user.addToPermissions("${uuid}:${role.toString()}")
				user.save(flush: true)
			}
			render(contentType: "application/json") {
				result = true
				//message = "Недостаточно прав"
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
			}
			render(contentType: "application/json") {
				result = true
				//message = "Недостаточно прав"
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
	def get_container_users() {
		String uuid = params.uuid;

		def container = Container.findByUuid(UUID.fromString(uuid))
		def out = [:]
		if (container != null) {
			def users = container.users.findAll()
			//println users
			for (User user : users) {
				for(UserRole role : UserRole.values()) {

					def perm = user.permissions.find {it == "${uuid}:${role.toString()}"}
					//println perm
					if (perm != null) {
						out.put("${user.username}", "${role}")
					}
					perm = null;
				}
			}
		}
		render (contentType: "application/json") {
			out
		}
	}
	/**
	 * Создать сетевой ресурс (контейнер типа SHARE_*) внутри parentuuid
	 * ?name=&description=&sharetype=SMB|FTP..
	 * Текущий пользователь получает право OWNER на созданный контейнер
	 * Пользователи родительского никаких прав на дочерние не получают.
	 * Доступ: ${parentuuid}:OWNER || ${parentuuid}:ADMIN || ${parentuuid}:MANAGER
	 * @return JSON - UUID идентификатор созданного контейнера.
	 */
	def share() {
		String parentuuid = params.parentuuid;
		String cname = params.name;
		String description = params.description;
		ContainerType ctype = params.sharetype;

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
		SmbShareControl done = new SmbShareControl()
		done.closeShare(uuid)
	}

}
