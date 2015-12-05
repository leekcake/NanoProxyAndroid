package co.kr.be.nanoproxy.filter;

import java.net.URI;

import co.kr.be.nanoproxy.http.HeaderBuilder;
import co.kr.be.nanoproxy.http.HeaderParser;

public final class FilterInformation {
	/**
	 * 클라이언트 -> 프록시로 온 요청을 저장한것
	 */
	private HeaderParser client_request;
	/**
	 * 프록시 -> 클라이언트로 보내야 할 응답을 저장한것
	 */
	private HeaderBuilder client_response;

	/**
	 * 프록시 -> 원격 서버로 보낼 요청을 저장한 것
	 */
	private HeaderBuilder server_request;

	/**
	 * 원격 서버 -> 프록시로 온 응답을 저장한 것
	 */
	private HeaderParser server_response;

	private URI requestOriginaluri = null;

	public byte[] postData = null;

	public URI getRequestURL() {
		return requestOriginaluri;
	}

	public void setRequestURL(final URI requestOriginaluri) {
		this.requestOriginaluri = requestOriginaluri;
	}

	public final void reset() {
		client_request = null;
		client_response = null;

		server_request = null;
		server_response = null;
	}

	public final void setClientRequestHeader(final HeaderParser parser) {
		client_request = parser;
	}

	public final HeaderParser getClientRequestHeader() {
		return client_request;
	}

	public HeaderBuilder getClientResponseHeader() {
		return client_response;
	}

	public void setClientResponseHeader(final HeaderBuilder client_response) {
		this.client_response = client_response;
	}

	public HeaderBuilder getServerRequestHeader() {
		return server_request;
	}

	public void setServerRequestHeader(final HeaderBuilder server_request) {
		this.server_request = server_request;
	}

	public HeaderParser getServerResponseHeader() {
		return server_response;
	}

	public void setServerResponseHeader(final HeaderParser server_response) {
		this.server_response = server_response;
	}
}
