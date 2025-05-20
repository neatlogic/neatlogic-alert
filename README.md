中文 / [English](README.en.md)



---
## 关于

neatlogic-alert是告警中心模块，主要用于收集来自不同来源的告警集中处理和展示。
neatlogic-alert不能单独部署，也不能单独构建，如需构建和部署，请参考[neatlogic-itom-all](../../../neatlogic-itom-all/blob/develop3.0.0/README.md)
的说明文档。

## 架构图
![img_1.png](README_IMAGES/img_1.png)

## 主要功能

### 告警类型管理

配置告警类型和adaptor
- 一个告警类型支持多个adaptor
- adaptor需要实现[neatlogic-alert-plugin-base](https://gitee.com/neat-logic/neatlogic-alert-plugin-base.git)接口

![img_2.png](README_IMAGES/img_2.png)

### 告警事件管理
- 目前支持的事件有：告警接入（起点事件）、创建告警、收敛告警、子告警加入、子告警移除、更新告警状态、关闭告警、打开告警、删除告警，将来根据需要可能会增加新的事件。
- 目前支持的插件有：创建告警、分配处理人、条件判断、定时调度、邮件通知、集成调用等，将来根据需要可能会增加新的事件插件，同时支持在自定义项目中实现个性化事件插件。

### 扩展属性管理
- 支持数字、文本、枚举和时间日期四种格式
- 枚举类型能自动保存数据作为搜索条件使用

![img.png](README_IMAGES/img_3.png)

### 告警视图
告警视图用于根据不同角色配置可查看的告警列表。
- 支持配置条件过滤告警数据
- 支持定义可视字段
- 支持授权限制使用范围

![img.png](README_IMAGES/img_4.png)

## 功能列表
<table><tr>
<td>编号</td><td>分类</td><td>功能点</td><td>说明</td></tr>
</table>
...待补充


