package com.avmurzin.avrora.system;

import com.avmurzin.avrora.global.ReturnMessage;

public interface ShareControl {

	/**
	 * Добавление сетевого ресурса.
	 * @param uuid - контейнер, с которым связан ресурс
	 * @param name - имя ресурса
	 * @return - @see ReturnMessage
	 */
	public ReturnMessage addShare(String uuid, String name, String desription);
	/**
	 * Удаление сетевого ресурса с удалением содержимого (!)
	 * @param - контейнер, с которым связан ресурс
	 * @return - @see ReturnMessage
	 */
	public ReturnMessage delShare(String uuid);
	/**
	 * Удаление (скрытие) сетевого ресурса без удаления содержимого.
	 * @param uuid - контейнер, с которым связан ресурс
	 * @return - @see ReturnMessag
	 */
	public ReturnMessage closeShare(String uuid);
}
