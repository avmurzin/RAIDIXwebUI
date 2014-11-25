package com.avmurzin.avrora.db

class Settings {
	long sambadays
	long webdays
	int indexes
	String email
	int quotapercent
	long lastcheck
	long period
	int trytimes;

	static mapping = {
		table "settings";
		sambadays column: "sambadays";
		webdays column: "webdays";
		index column: "index";
		email column: "email";
		quotapercent column: "quotapercent";
		lastcheck column: "lastcheck";
		period column: "period";
		trytimes column: "trytimes";
	}
    static constraints = {
    }
}
