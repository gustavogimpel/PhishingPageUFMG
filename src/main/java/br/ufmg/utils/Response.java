package br.ufmg.utils;

import java.util.List;

import net.lightbody.bmp.core.har.HarEntry;

public class Response {

	private boolean blocked;
	private boolean exception;
	private String urlLog;
	List<HarEntry> entries;

	public Response(boolean b1, boolean b2, String l1) {
		blocked = b1;
		exception = b2;
		urlLog = l1;
	}

	public Response(boolean b1, boolean b2, String l1, List<HarEntry> entradas) {
		blocked = b1;
		exception = b2;
		urlLog = l1;
		entries = entradas;
	}

	public Boolean getBlocked() {
		return blocked;
	}

	public Boolean getException() {
		return exception;
	}

	public String getUrlLog() {
		return urlLog;
	}

	public List<HarEntry> getHar() {
		return entries;
	}

}
