package com.zcpu.tzzmod.webadmin;

final class WebAdminFrontendDeviceScripts {
    private WebAdminFrontendDeviceScripts() {
    }

    static String appJs() {
        return new StringBuilder().append("""
                function signalListenerDeleteDraft(listener,channel='',lock=null,expectedFingerprint=''){
                  return {listener:{...(listener||{}),channel:listener?.channel||channel||''},confirmed:false,reason:'',expectedFingerprint:expectedFingerprint||listener?.expectedFingerprint||'',lockId:lock?.lockId||'',lockTargetType:'signal_listener_basic_config',lockTargetId:listener?.listenerId||listener?.id||'',lock:lock||null,saving:false,errors:[]};
                }
                function listenerDeleteModalBody(draft){
                  const l=draft.listener||{}, actionCount=Number(l.actionCount ?? (l.actions||[]).length ?? 0), title=l.name||l.id||'未命名监听器';
                  const errors=lifecycleErrorsHtml(draft,'删除监听器失败');
                  return `<form class="edit-form" data-listener-delete-modal="true" data-danger-confirm-modal="true" ${htmlEvent('onsubmit','event.preventDefault();deleteSignalListenerFromModal()')}><div class="readonly-note danger"><strong>删除虚拟监听器</strong><span>会删除该监听器内的 ${esc(actionCount)} 个动作引用；不会删除频道、接收器、设备或历史记录。</span></div><div class="identity-grid">${row('名称',esc(title))}${row('技术 ID',esc(l.id||'-'))}${row('监听频道',l.channel?channelButton(l.channel):'<span class="muted">未绑定</span>')}${row('动作数量',esc(actionCount))}</div><label class="switch-row"><span>我确认删除该虚拟监听器</span><input id="listener-delete-confirmed" type="checkbox" ${draft.confirmed?'checked':''}></label><label>原因（可选）<textarea id="listener-delete-reason" maxlength="200" placeholder="仅写入 WebAdmin audit，不影响业务逻辑。">${esc(draft.reason||'')}</textarea></label>${errors}</form>`;
                }
                function showSignalListenerDeleteModal(){
                  const draft=appState.signalListenerDelete;if(!draft)return;
                  openWebAdminModal('删除虚拟监听器',listenerDeleteModalBody(draft),dangerousModalFooter(draft.saving,'确认删除'),{className:'wa-config-modal',onClose:async()=>{await releaseLogicChainExistingEditLock(draft,true);appState.signalListenerDelete=null;return dismissWebAdminModal();}});
                }
                async function openSignalListenerDeleteModal(listenerId,routeChannel=''){
                  if(!canDeleteSignalListener()){toast('需要 EDITOR 或 OWNER 权限才能删除虚拟监听器。');return;}
                  let listener=(appState.listeners||[]).find(item=>listenerMatches(item,listenerId));
                  if(!listener){
                    const result=await loadSignalListenerDetail(listenerId);
                    if(!result.ok){toast(result.message||'无法读取虚拟监听器。');return;}
                    listener={...(result.data.listener||{}),channel:result.data.channel||routeChannel};
                  }
                  let cfg={};try{cfg=await api(`/api/webadmin/signal-listener-basic-config/${encodeURIComponent(listenerId)}`);}catch(err){toast(err.message||'无法读取虚拟监听器基础配置。');return;}
                  const canonicalId=cfg.listenerId||cfg.listenerRef||listener.id||listenerId, lockResult=await acquireWebAdminEditLock('signal_listener_basic_config',canonicalId);
                  if(!lockResult.success){toast(lockResult.message||'无法获取虚拟监听器删除锁。');return;}
                  listener={...listener,id:canonicalId,listenerId:canonicalId,channel:cfg.channel||listener.channel||routeChannel,expectedFingerprint:cfg.expectedFingerprint||cfg.fingerprint||''};
                  appState.signalListenerDelete=signalListenerDeleteDraft(listener,routeChannel,lockResult.data?.lock||{},listener.expectedFingerprint||'');
                  showSignalListenerDeleteModal();
                }
                async function deleteSignalListenerFromModal(){
                  const draft=appState.signalListenerDelete;if(!draft)return;
                  const l=draft.listener||{}, listenerId=l.id||'';
                  draft.confirmed=!!document.getElementById('listener-delete-confirmed')?.checked;
                  draft.reason=document.getElementById('listener-delete-reason')?.value||'';
                  draft.saving=true;draft.errors=[];appState.signalListenerDelete=draft;showSignalListenerDeleteModal();
                  try{
                    const result=await api(`/api/webadmin/signal-listeners/${encodeURIComponent(listenerId)}/delete`,{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({listenerId,confirmed:draft.confirmed,expectedFingerprint:draft.expectedFingerprint||'',lockId:draft.lockId||'',reason:draft.reason})});
                    if(result.success){
                      appState.signalListenerDelete=null;
                      appState.listeners=(appState.listeners||[]).filter(item=>!listenerMatches(item,listenerId));
                      await dismissWebAdminModal();
                      toast(result.message||'虚拟监听器已删除。');
                      const h=currentRouteHash();
                      if((h.startsWith('#/listeners/')&&routeDetailId(h,'#/listeners/')===listenerId)||(h.startsWith('#/signal-listeners/')&&routeDetailId(h,'#/signal-listeners/')===listenerId))location.hash=result.data?.routeTarget||'#/listeners';
                      else await route({silent:true});
                      return;
                    }
                    draft.saving=false;draft.errors=writeResultErrors(result,'删除虚拟监听器失败。');appState.signalListenerDelete=draft;showSignalListenerDeleteModal();toast(result.message||'删除虚拟监听器失败。');
                  }catch(err){draft.saving=false;draft.errors=[{message:err.message||'删除虚拟监听器失败。'}];appState.signalListenerDelete=draft;showSignalListenerDeleteModal();toast(err.message||'删除虚拟监听器失败。');}
                }
                """)
.append("""
                function virtualBlockDeleteDraft(detail,lock=null,expectedFingerprint=''){
                  return {device:{...(detail||{})},confirmationText:'',confirmed:false,expectedFingerprint:expectedFingerprint||detail?.basicConfig?.expectedFingerprint||detail?.expectedFingerprint||'',lockId:lock?.lockId||'',lockTargetType:'device_basic_config',lockTargetId:detail?.id||'',lock:lock||null,reason:'',saving:false,errors:[]};
                }
                function vbdDeleteModalBody(draft){
                  const d=draft.device||{}, cfg=d.configSummary||{}, block=firstKnown(cfg,['blockId','block','minecraftBlockId','boundBlockId'])||'--', title=d.displayName||d.id||'虚拟方块设备';
                  const errors=lifecycleErrorsHtml(draft,'删除 / 解绑虚拟方块设备失败');
                  return `<form class="edit-form" data-vbd-delete-modal="true" data-danger-confirm-modal="true" ${htmlEvent('onsubmit','event.preventDefault();deleteVirtualBlockDeviceFromModal()')}><div class="readonly-note danger"><strong>删除 / 解绑虚拟方块设备</strong><span>仅删除 SignalDeviceStore / WebAdmin registry 配置，不 setblock、不破坏世界方块，也不会删除其它类型 signal device。</span></div><div class="identity-grid">${row('显示名称',esc(title))}${row('设备 ID',esc(d.id||'-'))}${row('世界 / 坐标',esc(`${d.world||'-'} ${posText(d.pos)}`))}${row('方块 ID',esc(block))}${row('频道',d.channel?channelButton(d.channel):'<span class="muted">未绑定</span>')}</div><label class="switch-row"><span>我确认只解绑该虚拟方块设备</span><input id="vbd-delete-confirmed" type="checkbox" ${draft.confirmed?'checked':''}></label><label>输入设备 ID 或显示名称确认<input id="vbd-delete-confirmation" class="input" value="${esc(draft.confirmationText||'')}" placeholder="${esc(d.id||title)}"></label><label>原因（可选）<textarea id="vbd-delete-reason" maxlength="200" placeholder="仅写入 WebAdmin audit，不影响世界方块。">${esc(draft.reason||'')}</textarea></label>${errors}</form>`;
                }
                function showVirtualBlockDeviceDeleteModal(){
                  const draft=appState.virtualBlockDelete;if(!draft)return;
                  openWebAdminModal('删除 / 解绑虚拟方块设备',vbdDeleteModalBody(draft),dangerousModalFooter(draft.saving,'确认解绑'),{className:'wa-config-modal',onClose:async()=>{await releaseLogicChainExistingEditLock(draft,true);appState.virtualBlockDelete=null;return dismissWebAdminModal();}});
                }
                async function openVirtualBlockDeviceDeleteModal(deviceId){
                  if(!canDeleteVirtualBlockDevice()){toast('需要 EDITOR 或 OWNER 权限才能删除 / 解绑虚拟方块设备。');return;}
                  let detail=(appState.currentDeviceDetail&&appState.currentDeviceDetail.id===deviceId)?appState.currentDeviceDetail:null;
                  if(!detail){try{detail=await api(`/api/devices/${encodeURIComponent(deviceId)}`);}catch(err){toast(err.message||'无法读取虚拟方块设备。');return;}}
                  if(!isVirtualBlockDevice(detail)){toast('只能删除 / 解绑 virtual_block_device。');return;}
                  let basic=detail.basicConfig||{};if(!basic.expectedFingerprint){try{basic=await api(`/api/webadmin/device-basic-config/${encodeURIComponent(deviceId)}`);}catch(err){toast(err.message||'无法读取 VBD 基础配置指纹。');return;}}
                  const lockResult=await acquireWebAdminEditLock('device_basic_config',deviceId);
                  if(!lockResult.success){toast(lockResult.message||'无法获取 VBD 删除锁。');return;}
                  detail={...detail,basicConfig:{...(detail.basicConfig||{}),...basic},expectedFingerprint:basic.expectedFingerprint||detail.expectedFingerprint||''};
                  appState.virtualBlockDelete=virtualBlockDeleteDraft(detail,lockResult.data?.lock||{},detail.expectedFingerprint||'');
                  showVirtualBlockDeviceDeleteModal();
                }
                async function deleteVirtualBlockDeviceFromModal(){
                  const draft=appState.virtualBlockDelete;if(!draft)return;
                  const d=draft.device||{}, deviceId=d.id||'';
                  draft.confirmed=!!document.getElementById('vbd-delete-confirmed')?.checked;
                  draft.confirmationText=document.getElementById('vbd-delete-confirmation')?.value?.trim()||'';
                  draft.reason=document.getElementById('vbd-delete-reason')?.value||'';
                  draft.saving=true;draft.errors=[];appState.virtualBlockDelete=draft;showVirtualBlockDeviceDeleteModal();
                  try{
                    const result=await api(`/api/webadmin/virtual-block-devices/${encodeURIComponent(deviceId)}/delete`,{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({deviceId,confirmationText:draft.confirmationText,confirmed:draft.confirmed,expectedFingerprint:draft.expectedFingerprint||'',lockId:draft.lockId||'',reason:draft.reason})});
                    if(result.success){
                      appState.virtualBlockDelete=null;
                      appState.currentDeviceDetail=appState.currentDeviceDetail?.id===deviceId?null:appState.currentDeviceDetail;
                      appState.virtualBlockDevices=(appState.virtualBlockDevices||[]).filter(item=>String(item.id)!==deviceId);
                      if(appState.virtualBlockDetailCache)delete appState.virtualBlockDetailCache[deviceId];
                      await dismissWebAdminModal();
                      toast(result.message||'虚拟方块设备已删除 / 解绑。');
                      const h=currentRouteHash();
                      if(h.startsWith('#/devices/')&&routeDetailId(h,'#/devices/')===deviceId)location.hash=result.data?.routeTarget||'#/virtual-block-devices';
                      else await route({silent:true});
                      return;
                    }
                    draft.saving=false;draft.errors=writeResultErrors(result,'删除 / 解绑虚拟方块设备失败。');appState.virtualBlockDelete=draft;showVirtualBlockDeviceDeleteModal();toast(result.message||'删除 / 解绑失败。');
                  }catch(err){draft.saving=false;draft.errors=[{message:err.message||'删除 / 解绑虚拟方块设备失败。'}];appState.virtualBlockDelete=draft;showVirtualBlockDeviceDeleteModal();toast(err.message||'删除 / 解绑失败。');}
                }
                function deviceMetadataCard(detail){
                  const meta=detail.metadata||{}, lock=detail.metadataLock||{}, editable=canEditDeviceMetadata(), editing=appState.deviceMetadataEdit&&appState.deviceMetadataEdit.deviceId===detail.id, lockedByOther=lock.locked&&!lock.heldByCurrentUser;
                  const editingAction=editing?`<button class="secondary" onclick='showDeviceMetadataEditModal(${jsString(detail.id)})'>继续编辑</button>`:'';
                  const note=isBlank(meta.note)?'<span class="muted">暂无备注</span>':esc(meta.note);
                  const updated=isBlank(meta.updatedAt)?'暂无':`${formatDateTime(meta.updatedAt)} · ${esc(meta.updatedBy||'未知用户')}`, version=Number(meta.version||0);
                  const lockHint=lockedByOther?`<div class="readonly-note">当前由 ${esc(lock.holderUsername||'其他用户')} 正在编辑，锁到期：${esc(formatDateTime(lock.expiresAt))}</div>`:'';
                  const action=editing?editingAction:(editable&&!lockedByOther?`<button class="secondary" onclick='startDeviceMetadataEdit(${jsString(detail.id)},${jsString(meta.displayName||'')},${jsString(meta.note||'')},${jsString(meta.iconKey||'auto')},${version})'>编辑显示信息</button>`:(editable?lockHint:`<div class="readonly-note">需要 EDITOR 或 OWNER 权限才能编辑 WebAdmin 显示信息。</div>`));
                  return `<div class="identity-grid">${row('显示名称',esc(meta.displayName||meta.effectiveDisplayName||detail.displayName))}${row('备注',note)}${row('图标',esc(labelMetadataIcon(meta.iconKey||'auto')))}${row('版本',esc(version))}${row('最后修改',esc(updated))}</div><p class="muted">此信息仅用于 WebAdmin 展示，不改变 Minecraft 游戏逻辑、SignalBridge 行为或设备配置。</p>${action}`;
                }
                function deviceMetadataForm(detail,draft){
                  const errors=draft.errors&&draft.errors.length?`<ul class="validation-list">${draft.errors.map(e=>`<li>${esc(e.message||'输入未通过校验')}</li>`).join('')}</ul>`:'';
                  const lock=draft.lock||{}, lockLine=lock.locked?`<div class="readonly-note">正在编辑 · 锁到期：${esc(formatDateTime(lock.expiresAt))} · 持有人：${esc(lock.holderUsername||appState.me?.username||'当前用户')}</div>`:'<div class="readonly-note">正在获取编辑锁...</div>';
                  const conflict=draft.conflict?`<div class="readonly-note">检测到保存冲突。当前版本：${esc(draft.conflict.currentVersion ?? draft.conflict?.currentMetadata?.version ?? '未知')}。<button class="link-button" type="button" onclick='reloadDeviceMetadataAfterConflict(${jsString(detail.id)})'>刷新当前信息</button></div>`:'';
                  return `<form class="edit-form" onsubmit='event.preventDefault();saveDeviceMetadata(${jsString(detail.id)})'>
                    ${lockLine}
                    <label>显示名称<input id="metadata-display-name" class="input" maxlength="64" value="${esc(draft.displayName||'')}" placeholder="${esc(detail.displayName||'')}"></label>
                    <label>备注<textarea id="metadata-note" maxlength="500" placeholder="仅用于 WebAdmin 管理备注，不支持富文本。">${esc(draft.note||'')}</textarea></label>
                    <label>图标<select id="metadata-icon" class="select">${metadataIconOptions().map(k=>`<option value="${esc(k)}" ${k===(draft.iconKey||'auto')?'selected':''}>${esc(labelMetadataIcon(k))}</option>`).join('')}</select></label>
                    ${errors}
                    ${conflict}
                    <p class="muted">保存只会写入 WebAdmin 元数据文件，不会修改 enabled、channel、itemSubmit、action 或 region 等游戏逻辑配置。</p>
                    <div class="form-actions"><button class="secondary" type="submit">${draft.saving?'保存中...':'保存'}</button><button class="text-button" type="button" onclick='closeWebAdminModal()'>取消</button></div>
                  </form>`;
                }
                function showDeviceMetadataEditModal(deviceId){
                  const draft=appState.deviceMetadataEdit;if(!draft||draft.deviceId!==deviceId)return;
                  if(appState.deviceConfigEdit&&appState.deviceConfigEdit.deviceId===deviceId){showDeviceConfigEditModal(deviceId);return;}
                  markModalInitialSnapshot('device_metadata',draft);
                  openWebAdminModal('编辑设备显示信息',deviceMetadataForm({id:deviceId,displayName:draft.displayName},draft),editModalFooter(draft.saving),{onClose:()=>cancelDeviceMetadataEdit(deviceId),syncBeforeClose:()=>syncModalDraftBeforeClose('device_metadata',deviceId),dirtyCheck:()=>modalDraftDirty('device_metadata',appState.deviceMetadataEdit)});
                }
                function deviceDisplaySummaryCard(detail){
                  const meta=detail.metadata||{}, version=Number(meta.version||0);
                  const note=isBlank(meta.note)?'<span class="muted">暂无备注</span>':esc(meta.note);
                  const updated=isBlank(meta.updatedAt)?'暂无':`${formatDateTime(meta.updatedAt)} · ${esc(meta.updatedBy||'未知用户')}`;
                  return `<div class="identity-grid">${row('显示名称',esc(meta.displayName||meta.effectiveDisplayName||detail.displayName))}${row('备注',note)}${row('图标',esc(labelMetadataIcon(meta.iconKey||'auto')))}${row('版本',esc(version))}${row('最后修改',esc(updated))}</div><p class="muted">显示信息仅影响 WebAdmin，不改变游戏内设备逻辑。</p>`;
                }
                function deviceConfigOverview(detail){
                  const basic=detail.basicConfig||{}, ext=detail.extendedConfig||{}, meta=detail.metadata||{};
                  const extFields=ext.supportedFields||[], extValues=ext.values||{}, extLabels=ext.fieldLabels||{};
                  const extRows=extFields.length?extFields.map(field=>row(extLabels[field]||field,extendedFieldDisplay(field,extValues[field]))).join(''):`<div class="readonly-note">${esc(ext.unsupportedReason||'该设备类型暂无可编辑扩展配置。')}</div>`;
                  const extNote=deviceExtendedRuntimeNote(ext);
                  const editing=appState.deviceConfigEdit&&appState.deviceConfigEdit.deviceId===detail.id;
                  const action=editing?waButton('继续编辑设备配置','settings',htmlHandler(`showDeviceConfigEditModal(${jsString(detail.id)})`),'primary'):deviceConfigEditButton(detail,'编辑设备配置','primary');
                  const nativeTriggers=isVirtualBlockDevice(detail)?vbdNativeTriggerConfigSummaryCard(detail):'';
                  const legacyTypeSpecific=isVirtualBlockDevice(detail)?'':`<section class="wa-config-card" data-type-specific-config-card="true"><h3>类型专属配置</h3><div class="identity-grid">${extRows}</div>${extNote}</section>`;
                    const summaryNote=isVirtualBlockDevice(detail)?'VBD 基础配置和原生触发配置使用固定 Modal 编辑；交互物品匹配和 7.11 统一 itemSubmit requirement 编辑器都从右键交互条件层进入。不会创建新 consume 策略、ConditionEngine 或逻辑链图。':'基础配置与类型专属配置使用同一个固定 Modal 编辑；action_relay 可在同一 Modal 内打开 Action 列表。不会创建 itemSubmit、consume、ConditionEngine 或逻辑链图。';
                  const loadNotes=[detail.basicConfigError?`基础配置加载失败：${detail.basicConfigError.message||'未知错误'}`:'',detail.extendedConfigError?`扩展配置加载失败：${detail.extendedConfigError.message||'未知错误'}`:'',detail.nativeTriggersError?`原生触发配置加载失败：${detail.nativeTriggersError.message||'未知错误'}`:'',detail.interactionItemMatcherError?`交互物品匹配加载失败：${detail.interactionItemMatcherError.message||'未知错误'}`:''].filter(Boolean);
                  return `<div class="wa-config-summary ${nativeTriggers?'wa-vbd-config-summary':''}" ${nativeTriggers?'data-vbd-config-summary="true"':''}>
                    <section class="wa-config-card"><h3>显示信息</h3><div class="identity-grid">${row('显示名称',esc(meta.displayName||meta.effectiveDisplayName||detail.displayName))}${row('备注',isBlank(meta.note)?'<span class="muted">暂无备注</span>':esc(meta.note))}${row('图标',esc(labelMetadataIcon(meta.iconKey||'auto')))}</div></section>
                    <section class="wa-config-card"><h3>基础配置</h3><div class="identity-grid">${row('启用状态',esc(labelEnabledState(basic.enabled ?? detail.enabled)))}${row('主频道',channelCell(basic.channel||detail.channel))}</div></section>
                    ${legacyTypeSpecific}
                    ${nativeTriggers}
                  </div>${loadNotes.length?`<div class="readonly-note">${loadNotes.map(esc).join('<br>')}</div>`:''}<p class="muted">${esc(summaryNote)}</p><div class="inline-actions">${action}</div>`;
                }
                function nativeTriggerTypes(){return [
                  {type:'redstone_powered',label:'红石 / 受电状态',icon:'enabled'},
                  {type:'blockstate',label:'BlockState',icon:'device-overview'},
                  {type:'right_click',label:'右键交互',icon:'virtual-block-device'},
                  {type:'container_open',label:'容器打开',icon:'chest'},
                  {type:'container_close',label:'容器关闭',icon:'close'},
                  {type:'container_change',label:'容器内容变化',icon:'history'}
                ];}
                function labelNativeTriggerType(type){return (nativeTriggerTypes().find(item=>item.type===type)||{}).label||type;}
                function vbdNativeTriggerData(detail){return detail?.nativeTriggers||{};}
                function vbdNativeTriggerMap(detail){return vbdNativeTriggerData(detail).triggers||{};}
                function activeVbdNativeTriggerTypes(detail){
                  const data=vbdNativeTriggerData(detail), map=vbdNativeTriggerMap(detail);
                  const allowed=new Set(nativeTriggerTypes().map(item=>item.type));
                  const fromApi=Array.isArray(data.activeTriggerTypes)?data.activeTriggerTypes.filter(type=>allowed.has(type)):[];
                  if(fromApi.length)return fromApi;
                  return nativeTriggerTypes().map(item=>item.type).filter(type=>!!map[type]?.enabled);
                }
                function vbdSingleItemSubmitDisabledWarning(detail){
                  const interaction=(vbdNativeTriggerMap(detail).right_click)||{}, submit=interaction.itemSubmitLayer||{};
                  if(interaction.interactionEnabled||!(submit.configured||submit.enabled||Number(submit.requirementCount||0)>0))return '';
                  return '<div class="readonly-note warning" data-single-item-submit-disabled-warning="true" data-single-item-submit-hidden-when-interaction-disabled="true" data-unified-item-submit-disabled-warning="true">已配置 itemSubmit requirements，但右键交互触发尚未启用；itemSubmit 属于右键交互后的提交层，当前不会显示完整编辑入口。</div>';
                }
                function vbdNativeTriggerEditAction(detail,kind='primary'){
                  const data=vbdNativeTriggerData(detail), lock=data.lockStatus||appState.deviceEditLocks[editLockCacheKey('virtual_block_device_triggers',detail?.id||'')]||null;
                  if(lockHeldByOther(lock))return `${waButton('编辑原生触发配置','settings',`disabled title="${esc(lockMessage(lock,'原生触发配置'))}" data-vbd-native-trigger-lock-disabled="true"`,'ghost is-locked')}<span class="wa-lock-badge">${esc(lockMessage(lock,'原生触发配置'))}</span>`;
                  if(!canEditVbdNativeTriggers())return waButton('编辑原生触发配置','settings','disabled title="需要 EDITOR 或 OWNER 权限才能编辑原生触发配置。"','ghost');
                  return waButton('编辑原生触发配置','settings',htmlHandler(`startVbdNativeTriggerEdit(${jsString(detail.id)})`),kind);
                }
                function vbdNativeTriggerOverviewCard(detail,options={}){
                  if(!isVirtualBlockDevice(detail))return '';
                  if(detail.nativeTriggersError)return errorBlock(detail.nativeTriggersError.message||'原生触发配置加载失败');
                  const data=vbdNativeTriggerData(detail);
                  if(!data.supported)return `<div class="readonly-note">${esc(data.unsupportedReason||'当前设备不支持原生触发摘要。')}</div>`;
                  const active=activeVbdNativeTriggerTypes(detail);
                  const summaries=active.map(type=>vbdNativeTriggerCompactCard(detail,type,options)).join('');
                  const action=options.inConfigModal?'':`<div class="inline-actions">${vbdNativeTriggerEditAction(detail)}</div>`;
                  const submitWarning=vbdSingleItemSubmitDisabledWarning(detail);
                  return `<div class="wa-native-trigger-area" data-vbd-native-trigger-area="true" data-vbd-native-trigger-write-api="true" data-vbd-native-trigger-no-raw-json="true" data-vbd-native-trigger-no-manual-selector="true">
                    <p class="readonly-note">7.9 P2 按当前 VBD 已启用的原生触发数据展示摘要；点击编辑可配置六类原生触发源。</p>
                    <div class="wa-native-trigger-grid" data-vbd-native-trigger-summary-selected="true" data-vbd-native-trigger-summary-data-driven="true" data-vbd-native-trigger-inline-full-detail="false">${summaries||`<div class="readonly-note" data-vbd-native-trigger-empty-state="true" data-vbd-native-trigger-compact-empty-state="true">尚未启用原生触发方式。可点击编辑原生触发配置启用。</div>`}</div>
                    ${submitWarning}
                    ${action}
                  </div>`;
                }
                function vbdNativeTriggerConfigSummaryCard(detail){
                  const data=vbdNativeTriggerData(detail), active=activeVbdNativeTriggerTypes(detail).map(labelNativeTriggerType);
                  const state=data.supported===false?(data.unsupportedReason||'不可用'):'数据驱动可编辑摘要';
                  return `<section class="wa-config-card wa-vbd-native-config-card" data-vbd-native-trigger-config-summary="true"><h3>原生触发配置</h3><div class="identity-grid">${row('P2 状态',esc('可编辑'))}${row('展示方式',esc(state))}${row('已启用 / 已配置项',esc(active.join(' / ')||'暂无'))}${row('写入 API',esc(data.writeApiEnabled?'已启用':'不可用'))}</div><p class="muted">原生触发源仅包含红石、BlockState、右键交互、容器打开、容器关闭、容器内容变化。interaction item matcher 是右键之后的条件层。</p><div class="inline-actions">${vbdNativeTriggerEditAction(detail,'ghost')}</div></section>`;
                }
                function vbdNativeTriggerConfigModalSection(detail){
                  if(!isVirtualBlockDevice(detail))return '';
                  const draft=appState.vbdNativeTriggerEdit&&sameDeviceRef(appState.vbdNativeTriggerEdit.deviceId,detail.id)?appState.vbdNativeTriggerEdit:null;
                  const body=draft?vbdNativeTriggerEditForm(detail,draft,true):`${vbdNativeTriggerOverviewCard(detail,{inConfigModal:true})}<div class="inline-actions">${vbdNativeTriggerEditAction(detail,'ghost')}</div>`;
                  return `<section class="wa-edit-section" data-edit-section="vbd-native-triggers" data-vbd-native-trigger-config-modal-section="true"><header><h3>原生触发配置</h3><span class="pill ok">7.9 P2 / 7.11</span></header>${body}<p class="muted">保存只写入 VBD 原生触发字段；统一 itemSubmit requirement 编辑器从右键交互条件层进入。不会创建新 consume 策略、ConditionEngine、路径图，也不会清空 7.8 matcher 或容器 itemConditions。</p></section>`;
                }
                function vbdNativeTriggerCompactCard(detail,type,options={}){
                  const trigger=vbdNativeTriggerMap(detail)[type]||{}, clickable=!options.inConfigModal;
                  const title=labelNativeTriggerType(type), primary=vbdNativeTriggerPrimarySummary(type,trigger), recent=vbdNativeTriggerRecentSummary(type,trigger), note=vbdNativeTriggerShortSummary(type,trigger);
                  const clickAttrs=clickable?`role="button" tabindex="0" ${htmlHandler(`openVbdNativeTriggerReadonlyModal(${jsString(detail.id)},${jsString(type)})`)} onkeydown="if(event.key==='Enter'||event.key===' '){event.preventDefault();openVbdNativeTriggerReadonlyModal(${esc(jsString(detail.id))},${esc(jsString(type))});}" data-vbd-native-trigger-card-click="readonly-detail" data-vbd-native-trigger-open-readonly-detail="true"`:'role="button" tabindex="-1" aria-disabled="true" data-vbd-native-trigger-card-click="readonly-detail-disabled"';
                  const itemSubmitGate=type==='right_click'?`<span class="wa-native-trigger-compact-line"><small>itemSubmit gate</small>${conditionGateRecentStatusInline(trigger.itemSubmitRecentConditionGate)}</span>`:'';
                  return `<article class="wa-native-trigger-compact-card" ${clickAttrs} data-vbd-native-trigger-compact-card="true" data-vbd-native-trigger-card-summary="true" data-vbd-native-trigger-card-type="${esc(type)}" data-vbd-native-trigger-summary-selected="true" data-vbd-native-trigger-summary-active="true"><span class="wa-native-trigger-compact-head"><strong>${esc(title)}</strong><span class="pill ok">已启用 / 已配置</span></span><span class="wa-native-trigger-compact-line"><small>主项</small><b>${primary}</b></span><span class="wa-native-trigger-compact-line"><small>最近</small><b>${recent}</b></span>${conditionGateRecentStatusInline(trigger.recentConditionGate)}${itemSubmitGate}<span class="muted">${note}</span></article>`;
                }
                function vbdNativeTriggerPrimarySummary(type,t){
                  if(type==='redstone_powered')return esc(labelChannel(t.channel)||t.modeDisplayName||t.mode||'redstone');
                  if(type==='blockstate')return esc(t.conditionBlockId||Object.entries(t.conditionProperties||{}).map(([k,v])=>`${k}=${v}`).join(', ')||'BlockState 条件');
                  if(type==='right_click')return esc(labelChannel(t.interactChannel)||'右键交互启用');
                  if(type==='container_open')return esc(labelChannel(t.containerOpenChannel)||'容器打开');
                  if(type==='container_close')return esc(labelChannel(t.containerCloseChannel)||'容器关闭');
                  if(type==='container_change')return esc(labelChannel(t.containerChangeChannel)||'容器内容变化');
                  return esc('未知');
                }
                function vbdNativeTriggerRecentSummary(type,t){
                  if(type==='redstone_powered')return esc(t.lastTriggerResult||`${labelRuntimeBool(t.lastPowered)} · ${t.lastPowerLevel??0}`);
                  if(type==='blockstate')return esc(t.lastConditionResult||labelRuntimeBool(t.lastConditionMatched));
                  if(type==='right_click')return esc(t.lastInteractionResult||t.lastInteractionPlayerName||'暂无');
                  if(type==='container_open')return esc(t.lastContainerResult||nativeTriggerTime(t.lastContainerOpenWallTimeMillis));
                  if(type==='container_close')return esc(t.lastContainerResult||nativeTriggerTime(t.lastContainerCloseWallTimeMillis));
                  if(type==='container_change')return esc(t.lastContainerResult||nativeTriggerTime(t.lastContainerChangeWallTimeMillis));
                  return esc('暂无');
                }
                function vbdNativeTriggerShortSummary(type,t){
                  if(type==='redstone_powered')return esc(t.offChannel?`断电频道：${t.offChannel}`:'红石 / 受电状态触发');
                  if(type==='blockstate')return esc(`${t.supportedPropertyCount??(t.supportedProperties||[]).length} 个当前方块属性可读`);
                  if(type==='right_click'){const matcher=t.interactionItemMatcherLayer||{}, submit=t.itemSubmitLayer||{};const parts=[];if(matcher.enabled||matcher.configured)parts.push('交互物品匹配');if(submit.enabled||submit.configured||Number(submit.requirementCount||0)>0)parts.push('itemSubmit');return esc(parts.length?`含${parts.join(' / ')}条件层`:'玩家右键交互触发');}
                  if(type==='container_change')return esc(`物品条件数：${t.itemConditionCount??(t.itemConditions||[]).length}`);
                  if(type==='container_open'||type==='container_close')return esc('容器 open / close 共用 containerEnabled');
                  return esc('');
                }
                function openVbdNativeTriggerReadonlyModal(deviceId,type){
                  const detail=appState.currentDeviceDetail&&sameDeviceRef(appState.currentDeviceDetail.id,deviceId)?appState.currentDeviceDetail:null;
                  if(!detail){toast('当前设备详情已变化，请刷新后重试。');return;}
                  const title=`${labelNativeTriggerType(type)} · 原生触发详情`;
                  const body=`<section class="edit-form" data-vbd-native-trigger-readonly-modal="true" data-vbd-native-trigger-readonly-detail="true" data-vbd-native-trigger-detail-modal-body="true" data-vbd-native-trigger-detail-type="${esc(type)}" data-vbd-native-trigger-readonly-no-save="true" data-vbd-native-trigger-readonly-no-edit-lock="true" data-vbd-native-trigger-detail-no-dirty-guard="true" data-vbd-native-trigger-detail-no-write-request="true">${vbdNativeTriggerReadonlyDetail(detail,type)}<p class="muted">该弹窗只读展示，不获取编辑锁、不发送写请求；保存请使用“编辑原生触发配置”。</p></section>`;
                  openWebAdminModal(title,body,waButton('关闭','close','onclick="closeWebAdminModal()"','ghost'),{className:'wa-config-modal'});
                }
                function vbdNativeTriggerReadonlyDetail(detail,type){
                  const trigger=vbdNativeTriggerMap(detail)[type]||{};
                  if(type==='redstone_powered')return vbdRedstoneSummary(trigger);
                  if(type==='blockstate')return vbdBlockStateSummary(detail,trigger);
                  if(type==='right_click')return vbdInteractionTriggerSummary(detail,trigger);
                  if(type==='container_open')return vbdContainerOpenSummary(trigger);
                  if(type==='container_close')return vbdContainerCloseSummary(trigger);
                  if(type==='container_change')return vbdContainerChangeSummary(trigger);
                  return empty('未知触发方式。');
                }
                function vbdRedstoneSummary(t){
                  return `<div class="identity-grid">${row('模式',esc(t.modeDisplayName||t.mode||'redstone_rising'))}${row('通电频道',channelCell(t.channel))}${row('断电频道',channelCell(t.offChannel))}${row('当前通电',esc(labelRuntimeBool(t.currentPowered)))}${row('当前红石强度',esc(t.currentPowerLevel??'未知'))}${row('BlockState powered',esc(labelRuntimeBool(t.blockStatePowered)))}${row('上次通电',esc(labelRuntimeBool(t.lastPowered)))}${row('上次红石强度',esc(t.lastPowerLevel??0))}${row('最近结果',esc(t.lastTriggerResult||'暂无'))}</div>`;
                }
                function vbdBlockStateSummary(detail,t){
                  const props=t.supportedProperties||[], conditionProps=t.conditionProperties||{};
                  const configured=Object.entries(conditionProps).map(([k,v])=>`${k}=${v}`).join(', ')||t.conditionRaw||'未设置';
                  return `<div class="identity-grid">${row('已启用',esc(labelBool(!!t.conditionEnabled)))}${row('触发频道 / 主频道',channelCell(t.channel))}${row('退出 / 不满足频道',channelCell(t.offChannel))}${row('条件方块',esc(t.conditionBlockId||'未设置'))}${row('条件属性',esc(configured))}${row('条件模式',esc(t.conditionModeDisplayName||t.conditionMode||'condition_enter'))}${row('当前匹配',esc(labelRuntimeBool(t.currentMatched)))}${row('上次满足',esc(labelRuntimeBool(t.lastConditionMatched)))}${row('最近结果',esc(t.lastConditionResult||'暂无'))}${row('运行状态',esc(nativeBlockRuntimeStatus(detail.nativeTriggers?.boundBlock?.status||t.runtimeState)))}${row('支持属性数',esc(t.supportedPropertyCount??props.length))}</div><p class="muted">BlockState 条件使用 VBD 主 channel；condition_exit / condition_both 的退出边沿使用 offChannel，未设置时回退主 channel。保存配置不会立即触发，需要绑定方块状态变化。</p>${vbdBlockStatePropertyList(props,t.validationIssues||[])}`;
                }
                function vbdInteractionTriggerSummary(detail,t){
                  const matcher=t.interactionItemMatcherLayer||{};
                  const submit=t.itemSubmitLayer||{};
                  const configured=!!(matcher.enabled||matcher.configured);
                  const inactiveConfigured=configured&&!t.interactionEnabled;
                  const matcherState=configured?(inactiveConfigured?'已配置，但右键交互触发尚未启用，当前不生效':(matcher.summary||matcher.templateItemId||'已配置')):'未配置';
                  const matcherDetail=configured?`${matcher.templateItemId||matcher.summary||'已配置'}${matcher.countMode?` · ${labelCountMode(matcher.countMode)} ${matcher.requiredCount||1}`:''}${matcher.source||matcher.interactionItemSource?` · ${labelInteractionSource(matcher.source||matcher.interactionItemSource)}`:''}`:'尚未要求特定交互物品';
                  const warning=inactiveConfigured?`<p class="readonly-note warning" data-vbd-native-trigger-matcher-disabled-warning="true">已配置交互物品匹配，但右键交互触发尚未启用；matcher 是右键触发后的条件层，当前不会参与触发判定。</p>`:'';
                  const submitHtml=singleItemSubmitInlineSummary(detail,t,submit);
                """)
.append("""
                  return `<div class="identity-grid" data-vbd-native-trigger-interaction-matcher-summary="true">${row('交互启用',esc(labelBool(!!t.interactionEnabled)))}${row('交互频道',channelCell(t.interactChannel))}${row('冷却时间',esc(formatTicks(t.interactionCooldownTicks)||'0 tick'))}${row('最近玩家',esc(t.lastInteractionPlayerName||'暂无'))}${row('最近手 / 面',esc([t.lastInteractionHand,t.lastInteractionSide].filter(Boolean).join(' / ')||'暂无'))}${row('最近结果',esc(t.lastInteractionResult||'暂无'))}${row('matcher 条件层',esc(matcherState))}${row('matcher 摘要',esc(matcherDetail))}${row('原版交互策略',esc(submit.vanillaPolicyDisplayName||matcher.vanillaPolicyDisplayName||matcher.interactionItemVanillaPolicy||'沿用当前策略'))}</div>${warning}<div class="inline-actions wa-native-trigger-inline-actions" data-vbd-native-trigger-interaction-matcher-entry="true">${interactionItemMatcherInlineAction(detail)}</div>${submitHtml}<p class="muted">交互物品匹配和 itemSubmit requirements 都属于右键交互之后的条件 / 提交层，不是新的原生触发源。</p>`;
                }
                function interactionItemMatcherInlineAction(detail){
                  if(!isVirtualBlockDevice(detail))return '';
                  const data=detail.interactionItemMatcher||{}, locked=lockHeldByOther(data.lockStatus);
                  const canEdit=canEditInteractionItemMatcher()&&data.matcherEditable!==false;
                  if(locked)return waButton('只读查看交互物品匹配','virtual-block-device',htmlHandler(`openInteractionItemMatcherReadonlyModal(${jsString(detail.id)})`),'ghost');
                  return waButton(canEdit?'编辑交互物品匹配':'查看交互物品匹配','virtual-block-device',htmlHandler(`openInteractionItemMatcherModal(${jsString(detail.id)})`),canEdit?'primary':'ghost');
                }
                function singleItemSubmitInlineSummary(detail,t,submit={}){
                  const configured=!!(submit.configured||submit.enabled||Number(submit.requirementCount||0)>0);
                  const interactionEnabled=!!t.interactionEnabled;
                  if(!interactionEnabled&&configured)return `<div class="readonly-note warning" data-single-item-submit-disabled-warning="true" data-single-item-submit-hidden-when-interaction-disabled="true" data-unified-item-submit-disabled-warning="true">已配置 itemSubmit requirements，但右键交互触发尚未启用；itemSubmit 是右键交互之后的提交层，当前不会显示完整编辑入口。</div>`;
                  if(!interactionEnabled)return '<div data-single-item-submit-hidden-when-interaction-disabled="true"></div>';
                  const count=Number(submit.requirementCount||0), enabled=Number(submit.enabledRequirementCount||0);
                  const requirements=Array.isArray(submit.requirements)?submit.requirements:[];
                  const mode=count<=0?'未配置':(count===1?'单物品提交':`多物品提交，${count} 个条件`);
                  const first=requirements[0]||submit.requirement||{};
                  const detailLine=count<=0?'尚未配置 itemSubmit requirements':(count===1?`${mode} · ${(first.templateItemId||first.summary||submit.templateItemId||'提交物品')} · ${esc(labelCountMode(first.countMode||submit.countMode||'at_least'))} ${esc(first.requiredCount||first.count||submit.requiredCount||submit.count||1)} · consumeCount ${esc(first.consumeCount||submit.consumeCount||1)}`:`${mode} · 启用 ${enabled}/${count} · ${(first.templateItemId||first.summary||submit.templateItemId||'提交物品')}`);
                  const disabled=!canEditSingleItemSubmit();
                  const title=!canEditSingleItemSubmit()?'需要 EDITOR 或 OWNER 权限才能编辑 itemSubmit requirements。':'';
                  const action=t.singleItemSubmitUnified?'single-item-submit-open-unified':'single-item-submit-open';
                  const nativeDirty=appState.vbdNativeTriggerEdit&&sameDeviceRef(appState.vbdNativeTriggerEdit.deviceId,detail.id)&&vbdNativeTriggerDirty(appState.vbdNativeTriggerEdit);
                  const finalDisabled=disabled||nativeDirty;
                  const finalTitle=nativeDirty?'请先保存或放弃当前原生触发配置草稿，再启动 itemSubmit requirement 编辑。':title;
                  const buttonText=count<=0?'添加提交条件':(count===1?'编辑 / 添加另一个条件':'编辑多物品条件');
                  const button=waButton(buttonText,'chest',singleItemSubmitActionAttrs(action,detail.id,finalDisabled,finalTitle),finalDisabled?'ghost':'primary');
                  const consumeLine=submit.itemSubmitConsumeEnabled?`提交后消耗 · ${esc(submit.itemSubmitConsumeOrderDisplayName||submit.itemSubmitConsumeOrder||'hotbar_first')}`:'提交后不消耗';
                  const modeAttr=count<=0?'data-zero-requirement-add-only="true"':(count===1?'data-single-requirement-simplified="true"':'data-multi-requirement-summary="true" data-multi-requirement-controls-visible="true"');
                  return `<div class="readonly-note" data-single-item-submit-under-right-click="true" data-unified-item-submit-editor="true" data-unified-item-submit-summary="true" data-item-submit-requirement-list="true" data-unified-requirement-list-only="true" data-item-submit-adaptive-zero-one-many="true" ${modeAttr} data-old-multi-requirement-readonly-refusal-removed="true" data-single-item-submit-card="true" data-single-item-submit-advanced-editable="true" data-single-item-submit-consume-editor="true"><strong>itemSubmit 条件编辑器</strong><div>${esc(detailLine)}</div><small>${esc(consumeLine)}；0 / 1 / N requirements 分别表现为未配置、单物品提交、多物品提交；单项模式不显示排序、删除、批量管理按钮。</small><div class="inline-actions">${button}</div></div>`;
                }
                function vbdContainerOpenSummary(t){
                  return `<div class="identity-grid">${row('容器总开关',esc(labelBool(!!t.containerEnabled)))}${row('打开频道',channelCell(t.containerOpenChannel))}${row('容器冷却',esc(formatTicks(t.containerCooldownTicks)||'0 tick'))}${row('最近玩家',esc(t.lastContainerPlayerName||'暂无'))}${row('最近打开时间',esc(nativeTriggerTime(t.lastContainerOpenWallTimeMillis)))}${row('最近事件类型',esc(t.lastContainerEventType||'暂无'))}${row('最近结果',esc(t.lastContainerResult||'暂无'))}</div><p class="muted">容器打开、关闭和内容变化共用 containerEnabled。</p>`;
                }
                function vbdContainerCloseSummary(t){
                  return `<div class="identity-grid">${row('容器总开关',esc(labelBool(!!t.containerEnabled)))}${row('关闭频道',channelCell(t.containerCloseChannel))}${row('容器冷却',esc(formatTicks(t.containerCooldownTicks)||'0 tick'))}${row('最近玩家',esc(t.lastContainerPlayerName||'暂无'))}${row('最近关闭时间',esc(nativeTriggerTime(t.lastContainerCloseWallTimeMillis)))}${row('最近事件类型',esc(t.lastContainerEventType||'暂无'))}${row('最近结果',esc(t.lastContainerResult||'暂无'))}</div><p class="muted">容器打开、关闭和内容变化共用 containerEnabled。</p>`;
                }
                function vbdContainerChangeSummary(t){
                  const conditions=t.itemConditions||[];
                  const itemSummary=conditions.length?conditions.slice(0,3).map(c=>`${c.name||c.id||'条件'}: ${c.itemId||c.type||'模板'} · ${containerTemplateConditionChannelText(c)}`).join('；'):'未配置';
                  const deviceId=t.deviceId||appState.currentDeviceDetail?.id||'';
                  const entry=deviceId?`<div class="inline-actions" data-container-template-session-entry="detail" data-container-template-p3b-entry="true">${waButton('编辑容器变化模板','selection',containerTemplateActionAttrs('container-template-open',deviceId),'ghost')}</div>`:'';
                  return `<div class="identity-grid">${row('容器总开关',esc(labelBool(!!t.containerEnabled)))}${row('内容变化频道',channelCell(t.containerChangeChannel))}${row('容器冷却',esc(formatTicks(t.containerCooldownTicks)||'0 tick'))}${row('检查间隔',esc(formatTicks(t.containerChangeCheckIntervalTicks)||'0 tick'))}${row('物品条件数',esc(t.itemConditionCount??conditions.length))}${row('条件摘要',esc(itemSummary))}${row('最近指纹',esc(t.lastContainerFingerprint||'暂无'))}${row('最近变化时间',esc(nativeTriggerTime(t.lastContainerChangeWallTimeMillis)))}${row('最近结果',esc(t.lastContainerResult||'暂无'))}</div>${entry}<p class="muted">7.9 P3b 可从 WebAdmin 发起游戏内容器变化模板 GUI，左键复制 ghost 模板、右键清空、滚轮调整数量，点击保存才写入 itemConditions。</p>`;
                }
                function containerTemplateConditionChannelText(c){
                  const channel=c?.effectiveChannel||c?.channel||'';
                  if(!channel)return '触发频道：未设置';
                  return c?.inheritsContainerChangeChannel||c?.effectiveChannelSource==='container_change_channel'?`触发频道：继承容器内容变化频道 ${channel}`:`触发频道：${channel}`;
                }
                function vbdBlockStatePropertyList(properties,issues){
                  const issueHtml=(issues||[]).length?`<div class="readonly-note danger">${(issues||[]).map(esc).join('<br>')}</div>`:'';
                  if(!properties||properties.length===0)return `<div class="readonly-note" data-vbd-native-blockstate-properties-from-bound-block="true" data-vbd-native-blockstate-allowed-values="true">当前绑定方块没有可展示的 BlockState 属性，或世界 / 区块不可用。</div>${issueHtml}`;
                  const rows=properties.map(prop=>`<div class="wa-native-property" data-blockstate-property="${esc(prop.name||'')}" data-blockstate-kind="${esc(prop.kind||'value')}"><strong>${esc(prop.name||'unknown')}</strong><span>当前：${esc(prop.currentValue||'')}</span><small>可选值：${esc((prop.allowedValues||[]).join(' / ')||'无')}</small>${prop.selectedInCondition?`<small>目标：${esc(prop.targetValue||'')} ${prop.targetMatched?'（已匹配）':'（未匹配）'}</small>`:''}</div>`).join('');
                  return `<div class="wa-native-property-list" data-vbd-native-blockstate-properties-from-bound-block="true" data-vbd-native-blockstate-allowed-values="true">${rows}</div>${issueHtml}`;
                }
                function nativeBlockRuntimeStatus(value){return {ready:'ready / 当前方块可读取',world_unavailable:'世界不可用',chunk_unloaded:'区块未加载',air:'当前位置为空气',block_mismatch:'当前方块与绑定方块不一致'}[String(value||'')]||value||'未知';}
                function nativeTriggerTime(value){const n=Number(value||0);return n>0?formatDateTime(n):'暂无';}
                function labelCountMode(value){return {ignore:'不检查数量',at_least:'至少',exactly:'等于',at_most:'至多'}[String(value||'').toLowerCase()]||value||'至少';}
                """)
.append("""
                function nativeTriggerEditableValuesFrom(data={}){
                  const map=data.triggers||{}, red=map.redstone_powered||{}, block=map.blockstate||{}, interaction=map.right_click||{}, open=map.container_open||{}, close=map.container_close||{}, change=map.container_change||{};
                  const conditionRows=Object.entries(block.conditionProperties||{}).map(([property,value])=>({property,value}));
                  return {
                    redstoneEnabled:!!red.configured&&red.mode!=='redstone_disabled',
                    redstoneMode:red.mode==='redstone_disabled'?'redstone_rising':(red.mode||'redstone_rising'),
                    channel:red.channel||'',
                    offChannel:red.offChannel||'',
                    redstoneConditionGroupId:red.conditionGroupId||'',
                    blockStateEnabled:!!block.conditionEnabled,
                    conditionMode:block.conditionMode||'condition_enter',
                    conditionRows,
                    blockStateConditionGroupId:block.conditionGroupId||'',
                    interactionEnabled:!!interaction.interactionEnabled,
                    interactChannel:interaction.interactChannel||'',
                    interactionCooldownTicks:Number(interaction.interactionCooldownTicks||0),
                    interactionConditionGroupId:interaction.conditionGroupId||'',
                    itemSubmitConditionGroupId:interaction.itemSubmitConditionGroupId||'',
                    containerOpenEnabled:!!open.enabled,
                    containerOpenChannel:open.containerOpenChannel||'',
                    containerOpenConditionGroupId:open.conditionGroupId||'',
                    containerCloseEnabled:!!close.enabled,
                    containerCloseChannel:close.containerCloseChannel||'',
                    containerCloseConditionGroupId:close.conditionGroupId||'',
                    containerChangeEnabled:!!change.enabled,
                    containerChangeChannel:change.containerChangeChannel||'',
                    containerChangeConditionGroupId:change.conditionGroupId||'',
                    containerCooldownTicks:Number(change.containerCooldownTicks??open.containerCooldownTicks??close.containerCooldownTicks??0),
                    containerChangeCheckIntervalTicks:Number(change.containerChangeCheckIntervalTicks||10)
                  };
                }
                function conditionGateTargetTypes(){return {redstoneConditionGroupId:'VBD_REDSTONE',blockStateConditionGroupId:'VBD_BLOCKSTATE',interactionConditionGroupId:'VBD_INTERACTION',itemSubmitConditionGroupId:'ITEM_SUBMIT',containerOpenConditionGroupId:'CONTAINER_OPEN',containerCloseConditionGroupId:'CONTAINER_CLOSE',containerChangeConditionGroupId:'CONTAINER_CHANGE'};}
                async function loadConditionGateOptions(deviceId){const targets=Object.values(conditionGateTargetTypes());const pairs=await Promise.all(targets.map(async targetType=>{try{return [targetType,await api(`/api/webadmin/condition-groups/available?targetType=${encodeURIComponent(targetType)}&targetId=${encodeURIComponent(deviceId||'')}`)];}catch(err){return [targetType,{groups:[],error:err.message||'条件组候选加载失败'}];}}));return Object.fromEntries(pairs);}
                function conditionGatePickerMeta(field,targetType){const map={interactionConditionGroupId:['触发条件组 gate','失败时阻断旧右键交互逻辑。'],itemSubmitConditionGroupId:['itemSubmit gate','失败时阻断提交 / consume。'],containerOpenConditionGroupId:['容器打开 gate','Inventory 目标可用容器快照。'],containerCloseConditionGroupId:['容器关闭 gate','Inventory 目标可用容器快照。']};const meta=map[field]||['运行时 gate','失败时阻断旧逻辑。'];return {label:meta[0],help:'未配置条件组 = 保持旧逻辑，不拦截',title:`未配置条件组 = 保持旧逻辑，不拦截。${meta[1]} 只列出兼容条件组；后端保存时仍会二次拒绝不兼容绑定。`};}
                function conditionGatePicker(draft,field,targetType,disabled){const options=(draft.conditionGateOptions||{})[targetType]||{}, groups=Array.isArray(options.groups)?options.groups:[], current=(draft.values||{})[field]||'', safeId=`vbdnt-${field.replace(/[^a-zA-Z0-9_-]/g,'-')}`, compatible=!current||groups.some(g=>g.id===current), incompatibleCurrent=current&&!compatible?current:'', meta=conditionGatePickerMeta(field,targetType);const clearButton=incompatibleCurrent&&!disabled?`<button class="link-button" type="button" data-condition-runtime-clear-incompatible="true" ${htmlHandler(`clearVbdNativeConditionGate(${jsString(draft.deviceId)},${jsString(field)})`)}>清空条件组</button>`:'';const currentWarning=incompatibleCurrent?`<span class="condition-field-help danger" data-condition-runtime-incompatible-current="${esc(incompatibleCurrent)}">当前配置不兼容：${esc(incompatibleCurrent)}。保存会被拒绝。</span>${clearButton}`:'';const empty=groups.length?'':'<option value="" disabled>暂无适用于此触发方式的条件组</option>';const serverMessage=options.message?`<span class="condition-field-help danger">${esc(options.message)}</span>`:'';const error=options.error?`<span class="condition-field-help danger">候选加载失败，可清空为未配置。</span>`:'';return `<label class="condition-gate-picker" data-condition-runtime-gate-picker="true" data-condition-runtime-available-list="true" data-condition-runtime-target-type="${esc(targetType)}" data-condition-runtime-field="${esc(field)}" title="${esc(meta.title)}"><span>${esc(meta.label)}</span><select id="${esc(safeId)}" class="select" ${disabled?'disabled':''} data-condition-runtime-incompatible-current="${esc(incompatibleCurrent)}" ${htmlEvent('onchange',`syncVbdNativeTriggerDraftFromForm(${jsString(draft.deviceId)})`)}><option value="" ${current&&compatible?'':'selected'}>未配置条件组</option>${empty}${groups.map(g=>`<option value="${esc(g.id||'')}" ${current===g.id?'selected':''}>${esc(g.displayName||g.id||'条件组')} · ${esc(g.id||'')}</option>`).join('')}</select>${currentWarning}${serverMessage}${error}</label>`;}
                async function loadRuntimeConditionGateOptions(targetTypes,targetId,extra={}){const targets=[...new Set((targetTypes||[]).filter(Boolean))];const pairs=await Promise.all(targets.map(async targetType=>{try{const params=new URLSearchParams({targetType,targetId:String(targetId||'')});Object.entries(extra||{}).forEach(([k,v])=>{if(v!==undefined&&v!==null&&String(v)!=='')params.set(k,String(v));});return [targetType,await api(`/api/webadmin/condition-groups/available?${params.toString()}`)];}catch(err){return [targetType,{groups:[],error:err.message||'条件组候选加载失败'}];}}));return Object.fromEntries(pairs);}
                function runtimeConditionGatePicker(draft,field,targetType,targetId,disabled,onChange){const options=(draft.conditionGateOptions||{})[targetType]||{}, groups=Array.isArray(options.groups)?options.groups:[], current=String(draft[field]||''), safeId=`runtime-condition-${field.replace(/[^a-zA-Z0-9_-]/g,'-')}`, compatible=!current||groups.some(g=>g.id===current), incompatibleCurrent=current&&!compatible?current:'', labels={conditionGroupId:'外层条件组',enterConditionGroupId:'进入动作条件组',exitConditionGroupId:'离开动作条件组',stayConditionGroupId:'停留动作条件组'}, changeAttr=onChange?htmlEvent('onchange',onChange):'';const currentOption=incompatibleCurrent?`<option value="${esc(incompatibleCurrent)}" selected>当前不兼容：${esc(incompatibleCurrent)}</option>`:'';const empty=groups.length?'':'<option value="" disabled>暂无适用于此触发方式的条件组</option>';const warning=incompatibleCurrent?`<span class="condition-field-help danger" data-condition-runtime-incompatible-current="${esc(incompatibleCurrent)}">当前配置不兼容：${esc(incompatibleCurrent)}。保存会被后端拒绝；可改为“未配置条件组”。</span>`:'';const serverMessage=options.message?`<span class="condition-field-help danger">${esc(options.message)}</span>`:'';const error=options.error?`<span class="condition-field-help danger">候选加载失败；不会清空当前输入。</span>`:'';return `<label class="condition-gate-picker" data-condition-runtime-gate-picker="true" data-condition-runtime-available-list="true" data-condition-runtime-target-type="${esc(targetType)}" data-condition-runtime-target-id="${esc(targetId||'')}" data-condition-runtime-field="${esc(field)}" title="未配置条件组 = 保持旧逻辑，不拦截。只列出兼容条件组；后端保存时仍会二次拒绝不兼容绑定。"><span>${esc(labels[field]||'外层条件组')}</span><select id="${esc(safeId)}" class="select" ${disabled?'disabled':''} data-condition-runtime-incompatible-current="${esc(incompatibleCurrent)}" ${changeAttr}><option value="" ${current?'':'selected'}>未配置条件组</option>${currentOption}${empty}${groups.map(g=>`<option value="${esc(g.id||'')}" ${current===g.id?'selected':''}>${esc(g.displayName||g.id||'条件组')} · ${esc(g.id||'')}</option>`).join('')}</select><span class="condition-field-help">未配置条件组 = 保持旧逻辑，不拦截</span>${warning}${serverMessage}${error}</label>`;}
                function actionConditionGatePicker(draft,action,index,targetType,targetId,ownerMarker,onChange){const options=(draft.conditionGateOptions||{})[targetType]||{}, groups=Array.isArray(options.groups)?options.groups:[], current=String(action?.conditionGroupId||''), safeId=`action-condition-${String(ownerMarker||'action').replace(/[^a-zA-Z0-9_-]/g,'-')}-${index}`, disabled=(!draft.lockId&&draft.mode!=='create')||draft.saving, compatible=!current||groups.some(g=>g.id===current), incompatibleCurrent=current&&!compatible?current:'', changeAttr=onChange?htmlEvent('onchange',onChange):'', ownerAttr=ownerMarker?`${ownerMarker}="true"`:'', currentOption=incompatibleCurrent?`<option value="${esc(incompatibleCurrent)}" selected>当前不兼容：${esc(incompatibleCurrent)}</option>`:'', empty=groups.length?'':'<option value="" disabled>暂无适用于此 action 的条件组</option>', warning=incompatibleCurrent?`<span class="condition-field-help danger" data-action-condition-gate-incompatible-current="${esc(incompatibleCurrent)}">当前配置不兼容：${esc(incompatibleCurrent)}。保存会被后端拒绝；请清空或更换。</span>`:'', clear=incompatibleCurrent&&!disabled?`<button class="link-button" type="button" data-action-condition-gate-clear="true" ${htmlHandler(`document.getElementById(${jsString(safeId)}).value="";${onChange||''}`)}>清空条件组</button>`:'', recent=action?.recentActionConditionGate?.recordId?`<span class="condition-field-help" data-action-condition-gate-summary="true">最近：${esc(conditionGateResultLabel(action.recentActionConditionGate.status))}</span>`:`<span class="condition-field-help" data-action-condition-gate-summary="true">${current?'已配置单条 action gate':'未配置 = 此 action 不单独判断，保持旧执行逻辑'}</span>`, error=options.error?`<span class="condition-field-help danger">候选加载失败；不会清空当前输入。</span>`:'';return `<label class="condition-gate-picker action-condition-gate-picker" data-action-condition-gate-picker="true" ${ownerAttr} data-action-condition-gate-target-type="${esc(targetType)}" data-action-condition-gate-target-id="${esc(targetId||'')}" data-action-index="${esc(index)}"><span>单条 Action 条件组</span><select id="${esc(safeId)}" class="select" ${disabled?'disabled':''} data-action-condition-gate-incompatible-current="${esc(incompatibleCurrent)}" ${changeAttr}><option value="" ${current?'':'selected'}>未配置条件组</option>${currentOption}${empty}${groups.map(g=>`<option value="${esc(g.id||'')}" ${current===g.id?'selected':''}>${esc(g.displayName||g.id||'条件组')} · ${esc(g.id||'')}</option>`).join('')}</select>${recent}${warning}${clear}${error}</label>`;}
                function vbdNativeTriggerEditableJson(draft){const v=draft?.values||{};return JSON.stringify({...v,conditionRows:(v.conditionRows||[]).filter(r=>!isBlank(r.property)||!isBlank(r.value)).map(r=>({property:String(r.property||''),value:String(r.value||'')}))});}
                function vbdNativeTriggerDirty(draft){return !!draft&&vbdNativeTriggerEditableJson(draft)!==String(draft.originalJson||'{}');}
                async function prepareVbdNativeTriggerDraft(deviceId,acquireLock=false){
                  const encoded=encodeURIComponent(deviceApiRef(deviceId)), data=await api(`/api/webadmin/virtual-block-devices/${encoded}/native-triggers`), channelOptions=await loadSignalChannelOptions();
                  const canonicalId=data.deviceId||deviceApiRef(deviceId), values=nativeTriggerEditableValuesFrom(data);
                  const conditionGateOptions=await loadConditionGateOptions(canonicalId);
                  const draft={deviceId:canonicalId,displayName:data.displayName||canonicalId,supported:data.supported!==false,typeSupported:data.typeSupported!==false,unsupportedReason:data.unsupportedReason||'',values,originalJson:JSON.stringify(values),expectedFingerprint:data.expectedFingerprint||'',lockStatus:data.lockStatus||null,lockId:'',lock:null,errors:[],saving:false,conflict:null,data,channelOptions,channelOptionsError:appState.channelOptionsError,conditionGateOptions,channelComboOpen:{},channelComboIndex:{},channelComboQuery:{},channelComboSearchActive:{},initialSnapshot:JSON.stringify(values)};
                  if(acquireLock&&draft.supported&&canEditVbdNativeTriggers()){
                    if(lockHeldByOther(draft.lockStatus)){draft.errors=[{message:lockMessage(draft.lockStatus,'原生触发配置')}];appState.vbdNativeTriggerEdit=draft;return draft;}
                    const result=await acquireWebAdminEditLock('virtual_block_device_triggers',canonicalId);
                    if(result.success){draft.lock=result.data?.lock||{};draft.lockId=draft.lock.lockId||'';scheduleVbdNativeTriggerLockHeartbeat();}
                    else draft.errors=[{message:result.message||'原生触发配置编辑锁获取失败'}];
                  }
                  appState.vbdNativeTriggerEdit=draft;
                  return draft;
                }
                async function startVbdNativeTriggerEdit(deviceId){try{const draft=await prepareVbdNativeTriggerDraft(deviceId,true);showVbdNativeTriggerEditModal(draft.deviceId);if(draft.errors.length)toast(draft.errors[0].message||'无法进入原生触发配置编辑。');}catch(err){toast(err.message||'原生触发配置加载失败');}}
                function showVbdNativeTriggerEditModal(deviceId){
                  const draft=appState.vbdNativeTriggerEdit;if(!draft||!sameDeviceRef(draft.deviceId,deviceId))return;
                  markModalInitialSnapshot('vbd_native_triggers',draft);
                  const footer=draft.lockId?editModalFooter(draft.saving):waButton('关闭','close','onclick="closeWebAdminModal()"','ghost');
                  openWebAdminModal(draft.lockId?'编辑原生触发配置':'查看原生触发配置',vbdNativeTriggerEditForm({id:draft.deviceId,displayName:draft.displayName,nativeTriggers:draft.data},draft,false),footer,{className:'wa-config-modal',onClose:async()=>{await cancelVbdNativeTriggerEdit(draft.deviceId,true);await dismissWebAdminModal();},syncBeforeClose:()=>syncModalDraftBeforeClose('vbd_native_triggers',draft.deviceId),dirtyCheck:()=>!!appState.vbdNativeTriggerEdit?.lockId&&modalDraftDirty('vbd_native_triggers',appState.vbdNativeTriggerEdit)});
                }
                function vbdNativeTriggerEditForm(detail,draft,inline=false){
                  const v=draft.values||{}, disabled=!draft.lockId||draft.saving, data=draft.data||vbdNativeTriggerData(detail), errors=(draft.errors||[]).length?`<ul class="validation-list">${draft.errors.map(e=>`<li>${esc(e.message||e||'保存失败')}</li>`).join('')}</ul>`:'';
                  const conflict=draft.conflict?`<div class="readonly-note danger">原生触发配置已被其他操作修改，请重新加载后再保存。<button class="link-button" type="button" ${htmlHandler(`reloadVbdNativeTriggerAfterConflict(${jsString(draft.deviceId)})`)}>重新加载</button></div>`:'';
                  const lockLine=draft.lockId?`<div class="readonly-note">正在编辑原生触发配置 · 锁到期：${esc(formatDateTime(draft.lock?.expiresAt))}</div>`:(lockHeldByOther(draft.lockStatus)?`<div class="readonly-note danger">${esc(lockMessage(draft.lockStatus,'原生触发配置'))}</div>`:`<div class="readonly-note">当前为只读预览；需要获取编辑锁后才能保存。</div>`);
                  const toggles=`<div class="wa-native-trigger-enable-grid" data-vbd-native-trigger-enable-controls="true">${nativeTriggerTypes().map(item=>{const key=nativeTriggerEnabledKey(item.type), checked=!!v[key];return `<label class="switch-row wa-native-trigger-toggle" data-vbd-native-trigger-toggle="${esc(item.type)}"><span>${esc(item.label)}</span><input id="vbdnt-${esc(item.type)}-enabled" type="checkbox" ${checked?'checked':''} ${disabled?'disabled':''} ${htmlEvent('onchange',`toggleVbdNativeTrigger(${jsString(draft.deviceId)},${jsString(item.type)})`)}></label>`;}).join('')}</div>`;
                  const sections=[
                    v.redstoneEnabled?vbdNativeRedstoneEditSection(draft,disabled):'',
                    v.blockStateEnabled?vbdNativeBlockStateEditSection(draft,disabled):'',
                    v.interactionEnabled?vbdNativeInteractionEditSection(detail,draft,disabled,inline):'',
                    (v.containerOpenEnabled||v.containerCloseEnabled||v.containerChangeEnabled)?vbdNativeContainerCommonEditSection(draft,disabled):'',
                    v.containerOpenEnabled?vbdNativeContainerOpenEditSection(draft,disabled):'',
                    v.containerCloseEnabled?vbdNativeContainerCloseEditSection(draft,disabled):'',
                    v.containerChangeEnabled?vbdNativeContainerChangeEditSection(draft,disabled):''
                  ].filter(Boolean).join('');
                  const interaction=(data.triggers||{}).right_click||{}, submit=interaction.itemSubmitLayer||{};
                  const submitWarning=(!v.interactionEnabled&&(submit.configured||submit.enabled||Number(submit.requirementCount||0)>0))?'<div class="readonly-note warning" data-single-item-submit-disabled-warning="true" data-single-item-submit-hidden-when-interaction-disabled="true" data-unified-item-submit-disabled-warning="true">已配置 itemSubmit requirements，但右键交互触发尚未启用；启用“玩家右键交互”后才会显示 itemSubmit 摘要和编辑入口。</div>':'';
                  const empty=!sections?'<div class="readonly-note" data-vbd-native-trigger-edit-empty-state="true">尚未选择原生触发方式。启用上方任一触发方式后才会显示对应配置字段。</div>':'';
                  const body=`${lockLine}${errors}${conflict}${toggles}<div class="wa-native-trigger-edit-sections" data-vbd-native-trigger-edit-modal="true" data-vbd-native-trigger-patch-api="true" data-vbd-native-trigger-no-raw-json="true" data-vbd-native-trigger-no-template-gui="true" data-vbd-native-trigger-field-preservation="true">${sections||empty}${submitWarning}</div><p class="muted">本表单只编辑红石 / 受电状态、BlockState、右键交互和容器 open / close / change 基础字段。itemSubmit requirement 编辑器在右键交互启用后显示为条件 / 提交层入口；复杂 inventory/equipment、ConditionEngine、路径图或容器物品模板 GUI 不在此表单内编辑。隐藏未启用 section 不会清空 7.8 matcher、itemConditions 或未来保留字段。</p>`;
                  if(inline)return `<div class="wa-native-trigger-inline-editor" data-vbd-native-trigger-inline-edit="true">${body}</div>`;
                  return `<form class="edit-form" ${htmlEvent('onsubmit',`event.preventDefault();saveVbdNativeTrigger(${jsString(draft.deviceId)})`)}>${body}</form>`;
                }
                function nativeTriggerEnabledKey(type){return {redstone_powered:'redstoneEnabled',blockstate:'blockStateEnabled',right_click:'interactionEnabled',container_open:'containerOpenEnabled',container_close:'containerCloseEnabled',container_change:'containerChangeEnabled'}[type]||type;}
                function vbdNativeRedstoneEditSection(draft,disabled){
                  const v=draft.values||{};
                  return `<section class="wa-matcher-option" data-vbd-native-redstone-edit="true"><header><strong>红石 / 受电状态</strong><span class="pill ok" title="包含方块 powered 状态和当前位置红石强度。">currentPowered</span></header><div class="wa-action-editor-grid"><label>触发模式<select id="vbdnt-redstone-mode" class="select" ${disabled?'disabled':''} ${htmlEvent('onchange',`syncVbdNativeTriggerDraftFromForm(${jsString(draft.deviceId)})`)}>${[['redstone_rising','通电时触发'],['redstone_falling','断电时触发'],['redstone_both','通电和断电都触发']].map(([id,label])=>`<option value="${id}" ${v.redstoneMode===id?'selected':''}>${label}</option>`).join('')}</select></label><label>主频道${renderVbdNativeTriggerChannelCombo(draft.deviceId,'channel',v.channel,draft,disabled)}</label><label>断电频道${renderVbdNativeTriggerChannelCombo(draft.deviceId,'offChannel',v.offChannel,draft,disabled)}</label>${conditionGatePicker(draft,'redstoneConditionGroupId','VBD_REDSTONE',disabled)}</div><p id="vbdnt-channel-hint" class="readonly-note">${channelHintHtml(v.channel,draft.channelOptions||appState.channelOptions||[],draft.channelOptionsError||appState.channelOptionsError)}</p></section>`;
                }
                function vbdNativeBlockStateEditSection(draft,disabled){
                  const v=draft.values||{}, props=((draft.data||{}).triggers?.blockstate?.supportedProperties)||[], rows=(v.conditionRows||[]);
                  const rowHtml=rows.length?rows.map((row,index)=>vbdNativeBlockStateConditionRow(draft,index,row,props,disabled)).join(''):'<div class="readonly-note">尚未添加 BlockState 条件行。</div>';
                  return `<section class="wa-matcher-option" data-vbd-native-blockstate-edit="true" data-vbd-native-blockstate-property-dropdown-from-bound-block="true" data-vbd-native-blockstate-allowed-values-from-property="true" data-vbd-native-blockstate-trigger-channel-combo="true" data-vbd-native-blockstate-trigger-channel-shares-main-channel="true" data-vbd-native-blockstate-exit-channel-uses-off-channel="true" data-vbd-native-blockstate-channel-unified-catalog="true" data-vbd-native-blockstate-no-condition-channel="true"><header><strong>BlockState 条件</strong><span class="pill info" title="保存后等待绑定方块状态变化触发。">服务端二次校验</span></header><div class="wa-action-editor-grid"><label>BlockState 触发频道 / 主频道${renderVbdNativeTriggerChannelCombo(draft.deviceId,'channel',v.channel,draft,disabled)}</label><label>退出 / 不满足频道${renderVbdNativeTriggerChannelCombo(draft.deviceId,'offChannel',v.offChannel,draft,disabled)}</label>${conditionGatePicker(draft,'blockStateConditionGroupId','VBD_BLOCKSTATE',disabled)}</div><label>条件模式<select id="vbdnt-condition-mode" class="select" ${disabled?'disabled':''} ${htmlEvent('onchange',`syncVbdNativeTriggerDraftFromForm(${jsString(draft.deviceId)})`)}>${[['condition_enter','进入条件时触发'],['condition_exit','退出条件时触发'],['condition_both','进入和退出都触发']].map(([id,label])=>`<option value="${id}" ${v.conditionMode===id?'selected':''}>${label}</option>`).join('')}</select></label><div class="wa-native-blockstate-rows">${rowHtml}</div><div class="inline-actions"><button class="wa-btn ghost" type="button" ${disabled?'disabled':''} ${htmlHandler(`addVbdNativeBlockStateCondition(${jsString(draft.deviceId)})`)}>${icon('plus')}<span>新增属性条件</span></button></div>${vbdBlockStatePropertyList(props,((draft.data||{}).triggers?.blockstate?.validationIssues)||[])}<p class="muted">属性来自绑定方块 BlockState。</p></section>`;
                }
                function vbdNativeBlockStateConditionRow(draft,index,row,props,disabled){
                  const selected=String(row.property||''), prop=props.find(p=>p.name===selected)||props[0]||{}, values=prop.allowedValues||[], currentValue=values.includes(row.value)?row.value:(row.value||values[0]||'');
                  return `<div class="wa-action-editor-grid" data-vbd-native-blockstate-row="${index}"><label>属性<select id="vbdnt-condition-property-${index}" class="select" ${disabled?'disabled':''} ${htmlEvent('onchange',`changeVbdNativeBlockStateProperty(${jsString(draft.deviceId)},${index})`)}>${props.map(p=>`<option value="${esc(p.name||'')}" ${p.name===selected?'selected':''}>${esc(p.name||'')}</option>`).join('')}</select></label><label>目标值<select id="vbdnt-condition-value-${index}" class="select" ${disabled?'disabled':''} ${htmlEvent('onchange',`syncVbdNativeTriggerDraftFromForm(${jsString(draft.deviceId)})`)}>${values.map(value=>`<option value="${esc(value)}" ${value===currentValue?'selected':''}>${esc(value)}</option>`).join('')}</select></label><button class="wa-btn ghost" type="button" ${disabled?'disabled':''} ${htmlHandler(`removeVbdNativeBlockStateCondition(${jsString(draft.deviceId)},${index})`)}>删除</button></div>`;
                }
                function vbdNativeInteractionEditSection(detail,draft,disabled,inline=false){
                  const v=draft.values||{}, matcher=appState.interactionItemMatcherEdit&&sameDeviceRef(appState.interactionItemMatcherEdit.deviceId,draft.deviceId)?appState.interactionItemMatcherEdit:null;
                  const rightClickTrigger={...((draft.data?.triggers||{}).right_click||{interactionItemMatcherLayer:{}}),singleItemSubmitUnified:inline};
                  const summary=vbdInteractionTriggerSummary({id:draft.deviceId,nativeTriggers:draft.data,interactionItemMatcher:detail.interactionItemMatcher||{}},rightClickTrigger);
                  const matcherInline=inline&&matcher?`<div data-vbd-native-trigger-interaction-matcher-inline-edit="true">${interactionItemMatcherForm(detail,matcher,true)}</div>`:'';
                  const matcherLock=detail?.interactionItemMatcher?.lockStatus||appState.deviceEditLocks[editLockCacheKey('interaction_item_matcher',draft.deviceId)]||null;
                  const matcherEntry=!inline?'<div class="readonly-note" data-vbd-native-trigger-interaction-matcher-entry="true">交互物品匹配编辑入口保留在 VBD 详情页右键交互摘要和统一设备配置 modal 内；独立原生触发 modal 不嵌套第二个可保存编辑器。</div>':(matcherInline||`<div class="inline-actions" data-vbd-native-trigger-interaction-matcher-entry="true">${lockHeldByOther(matcherLock)?waButton('编辑交互物品匹配','settings',`disabled title="${esc(lockMessage(matcherLock,'交互物品匹配'))}" data-vbd-native-trigger-matcher-lock-disabled="true"`,'ghost is-locked'):waButton('编辑交互物品匹配','settings',disabled?'disabled':htmlHandler(`openInlineInteractionMatcherForVbdNativeTrigger(${jsString(draft.deviceId)})`),'ghost')}</div>`);
                  return `<section class="wa-matcher-option" data-vbd-native-interaction-edit="true" data-vbd-native-trigger-matcher-hidden-when-interaction-disabled="true"><header><strong>玩家右键交互</strong><span class="pill ok" title="交互物品匹配不是原生触发源；itemSubmit gate 在提交前执行。">matcher 条件层</span></header><div class="wa-action-editor-grid"><label>交互频道${renderVbdNativeTriggerChannelCombo(draft.deviceId,'interactChannel',v.interactChannel,draft,disabled)}</label><label>交互冷却 tick<input id="vbdnt-interaction-cooldown" class="input" type="number" min="0" max="72000" value="${esc(v.interactionCooldownTicks)}" ${disabled?'disabled':''} ${htmlEvent('oninput',`syncVbdNativeTriggerDraftFromForm(${jsString(draft.deviceId)})`)}></label>${conditionGatePicker(draft,'interactionConditionGroupId','VBD_INTERACTION',disabled)}${conditionGatePicker(draft,'itemSubmitConditionGroupId','ITEM_SUBMIT',disabled)}</div><div class="readonly-note" data-vbd-native-trigger-matcher-visible-inside-interaction="true">${summary}</div>${matcherEntry}<p class="muted">itemSubmit gate 在提交前执行。</p></section>`;
                }
                function vbdNativeContainerCommonEditSection(draft,disabled){const v=draft.values||{};return `<section class="wa-matcher-option" data-vbd-native-container-common-edit="true"><header><strong>容器公共设置</strong><span class="pill info">open / close / change 共用</span></header><label>容器冷却 tick<input id="vbdnt-container-cooldown" class="input" type="number" min="0" max="72000" value="${esc(v.containerCooldownTicks)}" ${disabled?'disabled':''} ${htmlEvent('oninput',`syncVbdNativeTriggerDraftFromForm(${jsString(draft.deviceId)})`)}></label><p class="muted">启用任一容器事件时 containerEnabled=true；全部关闭时容器事件和只读 itemConditions 都不会触发，但已保存字段会保留。</p></section>`;}
                function vbdNativeContainerOpenEditSection(draft,disabled){const v=draft.values||{};return `<section class="wa-matcher-option" data-vbd-native-container-open-edit="true"><header><strong>容器打开</strong><span class="pill info">共用 containerEnabled</span></header><div class="wa-action-editor-grid"><label>打开频道${renderVbdNativeTriggerChannelCombo(draft.deviceId,'containerOpenChannel',v.containerOpenChannel,draft,disabled)}</label>${conditionGatePicker(draft,'containerOpenConditionGroupId','CONTAINER_OPEN',disabled)}</div></section>`;}
                function vbdNativeContainerCloseEditSection(draft,disabled){const v=draft.values||{};return `<section class="wa-matcher-option" data-vbd-native-container-close-edit="true"><header><strong>容器关闭</strong><span class="pill info">共用 containerEnabled</span></header><div class="wa-action-editor-grid"><label>关闭频道${renderVbdNativeTriggerChannelCombo(draft.deviceId,'containerCloseChannel',v.containerCloseChannel,draft,disabled)}</label>${conditionGatePicker(draft,'containerCloseConditionGroupId','CONTAINER_CLOSE',disabled)}</div></section>`;}
                """)
.append("""
                function vbdNativeContainerChangeEditSection(draft,disabled){const v=draft.values||{}, change=(draft.data?.triggers||{}).container_change||{}, conditions=change.itemConditions||[];const itemSummary=conditions.length?conditions.map(c=>`${c.name||c.id||'条件'}: ${c.itemId||c.type||'模板'}`).join('；'):'暂无物品模板条件';const templateDisabled=disabled||vbdNativeTriggerDirty(draft);const templateTitle=vbdNativeTriggerDirty(draft)?'请先保存或放弃当前原生触发配置草稿，再启动游戏内模板编辑会话。':'当前没有可用编辑锁。';const templateAttrs=containerTemplateActionAttrs('container-template-open-unified',draft.deviceId,templateDisabled,templateDisabled?templateTitle:'');const templateEntry=`<div class="inline-actions" data-container-template-session-entry="unified-config" data-container-template-p3b-entry="true" data-container-template-session-requires-clean-native-draft="true">${waButton('编辑容器变化模板','selection',templateAttrs,'ghost')}</div>`;return `<section class="wa-matcher-option" data-vbd-native-container-change-edit="true"><header><strong>容器内容变化</strong><span class="pill ok" title="模板仍通过游戏内 GUI 编辑。">itemConditions 模板</span></header><div class="wa-action-editor-grid"><label>内容变化频道${renderVbdNativeTriggerChannelCombo(draft.deviceId,'containerChangeChannel',v.containerChangeChannel,draft,disabled)}</label><label>检查间隔 tick<input id="vbdnt-container-check-interval" class="input" type="number" min="1" max="72000" value="${esc(v.containerChangeCheckIntervalTicks)}" ${disabled?'disabled':''} ${htmlEvent('oninput',`syncVbdNativeTriggerDraftFromForm(${jsString(draft.deviceId)})`)}></label>${conditionGatePicker(draft,'containerChangeConditionGroupId','CONTAINER_CHANGE',disabled)}</div><div class="readonly-note" data-vbd-native-container-itemconditions-summary="true" data-vbd-native-container-itemconditions-readonly="true">当前物品条件：${esc(itemSummary)}</div>${templateEntry}<p class="muted">条件组失败时不发出变化频道。</p></section>`;}
                function renderVbdNativeTriggerChannelCombo(deviceId,key,value,draft,disabled){
                  const safeKey=String(key).replace(/[^a-zA-Z0-9_-]/g,'-'), open=(draft.channelComboOpen||{})[key]?' open':'';
                  return `<div id="vbdnt-${safeKey}-combo" class="channel-combo vbd-native-channel-combo${open}" data-vbd-native-channel-combo="${esc(key)}"><div class="channel-combo-control"><input id="vbdnt-${safeKey}" class="input" maxlength="128" value="${esc(value||'')}" ${disabled?'disabled':''} placeholder="选择已有频道或输入新频道" autocomplete="off" role="combobox" aria-expanded="${(draft.channelComboOpen||{})[key]?'true':'false'}" aria-controls="vbdnt-${safeKey}-menu" ${htmlEvent('onfocus',`openVbdNativeTriggerChannelMenu(${jsString(deviceId)},${jsString(key)})`)} ${htmlEvent('oninput',`syncVbdNativeTriggerDraftFromForm(${jsString(deviceId)},${jsString(key)},true)`)} ${htmlEvent('onkeydown',`handleVbdNativeTriggerChannelKey(event,${jsString(deviceId)},${jsString(key)})`)}><button class="channel-combo-toggle" type="button" ${disabled?'disabled':''} ${htmlHandler(`toggleVbdNativeTriggerChannelMenu(${jsString(deviceId)},${jsString(key)})`)} aria-label="显示已有频道">${icon('chevron-down')}</button></div><div id="vbdnt-${safeKey}-menu" class="channel-combo-menu" role="listbox">${vbdNativeTriggerChannelOptionsHtml(deviceId,key,draft)}</div></div>`;
                }
                function vbdNativeTriggerChannelOptionsHtml(deviceId,key,draft){
                  if(draft.channelOptionsError||appState.channelOptionsError)return '<div class="channel-combo-empty">频道候选加载失败，仍可手动输入新的频道名。</div>';
                  const value=(draft.values||{})[key]||'', options=filteredChannelOptions(draft.channelOptions||appState.channelOptions||[],channelComboQuery(draft,key)), current=normalizeChannelName(value).toLowerCase(), active=Math.max(0,Number((draft.channelComboIndex||{})[key]||0));
                  if(options.length===0)return '<div class="channel-combo-empty">没有匹配的已有频道，可直接保存为新频道</div>';
                  return options.map((c,index)=>`<button type="button" class="channel-combo-option ${index===active?'active':''} ${String(c.channel||'').trim().toLowerCase()===current?'selected':''}" role="option" onmousedown="event.preventDefault()" ${htmlHandler(`selectVbdNativeTriggerChannel(${jsString(deviceId)},${jsString(key)},${jsString(c.channel||'')})`)}><strong>${esc(c.channel||'未命名频道')}</strong><span>${esc(channelOptionLabel(c))}</span></button>`).join('');
                }
                function syncVbdNativeTriggerChannelCombo(deviceId,key){const draft=appState.vbdNativeTriggerEdit;if(!draft||!sameDeviceRef(draft.deviceId,deviceId))return;const safeKey=String(key).replace(/[^a-zA-Z0-9_-]/g,'-'), combo=document.getElementById(`vbdnt-${safeKey}-combo`), menu=document.getElementById(`vbdnt-${safeKey}-menu`), input=document.getElementById(`vbdnt-${safeKey}`);if(combo)combo.classList.toggle('open',!!(draft.channelComboOpen||{})[key]);if(input)input.setAttribute('aria-expanded',(draft.channelComboOpen||{})[key]?'true':'false');if(menu)menu.innerHTML=vbdNativeTriggerChannelOptionsHtml(deviceId,key,draft);}
                function openVbdNativeTriggerChannelMenu(deviceId,key){const draft=appState.vbdNativeTriggerEdit;if(!draft||!sameDeviceRef(draft.deviceId,deviceId))return;syncVbdNativeTriggerDraftFromForm(deviceId);closeAllCustomComboboxes();draft.channelComboOpen=draft.channelComboOpen||{};draft.channelComboIndex=draft.channelComboIndex||{};draft.channelComboOpen[key]=true;draft.channelComboIndex[key]=0;resetChannelComboQuery(draft,key);syncVbdNativeTriggerChannelCombo(deviceId,key);}
                function toggleVbdNativeTriggerChannelMenu(deviceId,key){const draft=appState.vbdNativeTriggerEdit;if(!draft||!sameDeviceRef(draft.deviceId,deviceId))return;syncVbdNativeTriggerDraftFromForm(deviceId);draft.channelComboOpen=draft.channelComboOpen||{};const wasOpen=!!draft.channelComboOpen[key];if(wasOpen){draft.channelComboOpen[key]=false;syncVbdNativeTriggerChannelCombo(deviceId,key);return;}closeAllCustomComboboxes();draft.channelComboOpen[key]=true;resetChannelComboQuery(draft,key);syncVbdNativeTriggerChannelCombo(deviceId,key);document.getElementById(`vbdnt-${String(key).replace(/[^a-zA-Z0-9_-]/g,'-')}`)?.focus();}
                function selectVbdNativeTriggerChannel(deviceId,key,channel){const draft=appState.vbdNativeTriggerEdit;if(!draft||!sameDeviceRef(draft.deviceId,deviceId))return;syncVbdNativeTriggerDraftFromForm(deviceId);draft.values[key]=channel||'';draft.channelComboOpen=draft.channelComboOpen||{};draft.channelComboIndex=draft.channelComboIndex||{};draft.channelComboOpen[key]=false;draft.channelComboIndex[key]=0;resetChannelComboQuery(draft,key);const input=document.getElementById(`vbdnt-${String(key).replace(/[^a-zA-Z0-9_-]/g,'-')}`);if(input)input.value=draft.values[key];syncVbdNativeTriggerChannelCombo(deviceId,key);}
                function handleVbdNativeTriggerChannelKey(event,deviceId,key){const draft=appState.vbdNativeTriggerEdit;if(!draft||!sameDeviceRef(draft.deviceId,deviceId))return;const options=filteredChannelOptions(draft.channelOptions||appState.channelOptions||[],channelComboQuery(draft,key));if(event.key==='Escape'){event.preventDefault();event.stopPropagation();draft.channelComboOpen=draft.channelComboOpen||{};draft.channelComboOpen[key]=false;syncVbdNativeTriggerChannelCombo(deviceId,key);return;}if(event.key==='ArrowDown'||event.key==='ArrowUp'){event.preventDefault();draft.channelComboOpen=draft.channelComboOpen||{};draft.channelComboIndex=draft.channelComboIndex||{};draft.channelComboOpen[key]=true;const max=Math.max(0,options.length-1), next=event.key==='ArrowDown'?Number(draft.channelComboIndex[key]||0)+1:Number(draft.channelComboIndex[key]||0)-1;draft.channelComboIndex[key]=Math.min(max,Math.max(0,next));syncVbdNativeTriggerChannelCombo(deviceId,key);return;}if(event.key==='Enter'&&draft.channelComboOpen?.[key]&&options.length>0){event.preventDefault();selectVbdNativeTriggerChannel(deviceId,key,options[Math.min(options.length-1,Number((draft.channelComboIndex||{})[key]||0))].channel);}}
                function syncVbdNativeTriggerDraftFromForm(deviceId,keyToOpen=null,openMenu=false){
                  const draft=appState.vbdNativeTriggerEdit;if(!draft||!sameDeviceRef(draft.deviceId,deviceId))return;const v=draft.values||{};
                  nativeTriggerTypes().forEach(item=>{const key=nativeTriggerEnabledKey(item.type), el=document.getElementById(`vbdnt-${item.type}-enabled`);if(el)v[key]=!!el.checked;});
                  if(document.getElementById('vbdnt-redstone-mode'))v.redstoneMode=document.getElementById('vbdnt-redstone-mode').value;
                  ['channel','offChannel','interactChannel','containerOpenChannel','containerCloseChannel','containerChangeChannel'].forEach(key=>{const el=document.getElementById(`vbdnt-${key.replace(/[^a-zA-Z0-9_-]/g,'-')}`);if(el)v[key]=el.value||'';});
                  Object.keys(conditionGateTargetTypes()).forEach(key=>{const el=document.getElementById(`vbdnt-${key.replace(/[^a-zA-Z0-9_-]/g,'-')}`);if(el){const incompatible=el.dataset.conditionRuntimeIncompatibleCurrent||'';if(el.value)v[key]=el.value;else if(incompatible&&v[key]===incompatible)v[key]=incompatible;else v[key]='';}});
                  if(document.getElementById('vbdnt-condition-mode'))v.conditionMode=document.getElementById('vbdnt-condition-mode').value;
                  if(v.blockStateEnabled)v.conditionRows=(v.conditionRows||[]).map((row,index)=>({property:document.getElementById(`vbdnt-condition-property-${index}`)?.value??row.property??'',value:document.getElementById(`vbdnt-condition-value-${index}`)?.value??row.value??''}));
                  const interactionCooldown=document.getElementById('vbdnt-interaction-cooldown');if(interactionCooldown)v.interactionCooldownTicks=Number(interactionCooldown.value||0);
                  const commonCooldown=document.getElementById('vbdnt-container-cooldown');if(commonCooldown)v.containerCooldownTicks=Number(commonCooldown.value||0);else{const cooldownIds=['vbdnt-container-cooldown-open','vbdnt-container-cooldown-close','vbdnt-container-cooldown-change'];for(const id of cooldownIds){const el=document.getElementById(id);if(el){v.containerCooldownTicks=Number(el.value||0);break;}}}
                  const interval=document.getElementById('vbdnt-container-check-interval');if(interval)v.containerChangeCheckIntervalTicks=Number(interval.value||1);
                  draft.values=v;if(openMenu&&keyToOpen){draft.channelComboOpen=draft.channelComboOpen||{};draft.channelComboIndex=draft.channelComboIndex||{};draft.channelComboOpen[keyToOpen]=true;draft.channelComboIndex[keyToOpen]=0;setChannelComboQuery(draft,v[keyToOpen]||'',keyToOpen);}
                  appState.vbdNativeTriggerEdit=draft;if(keyToOpen)syncVbdNativeTriggerChannelCombo(deviceId,keyToOpen);
                }
                function clearVbdNativeConditionGate(deviceId,field){const draft=appState.vbdNativeTriggerEdit;if(!draft||!sameDeviceRef(draft.deviceId,deviceId)||!draft.values)return;draft.values[field]='';const el=document.getElementById(`vbdnt-${String(field).replace(/[^a-zA-Z0-9_-]/g,'-')}`);if(el){el.dataset.conditionRuntimeIncompatibleCurrent='';el.value='';}rerenderVbdNativeTriggerEditor(deviceId);}
                function rerenderVbdNativeTriggerEditor(deviceId){const draft=appState.vbdNativeTriggerEdit;if(!draft||!sameDeviceRef(draft.deviceId,deviceId))return;withPreservedModalScroll(()=>{syncVbdNativeTriggerDraftFromForm(deviceId);if(appState.deviceConfigEdit&&sameDeviceRef(appState.deviceConfigEdit.deviceId,deviceId)){applyDeviceConfigDraftsFromForm(deviceId);showDeviceConfigEditModal(draft.deviceId);}else showVbdNativeTriggerEditModal(draft.deviceId);});}
                function toggleVbdNativeTrigger(deviceId,type){
                  syncVbdNativeTriggerDraftFromForm(deviceId);
                  const draft=appState.vbdNativeTriggerEdit;
                  if(!draft||!sameDeviceRef(draft.deviceId,deviceId))return;
                  const key=nativeTriggerEnabledKey(type);
                  draft.values[key]=!!document.getElementById(`vbdnt-${type}-enabled`)?.checked;
                  if(type==='redstone_powered'&&draft.values[key]&&(!draft.values.redstoneMode||draft.values.redstoneMode==='redstone_disabled'))draft.values.redstoneMode='redstone_rising';
                  if(type==='blockstate'&&draft.values[key]&&!(draft.values.conditionRows||[]).length){
                    const prop=(draft.data?.triggers?.blockstate?.supportedProperties||[])[0];
                    if(prop)draft.values.conditionRows=[{property:prop.name||'',value:(prop.targetValue||prop.currentValue||(prop.allowedValues||[])[0]||'')}];
                  }
                  if(type==='right_click'&&!draft.values[key]){
                    const matcher=appState.interactionItemMatcherEdit;
                    if(matcher&&sameDeviceRef(matcher.deviceId,deviceId))syncInteractionItemMatcherDraftFromForm(deviceId);
                    draft.matcherDraftHiddenWhenInteractionDisabled=true;
                  }else if(type==='right_click'&&draft.values[key]){
                    draft.matcherDraftHiddenWhenInteractionDisabled=false;
                  }
                  rerenderVbdNativeTriggerEditor(deviceId);
                }
                function changeVbdNativeBlockStateProperty(deviceId,index){syncVbdNativeTriggerDraftFromForm(deviceId);const draft=appState.vbdNativeTriggerEdit;if(!draft)return;const propName=draft.values.conditionRows[index]?.property||'', prop=(draft.data?.triggers?.blockstate?.supportedProperties||[]).find(p=>p.name===propName);if(prop)draft.values.conditionRows[index].value=prop.currentValue||(prop.allowedValues||[])[0]||'';rerenderVbdNativeTriggerEditor(deviceId);}
                function addVbdNativeBlockStateCondition(deviceId){syncVbdNativeTriggerDraftFromForm(deviceId);const draft=appState.vbdNativeTriggerEdit;if(!draft||!draft.lockId)return;const used=new Set((draft.values.conditionRows||[]).map(r=>r.property)), prop=(draft.data?.triggers?.blockstate?.supportedProperties||[]).find(p=>!used.has(p.name))||(draft.data?.triggers?.blockstate?.supportedProperties||[])[0]||{};draft.values.conditionRows=draft.values.conditionRows||[];draft.values.conditionRows.push({property:prop.name||'',value:prop.currentValue||(prop.allowedValues||[])[0]||''});rerenderVbdNativeTriggerEditor(deviceId);}
                function removeVbdNativeBlockStateCondition(deviceId,index){syncVbdNativeTriggerDraftFromForm(deviceId);const draft=appState.vbdNativeTriggerEdit;if(!draft||!draft.lockId)return;draft.values.conditionRows.splice(index,1);rerenderVbdNativeTriggerEditor(deviceId);}
                async function openInlineInteractionMatcherForVbdNativeTrigger(deviceId){try{syncVbdNativeTriggerDraftFromForm(deviceId);const draft=await prepareInteractionItemMatcherDraft(deviceId,canEditInteractionItemMatcher());rerenderVbdNativeTriggerEditor(deviceId);if(draft.errors.length)toast(draft.errors[0].message||'交互物品匹配编辑锁获取失败');}catch(err){toast(err.message||'交互物品匹配加载失败');}}
                function vbdNativeTriggerPatchBody(draft){const v=draft.values||{};return {expectedFingerprint:draft.expectedFingerprint||'',lockId:draft.lockId||'',redstoneEnabled:!!v.redstoneEnabled,redstoneMode:v.redstoneMode||'redstone_rising',channel:v.channel||'',offChannel:v.offChannel||'',redstoneConditionGroupId:v.redstoneConditionGroupId||'',blockStateEnabled:!!v.blockStateEnabled,conditionMode:v.conditionMode||'condition_enter',conditionProperties:(v.conditionRows||[]).filter(r=>!isBlank(r.property)||!isBlank(r.value)).map(r=>({property:r.property||'',value:r.value||''})),blockStateConditionGroupId:v.blockStateConditionGroupId||'',interactionEnabled:!!v.interactionEnabled,interactChannel:v.interactChannel||'',interactionCooldownTicks:Number(v.interactionCooldownTicks||0),interactionConditionGroupId:v.interactionConditionGroupId||'',itemSubmitConditionGroupId:v.itemSubmitConditionGroupId||'',containerOpenEnabled:!!v.containerOpenEnabled,containerOpenChannel:v.containerOpenChannel||'',containerOpenConditionGroupId:v.containerOpenConditionGroupId||'',containerCloseEnabled:!!v.containerCloseEnabled,containerCloseChannel:v.containerCloseChannel||'',containerCloseConditionGroupId:v.containerCloseConditionGroupId||'',containerChangeEnabled:!!v.containerChangeEnabled,containerChangeChannel:v.containerChangeChannel||'',containerChangeConditionGroupId:v.containerChangeConditionGroupId||'',containerCooldownTicks:Number(v.containerCooldownTicks||0),containerChangeCheckIntervalTicks:Number(v.containerChangeCheckIntervalTicks||1)};}
                async function patchVbdNativeTriggerDraft(deviceId,draft){return await api(`/api/webadmin/virtual-block-devices/${encodeURIComponent(deviceId)}/native-triggers`,{method:'PATCH',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify(vbdNativeTriggerPatchBody(draft))});}
                async function saveVbdNativeTrigger(deviceId){const draft=appState.vbdNativeTriggerEdit;if(!draft||!draft.lockId){toast('当前原生触发配置只读。');return;}syncVbdNativeTriggerDraftFromForm(deviceId);draft.saving=true;draft.errors=[];draft.conflict=null;appState.vbdNativeTriggerEdit=draft;rerenderVbdNativeTriggerEditor(deviceId);try{const result=await patchVbdNativeTriggerDraft(deviceId,draft);if(result.success){markChannelOptionsDirty({type:'device_config_changed',payload:{targetType:'virtual_block_device_triggers'}});appState.vbdNativeTriggerEdit=null;stopVbdNativeTriggerLockHeartbeat();appState.modalCloseHandler=null;await dismissWebAdminModal();toast(result.changed?(result.message||'原生触发配置已保存。'):'没有变更。');await refreshCurrentDeviceContext(deviceId);return;}draft.saving=false;draft.errors=result.validationErrors&&result.validationErrors.length?result.validationErrors:[{message:result.message||'保存失败'}];draft.conflict=result.code==='conflict_detected'?result.conflict||{remote:true}:null;if(['edit_lock_expired','edit_lock_conflict','edit_lock_required'].includes(result.code)){draft.lockId='';draft.lock=null;stopVbdNativeTriggerLockHeartbeat();}appState.vbdNativeTriggerEdit=draft;toast(result.message||'保存失败');rerenderVbdNativeTriggerEditor(deviceId);}catch(err){draft.saving=false;draft.errors=[{message:err.message||'保存失败'}];appState.vbdNativeTriggerEdit=draft;toast(err.message||'保存失败');rerenderVbdNativeTriggerEditor(deviceId);}}
                async function reloadVbdNativeTriggerAfterConflict(deviceId){try{await releaseVbdNativeTriggerLock(appState.vbdNativeTriggerEdit,true);await prepareVbdNativeTriggerDraft(deviceId,true);rerenderVbdNativeTriggerEditor(deviceId);}catch(err){toast(err.message||'原生触发配置重新加载失败');}}
                function scheduleVbdNativeTriggerLockHeartbeat(){stopVbdNativeTriggerLockHeartbeat();appState.vbdNativeTriggerLockTimer=setTimeout(async()=>{await heartbeatVbdNativeTriggerLock();if(appState.vbdNativeTriggerEdit?.lockId)scheduleVbdNativeTriggerLockHeartbeat();},30000);}
                function stopVbdNativeTriggerLockHeartbeat(){if(appState.vbdNativeTriggerLockTimer){clearTimeout(appState.vbdNativeTriggerLockTimer);appState.vbdNativeTriggerLockTimer=null;}}
                async function heartbeatVbdNativeTriggerLock(){const draft=appState.vbdNativeTriggerEdit;if(!draft||!draft.lockId)return;try{const result=await api('/api/webadmin/edit-locks/heartbeat',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'virtual_block_device_triggers',targetId:draft.deviceId,lockId:draft.lockId})});if(result.success){draft.lock=result.data?.lock||draft.lock;appState.vbdNativeTriggerEdit=draft;return;}draft.errors=[{message:result.message||'原生触发配置编辑锁续期失败'}];draft.lockId='';draft.lock=null;appState.vbdNativeTriggerEdit=draft;stopVbdNativeTriggerLockHeartbeat();rerenderVbdNativeTriggerEditor(draft.deviceId);}catch(err){draft.errors=[{message:err.message||'原生触发配置编辑锁续期失败'}];draft.lockId='';draft.lock=null;appState.vbdNativeTriggerEdit=draft;stopVbdNativeTriggerLockHeartbeat();rerenderVbdNativeTriggerEditor(draft.deviceId);}}
                async function releaseVbdNativeTriggerLock(draft,silent){if(!draft||!draft.lockId)return;try{await api('/api/webadmin/edit-locks/release',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'virtual_block_device_triggers',targetId:draft.deviceId,lockId:draft.lockId})});}catch(err){if(!silent)toast(err.message||'原生触发配置编辑锁释放失败，将等待自动过期。');}}
                async function cancelVbdNativeTriggerEdit(deviceId,silent=false){const draft=appState.vbdNativeTriggerEdit;if(draft&&sameDeviceRef(draft.deviceId,deviceId))await releaseVbdNativeTriggerLock(draft,silent);if(draft&&sameDeviceRef(draft.deviceId,deviceId)){appState.vbdNativeTriggerEdit=null;stopVbdNativeTriggerLockHeartbeat();}}
                function maybeReleaseVbdNativeTriggerEditForRoute(hash){const draft=appState.vbdNativeTriggerEdit;if(!draft)return;if(String(hash||'').startsWith('#/devices/')){const info=detailRoute(String(hash).substring('#/devices/'.length),'#/devices'), routeType=deviceTypeRefPrefix(info.id);if((!routeType||routeType==='virtual_block_device')&&sameDeviceRef(info.id,draft.deviceId))return;}releaseVbdNativeTriggerLock(draft,true);appState.vbdNativeTriggerEdit=null;stopVbdNativeTriggerLockHeartbeat();}
                function deviceFlowPanel(detail){
                  const cfg=detail.configSummary||{}, item=cfg.interactionItem||{};
                  const nodes=[
                    ['设备',detail.displayName||detail.id,labelType(detail.type),'device'],
                    ['主频道',labelChannel(detail.channel),isBlank(detail.channel)?'未设置':'SignalBridge','active-channel'],
                    ['反馈频道',[item.successChannel,item.failChannel].filter(v=>!isBlank(v)).join(' / ')||'暂无','类型专属配置','signalbridge-main'],
                    ['下游查看','频道详情 / History','只读导航','recent-event']
                  ];
                  return `<div class="wa-flow-chain">${nodes.map((n,index)=>`${index?'<div class="wa-flow-arrow">→</div>':''}<div class="wa-flow-node"><span class="wa-icon-bubble">${icon(n[3])}</span><strong>${esc(n[0])}</strong><span>${esc(n[1])}</span><small>${esc(n[2])}</small></div>`).join('')}</div>`;
                }
                function isDeviceConfigEditing(deviceId){return !!(appState.deviceConfigEdit&&appState.deviceConfigEdit.deviceId===deviceId);}
                function stripEditFormShell(html){const text=String(html||''), start=text.indexOf('>'), end=text.lastIndexOf('</form>');if(text.trim().startsWith('<form')&&start>=0&&end>start)return text.slice(start+1,end);return text;}
                function showDeviceConfigEditModal(deviceId){
                  const session=appState.deviceConfigEdit||{deviceId,saving:false,errors:[]};
                  if(session.deviceId!==deviceId)return;
                  const detail=(appState.currentDeviceDetail&&appState.currentDeviceDetail.id===deviceId)?appState.currentDeviceDetail:{id:deviceId,displayName:deviceId};
                  openWebAdminModal('编辑设备配置',deviceConfigForm(detail,session),editModalFooter(session.saving),{className:'wa-config-modal',onClose:()=>cancelDeviceConfigEdit(deviceId),syncBeforeClose:()=>syncModalDraftBeforeClose('device_config',deviceId),dirtyCheck:()=>isDeviceConfigModalDirty(deviceId)});
                }
                function deviceConfigForm(detail,session){
                  const errors=(session.errors||[]).length?`<ul class="validation-list">${session.errors.map(e=>`<li>${esc(e.message||e||'保存失败')}</li>`).join('')}</ul>`:'';
                  const sections=[];
                  if(appState.deviceMetadataEdit&&appState.deviceMetadataEdit.deviceId===detail.id)sections.push(`<section class="wa-edit-section" data-edit-section="metadata"><header><h3>显示信息</h3><span class="pill info">WebAdmin metadata</span></header>${stripEditFormShell(deviceMetadataForm(detail,appState.deviceMetadataEdit))}</section>`);
                  if(appState.deviceBasicConfigEdit&&appState.deviceBasicConfigEdit.deviceId===detail.id)sections.push(`<section class="wa-edit-section" data-edit-section="basic"><header><h3>基础配置</h3><span class="pill warning">enabled / channel</span></header>${stripEditFormShell(deviceBasicConfigForm(detail,appState.deviceBasicConfigEdit))}</section>`);
                  if(!isVirtualBlockDevice(detail)&&appState.deviceExtendedConfigEdit&&appState.deviceExtendedConfigEdit.deviceId===detail.id)sections.push(`<section class="wa-edit-section" data-edit-section="extended"><header><h3>类型专属配置</h3><span class="pill info">extended config</span></header>${stripEditFormShell(deviceExtendedConfigForm(detail,appState.deviceExtendedConfigEdit))}</section>`);
                  if(isVirtualBlockDevice(detail))sections.push(`<section class="wa-edit-section wa-edit-section-compact-note" data-edit-section="vbd-type-specific-suppressed" data-vbd-type-specific-suppressed="true"><header><h3>VBD 配置归属</h3><span class="pill info">7.9</span></header><p class="muted">VBD 旧类型专属配置已拆分到原生触发配置、右键交互条件层和后续 itemSubmit 阶段；这里不再提供旧 extended config 编辑器。</p></section>`);
                  if(isVirtualBlockDevice(detail))sections.push(vbdNativeTriggerConfigModalSection(detail));
                  if(appState.actionRelayActionsEdit&&appState.actionRelayActionsEdit.deviceId===detail.id)sections.push(`<section class="wa-edit-section" data-edit-section="action-relay-actions" data-action-relay-config-modal-section="true"><header><h3>Action 列表</h3><span class="pill warning">action_relay only</span></header>${actionRelayActionsForm(detail,appState.actionRelayActionsEdit,true)}</section>`);
                  const body=sections.length?sections.join(''):'<div class="readonly-note">当前没有可编辑配置区，可能权限不足或该设备类型不支持编辑。</div>';
                  return `<form class="edit-form wa-unified-config-form" data-unified-device-config="true" onsubmit='event.preventDefault();saveDeviceConfig(${jsString(detail.id)})'>${errors}${body}<p class="muted">保存会按已有安全写链路分别提交有变更的显示信息、基础配置、非 VBD 类型专属配置、VBD 原生触发配置 / 右键交互条件和 action_relay Action 列表；itemSubmit requirements 使用右键交互条件层内的独立游戏内 GUI 会话。不会创建或删除真实方块，也不会创建新 consume 策略、ConditionEngine 或逻辑链图。</p></form>`;
                }
                async function acquireWebAdminEditLock(targetType,targetId){
                  return await api('/api/webadmin/edit-locks/acquire',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType,targetId})});
                }
                """).toString();
    }
}
