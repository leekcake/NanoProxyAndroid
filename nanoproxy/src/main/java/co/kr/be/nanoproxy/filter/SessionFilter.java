package co.kr.be.nanoproxy.filter;

import java.io.BufferedOutputStream;
import java.io.IOException;

public abstract class SessionFilter {
	/**
	 * 받은 POST 요청에 대해서 POST의 데이터를 전달할 필요가 있는지를 반환해야 합니다
	 *
	 * @return 값을 따로 전달해야 하는가?
	 */
	public abstract boolean needRequestPostData(FilterInformation info);

	/**
	 * GET 요청을 처리하기 전에 호출됩니다<br>
	 * 결과값을 바꾸기 위해 (set)getServerRequestHeader 를 사용할 수 있습니다.
	 *
	 */
	public abstract void onClientGetRequest(FilterInformation info);

	/**
	 * POST 요청을 처리하기 전에 needRequestPostData가 false인 경우 호출됩니다<br>
	 * 결과값을 바꾸기 위해 (set)getServerRequestHeader 를 사용할 수 있습니다.
	 *
	 */
	public abstract void onClientPostRequest(FilterInformation info);

	/**
	 * POST 요청을 처리하기 전에 needRequestPostData가 true인 경우 호출됩니다<br>
	 * 결과값을 바꾸기 위해 (set)getServerRequestHeader 를 사용할 수 있습니다.
	 *
	 * @param postData
	 *            POST 요청 후에 들어온 값
	 */
	public abstract void onClientPostRequestwithPostData(FilterInformation info);

	/**
	 * 서버에 요청을 보내기 전에 호출됩니다.<br>
	 * 결과값을 바꾸기 위해 (set)getServerRequestHeader 를 사용할 수 있습니다.
	 *
	 */
	public abstract void onServerResponse(FilterInformation info);

	/**
	 * 이 요청에 대해 본문의 값이 필요한지의 여부를 반환합니다
	 *
	 * @return 본문이 필요한가?
	 */
	public abstract boolean needResponseBody(FilterInformation info);

	/**
	 * 본문의 값이 필요한 요청에 대해서 본문의 값을 전달합니다.<br>
	 * 값을 수정하기 위해서는 Override를 사용해야 합니다
	 *
	 * @param body
	 *            받은 본문
	 */
	public abstract void onServerResponseBody(FilterInformation info, byte[] body);

	/**
	 * 이 요청에 대해 원격 서버에 요청을 보내지 않고 대신 응답할지의 여부를 반환합니다
	 *
	 * @return 대신 응답해야 하는가?
	 */
	public abstract boolean needResponseOverride(FilterInformation info);

	/**
	 * 대신 응답해야 하는 요정을 대신 응답합니다
	 *
	 * @param bos
	 *            클라이언트 출력 Stream
	 * @throws IOException
	 *             IO 관련 오류를 위에서 캐치
	 */
	public abstract void onServerResponseOverride(FilterInformation info, BufferedOutputStream bos) throws IOException;
}
