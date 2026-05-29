package com.zcpu.tzzmod.webadmin;

final class WebAdminFrontendDeviceSessionScripts {
    private WebAdminFrontendDeviceSessionScripts() {
    }

    static String appJs() {
        return new StringBuilder().append("""
                function containerTemplateSessionId(value){return String(value?.sessionId||value?.sessionRef||value?.id||'');}
                function containerTemplateSessionDefault(deviceId,overview={},players=[]){
                  return {deviceId,overview,players,playersError:appState.onlinePlayerOptionsError,targetPlayerName:(players[0]?.name||''),targetPlayerUuid:(players[0]?.uuid||''),status:'ready',active:false,sessionId:'',lockId:'',lock:null,expectedFingerprint:overview.expectedFingerprint||'',saving:false,errors:[],message:''};
                }
                function containerTemplateActionAttrs(action,deviceId,disabled=false,title=''){
                  const disabledAttr=disabled?' disabled':'', titleAttr=title?` title="${esc(title)}"`:'';
                  return `type="button" data-action="${esc(action)}" data-device-id="${esc(deviceId)}"${disabledAttr}${titleAttr}`;
                }
                function handleContainerTemplateAction(event){
                  const target=event.target, button=target&&target.closest?target.closest('[data-action^="container-template-"]'):null;
                  if(!button||button.disabled)return false;
                  const action=String(button.dataset.action||''), deviceId=button.dataset.deviceId||appState.containerTemplateSession?.deviceId||'';
                  if(!action)return false;
                  event.preventDefault();event.stopPropagation();
                  if(action==='container-template-open')openContainerTemplateSessionModal(deviceId);
                  else if(action==='container-template-open-unified')openContainerTemplateSessionModalFromUnified(deviceId);
                  else if(action==='container-template-start')startContainerTemplateSession(deviceId);
                  else if(action==='container-template-cancel')requestContainerTemplateSessionCancel(deviceId);
                  else if(action==='container-template-close')closeWebAdminModal();
                  else return false;
                  return true;
                }
                async function openContainerTemplateSessionModal(deviceId){
                  try{
                    const overview=await api(`/api/webadmin/virtual-block-devices/${encodeURIComponent(deviceId)}/container-template`);
                    const players=await loadOnlinePlayerOptions(true);
                    appState.containerTemplateSession=containerTemplateSessionDefault(overview.deviceId||deviceId,overview,players||[]);
                    showContainerTemplateSessionModal(overview.deviceId||deviceId);
                  }catch(err){toast(err.message||'容器变化模板会话初始化失败');}
                }
                async function openContainerTemplateSessionModalFromUnified(deviceId){
                  if(isDeviceConfigModalDirty(deviceId)){toast('请先保存或放弃当前设备配置草稿，再启动游戏内模板编辑会话。');return;}
                  await releaseAllDeviceConfigLocks(deviceId,true);
                  appState.deviceConfigEdit=null;
                  await dismissWebAdminModal();
                  await openContainerTemplateSessionModal(deviceId);
                }
                function showContainerTemplateSessionModal(deviceId){
                  const draft=appState.containerTemplateSession;
                  if(!draft||!sameDeviceRef(draft.deviceId,deviceId))return;
                  const startDisabled=draft.saving||lockHeldByOther(draft.overview?.lockStatus);
                  const footer=`<button class="wa-btn ghost" ${containerTemplateActionAttrs('container-template-close',draft.deviceId,!!draft.saving)}>${icon('close')}<span>关闭</span></button>${draft.active?`<button class="wa-btn danger" ${containerTemplateActionAttrs('container-template-cancel',draft.deviceId,!!draft.saving)}>${icon('critical-issue')}<span>${draft.saving?'取消中...':'取消会话'}</span></button>`:`<button class="wa-btn primary" ${containerTemplateActionAttrs('container-template-start',draft.deviceId,startDisabled)}>${icon('selection')}<span>${draft.saving?'启动中...':'打开游戏内 GUI'}</span></button>`}`;
                  openWebAdminModal(draft.logicChainDraftContext?'Logic Chain container 捕获':'容器内容变化模板',containerTemplateSessionBody(draft),footer,{className:'wa-config-modal wa-container-template-modal',onClose:async()=>closeContainerTemplateSessionModal(draft.deviceId)});
                }
                function containerTemplateSessionBody(draft){
                  const overview=draft.overview||{}, lock=overview.lockStatus, playerOptions=draft.players||[];
                  const errors=(draft.errors||[]).length?`<ul class="validation-list">${draft.errors.map(e=>`<li>${esc(e.message||e||'会话操作失败')}</li>`).join('')}</ul>`:'';
                  const status=containerTemplateStatusLine(draft);
                  const lockLine=lockHeldByOther(lock)?`<div class="readonly-note danger">${esc(lockMessage(lock,'容器变化模板'))}</div>`:'';
                  const playerSelect=draft.active?`<div class="readonly-note">目标玩家：${esc(draft.targetPlayerName||'-')} · Session：${esc(shortId(draft.sessionId||''))}</div>`:`<label>目标在线玩家<select id="container-template-player" class="input" ${playerOptions.length?'':'disabled'} onchange="syncContainerTemplateSessionDraftFromForm(${jsString(draft.deviceId)})">${playerOptions.map(p=>`<option value="${esc(p.name||'')}" data-uuid="${esc(p.uuid||'')}" ${String(p.name||'')===String(draft.targetPlayerName||'')?'selected':''}>${esc(p.name||'未命名玩家')}</option>`).join('')}</select><span class="muted">${draft.playersError?'在线玩家加载失败。':'选择要打开游戏内模板 GUI 的在线玩家。'}</span></label>`;
                  const conditions=overview.itemConditions||[];
                  const conditionSummary=conditions.length?conditions.slice(0,8).map(c=>`<span class="pill info">${esc(c.name||c.type||'条件')} · ${esc(c.templateItemId||c.itemId||'模板')} ${esc(c.countMode||'')} · ${esc(containerTemplateConditionChannelText(c))}</span>`).join(''):'<span class="muted">当前没有已保存 itemConditions，GUI 会显示空状态。</span>';
                  const channelWarning=overview.containerChangeChannelMissingWarning?'<div class="readonly-note warning" data-p3b-inherited-channel-warning="true">容器内容变化频道未设置；模板条件可以保存，但不会发出 signal，直到配置 containerChangeChannel 或显式 condition channel。</div>':'';
                  return `<form class="edit-form wa-container-template-session-form" data-container-template-session="p3b" data-container-template-save-itemconditions="true" data-container-template-real-item-safe="true" data-container-template-ghost-editing="true" data-container-template-lock-target="virtual_block_device_container_template" data-container-template-fingerprint="itemConditions-only" onsubmit="event.preventDefault()">${errors}${lockLine}${status}${playerSelect}${channelWarning}<div class="readonly-note"><strong>已保存模板快照</strong><div class="wa-template-condition-pills">${conditionSummary}</div></div><p class="muted">P3b 会在目标玩家客户端打开箱子式模板 GUI。左键复制 ghost 模板、右键清空、滚轮调整数量；点击游戏内“保存模板”才写入 itemConditions，取消不会保存，不会修改世界容器或玩家物品。</p></form>`;
                }
                function syncContainerTemplateSessionDraftFromForm(deviceId){
                  const draft=appState.containerTemplateSession;if(!draft||!sameDeviceRef(draft.deviceId,deviceId))return draft;
                  const select=document.getElementById('container-template-player');
                  if(select){const option=select.options&&select.selectedIndex>=0?select.options[select.selectedIndex]:null;draft.targetPlayerName=select.value||'';draft.targetPlayerUuid=option?.dataset?.uuid||'';}
                  return draft;
                }
                function containerTemplateStatusLine(draft){
                  const status=String(draft.status||'ready');
                  const tone={ready:'info',started:'info',opened:'ok',saved:'ok',completed:'ok',cancelled:'warning',failed:'error',expired:'warning'}[status]||'info';
                  const label={ready:'准备启动',started:'已启动，等待玩家打开 GUI',opened:'GUI 已打开',saved:'模板已保存',completed:'模板已保存',cancelled:'会话已取消',failed:'会话失败',expired:'会话已过期'}[status]||status;
                  return `<div class="wa-selection-status ${tone}" data-container-template-session-status="${esc(status)}"><strong>${esc(label)}</strong><span>${esc(draft.message||'游戏内 ESC / 关闭窗口会取消，不保存。')}</span>${draft.lockId?`<span>锁：${esc(shortId(draft.lockId))} · expectedFingerprint：${esc(shortId(draft.expectedFingerprint||''))}</span>`:''}</div>`;
                }
                async function startContainerTemplateSession(deviceId){
                  const draft=syncContainerTemplateSessionDraftFromForm(deviceId)||appState.containerTemplateSession;
                  if(!draft||draft.saving)return;
                  if(!draft.targetPlayerName){draft.errors=[{message:'请选择在线玩家。'}];appState.containerTemplateSession=draft;showContainerTemplateSessionModal(deviceId);return;}
                  draft.saving=true;draft.errors=[];draft.message='正在获取编辑锁并打开游戏内 GUI...';appState.containerTemplateSession=draft;showContainerTemplateSessionModal(deviceId);
                  try{
                    const lockResult=await acquireWebAdminEditLock('virtual_block_device_container_template',draft.deviceId);
                    if(!lockResult.success){draft.saving=false;draft.errors=writeResultErrors(lockResult,'容器变化模板编辑锁获取失败。');appState.containerTemplateSession=draft;showContainerTemplateSessionModal(deviceId);toast(lockResult.message||'编辑锁获取失败');return;}
                    draft.lock=lockResult.data?.lock||{};draft.lockId=draft.lock.lockId||'';scheduleContainerTemplateSessionHeartbeat();
                    const result=await api(`/api/webadmin/virtual-block-devices/${encodeURIComponent(draft.deviceId)}/container-template-session/start`,{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({...logicChainVbdTemplateSessionContextPayload(draft.logicChainDraftContext),deviceId:draft.deviceId,targetPlayerName:draft.targetPlayerName,targetPlayerUuid:draft.targetPlayerUuid,lockId:draft.lockId,expectedFingerprint:draft.expectedFingerprint})});
                    if(result.success){const session=result.data?.containerTemplateSession||{};draft.sessionId=containerTemplateSessionId(session)||draft.sessionId;draft.status=session.status||'started';draft.active=true;draft.saving=false;draft.message=result.message||'已通知目标玩家打开 GUI。';appState.containerTemplateSession=draft;scheduleContainerTemplateSessionStatusPoll();showContainerTemplateSessionModal(deviceId);return;}
                    await releaseContainerTemplateSessionLock(draft,true);
                    draft.lockId='';draft.lock=null;draft.saving=false;draft.errors=writeResultErrors(result,'启动容器模板会话失败。');draft.conflict=result.conflict||null;draft.message=result.message||'启动容器模板会话失败。';appState.containerTemplateSession=draft;stopContainerTemplateSessionHeartbeat();showContainerTemplateSessionModal(deviceId);toast(draft.message);
                  }catch(err){await releaseContainerTemplateSessionLock(draft,true);draft.lockId='';draft.lock=null;draft.saving=false;draft.errors=[{message:err.message||'启动容器模板会话失败。'}];appState.containerTemplateSession=draft;stopContainerTemplateSessionHeartbeat();showContainerTemplateSessionModal(deviceId);toast(err.message||'启动容器模板会话失败');}
                }
                function scheduleContainerTemplateSessionHeartbeat(){stopContainerTemplateSessionHeartbeat();appState.containerTemplateSessionLockTimer=setTimeout(async()=>{await heartbeatContainerTemplateSession();if(appState.containerTemplateSession?.lockId)scheduleContainerTemplateSessionHeartbeat();},30000);}
                function stopContainerTemplateSessionHeartbeat(){if(appState.containerTemplateSessionLockTimer){clearTimeout(appState.containerTemplateSessionLockTimer);appState.containerTemplateSessionLockTimer=null;}}
                function scheduleContainerTemplateSessionStatusPoll(){stopContainerTemplateSessionStatusPoll();appState.containerTemplateSessionStatusTimer=setTimeout(async()=>{await refreshContainerTemplateSessionStatus('poll');if(containerTemplateSessionIsActive(appState.containerTemplateSession))scheduleContainerTemplateSessionStatusPoll();},2000);}
                function stopContainerTemplateSessionStatusPoll(){if(appState.containerTemplateSessionStatusTimer){clearTimeout(appState.containerTemplateSessionStatusTimer);appState.containerTemplateSessionStatusTimer=null;}}
                async function heartbeatContainerTemplateSession(){const draft=appState.containerTemplateSession;if(!draft||!draft.lockId)return;try{const result=await api('/api/webadmin/edit-locks/heartbeat',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'virtual_block_device_container_template',targetId:draft.deviceId,lockId:draft.lockId})});if(result.success){draft.lock=result.data?.lock||draft.lock;}await refreshContainerTemplateSessionStatus('heartbeat');}catch(err){draft.errors=[{message:err.message||'容器模板会话状态刷新失败'}];}}
                async function refreshContainerTemplateSessionStatus(source='status'){
                  const draft=appState.containerTemplateSession;if(!draft||!draft.sessionId||!draft.active)return;
                  const status=await api(`/api/webadmin/virtual-block-devices/${encodeURIComponent(draft.deviceId)}/container-template-session/status?sessionId=${encodeURIComponent(draft.sessionId)}`);
                  const sessionId=containerTemplateSessionId(status);if(sessionId&&draft.sessionId&&sessionId!==draft.sessionId)return;
                  draft.status=status.status||draft.status;draft.active=!!status.active;draft.message=status.message||draft.message;draft.sessionId=sessionId||draft.sessionId;
                  if(!draft.active){draft.lockId='';draft.lock=null;stopContainerTemplateSessionHeartbeat();stopContainerTemplateSessionStatusPoll();cancelContainerTemplateCancelConfirm();if(['saved','completed'].includes(String(draft.status||''))&&draft.logicChainDraftContext){applyLogicChainExistingVbdContainerCapture(status,draft.logicChainDraftContext);appState.containerTemplateSession=null;await showLogicChainExistingEditModalRestoringVbd(logicChainExistingVbdPageKey(appState.logicChainEditor?.existingEdit,'detail'));return;}if(['saved','completed'].includes(String(draft.status||''))){markRealtimeRouteKeyDirty('devices',{type:'config_changed'});if(draft.deviceId)markRealtimeRouteKeyDirty(`device:${draft.deviceId}`,{type:'config_changed'});await refreshContainerTemplateSessionOverview(draft.deviceId,true);return;}showContainerTemplateSessionModal(draft.deviceId);}
                  else if(source==='poll'){appState.containerTemplateSession=draft;}
                }
                async function refreshContainerTemplateSessionOverview(deviceId,silent=false){
                  const draft=appState.containerTemplateSession;if(!draft||!sameDeviceRef(draft.deviceId,deviceId))return;
                  try{
                    const overview=await api(`/api/webadmin/virtual-block-devices/${encodeURIComponent(draft.deviceId)}/container-template`);
                    const current=appState.containerTemplateSession;
                    if(!current||!sameDeviceRef(current.deviceId,deviceId))return;
                    current.overview=overview||current.overview||{};
                    current.expectedFingerprint=current.overview.expectedFingerprint||current.expectedFingerprint||'';
                    current.errors=[];
                    appState.containerTemplateSession=current;
                    showContainerTemplateSessionModal(current.deviceId);
                  }catch(err){
                    if(!silent){toast(err.message||'已保存模板快照刷新失败');}
                    draft.errors=[{message:err.message||'已保存模板快照刷新失败，请重新打开查看。'}];
                    appState.containerTemplateSession=draft;
                    showContainerTemplateSessionModal(draft.deviceId);
                  }
                }
                async function releaseContainerTemplateSessionLock(draft,silent){if(!draft||!draft.lockId)return;try{await api('/api/webadmin/edit-locks/release',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'virtual_block_device_container_template',targetId:draft.deviceId,lockId:draft.lockId})});}catch(err){if(!silent)toast(err.message||'容器变化模板编辑锁释放失败，将等待自动过期。');}}
                function containerTemplateSessionIsActive(draft){return !!(draft&&draft.active&&draft.sessionId);}
                function requestContainerTemplateSessionCancel(deviceId,options={}){
                  const draft=appState.containerTemplateSession;if(!draft||!sameDeviceRef(draft.deviceId,deviceId))return false;
                  if(containerTemplateSessionIsActive(draft)&&!options.confirmed){openContainerTemplateCancelConfirm(deviceId,{closeAfter:!!options.closeAfter,reason:options.reason||'WebAdmin 已取消容器模板会话。'});return false;}
                  return cancelContainerTemplateSession(deviceId,{...options,confirmed:true});
                }
                function openContainerTemplateCancelConfirm(deviceId,options={}){
                  const draft=appState.containerTemplateSession;if(!containerTemplateSessionIsActive(draft)||!sameDeviceRef(draft.deviceId,deviceId))return false;
                  const root=document.getElementById('wa-modal-root');if(!root)return false;
                  cancelContainerTemplateCancelConfirm();
                  appState.containerTemplateCancelConfirm={deviceId,closeAfter:!!options.closeAfter,reason:options.reason||'WebAdmin 已取消容器模板会话。'};
                  const layer=document.createElement('div');
                  layer.id='wa-container-template-cancel-confirm';
                  layer.className='wa-discard-confirm-layer';
                  layer.setAttribute('data-container-template-cancel-confirm','true');
                  layer.setAttribute('data-container-template-confirm-esc-continues','true');
                  layer.setAttribute('data-container-template-confirm-backdrop-continues','true');
                  layer.onclick=event=>{if(event.target===layer)cancelContainerTemplateCancelConfirm();};
                  layer.innerHTML=`<section class="wa-discard-confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="wa-container-template-cancel-title" onclick="event.stopPropagation()"><header><h3 id="wa-container-template-cancel-title">确认取消容器模板编辑会话？</h3></header><p>目标玩家的游戏内模板 GUI 正在打开。关闭后会取消会话，不会保存任何容器模板修改。</p><footer><button class="wa-btn ghost" type="button" onclick="cancelContainerTemplateCancelConfirm()">继续编辑</button><button class="wa-btn danger" type="button" onclick="confirmContainerTemplateSessionCancel()">${icon('critical-issue')}<span>确认取消会话</span></button></footer></section>`;
                  root.appendChild(layer);
                  return false;
                }
                function cancelContainerTemplateCancelConfirm(){
                  appState.containerTemplateCancelConfirm=null;
                  const layer=document.getElementById('wa-container-template-cancel-confirm');
                  if(layer)layer.remove();
                }
                async function confirmContainerTemplateSessionCancel(){
                  const confirm=appState.containerTemplateCancelConfirm;if(!confirm)return false;
                  cancelContainerTemplateCancelConfirm();
                  return await cancelContainerTemplateSession(confirm.deviceId,{closeAfter:!!confirm.closeAfter,reason:confirm.reason||'WebAdmin 已取消容器模板会话。',confirmed:true});
                }
                async function cancelContainerTemplateSession(deviceId,options={}){
                  const draft=appState.containerTemplateSession;if(!draft||!sameDeviceRef(draft.deviceId,deviceId))return false;
                  if(draft.saving)return false;
                  const closeAfter=!!options.closeAfter, reason=options.reason||'WebAdmin 已取消容器模板会话。';
                  draft.saving=true;draft.errors=[];draft.message='正在取消容器模板会话...';appState.containerTemplateSession=draft;showContainerTemplateSessionModal(deviceId);
                  try{
                    if(draft.sessionId){
                      const result=await api(`/api/webadmin/virtual-block-devices/${encodeURIComponent(draft.deviceId)}/container-template-session/cancel`,{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({deviceId:draft.deviceId,sessionId:draft.sessionId,lockId:draft.lockId,reason})});
                      if(!result.success){draft.saving=false;draft.errors=writeResultErrors(result,'取消容器模板会话失败。');draft.message=result.message||draft.message;appState.containerTemplateSession=draft;showContainerTemplateSessionModal(deviceId);toast(draft.message||'取消容器模板会话失败');return false;}
                      const session=result.data?.containerTemplateSession||{};
                      draft.errors=[];draft.message=result.message||session.message||'容器模板会话已取消。';draft.status=session.status||'cancelled';draft.active=false;
                    }else{
                      await releaseContainerTemplateSessionLock(draft,false);draft.status='cancelled';draft.active=false;draft.message='容器模板会话已取消。';
                    }
                  }catch(err){draft.saving=false;draft.errors=[{message:err.message||'取消容器模板会话失败。'}];appState.containerTemplateSession=draft;showContainerTemplateSessionModal(deviceId);toast(err.message||'取消容器模板会话失败');return false;}
                  draft.saving=false;draft.lockId='';draft.lock=null;stopContainerTemplateSessionHeartbeat();stopContainerTemplateSessionStatusPoll();
                  if(closeAfter){appState.containerTemplateSession=null;await dismissWebAdminModal();return true;}
                  appState.containerTemplateSession=draft;showContainerTemplateSessionModal(deviceId);return true;
                }
                async function closeContainerTemplateSessionModal(deviceId){
                  const draft=appState.containerTemplateSession;if(!draft||!sameDeviceRef(draft.deviceId,deviceId)){await dismissWebAdminModal();return true;}
                  if(draft.saving){toast('容器模板会话操作正在进行，请稍候。');showContainerTemplateSessionModal(deviceId);return false;}
                  if(draft.active&&draft.sessionId)return requestContainerTemplateSessionCancel(deviceId,{closeAfter:true,reason:'WebAdmin 关闭窗口时取消容器模板会话。'});
                  await releaseContainerTemplateSessionLock(draft,true);appState.containerTemplateSession=null;stopContainerTemplateSessionHeartbeat();stopContainerTemplateSessionStatusPoll();await dismissWebAdminModal();return true;
                }
                function maybeCancelContainerTemplateSessionForRoute(hash){
                  const draft=appState.containerTemplateSession;if(!draft)return;
                  const h=String(hash||'');if(h.startsWith('#/devices/')){const info=detailRoute(h.substring('#/devices/'.length),'#/devices');if(sameDeviceRef(info.id,draft.deviceId))return;}
                  if(draft.active&&draft.sessionId){requestContainerTemplateSessionCancel(draft.deviceId,{closeAfter:true,reason:'WebAdmin 离开页面时取消容器模板会话。'});return;}
                  releaseContainerTemplateSessionLock(draft,true);appState.containerTemplateSession=null;stopContainerTemplateSessionHeartbeat();stopContainerTemplateSessionStatusPoll();
                }
                function singleItemSubmitSessionId(value){return String(value?.sessionId||value?.sessionRef||value?.id||'');}
                function singleItemSubmitResultSession(result){return result?.data?.singleItemSubmitTemplateSession||result?.data?.singleItemSubmitSession||{};}
                function singleItemSubmitSessionDefault(deviceId,overview={},players=[]){
                  return {deviceId,overview,players,playersError:appState.onlinePlayerOptionsError,targetPlayerName:(players[0]?.name||''),targetPlayerUuid:(players[0]?.uuid||''),status:'ready',active:false,sessionId:'',lockId:'',lock:null,expectedFingerprint:overview.expectedFingerprint||'',saving:false,errors:[],message:''};
                }
                function singleItemSubmitActionAttrs(action,deviceId,disabled=false,title=''){
                  const disabledAttr=disabled?' disabled':'', titleAttr=title?` title="${esc(title)}"`:'';
                  return `type="button" data-action="${esc(action)}" data-device-id="${esc(deviceId)}"${disabledAttr}${titleAttr}`;
                }
                function handleSingleItemSubmitAction(event){
                  const target=event.target, button=target&&target.closest?target.closest('[data-action^="single-item-submit-"]'):null;
                  if(!button||button.disabled)return false;
                  const action=String(button.dataset.action||''), deviceId=button.dataset.deviceId||appState.singleItemSubmitSession?.deviceId||'';
                  if(!action)return false;
                  event.preventDefault();event.stopPropagation();
                  if(action==='single-item-submit-open')openSingleItemSubmitSessionModal(deviceId);
                  else if(action==='single-item-submit-open-unified')openSingleItemSubmitSessionModalFromUnified(deviceId);
                  else if(action==='single-item-submit-start')startSingleItemSubmitSession(deviceId);
                  else if(action==='single-item-submit-cancel')requestSingleItemSubmitSessionCancel(deviceId);
                  else if(action==='single-item-submit-close')closeWebAdminModal();
                  else return false;
                  return true;
                }
                async function openSingleItemSubmitSessionModal(deviceId){
                  try{
                    if(appState.vbdNativeTriggerEdit&&sameDeviceRef(appState.vbdNativeTriggerEdit.deviceId,deviceId)&&vbdNativeTriggerDirty(appState.vbdNativeTriggerEdit)){toast('请先保存或放弃当前原生触发配置草稿，再启动 itemSubmit 条件编辑器。');return;}
                    const overview=await api(`/api/webadmin/virtual-block-devices/${encodeURIComponent(deviceId)}/single-item-submit`);
                    const players=await loadOnlinePlayerOptions(true);
                    appState.singleItemSubmitSession=singleItemSubmitSessionDefault(overview.deviceId||deviceId,overview,players||[]);
                    showSingleItemSubmitSessionModal(overview.deviceId||deviceId);
                  }catch(err){toast(err.message||'itemSubmit 条件编辑器会话初始化失败');}
                }
                async function openSingleItemSubmitSessionModalFromUnified(deviceId){
                  if(isDeviceConfigModalDirty(deviceId)){toast('请先保存或放弃当前设备配置草稿，再启动 itemSubmit 条件编辑器。');return;}
                  await releaseAllDeviceConfigLocks(deviceId,true);
                  appState.deviceConfigEdit=null;
                  await dismissWebAdminModal();
                  await openSingleItemSubmitSessionModal(deviceId);
                }
                function showSingleItemSubmitSessionModal(deviceId){
                  const draft=appState.singleItemSubmitSession;
                  if(!draft||!sameDeviceRef(draft.deviceId,deviceId))return;
                  const overview=draft.overview||{};
                  const item=overview.itemSubmit||{};
                  const startDisabled=draft.saving||lockHeldByOther(overview.lockStatus)||(overview.interactionEnabled===false&&!draft.logicChainDraftContext);
                  const title=overview.interactionEnabled===false?'请先启用右键交互触发。':'';
                  const footer=`<button class="wa-btn ghost" ${singleItemSubmitActionAttrs('single-item-submit-close',draft.deviceId,!!draft.saving)}>${icon('close')}<span>关闭</span></button>${draft.active?`<button class="wa-btn danger" ${singleItemSubmitActionAttrs('single-item-submit-cancel',draft.deviceId,!!draft.saving)}>${icon('critical-issue')}<span>${draft.saving?'取消中...':'取消会话'}</span></button>`:`<button class="wa-btn primary" ${singleItemSubmitActionAttrs('single-item-submit-start',draft.deviceId,startDisabled,title)}>${icon('selection')}<span>${draft.saving?'启动中...':'打开 itemSubmit 条件 GUI'}</span></button>`}`;
                  openWebAdminModal(draft.logicChainDraftContext?'Logic Chain itemSubmit 捕获':'itemSubmit 条件编辑器',singleItemSubmitSessionBody(draft),footer,{className:'wa-config-modal wa-single-item-submit-modal',onClose:async()=>closeSingleItemSubmitSessionModal(draft.deviceId)});
                }
                function singleItemSubmitSessionBody(draft){
                  const overview=draft.overview||{}, lock=overview.lockStatus, playerOptions=draft.players||[], item=overview.itemSubmit||{}, req=item.requirement||item||{};
                  const errors=(draft.errors||[]).length?`<ul class="validation-list">${draft.errors.map(e=>`<li>${esc(e.message||e||'会话操作失败')}</li>`).join('')}</ul>`:'';
                  const status=singleItemSubmitStatusLine(draft);
                  const lockLine=lockHeldByOther(lock)?`<div class="readonly-note danger">${esc(lockMessage(lock,'itemSubmit 条件编辑器'))}</div>`:'';
                  if(overview.interactionEnabled===false&&!draft.logicChainDraftContext){
                    const disabledWarning='<div class="readonly-note warning" data-single-item-submit-hidden-when-interaction-disabled="true" data-unified-item-submit-disabled-warning="true">右键交互触发尚未启用；itemSubmit requirements 属于右键交互后的提交层，不能单独编辑。请先启用右键交互后再打开统一编辑器。</div>';
                    return `<form class="edit-form wa-single-item-submit-session-form" data-single-item-submit-session="7.11" data-unified-item-submit-editor="true" data-item-submit-hidden-when-interaction-disabled="true" data-single-item-submit-under-right-click="true" onsubmit="event.preventDefault()">${errors}${lockLine}${status}${disabledWarning}<p class="muted">此处不会显示玩家选择、已保存快照或完整编辑入口，避免把 itemSubmit 误导为独立触发源。</p></form>`;
                  }
                  const playerSelect=draft.active?`<div class="readonly-note">目标玩家：${esc(draft.targetPlayerName||'-')} · Session：${esc(shortId(draft.sessionId||''))}</div>`:`<label>目标在线玩家<select id="single-item-submit-player" class="input" ${playerOptions.length?'':'disabled'} onchange="syncSingleItemSubmitSessionDraftFromForm(${jsString(draft.deviceId)})">${playerOptions.map(p=>`<option value="${esc(p.name||'')}" data-uuid="${esc(p.uuid||'')}" ${String(p.name||'')===String(draft.targetPlayerName||'')?'selected':''}>${esc(p.name||'未命名玩家')}</option>`).join('')}</select><span class="muted">${draft.playersError?'在线玩家加载失败。':'选择要打开 itemSubmit 条件 GUI 的在线玩家。'}</span></label>`;
                  const warning='';
                  const multi='';
                  const advanced='';
                """)
.append("""
                  const matcherOptions=['matchDamage','matchCustomName','matchLore','matchCustomData','matchComponents'].filter(k=>item[k]===true).map(k=>({matchDamage:'damage',matchCustomName:'自定义名称',matchLore:'Lore',matchCustomData:'customData',matchComponents:'components'}[k])).join(' / ')||'仅物品 ID';
                  const consumeLine=item.itemSubmitConsumeEnabled?`消耗 ${esc(item.consumeCount||1)} · ${esc(item.itemSubmitConsumeOrderDisplayName||item.itemSubmitConsumeOrder||'hotbar_first')}`:'提交后不消耗';
                  const displayTemplatePill=item.displayTemplateComponentsPreserved||item.templateDisplayStack?'<span class="pill" data-single-item-submit-display-template-preserved="true">包含组件/附魔显示数据</span>':'';
                  const reqCount=Number(item.requirementCount||0), enabledCount=Number(item.enabledRequirementCount||0), reqs=Array.isArray(item.requirements)?item.requirements:[];
                  const modeLine=reqCount<=0?'未配置 itemSubmit':(reqCount===1?'单物品提交':'多物品提交：'+reqCount+' 个条件，'+enabledCount+' 个启用');
                  const modeAttr=reqCount<=0?'data-zero-requirement-add-only="true"':(reqCount===1?'data-single-requirement-simplified="true"':'data-multi-requirement-summary="true" data-multi-requirement-controls-visible="true"');
                  const reqRows=reqs.slice(0,4).map((r,i)=>`<span class="pill info">${i+1}. ${esc(r.templateItemId||r.summary||'未配置')} · ${esc(labelCountMode(r.countMode||'at_least'))} ${esc(r.requiredCount||r.count||1)}</span>`).join('');
                  const snapshot=reqCount>0?`${reqRows}${reqCount>4?`<span class="pill">+${reqCount-4} more</span>`:''}<span class="pill">${item.itemSubmitEnabled?'itemSubmit 已启用':'itemSubmit 已禁用'}</span><span class="pill">启用 ${enabledCount}/${reqCount}</span><span class="pill">matcher：${esc(matcherOptions)}</span><span class="pill">${consumeLine}</span>${displayTemplatePill}`:'<span class="muted">当前没有已保存 itemSubmit requirements。</span>';
                  return `<form class="edit-form wa-single-item-submit-session-form" data-single-item-submit-session="7.11" data-unified-item-submit-editor="true" data-item-submit-requirement-list="true" data-unified-requirement-list-only="true" data-item-submit-adaptive-zero-one-many="true" ${modeAttr} data-single-item-submit-under-right-click="true" data-old-multi-requirement-readonly-refusal-removed="true" data-single-item-submit-advanced-editable="true" data-single-item-submit-consume-editor="true" data-single-item-submit-vanilla-policy-existing-field="true" data-single-item-submit-no-raw-json="true" data-single-item-submit-no-condition-engine="true" data-single-item-submit-lock-target="virtual_block_device_single_item_submit" data-single-item-submit-fingerprint="itemSubmit-requirement-list" onsubmit="event.preventDefault()">${errors}${lockLine}${status}${warning}${multi}${advanced}${playerSelect}<div class="readonly-note"><strong>${esc(modeLine)}</strong><div class="wa-template-condition-pills">${snapshot}</div></div><div class="readonly-note"><strong>原版交互策略</strong><div>${esc(item.interactionItemVanillaPolicyDisplayName||overview.vanillaPolicyDisplayName||'沿用当前交互物品匹配策略')}</div><small>游戏内统一 GUI 编辑同一个 InteractionItemVanillaPolicy 字段，不新增屏蔽交互字段，不改变运行时 PASS / FAIL 语义。</small></div><p class="muted">7.11 会在目标玩家客户端打开统一 itemSubmit requirement list GUI。0 / 1 / N requirements 自动表现为未配置、单物品提交、多物品提交；单项模式不显示排序、删除、批量管理按钮；不会进入 inventory/equipment、ConditionEngine、路径图或 raw JSON。</p></form>`;
                }
                function syncSingleItemSubmitSessionDraftFromForm(deviceId){
                  const draft=appState.singleItemSubmitSession;if(!draft||!sameDeviceRef(draft.deviceId,deviceId))return draft;
                  const select=document.getElementById('single-item-submit-player');
                  if(select){const option=select.options&&select.selectedIndex>=0?select.options[select.selectedIndex]:null;draft.targetPlayerName=select.value||'';draft.targetPlayerUuid=option?.dataset?.uuid||'';}
                  return draft;
                }
                function singleItemSubmitStatusLine(draft){
                  const status=String(draft.status||'ready');
                  const tone={ready:'info',started:'info',opened:'ok',saved:'ok',completed:'ok',cancelled:'warning',failed:'error',expired:'warning'}[status]||'info';
                  const label={ready:'准备启动',started:'已启动，等待玩家打开 GUI',opened:'GUI 已打开',saved:'itemSubmit 条件已保存',completed:'itemSubmit 条件已保存',cancelled:'会话已取消',failed:'会话失败',expired:'会话已过期'}[status]||status;
                  return `<div class="wa-selection-status ${tone}" data-single-item-submit-session-status="${esc(status)}"><strong>${esc(label)}</strong><span>${esc(draft.message||'游戏内 ESC / 关闭窗口会取消，不保存。')}</span>${draft.lockId?`<span>锁：${esc(shortId(draft.lockId))} · expectedFingerprint：${esc(shortId(draft.expectedFingerprint||''))}</span>`:''}</div>`;
                }
                async function startSingleItemSubmitSession(deviceId){
                  const draft=syncSingleItemSubmitSessionDraftFromForm(deviceId)||appState.singleItemSubmitSession;
                  if(!draft||draft.saving)return;
                  if(!draft.targetPlayerName){draft.errors=[{message:'请选择在线玩家。'}];appState.singleItemSubmitSession=draft;showSingleItemSubmitSessionModal(deviceId);return;}
                  draft.saving=true;draft.errors=[];draft.message='正在获取编辑锁并打开 itemSubmit 条件 GUI...';appState.singleItemSubmitSession=draft;showSingleItemSubmitSessionModal(deviceId);
                  try{
                    const lockResult=await acquireWebAdminEditLock('virtual_block_device_single_item_submit',draft.deviceId);
                    if(!lockResult.success){draft.saving=false;draft.errors=writeResultErrors(lockResult,'itemSubmit 条件编辑锁获取失败。');appState.singleItemSubmitSession=draft;showSingleItemSubmitSessionModal(deviceId);toast(lockResult.message||'编辑锁获取失败');return;}
                    draft.lock=lockResult.data?.lock||{};draft.lockId=draft.lock.lockId||'';scheduleSingleItemSubmitSessionHeartbeat();
                    const result=await api(`/api/webadmin/virtual-block-devices/${encodeURIComponent(draft.deviceId)}/single-item-submit-session/start`,{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({...logicChainVbdTemplateSessionContextPayload(draft.logicChainDraftContext),deviceId:draft.deviceId,targetPlayerName:draft.targetPlayerName,targetPlayerUuid:draft.targetPlayerUuid,lockId:draft.lockId,expectedFingerprint:draft.expectedFingerprint})});
                    if(result.success){const session=singleItemSubmitResultSession(result);draft.sessionId=singleItemSubmitSessionId(session)||draft.sessionId;draft.status=session.status||'started';draft.active=true;draft.saving=false;draft.message=result.message||'已通知目标玩家打开 itemSubmit 条件 GUI。';appState.singleItemSubmitSession=draft;scheduleSingleItemSubmitSessionStatusPoll();showSingleItemSubmitSessionModal(deviceId);return;}
                    await releaseSingleItemSubmitSessionLock(draft,true);
                    draft.lockId='';draft.lock=null;draft.saving=false;draft.errors=writeResultErrors(result,'启动单物品提交会话失败。');draft.conflict=result.conflict||null;draft.message=result.message||'启动单物品提交会话失败。';appState.singleItemSubmitSession=draft;stopSingleItemSubmitSessionHeartbeat();showSingleItemSubmitSessionModal(deviceId);toast(draft.message);
                  }catch(err){await releaseSingleItemSubmitSessionLock(draft,true);draft.lockId='';draft.lock=null;draft.saving=false;draft.errors=[{message:err.message||'启动单物品提交会话失败。'}];appState.singleItemSubmitSession=draft;stopSingleItemSubmitSessionHeartbeat();showSingleItemSubmitSessionModal(deviceId);toast(err.message||'启动单物品提交会话失败');}
                }
                function scheduleSingleItemSubmitSessionHeartbeat(){stopSingleItemSubmitSessionHeartbeat();appState.singleItemSubmitSessionLockTimer=setTimeout(async()=>{await heartbeatSingleItemSubmitSession();if(appState.singleItemSubmitSession?.lockId)scheduleSingleItemSubmitSessionHeartbeat();},30000);}
                function stopSingleItemSubmitSessionHeartbeat(){if(appState.singleItemSubmitSessionLockTimer){clearTimeout(appState.singleItemSubmitSessionLockTimer);appState.singleItemSubmitSessionLockTimer=null;}}
                function scheduleSingleItemSubmitSessionStatusPoll(){stopSingleItemSubmitSessionStatusPoll();appState.singleItemSubmitSessionStatusTimer=setTimeout(async()=>{await refreshSingleItemSubmitSessionStatus('poll');if(singleItemSubmitSessionIsActive(appState.singleItemSubmitSession))scheduleSingleItemSubmitSessionStatusPoll();},2000);}
                function stopSingleItemSubmitSessionStatusPoll(){if(appState.singleItemSubmitSessionStatusTimer){clearTimeout(appState.singleItemSubmitSessionStatusTimer);appState.singleItemSubmitSessionStatusTimer=null;}}
                """)
.append("""
                async function heartbeatSingleItemSubmitSession(){const draft=appState.singleItemSubmitSession;if(!draft||!draft.lockId)return;try{const result=await api('/api/webadmin/edit-locks/heartbeat',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'virtual_block_device_single_item_submit',targetId:draft.deviceId,lockId:draft.lockId})});if(result.success){draft.lock=result.data?.lock||draft.lock;}await refreshSingleItemSubmitSessionStatus('heartbeat');}catch(err){draft.errors=[{message:err.message||'单物品提交会话状态刷新失败'}];}}
                async function refreshSingleItemSubmitSessionStatus(source='status'){
                  const draft=appState.singleItemSubmitSession;if(!draft||!draft.sessionId||!draft.active)return;
                  const status=await api(`/api/webadmin/virtual-block-devices/${encodeURIComponent(draft.deviceId)}/single-item-submit-session/status?sessionId=${encodeURIComponent(draft.sessionId)}`);
                  const sessionId=singleItemSubmitSessionId(status);if(sessionId&&draft.sessionId&&sessionId!==draft.sessionId)return;
                  draft.status=status.status||draft.status;draft.active=!!status.active;draft.message=status.message||draft.message;draft.sessionId=sessionId||draft.sessionId;
                  if(!draft.active){draft.lockId='';draft.lock=null;stopSingleItemSubmitSessionHeartbeat();stopSingleItemSubmitSessionStatusPoll();cancelSingleItemSubmitCancelConfirm();if(['saved','completed'].includes(String(draft.status||''))&&draft.logicChainDraftContext){applyLogicChainExistingVbdItemSubmitCapture(status,draft.logicChainDraftContext);appState.singleItemSubmitSession=null;await showLogicChainExistingEditModalRestoringVbd(logicChainExistingVbdPageKey(appState.logicChainEditor?.existingEdit,'detail'));return;}if(['saved','completed'].includes(String(draft.status||''))){markRealtimeRouteKeyDirty('devices',{type:'config_changed'});if(draft.deviceId){deviceDetailRouteKeys(draft.deviceId,'virtual_block_device').forEach(key=>markRealtimeRouteKeyDirty(key,{type:'config_changed'}));}await refreshSingleItemSubmitSessionOverview(draft.deviceId,true);return;}showSingleItemSubmitSessionModal(draft.deviceId);}
                  else if(source==='poll'){appState.singleItemSubmitSession=draft;}
                }
                async function refreshSingleItemSubmitSessionOverview(deviceId,silent=false){
                  const draft=appState.singleItemSubmitSession;if(!draft||!sameDeviceRef(draft.deviceId,deviceId))return;
                  try{
                    const overview=await api(`/api/webadmin/virtual-block-devices/${encodeURIComponent(draft.deviceId)}/single-item-submit`);
                    const current=appState.singleItemSubmitSession;
                    if(!current||!sameDeviceRef(current.deviceId,deviceId))return;
                    current.overview=overview||current.overview||{};
                    current.expectedFingerprint=current.overview.expectedFingerprint||current.expectedFingerprint||'';
                    current.errors=[];
                    appState.singleItemSubmitSession=current;
                    if(currentRouteHash().startsWith('#/devices/'))await renderDeviceDetail(currentDeviceRouteArg(current.deviceId),{silent:true});
                    showSingleItemSubmitSessionModal(current.deviceId);
                  }catch(err){
                    if(!silent){toast(err.message||'已保存单物品提交快照刷新失败');}
                    draft.errors=[{message:err.message||'已保存单物品提交快照刷新失败，请重新打开查看。'}];
                    appState.singleItemSubmitSession=draft;
                    showSingleItemSubmitSessionModal(draft.deviceId);
                  }
                }
                async function releaseSingleItemSubmitSessionLock(draft,silent){if(!draft||!draft.lockId)return;try{await api('/api/webadmin/edit-locks/release',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'virtual_block_device_single_item_submit',targetId:draft.deviceId,lockId:draft.lockId})});}catch(err){if(!silent)toast(err.message||'itemSubmit 条件编辑锁释放失败，将等待自动过期。');}}
                function singleItemSubmitSessionIsActive(draft){return !!(draft&&draft.active&&draft.sessionId);}
                function requestSingleItemSubmitSessionCancel(deviceId,options={}){
                  const draft=appState.singleItemSubmitSession;if(!draft||!sameDeviceRef(draft.deviceId,deviceId))return false;
                  if(singleItemSubmitSessionIsActive(draft)&&!options.confirmed){openSingleItemSubmitCancelConfirm(deviceId,{closeAfter:!!options.closeAfter,reason:options.reason||'WebAdmin 已取消 itemSubmit 条件会话。'});return false;}
                  return cancelSingleItemSubmitSession(deviceId,{...options,confirmed:true});
                }
                function openSingleItemSubmitCancelConfirm(deviceId,options={}){
                  const draft=appState.singleItemSubmitSession;if(!singleItemSubmitSessionIsActive(draft)||!sameDeviceRef(draft.deviceId,deviceId))return false;
                  const root=document.getElementById('wa-modal-root');if(!root)return false;
                  cancelSingleItemSubmitCancelConfirm();
                  appState.singleItemSubmitCancelConfirm={deviceId,closeAfter:!!options.closeAfter,reason:options.reason||'WebAdmin 已取消 itemSubmit 条件会话。'};
                  const layer=document.createElement('div');
                  layer.id='wa-single-item-submit-cancel-confirm';
                  layer.className='wa-discard-confirm-layer';
                  layer.setAttribute('data-single-item-submit-cancel-confirm','true');
                  layer.onclick=event=>{if(event.target===layer)cancelSingleItemSubmitCancelConfirm();};
                  layer.innerHTML=`<section class="wa-discard-confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="wa-single-item-submit-cancel-title" onclick="event.stopPropagation()"><header><h3 id="wa-single-item-submit-cancel-title">确认取消 itemSubmit 条件编辑？</h3></header><p>目标玩家的游戏内 itemSubmit 条件 GUI 正在打开。关闭后会取消会话，不会保存任何 requirement 修改。</p><footer><button class="wa-btn ghost" type="button" onclick="cancelSingleItemSubmitCancelConfirm()">继续编辑</button><button class="wa-btn danger" type="button" onclick="confirmSingleItemSubmitSessionCancel()">${icon('critical-issue')}<span>确认取消会话</span></button></footer></section>`;
                  root.appendChild(layer);
                  return false;
                }
                function cancelSingleItemSubmitCancelConfirm(){
                  appState.singleItemSubmitCancelConfirm=null;
                  const layer=document.getElementById('wa-single-item-submit-cancel-confirm');
                  if(layer)layer.remove();
                }
                async function confirmSingleItemSubmitSessionCancel(){
                  const confirm=appState.singleItemSubmitCancelConfirm;if(!confirm)return false;
                  cancelSingleItemSubmitCancelConfirm();
                  return await cancelSingleItemSubmitSession(confirm.deviceId,{closeAfter:!!confirm.closeAfter,reason:confirm.reason||'WebAdmin 已取消 itemSubmit 条件会话。',confirmed:true});
                }
                async function cancelSingleItemSubmitSession(deviceId,options={}){
                  const draft=appState.singleItemSubmitSession;if(!draft||!sameDeviceRef(draft.deviceId,deviceId))return false;
                  if(draft.saving)return false;
                  const closeAfter=!!options.closeAfter, reason=options.reason||'WebAdmin 已取消 itemSubmit 条件会话。';
                  draft.saving=true;draft.errors=[];draft.message='正在取消 itemSubmit 条件会话...';appState.singleItemSubmitSession=draft;showSingleItemSubmitSessionModal(deviceId);
                  try{
                    if(draft.sessionId){
                      const result=await api(`/api/webadmin/virtual-block-devices/${encodeURIComponent(draft.deviceId)}/single-item-submit-session/cancel`,{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({deviceId:draft.deviceId,sessionId:draft.sessionId,lockId:draft.lockId,reason})});
                      if(!result.success){draft.saving=false;draft.errors=writeResultErrors(result,'取消 itemSubmit 条件会话失败。');draft.message=result.message||draft.message;appState.singleItemSubmitSession=draft;showSingleItemSubmitSessionModal(deviceId);toast(draft.message||'取消 itemSubmit 条件会话失败');return false;}
                      const session=singleItemSubmitResultSession(result);
                      draft.errors=[];draft.message=result.message||session.message||'itemSubmit 条件会话已取消。';draft.status=session.status||'cancelled';draft.active=false;
                    }else{
                      await releaseSingleItemSubmitSessionLock(draft,false);draft.status='cancelled';draft.active=false;draft.message='itemSubmit 条件会话已取消。';
                    }
                  }catch(err){draft.saving=false;draft.errors=[{message:err.message||'取消 itemSubmit 条件会话失败。'}];appState.singleItemSubmitSession=draft;showSingleItemSubmitSessionModal(deviceId);toast(err.message||'取消 itemSubmit 条件会话失败');return false;}
                  draft.saving=false;draft.lockId='';draft.lock=null;stopSingleItemSubmitSessionHeartbeat();stopSingleItemSubmitSessionStatusPoll();
                  if(closeAfter){appState.singleItemSubmitSession=null;await dismissWebAdminModal();return true;}
                  appState.singleItemSubmitSession=draft;showSingleItemSubmitSessionModal(deviceId);return true;
                }
                async function closeSingleItemSubmitSessionModal(deviceId){
                  const draft=appState.singleItemSubmitSession;if(!draft||!sameDeviceRef(draft.deviceId,deviceId)){await dismissWebAdminModal();return true;}
                  if(draft.saving){toast('itemSubmit 条件会话操作正在进行，请稍候。');showSingleItemSubmitSessionModal(deviceId);return false;}
                  if(draft.active&&draft.sessionId)return requestSingleItemSubmitSessionCancel(deviceId,{closeAfter:true,reason:'WebAdmin 关闭窗口时取消 itemSubmit 条件会话。'});
                  await releaseSingleItemSubmitSessionLock(draft,true);appState.singleItemSubmitSession=null;stopSingleItemSubmitSessionHeartbeat();stopSingleItemSubmitSessionStatusPoll();await dismissWebAdminModal();return true;
                }
                function maybeCancelSingleItemSubmitSessionForRoute(hash){
                  const draft=appState.singleItemSubmitSession;if(!draft)return;
                  const h=String(hash||'');if(h.startsWith('#/devices/')){const info=detailRoute(h.substring('#/devices/'.length),'#/devices');if(sameDeviceRef(info.id,draft.deviceId))return;}
                  if(draft.active&&draft.sessionId){requestSingleItemSubmitSessionCancel(draft.deviceId,{closeAfter:true,reason:'WebAdmin 离开页面时取消 itemSubmit 条件会话。'});return;}
                  releaseSingleItemSubmitSessionLock(draft,true);appState.singleItemSubmitSession=null;stopSingleItemSubmitSessionHeartbeat();stopSingleItemSubmitSessionStatusPoll();
                }
                async function startDeviceConfigEdit(deviceId){
                  const lookupId=deviceApiRef(deviceId), encoded=encodeURIComponent(lookupId);
                  const detail=(appState.currentDeviceDetail&&sameDeviceRef(appState.currentDeviceDetail.id,deviceId))?appState.currentDeviceDetail:await api(`/api/devices/${encoded}`);
                  const expectedType=deviceTypeRefPrefix(deviceId);if(expectedType&&String(detail.type||'').toLowerCase()!==expectedType){toast('该位置当前设备类型已变化，目标类型不存在。');return;}
                  const canonicalId=detail.id||lookupId, canonicalEncoded=encodeURIComponent(canonicalId);
                  const isVbdDetail=isVirtualBlockDevice(detail);
                  const [metadataLockRes,basicRes,extendedRes,actionRes,matcherRes,nativeTriggerRes]=await Promise.all([
                    settle(`/api/webadmin/edit-locks/status?targetType=device_metadata&targetId=${canonicalEncoded}`),
                    settle(`/api/webadmin/device-basic-config/${canonicalEncoded}`),
                    isVbdDetail?Promise.resolve({ok:true,data:null}):settle(`/api/webadmin/device-extended-config/${canonicalEncoded}`),
                    isActionRelay(detail)?settle(`/api/webadmin/action-relay-actions/${canonicalEncoded}`):Promise.resolve({ok:true,data:null}),
                    isVbdDetail?settle(`/api/webadmin/interaction-item-matcher/${canonicalEncoded}`):Promise.resolve({ok:true,data:null}),
                    isVbdDetail?settle(`/api/webadmin/virtual-block-devices/${canonicalEncoded}/native-triggers`):Promise.resolve({ok:true,data:null})
                  ]);
                  detail.metadataLock=metadataLockRes.ok?metadataLockRes.data:null;
                  detail.basicConfig=basicRes.ok?basicRes.data:null;
                  detail.basicConfigError=basicRes.ok?null:basicRes.error;
                  detail.extendedConfig=extendedRes.ok?extendedRes.data:null;
                  detail.extendedConfigError=extendedRes.ok?null:extendedRes.error;
                  detail.actionRelayActions=actionRes.ok?actionRes.data:null;
                  detail.actionRelayActionsError=actionRes.ok?null:actionRes.error;
                  detail.interactionItemMatcher=matcherRes.ok?matcherRes.data:null;
                  detail.interactionItemMatcherError=matcherRes.ok?null:matcherRes.error;
                  detail.nativeTriggers=nativeTriggerRes.ok?nativeTriggerRes.data:null;
                  detail.nativeTriggersError=nativeTriggerRes.ok?null:nativeTriggerRes.error;
                  appState.currentDeviceDetail=detail;
                  const lockMessageText=deviceConfigLockMessage(detail);
                  if(lockMessageText){toast(lockMessageText);await renderDeviceDetail(currentDeviceRouteArg(canonicalId),{silent:true});return;}
                  """)
.append("""
                  const session={deviceId:canonicalId,saving:false,errors:[]};let acquired=0, visibleSections=0;
                  try{
                    if(canEditDeviceMetadata()){
                      const result=await acquireWebAdminEditLock('device_metadata',canonicalId);
                      if(result.success){const lock=result.data?.lock||{}, meta=detail.metadata||{};appState.deviceMetadataEdit={deviceId:canonicalId,displayName:meta.displayName||'',note:meta.note||'',iconKey:meta.iconKey||'auto',expectedVersion:Number(meta.version||0),lockId:lock.lockId||'',lock,errors:[],saving:false,conflict:null};markModalInitialSnapshot('device_metadata',appState.deviceMetadataEdit);scheduleDeviceMetadataLockHeartbeat();acquired++;}
                      else session.errors.push({message:result.message||'显示信息编辑锁获取失败'});
                    }
                    if(canEditDeviceBasicConfig()&&basicRes.ok&&basicRes.data?.supported!==false){
                      const result=await acquireWebAdminEditLock('device_basic_config',canonicalId);
                      if(result.success){const lock=result.data?.lock||{}, cfg=basicRes.data||{}, channelOptions=await loadSignalChannelOptions();appState.deviceBasicConfigEdit={deviceId:canonicalId,enabled:!!cfg.enabled,channel:cfg.channel||'',channelOptions,channelOptionsError:appState.channelOptionsError,channelComboOpen:false,channelComboIndex:0,channelComboQuery:'',channelComboSearchActive:false,expectedFingerprint:cfg.expectedFingerprint||'',lockId:lock.lockId||'',lock,errors:[],saving:false,conflict:null};markModalInitialSnapshot('device_basic_config',appState.deviceBasicConfigEdit);scheduleDeviceBasicConfigLockHeartbeat();acquired++;}
                      else session.errors.push({message:result.message||'基础配置编辑锁获取失败'});
                    }
                    if(!isVbdDetail&&canEditDeviceExtendedConfig()&&extendedRes.ok&&extendedRes.data?.supported!==false&&(extendedRes.data?.supportedFields||[]).length){
                      const cfg=extendedRes.data||{}, editable=cfg.editableFields||cfg.supportedFields||[], channelOptions=await loadSignalChannelOptions();let lock={};
                      if(editable.length){
                        const result=await acquireWebAdminEditLock('device_extended_config',canonicalId);
                        if(result.success){lock=result.data?.lock||{};acquired++;}
                        else session.errors.push({message:result.message||'扩展配置编辑锁获取失败'});
                      }
                      appState.deviceExtendedConfigEdit=makeDeviceExtendedConfigDraft(canonicalId,cfg,lock,channelOptions);
                      if(appState.deviceExtendedConfigEdit.lockId)scheduleDeviceExtendedConfigLockHeartbeat();
                      visibleSections++;
                    }
                    if(isActionRelay(detail)&&canEditActionRelayActions()){
                      const draft=await prepareActionRelayActionsDraft(canonicalId,true);
                      if(draft.lockId)acquired++;
                      else session.errors.push({message:draft.errors[0]?.message||draft.unsupportedReason||'Action 列表编辑锁获取失败'});
                    }
                    if(isVirtualBlockDevice(detail)){
                      const nativeDraft=await prepareVbdNativeTriggerDraft(canonicalId,canEditVbdNativeTriggers());
                      if(nativeDraft.lockId)acquired++;
                      else if(nativeDraft.supported)visibleSections++;
                      else session.errors.push({message:nativeDraft.errors[0]?.message||nativeDraft.unsupportedReason||'原生触发配置编辑锁获取失败'});
                      if(nativeDraft.values?.interactionEnabled){
                        const matcherDraft=await prepareInteractionItemMatcherDraft(canonicalId,canEditInteractionItemMatcher());
                        if(matcherDraft.lockId)acquired++;
                        else if(matcherDraft.matcherReadable)visibleSections++;
                        else session.errors.push({message:matcherDraft.errors[0]?.message||matcherDraft.unsupportedReason||'交互物品匹配编辑锁获取失败'});
                      }else{
                        appState.interactionItemMatcherEdit=null;stopInteractionItemMatcherLockHeartbeat();
                      }
                    }
                    if(!acquired&&!visibleSections){await releaseAllDeviceConfigLocks(canonicalId,true);toast(session.errors[0]?.message||'当前设备没有可编辑配置区。');return;}
                    appState.deviceConfigEdit=session;
                    await renderDeviceDetail(currentDeviceRouteArg(canonicalId),{silent:true});
                    showDeviceConfigEditModal(canonicalId);
                  }catch(err){
                    await releaseAllDeviceConfigLocks(canonicalId,true);
                    appState.deviceConfigEdit=null;
                    toast(err.message||'无法打开设备配置编辑器');
                    await renderDeviceDetail(currentDeviceRouteArg(canonicalId),{silent:true});
                  }
                }
                function applyDeviceConfigDraftsFromForm(deviceId){
                  const meta=appState.deviceMetadataEdit;if(meta&&meta.deviceId===deviceId){meta.displayName=document.getElementById('metadata-display-name')?.value||'';meta.note=document.getElementById('metadata-note')?.value||'';meta.iconKey=document.getElementById('metadata-icon')?.value||'auto';}
                  if(appState.deviceBasicConfigEdit&&appState.deviceBasicConfigEdit.deviceId===deviceId)updateDeviceBasicConfigDraftFromForm(deviceId);
                  if(appState.deviceExtendedConfigEdit&&appState.deviceExtendedConfigEdit.deviceId===deviceId)updateDeviceExtendedConfigDraftFromForm(deviceId);
                  if(appState.actionRelayActionsEdit&&appState.actionRelayActionsEdit.deviceId===deviceId)syncActionRelayActionsDraftFromForm(deviceId);
                  if(appState.vbdNativeTriggerEdit&&sameDeviceRef(appState.vbdNativeTriggerEdit.deviceId,deviceId))syncVbdNativeTriggerDraftFromForm(deviceId);
                  if(appState.interactionItemMatcherEdit&&sameDeviceRef(appState.interactionItemMatcherEdit.deviceId,deviceId))syncInteractionItemMatcherDraftFromForm(deviceId);
                }
                """)
.append("""
                async function saveDeviceConfig(deviceId){
                  const session=appState.deviceConfigEdit||{deviceId,errors:[]};
                  applyDeviceConfigDraftsFromForm(deviceId);session.saving=true;session.errors=[];appState.deviceConfigEdit=session;withPreservedModalScroll(()=>showDeviceConfigEditModal(deviceId));
                  let changed=false;
                  try{
                    const meta=appState.deviceMetadataEdit;
                    if(meta&&meta.deviceId===deviceId){
                      const result=await api(`/api/webadmin/device-metadata/${encodeURIComponent(deviceId)}`,{method:'PATCH',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({displayName:meta.displayName,note:meta.note,iconKey:meta.iconKey,expectedVersion:meta.expectedVersion,lockId:meta.lockId})});
                      if(!result.success)return deviceConfigSaveFailed(deviceId,meta,result,'metadata');
                      changed=changed||!!result.changed;appState.deviceMetadataEdit=null;stopDeviceMetadataLockHeartbeat();
                    }
                    const basic=appState.deviceBasicConfigEdit;
                    if(basic&&basic.deviceId===deviceId){
                      const result=await api(`/api/webadmin/device-basic-config/${encodeURIComponent(deviceId)}`,{method:'PATCH',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({enabled:basic.enabled,channel:basic.channel,expectedFingerprint:basic.expectedFingerprint,lockId:basic.lockId})});
                      if(!result.success)return deviceConfigSaveFailed(deviceId,basic,result,'basic');
                      markChannelOptionsDirty({type:'device_config_changed',payload:{targetType:'device_basic_config'}});
                      changed=changed||!!result.changed;appState.deviceBasicConfigEdit=null;stopDeviceBasicConfigLockHeartbeat();
                    }
                    """)
.append("""
                    const ext=appState.deviceExtendedConfigEdit;
                    if(ext&&ext.deviceId===deviceId&&ext.lockId){
                      const result=await api(`/api/webadmin/device-extended-config/${encodeURIComponent(deviceId)}`,{method:'PATCH',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify(deviceExtendedConfigPatchBody(ext))});
                      if(!result.success)return deviceConfigSaveFailed(deviceId,ext,result,'extended');
                      markChannelOptionsDirty({type:'device_config_changed',payload:{targetType:'device_extended_config'}});
                      changed=changed||!!result.changed;appState.deviceExtendedConfigEdit=null;stopDeviceExtendedConfigLockHeartbeat();
                    }else if(ext&&ext.deviceId===deviceId){
                      appState.deviceExtendedConfigEdit=null;
                    }
                    const matcherDraft=appState.interactionItemMatcherEdit, currentDetail=appState.currentDeviceDetail;
                    if(matcherDraft&&sameDeviceRef(matcherDraft.deviceId,deviceId)){
                      if(currentDetail&&sameDeviceRef(currentDetail.id,deviceId)&&!isVirtualBlockDevice(currentDetail)){
                        await releaseInteractionItemMatcherLock(matcherDraft,true);
                        appState.interactionItemMatcherEdit=null;stopInteractionItemMatcherLockHeartbeat();
                      }else{
                      if(matcherDraft.lockId){
                        if(interactionItemMatcherDirty(matcherDraft)){
                          const result=await patchInteractionItemMatcherDraft(deviceId,matcherDraft);
                          if(!result.success)return deviceConfigSaveFailed(deviceId,matcherDraft,result,'interactionItemMatcher');
                          markChannelOptionsDirty({type:'device_config_changed',payload:{targetType:'interaction_item_matcher'}});
                          changed=changed||!!result.changed;
                        }else{
                          await releaseInteractionItemMatcherLock(matcherDraft,true);
                        }
                      }
                      appState.interactionItemMatcherEdit=null;stopInteractionItemMatcherLockHeartbeat();
                      }
                    }
                    """)
.append("""
                    const nativeDraft=appState.vbdNativeTriggerEdit;
                    if(nativeDraft&&sameDeviceRef(nativeDraft.deviceId,deviceId)){
                      if(nativeDraft.lockId&&vbdNativeTriggerDirty(nativeDraft)){
                        const result=await patchVbdNativeTriggerDraft(deviceId,nativeDraft);
                        if(!result.success)return deviceConfigSaveFailed(deviceId,nativeDraft,result,'vbdNativeTriggers');
                        markChannelOptionsDirty({type:'device_config_changed',payload:{targetType:'virtual_block_device_triggers'}});
                        changed=changed||!!result.changed;
                      }else if(nativeDraft.lockId){
                        await releaseVbdNativeTriggerLock(nativeDraft,true);
                      }
                      appState.vbdNativeTriggerEdit=null;stopVbdNativeTriggerLockHeartbeat();
                    }
                    """)
.append("""
                    const actionDraft=appState.actionRelayActionsEdit;
                    if(actionDraft&&actionDraft.deviceId===deviceId){
                      if(actionRelayActionsDirty(actionDraft)){
                        const result=await patchActionRelayActionsDraft(deviceId,actionDraft);
                        if(!result.success)return deviceConfigSaveFailed(deviceId,actionDraft,result,'actionRelayActions');
                        markChannelOptionsDirty({type:'action_changed',payload:{targetType:'action_relay_actions'}});
                        changed=changed||!!result.changed;
                      }else{
                        await releaseActionRelayActionsLock(actionDraft,true);
                      }
                      appState.actionRelayActionsEdit=null;stopActionRelayActionsLockHeartbeat();
                    }
                    appState.deviceConfigEdit=null;await dismissWebAdminModal();toast(changed?'设备配置已保存。':'没有变更。');await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});
                  }catch(err){session.saving=false;session.errors=[{message:err.message||'保存失败'}];appState.deviceConfigEdit=session;toast(err.message||'保存失败');withPreservedModalScroll(()=>showDeviceConfigEditModal(deviceId));}
                }
                function deviceConfigSaveFailed(deviceId,draft,result,section){
                  const session=appState.deviceConfigEdit||{deviceId,errors:[]};
                  draft.saving=false;draft.errors=result.validationErrors&&result.validationErrors.length?result.validationErrors:[{message:result.message||'保存失败'}];draft.conflict=result.code==='conflict_detected'?result.conflict||{remote:true}:null;
                  if(section==='metadata'){appState.deviceMetadataEdit=draft;if(['edit_lock_expired','edit_lock_conflict','edit_lock_required'].includes(result.code))stopDeviceMetadataLockHeartbeat();}
                  if(section==='basic'){appState.deviceBasicConfigEdit=draft;if(['edit_lock_expired','edit_lock_conflict','edit_lock_required'].includes(result.code))stopDeviceBasicConfigLockHeartbeat();}
                  if(section==='extended'){appState.deviceExtendedConfigEdit=draft;if(['edit_lock_expired','edit_lock_conflict','edit_lock_required'].includes(result.code))stopDeviceExtendedConfigLockHeartbeat();}
                  if(section==='actionRelayActions'){appState.actionRelayActionsEdit=draft;if(['edit_lock_expired','edit_lock_conflict','edit_lock_required'].includes(result.code))stopActionRelayActionsLockHeartbeat();}
                  if(section==='vbdNativeTriggers'){if(['edit_lock_expired','edit_lock_conflict','edit_lock_required'].includes(result.code)){draft.lockId='';draft.lock=null;stopVbdNativeTriggerLockHeartbeat();}appState.vbdNativeTriggerEdit=draft;}
                  if(section==='interactionItemMatcher'){if(['edit_lock_expired','edit_lock_conflict','edit_lock_required'].includes(result.code)){draft.lockId='';draft.lock=null;stopInteractionItemMatcherLockHeartbeat();}appState.interactionItemMatcherEdit=draft;}
                  session.saving=false;session.errors=[{message:result.message||'保存失败'}];appState.deviceConfigEdit=session;toast(result.message||'保存失败');if(section==='actionRelayActions')rerenderActionRelayActionsEditor(deviceId);else withPreservedModalScroll(()=>showDeviceConfigEditModal(deviceId));return false;
                }
                """).toString();
    }
}
