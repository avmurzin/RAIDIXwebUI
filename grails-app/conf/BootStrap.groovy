import com.avmurzin.avrora.sec.User

import org.apache.shiro.crypto.hash.Sha256Hash

import com.avmurzin.avrora.db.Container
import com.avmurzin.avrora.db.Settings
import com.avmurzin.avrora.aux.ContainerType
import com.avmurzin.avrora.global.GlobalProperties

class BootStrap {

    def init = { servletContext ->
		if (true) {
		
		if (Container.count() == 0) {
			def folderRoot = new Container(uuid: UUID.randomUUID(), 
				parentUuid: GlobalProperties.ROOT_UUID,
				name: GlobalProperties.ROOT_NAME,
				description: "Корневой контейнер",
				freequota: 0,
				maxquota: 0,
				type: ContainerType.VIRTUAL,
				sharepath: "xxx");

			folderRoot.save();
		}
		
		def container = Container.findByParentUuid(GlobalProperties.ROOT_UUID)
		
		def user = new User(username: "admin", passwordHash: new Sha256Hash("password").toHex())
		user.addToPermissions("containerManipulation:*:*")
		user.addToPermissions("${container.uuid.toString()}:OWNER")
		user.save()
		
		def user1 = new User(username: "manager", passwordHash: new Sha256Hash("password").toHex())
		user1.addToPermissions("containerManipulation:*:*")
		user1.addToPermissions("${container.uuid.toString()}:MANAGER")
		user1.save()
		
		//def container = Container.findByParentUuid(GlobalProperties.ROOT_UUID)
		container.addToUsers(User.findByUsername("admin"))
		container.addToUsers(User.findByUsername("manager"))
		//container.maxQuota = 1073741824
		container.save()
		
		if (Settings.count() == 0) {
			def set = new Settings(indexes: 1, sambadays: 2592000, webdays: 2592000, email: "admin@oao.rzd", 
				quotapercent: 10, lastcheck: 0, period: 86400)

			set.save(flush: true)
		}
		
		}
    }
    def destroy = {
    }
}
