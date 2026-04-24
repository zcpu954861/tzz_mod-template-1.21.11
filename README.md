# Tzz_mod

**简介**

Tzz_mod（mod id: `tzz_mod`）是用于适配“全员逃走中”数据包的 Fabric mod，需与专属数据包配合使用。模组提供手机与 AR 界面、地图/区域/任务工具、以及可配置的“封锁卡”触发系统，用于在服务器端按条件执行命令和联动数据包逻辑。

- 最新发布版本: `1.1.5`
- 当前开发版本: 以 `gradle.properties` 的 `mod_version` 为准
- 作者: `zcpu`
- 目标 Minecraft: `1.21.11`
- 依赖: Fabric Loader `>=0.18.4`，Fabric API `0.141.3+1.21.11`
- 许可证: `CC0-1.0`

**主要功能（概览）**

- 手机系统：地图、聊天（含图片）、任务、图库、呼叫管理员等内置 App。客户端交互丰富，和服务端数据同步。
- AR 头显：可装备并打开增强现实界面，显示 3D 应用图标与快捷操作。
- 封锁卡系统：为一组可堆叠的“封锁卡”保存触发条件与服务器命令，在匹配实体或方块条件时自动触发执行命令。
- 卡片配置器：一次性装入/取出一组封锁卡并对其进行批量配置与保存。
- 地图与区域工具：添加地图标点、规划区域并广播到客户端地图。
- 任务配置器：面向数据包的任务创建/编辑界面（与数据包配合使用）。

**物品与使用（重点）**

- `phone`（手机，`tzz_mod:phone`）
  - 用途：右键打开手机界面（客户端）。包含地图、聊天、任务、图库、呼叫管理员和设置等 App。

- `ar_headset`（AR 头戴，`tzz_mod:ar_headset`）
  - 用途：可装备于头部；右键打开 AR 界面以获取空间化的交互与 3D 应用图标显示。

- `attention`（立正器，`tzz_mod:attention`）
  - 用途：右键即播放提示音并将玩家朝向对齐到最接近的 90°（南/西/北/东），服务端同步。

- 多色 `*_blocking_card`（封锁卡，e.g. `tzz_mod:white_blocking_card`，最大堆叠 64）
  - 用途：每张封锁卡可保存触发配置（激活类型、条件内容、要执行的命令、是否通知 OP）。
  - 触发类型：`entity`（实体，支持选择器或实体类型）、`block`（方块，支持 block[state]{nbt} 格式）、`disabled`（禁用）。
  - 动作：当满足条件时在服务端执行配置的命令（命令字符串不应包含前导 `/`）。可选择将激活信息通知具备 OP 权限的玩家。
  - 提示信息：已配置的封锁卡在物品提示呈现金色提示文本。

- `blocking_card_configurator`（卡片配置器，`tzz_mod:blocking_card_configurator`）
  - 用途：用于批量装入/取出并配置封锁卡。普通右键打开配置界面（客户端）；潜行+右键用于将主/副手的封锁卡整组存入或取出。
  - 使用流程：在副手拿着一叠封锁卡 → 潜行+右键配置器以装入；装入后打开配置界面保存触发设置，保存会把配置写入该组卡片。

- `password_config_card`（密码配置卡，`tzz_mod:password_config_card`）
  - 用途：右键打开密码配置界面（客户端）；卡内密码保存在物品的 Custom Data（键: `password_code`），与密码机/静默感应板等组件配合使用。

- `map_marker`（地图标点器，`tzz_mod:map_marker`）
  - 用途：潜行右键打开地图标点 UI；对方块普通右键可在该位置添加地图标点（服务端保存并广播）。

- `region_planner`（区域规划器，`tzz_mod:region_planner`）
  - 用途：潜行右键打开区域规划 UI；普通右键选择方块并在服务端记录区域选择（与地图/任务联动）。

- `task_configurator`（任务配置器，`tzz_mod:task_configurator`）
  - 用途：右键打开任务配置 UI（客户端），用于创建/编辑与数据包对应的任务。

- `app_icon_*`（若干应用图标项，比如 `app_icon_map` 等）
  - 用途：用于 AR/界面渲染的占位/渲染项，通常不可直接获得。

**封锁卡配置示例**

- 实体触发示例：激活类型 `entity`，激活输入可写 `minecraft:player[tag=guard]` 或 `@e[type=minecraft:cow,tag=test]`。
- 方块触发示例：激活输入可写 `minecraft:chest[facing=north]{Lock:"test"}`。
- 命令示例：`say 封锁卡触发`（写入时不要加 `/`）。

**开发与构建（快速）**

- 要求：JDK 21、Fabric Loader、Fabric API。
- 运行客户端（开发）:

```bash
./gradlew.bat runClient
```

- 打包构建:

```bash
./gradlew.bat build
```

构建产物位于 `build/libs/`，Loom 会生成并重映射 jar。

**贡献与许可**

- 欢迎提交 Issue 与 Pull Request。建议先 Fork 并使用 `runClient` 本地调试。
- 许可：CC0-1.0（详见 LICENSE）。
