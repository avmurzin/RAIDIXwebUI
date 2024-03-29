package com.avmurzin.avrora.system

import com.avmurzin.avrora.global.ReturnMessage

class ExecuteCommand {
	static public ReturnMessage execute(String command) {
		ReturnMessage returnMessage = new ReturnMessage()
		//println command
		StringBuffer output = new StringBuffer();

		Process p;
		try {
			p = Runtime.getRuntime().exec(command);
			p.waitFor();
			BufferedReader reader =
					new BufferedReader(new InputStreamReader(p.getInputStream()));

			String line = "";
			while ((line = reader.readLine())!= null) {
				output.append(line + "\n");
			}

			returnMessage.setMessage(output.toString())
			returnMessage.setResult(true)

		} catch (Exception e) {
			returnMessage.setMessage(e.message)
			returnMessage.setResult(false)
		}
		//println returnMessage.getMessage()
		return returnMessage

	}

	static public List executeForList(String command) {
		List<String> outList = new ArrayList<String>()
		outList.add("")

		StringBuffer output = new StringBuffer();

		Process p;
		try {
			p = Runtime.getRuntime().exec(command);
			p.waitFor();
			BufferedReader reader =
					new BufferedReader(new InputStreamReader(p.getInputStream()));

			String line = "";
			while ((line = reader.readLine())!= null) {
				outList.add(line);
			}

			return outList

		} catch (Exception e) {
			return outList
		}

		return outList;

	}
}
