package com.avmurzin.avrora.sec

import com.avmurzin.avrora.db.Container

class User {
    String username
    String passwordHash
    
    static hasMany = [ roles: Role, permissions: String, containers: Container ]
	static belongsTo = Container

    static constraints = {
        username(nullable: false, blank: false, unique: true)
    }
}
