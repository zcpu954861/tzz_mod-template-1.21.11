package com.zcpu.tzzmod.webadmin;

// VBD helper 模块负责 native trigger 的可读摘要、diff rows、trigger 输出行和 capture requirement
// 摘要。这里的 UI 表面必须以中文 summary 为主，nativeTriggerJson 只能作为 secondary/debug 数据。
// 输出频道仍由 graph edge/draft 预览表达，正式 VBD store 写入只发生在 Logic Chain save 边界。
final class WebAdminLogicChainVbdScripts {
    private WebAdminLogicChainVbdScripts() {
    }

    static String appJs() {
        return new StringBuilder().append("""
                const logicChainV16VbdTriggerGraphSummaryMarkers='data-logic-chain-vbd-trigger-readable-draft-summary data-logic-chain-vbd-native-json-not-primary-summary data-logic-chain-vbd-trigger-channel-draft-edge data-logic-chain-vbd-trigger-graph-render-before-save data-logic-chain-vbd-capture-modal-captured-state data-logic-chain-vbd-capture-modal-applied-state data-logic-chain-vbd-capture-button-state data-logic-chain-vbd-itemsubmit-capture-button-state data-logic-chain-vbd-container-capture-button-state';
                """)
                .append("""
                function logicChainJsonObject(text){try{const value=JSON.parse(String(text||'{}'));return value&&typeof value==='object'&&!Array.isArray(value)?value:{};}catch(_){return {};}}
                """)
                .append("""
                function logicChainJsonArray(text){try{const value=JSON.parse(String(text||'[]'));return Array.isArray(value)?value:[];}catch(_){return [];}}
                """)
                .append("""
                function logicChainVbdNativeOriginalValues(draft){return logicChainJsonObject(draft?.originalJson||'{}');}
                """)
                .append("""
                function logicChainVbdNormalizeConditionRows(rows){return (rows||[]).filter(r=>!isBlank(r?.property)||!isBlank(r?.value)).map(r=>({property:String(r.property||''),value:String(r.value||'')}));}
                """)
                .append("""
                function logicChainVbdConditionRowsText(rows){const list=logicChainVbdNormalizeConditionRows(rows);return list.length?list.map(r=>`${r.property}=${r.value}`).join('，'):'未配置';}
                """)
                .append("""
                function logicChainVbdReadableValue(value,kind='text'){if(kind==='bool')return value?'是':'否';if(kind==='rows')return logicChainVbdConditionRowsText(value);if(kind==='ticks')return `${Number(value||0)} tick`;if(kind==='channel')return normalizeLogicChainDraftChannel(value)||'未设置';if(kind==='gate')return value||'未配置';if(value===undefined||value===null||value==='')return '未配置';return String(value);}
                """)
                .append("""
                function logicChainVbdSameSummaryValue(a,b,kind='text'){const normalize=value=>kind==='rows'?JSON.stringify(logicChainVbdNormalizeConditionRows(value)):(kind==='channel'?normalizeLogicChainDraftChannel(value):String(value??''));return normalize(a)===normalize(b);}
                """)
                .append("""
                function logicChainVbdNativeTriggerSpecs(){return [
                """)
                .append("""
                  {type:'redstone_powered',label:'红石 / 受电状态',enabledKey:'redstoneEnabled',fields:[['启用','redstoneEnabled','bool'],['模式','redstoneMode','text'],['输出频道','channel','channel'],['断电频道','offChannel','channel'],['条件组','redstoneConditionGroupId','gate']]},
                """)
                .append("""
                  {type:'blockstate',label:'BlockState 条件',enabledKey:'blockStateEnabled',fields:[['启用','blockStateEnabled','bool'],['模式','conditionMode','text'],['输出频道','channel','channel'],['退出频道','offChannel','channel'],['属性条件','conditionRows','rows'],['条件组','blockStateConditionGroupId','gate']]},
                """)
                .append("""
                  {type:'right_click',label:'右键交互',enabledKey:'interactionEnabled',fields:[['启用','interactionEnabled','bool'],['交互频道','interactChannel','channel'],['冷却','interactionCooldownTicks','ticks'],['交互条件组','interactionConditionGroupId','gate']]},
                """)
                .append("""
                  {type:'itemSubmit',label:'itemSubmit',enabledKey:'interactionEnabled',fields:[['itemSubmit 条件组','itemSubmitConditionGroupId','gate']]},
                """)
                .append("""
                  {type:'container_open',label:'容器打开',enabledKey:'containerOpenEnabled',fields:[['启用','containerOpenEnabled','bool'],['输出频道','containerOpenChannel','channel'],['条件组','containerOpenConditionGroupId','gate'],['容器冷却','containerCooldownTicks','ticks']]},
                """)
                .append("""
                  {type:'container_close',label:'容器关闭',enabledKey:'containerCloseEnabled',fields:[['启用','containerCloseEnabled','bool'],['输出频道','containerCloseChannel','channel'],['条件组','containerCloseConditionGroupId','gate'],['容器冷却','containerCooldownTicks','ticks']]},
                """)
                .append("""
                  {type:'container_change',label:'容器内容变化',enabledKey:'containerChangeEnabled',fields:[['启用','containerChangeEnabled','bool'],['输出频道','containerChangeChannel','channel'],['检查间隔','containerChangeCheckIntervalTicks','ticks'],['条件组','containerChangeConditionGroupId','gate'],['容器冷却','containerCooldownTicks','ticks']]}
                """)
                .append("""
                ];}
                """)
                .append("""
                function logicChainVbdStoredNativeTriggerDraft(d){return d?.virtualBlockDevice?.nativeTriggerDraft||null;}
                """)
                .append("""
                function logicChainVbdNativeTriggerReadableDraftRows(d){const draft=logicChainVbdStoredNativeTriggerDraft(d);if(!draft||!vbdNativeTriggerDirty(draft))return [];const before=logicChainVbdNativeOriginalValues(draft),after=draft.values||{},rows=[];logicChainVbdNativeTriggerSpecs().forEach(spec=>{const changed=[];spec.fields.forEach(([label,key,kind])=>{if(!logicChainVbdSameSummaryValue(before[key],after[key],kind))changed.push({label,key,kind});});if(!changed.length)return;changed.forEach(field=>rows.push({title:`VBD 触发项更新：${spec.label}`,field:field.label,before:logicChainVbdReadableValue(before[field.key],field.kind),after:logicChainVbdReadableValue(after[field.key],field.kind),kind:'field',vbdNativeTriggerReadable:true}));});if(!rows.length)rows.push({title:d?.displayName||d?.label||d?.deviceId||'VBD',field:'VBD 触发项配置',before:'原配置',after:'已更新（部分字段无法摘要）',kind:'field',vbdNativeTriggerReadable:true});return rows;}
                """)
                .append("""
                function logicChainVbdRequirementListText(rows,kind){const list=Array.isArray(rows)?rows:[];if(!list.length)return '0 条';const head=list.slice(0,3).map((r,index)=>`${r.displayName||r.templateSummary||r.itemId||`${kind} #${index+1}`} x${r.count??r.requiredCount??1}`).join('；');return `${list.length} 条 · ${head}${list.length>3?'；...':''}`;}
                """)
                .append("""
                function logicChainVbdRequirementDraftReadableRows(d){if(String(d?.kind||'').toLowerCase()!=='virtual_block_device')return [];const vbd=d.virtualBlockDevice||{},rows=[],beforeItems=logicChainJsonArray(d.original?.vbdItemSubmitDraftJson),beforeContainers=logicChainJsonArray(d.original?.vbdContainerDraftJson),afterItems=vbd.itemSubmitRequirements||[],afterContainers=vbd.containerRequirements||[];if(JSON.stringify(beforeItems)!==JSON.stringify(afterItems))rows.push({title:'VBD 触发项更新：itemSubmit',field:'捕获 requirement',before:logicChainVbdRequirementListText(beforeItems,'itemSubmit'),after:logicChainVbdRequirementListText(afterItems,'itemSubmit'),kind:'field',vbdTriggerRequirementReadable:true});if(JSON.stringify(beforeContainers)!==JSON.stringify(afterContainers))rows.push({title:'VBD 触发项更新：容器内容变化',field:'container 捕获模板',before:logicChainVbdRequirementListText(beforeContainers,'container'),after:logicChainVbdRequirementListText(afterContainers,'container'),kind:'field',vbdTriggerRequirementReadable:true});return rows;}
                """)
                .append("""
                const logicChainExistingDiffRowsBeforeV16=logicChainExistingDiffRows;logicChainExistingDiffRows=function(d){const rows=logicChainExistingDiffRowsBeforeV16(d);if(String(d?.kind||'').toLowerCase()!=='virtual_block_device')return rows;const hidden=new Set(['nativeTriggerJson','vbdItemSubmitDraftJson','vbdContainerDraftJson']);return rows.filter(row=>!hidden.has(row.field)).concat(logicChainVbdNativeTriggerReadableDraftRows(d),logicChainVbdRequirementDraftReadableRows(d));};
                """)
                .append("""
                function logicChainVbdNativeTriggerOutputRows(values={}){const rows=[];const push=(type,label,channel,enabled)=>{const normalized=normalizeLogicChainDraftChannel(channel);if(enabled&&normalized)rows.push({type,label,channel:normalized});};push('redstone_powered','红石 / 受电状态',values.channel,values.redstoneEnabled);push('redstone_powered_off','红石断电',values.offChannel,values.redstoneEnabled);push('blockstate','BlockState 条件',values.channel,values.blockStateEnabled);push('blockstate_exit','BlockState 退出',values.offChannel,values.blockStateEnabled);push('right_click','右键交互',values.interactChannel,values.interactionEnabled);push('container_open','容器打开',values.containerOpenChannel,values.containerOpenEnabled);push('container_close','容器关闭',values.containerCloseChannel,values.containerCloseEnabled);push('container_change','容器内容变化',values.containerChangeChannel,values.containerChangeEnabled);return rows;}
                """)
                .append("""
                function logicChainVbdNativeTriggerOutputChannels(values){return Array.from(new Set(logicChainVbdNativeTriggerOutputRows(values).map(row=>row.channel).filter(Boolean)));}
                """)
                .append("""
                function logicChainVbdTriggerOutputSummary(values){const rows=logicChainVbdNativeTriggerOutputRows(values);return rows.length?rows.slice(0,3).map(row=>`${row.label} -> ${row.channel}`).join('；')+(rows.length>3?'；...':''):'触发项草稿尚未设置输出频道';}
                """)
                .append("""
                const logicChainOverlayHasConnectionChangesBeforeV16=logicChainOverlayHasConnectionChanges;logicChainOverlayHasConnectionChanges=function(editor){if(logicChainOverlayHasConnectionChangesBeforeV16(editor))return true;return logicChainExistingNodeDraftsForPreview(editor).some(d=>{if(String(d?.kind||'').toLowerCase()!=='virtual_block_device')return false;const draft=logicChainVbdStoredNativeTriggerDraft(d);if(!draft||!vbdNativeTriggerDirty(draft))return false;const before=logicChainVbdNativeTriggerOutputChannels(logicChainVbdNativeOriginalValues(draft)),after=logicChainVbdNativeTriggerOutputChannels(draft.values||{});return before.length!==after.length||before.some(channel=>!after.includes(channel))||after.some(channel=>!before.includes(channel));});};
                """).toString();
    }
}
