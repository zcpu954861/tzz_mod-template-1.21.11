package com.zcpu.tzzmod.webadmin;

public final class WebAdminActionFieldRenderScripts {
    private WebAdminActionFieldRenderScripts() {
    }

    public static String appJs() {
        return """
                // data-typed-action-schema-renderer="true"
                // 统一 Action value 字段渲染只读 schema，不负责保存、不发写请求。
                // owner 的 edit lock、expectedFingerprint、condition gate picker 和 payload assembly 仍留在原模块。
                function typedActionFieldSchema(actionType,fieldId){
                  const schema=actionSchemaByType(actionType);
                  return schema?(schema.fields||[]).find(field=>field.id===fieldId)||null:null;
                }
                function typedActionFieldMaxLength(actionType,fieldId,fallback){
                  const field=typedActionFieldSchema(actionType,fieldId);
                  return Number(field?.maxLength||fallback||0);
                }
                function typedActionInputEvent(name,handler){return handler?`${esc(name)}="${esc(handler)}"`:'';}
                function typedActionFieldMarkers(type,ownerMarker){
                  return `data-typed-action-schema-renderer="true" data-action-schema-field-render="true" data-action-owner-capability-filter="true" data-action-no-raw-json-primary-editor="true" data-action-type="${esc(type)}" ${ownerMarker||''}`;
                }
                function typedActionTypeMarker(options,type){
                  const markers=options.typeMarkers||{};
                  return markers[type]||'';
                }
                function typedActionChannelOptions(prefix,value,disabled){
                  const options=filteredChannelOptions(appState.channelOptions||[],value).slice(0,12);
                  const current=normalizeChannelName(value).toLowerCase();
                  const rows=options.map(item=>`<button type="button" class="channel-combo-option ${String(item.channel||'').trim().toLowerCase()===current?'selected':''}" role="option" ${disabled||''} data-typed-action-channel-option="${esc(item.channel||'')}" data-typed-action-channel-prefix="${esc(prefix)}"><strong>${esc(item.channel||'未命名频道')}</strong><span>${esc(channelOptionLabel(item))}</span></button>`).join('');
                  if(appState.channelOptionsError)return '<div class="channel-combo-empty">频道候选加载失败，仍可手动输入新的频道名。</div>';
                  return rows||'<div class="channel-combo-empty">没有匹配的已有频道，可直接保存为新频道。</div>';
                }
                function typedActionChannelPickerHint(value){
                  const hint=channelHintHtml(value,appState.channelOptions||[],appState.channelOptionsError);
                  return `<span class="readonly-note" data-typed-action-channel-picker-hint="true">${hint}</span>`;
                }
                function selectTypedActionChannel(prefix,channel){
                  const input=document.getElementById(`${prefix}-value`);
                  if(input){input.value=channel||'';input.dispatchEvent(new Event('input',{bubbles:true}));}
                }
                function handleTypedActionChannelOptionClick(event,button){
                  selectTypedActionChannel(button.dataset.typedActionChannelPrefix||'',button.dataset.typedActionChannelOption||'');
                  return true;
                }
                function renderTypedActionChannelPicker(prefix,action,disabled,oninput,markers){
                  const value=String(action.value||''), inputAttr=typedActionInputEvent('oninput',oninput);
                  return `<label class="wa-action-value-field" ${markers} data-typed-action-channel-picker="true">目标频道<div class="channel-combo typed-action-channel-combo" data-typed-action-channel-combobox="true"><div class="channel-combo-control"><input id="${prefix}-value" class="input" maxlength="128" value="${esc(value)}" ${disabled||''} placeholder="选择已有频道或输入新频道" autocomplete="off" role="combobox" aria-expanded="true" ${inputAttr}></div><div class="channel-combo-menu" role="listbox">${typedActionChannelOptions(prefix,value,disabled)}</div></div>${typedActionChannelPickerHint(value)}</label>`;
                }
                function renderTypedActionValueEditor(prefix,action={},options={}){
                  const type=normalizeActionTypeId(action.type||options.defaultType||'message'), schema=actionSchemaByType(type);
                  const disabled=options.disabled||'', oninput=options.oninput||'', changeHandler=options.onchange||oninput;
                  const ownerMarker=options.ownerMarker||'', markers=typedActionFieldMarkers(type,ownerMarker);
                  const typeMarker=typedActionTypeMarker(options,type);
                  if(!schema)return `<div class="readonly-note danger" ${markers}>暂不支持该动作类型：${esc(type||'空')}</div>`;
                  if(type==='state_variable')return stateActionEditor(prefix,action,disabled,oninput,changeHandler,`${markers} ${typeMarker} data-typed-action-state-variable-picker="true"`);
                  if(type==='timer_start'||type==='timer_cancel')return timerActionEditor(prefix,action,disabled,oninput,changeHandler,`${markers} ${typeMarker} data-typed-action-player-target-mode="true"`);
                  if(type==='signal'){
                    if(options.signalHtml)return `<section class="typed-action-field-shell" ${markers} data-typed-action-channel-picker="true">${options.signalHtml}</section>`;
                    return renderTypedActionChannelPicker(prefix,action,disabled,oninput,`${markers} ${typeMarker}`);
                  }
                  const value=String(action.value||(type==='sound'?'minecraft:entity.experience_orb.pickup':'')), inputAttr=typedActionInputEvent('oninput',oninput);
                  if(type==='message')return `<label class="wa-action-value-field" ${markers} ${typeMarker} data-message-action-editor="true">消息内容<textarea id="${prefix}-value" class="input wa-action-textarea" maxlength="${typedActionFieldMaxLength(type,'value',500)}" ${disabled||''} ${inputAttr}>${esc(value)}</textarea><span class="muted">${esc(schema.description||'纯文本消息；Rich Text Builder 不在 9.2 Phase 3 范围内。')}</span></label>`;
                  if(type==='sound')return `<label class="wa-action-value-field" ${markers} ${typeMarker} data-sound-action-editor="true">音效 ID<input id="${prefix}-value" class="input" maxlength="${typedActionFieldMaxLength(type,'value',128)}" value="${esc(value)}" ${disabled||''} placeholder="minecraft:entity.experience_orb.pickup" ${inputAttr}><span class="muted">当前保存 sound id；runtime 语义保持旧 ActionEngine 行为。</span></label>`;
                  return `<label class="wa-action-value-field" ${markers} ${typeMarker} data-command-action-editor="true">命令内容<input id="${prefix}-value" class="input" maxlength="${typedActionFieldMaxLength(type,'value',512)}" value="${esc(value)}" ${disabled||''} placeholder="say hello" ${inputAttr}><span class="readonly-note">不要输入开头的 /；后端会阻断 stop/op/ban/kick/whitelist 等危险服务器管理命令。</span></label>`;
                }
                """;
    }
}
