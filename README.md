## 同域模拟登录使用方式
> 1. 修改 DataEaseLoginController 中的相关变量
> 2. 修改本地 hosts，使本服务与 DataEase 处于同一根域名下
> 3. 启动此系统
> 4. 访问此系统，例：http://localhost:8080

## 跨域模拟登录使用方式
> 1. 修改 DataEaseLoginController 中的相关变量
> 2. 部署并配置 Nginx，使用 Nginx 代理访问 DataEase，并配置 HTTPS（HTTPS 配置可参考 [配置 DataEase 使用 HTTPS 访问](https://kb.fit2cloud.com/?p=9)）
> 3. 将本项目中的 login-template.html 和 logout-template.html 放置到 Nginx 所在服务器，并配置静态访问地址
```shell
    # Nginx 配置参考：
    location / {
        proxy_pass <DataEase服务器地址>;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header Host $http_host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
    
    location /sso {
        # 请将此配置 [./src/main/resources/templates] 修改为 login-template.html 和 logout-template.html 存放路径
        # login-template.html 和 logout-template.html 取自本项目 src/main/resources/templates/ 目录
        # 将其与 nginx 放置于同一服务器，然后在 nginx 配置文件中将此配置项填写为 login-template.html 和 logout-template.html 的所在目录;
        alias   ./src/main/resources/templates
        index   login-template.html;
    }
```
> 4. 本服务与 DataEase 处于不同域名下
> 5. 启动此系统
> 6. 访问此系统，例：http://localhost:8080/jump-demo.html

## 注意
> - 本服务默认端口是 8080
> - 使用同域嵌入时本服务与 DataEase 服务必须处于同一域名下方可生效
> - 使用跨域嵌入时，必须配置使用 Nginx 代理访问 DataEase 服务，且配置 HTTPS 协议
> - 本示例基于 DataEase V1.18.8 版本开发，旧版本需修改 DataEaseLoginController 中的 updatePassword 方法

## 说明
- [DataEaseLoginController.java](src%2Fmain%2Fjava%2Fdataease%2Flboo%2Fdemo%2Fcontroller%2FDataEaseLoginController.java) 主要业务逻辑
- [util](src%2Fmain%2Fjava%2Fdataease%2Flboo%2Fdemo%2Futil) 存放工具类
- [LoginAdapterDemoApplication.java](src%2Fmain%2Fjava%2Fdataease%2Flboo%2Fdemo%2FLoginAdapterDemoApplication.java) 服务启动类

- [jump-demo.html](src%2Fmain%2Fresources%2Fstatic%2Fjump-demo.html) 跨域嵌入示例页面
- [index.html](src%2Fmain%2Fresources%2Ftemplates%2Findex.html) 同域嵌入示例页面
- [login-template.html](src%2Fmain%2Fresources%2Ftemplates%2Flogin-template.html) 用于跨域嵌入设置 Cookie
- [logout-template.html](src%2Fmain%2Fresources%2Ftemplates%2Flogout-template.html) 用于跨域嵌入删除 Cookie
