# RegionController 使用说明

RegionController 是区域事件控制器。它把已有的规划区域和一组动作绑定起来，让玩家进入、离开或停留在区域内时自动执行动作。

RegionController 不修改规划区域本身。`PlannerRegionData` 继续保存区域形状、名称、维度等地图数据；`RegionControllerData` 单独保存控制器名称、绑定区域、目标过滤、停留间隔和动作列表。

## 创建规划区域

先用 `region_planner` 物品在游戏内创建规划区域。规划区域创建完成后，可以通过命令查看现有区域：

```text
/tzz regionctl regions
```

输出中会显示区域名称、短 ID、维度和点数。后续创建控制器时可以使用区域名称或区域 ID。

## 创建控制器

```text
/tzz regionctl create <区域名称或区域ID> A区控制器
```

创建成功后会显示控制器名称、短 ID、绑定区域和查看详情命令。控制器命令支持使用完整 ID、短 ID 或控制器名称。

## 查看控制器

查看列表：

```text
/tzz regionctl list
```

查看详情：

```text
/tzz regionctl info A区控制器
```

详情中会显示完整控制器 ID、绑定区域 ID、状态、目标过滤、停留间隔和动作数量。

## 添加动作

当前 `/tzz regionctl addAction` 只支持 `command` 动作。命令文本不要带开头的 `/`。

进入区域时执行：

```text
/tzz regionctl addAction A区控制器 enter command say 玩家进入A区
```

离开区域时执行：

```text
/tzz regionctl addAction A区控制器 exit command say 玩家离开A区
```

停留区域时周期执行：

```text
/tzz regionctl addAction A区控制器 stay command say 玩家仍在A区
```

清空某类动作：

```text
/tzz regionctl clearActions A区控制器 enter
/tzz regionctl clearActions A区控制器 exit
/tzz regionctl clearActions A区控制器 stay
```

## 设置触发对象

RegionController 支持三种目标过滤：

- `all`：所有玩家触发。
- `op`：只有 OP 玩家触发。
- `tag <tagName>`：只有拥有指定 scoreboard tag 的玩家触发。

示例：

```text
/tzz regionctl target A区控制器 all
/tzz regionctl target A区控制器 op
/tzz regionctl target A区控制器 tag runner
```

`tag` 使用玩家的 scoreboard tag。可以用原版命令给玩家添加标签，例如：

```text
/tag <玩家名> add runner
```

## 设置 STAY 间隔

`stayActions` 会在玩家持续停留在区域内时周期触发。

- 默认间隔：`100 tick`。
- 最小间隔：`20 tick`。
- 玩家刚进入区域时不会立刻触发 `stay`，达到间隔后才会触发。

设置间隔：

```text
/tzz regionctl stayInterval A区控制器 100
```

如果传入小于 `20` 的值，命令会拒绝并提示“停留触发间隔不能低于 20 tick”。

## 测试动作

可以手动测试某个触发类型的动作：

```text
/tzz regionctl test A区控制器 enter
/tzz regionctl test A区控制器 exit
/tzz regionctl test A区控制器 stay
```

测试命令会用当前执行命令的玩家、世界和坐标构造动作上下文。

## 启用、禁用和删除

```text
/tzz regionctl enable A区控制器
/tzz regionctl disable A区控制器
/tzz regionctl delete A区控制器
```

禁用后控制器不会参与自动扫描触发，但配置仍会保留。

## 事件语义

- 玩家第一次被扫描时，不触发 `ENTER`。
- 玩家退出服务器时，不触发 `EXIT`。
- 玩家跨维度时，对原区域触发 `EXIT`。
- 玩家传送跨过边界，也会触发 `ENTER` / `EXIT`。
- 区域边界是否算区域内，沿用现有区域几何判断。

## 配置文件

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

## 常见问题

### `/tzz regionctl create` 找不到区域

先执行 `/tzz regionctl regions`，确认规划区域已经保存，并使用输出中的区域名称或 ID。

### `stay` 没有立刻执行

这是预期行为。进入区域不会立刻触发 `stay`，需要等待配置的 `stayInterval`。

### `tag` 过滤没有触发

确认玩家拥有对应 scoreboard tag。可以用 `/tag <玩家名> list` 检查。

### 命令动作保存失败

`addAction ... command <command>` 的命令文本不要带开头的 `/`，并确认命令在服务器命令系统中可解析。
