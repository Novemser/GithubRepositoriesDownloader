## Github 代码仓库下载器
---
Java语言实现的多线程下载器 理论上可以下载Github所有仓库的代码   
链接、数据的获取使用到了Github API  
具体请参考https://developer.github.com/v3/
### 主要特点：
 - 支持多线程下线
 - 支持自动爬取代理并
 - 可自定义下载语言
 - 两种下载方式：
  - 使用搜索API下载，只能下载990条记录
  - 遍历整个Github仓库，选取指定语言的仓库
 - 指定输出目录
 - 较为详尽的Log文件

##### By: Novemser
##### 2016
##### Tongji 409 GitLab
