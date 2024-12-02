
<!-- PROJECT LOGO -->
<br />
<div align="center">
  <a href="https://github.com/InvertGeek/MixFile">
    <img src="https://invertgeek.github.io/mixfile-doc/logo.png" alt="Logo" width="100" height="100">
  </a>
  <h1 align="center">MixFile</h1>
  <p align="center">
    无限空间,不限速,加密储存
  </p>
</div>

## MixFile 介绍
MixFile是一款可以上传加密文件并分享的安卓APP \
可实现本地加密上传,加密下载文件,在线播放加密视频 \
加密时会自动生成随机密钥进行加密,上传完成后,密钥 文件地址 文件大小等信息会附加在分享码中 \
将分享码发送给他人即可实现分享文件 \
已发布版本: https://github.com/InvertGeek/MixFile/releases \
使用帮助: https://invertgeek.github.io/mixfile-doc/help.html

## 详细原理
将图片切割为1MB的分片,然后加密隐藏到空白图片中 \
非伪装,图片都是真实有效可被解析,然后上传所有分片,最后聚合分片索引信息 \
再将信息压缩隐写到一张索引图片中,最后上传索引图片 \
下载时优先解析索引,再根据索引信息获取到所有分片链接进行逆操作即可还原文件 \
视频上传后可直接在线播放,已实现range请求,可切换进度条,智能实时解析进度对应的图片信息 \
基于kotlin协程优化并发,上传下载均可达到数十Mb每秒,和普通文件直链基本无区别

## 命令行版本
https://github.com/InvertGeek/mixfilecli

## 免责声明

+   不能百分百保证文件永久有效,请自行对重要文件做好本地备份。
+   请勿使用本项目上传不符合社会主义核心价值观的文件。
+   该项目仅用于学习和技术交流，开发者不承担任何由使用者的行为带来的法律责任。

## Github Star
[![Stargazers over time](https://starchart.cc/InvertGeek/MixFile.svg?variant=adaptive)](https://starchart.cc/InvertGeek/MixFile)