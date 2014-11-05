package com.avmurzin.avrora.system

import java.io.BufferedReader

import com.avmurzin.avrora.global.ReturnMessage
import com.sun.xml.internal.ws.api.config.management.policy.ManagedServiceAssertion.ImplementationRecord;
import com.avmurzin.avrora.aux.ContainerType
import java.io.InputStreamReader
import java.util.regex.Pattern
import com.avmurzin.avrora.db.Container
import com.avmurzin.avrora.global.UserRole
import com.avmurzin.avrora.sec.User
import com.avmurzin.avrora.system.QuotaSet
import com.avmurzin.avrora.system.XfsQuotaSet
import com.avmurzin.avrora.system.ZfsQuotaSet
import com.avmurzin.avrora.ui.UiContainerTree

/**
 * Управление файлами конфигурации Samba. 
 * @author Andrei Murzin (http://avmurzin.com)
 *
 */
class SmbShareControl implements ShareControl {
	//TODO: заменить ссылку на файл конфигурации на глобальную переменную
	ReturnMessage returnMessage = new ReturnMessage()
	QuotaSet quotaSet;

	/**
	 * Добавление ресурса (открытие ранее созданного).
	 * Для каждой шары в каталоге config.smb.confdir
	 * создается отдельный файл с описанием ресурса. На созданный файл в файле
	 * config.smb.rootconf создается ссылка (include = ...). В свою очередь файл
	 * /etc/smb.conf содержит ссылку на config.smb.rootconf
	 * Сам ресурс создается в папке ${config.smb.sharefolder}/${name}-${uuid.getAt(1..4)}
	 * и имеет то же сетевое имя
	 * После изменения конфигурации необходимо перестартовать демон smb
	 * @param uuid - идентификатор контейнера (типа SHARE_*)
	 * @param name
	 * @return в случае успеха true
	 */
	@Override
	public ReturnMessage addShare(String uuid, String name, String description) {
		def config = new ConfigSlurper().parse(new File('ConfigSlurper/avrora.groovy').toURI().toURL())

		//определить тип используемой FS (по настройкам)

		switch (config.quota.fstype) {
			case "zfs":
			println "zfs"
				quotaSet = ZfsQuotaSet.getInstance()
				break;
			case "xfs":
			println "xfs"
				quotaSet = XfsQuotaSet.getInstance()
				break;
		}

		//если конфиг для шары есть, то он сначала будет удален
		closeShare(uuid)

		def container = Container.findByUuid(UUID.fromString(uuid))

		// выйти, если контейнер виртуальный или не найден
		if (container.type.equals(ContainerType.VIRTUAL)  || container == null) {
			returnMessage.setResult(false)
			returnMessage.setMessage("Контейнер не поддерживает операцию или не существует")
			return returnMessage
		}

		//имя самой шары
		def sharename = "${name}"
		//имя каталога, который будет расшарен
		//def sharepath = "${config.smb.sharefolder}/${name}-${uuid.getAt(1..4)}"
		//def sharepath = "${config.smb.sharefolder}/${name}"
		def sharepath = "${config.smb.sharefolder}/${uuid}"

		//если текущее имя расшаренного каталога не соответствует прошлому
		if (!container.sharepath.equals(sharepath) && !container.sharepath.equals("")) {
			//quotaSet.renameDir(container.sharepath, sharepath, container.uuid.toString())
			container.sharepath = sharepath
			container.save(flush: true)
		}

		//создание папки для расшаривания
//		def folder = new File(sharepath)
//		if( !folder.exists() ) {
//			folder.mkdirs()
//		}
		quotaSet.makeDir(sharepath, container.uuid.toString())

		//подготовка конфига для шары
		def confText = "[${sharename}]\n" +
				"comment = text\n" +
				"path = ${sharepath}\n" +
				"browseable = yes\n"
				


		def users = container.users.findAll()
		def rolist = "read list = "
		def rwlist = "write list = "

		for (User user : users) {
			for(UserRole role : UserRole.values()) {

				def perm = user.permissions.find {it == "${uuid}:${role.toString()}"}
				//println perm
				if (perm != null) {
					//out.put("${user.username}", "${role}")
					if (role == UserRole.ROUSER) {
						rolist += " ${user.username},"
					}
					if (role == UserRole.RWUSER) {
						rwlist += " ${user.username},"
					}
				}
				perm = null;
			}
		}

		confText += "${rolist} fake\n"
		confText += "${rwlist} fake\n"

		//файл с добавочным конфигом
		new File( "${config.smb.confdir}", "${uuid}.conf" ).withWriterAppend { w ->
			w << "${confText}"
		}

		//добавление ссылки в рутовый файл
		new File( "${config.smb.rootconf}" ).withWriterAppend { w ->
			w << "include = ${config.smb.confdir}/${uuid}.conf\n"
		}

		returnMessage.setResult(true)
		returnMessage.setMessage("Операция завершена успешно")

		//перезапуск smbd
		executeCommand("sudo ${config.smb.restartscript}")

		//XfsQuotaSet xfsQuotaSet = XfsQuotaSet.getInstance()
		quotaSet.setFolderQuota(container.uuid)

		return returnMessage
	}


	private String executeCommand(String command) {

		StringBuffer output = new StringBuffer();

		Process p;
		try {
			p = Runtime.getRuntime().exec(command);
			p.waitFor();
			BufferedReader reader =
					new BufferedReader(new InputStreamReader(p.getInputStream()));

			String line = "";
			while ((line = reader.readLine())!= null) {
				output.append(line + "\n");
			}

		} catch (Exception e) {
			//e.printStackTrace();
			returnMessage.setMessage(e.message)
			returnMessage.setResult(false)
		}

		return output.toString();

	}

	@Override
	public ReturnMessage delShare(String uuid) {
		def config = new ConfigSlurper().parse(new File('ConfigSlurper/avrora.groovy').toURI().toURL())
		switch (config.quota.fstype) {
			case "zfs":
				quotaSet = ZfsQuotaSet.getInstance()
				//println "zfs"
				break;
			case "xfs":
				quotaSet = XfsQuotaSet.getInstance()
				//println "xfs"
				break;
		}
		
		def container = Container.findByUuid(UUID.fromString(uuid))
		if (container != null) {
			def currentDir = new File(container.sharepath)
			def files = []
			currentDir.eachFileMatch(~/^.*$/) { files << it.name }
			if (files.empty) {
				
				//перед удалением освободить квоту
				UiContainerTree.getInstance().changeContainer(UUID.fromString(uuid),container.name,container.description,0)
				closeShare(uuid)
				//currentDir.deleteDir()
				quotaSet.deleteDir(container.sharepath, container.uuid.toString())
				returnMessage.setMessage("Каталог успешно удален")
				returnMessage.setResult(true)
			} else {
				returnMessage.setMessage("Каталог не пуст")
				returnMessage.setResult(false)
			}
		} else {
			returnMessage.setMessage("Каталог не существует")
			returnMessage.setResult(false)
		}


		//перезапуск smbd
		executeCommand("sudo ${config.smb.restartscript}")
		return returnMessage;
	}


	@Override
	public ReturnMessage closeShare(String uuid) {
		def config = new ConfigSlurper().parse(new File('ConfigSlurper/avrora.groovy').toURI().toURL())
		try {

			//удаление ссылки в рутовом файле
			def rootconf = new File( "${config.smb.rootconf}" )
			processFileInplace(rootconf) { text ->
				Pattern p = Pattern.compile(".*${uuid}.*\n")
				text.replaceAll(p, '')
			}
			//удаление файла с добавочным конфигом
			new File( "${config.smb.confdir}", "${uuid}.conf" ).delete()

			returnMessage.setMessage("Доступ к ресурсу закрыт")
			returnMessage.setResult(true)

		}  catch (Exception e) {
			returnMessage.setMessage(e.message)
			returnMessage.setResult(false)
		}
		
		//перезапуск smbd
		executeCommand("sudo ${config.smb.restartscript}")
		return returnMessage
	}

	def processFileInplace(file, Closure processText) {
		def text = file.text
		file.write(processText(text))
	}

}
