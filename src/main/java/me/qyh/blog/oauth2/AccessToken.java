package me.qyh.blog.oauth2;

public class AccessToken {

	private String token;// token
	private long expireIn;// 过期时间(s)

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public long getExpireIn() {
		return expireIn;
	}

	public void setExpireIn(long expireIn) {
		this.expireIn = expireIn;
	}
}