package com.avmurzin.avrora.db

class SambaLog {
	long timestamp
	String username
	String ipAddress
	String operation
	String status
	String filePath
	String sharePath
	

	static mapping = {
		table "sambalog";
		timestamp column: "timestamp";
		username column: "username";
		ipAddress column: "ipAddress";
		operation column: "operation";
		status column: "status";
		filePath column: "filePath";
		sharePath column: "sharePath";
	}
	
    static constraints = {
    }
}
