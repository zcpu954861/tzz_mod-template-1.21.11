# 9.1.1 Obsidian Second Brain Current Context

## Scope

Phase 8 creates a local Obsidian knowledge vault for TZZ Mod / WebAdmin / Logic Chain maintenance notes. This phase is docs-only for the main repository and does not change runtime code, WebAdmin API, WebAdmin UI behavior, Logic Chain save semantics, guard thresholds or MCP tooling.

## Vault

- Vault path: `E:\minecraftserver\fabricmod\tzz-mod-docs-obsidian\Tzz Mod work`
- Vault Git status: not a Git repository as of Phase 8; do not run `git init` inside this phase.
- Main index note: `99_索引/TZZ_Mod_索引.md`
- Source repo branch while writing: `feature/obsidian-second-brain-9-1-1`
- Source repo head while writing: `2abe4f0`
- Source repo upstream display while writing: `origin/feature/codebase-health-guard-ratchet-9-1-1`; Phase 8 push must explicitly target `origin feature/obsidian-second-brain-9-1-1`.
- Stable baseline: `v1.68.1-codebase-health-audit` peeled commit `57212e5bb40777620742dbdd8ee65a867a993b23`
- Vault checkpoint status: unavailable because the vault is not a Git repository. The main repo checkpoint records only this current-context file, not the vault markdown contents themselves.

## Created Vault Notes

- `99_索引/TZZ_Mod_索引.md`
- `00_项目总览/TZZ_Mod_项目总览.md`
- `01_版本时间线/TZZ_Mod_版本时间线.md`
- `02_架构总览/TZZ_Mod_模块边界图.md`
- `03_WebAdmin/WebAdmin_架构与文件职责.md`
- `04_LogicChain/LogicChain_全局编辑器.md`
- `04_LogicChain/LogicChain_Draft_事务与保存.md`
- `05_VBD_世界设备_区域控制器/VBD_世界设备_区域控制器.md`
- `06_Action_Condition_State/Action_Condition_State_体系.md`
- `07_9.1.1_代码健康治理/9.1.1_技术债治理总览.md`
- `07_9.1.1_代码健康治理/Phase1_to_Phase7_治理记录.md`
- `08_Codex工作流/Codex_工作流与提示词规范.md`
- `09_Roadmap_9x_10x/9x_10x_路线图.md`

## Source Materials

- `README.md`
- `docs/LOGIC_CHAIN_GLOBAL_EDITOR_COMPLETION_9_1_CURRENT_CONTEXT.md`
- `docs/LOGIC_CHAIN_GLOBAL_EDITOR_CAPABILITY_MATRIX_9_1.md`
- `docs/CODEBASE_HEALTH_GUARD_BASELINE_9_1_1_CURRENT_CONTEXT.md`
- `docs/CODE_QUALITY_GUARD_PLAN_9_1_1.md`
- `docs/PERFORMANCE_HOTSPOTS_9_1_1.md`
- `docs/IF_COMPLEXITY_HOTSPOT_AUDIT_9_1_1.md`
- `docs/REFACTOR_PLAN_9_1_1.md`
- `docs/LEGACY_DATAPACK_AUDIT_9_0.md`
- `docs/LEGACY_DATAPACK_PARITY_MATRIX_9_0.md`
- `docs/NINE_X_ROADMAP_INPUT_AFTER_AUDIT_9_0.md`
- `docs/TYPED_ACTIONS_RICH_TEXT_AUDIT_9_0.md`
- `docs/LEGACY_ITEMS_SYSTEMS_INTEGRATION_AUDIT_9_0.md`

## Maintenance Rules

- Treat the vault as a knowledge index, not a source-code mirror. Do not paste large code blocks.
- Update vault notes after reading the current prompt, `docs/*CURRENT_CONTEXT.md`, capability matrices and current code state.
- Do not claim future roadmap items as implemented. GameController, MissionSystem, PhaseController, if/else runtime, Scratch editor, freeform graph save, automatic datapack migration and typed action expansion remain future work unless a later phase implements them.
- Preserve the 9.1.1 guard vocabulary: safety `if` branches remain explicit and fail-closed; routing/render/UI-builder complexity is reduced only when equivalent behavior is proven by guard snapshots.
- Phase 8 deliverables are the markdown notes listed above. Keep `.obsidian/` config out of deliverables; if a local Obsidian app updates workspace UI state, report it and do not treat it as a Phase 8 checkpoint artifact.

## Validation

For this docs-only Phase 8 main-repo checkpoint, run `git diff --check`, explicitly scan the untracked current-context file and vault markdown for trailing whitespace, then run `git diff --cached --check` after explicit staging. Gradle and MCP npm validation are not required unless code, guard or MCP tooling changes.
