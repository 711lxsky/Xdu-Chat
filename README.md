# XDU-CHAT数据格式转换与漫游中台

## 项目说明
此项目原本在上半年用于承接西电自研GPT，进行数据中转格式转换、存储以及流式媒体传输实现

现已整体用`GO`语言迁移重构，但是因为大模型端部分接口未鉴权，为了防范攻击暂时不会开源

**注意**： ⚠本项目请求大模型端的接口均已废弃，仅作为需要者参考，如有更多需要请Issue

## 技术架构
很普通的一个基于`SpringBoot`的单体架构，特别一点儿的就是实现了`SSE`传输，详见服务层`Proxy`方法