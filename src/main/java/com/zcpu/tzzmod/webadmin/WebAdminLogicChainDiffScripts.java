package com.zcpu.tzzmod.webadmin;

final class WebAdminLogicChainDiffScripts {
    private WebAdminLogicChainDiffScripts() {
    }

    static String appJs() {
        return new StringBuilder().append("""
                function logicChainComparableExistingDraft(d){if(!d)return {};if(d.kind==='channel_metadata')return {displayName:d.displayName||'',note:d.note||'',iconKey:d.iconKey||'auto'};if(d.kind==='signal_join')return {displayName:d.displayName||'',note:d.note||'',enabled:d.enabled!==false,inputChannels:(d.inputChannels||[]).map(i=>String(i.channel||'').trim()).filter(Boolean),outputChannel:d.outputChannel||'',mode:d.modeValue||'ALL',threshold:Number(d.threshold||0),scopeMode:d.scopeMode||'GLOBAL',resetPolicy:d.resetPolicy||'RESET_AFTER_EMIT',timeoutTicks:Number(d.timeoutTicks||0),cooldownTicks:Number(d.cooldownTicks||0)};if(d.kind==='timer')return {displayName:d.displayName||'',note:d.note||'',enabled:d.enabled!==false,mode:d.modeValue||'DELAY',durationTicks:Number(d.durationTicks||0),intervalTicks:Number(d.intervalTicks||0),maxRuns:Number(d.maxRuns||0),scopeMode:d.scopeMode||'GLOBAL',startPolicy:d.startPolicy||'RESTART',outputChannel:d.outputChannel||''};if(d.kind==='signal_listener')return {enabled:d.enabled!==false,channel:d.channel||'',cooldownTicks:Number(d.cooldownTicks||0),conditionGroupId:d.conditionGroupId||''};return {};}
                """)
                .append("""
                function logicChainComparableAction(action){const a=action||{}, type=String(a.type||'').toLowerCase();return {type,value:type==='state_variable'||type==='timer_start'||type==='timer_cancel'?'':String(a.value||''),enabled:a.enabled!==false,cooldownTicks:Number(a.cooldownTicks||0),requiresOp:type==='command'&&!!a.requiresOp,notifyOps:type==='command'&&!!a.notifyOps,conditionGroupId:a.conditionGroupId||'',stateOperation:a.stateOperation||'',stateScope:a.stateScope||'',stateTargetMode:a.stateTargetMode||'',stateTargetId:a.stateTargetId||'',stateKey:a.stateKey||'',stateValueType:a.stateValueType||'',stateValue:a.stateValue||'',stateDelta:a.stateDelta||'',stateCreateIfMissing:!!a.stateCreateIfMissing,stateInitialValue:a.stateInitialValue||'',timerId:a.timerId||'',timerTargetMode:a.timerTargetMode||'',timerTargetId:a.timerTargetId||'',timerStartPolicyOverride:a.timerStartPolicyOverride||'',timerDurationOverrideTicks:Number(a.timerDurationOverrideTicks||0)};}
                """)
                .append("""
                function logicChainDiffRows(before={},after={},kind='field'){const keys=[...new Set([...Object.keys(before||{}),...Object.keys(after||{})])];return keys.filter(k=>JSON.stringify(before[k]??'')!==JSON.stringify(after[k]??'')).map(k=>({field:k,before:before[k],after:after[k],kind}));}
                """)
                .append("""
                function logicChainExistingDiffRows(d){const connectionFields=new Set(['inputChannels','outputChannel','channel']);return logicChainDiffRows(d?.original||{},logicChainComparableExistingDraft(d),'field').map(row=>connectionFields.has(row.field)?{...row,kind:'connection'}:row);}
                """)
                .append("""
                function logicChainExistingEditHasChanges(d){return !!d&&logicChainExistingDiffRows(d).length>0;}
                """)
                .append("""
                function logicChainActionEditHasChanges(d){if(!d)return false;const changed=logicChainDiffRows(d.original||{},logicChainComparableAction(d.action||{}),'action').length>0;return changed||(d.operation==='disable'&&(d.original||{}).enabled!==false);}
                """)
                .append("""
                function logicChainExistingEditModalDirty(){const d=appState.logicChainEditor?.existingEdit;return !!d&&!d.confirmed&&logicChainExistingEditHasChanges(d);}
                """)
                .append("""
                function logicChainActionEditModalDirty(){const d=appState.logicChainEditor?.actionEdit;return !!d&&!d.confirmed&&logicChainActionEditHasChanges(d);}
                """)
                .append("""
                function logicChainDiffValue(value){if(Array.isArray(value))return value.join(', ');if(typeof value==='boolean')return value?'是':'否';if(value===undefined||value===null||value==='')return '未配置';return String(value);}
                """)
                .append("""
                function logicChainDraftDiffLabel(row){const name=row.title?`${row.title} · `:'';return `${name}${row.field}：${logicChainDiffValue(row.before)} -> ${logicChainDiffValue(row.after)}`;}
                """)
                .append("""
                function logicChainDraftNodeTitle(n){return n?.signalJoin?.displayName||n?.timer?.displayName||n?.signalListener?.displayName||n?.signalListener?.name||n?.virtualBlockDevice?.displayName||n?.worldDevice?.displayName||n?.worldDevice?.deviceId||n?.regionController?.controllerDisplayName||n?.id||'新增节点';}
                """)
                .append("""
                function logicChainDraftDiffRowsForNestedActions(e){const rows=[];(e?.nodes||[]).forEach(node=>logicChainDraftNestedActions(node).forEach(entry=>{const action=entry.action||{},index=Number(entry.index||0)+1,title=`${logicChainDraftNodeTitle(node)} · ${logicChainActionAppendBucketLabel(entry.ownerType,entry.bucket)} #${index}`,summary=`${labelActionType(action.type)} ${logicChainDraftActionSummary(action)}`;rows.push(action._pendingDelete===true?{title,field:'删除草稿 Action',before:summary,after:'保存时过滤/删除',kind:'action',nestedAction:true,pendingDelete:true}:{title,field:'新增草稿 Action',before:'无',after:summary,kind:'action',nestedAction:true});}));return rows;}
                """)
                .append("""
                function logicChainDraftDiffRowsForSession(e){const rows=[];(e?.nodes||[]).filter(n=>String(n.type||'').toLowerCase()!=='channel_endpoint').forEach(n=>rows.push({title:logicChainDraftNodeTitle(n),field:'新增节点',before:'无',after:logicChainNodeTypeLabel(n.type,n.type),kind:'field'}));(e?.edges||[]).forEach(edge=>rows.push({title:'新增连线',field:edge.type||'edge',before:edge.from,after:edge.to,kind:'connection'}));(e?.draftChannels||[]).filter(c=>c.metadataDraft||c.cardDraft).forEach(c=>rows.push({title:c.displayName||c.channel,field:'频道端点',before:'无',after:c.channel,kind:'connection'}));if(e?.actionAppend?.confirmed){const a=e.actionAppend.action||{};rows.push({title:logicChainActionAppendOwnerLabel(e.actionAppend),field:'追加 Action',before:'无',after:`${labelActionType(a.type)} ${logicChainActionDraftSummary(a)}`,kind:'action'});}logicChainDraftDiffRowsForNestedActions(e).forEach(row=>rows.push(row));logicChainExistingNodeDraftsForPreview(e).forEach(d=>logicChainExistingDiffRows(d).forEach(row=>rows.push({...row,title:d.displayName||d.label||d.targetId||d.channel||d.id||'已有节点'})));logicChainActionDraftsForPreview(e).forEach(d=>{logicChainDiffRows(d.original||{},logicChainComparableAction(d.action||{}),'action').forEach(row=>rows.push({...row,title:`${logicChainActionAppendOwnerLabel(d)} #${Number(d.actionIndex||0)+1}`}));if(d.operation==='disable'&&(d.original||{}).enabled!==false)rows.push({title:`${logicChainActionAppendOwnerLabel(d)} #${Number(d.actionIndex||0)+1}`,field:'operation',before:'replace',after:'disable',kind:'action'});});logicChainConfirmedNodeDeleteDrafts(e).forEach(d=>rows.push({title:d.ownerLabel||d.targetId||'已有节点',field:'删除节点',before:d.nodeType||'typed-owned',after:'保存时删除',kind:'action'}));logicChainConfirmedActionDeleteDrafts(e).forEach(d=>rows.push({title:`${logicChainActionAppendOwnerLabel(d)} #${Number(d.actionIndex||0)+1}`,field:'删除 Action',before:'保留',after:'保存时删除',kind:'action'}));logicChainConfirmedActionReorderDrafts(e).forEach(d=>rows.push({title:logicChainActionAppendOwnerLabel(d),field:'重排 Action',before:`#${Number(d.fromIndex||0)+1}`,after:`#${Number(d.toIndex||0)+1}`,kind:'action'}));return rows;}
                """)
                .append("""
                function logicChainDraftDiffBanner(e){let rows=[];try{rows=logicChainDraftDiffRowsForSession(e);}catch(err){console.error('Logic Chain draft diff render failed',err);rows=[{title:'草稿摘要',field:'未保存修改',before:'详情渲染失败',after:'请查看校验提示',kind:'action'}];}if(!rows.length)return '';const expanded=!!e?.diffExpanded, latest=rows[rows.length-1], list=expanded?`<ul class="logic-chain-draft-diff-list" data-logic-chain-draft-diff-expand-all="true">${rows.map(row=>`<li data-logic-chain-diff-${row.kind==='action'?'action-change':(row.kind==='connection'?'connection-change':'field-change')}="true" data-logic-chain-diff-field-change="true" ${row.nestedAction?'data-logic-chain-draft-nested-action-diff="true"':''} ${row.pendingDelete?'data-logic-chain-draft-action-pending-delete-diff="true"':''} ${row.vbdNativeTriggerReadable?'data-logic-chain-vbd-trigger-readable-draft-summary="true" data-logic-chain-vbd-native-json-not-primary-summary="true"':''} ${row.vbdTriggerRequirementReadable?'data-logic-chain-vbd-capture-applied-diff-summary="true"':''}><strong>${esc(row.title||'草稿')}</strong><span>${esc(row.field)}</span><small>${esc(logicChainDiffValue(row.before))} -> ${esc(logicChainDiffValue(row.after))}</small></li>`).join('')}</ul>`:'';return `<section class="logic-chain-draft-diff-banner ${expanded?'expanded':''}" data-logic-chain-draft-diff="true" data-logic-chain-draft-diff-compact-banner="true" data-logic-chain-draft-diff-latest-only="${expanded?'false':'true'}" data-logic-chain-draft-diff-change-count="${rows.length}" data-logic-chain-local-reconnect="true" data-logic-chain-diff-field-change="true" data-logic-chain-diff-connection-change="true" data-logic-chain-diff-action-change="true" data-logic-chain-draft-diff-fail-soft="true"><div class="logic-chain-draft-diff-line"><strong>有未保存修改</strong><span>最近：${esc(logicChainDraftDiffLabel(latest))}</span><span class="pill warning" data-logic-chain-draft-diff-change-count="true">共 ${rows.length} 项</span><button class="wa-btn ghost" type="button" ${htmlHandler('toggleLogicChainDraftDiffExpanded()')} data-logic-chain-draft-diff-expand-all="${expanded?'false':'true'}" data-logic-chain-draft-diff-collapse="${expanded?'true':'false'}">${expanded?'收起':'展开'}</button></div>${list}</section>`;}
                """)
                .append("""
                function toggleLogicChainDraftDiffExpanded(){const e=appState.logicChainEditor;if(!e)return;e.diffExpanded=!e.diffExpanded;rerenderLogicChainEditorPreservingUi();}
                """)
                .append("""
                function logicChainDraftDiffHtml(e){return logicChainDraftDiffBanner(e);}
                """).toString();
    }
}
