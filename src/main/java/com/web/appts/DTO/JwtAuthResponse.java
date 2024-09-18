
package com.web.appts.DTO;

public class JwtAuthResponse {
	private String token;
	private UserDto user;

	public JwtAuthResponse() {
	}

	public String getToken() {
		return this.token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public UserDto getUser() {
		return this.user;
	}

	public void setUser(UserDto user) {
		this.user = user;
	}
}
