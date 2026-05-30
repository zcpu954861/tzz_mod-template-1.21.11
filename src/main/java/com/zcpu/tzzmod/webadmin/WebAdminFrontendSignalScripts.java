package com.zcpu.tzzmod.webadmin;

final class WebAdminFrontendSignalScripts {
    private WebAdminFrontendSignalScripts() {
    }

    static String appJs() {
        return new StringBuilder().append("""
                function channelMetadataCard(detail){
                  const meta=detail.metadata||{}, draft=appState.channelMetadataEdit;
                  const editing=draft&&draft.channel===detail.channel;
                  const canEdit=canEditChannelMetadata(), lock=meta.lockStatus||{};
                  const lockedByOther=!!lock.locked&&!lock.heldByCurrentUser;
                  const lockHint=lockedByOther?`<p class="readonly-note">${esc(lock.holderUsername||'其他用户')} 正在编辑，锁到期：${esc(formatDateTime(lock.expiresAt))}</p>`:'';
                  const action=editing?`<button class="secondary" ${htmlHandler(`showChannelMetadataEditModal(${jsString(detail.channel)})`)}>继续编辑</button>`:(canEdit&&!lockedByOther?`<button class="secondary" ${htmlHandler(`startChannelMetadataEdit(${jsString(detail.channel)})`)}>编辑频道显示信息</button>`:(canEdit?lockHint:'<p class="readonly-note">需要 EDITOR 或 OWNER 权限才能编辑。</p>'));
                  return `<div class="identity-grid">${row('显示名',esc(meta.displayName||'未设置'))}${row('原始频道',esc(detail.channel))}${row('备注',esc(meta.note||'暂无'))}${row('图标',esc(labelMetadataIcon(meta.iconKey||'auto')))}${row('最后修改',fmtTime(meta.updatedAt))}${row('修改人',esc(meta.updatedBy||'暂无'))}</div><p class="muted">此信息仅用于 WebAdmin 展示，不会创建频道，也不会改变 SignalBridge 运行语义。</p>${action}`;
                }
                function channelMetadataForm(detail,draft){
                  const errs=draft.errors?.length?`<ul class="validation-list">${draft.errors.map(e=>`<li>${esc(e.field?`${e.field}：`: '')}${esc(e.message||'保存失败')}</li>`).join('')}</ul>`:'';
                  const conflict=draft.conflict?`<div class="readonly-note">${esc(draft.errors?.[0]?.message||'频道显示信息已发生冲突，请刷新后再编辑。')} <button class="link-button" ${htmlHandler(`reloadChannelMetadataAfterConflict(${jsString(detail.channel)})`)}>刷新当前信息</button></div>`:'';
                  return `<form class="edit-form" ${htmlEvent('onsubmit',`event.preventDefault();saveChannelMetadata(${jsString(detail.channel)})`)}><label>显示名<input id="channel-metadata-display-name" class="input" maxlength="64" value="${esc(draft.displayName||'')}" placeholder="例如：大厅任务提交成功"></label><label>备注<textarea id="channel-metadata-note" maxlength="512" placeholder="仅用于 WebAdmin 展示">${esc(draft.note||'')}</textarea></label><label>图标<select id="channel-metadata-icon" class="select">${metadataIconOptions().map(key=>`<option value="${esc(key)}" ${key===(draft.iconKey||'auto')?'selected':''}>${esc(labelMetadataIcon(key))}</option>`).join('')}</select></label><p class="readonly-note">正在编辑频道显示信息。锁到期：${fmtTime(draft.lock?.expiresAt)}</p>${errs}${conflict}<div class="form-actions"><button class="primary" type="submit" ${draft.saving?'disabled':''}>${draft.saving?'保存中...':'保存'}</button><button class="secondary" type="button" ${htmlHandler('closeWebAdminModal()')}>取消</button></div></form>`;
                }
                function showChannelMetadataEditModal(channel){
                  const draft=appState.channelMetadataEdit;if(!draft||draft.channel!==channel)return;
                  markModalInitialSnapshot('channel_metadata',draft);
                  openWebAdminModal('编辑频道显示信息',channelMetadataForm({channel},draft),editModalFooter(draft.saving),{onClose:()=>cancelChannelMetadataEdit(channel),syncBeforeClose:()=>syncModalDraftBeforeClose('channel_metadata',channel),dirtyCheck:()=>modalDraftDirty('channel_metadata',appState.channelMetadataEdit)});
                }
                function maybeReleaseChannelMetadataEditForRoute(hash){const draft=appState.channelMetadataEdit;if(!draft)return;if(String(hash||'').startsWith('#/signals/')){const info=detailRoute(String(hash).substring('#/signals/'.length),'#/signals');if(info.id===draft.channel)return;}releaseChannelMetadataLock(draft,true);appState.channelMetadataEdit=null;stopChannelMetadataLockHeartbeat();}
                async function startChannelMetadataEdit(channel){
                  if(!canEditChannelMetadata())return;
                  try{
                    const meta=await api(`/api/webadmin/channel-metadata?channel=${encodeURIComponent(channel)}`);
                    const result=await api('/api/webadmin/edit-locks/acquire',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'channel_metadata',targetId:meta.channel||channel})});
                    if(!result.success){toast(result.message||'无法获取编辑锁');await renderSignalDetail(encodeURIComponent(channel),{silent:true});return;}
                    const lock=result.data?.lock||{};
                    appState.channelMetadataEdit={channel:meta.channel||channel,displayName:meta.displayName||'',note:meta.note||'',iconKey:meta.iconKey||'auto',expectedFingerprint:meta.expectedFingerprint||'',lockId:lock.lockId||'',lock,errors:[],saving:false,conflict:null};
                    markModalInitialSnapshot('channel_metadata',appState.channelMetadataEdit);
                    scheduleChannelMetadataLockHeartbeat();
                    await renderSignalDetail(encodeURIComponent(channel),{silent:true});
                    showChannelMetadataEditModal(meta.channel||channel);
                  }catch(err){toast(err.message||'无法获取编辑锁');}
                }
                async function cancelChannelMetadataEdit(channel){const draft=appState.channelMetadataEdit;if(draft&&draft.channel===channel){await releaseChannelMetadataLock(draft,false);appState.channelMetadataEdit=null;stopChannelMetadataLockHeartbeat();}await dismissWebAdminModal();await renderSignalDetail(encodeURIComponent(channel),{silent:true});}
                async function reloadChannelMetadataAfterConflict(channel){const draft=appState.channelMetadataEdit;if(draft&&draft.channel===channel)await releaseChannelMetadataLock(draft,true);appState.channelMetadataEdit=null;stopChannelMetadataLockHeartbeat();await renderSignalDetail(encodeURIComponent(channel),{silent:true});}
                function scheduleChannelMetadataLockHeartbeat(){stopChannelMetadataLockHeartbeat();appState.channelMetadataLockTimer=setTimeout(async()=>{await heartbeatChannelMetadataLock();if(appState.channelMetadataEdit)scheduleChannelMetadataLockHeartbeat();},30000);}
                function stopChannelMetadataLockHeartbeat(){if(appState.channelMetadataLockTimer){clearTimeout(appState.channelMetadataLockTimer);appState.channelMetadataLockTimer=null;}}
                async function heartbeatChannelMetadataLock(){const draft=appState.channelMetadataEdit;if(!draft||!draft.lockId)return;try{const result=await api('/api/webadmin/edit-locks/heartbeat',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'channel_metadata',targetId:draft.channel,lockId:draft.lockId})});if(result.success){draft.lock=result.data?.lock||draft.lock;return;}draft.errors=[{message:result.message||'编辑锁续期失败'}];stopChannelMetadataLockHeartbeat();await renderSignalDetail(encodeURIComponent(draft.channel),{silent:true});}catch(err){draft.errors=[{message:err.message||'编辑锁续期失败'}];stopChannelMetadataLockHeartbeat();}}
                async function releaseChannelMetadataLock(draft,silent){if(!draft||!draft.lockId)return;try{await api('/api/webadmin/edit-locks/release',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'channel_metadata',targetId:draft.channel,lockId:draft.lockId})});}catch(err){if(!silent)toast(err.message||'编辑锁释放失败，将等待自动过期。');}}
                async function saveChannelMetadata(channel){
                  const draft=appState.channelMetadataEdit||{channel};
                  draft.displayName=document.getElementById('channel-metadata-display-name')?.value||'';
                  draft.note=document.getElementById('channel-metadata-note')?.value||'';
                  draft.iconKey=document.getElementById('channel-metadata-icon')?.value||'auto';
                  draft.saving=true;draft.errors=[];draft.conflict=null;appState.channelMetadataEdit=draft;renderSignalDetail(encodeURIComponent(channel),{silent:true});
                  try{
                    const result=await api(`/api/webadmin/channel-metadata?channel=${encodeURIComponent(channel)}`,{method:'PATCH',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({displayName:draft.displayName,note:draft.note,iconKey:draft.iconKey,expectedFingerprint:draft.expectedFingerprint,lockId:draft.lockId})});
                    if(result.success){appState.channelMetadataEdit=null;stopChannelMetadataLockHeartbeat();await dismissWebAdminModal();toast(result.changed?(result.message||'频道显示信息已保存。'):'没有变更。');await renderSignalDetail(encodeURIComponent(channel),{silent:true});return;}
                    draft.saving=false;draft.errors=result.validationErrors&&result.validationErrors.length?result.validationErrors:[{message:result.message||'保存失败'}];draft.conflict=result.conflict||null;appState.channelMetadataEdit=draft;if(['edit_lock_expired','edit_lock_conflict','edit_lock_required'].includes(result.code))stopChannelMetadataLockHeartbeat();toast(result.message||'保存失败');await renderSignalDetail(encodeURIComponent(channel),{silent:true});showChannelMetadataEditModal(channel);
                  }catch(err){draft.saving=false;draft.errors=[{message:err.message||'保存失败'}];appState.channelMetadataEdit=draft;toast(err.message||'保存失败');await renderSignalDetail(encodeURIComponent(channel),{silent:true});showChannelMetadataEditModal(channel);}
                }
                function maybeReleaseDeviceBasicConfigEditForRoute(hash){const draft=appState.deviceBasicConfigEdit;if(!draft)return;if(String(hash||'').startsWith('#/devices/')){const info=detailRoute(String(hash).substring('#/devices/'.length),'#/devices');if(sameDeviceRef(info.id,draft.deviceId))return;}releaseDeviceBasicConfigLock(draft,true);appState.deviceBasicConfigEdit=null;stopDeviceBasicConfigLockHeartbeat();}
                async function startDeviceBasicConfigEdit(deviceId,enabled,channel,expectedFingerprint){
                  if(!canEditDeviceBasicConfig())return;
                  try{
                    const result=await api('/api/webadmin/edit-locks/acquire',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'device_basic_config',targetId:deviceId})});
                    if(!result.success){toast(result.message||'无法获取编辑锁');await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});return;}
                    const lock=result.data?.lock||{};
                    const channelOptions=await loadSignalChannelOptions();
                    appState.deviceBasicConfigEdit={deviceId,enabled:!!enabled,channel:channel||'',channelOptions,channelOptionsError:appState.channelOptionsError,channelComboOpen:false,channelComboIndex:0,channelComboQuery:'',channelComboSearchActive:false,expectedFingerprint:expectedFingerprint||'',lockId:lock.lockId||'',lock,errors:[],saving:false,conflict:null};
                    markModalInitialSnapshot('device_basic_config',appState.deviceBasicConfigEdit);
                    scheduleDeviceBasicConfigLockHeartbeat();
                    await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});
                    showDeviceBasicConfigEditModal(deviceId);
                  }catch(err){toast(err.message||'无法获取编辑锁');}
                }
                async function cancelDeviceBasicConfigEdit(deviceId){const draft=appState.deviceBasicConfigEdit;if(draft&&draft.deviceId===deviceId){await releaseDeviceBasicConfigLock(draft,false);appState.deviceBasicConfigEdit=null;stopDeviceBasicConfigLockHeartbeat();}await dismissWebAdminModal();await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});}
                async function reloadDeviceBasicConfigAfterConflict(deviceId){const draft=appState.deviceBasicConfigEdit;if(draft&&draft.deviceId===deviceId)await releaseDeviceBasicConfigLock(draft,true);appState.deviceBasicConfigEdit=null;stopDeviceBasicConfigLockHeartbeat();await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});}
                function scheduleDeviceBasicConfigLockHeartbeat(){stopDeviceBasicConfigLockHeartbeat();appState.deviceBasicConfigLockTimer=setTimeout(async()=>{await heartbeatDeviceBasicConfigLock();if(appState.deviceBasicConfigEdit)scheduleDeviceBasicConfigLockHeartbeat();},30000);}
                function stopDeviceBasicConfigLockHeartbeat(){if(appState.deviceBasicConfigLockTimer){clearTimeout(appState.deviceBasicConfigLockTimer);appState.deviceBasicConfigLockTimer=null;}}
                async function heartbeatDeviceBasicConfigLock(){const draft=appState.deviceBasicConfigEdit;if(!draft||!draft.lockId)return;try{const result=await api('/api/webadmin/edit-locks/heartbeat',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'device_basic_config',targetId:draft.deviceId,lockId:draft.lockId})});if(result.success){draft.lock=result.data?.lock||draft.lock;appState.deviceBasicConfigEdit=draft;return;}draft.errors=[{message:result.message||'编辑锁续期失败'}];appState.deviceBasicConfigEdit=draft;stopDeviceBasicConfigLockHeartbeat();await renderDeviceDetail(currentDeviceRouteArg(draft.deviceId),{silent:true});}catch(err){draft.errors=[{message:err.message||'编辑锁续期失败'}];appState.deviceBasicConfigEdit=draft;stopDeviceBasicConfigLockHeartbeat();}}
                async function releaseDeviceBasicConfigLock(draft,silent){if(!draft||!draft.lockId)return;try{await api('/api/webadmin/edit-locks/release',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'device_basic_config',targetId:draft.deviceId,lockId:draft.lockId})});}catch(err){if(!silent)toast(err.message||'编辑锁释放失败，将等待自动过期。');}}
                async function saveDeviceBasicConfig(deviceId){
                  const draft=appState.deviceBasicConfigEdit||{deviceId};
                  draft.enabled=(document.getElementById('basic-enabled')?.value||'false')==='true';
                  draft.channel=document.getElementById('basic-channel')?.value||'';
                  draft.saving=true;draft.errors=[];draft.conflict=null;appState.deviceBasicConfigEdit=draft;renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});
                  try{
                    const result=await api(`/api/webadmin/device-basic-config/${encodeURIComponent(deviceId)}`,{method:'PATCH',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({enabled:draft.enabled,channel:draft.channel,expectedFingerprint:draft.expectedFingerprint,lockId:draft.lockId})});
                    if(result.success){markChannelOptionsDirty({type:'device_config_changed',payload:{targetType:'device_basic_config'}});appState.deviceBasicConfigEdit=null;stopDeviceBasicConfigLockHeartbeat();await dismissWebAdminModal();toast(result.changed?(result.message||'设备基础配置已保存。'):'没有变更。');await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});return;}
                    draft.saving=false;draft.errors=result.validationErrors&&result.validationErrors.length?result.validationErrors:[{message:result.message||'保存失败'}];draft.conflict=result.conflict||null;appState.deviceBasicConfigEdit=draft;if(['edit_lock_expired','edit_lock_conflict','edit_lock_required'].includes(result.code))stopDeviceBasicConfigLockHeartbeat();toast(result.message||'保存失败');await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});showDeviceBasicConfigEditModal(deviceId);
                  }catch(err){
                    draft.saving=false;draft.errors=[{message:err.message||'保存失败'}];appState.deviceBasicConfigEdit=draft;toast(err.message||'保存失败');await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});showDeviceBasicConfigEditModal(deviceId);
                  }
                }
                """)
.append("""
                function chainPreview(detail){if(isBlank(detail.channel))return '<span class="muted">当前设备没有主频道。</span>';return `<div class="chain-row"><strong>${esc(detail.displayName)}</strong><span class="muted">→ 主频道：${esc(detail.channel)}</span><span class="muted">→ 可在频道详情页查看消费者与最近事件</span></div>`}
                function debugChecks(data){const checks=data?.checks||[];if(checks.length===0)return empty('当前设备暂无 debug 数据。');return `<div class="list-stack">${checks.map(c=>`<div class="check-row-card"><strong>${pill(c.status)} ${esc(debugTitle(c))}</strong><span class="muted">${esc(debugMessage(c))}</span></div>`).join('')}</div>`}
                function debugTitle(c){const name=String(c?.name||'');if(name.includes('_')&&!isBlank(c?.message))return localizeCheckMessage(c);return localizeCheckName(name);}
                function debugMessage(c){const name=String(c?.name||'');if(name.includes('_')&&!isBlank(c?.message))return '';return localizeCheckMessage(c);}
                function localizeCheckName(name){return {enabled:'设备状态',channel:'主频道',block_id:'方块 ID',blockId:'方块 ID'}[String(name||'')]||name||'检查项';}
                function localizeCheckMessage(c){const text=String(c?.message||'');if(text==='Device is enabled.')return'当前设备处于启用状态。';if(text==='Device is disabled.')return'当前设备处于禁用状态。';if(text==='Primary channel is empty.')return'当前设备没有设置主频道。';return text.replace('Device is enabled.','当前设备处于启用状态。').replace('Primary channel is empty.','当前设备没有设置主频道。');}
                function configSummary(detail){const obj=detail?.configSummary||{};if(!obj||Object.keys(obj).length===0)return empty('当前设备暂无配置摘要。');const cfg=obj, item=cfg.interactionItem||{};let html='';
                  html+=configGroup('基础配置',[
                    ['短 ID',shortId(detail.id||cfg.shortId)],
                    ['设备类型',labelType(detail.type)],
                    ['方块 ID',cfg.blockId],
                    ['工作模式',cfg.mode],
                    ['冷却时间',formatTicks(cfg.cooldownTicks)],
                    ['脉冲时间',formatTicks(cfg.pulseTicks)]
                  ]);
                  html+=configGroup('信号配置',[
                    ['主频道',labelChannel(detail.channel)],
                    ['成功频道',item.successChannel],
                    ['失败频道',item.failChannel],
                    ['动作数量',cfg.actionCount]
                  ]);
                  html+=configGroup('交互配置',[
                    ['普通交互',cfg.interactionEnabled?'已启用':''],
                    ['物品匹配',item.enabled?'已启用':''],
                    ['物品来源',item.sourceDisplayName||labelInteractionSource(item.source)],
                    ['原版交互策略',item.vanillaPolicyDisplayName||labelVanillaPolicy(item.vanillaPolicy)],
                    ['消耗策略',item.consumeEnabled?`${item.consumeCount||1} 个，${item.consumeSourceDisplayName||labelConsumeSource(item.consumeSource)}`:''],
                    ['背包消耗顺序',item.consumeEnabled?(item.inventoryConsumeOrderDisplayName||labelConsumeOrder(item.inventoryConsumeOrder)):''],
                    ['物品模板',item.templateSummary],
                    ['itemSubmit 条件层',(()=>{const n=Number(cfg.itemSubmitRequirementCount||0);if(!cfg.interactionEnabled&&n>0)return `右键交互未启用，仅保留警告：${n===1?'单物品提交':`多物品提交 ${n} 个条件`}`;if(n<=0)return '未配置 itemSubmit';return n===1?'单物品提交':`多物品提交：${n} 个条件`;})()]
                  ]);
                  html+=configGroup('容器配置',[
                    ['容器事件',cfg.containerEnabled?'已启用':''],
                    ['物品条件数量',cfg.itemConditionCount]
                  ]);
                  return `<div class="wa-config-stack">${html || empty('当前设备没有可展示的关键配置。')}</div>`;
                }
                function configGroup(title,rows){const filtered=(rows||[]).filter(([_,v])=>isMeaningful(v));if(filtered.length===0)return '';return `<section class="wa-config-card"><h3>${esc(title)}</h3><div class="identity-grid">${filtered.map(([k,v])=>row(k,esc(v))).join('')}</div></section>`}
                function configSection(title,rows,open=false){const filtered=(rows||[]).filter(([_,v])=>isMeaningful(v));if(filtered.length===0)return '';return `<details class="config-section" ${open?'open':''}><summary>${esc(title)}</summary><div class="list-stack">${filtered.map(([k,v])=>`<div class="kv-row"><span class="muted">${esc(k)}</span><strong>${esc(v)}</strong></div>`).join('')}</div></details>`}
                function isMeaningful(v){if(v===undefined||v===null)return false;if(typeof v==='number')return v!==0;if(typeof v==='boolean')return v;if(Array.isArray(v))return v.length>0;return String(v).trim()!==''&&String(v).trim()!=='-'&&String(v).trim()!=='未设置';}
                function formatTicks(value){const n=Number(value||0);return n>0?`${n} tick`:'';}
                function flatten(obj,prefix=''){const out=[];for(const [k,v] of Object.entries(obj||{})){const key=prefix?`${prefix}.${k}`:k;if(v&&typeof v==='object'&&!Array.isArray(v)){out.push(...flatten(v,key));}else{out.push([key,Array.isArray(v)?`${v.length} 项`:(v ?? '')]);}}return out;}
                function uniqueIssues(items){const seen=new Set();return (items||[]).filter(i=>{const key=i.id||`${i.title}:${i.relatedObjectId}`;if(seen.has(key))return false;seen.add(key);return true;});}
                function shortId(id){return String(id||'').length>12?String(id).slice(0,8):String(id||'');}
                """)
.append("""
                async function renderDoctorPage(options={}){
                  if(!options.silent)setView(loading('正在加载 Doctor 诊断...'));
                  let report;try{report=await api('/api/doctor')}catch(err){if(options.silent){toast('Doctor 诊断实时刷新失败，已保留当前页面。');return;}setView(errorBlock(err.message));return;}
                  appState.doctorReport=report||{summary:{},issues:[]};
                  renderDoctorList('',options);
                }
                function renderDoctorList(focusId,options={}){
                  waEnsureState();
                  const report=appState.doctorReport||{summary:{},issues:[]}, issues=report.issues||[], filtered=filterDoctorIssues(issues), summary=report.summary||{};
                  const total=issues.length, errors=summary.errorCount??issues.filter(i=>String(i.severity||'').toUpperCase()==='ERROR').length, warnings=summary.warningCount??issues.filter(i=>String(i.severity||'').toUpperCase()==='WARNING').length, infos=summary.infoCount??issues.filter(i=>String(i.severity||'').toUpperCase()==='INFO').length;
                  const score=Math.max(0,Math.round(100-errors*28-warnings*12-infos*3));
                  const page=waPageItems('doctor',filtered,10);
                  const rendered=setView(`<section class="wa-page">
                    ${waPageHead('信号诊断 / Doctor','只读查看 Signal、设备、监听器、动作和区域诊断；自动修复与清空问题均未开放。',`${waButton('自动修复','critical-issue','disabled','danger')}${waButton('清空问题','channel-error','disabled','ghost')}${waButton('导出报告','download','disabled','ghost')}`)}
                    <section class="wa-card-grid wa-metrics-5">
                      ${waMetric('总问题',total,'来自 /api/doctor','doctor-overview',total?'warning':'ok')}
                      ${waMetric('严重问题',errors,'severity=ERROR','critical-issue',errors?'error':'ok')}
                      ${waMetric('警告问题',warnings,'severity=WARNING','warning-issue',warnings?'warning':'ok')}
                      ${waMetric('信息提示',infos,'severity=INFO','info-issue')}
                      ${waMetric('健康度评分',`${score}%`,'只读前端估算','doctor-ok',score>=80?'ok':(score>=60?'warning':'error'))}
                    </section>
                    <section class="wa-two-column">
                      <div class="wa-table-card">
                        <div class="wa-filter-bar">
                          <label class="filter-field search-control"><span>搜索</span><input class="input" id="doctor-search" placeholder="搜索标题 / 对象 / 频道 / 建议" value="${esc(appState.doctorFilters.search)}"></label>
                          ${doctorFilterSelect('严重级别','doctor-severity',['ALL','ERROR','WARNING','INFO'],appState.doctorFilters.severity)}
                          ${doctorFilterSelect('对象类型','doctor-object',['ALL','DEVICE','CHANNEL','LISTENER','SIGNAL_JOIN','TIMER','CONDITION_GROUP','STATE_VARIABLE','SIGNAL_LISTENER_ACTION','ACTION_RELAY_ACTION','REGION_CONTROLLER_ACTION','TIMER_ACTION','SNAPSHOT','TEMPLATE','RECEIVER','ACTION_RELAY','ACTION','REGION','SYSTEM','UNKNOWN'],appState.doctorFilters.objectType)}
                          ${doctorFilterSelect('范围筛选','doctor-jump',['ALL','HAS_TARGET','NO_TARGET'],appState.doctorFilters.jump)}
                          ${waButton('刷新','refresh','onclick="renderDoctorPage()"','ghost')}
                        </div>
                        ${page.items.length?doctorTable(page.items):empty(issues.length?'没有匹配当前筛选条件的诊断问题。':'当前没有诊断问题。')}
                        ${waPagination('doctor',page)}
                      </div>
                      ${doctorSummaryPanel(issues,filtered)}
                    </section>
                  </section>`,options);
                  if(rendered)bindDoctorFilters(focusId);
                }
                function doctorFilterSelect(label,id,options,value){return `<label class="filter-field"><span>${esc(label)}</span><select class="select" id="${id}">${options.map(o=>`<option value="${esc(o)}" ${o===value?'selected':''}>${esc(doctorOptionLabel(o))}</option>`).join('')}</select></label>`}
                function doctorOptionLabel(v){return {ALL:'全部',ERROR:'错误',WARNING:'警告',INFO:'信息',DEVICE:'设备',CHANNEL:'频道',LISTENER:'监听器',SIGNAL_JOIN:'信号汇合',TIMER:'计时器',CONDITION_GROUP:'条件组',STATE_VARIABLE:'状态变量',SIGNAL_LISTENER_ACTION:'监听器单条 Action',ACTION_RELAY_ACTION:'继电器单条 Action',REGION_CONTROLLER_ACTION:'区域单条 Action',TIMER_ACTION:'Timer 单条 Action',SNAPSHOT:'配置时间轴',TEMPLATE:'模板',RECEIVER:'接收器',ACTION_RELAY:'动作继电器',ACTION:'动作',REGION:'区域',SYSTEM:'系统',UNKNOWN:'未知',HAS_TARGET:'有跳转目标',NO_TARGET:'无跳转目标'}[v]||v;}
                function bindDoctorFilters(focusId){
                  const update=(event)=>{appState.doctorFilters.search=document.getElementById('doctor-search').value;appState.doctorFilters.severity=document.getElementById('doctor-severity').value;appState.doctorFilters.objectType=document.getElementById('doctor-object').value;appState.doctorFilters.jump=document.getElementById('doctor-jump').value;appState.uiPages.doctor=1;renderDoctorList(event.target.id);};
                  ['doctor-search','doctor-severity','doctor-object','doctor-jump'].forEach(id=>document.getElementById(id).addEventListener(id==='doctor-search'?'input':'change',update));
                  if(focusId){const el=document.getElementById(focusId);if(el){el.focus();if(el.setSelectionRange&&el.value){el.setSelectionRange(el.value.length,el.value.length);}}}
                }
                function filterDoctorIssues(items){const f=appState.doctorFilters;return (items||[]).filter(i=>{const hay=[i.title,i.message,i.relatedObjectName,i.relatedObjectId,i.channel,i.suggestion,i.code,i.id].join(' ').toLowerCase();if(f.search&&!hay.includes(f.search.toLowerCase()))return false;if(f.severity!=='ALL'&&String(i.severity||'').toUpperCase()!==f.severity)return false;if(f.objectType!=='ALL'&&String(i.relatedObjectType||'UNKNOWN').toUpperCase()!==f.objectType)return false;const hasTarget=!isBlank(i.navigationTarget)||!isBlank(i.channel);if(f.jump==='HAS_TARGET'&&!hasTarget)return false;if(f.jump==='NO_TARGET'&&hasTarget)return false;return true;});}
                function doctorTable(items){return `<div class="wa-table-scroll"><table class="wa-table"><thead><tr><th>问题类型</th><th>对象</th><th>级别</th><th>描述</th><th>发现时间</th><th>操作</th></tr></thead><tbody>${items.map(i=>`<tr><td><span class="device-name"><span class="device-icon">${icon(doctorIssueIcon(i.severity))}</span><span><strong>${issueTitle(i)}</strong><span class="device-subtitle">${esc(i.id||i.code||'unknown')}</span></span></span></td><td><strong>${esc(i.relatedObjectName||i.relatedObjectId||'暂无')}</strong><span class="device-subtitle">${esc(labelObjectType(i.relatedObjectType))}${isBlank(i.channel)?'':` · ${esc(i.channel)}`}</span></td><td>${pill(i.severity)}</td><td><span>${issueMessage(i)}</span><span class="device-subtitle">${issueSuggestion(i)}</span></td><td>${fmtTime(i.detectedAt||i.createdAt||'')}</td><td><div class="wa-action-cell">${issueNavigation(i)}<button class="wa-btn ghost" disabled>自动修复</button>${waIconButton('导出不可用','download','disabled')}</div></td></tr>`).join('')}</tbody></table></div>`;}
                function doctorSummaryPanel(issues,filtered){if(!issues||issues.length===0)return `<aside class="wa-right-rail"><article class="wa-panel"><h2>问题分布</h2>${empty('当前没有诊断问题。')}</article><article class="wa-panel"><h2>快速操作</h2><div class="wa-quick-grid">${waButton('自动修复','critical-issue','disabled','danger')}${waButton('清空问题','channel-error','disabled','ghost')}${waButton('导出报告','download','disabled','ghost')}</div><p class="wa-disabled-note">Doctor 自动修复、清空与报告导出没有完整后端支持，本轮保持禁用。</p></article></aside>`;const current=filtered||issues, jumpTargets=issues.filter(i=>!isBlank(i.navigationTarget)||!isBlank(i.channel)).length;return `<aside class="wa-right-rail"><article class="wa-panel"><h2>问题分布</h2>${progressList(distributionItems(current,i=>String(i.severity||'UNKNOWN').toUpperCase(),labelStatus,Math.max(1,current.length)))}</article><article class="wa-panel"><h2>范围筛选</h2>${progressList(distributionItems(current,i=>String(i.relatedObjectType||'UNKNOWN').toUpperCase(),labelObjectType,Math.max(1,current.length)))}</article><article class="wa-panel"><h2>快速操作</h2><div class="wa-quick-grid"><button class="wa-btn ghost" onclick="appState.doctorFilters.severity='ERROR';appState.uiPages.doctor=1;renderDoctorList()">${icon('critical-issue')}<span>仅错误</span></button><button class="wa-btn ghost" onclick="appState.doctorFilters.jump='HAS_TARGET';appState.uiPages.doctor=1;renderDoctorList()">${icon('signalbridge-main')}<span>有跳转目标</span></button>${waButton('自动修复','critical-issue','disabled','danger')}${waButton('导出报告','download','disabled','ghost')}</div><p class="wa-disabled-note">可跳转问题 ${esc(jumpTargets)} 个。自动修复、清空问题和导出报告均未接入完整后端能力，不发送写请求。</p></article><article class="wa-panel"><h2>最近问题</h2><div class="list-stack">${issues.slice(0,5).map(i=>`<div class="event-row"><strong>${pill(i.severity)} ${issueTitle(i)}</strong><span class="meta">${esc(issueContext(i))}</span><span>${issueSuggestion(i)}</span></div>`).join('')}</div></article></aside>`;}
                function doctorIssueIcon(severity){const s=String(severity||'').toUpperCase();return s==='ERROR'?'doctor-error':(s==='WARNING'?'doctor-warning':'doctor-ok');}
                """)
.append("""
                async function renderUsersPage(options={}){
                  if(!options.silent)setView(loading('正在加载用户管理...'));
                  let data;try{data=await api('/api/webadmin/users')}catch(err){if(options.silent){toast('用户管理实时刷新失败，已保留当前页面。');return;}setView(`<div class="page-head"><div><h1>用户管理</h1><p>查看 WebAdmin 用户、角色、状态与登录情况</p></div><span class="pill info">只读模式</span></div>${err.status===403?errorBlock('权限不足：只有所有者可以查看用户管理。'):errorBlock(err.message)}`);return;}
                  appState.usersData=data||{summary:{},users:[],roles:[]};
                  renderUserList('',options);
                }
                function renderUserList(focusId,options={}){
                  const data=appState.usersData||{summary:{},users:[],roles:[]}, users=data.users||[], summary=data.summary||{}, filtered=filterUsers(users);
                  if(setView(`
                    <div class="page-head"><div><h1>用户管理</h1><p>查看 WebAdmin 用户、角色、状态与登录情况</p></div><span class="pill info">只读模式</span></div>
                    <section class="card-grid">
                      ${metric('用户总数',summary.totalCount ?? users.length,'','user')}
                      ${metric('在线用户',summary.onlineCount ?? users.filter(u=>u.online).length,'','ok')}
                      ${metric('所有者',summary.ownerCount ?? users.filter(u=>u.role==='OWNER').length,'','user')}
                      ${metric('编辑者',summary.editorCount ?? users.filter(u=>u.role==='EDITOR').length,'','settings')}
                      ${metric('测试者',summary.testerCount ?? users.filter(u=>u.role==='TESTER').length,'','action')}
                      ${metric('查看者',summary.viewerCount ?? users.filter(u=>u.role==='VIEWER').length,'','device')}
                      ${metric('禁用用户',summary.disabledCount ?? users.filter(u=>!u.enabled).length,(summary.disabledCount||0)>0?'warning':'','warning')}
                    </section>
                    <div class="toolbar">
                      <input class="input" id="user-search" placeholder="搜索用户名" value="${esc(appState.userFilters.search)}">
                      ${userFilterSelect('角色','user-role',['ALL','OWNER','EDITOR','TESTER','VIEWER'],appState.userFilters.role)}
                      ${userFilterSelect('状态','user-enabled',['ALL','ENABLED','DISABLED'],appState.userFilters.enabled)}
                      ${userFilterSelect('在线状态','user-online',['ALL','ONLINE','OFFLINE'],appState.userFilters.online)}
                    </div>
                    <section class="content-grid">
                      <article class="panel-card">${filtered.length?userTable(filtered):empty(users.length?'没有匹配当前筛选条件的用户。':'暂无 WebAdmin 用户。')}</article>
                      <aside class="panel-card"><h2>角色与安全说明</h2>${roleSummary(data.roles||[])}${securityTips()}</aside>
                    </section>`,options))bindUserFilters(focusId);
                }
                function userFilterSelect(label,id,options,value){return `<label class="filter-field"><span>${esc(label)}</span><select class="select" id="${id}">${options.map(o=>`<option value="${esc(o)}" ${o===value?'selected':''}>${esc(userOptionLabel(o))}</option>`).join('')}</select></label>`}
                function userOptionLabel(v){return {ALL:'全部',OWNER:'所有者',EDITOR:'编辑者',TESTER:'测试者',VIEWER:'查看者',ENABLED:'启用',DISABLED:'禁用',ONLINE:'在线',OFFLINE:'离线'}[v]||v;}
                function bindUserFilters(focusId){
                  const update=(event)=>{appState.userFilters.search=document.getElementById('user-search').value;appState.userFilters.role=document.getElementById('user-role').value;appState.userFilters.enabled=document.getElementById('user-enabled').value;appState.userFilters.online=document.getElementById('user-online').value;renderUserList(event.target.id);};
                  ['user-search','user-role','user-enabled','user-online'].forEach(id=>document.getElementById(id).addEventListener(id==='user-search'?'input':'change',update));
                  if(focusId){const el=document.getElementById(focusId);if(el){el.focus();if(el.setSelectionRange&&el.value){el.setSelectionRange(el.value.length,el.value.length);}}}
                }
                function filterUsers(users){const f=appState.userFilters;return (users||[]).filter(u=>{const hay=[u.username,u.displayName,u.role,u.createdBy].join(' ').toLowerCase();if(f.search&&!hay.includes(f.search.toLowerCase()))return false;if(f.role!=='ALL'&&String(u.role||'').toUpperCase()!==f.role)return false;if(f.enabled==='ENABLED'&&!u.enabled)return false;if(f.enabled==='DISABLED'&&u.enabled)return false;if(f.online==='ONLINE'&&!u.online)return false;if(f.online==='OFFLINE'&&u.online)return false;return true;});}
                function userTable(users){return `<div class="table-wrap"><table class="data-table"><thead><tr><th>用户名</th><th>角色</th><th>状态</th><th>在线状态</th><th>Session</th><th>最后登录</th><th>创建时间</th><th>创建者</th><th>说明</th></tr></thead><tbody>${users.map(u=>`<tr><td><span class="device-name"><span class="device-icon">${icon('user')}</span><span><strong>${esc(u.displayName||u.username)}</strong><span class="device-subtitle">用户名：${esc(u.username)}</span></span></span></td><td>${esc(labelRoleFull(u.role))}</td><td>${textPill(labelEnabledState(u.enabled),u.enabled?'ok':'warning')}</td><td>${textPill(labelOnline(u.online),u.online?'ok':'info')}</td><td>${esc(Number(u.sessionCount||0))}</td><td>${fmtTime(u.lastLoginAt)}</td><td>${fmtTime(u.createdAt)}</td><td>${esc(u.createdBy||'暂无')}</td><td>${u.forcePasswordChange?'<span class="pill warning">需首次改密</span>':'<span class="muted">暂无备注</span>'}</td></tr>`).join('')}</tbody></table></div>`;}
                function roleSummary(roles){const items=(roles&&roles.length?roles:[{role:'OWNER',displayName:'所有者（OWNER）',count:0},{role:'EDITOR',displayName:'编辑者（EDITOR）',count:0},{role:'TESTER',displayName:'测试者（TESTER）',count:0},{role:'VIEWER',displayName:'查看者（VIEWER）',count:0}]);return `<div class="list-stack">${items.map(r=>`<div class="kv-row"><span class="muted">${esc(r.displayName||labelRoleFull(r.role))}</span><strong>${esc(r.count ?? 0)}</strong></div>`).join('')}</div><h3>角色说明</h3><div class="list-stack"><div class="event-row"><strong>所有者</strong><span>完整管理权限。</span></div><div class="event-row"><strong>编辑者</strong><span>未来用于编辑配置。</span></div><div class="event-row"><strong>测试者</strong><span>未来用于测试触发。</span></div><div class="event-row"><strong>查看者</strong><span>只读查看。</span></div></div>`}
                function securityTips(){return `<h3>安全提示</h3><div class="list-stack"><div class="event-row"><span>密码不会明文保存，服务端使用 PBKDF2 哈希。</span></div><div class="event-row"><span>WebAdmin 用户按当前世界 / 存档目录隔离存储。</span></div><div class="event-row"><span>请只给可信协作者创建账号，多人访问建议配合可信网络、防火墙或反向代理。</span></div><div class="event-row"><span>6.5 页面只读展示，不提供重置密码、禁用、删除或踢出 session。</span></div></div>`}
                function storagePanel(storage,visibility){const restricted=storage.restricted||visibility.sensitiveStorageHidden;const hidden='受限信息已隐藏';return `<div class="identity-grid">${row('存储作用域',esc(storage.scope||'WORLD_SAVE'))}${row('按世界隔离',esc(storage.worldScoped?'是':'否'))}${row('WebAdmin 存储目录',esc(restricted?hidden:(storage.directory||'暂无')))}${row('配置文件',esc(restricted?hidden:(storage.configPath||'暂无')))}${row('用户文件',esc(restricted?hidden:(storage.usersPath||'暂无')))}${row('审计日志',esc(restricted?hidden:(storage.auditLogPath||'暂无')))}${row('配置文件存在',esc(storage.configExists?'是':'否'))}${row('用户文件存在',esc(storage.usersExists?'是':'否'))}${row('审计日志存在',esc(storage.auditLogExists?'是':'否'))}${row('旧全局文件提示',esc(storage.legacyGlobalFilesDetected?'检测到旧 config/tzz WebAdmin 文件，但不会自动加载':'未检测到旧全局文件'))}</div><p class="muted">WebAdmin 持久化文件统一放在当前世界 / 存档目录下的 tzz/webadmin/，不再使用全局 config/tzz。</p>`}
                function labelAuthMode(value){return {USERNAME_PASSWORD:'用户名 / 密码'}[String(value||'').toUpperCase()]||value||'暂无';}
                function labelServerType(value){return {DEDICATED:'专用服务器（DEDICATED）',INTEGRATED:'集成服务器（INTEGRATED）'}[String(value||'').toUpperCase()]||value||'暂无';}
                function formatMinutes(value){const n=Number(value||0);return n>0?`${n} 分钟`:'暂无';}
                function uniqueValues(items){return [...new Set(items)].sort((a,b)=>String(a).localeCompare(String(b)));}
                function regionHash(id){return `#/regions/${encodeURIComponent(id||'')}`;}
                function actionHash(id){return `#/actions/${encodeURIComponent(id||'')}`;}
                function regionButton(id,label){if(isBlank(id))return '<span class="muted">暂无区域</span>';return `<button class="link-button" ${navigationAttr(regionHash(id))}>${esc(label||id)}</button>`}
                function actionButton(id,label){if(isBlank(id))return '<span class="muted">暂无动作</span>';return `<button class="link-button" ${navigationAttr(actionHash(id))}>${esc(label||id)}</button>`}
                function normalizeBounds(bounds){if(!bounds)return null;const min=bounds.min||{}, max=bounds.max||{};const out={minX:bounds.minX??min.x,minY:bounds.minY??min.y,minZ:bounds.minZ??min.z,maxX:bounds.maxX??max.x,maxY:bounds.maxY??max.y,maxZ:bounds.maxZ??max.z};return Object.values(out).some(v=>v==null||Number.isNaN(Number(v)))?null:out;}
                function boundsText(bounds){const b=normalizeBounds(bounds);return b?`${b.minX} ${b.minY} ${b.minZ} → ${b.maxX} ${b.maxY} ${b.maxZ}`:'暂无';}
                function boundsSize(bounds){const b=normalizeBounds(bounds);if(!b)return '暂无';const x=Math.abs(Number(b.maxX)-Number(b.minX))+1,y=Math.abs(Number(b.maxY)-Number(b.minY))+1,z=Math.abs(Number(b.maxZ)-Number(b.minZ))+1;return `${x} × ${y} × ${z}`;}
                function boundsVolume(bounds){const b=normalizeBounds(bounds);if(!b)return '暂无';const x=Math.abs(Number(b.maxX)-Number(b.minX))+1,y=Math.abs(Number(b.maxY)-Number(b.minY))+1,z=Math.abs(Number(b.maxZ)-Number(b.minZ))+1;return `${x*y*z}`;}
                function boundsCenter(bounds){const b=normalizeBounds(bounds);if(!b)return '暂无';return `${Math.floor((Number(b.minX)+Number(b.maxX))/2)} ${Math.floor((Number(b.minY)+Number(b.maxY))/2)} ${Math.floor((Number(b.minZ)+Number(b.maxZ))/2)}`;}
                function labelRegionSort(value){return {NAME:'区域名',WORLD:'世界/维度',PLAYERS:'当前玩家数',RECENT:'最近事件'}[value]||value;}
                function labelPlayersFilter(value){return {ALL:'全部',HAS_PLAYERS:'有玩家',NO_PLAYERS:'无玩家'}[value]||value;}
                function labelActionSort(value){return {NAME:'动作名',TYPE:'动作类型',OWNER:'归属对象',RECENT:'最近执行'}[value]||value;}
                function labelActionResultFilter(value){return {ALL:'全部',SUCCESS:'成功',FAILED:'失败',UNKNOWN:'未执行'}[value]||value;}
                function cleanActionSummary(value){return typeof typedActionCleanSummary==='function'?typedActionCleanSummary(value):String(value||'').trim()||'暂无摘要';}
                function ownerLink(action){const ownerType=String(action?.ownerType||action?.owner?.ownerType||'').toUpperCase(), ownerId=action?.ownerId||action?.owner?.ownerId||'', ownerName=action?.ownerName||action?.owner?.ownerName||ownerId||'暂无';if(ownerType.startsWith('REGION'))return regionButton(ownerId,ownerName);if(ownerType==='ACTION_RELAY'||ownerType==='DEVICE')return navigationButton(`device:${ownerId}`,ownerName);return esc(ownerName);}
                """)
.append("""
                async function renderRegionsPage(options={}){
                  if(!options.silent)setView(loading('正在加载区域管理...'));
                  let regions;try{regions=await api('/api/regions')}catch(err){if(options.silent){toast('区域管理实时刷新失败，已保留当前页面。');return;}setView(errorBlock(err.message));return;}
                  appState.regions=regions||[];
                  renderRegionList('',options);
                }
                function renderRegionList(focusId,options={}){
                  const regions=appState.regions||[], worlds=uniqueValues(regions.map(r=>r.world).filter(v=>!isBlank(v))), filtered=filterRegions(regions);
                  const warning=regions.filter(r=>['WARNING','ERROR'].includes(String(r.doctorStatus||'').toUpperCase())).length;
                  if(setView(`
                    <div class="page-head"><div><h1>区域管理</h1><p>查看 RegionController 区域、边界、目标过滤、事件动作与实时状态</p></div><span class="pill info">只读模式</span></div>
                    <section class="card-grid">
                      ${metric('区域总数',regions.length,'','region')}
                      ${metric('启用区域',regions.filter(r=>r.enabled).length,'','ok')}
                      ${metric('禁用区域',regions.filter(r=>!r.enabled).length,regions.some(r=>!r.enabled)?'warning':'','warning')}
                      ${metric('当前有玩家区域',regions.filter(r=>Number(r.playersInside||0)>0).length,'','user')}
                      ${metric('Doctor 警告',warning,warning>0?'warning':'','doctor')}
                    </section>
                    <div class="toolbar">
                      <input class="input" id="region-search" placeholder="搜索区域名 / ID / world / channel" value="${esc(appState.regionFilters.search)}">
                      ${regionFilterSelect('世界/维度','region-world',['ALL',...worlds],appState.regionFilters.world)}
                      ${regionFilterSelect('启用状态','region-enabled',['ALL','ENABLED','DISABLED'],appState.regionFilters.enabled)}
                      ${regionFilterSelect('诊断状态','region-doctor',['ALL','OK','INFO','WARNING','ERROR','UNKNOWN'],appState.regionFilters.doctor)}
                      ${regionFilterSelect('玩家状态','region-players',['ALL','HAS_PLAYERS','NO_PLAYERS'],appState.regionFilters.players)}
                      ${regionFilterSelect('排序','region-sort',['NAME','WORLD','PLAYERS','RECENT'],appState.regionFilters.sort)}
                    </div>
                    ${filtered.length===0?(regions.length===0?empty('当前暂无区域数据。请使用现有 RegionController 命令创建区域后刷新页面。'):empty('没有匹配当前筛选条件的区域。')):regionTable(filtered)}
                  `,options))bindRegionFilters(focusId);
                }
                function regionFilterSelect(label,id,options,value){return `<label class="filter-field"><span>${esc(label)}</span><select class="select" id="${id}">${options.map(o=>`<option value="${esc(o)}" ${o===value?'selected':''}>${esc(regionOptionLabel(o))}</option>`).join('')}</select></label>`}
                function regionOptionLabel(v){return {ALL:'全部',ENABLED:'启用',DISABLED:'禁用',OK:'正常',INFO:'信息',WARNING:'警告',ERROR:'错误',UNKNOWN:'未知',HAS_PLAYERS:'有玩家',NO_PLAYERS:'无玩家',NAME:'区域名',WORLD:'世界/维度',PLAYERS:'当前玩家数',RECENT:'最近事件'}[v]||v;}
                function bindRegionFilters(focusId){
                  const update=(event)=>{appState.regionFilters.search=document.getElementById('region-search').value;appState.regionFilters.world=document.getElementById('region-world').value;appState.regionFilters.enabled=document.getElementById('region-enabled').value;appState.regionFilters.doctor=document.getElementById('region-doctor').value;appState.regionFilters.players=document.getElementById('region-players').value;appState.regionFilters.sort=document.getElementById('region-sort').value;renderRegionList(event.target.id);};
                  ['region-search','region-world','region-enabled','region-doctor','region-players','region-sort'].forEach(id=>document.getElementById(id).addEventListener(id==='region-search'?'input':'change',update));
                  if(focusId){const el=document.getElementById(focusId);if(el){el.focus();if(el.setSelectionRange&&el.value){el.setSelectionRange(el.value.length,el.value.length);}}}
                }
                function filterRegions(items){const f=appState.regionFilters;const filtered=(items||[]).filter(r=>{const hay=[r.id,r.name,r.world,r.boundChannel,r.targetFilter].join(' ').toLowerCase();if(f.search&&!hay.includes(f.search.toLowerCase()))return false;if(f.world!=='ALL'&&r.world!==f.world)return false;if(f.enabled==='ENABLED'&&!r.enabled)return false;if(f.enabled==='DISABLED'&&r.enabled)return false;if(f.doctor!=='ALL'&&String(r.doctorStatus||'UNKNOWN').toUpperCase()!==f.doctor)return false;if(f.players==='HAS_PLAYERS'&&Number(r.playersInside||0)<=0)return false;if(f.players==='NO_PLAYERS'&&Number(r.playersInside||0)>0)return false;return true;});return filtered.sort((a,b)=>{if(f.sort==='WORLD')return String(a.world||'').localeCompare(String(b.world||''))||String(a.name||'').localeCompare(String(b.name||''));if(f.sort==='PLAYERS')return Number(b.playersInside||0)-Number(a.playersInside||0);if(f.sort==='RECENT')return String(b.lastEventAt||'').localeCompare(String(a.lastEventAt||''));return String(a.name||a.id||'').localeCompare(String(b.name||b.id||''));});}
                function regionTable(items){return `<div class="table-wrap"><table class="data-table"><thead><tr><th>区域</th><th>世界</th><th>坐标范围</th><th>尺寸</th><th>目标过滤</th><th>动作数量</th><th>绑定频道</th><th>玩家</th><th>最近事件</th><th>诊断</th><th>操作</th></tr></thead><tbody>${items.map(r=>{const target=regionHash(r.id);return `<tr ${navigationAttr(target,false)}><td><span class="device-name"><span class="device-icon">${icon('region')}</span><span><strong>${esc(r.name||r.id)}</strong><span class="device-subtitle">ID：${esc(shortId(r.id))}</span></span></span></td><td>${esc(r.world||'暂无')}</td><td>${esc(boundsText(r.bounds))}</td><td>${esc(boundsSize(r.bounds))}</td><td>${esc(labelTargetFilter(r.targetFilter))}</td><td>进入 ${esc(r.enterActionCount||0)} / 离开 ${esc(r.exitActionCount||0)} / 停留 ${esc(r.stayActionCount||0)}</td><td>${channelCell(r.boundChannel)}</td><td>${esc(r.playersInside ?? '暂无')}</td><td>${fmtTime(r.lastEventAt)}</td><td>${pill(r.doctorStatus)}</td><td><button class="text-button" ${navigationAttr(target)}>查看详情</button></td></tr>`;}).join('')}</tbody></table></div>`}
                async function renderRegionDetail(id,options={}){
                  if(!options.silent)setView(loading('正在加载区域详情...'));
                  const routeInfo=detailRoute(id,'#/regions');
                  const [detailRes,listRes]=await Promise.all([settle(`/api/regions/${encodeURIComponent(routeInfo.id)}`),settle('/api/regions')]);
                  if(!detailRes.ok){if(options.silent){toast('区域详情实时刷新失败，已保留当前页面。');return;}setView(`<section class="wa-page"><div class="back-row">${backButton(routeInfo,'返回区域列表')}</div>${waPageHead('详情暂不可用','当前只读接口尚未提供该区域详情或区域已被删除。',waButton('返回列表','region',navigationAttr('#/regions'),'ghost'))}${detailRes.error.status===404?empty('区域不存在或已被删除。'):errorBlock(detailRes.error.message)}</section>`);return;}
                  const detail=detailRes.data, list=listRes.ok?listRes.data:[], entry=(list||[]).find(r=>r.id===detail.id)||{};
                  const b=normalizeBounds(detail.bounds), playerCount=(detail.playersInside||[]).length || entry.playersInside || 0, actionCount=Number(entry.enterActionCount||0)+Number(entry.exitActionCount||0)+Number(entry.stayActionCount||0);
                  const advancedRows=[
                    ['region.id',detail.id],
                    ['region.world',detail.world],
                    ['region.bounds',boundsText(detail.bounds)],
                    ['region.size',boundsSize(detail.bounds)],
                    ['region.targetFilter',labelTargetFilter(detail.targetFilter)],
                    ['region.lastEventAt',formatDateTime(entry.lastEventAt)],
                    ['region.playersInside',playerCount],
                    ['region.actionCount',actionCount]
                  ];
                  setView(`<section class="wa-page wa-detail-shell" data-detail-kind="region">
                    ${detailHeader({back:backButton(routeInfo,'返回区域列表'),kicker:'区域详情',iconName:'region',title:detail.name||detail.id,subtitle:`${detail.world||'world 未提供'} · ${boundsText(detail.bounds)}`,copyValue:detail.id,badges:[pill(entry.enabled?'OK':'WARNING'),pill(entry.doctorStatus||'UNKNOWN'),`<span class="pill info">${esc(labelTargetFilter(detail.targetFilter))}</span>`],actions:[waButton('编辑区域','pencil','disabled','ghost'),waButton('定位区域','eye','disabled','ghost'),waButton('更多','more','disabled','ghost')]})}
                    ${detailTabs(['基本信息','坐标范围','最近事件','关联对象','Doctor'])}
                    <section class="wa-detail-first-row">
                      ${detailCard('基本信息 / 坐标范围',detailInfoGrid([
                        ['区域 ID',detail.id],
                        ['名称',detail.name||detail.id],
                        ['世界/维度',detail.world||'暂无'],
                        ['状态',safeHtml(textPill(labelEnabledState(entry.enabled),entry.enabled?'ok':'warning'))],
                        ['目标过滤',labelTargetFilter(detail.targetFilter)],
                        ['最小点',b?`${b.minX}, ${b.minY}, ${b.minZ}`:'暂无'],
                        ['最大点',b?`${b.maxX}, ${b.maxY}, ${b.maxZ}`:'暂无'],
                        ['完整范围',safeHtml(`<span class="wa-code-line" title="${esc(boundsText(detail.bounds))}">${esc(boundsText(detail.bounds))}</span>`)]
                      ]))}
                      ${detailCard('状态与统计',`${detailStatGrid([
                        {label:'区域尺寸',value:boundsSize(detail.bounds),sub:'bounds',icon:'region-controller'},
                        {label:'当前玩家',value:playerCount,sub:'playersInside',icon:'user-total'},
                        {label:'动作数量',value:actionCount,sub:'enter / exit / stay',icon:'action-binding'},
                        {label:'Doctor',value:labelStatus(entry.doctorStatus||'UNKNOWN'),sub:'诊断摘要',icon:'doctor-overview',kind:String(entry.doctorStatus||'').toUpperCase()==='OK'?'ok':'warning'}
                      ])}${detailConsumerGrid([
                        {label:'关联控制器',value:entry.controllerCount ?? '--',icon:'region-controller'},
                        {label:'绑定频道',value:(detail.boundChannels||[]).length,icon:'active-channel',target:(detail.boundChannels||[])[0]?signalHash((detail.boundChannels||[])[0]):''},
                        {label:'当前玩家',value:playerCount,icon:'user-total'},
                        {label:'Doctor',value:(detail.doctorIssues||[]).length,icon:'doctor-overview',target:'#/doctor'}
                      ])}`)}
                    </section>
                    ${detailFixedLayout([
                      detailCard('关联控制器 / 最近事件',`${regionActionGroups(detail.actions||{})}${regionPlayersAndEvents(detail)}`,'','detail-card-stretchable'),
                      detailCard('目标过滤',detailInfoGrid([['过滤模式',labelTargetFilter(detail.targetFilter)],['绑定频道',safeHtml(regionChannels(detail.boundChannels||[]))],['最近事件',safeHtml(fmtTime(entry.lastEventAt))]]))
                    ],[
                      detailCard('Doctor / Debug',doctorList(detail.doctorIssues||[],8),'','detail-card-stretchable'),
                      detailCard('快捷操作',`<div class="wa-quick-grid">${waButton('编辑区域','pencil','disabled','ghost')}${waButton('定位区域','eye','disabled','ghost')}${waButton('导出区域','download','disabled','ghost')}${waButton('删除区域','channel-error','disabled','danger')}</div><p class="wa-disabled-note">区域编辑、删除、定位和导入导出没有完整 WebAdmin 写 API，本轮保持禁用且不发送写请求。</p>`)
                    ],[
                      advancedDetailCard('regions',detail.id,advancedRows,[
                      {title:'坐标与目标过滤',rows:advancedRowsFromObject({bounds:detail.bounds,targetFilter:detail.targetFilter,boundChannels:detail.boundChannels},'region')},
                      {title:'动作与控制器引用',rows:advancedRowsFromObject({actions:detail.actions,controllerCount:entry.controllerCount},'references')},
                      {title:'运行计数与调试',rows:advancedRowsFromObject({playersInside:detail.playersInside,recentEvents:detail.recentEvents,doctorIssues:detail.doctorIssues,entry},'runtime')}
                    ])
                    ])}
                  </section>`,options);
                }
                function regionActionGroups(actions){const groups=[['进入动作',actions.enter||[]],['离开动作',actions.exit||[]],['停留动作',actions.stay||[]]];return `<div class="list-stack">${groups.map(([name,items])=>`<div class="event-row"><strong>${esc(name)}：${items.length}</strong>${items.length?items.map(a=>`<span>${actionButton(a.id,labelActionType(a.type))} <span class="muted">${esc(cleanActionSummary(a.summary))} / ${a.enabled?'启用':'禁用'}</span></span>`).join(''):'<span class="muted">暂无动作</span>'}</div>`).join('')}</div>`}
                function regionChannels(channels){if(!channels||channels.length===0)return empty('未绑定频道。');return `<div class="list-stack">${channels.map(c=>`<div class="event-row"><strong>关联频道</strong>${channelButton(c)}<span class="muted">点击可查看 Signal 频道详情。</span></div>`).join('')}</div>`}
                function regionPlayersAndEvents(detail){const players=detail.playersInside||[], events=detail.recentEvents||[];return `<div class="list-stack"><div class="event-row"><strong>当前玩家</strong>${players.length?players.map(p=>`<span>${esc(p)}</span>`).join(''):'<span class="muted">暂无玩家状态数据</span>'}</div><div class="event-row"><strong>最近事件</strong>${events.length?events.map(e=>`<span>${esc(e.type||'事件')} · ${fmtTime(e.time)} · ${esc(e.playerName||'暂无玩家')}</span>`).join(''):'<span class="muted">暂无最近事件</span>'}</div></div>`}
                """)
.append("""
                async function renderActionsPage(options={}){
                  if(!options.silent)setView(loading('正在加载动作系统...'));
                  let actions;try{actions=await api('/api/actions')}catch(err){if(options.silent){toast('动作系统实时刷新失败，已保留当前页面。');return;}setView(errorBlock(err.message));return;}
                  appState.actions=actions||[];
                  renderActionList('',options);
                }
                function renderActionList(focusId,options={}){
                  const actions=appState.actions||[], ownerTypes=uniqueValues(actions.map(a=>a.ownerType).filter(v=>!isBlank(v))), filtered=filterActions(actions);
                  const warning=actions.filter(a=>['WARNING','ERROR'].includes(String(a.doctorStatus||'').toUpperCase())).length;
                  if(setView(`
                    <div class="page-head"><div><h1>动作系统</h1><p>查看 ActionEngine 动作、引用来源、执行记录与诊断状态</p></div><span class="pill info">只读模式</span></div>
                    <section class="card-grid">
                      ${metric('Action 总数',actions.length,'','action')}
                      ${metric('被引用动作',actions.filter(a=>Number(a.referencedByCount||0)>0).length,'','signal')}
                      ${metric('成功执行',actions.filter(a=>String(a.lastResult||'').toUpperCase()==='SUCCESS').length,'','ok')}
                      ${metric('失败执行',actions.filter(a=>String(a.lastResult||'').toUpperCase()==='FAILED').length,'warning','warning')}
                      ${metric('Doctor 警告',warning,warning>0?'warning':'','doctor')}
                    </section>
                    <div class="toolbar">
                      <input class="input" id="action-search" placeholder="搜索动作名称 / ID / 类型 / owner / channel" value="${esc(appState.actionFilters.search)}">
                      ${actionFilterSelect('动作类型','action-type',['ALL','COMMAND','MESSAGE','SOUND','SIGNAL','UNKNOWN'],appState.actionFilters.type)}
                      ${actionFilterSelect('归属类型','action-owner',['ALL',...ownerTypes],appState.actionFilters.owner)}
                      ${actionFilterSelect('执行结果','action-result',['ALL','SUCCESS','FAILED','UNKNOWN'],appState.actionFilters.result)}
                      ${actionFilterSelect('诊断状态','action-doctor',['ALL','OK','INFO','WARNING','ERROR','UNKNOWN'],appState.actionFilters.doctor)}
                      ${actionFilterSelect('排序','action-sort',['NAME','TYPE','OWNER','RECENT'],appState.actionFilters.sort)}
                    </div>
                    ${filtered.length===0?(actions.length===0?empty('当前暂无动作数据。请配置 listener、action_relay 或 region action 后刷新页面。'):empty('没有匹配当前筛选条件的动作。')):actionTable(filtered)}
                  `,options))bindActionFilters(focusId);
                }
                function actionFilterSelect(label,id,options,value){return `<label class="filter-field"><span>${esc(label)}</span><select class="select" id="${id}">${options.map(o=>`<option value="${esc(o)}" ${o===value?'selected':''}>${esc(actionOptionLabel(o))}</option>`).join('')}</select></label>`}
                function actionOptionLabel(v){return {ALL:'全部',COMMAND:'命令动作',MESSAGE:'消息动作',SOUND:'音效动作',SIGNAL:'信号动作',STATE_VARIABLE:'状态变量动作',UNKNOWN:'未执行 / 未知',OK:'正常',INFO:'信息',WARNING:'警告',ERROR:'错误',SUCCESS:'成功',FAILED:'失败',LISTENER:'监听器',ACTION_RELAY:'动作继电器',REGION_ENTER:'区域进入动作',REGION_EXIT:'区域离开动作',REGION_STAY:'区域停留动作',REGION:'区域',DEVICE:'设备',SYSTEM:'系统',NAME:'动作名',TYPE:'动作类型',OWNER:'归属对象',RECENT:'最近执行'}[v]||v;}
                function bindActionFilters(focusId){
                  const update=(event)=>{appState.actionFilters.search=document.getElementById('action-search').value;appState.actionFilters.type=document.getElementById('action-type').value;appState.actionFilters.owner=document.getElementById('action-owner').value;appState.actionFilters.result=document.getElementById('action-result').value;appState.actionFilters.doctor=document.getElementById('action-doctor').value;appState.actionFilters.sort=document.getElementById('action-sort').value;renderActionList(event.target.id);};
                  ['action-search','action-type','action-owner','action-result','action-doctor','action-sort'].forEach(id=>document.getElementById(id).addEventListener(id==='action-search'?'input':'change',update));
                  if(focusId){const el=document.getElementById(focusId);if(el){el.focus();if(el.setSelectionRange&&el.value){el.setSelectionRange(el.value.length,el.value.length);}}}
                }
                function filterActions(items){const f=appState.actionFilters;const filtered=(items||[]).filter(a=>{const hay=[a.id,a.name,a.type,a.summary,a.ownerType,a.ownerName,a.ownerId,a.channel].join(' ').toLowerCase();if(f.search&&!hay.includes(f.search.toLowerCase()))return false;if(f.type!=='ALL'&&String(a.type||'UNKNOWN').toUpperCase()!==f.type)return false;if(f.owner!=='ALL'&&String(a.ownerType||'UNKNOWN').toUpperCase()!==f.owner)return false;if(f.result!=='ALL'&&String(a.lastResult||'UNKNOWN').toUpperCase()!==f.result)return false;if(f.doctor!=='ALL'&&String(a.doctorStatus||'UNKNOWN').toUpperCase()!==f.doctor)return false;return true;});return filtered.sort((a,b)=>{if(f.sort==='TYPE')return String(a.type||'').localeCompare(String(b.type||''))||String(a.name||'').localeCompare(String(b.name||''));if(f.sort==='OWNER')return String(a.ownerName||a.ownerId||'').localeCompare(String(b.ownerName||b.ownerId||''));if(f.sort==='RECENT')return String(b.lastExecutedAt||'').localeCompare(String(a.lastExecutedAt||''));return String(a.name||a.id||'').localeCompare(String(b.name||b.id||''));});}
                function actionTable(items){return `<div class="table-wrap"><table class="data-table"><thead><tr><th>动作</th><th>类型</th><th>归属对象</th><th>关联频道</th><th>引用</th><th>执行次数</th><th>最近结果</th><th>最近执行</th><th>诊断</th><th>操作</th></tr></thead><tbody>${items.map(a=>{const target=actionHash(a.id);return `<tr ${navigationAttr(target,false)}><td><span class="device-name"><span class="device-icon">${icon('action')}</span><span><strong>${esc(a.name||a.id)}</strong><span class="device-subtitle">ID：${esc(shortId(a.id))}</span></span></span></td><td>${esc(labelActionType(a.type))}</td><td>${ownerLink(a)} <span class="muted">(${esc(labelOwnerType(a.ownerType))})</span></td><td>${channelCell(a.channel)}</td><td>${esc(a.referencedByCount ?? 0)}</td><td>${esc(a.executionCount ?? 0)}</td><td>${pill(a.lastResult||'UNKNOWN')}</td><td>${fmtTime(a.lastExecutedAt)}</td><td>${pill(a.doctorStatus)}</td><td><button class="text-button" ${navigationAttr(target)}>查看详情</button></td></tr>`;}).join('')}</tbody></table></div>`}
                function actionOwnerTarget(action){
                  const type=String(action?.ownerType||action?.owner?.ownerType||'').toUpperCase(), id=action?.ownerId||action?.owner?.ownerId||'', channel=action?.channel||action?.owner?.channel||'';
                  if(type.includes('LISTENER')&&!isBlank(id))return listenerHash(id);
                  if(type.includes('REGION')&&!isBlank(id))return regionHash(id);
                  if(type.includes('DEVICE')&&!isBlank(id))return deviceHash(id);
                  if(!isBlank(channel))return signalHash(channel);
                  return '';
                }
                async function renderActionDetail(id,options={}){
                  if(!options.silent)setView(loading('正在加载动作详情...'));
                  const routeInfo=detailRoute(id,'#/actions');
                  const [detailRes,listRes]=await Promise.all([settle(`/api/actions/${encodeURIComponent(routeInfo.id)}`),settle('/api/actions')]);
                  if(!detailRes.ok){if(options.silent){toast('动作详情实时刷新失败，已保留当前页面。');return;}setView(`<section class="wa-page"><div class="back-row">${backButton(routeInfo,'返回动作列表')}</div>${waPageHead('详情暂不可用','当前只读接口尚未提供该动作详情或动作已被删除。',waButton('返回列表','action',navigationAttr('#/actions'),'ghost'))}${detailRes.error.status===404?empty('动作不存在或已被删除。'):errorBlock(detailRes.error.message)}</section>`);return;}
                  const detail=detailRes.data, list=listRes.ok?listRes.data:[], entry=(list||[]).find(a=>a.id===detail.id)||{}, owner=detail.owner||{};
                  const title=entry.name||detail.configSummary?.name||detail.id, type=detail.type||entry.type, ownerType=owner.ownerType||entry.ownerType;
                  const ownerChannel=owner.channel||entry.channel;
                  const referencedBy=entry.referencedByCount ?? detail.configSummary?.referencedByCount ?? 0;
                  const executionCount=entry.executionCount ?? detail.configSummary?.executionCount ?? 0;
                  const advancedRows=[
                    ['action.id',detail.id],
                    ['action.type',labelActionType(type)],
                    ['action.ownerType',labelOwnerType(ownerType)],
                    ['action.owner',owner.ownerName||entry.ownerName||owner.ownerId||entry.ownerId],
                    ['action.channel',ownerChannel],
                    ['action.referencedByCount',referencedBy],
                    ['action.executionCount',executionCount],
                    ['action.lastResult',labelStatus(entry.lastResult||'UNKNOWN')]
                  ];
                  setView(`<section class="wa-page wa-detail-shell" data-detail-kind="action">
                    ${detailHeader({back:backButton(routeInfo,'返回动作列表'),kicker:'动作详情',iconName:actionIcon(type),title:title,subtitle:`${labelActionType(type)} · ${labelOwnerType(ownerType)}`,copyValue:detail.id,badges:[`<span class="pill">${esc(labelActionType(type))}</span>`,pill(entry.doctorStatus||'UNKNOWN'),pill(entry.lastResult||'UNKNOWN')],actions:[waButton('查看逻辑链','action-binding',navigationAttr(logicChainResolveHash('action',detail.id)),'primary'),waButton('编辑动作','settings','disabled','ghost'),waButton('测试执行','play','disabled','ghost'),waButton('更多','more','disabled','ghost')]})}
                    ${detailTabs(['基本信息','执行内容','最近执行','引用来源','Doctor'])}
                    <section class="wa-detail-first-row">
                      ${detailCard('基本信息',detailInfoGrid([
                        ['Action ID',detail.id],
                        ['名称',title],
                        ['类型',labelActionType(type)],
                        ['归属类型',labelOwnerType(ownerType)],
                        ['归属对象',safeHtml(ownerLink(entry))],
                        ['关联频道',safeHtml(channelCell(ownerChannel))],
                        ['引用次数',referencedBy],
                        ['最近结果',safeHtml(pill(entry.lastResult||'UNKNOWN'))]
                      ]))}
                      ${detailCard('执行统计',`${detailStatGrid([
                        {label:'动作类型',value:labelActionType(type),sub:'type',icon:actionIcon(type),kind:actionTypeTone(type)},
                        {label:'最近结果',value:labelStatus(entry.lastResult||'UNKNOWN'),sub:'last result',icon:'check-pass',kind:String(entry.lastResult||'').toUpperCase()==='FAILED'?'warning':'ok'},
                        {label:'最近执行',value:formatDateTime(entry.lastExecutedAt),sub:'time',icon:'recent-event'},
                        {label:'执行次数',value:executionCount,sub:'count',icon:'today-trigger'}
                      ])}${detailConsumerGrid([
                        {label:'归属对象',value:owner.ownerName||entry.ownerName||'--',icon:'action-binding',target:actionOwnerTarget({...entry,owner})},
                        {label:'关联频道',value:labelChannel(ownerChannel),icon:'active-channel',target:ownerChannel?signalHash(ownerChannel):''},
                        {label:'引用来源',value:referencedBy,icon:'consumer-listener'},
                        {label:'Doctor',value:(detail.doctorIssues||[]).length,icon:'doctor-overview',target:'#/doctor'}
                      ])}`)}
                    </section>
                    ${detailFixedLayout([
                      detailCard('引用来源 / 执行内容摘要',`${actionConfigPanel(detail,entry)}<div class="wa-compact-list"><div class="wa-compact-row"><strong>${esc(labelOwnerType(ownerType))}</strong><span>${ownerLink(entry)}</span><small>只读展示当前可从配置收集到的引用来源。</small></div></div>`,'','detail-card-stretchable'),
                      detailCard('最近执行',actionExecutions(detail.recentExecutions||[]))
                    ],[
                      detailCard('Doctor / Debug',doctorList(detail.doctorIssues||[],8),'','detail-card-stretchable'),
                      detailCard('快捷操作',`<div class="wa-quick-grid">${waButton('查看逻辑链','action-binding',navigationAttr(logicChainResolveHash('action',detail.id)),'ghost')}${waButton('编辑动作','settings','disabled','ghost')}${waButton('动作链编辑','action-binding','disabled','ghost')}${waButton('测试执行','play','disabled','ghost')}${waButton('删除动作','channel-error','disabled','danger')}</div><p class="wa-disabled-note">动作编辑、动作链、测试执行和删除没有完整 WebAdmin 写 API，本轮保持禁用且不发送写请求。</p>`)
                    ],[
                      advancedDetailCard('actions',detail.id,advancedRows,[
                      {title:'动作 payload / 配置摘要',rows:advancedRowsFromObject(detail.configSummary||{},'configSummary')},
                      {title:'引用与归属',rows:advancedRowsFromObject({owner,entry},'reference')},
                      {title:'执行与诊断',rows:advancedRowsFromObject({recentExecutions:detail.recentExecutions,doctorIssues:detail.doctorIssues,lastResult:entry.lastResult,lastExecutedAt:entry.lastExecutedAt},'runtime')}
                    ])
                    ])}
                  </section>`,options);
                }
                function actionConfigPanel(detail,entry){const type=String(detail.type||entry.type||'UNKNOWN').toUpperCase(), summary=cleanActionSummary(detail.summary||entry.summary);const cfg=detail.configSummary||{};const rows=[['动作类型',labelActionType(type)],['摘要',summary],['归属',labelOwnerType(detail.owner?.ownerType||entry.ownerType)],['下游频道',type==='SIGNAL'?(entry.channel||detail.owner?.channel):''],['引用数量',cfg.referencedByCount],['执行次数',cfg.executionCount],['Doctor 状态',entry.doctorStatus||cfg.doctorStatus]];let html=configGroup('关键配置',rows);if(type==='COMMAND')html+=`<p class="muted">命令动作仅只读展示摘要，不提供执行、复制执行或测试按钮。</p>`;if(type==='SIGNAL'&&!isBlank(entry.channel))html+=`<p>${channelButton(entry.channel)}</p>`;return `<div class="wa-config-stack">${html||empty('暂无可用配置摘要。')}</div>`}
                function actionExecutions(items){if(!items||items.length===0)return empty('暂无执行记录。');return `<div class="list-stack">${items.map(e=>`<div class="event-row"><strong>${fmtTime(e.time||e.executedAt)}</strong><span>${esc(e.owner||'暂无归属')} · ${esc(labelStatus(e.result||'UNKNOWN'))}</span><span class="muted">${esc(e.detail||'暂无详情')}</span></div>`).join('')}</div>`}
                function labelSignalJoinMode(v){return {ALL:'ALL：所有输入均到达',ANY_N:'ANY_N：任意 N 个输入',COUNT:'COUNT：累计输入次数'}[String(v||'').toUpperCase()]||v||'-';}
                function signalJoinModeIcon(v){const mode=String(v||'').toUpperCase();if(mode==='ALL')return 'signal-barrier';if(mode==='ANY_N'||mode==='COUNT')return 'signal-aggregator';return 'signal-join';}
                function labelSignalJoinScope(v){return {GLOBAL:'GLOBAL：全局共享',PLAYER:'PLAYER：按玩家隔离'}[String(v||'').toUpperCase()]||v||'-';}
                function labelSignalJoinReset(v){return {RESET_AFTER_EMIT:'输出后清空，可重复触发',LATCH_UNTIL_MANUAL_RESET:'输出后锁存，需手动重置'}[String(v||'').toUpperCase()]||v||'-';}
                """)
.append("""
                async function renderSignalJoinsPage(options={}){
                  if(!options.silent)setView(loading('正在加载信号汇合...'));
                  let data;try{data=await api('/api/webadmin/signal-joins')}catch(err){if(options.silent){toast('信号汇合实时刷新失败，已保留当前页面。');return;}setView(errorBlock(err.message));return;}
                  appState.signalJoins=data?.joins||[];
                  renderSignalJoinList('',options,data||{});
                }
                function renderSignalJoinList(focusId,options={},meta={}){
                  const joins=appState.signalJoins||[], filtered=filterSignalJoins(joins), enabled=joins.filter(j=>j.enabled!==false).length, pending=joins.reduce((sum,j)=>sum+Number(j.status?.pendingScopeCount||0),0);
                  const create=canEditSignalJoin()?waButton('新建汇合','plus',htmlHandler('startSignalJoinCreate()'),'primary'):waButton('需要 EDITOR / OWNER','plus','disabled','ghost');
                  if(setView(`<section class="wa-page signal-join-page" data-signal-join-page="true" data-no-signal-join-raw-json-editor="true">
                    ${waPageHead('信号汇合','配置 A + B 到达后发出 C 的 Join / Barrier / Aggregator。配置持久化，pending runtime state 仅保存在内存。',`${create}${waButton('刷新','refresh','onclick="renderSignalJoinsPage()"','ghost')}`)}
                    <section class="card-grid">
                      ${metric('汇合配置',joins.length,'世界级配置','signal-join')}
                      ${metric('已启用',enabled,'启用状态','enabled')}
                      ${metric('待满足作用域',pending,'运行态内存','join-status')}
                      ${metric('配置文件',meta.storeDegraded?'降级':'signal_joins.json',meta.storeDegraded?'warning':'','settings')}
                    </section>
                    <div class="toolbar">
                      <input class="input" id="signal-join-search" placeholder="搜索名称、ID、频道" value="${esc(appState.signalJoinFilters.search||'')}">
                      ${signalJoinFilterSelect('状态','signal-join-filter-enabled',['ALL','ENABLED','DISABLED'],appState.signalJoinFilters.enabled||'ALL')}
                      ${signalJoinFilterSelect('模式','signal-join-filter-mode',['ALL','ALL_MODE','ANY_N','COUNT'],appState.signalJoinFilters.mode||'ALL')}
                      ${signalJoinFilterSelect('作用域','signal-join-filter-scope',['ALL','GLOBAL','PLAYER'],appState.signalJoinFilters.scope||'ALL')}
                    </div>
                    ${filtered.length?signalJoinTable(filtered):empty(joins.length?'没有匹配当前筛选条件的 Signal Join。':'当前暂无 Signal Join 配置。')}
                    <article class="readonly-note">超时 tick 使用 lazy timeout：不会启动后台 tick 扫描，只会在下一次相关事件或状态查询时清理过期待满足状态。</article>
                  </section>`,options))bindSignalJoinFilters(focusId);
                }
                function signalJoinFilterSelect(label,id,options,value){return `<label class="filter-field"><span>${esc(label)}</span><select class="select" id="${id}">${options.map(o=>`<option value="${esc(o)}" ${o===value?'selected':''}>${esc({ALL:'全部',ENABLED:'启用',DISABLED:'停用',ALL_MODE:'ALL',ANY_N:'ANY_N',COUNT:'COUNT',GLOBAL:'GLOBAL',PLAYER:'PLAYER'}[o]||o)}</option>`).join('')}</select></label>`}
                function bindSignalJoinFilters(focusId){const update=(event)=>{appState.signalJoinFilters.search=document.getElementById('signal-join-search')?.value||'';appState.signalJoinFilters.enabled=document.getElementById('signal-join-filter-enabled')?.value||'ALL';appState.signalJoinFilters.mode=document.getElementById('signal-join-filter-mode')?.value||'ALL';appState.signalJoinFilters.scope=document.getElementById('signal-join-filter-scope')?.value||'ALL';renderSignalJoinList(event?.target?.id||'');};['signal-join-search','signal-join-filter-enabled','signal-join-filter-mode','signal-join-filter-scope'].forEach(id=>document.getElementById(id)?.addEventListener(id==='signal-join-search'?'input':'change',update));restoreFocusEnd(focusId);}
                function filterSignalJoins(items){const f=appState.signalJoinFilters||{};return (items||[]).filter(j=>{const text=[j.id,j.displayName,j.note,j.outputChannel,...(j.inputChannels||[]).map(i=>i.channel)].join(' ').toLowerCase();if(f.search&&!text.includes(String(f.search).toLowerCase()))return false;if(f.enabled==='ENABLED'&&j.enabled===false)return false;if(f.enabled==='DISABLED'&&j.enabled!==false)return false;if(f.mode&&f.mode!=='ALL'&&f.mode!=='ALL_MODE'&&String(j.mode||'')!==f.mode)return false;if(f.mode==='ALL_MODE'&&String(j.mode||'')!=='ALL')return false;if(f.scope&&f.scope!=='ALL'&&String(j.scopeMode||'')!==f.scope)return false;return true;}).sort((a,b)=>String(a.displayName||a.id).localeCompare(String(b.displayName||b.id)));}
                function signalJoinTable(items){return `<div class="table-wrap"><table class="data-table"><thead><tr><th>名称</th><th>模式</th><th>输入</th><th>输出</th><th>作用域</th><th>待满足</th><th>最近结果</th><th>操作</th></tr></thead><tbody>${items.map(j=>`<tr ${navigationAttr(signalJoinHash(j.id),false)}><td><span class="device-name"><span class="device-icon">${icon(signalJoinModeIcon(j.mode))}</span><span><strong>${esc(j.displayName||j.id)}</strong><span class="device-subtitle">${esc(j.id)}</span></span></span></td><td>${esc(labelSignalJoinMode(j.mode))}</td><td>${esc(j.inputChannelCount||j.inputChannels?.length||0)} 个</td><td>${channelButton(j.outputChannel)}</td><td>${esc(labelSignalJoinScope(j.scopeMode))}</td><td>${esc(j.status?.pendingScopeCount ?? 0)}</td><td>${esc(j.status?.lastResult||j.status?.lastFailureReason||'暂无')}</td><td><button class="text-button" ${navigationAttr(signalJoinHash(j.id))}>查看详情</button></td></tr>`).join('')}</tbody></table></div>`;}
                async function renderSignalJoinDetail(rawId,options={}){
                  const routeInfo=detailRoute(rawId,'#/signal-joins'), id=routeInfo.id||'';
                  if(!options.silent)setView(loading('正在加载信号汇合详情...'));
                  let detail;try{detail=await api(`/api/webadmin/signal-joins/${encodeURIComponent(id)}`)}catch(err){if(options.silent){toast('信号汇合详情实时刷新失败，已保留当前页面。');return;}setView(`<section class="wa-page">${backButton(routeInfo,'返回信号汇合')}${errorBlock(err.message)}</section>`);return;}
                  const status=detail.status||{}, lock=detail.lockStatus||{}, locked=lockHeldByOther(lock), edit=canEditSignalJoin()&&!locked?waButton('编辑配置','settings',htmlHandler(`startSignalJoinEdit(${jsString(detail.id)})`),'primary'):waButton(locked?'配置已锁定':'需要 EDITOR / OWNER','settings','disabled','ghost');
                  const reset=canEditSignalJoin()&&!locked?waButton('重置 runtime state','refresh',htmlHandler(`startSignalJoinReset(${jsString(detail.id)})`),'ghost'):waButton('重置 runtime state','refresh','disabled','ghost');
                  const del=canEditSignalJoin()&&!locked?waButton('删除','critical-issue',htmlHandler(`startSignalJoinDelete(${jsString(detail.id)})`),'danger'):waButton('删除','critical-issue','disabled','danger');
                  setView(`<section class="wa-page wa-detail-shell signal-join-detail-page" data-signal-join-detail="true" data-no-signal-join-raw-json-editor="true">
                    ${detailHeader({back:backButton(routeInfo,'返回信号汇合'),kicker:'SignalBridge / 信号汇合',iconName:signalJoinModeIcon(detail.mode),title:detail.displayName||detail.id,subtitle:detail.note||'多输入 signal 到达后发出 output signal。',copyValue:detail.id,badges:[pill(detail.enabled?'OK':'WARNING'),`<span class="pill info">${esc(labelSignalJoinMode(detail.mode))}</span>`,status.pendingScopeCount?`<span class="pill warning">${esc(status.pendingScopeCount)} pending</span>`:'<span class="pill">无 pending</span>'],actions:[edit,reset,del]})}
                    ${detailTabs(['配置摘要','运行状态','输入频道','逻辑链入口','写入安全','完整详情'])}
                    <section class="wa-detail-first-row">
                      ${detailCard('配置摘要',detailInfoGrid([
                        ['ID',detail.id],['启用',detail.enabled?'是':'否'],['模式',labelSignalJoinMode(detail.mode)],['阈值',detail.threshold],['作用域',labelSignalJoinScope(detail.scopeMode)],['重置策略',labelSignalJoinReset(detail.resetPolicy)],['超时 tick',detail.timeoutTicks],['输出冷却 tick',detail.cooldownTicks],['输出频道',safeHtml(channelButton(detail.outputChannel))]
                      ]))}
                      ${detailCard('运行状态',signalJoinStatusPanel(status),'','detail-card-stretchable')}
                    </section>
                    ${detailFixedLayout([
                      detailCard('输入频道',signalJoinInputList(detail.inputChannels||[]),'','detail-card-stretchable'),
                      detailCard('逻辑链入口',`<div class="wa-quick-grid">${waButton('从输出频道查看','logic-chain',navigationAttr(logicChainResolveHash('channel',detail.outputChannel)),'ghost')}${(detail.inputChannels||[]).slice(0,3).map(i=>waButton(i.channel,'active-channel',navigationAttr(logicChainResolveHash('channel',i.channel)),'ghost')).join('')}</div>`)
                    ],[
                      detailCard('写入安全',`${locked?`<div class="readonly-note">${esc(lockMessage(lock,'Signal Join 配置'))}</div>`:'<div class="readonly-note">写操作走 permission / CSRF / same-origin / edit lock / expectedFingerprint / audit / realtime。</div>'}`),
                      detailCard('说明',`<p class="muted">配置保存到 world-scoped <code>tzz/webadmin/signal_joins.json</code>；pending/latched runtime state 仅内存保存，服务器重启后清空。</p>`)
                    ],[
                      advancedDetailCard('signal-joins',detail.id,[['fingerprint',detail.expectedFingerprint],['version',detail.version],['updatedAt',detail.updatedAt],['updatedBy',detail.updatedBy]],[{title:'status',rows:advancedRowsFromObject(status,'status')},{title:'validation',rows:advancedRowsFromObject({validationErrors:detail.validationErrors},'validation')}])
                    ])}
                  </section>`,options);
                }
                function signalJoinInputList(inputs){if(!inputs||!inputs.length)return empty('尚未配置输入频道。');return `<div class="list-stack">${inputs.map(i=>`<div class="event-row"><strong>${channelButton(i.channel)}</strong><span>${esc(i.displayName||'输入频道')}</span><span class="muted">${esc(i.note||'')}</span></div>`).join('')}</div>`;}
                function signalJoinStatusPanel(status){const scopes=status?.scopes||[];const rows=scopes.slice(0,6).map(s=>`<div class="event-row"><strong>${esc(s.scopeKey||'global')}</strong><span>${esc((s.matchedChannels||[]).join(' / ')||'暂无匹配')}</span><span class="muted">总次数=${esc(s.totalCount||0)} · ${esc(s.lastResult||'PENDING')}</span></div>`).join('');return `<div data-signal-join-status-panel="true">${detailStatGrid([{label:'待满足作用域',value:status?.pendingScopeCount||0,sub:'内存态',icon:'join-status'},{label:'最近结果',value:status?.lastResult||'暂无',sub:status?.lastFailureReason||'',icon:'check-pass'},{label:'持久化',value:'否',sub:'运行态仅保存在内存',icon:'settings'}])}${rows?`<div class="list-stack">${rows}</div>`:empty('当前没有 pending runtime state。')}</div>`;}
                """)
.append("""
                function signalJoinDraftFromDetail(detail={},mode='edit'){return {mode,id:detail.id||'',displayName:detail.displayName||'',note:detail.note||'',enabled:detail.enabled!==false,inputChannels:(detail.inputChannels&&detail.inputChannels.length?detail.inputChannels:[{channel:'',displayName:'',note:'',requiredCount:1}]).map(i=>({channel:i.channel||'',displayName:i.displayName||'',note:i.note||'',requiredCount:Number(i.requiredCount||1)})),outputChannel:detail.outputChannel||'',modeValue:detail.mode||'ALL',threshold:Number(detail.threshold||2),scopeMode:detail.scopeMode||'GLOBAL',resetPolicy:detail.resetPolicy||'RESET_AFTER_EMIT',timeoutTicks:Number(detail.timeoutTicks||0),cooldownTicks:Number(detail.cooldownTicks||0),expectedFingerprint:detail.expectedFingerprint||detail.fingerprint||'',lockId:'',lock:null,lockTargetId:'',errors:[],saving:false};}
                function normalizeSignalJoinId(id){return String(id||'').trim().toLowerCase().replace(/\\s+/g,'-').replace(/[^a-z0-9_.:-]/g,'').substring(0,96);}
                async function startSignalJoinCreate(){appState.signalJoinEdit=signalJoinDraftFromDetail({id:'',displayName:'',inputChannels:[{channel:'',displayName:'',note:'',requiredCount:1}],mode:'ALL',scopeMode:'GLOBAL',resetPolicy:'RESET_AFTER_EMIT'},'create');showSignalJoinEditModal();}
                async function startSignalJoinEdit(id){if(!canEditSignalJoin())return;try{const detail=await api(`/api/webadmin/signal-joins/${encodeURIComponent(id)}`);const result=await acquireSignalJoinLock(detail.id);if(!result.success){toast(result.message||'无法获取编辑锁');await renderSignalJoinDetail(encodeURIComponent(id),{silent:true});return;}const draft=signalJoinDraftFromDetail(detail,'edit');draft.lockId=result.data?.lock?.lockId||'';draft.lock=result.data?.lock||null;draft.lockTargetId=detail.id;appState.signalJoinEdit=draft;scheduleSignalJoinLockHeartbeat();showSignalJoinEditModal();}catch(err){toast(err.message||'无法打开 Signal Join 编辑器');}}
                async function acquireSignalJoinLock(id){return api('/api/webadmin/edit-locks/acquire',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'signal_join_config',targetId:id})});}
                function signalJoinFieldLabel(field){const key=String(field||'').replace(/\\[\\d+\\]/g,'[]');return {id:'信号汇合 ID',displayName:'显示名称',inputChannels:'输入频道','inputChannels[].channel':'输入频道',outputChannel:'输出频道',mode:'模式',threshold:'阈值',scopeMode:'作用域',resetPolicy:'重置策略',timeoutTicks:'超时 tick',cooldownTicks:'输出冷却 tick',expectedFingerprint:'配置指纹',lockId:'编辑锁'}[key]||field||'';}
                function signalJoinValidationErrorsHtml(errors){return (errors||[]).length?`<ul class="validation-list" data-signal-join-validation-preserves-input="true">${errors.map(e=>`<li>${esc(e.field?`${signalJoinFieldLabel(e.field)}：`: '')}${esc(e.message||'保存失败')}</li>`).join('')}</ul>`:'';}
                function signalJoinThresholdSection(d){const mode=String(d.modeValue||'ALL').toUpperCase();if(mode==='ALL')return `<div class="readonly-note" data-signal-join-threshold-mode-conditional="ALL">阈值由系统自动等于输入频道数量；所有输入频道均到达后输出。</div>`;const help=mode==='ANY_N'?'任意 N 个不同输入频道到达后输出。':'累计匹配输入事件数量达到 N 后输出，重复同一频道会计数。';return `<label data-signal-join-threshold-mode-conditional="${esc(mode)}"><span>阈值</span><input id="signal-join-threshold" class="input" type="number" min="1" step="1" value="${esc(d.threshold)}" oninput="syncSignalJoinDraft()"><span class="muted">${esc(help)} 技术字段：threshold。</span></label>`;}
                function signalJoinEditFormField(id){const form=document.querySelector('#wa-modal-root .signal-join-form');return form?form.querySelector(`#${id}`):null;}
                function rerenderSignalJoinEditModal(){withPreservedModalScroll(()=>showSignalJoinEditModal());}
                function showSignalJoinEditModal(){const d=appState.signalJoinEdit;if(!d)return;markModalInitialSnapshot('signal_join_config',d);const errs=signalJoinValidationErrorsHtml(d.errors);const lockLine=d.lockId?`<div class="readonly-note">正在编辑 Signal Join，编辑锁到期：${esc(formatDateTime(d.lock?.expiresAt))}</div>`:'<div class="readonly-note">新建时会在保存前按 ID 获取编辑锁。</div>';openWebAdminModal(d.mode==='create'?'新建信号汇合':'编辑信号汇合',`<form class="edit-form signal-join-form" data-no-signal-join-raw-json-editor="true" data-signal-join-modal-preserve-scroll="true" data-signal-join-save-payload-typed="true" onsubmit="event.preventDefault();saveSignalJoinEdit()">${lockLine}<label><span>信号汇合 ID</span><input id="signal-join-id" class="input" maxlength="96" value="${esc(d.id)}" ${d.mode==='edit'?'disabled':''} oninput="syncSignalJoinDraft()"><span class="muted">技术字段：id。</span></label><label><span>显示名称</span><input id="signal-join-name" class="input" maxlength="64" value="${esc(d.displayName)}" oninput="syncSignalJoinDraft()"></label><label><span>备注</span><textarea id="signal-join-note" class="input wa-action-textarea" maxlength="512" oninput="syncSignalJoinDraft()">${esc(d.note)}</textarea></label><label class="switch-row"><span>启用</span><input id="signal-join-enabled" type="checkbox" ${d.enabled?'checked':''} onchange="syncSignalJoinDraft()"></label><div data-signal-join-input-list-editor="true"><strong>输入频道</strong>${signalJoinInputEditor(d)}</div><label><span>输出频道</span><div data-signal-join-output-channel-field="true">${signalJoinChannelField('output',-1,d.outputChannel)}</div></label><label><span>模式</span><select id="signal-join-mode" class="select" data-signal-join-mode-selector="true" data-signal-join-mode-internal-value="ALL|ANY_N|COUNT" onchange="syncSignalJoinDraft();rerenderSignalJoinEditModal()">${['ALL','ANY_N','COUNT'].map(v=>`<option value="${v}" ${d.modeValue===v?'selected':''}>${esc(labelSignalJoinMode(v))}</option>`).join('')}</select><span class="muted">保存值只会使用 ALL / ANY_N / COUNT。</span></label>${signalJoinThresholdSection(d)}<label><span>作用域</span><select id="signal-join-scope" class="select" data-signal-join-scope-selector="true" data-signal-join-scope-mode-internal-value="GLOBAL|PLAYER" onchange="syncSignalJoinDraft()">${['GLOBAL','PLAYER'].map(v=>`<option value="${v}" ${d.scopeMode===v?'selected':''}>${esc(labelSignalJoinScope(v))}</option>`).join('')}</select><span class="muted">技术字段：scopeMode；保存值只会使用 GLOBAL / PLAYER。</span></label><label><span>重置策略</span><select id="signal-join-reset-policy" class="select" data-signal-join-reset-policy-selector="true" data-signal-join-reset-policy-internal-value="RESET_AFTER_EMIT|LATCH_UNTIL_MANUAL_RESET" onchange="syncSignalJoinDraft()">${['RESET_AFTER_EMIT','LATCH_UNTIL_MANUAL_RESET'].map(v=>`<option value="${v}" ${d.resetPolicy===v?'selected':''}>${esc(labelSignalJoinReset(v))}</option>`).join('')}</select><span class="muted">保存值只会使用内部枚举。</span></label><label><span>超时 tick</span><input id="signal-join-timeout" class="input" data-signal-join-timeout-field="true" type="number" min="0" step="1" value="${esc(d.timeoutTicks)}" oninput="syncSignalJoinDraft()"><span class="muted">0 表示不启用；启用时采用 lazy timeout。技术字段：timeoutTicks。</span></label><label><span>输出冷却 tick</span><input id="signal-join-cooldown" class="input" data-signal-join-cooldown-field="true" type="number" min="0" step="1" value="${esc(d.cooldownTicks)}" oninput="syncSignalJoinDraft()"><span class="muted">0 表示不启用；输出后冷却期间忽略新的 Join 输入且不会发出 output。技术字段：cooldownTicks。</span></label>${errs}</form>`,editModalFooter(d.saving),{className:'wa-config-modal',syncBeforeClose:()=>syncSignalJoinDraft(),dirtyCheck:()=>modalDraftDirty('signal_join_config',appState.signalJoinEdit),onClose:async()=>{await cancelSignalJoinEdit(true);await dismissWebAdminModal();}});}
                function signalJoinInputEditor(d){return `<div class="list-stack">${(d.inputChannels||[]).map((i,index)=>`<div class="event-row"><strong>输入 #${index+1}</strong><span>${signalJoinChannelField('input',index,i.channel)}</span><button class="wa-icon-btn" type="button" aria-label="删除输入" title="删除输入" ${htmlHandler(`removeSignalJoinInput(${index})`)}>${icon('close')}</button></div>`).join('')}</div><button class="wa-btn ghost" type="button" ${htmlHandler('addSignalJoinInput()')}>${icon('plus')}<span>添加输入频道</span></button>`;}
                function signalJoinChannelInputId(kind,index){return kind==='output'?'signal-join-output':`signal-join-input-${index}`;}
                function signalJoinChannelField(kind,index,value){const id=signalJoinChannelInputId(kind,index);return `<div class="channel-combo signal-join-channel-combo" data-signal-join-channel-combo-close-on-toggle="true" data-signal-join-channel-combo-outside-click-close="true" data-signal-join-channel-combo-escape-close="true"><div class="channel-combo-control"><input id="${id}" class="input" value="${esc(value||'')}" placeholder="选择已有频道或输入新频道" autocomplete="off" role="combobox" aria-expanded="false" aria-controls="${id}-menu" oninput="syncSignalJoinDraft()" onkeydown="handleSignalJoinChannelKey(event)"><button class="channel-combo-toggle" type="button" aria-label="显示已有频道" ${htmlHandler(`toggleSignalJoinChannelOptions(${jsString(kind)},${index})`)}>${icon('chevron-down')}</button></div><div id="${id}-menu" class="channel-combo-menu" role="listbox">${signalJoinChannelOptions(kind,index,value)}</div></div>`;}
                function signalJoinChannelOptions(kind,index,value){const options=filteredChannelOptions(appState.channelOptions||[],String(value||'')).slice(0,8);return options.length?options.map(c=>`<button type="button" class="channel-combo-option" onmousedown="event.preventDefault()" ${htmlHandler(`selectSignalJoinChannel(${jsString(kind)},${index},${jsString(c.channel||'')})`)}><strong>${esc(c.channel||'')}</strong><span>${esc(channelOptionLabel(c))}</span></button>`).join(''):'<div class="channel-combo-empty">可直接输入新的频道名</div>';}
                function closeSignalJoinChannelOptions(){let closed=false;document.querySelectorAll('#wa-modal-root .signal-join-channel-combo.open').forEach(combo=>{closed=true;combo.classList.remove('open');const input=combo.querySelector('input[role="combobox"]');if(input)input.setAttribute('aria-expanded','false');});return closed;}
                async function toggleSignalJoinChannelOptions(kind,index){const id=signalJoinChannelInputId(kind,index), combo=document.getElementById(id)?.closest('.signal-join-channel-combo'), wasOpen=!!combo?.classList.contains('open');if(!appState.channelOptions&&!appState.channelOptionsError)await loadSignalChannelOptions().catch(()=>[]);closeAllCustomComboboxes();if(wasOpen)return;if(combo){combo.classList.add('open');const input=document.getElementById(id);if(input){input.setAttribute('aria-expanded','true');input.focus();}const menu=document.getElementById(`${id}-menu`);if(menu)menu.innerHTML=signalJoinChannelOptions(kind,index,input?.value||'');}}
                function handleSignalJoinChannelKey(event){if(event.key==='Escape'&&closeSignalJoinChannelOptions()){event.preventDefault();event.stopPropagation();}}
                function selectSignalJoinChannel(kind,index,channel){const id=signalJoinChannelInputId(kind,index);const el=signalJoinEditFormField(id);if(el)el.value=channel||'';closeSignalJoinChannelOptions();syncSignalJoinDraft();rerenderSignalJoinEditModal();}
                function addSignalJoinInput(){syncSignalJoinDraft();const d=appState.signalJoinEdit;if(!d)return;d.inputChannels.push({channel:'',displayName:'',note:'',requiredCount:1});rerenderSignalJoinEditModal();}
                function removeSignalJoinInput(index){syncSignalJoinDraft();const d=appState.signalJoinEdit;if(!d)return;d.inputChannels.splice(index,1);if(d.inputChannels.length===0)d.inputChannels.push({channel:'',displayName:'',note:'',requiredCount:1});rerenderSignalJoinEditModal();}
                function syncSignalJoinDraft(){const d=appState.signalJoinEdit;if(!d)return;const get=id=>signalJoinEditFormField(id);d.id=get('signal-join-id')?.value ?? d.id;d.displayName=get('signal-join-name')?.value ?? d.displayName;d.note=get('signal-join-note')?.value ?? d.note;const enabled=get('signal-join-enabled');if(enabled)d.enabled=!!enabled.checked;d.outputChannel=get('signal-join-output')?.value ?? d.outputChannel;d.modeValue=get('signal-join-mode')?.value ?? d.modeValue;const threshold=get('signal-join-threshold');if(threshold)d.threshold=Number(threshold.value ?? d.threshold);d.scopeMode=get('signal-join-scope')?.value ?? d.scopeMode;d.resetPolicy=get('signal-join-reset-policy')?.value ?? d.resetPolicy;d.timeoutTicks=Number(get('signal-join-timeout')?.value ?? d.timeoutTicks);d.cooldownTicks=Number(get('signal-join-cooldown')?.value ?? d.cooldownTicks);d.inputChannels=(d.inputChannels||[]).map((i,index)=>({...i,channel:get(`signal-join-input-${index}`)?.value ?? i.channel}));}
                async function ensureSignalJoinDraftLock(d){d.id=normalizeSignalJoinId(d.id);if(d.lockId&&d.lockTargetId===d.id)return true;if(d.lockId)await releaseSignalJoinLock(d,true);if(isBlank(d.id)){d.errors=[{field:'id',message:'信号汇合 ID 不能为空。'}];return false;}const result=await acquireSignalJoinLock(d.id);if(!result.success){d.errors=[{message:result.message||'无法获取编辑锁'}];return false;}d.lockId=result.data?.lock?.lockId||'';d.lock=result.data?.lock||null;d.lockTargetId=d.id;scheduleSignalJoinLockHeartbeat();return true;}
                async function saveSignalJoinEdit(){syncSignalJoinDraft();const d=appState.signalJoinEdit;if(!d)return;d.saving=true;d.errors=[];rerenderSignalJoinEditModal();if(!await ensureSignalJoinDraftLock(d)){d.saving=false;rerenderSignalJoinEditModal();return;}const body={id:d.id,displayName:d.displayName,note:d.note,enabled:d.enabled,inputChannels:d.inputChannels,outputChannel:d.outputChannel,mode:d.modeValue,threshold:d.threshold,scopeMode:d.scopeMode,resetPolicy:d.resetPolicy,timeoutTicks:d.timeoutTicks,cooldownTicks:d.cooldownTicks,expectedFingerprint:d.expectedFingerprint,lockId:d.lockId};try{const result=await api(d.mode==='create'?'/api/webadmin/signal-joins':`/api/webadmin/signal-joins/${encodeURIComponent(d.id)}`,{method:d.mode==='create'?'POST':'PATCH',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify(body)});if(result.success){const target=result.data?.routeTarget||signalJoinHash(result.data?.join?.id||d.id);appState.signalJoinEdit=null;stopSignalJoinLockHeartbeat();await dismissWebAdminModal();toast(result.message||'Signal Join 已保存。');location.hash=target;return;}d.saving=false;d.errors=result.validationErrors&&result.validationErrors.length?result.validationErrors:[{message:result.message||'保存失败'}];if(['edit_lock_expired','edit_lock_conflict','edit_lock_required'].includes(result.code))stopSignalJoinLockHeartbeat();rerenderSignalJoinEditModal();}catch(err){d.saving=false;d.errors=[{message:err.message||'保存失败'}];rerenderSignalJoinEditModal();}}
                async function cancelSignalJoinEdit(silent){const d=appState.signalJoinEdit;if(d&&d.lockId)await releaseSignalJoinLock(d,silent);appState.signalJoinEdit=null;stopSignalJoinLockHeartbeat();}
                async function releaseSignalJoinLock(d,silent){try{await api('/api/webadmin/edit-locks/release',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'signal_join_config',targetId:d.lockTargetId||d.id,lockId:d.lockId})});}catch(err){if(!silent)toast(err.message||'编辑锁释放失败，将等待自动过期。');}}
                function maybeReleaseSignalJoinEditForRoute(hash,options={}){const d=appState.signalJoinEdit;if(!d||options.silent)return;const h=String(hash||'');if(h==='#/signal-joins'&&d.mode==='create')return;if(h.startsWith('#/signal-joins/')){const info=detailRoute(h.substring('#/signal-joins/'.length),'#/signal-joins');if(info.id===(d.lockTargetId||d.id))return;}cancelSignalJoinEdit(true);}
                function scheduleSignalJoinLockHeartbeat(){stopSignalJoinLockHeartbeat();appState.signalJoinLockTimer=setTimeout(async()=>{await heartbeatSignalJoinLock();if(appState.signalJoinEdit?.lockId)scheduleSignalJoinLockHeartbeat();},30000);}
                function stopSignalJoinLockHeartbeat(){if(appState.signalJoinLockTimer){clearTimeout(appState.signalJoinLockTimer);appState.signalJoinLockTimer=null;}}
                async function heartbeatSignalJoinLock(){const d=appState.signalJoinEdit;if(!d||!d.lockId)return;try{const result=await api('/api/webadmin/edit-locks/heartbeat',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'signal_join_config',targetId:d.lockTargetId||d.id,lockId:d.lockId})});if(result.success){d.lock=result.data?.lock||d.lock;return;}d.errors=[{message:result.message||'编辑锁续期失败'}];d.lockId='';stopSignalJoinLockHeartbeat();rerenderSignalJoinEditModal();}catch(err){d.errors=[{message:err.message||'编辑锁续期失败'}];d.lockId='';stopSignalJoinLockHeartbeat();rerenderSignalJoinEditModal();}}
                async function startSignalJoinReset(id){if(!canEditSignalJoin())return;try{const detail=await api(`/api/webadmin/signal-joins/${encodeURIComponent(id)}`);const result=await acquireSignalJoinLock(detail.id);if(!result.success){toast(result.message||'无法获取编辑锁');return;}appState.signalJoinEdit={mode:'reset',id:detail.id,displayName:detail.displayName||detail.id,expectedFingerprint:detail.expectedFingerprint||detail.fingerprint||'',lockId:result.data?.lock?.lockId||'',lock:result.data?.lock||null,lockTargetId:detail.id,errors:[],saving:false};scheduleSignalJoinLockHeartbeat();showSignalJoinResetModal();}catch(err){toast(err.message||'无法重置 Signal Join');}}
                function showSignalJoinResetModal(){const d=appState.signalJoinEdit;if(!d)return;const errs=(d.errors||[]).length?`<ul class="validation-list">${d.errors.map(e=>`<li>${esc(e.message||'重置失败')}</li>`).join('')}</ul>`:'';openWebAdminModal('重置信号汇合运行状态',`<form class="edit-form" onsubmit="event.preventDefault();resetSignalJoinRuntime()"><div class="readonly-note" data-danger-confirm-modal="true"><strong>将重置 ${esc(d.displayName||d.id)} 的 pending / latched runtime state</strong><span>不会删除配置，不会发出 signal；写入链路仍校验 expectedFingerprint 和 edit lock。</span></div>${errs}</form>`,dangerousModalFooter(d.saving,'确认重置'),{className:'wa-config-modal',dirtyCheck:()=>false,onClose:async()=>{await cancelSignalJoinEdit(true);await dismissWebAdminModal();}});}
                async function resetSignalJoinRuntime(){const d=appState.signalJoinEdit;if(!d)return;d.saving=true;d.errors=[];showSignalJoinResetModal();try{const write=await api(`/api/webadmin/signal-joins/${encodeURIComponent(d.id)}/reset`,{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({expectedFingerprint:d.expectedFingerprint,lockId:d.lockId,scopeKey:'',confirmed:true})});if(write.success){appState.signalJoinEdit=null;stopSignalJoinLockHeartbeat();await dismissWebAdminModal();toast(write.message||'Signal Join runtime state 已重置。');await renderSignalJoinDetail(encodeURIComponent(d.id),{silent:true});return;}d.saving=false;d.errors=write.validationErrors&&write.validationErrors.length?write.validationErrors:[{message:write.message||'重置失败'}];showSignalJoinResetModal();}catch(err){d.saving=false;d.errors=[{message:err.message||'重置失败'}];showSignalJoinResetModal();}}
                async function startSignalJoinDelete(id){if(!canEditSignalJoin())return;try{const detail=await api(`/api/webadmin/signal-joins/${encodeURIComponent(id)}`);const result=await acquireSignalJoinLock(detail.id);if(!result.success){toast(result.message||'无法获取编辑锁');return;}appState.signalJoinEdit={mode:'delete',id:detail.id,displayName:detail.displayName||detail.id,expectedFingerprint:detail.expectedFingerprint||detail.fingerprint||'',lockId:result.data?.lock?.lockId||'',lock:result.data?.lock||null,lockTargetId:detail.id,errors:[],saving:false};scheduleSignalJoinLockHeartbeat();showSignalJoinDeleteModal();}catch(err){toast(err.message||'无法删除 Signal Join');}}
                function showSignalJoinDeleteModal(){const d=appState.signalJoinEdit;if(!d)return;const errs=(d.errors||[]).length?`<ul class="validation-list">${d.errors.map(e=>`<li>${esc(e.message||'删除失败')}</li>`).join('')}</ul>`:'';openWebAdminModal('删除信号汇合',`<form class="edit-form" onsubmit="event.preventDefault();deleteSignalJoin()"><div class="readonly-note danger" data-danger-confirm-modal="true"><strong>将删除 ${esc(d.displayName||d.id)}</strong><span>不需要输入完整 ID 或名称；删除不会触发 runtime signal，但会清理该 Join 的 pending state。</span></div>${errs}</form>`,dangerousModalFooter(d.saving,'确认删除'),{className:'wa-config-modal',dirtyCheck:()=>false,onClose:async()=>{await cancelSignalJoinEdit(true);await dismissWebAdminModal();}});}
                async function deleteSignalJoin(){const d=appState.signalJoinEdit;if(!d)return;d.saving=true;d.errors=[];showSignalJoinDeleteModal();try{const result=await api(`/api/webadmin/signal-joins/${encodeURIComponent(d.id)}/delete`,{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({expectedFingerprint:d.expectedFingerprint,lockId:d.lockId,confirmed:true})});if(result.success){appState.signalJoinEdit=null;stopSignalJoinLockHeartbeat();await dismissWebAdminModal();toast(result.message||'Signal Join 已删除。');location.hash='#/signal-joins';return;}d.saving=false;d.errors=result.validationErrors&&result.validationErrors.length?result.validationErrors:[{message:result.message||'删除失败'}];showSignalJoinDeleteModal();}catch(err){d.saving=false;d.errors=[{message:err.message||'删除失败'}];showSignalJoinDeleteModal();}}
                """).toString();
    }
}
