package com.avmurzin.avrora.db

class WebUiLog {
	long timestamp
	String username
	String ipAddress
	String operation
	String description

	

	static mapping = {
		table "webuilog";
		timestamp column: "timestamp";
		username column: "username";
		ipAddress column: "ipAddress";
		operation column: "operation";
		description column: "description";
	}
	
    static constraints = {
    }
}
