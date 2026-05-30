package com.zcpu.tzzmod.webadmin;

// Typed Action 摘要 helper 只做前端展示文本。它不发请求、不保存草稿、不改变 dirty
// comparison，也不把 raw JSON 当主摘要；后端 WebAdminActionSummaryService 仍是 audit /
// snapshot 的权威实现，前端这里只镜像当前字段用于 draft 和旧 DTO fallback。
final class WebAdminActionSummaryScripts {
    private WebAdminActionSummaryScripts() {
    }

    static String appJs() {
        return """
                function typedActionCleanSummary(value){const text=String(value||'').trim();if(!text)return '暂无摘要';return text.replace(/^command:\\s*/i,'执行命令 /').replace(/^message:\\s*/i,'向玩家显示消息：').replace(/^sound:\\s*/i,'播放音效：').replace(/^signal:\\s*/i,'发送信号到频道 ').replace(/^state_variable:\\s*/i,'状态变量动作 ').replace(/^timer_start:\\s*/i,'启动计时器 ').replace(/^timer_cancel:\\s*/i,'取消计时器 ').replace(/^unknown:\\s*/i,'未知：');}
                function typedActionTruncate(value,limit=96){const text=String(value||'').trim();return text.length>limit?text.slice(0,limit-3)+'...':text;}
                function typedActionStateSummary(action={},audit=false){const key=String(action.stateKey||'').trim()||'未配置 key',op=String(action.stateOperation||'').toLowerCase(),scope=String(action.stateScope||'').trim(),target=String(action.stateTargetMode||'').trim(),targetText=[scope,target].filter(Boolean).join(' · '),suffix=targetText?` · ${targetText}`:'';if(audit)return `状态变量动作 ${key}`;if(op==='set_variable')return `设置状态变量 ${key} = ${typedActionTruncate(action.stateValue||'未配置值')}${suffix}`;if(op==='increment_variable')return `增加状态变量 ${key} += ${Number(action.stateDelta||0)}${suffix}`;if(op==='decrement_variable')return `减少状态变量 ${key} -= ${Number(action.stateDelta||0)}${suffix}`;if(op==='toggle_boolean')return `切换状态变量 ${key}${suffix}`;if(op==='clear_variable')return `清除状态变量 ${key}${suffix}`;return `状态变量动作 ${key}${suffix}`;}
                function typedActionTimerSummary(action={},start=true){const id=String(action.timerId||'').trim()||'未配置 Timer',target=String(action.timerTargetMode||'').trim()||'默认目标';let text=`${start?'启动计时器':'取消计时器'} ${id} · ${target}`;if(start){const policy=String(action.timerStartPolicyOverride||'').trim()||'使用 Timer 定义策略';text+=` · ${policy}`;if(Number(action.timerDurationOverrideTicks||0)>0)text+=` · 覆盖时长 ${Number(action.timerDurationOverrideTicks||0)} ticks`;}else if(action.timerMissingBehavior){text+=` · 缺失策略 ${action.timerMissingBehavior}`;}return text;}
                function typedActionDisplaySummary(action={},options={}){const a=action||{},redact=!!options.redactCommand,type=String(a.type||'').toLowerCase();if(!redact&&!isBlank(a.summary))return typedActionCleanSummary(a.summary);let base;if(type==='command'){const value=String(a.value||'').trim();base=redact?(value?`执行命令（已隐藏，长度 ${value.length}）`:'执行命令（未配置）'):(value?`执行命令 /${typedActionTruncate(value)}`:'执行命令（未配置）');}else if(type==='message')base=`向玩家显示消息：${typedActionTruncate(a.value||'未配置消息')}`;else if(type==='sound')base=`播放音效：${typedActionTruncate(a.value||'未配置音效')}`;else if(type==='signal')base=`发送信号到频道 ${typedActionTruncate(a.value||'未配置频道')}`;else if(type==='state_variable')base=typedActionStateSummary(a,redact);else if(type==='timer_start')base=typedActionTimerSummary(a,true);else if(type==='timer_cancel')base=typedActionTimerSummary(a,false);else base=typedActionCleanSummary(a.summary||a.value||'尚未配置');const parts=[];if(a.enabled===false)parts.push('已禁用');parts.push(base);if(a.conditionGroupId)parts.push(`条件组 ${a.conditionGroupId}`);if(Number(a.cooldownTicks||0)>0)parts.push(`冷却 ${Number(a.cooldownTicks||0)} ticks`);return parts.join(' · ');}
                """;
    }
}
