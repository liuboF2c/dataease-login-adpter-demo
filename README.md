## 同域模拟登录使用方式
> 1. 修改 DataEaseLoginController 中的相关变量
> 2. 修改本地 hosts，使本服务与 DataEase 处于同一根域名下
> 3. 启动此系统
> 4. 访问此系统，如：http://localhost:8080

## 注意
> - 本服务默认端口是 8080
> - 使用同域嵌入时本服务与 DataEase 服务必须处于同一域名下方可生效
> - 使用跨域嵌入时，必须配置 Nginx 本服务与 DataEase 服务必须处于同一域名下方可生效
> - 本示例基于 DataEase V1.12 版本开发

## 说明
- [DataEaseLoginController.java](src%2Fmain%2Fjava%2Fdataease%2Flboo%2Fdemo%2Fcontroller%2FDataEaseLoginController.java) 主要业务逻辑
- [util](src%2Fmain%2Fjava%2Fdataease%2Flboo%2Fdemo%2Futil) 存放工具
- [LoginAdapterDemoApplication.java](src%2Fmain%2Fjava%2Fdataease%2Flboo%2Fdemo%2FLoginAdapterDemoApplication.java) 服务启动类

- [jump-demo.html](src%2Fmain%2Fresources%2Fstatic%2Fjump-demo.html) 跨域嵌入示例页面
- [index.html](src%2Fmain%2Fresources%2Ftemplates%2Findex.html) 同域嵌入示例页面
- [login-template.html](src%2Fmain%2Fresources%2Ftemplates%2Flogin-template.html) 用于跨域嵌入设置 Cookie
- [logout-template.html](src%2Fmain%2Fresources%2Ftemplates%2Flogout-template.html) 用于跨域嵌入删除 Cookie
