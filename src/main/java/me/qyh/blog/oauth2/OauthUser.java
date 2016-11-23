/*
 * Copyright 2016 qyh.me
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.qyh.blog.oauth2;

import java.sql.Timestamp;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import me.qyh.blog.entity.BaseEntity;
import me.qyh.blog.input.JsonHtmlXssSerializer;
import me.qyh.blog.message.Message;
import me.qyh.util.Validators;

/**
 * 这里没有保存凭证<br>
 * 不同的oauth服务商提供的凭证不同，而且保存凭证并没有多大的意义<br>
 * 在绑定账户中需要更新用户信息需要用到凭证，但是让该社交账号再次走oauth登录流程即可。<br>
 * 除此之外，再无用到，所以这里不存储第三方凭证
 * 
 * @author Administrator
 *
 */
public class OauthUser extends BaseEntity {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String oauthid;
	@JsonSerialize(using = JsonHtmlXssSerializer.class)
	private String nickname;
	private String avatar;
	private OauthUserStatus status;
	private Timestamp registerDate;
	private String serverId;
	private String serverName;
	private Boolean admin;
	private String email;

	/**
	 * oauth用户状态
	 * 
	 * @author Administrator
	 *
	 */
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

	/**
	 * default
	 */
	public OauthUser() {
		super();
	}

	/**
	 * 
	 * @param info
	 *            用户信息
	 */
	public OauthUser(UserInfo info) {
		this.avatar = info.getAvatar();
		this.nickname = info.getNickname();
		this.oauthid = info.getId();
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

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public boolean hasEmail() {
		return !Validators.isEmptyOrNull(email, true);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(id).build();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj.getClass() != getClass()) {
			return false;
		}
		OauthUser rhs = (OauthUser) obj;
		return new EqualsBuilder().append(id, rhs.id).isEquals();
	}
}
