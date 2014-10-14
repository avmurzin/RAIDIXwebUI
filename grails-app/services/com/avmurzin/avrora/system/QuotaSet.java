package com.avmurzin.avrora.system;

import java.util.UUID;

import com.avmurzin.avrora.global.ReturnMessage;
/**
 * Набор методов для работы с квотами.
 * Тип реализации выбирается в соответствии со значением config.quota.fstype
 * @author Andrei Murzin (http://avmurzin.com)
 *
 */
public interface QuotaSet {
	/**
	 * Создание каталога, который будет расшарен.
	 * @param sharepath
	 * @return
	 */
	public ReturnMessage makeDir(String sharepath);
	/**
	 * Удаление каталога.
	 * @param sharepath
	 * @return
	 */
	public ReturnMessage deleteDir(String sharepath);
	/**
	 * Установка квоты для каталога.
	 * @param uuid
	 * @return
	 */
	public ReturnMessage setFolderQuota(UUID uuid);
	public long getFolderQuota(UUID uuid);
	public ReturnMessage setUserQuota(UUID uuid);
	public long getUserQuota(UUID uuid);
}
