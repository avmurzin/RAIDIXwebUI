class UrlMappings {

	static mappings = {
        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }

		//получить дерево контейнеров
		"/get_tree" (controller: "containerManipulation", action: "get_tree")
		
		//создать контейнер внутри родительского parentuuid ?name=&description=
		"/new_container/$parentuuid" (controller: "containerManipulation", action: "new_container")
		
		//Удалить контейнер uuid (выполняется только если нет дочерних)
		"/del_container/$uuid" (controller: "containerManipulation", action: "del_container")
		
		//получить свойства контейнера (uuid, description, maxquota, freequota)
		"/container_properties/$uuid" (controller: "containerManipulation", action: "container_properties")
		
		//изменить свойства контейнера uuid.
		//?username=&description=&maxquota=
		"/change_container_properties/$uuid" (controller: "containerManipulation", action: "change_container_properties")
		
		//Добавить пользователя контейнера uuid с указанием его роли
		// ?username=&role=OWNER|ADMIN|MANAGER|ROUSER|RWUSER
		"/add_container_user/$uuid" (controller: "containerManipulation", action: "add_container_user")
		
		//Удалить пользователя контейнера uuid с удалением всех прав на контейнер
		// ?username=
		"/del_container_user/$uuid" (controller: "containerManipulation", action: "del_container_user")
		
		//Получить список всех пользователей конейнера uuid с их ролями
		"/get_container_users/$uuid" (controller: "containerManipulation", action: "get_container_users")
		
		//создать сетевой ресурс (контейнер типа SHARE_*) внутти parentuuid
		// ?name=&description=&sharetype=SHARE_SMB|SHARE_FTP... (совпадает с типом конейнера)
		"/share/$parentuuid" (controller: "containerManipulation", action: "share")
		//временно закрыть сетевой ресурс
		"/close_share/$uuid" (controller: "containerManipulation", action: "close_share")
		
		//Добавить права (в формате Apache Shiro) пользователю ?username=&permission= 
		"/add_permission" (controller: "containerManipulation", action: "add_permission")
		
		//Удалить права (в формате Apache Shiro) пользователю ?username=&permission=
		"/del_permission" (controller: "containerManipulation", action: "del_permission")
		
		"/experimental" (controller: "containerManipulation", action: "experimental")
		
        "/"(view:"/index")
        "500"(view:'/error')
	}
}
