package com.example.emos.wx.controller.form;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
@ApiModel
public class RegisterForm {
    @NotBlank(message = "Register Code is required.")
    @Pattern(regexp = "^[0-9]{6}$",message = "Code must be 6 digits.")
    private String registerCode;

    @NotBlank(message = "Wechat authorization is required.")
    private String code;

    @NotBlank(message = "Nickname is required.")
    private String nickname;

    @NotBlank(message = "Photo is required.")
    private String photo;

}
