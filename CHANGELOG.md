# Changelog

## 下一开发版本

- 统一命令入口到 `/tzz`。
- 新增 `/tzz regionctl` 区域事件控制器命令。
- 支持将已有规划区域绑定为区域控制器。
- 支持玩家进入区域触发 `enterActions`。
- 支持玩家离开区域触发 `exitActions`。
- 支持玩家停留区域周期触发 `stayActions`。
- 区域动作接入 `ActionEngine` 统一执行。
- 支持区域控制器启用、禁用、删除、动作测试、目标过滤。
- 支持 `all`、`op`、`tag` 三种触发对象过滤。
- 新增 `region_controllers.json` 持久化存储。
- `/tzz regionctl` 命令反馈已中文化、颜色化，并优先显示名称和短 ID。
- 保留封锁卡原有使用方式，封锁卡命令执行已接入 `ActionEngine`。
- 补充 RegionController 使用说明和最小验收流程文档。

- 新增 SignalBridge 事件桥系统。
- 新增 `/tzz signal` 命令。
- 支持 signal listener 创建、删除、启用、禁用、查看、测试。
- 支持 listener 绑定 command action。
- 支持 listener 绑定 signal action，实现链式事件。
- 支持 listener cooldown。
- 支持 signal 递归保护。
- RegionController `addAction` 已支持 signal 类型。
- 支持 RegionController -> SignalBridge -> Listener -> ActionEngine 联动。
- `/tzz signal` 命令反馈已中文化、颜色化、名称优先。
- 补充 SignalBridge 使用说明和常见问题文档。

## 1.1.5

- 同步当前 GitHub Release 版本。
- 保留现有手机、AR、封锁卡、地图、任务、密码、图库、笔记等功能。
