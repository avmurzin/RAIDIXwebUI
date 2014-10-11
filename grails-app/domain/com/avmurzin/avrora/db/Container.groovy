package com.avmurzin.avrora.db

import com.avmurzin.avrora.aux.ContainerType
import com.avmurzin.avrora.sec.User
/**
 * 
 * @author Andrei Murzin (http://avmurzin.com)
 *
 */
class Container {
    UUID uuid;
    UUID parentUuid;
    String name;
    String description;
	long maxQuota; //максимальная разрешенная емкость контейнера, bytes
	long freeQuota;//текущая свободная емкость контейнера, bytes 
	ContainerType type;
	String sharepath;
	
	static mapping = {
		table "container";
		uuid column: "uuid", length: 16;
		parentUuid column: "parentuuid", length: 16;
		name column: "name";
		description column: "description", length: 400;
		maxQuota column: "maxquota", sqlType: "bigint", length: 20;
		freeQuota column: "freequota", sqlType: "bigint", length: 20;
		type column: "type";
		sharepath column: "sharepath";
	}
	
	static hasMany = [users:User]
	
    static constraints = {
    }
}
