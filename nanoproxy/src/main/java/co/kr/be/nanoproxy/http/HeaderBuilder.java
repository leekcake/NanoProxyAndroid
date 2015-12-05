package co.kr.be.nanoproxy.http;

import java.util.ArrayList;

public final class HeaderBuilder {
	public ArrayList<String> Headers = new ArrayList<>();

	public final void addHeader(final String Value) {
		Headers.add(Value);
	}

	public final void addHeader(final String Head, final String Value) {
		addHeader(Head + ": " + Value);
	}

	public final byte[] getHeader() {
		final StringBuilder builder = new StringBuilder();

		for (final String Header : Headers) {
			builder.append(Header + "\r\n");
		}

		builder.append("\r\n");

		return builder.toString().getBytes();
	}
}
