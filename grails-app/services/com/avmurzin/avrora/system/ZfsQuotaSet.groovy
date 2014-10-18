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

		return ExecuteCommand.execute("sudo ${config.quota.setscript} ${sharepath} ${hardLimit}")
	}

	@Override
	public long getFolderQuota(UUID uuid) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ReturnMessage setUserQuota(String username, long maxquota) {
		def config = new ConfigSlurper().parse(new File('ConfigSlurper/avrora.groovy').toURI().toURL())
		def user = User.findByUsername(username)
		if (user == null) {
			returnMessage.setMessage("Пользователь не сущесвтует")
			returnMessage.setResult(false)

		} else {
			user.maxQuota = maxquota
			user.save(flush: true)
			ExecuteCommand.execute("sudo ${config.quota.setuserscript} ${user.username} ${user.maxQuota / 1024} ${config.quota.zfs.pool}${config.smb.sharefolder}")
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

	@Override
	public ReturnMessage makeDir(String sharepath) {
		def config = new ConfigSlurper().parse(new File('ConfigSlurper/avrora.groovy').toURI().toURL())
		try {
			//ExecuteCommand.execute("zfs create ${config.quota.zfs.pool}${sharepath}")
			ExecuteCommand.execute("sudo ${config.quota.zfs.makedir} ${config.quota.zfs.pool}${sharepath}")
			returnMessage.setResult(true)
			returnMessage.setMessage("")
		} catch (Exception e) {
			returnMessage.setResult(false)
			returnMessage.setMessage(e.toString())
		}

		return returnMessage;
	}

	@Override
	public ReturnMessage deleteDir(String sharepath) {
		def config = new ConfigSlurper().parse(new File('ConfigSlurper/avrora.groovy').toURI().toURL())
		try {
			//ExecuteCommand.execute("zfs destroy ${config.quota.zfs.pool}${sharepath}")
			ExecuteCommand.execute("sudo ${config.quota.zfs.deletedir} ${config.quota.zfs.pool}${sharepath}")
			returnMessage.setResult(true)
			returnMessage.setMessage("")
		} catch (Exception e) {
			returnMessage.setResult(false)
			returnMessage.setMessage(e.toString())
		}

		return returnMessage;
	}

	@Override
	public ReturnMessage renameDir(String oldsharepath, String newsharepath) {
		def config = new ConfigSlurper().parse(new File('ConfigSlurper/avrora.groovy').toURI().toURL())
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
