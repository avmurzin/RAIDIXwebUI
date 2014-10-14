package com.avmurzin.avrora.ui

import com.avmurzin.avrora.db.Container
import java.util.concurrent.ConcurrentHashMap
import com.avmurzin.avrora.global.*
import com.avmurzin.avrora.aux.ContainerType
import com.avmurzin.avrora.sec.User
import com.avmurzin.avrora.system.ShareControl
import com.avmurzin.avrora.system.SmbShareControl
import com.avmurzin.avrora.system.XfsQuotaSet

/**
 * Генерация JSON-данных для элемента UI - дерево контейнеров на основе ссылки
 * на репозиторий контейнеров (выборка всех объектов и установка связей между
 * ними на базе UUID и ParentUUID).
 * @author Andrei Murzin (http://avmurzin.com)
 *
 */
class UiContainerTree {
	public static final UiContainerTree INSTANCE = new UiContainerTree();

	private Map<UUID, TreeElement> map = new ConcurrentHashMap<UUID, TreeElement>();
	private TreeElement tree, root;
	private UUID key;

	public static UiContainerTree getInstance() {
		return INSTANCE;
	}

	/**
	 * Получить корень дерева, при сериализации в JSON "вытягивает" всё дерево.
	 * @return
	 */
	public TreeElement getTree() {
		return root;
	}

	/**
	 * Выполнить генерацию дерева заново из данных, содержащихся в БД.
	 */
	public synchronized void refreshTree() {
		map.clear();
		for (Container folder : Container.list()) {
			tree = new TreeElement();
			tree.setId(folder.getUuid().toString());
			tree.setValue(folder.getName());
			tree.setImage(folder.type.toString())
			if(folder.getParentUuid().equals(GlobalProperties.ROOT_UUID)) {
				key = folder.getUuid();
			}
			if (!map.containsKey(folder.getUuid())) {
				map.put(folder.getUuid(), tree);
			}
			tree = null;
		}
		for (Container folder : Container.list()) {
			if (map.containsKey(folder.getParentUuid())) {
				map.get(folder.getParentUuid()).getData().add(map.get(folder.getUuid()));
			}
		}
		root = new TreeElement();
		root.setId("TheGodId");
		root.getData().add(map.get(key));

	}

	/**
	 * Создать новый контейнер в качестве дочернего для parentuuid.
	 * @param parentuuid - идентификаторо родительского.
	 * @param name - имя нового.
	 * @param description - описание нового.
	 * @param username - имя пользователя, создающего контейнер.
	 * @return новый контейнер.
	 */
	public Container getNewContainer(String parentuuid, String name, String description, String username) {

		def parent = Container.findByUuid(UUID.fromString(parentuuid))
		if (parent.type != ContainerType.VIRTUAL) {
			return null
		}

		def container = new Container(uuid: UUID.randomUUID(),
		parentUuid: UUID.fromString(parentuuid),
		name: name,
		description: description,
		freequota: 0,
		maxquota: 0,
		type: ContainerType.VIRTUAL,
		sharepath: 'xxx').save(failOnError: true, flush: true);

		def user = User.findByUsername(username)

		if ((container != null) && (user != null)) {
			container.addToUsers(user)
			container.save(flush: true)
			//текущий пользователь становится OWNER созданного контейнера
			user.addToPermissions("${container.uuid}:${UserRole.OWNER.toString()}")
			user.save(flush: true)
		}
		refreshTree()
		return container
	}

	/**
	 * Создать новый контейнер-сетевой ресурс в качестве дочернего для parentuuid.
	 * Сетевые ресурсы можно создавать только внутри VIRTUAL контейнера.
	 * @param parentuuid - идентификаторо родительского.
	 * @param name - имя нового.
	 * @param description - описание нового.
	 * @param username - имя пользователя, создающего контейнер.
	 * @param ctype - тип создаваемого контейнера-ресурса
	 * @return новый контейнер.
	 */
	public Container getNewShare(String parentuuid, String name,
			String description, String username, ContainerType ctype) {

		ShareControl shareControl;

		def folder = Container.findByUuid(parentuuid);
		if (folder.type != ContainerType.VIRTUAL) {
			return null
		}

		//если такая шара уже существует, то добавить набор цифр к имени
		if (Container.findByNameAndTypeNotEqual(name, ContainerType.VIRTUAL) != null) {
			name += "-${UUID.randomUUID().toString().getAt(1..4)}"
		}

		def container = new Container(uuid: UUID.randomUUID(),
		parentUuid: UUID.fromString(parentuuid),
		name: name,
		description: description,
		freequota: 0,
		maxquota: 0,
		type: ctype,
		sharepath: 'xxx').save(failOnError: true, flush: true);

		def user = User.findByUsername(username)
		if ((container != null) && (user != null)) {

			container.addToUsers(user)
			container.save(flush: true)
			//текущий пользователь становится OWNER созданного контейнера
			user.addToPermissions("${container.uuid}:${UserRole.OWNER.toString()}")
			user.save(flush: true)
		}
		refreshTree()

		switch (ctype) {
			case ContainerType.SHARE_SMB:
				shareControl = new SmbShareControl()
				break;
		}

		shareControl.addShare(container.uuid.toString(), container.name,
				container.description)

		return container
	}

	/**
	 * Временное закрытие шары uuid.		
	 * @return
	 */
	public boolean closeShare(UUID uuid) {
		ShareControl shareControl;
		def container = Container.findByUuid(uuid)

		switch (container.type) {
			case ContainerType.SHARE_SMB:
				shareControl = new SmbShareControl()
				container.type = ContainerType.UNSHARE_SMB
				container.save(flush: true)
				break;
			default: return false
				break;
		}
		return shareControl.closeShare(uuid.toString()).result
	}

	/**
	 * Открытие ранее созданной шары.
	 * @param uuid
	 * @return
	 */
	public boolean openShare(UUID uuid) {
		ShareControl shareControl;
		def container = Container.findByUuid(uuid)

		switch (container.type) {
			case ContainerType.UNSHARE_SMB:
				shareControl = new SmbShareControl()
				container.type = ContainerType.SHARE_SMB
				container.save(flush: true)
				break;
			default: return false
				break;
		}

		return shareControl.addShare(uuid.toString(), container.name, container.description).result
	}

	/**
	 * Удалить контейнер uuid. Также удаляются пользователи этого контейнера и
	 * права пользователей контейнера. В случае успеха вызывается обновление
	 * дерева.
	 * @param uuid
	 * @return true - удалено, false - удаление не произошло.
	 */
	public boolean delContainer(UUID uuid) {
		ShareControl shareControl;
		def container = Container.findByUuid(uuid)
		if (container != null) {

			//если контейнер виртуальный, то проконтролировать наличие дочерних
			if ((Container.findByParentUuid(uuid) == null) && (container.type.equals(ContainerType.VIRTUAL))) {

				for (User user: container.users.findAll()) {
					for (String perm : user.permissions.findAll {it.contains(uuid.toString())}) {
						user.removeFromPermissions(perm)
						user.save(flush: true)
					}
				}
				//перед удалением освободить квоту
				changeContainer(uuid,container.name,container.description,0)
				container.delete(flush: true)
				refreshTree()
				return true
			}

			//если контейнер невирутальный, то контроль наличия дочерних возлагается на обработчик расшаривания
			if (!container.type.equals(ContainerType.VIRTUAL)) {

				switch (container.type) {
					case ContainerType.UNSHARE_SMB: case ContainerType.SHARE_SMB:
						shareControl = new SmbShareControl()
						break;
					default: return false
						break;
				}

				if (shareControl.delShare(uuid.toString()).result) {
					//перед удалением освободить квоту
					changeContainer(uuid,container.name,container.description,0)
					container.delete(flush: true)
					refreshTree()
					return true
				}

			}
		}
		return false
	}

	/**
	 * Изменить параметры контейнера uuid. После изменения обновляется дерево.
	 * @param uuid
	 * @param name
	 * @param description
	 * @param newmaxquota
	 * @return
	 */
	public Container changeContainer(UUID uuid, String name,
			String description, long newmaxquota) {

		def container = Container.findByUuid(uuid)
		if (container != null) {
			container.name = name
			container.description =description
			container.save(flush: true)
			def parent = Container.findByUuid(container.parentUuid)
			//если квота не устанавливалась, т.е. f/m == 0/0
			if (container.freeQuota == 0 && container.maxQuota == 0) {
				if (newmaxquota <= parent.freeQuota) {
					container.freeQuota = newmaxquota
					container.maxQuota = newmaxquota
					container.save(flush: true)
					parent.freeQuota -= newmaxquota
					parent.save(flush: true)
				} else {
					container.freeQuota = parent.freeQuota
					container.maxQuota = parent.freeQuota
					container.save(flush: true)
					parent.freeQuota = 0
					parent.save(flush: true)
				}
			}
			//если квота уменьшается, т.е. newmaxquota < maxquota
			if (newmaxquota < container.maxQuota) {
				if (newmaxquota >= container.maxQuota - container.freeQuota) {
					parent.freeQuota += (container.maxQuota - newmaxquota)
					parent.save(flush: true)
					container.freeQuota -= (container.maxQuota - newmaxquota)
					container.maxQuota = newmaxquota
					container.save(flush: true)
				} else {
					parent.freeQuota += container.freeQuota
					parent.save(flush: true)
					container.maxQuota -= container.freeQuota
					container.freeQuota = 0
					container.save(flush: true)
				}
			}
			//если квота увеличивается, т.е. newmaxquota > maxquota
			if (newmaxquota > container.maxQuota) {
				if ((newmaxquota - container.maxQuota) <= parent.freeQuota) {
					parent.freeQuota -= (newmaxquota - container.maxQuota)
					parent.save(flush: true)
					container.freeQuota += (newmaxquota - container.maxQuota)
					container.maxQuota = newmaxquota
					container.save(flush: true)
				} else {
					container.maxQuota += parent.freeQuota
					container.freeQuota += parent.freeQuota
					container.save(flush: true)
					parent.freeQuota = 0
					parent.save(flush: true)

				}

			}
			refreshShareConfig(container)
		}
		refreshTree()
		return container
	}


	/**
	 * Обновление конфига для контейнера типа SHARE_*
	 * @param container
	 * @return
	 */
	public boolean refreshShareConfig(Container container) {
		ShareControl shareControl;
		//XfsQuotaSet xfsQuotaSet;

		switch (container.type) {
			case ContainerType.SHARE_SMB:
				shareControl = new SmbShareControl()
			//xfsQuotaSet = XfsQuotaSet.getInstance()
				break;
			default: return false
				break;
		}

		//xfsQuotaSet.setFolderQuota(container.uuid)
		return shareControl.addShare(container.uuid.toString(), container.name, container.description).result
	}


	private UiContainerTree() {

	}
}
