package com.avmurzin.avrora.sec

class Usergroup {
	String groupname
	
	static mapping = {
		groupname column: "groupname", length:255
	}
	
	static constraints = {
	}
}
