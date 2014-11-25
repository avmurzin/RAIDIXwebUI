package com.avmurzin.avrora.sec

import org.apache.shiro.SecurityUtils
import org.apache.shiro.authc.AuthenticationException
import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.web.util.SavedRequest
import org.apache.shiro.web.util.WebUtils
import com.avmurzin.avrora.db.Settings
import com.avmurzin.avrora.db.WebUiLog
import com.avmurzin.avrora.global.ReturnMessage;
import com.avmurzin.avrora.system.ExecuteCommand;

class AuthController {
	def shiroSecurityManager

	def index = { redirect(action: "login", params: params) }

	def login = {
		return [ username: params.username, rememberMe: (params.rememberMe != null), targetUri: params.targetUri ]
	}

	def signIn = {

		/**
		 *
		 */
		String test = params.password

		def config = new ConfigSlurper().parse(new File('ConfigSlurper/avrora.groovy').toURI().toURL())
		def command = "${config.smb.auth} --username=${params.username} --password=${params.password}"
		//println command
		ReturnMessage msg = ExecuteCommand.execute(command)
		//println msg.getMessage()
		if(msg.getMessage().contains("NT_STATUS_OK")) {

			params.password = 'AuriraWebUISuperMegaPassword'
			//32-bit hash for MySQL: f57a34f65dfdee584d10313738e8e4beb29b5d5c8b6d8c3f6cbceffb3b36444b
			//Do not use PASSWORD('')!
		}
		/**
		 *
		 */


		def authToken = new UsernamePasswordToken(params.username, params.password as String)

		// Support for "remember me"
		if (params.rememberMe) {
			authToken.rememberMe = true
		}

		// If a controller redirected to this page, redirect back
		// to it. Otherwise redirect to the root URI.
		def targetUri = params.targetUri ?: "/"

		// Handle requests saved by Shiro filters.
		SavedRequest savedRequest = WebUtils.getSavedRequest(request)
		if (savedRequest) {
			targetUri = savedRequest.requestURI - request.contextPath
			if (savedRequest.queryString) targetUri = targetUri + '?' + savedRequest.queryString
		}

		try{
			if (test.equals('AuriraWebUISuperMegaPassword')) {
				throw new AuthenticationException()
			}
			// Perform the actual login. An AuthenticationException
			// will be thrown if the username is unrecognised or the
			// password is incorrect.
			SecurityUtils.subject.login(authToken)

			//если прошло успешно, то затереть лог
			def alert = WebUiLog.findByUsernameAndOperation("${params.username}", "login_alert")
			if (alert != null) {
				alert.delete(flush: true)
			}
			
			log.info "Redirecting to '${targetUri}'."
			redirect(uri: targetUri)


		}
		catch (AuthenticationException ex){
			int times = 0;
			// проверка числа попыток входа
			def trytimes = Settings.findByIndexes(1).trytimes
			Calendar calendar = new GregorianCalendar()
			long timestamp = calendar.getTimeInMillis() / 1000
			def alert = WebUiLog.findByUsernameAndOperation("${params.username}", "login_alert")
			if (alert != null) {

				try {

					times = Integer.parseInt("${alert.description}")

				} catch (Exception e) { println "catch"	}

				if ((times+1) >= trytimes ) {
					def conf = new ConfigSlurper().parse(new File('ConfigSlurper/avrora.groovy').toURI().toURL())
					ExecuteCommand.execute("sudo ${config.security.alert} ${params.username}")
				} else {
					++ times
					alert.username = params.username
					alert.ipAddress = ""
					alert.operation = "login_alert"
					alert.timestamp = timestamp
					alert.description = times
					alert.save(flush: true)
				}

			} else {
				alert = new WebUiLog()
				alert.username = params.username
				alert.ipAddress = ""
				alert.operation = "login_alert"
				alert.timestamp = timestamp
				alert.description = "1"
				alert.save(flush: true)
			}
			
			///////



			// Authentication failed, so display the appropriate message
			// on the login page.
			log.info "Authentication failure for user '${params.username}'."
			flash.message = message(code: "login.failed")

			// Keep the username and "remember me" setting so that the
			// user doesn't have to enter them again.
			def m = [ username: params.username ]
			if (params.rememberMe) {
				m["rememberMe"] = true
			}

			// Remember the target URI too.
			if (params.targetUri) {
				m["targetUri"] = params.targetUri
			}

			// Now redirect back to the login page.
			redirect(action: "login", params: m)
		}
	}

	def signOut = {
		// Log the user out of the application.
		SecurityUtils.subject?.logout()
		webRequest.getCurrentRequest().session = null

		// For now, redirect back to the home page.
		redirect(uri: "/")
	}

	def unauthorized = {
		render "You do not have permission to access this page."
	}
}
//ntlm_auth

//--username=USERNAME
//Specify username of user to authenticate
//--domain=DOMAIN
//Specify domain of user to authenticate
//--workstation=WORKSTATION
//Specify the workstation the user authenticated from
//--challenge=STRING
//NTLM challenge (in HEXADECIMAL)
//--lm-response=RESPONSE
//LM Response to the challenge (in HEXADECIMAL)
//--nt-response=RESPONSE
//NT or NTLMv2 Response to the challenge (in HEXADECIMAL)
//--password=PASSWORD
//User's plaintext password