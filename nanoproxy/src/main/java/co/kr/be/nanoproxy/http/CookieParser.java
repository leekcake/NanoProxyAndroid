package co.kr.be.nanoproxy.http;

import java.util.HashMap;

public final class CookieParser {
	protected final HashMap<String, String> Cookies = new HashMap<>();

	public CookieParser(String Cookie) {
		Cookie = Cookie.trim();
		String[] split;
		if (Cookie.contains(";")) {
			split = Cookie.split(";");
		} else {
			split = new String[1];
			split[0] = Cookie;
		}

		for (final String data : split) {
			// name and content
			final String[] nc = data.split("=");
			Cookies.put(nc[0].trim(), nc[1].trim());
		}
	}

	public final String getCookie(final String Name) {
		if (Cookies.containsKey(Name))
			return Cookies.get(Name);
		else
			return null;
	}
}
