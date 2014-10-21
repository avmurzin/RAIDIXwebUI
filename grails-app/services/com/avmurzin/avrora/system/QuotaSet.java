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
	public ReturnMessage makeDir(String sharepath, String uuid);
	/**
	 * Удаление каталога.
	 * @param sharepath
	 * @return
	 */
	public ReturnMessage deleteDir(String sharepath, String uuid);
	public ReturnMessage renameDir(String oldsharepath,String newsharepath, String uuid);
	/**
	 * Установка квоты для каталога.
	 * @param uuid
	 * @return
	 */
	public ReturnMessage setFolderQuota(UUID uuid);
	public long getFolderQuota(UUID uuid);
	public ReturnMessage setUserQuota(String username, long maxquota);
	public long getUserQuota(String username);
}
