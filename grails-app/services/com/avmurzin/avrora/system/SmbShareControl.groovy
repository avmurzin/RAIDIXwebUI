package com.avmurzin.avrora.system

import java.io.BufferedReader

import com.avmurzin.avrora.global.ReturnMessage
import com.sun.xml.internal.ws.api.config.management.policy.ManagedServiceAssertion.ImplementationRecord;

import java.io.InputStreamReader
import java.util.regex.Pattern

/**
 * Управление файлами конфигурации Samba. 
 * @author Andrei Murzin (http://avmurzin.com)
 *
 */
class SmbShareControl implements ShareControl {
	//TODO: заменить ссылку на файл конфигурации на глобальную переменную
	ReturnMessage returnMessage = new ReturnMessage()

	/**
	 * Добавление ресурса.
	 * Для каждой шары в каталоге config.smb.confdir
	 * создается отдельный файл с описанием ресурса. На созданный файл в файле
	 * config.smb.rootconf создается ссылка (include = ...). В свою очередь файл
	 * /etc/smb.conf содержит ссылку на config.smb.rootconf
	 * После изменения конфигурации необходимо перестартовать демон smb
	 * @param uuid - идентификатор контейнера (типа SHARE_*)
	 * @param name
	 * @return
	 */
	@Override
	public ReturnMessage addShare(String uuid, String name, String description) {
		def config = new ConfigSlurper().parse(new File('ConfigSlurper/avrora.groovy').toURI().toURL())
		//создание папки для расшаривания
		def folder = new File("${config.smb.sharefolder}/${uuid}/${name}")
		if( !folder.exists() ) {
			folder.mkdirs()
		}

		//подготовка конфига для шары
		def confText = "[${uuid}-${name}]\n" +
				"comment = text\n" +
				"path = ${config.smb.sharefolder}/${uuid}/${name}\n" +
				"public = yes\n" +
				"writable = no\n" +
				"force user = murzin\n" +
				"force group = murzin\n"

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

		//удаление ссылки в рутовом файле
		def rootconf = new File( "${config.smb.rootconf}" )
		processFileInplace(rootconf) { text ->
			Pattern p = Pattern.compile(".*${uuid}.*\n")
			//println p.toString()
			//println "123"
			text.replaceAll(p, '')
		}
		return null;
	}

	def processFileInplace(file, Closure processText) {
		def text = file.text
		file.write(processText(text))
	}

}
