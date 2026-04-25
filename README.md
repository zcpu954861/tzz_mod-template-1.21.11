# Tzz_mod

Tzz_mod（mod id: `tzz_mod`）是用于适配“全员逃走中”数据包和服务器玩法的 Fabric mod。模组提供手机、AR、地图区域、任务、封锁卡、动作执行和区域事件控制等服务端与客户端能力。

- 最新发布版本：`1.1.5`
- 当前开发版本：以 `gradle.properties` 的 `mod_version` 为准
- 作者：`zcpu`
- 目标 Minecraft：`1.21.11`
- 依赖：Fabric Loader `>=0.18.4`，Fabric API `0.141.3+1.21.11`
- 许可证：`CC0-1.0`

## 主要功能

- 手机系统：地图、聊天、任务、图库、呼叫管理员和设置等内置 App。
- AR 头显：提供空间化的应用入口和调试展示能力。
- 地图与区域工具：创建地图标点、规划区域，并同步到客户端地图。
- 任务配置器：配合数据包创建和编辑任务线。
- 封锁卡系统：保存触发条件和命令动作，并在命中实体或方块条件时执行。
- ActionEngine：统一执行命令、消息、音效等动作。
- RegionController：为已有规划区域绑定进入、离开、停留事件动作。

## 命令入口

当前主要命令入口已经统一到 `/tzz`：

```text
/tzz map ...
/tzz task ...
/tzz note ...
/tzz sendmsg ...
/tzz regionctl ...
```

旧根命令已迁移到 `/tzz` 子命令下；当前代码不再注册旧的 `/map`、`/task`、`/note`、`/sendmsg` 根命令。

## RegionController

RegionController 是“区域事件控制器”，用于让已有规划区域拥有逻辑触发能力：

```text
已有规划区域
-> 创建区域控制器
-> 玩家进入区域触发 enterActions
-> 玩家离开区域触发 exitActions
-> 玩家停留区域触发 stayActions
-> 动作通过 ActionEngine 执行
```

RegionController 不改变区域本身数据。`PlannerRegionData` 仍然只负责区域形状、名称、维度等地图数据；`RegionControllerData` 单独保存触发逻辑。

完整使用说明见 [docs/region_controller.md](docs/region_controller.md)。

### 快速示例

```text
/tzz regionctl regions
/tzz regionctl create <区域名称或区域ID> A区控制器
/tzz regionctl addAction A区控制器 enter command say 玩家进入A区
/tzz regionctl addAction A区控制器 exit command say 玩家离开A区
/tzz regionctl addAction A区控制器 stay command say 玩家仍在A区
/tzz regionctl stayInterval A区控制器 100
/tzz regionctl target A区控制器 all
/tzz regionctl test A区控制器 enter
```

### 触发对象过滤

- `all`：所有玩家触发。
- `op`：只有 OP 玩家触发。
- `tag <tagName>`：只有拥有指定 scoreboard tag 的玩家触发。

示例：

```text
/tzz regionctl target A区控制器 tag runner
```

### STAY 语义

`stayActions` 是玩家持续停留在区域内时周期触发的动作。

- 默认间隔为 `100 tick`。
- 最小间隔为 `20 tick`。
- 进入区域后不会立刻触发 `stay`，而是在达到间隔后触发。

### 事件语义

- 玩家第一次被扫描时，不触发 `ENTER`。
- 玩家退出服务器时，不触发 `EXIT`。
- 玩家跨维度时，对原区域触发 `EXIT`。
- 玩家传送跨过边界，也会触发 `ENTER` / `EXIT`。
- 区域边界是否算区域内，沿用现有区域几何判断。

### 配置文件

RegionController 配置保存到：

```text
world/tzz_mod/region_controllers.json
```

该文件由模组自动维护，不建议手动编辑，除非你熟悉当前 JSON 结构。

## 最小验收流程

1. 创建一个规划区域。
2. 执行 `/tzz regionctl regions`。
3. 执行 `/tzz regionctl create <region> 测试控制器`。
4. 添加 `enter` 动作。
5. 添加 `exit` 动作。
6. 执行 `/tzz regionctl test <controller> enter`。
7. 实际走入区域。
8. 实际走出区域。
9. 添加 `stay` 动作并测试。
10. 重启世界后确认配置仍存在。

## 物品与使用

- `phone`：右键打开手机界面。
- `ar_headset`：可装备到头部，右键打开 AR 界面。
- `attention`：右键播放提示音并将玩家朝向对齐到最近的 90 度方向。
- `*_blocking_card`：保存实体或方块触发配置，并在满足条件时执行动作。
- `blocking_card_configurator`：批量装入、取出和配置封锁卡。
- `password_config_card`：打开密码配置界面。
- `map_marker`：添加地图标点。
- `region_planner`：创建和编辑规划区域。
- `task_configurator`：创建和编辑任务配置。

## 开发与构建

要求：JDK 21、Fabric Loader、Fabric API。

运行客户端：

```bash
./gradlew.bat runClient
```

构建：

```bash
./gradlew.bat build
```

完整验证：

```bash
./gradlew.bat clean build
```

构建产物位于 `build/libs/`。

## 贡献与许可

欢迎提交 Issue 和 Pull Request。建议先使用 `runClient` 本地调试。

许可证：`CC0-1.0`，详见 [LICENSE](LICENSE)。
