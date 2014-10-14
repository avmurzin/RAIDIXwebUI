package com.avmurzin.avrora.system

import java.util.UUID;

import com.avmurzin.avrora.global.ReturnMessage;

class ZfsQuotaSet implements QuotaSet {
	public static final ZfsQuotaSet INSTANCE = new ZfsQuotaSet();
	
	public static ZfsQuotaSet getInstance() {
		return INSTANCE;
	}
	private ZfsQuotaSet() {}
	
	@Override
	public ReturnMessage setFolderQuota(UUID uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getFolderQuota(UUID uuid) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ReturnMessage setUserQuota(UUID uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getUserQuota(UUID uuid) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ReturnMessage makeDir(String sharepath) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ReturnMessage deleteDir(String sharepath) {
		// TODO Auto-generated method stub
		return null;
	}

}
