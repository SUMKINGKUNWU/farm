package com.farm.exchange.user;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,32}$", message = "用户名只能包含字母、数字、下划线，长度 3-32")
    private String username;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 64, message = "昵称最多 64 个字符")
    private String nickname;

    @NotBlank(message = "登录密码不能为空")
    @Size(min = 6, max = 64, message = "登录密码长度必须是 6-64")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

