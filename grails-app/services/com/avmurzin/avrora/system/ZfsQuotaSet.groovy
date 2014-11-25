package com.avmurzin.avrora.system

import java.util.UUID;

import com.avmurzin.avrora.global.ReturnMessage;
import com.avmurzin.avrora.system.ExecuteCommand;
import com.avmurzin.avrora.db.Container
import com.avmurzin.avrora.sec.User
import com.avmurzin.avrora.aux.ContainerType

class ZfsQuotaSet implements QuotaSet {
	public static final ZfsQuotaSet INSTANCE = new ZfsQuotaSet();

	public static ZfsQuotaSet getInstance() {
		return INSTANCE;
	}
	private ZfsQuotaSet() {}

	ReturnMessage returnMessage = new ReturnMessage()

	/**
	 * Установить квоту на шару (== набор данных ZFS).
	 * вызывается скрипт config.quota.zfs.setscript
	 * с параметром zfsPool uuid quota
	 * имя пула ZFS, id расшариваемого каталога, квота Кб
	 * 
	 * #!/bin/bash
	 * #$1 - pool, $2 - uuid, $3 - quota
	 * zfs set quota=${3}K $1/$2
	 */
	@Override
	public ReturnMessage setFolderQuota(UUID uuid) {
		def config = new ConfigSlurper().parse(new File('ConfigSlurper/avrora.groovy').toURI().toURL())
		def container = Container.findByUuid(uuid)

		// выйти, если контейнер виртуальный или не найден
		if (container.type.equals(ContainerType.VIRTUAL)  || container == null) {
			returnMessage.setResult(false)
			returnMessage.setMessage("Контейнер не поддерживает операцию или не существует")
			return returnMessage
		}

		def sharepath = "${container.sharepath}"
		def hardLimit = container.maxQuota / 1024 //на входе метода байты, системный скрипт предполагает килобайты

		return ExecuteCommand.execute("sudo ${config.quota.setscript} ${config.quota.zfs.pool} ${uuid} ${hardLimit}")
	}

	@Override
	public long getFolderQuota(UUID uuid) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ReturnMessage setUserQuota(String username, long maxquota, String cuuid) {
		def config = new ConfigSlurper().parse(new File('ConfigSlurper/avrora.groovy').toURI().toURL())
		def user = User.findByUsername(username)
		String st = ""
		if (user == null) {
			returnMessage.setMessage("Пользователь не сущесвтует")
			returnMessage.setResult(false)

		} else {
			st = username
			user.maxQuota = maxquota
			user.save(flush: true)
			//если это группа
			if (st.matches("@.*\$")) {
				st = st.replaceAll("@", "")
				st = st.replaceAll("\"", "")
				ExecuteCommand.execute("sudo ${config.quota.setgroupquota} ${st} ${user.maxQuota / 1024} ${config.quota.zfs.pool} ${cuuid}")
			} else {
				//если это юзер
				ExecuteCommand.execute("sudo ${config.quota.setuserscript} ${user.username} ${user.maxQuota / 1024} ${config.quota.zfs.pool} ${cuuid}")
			}
			//


			returnMessage.setMessage("")
			returnMessage.setResult(true)
		}
		return returnMessage;
	}

	@Override
	public long getUserQuota(String username) {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * Создать каталог и набор данных для ZFS
	 * вызывается скрипт config.quota.zfs.makedir
	 * с параметрами zfsPool sharepath uuid 
	 * имя пула ZFS, путь к каталогу для конфига smb, id расшариваемого каталога
	 * 
	 * #!/bin/bash
	 * #$1 - pool, $2 - sharepath, $3 - uuid
	 * mkdir $2
	 * zfs create -o mountpoint=$2 $1/$3
	 */
	@Override
	public ReturnMessage makeDir(String sharepath, String uuid) {
		def config = new ConfigSlurper().parse(new File('ConfigSlurper/avrora.groovy').toURI().toURL())
		try {
			//ExecuteCommand.execute("zfs create ${config.quota.zfs.pool}${sharepath}")
			ExecuteCommand.execute("sudo ${config.quota.zfs.makedir} ${config.quota.zfs.pool} ${sharepath} ${uuid}")
			returnMessage.setResult(true)
			returnMessage.setMessage("")
		} catch (Exception e) {
			returnMessage.setResult(false)
			returnMessage.setMessage(e.toString())
		}

		return returnMessage;
	}

	/**
	 * Удалить каталог и набор данных для ZFS
	 * вызывается скрипт config.quota.zfs.deletedir
	 * с параметрами zfsPool sharepath uuid
	 * имя пула ZFS, путь к каталогу для конфига smb, id расшариваемого каталога
	 * 
	 * #!/bin/bash
	 * #$1 - pool, $2 - sharepath, $3 - uuid
	 * #mkdir $2
	 * zfs destroy $1/$3
	 * rmdir $2
	 */
	@Override
	public ReturnMessage deleteDir(String sharepath, String uuid) {
		def config = new ConfigSlurper().parse(new File('ConfigSlurper/avrora.groovy').toURI().toURL())
		try {
			//ExecuteCommand.execute("zfs destroy ${config.quota.zfs.pool}${sharepath}")
			ExecuteCommand.execute("sudo ${config.quota.zfs.deletedir} ${config.quota.zfs.pool} ${sharepath} ${uuid}")
			returnMessage.setResult(true)
			returnMessage.setMessage("")
		} catch (Exception e) {
			returnMessage.setResult(false)
			returnMessage.setMessage(e.toString())
		}

		return returnMessage;
	}

	/**
	 * Переименование шары для ZFS в текущей версии заблокировано.
	 * 
	 */
	@Override
	public ReturnMessage renameDir(String oldsharepath, String newsharepath, String uuid) {
		def config = new ConfigSlurper().parse(new File('ConfigSlurper/avrora.groovy').toURI().toURL())

		//		returnMessage.setResult(false)
		//		returnMessage.setMessage("В текущей версии переименование сетевого ресурса заблокировано")

		try {
			//ExecuteCommand.execute("zfs rename ${config.quota.zfs.pool}${oldsharepath} ${config.quota.zfs.pool}${newsharepath}")
			ExecuteCommand.execute("sudo ${config.quota.zfs.renamedir} ${config.quota.zfs.pool}${oldsharepath} ${config.quota.zfs.pool}${newsharepath}")
			returnMessage.setResult(true)
			returnMessage.setMessage("")
		} catch (Exception e) {
			returnMessage.setResult(false)
			returnMessage.setMessage(e.toString())
		}

		return returnMessage;
	}

}
