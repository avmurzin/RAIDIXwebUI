import com.avmurzin.avrora.sec.User
import org.apache.shiro.crypto.hash.Sha256Hash
import com.avmurzin.avrora.db.Container
import com.avmurzin.avrora.aux.ContainerType
import com.avmurzin.avrora.global.GlobalProperties

class BootStrap {

    def init = { servletContext ->
		if (true) {
		def user = new User(username: "admin", passwordHash: new Sha256Hash("password").toHex())
		user.addToPermissions("containerManipulation:*:*")
		user.save()
		
		def user1 = new User(username: "manager", passwordHash: new Sha256Hash("password").toHex())
		user1.addToPermissions("containerManipulation:share:*")
		user1.save()
		
		
		
		if (Container.count() == 0) {
			def folderRoot = new Container(uuid: UUID.randomUUID(), 
				parentUuid: GlobalProperties.ROOT_UUID,
				name: GlobalProperties.ROOT_NAME,
				description: "Корневой контейнер",
				freequota: 0,
				maxquota: 0,
				type: ContainerType.VIRTUAL);

			folderRoot.save();
		}
		
		def container = Container.findByParentUuid(GlobalProperties.ROOT_UUID)
		container.addToUsers(User.findByUsername("admin"))
		container.maxQuota = 100
		container.freeQuota = 100
		container.save()
		
		}
    }
    def destroy = {
    }
}
