package com.avmurzin.avrora.system

import java.util.UUID;

import com.avmurzin.avrora.global.ReturnMessage
import com.avmurzin.avrora.ui.UiContainerTree;
import com.avmurzin.avrora.db.Container
import com.avmurzin.avrora.aux.ContainerType

/**
 * Управление квотами файловой системы XFS.
 * @author Andrei Murzin (http://avmurzin.com)
 *
 */
class XfsQuotaSet implements QuotaSet {
	public static final XfsQuotaSet INSTANCE = new XfsQuotaSet();
	
	public static XfsQuotaSet getInstance() {
		return INSTANCE;
	}
	
	ReturnMessage returnMessage = new ReturnMessage()

	/**
	 * Установить квоту на шару (при размещении в XFS).
	 * @param uuid -идентификатор контейнера
	 * @param hardLimit - квота в байтах
	 * @return
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
		def sharefolder = "${config.smb.sharefolder}"
		def id = container.id
		def hardLimit = container.maxQuota / 1024 //на входе метода байты, системный скрипт предполагает килобайты

		return ExecuteCommand.execute("sudo ${config.quota.setscript} ${sharepath} ${sharefolder} ${id} ${hardLimit}")
		//    скрипт ${quota.setscript} должен содеражть команды:
		// xfs_quota -x -c 'project -s -p $1 $3' $2
		// xfs_quota -x -c 'limit -p bhard=$4 $3' $2
		//    что означает:
		//# xfs_quota -x -c 'project -s -p /var/log 42' /var
		//# xfs_quota -x -c 'limit -p bhard=1g 42' /var
		// 42 - числовой идентификатор для примера
	}
	
	@Override
	public long getFolderQuota(UUID uuid) {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public ReturnMessage setUserQuota(UUID uuid) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public long getUserQuota(UUID uuid) {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public ReturnMessage makeDir(String sharepath) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public ReturnMessage deleteDir(String sharepath) {
		// TODO Auto-generated method stub
		return null;
	}


	private XfsQuotaSet() {}
}
