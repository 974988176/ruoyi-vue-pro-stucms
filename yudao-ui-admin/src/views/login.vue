<template>
  <div class="container">
    <div class="logo"></div>
    <!-- 登录区域 -->
    <div class="content">
      <!-- 配图 -->
      <div class="pic"></div>
      <!-- 表单 -->
      <div class="field">
        <!-- [移动端]标题 -->
        <h2 class="mobile-title">
          <h3 class="title">学生管理系统</h3>
        </h2>

        <!-- 表单 -->
        <div class="form-cont">
          <el-tabs class="form" v-model="loginForm.loginType" style=" float:none;">
            <el-tab-pane label="账号密码登录" name="uname">
            </el-tab-pane>
          </el-tabs>
          <div>
            <el-form ref="loginForm" :model="loginForm" :rules="LoginRules" class="login-form">
              <el-form-item prop="tenantName" v-if="tenantEnable">
                <el-input v-model="loginForm.tenantName" type="text" auto-complete="off" placeholder='租户'>
                  <svg-icon slot="prefix" icon-class="tree" class="el-input__icon input-icon"/>
                </el-input>
              </el-form-item>
              <!-- 账号密码登录 -->
              <div>
                <el-form-item prop="username">
                  <el-input v-model="loginForm.username" type="text" auto-complete="off" placeholder="账号">
                    <svg-icon slot="prefix" icon-class="user" class="el-input__icon input-icon"/>
                  </el-input>
                </el-form-item>
                <el-form-item prop="password">
                  <el-input v-model="loginForm.password" type="password" auto-complete="off" placeholder="密码"
                            @keyup.enter.native="handleLogin">
                    <svg-icon slot="prefix" icon-class="password" class="el-input__icon input-icon"/>
                  </el-input>
                </el-form-item>
                <el-form-item prop="code" v-if="captchaEnable">
                  <el-input v-model="loginForm.code" auto-complete="off" placeholder="验证码" style="width: 63%"
                            @keyup.enter.native="handleLogin">
                    <svg-icon slot="prefix" icon-class="validCode" class="el-input__icon input-icon"/>
                  </el-input>
                  <div class="login-code">
                    <img :src="codeUrl" @click="getCode" class="login-code-img"/>
                  </div>
                </el-form-item>
                <el-checkbox v-model="loginForm.rememberMe" style="margin:0 0 25px 0;">记住密码</el-checkbox>
              </div>
              <!-- 下方的登录按钮 -->
              <el-form-item style="width:100%;">
                <el-button :loading="loading" size="medium" type="primary" style="width:100%;"
                    @click.native.prevent="handleLogin">
                  <span v-if="!loading">登 录</span>
                  <span v-else>登 录 中...</span>
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </div>
    </div>
    <!-- footer -->
    <div class="footer">
      Copyright © 2020-2022 iocoder.cn All Rights Reserved.
    </div>
  </div>
</template>

<script>
import {getCodeImg, sendSmsCode, socialAuthRedirect} from "@/api/login";
import {getTenantIdByName} from "@/api/system/tenant";
import {SystemUserSocialTypeEnum} from "@/utils/constants";
import {getTenantEnable} from "@/utils/ruoyi";
import {
  getPassword,
  getRememberMe,
  getTenantName,
  getUsername,
  removePassword,
  removeRememberMe,
  removeTenantName,
  removeUsername,
  setPassword,
  setRememberMe,
  setTenantId,
  setTenantName,
  setUsername
} from "@/utils/auth";

export default {
  name: "Login",
  data() {
    return {
      codeUrl: "",
      captchaEnable: true,
      tenantEnable: true,
      mobileCodeTimer: 0,
      loginForm: {
        loginType: "uname",
        username: "admin",
        password: "admin123",
        mobile: "",
        mobileCode: "",
        rememberMe: false,
        code: "",
        uuid: "",
        tenantName: "芋道源码",
      },
      scene: 21,

      LoginRules: {
        username: [
          {required: true, trigger: "blur", message: "用户名不能为空"}
        ],
        password: [
          {required: true, trigger: "blur", message: "密码不能为空"}
        ],
        code: [{required: true, trigger: "change", message: "验证码不能为空"}],
        mobile: [
          {required: true, trigger: "blur", message: "手机号不能为空"},
          {
            validator: function (rule, value, callback) {
              if (/^(?:(?:\+|00)86)?1(?:(?:3[\d])|(?:4[5-79])|(?:5[0-35-9])|(?:6[5-7])|(?:7[0-8])|(?:8[\d])|(?:9[189]))\d{8}$/.test(value) === false) {
                callback(new Error("手机号格式错误"));
              } else {
                callback();
              }
            }, trigger: "blur"
          }
        ],
        tenantName: [
          {required: true, trigger: "blur", message: "租户不能为空"},
          {
            validator: (rule, value, callback) => {
              // debugger
              getTenantIdByName(value).then(res => {
                const tenantId = res.data;
                if (tenantId && tenantId >= 0) {
                  // 设置租户
                  setTenantId(tenantId)
                  callback();
                } else {
                  callback('租户不存在');
                }
              });
            },
            trigger: 'blur'
          }
        ]
      },
      loading: false,
      redirect: undefined,
      // 枚举
      SysUserSocialTypeEnum: SystemUserSocialTypeEnum,
    };
  },
  // watch: {
  //   $route: {
  //     handler: function(route) {
  //       this.redirect = route.query && route.query.redirect;
  //     },
  //     immediate: true
  //   }
  // },
  created() {
    // 租户开关
    this.tenantEnable = getTenantEnable();
    // 重定向地址
    this.redirect = this.$route.query.redirect;
    this.getCode();
    this.getCookie();
  },
  methods: {
    getCode() {
      // 只有开启的状态，才加载验证码。默认开启
      if (!this.captchaEnable) {
        return;
      }
      // 请求远程，获得验证码
      getCodeImg().then(res => {
        res = res.data;
        this.captchaEnable = res.enable;
        if (this.captchaEnable) {
          this.codeUrl = "data:image/gif;base64," + res.img;
          this.loginForm.uuid = res.uuid;
        }
      });
    },
    getCookie() {
      const username = getUsername();
      const password = getPassword();
      const rememberMe = getRememberMe();
      const tenantName = getTenantName();
      this.loginForm = {
        ...this.loginForm,
        username: username ? username : this.loginForm.username,
        password: password ? password : this.loginForm.password,
        rememberMe: rememberMe ? getRememberMe() : false,
        tenantName: tenantName ? tenantName : this.loginForm.tenantName,
      };
    },
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          this.loading = true;
          // 设置 Cookie
          if (this.loginForm.rememberMe) {
            setUsername(this.loginForm.username)
            setPassword(this.loginForm.password)
            setRememberMe(this.loginForm.rememberMe)
            setTenantName(this.loginForm.tenantName)
          } else {
            removeUsername()
            removePassword()
            removeRememberMe()
            removeTenantName()
          }
          // 发起登陆
          // console.log("发起登录", this.loginForm);
          this.$store.dispatch("Login", this.loginForm).then(() => {
            this.$router.push({path: this.redirect || "/"}).catch(() => {
            });
          }).catch(() => {
            this.loading = false;
            this.getCode();
          });
        }
      });
    },
  }
};
</script>
<style lang="scss" scoped>
@import "~@/assets/styles/login.scss";


.oauth-login {
  display: flex;
  align-items: cen;
  cursor:pointer;
}
.oauth-login-item {
  display: flex;
  align-items: center;
  margin-right: 10px;
}
.oauth-login-item img {
  height: 25px;
  width: 25px;
}
.oauth-login-item span:hover {
  text-decoration: underline red;
  color: red;
}
</style>
