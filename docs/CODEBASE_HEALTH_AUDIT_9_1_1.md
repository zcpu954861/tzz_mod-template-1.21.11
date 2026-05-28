# TZZ 9.1.1 Codebase Health Audit

## 当前基线

- 审计日期：2026-05-28
- 当前分支：`feature/codebase-health-audit-9-1-1`
- 当前 HEAD：`533c9e35008c77b020c0df7d3f94ce08de5dd38b`
- `origin/master`：`533c9e35008c77b020c0df7d3f94ce08de5dd38b`
- baseline tag：`v1.68.0-logic-chain-global-editor-capability-completion` -> `533c9e35008c77b020c0df7d3f94ce08de5dd38b`（本地 `git rev-parse --verify` 已核验）
- 9.1 checkpoint：`67ce0d083e05a6ac57505526994e19f0591bb9c8`
- 工作区额外项：`.codex/`、`logs/` 未跟踪，本轮按规则不处理。

## 审计范围

本轮是 9.1.1 Codebase Health / Performance Stabilization 的第一阶段：只读审计与治理计划。允许新增本文档及配套计划文档；不做 runtime / WebAdmin 行为改动，不改 `src/main`，不重构实现，不启动 Minecraft，不跑 MCP scenario，不截图矩阵，不 commit / push / merge / tag。

本文和配套三份文档只描述后续 9.1.1 implementation plan / guard plan，不是当前 docs-only audit 之外的执行授权。

9.1.1 实施计划必须保持在现有 Java 字符串 WebAdmin 架构内做渐进拆分、加 guard、建立性能基线；不得迁移 React/Vite，不重写 WebAdmin，不改路由模式，不改 Logic Chain / VBD / WorldDevice / RegionController runtime 语义，不引入 9.2 typed actions / Rich Text Builder。

## 当前文档一致性发现

- `docs/LOGIC_CHAIN_GLOBAL_EDITOR_COMPLETION_9_1_CURRENT_CONTEXT.md` 与 `docs/LOGIC_CHAIN_GLOBAL_EDITOR_CAPABILITY_MATRIX_9_1.md` 已明确 9.1 是受控 typed config editor，不是 freeform graph document / Game Program AST。
- `README.md` 第 5 行仍写当前稳定版本为 `v1.67.0-legacy-datapack-parity-audit`，与本轮提示词稳定基线 `v1.68.0-logic-chain-global-editor-capability-completion` 不一致。本轮不直接改 README；9.1.1 实施阶段应把 README 一致性纳入 docs guard 或独立 docs cleanup。

## 最大文件表

统计基于 `git -c core.quotePath=false ls-files` 的已跟踪文件；未跟踪 `.codex/`、`logs/` 不纳入。

### Top 30 by bytes

| Rank | File | Bytes |
| --- | --- | ---: |
| 1 | `src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendScripts.java` | 1,984,343 |
| 2 | `src/test/java/com/zcpu/tzzmod/stabilization/StabilizationGuardTest.java` | 839,370 |
| 3 | `src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminLogicChainEditorService.java` | 318,695 |
| 4 | `src/main/resources/assets/tzz_mod/icon.png` | 231,702 |
| 5 | `src/main/resources/assets/tzz_mod/phone/apps/map/map.png` | 231,414 |
| 6 | `src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminLogicChainService.java` | 217,770 |
| 7 | `src/test/java/com/zcpu/tzzmod/webadmin/service/WebAdminLogicChainEditorServiceTest.java` | 207,866 |
| 8 | `src/main/java/com/zcpu/tzzmod/signal/device/SignalDeviceStore.java` | 177,493 |
| 9 | `src/main/java/com/zcpu/tzzmod/webadmin/WebAdminServer.java` | 164,014 |
| 10 | `README.md` | 137,444 |
| 11 | `src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendStyles.java` | 123,798 |
| 12 | `src/main/java/com/zcpu/tzzmod/signal/device/SignalDeviceCommand.java` | 102,022 |
| 13 | `src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminActionRelayActionsService.java` | 95,711 |
| 14 | `src/main/java/com/zcpu/tzzmod/client/webadmin/WebAdminSingleItemSubmitTemplateScreen.java` | 86,605 |
| 15 | `src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminVirtualBlockDeviceNativeTriggerService.java` | 85,338 |
| 16 | `src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminTemplateService.java` | 85,303 |
| 17 | `src/main/java/com/zcpu/tzzmod/webadmin/snapshot/WebAdminSnapshotService.java` | 84,057 |
| 18 | `src/main/java/com/zcpu/tzzmod/signal/device/ContainerItemConditionCommand.java` | 84,016 |
| 19 | `src/main/java/com/zcpu/tzzmod/webadmin/selection/WebAdminSelectionSessions.java` | 81,847 |
| 20 | `docs/SIGNAL_BRIDGE.md` | 81,416 |
| 21 | `src/test/java/com/zcpu/tzzmod/stabilization/LocalTestMcpFoundationGuardTest.java` | 78,904 |
| 22 | `src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminTimerService.java` | 75,298 |
| 23 | `src/main/java/com/zcpu/tzzmod/webadmin/testbridge/WebAdminTestBridgeRoutes.java` | 69,709 |
| 24 | `src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminRegionControllerService.java` | 67,596 |
| 25 | `src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminHelpCatalogService.java` | 66,420 |
| 26 | `src/main/java/com/zcpu/tzzmod/signal/device/VirtualBlockDeviceCommand.java` | 65,340 |
| 27 | `tools/tzz-test-mcp/src/tools/webadmin.ts` | 64,211 |
| 28 | `src/main/java/com/zcpu/tzzmod/condition/ConditionRegistry.java` | 61,719 |
| 29 | `net/minecraft/client/render/WorldRenderer.java` | 61,694 |
| 30 | `tools/tzz-test-mcp/src/tools/testbridge.ts` | 58,668 |

### Top 30 by line count

注：二进制资源的 line count 对维护复杂度没有实际意义，表中 PNG 仅因文件内字节换行被统计到；code-health guard 应把 binary assets 单独分类。

| Rank | File | Lines |
| --- | --- | ---: |
| 1 | `src/test/java/com/zcpu/tzzmod/stabilization/StabilizationGuardTest.java` | 12,423 |
| 2 | `src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendScripts.java` | 8,433 |
| 3 | `src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminLogicChainEditorService.java` | 5,205 |
| 4 | `src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminLogicChainService.java` | 4,257 |
| 5 | `src/main/java/com/zcpu/tzzmod/signal/device/SignalDeviceStore.java` | 3,957 |
| 6 | `src/test/java/com/zcpu/tzzmod/webadmin/service/WebAdminLogicChainEditorServiceTest.java` | 3,183 |
| 7 | `src/main/java/com/zcpu/tzzmod/webadmin/WebAdminServer.java` | 3,102 |
| 8 | `src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminActionRelayActionsService.java` | 1,853 |
| 9 | `src/main/resources/assets/tzz_mod/icon.png` | 1,755 |
| 10 | `src/main/resources/assets/tzz_mod/phone/apps/map/map.png` | 1,755 |
| 11 | `src/main/java/com/zcpu/tzzmod/client/webadmin/WebAdminSingleItemSubmitTemplateScreen.java` | 1,730 |
| 12 | `README.md` | 1,658 |
| 13 | `src/main/java/com/zcpu/tzzmod/signal/device/SignalDeviceCommand.java` | 1,652 |
| 14 | `src/main/java/com/zcpu/tzzmod/webadmin/snapshot/WebAdminSnapshotService.java` | 1,628 |
| 15 | `tools/tzz-test-mcp/src/tools/webadmin.ts` | 1,616 |
| 16 | `src/main/java/com/zcpu/tzzmod/webadmin/selection/WebAdminSelectionSessions.java` | 1,603 |
| 17 | `src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminTemplateService.java` | 1,574 |
| 18 | `tools/tzz-test-mcp/src/tools/testbridge.ts` | 1,574 |
| 19 | `docs/SIGNAL_BRIDGE.md` | 1,544 |
| 20 | `net/minecraft/client/render/WorldRenderer.java` | 1,516 |
| 21 | `src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminVirtualBlockDeviceNativeTriggerService.java` | 1,510 |
| 22 | `src/main/java/com/zcpu/tzzmod/signal/device/ContainerItemConditionCommand.java` | 1,498 |
| 23 | `src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminTimerService.java` | 1,391 |
| 24 | `src/main/java/com/zcpu/tzzmod/webadmin/testbridge/WebAdminTestBridgeRoutes.java` | 1,355 |
| 25 | `src/main/java/com/zcpu/tzzmod/client/phone/ui/AbstractPhoneScreen.java` | 1,216 |
| 26 | `src/main/java/com/zcpu/tzzmod/condition/ConditionRegistry.java` | 1,193 |
| 27 | `src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminRegionControllerService.java` | 1,166 |
| 28 | `src/main/java/com/zcpu/tzzmod/client/webadmin/WebAdminContainerTemplatePreviewScreen.java` | 1,129 |
| 29 | `src/main/java/com/zcpu/tzzmod/signal/device/VirtualBlockDeviceCommand.java` | 1,073 |
| 30 | `docs/visual-system-8-19/uiux-pro-max-v2/visual-system.css` | 1,071 |

### 指定关键文件

| File | Lines | Bytes | Finding |
| --- | ---: | ---: | --- |
| `WebAdminFrontendScripts.java` | 8,433 | 1,984,343 | P0 巨型 Java text-block bundle；业务逻辑、UI 状态、draft 状态、事件路由混在一起。 |
| `WebAdminFrontendStyles.java` | 75 | 123,798 | CSS 被压缩到少数超长行；维护难度和 diff 可读性差。 |
| `WebAdminLogicChainEditorService.java` | 5,205 | 318,695 | P0 保存协调、顺序写边界、验证、typed writes、commit/cleanup adapter 过载。 |
| `WebAdminServer.java` | 3,102 | 164,014 | P1 路由分发、服务装配、auto snapshot 边界集中。 |
| `WebAdminProtectedDraftRegistry.java` | 620 | 26,369 | P1 static synchronized 全局状态机，类型化 state/save contract 不足。 |

### Top Java files

| Rank | File | Lines | Bytes |
| --- | --- | ---: | ---: |
| 1 | `src/test/java/com/zcpu/tzzmod/stabilization/StabilizationGuardTest.java` | 12,423 | 839,370 |
| 2 | `src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendScripts.java` | 8,433 | 1,984,343 |
| 3 | `src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminLogicChainEditorService.java` | 5,205 | 318,695 |
| 4 | `src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminLogicChainService.java` | 4,257 | 217,770 |
| 5 | `src/main/java/com/zcpu/tzzmod/signal/device/SignalDeviceStore.java` | 3,957 | 177,493 |
| 6 | `src/test/java/com/zcpu/tzzmod/webadmin/service/WebAdminLogicChainEditorServiceTest.java` | 3,183 | 207,866 |
| 7 | `src/main/java/com/zcpu/tzzmod/webadmin/WebAdminServer.java` | 3,102 | 164,014 |
| 8 | `src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminActionRelayActionsService.java` | 1,853 | 95,711 |
| 9 | `src/main/java/com/zcpu/tzzmod/client/webadmin/WebAdminSingleItemSubmitTemplateScreen.java` | 1,730 | 86,605 |
| 10 | `src/main/java/com/zcpu/tzzmod/signal/device/SignalDeviceCommand.java` | 1,652 | 102,022 |
| 11 | `src/main/java/com/zcpu/tzzmod/webadmin/snapshot/WebAdminSnapshotService.java` | 1,628 | 84,057 |
| 12 | `src/main/java/com/zcpu/tzzmod/webadmin/selection/WebAdminSelectionSessions.java` | 1,603 | 81,847 |
| 13 | `src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminTemplateService.java` | 1,574 | 85,303 |
| 14 | `net/minecraft/client/render/WorldRenderer.java` | 1,516 | 61,694 |
| 15 | `src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminVirtualBlockDeviceNativeTriggerService.java` | 1,510 | 85,338 |
| 16 | `src/main/java/com/zcpu/tzzmod/signal/device/ContainerItemConditionCommand.java` | 1,498 | 84,016 |
| 17 | `src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminTimerService.java` | 1,391 | 75,298 |
| 18 | `src/main/java/com/zcpu/tzzmod/webadmin/testbridge/WebAdminTestBridgeRoutes.java` | 1,355 | 69,709 |
| 19 | `src/main/java/com/zcpu/tzzmod/client/phone/ui/AbstractPhoneScreen.java` | 1,216 | 56,695 |
| 20 | `src/main/java/com/zcpu/tzzmod/condition/ConditionRegistry.java` | 1,193 | 61,719 |
| 21 | `src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminRegionControllerService.java` | 1,166 | 67,596 |
| 22 | `src/main/java/com/zcpu/tzzmod/client/webadmin/WebAdminContainerTemplatePreviewScreen.java` | 1,129 | 49,851 |
| 23 | `src/main/java/com/zcpu/tzzmod/signal/device/VirtualBlockDeviceCommand.java` | 1,073 | 65,340 |
| 24 | `src/main/java/com/zcpu/tzzmod/client/map/MapClient.java` | 1,016 | 39,906 |
| 25 | `src/main/java/com/zcpu/tzzmod/webadmin/itemsubmit/WebAdminSingleItemSubmitTemplateSessions.java` | 1,014 | 57,774 |
| 26 | `src/main/java/com/zcpu/tzzmod/signal/SignalCommand.java` | 1,006 | 52,208 |
| 27 | `src/main/java/com/zcpu/tzzmod/client/ar/ui/AbstractARScreen.java` | 999 | 45,291 |
| 28 | `src/main/java/com/zcpu/tzzmod/webadmin/container/WebAdminContainerTemplateSessions.java` | 937 | 51,224 |
| 29 | `src/test/java/com/zcpu/tzzmod/webadmin/service/WebAdminLogicChainServiceTest.java` | 932 | 56,610 |
| 30 | `net/minecraft/client/render/GameRenderer.java` | 920 | 36,416 |

### Top test files

| Rank | File | Lines | Bytes |
| --- | --- | ---: | ---: |
| 1 | `src/test/java/com/zcpu/tzzmod/stabilization/StabilizationGuardTest.java` | 12,423 | 839,370 |
| 2 | `src/test/java/com/zcpu/tzzmod/webadmin/service/WebAdminLogicChainEditorServiceTest.java` | 3,183 | 207,866 |
| 3 | `src/test/java/com/zcpu/tzzmod/webadmin/service/WebAdminLogicChainServiceTest.java` | 932 | 56,610 |
| 4 | `src/test/java/com/zcpu/tzzmod/stabilization/LocalTestMcpFoundationGuardTest.java` | 795 | 78,904 |
| 5 | `src/test/java/com/zcpu/tzzmod/webadmin/service/WebAdminConditionGroupServiceTest.java` | 713 | 52,295 |
| 6 | `src/test/java/com/zcpu/tzzmod/webadmin/service/WebAdminTemplateServiceTest.java` | 657 | 47,974 |
| 7 | `src/test/java/com/zcpu/tzzmod/webadmin/snapshot/WebAdminSnapshotServiceTest.java` | 581 | 33,470 |
| 8 | `src/test/java/com/zcpu/tzzmod/condition/ConditionStateVariableTest.java` | 530 | 39,785 |
| 9 | `src/test/java/com/zcpu/tzzmod/webadmin/service/WebAdminTimerServiceTest.java` | 528 | 34,182 |
| 10 | `src/test/java/com/zcpu/tzzmod/condition/runtime/ConditionActionGateServiceTest.java` | 448 | 22,467 |
| 11 | `src/test/java/com/zcpu/tzzmod/condition/ConditionRegionSignalLogicChainTest.java` | 424 | 35,095 |
| 12 | `src/test/java/com/zcpu/tzzmod/condition/ConditionItemInventoryContainerTest.java` | 413 | 35,869 |
| 13 | `src/test/java/com/zcpu/tzzmod/action/ControlledStateActionServiceTest.java` | 392 | 24,852 |
| 14 | `src/test/java/com/zcpu/tzzmod/webadmin/service/WebAdminConditionRuntimeDoctorServiceTest.java` | 350 | 24,090 |
| 15 | `src/test/java/com/zcpu/tzzmod/condition/runtime/ConditionGroupCompatibilityServiceTest.java` | 346 | 27,436 |
| 16 | `src/test/java/com/zcpu/tzzmod/webadmin/service/WebAdminStateVariableServiceTest.java` | 343 | 19,027 |
| 17 | `src/test/java/com/zcpu/tzzmod/condition/ConditionBasicPlayerContextTest.java` | 298 | 26,038 |
| 18 | `src/test/java/com/zcpu/tzzmod/scheduler/TimerRuntimeServiceTest.java` | 289 | 18,116 |
| 19 | `src/test/java/com/zcpu/tzzmod/condition/runtime/ConditionGateServiceTest.java` | 285 | 15,413 |
| 20 | `src/test/java/com/zcpu/tzzmod/webadmin/service/WebAdminHelpCatalogServiceTest.java` | 277 | 13,825 |
| 21 | `src/test/java/com/zcpu/tzzmod/webadmin/service/WebAdminControlledStateActionServiceTest.java` | 266 | 13,149 |
| 22 | `src/test/java/com/zcpu/tzzmod/resources/ResourceIntegrityTest.java` | 233 | 9,447 |
| 23 | `src/test/java/com/zcpu/tzzmod/condition/runtime/ConditionGateHistoryServiceTest.java` | 229 | 13,850 |
| 24 | `src/test/java/com/zcpu/tzzmod/action/TimerActionExecutionTest.java` | 217 | 11,371 |
| 25 | `src/test/java/com/zcpu/tzzmod/webadmin/service/WebAdminSignalJoinServiceTest.java` | 190 | 10,747 |
| 26 | `src/test/java/com/zcpu/tzzmod/signal/join/SignalJoinBarrierAggregatorTest.java` | 190 | 12,673 |
| 27 | `src/test/java/com/zcpu/tzzmod/condition/ConditionEngineCoreTest.java` | 190 | 11,915 |
| 28 | `src/test/java/com/zcpu/tzzmod/webadmin/service/WebAdminConditionGateConfigTest.java` | 186 | 13,242 |
| 29 | `src/test/java/com/zcpu/tzzmod/condition/runtime/ConditionGateReplayServiceTest.java` | 185 | 11,656 |
| 30 | `src/test/java/com/zcpu/tzzmod/webadmin/service/TimerDoctorTest.java` | 138 | 6,800 |

### Top docs

| Rank | File | Lines | Bytes |
| --- | --- | ---: | ---: |
| 1 | `README.md` | 1,658 | 137,444 |
| 2 | `docs/SIGNAL_BRIDGE.md` | 1,544 | 81,416 |
| 3 | `docs/visual-system-8-19/uiux-pro-max-v2/visual-system.css` | 1,071 | 21,015 |
| 4 | `docs/STABILIZATION_AUDIT_5_15.md` | 1,001 | 45,895 |
| 5 | `docs/2026-5-9 夜间开发文档.md` | 758 | 20,405 |
| 6 | `docs/WEBADMIN_VISUAL_SYSTEM_UIUX_PRO_MAX_8_19.md` | 519 | 19,441 |
| 7 | `docs/WEBADMIN_UI_REFACTOR_7_5_CURRENT_CONTEXT.md` | 450 | 26,310 |
| 8 | `docs/WEBADMIN_VISUAL_SYSTEM_UIUX_PRO_MAX_SAMPLES_V2_8_19.md` | 441 | 12,169 |
| 9 | `docs/LOCAL_TEST_MCP_FOUNDATION_CURRENT_CONTEXT.md` | 390 | 25,398 |
| 10 | `docs/test/测试_7.11_WebAdmin统一ItemSubmit编辑器验收.md` | 350 | 12,497 |
| 11 | `docs/CONDITION_RUNTIME_DEBUGGER_8_8_CURRENT_CONTEXT.md` | 339 | 9,910 |
| 12 | `docs/SCHEDULER_DELAY_TIMER_8_12_CURRENT_CONTEXT.md` | 320 | 9,961 |
| 13 | `docs/web_admin_foundation.md` | 295 | 6,827 |
| 14 | `docs/LEGACY_DATAPACK_AUDIT_9_0.md` | 280 | 15,509 |
| 15 | `docs/REGRESSION_TEST_6_7.md` | 277 | 10,438 |
| 16 | `docs/CONDITION_ENGINE_CORE_8_0_CURRENT_CONTEXT.md` | 268 | 6,710 |
| 17 | `docs/SIGNAL_JOIN_BARRIER_AGGREGATOR_8_10_CURRENT_CONTEXT.md` | 257 | 7,870 |
| 18 | `docs/SNAPSHOT_ROLLBACK_TIMELINE_8_18_CURRENT_CONTEXT.md` | 257 | 14,051 |
| 19 | `docs/WEBADMIN_VBD_NATIVE_TRIGGER_CONFIG_7_9_CURRENT_CONTEXT.md` | 255 | 19,019 |
| 20 | `docs/LOGIC_CHAIN_GLOBAL_EDITOR_CAPABILITY_MATRIX_9_1.md` | 249 | 23,861 |
| 21 | `docs/CONDITION_REGION_SIGNAL_LOGIC_CHAIN_8_4_CURRENT_CONTEXT.md` | 246 | 9,727 |
| 22 | `docs/WEBADMIN_UNIFIED_ITEM_SUBMIT_EDITOR_7_11_CURRENT_CONTEXT.md` | 243 | 13,565 |
| 23 | `docs/LOGIC_CHAIN_EDITOR_CAPABILITY_MATRIX_8_14.md` | 231 | 20,403 |
| 24 | `docs/REGRESSION_TEST_6_8.md` | 228 | 5,463 |
| 25 | `docs/CONTROLLED_STATE_ACTIONS_8_11_CURRENT_CONTEXT.md` | 223 | 7,910 |
| 26 | `docs/LOGIC_CHAIN_VIEWER_ENHANCEMENT_8_13_CURRENT_CONTEXT.md` | 221 | 13,361 |
| 27 | `docs/STABILIZATION_AUDIT_5_15_ROUND3.md` | 220 | 9,467 |
| 28 | `docs/test/测试_8.8_WebAdmin条件模拟诊断回放验收.md` | 218 | 8,123 |
| 29 | `docs/LOGIC_CHAIN_CAPABILITY_MATRIX_8_13.md` | 217 | 16,188 |
| 30 | `docs/CONDITION_STATE_VARIABLES_8_2_CURRENT_CONTEXT.md` | 213 | 7,197 |

## 最大方法 / JS 函数启发式统计

统计口径：Java 方法使用 brace-count 启发式，并忽略 Java string / text block 内部 brace 以避免误判方法结束；`appJs()` 的行跨度仍代表生成 bundle 的真实维护面。JS 函数统计只解析 `WebAdminFrontendScripts.appJs()` 中的单行 `function name(...)` / assignment function / arrow function，`chars` 比行数更能反映当前单行压缩函数的维护成本。

### Top 50 Java methods

| Rank | Method | Lines | Location |
| --- | --- | ---: | --- |
| 1 | `WebAdminFrontendScripts.appJs` | 8,281 | `src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendScripts.java:152` |
| 2 | `StabilizationGuardTest.webAdminRenderSmokeHarness` | 1,028 | `src/test/java/com/zcpu/tzzmod/stabilization/StabilizationGuardTest.java:1346` |
| 3 | `StabilizationGuardTest.testWebAdminVbdNativeTriggerOverview` | 854 | `src/test/java/com/zcpu/tzzmod/stabilization/StabilizationGuardTest.java:3263` |
| 4 | `StabilizationGuardTest.testWebAdminWriteFoundation` | 766 | `src/test/java/com/zcpu/tzzmod/stabilization/StabilizationGuardTest.java:4118` |
| 5 | `StabilizationGuardTest.testLogicChainEditorMvp814` | 684 | `src/test/java/com/zcpu/tzzmod/stabilization/StabilizationGuardTest.java:9320` |
| 6 | `StabilizationGuardTest.testLogicChainGlobalEditorCompletion91` | 664 | `src/test/java/com/zcpu/tzzmod/stabilization/StabilizationGuardTest.java:11546` |
| 7 | `LocalTestMcpFoundationGuardTest.testSafetyMarkers` | 446 | `src/test/java/com/zcpu/tzzmod/stabilization/LocalTestMcpFoundationGuardTest.java:250` |
| 8 | `StabilizationGuardTest.testWebAdminPhysicalDeviceActionRelayFoundation` | 391 | `src/test/java/com/zcpu/tzzmod/stabilization/StabilizationGuardTest.java:2666` |
| 9 | `StabilizationGuardTest.testLogicChainViewerEnhancement813` | 369 | `src/test/java/com/zcpu/tzzmod/stabilization/StabilizationGuardTest.java:8950` |
| 10 | `StabilizationGuardTest.testSnapshotRollbackTimeline818` | 361 | `src/test/java/com/zcpu/tzzmod/stabilization/StabilizationGuardTest.java:10906` |
| 11 | `StabilizationGuardTest.testSchedulerDelayTimer812` | 328 | `src/test/java/com/zcpu/tzzmod/stabilization/StabilizationGuardTest.java:8621` |
| 12 | `StabilizationGuardTest.testWebAdminHelpExampleCenter817` | 307 | `src/test/java/com/zcpu/tzzmod/stabilization/StabilizationGuardTest.java:10598` |
| 13 | `StabilizationGuardTest.testTemplatesPrefabImportExport815` | 307 | `src/test/java/com/zcpu/tzzmod/stabilization/StabilizationGuardTest.java:10005` |
| 14 | `StabilizationGuardTest.testWebAdminReadonlyFrontendAssets` | 293 | `src/test/java/com/zcpu/tzzmod/stabilization/StabilizationGuardTest.java:840` |
| 15 | `StabilizationGuardTest.testConditionRuntimeGates86` | 290 | `src/test/java/com/zcpu/tzzmod/stabilization/StabilizationGuardTest.java:7204` |
| 16 | `StabilizationGuardTest.testControlledStateActions811` | 286 | `src/test/java/com/zcpu/tzzmod/stabilization/StabilizationGuardTest.java:8334` |
| 17 | `StabilizationGuardTest.testWebAdminConditionEditor85` | 284 | `src/test/java/com/zcpu/tzzmod/stabilization/StabilizationGuardTest.java:6919` |
| 18 | `StabilizationGuardTest.testLogicChainEditorExistingNodeEditing816` | 284 | `src/test/java/com/zcpu/tzzmod/stabilization/StabilizationGuardTest.java:10313` |
| 19 | `StabilizationGuardTest.testPre9StabilizationHardening820` | 277 | `src/test/java/com/zcpu/tzzmod/stabilization/StabilizationGuardTest.java:11268` |
| 20 | `VirtualBlockInteractionItemCommand.build` | 272 | `src/main/java/com/zcpu/tzzmod/signal/device/VirtualBlockInteractionItemCommand.java:34` |
| 21 | `StabilizationGuardTest.testConditionRuntimeDebugger88` | 243 | `src/test/java/com/zcpu/tzzmod/stabilization/StabilizationGuardTest.java:7688` |
| 22 | `WebAdminServer.handleRegionControllers` | 240 | `src/main/java/com/zcpu/tzzmod/webadmin/WebAdminServer.java:2616` |
| 23 | `VirtualBlockDeviceCommand.build` | 232 | `src/main/java/com/zcpu/tzzmod/signal/device/VirtualBlockDeviceCommand.java:33` |
| 24 | `ContainerItemConditionCommand.build` | 229 | `src/main/java/com/zcpu/tzzmod/signal/device/ContainerItemConditionCommand.java:37` |
| 25 | `StabilizationGuardTest.testConditionRuntimeSingleActionGates89` | 228 | `src/test/java/com/zcpu/tzzmod/stabilization/StabilizationGuardTest.java:7932` |
| 26 | `WebAdminServer.handle` | 223 | `src/main/java/com/zcpu/tzzmod/webadmin/WebAdminServer.java:190` |
| 27 | `StabilizationGuardTest.testConditionBasicPlayerContext81` | 214 | `src/test/java/com/zcpu/tzzmod/stabilization/StabilizationGuardTest.java:6179` |
| 28 | `WebAdminSelectionSessions.completeFromClient` | 211 | `src/main/java/com/zcpu/tzzmod/webadmin/selection/WebAdminSelectionSessions.java:434` |
| 29 | `WebAdminTestBridgeRoutes.handle` | 209 | `src/main/java/com/zcpu/tzzmod/webadmin/testbridge/WebAdminTestBridgeRoutes.java:74` |
| 30 | `WebAdminServer.handleTimers` | 208 | `src/main/java/com/zcpu/tzzmod/webadmin/WebAdminServer.java:1600` |
| 31 | `StabilizationGuardTest.testWebAdminSignalListenerEditing` | 208 | `src/test/java/com/zcpu/tzzmod/stabilization/StabilizationGuardTest.java:5074` |
| 32 | `StabilizationGuardTest.testConditionStateVariables82` | 205 | `src/test/java/com/zcpu/tzzmod/stabilization/StabilizationGuardTest.java:6394` |
| 33 | `StabilizationGuardTest.testWebAdminLogicChainViewer715` | 204 | `src/test/java/com/zcpu/tzzmod/stabilization/StabilizationGuardTest.java:5769` |
| 34 | `StabilizationGuardTest.testConditionEngineCore80` | 204 | `src/test/java/com/zcpu/tzzmod/stabilization/StabilizationGuardTest.java:5974` |
| 35 | `WebAdminHelpCatalogServiceTest.run` | 198 | `src/test/java/com/zcpu/tzzmod/webadmin/service/WebAdminHelpCatalogServiceTest.java:12` |
| 36 | `SignalDeviceCommand.sendVirtualDebug` | 195 | `src/main/java/com/zcpu/tzzmod/signal/device/SignalDeviceCommand.java:736` |
| 37 | `StabilizationGuardTest.testWebAdminInteractionItemMatcherEditing` | 193 | `src/test/java/com/zcpu/tzzmod/stabilization/StabilizationGuardTest.java:3069` |
| 38 | `StabilizationGuardTest.testConditionRuntimeReceiverGates87` | 192 | `src/test/java/com/zcpu/tzzmod/stabilization/StabilizationGuardTest.java:7495` |
| 39 | `WebAdminLogicChainService.buildSegment` | 191 | `src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminLogicChainService.java:849` |
| 40 | `StabilizationGuardTest.testWebAdminRegionControllerEditing` | 188 | `src/test/java/com/zcpu/tzzmod/stabilization/StabilizationGuardTest.java:4885` |
| 41 | `WebAdminServer.handleSignalListenerLifecycle` | 180 | `src/main/java/com/zcpu/tzzmod/webadmin/WebAdminServer.java:2396` |
| 42 | `StabilizationGuardTest.testSignalJoinBarrierAggregator810` | 172 | `src/test/java/com/zcpu/tzzmod/stabilization/StabilizationGuardTest.java:8161` |
| 43 | `WebAdminTemplateService.buildApplyPlan` | 171 | `src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminTemplateService.java:430` |
| 44 | `StabilizationGuardTest.testConditionRegionSignalLogicChain84` | 162 | `src/test/java/com/zcpu/tzzmod/stabilization/StabilizationGuardTest.java:6756` |
| 45 | `WebAdminServer.handleSignalJoins` | 160 | `src/main/java/com/zcpu/tzzmod/webadmin/WebAdminServer.java:1341` |
| 46 | `StabilizationGuardTest.testConditionItemInventoryContainer83` | 155 | `src/test/java/com/zcpu/tzzmod/stabilization/StabilizationGuardTest.java:6600` |
| 47 | `SignalDeviceData.normalized` | 153 | `src/main/java/com/zcpu/tzzmod/signal/device/SignalDeviceData.java:590` |
| 48 | `VirtualBlockItemSubmitCommand.build` | 151 | `src/main/java/com/zcpu/tzzmod/signal/device/VirtualBlockItemSubmitCommand.java:29` |
| 49 | `Tzz_modClient.onInitializeClient` | 148 | `src/main/java/com/zcpu/tzzmod/client/Tzz_modClient.java:60` |
| 50 | `MapClient.applyState` | 148 | `src/main/java/com/zcpu/tzzmod/client/map/MapClient.java:378` |

### Top 50 JS functions by chars

| Rank | Function | Line | Chars | `if` | `.closest(` | `querySelector(` | Inline handlers |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | `showLogicChainNewNodeModal` | 7888 | 10,447 | 2 | 0 | 0 | 21 |
| 2 | `showLogicChainPlacedDraftNodeEditModal` | 7633 | 10,318 | 2 | 0 | 0 | 32 |
| 3 | `logicChainMindMap` | 7841 | 6,470 | 1 | 0 | 0 | 0 |
| 4 | `showTimerEditModal` | 4053 | 4,946 | 1 | 0 | 0 | 10 |
| 5 | `logicChainCanvasToolbar` | 7578 | 4,809 | 0 | 0 | 0 | 9 |
| 6 | `renderLogicChainProtectedPlayerPickerIntoModal` | 7900 | 4,783 | 10 | 0 | 3 | 10 |
| 7 | `logicChainNodeCard` | 8081 | 4,433 | 1 | 0 | 0 | 0 |
| 8 | `logicChainLayoutGraphV2` | 7712 | 4,335 | 14 | 0 | 0 | 0 |
| 9 | `logicChainNodeMarkers` | 7609 | 3,990 | 22 | 0 | 0 | 0 |
| 10 | `logicChainEdgePath` | 7850 | 3,975 | 1 | 0 | 0 | 0 |
| 11 | `setLane` | 7712 | 3,847 | 13 | 0 | 0 | 0 |
| 12 | `showSignalJoinEditModal` | 3955 | 3,801 | 1 | 0 | 0 | 9 |
| 13 | `logicChainLayoutWithDraft` | 8233 | 3,788 | 6 | 0 | 0 | 0 |
| 14 | `syncLogicChainPlacedDraftNodeEdit` | 7634 | 3,749 | 17 | 0 | 0 | 0 |
| 15 | `showLogicChainActionEditModal` | 7987 | 3,733 | 1 | 0 | 0 | 8 |
| 16 | `showLogicChainMetadataModal` | 8417 | 3,604 | 1 | 0 | 0 | 10 |
| 17 | `stateActionEditor` | 3107 | 3,440 | 0 | 0 | 0 | 0 |
| 18 | `logicChainDraftSlotOverlay` | 7839 | 3,427 | 1 | 0 | 0 | 0 |
| 19 | `logicChainExistingVbdTriggerFields` | 8280 | 3,420 | 6 | 0 | 0 | 0 |
| 20 | `logicChainNewDraftGraphNode` | 7048 | 3,314 | 9 | 0 | 0 | 0 |
| 21 | `logicChainExistingDeviceForm` | 8143 | 3,205 | 0 | 0 | 0 | 4 |
| 22 | `logicChainLegend` | 7565 | 3,077 | 0 | 0 | 0 | 2 |
| 23 | `makeLogicChainEditorDraft` | 7911 | 3,044 | 10 | 0 | 0 | 0 |
| 24 | `logicChainExistingEditCapability` | 7620 | 3,021 | 9 | 0 | 0 | 0 |
| 25 | `logicChainActionNodeMaintenanceCard` | 8224 | 3,007 | 3 | 0 | 0 | 0 |
| 26 | `logicChainExistingSignalJoinForm` | 8055 | 2,966 | 0 | 0 | 0 | 8 |
| 27 | `timerActionEditor` | 3098 | 2,934 | 0 | 0 | 0 | 0 |
| 28 | `showLogicChainActionAppendModal` | 7941 | 2,911 | 1 | 0 | 0 | 7 |
| 29 | `logicChainApplyVbdNativeTriggerDraftGraphOverlay` | 8390 | 2,886 | 7 | 0 | 0 | 0 |
| 30 | `syncLogicChainExistingEditDraft` | 8070 | 2,885 | 11 | 0 | 0 | 0 |
| 31 | `logicChainExistingActionEditSectionForNode` | 8061 | 2,855 | 1 | 0 | 0 | 0 |
| 32 | `logicChainApplyVbdNativeTriggerDraftGraphOverlay` | 8361 | 2,786 | 7 | 0 | 0 | 0 |
| 33 | `regionControllerEditForm` | 6973 | 2,754 | 0 | 0 | 0 | 5 |
| 34 | `showLogicChainDraftChannelModal` | 8071 | 2,753 | 3 | 0 | 0 | 5 |
| 35 | `showLogicChainDraftActionEditModal` | 7645 | 2,713 | 1 | 0 | 0 | 7 |
| 36 | `logicChainPollProtectedDraftSelection` | 7903 | 2,695 | 11 | 0 | 1 | 0 |
| 37 | `logicChainExistingDeviceForm` | 8144 | 2,676 | 1 | 0 | 0 | 6 |
| 38 | `logicChainSelectedNodePanel` | 7613 | 2,597 | 1 | 0 | 0 | 2 |
| 39 | `logicChainPrepareActionMaintenanceDraftFromList` | 8064 | 2,592 | 6 | 0 | 0 | 0 |
| 40 | `startLogicChainProtectedNodeSelection` | 7905 | 2,572 | 3 | 0 | 0 | 0 |
| 41 | `actionConditionGatePicker` | 2232 | 2,509 | 0 | 0 | 0 | 0 |
| 42 | `showConditionNodeEditorModal` | 7414 | 2,473 | 6 | 0 | 0 | 6 |
| 43 | `signalListenerActionAddForm` | 4470 | 2,466 | 0 | 0 | 0 | 5 |
| 44 | `startLogicChainExistingActionEdit` | 7992 | 2,447 | 5 | 0 | 0 | 0 |
| 45 | `syncLogicChainEditorDraft` | 7890 | 2,421 | 4 | 0 | 0 | 0 |
| 46 | `logicChainRenderedGraphWithDraftOverlay` | 8097 | 2,419 | 10 | 0 | 0 | 0 |
| 47 | `logicChainExistingVbdRequirementList` | 8297 | 2,412 | 0 | 0 | 0 | 0 |
| 48 | `logicChainExistingActionValueEditor` | 7978 | 2,402 | 7 | 0 | 0 | 3 |
| 49 | `logicChainPrepareExistingActionConnectionDraft` | 8046 | 2,389 | 4 | 0 | 0 | 0 |
| 50 | `logicChainCanvas` | 7710 | 2,363 | 0 | 0 | 0 | 0 |

### Top 30 JS functions by complexity heuristic

This is the required Top 30 by `if` / `.closest(` / `querySelector(` usage heuristic. Score = `if * 2 + .closest( * 3 + querySelector( * 3 + inline handler * 2 + chars / 1000`。

| Rank | Function | Line | Score | Chars | `if` | `.closest(` | `querySelector(` | Inline handlers |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | `showLogicChainPlacedDraftNodeEditModal` | 7633 | 78.3 | 10,318 | 2 | 0 | 0 | 32 |
| 2 | `showLogicChainNewNodeModal` | 7888 | 56.4 | 10,447 | 2 | 0 | 0 | 21 |
| 3 | `logicChainNodeTypeLabel` | 7016 | 55.5 | 1,538 | 27 | 0 | 0 | 0 |
| 4 | `renderLogicChainProtectedPlayerPickerIntoModal` | 7900 | 53.8 | 4,783 | 10 | 0 | 3 | 10 |
| 5 | `logicChainNodeMarkers` | 7609 | 48.0 | 3,990 | 22 | 0 | 0 | 0 |
| 6 | `syncLogicChainPlacedDraftNodeEdit` | 7634 | 37.7 | 3,749 | 17 | 0 | 0 | 0 |
| 7 | `handleLogicChainEditorDelegatedClick` | 8104 | 33.5 | 1,456 | 7 | 6 | 0 | 0 |
| 8 | `logicChainLayoutGraphV2` | 7712 | 32.3 | 4,335 | 14 | 0 | 0 | 0 |
| 9 | `logicChainConnectExistingCandidate` | 8086 | 31.9 | 1,897 | 15 | 0 | 0 | 0 |
| 10 | `setLane` | 7712 | 29.8 | 3,847 | 13 | 0 | 0 | 0 |
| 11 | `logicChainPollProtectedDraftSelection` | 7903 | 27.7 | 2,695 | 11 | 0 | 1 | 0 |
| 12 | `deviceExtendedConfigPatchBody` | 411 | 27.0 | 995 | 13 | 0 | 0 | 0 |
| 13 | `showTimerEditModal` | 4053 | 26.9 | 4,946 | 1 | 0 | 0 | 10 |
| 14 | `addChannel` | 411 | 26.8 | 822 | 13 | 0 | 0 | 0 |
| 15 | `showConditionNodeEditorModal` | 7414 | 26.5 | 2,473 | 6 | 0 | 0 | 6 |
| 16 | `heartbeatLogicChainExistingEditLock` | 7999 | 26.3 | 1,297 | 8 | 0 | 3 | 0 |
| 17 | `showLogicChainMetadataModal` | 8417 | 25.6 | 3,604 | 1 | 0 | 0 | 10 |
| 18 | `handleSingleItemSubmitSessionRealtimeEvent` | 8381 | 25.3 | 1,330 | 12 | 0 | 0 | 0 |
| 19 | `handleContainerTemplateSessionRealtimeEvent` | 8382 | 25.3 | 1,299 | 12 | 0 | 0 | 0 |
| 20 | `syncLogicChainExistingEditDraft` | 8070 | 24.9 | 2,885 | 11 | 0 | 0 | 0 |
| 21 | `logicChainReadonlyDeferredCard` | 8052 | 24.2 | 2,161 | 11 | 0 | 0 | 0 |
| 22 | `logicChainChildNodesForMode` | 7597 | 23.9 | 1,883 | 11 | 0 | 0 | 0 |
| 23 | `showSignalJoinEditModal` | 3955 | 23.8 | 3,801 | 1 | 0 | 0 | 9 |
| 24 | `makeLogicChainEditorDraft` | 7911 | 23.0 | 3,044 | 10 | 0 | 0 | 0 |
| 25 | `logicChainBaseLane` | 7581 | 22.9 | 870 | 11 | 0 | 0 | 0 |
| 26 | `logicChainCanvasToolbar` | 7578 | 22.8 | 4,809 | 0 | 0 | 0 | 9 |
| 27 | `logicChainRenderedGraphWithDraftOverlay` | 8097 | 22.4 | 2,419 | 10 | 0 | 0 | 0 |
| 28 | `logicChainExistingActionValueEditor` | 7978 | 22.4 | 2,402 | 7 | 0 | 0 | 3 |
| 29 | `logicChainOverlayNodeWithExistingDraft` | 7056 | 22.2 | 2,200 | 10 | 0 | 0 | 0 |
| 30 | `connectLogicChainDraftCandidate` | 8087 | 22.2 | 2,182 | 10 | 0 | 0 | 0 |

### Top 30 event-handler hotspots

事件统计口径固定为 `.closest(` 和 `querySelector(` 调用数；单独的 `closest` token 不用于 guard 阈值。

| Rank | Handler / source | Line | Score | Chars | `if` | `.closest(` | `querySelector(` | Inline handlers |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | `showLogicChainPlacedDraftNodeEditModal` | 7633 | 78.3 | 10,334 | 2 | 0 | 0 | 32 |
| 2 | `showLogicChainNewNodeModal` | 7888 | 56.5 | 10,463 | 2 | 0 | 0 | 21 |
| 3 | `renderLogicChainProtectedPlayerPickerIntoModal` | 7900 | 53.8 | 4,799 | 10 | 0 | 3 | 10 |
| 4 | `handleLogicChainEditorDelegatedClick` | 8104 | 33.5 | 1,472 | 7 | 6 | 0 | 0 |
| 5 | `logicChainPollProtectedDraftSelection` | 7903 | 27.7 | 2,711 | 11 | 0 | 1 | 0 |
| 6 | `showTimerEditModal` | 4053 | 27.0 | 4,962 | 1 | 0 | 0 | 10 |
| 7 | `addEventListener:click` condition node modal | 7414 | 26.5 | 2,489 | 6 | 0 | 0 | 6 |
| 8 | `showLogicChainMetadataModal` | 8417 | 25.6 | 3,620 | 1 | 0 | 0 | 10 |
| 9 | `showSignalJoinEditModal` | 3955 | 23.8 | 3,817 | 1 | 0 | 0 | 9 |
| 10 | `logicChainExistingActionValueEditor` | 7978 | 22.4 | 2,418 | 7 | 0 | 0 | 3 |
| 11 | `restoreConditionNodeModalUiState` | 7412 | 22.0 | 987 | 6 | 0 | 3 | 0 |
| 12 | `logicChainDraftActionValueEditor` | 7644 | 21.9 | 1,860 | 6 | 0 | 0 | 4 |
| 13 | `showLogicChainActionEditModal` | 7987 | 21.7 | 3,749 | 1 | 0 | 0 | 8 |
| 14 | `signalListenerActionValueEditor` | 4469 | 20.3 | 2,265 | 6 | 0 | 0 | 3 |
| 15 | `regionControllerActionValueEditor` | 6995 | 20.3 | 2,259 | 6 | 0 | 0 | 3 |
| 16 | `logicChainActionAppendValueEditor` | 7940 | 20.1 | 2,082 | 6 | 0 | 0 | 3 |
| 17 | `addEventListener:keydown` ESC router | 5294 | 20.0 | 968 | 8 | 0 | 1 | 0 |
| 18 | `timerActionValueEditor` | 4063 | 19.2 | 1,250 | 5 | 0 | 0 | 4 |
| 19 | `showLogicChainActionAppendModal` | 7941 | 18.9 | 2,927 | 1 | 0 | 0 | 7 |
| 20 | `showLogicChainDraftChannelModal` | 8071 | 18.8 | 2,769 | 3 | 0 | 0 | 5 |
| 21 | `showLogicChainDraftActionEditModal` | 7645 | 18.7 | 2,729 | 1 | 0 | 0 | 7 |
| 22 | `addEventListener:click` protected picker | 7893 | 17.5 | 1,466 | 3 | 0 | 2 | 2 |
| 23 | `logicChainCrossingSortKey` | 7587 | 17.1 | 1,124 | 8 | 0 | 0 | 0 |
| 24 | `filterActions` | 3806 | 17.1 | 1,107 | 8 | 0 | 0 | 0 |
| 25 | `filterActionTemplates` | 6430 | 16.9 | 880 | 8 | 0 | 0 | 0 |
| 26 | `restoreConditionGroupModalUiState` | 7399 | 16.7 | 738 | 5 | 0 | 2 | 0 |
| 27 | `handleLogicChainVbdCaptureRetryDelegatedClick` | 8396 | 15.9 | 855 | 6 | 1 | 0 | 0 |
| 28 | `logicChainExistingVbdTriggerFields` | 8280 | 15.4 | 3,436 | 6 | 0 | 0 | 0 |
| 29 | `heartbeatLogicChainActionAppendLock` | 7949 | 15.4 | 1,380 | 4 | 0 | 2 | 0 |
| 30 | `handleConditionNodeModalDelegatedClick` | 7413 | 14.7 | 693 | 4 | 2 | 0 | 0 |

## 前端屎山点

- `WebAdminFrontendScripts.appJs()` 是 8,282 行 Java text block，集中 boot/router/API/realtime/modal/所有页面/Logic Chain viewer/editor。
- `appState` 在 `WebAdminFrontendScripts.java:164` 同时承载全局页面数据、edit lock timer、modal 状态、Logic Chain canvas 状态、Logic Chain draft save state、selection terminal 和 capture session。
- 全局 `document.addEventListener` 在 `1260-1339` 同时处理 Logic Chain、combobox、dirty confirm、help popover、ESC、selection cancel 等，事件边界不清。
- Logic Chain delegated handlers 在 `8103-8123` 与全局 capture/bubble click handler 重叠，存在重复扫描和事件抢占风险。
- Inline handler 仍大量存在：`onclick=` 251、`oninput=` 150、`onchange=` 108、`htmlHandler(` 177、`innerHTML` 61。
- `WebAdminFrontendShell.java:4` 和 `WebAdminFrontendScripts.java:162` 的 asset version 仍是 `8.18-snapshot-rollback-timeline-clickfix`，9.1 后 cache-busting 标识陈旧。
- `WebAdminFrontendStyles.java` 只有 75 行但 123,798 bytes，说明 CSS 压在少数超长行，后续 diff / review / guard 都困难。

### Frontend asset boundary

当前 WebAdmin 不是独立前端工程，asset 关系必须保持：

```text
WebAdminFrontendShell -> HTML shell / asset version
WebAdminFrontendStyles -> appCss() Java text block
WebAdminFrontendScripts -> appJs() Java text block
WebAdminFrontendAssets -> shell/css/js facade
WebAdminServer -> /assets/app.css and /assets/app.js routes
```

`WebAdminFrontendAssets.java` 当前只有 facade 职责，`StabilizationGuardTest` 也守护它委托到 Shell/Styles/Scripts。9.1.1 拆分不得让 `WebAdminServer` 直接绕过 facade，也不得新增独立 `vite.config`、npm build 前置、CDN framework 或 React/Vite runtime。输出形态仍是 Java 生成 `/assets/app.js` 与 `/assets/app.css`。

CSS 拆分必须保持 concat 顺序。当前 selector 覆盖关系依赖 `WebAdminFrontendStyles.appCss()` 内 append 顺序，后续拆模块时 guard 不应只比 bytes，还要守护关键 selector 的先后关系。

### Escaping contract

前端拆分的高风险不是单纯文件移动，而是 HTML / JS escaping contract 被破坏：

- `esc(...)` 用于 HTML 文本和属性上下文。
- `jsString(...)` 用于 JavaScript string literal。
- `htmlEvent(...)` / `htmlHandler(...)` 是现有 inline handler 过渡入口，不是新增 handler 的推荐接口。
- `innerHTML` sink 必须只接收经过明确模板构造和 escaping 的 HTML；新增 raw user value 进入 `innerHTML` 应 hard fail。
- Phase 3 后新增 `onclick=` / `oninput=` / `onchange=` / `htmlHandler(` 应 hard fail；已有入口只能随拆分逐步收敛。

### 前端事件处理审计

| Event area | Current location | Current behavior / risk | Refactor direction |
| --- | --- | --- | --- |
| Global `click` capture/bubble | `WebAdminFrontendScripts.java:1260-1339` | 12 listeners in slice；约 10,446 chars，94 `if`，35 `.closest(` calls，4 `querySelector(` calls。处理 dirty confirm、combobox close、help popover、selection cancel 等多域事件。 | 先记录 capture/bubble 顺序，再拆 route table；不得改变 outside-click close 和 dirty confirm 优先级。 |
| Global `mouseover` / `mouseout` | `1263-1264` | 进入 Logic Chain hover 后可能触发 full render。 | 先加 DOM 等价 guard，再考虑 class-only 更新。 |
| Global `mousemove` / pointer paths | `1259` 附近与 Logic Chain draft pointer handlers | drag preview 可在 pointermove 中触发布局。 | rAF/debounce 只能在 slot 不变时合并；不得改 drop preview 可见结果。 |
| Global `keydown` ESC router | `5294` | 独立 ESC router，处理 modal / combobox / condition editor / connection mode 等优先级。 | Phase 3 必须纳入事件表；不得漏掉此独立 router。 |
| Custom combobox | global click + keydown + combobox helpers | outside click / ESC close 与 modal ESC 存在抢事件风险。 | 单独 `CustomComboboxEventRouter`，保留当前关闭顺序。 |
| Logic Chain delegated click | `8103-8123` | 6 listeners in slice；约 10,819 chars，48 `if`，13 `.closest(` calls，1 `querySelector(` call。 | `LogicChainEventRouter` route table；先保持 early return 和 stopPropagation。 |
| VBD trigger / capture retry | `8128-8397` | patch stack 约 188,832 chars，368 `if`，1 `.closest(` call，3 `querySelector(` calls，33 inline handlers。 | 拆 VBD module + stable trigger route；不得改变 capture retry pointerup/click 双入口。 |
| Action panel handlers | `showLogicChainActionEditModal` / `showLogicChainActionAppendModal` / action value editors | modal builder 与 inline handler 混合，保存/dirty/validation 容易互相影响。 | action editor builder、value sync、modal footer 命令分离。 |
| Selection session handlers | protected player picker / selection realtime / cancel | UI selection、world selection、protected draft selection 名称接近，容易误用 state。 | 明确前端 UI selection 只控制高亮/详情，backend selection session 才能产生 protected draft source。 |

## 后端屎山点

- `WebAdminLogicChainEditorService.saveDraft` 从 `238` 开始，承担 write preflight、editor lock、base graph fingerprint、draft validation、target lock preflight、typed write sequencing、failure lock preservation、audit/realtime outcome 等多层职责。
- `saveDraft` 的真实写入在 `336-429` 顺序执行；这不是跨 store 原子事务，而是三层边界：preflight/lock/fingerprint/validation、fail-closed mixed-write guards、sequential typed execution + failure preserves editor lock。
- `channelMetadataDrafts` 不能被简单描述成 typed save boundary 的普通参与者：当前代码先拒绝 typed store drafts 与 `channelMetadataDrafts` 混存，而 `validateDraftRequest` 的 `hasAnyWrite` 又不把纯 channel metadata draft 算作普通 typed write。后续实现阶段必须单独确认/修正这个边界。
- VBD commit 在 `saveVirtualBlockDeviceDraft` (`543`) 与 requirement conversion (`791`, `851`) 中混合创建、验证、回滚。
- World Device / RegionController protected draft commit 分别在 `952`、`1128`，把 world block / SignalDeviceStore / MapDataStore / RegionControllerStore 写入逻辑嵌在 Logic Chain editor service。
- rollback 语义必须收窄理解：VBD rollback 主要覆盖失败时移除新建 virtual block；WorldDevice 写入 basic config / action / metadata 后没有通用旧状态跨 store 回滚；RegionController rollback 主要覆盖新建 Region + Controller 失败路径。不得在文档或实现中制造“完整跨 store atomic save”假象。
- `WebAdminServer` 在 `128` 直接装配 `WebAdminLogicChainEditorService`，在 `343` 分派 Logic Chain editor 路由，在 `1193` 做 auto snapshot，然后调用 `saveDraft`。route adapter 与 write save coordinator 边界混合。
- `WebAdminSelectionSessions` 是 1,603 行 static synchronized session manager，含 start/cancel/client complete/world use/protected cleanup/audit/realtime。
- 提示词中的 `WebAdminMapServer.java` 当前不存在；实际相关类是 `src/main/java/com/zcpu/tzzmod/map/MapServer.java` 与 `MapDataStore.java`。

## 状态耦合点

- `startLogicChainEditMode` (`7869`) 把 `nodes`、`edges`、`draftChannels`、`existingNodeEdits`、`actionEdits`、`nodeDeletes`、`actionDeletes`、`actionReorders`、`existingEdit`、`actionEdit`、`connectionMode`、`saving` 全塞入 `appState.logicChainEditor`。
- `logicChainActiveDraftNode` (`7039`) 优先读取 `appState.logicChainCanvas.selectedNodeId`，导致 canvas selection 能影响 draft target。
- `logicChainEditorHasDraftContent` (`7872`) 把 `connectionMode` 也算作草稿内容，UI transient 状态会触发 dirty。
- `logicChainFindDraftTargetNode` 的 V17 wrapper (`8386`) 对 VBD draft target fallback 到 `selectedNodeId`，这是 capture / overlay 定位的高风险耦合点。
- itemSubmit/container `logicChainDraftOnly` capture 在 backend session service (`WebAdminVirtualBlockDeviceSingleItemSubmitTemplateSessionService.java:123/151`、`WebAdminVirtualBlockDeviceContainerTemplateSessionService.java:142/184`) 已校验 VBD target、expectedFingerprint、VBD template edit lock、CSRF/same-origin；Logic Chain edit lock 是 draft-only 模式的额外校验。后续拆分不能削弱这些安全边界。
- capture writeback 的正式 VBD 写入仍发生在 final Logic Chain save；后续计划必须要求 stable target id / triggerKey / draftSessionId / editLockId 一致性由后端校验，不能只靠前端把 captured rows 合并进 `existingNodeEdits`。
- 新增 draft action 的删除使用前端 `_pendingDelete` (`8214`、`8235`、`8236`)；已有 action/node 删除走 backend payload (`nodeDeletes`/`actionDeletes`)。这是合理双轨，但对象原地突变和 rerender monkey patch 会增加误恢复风险。

## Patch-stacking / BeforeVxx 问题

`WebAdminFrontendScripts.java` 中 patch-stacking 计数：

- `BeforeV13`: 23
- `BeforeV14`: 28
- `BeforeV15`: 8
- `BeforeV16`: 16
- `BeforeV17`: 22

集中区域：

- `8128-8397`：VBD trigger、capture writeback、selected source card、overlay graph patch。
- `logicChainApplyVbdNativeTriggerDraftGraphOverlay` 在 `8344`、`8361`、`8390` 连续定义/重写。
- `logicChainNodeCard`、`logicChainCanvas`、`renderLogicChainViewer` 等基础渲染函数被 wrapper 包裹，行为依赖执行顺序。

9.1.1 必须先冻结新增 `BeforeVxx`，再通过无行为拆分把 patch pipeline 合并成显式模块顺序。

## 风险分级

### 可读性风险

| Priority | Finding | Evidence | Required action |
| --- | --- | --- | --- |
| P0 | `WebAdminFrontendScripts.appJs()` 已不可局部理解 | 8,282 行 / 1.85M chars method | 前端模块拆分前禁止继续加 9.2 业务逻辑。 |
| P0 | `WebAdminLogicChainEditorService` 职责过载 | 5,205 行，`saveDraft` 204 行并串联多 store writes | 先拆 validation / planner / coordinator / typed executors。 |
| P1 | `StabilizationGuardTest` 成为 12k 行总入口 | 12,423 行 | 新 guard 不再继续塞入此类，拆出 code-health guard。 |
| P1 | BeforeVxx patch stack | `BeforeV13-17` 共 97 次 | 新增禁止，历史分阶段消化。 |
| P2 | CSS 单行压缩 | 75 行 / 123KB | 拆 tokens/layout/page/logic-chain 模块后再 concat。 |

### 维护风险

| Priority | Finding | Behavior at risk |
| --- | --- | --- |
| P0 | Hover/click/drag/zoom 走全量 render/layout | Logic Chain 编辑体验、modal 输入流畅度、scroll retention |
| P0 | Logic Chain save 是跨 store 顺序 side effect | VBD / world device / RegionController / action delete failure recovery |
| P1 | selected node 参与 VBD draft target fallback | 修 selection 或详情面板可能误改 capture writeback target |
| P1 | protected draft registry 是 static synchronized 全局状态机 | 过期、cleanup、server thread boundary、并发语义 |
| P1 | WebAdminServer route + snapshot + service wiring 混合 | 新写入口容易漏 snapshot / audit / CSRF / same-origin |
| P2 | README stable version stale | 文档引用和验收提示词不一致 |

## 行为冻结清单

后续 9.1.1 refactor 必须冻结：

- 未配置 `conditionGroupId` 时不读 store、不建 EvaluationContext、不 evaluate。
- Gate 只表达 allow/block/skip，不产生 branch/else/fallback。
- Action type 不新增，保持 `command/message/sound/signal/state_variable/timer_start/timer_cancel`。
- Logic Chain canvas 只保存 typed config 和 channel metadata，不保存 freeform graph document。
- Channel Endpoint 仍是 metadata/reference，不是 runtime consumer；SignalEmitter/VBD output、SignalReceiver/ActionRelay input 由 graph edge 拥有，不恢复可编辑 channel 字段。
- VBD / World Device / RegionController 仍必须通过 protected client-assisted draft；fake world/position payload 后端拒绝。
- Protected draft actor + editLock + draftSession 必须匹配；fake world/pos 拒绝；cancel/timeout/server stop 为 terminal；world-device cleanup / current rollback-supported boundary、inventory restore、command mutation block、不 force-load chunk 的边界不得放松。
- Logic Chain save 必须保留 permission、CSRF/same-origin、edit lock、expectedFingerprint、validation、audit、realtime、write-before snapshot、failure keeps draft/lock。
- itemSubmit/container capture 在 final Logic Chain save 前不得写 formal VBD。
- VBD trigger modal 保持 in-place second-level page、scroll/page stack、capture retry、empty requirement writeback；final Logic Chain save 前不写 formal VBD。
- Node delete 保持 reference node 拒绝、typed-owned delete draft-only、单次 node delete fail-closed、确认短语、VBD unbind 不破坏世界方块、physical device delete 才删除方块且有警告。
- ConditionGroup available list / compatibility profile / backend validation 必须一致；missing/disabled/invalid/incompatible fail-safe 且中文可读。
- StateVariable 仍只允许 GLOBAL/PLAYER + BOOLEAN/INTEGER/STRING；stable identity rename 拒绝；`state_variable` action-first visual 不变。
- SignalBridge emit、ActionEngine action order、RegionController enter/exit/stay、VBD consume/emit 顺序不得因拆分改变。
- Realtime / silent refresh 不得闪屏、跳顶、关 modal、清输入、重置筛选/分页。

## 为什么不建议继续直接堆 9.2

9.2 typed actions / Rich Text Builder 会新增更多 action editor UI、payload draft state、modal state、validation copy、guard markers 和 backend write paths。如果直接叠到当前结构：

- `WebAdminFrontendScripts.java` 会继续膨胀，inline handler 和 BeforeVxx wrapper 继续增加。
- Logic Chain editor 的 `appState.logicChainEditor` 会混入 rich text draft、typed action builder draft、selection state 和 save coordination state。
- `WebAdminLogicChainEditorService.saveDraft` 会继续成为跨 store “补丁总线”，无法证明 failure recovery 和 lock release 不变。
- `StabilizationGuardTest` 会继续膨胀，guard 本身变成维护风险。

结论：9.1.1 必须先做行为冻结、模块拆分、状态所有权矩阵、性能 marker 和 code-health guard，再进入 9.2。
