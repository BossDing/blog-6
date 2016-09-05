package me.qyh.blog.oauth2;

import java.sql.Timestamp;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import me.qyh.blog.entity.Id;
import me.qyh.blog.input.JsonHtmlXssSerializer;
import me.qyh.blog.message.Message;

/**
 * 这里没有保存凭证<br>
 * 不同的oauth服务商提供的凭证不同，而且保存凭证并没有多大的意义<br>
 * 在绑定账户中需要更新用户信息需要用到凭证，但是让该社交账号再次走oauth登录流程即可。<br>
 * 除此之外，再无用到，所以这里不存储第三方凭证
 * 
 * @author mhlx
 *
 */
public class OauthUser extends Id {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String oauthid;
	@JsonSerialize(using = JsonHtmlXssSerializer.class)
	private String nickname;
	private String avatar;
	private OauthUserStatus status;
	private Timestamp registerDate;// 注册日期
	private String serverId;
	private String serverName;

	private Boolean admin;

	public enum OauthUserStatus {
		NORMAL(new Message("oauth.status.normal", "正常")), DISABLED(new Message("oauth.status.disabled", "禁用"));// 禁用

		private Message message;

		private OauthUserStatus(Message message) {
			this.message = message;
		}

		private OauthUserStatus() {

		}

		public Message getMessage() {
			return message;
		}
	}

	public String getOauthid() {
		return oauthid;
	}

	public void setOauthid(String oauthid) {
		this.oauthid = oauthid;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getAvatar() {
		return avatar;
	}

	public void setAvatar(String avatar) {
		this.avatar = avatar;
	}

	public Boolean getAdmin() {
		return admin;
	}

	public void setAdmin(Boolean admin) {
		this.admin = admin;
	}

	public String getServerId() {
		return serverId;
	}

	public void setServerId(String serverId) {
		this.serverId = serverId;
	}

	public OauthUser() {

	}

	public OauthUser(UserInfo info) {
		this.avatar = info.getAvatar();
		this.nickname = info.getNickname();
		this.oauthid = info.getId();
	}

	public OauthUserStatus getStatus() {
		return status;
	}

	public void setStatus(OauthUserStatus status) {
		this.status = status;
	}

	public boolean isDisabled() {
		return OauthUserStatus.DISABLED.equals(status);
	}

	public Timestamp getRegisterDate() {
		return registerDate;
	}

	public void setRegisterDate(Timestamp registerDate) {
		this.registerDate = registerDate;
	}

	public String getServerName() {
		return serverName;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

}
