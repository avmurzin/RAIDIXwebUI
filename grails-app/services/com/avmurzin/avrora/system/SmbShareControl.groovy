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

/**
 * Управление файлами конфигурации Samba. 
 * @author Andrei Murzin (http://avmurzin.com)
 *
 */
class SmbShareControl implements ShareControl {
	//TODO: заменить ссылку на файл конфигурации на глобальную переменную
	ReturnMessage returnMessage = new ReturnMessage()

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
		
		//если конфиг для шары есть, то он сначала будет удален
		closeShare(uuid)
		
		//создание папки для расшаривания
		def folder = new File("${config.smb.sharefolder}/${name}-${uuid.getAt(1..4)}")
		if( !folder.exists() ) {
			folder.mkdirs()
		}

		//подготовка конфига для шары
		def confText = "[${name}-${uuid.getAt(1..4)}]\n" +
				"comment = text\n" +
				"path = ${config.smb.sharefolder}/${name}-${uuid.getAt(1..4)}\n" +
				"browseable = yes\n"
				
				def container = Container.findByUuid(UUID.fromString(uuid))
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
		// TODO Auto-generated method stub
		return null;
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
		return returnMessage
	}

	def processFileInplace(file, Closure processText) {
		def text = file.text
		file.write(processText(text))
	}

}
