package com.avmurzin.avrora.global;

public class ReturnMessage {
	private boolean result = false;
	private String message = "";
	
	public ReturnMessage(){};
	
	public void setResult(boolean result) {
		this.result = result;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public boolean getResult() {
		return result;
	}
	public String getMessage() {
		return message;
	}

}
