package com.zcpu.tzzmod.webadmin;

public final class WebAdminFrontendScripts {
    private static final String[] FLAT_ICON_KEYS = {
            "logo", "dashboard", "signalbridge-main", "receiver-main", "history", "doctor", "region", "device",
            "action", "user", "settings", "server-online", "logout", "device-overview", "doctor-overview", "signal-overview",
            "region-overview", "action-overview", "action-template", "user-overview", "signal-device", "signal-receiver", "virtual-block-device", "action-relay", "critical-issue",
            "warning-issue", "info-issue", "check-pass", "active-channel", "listener-receiver", "recent-event", "response-time", "region-controller",
            "active-region", "action-binding", "today-trigger", "action-total", "enabled", "success-rate", "user-total", "current-user",
            "current-role", "session", "channel-total", "channel-with-consumers", "channel-orphan", "channel-error", "consumer-listener", "consumer-receiver",
            "consumer-relay", "consumer-region", "doctor-ok", "doctor-warning", "doctor-error", "receiver-total", "receiver-enabled", "receiver-disabled",
            "receiver-outputting", "receiver-trigger-today", "pulse-duration", "redstone-output", "receiver-row", "channel-list", "refresh", "more"
    };

    private static final String[][] FLAT_ICON_GEOMETRY = {
            {"logo", "<path d=\"M12 3 20 7.5v9L12 21 4 16.5v-9L12 3Z\"/><path d=\"M12 12 4 7.5M12 12l8-4.5M12 12v9\"/><path d=\"M8 9.7v4.8l4 2.3 4-2.3V9.7\"/>"},
            {"dashboard", "<rect x=\"4\" y=\"4\" width=\"6\" height=\"6\" rx=\"1.4\"/><rect x=\"14\" y=\"4\" width=\"6\" height=\"6\" rx=\"1.4\"/><rect x=\"4\" y=\"14\" width=\"6\" height=\"6\" rx=\"1.4\"/><rect x=\"14\" y=\"14\" width=\"6\" height=\"6\" rx=\"1.4\"/>"},
            {"signalbridge-main", "<path d=\"M12 20v-7\"/><circle cx=\"12\" cy=\"11\" r=\"1.7\"/><path d=\"M8.3 15a5.2 5.2 0 0 1 0-8M15.7 7a5.2 5.2 0 0 1 0 8M5.5 17.8a9 9 0 0 1 0-13.6M18.5 4.2a9 9 0 0 1 0 13.6\"/>"},
            {"receiver-main", "<path d=\"M12 21v-8\"/><path d=\"M7.5 9.5a4.5 4.5 0 0 1 9 0\"/><path d=\"M4.5 10a7.5 7.5 0 0 1 15 0\"/><circle cx=\"12\" cy=\"11\" r=\"2\"/><path d=\"M9 21h6\"/>"},
            {"history", "<path d=\"M5 7.5A8 8 0 1 1 4.4 16\"/><path d=\"M5 4v4h4\"/><path d=\"M12 8v5l3.2 2\"/>"},
            {"doctor", "<path d=\"M7 4v5a4 4 0 0 0 8 0V4\"/><path d=\"M15 9v3.5a4.5 4.5 0 0 0 9 0v-.5\"/><circle cx=\"20\" cy=\"12\" r=\"2.1\"/><path d=\"M5 4h4M13 4h4\"/>"},
            {"region", "<path d=\"M6 20V5\"/><path d=\"M6 6h10l-1.8 3L16 12H6\"/><path d=\"M4 20h9\"/><circle class=\"fill\" cx=\"17.5\" cy=\"17\" r=\"1.3\"/>"},
            {"device", "<path d=\"M12 4 19 8v8l-7 4-7-4V8l7-4Z\"/><path d=\"M12 12 5 8M12 12l7-4M12 12v8\"/>"},
            {"action", "<path d=\"M13 3 5 14h6l-1 7 9-12h-6l1-6Z\"/>"},
            {"user", "<circle cx=\"12\" cy=\"8\" r=\"3.1\"/><path d=\"M5.5 20a6.5 6.5 0 0 1 13 0\"/><path d=\"M17 10.5a3 3 0 0 1 3.5 2.9M3.5 13.4A3 3 0 0 1 7 10.5\"/>"},
            {"settings", "<circle cx=\"12\" cy=\"12\" r=\"3\"/><path d=\"M12 3v2.2M12 18.8V21M4.2 7.5l1.9 1.1M17.9 15.4l1.9 1.1M4.2 16.5l1.9-1.1M17.9 8.6l1.9-1.1M3 12h2.2M18.8 12H21\"/>"},
            {"server-online", "<rect x=\"4\" y=\"5\" width=\"16\" height=\"5\" rx=\"1.4\"/><rect x=\"4\" y=\"14\" width=\"16\" height=\"5\" rx=\"1.4\"/><circle class=\"fill\" cx=\"17\" cy=\"7.5\" r=\"1\"/><circle class=\"fill\" cx=\"17\" cy=\"16.5\" r=\"1\"/>"},
            {"logout", "<path d=\"M10 5H6a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h4\"/><path d=\"M13 8l4 4-4 4\"/><path d=\"M17 12H8\"/>"},
            {"device-overview", "<path d=\"M12 4 19 8v8l-7 4-7-4V8l7-4Z\"/><path d=\"M12 12 5 8M12 12l7-4M12 12v8\"/><circle class=\"fill\" cx=\"4.5\" cy=\"4.5\" r=\"1.2\"/><circle class=\"fill\" cx=\"19.5\" cy=\"19.5\" r=\"1.2\"/><path d=\"M5.3 5.3 8 7M16 17l2.7 1.7\"/>"},
            {"doctor-overview", "<path d=\"M5 6.5h8.5a4.5 4.5 0 0 1 4.5 4.5v7H5V6.5Z\"/><path d=\"M8 10h5M8 13.5h3.8\"/><path d=\"M17 5v5M14.5 7.5h5\"/><path d=\"M14.5 16l1.4-1.4 2.3 2.3 2.8-4\"/>"},
            {"signal-overview", "<path d=\"M12 20v-7\"/><circle cx=\"12\" cy=\"11\" r=\"2\"/><path d=\"M8.6 14.4a4.8 4.8 0 0 1 0-6.8M15.4 7.6a4.8 4.8 0 0 1 0 6.8\"/><path d=\"M5.4 17.6a8.8 8.8 0 0 1 0-12.2M18.6 5.4a8.8 8.8 0 0 1 0 12.2\"/>"},
            {"region-overview", "<path d=\"M6 20V5\"/><path d=\"M6 6h10l-1.7 3 1.7 3H6\"/><path d=\"M4 20h16\"/><path d=\"M9 17c1.6-1 4.4-1 6 0\"/><circle class=\"fill\" cx=\"18\" cy=\"17\" r=\"1.2\"/>"},
            {"action-overview", "<path d=\"M13 3 5 14h6l-1 7 9-12h-6l1-6Z\"/><path d=\"M5 20h5M15 4h4\"/>"},
            {"action-template", "<path d=\"M6 4h9l3 3v13H6V4Z\"/><path d=\"M15 4v4h4\"/><path d=\"M9 11h6M9 14h7M9 17h4\"/>"},
            {"user-overview", "<circle cx=\"9\" cy=\"8\" r=\"2.8\"/><circle cx=\"16\" cy=\"9\" r=\"2.3\"/><path d=\"M3.8 20a5.4 5.4 0 0 1 10.4 0\"/><path d=\"M13.5 19.7a4.7 4.7 0 0 1 6.7-3.8\"/><path d=\"M17 15l1.5 1.5L21 13\"/>"},
            {"signal-device", "<path d=\"M12 20v-7\"/><circle cx=\"12\" cy=\"11\" r=\"1.7\"/><path d=\"M8.8 14a4.5 4.5 0 0 1 0-6M15.2 8a4.5 4.5 0 0 1 0 6\"/><path d=\"M7 20h10\"/>"},
            {"signal-receiver", "<path d=\"M12 20v-6\"/><path d=\"M8 10a4 4 0 0 1 8 0M5 10a7 7 0 0 1 14 0\"/><path d=\"M9 15l3-3 3 3\"/>"},
            {"virtual-block-device", "<path d=\"M12 4 19 8v8l-7 4-7-4V8l7-4Z\"/><path d=\"M12 12 5 8M12 12l7-4M12 12v8\"/><path d=\"M8.2 6.2 15.8 17.8\"/>"},
            {"action-relay", "<circle cx=\"6\" cy=\"12\" r=\"2\"/><circle cx=\"18\" cy=\"7\" r=\"2\"/><circle cx=\"18\" cy=\"17\" r=\"2\"/><path d=\"M8 11.2 16 7.8M8 12.8l8 3.4\"/>"},
            {"critical-issue", "<path d=\"M12 4 21 20H3L12 4Z\"/><path d=\"M12 9v5\"/><circle class=\"fill\" cx=\"12\" cy=\"17\" r=\"1\"/>"},
            {"warning-issue", "<path d=\"M12 4 21 20H3L12 4Z\"/><path d=\"M12 9.5v4\"/><circle class=\"fill\" cx=\"12\" cy=\"16.8\" r=\"1\"/>"},
            {"info-issue", "<circle cx=\"12\" cy=\"12\" r=\"8\"/><path d=\"M12 11v5\"/><circle class=\"fill\" cx=\"12\" cy=\"8\" r=\"1\"/>"},
            {"check-pass", "<circle cx=\"12\" cy=\"12\" r=\"8\"/><path d=\"M8 12.3l2.6 2.6L16.5 9\"/>"},
            {"active-channel", "<path d=\"M12 20v-7\"/><circle cx=\"12\" cy=\"11\" r=\"2\"/><path d=\"M7 7a7 7 0 0 0 0 10M17 7a7 7 0 0 1 0 10\"/>"},
            {"listener-receiver", "<path d=\"M7 17v-5\"/><path d=\"M12 19v-7\"/><path d=\"M17 17v-5\"/><circle cx=\"7\" cy=\"10\" r=\"2\"/><circle cx=\"12\" cy=\"9\" r=\"2\"/><circle cx=\"17\" cy=\"10\" r=\"2\"/><path d=\"M7 12l5 3 5-3\"/>"},
            {"recent-event", "<circle cx=\"12\" cy=\"12\" r=\"8\"/><path d=\"M12 8v4l3 2\"/><path d=\"M4 6l2.2-.3M18 18l2 .4\"/>"},
            {"response-time", "<path d=\"M3 13h4l2-5 3 9 2-6h7\"/>"},
            {"region-controller", "<rect x=\"4\" y=\"5\" width=\"16\" height=\"14\" rx=\"2\"/><path d=\"M8 16V8\"/><path d=\"M8 9h7l-1.2 2.4L15 14H8\"/>"},
            {"active-region", "<path d=\"M6 20V5\"/><path d=\"M6 6h9l-1.4 2.7L15 11H6\"/><path d=\"M13 17l2 2 4-5\"/>"},
            {"action-binding", "<circle cx=\"7\" cy=\"12\" r=\"2.2\"/><circle cx=\"17\" cy=\"7\" r=\"2.2\"/><circle cx=\"17\" cy=\"17\" r=\"2.2\"/><path d=\"M9.2 11.2 14.8 7.8M9.2 12.8l5.6 3.4\"/>"},
            {"today-trigger", "<rect x=\"5\" y=\"5\" width=\"14\" height=\"15\" rx=\"2\"/><path d=\"M8 3v4M16 3v4M5 10h14\"/><path d=\"M13 12l-3 4h3l-1 3\"/>"},
            {"action-total", "<path d=\"M8 7h10M6 12h10M8 17h10\"/><path d=\"M5 5l2 2-2 2M17 11l2 2-2 2\"/>"},
            {"enabled", "<circle cx=\"12\" cy=\"12\" r=\"8\"/><path d=\"M10 8.5 16 12l-6 3.5V8.5Z\"/>"},
            {"success-rate", "<path d=\"M5 19V9M10 19v-6M15 19V6M20 19v-9\"/><path d=\"M4 19h17\"/><path d=\"M5 10l4 2 5-6 5 3\"/>"},
            {"user-total", "<circle cx=\"9\" cy=\"8\" r=\"2.7\"/><circle cx=\"16\" cy=\"9\" r=\"2.2\"/><path d=\"M4 20a5 5 0 0 1 10 0\"/><path d=\"M13.5 20a4.2 4.2 0 0 1 6.7-3.2\"/>"},
            {"current-user", "<circle cx=\"12\" cy=\"8\" r=\"3\"/><path d=\"M5.5 20a6.5 6.5 0 0 1 13 0\"/><circle class=\"fill\" cx=\"17.5\" cy=\"17.5\" r=\"1.2\"/>"},
            {"current-role", "<path d=\"M12 3 19 6v5c0 4.5-2.8 8-7 10-4.2-2-7-5.5-7-10V6l7-3Z\"/><path d=\"M9 12l2 2 4-5\"/>"},
            {"session", "<rect x=\"4\" y=\"6\" width=\"16\" height=\"12\" rx=\"2\"/><path d=\"M8 10h8M8 14h5\"/><circle class=\"fill\" cx=\"17\" cy=\"14\" r=\"1\"/>"},
            {"channel-total", "<path d=\"M5 7h14M5 12h14M5 17h14\"/><circle class=\"fill\" cx=\"3.5\" cy=\"7\" r=\"1\"/><circle class=\"fill\" cx=\"3.5\" cy=\"12\" r=\"1\"/><circle class=\"fill\" cx=\"3.5\" cy=\"17\" r=\"1\"/>"},
            {"channel-with-consumers", "<path d=\"M5 7h7M5 12h6M5 17h7\"/><circle cx=\"17\" cy=\"9\" r=\"2\"/><path d=\"M13.5 19a3.8 3.8 0 0 1 7 0\"/>"},
            {"channel-orphan", "<path d=\"M5 7h8M5 12h6M5 17h8\"/><path d=\"M16 8l4 4-4 4M20 12h-7\"/>"},
            {"channel-error", "<path d=\"M12 4 21 20H3L12 4Z\"/><path d=\"M9 10l6 6M15 10l-6 6\"/>"},
            {"consumer-listener", "<path d=\"M7 18v-5\"/><circle cx=\"7\" cy=\"11\" r=\"2\"/><path d=\"M12 20v-8\"/><circle cx=\"12\" cy=\"10\" r=\"2\"/><path d=\"M17 18v-5\"/><circle cx=\"17\" cy=\"11\" r=\"2\"/>"},
            {"consumer-receiver", "<path d=\"M12 20v-6\"/><path d=\"M8 10a4 4 0 0 1 8 0M5 10a7 7 0 0 1 14 0\"/><circle cx=\"12\" cy=\"12\" r=\"1.8\"/>"},
            {"consumer-relay", "<circle cx=\"6\" cy=\"12\" r=\"2\"/><circle cx=\"18\" cy=\"8\" r=\"2\"/><circle cx=\"18\" cy=\"16\" r=\"2\"/><path d=\"M8 11l8-3M8 13l8 3\"/>"},
            {"consumer-region", "<path d=\"M6 20V5\"/><path d=\"M6 6h9l-1.4 2.7L15 11H6\"/><path d=\"M14 18h5M16.5 15.5V20.5\"/>"},
            {"doctor-ok", "<circle cx=\"12\" cy=\"12\" r=\"8\"/><path d=\"M8 12.5l2.4 2.4 5.8-6\"/>"},
            {"doctor-warning", "<path d=\"M12 4 21 20H3L12 4Z\"/><path d=\"M12 9.5v4\"/><circle class=\"fill\" cx=\"12\" cy=\"16.8\" r=\"1\"/>"},
            {"doctor-error", "<circle cx=\"12\" cy=\"12\" r=\"8\"/><path d=\"M9 9l6 6M15 9l-6 6\"/>"},
            {"receiver-total", "<path d=\"M12 20v-6\"/><path d=\"M8 10a4 4 0 0 1 8 0M5 10a7 7 0 0 1 14 0\"/><path d=\"M6 21h12\"/>"},
            {"receiver-enabled", "<circle cx=\"12\" cy=\"12\" r=\"8\"/><path d=\"M10 8.5 16 12l-6 3.5V8.5Z\"/>"},
            {"receiver-disabled", "<circle cx=\"12\" cy=\"12\" r=\"8\"/><path d=\"M9 8.5v7M15 8.5v7\"/>"},
            {"receiver-outputting", "<path d=\"M3 13h4l2-5 3 9 2-6h7\"/><circle class=\"fill\" cx=\"18\" cy=\"7\" r=\"1.2\"/>"},
            {"receiver-trigger-today", "<rect x=\"5\" y=\"5\" width=\"14\" height=\"15\" rx=\"2\"/><path d=\"M8 3v4M16 3v4M5 10h14\"/><path d=\"M9 15h6\"/>"},
            {"pulse-duration", "<path d=\"M4 13h4l2-5 3 9 2-6h5\"/><path d=\"M18 4v4M16 6h4\"/>"},
            {"redstone-output", "<path d=\"M4 12h4l2-5 4 10 2-5h4\"/><path d=\"M6 18h12\"/><circle class=\"fill\" cx=\"20\" cy=\"12\" r=\"1.1\"/>"},
            {"receiver-row", "<path d=\"M12 20v-6\"/><path d=\"M8 10a4 4 0 0 1 8 0M5 10a7 7 0 0 1 14 0\"/><path d=\"M7 18h10\"/>"},
            {"channel-list", "<path d=\"M6 7h12M6 12h12M6 17h12\"/><circle class=\"fill\" cx=\"3.8\" cy=\"7\" r=\"1\"/><circle class=\"fill\" cx=\"3.8\" cy=\"12\" r=\"1\"/><circle class=\"fill\" cx=\"3.8\" cy=\"17\" r=\"1\"/>"},
            {"refresh", "<path d=\"M19 8a7 7 0 0 0-12-2l-2 2\"/><path d=\"M5 4v4h4\"/><path d=\"M5 16a7 7 0 0 0 12 2l2-2\"/><path d=\"M19 20v-4h-4\"/>"},
            {"more", "<circle class=\"fill\" cx=\"6\" cy=\"12\" r=\"1.5\"/><circle class=\"fill\" cx=\"12\" cy=\"12\" r=\"1.5\"/><circle class=\"fill\" cx=\"18\" cy=\"12\" r=\"1.5\"/>"}
    };

    private WebAdminFrontendScripts() {
    }

    private static String flatIconRegistryJs() {
        StringBuilder js = new StringBuilder("const FLAT_ICON_KEYS=[");
        boolean first = true;
        for (String key : FLAT_ICON_KEYS) {
            if (!first) {
                js.append(',');
            }
            js.append(jsString(key));
            first = false;
        }
        return js.append("];const FLAT_ICON_ASSETS=Object.fromEntries(FLAT_ICON_KEYS.map(key=>[key,key]));").toString();
    }

    private static String flatIconGeometryJs() {
        StringBuilder js = new StringBuilder("const ICON_GEOMETRY={");
        boolean first = true;
        for (String[] entry : FLAT_ICON_GEOMETRY) {
            if (!first) {
                js.append(',');
            }
            js.append(jsString(entry[0])).append(':').append(jsString(entry[1]));
            first = false;
        }
        return js.append("};").toString();
    }
    private static String jsString(String value) {
        return "\"" + String.valueOf(value)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r") + "\"";
    }

    public static String appJs() {
        return new StringBuilder()
                .append(flatIconRegistryJs())
                .append("\n")
                .append(flatIconGeometryJs())
                .append("\n")
                .append("""
                class ApiError extends Error{
                  constructor(status, code, message){super(message || '请求失败');this.status=status;this.code=code || 'ERROR';}
                }
                const appState={me:null,status:null,capabilities:null,channelOptions:null,channelOptionsError:null,channelOptionsDirty:false,onlinePlayerOptions:null,onlinePlayerOptionsError:null,currentDeviceDetail:null,deviceConfigEdit:null,deviceMetadataEdit:null,deviceMetadataLockTimer:null,deviceBasicConfigEdit:null,deviceBasicConfigLockTimer:null,deviceExtendedConfigEdit:null,deviceExtendedConfigLockTimer:null,actionRelayActionsEdit:null,actionRelayActionsLockTimer:null,vbdNativeTriggerEdit:null,vbdNativeTriggerLockTimer:null,interactionItemMatcherEdit:null,interactionItemMatcherLockTimer:null,channelMetadataEdit:null,channelMetadataLockTimer:null,signalListenerBasicConfigEdit:null,signalListenerBasicConfigLockTimer:null,selectionCreateVirtualBlock:null,virtualBlockDelete:null,signalListenerCreate:null,signalListenerDelete:null,selectionTerminalById:{},deviceEditLocks:{},openDeviceMoreMenuId:'',deviceMorePopover:null,deviceFilters:{search:'',type:'ALL',enabled:'ALL',doctor:'ALL',world:'ALL'},signalFilters:{search:'',consumer:'ALL',status:'ALL',sort:'RECENT'},doctorFilters:{search:'',severity:'ALL',objectType:'ALL',jump:'ALL'},historyFilters:{search:'',channel:'ALL',sourceType:'ALL',result:'ALL',range:'ALL',sort:'NEWEST'},userFilters:{search:'',role:'ALL',enabled:'ALL',online:'ALL'},regionFilters:{search:'',world:'ALL',enabled:'ALL',doctor:'ALL',players:'ALL',sort:'NAME'},actionFilters:{search:'',type:'ALL',owner:'ALL',result:'ALL',doctor:'ALL',sort:'NAME'},templateFilters:{search:'',type:'ALL',status:'ALL',favorite:'ALL',sort:'NAME'},advancedDetailOpen:{}};
                appState.modalClosePromise=null;appState.modalDismissPromise=null;appState.modalCloseHandler=null;appState.modalDirtyChecker=null;appState.modalSyncBeforeClose=null;appState.modalDiscardConfirmOpen=false;
                appState.realtime={source:null,status:'DISCONNECTED',reconnectTimer:null,reconnectAttempt:0,lastEventAt:'',lastSeenSeq:0,lastEventId:'',wasDisconnected:false,missed:false,offline:typeof navigator!=='undefined'&&!navigator.onLine,refreshTimers:{},dirtyRoutes:{},pendingRefresh:{},refreshSeq:{},pollTimer:null,pollHash:null};
                function esc(value){return String(value ?? '').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));}
                function isBlank(value){return value===undefined||value===null||String(value).trim()==='';}
                function iconKey(name){
                  const raw=String(name||'').trim().toLowerCase().replace(/_/g,'-');
                  const compact=raw.replace(/[^a-z0-9]/g,'');
                  const aliases={
                    signal:'signalbridge-main',signalbridge:'signalbridge-main',channel:'active-channel',broadcast:'signal-overview',
                    receiver:'receiver-main',listener:'consumer-listener',relay:'action-relay',virtual:'virtual-block-device',auto:'device',
                    cube:'device-overview',deviceoverview:'device-overview',doctoroverview:'doctor-overview',signaloverview:'signal-overview',regionoverview:'region-overview',actionoverview:'action-overview',template:'action-template',templates:'action-template',actiontemplate:'action-template',useroverview:'user-overview',
                    signaldevice:'signal-device',signalemitter:'signal-device',signallistener:'consumer-listener',signalreceiver:'signal-receiver',virtualblockdevice:'virtual-block-device',actionrelay:'action-relay',regioncontroller:'region-controller',
                    critical:'critical-issue',danger:'critical-issue',error:'critical-issue',warning:'warning-issue',info:'info-issue',ok:'check-pass',pass:'check-pass',success:'check-pass',healthy:'doctor-ok',
                    active:'active-region',consumers:'listener-receiver',consumer:'listener-receiver',response:'response-time',avgresponse:'response-time',today:'today-trigger',lightning:'action-overview',pulse:'pulse-duration',
                    users:'user-total',role:'current-role',status:'doctor-ok',enabled:'enabled',disabled:'receiver-disabled',pause:'receiver-disabled',play:'enabled',
                    orphan:'channel-orphan',search:'doctor-overview',eye:'current-user',login:'logout',plus:'receiver-total',upload:'action-binding',download:'channel-total',archive:'history',copy:'session',edit:'settings',pencil:'settings',close:'channel-error',chevronleft:'logout',chevronright:'logout','import':'action-binding','export':'channel-total',
                    key:'current-role',chest:'channel-total',door:'logout',custom1:'device-overview'
                  };
                  return FLAT_ICON_ASSETS[raw]?raw:(aliases[raw]||aliases[compact]||raw||'doctor-ok');
                }
                function iconClassName(name){return iconKey(name).replace(/[^a-z0-9_-]/g,'-');}
                function iconAssetKey(name){const key=iconKey(name);return FLAT_ICON_ASSETS[key]?key:'doctor-ok';}
                function iconSvgBody(key){return ICON_GEOMETRY[key]||ICON_GEOMETRY['doctor-ok'];}
                function icon(name){
                  const key=iconAssetKey(name);
                  const classes=`icon-img icon-geo icon-${iconClassName(name)} icon-asset-${key}`;
                  return `<svg class="${classes}" viewBox="0 0 24 24" aria-hidden="true" focusable="false">${iconSvgBody(key)}</svg>`;
                }
                function hydrateIcons(root=document){
                  if(Object.keys(ICON_GEOMETRY).length===0)document.documentElement.classList.add('flat-icons-missing');
                  const scope=root&&typeof root.querySelectorAll==='function'?root:document;
                  scope.querySelectorAll('[data-icon]').forEach(el=>{el.innerHTML=icon(el.dataset.icon);});
                }
                function renderIcons(root=document){hydrateIcons(root);}
                function statusClass(value){const v=String(value||'').toUpperCase();if(v==='ERROR'||v==='FAILED')return'error';if(v==='WARNING')return'warning';if(v==='INFO'||v==='UNKNOWN')return'info';return'ok';}
                function pill(value){return `<span class="pill ${statusClass(value)}">${esc(labelStatus(value))}</span>`}
                function textPill(text, kind='info'){return `<span class="pill ${esc(kind)}">${esc(text)}</span>`}
                function labelStatus(value){const v=String(value||'UNKNOWN').toUpperCase();return {OK:'正常',INFO:'信息',WARNING:'警告',ERROR:'错误',UNKNOWN:'未知',SUCCESS:'成功',FAILED:'失败',SKIPPED:'跳过'}[v]||value;}
                function labelBool(value){return value?'已启用':'已禁用';}
                function labelRuntimeBool(value){if(value===true)return '是';if(value===false)return '否';return '未知';}
                function labelType(value){const v=String(value||'UNKNOWN').toUpperCase();return {SIGNAL_EMITTER:'信号发射器',SIGNAL_RECEIVER:'信号接收器',ACTION_RELAY:'动作继电器',VIRTUAL_BLOCK_DEVICE:'虚拟方块设备',REGION_CONTROLLER:'区域控制器',UNKNOWN:'未知设备'}[v]||value||'未知设备';}
                function labelSourceType(value){return {DEVICE:'设备',LISTENER:'监听器',RECEIVER:'信号接收器',ACTION_RELAY:'动作继电器',REGION:'区域',COMMAND:'命令',MANUAL:'手动',SYSTEM:'系统',UNKNOWN:'未知来源'}[String(value||'UNKNOWN').toUpperCase()]||value||'-';}
                function labelEndpointType(value){return {DEVICE:'触发设备',LISTENER:'监听器',RECEIVER:'信号接收器',ACTION_RELAY:'动作继电器',REGION:'区域',COMMAND:'命令',SYSTEM:'系统',UNKNOWN:'未知节点'}[String(value||'UNKNOWN').toUpperCase()]||value||'未知节点';}
                function labelActionType(value){return {COMMAND:'命令动作',MESSAGE:'消息动作',SOUND:'音效动作',SIGNAL:'信号动作',UNKNOWN:'未知动作'}[String(value||'UNKNOWN').toUpperCase()]||value||'未知动作';}
                function labelOwnerType(value){return {LISTENER:'监听器',ACTION_RELAY:'动作继电器',REGION_ENTER:'区域进入动作',REGION_EXIT:'区域离开动作',REGION_STAY:'区域停留动作',REGION:'区域',DEVICE:'设备',SYSTEM:'系统',UNKNOWN:'未知归属'}[String(value||'UNKNOWN').toUpperCase()]||value||'未知归属';}
                function labelTargetFilter(value){return {ALL:'全部玩家',OP:'管理员',TAG:'标签过滤',TEAM:'队伍过滤',UNKNOWN:'未知'}[String(value||'UNKNOWN').toUpperCase()]||value||'未设置';}
                function labelSubType(value){const v=String(value||'').toLowerCase();return {signal_listener:'监听器',signal_emitter:'信号发射器',signal_receiver:'信号接收器',action_relay:'动作继电器',virtual_block_device:'虚拟方块设备'}[v]||labelType(value);}
                function labelServerStatus(value){return {RUNNING:'运行中',STOPPED:'已停止',STARTING:'启动中',UNKNOWN:'未知'}[String(value||'').toUpperCase()]||value||'-';}
                function labelAccessMode(value){return {LOCAL_ONLY:'本机模式',LAN_DEV:'局域网开发模式',MULTIPLAYER_DEV:'多人开发模式'}[String(value||'').toUpperCase()]||value||'-';}
                function labelRole(value){return {OWNER:'所有者',EDITOR:'编辑者',TESTER:'测试者',VIEWER:'查看者'}[String(value||'').toUpperCase()]||value||'-';}
                function labelRoleFull(value){const id=String(value||'').toUpperCase();return `${labelRole(id)}${id&&id!=='-'?`（${id}）`:''}`;}
                function labelEnabledState(value){return value?'启用':'禁用';}
                function labelOnline(value){return value?'在线':'离线';}
                function labelChannel(value){return isBlank(value)?'未设置':value;}
                function labelChannelType(value){return {DEVICE:'设备频道',REGION:'区域频道',SYSTEM:'系统频道',GAME:'游戏流程频道'}[String(value||'').toUpperCase()]||'频道';}
                function labelConsumerFilter(value){return {ALL:'全部',HAS_CONSUMER:'有消费者',NO_CONSUMER:'无消费者',HAS_LISTENER:'有监听器',HAS_RECEIVER:'有接收器',HAS_RELAY:'有动作继电器'}[value]||value;}
                function labelSignalStatusFilter(value){return {ALL:'全部',RECENT:'最近有事件',NO_RECENT:'暂无事件',WARNING:'有警告'}[value]||value;}
                function labelSignalSort(value){return {RECENT:'最近触发时间',CHANNEL:'频道名',CONSUMERS:'消费者数量'}[value]||value;}
                function labelObjectType(value){return {DEVICE:'设备',CHANNEL:'频道',LISTENER:'监听器',RECEIVER:'接收器',ACTION_RELAY:'动作继电器',ACTION:'动作',REGION:'区域',SYSTEM:'系统',UNKNOWN:'未知'}[String(value||'UNKNOWN').toUpperCase()]||value||'未知';}
                function labelHistoryRange(value){return {ALL:'全部',M10:'最近 10 分钟',H1:'最近 1 小时',H24:'最近 24 小时'}[value]||value;}
                function labelHistorySort(value){return {NEWEST:'最新优先',OLDEST:'最旧优先'}[value]||value;}
                function consumerCount(c){return Number(c?.listenerCount||0)+Number(c?.receiverCount||0)+Number(c?.actionRelayCount||0);}
                async function loadSignalChannelOptions(force=false){
                  if(!force&&!appState.channelOptionsDirty&&Array.isArray(appState.channelOptions))return appState.channelOptions;
                  try{return storeSignalChannelOptions(await api('/api/signals/channels'));}
                  catch(err){appState.channelOptions=Array.isArray(appState.channelOptions)?appState.channelOptions:[];appState.channelOptionsError=err.message||'频道候选加载失败';}
                  return appState.channelOptions||[];
                }
                function storeSignalChannelOptions(channels){
                  appState.channelOptions=Array.isArray(channels)?channels:[];
                  appState.channelOptionsError=null;
                  appState.channelOptionsDirty=false;
                  return appState.channelOptions;
                }
                function markChannelOptionsDirty(event){
                  const type=String(event?.type||''), target=String(event?.payload?.targetType||'').toLowerCase(), source=String(event?.sourceType||event?.payload?.deviceType||'').toLowerCase();
                  const types=['signal_channel_changed','channel_metadata_changed','signal_listener_changed','signal_listener_config_changed','device_registered','device_removed','device_changed','device_config_changed','virtual_block_device_changed','selection_completed','selection_cancelled','selection_failed','action_changed','signal_listener_action_changed','config_changed','signal_history_appended','sync_required'];
                  const targets=['device_basic_config','device_extended_config','interaction_item_matcher','virtual_block_device_triggers','action_relay_actions','channel_metadata','signal_listener_basic_config','virtual_block_device'];
                  const sources=['signal_receiver','action_relay','signal_emitter','virtual_block_device'];
                  if(types.includes(type)||target.includes('channel')||targets.includes(target)||sources.includes(source))appState.channelOptionsDirty=true;
                }
                function normalizeChannelName(value){return String(value||'').trim();}
                function findChannelOption(channel,options){const name=normalizeChannelName(channel).toLowerCase();if(!name)return null;return (options||[]).find(c=>String(c.channel||'').trim().toLowerCase()===name)||null;}
                function channelOptionLabel(c){const parts=[`消费者：${consumerCount(c)}`];if(!isBlank(c?.lastTriggeredAt))parts.push(`最近触发：${formatDateTime(c.lastTriggeredAt)}`);if(!isBlank(c?.doctorStatus))parts.push(`诊断：${labelStatus(c.doctorStatus)}`);return parts.join(' · ');}
                function filteredChannelOptions(options,channel){const query=normalizeChannelName(channel).toLowerCase();const list=(options||[]).filter(c=>!query||String(c.channel||'').toLowerCase().includes(query));return list.slice(0,50);}
                function channelComboQuery(draft,key=''){
                  if(!draft)return '';
                  if(key!==''&&key!==null&&key!==undefined){
                    const active=(draft.channelComboSearchActive||{})[key], query=(draft.channelComboQuery||{})[key];
                    return active?query||'':'';
                  }
                  return draft.channelComboSearchActive?draft.channelComboQuery||'':'';
                }
                function setChannelComboQuery(draft,query='',key=''){
                  if(!draft)return;
                  if(key!==''&&key!==null&&key!==undefined){
                    draft.channelComboSearchActive=draft.channelComboSearchActive||{};
                    draft.channelComboQuery=draft.channelComboQuery||{};
                    draft.channelComboSearchActive[key]=!isBlank(query);
                    draft.channelComboQuery[key]=query||'';
                    return;
                  }
                  draft.channelComboSearchActive=!isBlank(query);
                  draft.channelComboQuery=query||'';
                }
                function resetChannelComboQuery(draft,key=''){setChannelComboQuery(draft,'',key);}
                async function loadOnlinePlayerOptions(force=false){
                  if(!force&&Array.isArray(appState.onlinePlayerOptions))return appState.onlinePlayerOptions;
                  try{appState.onlinePlayerOptions=await api('/api/webadmin/online-players');appState.onlinePlayerOptionsError=null;}
                  catch(err){appState.onlinePlayerOptions=Array.isArray(appState.onlinePlayerOptions)?appState.onlinePlayerOptions:[];appState.onlinePlayerOptionsError=err.message||'在线玩家候选加载失败';}
                  return appState.onlinePlayerOptions||[];
                }
                function filteredOnlinePlayerOptions(options,playerName){const query=String(playerName||'').trim().toLowerCase();const list=(options||[]).filter(p=>!query||String(p.name||'').toLowerCase().includes(query)||String(p.uuid||'').toLowerCase().includes(query));return list.slice(0,50);}
                function channelComboOptionsHtml(deviceId,draft){
                  if(draft.channelOptionsError||appState.channelOptionsError)return '<div class="channel-combo-empty">频道候选加载失败，仍可手动输入新的频道名。</div>';
                  const options=filteredChannelOptions(draft.channelOptions||appState.channelOptions||[],channelComboQuery(draft)), current=normalizeChannelName(draft.channel).toLowerCase(), active=Math.max(0,Number(draft.channelComboIndex||0));
                  if(options.length===0)return '<div class="channel-combo-empty">没有匹配的已有频道，可直接保存为新频道</div>';
                  return options.map((c,index)=>`<button type="button" class="channel-combo-option ${index===active?'active':''} ${String(c.channel||'').trim().toLowerCase()===current?'selected':''}" role="option" onmousedown="event.preventDefault()" onclick='selectDeviceBasicConfigChannel(${jsString(deviceId)},${jsString(c.channel||'')})'><strong>${esc(c.channel||'未命名频道')}</strong><span>${esc(channelOptionLabel(c))}</span></button>`).join('');
                }
                function renderDeviceBasicConfigChannelCombo(deviceId,draft){
                  const open=draft.channelComboOpen?' open':'';
                  return `<div id="basic-channel-combo" class="channel-combo${open}"><div class="channel-combo-control"><input id="basic-channel" class="input" maxlength="128" value="${esc(draft.channel||'')}" placeholder="选择已有频道或输入新频道" autocomplete="off" role="combobox" aria-expanded="${draft.channelComboOpen?'true':'false'}" aria-controls="basic-channel-menu" onfocus='openDeviceBasicConfigChannelMenu(${jsString(deviceId)})' oninput='updateDeviceBasicConfigDraftFromForm(${jsString(deviceId)},true)' onkeydown='handleDeviceBasicConfigChannelKey(event,${jsString(deviceId)})'><button class="channel-combo-toggle" type="button" onclick='toggleDeviceBasicConfigChannelMenu(${jsString(deviceId)})' aria-label="显示已有频道">⌄</button></div><div id="basic-channel-menu" class="channel-combo-menu" role="listbox">${channelComboOptionsHtml(deviceId,draft)}</div></div>`;
                }
                function channelHintHtml(channel,options,loadError){
                  if(loadError)return `<span class="muted">频道候选加载失败，仍可手动输入新的频道名。</span>`;
                  const name=normalizeChannelName(channel);
                  if(!name)return '<span class="muted">可选择已有频道，也可以输入新的频道名。新频道不会自动创建消费者。</span>';
                  const found=findChannelOption(name,options);
                  if(!found)return '<span class="muted">该频道当前未在系统中发现。保存后设备会使用此频道，但不会自动创建监听器、接收器或动作继电器。</span>';
                  const count=consumerCount(found), bits=[`已选择已有频道：${found.channel||name}`, `消费者：${count}`];
                  if(!isBlank(found.lastTriggeredAt))bits.push(`最近触发：${formatDateTime(found.lastTriggeredAt)}`);
                  if(['WARNING','ERROR'].includes(String(found.doctorStatus||'').toUpperCase()))bits.push(`诊断：${labelStatus(found.doctorStatus)}`);
                  if(count===0)bits.push('该频道当前暂无消费者。');
                  return bits.map(esc).join('<br>');
                }
                function updateDeviceBasicConfigDraftFromForm(deviceId,openMenu=false){
                  const draft=appState.deviceBasicConfigEdit;
                  if(!draft||draft.deviceId!==deviceId)return;
                  draft.enabled=(document.getElementById('basic-enabled')?.value||'false')==='true';
                  draft.channel=document.getElementById('basic-channel')?.value||'';
                  if(openMenu){draft.channelComboOpen=true;draft.channelComboIndex=0;setChannelComboQuery(draft,draft.channel);}
                  const hint=document.getElementById('basic-channel-hint');
                  if(hint)hint.innerHTML=channelHintHtml(draft.channel,draft.channelOptions||appState.channelOptions||[],draft.channelOptionsError||appState.channelOptionsError);
                  syncDeviceBasicConfigChannelCombo(deviceId);
                }
                function syncDeviceBasicConfigChannelCombo(deviceId){
                  const draft=appState.deviceBasicConfigEdit;if(!draft||draft.deviceId!==deviceId)return;
                  const combo=document.getElementById('basic-channel-combo'), menu=document.getElementById('basic-channel-menu'), input=document.getElementById('basic-channel');
                  if(combo)combo.classList.toggle('open',!!draft.channelComboOpen);
                  if(input)input.setAttribute('aria-expanded',draft.channelComboOpen?'true':'false');
                  if(menu)menu.innerHTML=channelComboOptionsHtml(deviceId,draft);
                }
                function openDeviceBasicConfigChannelMenu(deviceId){const draft=appState.deviceBasicConfigEdit;if(!draft||draft.deviceId!==deviceId)return;updateDeviceBasicConfigDraftFromForm(deviceId,false);draft.channelComboOpen=true;resetChannelComboQuery(draft);syncDeviceBasicConfigChannelCombo(deviceId);}
                function closeDeviceBasicConfigChannelMenu(deviceId){const draft=appState.deviceBasicConfigEdit;if(!draft||draft.deviceId!==deviceId)return;draft.channelComboOpen=false;syncDeviceBasicConfigChannelCombo(deviceId);}
                function toggleDeviceBasicConfigChannelMenu(deviceId){const draft=appState.deviceBasicConfigEdit;if(!draft||draft.deviceId!==deviceId)return;updateDeviceBasicConfigDraftFromForm(deviceId,false);draft.channelComboOpen=!draft.channelComboOpen;if(draft.channelComboOpen)resetChannelComboQuery(draft);syncDeviceBasicConfigChannelCombo(deviceId);document.getElementById('basic-channel')?.focus();}
                function selectDeviceBasicConfigChannel(deviceId,channel){const draft=appState.deviceBasicConfigEdit;if(!draft||draft.deviceId!==deviceId)return;draft.channel=channel||'';draft.channelComboOpen=false;draft.channelComboIndex=0;resetChannelComboQuery(draft);const input=document.getElementById('basic-channel');if(input)input.value=draft.channel;const hint=document.getElementById('basic-channel-hint');if(hint)hint.innerHTML=channelHintHtml(draft.channel,draft.channelOptions||appState.channelOptions||[],draft.channelOptionsError||appState.channelOptionsError);syncDeviceBasicConfigChannelCombo(deviceId);}
                function handleDeviceBasicConfigChannelKey(event,deviceId){
                  const draft=appState.deviceBasicConfigEdit;if(!draft||draft.deviceId!==deviceId)return;
                  const options=filteredChannelOptions(draft.channelOptions||appState.channelOptions||[],channelComboQuery(draft));
                  if(event.key==='Escape'){event.preventDefault();event.stopPropagation();draft.channelComboOpen=false;syncDeviceBasicConfigChannelCombo(deviceId);return;}
                  if(event.key==='ArrowDown'||event.key==='ArrowUp'){event.preventDefault();draft.channelComboOpen=true;const max=Math.max(0,options.length-1), next=event.key==='ArrowDown'?Number(draft.channelComboIndex||0)+1:Number(draft.channelComboIndex||0)-1;draft.channelComboIndex=Math.min(max,Math.max(0,next));syncDeviceBasicConfigChannelCombo(deviceId);return;}
                  if(event.key==='Enter'&&draft.channelComboOpen&&options.length>0){event.preventDefault();selectDeviceBasicConfigChannel(deviceId,options[Math.min(options.length-1,Number(draft.channelComboIndex||0))].channel);return;}
                }
                function extendedFieldId(field){return String(field||'').replace(/[^a-zA-Z0-9_-]/g,'_');}
                function isExtendedChannelField(field){return ['interactChannel','successChannel','failChannel'].includes(String(field||''));}
                function isExtendedTickField(field){return ['interactionCooldownTicks','pulseTicks','cooldownTicks'].includes(String(field||''));}
                function extendedEditableFields(draft){return draft&&Array.isArray(draft.editableFields)?draft.editableFields:(draft?.supportedFields||[]);}
                function extendedFieldEditable(draft,field){return !!(draft&&draft.lockId&&extendedEditableFields(draft).includes(field));}
                function deviceExtendedRuntimeNote(cfg){
                  if(!cfg)return '';
                  const state=String(cfg.runtimeState||''), reason=cfg.unsupportedReason||'', block=(cfg.blockId&&!String(reason).includes('当前方块'))?`当前方块：${cfg.blockId}`:'', be=cfg.blockEntityType?`方块实体：${cfg.blockEntityType}`:'';
                  if(!state||state==='ready'||state==='unsupported')return reason?`<div class="readonly-note">${esc(reason)}</div>`:'';
                  return `<div class="readonly-note danger" data-device-runtime-state="${esc(state)}">${esc([reason,block,be].filter(Boolean).join(' · '))}</div>`;
                }
                function makeDeviceExtendedConfigDraft(deviceId,cfg,lock,channelOptions){
                  const draft={deviceId,values:{...(cfg.values||{})},originalValues:{...(cfg.values||{})},supportedFields:[...(cfg.supportedFields||[])],editableFields:[...(cfg.editableFields||cfg.supportedFields||[])],fieldLabels:{...(cfg.fieldLabels||{})},clearableFields:{...(cfg.clearableFields||{})},fieldDisabledReasons:{...(cfg.fieldDisabledReasons||{})},runtimeState:cfg.runtimeState||'',worldAvailable:cfg.worldAvailable,chunkLoaded:cfg.chunkLoaded,blockEntityLoaded:cfg.blockEntityLoaded,blockEntityType:cfg.blockEntityType||'',blockId:cfg.blockId||'',unsupportedReason:cfg.unsupportedReason||'',clear:{},channelOptions,channelOptionsError:appState.channelOptionsError,channelComboOpen:{},channelComboIndex:{},channelComboQuery:{},channelComboSearchActive:{},expectedFingerprint:cfg.expectedFingerprint||'',lockId:lock?.lockId||'',lock:lock||null,errors:[],saving:false,conflict:null};
                  markModalInitialSnapshot('device_extended_config',draft);
                  return draft;
                }
                function extendedChannelComboOptionsHtml(deviceId,field,draft){
                  if(draft.channelOptionsError||appState.channelOptionsError)return '<div class="channel-combo-empty">频道候选加载失败，仍可手动输入新的频道名。</div>';
                  const value=(draft.values||{})[field]||'', options=filteredChannelOptions(draft.channelOptions||appState.channelOptions||[],channelComboQuery(draft,field)), current=normalizeChannelName(value).toLowerCase(), indexes=draft.channelComboIndex||{}, active=Math.max(0,Number(indexes[field]||0));
                  if(options.length===0)return '<div class="channel-combo-empty">没有匹配的已有频道，可直接保存为新频道</div>';
                  return options.map((c,index)=>`<button type="button" class="channel-combo-option ${index===active?'active':''} ${String(c.channel||'').trim().toLowerCase()===current?'selected':''}" role="option" onmousedown="event.preventDefault()" onclick='selectDeviceExtendedConfigChannel(${jsString(deviceId)},${jsString(field)},${jsString(c.channel||'')})'><strong>${esc(c.channel||'未命名频道')}</strong><span>${esc(channelOptionLabel(c))}</span></button>`).join('');
                }
                function renderDeviceExtendedConfigChannelCombo(deviceId,field,draft){
                  const id=extendedFieldId(field), open=(draft.channelComboOpen||{})[field]?' open':'', value=(draft.values||{})[field]||'';
                  const disabled=extendedFieldEditable(draft,field)?'':'disabled';
                  return `<div id="extended-channel-combo-${id}" class="channel-combo extended-channel-combo${open}"><div class="channel-combo-control"><input id="extended-${id}" class="input" maxlength="128" value="${esc(value)}" ${disabled} placeholder="选择已有频道或输入新频道" autocomplete="off" role="combobox" aria-expanded="${(draft.channelComboOpen||{})[field]?'true':'false'}" aria-controls="extended-${id}-menu" onfocus='openDeviceExtendedConfigChannelMenu(${jsString(deviceId)},${jsString(field)})' oninput='updateDeviceExtendedConfigDraftFromForm(${jsString(deviceId)},${jsString(field)})' onkeydown='handleDeviceExtendedConfigChannelKey(event,${jsString(deviceId)},${jsString(field)})'><button class="channel-combo-toggle" type="button" ${disabled} onclick='toggleDeviceExtendedConfigChannelMenu(${jsString(deviceId)},${jsString(field)})' aria-label="显示已有频道">⌄</button></div><div id="extended-${id}-menu" class="channel-combo-menu" role="listbox">${extendedChannelComboOptionsHtml(deviceId,field,draft)}</div></div>`;
                }
                function updateDeviceExtendedConfigDraftFromForm(deviceId,openField=''){
                  const draft=appState.deviceExtendedConfigEdit;if(!draft||draft.deviceId!==deviceId)return;
                  draft.values=draft.values||{};draft.clear=draft.clear||{};draft.channelComboOpen=draft.channelComboOpen||{};draft.channelComboIndex=draft.channelComboIndex||{};
                  (draft.supportedFields||[]).forEach(field=>{
                    const id=extendedFieldId(field);
                    if(isExtendedChannelField(field)){
                      draft.values[field]=document.getElementById(`extended-${id}`)?.value||'';
                      draft.clear[field]=!!document.getElementById(`extended-clear-${id}`)?.checked;
                    }else if(isExtendedTickField(field)){
                      draft.values[field]=document.getElementById(`extended-${id}`)?.value||'';
                    }
                  });
                  if(openField){draft.channelComboOpen[openField]=true;draft.channelComboIndex[openField]=0;setChannelComboQuery(draft,(draft.values||{})[openField]||'',openField);}
                  (draft.supportedFields||[]).filter(isExtendedChannelField).forEach(field=>{
                    const id=extendedFieldId(field), hint=document.getElementById(`extended-${id}-hint`);
                    if(hint)hint.innerHTML=channelHintHtml((draft.values||{})[field],draft.channelOptions||appState.channelOptions||[],draft.channelOptionsError||appState.channelOptionsError);
                    syncDeviceExtendedConfigChannelCombo(deviceId,field);
                  });
                }
                function syncDeviceExtendedConfigChannelCombo(deviceId,field){
                  const draft=appState.deviceExtendedConfigEdit;if(!draft||draft.deviceId!==deviceId)return;
                  const id=extendedFieldId(field), combo=document.getElementById(`extended-channel-combo-${id}`), menu=document.getElementById(`extended-${id}-menu`), input=document.getElementById(`extended-${id}`);
                  if(combo)combo.classList.toggle('open',!!(draft.channelComboOpen||{})[field]);
                  if(input)input.setAttribute('aria-expanded',(draft.channelComboOpen||{})[field]?'true':'false');
                  if(menu)menu.innerHTML=extendedChannelComboOptionsHtml(deviceId,field,draft);
                }
                function openDeviceExtendedConfigChannelMenu(deviceId,field){const draft=appState.deviceExtendedConfigEdit;if(!draft||draft.deviceId!==deviceId)return;updateDeviceExtendedConfigDraftFromForm(deviceId,'');draft.channelComboOpen=draft.channelComboOpen||{};draft.channelComboOpen[field]=true;resetChannelComboQuery(draft,field);syncDeviceExtendedConfigChannelCombo(deviceId,field);}
                function closeDeviceExtendedConfigChannelMenu(deviceId,field){const draft=appState.deviceExtendedConfigEdit;if(!draft||draft.deviceId!==deviceId)return;draft.channelComboOpen=draft.channelComboOpen||{};draft.channelComboOpen[field]=false;syncDeviceExtendedConfigChannelCombo(deviceId,field);}
                function toggleDeviceExtendedConfigChannelMenu(deviceId,field){const draft=appState.deviceExtendedConfigEdit;if(!draft||draft.deviceId!==deviceId)return;updateDeviceExtendedConfigDraftFromForm(deviceId,'');draft.channelComboOpen=draft.channelComboOpen||{};draft.channelComboOpen[field]=!draft.channelComboOpen[field];if(draft.channelComboOpen[field])resetChannelComboQuery(draft,field);syncDeviceExtendedConfigChannelCombo(deviceId,field);document.getElementById(`extended-${extendedFieldId(field)}`)?.focus();}
                function selectDeviceExtendedConfigChannel(deviceId,field,channel){const draft=appState.deviceExtendedConfigEdit;if(!draft||draft.deviceId!==deviceId)return;draft.values=draft.values||{};draft.clear=draft.clear||{};draft.channelComboOpen=draft.channelComboOpen||{};draft.channelComboIndex=draft.channelComboIndex||{};draft.values[field]=channel||'';draft.clear[field]=false;draft.channelComboOpen[field]=false;draft.channelComboIndex[field]=0;resetChannelComboQuery(draft,field);const id=extendedFieldId(field), input=document.getElementById(`extended-${id}`), clear=document.getElementById(`extended-clear-${id}`);if(input)input.value=draft.values[field];if(clear)clear.checked=false;const hint=document.getElementById(`extended-${id}-hint`);if(hint)hint.innerHTML=channelHintHtml(draft.values[field],draft.channelOptions||appState.channelOptions||[],draft.channelOptionsError||appState.channelOptionsError);syncDeviceExtendedConfigChannelCombo(deviceId,field);}
                function handleDeviceExtendedConfigChannelKey(event,deviceId,field){
                  const draft=appState.deviceExtendedConfigEdit;if(!draft||draft.deviceId!==deviceId)return;
                  const options=filteredChannelOptions(draft.channelOptions||appState.channelOptions||[],channelComboQuery(draft,field));
                  draft.channelComboOpen=draft.channelComboOpen||{};draft.channelComboIndex=draft.channelComboIndex||{};
                  if(event.key==='Escape'){event.preventDefault();event.stopPropagation();draft.channelComboOpen[field]=false;syncDeviceExtendedConfigChannelCombo(deviceId,field);return;}
                  if(event.key==='ArrowDown'||event.key==='ArrowUp'){event.preventDefault();draft.channelComboOpen[field]=true;const max=Math.max(0,options.length-1), next=event.key==='ArrowDown'?Number(draft.channelComboIndex[field]||0)+1:Number(draft.channelComboIndex[field]||0)-1;draft.channelComboIndex[field]=Math.min(max,Math.max(0,next));syncDeviceExtendedConfigChannelCombo(deviceId,field);return;}
                  if(event.key==='Enter'&&draft.channelComboOpen[field]&&options.length>0){event.preventDefault();selectDeviceExtendedConfigChannel(deviceId,field,options[Math.min(options.length-1,Number(draft.channelComboIndex[field]||0))].channel);return;}
                }
                function maybeReleaseDeviceExtendedConfigEditForRoute(hash){const draft=appState.deviceExtendedConfigEdit;if(!draft)return;if(String(hash||'').startsWith('#/devices/')){const info=detailRoute(String(hash).substring('#/devices/'.length),'#/devices');if(sameDeviceRef(info.id,draft.deviceId))return;}releaseDeviceExtendedConfigLock(draft,true);appState.deviceExtendedConfigEdit=null;stopDeviceExtendedConfigLockHeartbeat();}
                async function startDeviceExtendedConfigEdit(deviceId){if(!canEditDeviceExtendedConfig())return;try{const cfg=await api(`/api/webadmin/device-extended-config/${encodeURIComponent(deviceId)}`), fields=cfg.supportedFields||[], editable=cfg.editableFields||fields;if(cfg.supported===false||!fields.length){toast(cfg.unsupportedReason||'该设备类型暂无可编辑扩展配置。');return;}let lock={};if(editable.length){const result=await api('/api/webadmin/edit-locks/acquire',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'device_extended_config',targetId:deviceId})});if(!result.success){toast(result.message||'无法获取编辑锁');await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});return;}lock=result.data?.lock||{};}else{toast(cfg.unsupportedReason||'当前运行状态下只能查看类型专属配置快照。');}const channelOptions=await loadSignalChannelOptions();appState.deviceExtendedConfigEdit=makeDeviceExtendedConfigDraft(deviceId,cfg,lock,channelOptions);if(appState.deviceExtendedConfigEdit.lockId)scheduleDeviceExtendedConfigLockHeartbeat();await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});showDeviceExtendedConfigEditModal(deviceId);}catch(err){toast(err.message||'无法获取编辑锁');}}
                async function cancelDeviceExtendedConfigEdit(deviceId){const draft=appState.deviceExtendedConfigEdit;if(draft&&draft.deviceId===deviceId){await releaseDeviceExtendedConfigLock(draft,false);appState.deviceExtendedConfigEdit=null;stopDeviceExtendedConfigLockHeartbeat();}await dismissWebAdminModal();await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});}
                async function reloadDeviceExtendedConfigAfterConflict(deviceId){const draft=appState.deviceExtendedConfigEdit;if(draft&&draft.deviceId===deviceId)await releaseDeviceExtendedConfigLock(draft,true);appState.deviceExtendedConfigEdit=null;stopDeviceExtendedConfigLockHeartbeat();await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});}
                function scheduleDeviceExtendedConfigLockHeartbeat(){stopDeviceExtendedConfigLockHeartbeat();appState.deviceExtendedConfigLockTimer=setTimeout(async()=>{await heartbeatDeviceExtendedConfigLock();if(appState.deviceExtendedConfigEdit)scheduleDeviceExtendedConfigLockHeartbeat();},30000);}
                function stopDeviceExtendedConfigLockHeartbeat(){if(appState.deviceExtendedConfigLockTimer){clearTimeout(appState.deviceExtendedConfigLockTimer);appState.deviceExtendedConfigLockTimer=null;}}
                async function heartbeatDeviceExtendedConfigLock(){const draft=appState.deviceExtendedConfigEdit;if(!draft||!draft.lockId)return;try{const result=await api('/api/webadmin/edit-locks/heartbeat',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'device_extended_config',targetId:draft.deviceId,lockId:draft.lockId})});if(result.success){draft.lock=result.data?.lock||draft.lock;appState.deviceExtendedConfigEdit=draft;return;}draft.errors=[{message:result.message||'编辑锁续期失败'}];appState.deviceExtendedConfigEdit=draft;stopDeviceExtendedConfigLockHeartbeat();await renderDeviceDetail(currentDeviceRouteArg(draft.deviceId),{silent:true});}catch(err){draft.errors=[{message:err.message||'编辑锁续期失败'}];appState.deviceExtendedConfigEdit=draft;stopDeviceExtendedConfigLockHeartbeat();}}
                async function releaseDeviceExtendedConfigLock(draft,silent){if(!draft||!draft.lockId)return;try{await api('/api/webadmin/edit-locks/release',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'device_extended_config',targetId:draft.deviceId,lockId:draft.lockId})});}catch(err){if(!silent)toast(err.message||'编辑锁释放失败，将等待自动过期。');}}
                function deviceExtendedConfigPatchBody(draft){const body={expectedFingerprint:draft.expectedFingerprint||'',lockId:draft.lockId||''};const original=draft.originalValues||{};const addChannel=(field,value,clearName,valueName)=>{if((draft.clear||{})[field]){body[clearName]=true;return;}const text=String(value??'').trim(), before=String(original[field]??'').trim();if(text||before)body[valueName]=value||'';};extendedEditableFields(draft).forEach(field=>{const value=(draft.values||{})[field];if(field==='interactChannel')addChannel(field,value,'clearInteractChannel','interactChannel');else if(field==='successChannel')addChannel(field,value,'clearSuccessChannel','successChannel');else if(field==='failChannel')addChannel(field,value,'clearFailChannel','failChannel');else if(field==='interactionCooldownTicks')body.interactionCooldownTicks=Number(value);else if(field==='pulseTicks')body.pulseTicks=Number(value);else if(field==='cooldownTicks')body.cooldownTicks=Number(value);});return body;}
                async function saveDeviceExtendedConfig(deviceId){const draft=appState.deviceExtendedConfigEdit||{deviceId};if(!draft.lockId){toast(draft.unsupportedReason||'当前运行状态下类型专属配置只读。');showDeviceExtendedConfigEditModal(deviceId);return;}updateDeviceExtendedConfigDraftFromForm(deviceId);draft.saving=true;draft.errors=[];draft.conflict=null;appState.deviceExtendedConfigEdit=draft;renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});try{const result=await api(`/api/webadmin/device-extended-config/${encodeURIComponent(deviceId)}`,{method:'PATCH',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify(deviceExtendedConfigPatchBody(draft))});if(result.success){markChannelOptionsDirty({type:'device_config_changed',payload:{targetType:'device_extended_config'}});appState.deviceExtendedConfigEdit=null;stopDeviceExtendedConfigLockHeartbeat();await dismissWebAdminModal();toast(result.changed?(result.message||'设备扩展配置已保存。'):'没有变更。');await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});return;}draft.saving=false;draft.errors=result.validationErrors&&result.validationErrors.length?result.validationErrors:[{message:result.message||'保存失败'}];draft.conflict=result.conflict||null;appState.deviceExtendedConfigEdit=draft;if(['edit_lock_expired','edit_lock_conflict','edit_lock_required'].includes(result.code))stopDeviceExtendedConfigLockHeartbeat();toast(result.message||'保存失败');await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});showDeviceExtendedConfigEditModal(deviceId);}catch(err){draft.saving=false;draft.errors=[{message:err.message||'保存失败'}];appState.deviceExtendedConfigEdit=draft;toast(err.message||'保存失败');await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});showDeviceExtendedConfigEditModal(deviceId);}}
                function signalHash(channel){return `#/signals/${encodeURIComponent(channel||'')}`;}
                function listenerHash(id){return `#/listeners/${encodeURIComponent(id||'')}`;}
                function deviceRouteRef(id,type=''){
                  const clean=String(id||''), normalizedType=String(type||'').toLowerCase();
                  if(['signal_emitter','signal_receiver','action_relay'].includes(normalizedType)&&!clean.startsWith(`${normalizedType}:`))return `${normalizedType}:${clean}`;
                  return clean;
                }
                function deviceHash(idOrDevice,type=''){const ref=idOrDevice&&typeof idOrDevice==='object'?deviceRouteRef(idOrDevice.id,idOrDevice.type):deviceRouteRef(idOrDevice,type);return `#/devices/${encodeURIComponent(ref)}`;}
                function deviceTypeRefPrefix(id){const value=String(id||'');for(const type of ['signal_emitter','signal_receiver','action_relay','virtual_block_device']){if(value.startsWith(`${type}:`))return type;}return '';}
                function stripDeviceTypeRef(id){let value=String(id||'');['signal_emitter','signal_receiver','action_relay','virtual_block_device'].forEach(type=>{const prefix=`${type}:`;if(value.startsWith(prefix))value=value.substring(prefix.length);});return value;}
                function deviceApiRef(id){return stripDeviceTypeRef(id);}
                function sameDeviceRef(a,b){return stripDeviceTypeRef(a)===stripDeviceTypeRef(b);}
                function deviceDetailRouteKeys(id,type=''){const clean=String(id||''), keys=new Set();if(!clean)return keys;keys.add(`deviceDetail:${clean}`);const ref=deviceRouteRef(clean,type);keys.add(`deviceDetail:${ref}`);return keys;}
                function addDeviceDetailRouteKeys(add,id,type=''){deviceDetailRouteKeys(id,type).forEach(key=>add(key));}
                function eventAffectedChannels(event){const raw=event?.payload?.affectedChannels;if(Array.isArray(raw))return raw.map(v=>String(v||'')).filter(v=>!isBlank(v));return [];}
                function lockHeldByOther(lock){return !!(lock&&lock.locked&&!lock.heldByCurrentUser);}
                function lockMessage(lock,label='配置'){return `${label}正在由 ${lock?.holderUsername||'其他用户'} 编辑，锁到期：${formatDateTime(lock?.expiresAt)}`;}
                const DEVICE_EDIT_LOCK_TYPES=['device_metadata','device_basic_config','device_extended_config','action_relay_actions','virtual_block_device_triggers','interaction_item_matcher'];
                function isDeviceConfigLockType(targetType){return DEVICE_EDIT_LOCK_TYPES.includes(String(targetType||''));}
                function deviceConfigLockLabel(targetType){const type=String(targetType||'');if(type==='device_metadata')return '显示信息';if(type==='device_basic_config')return '基础配置';if(type==='device_extended_config')return '类型专属配置';if(type==='action_relay_actions')return 'Action 列表';if(type==='virtual_block_device_triggers')return '原生触发配置';if(type==='interaction_item_matcher')return '交互物品匹配';return '配置';}
                function editLockCacheKey(targetType,targetId){return `${String(targetType||'')}\n${stripDeviceTypeRef(targetId)}`;}
                function rememberDeviceEditLockEvent(event){
                  if(String(event?.type||'')!=='edit_lock_changed')return;
                  const payload=event.payload||{}, targetType=String(payload.targetType||''), targetId=String(payload.targetId||event.deviceId||'');
                  if(!isDeviceConfigLockType(targetType)||isBlank(targetId))return;
                  const key=editLockCacheKey(targetType,targetId);
                  if(payload.locked===false){delete appState.deviceEditLocks[key];return;}
                  appState.deviceEditLocks[key]={locked:true,heldByCurrentUser:false,targetType,targetId:stripDeviceTypeRef(targetId),holderUsername:payload.holderUsername||payload.actor||'',holderRole:payload.holderRole||'',expiresAt:payload.expiresAt||''};
                }
                function cachedDeviceConfigLocks(deviceId){
                  const raw=stripDeviceTypeRef(deviceId);
                  if(isBlank(raw))return [];
                  return DEVICE_EDIT_LOCK_TYPES.map(targetType=>({label:deviceConfigLockLabel(targetType),lock:appState.deviceEditLocks[editLockCacheKey(targetType,raw)]})).filter(item=>item.lock);
                }
                function actionRelayLockForDevice(detail){return detail?.actionRelayActions?.lockStatus||appState.deviceEditLocks[editLockCacheKey('action_relay_actions',detail?.id||'')]||null;}
                function actionRelayLockHeldByOther(lock){return lockHeldByOther(lock);}
                function actionRelayLockMessage(lock){return lockMessage(lock,'Action 列表');}
                function deviceConfigLocks(detail){
                  const locks=[];
                  if(detail?.metadataLock)locks.push({label:'显示信息',lock:detail.metadataLock});
                  if(detail?.basicConfig?.lockStatus)locks.push({label:'基础配置',lock:detail.basicConfig.lockStatus});
                  if(detail?.extendedConfig?.lockStatus)locks.push({label:'类型专属配置',lock:detail.extendedConfig.lockStatus});
                  if(isActionRelay(detail)){const actionLock=actionRelayLockForDevice(detail);if(actionLock)locks.push({label:'Action 列表',lock:actionLock});}
                  if(isVirtualBlockDevice(detail)&&detail?.nativeTriggers?.lockStatus)locks.push({label:'原生触发配置',lock:detail.nativeTriggers.lockStatus});
                  if(isVirtualBlockDevice(detail)&&detail?.interactionItemMatcher?.lockStatus)locks.push({label:'交互物品匹配',lock:detail.interactionItemMatcher.lockStatus});
                  const seen=new Set(locks.map(item=>String(item.lock?.targetType||'')));
                  cachedDeviceConfigLocks(detail?.id||'').forEach(item=>{if(!seen.has(String(item.lock?.targetType||''))){locks.push(item);seen.add(String(item.lock?.targetType||''));}});
                  return locks;
                }
                function deviceConfigLockBlocker(detail){return deviceConfigLocks(detail).find(item=>lockHeldByOther(item.lock))||null;}
                function deviceConfigLockMessage(detail){const blocker=deviceConfigLockBlocker(detail);return blocker?lockMessage(blocker.lock,blocker.label):'';}
                function canEditDeviceConfig(detail){return canEditDeviceMetadata()||canEditDeviceBasicConfig()||canEditDeviceExtendedConfig()||(isActionRelay(detail)&&canEditActionRelayActions())||(isVirtualBlockDevice(detail)&&(canEditVbdNativeTriggers()||canEditInteractionItemMatcher()));}
                function deviceConfigEditButton(detail,label='编辑设备配置',kind='primary'){
                  const canEdit=canEditDeviceConfig(detail);
                  if(!canEdit)return waButton(label,'settings','disabled title="需要 EDITOR 或 OWNER 权限才能编辑设备配置。"','ghost');
                  const message=deviceConfigLockMessage(detail);
                  if(message)return `${waButton(label,'settings',`disabled title="${esc(message)}" data-device-config-lock-disabled="true"`,'ghost is-locked')}<span class="wa-lock-badge" data-device-config-lock-badge="true">${esc(message)}</span>`;
                  return waButton(label,'settings',htmlHandler(`startDeviceConfigEdit(${jsString(detail.id)})`),kind);
                }
                function historyHash(channel){return isBlank(channel)?'#/history':`#/history?channel=${encodeURIComponent(channel)}`;}
                function currentRouteHash(){return location.hash||'#/dashboard';}
                function isDetailHash(hash){const h=String(hash||'');return h.startsWith('#/devices/')||h.startsWith('#/signals/')||h.startsWith('#/listeners/')||h.startsWith('#/signal-listeners/')||h.startsWith('#/regions/')||h.startsWith('#/actions/');}
                function isValidReturnHash(hash){const h=String(hash||'');if(!h.startsWith('#/'))return false;if(h.startsWith('#/login'))return false;if(h.includes('://'))return false;return ['#/dashboard','#/devices','#/virtual-block-devices','#/block-devices','#/receivers','#/listeners','#/signal-listeners','#/signals','#/signalbridge','#/doctor','#/diagnostics','#/signal-doctor','#/history','#/events','#/users','#/permissions','#/users-permissions','#/settings','#/system-settings','#/config','#/config-management','#/settings/config','#/regions','#/region-list','#/region-controllers','#/regionctl','#/actions','#/action-templates','#/templates'].some(prefix=>h===prefix||h.startsWith(prefix+'/')||h.startsWith(prefix+'?'));}
                function withReturnContext(targetHash){const target=String(targetHash||'#/dashboard');if(!isDetailHash(target))return target;const source=currentRouteHash();if(!isValidReturnHash(source))return target;return `${target}${target.includes('?')?'&':'?'}returnTo=${encodeURIComponent(source)}`;}
                function navigateTo(targetHash){location.hash=withReturnContext(targetHash);}
                function safeDecodeRoutePart(value){try{return decodeURIComponent(String(value||''));}catch(_){return String(value||'');}}
                function detailRoute(raw,fallback){const text=String(raw||''), index=text.indexOf('?'), encodedId=index>=0?text.substring(0,index):text, query=index>=0?text.substring(index+1):'', id=safeDecodeRoutePart(encodedId), params=new URLSearchParams(query);const returnTo=params.get('returnTo')||'';return {id, fallback, returnTo:isValidReturnHash(returnTo)?returnTo:''};}
                function goBackOrFallback(returnTo,fallback){location.hash=isValidReturnHash(returnTo)?returnTo:fallback;}
                function jsString(value){return JSON.stringify(String(value||''));}
                function htmlEvent(name,script){return `${esc(name)}="${esc(script)}"`;}
                function htmlHandler(script){return htmlEvent('onclick',script);}
                function navigationAttr(target,stop=true){return htmlHandler(`${stop?'event.stopPropagation();':''}navigateTo(${jsString(target)})`);}
                function navDataAttr(target,label='打开详情'){if(isBlank(target))return 'aria-disabled="true"';return `data-nav-route="${esc(target)}" role="link" tabindex="0" aria-label="${esc(label)}"`;}
                function activateNavRoute(el){const target=el&&el.dataset?el.dataset.navRoute:'';if(!target)return;navigateTo(target);}
                function backButton(route,fallbackLabel){const label=route.returnTo?'返回上一页':fallbackLabel;return `<button class="secondary" ${htmlHandler(`goBackOrFallback(${jsString(route.returnTo)},${jsString(route.fallback)})`)}>${esc(label)}</button>`}
                function channelButton(channel){if(isBlank(channel))return '<span class="muted">未设置</span>';return `<button class="link-button" ${navigationAttr(signalHash(channel))}>${esc(channel)}</button>`}
                function navigationButton(target,label){if(isBlank(target))return esc(label||'-');if(String(target).startsWith('device:'))return `<button class="link-button" ${navigationAttr(deviceHash(String(target).substring(7)))}>${esc(label)}</button>`;if(String(target).startsWith('channel:'))return `<button class="link-button" ${navigationAttr(signalHash(String(target).substring(8)))}>${esc(label)}</button>`;if(String(target).startsWith('region:'))return `<button class="link-button" ${navigationAttr('#/regions/'+encodeURIComponent(String(target).substring(7)))}>${esc(label)}</button>`;if(String(target).startsWith('action:'))return `<button class="link-button" ${navigationAttr('#/actions/'+encodeURIComponent(String(target).substring(7)))}>${esc(label)}</button>`;return esc(label||target);}
                function labelInteractionSource(value){return {main_hand:'主手',off_hand:'副手',inventory_contains:'背包/热键栏',armor_head:'头盔槽',armor_chest:'胸甲槽',armor_legs:'护腿槽',armor_feet:'靴子槽',armor_any:'任意盔甲槽'}[String(value||'').toLowerCase()]||value;}
                function labelConsumeSource(value){return {matched_source:'匹配来源',main_hand:'主手',off_hand:'副手',inventory:'背包/热键栏'}[String(value||'').toLowerCase()]||value;}
                function labelConsumeOrder(value){return {hotbar_first:'优先热键栏',main_inventory_first:'优先主背包'}[String(value||'').toLowerCase()]||value;}
                function labelVanillaPolicy(value){return {allow:'允许原版交互',require_item_match:'需要物品匹配才允许原版交互'}[String(value||'').toLowerCase()]||value;}
                function posText(pos){return pos?`${pos.x} ${pos.y} ${pos.z}`:'-';}
                function deviceIcon(type){const v=String(type||'UNKNOWN').toUpperCase();return icon({SIGNAL_EMITTER:'signal',SIGNAL_RECEIVER:'receiver',ACTION_RELAY:'relay',VIRTUAL_BLOCK_DEVICE:'virtual',REGION_CONTROLLER:'region',UNKNOWN:'device'}[v]||'device');}
                function deviceMetadataIcon(detail){const key=String(detail?.metadata?.effectiveIconKey||detail?.metadata?.iconKey||'auto').toLowerCase();if(key&&key!=='auto')return icon({signal_emitter:'signal',signal_receiver:'receiver',action_relay:'relay',virtual_block_device:'virtual',region:'region',action:'action',warning:'warning',key:'settings',chest:'device',door:'device',signal:'signal',custom_1:'device'}[key]||key);return deviceIcon(detail?.type);}
                function parseTime(value){if(isBlank(value))return null;const raw=typeof value==='number'?value:String(value).trim();const d=new Date(raw);return Number.isNaN(d.getTime())?null:d;}
                function pad2(value){return String(value).padStart(2,'0');}
                function formatDateTime(value){const d=parseTime(value);if(!d)return '暂无';return `${d.getFullYear()}-${pad2(d.getMonth()+1)}-${pad2(d.getDate())} ${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`;}
                function formatRelativeTime(value){const d=parseTime(value);if(!d)return '暂无';const seconds=Math.max(0,Math.floor((Date.now()-d.getTime())/1000));if(seconds<60)return `${seconds} 秒前`;const minutes=Math.floor(seconds/60);if(minutes<60)return `${minutes} 分钟前`;const hours=Math.floor(minutes/60);if(hours<24)return `${hours} 小时前`;return `${Math.floor(hours/24)} 天前`;}
                function fmtTime(value){return esc(formatDateTime(value));}
                function appView(){return document.getElementById('app-view');}
                function captureViewState(){
                  const view=appView(), active=document.activeElement;
                  return {
                    scrollTop:view?view.scrollTop:0,
                    scrollLeft:view?view.scrollLeft:0,
                    activeId:active&&active.id?active.id:'',
                    selectionStart:active&&typeof active.selectionStart==='number'?active.selectionStart:null,
                    selectionEnd:active&&typeof active.selectionEnd==='number'?active.selectionEnd:null,
                    details:[...((view&&view.querySelectorAll)?view.querySelectorAll('details'):[])].map((d,i)=>({key:detailPersistKey(d,i),open:d.open}))
                  };
                }
                function detailPersistKey(detail,index){return detail.dataset.persistKey||detail.querySelector('summary')?.textContent?.trim()||`details-${index}`;}
                function restoreViewState(state){
                  if(!state)return;
                  requestAnimationFrame(()=>{
                    const view=appView();if(!view)return;
                    [...view.querySelectorAll('details')].forEach((d,i)=>{const key=detailPersistKey(d,i), saved=(state.details||[]).find(item=>item.key===key);if(saved)d.open=!!saved.open;});
                    view.scrollTop=state.scrollTop||0;
                    view.scrollLeft=state.scrollLeft||0;
                    if(state.activeId){const active=document.getElementById(state.activeId);if(active){active.focus({preventScroll:true});if(typeof active.setSelectionRange==='function'&&state.selectionStart!==null)active.setSelectionRange(state.selectionStart,state.selectionEnd ?? state.selectionStart);}}
                  });
                }
                function setView(html,options={}){
                  if(options.expectedHash&&currentRouteHash()!==options.expectedHash)return false;
                  if(options.expectedSeq&&options.expectedHash&&appState.realtime.refreshSeq[realtimeRouteKey(options.expectedHash)]!==options.expectedSeq)return false;
                  if(options.silent&&options.expectedHash&&appState.realtime.dirtyRoutes[realtimeRouteKey(options.expectedHash)])return false;
                  const text=String(html ?? '');
                  if(options.silent&&text.includes('loading-state'))return false;
                  if(options.silent&&text.includes('error-state')){toast('实时同步刷新失败，已保留当前页面。');return false;}
                  const view=appView();if(!view)return false;
                  const snapshot=options.silent?captureViewState():null;
                  view.innerHTML=text;
                  if(!options.silent)delete appState.realtime.dirtyRoutes[realtimeRouteKey(currentRouteHash())];
                  if(snapshot)restoreViewState(snapshot);
                  return true;
                }
                function loading(text='正在加载...'){return `<div class="loading-state">${esc(text)}</div>`}
                function empty(text){return `<div class="empty-state">${esc(text)}</div>`}
                function errorBlock(text){return `<div class="error-state">${esc(text)}</div>`}
                function toast(text){const box=document.getElementById('toast');if(!box)return;box.textContent=text;box.hidden=false;clearTimeout(box._timer);box._timer=setTimeout(()=>box.hidden=true,2800);}
                function row(k,v){return `<div class="k">${esc(k)}</div><div class="v">${v ?? ''}</div>`}
                async function api(path, options={}){
                  let res;
                  try{
                    res=await fetch(path,{credentials:'same-origin',headers:{'Content-Type':'application/json',...(options.headers||{})},...options});
                  }catch(err){throw new ApiError(0,'NETWORK_ERROR','无法连接 WebAdmin 服务');}
                  const json=await res.json().catch(()=>({ok:false,error:{code:'BAD_JSON',message:'响应解析失败'}}));
                  if(res.status===401 && document.body.dataset.page==='app'){
                    closeRealtime();
                    sessionStorage.setItem('webadmin.message','登录已失效，请重新登录');
                    location.href='/login';
                    throw new ApiError(401,'UNAUTHORIZED','登录已失效，请重新登录');
                  }
                  if(!res.ok||!json.ok){
                    throw new ApiError(res.status,json.error?.code,json.error?.message||'请求失败');
                  }
                  return json.data;
                }
                async function initLogin(){
                  const form=document.getElementById('login-form'); if(!form) return;
                  hydrateIcons();
                  const pending=sessionStorage.getItem('webadmin.message'); if(pending){document.getElementById('message').textContent=pending;sessionStorage.removeItem('webadmin.message');}
                  document.getElementById('toggle-password').onclick=()=>{const p=document.getElementById('password');p.type=p.type==='password'?'text':'password'};
                  form.onsubmit=async e=>{e.preventDefault();const msg=document.getElementById('message');msg.textContent='正在登录...';try{await api('/api/auth/login',{method:'POST',body:JSON.stringify({username:username.value,password:password.value,rememberMe:remember.checked})});location.href='/app#/dashboard'}catch(err){msg.textContent=err.message}};
                }
                """).append("""
                async function initApp(){
                  if(document.body.dataset.page!=='app') return;
                  bindChrome();
                  try{
                    appState.me=await api('/api/auth/me');
                    appState.status=await api('/api/status');
                    appState.capabilities=await api('/api/webadmin/write/capabilities');
                    renderTopbar();
                    startTopbarClock();
                    connectRealtime();
                  }catch(err){return;}
                  if(!location.hash){location.hash='#/dashboard';return;}
                  window.addEventListener('hashchange',()=>route());
                  route();
                }
                function bindChrome(){
                  hydrateIcons();
                  document.querySelectorAll('.nav-item[data-route]').forEach(btn=>btn.onclick=()=>{location.hash=btn.dataset.route});
                  document.querySelectorAll('.nav-item[data-pending]').forEach(btn=>btn.onclick=()=>toast(btn.dataset.pending));
                  document.getElementById('logout').onclick=async()=>{try{closeRealtime();await api('/api/auth/logout',{method:'POST',body:'{}'});}finally{location.href='/login'}};
                }
                function renderTopbar(){
                  const s=appState.status, me=appState.me;
                  const serverState=document.getElementById('server-state');
                  if(serverState)serverState.innerHTML=`${icon('server-online')}<span>服务器在线</span>`;
                  document.getElementById('access-mode').textContent=`访问模式：${labelAccessMode(s?.webAdmin?.accessMode)}`;
                  document.getElementById('current-user').textContent=`${me?.displayName || me?.username || '-'}`;
                  document.getElementById('current-role').textContent=`${labelRole(me?.role)}`;
                  updateRealtimeStatus();
                }
                function startTopbarClock(){if(appState.topbarClockTimer)clearTimeout(appState.topbarClockTimer);const tick=()=>{updateTopbarClock();appState.topbarClockTimer=setTimeout(tick,1000);};tick();}
                function updateTopbarClock(){const el=document.getElementById('topbar-clock');if(!el)return;const d=new Date();el.textContent=`${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`;}
                function route(options={}){
                  let hash=location.hash || '#/dashboard';
                  try{
                    const result=routeUnsafe(hash,options);
                    if(result&&typeof result.catch==='function')return result.catch(err=>handleRouteRenderError(hash,err,options));
                    return result;
                  }catch(err){
                    return handleRouteRenderError(hash,err,options);
                  }
                }
                function routeUnsafe(hash,options={}){
                  closeDeviceMoreMenu(false);
                  maybeReleaseDeviceMetadataEditForRoute(hash);
                  maybeReleaseDeviceBasicConfigEditForRoute(hash);
                  maybeReleaseDeviceExtendedConfigEditForRoute(hash);
                  maybeReleaseVbdNativeTriggerEditForRoute(hash);
                  maybeReleaseInteractionItemMatcherEditForRoute(hash);
                  maybeReleaseDeviceConfigEditForRoute(hash);
                  maybeReleaseChannelMetadataEditForRoute(hash);
                  maybeReleaseSignalListenerBasicConfigEditForRoute(hash);
                  document.querySelectorAll('.nav-item').forEach(btn=>btn.classList.toggle('active', isRouteActive(btn.dataset.route,hash)));
                  enterRealtimeRoute(hash);
                  if(hash==='#/dashboard') return renderDashboard(options);
                  if(hash==='#/devices') return renderDevices(options);
                  if(hash.startsWith('#/devices/')) return renderDeviceDetail(hash.substring('#/devices/'.length),options);
                  if(hash==='#/virtual-block-devices'||hash==='#/block-devices') return renderVirtualBlockDevices(options);
                  if(hash==='#/listeners'||hash==='#/signal-listeners') return renderListeners(options);
                  if(hash.startsWith('#/listeners/')) return renderSignalListenerDetail(hash.substring('#/listeners/'.length),options);
                  if(hash.startsWith('#/signal-listeners/')) return renderSignalListenerDetail(hash.substring('#/signal-listeners/'.length),options);
                  if(hash==='#/receivers') return renderReceivers(options);
                  if(hash==='#/signals'||hash==='#/signalbridge') return renderSignals(options);
                  if(hash.startsWith('#/signals/')) return renderSignalDetail(hash.substring('#/signals/'.length),options);
                  if(hash==='#/doctor'||hash==='#/diagnostics'||hash==='#/signal-doctor') return renderDoctorPage(options);
                  if(hash.startsWith('#/history')) return renderHistoryPage(hash.substring('#/history'.length),options);
                  if(hash.startsWith('#/events')) return renderHistoryPage(hash.substring('#/events'.length),options);
                  if(hash==='#/config'||hash==='#/config-management'||hash==='#/settings/config') return renderConfigPage(options);
                  if(hash==='#/users'||hash==='#/permissions'||hash==='#/users-permissions') return renderUsersPage(options);
                  if(hash==='#/settings'||hash==='#/system-settings') return renderSettingsPage(options);
                  if(hash==='#/regions'||hash==='#/region-list') return renderRegionsPage(options);
                  if(hash==='#/region-controllers'||hash==='#/regionctl') return renderRegionControllersPage(options);
                  if(hash.startsWith('#/regions/')) return renderRegionDetail(hash.substring('#/regions/'.length),options);
                  if(hash==='#/actions') return renderActionsPage(options);
                  if(hash==='#/action-templates'||hash==='#/templates') return renderActionTemplatesPage(options);
                  if(hash.startsWith('#/actions/')) return renderActionDetail(hash.substring('#/actions/'.length),options);
                  renderPlaceholder('页面暂未接入','该页面将在后续版本接入。');
                }
                function handleRouteRenderError(hash,err,options={}){
                  const message=err&&err.message?err.message:'页面渲染失败';
                  const key=`${hash}|${err&&err.name?err.name:'Error'}|${message}`;
                  if(appState.realtime.lastRenderError!==key){
                    appState.realtime.lastRenderError=key;
                    console.error('WebAdmin route render failed',hash,err);
                  }
                  if(options.silent){toast('实时同步刷新失败，已保留当前页面。');return false;}
                  setView(`<section class="wa-page">${waPageHead('页面渲染失败','当前路由渲染时发生错误，请检查 Console 或刷新重试。',waButton('重试','refresh','onclick="route()"','ghost'))}${errorBlock(message)}</section>`);
                  return false;
                }
                function isRouteActive(route,hash){
                  const r=String(route||''), h=String(hash||'#/dashboard');
                  if(!r)return false;
                  if(r==='#/signals')return h==='#/signals'||h==='#/signalbridge'||h.startsWith('#/signals/');
                  if(r==='#/listeners')return h==='#/listeners'||h==='#/signal-listeners';
                  if(r==='#/history')return h.startsWith('#/history')||h.startsWith('#/events');
                  if(r==='#/devices')return h==='#/devices'||h.startsWith('#/devices/');
                  if(r==='#/virtual-block-devices')return h==='#/virtual-block-devices'||h==='#/block-devices';
                  if(r==='#/actions')return h==='#/actions'||h.startsWith('#/actions/')||h==='#/action-templates'||h==='#/templates';
                  if(r==='#/regions')return h==='#/regions'||h==='#/region-list'||h.startsWith('#/regions/');
                  if(r==='#/region-controllers')return h==='#/region-controllers'||h==='#/regionctl';
                  if(r==='#/users')return h==='#/users'||h==='#/permissions'||h==='#/users-permissions';
                  if(r==='#/settings')return h==='#/settings'||h==='#/system-settings';
                  if(r==='#/config')return h==='#/config'||h==='#/config-management'||h==='#/settings/config';
                  if(r==='#/doctor')return h==='#/doctor'||h==='#/diagnostics'||h==='#/signal-doctor';
                  return h===r||h.startsWith(r+'/')||h.startsWith(r+'?');
                }
                async function settle(path){try{return{ok:true,data:await api(path)}}catch(err){return{ok:false,error:err}}}
                const REALTIME_EVENT_TYPES=['realtime_connected','heartbeat','sync_required','device_registered','device_removed','device_changed','device_config_changed','device_metadata_changed','receiver_changed','receiver_pulse_changed','virtual_block_device_changed','selection_started','selection_completed','selection_cancelled','selection_failed','signal_channel_changed','signal_emitted','signal_history_appended','history_appended','signal_listener_changed','signal_listener_enabled_changed','signal_listener_action_changed','action_changed','action_history_appended','action_execution_appended','region_changed','region_controller_changed','region_event_appended','doctor_issues_changed','webadmin_user_changed','webadmin_audit_appended','webadmin_settings_changed','device_updated','doctor_changed','action_executed','receiver_pulse','region_event','config_changed','write_audit_appended','permission_denied','validation_failed','user_changed','system_settings_changed','signal_config_changed','channel_metadata_changed','signal_listener_config_changed','region_config_changed','action_config_changed','edit_lock_changed','webadmin_user_connected','webadmin_user_disconnected'];
                const REALTIME_KNOWN_ROUTE_KEYS=['dashboard','signals','receivers','listeners','actions','actionTemplates','devices','virtualBlockDevices','history','doctor','regions','regionControllers','users','settings','config'];
                function setRealtimeStatus(status,lastEventAt){
                  appState.realtime.status=status;
                  if(lastEventAt)appState.realtime.lastEventAt=lastEventAt;
                  updateRealtimeStatus();
                }
                function updateRealtimeStatus(){
                  const state=document.getElementById('realtime-state'), last=document.getElementById('last-realtime-event');
                  if(!state)return;
                  const label={CONNECTED:'已连接',RECONNECTING:'正在重连',DISCONNECTED:'已断开',UNAVAILABLE:'不可用'}[appState.realtime.status]||'未连接';
                  state.textContent=`实时同步：${label}`;
                  if(last)last.textContent=`最后事件：${formatRelativeTime(appState.realtime.lastEventAt)}`;
                }
                function connectRealtime(force=false){
                  if(document.body.dataset.page!=='app')return;
                  if(appState.realtime.offline){setRealtimeStatus('DISCONNECTED');return;}
                  if(force&&appState.realtime.source){appState.realtime.source.close();appState.realtime.source=null;}
                  if(appState.realtime.source)return;
                  if(typeof EventSource==='undefined'){setRealtimeStatus('UNAVAILABLE');return;}
                  clearTimeout(appState.realtime.reconnectTimer);
                  setRealtimeStatus(appState.realtime.reconnectAttempt>0?'RECONNECTING':'RECONNECTING');
                  const lastSeq=Math.max(0,Number(appState.realtime.lastSeenSeq||0));
                  const url=lastSeq>0?`/api/realtime/events?lastEventId=${encodeURIComponent(String(lastSeq))}`:'/api/realtime/events';
                  const source=new EventSource(url);
                  appState.realtime.source=source;
                  source.onopen=()=>{
                    const shouldSync=appState.realtime.wasDisconnected||appState.realtime.missed;
                    appState.realtime.reconnectAttempt=0;
                    setRealtimeStatus('CONNECTED');
                    if(shouldSync){appState.realtime.wasDisconnected=false;markRealtimeDirty(currentRouteHash(),{type:'reconnect'});flushVisibleRealtimeRefresh('reconnect');}
                  };
                  source.onerror=()=>{if(appState.realtime.source===source){source.close();appState.realtime.source=null;appState.realtime.wasDisconnected=true;scheduleRealtimeReconnect();}};
                  source.addEventListener('message',event=>handleRealtimeEvent('message',event));
                  REALTIME_EVENT_TYPES.forEach(type=>{
                    source.addEventListener(type,event=>handleRealtimeEvent(type,event));
                  });
                }
                function closeRealtime(status='DISCONNECTED'){
                  clearTimeout(appState.realtime.reconnectTimer);
                  appState.realtime.reconnectTimer=null;
                  if(appState.realtime.source){appState.realtime.source.close();appState.realtime.source=null;}
                  appState.realtime.reconnectAttempt=0;
                  setRealtimeStatus('DISCONNECTED');
                }
                function scheduleRealtimeReconnect(){
                  if(document.body.dataset.page!=='app')return;
                  if(appState.realtime.offline)return;
                  if(appState.realtime.reconnectTimer)return;
                  appState.realtime.reconnectAttempt=Math.min(appState.realtime.reconnectAttempt+1,6);
                  const delay=Math.min(30000,1000*Math.pow(2,appState.realtime.reconnectAttempt-1));
                  setRealtimeStatus('RECONNECTING');
                  appState.realtime.reconnectTimer=setTimeout(()=>{appState.realtime.reconnectTimer=null;connectRealtime();},delay);
                }
                function handleRealtimeEvent(type,event){
                  let data={type};
                  try{data=JSON.parse(event.data||'{}');}catch(_){data={type};}
                  data.type=data.type||type;
                  const gap=recordRealtimeSeq(data,event);
                  setRealtimeStatus('CONNECTED',data.occurredAt);
                  if(data.type==='sync_required'||gap){
                    appState.realtime.missed=true;
                    markAllRealtimeDirty(data);
                    markRealtimeDirty(currentRouteHash(),data);
                    markChannelOptionsDirty(data);
                    flushVisibleRealtimeRefresh(data.type==='sync_required'?'sync_required':'seq_gap');
                    return;
                  }
                  if(data.type==='heartbeat'||data.type==='realtime_connected')return;
                  markChannelOptionsDirty(data);
                  rememberDeviceEditLockEvent(data);
                  markRealtimeRoutesForEvent(data);
                  handleSelectionRealtimeEvent(data);
                  handleActionRelayActionsRealtimeEvent(data);
                  handleInteractionItemMatcherRealtimeEvent(data);
                  const hash=currentRouteHash();
                  if(shouldHandleRealtimeEvent(hash,data)){
                    if(document.hidden||appState.realtime.offline){markRealtimeDirty(hash,data);return;}
                    scheduleRealtimeRefresh(hash,data);
                  }
                }
                function recordRealtimeSeq(data,event){
                  const seq=Number(data?.seq||event?.lastEventId||data?.id||0);
                  if(!Number.isFinite(seq)||seq<=0)return false;
                  const last=Number(appState.realtime.lastSeenSeq||0);
                  const control=['heartbeat','realtime_connected'].includes(String(data?.type||''));
                  const gap=last>0&&seq>last+1&&!control;
                  if(seq>last){
                    appState.realtime.lastSeenSeq=seq;
                    appState.realtime.lastEventId=String(seq);
                  }
                  return gap;
                }
                function shouldHandleRealtimeEvent(hash,event){
                  const key=realtimeRouteKey(hash);
                  if(realtimeRouteKeysForEvent(event).has(key))return true;
                  const type=String(event.type||'');
                  if(String(hash||'').startsWith('#/signals/')){const id=routeDetailId(hash,'#/signals/');return event.channel===id||eventAffectedChannels(event).includes(id)||(type==='edit_lock_changed'&&['channel_metadata','signal_listener_basic_config'].includes(String(event.payload?.targetType||'')));}
                  if(String(hash||'').startsWith('#/listeners/')){const id=routeDetailId(hash,'#/listeners/');return listenerEventRef(event)===id||(event.channel&&!isBlank(id)&&String(event.type||'').startsWith('signal_listener_'));}
                  if(String(hash||'').startsWith('#/signal-listeners/')){const id=routeDetailId(hash,'#/signal-listeners/');return listenerEventRef(event)===id||(event.channel&&!isBlank(id)&&String(event.type||'').startsWith('signal_listener_'));}
                  if(String(hash||'').startsWith('#/devices/'))return !!event.deviceId&&sameDeviceRef(event.deviceId,routeDetailId(hash,'#/devices/'));
                  if(String(hash||'').startsWith('#/regions/'))return !!event.regionId&&event.regionId===routeDetailId(hash,'#/regions/');
                  if(String(hash||'').startsWith('#/actions/'))return !!event.actionId&&event.actionId===routeDetailId(hash,'#/actions/');
                  return false;
                }
                function handleActionRelayActionsRealtimeEvent(event){
                  const draft=appState.actionRelayActionsEdit;
                  if(!draft||draft.saving)return;
                  const target=String(event?.payload?.targetType||'');
                  const type=String(event?.type||''), source=String(event?.sourceType||event?.payload?.deviceType||'').toLowerCase();
                  const eventDevice=String(event?.deviceId||event?.payload?.deviceId||'');
                  if(type==='edit_lock_changed')return;
                  const actionRelayChange=target==='action_relay_actions'||(type==='action_config_changed'&&source==='action_relay');
                  if(!actionRelayChange||!eventDevice||!sameDeviceRef(eventDevice,draft.deviceId))return;
                  syncActionRelayActionsDraftFromForm(draft.deviceId);
                  const current=appState.actionRelayActionsEdit;
                  if(!current||current.deviceId!==draft.deviceId)return;
                  current.conflict=current.conflict||{remote:true,message:'Action 列表已被其他 WebAdmin 客户端修改。'};
                  current.errors=current.errors&&current.errors.length?current.errors:[{message:'Action 列表已被其他 WebAdmin 客户端修改，请重新加载后再保存。'}];
                  appState.actionRelayActionsEdit=current;
                  rerenderActionRelayActionsEditor(current.deviceId);
                }
                function handleInteractionItemMatcherRealtimeEvent(event){
                  const draft=appState.interactionItemMatcherEdit;
                  if(!draft||draft.saving)return;
                  const target=String(event?.payload?.targetType||''), type=String(event?.type||'');
                  const eventDevice=String(event?.deviceId||event?.payload?.deviceId||'');
                  if(type==='edit_lock_changed')return;
                  if(target!=='interaction_item_matcher'||!eventDevice||!sameDeviceRef(eventDevice,draft.deviceId))return;
                  syncInteractionItemMatcherDraftFromForm(draft.deviceId);
                  const current=appState.interactionItemMatcherEdit;
                  if(!current||!sameDeviceRef(current.deviceId,draft.deviceId))return;
                  current.conflict=current.conflict||{remote:true,message:'交互物品匹配已被其他 WebAdmin 客户端修改。'};
                  current.errors=current.errors&&current.errors.length?current.errors:[{message:'交互物品匹配已被其他 WebAdmin 客户端修改，请重新加载后再保存。'}];
                  appState.interactionItemMatcherEdit=current;
                  if(appState.deviceConfigEdit&&sameDeviceRef(appState.deviceConfigEdit.deviceId,current.deviceId))showDeviceConfigEditModal(current.deviceId);else rerenderInteractionItemMatcherEditor(current.deviceId);
                }
                function listenerEventRef(event){return String(event?.listenerId||event?.payload?.listenerId||event?.payload?.listenerRef||event?.payload?.targetId||event?.payload?.id||'');}
                function markRealtimeRoutesForEvent(event){
                  const keys=realtimeRouteKeysForEvent(event);
                  keys.forEach(key=>markRealtimeRouteKeyDirty(key,event));
                }
                function realtimeRouteKeysForEvent(event){
                  const type=String(event?.type||''), keys=new Set();
                  const add=(...items)=>items.filter(Boolean).forEach(item=>keys.add(item));
                  const starts=(...prefixes)=>prefixes.some(prefix=>type.startsWith(prefix));
                  const isAny=(...items)=>items.includes(type);
                  if(isAny('config_changed')){
                    const target=String(event?.payload?.targetType||'');
                    if(target==='action_relay_actions'){add('dashboard','signals','devices','actions','actionTemplates','doctor');if(event?.deviceId)addDeviceDetailRouteKeys(add,event.deviceId,event?.sourceType||event?.payload?.deviceType||'action_relay');if(event?.channel)add(`signalDetail:${event.channel}`);eventAffectedChannels(event).forEach(channel=>add(`signalDetail:${channel}`));}
                    else if(target==='interaction_item_matcher'||target==='virtual_block_device_triggers'){add('dashboard','signals','devices','virtualBlockDevices','doctor');if(event?.deviceId)addDeviceDetailRouteKeys(add,event.deviceId,event?.sourceType||event?.payload?.deviceType||'virtual_block_device');if(event?.channel)add(`signalDetail:${event.channel}`);eventAffectedChannels(event).forEach(channel=>add(`signalDetail:${channel}`));}
                    else if(target.includes('device')){const source=String(event?.sourceType||event?.payload?.deviceType||'').toLowerCase();add('dashboard','devices','doctor');if(target==='device_basic_config'){add('signals');if(event?.channel)add(`signalDetail:${event.channel}`);if(event?.payload?.previousChannel)add(`signalDetail:${event.payload.previousChannel}`);}if(source==='signal_receiver')add('receivers');else if(source==='virtual_block_device')add('virtualBlockDevices');else if(source==='action_relay')add('actions','actionTemplates');else if(!source)add('receivers','virtualBlockDevices','actions','actionTemplates');if(event?.deviceId)addDeviceDetailRouteKeys(add,event.deviceId,source);}
                    else if(target.includes('listener')){add('dashboard','signals','listeners','doctor');if(listenerEventRef(event))add(`listenerDetail:${listenerEventRef(event)}`);}
                    else if(target.includes('channel')){add('dashboard','signals','doctor');if(event?.channel)add(`signalDetail:${event.channel}`);}
                    else add('dashboard','config');
                  }
                  if(starts('device_')||starts('receiver_')||starts('virtual_block_device_')||isAny('device_updated','receiver_pulse','device_config_changed')){
                    const source=String(event?.sourceType||event?.payload?.deviceType||'').toLowerCase();
                    const target=String(event?.payload?.targetType||'');
                    add('dashboard','devices','doctor');
                    if(target==='device_basic_config'){add('signals');if(event?.channel)add(`signalDetail:${event.channel}`);if(event?.payload?.previousChannel)add(`signalDetail:${event.payload.previousChannel}`);}
                    if(source==='signal_receiver'||starts('receiver_')||isAny('receiver_pulse'))add('receivers');
                    if(source==='virtual_block_device'||starts('virtual_block_device_'))add('virtualBlockDevices');
                    if(source==='action_relay')add('actions','actionTemplates');
                    if(isAny('device_config_changed')&&!source)add('config');
                    if(event?.deviceId)addDeviceDetailRouteKeys(add,event.deviceId,source);
                  }
                  if(starts('selection_')){
                    add('dashboard','devices','virtualBlockDevices','history');
                    const deviceId=event?.deviceId||event?.payload?.deviceId;
                    if(deviceId)addDeviceDetailRouteKeys(add,deviceId,event?.sourceType||event?.payload?.deviceType||'virtual_block_device');
                  }
                  if(starts('signal_')||isAny('signal_emitted','history_appended','signal_history_appended','channel_metadata_changed','signal_listener_config_changed','signal_config_changed')){
                    add('dashboard','signals','listeners','history','doctor');
                    if(isAny('signal_listener_changed','signal_listener_enabled_changed','signal_listener_action_changed','signal_listener_config_changed','signal_config_changed'))add('config');
                    if(event?.channel)add(`signalDetail:${event.channel}`);
                    if(listenerEventRef(event))add(`listenerDetail:${listenerEventRef(event)}`);
                  }
                  if(starts('action_')||isAny('action_executed','action_config_changed')){
                    const source=String(event?.sourceType||event?.payload?.deviceType||'').toLowerCase();
                    add('dashboard','actions','actionTemplates','history');
                    if(source==='action_relay'){add('signals');if(event?.channel)add(`signalDetail:${event.channel}`);eventAffectedChannels(event).forEach(channel=>add(`signalDetail:${channel}`));}
                    if(isAny('action_changed','action_config_changed')){add('devices','doctor');if(event?.deviceId)addDeviceDetailRouteKeys(add,event.deviceId,source);}
                    if(event?.actionId)add(`actionDetail:${event.actionId}`);
                  }
                  if(starts('region_')||isAny('region_event','region_config_changed')){
                    add('dashboard','regions','regionControllers','history');
                    if(isAny('region_changed','region_config_changed','region_controller_changed'))add('config');
                    if(event?.regionId)add(`regionDetail:${event.regionId}`);
                  }
                  if(starts('doctor_')||isAny('doctor_changed'))add('dashboard','doctor','settings');
                  if(starts('webadmin_user_')||isAny('webadmin_user_connected','webadmin_user_disconnected','user_changed'))add('dashboard','users');
                  if(starts('webadmin_audit_')||isAny('write_audit_appended')){
                    const target=String(event?.payload?.targetType||'');
                    if(target==='action_relay_actions'){add('dashboard','history','signals','devices','actions','actionTemplates');if(event?.deviceId)addDeviceDetailRouteKeys(add,event.deviceId,event?.sourceType||event?.payload?.deviceType||'action_relay');if(event?.channel)add(`signalDetail:${event.channel}`);eventAffectedChannels(event).forEach(channel=>add(`signalDetail:${channel}`));}
                    else if(target==='interaction_item_matcher'||target==='virtual_block_device_triggers'){add('dashboard','history','devices','virtualBlockDevices','signals');if(event?.deviceId)addDeviceDetailRouteKeys(add,event.deviceId,event?.sourceType||event?.payload?.deviceType||'virtual_block_device');if(event?.channel)add(`signalDetail:${event.channel}`);eventAffectedChannels(event).forEach(channel=>add(`signalDetail:${channel}`));}
                    else if(target.includes('device')){const source=String(event?.sourceType||event?.payload?.deviceType||'').toLowerCase();add('dashboard','history','devices');if(target==='device_basic_config'){add('signals');if(event?.channel)add(`signalDetail:${event.channel}`);if(event?.payload?.previousChannel)add(`signalDetail:${event.payload.previousChannel}`);}if(source==='signal_receiver')add('receivers');else if(source==='virtual_block_device')add('virtualBlockDevices');else if(source==='action_relay')add('actions','actionTemplates');if(event?.deviceId)addDeviceDetailRouteKeys(add,event.deviceId,source);}
                    else add('dashboard','history','settings','config','users');
                  }
                  if(starts('webadmin_settings_')||isAny('system_settings_changed'))add('settings','config','dashboard');
                  if(type==='edit_lock_changed'){
                    const target=String(event?.payload?.targetType||'');
                    if(target.includes('device')||target==='action_relay_actions'||target==='interaction_item_matcher'||target==='virtual_block_device_triggers'){add('devices');if(event?.payload?.targetId)addDeviceDetailRouteKeys(add,event.payload.targetId,target==='action_relay_actions'?'action_relay':(target==='interaction_item_matcher'||target==='virtual_block_device_triggers'?'virtual_block_device':''));}
                    if(target.includes('channel'))add('signals');
                    if(target.includes('listener')){add('listeners','signals');if(listenerEventRef(event))add(`listenerDetail:${listenerEventRef(event)}`);}
                  }
                  return keys;
                }
                function routeDetailId(hash,prefix){const raw=String(hash||'').substring(prefix.length), q=raw.indexOf('?'), value=q>=0?raw.substring(0,q):raw;try{return decodeURIComponent(value)}catch(_){return value;}}
                function scheduleRealtimeRefresh(hash,event){
                  const key=realtimeRouteKey(hash);
                  if(document.hidden||appState.realtime.offline){markRealtimeDirty(hash,event);return;}
                  if(appState.realtime.pendingRefresh[key]){markRealtimeDirty(hash,event);return;}
                  if(appState.realtime.refreshTimers[key])return;
                  appState.realtime.refreshTimers[key]=setTimeout(()=>runRealtimeRefresh(hash,key),250);
                }
                async function runRealtimeRefresh(hash,key){
                  delete appState.realtime.refreshTimers[key];
                  if(currentRouteHash()!==hash)return;
                  if(appState.realtime.pendingRefresh[key]){markRealtimeDirty(hash,{type:'pending'});return;}
                  appState.realtime.pendingRefresh[key]=true;
                  delete appState.realtime.dirtyRoutes[key];
                  const seq=(appState.realtime.refreshSeq[key]||0)+1;
                  appState.realtime.refreshSeq[key]=seq;
                  try{
                    await route({silent:true,expectedHash:hash,expectedSeq:seq});
                    appState.realtime.missed=false;
                    appState.realtime.wasDisconnected=false;
                  }catch(err){
                    toast('实时同步刷新失败，已保留当前页面。');
                  }finally{
                    delete appState.realtime.pendingRefresh[key];
                    if(appState.realtime.dirtyRoutes[key]&&currentRouteHash()===hash&&!document.hidden)scheduleRealtimeRefresh(hash,{type:'dirty'});
                  }
                }
                function markRealtimeDirty(hash,event){
                  appState.realtime.dirtyRoutes[realtimeRouteKey(hash)]={hash,event,at:Date.now()};
                }
                function markRealtimeRouteKeyDirty(key,event){
                  if(!key)return;
                  appState.realtime.dirtyRoutes[key]={hash:null,event,at:Date.now()};
                }
                function markAllRealtimeDirty(event){
                  REALTIME_KNOWN_ROUTE_KEYS.forEach(key=>markRealtimeRouteKeyDirty(key,event));
                }
                function flushVisibleRealtimeRefresh(reason='visibility'){
                  const hash=currentRouteHash(), key=realtimeRouteKey(hash);
                  if(appState.realtime.offline)return;
                  if(appState.realtime.dirtyRoutes[key]||appState.realtime.missed||appState.realtime.wasDisconnected)scheduleRealtimeRefresh(hash,appState.realtime.dirtyRoutes[key]?.event||{type:reason});
                }
                function realtimeRouteKey(hash){
                  const h=String(hash||'#/dashboard');
                  if(h.startsWith('#/devices/'))return `deviceDetail:${routeDetailId(h,'#/devices/')}`;
                  if(h.startsWith('#/signals/'))return `signalDetail:${routeDetailId(h,'#/signals/')}`;
                  if(h.startsWith('#/listeners/'))return `listenerDetail:${routeDetailId(h,'#/listeners/')}`;
                  if(h.startsWith('#/signal-listeners/'))return `listenerDetail:${routeDetailId(h,'#/signal-listeners/')}`;
                  if(h.startsWith('#/regions/'))return `regionDetail:${routeDetailId(h,'#/regions/')}`;
                  if(h.startsWith('#/actions/'))return `actionDetail:${routeDetailId(h,'#/actions/')}`;
                  if(h.startsWith('#/history')||h.startsWith('#/events'))return 'history';
                  if(h==='#/dashboard')return 'dashboard';
                  if(h==='#/devices')return 'devices';
                  if(h==='#/virtual-block-devices'||h==='#/block-devices')return 'virtualBlockDevices';
                  if(h==='#/listeners'||h==='#/signal-listeners')return 'listeners';
                  if(h==='#/receivers')return 'receivers';
                  if(h==='#/signals'||h==='#/signalbridge')return 'signals';
                  if(h==='#/doctor'||h==='#/diagnostics'||h==='#/signal-doctor')return 'doctor';
                  if(h==='#/regions'||h==='#/region-list')return 'regions';
                  if(h==='#/region-controllers'||h==='#/regionctl')return 'regionControllers';
                  if(h==='#/actions')return 'actions';
                  if(h==='#/action-templates'||h==='#/templates')return 'actionTemplates';
                  if(h==='#/users'||h==='#/permissions'||h==='#/users-permissions')return 'users';
                  if(h==='#/settings'||h==='#/system-settings')return 'settings';
                  if(h==='#/config'||h==='#/config-management'||h==='#/settings/config')return 'config';
                  return h;
                }
                function routePollInterval(hash){
                  return 0;
                }
                function enterRealtimeRoute(hash){
                  startRouteSilentPolling(hash);
                }
                function startRouteSilentPolling(hash){
                  clearTimeout(appState.realtime.pollTimer);
                  appState.realtime.pollTimer=null;
                  const delay=routePollInterval(hash);
                  if(!delay||document.body.dataset.page!=='app')return;
                  const key=realtimeRouteKey(hash);
                  appState.realtime.pollHash=hash;
                  appState.realtime.pollTimer=setTimeout(()=>{
                    appState.realtime.pollTimer=null;
                    const current=currentRouteHash();
                    if(realtimeRouteKey(current)!==key)return;
                    if(document.hidden){markRealtimeDirty(current,{type:'poll'});startRouteSilentPolling(current);return;}
                    scheduleRealtimeRefresh(current,{type:'poll'});
                    startRouteSilentPolling(current);
                  },delay);
                }
                document.addEventListener('visibilitychange',()=>{if(!document.hidden&&document.body.dataset.page==='app'){updateRealtimeStatus();connectRealtime();flushVisibleRealtimeRefresh('visibility');}});
                window.addEventListener('online',()=>{appState.realtime.offline=false;appState.realtime.wasDisconnected=true;connectRealtime(true);markRealtimeDirty(currentRouteHash(),{type:'online'});flushVisibleRealtimeRefresh('online');});
                window.addEventListener('offline',()=>{appState.realtime.offline=true;appState.realtime.wasDisconnected=true;if(appState.realtime.source){appState.realtime.source.close();appState.realtime.source=null;}markRealtimeDirty(currentRouteHash(),{type:'offline'});setRealtimeStatus('DISCONNECTED');});
                window.addEventListener('pagehide',()=>closeRealtime('DISCONNECTED'));
                document.addEventListener('click',event=>{
                  const target=event.target;
                  const basic=appState.deviceBasicConfigEdit;
                  if(basic&&!(target&&target.closest&&target.closest('#basic-channel-combo'))){basic.channelComboOpen=false;syncDeviceBasicConfigChannelCombo(basic.deviceId);}
                  const extended=appState.deviceExtendedConfigEdit;
                  if(extended&&!(target&&target.closest&&target.closest('.extended-channel-combo'))){Object.keys(extended.channelComboOpen||{}).forEach(field=>extended.channelComboOpen[field]=false);(extended.supportedFields||[]).filter(isExtendedChannelField).forEach(field=>syncDeviceExtendedConfigChannelCombo(extended.deviceId,field));}
                  const listener=appState.signalListenerBasicConfigEdit;
                  if(listener&&!(target&&target.closest&&target.closest('.listener-channel-combo'))){listener.channelComboOpen=false;syncSignalListenerBasicConfigChannelCombo(listener.listenerRef);}
                  const actionRelay=appState.actionRelayActionsEdit;
                  if(actionRelay&&!(target&&target.closest&&target.closest('.action-relay-channel-combo'))){Object.keys(actionRelay.channelComboOpen||{}).forEach(index=>actionRelay.channelComboOpen[index]=false);(actionRelay.actions||[]).forEach((_,index)=>syncActionRelayChannelCombo(actionRelay.deviceId,index));}
                  if(appState.openDeviceMoreMenuId&&!(target&&target.closest&&target.closest('.wa-device-more-popover,.wa-menu-wrap,[data-device-more-trigger]')))closeDeviceMoreMenu(false);
                  const selection=appState.selectionCreateVirtualBlock;
                  if(selection&&selection.step==='config'){
                    if(!(target&&target.closest&&target.closest('.selection-player-combo'))){selection.playerComboOpen=false;}
                    if(!(target&&target.closest&&target.closest('.selection-channel-combo'))){selection.channelComboOpen=false;}
                    syncSelectionCombos();
                  }
                  const nav=target&&target.closest?target.closest('[data-nav-route]'):null;
                  if(nav){
                    const interactive=target.closest('button,a,input,select,textarea,[data-no-nav]');
                    if(!interactive||interactive===nav){event.preventDefault();event.stopPropagation();activateNavRoute(nav);}
                  }
                });
                document.addEventListener('keydown',event=>{
                  if(event.key==='Escape'&&appState.openDeviceMoreMenuId){event.preventDefault();closeDeviceMoreMenu(false);return;}
                  if(event.key!=='Enter'&&event.key!==' ')return;
                  const nav=event.target&&event.target.closest?event.target.closest('[data-nav-route]'):null;
                  if(!nav||event.target!==nav||nav.tagName==='BUTTON'||nav.tagName==='A')return;
                  event.preventDefault();activateNavRoute(nav);
                });
                async function renderDashboard(options={}){
                  if(!options.silent)setView(loading('正在加载总览...'));
                  const [status,devices,channels,history,doctor,regions,actions]=await Promise.all([
                    settle('/api/status'),settle('/api/devices'),settle('/api/signals/channels'),settle('/api/signals/history?limit=10'),settle('/api/doctor'),settle('/api/regions'),settle('/api/actions')
                  ]);
                  const deviceList=devices.ok?devices.data:[], channelList=channels.ok?channels.data:[], regionList=regions.ok?regions.data:[], actionList=actions.ok?actions.data:[], hist=history.ok?history.data:[], doc=doctor.ok?doctor.data:{summary:{errorCount:0,warningCount:0,infoCount:0},issues:[]};
                  setView(`
                    <div class="page-head"><div><h1>总览</h1><p>查看服务器、设备、信号与诊断状态</p></div><button class="secondary" onclick="renderDashboard()">刷新</button></div>
                    <section class="card-grid">
                      ${metric('服务器状态',status.ok?labelServerStatus(status.data.server?.status):'加载失败','','dashboard')}
                      ${metric('设备总数',deviceList.length,'','device')}
                      ${metric('信号频道数',channelList.length,'','signal')}
                      ${metric('区域 / 动作',`${regionList.length} / ${actionList.length}`,'','region')}
                      ${metric('诊断错误',doc.summary?.errorCount||0,'error','doctor')}
                      ${metric('诊断警告',doc.summary?.warningCount||0,'warning','warning')}
                      ${metric('当前用户',appState.me?.displayName||appState.me?.username||'-','','user')}
                      ${metric('访问模式',labelAccessMode(appState.status?.webAdmin?.accessMode),'','settings')}
                    </section>
                    <section class="content-grid">
                      <article class="panel-card"><h2>最近信号触发</h2>${history.ok?historyList(hist):errorBlock(history.error.message)}<p class="muted"><button class="link-button" onclick="location.hash='#/history'">查看全部历史</button></p></article>
                      <article class="panel-card"><h2>诊断摘要</h2>${doctor.ok?doctorList(doc.issues||[],5):errorBlock(doctor.error.message)}<p class="muted"><button class="link-button" onclick="location.hash='#/doctor'">查看 Doctor 诊断</button></p></article>
                      <article class="panel-card"><h2>设备概览</h2>${devices.ok?deviceOverview(deviceList):errorBlock(devices.error.message)}</article>
                      <article class="panel-card"><h2>WebAdmin 状态</h2><p class="muted">Dashboard、设备管理、Signal 频道、Doctor 诊断、History 历史、用户管理、系统设置、区域管理和动作系统只读页面已接入。编辑、配置写入、WebSocket 和完整写操作将在后续阶段接入。</p><p><button class="link-button" onclick="location.hash='#/signals'">进入 Signal 管理</button> / <button class="link-button" onclick="location.hash='#/regions'">查看区域</button> / <button class="link-button" onclick="location.hash='#/actions'">查看动作</button> / <button class="link-button" onclick="location.hash='#/doctor'">查看 Doctor</button> / <button class="link-button" onclick="location.hash='#/history'">查看 History</button></p></article>
                    </section>`,options);
                }
                """).append("""
                function metric(label,value,kind='',iconName=''){return `<article class="metric-card ${kind}"><div class="metric-head"><div class="label">${esc(label)}</div>${iconName?`<span class="metric-icon">${icon(iconName)}</span>`:''}</div><div class="value">${esc(value)}</div></article>`}
                function historyList(items){if(!items||items.length===0)return empty('暂无 Signal 历史记录。');return `<div class="list-stack">${items.map(h=>`<div class="event-row"><strong>${esc(labelChannel(h.channel))}</strong><span class="meta">${fmtTime(h.time)} · ${esc(labelSourceType(h.sourceType))} / ${esc(h.sourceName||'-')} · ${labelStatus(h.result)}</span><span>${esc(h.description||'')}</span></div>`).join('')}</div>`}
                function doctorList(items,limit){if(!items||items.length===0)return empty('当前没有诊断问题。');return `<div class="list-stack">${items.slice(0,limit).map(i=>`<div class="issue-row"><strong>${pill(i.severity)} ${esc(i.title||'诊断问题')}</strong><span class="meta">${esc(issueContext(i))}</span><span>${esc(i.suggestion||i.message||'')}</span></div>`).join('')}</div>`}
                function issueContext(i){if(!i)return '';if(!isBlank(i.relatedObjectName))return i.relatedObjectType==='DEVICE'?`设备：${i.relatedObjectName}`:i.relatedObjectName;if(!isBlank(i.channel))return `频道：${i.channel}`;if(!isBlank(i.relatedObjectId))return `${labelSourceType(i.relatedObjectType)}：${i.relatedObjectId}`;return '';}
                function issueNavigation(i){if(!i)return '<span class="muted">暂无跳转目标</span>';const target=i.navigationTarget||(!isBlank(i.relatedObjectId)&&String(i.relatedObjectType).toUpperCase()==='DEVICE'?`device:${i.relatedObjectId}`:(!isBlank(i.channel)?`channel:${i.channel}`:''));const buttons=[];if(target)buttons.push(navigationButton(target,'查看对象'));if(!isBlank(i.channel))buttons.push(`<button class="link-button" ${navigationAttr(historyHash(i.channel))}>查看历史</button>`);return buttons.length?buttons.join(' / '):'<span class="muted">暂无跳转目标</span>';}
                function issueTitle(i){return esc(i?.title||'诊断问题');}
                function issueMessage(i){return esc(i?.message||i?.impact||'暂无说明');}
                function issueSuggestion(i){return esc(i?.suggestion||'暂无建议');}
                function historyAction(h){const buttons=[];const type=String(h?.sourceType||'').toUpperCase();if(!isBlank(h?.channel))buttons.push(channelButton(h.channel));if(type==='DEVICE'&&!isBlank(h?.sourceId))buttons.push(navigationButton(`device:${h.sourceId}`,'查看设备'));if(type==='REGION'&&!isBlank(h?.sourceId))buttons.push(regionButton(h.sourceId,'查看区域'));if(type==='ACTION'&&!isBlank(h?.sourceId))buttons.push(actionButton(h.sourceId,'查看动作'));return buttons.length?buttons.join(' / '):'<span class="muted">暂无关联对象</span>';}
                function deviceOverview(items){if(!items||items.length===0)return empty('当前暂无设备数据。');const enabled=items.filter(d=>d.enabled).length;const warn=items.filter(d=>['WARNING','ERROR'].includes(String(d.doctorStatus||'').toUpperCase())).length;return `<div class="summary-grid">${metric('启用设备',enabled)}${metric('禁用设备',items.length-enabled)}${metric('诊断警告/错误',warn)}${metric('虚拟方块设备',items.filter(d=>d.type==='VIRTUAL_BLOCK_DEVICE').length)}</div>`}
                async function renderDevices(options={}){
                  if(!options.silent)setView(loading('正在加载设备列表...'));
                  let devices;try{devices=await api('/api/devices')}catch(err){if(options.silent){toast('设备列表实时刷新失败，已保留当前页面。');return;}setView(errorBlock(err.message));return;}
                  appState.devices=devices||[];
                  renderDeviceList('',options);
                }
                function renderDeviceList(focusId,options={}){
                  const devices=appState.devices||[], worlds=[...new Set(devices.map(d=>d.world).filter(Boolean))].sort();
                  const filtered=filterDevices(devices);
                  if(setView(`
                    <div class="page-head"><div><h1>设备管理</h1><p>查看信号设备、虚拟方块设备、动作继电器等状态</p></div></div>
                    <section class="card-grid">${metric('设备总数',devices.length,'','device')}${metric('启用设备',devices.filter(d=>d.enabled).length,'','ok')}${metric('禁用设备',devices.filter(d=>!d.enabled).length,'warning','warning')}${metric('诊断警告/错误',devices.filter(d=>['WARNING','ERROR'].includes(String(d.doctorStatus||'').toUpperCase())).length,'','doctor')}</section>
                    <div class="toolbar">
                      <input class="input" id="device-search" placeholder="搜索设备名称 / id / channel / 坐标" value="${esc(appState.deviceFilters.search)}">
                      ${filterSelect('设备类型','device-type',['ALL','SIGNAL_EMITTER','SIGNAL_RECEIVER','ACTION_RELAY','VIRTUAL_BLOCK_DEVICE','UNKNOWN'],appState.deviceFilters.type)}
                      ${filterSelect('启用状态','device-enabled',['ALL','ENABLED','DISABLED'],appState.deviceFilters.enabled)}
                      ${filterSelect('诊断状态','device-doctor',['ALL','OK','INFO','WARNING','ERROR','UNKNOWN'],appState.deviceFilters.doctor)}
                      ${filterSelect('世界/维度','device-world',['ALL',...worlds],appState.deviceFilters.world)}
                    </div>
                    ${filtered.length===0?(devices.length===0?empty('当前暂无设备数据。请在游戏内创建或绑定设备后刷新页面。'):empty('没有匹配当前筛选条件的设备。')):deviceTable(filtered)}
                  `,options))bindDeviceFilters(focusId);
                }
                function filterSelect(label,id,options,value){return `<label class="filter-field"><span>${esc(label)}</span>${select(id,options,value)}</label>`}
                function select(id,options,value){return `<select class="select" id="${id}">${options.map(o=>`<option value="${esc(o)}" ${o===value?'selected':''}>${esc(optionLabel(o))}</option>`).join('')}</select>`}
                function optionLabel(v){return {ALL:'全部',ENABLED:'已启用',DISABLED:'已禁用',SIGNAL_EMITTER:'信号发射器',SIGNAL_RECEIVER:'信号接收器',ACTION_RELAY:'动作继电器',VIRTUAL_BLOCK_DEVICE:'虚拟方块设备',UNKNOWN:'未知',OK:'正常',INFO:'信息',WARNING:'警告',ERROR:'错误'}[v]||v;}
                function bindDeviceFilters(focusId){
                  const update=(event)=>{appState.deviceFilters.search=document.getElementById('device-search').value;appState.deviceFilters.type=document.getElementById('device-type').value;appState.deviceFilters.enabled=document.getElementById('device-enabled').value;appState.deviceFilters.doctor=document.getElementById('device-doctor').value;appState.deviceFilters.world=document.getElementById('device-world').value;renderDeviceList(event.target.id);};
                  ['device-search','device-type','device-enabled','device-doctor','device-world'].forEach(id=>document.getElementById(id).addEventListener(id==='device-search'?'input':'change',update));
                  if(focusId){const el=document.getElementById(focusId);if(el){el.focus();if(el.setSelectionRange&&el.value){el.setSelectionRange(el.value.length,el.value.length);}}}
                }
                function filterDevices(items){const f=appState.deviceFilters;return items.filter(d=>{const hay=[d.id,d.displayName,d.channel,d.world,posText(d.pos),d.type].join(' ').toLowerCase();if(f.search&& !hay.includes(f.search.toLowerCase()))return false;if(f.type!=='ALL'&&d.type!==f.type)return false;if(f.enabled==='ENABLED'&&!d.enabled)return false;if(f.enabled==='DISABLED'&&d.enabled)return false;if(f.doctor!=='ALL'&&String(d.doctorStatus||'UNKNOWN').toUpperCase()!==f.doctor)return false;if(f.world!=='ALL'&&d.world!==f.world)return false;return true;});}
                function deviceTable(items){return `<div class="table-wrap"><table class="data-table"><thead><tr><th>设备</th><th>类型</th><th>世界/维度</th><th>坐标</th><th>主频道</th><th>状态</th><th>最近触发</th><th>诊断</th><th>操作</th></tr></thead><tbody>${items.map(d=>{const target=deviceHash(d);return `<tr ${navigationAttr(target,false)}><td><span class="device-name"><span class="device-icon">${deviceMetadataIcon(d)}</span><span><strong>${esc(d.displayName)}</strong>${deviceSubtitle(d)}</span></span></td><td>${esc(labelType(d.type))}</td><td>${esc(d.world||'-')}</td><td>${esc(posText(d.pos))}</td><td>${channelCell(d.channel)}</td><td>${pill(d.enabled?'OK':'WARNING')} ${esc(labelBool(d.enabled))}</td><td>${fmtTime(d.lastTriggeredAt)}</td><td>${pill(d.doctorStatus)}</td><td><button class="text-button" ${navigationAttr(target)}>查看详情</button></td></tr>`;}).join('')}</tbody></table></div>`}
                function deviceSubtitle(d){const id=shortId(d.id);if(!isBlank(id)&&!String(id).toLowerCase().startsWith('minecraf'))return `<span class="device-subtitle">ID：${esc(id)}</span>`;if(!isBlank(d.world))return `<span class="device-subtitle">维度：${esc(d.world)}</span>`;return '';}
                function channelCell(channel){return channelButton(channel);}
                async function renderDeviceDetail(id,options={}){
                  if(!options.silent)setView(loading('正在加载设备详情...'));
                  const routeInfo=detailRoute(id,'#/devices'), lookupId=deviceApiRef(routeInfo.id), encoded=encodeURIComponent(lookupId);
                  let detail;try{detail=await api(`/api/devices/${encoded}`);const expectedType=deviceTypeRefPrefix(routeInfo.id);if(expectedType&&String(detail.type||'').toLowerCase()!==expectedType){const mismatch=new Error('该位置当前设备类型已变化，目标类型不存在。');mismatch.status=404;throw mismatch;}}catch(err){if(options.silent){toast('设备详情实时刷新失败，已保留当前页面。');return;}setView(`<section class="wa-page"><div class="back-row">${backButton(routeInfo,'返回设备管理')}</div>${waPageHead('详情暂不可用','当前只读接口尚未提供该设备详情或设备已被删除。',waButton('返回列表','device',navigationAttr('#/devices'),'ghost'))}${err.status===404?empty('设备不存在或已被删除。'):errorBlock(err.message)}</section>`);return;}
                  if(!routeInfo.returnTo&&isVirtualBlockDevice(detail))routeInfo.fallback='#/virtual-block-devices';
                  const canonicalEncoded=encodeURIComponent(detail.id||lookupId);
                  const [debug,history,doctor,lockStatus,basicConfig,extendedConfig,actionRelayActions,interactionItemMatcher,nativeTriggers]=await Promise.all([settle(`/api/devices/${canonicalEncoded}/debug`),detail.channel?settle(`/api/signals/history?channel=${encodeURIComponent(detail.channel)}&limit=10`):Promise.resolve({ok:true,data:[]}),settle('/api/doctor'),settle(`/api/webadmin/edit-locks/status?targetType=device_metadata&targetId=${canonicalEncoded}`),settle(`/api/webadmin/device-basic-config/${canonicalEncoded}`),settle(`/api/webadmin/device-extended-config/${canonicalEncoded}`),isActionRelay(detail)?settle(`/api/webadmin/action-relay-actions/${canonicalEncoded}`):Promise.resolve({ok:true,data:null}),isVirtualBlockDevice(detail)?settle(`/api/webadmin/interaction-item-matcher/${canonicalEncoded}`):Promise.resolve({ok:true,data:null}),isVirtualBlockDevice(detail)?settle(`/api/webadmin/virtual-block-devices/${canonicalEncoded}/native-triggers`):Promise.resolve({ok:true,data:null})]);
                  detail.metadataLock=lockStatus.ok?lockStatus.data:null;
                  detail.basicConfig=basicConfig.ok?basicConfig.data:null;
                  detail.basicConfigError=basicConfig.ok?null:basicConfig.error;
                  detail.extendedConfig=extendedConfig.ok?extendedConfig.data:null;
                  detail.extendedConfigError=extendedConfig.ok?null:extendedConfig.error;
                  detail.actionRelayActions=actionRelayActions.ok?actionRelayActions.data:null;
                  detail.actionRelayActionsError=actionRelayActions.ok?null:actionRelayActions.error;
                  detail.interactionItemMatcher=interactionItemMatcher.ok?interactionItemMatcher.data:null;
                  detail.interactionItemMatcherError=interactionItemMatcher.ok?null:interactionItemMatcher.error;
                  detail.nativeTriggers=nativeTriggers.ok?nativeTriggers.data:null;
                  detail.nativeTriggersError=nativeTriggers.ok?null:nativeTriggers.error;
                  appState.currentDeviceDetail=detail;
                  const relatedDoctor=[...(detail.doctorIssues||[])];
                  if(doctor.ok){relatedDoctor.push(...(doctor.data.issues||[]).filter(i=>i.relatedObjectId===detail.id||(!isBlank(detail.channel)&&i.channel===detail.channel)));}
                  const configAction=deviceConfigEditButton(detail,'编辑设备配置','primary');
                  const actionLock=detail.actionRelayActions?.lockStatus||null, actionLockedByOther=actionRelayLockHeldByOther(actionLock);
                  const actionListAction=isActionRelay(detail)?waButton(actionLockedByOther?'只读查看 Action 列表':((detail.actionRelayActions&&detail.actionRelayActions.actionsEditable===false)?'查看 Action 状态':(canEditActionRelayActions()?'编辑 Action 列表':'查看 Action 列表')),'action-relay',actionLockedByOther?htmlHandler(`openActionRelayActionsReadonlyModal(${jsString(detail.id)})`):htmlHandler(`openActionRelayActionsModal(${jsString(detail.id)})`),(canEditActionRelayActions()&&!actionLockedByOther&&!(detail.actionRelayActions&&detail.actionRelayActions.actionsEditable===false))?'primary':'ghost'):'';
                  const deleteAction=isVirtualBlockDevice(detail)?(canDeleteVirtualBlockDevice()?waButton('删除 / 解绑','channel-error',htmlHandler(`openVirtualBlockDeviceDeleteModal(${jsString(detail.id)})`),'danger'):waButton('删除 / 解绑','channel-error','disabled','danger')):'';
                  const quickNote=isVirtualBlockDevice(detail)?'删除 / 解绑只移除 virtual_block_device 配置，不破坏世界方块；导出和其它写操作仍保持禁用。':(isPhysicalSignalDevice(detail)?'这是已放置的真实方块设备。WebAdmin 只编辑安全配置，不创建、不删除、不 setblock；删除请在游戏内破坏方块。':'仅设备显示信息、基础配置和安全扩展配置可写；删除、导出和其它写操作没有完整后端支持，保持禁用。');
                  const statusValue=detail.enabled?'启用':'停用';
                  const doctorStatus=detail.doctorStatus||detail.debugSummary?.status||'UNKNOWN';
                  const recentEvents=history.ok?(history.data||[]):[];
                  const advancedRows=[
                    ['device.id',detail.id],
                    ['device.type',detail.type],
                    ['device.world',detail.world],
                    ['device.pos',posText(detail.pos)],
                    ['device.channel',detail.channel],
                    ['device.enabled',statusValue],
                    ['device.lastTriggeredAt',formatDateTime(detail.lastTriggeredAt)],
                    ['doctor.status',doctorStatus]
                  ];
                  setView(`<section class="wa-page wa-detail-shell" data-detail-kind="device">
                    ${detailHeader({back:backButton(routeInfo,'返回设备管理'),kicker:'设备详情',iconName:deviceTypeIcon(detail.type),title:detail.displayName||detail.id,subtitle:`${detail.world||'暂无世界'} · ${posText(detail.pos)} · ${labelChannel(detail.channel)}`,copyValue:detail.id,badges:[`<span class="pill">${esc(labelType(detail.type))}</span>`,pill(detail.enabled?'OK':'WARNING'),pill(doctorStatus)],actions:[actionListAction,configAction,waButton('导出设备配置','download','disabled','ghost'),waButton('更多','more','disabled','ghost')].filter(Boolean)})}
                    ${detailTabs(['基本信息','配置','最近事件','关联对象','Doctor'])}
                    <section class="wa-detail-first-row">
                      ${detailCard('基本信息',detailInfoGrid([
                        ['设备名称',detail.displayName||detail.id],
                        ['设备 ID',detail.id],
                        ['类型',labelType(detail.type)],
                        ['状态',safeHtml(pill(detail.enabled?'OK':'WARNING')+' '+esc(labelBool(detail.enabled)))],
                        ['世界/维度',detail.world||'暂无'],
                        ['坐标',posText(detail.pos)],
                        ['主频道',safeHtml(channelCell(detail.channel))],
                        ['最近触发',safeHtml(fmtTime(detail.lastTriggeredAt))]
                      ]),configAction)}
                      ${detailCard('状态与统计',`${detailStatGrid([
                        {label:'启用状态',value:statusValue,sub:'基础配置',icon:'enabled',kind:detail.enabled?'ok':'warning'},
                        {label:'最近触发',value:formatDateTime(detail.lastTriggeredAt),sub:'运行状态',icon:'recent-event'},
                        {label:'Doctor',value:labelStatus(doctorStatus),sub:'诊断摘要',icon:'doctor-overview',kind:String(doctorStatus).toUpperCase()==='OK'?'ok':'warning'},
                        {label:'历史事件',value:recentEvents.length,sub:'当前频道缓存',icon:'history'}
                      ])}${detailConsumerGrid([
                        {label:'关联频道',value:labelChannel(detail.channel),icon:'active-channel',target:detail.channel?signalHash(detail.channel):''},
                        {label:'Doctor 诊断',value:uniqueIssues(relatedDoctor).length,icon:'doctor-overview',target:'#/doctor'},
                        {label:'历史记录',value:recentEvents.length,icon:'history',target:detail.channel?historyHash(detail.channel):''},
                        {label:'设备列表',value:'返回',icon:'device',target:'#/devices'}
                      ])}`)}
                    </section>
                    ${detailFixedLayout([
                      detailCard('配置摘要',deviceConfigOverview(detail),'','detail-card-stretchable'),
                      isActionRelay(detail)?detailCard('Action 列表',actionRelayActionListReadonlyCard(detail),actionListAction):'',
                      detailCard('最近事件',`${history.ok?compactEventList(recentEvents,'当前设备暂无关联频道历史。'):errorBlock(history.error.message)}<p class="muted">${isBlank(detail.channel)?'当前设备暂无关联频道历史。':`<button class="link-button" ${navigationAttr(historyHash(detail.channel),false)}>查看相关历史</button>`}</p>`)
                    ],[
                      isVirtualBlockDevice(detail)?detailCard('原生触发配置',`<div data-vbd-native-trigger-side-card="true" data-detail-side-card="vbd-native-triggers">${vbdNativeTriggerOverviewCard(detail)}</div>`,'','detail-card-stretchable'):'',
                      detailCard('关联对象 / Doctor',`${deviceChannelSideCard(detail)}${deviceDoctorSideCard(detail,uniqueIssues(relatedDoctor))}`,'','detail-card-stretchable'),
                      detailCard('快捷操作',`<div class="wa-quick-grid">${actionListAction}${configAction}${waButton('打开频道','active-channel',detail.channel?navigationAttr(signalHash(detail.channel)):'disabled','ghost')}${waButton('查看历史','history',detail.channel?navigationAttr(historyHash(detail.channel)):'disabled','ghost')}${deleteAction}</div><p class="wa-disabled-note">${esc(quickNote)}</p>`)
                    ],[
                      advancedDetailCard('devices',detail.id,advancedRows,[
                      {title:'配置摘要完整字段',rows:advancedRowsFromObject(detail.configSummary||{},'configSummary')},
                      {title:'基础配置',rows:advancedRowsFromObject(detail.basicConfig||{},'basicConfig')},
                      {title:'扩展配置',rows:advancedRowsFromObject(detail.extendedConfig||{},'extendedConfig')},
                      {title:'7.9 原生触发只读摘要',rows:advancedRowsFromObject(detail.nativeTriggers||{},'nativeTriggers')},
                      {title:'运行与调试',rows:advancedRowsFromObject({metadata:detail.metadata,debug:debug.ok?debug.data:null,debugSummary:detail.debugSummary,doctorIssues:uniqueIssues(relatedDoctor),lastResult:detail.lastResult},'runtime')}
                    ])
                    ])}
                  </section>`,options);
                }
                function canEditDeviceMetadata(){const role=String(appState.me?.role||'').toUpperCase();return role==='EDITOR'||role==='OWNER';}
                function canEditDeviceBasicConfig(){const role=String(appState.me?.role||'').toUpperCase();return role==='EDITOR'||role==='OWNER';}
                function canEditDeviceExtendedConfig(){const role=String(appState.me?.role||'').toUpperCase();return role==='EDITOR'||role==='OWNER';}
                function canEditChannelMetadata(){const role=String(appState.me?.role||'').toUpperCase();return role==='EDITOR'||role==='OWNER';}
                function canEditSignalListenerBasicConfig(){const role=String(appState.me?.role||'').toUpperCase();return role==='EDITOR'||role==='OWNER';}
                function canStartObjectSelection(){const role=String(appState.me?.role||'').toUpperCase();return role==='EDITOR'||role==='OWNER';}
                function roleCanWriteLifecycle(){const role=String(appState.me?.role||'').toUpperCase();return role==='EDITOR'||role==='OWNER';}
                function operationAllowed(operation){const found=(appState.capabilities?.operations||[]).find(entry=>String(entry.operation||'')===operation);return found?!!found.allowed:roleCanWriteLifecycle();}
                function canDeleteVirtualBlockDevice(){const flag=appState.capabilities?.virtualBlockDeviceLifecycleEnabled;return flag!==false&&operationAllowed('DELETE_VIRTUAL_BLOCK_DEVICE');}
                function canWriteSignalListenerLifecycle(){const flag=appState.capabilities?.signalListenerLifecycleWriteEnabled;return flag!==false&&(operationAllowed('CREATE_SIGNAL_LISTENER')||operationAllowed('DELETE_SIGNAL_LISTENER'));}
                function canCreateSignalListener(){const flag=appState.capabilities?.signalListenerLifecycleWriteEnabled;return flag!==false&&operationAllowed('CREATE_SIGNAL_LISTENER');}
                function canDeleteSignalListener(){const flag=appState.capabilities?.signalListenerLifecycleWriteEnabled;return flag!==false&&operationAllowed('DELETE_SIGNAL_LISTENER');}
                function canEditActionRelayActions(){const flag=appState.capabilities?.actionRelayActionListWriteEnabled;return flag!==false&&operationAllowed('EDIT_ACTION_RELAY_ACTIONS');}
                function canEditVbdNativeTriggers(){const flag=appState.capabilities?.vbdNativeTriggerWriteEnabled;return flag!==false&&operationAllowed('EDIT_VIRTUAL_BLOCK_DEVICE_TRIGGERS');}
                function canEditInteractionItemMatcher(){const flag=appState.capabilities?.interactionItemMatcherWriteEnabled;return flag!==false&&operationAllowed('EDIT_ITEM_MATCHER');}
                function isActionRelay(d){return String(d?.type||d?.deviceType||'').toUpperCase()==='ACTION_RELAY';}
                function isPhysicalSignalDevice(d){return ['SIGNAL_EMITTER','SIGNAL_RECEIVER','ACTION_RELAY'].includes(String(d?.type||d?.deviceType||'').toUpperCase());}
                function csrfToken(){return appState.capabilities?.csrf?.token || '';}
                function metadataIconOptions(){return ['auto','signal_emitter','signal_receiver','action_relay','virtual_block_device','region','action','warning','key','chest','door','signal','custom_1'];}
                function labelMetadataIcon(value){return {auto:'自动图标',signal_emitter:'信号发射器',signal_receiver:'信号接收器',action_relay:'动作继电器',virtual_block_device:'虚拟方块设备',region:'区域',action:'动作',warning:'警告',key:'钥匙',chest:'箱子',door:'门',signal:'Signal',custom_1:'自定义 1'}[String(value||'auto')]||value;}
                """).append("""
                function selectionModalDefaultDraft(){
                  return {step:'config',status:'draft',purpose:'create_virtual_block_device',targetPlayerName:'',channel:'',displayName:'',note:'',iconKey:'auto',enabled:true,selectionId:'',deviceId:'',routeTarget:'',returnTo:'#/virtual-block-devices',message:'',errors:[],saving:false,playerOptions:[],playerOptionsError:null,playerComboOpen:false,playerComboIndex:0,channelOptions:[],channelOptionsError:null,channelComboOpen:false,channelComboIndex:0,channelComboQuery:'',channelComboSearchActive:false,terminalStatus:'',terminalEventId:''};
                }
                function selectionSourceReturnTo(){
                  const hash=currentRouteHash();
                  if(hash==='#/virtual-block-devices'||hash==='#/block-devices'||hash==='#/devices')return hash;
                  return '#/virtual-block-devices';
                }
                function selectionDeviceDetailRoute(deviceId,returnTo){
                  if(isBlank(deviceId))return '';
                  const safeReturn=isValidReturnHash(returnTo)?returnTo:'#/virtual-block-devices';
                  return `${deviceHash(deviceId)}?returnTo=${encodeURIComponent(safeReturn)}`;
                }
                async function openCreateVirtualBlockDeviceModal(){
                  waEnsureState();
                  if(!canStartObjectSelection()){toast('需要 EDITOR 或 OWNER 权限才能新建虚拟方块设备。');return;}
                  appState.selectionCreateVirtualBlock={...selectionModalDefaultDraft(),returnTo:selectionSourceReturnTo()};
                  markModalInitialSnapshot('selection_create_virtual_block',appState.selectionCreateVirtualBlock);
                  showCreateVirtualBlockDeviceModal();
                  const [players,channels]=await Promise.all([loadOnlinePlayerOptions(),loadSignalChannelOptions()]);
                  const draft=appState.selectionCreateVirtualBlock;
                  if(draft&&draft.step==='config'){
                    appState.selectionCreateVirtualBlock={...draft,playerOptions:players,playerOptionsError:appState.onlinePlayerOptionsError,channelOptions:channels,channelOptionsError:appState.channelOptionsError};
                    showCreateVirtualBlockDeviceModal();
                  }
                }
                function selectionDraftFromForm(){
                  const draft={...(appState.selectionCreateVirtualBlock||selectionModalDefaultDraft())};
                  const targetInput=document.getElementById('selection-target-player'), channelInput=document.getElementById('selection-channel');
                  draft.targetPlayerName=targetInput?targetInput.value.trim():(draft.targetPlayerName||'');
                  draft.channel=channelInput?channelInput.value.trim():(draft.channel||'');
                  draft.displayName=document.getElementById('selection-display-name')?.value?.trim()||draft.displayName||'';
                  draft.note=document.getElementById('selection-note')?.value||draft.note||'';
                  draft.iconKey=document.getElementById('selection-icon-key')?.value||draft.iconKey||'auto';
                  const enabled=document.getElementById('selection-enabled');
                  draft.enabled=enabled?!!enabled.checked:draft.enabled!==false;
                  return draft;
                }
                function selectionPlayerOptionsHtml(draft){
                  if(draft.playerOptionsError||appState.onlinePlayerOptionsError)return '<div class="channel-combo-empty">在线玩家候选加载失败，仍可手动输入玩家名。</div>';
                  const options=filteredOnlinePlayerOptions(draft.playerOptions||appState.onlinePlayerOptions||[],draft.targetPlayerName), current=String(draft.targetPlayerName||'').trim().toLowerCase(), active=Math.max(0,Number(draft.playerComboIndex||0));
                  if(options.length===0)return '<div class="channel-combo-empty">没有匹配的在线玩家，可继续手动输入。</div>';
                  return options.map((p,index)=>`<button type="button" class="channel-combo-option ${index===active?'active':''} ${String(p.name||'').trim().toLowerCase()===current?'selected':''}" role="option" onmousedown="event.preventDefault()" ${htmlHandler(`selectSelectionTargetPlayer(${jsString(p.name||'')})`)}><strong>${esc(p.name||'未命名玩家')}</strong><span>UUID：${esc(p.uuid||'-')}</span></button>`).join('');
                }
                function renderSelectionPlayerCombo(draft){
                  const open=draft.playerComboOpen?' open':'';
                  return `<div id="selection-target-player-combo" class="channel-combo selection-player-combo${open}" data-selection-player-combo="true"><div class="channel-combo-control"><input id="selection-target-player" class="input" maxlength="64" value="${esc(draft.targetPlayerName||'')}" placeholder="选择在线玩家或输入玩家名" autocomplete="off" role="combobox" aria-expanded="${draft.playerComboOpen?'true':'false'}" aria-controls="selection-target-player-menu" onfocus='openSelectionTargetPlayerMenu()' oninput='updateSelectionCombosFromForm("player")' onkeydown='handleSelectionTargetPlayerKey(event)'><button class="channel-combo-toggle" type="button" onclick='toggleSelectionTargetPlayerMenu()' aria-label="显示在线玩家">⌄</button></div><div id="selection-target-player-menu" class="channel-combo-menu" role="listbox">${selectionPlayerOptionsHtml(draft)}</div></div>`;
                }
                function selectionChannelOptionsHtml(draft){
                  if(draft.channelOptionsError||appState.channelOptionsError)return '<div class="channel-combo-empty">频道候选加载失败，仍可手动输入新的频道名。</div>';
                  const options=filteredChannelOptions(draft.channelOptions||appState.channelOptions||[],channelComboQuery(draft)), current=normalizeChannelName(draft.channel).toLowerCase(), active=Math.max(0,Number(draft.channelComboIndex||0));
                  if(options.length===0)return '<div class="channel-combo-empty">没有匹配的已有频道，可直接使用新频道。</div>';
                  return options.map((c,index)=>`<button type="button" class="channel-combo-option ${index===active?'active':''} ${String(c.channel||'').trim().toLowerCase()===current?'selected':''}" role="option" onmousedown="event.preventDefault()" ${htmlHandler(`selectSelectionChannel(${jsString(c.channel||'')})`)}><strong>${esc(c.channel||'未命名频道')}</strong><span>${esc(channelOptionLabel(c))}</span></button>`).join('');
                }
                function renderSelectionChannelCombo(draft){
                  const open=draft.channelComboOpen?' open':'';
                  return `<div id="selection-channel-combo" class="channel-combo selection-channel-combo${open}" data-selection-channel-combo="true"><div class="channel-combo-control"><input id="selection-channel" class="input" maxlength="128" value="${esc(draft.channel||'')}" placeholder="选择已有频道或输入新频道" autocomplete="off" role="combobox" aria-expanded="${draft.channelComboOpen?'true':'false'}" aria-controls="selection-channel-menu" onfocus='openSelectionChannelMenu()' oninput='updateSelectionCombosFromForm("channel")' onkeydown='handleSelectionChannelKey(event)'><button class="channel-combo-toggle" type="button" onclick='toggleSelectionChannelMenu()' aria-label="显示已有频道">⌄</button></div><div id="selection-channel-menu" class="channel-combo-menu" role="listbox">${selectionChannelOptionsHtml(draft)}</div></div>`;
                }
                function updateSelectionCombosFromForm(openTarget=''){
                  const draft=appState.selectionCreateVirtualBlock;if(!draft||draft.step!=='config')return;
                  draft.targetPlayerName=document.getElementById('selection-target-player')?.value||'';
                  draft.channel=document.getElementById('selection-channel')?.value||'';
                  if(openTarget==='player'){draft.playerComboOpen=true;draft.playerComboIndex=0;}
                  if(openTarget==='channel'){draft.channelComboOpen=true;draft.channelComboIndex=0;setChannelComboQuery(draft,draft.channel);}
                  const hint=document.getElementById('selection-channel-hint');
                  if(hint)hint.innerHTML=channelHintHtml(draft.channel,draft.channelOptions||appState.channelOptions||[],draft.channelOptionsError||appState.channelOptionsError);
                  syncSelectionCombos();
                }
                function syncSelectionCombos(){
                  const draft=appState.selectionCreateVirtualBlock;if(!draft)return;
                  const playerCombo=document.getElementById('selection-target-player-combo'), playerMenu=document.getElementById('selection-target-player-menu'), playerInput=document.getElementById('selection-target-player');
                  if(playerCombo)playerCombo.classList.toggle('open',!!draft.playerComboOpen);
                  if(playerInput)playerInput.setAttribute('aria-expanded',draft.playerComboOpen?'true':'false');
                  if(playerMenu)playerMenu.innerHTML=selectionPlayerOptionsHtml(draft);
                  const channelCombo=document.getElementById('selection-channel-combo'), channelMenu=document.getElementById('selection-channel-menu'), channelInput=document.getElementById('selection-channel');
                  if(channelCombo)channelCombo.classList.toggle('open',!!draft.channelComboOpen);
                  if(channelInput)channelInput.setAttribute('aria-expanded',draft.channelComboOpen?'true':'false');
                  if(channelMenu)channelMenu.innerHTML=selectionChannelOptionsHtml(draft);
                }
                function openSelectionTargetPlayerMenu(){const draft=appState.selectionCreateVirtualBlock;if(!draft)return;updateSelectionCombosFromForm('');draft.playerComboOpen=true;syncSelectionCombos();}
                function toggleSelectionTargetPlayerMenu(){const draft=appState.selectionCreateVirtualBlock;if(!draft)return;updateSelectionCombosFromForm('');draft.playerComboOpen=!draft.playerComboOpen;syncSelectionCombos();document.getElementById('selection-target-player')?.focus();}
                function selectSelectionTargetPlayer(playerName){const draft=appState.selectionCreateVirtualBlock;if(!draft)return;draft.targetPlayerName=playerName||'';draft.playerComboOpen=false;draft.playerComboIndex=0;const input=document.getElementById('selection-target-player');if(input)input.value=draft.targetPlayerName;syncSelectionCombos();}
                function handleSelectionTargetPlayerKey(event){const draft=appState.selectionCreateVirtualBlock;if(!draft)return;const options=filteredOnlinePlayerOptions(draft.playerOptions||appState.onlinePlayerOptions||[],document.getElementById('selection-target-player')?.value||draft.targetPlayerName);if(event.key==='Escape'){event.preventDefault();event.stopPropagation();draft.playerComboOpen=false;syncSelectionCombos();return;}if(event.key==='ArrowDown'||event.key==='ArrowUp'){event.preventDefault();draft.playerComboOpen=true;const max=Math.max(0,options.length-1), next=event.key==='ArrowDown'?Number(draft.playerComboIndex||0)+1:Number(draft.playerComboIndex||0)-1;draft.playerComboIndex=Math.min(max,Math.max(0,next));syncSelectionCombos();return;}if(event.key==='Enter'&&draft.playerComboOpen&&options.length>0){event.preventDefault();selectSelectionTargetPlayer(options[Math.min(options.length-1,Number(draft.playerComboIndex||0))].name);return;}}
                function openSelectionChannelMenu(){const draft=appState.selectionCreateVirtualBlock;if(!draft)return;updateSelectionCombosFromForm('');draft.channelComboOpen=true;resetChannelComboQuery(draft);syncSelectionCombos();}
                function toggleSelectionChannelMenu(){const draft=appState.selectionCreateVirtualBlock;if(!draft)return;updateSelectionCombosFromForm('');draft.channelComboOpen=!draft.channelComboOpen;if(draft.channelComboOpen)resetChannelComboQuery(draft);syncSelectionCombos();document.getElementById('selection-channel')?.focus();}
                function selectSelectionChannel(channel){const draft=appState.selectionCreateVirtualBlock;if(!draft)return;draft.channel=channel||'';draft.channelComboOpen=false;draft.channelComboIndex=0;resetChannelComboQuery(draft);const input=document.getElementById('selection-channel'), hint=document.getElementById('selection-channel-hint');if(input)input.value=draft.channel;if(hint)hint.innerHTML=channelHintHtml(draft.channel,draft.channelOptions||appState.channelOptions||[],draft.channelOptionsError||appState.channelOptionsError);syncSelectionCombos();}
                function handleSelectionChannelKey(event){const draft=appState.selectionCreateVirtualBlock;if(!draft)return;const options=filteredChannelOptions(draft.channelOptions||appState.channelOptions||[],channelComboQuery(draft));if(event.key==='Escape'){event.preventDefault();event.stopPropagation();draft.channelComboOpen=false;syncSelectionCombos();return;}if(event.key==='ArrowDown'||event.key==='ArrowUp'){event.preventDefault();draft.channelComboOpen=true;const max=Math.max(0,options.length-1), next=event.key==='ArrowDown'?Number(draft.channelComboIndex||0)+1:Number(draft.channelComboIndex||0)-1;draft.channelComboIndex=Math.min(max,Math.max(0,next));syncSelectionCombos();return;}if(event.key==='Enter'&&draft.channelComboOpen&&options.length>0){event.preventDefault();selectSelectionChannel(options[Math.min(options.length-1,Number(draft.channelComboIndex||0))].channel);return;}}
                function selectionErrorsHtml(draft){
                  const errors=(draft?.errors||[]).filter(Boolean);
                  if(!errors.length)return '';
                  return `<ul class="validation-list">${errors.map(err=>`<li>${esc(err.field?`${err.field}：`: '')}${esc(err.message||err||'选择请求失败')}</li>`).join('')}</ul>`;
                }
                function selectionStatusTone(status){
                  const value=String(status||'').toLowerCase();
                  if(value==='completed')return 'ok';
                  if(value==='failed')return 'error';
                  if(value==='cancelled')return 'warning';
                  return 'info';
                }
                function selectionModalBody(draft){
                  const d=draft||selectionModalDefaultDraft(), errors=selectionErrorsHtml(d);
                  if(d.step==='waiting'){
                    return `<div class="wa-selection-wizard" data-selection-wizard="virtual_block_device"><div class="wa-selection-status info"><strong>等待玩家在游戏内右键方块</strong><span>目标玩家：${esc(d.targetPlayerName||'-')} · Session：${esc(d.selectionId||'-')}</span><span>客户端会显示“选择虚拟方块设备目标 / 右键方块确认 / ESC 取消”。选择模式不会影响移动和视角。</span></div>${errors}</div>`;
                  }
                  if(d.step==='completed'){
                    return `<div class="wa-selection-wizard" data-selection-wizard="virtual_block_device"><div class="wa-selection-status ok"><strong>选择完成</strong><span>${esc(d.message||'虚拟方块设备已创建。')}</span>${d.deviceId?`<span>设备 ID：${esc(d.deviceId)}</span>`:''}<span>正在进入设备详情...</span></div></div>`;
                  }
                  if(d.step==='cancelled'||d.step==='failed'){
                    return `<div class="wa-selection-wizard" data-selection-wizard="virtual_block_device"><div class="wa-selection-status ${selectionStatusTone(d.step)}"><strong>${d.step==='failed'?'选择失败':'选择已取消'}</strong><span>${esc(d.message||'本次选择未创建设备。')}</span></div>${errors}</div>`;
                  }
                  return `<form class="edit-form wa-selection-form" data-selection-wizard="virtual_block_device" ${htmlEvent('onsubmit','event.preventDefault();startCreateVirtualBlockDeviceSelection()')}>
                    <div class="wa-selection-grid">
                      <label>目标在线玩家${renderSelectionPlayerCombo(d)}<span class="muted">可选择当前在线玩家，也可手动输入玩家名。</span></label>
                      <label>Channel${renderSelectionChannelCombo(d)}<span id="selection-channel-hint" class="muted">${channelHintHtml(d.channel,d.channelOptions||appState.channelOptions||[],d.channelOptionsError||appState.channelOptionsError)}</span></label>
                      <label>显示名<input id="selection-display-name" class="input" maxlength="64" value="${esc(d.displayName||'')}" placeholder="可选，仅用于 WebAdmin 展示"></label>
                      <label>图标<select id="selection-icon-key" class="select">${metadataIconOptions().map(k=>`<option value="${esc(k)}" ${k===(d.iconKey||'auto')?'selected':''}>${esc(labelMetadataIcon(k))}</option>`).join('')}</select></label>
                    </div>
                    <label>备注<textarea id="selection-note" maxlength="512" placeholder="可选，仅用于 WebAdmin 展示">${esc(d.note||'')}</textarea></label>
                    <label class="check-row"><input id="selection-enabled" type="checkbox" ${d.enabled!==false?'checked':''}><span>创建后启用设备</span></label>
                    <p class="readonly-note">开始后目标玩家客户端进入选择模式。右键任意方块都会作为目标选择，并阻断原方块交互、手持物品使用和 GUI 打开。</p>
                    ${errors}
                  </form>`;
                }
                function selectionModalFooter(draft){
                  const d=draft||selectionModalDefaultDraft();
                  if(d.step==='waiting')return `${waButton('取消选择','close',d.saving?'disabled':htmlHandler('cancelCreateVirtualBlockDeviceSelection()'),'danger')}${waButton('等待玩家选择','virtual-block-device','disabled','primary')}`;
                  if(d.step==='completed')return `${waButton('正在进入详情','eye','disabled','primary')}`;
                  if(d.step==='cancelled'||d.step==='failed')return `${waButton('返回配置','settings',htmlHandler('resetCreateVirtualBlockDeviceModalToConfig()'),'ghost')}${waButton('关闭','close',htmlHandler('closeWebAdminModal()'),'primary')}`;
                  return `${waButton('取消','close',htmlHandler('closeWebAdminModal()'),'ghost')}${waButton(d.saving?'开始中...':'开始选择','virtual-block-device',d.saving?'disabled':htmlHandler('startCreateVirtualBlockDeviceSelection()'),'primary')}`;
                }
                function showCreateVirtualBlockDeviceModal(){
                  const draft=appState.selectionCreateVirtualBlock||selectionModalDefaultDraft();
                  if(draft.step==='config')markModalInitialSnapshot('selection_create_virtual_block',draft);
                  openWebAdminModal('新建虚拟方块设备',selectionModalBody(draft),selectionModalFooter(draft),{className:'wa-selection-modal',onClose:()=>closeCreateVirtualBlockDeviceModal(true),syncBeforeClose:()=>syncModalDraftBeforeClose('selection_create_virtual_block'),dirtyCheck:()=>{const d=appState.selectionCreateVirtualBlock;return !!d&&d.step==='config'&&modalDraftDirty('selection_create_virtual_block',d);}});
                }
                function writeSelectionData(result){return result?.data?.selection||result?.data||result?.selection||{};}
                function writeResultErrors(result,fallback){return result?.validationErrors&&result.validationErrors.length?result.validationErrors:[{message:result?.message||fallback||'选择请求失败'}];}
                function selectionTerminalStatus(type){const t=String(type||'');if(t==='selection_completed')return 'completed';if(t==='selection_cancelled')return 'cancelled';if(t==='selection_failed')return 'failed';return '';}
                function shouldIgnoreSelectionTerminal(selectionId,status,eventId=''){
                  if(isBlank(selectionId)||isBlank(status))return false;
                  const existing=appState.selectionTerminalById[selectionId];
                  if(!existing)return false;
                  if(existing.eventId&&eventId&&existing.eventId===eventId)return true;
                  return ['completed','cancelled','failed'].includes(existing.status);
                }
                function markSelectionTerminal(selectionId,status,eventId=''){
                  if(isBlank(selectionId)||isBlank(status))return;
                  if(!appState.selectionTerminalById[selectionId])appState.selectionTerminalById[selectionId]={status,eventId};
                }
                async function startCreateVirtualBlockDeviceSelection(){
                  if(!canStartObjectSelection())return;
                  const draft=selectionDraftFromForm();
                  draft.saving=true;draft.errors=[];draft.message='';
                  appState.selectionCreateVirtualBlock=draft;showCreateVirtualBlockDeviceModal();
                  try{
                    const result=await api('/api/webadmin/selection/start',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({purpose:'create_virtual_block_device',targetPlayerName:draft.targetPlayerName,channel:draft.channel,displayName:draft.displayName,note:draft.note,iconKey:draft.iconKey,enabled:draft.enabled})});
                    const selection=writeSelectionData(result);
                    if(result.success){
                      appState.selectionCreateVirtualBlock={...draft,...selection,step:'waiting',status:'started',saving:false,selectionId:selection.selectionId||result.targetId||draft.selectionId,message:result.message||'已通知目标玩家进入选择模式。',errors:[]};
                      showCreateVirtualBlockDeviceModal();
                      toast('已通知目标玩家进入选择模式。');
                      return;
                    }
                    draft.saving=false;draft.errors=writeResultErrors(result,'无法开始选择。');draft.message=result.message||'无法开始选择。';appState.selectionCreateVirtualBlock=draft;showCreateVirtualBlockDeviceModal();toast(draft.message);
                  }catch(err){draft.saving=false;draft.errors=[{message:err.message||'无法开始选择。'}];draft.message=err.message||'无法开始选择。';appState.selectionCreateVirtualBlock=draft;showCreateVirtualBlockDeviceModal();toast(draft.message);}
                }
                async function cancelCreateVirtualBlockDeviceSelection(){
                  const draft=appState.selectionCreateVirtualBlock||selectionModalDefaultDraft();
                  if(draft.terminalStatus||shouldIgnoreSelectionTerminal(draft.selectionId,draft.status)){return;}
                  if(!draft.selectionId){appState.selectionCreateVirtualBlock={...draft,step:'cancelled',status:'cancelled',message:'已取消选择。'};showCreateVirtualBlockDeviceModal();return;}
                  draft.saving=true;draft.errors=[];appState.selectionCreateVirtualBlock=draft;showCreateVirtualBlockDeviceModal();
                  try{
                    const result=await api('/api/webadmin/selection/cancel',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({selectionId:draft.selectionId,reason:'WebAdmin 已取消选择。'})});
                    const selection=writeSelectionData(result);
                    const returnedStatus=String(selection.status||'').toLowerCase(), finalStatus=result.success&&['completed','cancelled','failed'].includes(returnedStatus)?returnedStatus:(result.success?'cancelled':'failed');
                    markSelectionTerminal(draft.selectionId,finalStatus,'cancel-api');
                    if(finalStatus==='completed'){
                      const deviceId=String(selection.deviceId||draft.deviceId||''), routeTarget=selectionDeviceDetailRoute(deviceId,draft.returnTo||'#/virtual-block-devices');
                      appState.selectionCreateVirtualBlock={...draft,...selection,step:'completed',status:'completed',terminalStatus:'completed',saving:false,deviceId,routeTarget,message:selection.message||result.message||'虚拟方块设备已创建。',errors:[]};
                      showCreateVirtualBlockDeviceModal();
                      if(routeTarget){dismissWebAdminModal();appState.selectionCreateVirtualBlock=null;location.hash=routeTarget;}
                      return;
                    }
                    appState.selectionCreateVirtualBlock={...draft,...selection,step:finalStatus,status:finalStatus,terminalStatus:finalStatus,saving:false,message:result.message||selection.message||'选择已取消。',errors:result.success?[]:writeResultErrors(result,'取消选择失败。')};
                    showCreateVirtualBlockDeviceModal();
                    toast(result.message||'选择已取消。');
                  }catch(err){draft.saving=false;draft.errors=[{message:err.message||'取消选择失败。'}];draft.message=err.message||'取消选择失败。';appState.selectionCreateVirtualBlock=draft;showCreateVirtualBlockDeviceModal();toast(draft.message);}
                }
                async function closeCreateVirtualBlockDeviceModal(cancelActive){
                  const draft=appState.selectionCreateVirtualBlock;
                  if(cancelActive&&draft&&draft.step==='waiting'&&draft.selectionId&&!draft.terminalStatus&&!shouldIgnoreSelectionTerminal(draft.selectionId,draft.status)){
                    try{await api('/api/webadmin/selection/cancel',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({selectionId:draft.selectionId,reason:'WebAdmin modal closed.'})});}catch(_){}
                  }
                  appState.selectionCreateVirtualBlock=null;
                  await dismissWebAdminModal();
                }
                function resetCreateVirtualBlockDeviceModalToConfig(){
                  const current=appState.selectionCreateVirtualBlock||selectionModalDefaultDraft();
                  appState.selectionCreateVirtualBlock={...selectionModalDefaultDraft(),targetPlayerName:current.targetPlayerName||'',channel:current.channel||'',displayName:current.displayName||'',note:current.note||'',iconKey:current.iconKey||'auto',enabled:current.enabled!==false,returnTo:current.returnTo||selectionSourceReturnTo(),playerOptions:current.playerOptions||appState.onlinePlayerOptions||[],playerOptionsError:current.playerOptionsError||appState.onlinePlayerOptionsError,channelOptions:current.channelOptions||appState.channelOptions||[],channelOptionsError:current.channelOptionsError||appState.channelOptionsError};
                  showCreateVirtualBlockDeviceModal();
                }
                function handleSelectionRealtimeEvent(event){
                  const draft=appState.selectionCreateVirtualBlock;
                  if(!draft||!draft.selectionId)return;
                  const payload=event?.payload||{}, selectionId=String(event?.selectionId||payload.selectionId||'');
                  if(selectionId&&selectionId!==draft.selectionId)return;
                  const type=String(event?.type||'');
                  const terminal=selectionTerminalStatus(type), eventId=String(event?.id||event?.seq||'');
                  if(terminal&&shouldIgnoreSelectionTerminal(draft.selectionId,terminal,eventId))return;
                  if(type==='selection_completed'){
                    const deviceId=String(event?.deviceId||payload.deviceId||''), routeTarget=selectionDeviceDetailRoute(deviceId,draft.returnTo||'#/virtual-block-devices');
                    markChannelOptionsDirty(event);
                    markSelectionTerminal(draft.selectionId,'completed',eventId);
                    appState.selectionCreateVirtualBlock={...draft,...payload,step:'completed',status:'completed',terminalStatus:'completed',deviceId,routeTarget,message:event?.summary||payload.message||'虚拟方块设备已创建。',errors:[],saving:false};
                    showCreateVirtualBlockDeviceModal();
                    toast('虚拟方块设备已创建。');
                    if(routeTarget){dismissWebAdminModal();appState.selectionCreateVirtualBlock=null;location.hash=routeTarget;}
                  }else if(type==='selection_cancelled'){
                    markSelectionTerminal(draft.selectionId,'cancelled',eventId);
                    appState.selectionCreateVirtualBlock={...draft,...payload,step:'cancelled',status:'cancelled',terminalStatus:'cancelled',message:event?.summary||payload.message||'选择已取消。',errors:[],saving:false};
                    showCreateVirtualBlockDeviceModal();
                  }else if(type==='selection_failed'){
                    markSelectionTerminal(draft.selectionId,'failed',eventId);
                    appState.selectionCreateVirtualBlock={...draft,...payload,step:'failed',status:'failed',terminalStatus:'failed',message:event?.summary||payload.message||'选择失败。',errors:[{message:event?.summary||payload.message||'选择失败。'}],saving:false};
                    showCreateVirtualBlockDeviceModal();
                  }
                }
                """).append("""
                function lifecycleErrorsHtml(draft,fallback='操作失败'){
                  const errors=(draft?.errors||[]).filter(Boolean);
                  if(!errors.length)return '';
                  return `<ul class="validation-list">${errors.map(err=>`<li>${esc(err.field?`${err.field}：`: '')}${esc(err.message||err||fallback)}</li>`).join('')}</ul>`;
                }
                function dangerousModalFooter(saving=false,label='确认删除'){
                  return `<button class="wa-btn ghost" type="button" onclick="closeWebAdminModal()">${icon('close')}<span>取消</span></button><button class="wa-btn danger" type="button" ${saving?'disabled':''} onclick="document.querySelector('#wa-modal-root form')?.requestSubmit()">${icon('critical-issue')}<span>${saving?'处理中...':esc(label)}</span></button>`;
                }
                function lifecycleRouteWithReturn(targetHash,returnTo){
                  const safeReturn=isValidReturnHash(returnTo)?returnTo:'#/listeners';
                  return `${targetHash}${targetHash.includes('?')?'&':'?'}returnTo=${encodeURIComponent(safeReturn)}`;
                }
                function listenerCreateDefaultDraft(){
                  const draft={name:'',channel:'',enabled:true,cooldownTicks:0,channelOptions:appState.channelOptions||[],channelOptionsError:appState.channelOptionsError||null,channelComboOpen:false,channelComboIndex:0,channelComboQuery:'',channelComboSearchActive:false,saving:false,errors:[]};
                  markModalInitialSnapshot('signal_listener_create',draft);
                  return draft;
                }
                function listenerCreateChannelOptionsHtml(draft){
                  if(draft.channelOptionsError||appState.channelOptionsError)return '<div class="channel-combo-empty">频道候选加载失败，仍可手动输入新的频道名。</div>';
                  const options=filteredChannelOptions(draft.channelOptions||appState.channelOptions||[],channelComboQuery(draft)), current=normalizeChannelName(draft.channel).toLowerCase(), active=Math.max(0,Number(draft.channelComboIndex||0));
                  if(options.length===0)return '<div class="channel-combo-empty">没有匹配的已有频道，可直接使用新频道。</div>';
                  return options.map((c,index)=>`<button type="button" class="channel-combo-option ${index===active?'active':''} ${String(c.channel||'').trim().toLowerCase()===current?'selected':''}" role="option" onmousedown="event.preventDefault()" ${htmlHandler(`selectSignalListenerCreateChannel(${jsString(c.channel||'')})`)}><strong>${esc(c.channel||'未命名频道')}</strong><span>${esc(channelOptionLabel(c))}</span></button>`).join('');
                }
                function renderSignalListenerCreateChannelCombo(draft){
                  const open=draft.channelComboOpen?' open':'';
                  return `<div id="listener-create-channel-combo" class="channel-combo listener-create-channel-combo${open}" data-listener-create-channel-combo="true"><div class="channel-combo-control"><input id="listener-create-channel" class="input" maxlength="128" value="${esc(draft.channel||'')}" placeholder="选择已有频道或输入新频道" autocomplete="off" role="combobox" aria-expanded="${draft.channelComboOpen?'true':'false'}" aria-controls="listener-create-channel-menu" onfocus='openSignalListenerCreateChannelMenu()' oninput='updateSignalListenerCreateDraftFromForm(true)' onkeydown='handleSignalListenerCreateChannelKey(event)'><button class="channel-combo-toggle" type="button" onclick='toggleSignalListenerCreateChannelMenu()' aria-label="显示已有频道">⌄</button></div><div id="listener-create-channel-menu" class="channel-combo-menu" role="listbox">${listenerCreateChannelOptionsHtml(draft)}</div></div>`;
                }
                function updateSignalListenerCreateDraftFromForm(openMenu=false){
                  const draft=appState.signalListenerCreate;if(!draft)return;
                  const nameInput=document.getElementById('listener-create-name'), channelInput=document.getElementById('listener-create-channel');
                  draft.name=nameInput?nameInput.value.trim():(draft.name||'');
                  draft.channel=channelInput?channelInput.value:(draft.channel||'');
                  draft.enabled=!!document.getElementById('listener-create-enabled')?.checked;
                  draft.cooldownTicks=document.getElementById('listener-create-cooldown')?.value||draft.cooldownTicks||0;
                  if(openMenu){draft.channelComboOpen=true;draft.channelComboIndex=0;setChannelComboQuery(draft,draft.channel);}
                  const hint=document.getElementById('listener-create-channel-hint');
                  if(hint)hint.innerHTML=channelHintHtml(draft.channel,draft.channelOptions||appState.channelOptions||[],draft.channelOptionsError||appState.channelOptionsError);
                  syncSignalListenerCreateChannelCombo();
                }
                function syncSignalListenerCreateChannelCombo(){
                  const draft=appState.signalListenerCreate;if(!draft)return;
                  const combo=document.getElementById('listener-create-channel-combo'), menu=document.getElementById('listener-create-channel-menu'), input=document.getElementById('listener-create-channel');
                  if(combo)combo.classList.toggle('open',!!draft.channelComboOpen);
                  if(input)input.setAttribute('aria-expanded',draft.channelComboOpen?'true':'false');
                  if(menu)menu.innerHTML=listenerCreateChannelOptionsHtml(draft);
                }
                function openSignalListenerCreateChannelMenu(){const draft=appState.signalListenerCreate;if(!draft)return;updateSignalListenerCreateDraftFromForm(false);draft.channelComboOpen=true;resetChannelComboQuery(draft);syncSignalListenerCreateChannelCombo();}
                function toggleSignalListenerCreateChannelMenu(){const draft=appState.signalListenerCreate;if(!draft)return;updateSignalListenerCreateDraftFromForm(false);draft.channelComboOpen=!draft.channelComboOpen;if(draft.channelComboOpen)resetChannelComboQuery(draft);syncSignalListenerCreateChannelCombo();document.getElementById('listener-create-channel')?.focus();}
                function selectSignalListenerCreateChannel(channel){const draft=appState.signalListenerCreate;if(!draft)return;draft.channel=channel||'';draft.channelComboOpen=false;draft.channelComboIndex=0;resetChannelComboQuery(draft);const input=document.getElementById('listener-create-channel'), hint=document.getElementById('listener-create-channel-hint');if(input)input.value=draft.channel;if(hint)hint.innerHTML=channelHintHtml(draft.channel,draft.channelOptions||appState.channelOptions||[],draft.channelOptionsError||appState.channelOptionsError);syncSignalListenerCreateChannelCombo();}
                function handleSignalListenerCreateChannelKey(event){
                  const draft=appState.signalListenerCreate;if(!draft)return;
                  const options=filteredChannelOptions(draft.channelOptions||appState.channelOptions||[],channelComboQuery(draft));
                  if(event.key==='Escape'){event.preventDefault();event.stopPropagation();draft.channelComboOpen=false;syncSignalListenerCreateChannelCombo();return;}
                  if(event.key==='ArrowDown'||event.key==='ArrowUp'){event.preventDefault();draft.channelComboOpen=true;const max=Math.max(0,options.length-1), next=event.key==='ArrowDown'?Number(draft.channelComboIndex||0)+1:Number(draft.channelComboIndex||0)-1;draft.channelComboIndex=Math.min(max,Math.max(0,next));syncSignalListenerCreateChannelCombo();return;}
                  if(event.key==='Enter'&&draft.channelComboOpen&&options.length>0){event.preventDefault();selectSignalListenerCreateChannel(options[Math.min(options.length-1,Number(draft.channelComboIndex||0))].channel);return;}
                }
                function signalListenerCreateModalBody(draft){
                  const errors=lifecycleErrorsHtml(draft,'创建监听器失败');
                  return `<form class="edit-form" data-listener-create-modal="true" ${htmlEvent('onsubmit','event.preventDefault();saveSignalListenerCreateModal()')}><label>监听器名称<input id="listener-create-name" class="input" maxlength="64" value="${esc(draft.name||'')}" placeholder="例如：spawn_entry_listener"></label><label>监听频道${renderSignalListenerCreateChannelCombo(draft)}<span id="listener-create-channel-hint" class="muted">${channelHintHtml(draft.channel,draft.channelOptions||appState.channelOptions||[],draft.channelOptionsError||appState.channelOptionsError)}</span></label><label class="switch-row"><span>启用监听器</span><input id="listener-create-enabled" type="checkbox" ${draft.enabled!==false?'checked':''}></label><label>冷却时间（ticks）<input id="listener-create-cooldown" class="input" type="number" min="0" max="72000" step="1" value="${esc(draft.cooldownTicks ?? 0)}"></label><p class="readonly-note">新建 Signal Listener 默认 actions 为空；本阶段不会创建 matcher、itemSubmit、ConditionEngine 或动作列表编辑。</p>${errors}</form>`;
                }
                function showSignalListenerCreateModal(){
                  const draft=appState.signalListenerCreate||listenerCreateDefaultDraft();
                  markModalInitialSnapshot('signal_listener_create',draft);
                  openWebAdminModal('新建 Signal Listener',signalListenerCreateModalBody(draft),editModalFooter(draft.saving),{className:'wa-config-modal',onClose:()=>{appState.signalListenerCreate=null;return dismissWebAdminModal();},syncBeforeClose:()=>syncModalDraftBeforeClose('signal_listener_create'),dirtyCheck:()=>modalDraftDirty('signal_listener_create',appState.signalListenerCreate)});
                }
                async function openSignalListenerCreateModal(){
                  waEnsureState();
                  if(!canCreateSignalListener()){toast('需要 EDITOR 或 OWNER 权限才能新建 Signal Listener。');return;}
                  appState.signalListenerCreate=listenerCreateDefaultDraft();
                  showSignalListenerCreateModal();
                  const channels=await loadSignalChannelOptions();
                  const draft=appState.signalListenerCreate;
                  if(draft){appState.signalListenerCreate={...draft,channelOptions:channels,channelOptionsError:appState.channelOptionsError};showSignalListenerCreateModal();}
                }
                async function saveSignalListenerCreateModal(){
                  const draft=appState.signalListenerCreate||listenerCreateDefaultDraft();
                  updateSignalListenerCreateDraftFromForm(false);
                  draft.saving=true;draft.errors=[];appState.signalListenerCreate=draft;showSignalListenerCreateModal();
                  try{
                    const result=await api('/api/webadmin/signal-listeners',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({name:draft.name,displayName:draft.name,channel:String(draft.channel||'').trim(),enabled:draft.enabled!==false,cooldownTicks:Number(draft.cooldownTicks||0)})});
                    if(result.success){
                      markChannelOptionsDirty({type:'signal_listener_changed'});
                      const routeTarget=result.data?.routeTarget||lifecycleRouteWithReturn(listenerHash(result.data?.listenerId||result.targetId||''),'#/listeners');
                      appState.signalListenerCreate=null;
                      await dismissWebAdminModal();
                      toast(result.message||'Signal Listener 已创建。');
                      if(routeTarget)location.hash=routeTarget;else await renderListeners({silent:true});
                      return;
                    }
                    draft.saving=false;draft.errors=writeResultErrors(result,'创建 Signal Listener 失败。');appState.signalListenerCreate=draft;showSignalListenerCreateModal();toast(result.message||'创建 Signal Listener 失败。');
                  }catch(err){draft.saving=false;draft.errors=[{message:err.message||'创建 Signal Listener 失败。'}];appState.signalListenerCreate=draft;showSignalListenerCreateModal();toast(err.message||'创建 Signal Listener 失败。');}
                }
                """).append("""
                function signalListenerDeleteDraft(listener,channel=''){
                  return {listener:{...(listener||{}),channel:listener?.channel||channel||''},confirmationText:'',confirmed:false,reason:'',saving:false,errors:[]};
                }
                function listenerDeleteModalBody(draft){
                  const l=draft.listener||{}, actionCount=Number(l.actionCount ?? (l.actions||[]).length ?? 0), title=l.name||l.id||'未命名监听器';
                  const errors=lifecycleErrorsHtml(draft,'删除监听器失败');
                  return `<form class="edit-form" data-listener-delete-modal="true" data-danger-confirm-modal="true" ${htmlEvent('onsubmit','event.preventDefault();deleteSignalListenerFromModal()')}><div class="readonly-note danger"><strong>删除 Signal Listener</strong><span>会删除该 listener 内的 ${esc(actionCount)} 个 action 引用；不会删除 channel、receiver、device 或历史记录。</span></div><div class="identity-grid">${row('名称',esc(title))}${row('ID',esc(l.id||'-'))}${row('频道',l.channel?channelButton(l.channel):'<span class="muted">未绑定</span>')}${row('Action 数量',esc(actionCount))}</div><label class="switch-row"><span>我确认删除该 Signal Listener</span><input id="listener-delete-confirmed" type="checkbox" ${draft.confirmed?'checked':''}></label><label>输入 Listener ID 或名称确认<input id="listener-delete-confirmation" class="input" value="${esc(draft.confirmationText||'')}" placeholder="${esc(l.id||title)}"></label><label>原因（可选）<textarea id="listener-delete-reason" maxlength="200" placeholder="仅写入 WebAdmin audit，不影响业务逻辑。">${esc(draft.reason||'')}</textarea></label>${errors}</form>`;
                }
                function showSignalListenerDeleteModal(){
                  const draft=appState.signalListenerDelete;if(!draft)return;
                  openWebAdminModal('删除 Signal Listener',listenerDeleteModalBody(draft),dangerousModalFooter(draft.saving,'确认删除'),{className:'wa-config-modal',onClose:()=>{appState.signalListenerDelete=null;return dismissWebAdminModal();}});
                }
                async function openSignalListenerDeleteModal(listenerId,routeChannel=''){
                  if(!canDeleteSignalListener()){toast('需要 EDITOR 或 OWNER 权限才能删除 Signal Listener。');return;}
                  let listener=(appState.listeners||[]).find(item=>listenerMatches(item,listenerId));
                  if(!listener){
                    const result=await loadSignalListenerDetail(listenerId);
                    if(!result.ok){toast(result.message||'无法读取 Signal Listener。');return;}
                    listener={...(result.data.listener||{}),channel:result.data.channel||routeChannel};
                  }
                  appState.signalListenerDelete=signalListenerDeleteDraft(listener,routeChannel);
                  showSignalListenerDeleteModal();
                }
                async function deleteSignalListenerFromModal(){
                  const draft=appState.signalListenerDelete;if(!draft)return;
                  const l=draft.listener||{}, listenerId=l.id||'';
                  draft.confirmed=!!document.getElementById('listener-delete-confirmed')?.checked;
                  draft.confirmationText=document.getElementById('listener-delete-confirmation')?.value?.trim()||'';
                  draft.reason=document.getElementById('listener-delete-reason')?.value||'';
                  draft.saving=true;draft.errors=[];appState.signalListenerDelete=draft;showSignalListenerDeleteModal();
                  try{
                    const result=await api(`/api/webadmin/signal-listeners/${encodeURIComponent(listenerId)}/delete`,{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({listenerId,confirmationText:draft.confirmationText,confirmed:draft.confirmed,reason:draft.reason})});
                    if(result.success){
                      appState.signalListenerDelete=null;
                      appState.listeners=(appState.listeners||[]).filter(item=>!listenerMatches(item,listenerId));
                      await dismissWebAdminModal();
                      toast(result.message||'Signal Listener 已删除。');
                      const h=currentRouteHash();
                      if((h.startsWith('#/listeners/')&&routeDetailId(h,'#/listeners/')===listenerId)||(h.startsWith('#/signal-listeners/')&&routeDetailId(h,'#/signal-listeners/')===listenerId))location.hash=result.data?.routeTarget||'#/listeners';
                      else await route({silent:true});
                      return;
                    }
                    draft.saving=false;draft.errors=writeResultErrors(result,'删除 Signal Listener 失败。');appState.signalListenerDelete=draft;showSignalListenerDeleteModal();toast(result.message||'删除 Signal Listener 失败。');
                  }catch(err){draft.saving=false;draft.errors=[{message:err.message||'删除 Signal Listener 失败。'}];appState.signalListenerDelete=draft;showSignalListenerDeleteModal();toast(err.message||'删除 Signal Listener 失败。');}
                }
                """).append("""
                function virtualBlockDeleteDraft(detail){
                  return {device:{...(detail||{})},confirmationText:'',confirmed:false,reason:'',saving:false,errors:[]};
                }
                function vbdDeleteModalBody(draft){
                  const d=draft.device||{}, cfg=d.configSummary||{}, block=firstKnown(cfg,['blockId','block','minecraftBlockId','boundBlockId'])||'--', title=d.displayName||d.id||'虚拟方块设备';
                  const errors=lifecycleErrorsHtml(draft,'删除 / 解绑虚拟方块设备失败');
                  return `<form class="edit-form" data-vbd-delete-modal="true" data-danger-confirm-modal="true" ${htmlEvent('onsubmit','event.preventDefault();deleteVirtualBlockDeviceFromModal()')}><div class="readonly-note danger"><strong>删除 / 解绑虚拟方块设备</strong><span>仅删除 SignalDeviceStore / WebAdmin registry 配置，不 setblock、不破坏世界方块，也不会删除其它类型 signal device。</span></div><div class="identity-grid">${row('显示名称',esc(title))}${row('设备 ID',esc(d.id||'-'))}${row('世界 / 坐标',esc(`${d.world||'-'} ${posText(d.pos)}`))}${row('方块 ID',esc(block))}${row('频道',d.channel?channelButton(d.channel):'<span class="muted">未绑定</span>')}</div><label class="switch-row"><span>我确认只解绑该虚拟方块设备</span><input id="vbd-delete-confirmed" type="checkbox" ${draft.confirmed?'checked':''}></label><label>输入设备 ID 或显示名称确认<input id="vbd-delete-confirmation" class="input" value="${esc(draft.confirmationText||'')}" placeholder="${esc(d.id||title)}"></label><label>原因（可选）<textarea id="vbd-delete-reason" maxlength="200" placeholder="仅写入 WebAdmin audit，不影响世界方块。">${esc(draft.reason||'')}</textarea></label>${errors}</form>`;
                }
                function showVirtualBlockDeviceDeleteModal(){
                  const draft=appState.virtualBlockDelete;if(!draft)return;
                  openWebAdminModal('删除 / 解绑虚拟方块设备',vbdDeleteModalBody(draft),dangerousModalFooter(draft.saving,'确认解绑'),{className:'wa-config-modal',onClose:()=>{appState.virtualBlockDelete=null;return dismissWebAdminModal();}});
                }
                async function openVirtualBlockDeviceDeleteModal(deviceId){
                  if(!canDeleteVirtualBlockDevice()){toast('需要 EDITOR 或 OWNER 权限才能删除 / 解绑虚拟方块设备。');return;}
                  let detail=(appState.currentDeviceDetail&&appState.currentDeviceDetail.id===deviceId)?appState.currentDeviceDetail:null;
                  if(!detail){try{detail=await api(`/api/devices/${encodeURIComponent(deviceId)}`);}catch(err){toast(err.message||'无法读取虚拟方块设备。');return;}}
                  if(!isVirtualBlockDevice(detail)){toast('只能删除 / 解绑 virtual_block_device。');return;}
                  appState.virtualBlockDelete=virtualBlockDeleteDraft(detail);
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
                    const result=await api(`/api/webadmin/virtual-block-devices/${encodeURIComponent(deviceId)}/delete`,{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({deviceId,confirmationText:draft.confirmationText,confirmed:draft.confirmed,reason:draft.reason})});
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
                  const loadNotes=[detail.basicConfigError?`基础配置加载失败：${detail.basicConfigError.message||'未知错误'}`:'',detail.extendedConfigError?`扩展配置加载失败：${detail.extendedConfigError.message||'未知错误'}`:'',detail.nativeTriggersError?`原生触发配置加载失败：${detail.nativeTriggersError.message||'未知错误'}`:'',detail.interactionItemMatcherError?`交互物品匹配加载失败：${detail.interactionItemMatcherError.message||'未知错误'}`:''].filter(Boolean);
                  return `<div class="wa-config-summary ${nativeTriggers?'wa-vbd-config-summary':''}" ${nativeTriggers?'data-vbd-config-summary="true"':''}>
                    <section class="wa-config-card"><h3>显示信息</h3><div class="identity-grid">${row('显示名称',esc(meta.displayName||meta.effectiveDisplayName||detail.displayName))}${row('备注',isBlank(meta.note)?'<span class="muted">暂无备注</span>':esc(meta.note))}${row('图标',esc(labelMetadataIcon(meta.iconKey||'auto')))}</div></section>
                    <section class="wa-config-card"><h3>基础配置</h3><div class="identity-grid">${row('启用状态',esc(labelEnabledState(basic.enabled ?? detail.enabled)))}${row('主频道',channelCell(basic.channel||detail.channel))}</div></section>
                    <section class="wa-config-card"><h3>类型专属配置</h3><div class="identity-grid">${extRows}</div>${extNote}</section>
                    ${nativeTriggers}
                  </div>${loadNotes.length?`<div class="readonly-note">${loadNotes.map(esc).join('<br>')}</div>`:''}<p class="muted">基础配置与扩展配置使用同一个固定 Modal 编辑；virtual_block_device 的交互物品匹配从原生触发配置的右键交互条件层进入；action_relay 可在同一 Modal 内打开 Action 列表。不会创建 itemSubmit、consume、ConditionEngine 或逻辑链图。</p><div class="inline-actions">${action}</div>`;
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
                  return `<div class="wa-native-trigger-area" data-vbd-native-trigger-area="true" data-vbd-native-trigger-write-api="true" data-vbd-native-trigger-no-raw-json="true" data-vbd-native-trigger-no-manual-selector="true">
                    <p class="readonly-note">7.9 P2 按当前 VBD 已启用的原生触发数据展示摘要；点击编辑可配置六类原生触发源。</p>
                    <div class="wa-native-trigger-grid" data-vbd-native-trigger-summary-selected="true" data-vbd-native-trigger-summary-data-driven="true" data-vbd-native-trigger-inline-full-detail="false">${summaries||`<div class="readonly-note" data-vbd-native-trigger-empty-state="true" data-vbd-native-trigger-compact-empty-state="true">尚未启用原生触发方式。可点击编辑原生触发配置启用。</div>`}</div>
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
                  return `<section class="wa-edit-section" data-edit-section="vbd-native-triggers" data-vbd-native-trigger-config-modal-section="true"><header><h3>原生触发配置</h3><span class="pill ok">7.9 P2 edit</span></header>${body}<p class="muted">保存只写入 VBD 原生触发字段；不会创建 itemSubmit、consume、ConditionEngine、路径图，也不会清空 7.8 matcher 或容器 itemConditions。</p></section>`;
                }
                function vbdNativeTriggerCompactCard(detail,type,options={}){
                  const trigger=vbdNativeTriggerMap(detail)[type]||{}, clickable=!options.inConfigModal;
                  const title=labelNativeTriggerType(type), primary=vbdNativeTriggerPrimarySummary(type,trigger), recent=vbdNativeTriggerRecentSummary(type,trigger), note=vbdNativeTriggerShortSummary(type,trigger);
                  const clickAttrs=clickable?`type="button" ${htmlHandler(`openVbdNativeTriggerReadonlyModal(${jsString(detail.id)},${jsString(type)})`)} onkeydown="if(event.key==='Enter'||event.key===' '){event.preventDefault();openVbdNativeTriggerReadonlyModal(${esc(jsString(detail.id))},${esc(jsString(type))});}" data-vbd-native-trigger-card-click="readonly-detail" data-vbd-native-trigger-open-readonly-detail="true"`:'type="button" disabled data-vbd-native-trigger-card-click="readonly-detail-disabled"';
                  return `<button class="wa-native-trigger-compact-card" ${clickAttrs} data-vbd-native-trigger-compact-card="true" data-vbd-native-trigger-card-summary="true" data-vbd-native-trigger-card-type="${esc(type)}" data-vbd-native-trigger-summary-selected="true" data-vbd-native-trigger-summary-active="true"><span class="wa-native-trigger-compact-head"><strong>${esc(title)}</strong><span class="pill ok">已启用 / 已配置</span></span><span class="wa-native-trigger-compact-line"><small>主项</small><b>${primary}</b></span><span class="wa-native-trigger-compact-line"><small>最近</small><b>${recent}</b></span><span class="muted">${note}</span></button>`;
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
                  if(type==='right_click'){const matcher=t.interactionItemMatcherLayer||{};return esc(matcher.enabled||matcher.configured?'含交互物品匹配条件':'玩家右键交互触发');}
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
                  return `<div class="identity-grid">${row('已启用',esc(labelBool(!!t.conditionEnabled)))}${row('条件方块',esc(t.conditionBlockId||'未设置'))}${row('条件属性',esc(configured))}${row('条件模式',esc(t.conditionModeDisplayName||t.conditionMode||'condition_enter'))}${row('当前匹配',esc(labelRuntimeBool(t.currentMatched)))}${row('上次满足',esc(labelRuntimeBool(t.lastConditionMatched)))}${row('最近结果',esc(t.lastConditionResult||'暂无'))}${row('运行状态',esc(nativeBlockRuntimeStatus(detail.nativeTriggers?.boundBlock?.status||t.runtimeState)))}${row('支持属性数',esc(t.supportedPropertyCount??props.length))}</div>${vbdBlockStatePropertyList(props,t.validationIssues||[])}`;
                }
                function vbdInteractionTriggerSummary(detail,t){
                  const matcher=t.interactionItemMatcherLayer||{};
                  const configured=!!(matcher.enabled||matcher.configured);
                  const inactiveConfigured=configured&&!t.interactionEnabled;
                  const matcherState=configured?(inactiveConfigured?'已配置，但右键交互触发尚未启用，当前不生效':(matcher.summary||matcher.templateItemId||'已配置')):'未配置';
                  const matcherDetail=configured?`${matcher.templateItemId||matcher.summary||'已配置'}${matcher.countMode?` · ${labelCountMode(matcher.countMode)} ${matcher.requiredCount||1}`:''}${matcher.source||matcher.interactionItemSource?` · ${labelInteractionSource(matcher.source||matcher.interactionItemSource)}`:''}`:'尚未要求特定交互物品';
                  const warning=inactiveConfigured?`<p class="readonly-note warning" data-vbd-native-trigger-matcher-disabled-warning="true">已配置交互物品匹配，但右键交互触发尚未启用；matcher 是右键触发后的条件层，当前不会参与触发判定。</p>`:'';
                  return `<div class="identity-grid" data-vbd-native-trigger-interaction-matcher-summary="true">${row('交互启用',esc(labelBool(!!t.interactionEnabled)))}${row('交互频道',channelCell(t.interactChannel))}${row('冷却时间',esc(formatTicks(t.interactionCooldownTicks)||'0 tick'))}${row('最近玩家',esc(t.lastInteractionPlayerName||'暂无'))}${row('最近手 / 面',esc([t.lastInteractionHand,t.lastInteractionSide].filter(Boolean).join(' / ')||'暂无'))}${row('最近结果',esc(t.lastInteractionResult||'暂无'))}${row('matcher 条件层',esc(matcherState))}${row('matcher 摘要',esc(matcherDetail))}</div>${warning}<div class="inline-actions wa-native-trigger-inline-actions" data-vbd-native-trigger-interaction-matcher-entry="true">${interactionItemMatcherInlineAction(detail)}</div><p class="muted">交互物品匹配是右键触发之后的条件 / 判定层，不是新的原生触发源。</p>`;
                }
                function interactionItemMatcherInlineAction(detail){
                  if(!isVirtualBlockDevice(detail))return '';
                  const data=detail.interactionItemMatcher||{}, locked=lockHeldByOther(data.lockStatus);
                  const canEdit=canEditInteractionItemMatcher()&&data.matcherEditable!==false;
                  if(locked)return waButton('只读查看交互物品匹配','virtual-block-device',htmlHandler(`openInteractionItemMatcherReadonlyModal(${jsString(detail.id)})`),'ghost');
                  return waButton(canEdit?'编辑交互物品匹配':'查看交互物品匹配','virtual-block-device',htmlHandler(`openInteractionItemMatcherModal(${jsString(detail.id)})`),canEdit?'primary':'ghost');
                }
                function vbdContainerOpenSummary(t){
                  return `<div class="identity-grid">${row('容器总开关',esc(labelBool(!!t.containerEnabled)))}${row('打开频道',channelCell(t.containerOpenChannel))}${row('容器冷却',esc(formatTicks(t.containerCooldownTicks)||'0 tick'))}${row('最近玩家',esc(t.lastContainerPlayerName||'暂无'))}${row('最近打开时间',esc(nativeTriggerTime(t.lastContainerOpenWallTimeMillis)))}${row('最近事件类型',esc(t.lastContainerEventType||'暂无'))}${row('最近结果',esc(t.lastContainerResult||'暂无'))}</div><p class="muted">容器打开、关闭和内容变化共用 containerEnabled。</p>`;
                }
                function vbdContainerCloseSummary(t){
                  return `<div class="identity-grid">${row('容器总开关',esc(labelBool(!!t.containerEnabled)))}${row('关闭频道',channelCell(t.containerCloseChannel))}${row('容器冷却',esc(formatTicks(t.containerCooldownTicks)||'0 tick'))}${row('最近玩家',esc(t.lastContainerPlayerName||'暂无'))}${row('最近关闭时间',esc(nativeTriggerTime(t.lastContainerCloseWallTimeMillis)))}${row('最近事件类型',esc(t.lastContainerEventType||'暂无'))}${row('最近结果',esc(t.lastContainerResult||'暂无'))}</div><p class="muted">容器打开、关闭和内容变化共用 containerEnabled。</p>`;
                }
                function vbdContainerChangeSummary(t){
                  const conditions=t.itemConditions||[];
                  const itemSummary=conditions.length?conditions.slice(0,3).map(c=>`${c.name||c.id||'条件'}: ${c.itemId||c.type||'模板'}`).join('；'):'未配置';
                  return `<div class="identity-grid">${row('容器总开关',esc(labelBool(!!t.containerEnabled)))}${row('内容变化频道',channelCell(t.containerChangeChannel))}${row('容器冷却',esc(formatTicks(t.containerCooldownTicks)||'0 tick'))}${row('检查间隔',esc(formatTicks(t.containerChangeCheckIntervalTicks)||'0 tick'))}${row('物品条件数',esc(t.itemConditionCount??conditions.length))}${row('条件摘要',esc(itemSummary))}${row('最近指纹',esc(t.lastContainerFingerprint||'暂无'))}${row('最近变化时间',esc(nativeTriggerTime(t.lastContainerChangeWallTimeMillis)))}${row('最近结果',esc(t.lastContainerResult||'暂无'))}</div><p class="muted">容器内容变化物品模板 GUI 放到 7.9 P3；P1 只展示摘要。</p>`;
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
                """).append("""
                function nativeTriggerEditableValuesFrom(data={}){
                  const map=data.triggers||{}, red=map.redstone_powered||{}, block=map.blockstate||{}, interaction=map.right_click||{}, open=map.container_open||{}, close=map.container_close||{}, change=map.container_change||{};
                  const conditionRows=Object.entries(block.conditionProperties||{}).map(([property,value])=>({property,value}));
                  return {
                    redstoneEnabled:!!red.configured&&red.mode!=='redstone_disabled',
                    redstoneMode:red.mode==='redstone_disabled'?'redstone_rising':(red.mode||'redstone_rising'),
                    channel:red.channel||'',
                    offChannel:red.offChannel||'',
                    blockStateEnabled:!!block.conditionEnabled,
                    conditionMode:block.conditionMode||'condition_enter',
                    conditionRows,
                    interactionEnabled:!!interaction.interactionEnabled,
                    interactChannel:interaction.interactChannel||'',
                    interactionCooldownTicks:Number(interaction.interactionCooldownTicks||0),
                    containerOpenEnabled:!!open.enabled,
                    containerOpenChannel:open.containerOpenChannel||'',
                    containerCloseEnabled:!!close.enabled,
                    containerCloseChannel:close.containerCloseChannel||'',
                    containerChangeEnabled:!!change.enabled,
                    containerChangeChannel:change.containerChangeChannel||'',
                    containerCooldownTicks:Number(change.containerCooldownTicks??open.containerCooldownTicks??close.containerCooldownTicks??0),
                    containerChangeCheckIntervalTicks:Number(change.containerChangeCheckIntervalTicks||10)
                  };
                }
                function vbdNativeTriggerEditableJson(draft){const v=draft?.values||{};return JSON.stringify({...v,conditionRows:(v.conditionRows||[]).filter(r=>!isBlank(r.property)||!isBlank(r.value)).map(r=>({property:String(r.property||''),value:String(r.value||'')}))});}
                function vbdNativeTriggerDirty(draft){return !!draft&&vbdNativeTriggerEditableJson(draft)!==String(draft.originalJson||'{}');}
                async function prepareVbdNativeTriggerDraft(deviceId,acquireLock=false){
                  const encoded=encodeURIComponent(deviceApiRef(deviceId)), data=await api(`/api/webadmin/virtual-block-devices/${encoded}/native-triggers`), channelOptions=await loadSignalChannelOptions();
                  const canonicalId=data.deviceId||deviceApiRef(deviceId), values=nativeTriggerEditableValuesFrom(data);
                  const draft={deviceId:canonicalId,displayName:data.displayName||canonicalId,supported:data.supported!==false,typeSupported:data.typeSupported!==false,unsupportedReason:data.unsupportedReason||'',values,originalJson:JSON.stringify(values),expectedFingerprint:data.expectedFingerprint||'',lockStatus:data.lockStatus||null,lockId:'',lock:null,errors:[],saving:false,conflict:null,data,channelOptions,channelOptionsError:appState.channelOptionsError,channelComboOpen:{},channelComboIndex:{},channelComboQuery:{},channelComboSearchActive:{},initialSnapshot:JSON.stringify(values)};
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
                  const conflict=draft.conflict?`<div class="readonly-note danger">原生触发配置已被其他操作修改，请重新加载后再保存。<button class="link-button" type="button" onclick='reloadVbdNativeTriggerAfterConflict(${jsString(draft.deviceId)})'>重新加载</button></div>`:'';
                  const lockLine=draft.lockId?`<div class="readonly-note">正在编辑原生触发配置 · 锁到期：${esc(formatDateTime(draft.lock?.expiresAt))}</div>`:(lockHeldByOther(draft.lockStatus)?`<div class="readonly-note danger">${esc(lockMessage(draft.lockStatus,'原生触发配置'))}</div>`:`<div class="readonly-note">当前为只读预览；需要获取编辑锁后才能保存。</div>`);
                  const toggles=`<div class="wa-native-trigger-enable-grid" data-vbd-native-trigger-enable-controls="true">${nativeTriggerTypes().map(item=>{const key=nativeTriggerEnabledKey(item.type), checked=!!v[key];return `<label class="switch-row wa-native-trigger-toggle" data-vbd-native-trigger-toggle="${esc(item.type)}"><span>${esc(item.label)}</span><input id="vbdnt-${esc(item.type)}-enabled" type="checkbox" ${checked?'checked':''} ${disabled?'disabled':''} onchange='toggleVbdNativeTrigger(${jsString(draft.deviceId)},${jsString(item.type)})'></label>`;}).join('')}</div>`;
                  const sections=[
                    v.redstoneEnabled?vbdNativeRedstoneEditSection(draft,disabled):'',
                    v.blockStateEnabled?vbdNativeBlockStateEditSection(draft,disabled):'',
                    v.interactionEnabled?vbdNativeInteractionEditSection(detail,draft,disabled,inline):'',
                    (v.containerOpenEnabled||v.containerCloseEnabled||v.containerChangeEnabled)?vbdNativeContainerCommonEditSection(draft,disabled):'',
                    v.containerOpenEnabled?vbdNativeContainerOpenEditSection(draft,disabled):'',
                    v.containerCloseEnabled?vbdNativeContainerCloseEditSection(draft,disabled):'',
                    v.containerChangeEnabled?vbdNativeContainerChangeEditSection(draft,disabled):''
                  ].filter(Boolean).join('');
                  const empty=!sections?'<div class="readonly-note" data-vbd-native-trigger-edit-empty-state="true">尚未选择原生触发方式。启用上方任一触发方式后才会显示对应配置字段。</div>':'';
                  const body=`${lockLine}${errors}${conflict}${toggles}<div class="wa-native-trigger-edit-sections" data-vbd-native-trigger-edit-modal="true" data-vbd-native-trigger-patch-api="true" data-vbd-native-trigger-no-raw-json="true" data-vbd-native-trigger-no-template-gui="true" data-vbd-native-trigger-field-preservation="true">${sections||empty}</div><p class="muted">本表单只编辑红石 / 受电状态、BlockState、右键交互和容器 open / close / change 基础字段。不会编辑 itemSubmit、consume、inventory/equipment、ConditionEngine、路径图或容器物品模板 GUI。隐藏未启用 section 不会清空 7.8 matcher、itemConditions 或未来保留字段。</p>`;
                  if(inline)return `<div class="wa-native-trigger-inline-editor" data-vbd-native-trigger-inline-edit="true">${body}</div>`;
                  return `<form class="edit-form" onsubmit='event.preventDefault();saveVbdNativeTrigger(${jsString(draft.deviceId)})'>${body}</form>`;
                }
                function nativeTriggerEnabledKey(type){return {redstone_powered:'redstoneEnabled',blockstate:'blockStateEnabled',right_click:'interactionEnabled',container_open:'containerOpenEnabled',container_close:'containerCloseEnabled',container_change:'containerChangeEnabled'}[type]||type;}
                function vbdNativeRedstoneEditSection(draft,disabled){
                  const v=draft.values||{};
                  return `<section class="wa-matcher-option" data-vbd-native-redstone-edit="true"><header><strong>红石 / 受电状态</strong><span class="pill ok">currentPowered = blockStatePowered || receivedPowerLevel &gt; 0</span></header><div class="wa-action-editor-grid"><label>触发模式<select id="vbdnt-redstone-mode" class="select" ${disabled?'disabled':''} onchange='syncVbdNativeTriggerDraftFromForm(${jsString(draft.deviceId)})'>${[['redstone_rising','通电时触发'],['redstone_falling','断电时触发'],['redstone_both','通电和断电都触发']].map(([id,label])=>`<option value="${id}" ${v.redstoneMode===id?'selected':''}>${label}</option>`).join('')}</select></label><label>主频道${renderVbdNativeTriggerChannelCombo(draft.deviceId,'channel',v.channel,draft,disabled)}</label><label>断电频道${renderVbdNativeTriggerChannelCombo(draft.deviceId,'offChannel',v.offChannel,draft,disabled)}</label></div><p class="muted">红石受电状态包含方块自身 powered=true，也包含当前位置收到的弱充能 / 强充能 / 红石强度。</p><p id="vbdnt-channel-hint" class="readonly-note">${channelHintHtml(v.channel,draft.channelOptions||appState.channelOptions||[],draft.channelOptionsError||appState.channelOptionsError)}</p></section>`;
                }
                function vbdNativeBlockStateEditSection(draft,disabled){
                  const v=draft.values||{}, props=((draft.data||{}).triggers?.blockstate?.supportedProperties)||[], rows=(v.conditionRows||[]);
                  const rowHtml=rows.length?rows.map((row,index)=>vbdNativeBlockStateConditionRow(draft,index,row,props,disabled)).join(''):'<div class="readonly-note">尚未添加 BlockState 条件行。</div>';
                  return `<section class="wa-matcher-option" data-vbd-native-blockstate-edit="true" data-vbd-native-blockstate-property-dropdown-from-bound-block="true" data-vbd-native-blockstate-allowed-values-from-property="true"><header><strong>BlockState 条件</strong><span class="pill info">服务端保存时二次校验</span></header><label>条件模式<select id="vbdnt-condition-mode" class="select" ${disabled?'disabled':''} onchange='syncVbdNativeTriggerDraftFromForm(${jsString(draft.deviceId)})'>${[['condition_enter','进入条件时触发'],['condition_exit','退出条件时触发'],['condition_both','进入和退出都触发']].map(([id,label])=>`<option value="${id}" ${v.conditionMode===id?'selected':''}>${label}</option>`).join('')}</select></label><div class="wa-native-blockstate-rows">${rowHtml}</div><div class="inline-actions"><button class="wa-btn ghost" type="button" ${disabled?'disabled':''} onclick='addVbdNativeBlockStateCondition(${jsString(draft.deviceId)})'>${icon('plus')}<span>新增属性条件</span></button></div>${vbdBlockStatePropertyList(props,((draft.data||{}).triggers?.blockstate?.validationIssues)||[])}<p class="muted">属性名和目标值都来自当前绑定方块实际 BlockState；不能输入任意 NBT path 或 raw JSON。</p></section>`;
                }
                function vbdNativeBlockStateConditionRow(draft,index,row,props,disabled){
                  const selected=String(row.property||''), prop=props.find(p=>p.name===selected)||props[0]||{}, values=prop.allowedValues||[], currentValue=values.includes(row.value)?row.value:(row.value||values[0]||'');
                  return `<div class="wa-action-editor-grid" data-vbd-native-blockstate-row="${index}"><label>属性<select id="vbdnt-condition-property-${index}" class="select" ${disabled?'disabled':''} onchange='changeVbdNativeBlockStateProperty(${jsString(draft.deviceId)},${index})'>${props.map(p=>`<option value="${esc(p.name||'')}" ${p.name===selected?'selected':''}>${esc(p.name||'')}</option>`).join('')}</select></label><label>目标值<select id="vbdnt-condition-value-${index}" class="select" ${disabled?'disabled':''} onchange='syncVbdNativeTriggerDraftFromForm(${jsString(draft.deviceId)})'>${values.map(value=>`<option value="${esc(value)}" ${value===currentValue?'selected':''}>${esc(value)}</option>`).join('')}</select></label><button class="wa-btn ghost" type="button" ${disabled?'disabled':''} onclick='removeVbdNativeBlockStateCondition(${jsString(draft.deviceId)},${index})'>删除</button></div>`;
                }
                function vbdNativeInteractionEditSection(detail,draft,disabled,inline=false){
                  const v=draft.values||{}, matcher=appState.interactionItemMatcherEdit&&sameDeviceRef(appState.interactionItemMatcherEdit.deviceId,draft.deviceId)?appState.interactionItemMatcherEdit:null;
                  const summary=vbdInteractionTriggerSummary({id:draft.deviceId,nativeTriggers:draft.data,interactionItemMatcher:detail.interactionItemMatcher||{}},(draft.data?.triggers||{}).right_click||{interactionItemMatcherLayer:{}});
                  const matcherInline=inline&&matcher?`<div data-vbd-native-trigger-interaction-matcher-inline-edit="true">${interactionItemMatcherForm(detail,matcher,true)}</div>`:'';
                  const matcherLock=detail?.interactionItemMatcher?.lockStatus||appState.deviceEditLocks[editLockCacheKey('interaction_item_matcher',draft.deviceId)]||null;
                  const matcherEntry=!inline?'<div class="readonly-note" data-vbd-native-trigger-interaction-matcher-entry="true">交互物品匹配编辑入口保留在 VBD 详情页右键交互摘要和统一设备配置 modal 内；独立原生触发 modal 不嵌套第二个可保存编辑器。</div>':(matcherInline||`<div class="inline-actions" data-vbd-native-trigger-interaction-matcher-entry="true">${lockHeldByOther(matcherLock)?waButton('编辑交互物品匹配','settings',`disabled title="${esc(lockMessage(matcherLock,'交互物品匹配'))}" data-vbd-native-trigger-matcher-lock-disabled="true"`,'ghost is-locked'):waButton('编辑交互物品匹配','settings',disabled?'disabled':htmlHandler(`openInlineInteractionMatcherForVbdNativeTrigger(${jsString(draft.deviceId)})`),'ghost')}</div>`);
                  return `<section class="wa-matcher-option" data-vbd-native-interaction-edit="true" data-vbd-native-trigger-matcher-hidden-when-interaction-disabled="true"><header><strong>玩家右键交互</strong><span class="pill ok">matcher 条件层在此处</span></header><div class="wa-action-editor-grid"><label>交互频道${renderVbdNativeTriggerChannelCombo(draft.deviceId,'interactChannel',v.interactChannel,draft,disabled)}</label><label>交互冷却 tick<input id="vbdnt-interaction-cooldown" class="input" type="number" min="0" max="72000" value="${esc(v.interactionCooldownTicks)}" ${disabled?'disabled':''} oninput='syncVbdNativeTriggerDraftFromForm(${jsString(draft.deviceId)})'></label></div><div class="readonly-note" data-vbd-native-trigger-matcher-visible-inside-interaction="true">${summary}</div>${matcherEntry}<p class="muted">交互物品匹配不是原生触发源；未启用右键交互时会完全隐藏且不会清空已有 matcher 数据。</p></section>`;
                }
                function vbdNativeContainerCommonEditSection(draft,disabled){const v=draft.values||{};return `<section class="wa-matcher-option" data-vbd-native-container-common-edit="true"><header><strong>容器公共设置</strong><span class="pill info">open / close / change 共用</span></header><label>容器冷却 tick<input id="vbdnt-container-cooldown" class="input" type="number" min="0" max="72000" value="${esc(v.containerCooldownTicks)}" ${disabled?'disabled':''} oninput='syncVbdNativeTriggerDraftFromForm(${jsString(draft.deviceId)})'></label><p class="muted">启用任一容器事件时 containerEnabled=true；全部关闭时容器事件和只读 itemConditions 都不会触发，但已保存字段会保留。</p></section>`;}
                function vbdNativeContainerOpenEditSection(draft,disabled){const v=draft.values||{};return `<section class="wa-matcher-option" data-vbd-native-container-open-edit="true"><header><strong>容器打开</strong><span class="pill info">共用 containerEnabled</span></header><label>打开频道${renderVbdNativeTriggerChannelCombo(draft.deviceId,'containerOpenChannel',v.containerOpenChannel,draft,disabled)}</label></section>`;}
                function vbdNativeContainerCloseEditSection(draft,disabled){const v=draft.values||{};return `<section class="wa-matcher-option" data-vbd-native-container-close-edit="true"><header><strong>容器关闭</strong><span class="pill info">共用 containerEnabled</span></header><label>关闭频道${renderVbdNativeTriggerChannelCombo(draft.deviceId,'containerCloseChannel',v.containerCloseChannel,draft,disabled)}</label></section>`;}
                function vbdNativeContainerChangeEditSection(draft,disabled){const v=draft.values||{}, change=(draft.data?.triggers||{}).container_change||{}, conditions=change.itemConditions||[];const itemSummary=conditions.length?conditions.map(c=>`${c.name||c.id||'条件'}: ${c.itemId||c.type||'模板'}`).join('；'):'暂无物品模板条件';return `<section class="wa-matcher-option" data-vbd-native-container-change-edit="true"><header><strong>容器内容变化</strong><span class="pill warning">itemConditions 只读，模板 GUI 在 P3</span></header><div class="wa-action-editor-grid"><label>内容变化频道${renderVbdNativeTriggerChannelCombo(draft.deviceId,'containerChangeChannel',v.containerChangeChannel,draft,disabled)}</label><label>检查间隔 tick<input id="vbdnt-container-check-interval" class="input" type="number" min="1" max="72000" value="${esc(v.containerChangeCheckIntervalTicks)}" ${disabled?'disabled':''} oninput='syncVbdNativeTriggerDraftFromForm(${jsString(draft.deviceId)})'></label></div><div class="readonly-note" data-vbd-native-container-itemconditions-readonly="true">物品条件只读保留：${esc(itemSummary)}</div><p class="muted">P2 不做容器物品模板 GUI、ghost/template item、consume 或 itemSubmit。</p></section>`;}
                function renderVbdNativeTriggerChannelCombo(deviceId,key,value,draft,disabled){
                  const safeKey=String(key).replace(/[^a-zA-Z0-9_-]/g,'-'), open=(draft.channelComboOpen||{})[key]?' open':'';
                  return `<div id="vbdnt-${safeKey}-combo" class="channel-combo vbd-native-channel-combo${open}" data-vbd-native-channel-combo="${esc(key)}"><div class="channel-combo-control"><input id="vbdnt-${safeKey}" class="input" maxlength="128" value="${esc(value||'')}" ${disabled?'disabled':''} placeholder="选择已有频道或输入新频道" autocomplete="off" role="combobox" aria-expanded="${(draft.channelComboOpen||{})[key]?'true':'false'}" aria-controls="vbdnt-${safeKey}-menu" onfocus='openVbdNativeTriggerChannelMenu(${jsString(deviceId)},${jsString(key)})' oninput='syncVbdNativeTriggerDraftFromForm(${jsString(deviceId)},${jsString(key)},true)' onkeydown='handleVbdNativeTriggerChannelKey(event,${jsString(deviceId)},${jsString(key)})'><button class="channel-combo-toggle" type="button" ${disabled?'disabled':''} onclick='toggleVbdNativeTriggerChannelMenu(${jsString(deviceId)},${jsString(key)})' aria-label="显示已有频道">⌄</button></div><div id="vbdnt-${safeKey}-menu" class="channel-combo-menu" role="listbox">${vbdNativeTriggerChannelOptionsHtml(deviceId,key,draft)}</div></div>`;
                }
                function vbdNativeTriggerChannelOptionsHtml(deviceId,key,draft){
                  if(draft.channelOptionsError||appState.channelOptionsError)return '<div class="channel-combo-empty">频道候选加载失败，仍可手动输入新的频道名。</div>';
                  const value=(draft.values||{})[key]||'', options=filteredChannelOptions(draft.channelOptions||appState.channelOptions||[],channelComboQuery(draft,key)), current=normalizeChannelName(value).toLowerCase(), active=Math.max(0,Number((draft.channelComboIndex||{})[key]||0));
                  if(options.length===0)return '<div class="channel-combo-empty">没有匹配的已有频道，可直接保存为新频道</div>';
                  return options.map((c,index)=>`<button type="button" class="channel-combo-option ${index===active?'active':''} ${String(c.channel||'').trim().toLowerCase()===current?'selected':''}" role="option" onmousedown="event.preventDefault()" onclick='selectVbdNativeTriggerChannel(${jsString(deviceId)},${jsString(key)},${jsString(c.channel||'')})'><strong>${esc(c.channel||'未命名频道')}</strong><span>${esc(channelOptionLabel(c))}</span></button>`).join('');
                }
                function syncVbdNativeTriggerChannelCombo(deviceId,key){const draft=appState.vbdNativeTriggerEdit;if(!draft||!sameDeviceRef(draft.deviceId,deviceId))return;const safeKey=String(key).replace(/[^a-zA-Z0-9_-]/g,'-'), combo=document.getElementById(`vbdnt-${safeKey}-combo`), menu=document.getElementById(`vbdnt-${safeKey}-menu`), input=document.getElementById(`vbdnt-${safeKey}`);if(combo)combo.classList.toggle('open',!!(draft.channelComboOpen||{})[key]);if(input)input.setAttribute('aria-expanded',(draft.channelComboOpen||{})[key]?'true':'false');if(menu)menu.innerHTML=vbdNativeTriggerChannelOptionsHtml(deviceId,key,draft);}
                function openVbdNativeTriggerChannelMenu(deviceId,key){const draft=appState.vbdNativeTriggerEdit;if(!draft||!sameDeviceRef(draft.deviceId,deviceId))return;syncVbdNativeTriggerDraftFromForm(deviceId);draft.channelComboOpen=draft.channelComboOpen||{};draft.channelComboIndex=draft.channelComboIndex||{};draft.channelComboOpen[key]=true;draft.channelComboIndex[key]=0;resetChannelComboQuery(draft,key);syncVbdNativeTriggerChannelCombo(deviceId,key);}
                function toggleVbdNativeTriggerChannelMenu(deviceId,key){const draft=appState.vbdNativeTriggerEdit;if(!draft||!sameDeviceRef(draft.deviceId,deviceId))return;syncVbdNativeTriggerDraftFromForm(deviceId);draft.channelComboOpen=draft.channelComboOpen||{};draft.channelComboOpen[key]=!draft.channelComboOpen[key];if(draft.channelComboOpen[key])resetChannelComboQuery(draft,key);syncVbdNativeTriggerChannelCombo(deviceId,key);document.getElementById(`vbdnt-${String(key).replace(/[^a-zA-Z0-9_-]/g,'-')}`)?.focus();}
                function selectVbdNativeTriggerChannel(deviceId,key,channel){const draft=appState.vbdNativeTriggerEdit;if(!draft||!sameDeviceRef(draft.deviceId,deviceId))return;syncVbdNativeTriggerDraftFromForm(deviceId);draft.values[key]=channel||'';draft.channelComboOpen=draft.channelComboOpen||{};draft.channelComboIndex=draft.channelComboIndex||{};draft.channelComboOpen[key]=false;draft.channelComboIndex[key]=0;resetChannelComboQuery(draft,key);const input=document.getElementById(`vbdnt-${String(key).replace(/[^a-zA-Z0-9_-]/g,'-')}`);if(input)input.value=draft.values[key];syncVbdNativeTriggerChannelCombo(deviceId,key);}
                function handleVbdNativeTriggerChannelKey(event,deviceId,key){const draft=appState.vbdNativeTriggerEdit;if(!draft||!sameDeviceRef(draft.deviceId,deviceId))return;const options=filteredChannelOptions(draft.channelOptions||appState.channelOptions||[],channelComboQuery(draft,key));if(event.key==='Escape'){event.preventDefault();event.stopPropagation();draft.channelComboOpen=draft.channelComboOpen||{};draft.channelComboOpen[key]=false;syncVbdNativeTriggerChannelCombo(deviceId,key);return;}if(event.key==='ArrowDown'||event.key==='ArrowUp'){event.preventDefault();draft.channelComboOpen=draft.channelComboOpen||{};draft.channelComboIndex=draft.channelComboIndex||{};draft.channelComboOpen[key]=true;const max=Math.max(0,options.length-1), next=event.key==='ArrowDown'?Number(draft.channelComboIndex[key]||0)+1:Number(draft.channelComboIndex[key]||0)-1;draft.channelComboIndex[key]=Math.min(max,Math.max(0,next));syncVbdNativeTriggerChannelCombo(deviceId,key);return;}if(event.key==='Enter'&&draft.channelComboOpen?.[key]&&options.length>0){event.preventDefault();selectVbdNativeTriggerChannel(deviceId,key,options[Math.min(options.length-1,Number((draft.channelComboIndex||{})[key]||0))].channel);}}
                function syncVbdNativeTriggerDraftFromForm(deviceId,keyToOpen=null,openMenu=false){
                  const draft=appState.vbdNativeTriggerEdit;if(!draft||!sameDeviceRef(draft.deviceId,deviceId))return;const v=draft.values||{};
                  nativeTriggerTypes().forEach(item=>{const key=nativeTriggerEnabledKey(item.type), el=document.getElementById(`vbdnt-${item.type}-enabled`);if(el)v[key]=!!el.checked;});
                  if(document.getElementById('vbdnt-redstone-mode'))v.redstoneMode=document.getElementById('vbdnt-redstone-mode').value;
                  ['channel','offChannel','interactChannel','containerOpenChannel','containerCloseChannel','containerChangeChannel'].forEach(key=>{const el=document.getElementById(`vbdnt-${key.replace(/[^a-zA-Z0-9_-]/g,'-')}`);if(el)v[key]=el.value||'';});
                  if(document.getElementById('vbdnt-condition-mode'))v.conditionMode=document.getElementById('vbdnt-condition-mode').value;
                  if(v.blockStateEnabled)v.conditionRows=(v.conditionRows||[]).map((row,index)=>({property:document.getElementById(`vbdnt-condition-property-${index}`)?.value??row.property??'',value:document.getElementById(`vbdnt-condition-value-${index}`)?.value??row.value??''}));
                  const interactionCooldown=document.getElementById('vbdnt-interaction-cooldown');if(interactionCooldown)v.interactionCooldownTicks=Number(interactionCooldown.value||0);
                  const commonCooldown=document.getElementById('vbdnt-container-cooldown');if(commonCooldown)v.containerCooldownTicks=Number(commonCooldown.value||0);else{const cooldownIds=['vbdnt-container-cooldown-open','vbdnt-container-cooldown-close','vbdnt-container-cooldown-change'];for(const id of cooldownIds){const el=document.getElementById(id);if(el){v.containerCooldownTicks=Number(el.value||0);break;}}}
                  const interval=document.getElementById('vbdnt-container-check-interval');if(interval)v.containerChangeCheckIntervalTicks=Number(interval.value||1);
                  draft.values=v;if(openMenu&&keyToOpen){draft.channelComboOpen=draft.channelComboOpen||{};draft.channelComboIndex=draft.channelComboIndex||{};draft.channelComboOpen[keyToOpen]=true;draft.channelComboIndex[keyToOpen]=0;setChannelComboQuery(draft,v[keyToOpen]||'',keyToOpen);}
                  appState.vbdNativeTriggerEdit=draft;if(keyToOpen)syncVbdNativeTriggerChannelCombo(deviceId,keyToOpen);
                }
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
                function vbdNativeTriggerPatchBody(draft){const v=draft.values||{};return {expectedFingerprint:draft.expectedFingerprint||'',lockId:draft.lockId||'',redstoneEnabled:!!v.redstoneEnabled,redstoneMode:v.redstoneMode||'redstone_rising',channel:v.channel||'',offChannel:v.offChannel||'',blockStateEnabled:!!v.blockStateEnabled,conditionMode:v.conditionMode||'condition_enter',conditionProperties:(v.conditionRows||[]).filter(r=>!isBlank(r.property)||!isBlank(r.value)).map(r=>({property:r.property||'',value:r.value||''})),interactionEnabled:!!v.interactionEnabled,interactChannel:v.interactChannel||'',interactionCooldownTicks:Number(v.interactionCooldownTicks||0),containerOpenEnabled:!!v.containerOpenEnabled,containerOpenChannel:v.containerOpenChannel||'',containerCloseEnabled:!!v.containerCloseEnabled,containerCloseChannel:v.containerCloseChannel||'',containerChangeEnabled:!!v.containerChangeEnabled,containerChangeChannel:v.containerChangeChannel||'',containerCooldownTicks:Number(v.containerCooldownTicks||0),containerChangeCheckIntervalTicks:Number(v.containerChangeCheckIntervalTicks||1)};}
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
                  if(appState.deviceExtendedConfigEdit&&appState.deviceExtendedConfigEdit.deviceId===detail.id)sections.push(`<section class="wa-edit-section" data-edit-section="extended"><header><h3>类型专属配置</h3><span class="pill info">extended config</span></header>${stripEditFormShell(deviceExtendedConfigForm(detail,appState.deviceExtendedConfigEdit))}</section>`);
                  if(isVirtualBlockDevice(detail))sections.push(vbdNativeTriggerConfigModalSection(detail));
                  if(appState.actionRelayActionsEdit&&appState.actionRelayActionsEdit.deviceId===detail.id)sections.push(`<section class="wa-edit-section" data-edit-section="action-relay-actions" data-action-relay-config-modal-section="true"><header><h3>Action 列表</h3><span class="pill warning">action_relay only</span></header>${actionRelayActionsForm(detail,appState.actionRelayActionsEdit,true)}</section>`);
                  const body=sections.length?sections.join(''):'<div class="readonly-note">当前没有可编辑配置区，可能权限不足或该设备类型不支持编辑。</div>';
                  return `<form class="edit-form wa-unified-config-form" data-unified-device-config="true" onsubmit='event.preventDefault();saveDeviceConfig(${jsString(detail.id)})'>${errors}${body}<p class="muted">保存会按已有安全写链路分别提交有变更的显示信息、基础配置、类型专属配置、virtual_block_device 交互物品匹配和 action_relay Action 列表；不会创建或删除真实方块，也不会创建 itemSubmit、consume、ConditionEngine 或逻辑链图。</p></form>`;
                }
                async function acquireWebAdminEditLock(targetType,targetId){
                  return await api('/api/webadmin/edit-locks/acquire',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType,targetId})});
                }
                async function startDeviceConfigEdit(deviceId){
                  const lookupId=deviceApiRef(deviceId), encoded=encodeURIComponent(lookupId);
                  const detail=(appState.currentDeviceDetail&&sameDeviceRef(appState.currentDeviceDetail.id,deviceId))?appState.currentDeviceDetail:await api(`/api/devices/${encoded}`);
                  const expectedType=deviceTypeRefPrefix(deviceId);if(expectedType&&String(detail.type||'').toLowerCase()!==expectedType){toast('该位置当前设备类型已变化，目标类型不存在。');return;}
                  const canonicalId=detail.id||lookupId, canonicalEncoded=encodeURIComponent(canonicalId);
                  const [metadataLockRes,basicRes,extendedRes,actionRes,matcherRes,nativeTriggerRes]=await Promise.all([
                    settle(`/api/webadmin/edit-locks/status?targetType=device_metadata&targetId=${canonicalEncoded}`),
                    settle(`/api/webadmin/device-basic-config/${canonicalEncoded}`),
                    settle(`/api/webadmin/device-extended-config/${canonicalEncoded}`),
                    isActionRelay(detail)?settle(`/api/webadmin/action-relay-actions/${canonicalEncoded}`):Promise.resolve({ok:true,data:null}),
                    isVirtualBlockDevice(detail)?settle(`/api/webadmin/interaction-item-matcher/${canonicalEncoded}`):Promise.resolve({ok:true,data:null}),
                    isVirtualBlockDevice(detail)?settle(`/api/webadmin/virtual-block-devices/${canonicalEncoded}/native-triggers`):Promise.resolve({ok:true,data:null})
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
                    if(canEditDeviceExtendedConfig()&&extendedRes.ok&&extendedRes.data?.supported!==false&&(extendedRes.data?.supportedFields||[]).length){
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
                """).append("""
                function normalizeInteractionItemMatcherDraftData(raw={}){
                  const matcher=raw.matcher||raw||{};
                  return {
                    enabled:!!matcher.enabled,
                    templateItemId:String(matcher.templateItemId||''),
                    countMode:String(matcher.countMode||'at_least'),
                    requiredCount:Number(matcher.requiredCount||1),
                    matchDamage:!!matcher.matchDamage,
                    templateDamage:Number(matcher.templateDamage||0),
                    matchCustomName:!!matcher.matchCustomName,
                    templateCustomName:String(matcher.templateCustomName||''),
                    matchLore:!!matcher.matchLore,
                    templateLore:Array.isArray(matcher.templateLore)?matcher.templateLore.map(v=>String(v||'')):[],
                    interactionItemSource:String(matcher.interactionItemSource||matcher.source||'main_hand'),
                    interactionItemVanillaPolicy:String(matcher.interactionItemVanillaPolicy||matcher.vanillaPolicy||'allow')
                  };
                }
                function interactionItemMatcherEditableJson(draft){const v=normalizeInteractionItemMatcherDraftData(draft?.values||{});return JSON.stringify({...v,templateLore:v.templateLore.filter(line=>!isBlank(line))});}
                function interactionItemMatcherDirty(draft){return !!draft&&interactionItemMatcherEditableJson(draft)!==String(draft.originalMatcherJson||'{}');}
                async function prepareInteractionItemMatcherDraft(deviceId,acquireLock=false){
                  const encoded=encodeURIComponent(deviceApiRef(deviceId)), data=await api(`/api/webadmin/interaction-item-matcher/${encoded}`);
                  const canonicalId=data.deviceId||deviceApiRef(deviceId), values=normalizeInteractionItemMatcherDraftData(data);
                  const draft={deviceId:canonicalId,displayName:data.displayName||canonicalId,supported:data.supported!==false,typeSupported:data.typeSupported!==false,matcherReadable:data.matcherReadable!==false,matcherEditable:data.matcherEditable===true,unsupportedReason:data.unsupportedReason||'',itemSubmitEnabled:!!data.itemSubmitEnabled,interactionEnabled:!!data.interactionEnabled,interactChannel:data.interactChannel||'',interactionCooldownTicks:Number(data.interactionCooldownTicks||0),matcher:data.matcher||{},values,originalMatcherJson:JSON.stringify({...values,templateLore:values.templateLore.filter(line=>!isBlank(line))}),expectedFingerprint:data.expectedFingerprint||'',lockStatus:data.lockStatus||null,lockId:'',lock:null,errors:[],saving:false,conflict:null,allowedCountModes:data.allowedCountModes||['ignore','at_least','exactly','at_most'],allowedSources:data.allowedSources||['main_hand','off_hand'],allowedVanillaPolicies:data.allowedVanillaPolicies||['allow','require_item_match'],readOnlyFields:data.readOnlyFields||[],forbiddenFields:data.forbiddenFields||[],notes:data.notes||[],initialSnapshot:JSON.stringify({...values,templateLore:values.templateLore.filter(line=>!isBlank(line))})};
                  if(acquireLock&&draft.matcherEditable&&canEditInteractionItemMatcher()){
                    if(lockHeldByOther(draft.lockStatus)){draft.errors=[{message:lockMessage(draft.lockStatus,'交互物品匹配')}];appState.interactionItemMatcherEdit=draft;return draft;}
                    const result=await acquireWebAdminEditLock('interaction_item_matcher',canonicalId);
                    if(result.success){draft.lock=result.data?.lock||{};draft.lockId=draft.lock.lockId||'';scheduleInteractionItemMatcherLockHeartbeat();}
                    else draft.errors=[{message:result.message||'交互物品匹配编辑锁获取失败'}];
                  }
                  appState.interactionItemMatcherEdit=draft;
                  return draft;
                }
                async function openInteractionItemMatcherModal(deviceId){try{await prepareInteractionItemMatcherDraft(deviceId,true);showInteractionItemMatcherModal(deviceId);}catch(err){toast(err.message||'无法打开交互物品匹配编辑器');}}
                async function openInteractionItemMatcherReadonlyModal(deviceId){try{await prepareInteractionItemMatcherDraft(deviceId,false);showInteractionItemMatcherModal(deviceId);}catch(err){toast(err.message||'无法查看交互物品匹配');}}
                function showInteractionItemMatcherModal(deviceId){
                  const draft=appState.interactionItemMatcherEdit;if(!draft||draft.deviceId!==deviceApiRef(deviceId)&&!sameDeviceRef(draft.deviceId,deviceId))return;
                  markModalInitialSnapshot('interaction_item_matcher',draft);
                  const canSave=!!draft.lockId&&draft.matcherEditable;
                  const footer=canSave?editModalFooter(draft.saving):waButton('关闭','close','onclick="closeWebAdminModal()"','ghost');
                  openWebAdminModal(canSave?'编辑交互物品匹配':'查看交互物品匹配',interactionItemMatcherForm({id:draft.deviceId,displayName:draft.displayName},draft,false),footer,{className:'wa-config-modal',onClose:async()=>{await cancelInteractionItemMatcherEdit(draft.deviceId,true);await dismissWebAdminModal();},syncBeforeClose:()=>syncModalDraftBeforeClose('interaction_item_matcher',draft.deviceId),dirtyCheck:()=>modalDraftDirty('interaction_item_matcher',appState.interactionItemMatcherEdit)});
                }
                function interactionItemMatcherForm(detail,draft,inline=false){
                  const values=draft.values||{}, disabled=!draft.lockId||!draft.matcherEditable||draft.saving, errors=(draft.errors||[]).length?`<ul class="validation-list">${draft.errors.map(e=>`<li>${esc(e.message||'输入未通过校验')}</li>`).join('')}</ul>`:'';
                  const conflict=draft.conflict?`<div class="readonly-note danger">交互物品匹配已被其他操作修改，请重新加载后再保存。<button class="link-button" type="button" onclick='reloadInteractionItemMatcherAfterConflict(${jsString(draft.deviceId)})'>重新加载</button></div>`:'';
                  const lockLine=draft.lockId?`<div class="readonly-note">正在编辑 · 锁到期：${esc(formatDateTime(draft.lock?.expiresAt))}</div>`:(lockHeldByOther(draft.lockStatus)?`<div class="readonly-note danger">${esc(lockMessage(draft.lockStatus,'交互物品匹配'))}</div>`:`<div class="readonly-note">${esc(draft.unsupportedReason||'当前以只读方式查看。')}</div>`);
                  const lore=(values.templateLore||[]).join('\\n');
                  const sourceAdvanced=!['main_hand','off_hand'].includes(String(values.interactionItemSource||'main_hand'));
                  const policyAdvanced=!['allow','require_item_match'].includes(String(values.interactionItemVanillaPolicy||'allow'));
                  const sourceControl=sourceAdvanced?`<label>物品来源<input id="matcher-source-readonly" class="input" value="${esc(labelInteractionSource(values.interactionItemSource))}" disabled data-matcher-source-readonly="true"></label>`:`<label>物品来源<select id="matcher-source" data-matcher-source="true" class="select" ${disabled?'disabled':''} onchange='syncInteractionItemMatcherDraftFromForm(${jsString(draft.deviceId)})'>${(draft.allowedSources||[]).map(source=>`<option value="${esc(source)}" ${source===values.interactionItemSource?'selected':''}>${esc(labelInteractionSource(source))}</option>`).join('')}</select></label>`;
                  const policyControl=policyAdvanced?`<label>原版交互策略<input id="matcher-vanilla-policy-readonly" class="input" value="${esc(labelVanillaPolicy(values.interactionItemVanillaPolicy))}" disabled data-matcher-vanilla-policy-readonly="true"></label>`:`<label>原版交互策略<select id="matcher-vanilla-policy" data-matcher-vanilla-policy="true" class="select" ${disabled?'disabled':''} onchange='syncInteractionItemMatcherDraftFromForm(${jsString(draft.deviceId)})'>${(draft.allowedVanillaPolicies||[]).map(policy=>`<option value="${esc(policy)}" ${policy===values.interactionItemVanillaPolicy?'selected':''}>${esc(labelVanillaPolicy(policy))}</option>`).join('')}</select></label>`;
                  const matcherEnabled=!!values.enabled;
                  const countValueRow=matcherEnabled&&values.countMode!=='ignore'?`<label data-matcher-count-value-row="true" data-visible-when="countMode:not-ignore">数量<input id="matcher-required-count" data-matcher-required-count="true" class="input" type="number" min="1" max="64" step="1" value="${esc(values.requiredCount||1)}" ${disabled?'disabled':''} oninput='syncInteractionItemMatcherDraftFromForm(${jsString(draft.deviceId)})'></label>`:`<div class="readonly-note" data-matcher-count-ignore-hides-count="true">当前数量规则不检查数量，数量输入已收起。</div>`;
                  const detailFields=matcherEnabled?`<div class="wa-matcher-detail-fields" data-matcher-enabled-item-id-shown="true">
                    <label>物品 ID<input id="matcher-template-item-id" data-matcher-template-item-id="true" class="input" maxlength="128" value="${esc(values.templateItemId||'')}" ${disabled?'disabled':''} placeholder="minecraft:diamond" oninput='syncInteractionItemMatcherDraftFromForm(${jsString(draft.deviceId)})'></label>
                    <section class="wa-matcher-option" data-matcher-count-section="true"><header><strong>数量规则</strong><span class="pill ${values.countMode==='ignore'?'info':'ok'}">${esc(labelCountMode(values.countMode))}</span></header><div class="wa-action-editor-grid"><label>数量规则<select id="matcher-count-mode" data-matcher-count-mode="true" class="select" ${disabled?'disabled':''} onchange='rerenderInteractionItemMatcherEditor(${jsString(draft.deviceId)})'>${(draft.allowedCountModes||[]).map(mode=>`<option value="${esc(mode)}" ${mode===values.countMode?'selected':''}>${esc(labelCountMode(mode))}</option>`).join('')}</select></label>${countValueRow}</div></section>
                    <section class="wa-matcher-option" data-matcher-damage-section="true"><header><label class="switch-row"><span>匹配 damage</span><input id="matcher-match-damage" data-matcher-match-damage="true" type="checkbox" ${values.matchDamage?'checked':''} ${disabled?'disabled':''} onchange='rerenderInteractionItemMatcherEditor(${jsString(draft.deviceId)})'></label><span class="pill ${values.matchDamage?'ok':'info'}">${values.matchDamage?'已启用':'未启用'}</span></header>${values.matchDamage?`<label data-matcher-damage-value-row="true" data-visible-when="matchDamage">damage 值<input id="matcher-template-damage" data-matcher-template-damage="true" class="input" type="number" min="0" step="1" value="${esc(values.templateDamage||0)}" ${disabled?'disabled':''} oninput='syncInteractionItemMatcherDraftFromForm(${jsString(draft.deviceId)})'></label>`:`<p class="muted" data-matcher-damage-value-hidden="true">未启用时不参与匹配，已保存的草稿值会保留。</p>`}</section>
                    <section class="wa-matcher-option" data-matcher-custom-name-section="true"><header><label class="switch-row"><span>匹配自定义名称</span><input id="matcher-match-custom-name" data-matcher-match-custom-name="true" type="checkbox" ${values.matchCustomName?'checked':''} ${disabled?'disabled':''} onchange='rerenderInteractionItemMatcherEditor(${jsString(draft.deviceId)})'></label><span class="pill ${values.matchCustomName?'ok':'info'}">${values.matchCustomName?'已启用':'未启用'}</span></header>${values.matchCustomName?`<label data-matcher-custom-name-value-row="true" data-visible-when="matchCustomName">自定义名称<input id="matcher-template-custom-name" data-matcher-template-custom-name="true" class="input" maxlength="128" value="${esc(values.templateCustomName||'')}" ${disabled?'disabled':''} oninput='syncInteractionItemMatcherDraftFromForm(${jsString(draft.deviceId)})'></label>`:`<p class="muted" data-matcher-custom-name-value-hidden="true">未启用时不检查自定义名称，草稿值保留。</p>`}</section>
                    <section class="wa-matcher-option" data-matcher-lore-section="true"><header><label class="switch-row"><span>匹配 Lore</span><input id="matcher-match-lore" data-matcher-match-lore="true" type="checkbox" ${values.matchLore?'checked':''} ${disabled?'disabled':''} onchange='rerenderInteractionItemMatcherEditor(${jsString(draft.deviceId)})'></label><span class="pill ${values.matchLore?'ok':'info'}">${values.matchLore?'已启用':'未启用'}</span></header>${values.matchLore?`<label data-matcher-lore-value-row="true" data-visible-when="matchLore">Lore（每行一条）<textarea id="matcher-template-lore" data-matcher-template-lore="true" data-matcher-match-lore-value="true" maxlength="3000" ${disabled?'disabled':''} oninput='syncInteractionItemMatcherDraftFromForm(${jsString(draft.deviceId)})'>${esc(lore)}</textarea></label>`:`<p class="muted" data-matcher-lore-value-hidden="true">未启用时不检查 Lore，草稿行会保留。</p>`}</section>
                    <section class="wa-matcher-option" data-matcher-source-policy-section="true"><header><strong>来源与原版交互</strong><span class="pill info">普通 matcher</span></header><div class="wa-action-editor-grid">${sourceControl}${policyControl}</div></section>
                  </div>`:`<div class="readonly-note" data-matcher-disabled-fields-collapsed="true">未要求交互物品匹配。详细配置已收起；已有草稿值会保留，重新启用后可继续编辑。</div>`;
                  const fields=`<section class="wa-edit-section" data-edit-section="interaction-item-matcher" data-interaction-item-matcher-section="true" data-interaction-item-matcher-config-modal-section="true" data-matcher-toggle-preserves-scroll="true"><header><h3>交互物品匹配</h3><span class="pill warning">virtual_block_device only</span></header>
                    ${lockLine}
                    ${draft.itemSubmitEnabled?'<div class="readonly-note danger">当前设备已启用 itemSubmit；7.8 不编辑 itemSubmit 或多物品提交，普通 matcher 只读。</div>':''}
                    <label class="switch-row"><span>要求交互物品匹配</span><input id="matcher-enabled" data-matcher-enabled="true" type="checkbox" ${values.enabled?'checked':''} ${disabled?'disabled':''} onchange='rerenderInteractionItemMatcherEditor(${jsString(draft.deviceId)})'></label>
                    ${detailFields}
                    ${sourceAdvanced||policyAdvanced?`<p class="readonly-note danger">当前 matcher 使用 7.8 暂不编辑的来源或原版策略，只能只读查看，避免覆盖存量高级配置。</p>`:''}
                    <div class="identity-grid" data-matcher-advanced-readonly-section="true">${row('交互频道',channelCell(draft.interactChannel))}${row('成功频道',channelCell(draft.matcher?.successChannel))}${row('失败频道',channelCell(draft.matcher?.failChannel))}${row('交互冷却',esc(`${draft.interactionCooldownTicks||0} ticks`))}${row('consume',esc(draft.matcher?.consumeEnabled?'已配置（只读）':'未启用'))}${row('raw component',esc(draft.matcher?.templateComponentsPresent?'存在（只读）':'无'))}</div>
                    ${errors}${conflict}
                    <p class="muted" data-interaction-item-matcher-no-raw-json="true">本阶段不会打开 raw JSON / NBT / data component 编辑器，不会创建 itemSubmit、consume、inventory/equipment matcher、ConditionEngine 或成功/失败路径图。</p>
                  </section>`;
                  if(inline)return fields;
                  return `<form class="edit-form" data-interaction-item-matcher-modal="true" onsubmit='event.preventDefault();saveInteractionItemMatcher(${jsString(draft.deviceId)})'>${fields}</form>`;
                }
                function syncInteractionItemMatcherDraftFromForm(deviceId){
                  const draft=appState.interactionItemMatcherEdit;if(!draft||!sameDeviceRef(draft.deviceId,deviceId))return;
                  if(!document.getElementById('matcher-enabled'))return;
                  const v=draft.values||{};
                  v.enabled=!!document.getElementById('matcher-enabled')?.checked;
                  v.templateItemId=document.getElementById('matcher-template-item-id')?.value ?? v.templateItemId ?? '';
                  v.countMode=document.getElementById('matcher-count-mode')?.value||v.countMode||'at_least';
                  v.requiredCount=Number(document.getElementById('matcher-required-count')?.value ?? v.requiredCount ?? 1);
                  v.matchDamage=!!document.getElementById('matcher-match-damage')?.checked;
                  v.templateDamage=Number(document.getElementById('matcher-template-damage')?.value ?? v.templateDamage ?? 0);
                  v.matchCustomName=!!document.getElementById('matcher-match-custom-name')?.checked;
                  v.templateCustomName=document.getElementById('matcher-template-custom-name')?.value ?? v.templateCustomName ?? '';
                  v.matchLore=!!document.getElementById('matcher-match-lore')?.checked;
                  if(document.getElementById('matcher-template-lore'))v.templateLore=String(document.getElementById('matcher-template-lore')?.value||'').split('\\n').map(line=>line.trim()).filter(Boolean);
                  else v.templateLore=Array.isArray(v.templateLore)?v.templateLore:[];
                  v.interactionItemSource=document.getElementById('matcher-source')?.value||v.interactionItemSource||'main_hand';
                  v.interactionItemVanillaPolicy=document.getElementById('matcher-vanilla-policy')?.value||v.interactionItemVanillaPolicy||'allow';
                  draft.values=v;appState.interactionItemMatcherEdit=draft;
                }
                function interactionItemMatcherPatchBody(draft){const v=normalizeInteractionItemMatcherDraftData(draft.values||{});return {enabled:v.enabled,templateItemId:v.templateItemId,countMode:v.countMode,requiredCount:v.requiredCount,matchDamage:v.matchDamage,templateDamage:v.templateDamage,matchCustomName:v.matchCustomName,templateCustomName:v.templateCustomName,matchLore:v.matchLore,templateLore:v.templateLore,interactionItemSource:v.interactionItemSource,interactionItemVanillaPolicy:v.interactionItemVanillaPolicy,expectedFingerprint:draft.expectedFingerprint||'',lockId:draft.lockId||''};}
                async function patchInteractionItemMatcherDraft(deviceId,draft){return await api(`/api/webadmin/interaction-item-matcher/${encodeURIComponent(deviceApiRef(deviceId))}`,{method:'PATCH',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify(interactionItemMatcherPatchBody(draft))});}
                async function saveInteractionItemMatcher(deviceId){const draft=appState.interactionItemMatcherEdit;if(!draft||!draft.lockId){toast('当前交互物品匹配只读。');return;}syncInteractionItemMatcherDraftFromForm(deviceId);draft.saving=true;draft.errors=[];draft.conflict=null;appState.interactionItemMatcherEdit=draft;rerenderInteractionItemMatcherEditor(deviceId);try{const result=await patchInteractionItemMatcherDraft(deviceId,draft);if(result.success){markChannelOptionsDirty({type:'device_config_changed',payload:{targetType:'interaction_item_matcher'}});appState.interactionItemMatcherEdit=null;stopInteractionItemMatcherLockHeartbeat();appState.modalCloseHandler=null;await dismissWebAdminModal();toast(result.changed?(result.message||'交互物品匹配已保存。'):'没有变更。');await refreshCurrentDeviceContext(deviceId);return;}draft.saving=false;draft.errors=result.validationErrors&&result.validationErrors.length?result.validationErrors:[{message:result.message||'保存失败'}];draft.conflict=result.code==='conflict_detected'?result.conflict||{remote:true}:null;if(['edit_lock_expired','edit_lock_conflict','edit_lock_required'].includes(result.code)){draft.lockId='';draft.lock=null;stopInteractionItemMatcherLockHeartbeat();}appState.interactionItemMatcherEdit=draft;toast(result.message||'保存失败');rerenderInteractionItemMatcherEditor(deviceId);}catch(err){draft.saving=false;draft.errors=[{message:err.message||'保存失败'}];appState.interactionItemMatcherEdit=draft;toast(err.message||'保存失败');rerenderInteractionItemMatcherEditor(deviceId);}}
                function captureModalScrollState(){const body=document.querySelector('#wa-modal-root .wa-modal-body'), active=document.activeElement;return {scrollTop:body?body.scrollTop:0,activeId:active&&active.id?active.id:'',selectionStart:active&&typeof active.selectionStart==='number'?active.selectionStart:null,selectionEnd:active&&typeof active.selectionEnd==='number'?active.selectionEnd:null};}
                function restoreModalScrollState(state){requestAnimationFrame(()=>{const body=document.querySelector('#wa-modal-root .wa-modal-body');if(body&&state)body.scrollTop=state.scrollTop||0;const active=state&&state.activeId?document.getElementById(state.activeId):null;if(active&&typeof active.focus==='function'){active.focus({preventScroll:true});if(state.selectionStart!==null&&typeof active.setSelectionRange==='function')active.setSelectionRange(state.selectionStart,state.selectionEnd);}});}
                function withPreservedModalScroll(work){const state=captureModalScrollState();work();restoreModalScrollState(state);}
                function rerenderInteractionItemMatcherEditor(deviceId){const draft=appState.interactionItemMatcherEdit;if(!draft||!sameDeviceRef(draft.deviceId,deviceId))return;withPreservedModalScroll(()=>{syncInteractionItemMatcherDraftFromForm(deviceId);if(appState.deviceConfigEdit&&sameDeviceRef(appState.deviceConfigEdit.deviceId,deviceId)){applyDeviceConfigDraftsFromForm(deviceId);showDeviceConfigEditModal(draft.deviceId);}else showInteractionItemMatcherModal(draft.deviceId);});}
                async function reloadInteractionItemMatcherAfterConflict(deviceId){try{await releaseInteractionItemMatcherLock(appState.interactionItemMatcherEdit,true);await prepareInteractionItemMatcherDraft(deviceId,true);rerenderInteractionItemMatcherEditor(deviceId);}catch(err){toast(err.message||'交互物品匹配重新加载失败');}}
                function scheduleInteractionItemMatcherLockHeartbeat(){stopInteractionItemMatcherLockHeartbeat();appState.interactionItemMatcherLockTimer=setTimeout(async()=>{await heartbeatInteractionItemMatcherLock();if(appState.interactionItemMatcherEdit?.lockId)scheduleInteractionItemMatcherLockHeartbeat();},30000);}
                function stopInteractionItemMatcherLockHeartbeat(){if(appState.interactionItemMatcherLockTimer){clearTimeout(appState.interactionItemMatcherLockTimer);appState.interactionItemMatcherLockTimer=null;}}
                async function heartbeatInteractionItemMatcherLock(){const draft=appState.interactionItemMatcherEdit;if(!draft||!draft.lockId)return;try{const result=await api('/api/webadmin/edit-locks/heartbeat',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'interaction_item_matcher',targetId:draft.deviceId,lockId:draft.lockId})});if(result.success){draft.lock=result.data?.lock||draft.lock;appState.interactionItemMatcherEdit=draft;return;}draft.errors=[{message:result.message||'交互物品匹配编辑锁续期失败'}];draft.lockId='';draft.lock=null;appState.interactionItemMatcherEdit=draft;stopInteractionItemMatcherLockHeartbeat();rerenderInteractionItemMatcherEditor(draft.deviceId);}catch(err){draft.errors=[{message:err.message||'交互物品匹配编辑锁续期失败'}];draft.lockId='';draft.lock=null;appState.interactionItemMatcherEdit=draft;stopInteractionItemMatcherLockHeartbeat();rerenderInteractionItemMatcherEditor(draft.deviceId);}}
                async function releaseInteractionItemMatcherLock(draft,silent){if(!draft||!draft.lockId)return;try{await api('/api/webadmin/edit-locks/release',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'interaction_item_matcher',targetId:draft.deviceId,lockId:draft.lockId})});}catch(err){if(!silent)toast(err.message||'交互物品匹配编辑锁释放失败，将等待自动过期。');}}
                async function cancelInteractionItemMatcherEdit(deviceId,silent=false){const draft=appState.interactionItemMatcherEdit;if(draft&&sameDeviceRef(draft.deviceId,deviceId))await releaseInteractionItemMatcherLock(draft,silent);if(draft&&sameDeviceRef(draft.deviceId,deviceId)){appState.interactionItemMatcherEdit=null;stopInteractionItemMatcherLockHeartbeat();}}
                function maybeReleaseInteractionItemMatcherEditForRoute(hash){const draft=appState.interactionItemMatcherEdit;if(!draft)return;if(String(hash||'').startsWith('#/devices/')){const info=detailRoute(String(hash).substring('#/devices/'.length),'#/devices'), routeType=deviceTypeRefPrefix(info.id);if((!routeType||routeType==='virtual_block_device')&&sameDeviceRef(info.id,draft.deviceId))return;}releaseInteractionItemMatcherLock(draft,true);appState.interactionItemMatcherEdit=null;stopInteractionItemMatcherLockHeartbeat();}
                function actionRelayActionListReadonlyCard(detail){
                  if(detail.actionRelayActionsError)return errorBlock(detail.actionRelayActionsError.message||'Action 列表加载失败');
                  const data=detail.actionRelayActions||{}, actions=data.actions||[];
                  const lockedByOther=actionRelayLockHeldByOther(data.lockStatus);
                  const rows=!data.actionsReadable?`<div class="empty-state">动作详情当前不可读取；store 快照记录 ${esc(data.snapshotActionCount??data.actionCount??0)} 条动作。请根据下方运行状态检查 world / chunk / block entity。</div>`:(actions.length?actions.slice(0,6).map(a=>`<div class="wa-compact-row"><strong>#${esc(Number(a.displayIndex||a.index+1||1))} ${esc(labelActionType(a.type))}</strong><span>${esc(a.summary||a.value||'')}</span><small>${a.enabled===false?'已禁用':'已启用'}</small></div>`).join(''):'<div class="empty-state">当前 Action Relay 还没有动作。</div>');
                  const note=lockedByOther?`<p class="readonly-note" data-action-relay-lock-held="true">${esc(actionRelayLockMessage(data.lockStatus))}</p>`:(data.actionsEditable===false?`<p class="readonly-note danger">${esc(data.unsupportedReason||'当前 action_relay 不可编辑。')}</p>`:`<p class="muted">Action 列表只属于已放置 action_relay 的配置；不会创建或删除世界方块。</p>`);
                  const buttonLabel=lockedByOther?'只读查看 Action 列表':(data.actionsEditable===false?'查看 Action 状态':'编辑 Action 列表');
                  const handler=lockedByOther?htmlHandler(`openActionRelayActionsReadonlyModal(${jsString(detail.id)})`):(canEditActionRelayActions()?htmlHandler(`openActionRelayActionsModal(${jsString(detail.id)})`):'disabled');
                  return `<div class="wa-action-readonly-card" data-action-relay-detail-card="true">${rows}${actions.length>6?`<p class="muted">还有 ${esc(actions.length-6)} 条动作未展示。</p>`:''}${note}<div class="inline-actions">${waButton(buttonLabel,'action-relay',handler,lockedByOther?'ghost':'primary')}</div></div>`;
                }
                function actionRelayRuntimeStatusHtml(data){
                  const state=String(data?.loadedState||''), reason=data?.unsupportedReason||'', block=data?.blockId?`当前方块：${data.blockId}`:'', be=data?.blockEntityType?`方块实体：${data.blockEntityType}`:'', count=Number(data?.snapshotActionCount??data?.actionCount??0);
                  if(!state||state==='ready')return `<div class="readonly-note">Action Relay 方块实体已加载，Action 列表可按权限编辑。</div>`;
                  return `<div class="readonly-note danger" data-action-relay-loaded-state="${esc(state)}">${esc([reason,block,be,`快照动作数：${count}`].filter(Boolean).join(' · '))}</div>`;
                }
                function labelActionType(type){return {command:'命令',signal:'Signal',message:'消息',sound:'音效',COMMAND:'命令',SIGNAL:'Signal',MESSAGE:'消息',SOUND:'音效'}[String(type||'')]||String(type||'未知');}
                function actionTypeOptions(value){return ['command','signal','message','sound'].map(type=>`<option value="${type}" ${String(value||'command').toLowerCase()===type?'selected':''}>${esc(labelActionType(type))}</option>`).join('');}
                function normalizeActionRelayEditableAction(action={}){return {type:String(action.type||'command').toLowerCase(),value:String(action.value||''),enabled:action.enabled!==false,requiresOp:!!action.requiresOp,cooldownTicks:Number(action.cooldownTicks||0),notifyOps:!!action.notifyOps};}
                function normalizeActionRelayDraftAction(action={}){return {...normalizeActionRelayEditableAction(action),summary:String(action.summary||'')};}
                function actionRelayActionsEditableJson(actions){return JSON.stringify((actions||[]).map(normalizeActionRelayEditableAction));}
                function actionRelayActionsJson(actions){return JSON.stringify((actions||[]).map(normalizeActionRelayDraftAction));}
                function actionRelayActionsDirty(draft){return !!draft&&actionRelayActionsEditableJson(draft.actions)!==String(draft.originalActionsJson||'[]');}
                async function prepareActionRelayActionsDraft(deviceId,acquireLock=false){
                  const encoded=encodeURIComponent(deviceApiRef(deviceId)), data=await api(`/api/webadmin/action-relay-actions/${encoded}`), channelOptions=await loadSignalChannelOptions();
                  const canonicalId=data.deviceId||deviceApiRef(deviceId);
                  const actions=(data.actions||[]).map(normalizeActionRelayDraftAction);
                  const draft={deviceId:canonicalId,displayName:data.displayName||canonicalId,channel:data.channel||'',supported:data.supported!==false,typeSupported:data.typeSupported!==false,actionsReadable:data.actionsReadable===true,actionsEditable:data.actionsEditable===true,unsupportedReason:data.unsupportedReason||'',loadedState:data.loadedState||'',worldAvailable:data.worldAvailable,chunkLoaded:data.chunkLoaded,blockEntityLoaded:data.blockEntityLoaded,blockEntityType:data.blockEntityType||'',blockId:data.blockId||'',snapshotActionCount:data.snapshotActionCount??data.actionCount??0,actions,originalActionsJson:actionRelayActionsEditableJson(actions),expectedFingerprint:data.expectedFingerprint||'',lockStatus:data.lockStatus||null,lockId:'',lock:null,errors:[],saving:false,conflict:null,channelOptions,channelOptionsError:appState.channelOptionsError,channelComboOpen:{},channelComboIndex:{},channelComboQuery:{},channelComboSearchActive:{},initialSnapshot:actionRelayActionsEditableJson(actions)};
                  if(acquireLock&&draft.actionsEditable&&canEditActionRelayActions()){
                    if(actionRelayLockHeldByOther(draft.lockStatus)){draft.errors=[{message:actionRelayLockMessage(draft.lockStatus)}];appState.actionRelayActionsEdit=draft;return draft;}
                    const result=await acquireWebAdminEditLock('action_relay_actions',canonicalId);
                    if(result.success){const lock=result.data?.lock||{};draft.lockId=lock.lockId||'';draft.lock=lock;scheduleActionRelayActionsLockHeartbeat();}
                    else draft.errors=[{message:result.message||'Action 列表编辑锁获取失败'}];
                  }
                  appState.actionRelayActionsEdit=draft;
                  return draft;
                }
                async function openActionRelayActionsModal(deviceId){
                  try{
                    const draft=await prepareActionRelayActionsDraft(deviceId,true);
                    showActionRelayActionsModal(draft.deviceId);
                    if(!draft.actionsEditable)toast(draft.unsupportedReason||'当前 Action Relay 不可编辑。');
                    else if(draft.errors.length)toast(draft.errors[0].message||'无法进入 Action 列表编辑。');
                  }catch(err){toast(err.message||'Action 列表加载失败');}
                }
                async function openActionRelayActionsReadonlyModal(deviceId){
                  try{const draft=await prepareActionRelayActionsDraft(deviceId,false);showActionRelayActionsModal(draft.deviceId);}catch(err){toast(err.message||'Action 列表加载失败');}
                }
                function showActionRelayActionsModal(deviceId){
                  const draft=appState.actionRelayActionsEdit;if(!draft||draft.deviceId!==deviceId)return;
                  markModalInitialSnapshot('action_relay_actions',draft);
                  const footer=draft.lockId?editModalFooter(draft.saving):waButton('关闭','close','onclick="closeWebAdminModal()"','ghost');
                  openWebAdminModal(draft.lockId?'编辑 Action 列表':'查看 Action 列表',actionRelayActionsForm({id:deviceId,displayName:draft.displayName},draft,false),footer,{className:'wa-action-relay-modal',onClose:async()=>{await cancelActionRelayActionsEdit(deviceId,true);await dismissWebAdminModal();},syncBeforeClose:()=>syncModalDraftBeforeClose('action_relay_actions',deviceId),dirtyCheck:()=>!!appState.actionRelayActionsEdit?.lockId&&modalDraftDirty('action_relay_actions',appState.actionRelayActionsEdit)});
                }
                function actionRelayActionsForm(detail,draft,inline=false){
                  const errors=(draft.errors||[]).length?`<ul class="validation-list">${draft.errors.map(e=>`<li>${esc(e.message||e||'保存失败')}</li>`).join('')}</ul>`:'';
                  const lockLine=draft.lockId?`<div class="readonly-note">正在编辑 action_relay_actions · 锁到期：${esc(formatDateTime(draft.lock?.expiresAt))}</div>`:(draft.actionsEditable?'<div class="readonly-note">当前为只读预览；需要获取编辑锁后才能保存。</div>':'<div class="readonly-note">当前 Action 列表以只读状态显示；运行状态见下方状态卡。</div>');
                  const conflict=draft.conflict?`<div class="readonly-note danger">Action 列表已被其他用户修改，请重新加载。<button class="link-button" type="button" onclick='reloadActionRelayActionsAfterConflict(${jsString(draft.deviceId)})'>重新加载</button></div>`:'';
                  const rows=!draft.actionsReadable?`<div class="empty-state">动作详情当前不可读取；store 快照记录 ${esc(draft.snapshotActionCount??0)} 条动作。请让玩家加载该方块并确认当前位置仍是 action_relay。</div>`:((draft.actions||[]).length?draft.actions.map((action,index)=>actionRelayActionRow(draft.deviceId,action,index,draft)).join(''):'<div class="empty-state">当前 Action Relay 还没有动作。可以从下方新增 command、signal、message 或 sound action。</div>');
                  const addDisabled=!draft.lockId||draft.saving?'disabled':'';
                  const addButtons=`<div class="inline-actions" data-action-add-controls="true">${['command','signal','message','sound'].map(type=>`<button class="wa-btn ghost" type="button" ${addDisabled} data-action-add="${type}" onclick='addActionRelayAction(${jsString(draft.deviceId)},${jsString(type)})'>${icon('plus')}<span>新增 ${esc(labelActionType(type))}</span></button>`).join('')}</div>`;
                  const content=`${lockLine}${actionRelayRuntimeStatusHtml(draft)}${errors}${conflict}<div class="wa-action-list-editor" data-action-relay-actions-modal="true" data-action-relay-action-list="true" data-action-relay-no-raw-json="true" data-action-list-preserve-scroll="true">${rows}</div>${addButtons}<p class="muted">Action 列表按顺序执行；signal action 可选择已有频道或手动输入新频道，新频道不会自动创建消费者。此处不会打开 JSON 文本框，不会启用 matcher / itemSubmit / ConditionEngine。</p>`;
                  if(inline)return `<div class="wa-action-inline-section" data-action-relay-config-modal-section="true">${content}</div>`;
                  return `<form class="edit-form" onsubmit='event.preventDefault();saveActionRelayActions(${jsString(draft.deviceId)})'>${content}</form>`;
                }
                function actionRelayActionRow(deviceId,action,index,draft){
                  const valueEditor=actionRelayActionValueEditor(deviceId,action,index,draft);
                  const disabled=!draft.lockId||draft.saving?'disabled':'';
                  const pending=draft.pendingDeleteIndex===index?actionRelayActionDeleteConfirm(deviceId,action,index):'';
                  return `<article class="wa-action-row" data-action-index="${index}" data-action-type="${esc(action.type)}"><header><div><strong>#${index+1} ${esc(labelActionType(action.type))}</strong><small>${esc(action.summary||action.value||'尚未配置')}</small></div><select class="select" id="ara-${index}-type" ${disabled} onchange='changeActionRelayActionType(${jsString(deviceId)},${index})'>${actionTypeOptions(action.type)}</select></header><div class="wa-action-editor-grid">${valueEditor}<label class="switch-row">启用<input id="ara-${index}-enabled" type="checkbox" ${action.enabled?'checked':''} ${disabled} onchange='syncActionRelayActionsDraftFromForm(${jsString(deviceId)})'></label><label class="switch-row">需要 OP<input id="ara-${index}-requires-op" type="checkbox" ${action.requiresOp?'checked':''} ${disabled} onchange='syncActionRelayActionsDraftFromForm(${jsString(deviceId)})'></label><label>冷却 tick<input id="ara-${index}-cooldown" class="input" type="number" min="0" max="72000" value="${esc(action.cooldownTicks)}" ${disabled} onchange='syncActionRelayActionsDraftFromForm(${jsString(deviceId)})'><span class="muted">字段会保留；执行侧按现有 ActionEngine 语义生效。</span></label><label class="switch-row">通知 OP<input id="ara-${index}-notify" type="checkbox" ${action.notifyOps?'checked':''} ${disabled} onchange='syncActionRelayActionsDraftFromForm(${jsString(deviceId)})'></label></div>${pending}<div class="inline-actions"><button class="wa-btn ghost" type="button" ${disabled||index===0?'disabled':''} data-action-reorder="up" onclick='moveActionRelayAction(${jsString(deviceId)},${index},-1)'>上移</button><button class="wa-btn ghost" type="button" ${disabled||index>=(draft.actions||[]).length-1?'disabled':''} data-action-reorder="down" onclick='moveActionRelayAction(${jsString(deviceId)},${index},1)'>下移</button><button class="wa-btn danger" type="button" ${disabled} data-action-delete="true" onclick='requestDeleteActionRelayAction(${jsString(deviceId)},${index})'>删除</button></div></article>`;
                }
                function actionRelayActionDeleteConfirm(deviceId,action,index){
                  return `<div class="readonly-note danger" data-action-delete-confirm="true" data-danger-confirm-modal="true"><strong>确认删除 #${index+1} ${esc(labelActionType(action.type))} action</strong><span>摘要：${esc(action.summary||action.value||'尚未配置')}。删除只修改当前草稿，点击保存后才会写入 action_relay 配置。</span><div class="inline-actions"><button class="wa-btn danger" type="button" onclick='confirmDeleteActionRelayAction(${jsString(deviceId)},${index})'>确认删除</button><button class="wa-btn ghost" type="button" onclick='cancelDeleteActionRelayAction(${jsString(deviceId)})'>取消</button></div></div>`;
                }
                function actionRelayActionValueEditor(deviceId,action,index,draft){
                  const disabled=!draft.lockId||draft.saving?'disabled':'';
                  const type=String(action.type||'command').toLowerCase(), value=action.value||'';
                  if(type==='signal')return `<label class="wa-action-value-field" data-signal-action-editor="true">目标频道${renderActionRelaySignalChannelCombo(deviceId,index,action,draft)}<span id="ara-${index}-channel-hint" class="readonly-note">${channelHintHtml(value,draft.channelOptions||appState.channelOptions||[],draft.channelOptionsError||appState.channelOptionsError)}</span></label>`;
                  if(type==='message')return `<label class="wa-action-value-field" data-message-action-editor="true">消息文本<textarea id="ara-${index}-value" class="input wa-action-textarea" maxlength="500" ${disabled} oninput='syncActionRelayActionsDraftFromForm(${jsString(deviceId)})'>${esc(value)}</textarea></label>`;
                  if(type==='sound')return `<label class="wa-action-value-field" data-sound-action-editor="true">音效 ID<input id="ara-${index}-value" class="input" maxlength="128" value="${esc(value)}" ${disabled} placeholder="minecraft:entity.experience_orb.pickup" oninput='syncActionRelayActionsDraftFromForm(${jsString(deviceId)})'><span class="muted">当前底层只保存 sound id；实际播放按现有 ActionEngine 能力生效。</span></label>`;
                  return `<label class="wa-action-value-field" data-command-action-editor="true">命令内容<input id="ara-${index}-value" class="input" maxlength="512" value="${esc(value)}" ${disabled} placeholder="say hello" oninput='syncActionRelayActionsDraftFromForm(${jsString(deviceId)})'><span class="muted">不要输入开头的 /；setblock、fill、clone、function、schedule、execute、tp、teleport、data、gamerule 等地图控制命令允许保存。WebAdmin 只硬阻断 ban、kick、op、stop、whitelist 等服务器管理高风险命令。</span></label>`;
                }
                function renderActionRelaySignalChannelCombo(deviceId,index,action,draft){
                  const open=(draft.channelComboOpen||{})[index]?' open':'';
                  const disabled=!draft.lockId||draft.saving?'disabled':'';
                  return `<div id="ara-${index}-channel-combo" class="channel-combo action-relay-channel-combo${open}"><div class="channel-combo-control"><input id="ara-${index}-value" class="input" maxlength="128" value="${esc(action.value||'')}" ${disabled} placeholder="选择已有频道或输入新频道" autocomplete="off" role="combobox" aria-expanded="${(draft.channelComboOpen||{})[index]?'true':'false'}" aria-controls="ara-${index}-channel-menu" onfocus='openActionRelayActionChannelMenu(${jsString(deviceId)},${index})' oninput='syncActionRelayActionsDraftFromForm(${jsString(deviceId)},${index},true)' onkeydown='handleActionRelayActionChannelKey(event,${jsString(deviceId)},${index})'><button class="channel-combo-toggle" type="button" ${disabled} onclick='toggleActionRelayActionChannelMenu(${jsString(deviceId)},${index})' aria-label="显示已有频道">⌄</button></div><div id="ara-${index}-channel-menu" class="channel-combo-menu" role="listbox">${actionRelayChannelOptionsHtml(deviceId,index,draft)}</div></div>`;
                }
                function actionRelayChannelOptionsHtml(deviceId,index,draft){
                  if(draft.channelOptionsError||appState.channelOptionsError)return '<div class="channel-combo-empty">频道候选加载失败，仍可手动输入新的频道名。</div>';
                  const action=(draft.actions||[])[index]||{}, options=filteredChannelOptions(draft.channelOptions||appState.channelOptions||[],channelComboQuery(draft,index)), current=normalizeChannelName(action.value).toLowerCase(), indexes=draft.channelComboIndex||{}, active=Math.max(0,Number(indexes[index]||0));
                  if(options.length===0)return '<div class="channel-combo-empty">没有匹配的已有频道，可直接保存为新频道</div>';
                  return options.map((c,i)=>`<button type="button" class="channel-combo-option ${i===active?'active':''} ${String(c.channel||'').trim().toLowerCase()===current?'selected':''}" role="option" onmousedown="event.preventDefault()" onclick='selectActionRelayActionChannel(${jsString(deviceId)},${index},${jsString(c.channel||'')})'><strong>${esc(c.channel||'未命名频道')}</strong><span>${esc(channelOptionLabel(c))}</span></button>`).join('');
                }
                function syncActionRelayActionsDraftFromForm(deviceId,indexToOpen=null,openMenu=false){
                  const draft=appState.actionRelayActionsEdit;if(!draft||draft.deviceId!==deviceId)return;
                  draft.actions=(draft.actions||[]).map((action,index)=>{const type=String(document.getElementById(`ara-${index}-type`)?.value||action.type||'command').toLowerCase();return {type,value:document.getElementById(`ara-${index}-value`)?.value||'',enabled:!!document.getElementById(`ara-${index}-enabled`)?.checked,requiresOp:!!document.getElementById(`ara-${index}-requires-op`)?.checked,cooldownTicks:Number(document.getElementById(`ara-${index}-cooldown`)?.value||0),notifyOps:!!document.getElementById(`ara-${index}-notify`)?.checked};});
                  if(openMenu&&indexToOpen!==null){draft.channelComboOpen=draft.channelComboOpen||{};draft.channelComboIndex=draft.channelComboIndex||{};draft.channelComboOpen[indexToOpen]=true;draft.channelComboIndex[indexToOpen]=0;setChannelComboQuery(draft,(draft.actions||[])[indexToOpen]?.value||'',indexToOpen);}
                  appState.actionRelayActionsEdit=draft;syncActionRelayChannelCombo(deviceId,indexToOpen);
                }
                function captureActionRelayEditorUiState(){const body=document.querySelector('#wa-modal-root .wa-modal-body'), active=document.activeElement;return {scrollTop:body?body.scrollTop:0,activeId:active&&active.id?active.id:'',selectionStart:active&&typeof active.selectionStart==='number'?active.selectionStart:null,selectionEnd:active&&typeof active.selectionEnd==='number'?active.selectionEnd:null};}
                function restoreActionRelayEditorUiState(state){requestAnimationFrame(()=>{const body=document.querySelector('#wa-modal-root .wa-modal-body');if(body&&state)body.scrollTop=state.scrollTop||0;const active=state&&state.activeId?document.getElementById(state.activeId):null;if(active&&typeof active.focus==='function'){active.focus();if(state.selectionStart!==null&&typeof active.setSelectionRange==='function')active.setSelectionRange(state.selectionStart,state.selectionEnd);}});}
                function rerenderActionRelayActionsEditor(deviceId){const draft=appState.actionRelayActionsEdit;if(!draft||draft.deviceId!==deviceId)return;const uiState=captureActionRelayEditorUiState();if(appState.deviceConfigEdit&&appState.deviceConfigEdit.deviceId===deviceId){applyDeviceConfigDraftsFromForm(deviceId);showDeviceConfigEditModal(deviceId);}else showActionRelayActionsModal(deviceId);restoreActionRelayEditorUiState(uiState);}
                function changeActionRelayActionType(deviceId,index){syncActionRelayActionsDraftFromForm(deviceId);const draft=appState.actionRelayActionsEdit;if(!draft)return;const action=draft.actions[index];if(action){action.type=String(document.getElementById(`ara-${index}-type`)?.value||'command').toLowerCase();action.value=action.type==='sound'?'minecraft:entity.experience_orb.pickup':'';}rerenderActionRelayActionsEditor(deviceId);}
                function addActionRelayAction(deviceId,type){syncActionRelayActionsDraftFromForm(deviceId);const draft=appState.actionRelayActionsEdit;if(!draft||!draft.lockId)return;draft.actions.push(normalizeActionRelayDraftAction({type,value:type==='sound'?'minecraft:entity.experience_orb.pickup':''}));rerenderActionRelayActionsEditor(deviceId);}
                function requestDeleteActionRelayAction(deviceId,index){syncActionRelayActionsDraftFromForm(deviceId);const draft=appState.actionRelayActionsEdit;if(!draft||!draft.lockId)return;draft.pendingDeleteIndex=index;rerenderActionRelayActionsEditor(deviceId);}
                function cancelDeleteActionRelayAction(deviceId){const draft=appState.actionRelayActionsEdit;if(!draft||draft.deviceId!==deviceId)return;draft.pendingDeleteIndex=null;rerenderActionRelayActionsEditor(deviceId);}
                function confirmDeleteActionRelayAction(deviceId,index){syncActionRelayActionsDraftFromForm(deviceId);const draft=appState.actionRelayActionsEdit;if(!draft||!draft.lockId)return;if(index<0||index>=draft.actions.length)return;draft.actions.splice(index,1);draft.pendingDeleteIndex=null;rerenderActionRelayActionsEditor(deviceId);}
                function moveActionRelayAction(deviceId,index,delta){syncActionRelayActionsDraftFromForm(deviceId);const draft=appState.actionRelayActionsEdit;if(!draft||!draft.lockId)return;const next=index+delta;if(next<0||next>=draft.actions.length)return;const [item]=draft.actions.splice(index,1);draft.actions.splice(next,0,item);rerenderActionRelayActionsEditor(deviceId);}
                function openActionRelayActionChannelMenu(deviceId,index){const draft=appState.actionRelayActionsEdit;if(!draft||draft.deviceId!==deviceId)return;syncActionRelayActionsDraftFromForm(deviceId);draft.channelComboOpen=draft.channelComboOpen||{};draft.channelComboIndex=draft.channelComboIndex||{};draft.channelComboOpen[index]=true;draft.channelComboIndex[index]=0;resetChannelComboQuery(draft,index);syncActionRelayChannelCombo(deviceId,index);}
                function toggleActionRelayActionChannelMenu(deviceId,index){const draft=appState.actionRelayActionsEdit;if(!draft||draft.deviceId!==deviceId)return;syncActionRelayActionsDraftFromForm(deviceId);draft.channelComboOpen=draft.channelComboOpen||{};draft.channelComboOpen[index]=!draft.channelComboOpen[index];if(draft.channelComboOpen[index])resetChannelComboQuery(draft,index);syncActionRelayChannelCombo(deviceId,index);document.getElementById(`ara-${index}-value`)?.focus();}
                function selectActionRelayActionChannel(deviceId,index,channel){const draft=appState.actionRelayActionsEdit;if(!draft||draft.deviceId!==deviceId)return;syncActionRelayActionsDraftFromForm(deviceId);draft.actions[index].value=channel||'';draft.channelComboOpen=draft.channelComboOpen||{};draft.channelComboIndex=draft.channelComboIndex||{};draft.channelComboOpen[index]=false;draft.channelComboIndex[index]=0;resetChannelComboQuery(draft,index);const input=document.getElementById(`ara-${index}-value`);if(input)input.value=draft.actions[index].value;const hint=document.getElementById(`ara-${index}-channel-hint`);if(hint)hint.innerHTML=channelHintHtml(draft.actions[index].value,draft.channelOptions||appState.channelOptions||[],draft.channelOptionsError||appState.channelOptionsError);syncActionRelayChannelCombo(deviceId,index);}
                function handleActionRelayActionChannelKey(event,deviceId,index){const draft=appState.actionRelayActionsEdit;if(!draft||draft.deviceId!==deviceId)return;draft.channelComboOpen=draft.channelComboOpen||{};draft.channelComboIndex=draft.channelComboIndex||{};const options=filteredChannelOptions(draft.channelOptions||appState.channelOptions||[],channelComboQuery(draft,index));if(event.key==='Escape'){event.preventDefault();event.stopPropagation();draft.channelComboOpen[index]=false;syncActionRelayChannelCombo(deviceId,index);return;}if(event.key==='ArrowDown'||event.key==='ArrowUp'){event.preventDefault();draft.channelComboOpen[index]=true;const max=Math.max(0,options.length-1), next=event.key==='ArrowDown'?Number((draft.channelComboIndex||{})[index]||0)+1:Number((draft.channelComboIndex||{})[index]||0)-1;draft.channelComboIndex[index]=Math.min(max,Math.max(0,next));syncActionRelayChannelCombo(deviceId,index);return;}if(event.key==='Enter'&&draft.channelComboOpen[index]&&options.length>0){event.preventDefault();selectActionRelayActionChannel(deviceId,index,options[Math.min(options.length-1,Number(draft.channelComboIndex[index]||0))].channel);}}
                function syncActionRelayChannelCombo(deviceId,index){const draft=appState.actionRelayActionsEdit;if(!draft||draft.deviceId!==deviceId||index===null||index===undefined)return;const combo=document.getElementById(`ara-${index}-channel-combo`), menu=document.getElementById(`ara-${index}-channel-menu`), input=document.getElementById(`ara-${index}-value`);if(combo)combo.classList.toggle('open',!!(draft.channelComboOpen||{})[index]);if(input)input.setAttribute('aria-expanded',(draft.channelComboOpen||{})[index]?'true':'false');if(menu)menu.innerHTML=actionRelayChannelOptionsHtml(deviceId,index,draft);}
                async function saveActionRelayActions(deviceId){const draft=appState.actionRelayActionsEdit||{deviceId,errors:[]};syncActionRelayActionsDraftFromForm(deviceId);draft.saving=true;draft.errors=[];draft.conflict=null;appState.actionRelayActionsEdit=draft;rerenderActionRelayActionsEditor(deviceId);try{const result=await patchActionRelayActionsDraft(deviceId,draft);if(result.success){markChannelOptionsDirty({type:'action_changed',payload:{targetType:'action_relay_actions'}});appState.actionRelayActionsEdit=null;stopActionRelayActionsLockHeartbeat();appState.modalCloseHandler=null;await dismissWebAdminModal();toast(result.changed?(result.message||'Action 列表已保存。'):'没有变更。');await refreshCurrentDeviceContext(deviceId);return;}draft.saving=false;draft.errors=result.validationErrors&&result.validationErrors.length?result.validationErrors:[{message:result.message||'保存失败'}];draft.conflict=result.code==='conflict_detected'?result.conflict||{remote:true}:null;appState.actionRelayActionsEdit=draft;if(['edit_lock_expired','edit_lock_conflict','edit_lock_required'].includes(result.code))stopActionRelayActionsLockHeartbeat();toast(result.message||'保存失败');rerenderActionRelayActionsEditor(deviceId);}catch(err){draft.saving=false;draft.errors=[{message:err.message||'保存失败'}];draft.conflict=null;appState.actionRelayActionsEdit=draft;toast(err.message||'保存失败');rerenderActionRelayActionsEditor(deviceId);}}
                async function patchActionRelayActionsDraft(deviceId,draft){return await api(`/api/webadmin/action-relay-actions/${encodeURIComponent(deviceId)}`,{method:'PATCH',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({deviceId,actions:(draft.actions||[]).map(normalizeActionRelayDraftAction),expectedFingerprint:draft.expectedFingerprint||'',lockId:draft.lockId||''})});}
                async function reloadActionRelayActionsAfterConflict(deviceId){try{await releaseActionRelayActionsLock(appState.actionRelayActionsEdit,true);await prepareActionRelayActionsDraft(deviceId,true);rerenderActionRelayActionsEditor(deviceId);}catch(err){toast(err.message||'Action 列表重新加载失败');}}
                function scheduleActionRelayActionsLockHeartbeat(){stopActionRelayActionsLockHeartbeat();appState.actionRelayActionsLockTimer=setTimeout(async()=>{await heartbeatActionRelayActionsLock();if(appState.actionRelayActionsEdit)scheduleActionRelayActionsLockHeartbeat();},30000);}
                function stopActionRelayActionsLockHeartbeat(){if(appState.actionRelayActionsLockTimer){clearTimeout(appState.actionRelayActionsLockTimer);appState.actionRelayActionsLockTimer=null;}}
                async function heartbeatActionRelayActionsLock(){const draft=appState.actionRelayActionsEdit;if(!draft||!draft.lockId)return;try{const result=await api('/api/webadmin/edit-locks/heartbeat',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'action_relay_actions',targetId:draft.deviceId,lockId:draft.lockId})});if(result.success){draft.lock=result.data?.lock||draft.lock;appState.actionRelayActionsEdit=draft;return;}draft.errors=[{message:result.message||'Action 列表编辑锁续期失败'}];appState.actionRelayActionsEdit=draft;stopActionRelayActionsLockHeartbeat();}catch(err){draft.errors=[{message:err.message||'Action 列表编辑锁续期失败'}];appState.actionRelayActionsEdit=draft;stopActionRelayActionsLockHeartbeat();}}
                async function releaseActionRelayActionsLock(draft,silent){if(!draft||!draft.lockId)return;try{await api('/api/webadmin/edit-locks/release',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'action_relay_actions',targetId:draft.deviceId,lockId:draft.lockId})});}catch(err){if(!silent)toast(err.message||'Action 列表编辑锁释放失败，将等待自动过期。');}}
                async function cancelActionRelayActionsEdit(deviceId,silent=false){const draft=appState.actionRelayActionsEdit;if(draft&&draft.deviceId===deviceId)await releaseActionRelayActionsLock(draft,silent);if(draft&&draft.deviceId===deviceId){appState.actionRelayActionsEdit=null;stopActionRelayActionsLockHeartbeat();}}
                """).append("""
                async function releaseAllDeviceConfigLocks(deviceId,silent){
                  const meta=appState.deviceMetadataEdit,basic=appState.deviceBasicConfigEdit,ext=appState.deviceExtendedConfigEdit,actions=appState.actionRelayActionsEdit,native=appState.vbdNativeTriggerEdit,matcher=appState.interactionItemMatcherEdit;
                  if(meta&&meta.deviceId===deviceId)await releaseDeviceMetadataLock(meta,silent);
                  if(basic&&basic.deviceId===deviceId)await releaseDeviceBasicConfigLock(basic,silent);
                  if(ext&&ext.deviceId===deviceId)await releaseDeviceExtendedConfigLock(ext,silent);
                  if(actions&&actions.deviceId===deviceId)await releaseActionRelayActionsLock(actions,silent);
                  if(native&&sameDeviceRef(native.deviceId,deviceId))await releaseVbdNativeTriggerLock(native,silent);
                  if(matcher&&sameDeviceRef(matcher.deviceId,deviceId))await releaseInteractionItemMatcherLock(matcher,silent);
                  if(meta&&meta.deviceId===deviceId){appState.deviceMetadataEdit=null;stopDeviceMetadataLockHeartbeat();}
                  if(basic&&basic.deviceId===deviceId){appState.deviceBasicConfigEdit=null;stopDeviceBasicConfigLockHeartbeat();}
                  if(ext&&ext.deviceId===deviceId){appState.deviceExtendedConfigEdit=null;stopDeviceExtendedConfigLockHeartbeat();}
                  if(actions&&actions.deviceId===deviceId){appState.actionRelayActionsEdit=null;stopActionRelayActionsLockHeartbeat();}
                  if(native&&sameDeviceRef(native.deviceId,deviceId)){appState.vbdNativeTriggerEdit=null;stopVbdNativeTriggerLockHeartbeat();}
                  if(matcher&&sameDeviceRef(matcher.deviceId,deviceId)){appState.interactionItemMatcherEdit=null;stopInteractionItemMatcherLockHeartbeat();}
                }
                async function cancelDeviceConfigEdit(deviceId){
                  await releaseAllDeviceConfigLocks(deviceId,false);
                  appState.deviceConfigEdit=null;await dismissWebAdminModal();await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});
                }
                function maybeReleaseDeviceConfigEditForRoute(hash){
                  const session=appState.deviceConfigEdit;if(!session)return;
                  if(String(hash||'').startsWith('#/devices/')){const info=detailRoute(String(hash).substring('#/devices/'.length),'#/devices');if(sameDeviceRef(info.id,session.deviceId))return;}
                  appState.deviceConfigEdit=null;
                }
                function deviceBasicConfigCard(detail){
                  if(detail.basicConfigError)return errorBlock(detail.basicConfigError.message||'设备基础配置加载失败');
                  const cfg=detail.basicConfig||{}, lock=cfg.lockStatus||{}, editable=canEditDeviceBasicConfig(), editing=appState.deviceBasicConfigEdit&&appState.deviceBasicConfigEdit.deviceId===detail.id, lockedByOther=lock.locked&&!lock.heldByCurrentUser;
                  const supported=cfg.supported!==false;
                  const lockHint=lockedByOther?`<div class="readonly-note">当前由 ${esc(lock.holderUsername||'其他用户')} 正在编辑基础配置，锁到期：${esc(formatDateTime(lock.expiresAt))}</div>`:'';
                  let action='';
                  if(!supported)action=`<div class="readonly-note">${esc(cfg.unsupportedReason||'当前设备类型暂不支持基础配置编辑。')}</div>`;
                  else if(editing)action=`<button class="secondary" onclick='showDeviceBasicConfigEditModal(${jsString(detail.id)})'>继续编辑</button>`;
                  else if(editable&&!lockedByOther)action=`<button class="secondary" onclick='startDeviceBasicConfigEdit(${jsString(detail.id)},${cfg.enabled?'true':'false'},${jsString(cfg.channel||'')},${jsString(cfg.expectedFingerprint||'')})'>编辑基础配置</button>`;
                  else if(editable)action=lockHint;
                  else action='<div class="readonly-note">需要 EDITOR 或 OWNER 权限才能编辑设备基础配置。</div>';
                  return `<div class="identity-grid">${row('启用状态',esc(labelEnabledState(cfg.enabled)))}${row('主频道',channelCell(cfg.channel))}</div><p class="readonly-note">这些配置会影响设备触发与 Signal 分发，修改后会立即应用到当前世界。7.2 仅支持 enabled / 主频道。</p>${action}`;
                }
                function deviceBasicConfigForm(detail,draft){
                  const errors=draft.errors&&draft.errors.length?`<ul class="validation-list">${draft.errors.map(e=>`<li>${esc(e.message||'输入未通过校验')}</li>`).join('')}</ul>`:'';
                  const lock=draft.lock||{}, lockLine=lock.locked?`<div class="readonly-note">正在编辑基础配置 · 锁到期：${esc(formatDateTime(lock.expiresAt))} · 持有人：${esc(lock.holderUsername||appState.me?.username||'当前用户')}</div>`:'<div class="readonly-note">正在获取编辑锁...</div>';
                  const conflict=draft.conflict?`<div class="readonly-note">设备基础配置已被其他操作修改，请刷新后再编辑。<button class="link-button" type="button" onclick='reloadDeviceBasicConfigAfterConflict(${jsString(detail.id)})'>刷新当前配置</button></div>`:'';
                  const channelOptions=draft.channelOptions||appState.channelOptions||[], channelLoadError=draft.channelOptionsError||appState.channelOptionsError;
                  const combo=renderDeviceBasicConfigChannelCombo(detail.id,draft), hint=channelHintHtml(draft.channel,channelOptions,channelLoadError);
                  return `<form class="edit-form" onsubmit='event.preventDefault();saveDeviceBasicConfig(${jsString(detail.id)})'>
                    ${lockLine}
                    <label>启用状态<select id="basic-enabled" class="select" onchange='updateDeviceBasicConfigDraftFromForm(${jsString(detail.id)})'><option value="true" ${draft.enabled?'selected':''}>启用</option><option value="false" ${!draft.enabled?'selected':''}>禁用</option></select></label>
                    <label>主频道${combo}</label>
                    <p class="muted">可选择已有频道，也可以输入新的频道名。新频道不会自动创建消费者。</p>
                    <p id="basic-channel-hint" class="readonly-note">${hint}</p>
                    ${errors}
                    ${conflict}
                    <p class="muted">保存会写入真实设备基础配置，仅限 enabled / 主频道；不会修改 itemSubmit、matcher、action、region、用户或系统设置。7.2 暂不支持清空主频道。</p>
                    <div class="form-actions"><button class="secondary" type="submit">${draft.saving?'保存中...':'保存'}</button><button class="text-button" type="button" onclick='closeWebAdminModal()'>取消</button></div>
                  </form>`;
                }
                function showDeviceBasicConfigEditModal(deviceId){
                  const draft=appState.deviceBasicConfigEdit;if(!draft||draft.deviceId!==deviceId)return;
                  if(appState.deviceConfigEdit&&appState.deviceConfigEdit.deviceId===deviceId){showDeviceConfigEditModal(deviceId);return;}
                  markModalInitialSnapshot('device_basic_config',draft);
                  openWebAdminModal('编辑设备基础配置',deviceBasicConfigForm({id:deviceId},draft),editModalFooter(draft.saving),{onClose:()=>cancelDeviceBasicConfigEdit(deviceId),syncBeforeClose:()=>syncModalDraftBeforeClose('device_basic_config',deviceId),dirtyCheck:()=>modalDraftDirty('device_basic_config',appState.deviceBasicConfigEdit)});
                }
                function deviceExtendedConfigCard(detail){
                  if(detail.extendedConfigError)return errorBlock(detail.extendedConfigError.message||'设备扩展配置加载失败');
                  const cfg=detail.extendedConfig||{}, lock=cfg.lockStatus||{}, editable=canEditDeviceExtendedConfig(), editing=appState.deviceExtendedConfigEdit&&appState.deviceExtendedConfigEdit.deviceId===detail.id, lockedByOther=lock.locked&&!lock.heldByCurrentUser, fields=cfg.supportedFields||[], editableFields=cfg.editableFields||fields;
                  if(cfg.supported===false||fields.length===0)return `<p class="muted">${esc(cfg.unsupportedReason||'该设备类型暂无可编辑扩展配置。')}</p>`;
                  const values=cfg.values||{}, labels=cfg.fieldLabels||{};
                  const rows=fields.map(field=>row(labels[field]||field,extendedFieldDisplay(field,values[field]))).join('');
                  const runtimeNote=deviceExtendedRuntimeNote(cfg);
                  const lockHint=lockedByOther?`<div class="readonly-note">当前由 ${esc(lock.holderUsername||'其他用户')} 正在编辑扩展配置，锁到期：${esc(formatDateTime(lock.expiresAt))}</div>`:'';
                  let action='';
                  if(editing)action=`<button class="secondary" onclick='showDeviceExtendedConfigEditModal(${jsString(detail.id)})'>继续编辑</button>`;
                  else if(editable&&!lockedByOther)action=`<button class="secondary" onclick='startDeviceExtendedConfigEdit(${jsString(detail.id)})'>${editableFields.length?'编辑扩展配置':'查看运行参数状态'}</button>`;
                  else if(editable)action=lockHint;
                  else action='<div class="readonly-note">需要 EDITOR 或 OWNER 权限才能编辑设备扩展配置。</div>';
                  return `<div class="identity-grid">${rows}</div>${runtimeNote}<p class="readonly-note">这些配置会影响设备交互、反馈频道或时间参数；不会编辑 itemSubmit、matcher、action、region、用户或系统设置。</p>${action}`;
                }
                function extendedFieldDisplay(field,value){
                  if(isExtendedChannelField(field))return channelCell(value);
                  if(isExtendedTickField(field))return isBlank(value)?'<span class="muted">暂无</span>':esc(formatTicks(value));
                  return isBlank(value)?'<span class="muted">暂无</span>':esc(value);
                }
                function extendedFieldHelp(field){
                  return {
                    interactChannel:'交互触发时使用的扩展频道；可选择已有频道，也可以输入新频道。',
                    successChannel:'交互匹配成功后的反馈频道；可选字段，可明确设为未设置。',
                    failChannel:'交互匹配失败后的反馈频道；可选字段，可明确设为未设置。',
                    interactionCooldownTicks:'虚拟方块交互冷却时间，0 表示无冷却。',
                    pulseTicks:'signal_receiver 输出脉冲时间，必须大于 0。',
                    cooldownTicks:'action_relay 动作冷却时间，0 表示无冷却。'
                  }[field]||'设备扩展配置字段。';
                }
                function deviceExtendedConfigForm(detail,draft){
                  const errors=draft.errors&&draft.errors.length?`<ul class="validation-list">${draft.errors.map(e=>`<li>${esc(e.message||'输入未通过校验')}</li>`).join('')}</ul>`:'';
                  const lock=draft.lock||{}, hasEditable=(draft.editableFields||[]).length>0;
                  const lockLine=draft.lockId?`<div class="readonly-note">正在编辑扩展配置 · 锁到期：${esc(formatDateTime(lock.expiresAt))} · 持有人：${esc(lock.holderUsername||appState.me?.username||'当前用户')}</div>`:(hasEditable?'<div class="readonly-note danger">扩展配置编辑锁未获取，字段暂不可保存。</div>':'<div class="readonly-note">当前类型专属配置以只读快照显示；运行状态见下方状态卡。</div>');
                  const conflict=draft.conflict?`<div class="readonly-note">设备扩展配置已被其他操作修改，请刷新后再编辑。<button class="link-button" type="button" onclick='reloadDeviceExtendedConfigAfterConflict(${jsString(detail.id)})'>刷新当前配置</button></div>`:'';
                  const fields=draft.supportedFields||[], body=fields.length?fields.map(field=>extendedFieldForm(detail.id,field,draft)).join(''):'<p class="muted">该设备类型暂无可编辑扩展配置。</p>';
                  return `<form class="edit-form" onsubmit='event.preventDefault();saveDeviceExtendedConfig(${jsString(detail.id)})'>
                    ${lockLine}
                    ${deviceExtendedRuntimeNote(draft)}
                    ${body}
                    ${errors}
                    ${conflict}
                    <p class="muted">保存会写入真实设备扩展配置；仅限当前设备类型支持的扩展频道或基础时间参数。不会修改 itemSubmit、matcher、interactionItem 复杂条件、action、region、用户或系统设置。</p>
                    <div class="form-actions"><button class="secondary" type="submit" ${draft.lockId?'':'disabled'}>${draft.saving?'保存中...':'保存'}</button><button class="text-button" type="button" onclick='closeWebAdminModal()'>取消</button></div>
                  </form>`;
                }
                function showDeviceExtendedConfigEditModal(deviceId){
                  const draft=appState.deviceExtendedConfigEdit;if(!draft||draft.deviceId!==deviceId)return;
                  if(appState.deviceConfigEdit&&appState.deviceConfigEdit.deviceId===deviceId){showDeviceConfigEditModal(deviceId);return;}
                  markModalInitialSnapshot('device_extended_config',draft);
                  openWebAdminModal(draft.lockId?'编辑设备扩展配置':'查看设备扩展配置状态',deviceExtendedConfigForm({id:deviceId},draft),draft.lockId?editModalFooter(draft.saving):waButton('关闭','close','onclick="closeWebAdminModal()"','ghost'),{onClose:()=>cancelDeviceExtendedConfigEdit(deviceId),syncBeforeClose:()=>syncModalDraftBeforeClose('device_extended_config',deviceId),dirtyCheck:()=>!!appState.deviceExtendedConfigEdit?.lockId&&modalDraftDirty('device_extended_config',appState.deviceExtendedConfigEdit)});
                }
                function extendedReadonlyReason(draft,field){
                  const state=String(draft?.runtimeState||'');
                  if(!draft?.lockId&&state&&state!=='ready'&&state!=='unsupported')return '当前运行状态只读；请查看上方状态卡。';
                  if(!draft?.lockId)return '当前未获取编辑锁，字段暂不可保存。';
                  return (draft.fieldDisabledReasons||{})[field]||draft.unsupportedReason||'当前运行状态下此字段只读。';
                }
                function extendedFieldForm(deviceId,field,draft){
                  const label=(draft.fieldLabels||{})[field]||field, value=(draft.values||{})[field]??'', id=extendedFieldId(field), help=extendedFieldHelp(field);
                  const disabled=extendedFieldEditable(draft,field)?'':'disabled', disabledReason=extendedReadonlyReason(draft,field);
                  if(isExtendedChannelField(field)){
                    const combo=renderDeviceExtendedConfigChannelCombo(deviceId,field,draft), hint=channelHintHtml(value,draft.channelOptions||appState.channelOptions||[],draft.channelOptionsError||appState.channelOptionsError), clearable=(draft.clearableFields||{})[field]===true, checked=(draft.clear||{})[field]?'checked':'';
                    return `<label>${esc(label)}${combo}</label>${clearable?`<label class="readonly-note"><input id="extended-clear-${id}" type="checkbox" ${checked} ${disabled} onchange='updateDeviceExtendedConfigDraftFromForm(${jsString(deviceId)})'> 设为未设置</label>`:''}<p id="extended-${id}-hint" class="readonly-note">${hint}</p>${disabled?`<p class="readonly-note danger">${esc(disabledReason)}</p>`:''}<p class="muted">${esc(help)}</p>`;
                  }
                  const min=field==='pulseTicks'?1:0, max=72000;
                  return `<label>${esc(label)}<input id="extended-${id}" class="input" type="number" min="${min}" max="${max}" step="1" value="${esc(value)}" ${disabled} data-physical-tick-field="${esc(field)}" oninput='updateDeviceExtendedConfigDraftFromForm(${jsString(deviceId)})'></label>${disabled?`<p class="readonly-note danger">${esc(disabledReason)}</p>`:''}<p class="muted">${esc(help)}</p>`;
                }
                function deviceDoctorSideCard(detail,issues){
                  const list=issues||[], status=detail.doctorStatus||detail.debugSummary?.status||(list.length?'WARNING':'OK');
                  const top=list.slice(0,3).map(i=>`<div class="issue-row"><strong>${pill(i.severity)} ${esc(i.title||'诊断问题')}</strong><span class="meta">${esc(i.suggestion||i.message||'暂无建议')}</span></div>`).join('');
                  return `<div class="identity-grid">${row('当前状态',pill(status))}${row('问题数量',esc(list.length))}</div>${top?`<div class="list-stack side-list">${top}</div>`:'<p class="muted">当前没有诊断问题。</p>'}<p><button class="link-button" onclick="location.hash='#/doctor'">查看全局诊断</button></p>`;
                }
                function deviceChannelSideCard(detail){
                  const success=detail.configSummary?.interactionItem?.successChannel, fail=detail.configSummary?.interactionItem?.failChannel;
                  let html=`<div class="identity-grid">${row('主频道',channelCell(detail.channel))}${row('成功频道',channelCell(success))}${row('失败频道',channelCell(fail))}</div>`;
                  html+=isBlank(detail.channel)?'<p class="muted">主频道未设置。</p>':`<p><button class="link-button" ${navigationAttr(signalHash(detail.channel),false)}>查看频道详情 / 逻辑链</button></p>`;
                  return html;
                }
                function deviceIdentitySideCard(detail){
                  const cfg=detail.configSummary||{}, rows=[
                    ['短 ID',esc(shortId(detail.id))],
                    ['设备 ID',esc(detail.id)],
                    ['世界',esc(detail.world||'暂无')],
                    ['坐标',esc(posText(detail.pos))],
                    ['当前方块 ID',esc(cfg.blockId||'')],
                    ['绑定方块 ID',esc(cfg.boundBlockId||cfg.expectedBlockId||'')]
                  ].filter(([,v])=>!isBlank(v));
                  return `<div class="identity-grid">${rows.map(([k,v])=>row(k,v)).join('')}</div>`;
                }
                function deviceQuickLinksCard(detail,routeInfo){
                  const historyLink=isBlank(detail.channel)?'<span class="muted">暂无相关 History</span>':`<button class="link-button" ${navigationAttr(historyHash(detail.channel),false)}>查看相关 History</button>`;
                  const signalLink=isBlank(detail.channel)?'<span class="muted">暂无相关 Signal 频道</span>':`<button class="link-button" ${navigationAttr(signalHash(detail.channel),false)}>查看相关 Signal 频道</button>`;
                  return `<div class="side-actions">${historyLink}${signalLink}<button class="link-button" onclick="location.hash='#/doctor'">查看全局 Doctor</button><button class="link-button" onclick="location.hash='#/devices'">返回设备列表</button></div>`;
                }
                function deviceRecentStatusCard(detail,historyItems){
                  const latest=(historyItems||[])[0];
                  if(isBlank(detail.lastTriggeredAt)&&!latest)return '<p class="muted">暂无最近状态。</p>';
                  const rows=[
                    ['最近触发',fmtTime(detail.lastTriggeredAt||latest?.time)],
                    ['最近结果',latest?esc(labelStatus(latest.result||'UNKNOWN')):''],
                    ['最近玩家',latest?esc(latest.playerName||'暂无玩家上下文'):''],
                    ['最近来源',latest?esc(latest.sourceName||labelSourceType(latest.sourceType)):'']
                  ].filter(([,v])=>!isBlank(v));
                  return `<div class="identity-grid">${rows.map(([k,v])=>row(k,v)).join('')}</div>`;
                }
                function currentDeviceRouteArg(deviceId){const h=currentRouteHash();return h.startsWith('#/devices/')?h.substring('#/devices/'.length):deviceId;}
                async function refreshCurrentDeviceContext(deviceId){const h=currentRouteHash();if(h.startsWith('#/devices/')){await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});return;}if(h==='#/devices'){await renderDevices({silent:true});return;}if(h==='#/actions'){await renderActionsPage({silent:true});return;}if(h==='#/signals'||h==='#/signalbridge'){await renderSignals({silent:true});return;}if(h.startsWith('#/signals/')){await renderSignalDetail(h.substring('#/signals/'.length),{silent:true});return;}await route({silent:true});}
                function maybeReleaseDeviceMetadataEditForRoute(hash){const draft=appState.deviceMetadataEdit;if(!draft)return;if(String(hash||'').startsWith('#/devices/')){const info=detailRoute(String(hash).substring('#/devices/'.length),'#/devices');if(sameDeviceRef(info.id,draft.deviceId))return;}releaseDeviceMetadataLock(draft,true);appState.deviceMetadataEdit=null;stopDeviceMetadataLockHeartbeat();}
                async function startDeviceMetadataEdit(deviceId,displayName,note,iconKey,expectedVersion){
                  if(!canEditDeviceMetadata())return;
                  try{
                    const result=await api('/api/webadmin/edit-locks/acquire',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'device_metadata',targetId:deviceId})});
                    if(!result.success){toast(result.message||'无法获取编辑锁');await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});return;}
                    const lock=result.data?.lock||{};
                    appState.deviceMetadataEdit={deviceId,displayName,note,iconKey:iconKey||'auto',expectedVersion:Number(expectedVersion||0),lockId:lock.lockId||'',lock,errors:[],saving:false,conflict:null};
                    markModalInitialSnapshot('device_metadata',appState.deviceMetadataEdit);
                    scheduleDeviceMetadataLockHeartbeat();
                    await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});
                    showDeviceMetadataEditModal(deviceId);
                  }catch(err){toast(err.message||'无法获取编辑锁');}
                }
                async function cancelDeviceMetadataEdit(deviceId){const draft=appState.deviceMetadataEdit;if(draft&&draft.deviceId===deviceId){await releaseDeviceMetadataLock(draft,false);appState.deviceMetadataEdit=null;stopDeviceMetadataLockHeartbeat();}await dismissWebAdminModal();await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});}
                async function reloadDeviceMetadataAfterConflict(deviceId){const draft=appState.deviceMetadataEdit;if(draft&&draft.deviceId===deviceId)await releaseDeviceMetadataLock(draft,true);appState.deviceMetadataEdit=null;stopDeviceMetadataLockHeartbeat();await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});}
                function scheduleDeviceMetadataLockHeartbeat(){stopDeviceMetadataLockHeartbeat();appState.deviceMetadataLockTimer=setTimeout(async()=>{await heartbeatDeviceMetadataLock();if(appState.deviceMetadataEdit)scheduleDeviceMetadataLockHeartbeat();},30000);}
                function stopDeviceMetadataLockHeartbeat(){if(appState.deviceMetadataLockTimer){clearTimeout(appState.deviceMetadataLockTimer);appState.deviceMetadataLockTimer=null;}}
                async function heartbeatDeviceMetadataLock(){const draft=appState.deviceMetadataEdit;if(!draft||!draft.lockId)return;try{const result=await api('/api/webadmin/edit-locks/heartbeat',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'device_metadata',targetId:draft.deviceId,lockId:draft.lockId})});if(result.success){draft.lock=result.data?.lock||draft.lock;appState.deviceMetadataEdit=draft;return;}draft.errors=[{message:result.message||'编辑锁续期失败'}];appState.deviceMetadataEdit=draft;stopDeviceMetadataLockHeartbeat();await renderDeviceDetail(currentDeviceRouteArg(draft.deviceId),{silent:true});}catch(err){draft.errors=[{message:err.message||'编辑锁续期失败'}];appState.deviceMetadataEdit=draft;stopDeviceMetadataLockHeartbeat();}}
                async function releaseDeviceMetadataLock(draft,silent){if(!draft||!draft.lockId)return;try{await api('/api/webadmin/edit-locks/release',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'device_metadata',targetId:draft.deviceId,lockId:draft.lockId})});}catch(err){if(!silent)toast(err.message||'编辑锁释放失败，将等待自动过期。');}}
                async function saveDeviceMetadata(deviceId){
                  const draft=appState.deviceMetadataEdit||{deviceId};
                  draft.displayName=document.getElementById('metadata-display-name')?.value||'';
                  draft.note=document.getElementById('metadata-note')?.value||'';
                  draft.iconKey=document.getElementById('metadata-icon')?.value||'auto';
                  draft.saving=true;draft.errors=[];draft.conflict=null;appState.deviceMetadataEdit=draft;renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});
                  try{
                    const result=await api(`/api/webadmin/device-metadata/${encodeURIComponent(deviceId)}`,{method:'PATCH',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({displayName:draft.displayName,note:draft.note,iconKey:draft.iconKey,expectedVersion:draft.expectedVersion,lockId:draft.lockId})});
                    if(result.success){appState.deviceMetadataEdit=null;stopDeviceMetadataLockHeartbeat();await dismissWebAdminModal();toast(result.changed?(result.message||'WebAdmin 显示信息已保存。'):'没有变更。');await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});return;}
                    draft.saving=false;draft.errors=result.validationErrors&&result.validationErrors.length?result.validationErrors:[{message:result.message||'保存失败'}];draft.conflict=result.conflict||null;appState.deviceMetadataEdit=draft;if(['edit_lock_expired','edit_lock_conflict','edit_lock_required'].includes(result.code))stopDeviceMetadataLockHeartbeat();toast(result.message||'保存失败');await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});showDeviceMetadataEditModal(deviceId);
                  }catch(err){
                    draft.saving=false;draft.errors=[{message:err.message||'保存失败'}];appState.deviceMetadataEdit=draft;toast(err.message||'保存失败');await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});showDeviceMetadataEditModal(deviceId);
                  }
                }
                """).append("""
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
                    ['物品模板',item.templateSummary]
                  ]);
                  html+=configGroup('容器配置',[
                    ['容器事件',cfg.containerEnabled?'已启用':''],
                    ['物品条件数量',cfg.itemConditionCount]
                  ]);
                  html+=configGroup('物品提交',[
                    ['多物品提交',cfg.itemSubmitEnabled?'已启用':''],
                    ['提交条件数量',cfg.itemSubmitRequirementCount]
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
                          ${doctorFilterSelect('对象类型','doctor-object',['ALL','DEVICE','CHANNEL','LISTENER','RECEIVER','ACTION_RELAY','ACTION','REGION','SYSTEM','UNKNOWN'],appState.doctorFilters.objectType)}
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
                function doctorOptionLabel(v){return {ALL:'全部',ERROR:'错误',WARNING:'警告',INFO:'信息',DEVICE:'设备',CHANNEL:'频道',LISTENER:'监听器',RECEIVER:'接收器',ACTION_RELAY:'动作继电器',ACTION:'动作',REGION:'区域',SYSTEM:'系统',UNKNOWN:'未知',HAS_TARGET:'有跳转目标',NO_TARGET:'无跳转目标'}[v]||v;}
                function bindDoctorFilters(focusId){
                  const update=(event)=>{appState.doctorFilters.search=document.getElementById('doctor-search').value;appState.doctorFilters.severity=document.getElementById('doctor-severity').value;appState.doctorFilters.objectType=document.getElementById('doctor-object').value;appState.doctorFilters.jump=document.getElementById('doctor-jump').value;renderDoctorList(event.target.id);};
                  ['doctor-search','doctor-severity','doctor-object','doctor-jump'].forEach(id=>document.getElementById(id).addEventListener(id==='doctor-search'?'input':'change',update));
                  if(focusId){const el=document.getElementById(focusId);if(el){el.focus();if(el.setSelectionRange&&el.value){el.setSelectionRange(el.value.length,el.value.length);}}}
                }
                function filterDoctorIssues(items){const f=appState.doctorFilters;return (items||[]).filter(i=>{const hay=[i.title,i.message,i.relatedObjectName,i.relatedObjectId,i.channel,i.suggestion,i.code,i.id].join(' ').toLowerCase();if(f.search&&!hay.includes(f.search.toLowerCase()))return false;if(f.severity!=='ALL'&&String(i.severity||'').toUpperCase()!==f.severity)return false;if(f.objectType!=='ALL'&&String(i.relatedObjectType||'UNKNOWN').toUpperCase()!==f.objectType)return false;const hasTarget=!isBlank(i.navigationTarget)||!isBlank(i.channel);if(f.jump==='HAS_TARGET'&&!hasTarget)return false;if(f.jump==='NO_TARGET'&&hasTarget)return false;return true;});}
                function doctorTable(items){return `<div class="wa-table-scroll"><table class="wa-table"><thead><tr><th>问题类型</th><th>对象</th><th>级别</th><th>描述</th><th>发现时间</th><th>操作</th></tr></thead><tbody>${items.map(i=>`<tr><td><span class="device-name"><span class="device-icon">${icon(doctorIssueIcon(i.severity))}</span><span><strong>${issueTitle(i)}</strong><span class="device-subtitle">${esc(i.id||i.code||'unknown')}</span></span></span></td><td><strong>${esc(i.relatedObjectName||i.relatedObjectId||'暂无')}</strong><span class="device-subtitle">${esc(labelObjectType(i.relatedObjectType))}${isBlank(i.channel)?'':` · ${esc(i.channel)}`}</span></td><td>${pill(i.severity)}</td><td><span>${issueMessage(i)}</span><span class="device-subtitle">${issueSuggestion(i)}</span></td><td>${fmtTime(i.detectedAt||i.createdAt||'')}</td><td><div class="wa-action-cell">${issueNavigation(i)}<button class="wa-btn ghost" disabled>自动修复</button>${waIconButton('导出不可用','download','disabled')}</div></td></tr>`).join('')}</tbody></table></div>`;}
                function doctorSummaryPanel(issues,filtered){if(!issues||issues.length===0)return `<aside class="wa-right-rail"><article class="wa-panel"><h2>问题分布</h2>${empty('当前没有诊断问题。')}</article><article class="wa-panel"><h2>快速操作</h2><div class="wa-quick-grid">${waButton('自动修复','critical-issue','disabled','danger')}${waButton('清空问题','channel-error','disabled','ghost')}${waButton('导出报告','download','disabled','ghost')}</div><p class="wa-disabled-note">Doctor 自动修复、清空与报告导出没有完整后端支持，本轮保持禁用。</p></article></aside>`;const current=filtered||issues, jumpTargets=issues.filter(i=>!isBlank(i.navigationTarget)||!isBlank(i.channel)).length;return `<aside class="wa-right-rail"><article class="wa-panel"><h2>问题分布</h2>${progressList(distributionItems(current,i=>String(i.severity||'UNKNOWN').toUpperCase(),labelStatus,Math.max(1,current.length)))}</article><article class="wa-panel"><h2>范围筛选</h2>${progressList(distributionItems(current,i=>String(i.relatedObjectType||'UNKNOWN').toUpperCase(),labelObjectType,Math.max(1,current.length)))}</article><article class="wa-panel"><h2>快速操作</h2><div class="wa-quick-grid"><button class="wa-btn ghost" onclick="appState.doctorFilters.severity='ERROR';renderDoctorList()">${icon('critical-issue')}<span>仅错误</span></button><button class="wa-btn ghost" onclick="appState.doctorFilters.jump='HAS_TARGET';renderDoctorList()">${icon('signalbridge-main')}<span>有跳转目标</span></button>${waButton('自动修复','critical-issue','disabled','danger')}${waButton('导出报告','download','disabled','ghost')}</div><p class="wa-disabled-note">可跳转问题 ${esc(jumpTargets)} 个。自动修复、清空问题和导出报告均未接入完整后端能力，不发送写请求。</p></article><article class="wa-panel"><h2>最近问题</h2><div class="list-stack">${issues.slice(0,5).map(i=>`<div class="event-row"><strong>${pill(i.severity)} ${issueTitle(i)}</strong><span class="meta">${esc(issueContext(i))}</span><span>${issueSuggestion(i)}</span></div>`).join('')}</div></article></aside>`;}
                function doctorIssueIcon(severity){const s=String(severity||'').toUpperCase();return s==='ERROR'?'doctor-error':(s==='WARNING'?'doctor-warning':'doctor-ok');}
                """).append("""
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
                function cleanActionSummary(value){const text=String(value||'').trim();return text.replace(/^command:\\s*/i,'命令：').replace(/^message:\\s*/i,'消息：').replace(/^sound:\\s*/i,'音效：').replace(/^signal:\\s*/i,'下游频道：').replace(/^unknown:\\s*/i,'未知：')||'暂无摘要';}
                function ownerLink(action){const ownerType=String(action?.ownerType||action?.owner?.ownerType||'').toUpperCase(), ownerId=action?.ownerId||action?.owner?.ownerId||'', ownerName=action?.ownerName||action?.owner?.ownerName||ownerId||'暂无';if(ownerType.startsWith('REGION'))return regionButton(ownerId,ownerName);if(ownerType==='ACTION_RELAY'||ownerType==='DEVICE')return navigationButton(`device:${ownerId}`,ownerName);return esc(ownerName);}
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
                function actionOptionLabel(v){return {ALL:'全部',COMMAND:'命令动作',MESSAGE:'消息动作',SOUND:'音效动作',SIGNAL:'信号动作',UNKNOWN:'未执行 / 未知',OK:'正常',INFO:'信息',WARNING:'警告',ERROR:'错误',SUCCESS:'成功',FAILED:'失败',LISTENER:'监听器',ACTION_RELAY:'动作继电器',REGION_ENTER:'区域进入动作',REGION_EXIT:'区域离开动作',REGION_STAY:'区域停留动作',REGION:'区域',DEVICE:'设备',SYSTEM:'系统',NAME:'动作名',TYPE:'动作类型',OWNER:'归属对象',RECENT:'最近执行'}[v]||v;}
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
                    ${detailHeader({back:backButton(routeInfo,'返回动作列表'),kicker:'动作详情',iconName:actionIcon(type),title:title,subtitle:`${labelActionType(type)} · ${labelOwnerType(ownerType)}`,copyValue:detail.id,badges:[`<span class="pill">${esc(labelActionType(type))}</span>`,pill(entry.doctorStatus||'UNKNOWN'),pill(entry.lastResult||'UNKNOWN')],actions:[waButton('编辑动作','settings','disabled','ghost'),waButton('测试执行','play','disabled','ghost'),waButton('更多','more','disabled','ghost')]})}
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
                      detailCard('快捷操作',`<div class="wa-quick-grid">${waButton('编辑动作','settings','disabled','ghost')}${waButton('动作链编辑','action-binding','disabled','ghost')}${waButton('测试执行','play','disabled','ghost')}${waButton('删除动作','channel-error','disabled','danger')}</div><p class="wa-disabled-note">动作编辑、动作链、测试执行和删除没有完整 WebAdmin 写 API，本轮保持禁用且不发送写请求。</p>`)
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
                async function renderSignals(options={}){
                  if(!options.silent)setView(loading('正在加载 Signal 频道...'));
                  let channels;try{channels=await api('/api/signals/channels')}catch(err){if(options.silent){toast('Signal 频道实时刷新失败，已保留当前页面。');return;}setView(errorBlock(err.message));return;}
                  storeSignalChannelOptions(channels);
                  appState.signals=channels||[];
                  renderSignalList('',options);
                }
                function renderSignalList(focusId,options={}){
                  const channels=appState.signals||[], filtered=filterSignalChannels(channels);
                  const hasConsumers=channels.filter(c=>consumerCount(c)>0).length;
                  const recent=channels.filter(c=>!isBlank(c.lastTriggeredAt)).length;
                  const warning=channels.filter(c=>['WARNING','ERROR'].includes(String(c.doctorStatus||'').toUpperCase())).length;
                  if(setView(`
                    <div class="page-head"><div><h1>Signal 管理</h1><p>查看频道、消费者、最近触发与逻辑链入口</p></div><span class="pill info">只读模式</span></div>
                    <section class="card-grid">
                      ${metric('频道总数',channels.length,'','signal')}
                      ${metric('有消费者频道',hasConsumers,'','receiver')}
                      ${metric('无消费者频道',channels.length-hasConsumers,(channels.length-hasConsumers)>0?'warning':'','warning')}
                      ${metric('最近触发频道',recent,'','history')}
                      ${metric('最近 Signal 事件',channels.reduce((sum,c)=>sum+Number(c.triggerCountToday||0),0),'','history')}
                      ${metric('Doctor 警告',warning,warning>0?'warning':'','doctor')}
                    </section>
                    <div class="toolbar">
                      <input class="input" id="signal-search" placeholder="搜索频道名" value="${esc(appState.signalFilters.search)}">
                      ${signalFilterSelect('消费者','signal-consumer',['ALL','HAS_CONSUMER','NO_CONSUMER','HAS_LISTENER','HAS_RECEIVER','HAS_RELAY'],appState.signalFilters.consumer)}
                      ${signalFilterSelect('状态','signal-status',['ALL','RECENT','NO_RECENT','WARNING'],appState.signalFilters.status)}
                      ${signalFilterSelect('排序','signal-sort',['RECENT','CHANNEL','CONSUMERS'],appState.signalFilters.sort)}
                    </div>
                    ${filtered.length===0?(channels.length===0?empty('当前暂无 Signal 频道数据。请在游戏内触发 signal 或配置 listener / receiver / action_relay 后刷新。'):empty('没有匹配当前筛选条件的频道。')):signalTable(filtered)}
                    <article class="panel-card" style="margin-top:16px"><h2>预设频道图标说明</h2><p class="muted">6.3 只读阶段按频道状态和类型显示预设 2D 图标，不提供图标编辑或上传。</p></article>
                  `,options))bindSignalFilters(focusId);
                }
                function signalFilterSelect(label,id,options,value){return `<label class="filter-field"><span>${esc(label)}</span><select class="select" id="${id}">${options.map(o=>`<option value="${esc(o)}" ${o===value?'selected':''}>${esc(signalOptionLabel(o))}</option>`).join('')}</select></label>`}
                function signalOptionLabel(v){return {ALL:'全部',HAS_CONSUMER:'有消费者',NO_CONSUMER:'无消费者',HAS_LISTENER:'有监听器',HAS_RECEIVER:'有接收器',HAS_RELAY:'有动作继电器',RECENT:'最近有事件',NO_RECENT:'暂无事件',WARNING:'有警告',CHANNEL:'频道名',CONSUMERS:'消费者数量'}[v]||labelSignalSort(v)||v;}
                function bindSignalFilters(focusId){
                  const update=(event)=>{appState.signalFilters.search=document.getElementById('signal-search').value;appState.signalFilters.consumer=document.getElementById('signal-consumer').value;appState.signalFilters.status=document.getElementById('signal-status').value;appState.signalFilters.sort=document.getElementById('signal-sort').value;renderSignalList(event.target.id);};
                  ['signal-search','signal-consumer','signal-status','signal-sort'].forEach(id=>document.getElementById(id).addEventListener(id==='signal-search'?'input':'change',update));
                  if(focusId){const el=document.getElementById(focusId);if(el){el.focus();if(el.setSelectionRange&&el.value){el.setSelectionRange(el.value.length,el.value.length);}}}
                }
                function filterSignalChannels(items){
                  const f=appState.signalFilters;
                  const filtered=items.filter(c=>{
                    if(f.search && ![c.channel,c.displayName,c.note].join(' ').toLowerCase().includes(f.search.toLowerCase()))return false;
                    if(f.consumer==='HAS_CONSUMER'&&consumerCount(c)===0)return false;
                    if(f.consumer==='NO_CONSUMER'&&consumerCount(c)>0)return false;
                    if(f.consumer==='HAS_LISTENER'&&Number(c.listenerCount||0)===0)return false;
                    if(f.consumer==='HAS_RECEIVER'&&Number(c.receiverCount||0)===0)return false;
                    if(f.consumer==='HAS_RELAY'&&Number(c.actionRelayCount||0)===0)return false;
                    if(f.status==='RECENT'&&isBlank(c.lastTriggeredAt))return false;
                    if(f.status==='NO_RECENT'&&!isBlank(c.lastTriggeredAt))return false;
                    if(f.status==='WARNING'&&!['WARNING','ERROR'].includes(String(c.doctorStatus||'').toUpperCase()))return false;
                    return true;
                  });
                  return filtered.sort((a,b)=>{
                    if(f.sort==='CHANNEL')return String(a.channel||'').localeCompare(String(b.channel||''));
                    if(f.sort==='CONSUMERS')return consumerCount(b)-consumerCount(a);
                    return String(b.lastTriggeredAt||'').localeCompare(String(a.lastTriggeredAt||'')) || String(a.channel||'').localeCompare(String(b.channel||''));
                  });
                }
                function signalTable(items){return `<div class="table-wrap"><table class="data-table"><thead><tr><th>频道</th><th>消费者摘要</th><th>监听器</th><th>接收器</th><th>动作继电器</th><th>最近触发</th><th>最近来源</th><th>诊断</th><th>操作</th></tr></thead><tbody>${items.map(c=>{const target=signalHash(c.channel);return `<tr ${navigationAttr(target,false)}><td><span class="device-name"><span class="device-icon">${icon(c.iconKey||'signal')}</span><span><strong>${esc(c.displayName||c.channel)}</strong><span class="device-subtitle">${esc(c.channel||'未命名频道')}</span>${!isBlank(c.note)?`<span class="device-subtitle">${esc(c.note)}</span>`:''}</span></span></td><td>${consumerSummary(c)}</td><td>${Number(c.listenerCount||0)}</td><td>${Number(c.receiverCount||0)}</td><td>${Number(c.actionRelayCount||0)}</td><td>${fmtTime(c.lastTriggeredAt)}</td><td>${esc(c.sourceCount?`${c.sourceCount} 个来源`:'暂无')}</td><td>${pill(c.doctorStatus)}</td><td><button class="text-button" ${navigationAttr(target)}>查看详情</button></td></tr>`;}).join('')}</tbody></table></div>`}
                function consumerSummary(c){const count=consumerCount(c);if(count===0)return '<span class="muted">暂无消费者</span>';return `<span class="pill info">${count} 个消费者</span>`}
                function channelListenerTable(listeners,detail){
                  const rows=(listeners||[]).slice(0,5);
                  if(rows.length===0)return empty('当前频道暂无监听器。');
                  const more=(listeners||[]).length>rows.length?`<p class="muted">当前只在主列表显示前 ${rows.length} 个监听器；完整 listener 字段可在“完整详情”中查看。</p>`:'';
                  return `${detailTableWrap(`<table class="wa-table wa-detail-table"><thead><tr><th>监听器名称 / ID</th><th>状态</th><th>冷却时间</th><th>动作数量</th><th>最后触发</th><th>操作</th></tr></thead><tbody>${rows.map(listener=>{
                    const id=listener.id||listener.name||'', cfg=listener.basicConfig||{}, enabled=listener.enabled ?? cfg.enabled, cooldown=listener.cooldownTicks ?? cfg.cooldownTicks, actionCount=listener.actionCount ?? (listener.actions||[]).length;
                    return `<tr><td><span class="device-name"><span class="device-icon">${icon('listener-receiver')}</span><span><strong>${esc(listener.name||id||'未命名监听器')}</strong><span class="device-subtitle">${esc(id||'无稳定 ID')}</span></span></span></td><td>${pill(enabled!==false?'OK':'WARNING')} ${esc(enabled!==false?'启用':'停用')}</td><td>${esc(formatTicks(cooldown)||'0 tick')}</td><td>${esc(actionCount)}</td><td>${fmtTime(listener.lastTriggeredAt)}</td><td><div class="wa-action-cell">${id?waButton('查看','eye',navigationAttr(listenerHash(id)),'ghost'):waButton('查看','eye','disabled','ghost')}${id&&canEditSignalListenerBasicConfig()?waIconButton('编辑基础配置','settings',htmlHandler(`startSignalListenerBasicConfigEdit(${jsString(id)},${jsString(detail.channel)})`)):waIconButton('编辑基础配置不可用','settings','disabled')}</div></td></tr>`;
                  }).join('')}</tbody></table>`)}${more}`;
                }
                function actionTypeDistributionPanel(actions){
                  const items=actions||[];
                  if(items.length===0)return empty('暂无动作类型分布。');
                  const counts=countBy(items,a=>String(a.type||'UNKNOWN').toUpperCase());
                  return `<div class="wa-distribution-grid">${Object.entries(counts).sort((a,b)=>b[1]-a[1]).map(([type,count])=>`<div class="wa-distribution-item"><span>${icon(actionIcon(type))}</span><strong>${esc(labelActionType(type))}</strong><small>${esc(count)} 个</small></div>`).join('')}</div>`;
                }
                function channelConsumerDetailPanel(detail){
                  const groups=[
                    ['关联接收器',detail.receivers||[],'consumer-receiver'],
                    ['动作继电器',detail.actionRelays||[],'consumer-relay'],
                    ['信号设备 / 触发源',detail.sources||[],'signal-device'],
                    ['下游频道',detail.downstreamSignals||[],'active-channel']
                  ];
                  const html=groups.map(([label,items,iconName])=>{
                    const list=(items||[]).slice(0,3);
                    const body=list.length?list.map(item=>{
                      const text=typeof item==='string'?item:(item.name||item.id||item.channel||'未命名');
                      const target=typeof item==='string'?signalHash(item):(item.navigationTarget||item.target||'');
                      return `<span>${target?navigationButton(target,text):esc(text)}</span>`;
                    }).join(''):'<span class="muted">暂无</span>';
                    const more=(items||[]).length>list.length?`<small>另有 ${(items||[]).length-list.length} 项在完整详情中查看</small>`:'';
                    return `<div class="wa-compact-row"><strong>${icon(iconName)} ${esc(label)}</strong><span>${body}</span>${more}</div>`;
                  }).join('');
                  return `<div class="wa-compact-list">${html}</div>`;
                }
                async function renderSignalDetail(channel,options={}){
                  const routeInfo=detailRoute(channel,'#/signals'), decoded=routeInfo.id||'';
                  if(!options.silent)setView(loading('正在加载频道详情...'));
                  let detail;try{detail=await api(`/api/signals/channels/${encodeURIComponent(decoded)}`)}catch(err){if(options.silent){toast('频道详情实时刷新失败，已保留当前页面。');return;}setView(`<section class="wa-page"><div class="back-row">${backButton(routeInfo,'返回 Signal 管理')}</div>${waPageHead('详情暂不可用','当前只读接口尚未提供该频道详情或频道没有可读取数据。',waButton('返回列表','signalbridge-main',navigationAttr('#/signals'),'ghost'))}${err.status===404?empty('频道不存在或当前没有可读取数据。'):errorBlock(err.message)}</section>`);return;}
                  const metadataRes=await settle(`/api/webadmin/channel-metadata?channel=${encodeURIComponent(decoded)}`);
                  if(metadataRes.ok)detail.metadata=metadataRes.data;
                  const listenerConfigResults=await Promise.all((detail.listeners||[]).map(l=>{const ref=l.id||l.name||'';return ref?settle(`/api/webadmin/signal-listener-basic-config/${encodeURIComponent(ref)}`):Promise.resolve({ok:false});}));
                  (detail.listeners||[]).forEach((listener,index)=>{const result=listenerConfigResults[index];if(result&&result.ok)listener.basicConfig=result.data;});
                  const channelMeta=detail.metadata||{}, channelTitle=channelMeta.effectiveDisplayName||detail.channel;
                  const stats=detail.stats||{}, regionCount=Number(stats.regionControllerCount||stats.regionCount||0), totalConsumers=Number(stats.listenerCount||0)+Number(stats.receiverCount||0)+Number(stats.actionRelayCount||0)+regionCount;
                  const allActions=[...(detail.actions||[]),...(detail.listeners||[]).flatMap(listener=>listener.actions||[])];
                  const status=(detail.doctorIssues||[]).some(i=>i.severity==='ERROR')?'ERROR':((detail.doctorIssues||[]).length?'WARNING':'OK');
                  const editAction=canEditChannelMetadata()?waButton('编辑基本信息','settings',htmlHandler(`startChannelMetadataEdit(${jsString(detail.channel)})`),'primary'):waButton('编辑基本信息','settings','disabled','ghost');
                  const advancedRows=[
                    ['channel.raw',detail.channel],
                    ['channel.displayName',channelTitle],
                    ['channel.type',labelChannelType(detail.type)],
                    ['channel.status',status],
                    ['stats.lastTriggeredAt',formatDateTime(stats.lastTriggeredAt)],
                    ['stats.totalConsumers',totalConsumers],
                    ['stats.listenerCount',Number(stats.listenerCount||0)],
                    ['stats.receiverCount',Number(stats.receiverCount||0)]
                  ];
                  setView(`<section class="wa-page wa-detail-shell" data-detail-kind="signal">
                    ${detailHeader({back:backButton(routeInfo,'返回 Signal 管理'),kicker:'SignalBridge / 频道详情',iconName:channelMeta.iconKey||'active-channel',title:`频道详情：${channelTitle}`,subtitle:channelMeta.note||'频道说明未配置，当前展示运行态与消费者关系。',copyValue:detail.channel,badges:[`<span class="pill info">ID: ${esc(detail.channel)}</span>`,pill(status),`<span class="pill">${esc(labelChannelType(detail.type))}</span>`],actions:[waButton('导出频道配置','download','disabled','ghost'),waButton('诊断','doctor-overview',navigationAttr('#/doctor'),'ghost'),waButton('更多','more','disabled','ghost')]})}
                    ${detailTabs(['基本信息','监听器列表','最近事件','触发统计','递归检查'])}
                    <section class="wa-detail-first-row">
                      ${detailCard('基本信息',detailInfoGrid([
                        ['频道名称 Channel',detail.channel],
                        ['显示名称',channelTitle],
                        ['状态',safeHtml(pill(status))],
                        ['创建时间',channelMeta.createdAt||'暂无'],
                        ['最后修改',channelMeta.updatedAt?`${formatDateTime(channelMeta.updatedAt)} · ${channelMeta.updatedBy||'未知用户'}`:'暂无'],
                        ['描述',channelMeta.note||'暂无描述']
                      ]),editAction)}
                      ${detailCard('状态与统计',`${detailStatGrid([
                        {label:'今日触发次数',value:stats.triggerCountToday ?? 0,sub:'today',icon:'today-trigger'},
                        {label:'最后触发时间',value:formatDateTime(stats.lastTriggeredAt),sub:'last triggered',icon:'recent-event'},
                        {label:'总触发次数',value:stats.totalTriggerCount ?? stats.triggerCountTotal ?? 0,sub:'total',icon:'history'},
                        {label:'监听器数量',value:Number(stats.listenerCount||0),sub:'listeners',icon:'consumer-listener'}
                      ])}<h3 class="wa-detail-subhead">消费者关系</h3>${detailConsumerGrid([
                        {label:'关联接收器',value:Number(stats.receiverCount||0),icon:'consumer-receiver',target:'#/receivers'},
                        {label:'关联动作继电器',value:Number(stats.actionRelayCount||0),icon:'consumer-relay',target:'#/devices'},
                        {label:'关联信号设备',value:Number(stats.sourceDeviceCount||stats.sourceCount||0),icon:'signal-device',target:'#/devices'},
                        {label:'关联区域控制器',value:regionCount,icon:'region-controller',target:'#/regions'}
                      ])}`)}
                    </section>
                    ${detailFixedLayout([
                      detailCard(`监听器列表（共 ${(detail.listeners||[]).length} 个）`,channelListenerTable(detail.listeners||[],detail),'','detail-card-stretchable'),
                      detailCard('消费者关系明细',channelConsumerDetailPanel(detail))
                    ],[
                      detailCard('最近事件',`${compactEventList(detail.recentHistory||[],'暂无最近 Signal 事件。')}<p class="muted"><button class="link-button" ${navigationAttr(historyHash(detail.channel),false)}>查看该频道历史</button></p>`),
                      detailCard('动作类型分布',actionTypeDistributionPanel(allActions)),
                      detailCard('频道诊断',`${doctorList(detail.doctorIssues||[],6)}<p class="muted"><button class="link-button" onclick="location.hash='#/doctor'">查看全局诊断</button></p>`)
                    ],[
                      advancedDetailCard('signals',detail.channel,advancedRows,[
                      {title:'metadata / raw key',rows:advancedRowsFromObject(channelMeta,'metadata')},
                      {title:'history summary / diagnostic summary',rows:advancedRowsFromObject({stats,doctorIssues:detail.doctorIssues,recentHistory:detail.recentHistory},'runtime')},
                      {title:'消费者与下游摘要',rows:advancedRowsFromObject({sources:detail.sources,listeners:detail.listeners,receivers:detail.receivers,actionRelays:detail.actionRelays,actions:detail.actions,downstreamSignals:detail.downstreamSignals},'relations')}
                    ])
                    ])}
                  </section>`,options);
                }
                function logicChain(detail){
                  const sources=detail.sources||[], listeners=detail.listeners||[], receivers=detail.receivers||[], relays=detail.actionRelays||[], actions=detail.actions||[], downstream=detail.downstreamSignals||[];
                  return `<div class="wa-flow-chain">
                    <div class="wa-flow-node"><span class="wa-icon-bubble">${icon('signal-device')}</span><strong>触发源</strong>${endpointCompact(sources,'暂无可推断触发源')}</div>
                    <div class="wa-flow-arrow">→</div>
                    <div class="wa-flow-node"><span class="wa-icon-bubble">${icon('active-channel')}</span><strong>${esc((detail.metadata&&detail.metadata.effectiveDisplayName)||detail.channel)}</strong><span class="muted">${esc(detail.channel)}</span><small>${esc(labelChannelType(detail.type))}</small>${pill((detail.doctorIssues||[]).length?'WARNING':'OK')}</div>
                    <div class="wa-flow-arrow">→</div>
                    <div class="wa-flow-node"><span class="wa-icon-bubble">${icon('listener-receiver')}</span><strong>消费者</strong>${endpointCompact([...listeners,...receivers,...relays],'暂无消费者')}</div>
                    <div class="wa-flow-arrow">→</div>
                    <div class="wa-flow-node"><span class="wa-icon-bubble">${icon('action-overview')}</span><strong>动作 / 下游影响</strong>${actions.length?actions.slice(0,4).map(a=>`<span>${actionButton(a.id,labelActionType(a.type))}：${esc(cleanActionSummary(a.summary||a.name||'-'))}</span>`).join(''):(downstream.length?downstream.map(c=>`<span>下游频道：${esc(c)}</span>`).join(''):'<span class="muted">暂无可用动作详情</span>')}</div>
                  </div>`;
                }
                function endpointCompact(items,emptyText){if(!items||items.length===0)return `<span class="muted">${esc(emptyText)}</span>`;return items.slice(0,4).map(e=>`<span>${navigationButton(e.navigationTarget,e.name||e.id)} <span class="muted">(${esc(labelEndpointType(e.type))})</span></span>`).join('');}
                """).append("""
                function signalListenerFieldId(ref){return String(ref||'').replace(/[^a-zA-Z0-9_-]/g,'_');}
                function signalListenerBasicConfigCard(e,detail){
                  const ref=e.id||e.name||'', draft=appState.signalListenerBasicConfigEdit, isEditing=draft&&draft.listenerRef===ref;
                  const cfg=e.basicConfig||{}, lock=cfg.lockStatus||{}, canEdit=canEditSignalListenerBasicConfig(), lockedByOther=!!lock.locked&&!lock.heldByCurrentUser;
                  const enabled=cfg.listenerRef?cfg.enabled:e.enabled, channel=cfg.listenerRef?cfg.channel:e.channel, cooldown=cfg.listenerRef?cfg.cooldownTicks:e.cooldownTicks, actionCount=cfg.listenerRef?cfg.actionCount:e.actionCount;
                  const lockHint=lockedByOther?`<div class="readonly-note">${esc(lock.holderUsername||'其他用户')} 正在编辑，锁到期：${esc(formatDateTime(lock.expiresAt))}</div>`:'';
                  const action=isEditing?`<button class="secondary" type="button" ${htmlHandler(`showSignalListenerBasicConfigEditModal(${jsString(ref)},${jsString(detail.channel)})`)}>继续编辑</button>`:(canEdit&&!lockedByOther?`<button class="secondary" type="button" ${htmlHandler(`startSignalListenerBasicConfigEdit(${jsString(ref)},${jsString(detail.channel)})`)}>编辑基础配置</button>`:(canEdit?lockHint:'<span class="muted">需要 EDITOR 或 OWNER 权限才能编辑。</span>'));
                  return `<div class="readonly-note"><div class="kv-row"><span class="muted">基础配置</span><strong>${enabled?'启用':'禁用'} / ${esc(channel||detail.channel||'未设置')} / 冷却 ${esc(cooldown ?? 0)} tick</strong></div><div class="kv-row"><span class="muted">Action</span><strong>${esc(actionCount ?? 0)} 个（只读）</strong></div>${action}</div>`;
                }
                function listenerChannelComboOptionsHtml(ref,draft){
                  if(draft.channelOptionsError||appState.channelOptionsError)return '<div class="channel-combo-empty">频道候选加载失败，仍可手动输入新的频道名。</div>';
                  const options=filteredChannelOptions(draft.channelOptions||appState.channelOptions||[],channelComboQuery(draft)), current=normalizeChannelName(draft.channel).toLowerCase(), active=Math.max(0,Number(draft.channelComboIndex||0));
                  if(options.length===0)return '<div class="channel-combo-empty">没有匹配的已有频道，可直接保存为新频道</div>';
                  return options.map((c,index)=>`<button type="button" class="channel-combo-option ${index===active?'active':''} ${String(c.channel||'').trim().toLowerCase()===current?'selected':''}" role="option" onmousedown="event.preventDefault()" ${htmlHandler(`selectSignalListenerBasicConfigChannel(${jsString(ref)},${jsString(c.channel||'')})`)}><strong>${esc(c.channel||'未命名频道')}</strong><span>${esc(channelOptionLabel(c))}</span></button>`).join('');
                }
                function renderSignalListenerConfigChannelCombo(ref,draft){
                  const id=signalListenerFieldId(ref), open=draft.channelComboOpen?' open':'';
                  return `<div id="listener-channel-combo-${id}" class="channel-combo listener-channel-combo${open}"><div class="channel-combo-control"><input id="listener-channel-${id}" class="input" maxlength="128" value="${esc(draft.channel||'')}" placeholder="选择已有频道或输入新频道" autocomplete="off" role="combobox" aria-expanded="${draft.channelComboOpen?'true':'false'}" aria-controls="listener-channel-menu-${id}" ${htmlEvent('onfocus',`openSignalListenerBasicConfigChannelMenu(${jsString(ref)})`)} ${htmlEvent('oninput',`updateSignalListenerBasicConfigDraftFromForm(${jsString(ref)},true)`)} ${htmlEvent('onkeydown',`handleSignalListenerBasicConfigChannelKey(event,${jsString(ref)})`)}><button class="channel-combo-toggle" type="button" ${htmlHandler(`toggleSignalListenerBasicConfigChannelMenu(${jsString(ref)})`)} aria-label="显示已有频道">⌄</button></div><div id="listener-channel-menu-${id}" class="channel-combo-menu" role="listbox">${listenerChannelComboOptionsHtml(ref,draft)}</div></div>`;
                }
                function signalListenerBasicConfigForm(e,detail,draft){
                  const id=signalListenerFieldId(draft.listenerRef), errs=draft.errors?.length?`<ul class="validation-list">${draft.errors.map(err=>`<li>${esc(err.field?`${err.field}：`: '')}${esc(err.message||'保存失败')}</li>`).join('')}</ul>`:'';
                  const conflict=draft.conflict?`<div class="readonly-note">${esc(draft.errors?.[0]?.message||'Listener 基础配置已发生冲突，请刷新后再编辑。')} <button class="link-button" ${htmlHandler(`reloadSignalListenerBasicConfigAfterConflict(${jsString(draft.listenerRef)},${jsString(draft.routeChannel||detail.channel)})`)}>刷新当前信息</button></div>`:'';
                  return `<form class="edit-form" ${htmlEvent('onsubmit',`event.preventDefault();saveSignalListenerBasicConfig(${jsString(draft.listenerRef)},${jsString(draft.routeChannel||detail.channel)})`)}><label>启用状态<select id="listener-enabled-${id}" class="select"><option value="true" ${draft.enabled?'selected':''}>启用</option><option value="false" ${!draft.enabled?'selected':''}>禁用</option></select></label><label>频道${renderSignalListenerConfigChannelCombo(draft.listenerRef,draft)}<span id="listener-channel-hint-${id}" class="muted">${channelHintHtml(draft.channel,draft.channelOptions||appState.channelOptions||[],draft.channelOptionsError||appState.channelOptionsError)}</span></label><label>冷却时间（ticks）<input id="listener-cooldown-${id}" class="input" type="number" min="0" max="72000" step="1" value="${esc(draft.cooldownTicks ?? 0)}"></label><p class="readonly-note">正在编辑 Signal Listener 基础配置。Action 列表保持只读，不会被修改。锁到期：${fmtTime(draft.lock?.expiresAt)}</p>${errs}${conflict}<div class="form-actions"><button class="primary" type="submit" ${draft.saving?'disabled':''}>${draft.saving?'保存中...':'保存'}</button><button class="secondary" type="button" ${htmlHandler('closeWebAdminModal()')}>取消</button></div></form>`;
                }
                function showSignalListenerBasicConfigEditModal(ref,routeChannel){
                  const draft=appState.signalListenerBasicConfigEdit;if(!draft||draft.listenerRef!==ref)return;
                  markModalInitialSnapshot('signal_listener_basic_config',draft);
                  openWebAdminModal('编辑监听器基础配置',signalListenerBasicConfigForm({id:ref}, {channel:routeChannel||draft.routeChannel||draft.channel||''}, draft),editModalFooter(draft.saving),{onClose:()=>cancelSignalListenerBasicConfigEdit(ref,routeChannel||draft.routeChannel||draft.channel||''),syncBeforeClose:()=>syncModalDraftBeforeClose('signal_listener_basic_config',ref),dirtyCheck:()=>modalDraftDirty('signal_listener_basic_config',appState.signalListenerBasicConfigEdit)});
                }
                function updateSignalListenerBasicConfigDraftFromForm(ref,openMenu=false){
                  const draft=appState.signalListenerBasicConfigEdit;if(!draft||draft.listenerRef!==ref)return;
                  const id=signalListenerFieldId(ref);
                  draft.enabled=(document.getElementById(`listener-enabled-${id}`)?.value||'false')==='true';
                  draft.channel=document.getElementById(`listener-channel-${id}`)?.value||'';
                  draft.cooldownTicks=document.getElementById(`listener-cooldown-${id}`)?.value||'0';
                  if(openMenu){draft.channelComboOpen=true;draft.channelComboIndex=0;setChannelComboQuery(draft,draft.channel);}
                  const hint=document.getElementById(`listener-channel-hint-${id}`);
                  if(hint)hint.innerHTML=channelHintHtml(draft.channel,draft.channelOptions||appState.channelOptions||[],draft.channelOptionsError||appState.channelOptionsError);
                  syncSignalListenerBasicConfigChannelCombo(ref);
                }
                function syncSignalListenerBasicConfigChannelCombo(ref){
                  const draft=appState.signalListenerBasicConfigEdit;if(!draft||draft.listenerRef!==ref)return;
                  const id=signalListenerFieldId(ref), combo=document.getElementById(`listener-channel-combo-${id}`), menu=document.getElementById(`listener-channel-menu-${id}`), input=document.getElementById(`listener-channel-${id}`);
                  if(combo)combo.classList.toggle('open',!!draft.channelComboOpen);
                  if(input)input.setAttribute('aria-expanded',draft.channelComboOpen?'true':'false');
                  if(menu)menu.innerHTML=listenerChannelComboOptionsHtml(ref,draft);
                }
                function openSignalListenerBasicConfigChannelMenu(ref){const draft=appState.signalListenerBasicConfigEdit;if(!draft||draft.listenerRef!==ref)return;updateSignalListenerBasicConfigDraftFromForm(ref,false);draft.channelComboOpen=true;resetChannelComboQuery(draft);syncSignalListenerBasicConfigChannelCombo(ref);}
                function toggleSignalListenerBasicConfigChannelMenu(ref){const draft=appState.signalListenerBasicConfigEdit;if(!draft||draft.listenerRef!==ref)return;updateSignalListenerBasicConfigDraftFromForm(ref,false);draft.channelComboOpen=!draft.channelComboOpen;if(draft.channelComboOpen)resetChannelComboQuery(draft);syncSignalListenerBasicConfigChannelCombo(ref);document.getElementById(`listener-channel-${signalListenerFieldId(ref)}`)?.focus();}
                function selectSignalListenerBasicConfigChannel(ref,channel){const draft=appState.signalListenerBasicConfigEdit;if(!draft||draft.listenerRef!==ref)return;draft.channel=channel||'';draft.channelComboOpen=false;draft.channelComboIndex=0;resetChannelComboQuery(draft);const id=signalListenerFieldId(ref), input=document.getElementById(`listener-channel-${id}`), hint=document.getElementById(`listener-channel-hint-${id}`);if(input)input.value=draft.channel;if(hint)hint.innerHTML=channelHintHtml(draft.channel,draft.channelOptions||appState.channelOptions||[],draft.channelOptionsError||appState.channelOptionsError);syncSignalListenerBasicConfigChannelCombo(ref);}
                function handleSignalListenerBasicConfigChannelKey(event,ref){
                  const draft=appState.signalListenerBasicConfigEdit;if(!draft||draft.listenerRef!==ref)return;
                  const options=filteredChannelOptions(draft.channelOptions||appState.channelOptions||[],channelComboQuery(draft));
                  if(event.key==='Escape'){event.preventDefault();event.stopPropagation();draft.channelComboOpen=false;syncSignalListenerBasicConfigChannelCombo(ref);return;}
                  if(event.key==='ArrowDown'||event.key==='ArrowUp'){event.preventDefault();draft.channelComboOpen=true;const max=Math.max(0,options.length-1), next=event.key==='ArrowDown'?Number(draft.channelComboIndex||0)+1:Number(draft.channelComboIndex||0)-1;draft.channelComboIndex=Math.min(max,Math.max(0,next));syncSignalListenerBasicConfigChannelCombo(ref);return;}
                  if(event.key==='Enter'&&draft.channelComboOpen&&options.length>0){event.preventDefault();selectSignalListenerBasicConfigChannel(ref,options[Math.min(options.length-1,Number(draft.channelComboIndex||0))].channel);return;}
                }
                function maybeReleaseSignalListenerBasicConfigEditForRoute(hash){const draft=appState.signalListenerBasicConfigEdit;if(!draft)return;const h=String(hash||'');if(h.startsWith('#/signals/')){const info=detailRoute(h.substring('#/signals/'.length),'#/signals');if(info.id===(draft.routeChannel||draft.channel))return;}if(h.startsWith('#/listeners/')){const info=detailRoute(h.substring('#/listeners/'.length),'#/listeners');if(info.id===draft.listenerRef||info.id===draft.listenerId)return;}if(h.startsWith('#/signal-listeners/')){const info=detailRoute(h.substring('#/signal-listeners/'.length),'#/signal-listeners');if(info.id===draft.listenerRef||info.id===draft.listenerId)return;}releaseSignalListenerBasicConfigLock(draft,true);appState.signalListenerBasicConfigEdit=null;stopSignalListenerBasicConfigLockHeartbeat();}
                async function startSignalListenerBasicConfigEdit(ref,routeChannel){
                  if(!canEditSignalListenerBasicConfig())return;
                  try{
                    const cfg=await api(`/api/webadmin/signal-listener-basic-config/${encodeURIComponent(ref)}`);
                    const result=await api('/api/webadmin/edit-locks/acquire',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'signal_listener_basic_config',targetId:cfg.listenerId||cfg.listenerRef||ref})});
                    if(!result.success){toast(result.message||'无法获取编辑锁');await renderSignalDetail(encodeURIComponent(routeChannel||cfg.channel||''),{silent:true});return;}
                    const lock=result.data?.lock||{}, channelOptions=await loadSignalChannelOptions();
                    appState.signalListenerBasicConfigEdit={listenerRef:cfg.listenerRef||ref,listenerId:cfg.listenerId||ref,displayName:cfg.displayName||ref,enabled:!!cfg.enabled,channel:cfg.channel||'',cooldownTicks:cfg.cooldownTicks ?? 0,actionCount:cfg.actionCount||0,actionSummaries:cfg.actionSummaries||[],expectedFingerprint:cfg.expectedFingerprint||'',routeChannel:routeChannel||cfg.channel||'',lockId:lock.lockId||'',lock,channelOptions,channelOptionsError:appState.channelOptionsError,channelComboOpen:false,channelComboIndex:0,channelComboQuery:'',channelComboSearchActive:false,errors:[],saving:false,conflict:null};
                    markModalInitialSnapshot('signal_listener_basic_config',appState.signalListenerBasicConfigEdit);
                    scheduleSignalListenerBasicConfigLockHeartbeat();
                    await route({silent:true});
                    showSignalListenerBasicConfigEditModal(cfg.listenerRef||ref,routeChannel||cfg.channel||'');
                  }catch(err){toast(err.message||'无法获取编辑锁');}
                }
                async function cancelSignalListenerBasicConfigEdit(ref,routeChannel){const draft=appState.signalListenerBasicConfigEdit;if(draft&&draft.listenerRef===ref){await releaseSignalListenerBasicConfigLock(draft,false);appState.signalListenerBasicConfigEdit=null;stopSignalListenerBasicConfigLockHeartbeat();}await dismissWebAdminModal();await route({silent:true});}
                async function reloadSignalListenerBasicConfigAfterConflict(ref,routeChannel){const draft=appState.signalListenerBasicConfigEdit;if(draft&&draft.listenerRef===ref)await releaseSignalListenerBasicConfigLock(draft,true);appState.signalListenerBasicConfigEdit=null;stopSignalListenerBasicConfigLockHeartbeat();await renderSignalDetail(encodeURIComponent(routeChannel||''),{silent:true});}
                function scheduleSignalListenerBasicConfigLockHeartbeat(){stopSignalListenerBasicConfigLockHeartbeat();appState.signalListenerBasicConfigLockTimer=setTimeout(async()=>{await heartbeatSignalListenerBasicConfigLock();if(appState.signalListenerBasicConfigEdit)scheduleSignalListenerBasicConfigLockHeartbeat();},30000);}
                function stopSignalListenerBasicConfigLockHeartbeat(){if(appState.signalListenerBasicConfigLockTimer){clearTimeout(appState.signalListenerBasicConfigLockTimer);appState.signalListenerBasicConfigLockTimer=null;}}
                async function heartbeatSignalListenerBasicConfigLock(){const draft=appState.signalListenerBasicConfigEdit;if(!draft||!draft.lockId)return;try{const result=await api('/api/webadmin/edit-locks/heartbeat',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'signal_listener_basic_config',targetId:draft.listenerId,lockId:draft.lockId})});if(result.success){draft.lock=result.data?.lock||draft.lock;return;}draft.errors=[{message:result.message||'编辑锁续期失败'}];stopSignalListenerBasicConfigLockHeartbeat();await renderSignalDetail(encodeURIComponent(draft.routeChannel||draft.channel||''),{silent:true});}catch(err){draft.errors=[{message:err.message||'编辑锁续期失败'}];stopSignalListenerBasicConfigLockHeartbeat();}}
                async function releaseSignalListenerBasicConfigLock(draft,silent){if(!draft||!draft.lockId)return;try{await api('/api/webadmin/edit-locks/release',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'signal_listener_basic_config',targetId:draft.listenerId,lockId:draft.lockId})});}catch(err){if(!silent)toast(err.message||'编辑锁释放失败，将等待自动过期。');}}
                async function saveSignalListenerBasicConfig(ref,routeChannel){
                  const draft=appState.signalListenerBasicConfigEdit||{listenerRef:ref,routeChannel};
                  updateSignalListenerBasicConfigDraftFromForm(ref,false);
                  draft.saving=true;draft.errors=[];draft.conflict=null;appState.signalListenerBasicConfigEdit=draft;route({silent:true});
                  try{
                    const result=await api(`/api/webadmin/signal-listener-basic-config/${encodeURIComponent(ref)}`,{method:'PATCH',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({enabled:draft.enabled,channel:draft.channel,cooldownTicks:Number(draft.cooldownTicks),expectedFingerprint:draft.expectedFingerprint,lockId:draft.lockId})});
                    if(result.success){markChannelOptionsDirty({type:'signal_listener_config_changed'});appState.signalListenerBasicConfigEdit=null;stopSignalListenerBasicConfigLockHeartbeat();await dismissWebAdminModal();toast(result.changed?(result.message||'Signal Listener 基础配置已保存。'):'没有变更。');await route({silent:true});return;}
                    draft.saving=false;draft.errors=result.validationErrors&&result.validationErrors.length?result.validationErrors:[{message:result.message||'保存失败'}];draft.conflict=result.conflict||null;appState.signalListenerBasicConfigEdit=draft;if(['edit_lock_expired','edit_lock_conflict','edit_lock_required'].includes(result.code))stopSignalListenerBasicConfigLockHeartbeat();toast(result.message||'保存失败');await route({silent:true});showSignalListenerBasicConfigEditModal(ref,routeChannel||draft.routeChannel||draft.channel||'');
                  }catch(err){draft.saving=false;draft.errors=[{message:err.message||'保存失败'}];appState.signalListenerBasicConfigEdit=draft;toast(err.message||'保存失败');await route({silent:true});showSignalListenerBasicConfigEditModal(ref,routeChannel||draft.routeChannel||draft.channel||'');}
                }
                """).append("""
                function endpointGroups(detail){
                  const groups=[['监听器',detail.listeners||[]],['接收器',detail.receivers||[]],['动作继电器',detail.actionRelays||[]]];
                  return `<div class="list-stack">${groups.map(([name,items])=>`<div class="endpoint-row"><strong>${esc(name)}：${items.length}</strong>${items.length?items.map(e=>endpointRow(e,detail)).join(''): '<span class="muted">暂无</span>'}</div>`).join('')}</div>`;
                }
                function endpointRow(e,detail){const base=`<span>${navigationButton(e.navigationTarget,e.name||e.id)} <span class="meta">${esc(labelEndpointType(e.type))} / ${esc(labelSubType(e.subType))} / ${e.enabled?'启用':'禁用'}${e.pos?` / ${esc(posText(e.pos))}`:''}</span></span>`;return String(e.type||'').toUpperCase()==='LISTENER'?`${base}${signalListenerBasicConfigCard(e,detail)}`:base;}
                function actionsPanel(detail){
                  const actions=detail.actions||[], downstream=detail.downstreamSignals||[];
                  if(actions.length===0&&downstream.length===0)return empty('暂无可用动作详情。');
                  return `<div class="list-stack">${actions.map(a=>`<div class="event-row"><strong>${actionButton(a.id,labelActionType(a.type))}</strong><span class="meta">${esc(a.ownerName||a.ownerId||'-')} · ${esc(labelEndpointType(a.ownerType))}</span><span>${esc(cleanActionSummary(a.summary||''))}</span></div>`).join('')}${downstream.map(c=>`<div class="event-row"><strong>下游频道</strong><span>${channelButton(c)}</span></div>`).join('')}</div>`;
                }
                function waEnsureState(){
                  appState.uiPages=appState.uiPages||{};
                  appState.listenerFilters=appState.listenerFilters||{search:'',enabled:'ALL',channel:'ALL'};
                  appState.virtualBlockFilters=appState.virtualBlockFilters||{search:'',enabled:'ALL',trigger:'ALL',world:'ALL',doctor:'ALL'};
                  appState.virtualBlockDetailCache=appState.virtualBlockDetailCache||{};
                  appState.deviceFilters=appState.deviceFilters||{search:'',type:'ALL',enabled:'ALL',doctor:'ALL',world:'ALL'};
                  appState.historyFilters=appState.historyFilters||{search:'',channel:'ALL',sourceType:'ALL',result:'ALL',range:'ALL',sort:'NEWEST'};
                  appState.actionFilters=appState.actionFilters||{search:'',type:'ALL',owner:'ALL',result:'ALL',doctor:'ALL',sort:'NAME'};
                  appState.templateFilters=appState.templateFilters||{search:'',type:'ALL',status:'ALL',favorite:'ALL',sort:'NAME'};
                  appState.receiverFilters=appState.receiverFilters||{search:'',enabled:'ALL',output:'ALL',channel:'ALL'};
                  appState.receiverDetailCache=appState.receiverDetailCache||{};
                  appState.configFilters=appState.configFilters||{search:'',status:'ALL',type:'ALL'};
                  appState.userFilters=appState.userFilters||{search:'',role:'ALL',enabled:'ALL',online:'ALL'};
                  appState.regionFilters=appState.regionFilters||{search:'',world:'ALL',enabled:'ALL',doctor:'ALL',players:'ALL',sort:'NAME'};
                  appState.regionControllerFilters=appState.regionControllerFilters||{search:'',enabled:'ALL',target:'ALL',event:'ALL'};
                  appState.advancedDetailOpen=appState.advancedDetailOpen||{};
                  if(appState.selectionCreateVirtualBlock===undefined)appState.selectionCreateVirtualBlock=null;
                  if(appState.virtualBlockDelete===undefined)appState.virtualBlockDelete=null;
                  if(appState.signalListenerCreate===undefined)appState.signalListenerCreate=null;
                  if(appState.signalListenerDelete===undefined)appState.signalListenerDelete=null;
                }
                function waMetric(label,value,sub='',iconName='dashboard',kind=''){
                  return `<article class="wa-metric ${esc(kind)}"><div class="wa-metric-top"><div class="wa-metric-label">${esc(label)}</div><span class="wa-icon-bubble ${esc(kind)} icon-bubble-${iconClassName(iconName)}">${icon(iconName)}</span></div><div class="wa-metric-value">${esc(value)}</div>${sub?`<div class="wa-metric-sub">${esc(sub)}</div>`:''}</article>`;
                }
                function waPageHead(title,desc,actions=''){
                  return `<div class="wa-head"><div><h1>${esc(title)}</h1><p>${esc(desc)}</p></div>${actions?`<div class="wa-actions">${actions}</div>`:''}</div>`;
                }
                function waButton(label,iconName='',attrs='',kind='ghost'){
                  return `<button class="wa-btn ${esc(kind)}" ${attrs}>${iconName?icon(iconName):''}<span>${esc(label)}</span></button>`;
                }
                function waIconButton(label,iconName,attrs=''){
                  return `<button class="wa-icon-btn" aria-label="${esc(label)}" title="${esc(label)}" ${attrs}>${icon(iconName)}</button>`;
                }
                function safeHtml(value){return {__waHtml:String(value||'')};}
                function detailValue(value){
                  if(value&&typeof value==='object'&&Object.prototype.hasOwnProperty.call(value,'__waHtml'))return value.__waHtml;
                  if(value===null||value===undefined||value==='')return '<span class="muted">暂无</span>';
                  return esc(value);
                }
                async function copyTextToClipboard(value){
                  try{
                    if(navigator.clipboard&&navigator.clipboard.writeText)await navigator.clipboard.writeText(String(value||''));
                    else throw new Error('clipboard unavailable');
                    toast('已复制。');
                  }catch(_){
                    toast('当前浏览器不允许直接复制，请手动选择文本复制。');
                  }
                }
                function detailHeader(opts){
                  const badges=(opts.badges||[]).filter(Boolean).join('');
                  const actions=(opts.actions||[]).filter(Boolean).join('');
                  const copy=opts.copyValue?waIconButton('复制 ID','copy',htmlHandler(`copyTextToClipboard(${jsString(opts.copyValue)})`)):'';
                  return `<header class="wa-detail-head">
                    <div class="wa-detail-title-wrap">
                      ${opts.back?`<div class="back-row compact">${opts.back}</div>`:''}
                      <div class="wa-detail-title-line">
                        <span class="wa-detail-icon icon-bubble-${iconClassName(opts.iconName||'dashboard')}">${icon(opts.iconName||'dashboard')}</span>
                        <div class="wa-detail-title-copy">
                          <div class="wa-detail-kicker">${esc(opts.kicker||'WebAdmin 7.5')}</div>
                          <h1>${esc(opts.title||'详情')}</h1>
                          <p>${esc(opts.subtitle||'')}</p>
                          <div class="wa-detail-badges">${badges}${copy}</div>
                        </div>
                      </div>
                    </div>
                    <div class="wa-detail-actions">${actions}</div>
                  </header>`;
                }
                function detailTabs(labels,active='基本信息'){
                  return `<nav class="wa-detail-tabs wa-tabs-scroll" data-detail-tabs="true" data-responsive-tabs="true" role="tablist" aria-label="详情页分栏">${labels.map(label=>`<button class="wa-detail-tab ${label===active?'active':''}" type="button" role="tab" aria-selected="${label===active?'true':'false'}" ${htmlHandler(`scrollDetailSection(${jsString(label)})`)}>${esc(label)}</button>`).join('')}</nav>`;
                }
                function scrollDetailSection(label){
                  const view=appView();
                  if(!view||!view.querySelectorAll)return;
                  const key=String(label||'').replace(/列表|信息|统计|检查/g,'').trim();
                  const cards=Array.from(view.querySelectorAll('.wa-detail-card'));
                  const target=cards.find(card=>String(card.textContent||'').includes(label)||key&&String(card.textContent||'').includes(key));
                  if(target&&target.scrollIntoView)target.scrollIntoView({behavior:'smooth',block:'start'});
                }
                function detailCard(title,body,actions='',extraClass=''){
                  return `<article class="wa-detail-card ${esc(extraClass)}"><header class="wa-detail-card-head"><h2>${esc(title)}</h2>${actions?`<div class="wa-detail-card-actions">${actions}</div>`:''}</header><div class="wa-detail-card-body">${body}</div></article>`;
                }
                function detailInfoGrid(rows){
                  const html=(rows||[]).filter(row=>row&&row.length>=2).map(([key,value])=>`<div class="k">${esc(key)}</div><div class="v">${detailValue(value)}</div>`).join('');
                  return `<div class="identity-grid wa-detail-info">${html||'<div class="muted">暂无详情字段。</div>'}</div>`;
                }
                function detailStatGrid(items){
                  return `<div class="wa-detail-stat-grid">${(items||[]).map(item=>`<div class="wa-detail-stat ${esc(item.kind||'')}"><span class="wa-detail-stat-icon">${icon(item.icon||'status')}</span><span class="wa-detail-stat-label">${esc(item.label)}</span><strong>${esc(item.value??'--')}</strong>${item.sub?`<small>${esc(item.sub)}</small>`:''}</div>`).join('')}</div>`;
                }
                function detailConsumerGrid(items){
                  return `<div class="wa-detail-consumers">${(items||[]).map(item=>`<div class="wa-detail-consumer"><span class="wa-detail-consumer-icon">${icon(item.icon||'signal')}</span><span><strong>${esc(item.label)}</strong><small>${esc(item.value??0)}</small></span>${item.target?`<button class="wa-btn ghost" ${navigationAttr(item.target,false)}>查看</button>`:`<button class="wa-btn ghost" disabled>查看</button>`}</div>`).join('')}</div>`;
                }
                function detailSideStack(cards){return `<div class="wa-detail-side-stack" data-detail-right-rail="true">${(cards||[]).filter(Boolean).join('')}</div>`;}
                function detailFixedLayout(leftCards=[],rightCards=[],bottomCards=[]){
                  const left=(leftCards||[]).filter(Boolean).join('');
                  const right=(rightCards||[]).filter(Boolean).join('');
                  const bottom=(bottomCards||[]).filter(Boolean).join('');
                  return `<section class="wa-detail-second-row detail-fixed-layout" data-detail-layout="fixed-two-column" data-detail-row="main" data-detail-two-column="true"><div class="detail-column detail-column-left" data-detail-column="left">${left}</div><div class="detail-column detail-column-right" data-detail-column="right">${right}</div></section>${bottom?`<section class="detail-full-width-stack" data-detail-bottom-full-width="true">${bottom}</section>`:''}`;
                }
                function detailTableWrap(html){return `<div class="wa-table-scroll wa-detail-table-scroll" data-table-row-stretch="false">${html}</div>`;}
                function compactEventList(items,emptyText='暂无最近事件。'){
                  if(!items||items.length===0)return empty(emptyText);
                  return `<div class="wa-compact-list">${items.slice(0,5).map(e=>`<div class="wa-compact-row"><strong>${fmtTime(e.time||e.triggeredAt||e.lastTriggeredAt||e.executedAt)}</strong><span>${esc(e.channel||e.type||e.result||'事件')}</span><small>${esc(e.source||e.sourceType||e.playerName||e.detail||'')}</small></div>`).join('')}</div>`;
                }
                function detailToolGrid(items){
                  return `<div class="wa-quick-grid">${(items||[]).map(item=>waButton(item.label,item.icon||'',item.attrs||'disabled',item.kind||'ghost')).join('')}</div>${items&&items.note?`<p class="wa-disabled-note">${esc(items.note)}</p>`:''}`;
                }
                function advancedDetailKey(kind,id){return `${kind}:${String(id||'')}`;}
                function isAdvancedDetailOpen(kind,id){waEnsureState();return !!appState.advancedDetailOpen[advancedDetailKey(kind,id)];}
                async function toggleAdvancedDetail(kind,id){
                  waEnsureState();
                  const key=advancedDetailKey(kind,id);
                  appState.advancedDetailOpen[key]=!appState.advancedDetailOpen[key];
                  await route({silent:true});
                }
                function advancedValue(value){
                  if(value===null||value===undefined||value==='')return '暂无';
                  if(typeof value==='object'){try{return JSON.stringify(value);}catch(_){return String(value);}}
                  return String(value);
                }
                function advancedRowsFromObject(obj,prefix='',limit=48){
                  const rows=[];
                  const visit=(value,path,depth)=>{
                    if(rows.length>=limit||value===null||value===undefined)return;
                    if(typeof value==='object'&&!Array.isArray(value)&&depth<2){
                      Object.keys(value).sort().forEach(key=>visit(value[key],path?`${path}.${key}`:key,depth+1));
                      return;
                    }
                    rows.push([path,advancedValue(value)]);
                  };
                  visit(obj,prefix,0);
                  return rows;
                }
                function advancedTable(rows){
                  const clean=(rows||[]).filter(r=>r&&r.length>=2);
                  if(clean.length===0)return '<div class="muted">暂无额外字段。</div>';
                  return `<div class="wa-advanced-table">${clean.map(([key,value])=>`<div class="k">${esc(key)}</div><div class="v"><code>${esc(advancedValue(value))}</code></div>`).join('')}</div>`;
                }
                function advancedDetailCard(kind,id,summaryRows=[],groups=[]){
                  const open=isAdvancedDetailOpen(kind,id), button=open?'收起详情':'显示全部详情';
                  const preview=summaryRows.slice(0,6);
                  const body=open?`<div class="wa-advanced-groups">${groups.map(group=>`<section class="wa-advanced-group"><h3>${esc(group.title)}</h3>${advancedTable(group.rows)}</section>`).join('')||advancedTable(summaryRows)}</div>`:advancedTable(preview);
                  return `<article class="wa-detail-card wa-advanced-card detail-bottom-full ${open?'is-open':'is-closed'}" data-collapsible-detail="true" data-detail-bottom-card="advanced" data-detail-full-width="true" data-advanced-open="${open?'true':'false'}"><header class="wa-detail-card-head"><h2>完整详情</h2><button class="wa-btn ghost" type="button" ${htmlHandler(`toggleAdvancedDetail(${jsString(kind)},${jsString(id)})`)}>${esc(button)}</button></header><div class="wa-advanced-body">${body}</div></article>`;
                }
                function waSelect(id,options,value,labeler=(v)=>v){
                  return `<select class="select" id="${esc(id)}">${options.map(o=>`<option value="${esc(o)}" ${String(o)===String(value)?'selected':''}>${esc(labeler(o))}</option>`).join('')}</select>`;
                }
                function waPageItems(key,items,pageSize=10){
                  waEnsureState();
                  const total=items.length, pages=Math.max(1,Math.ceil(total/pageSize)), current=Math.min(pages,Math.max(1,Number(appState.uiPages[key]||1)));
                  appState.uiPages[key]=current;
                  return {items:items.slice((current-1)*pageSize,current*pageSize),total,pages,current,pageSize};
                }
                function setWaPage(key,page){
                  waEnsureState();
                  appState.uiPages[key]=Math.max(1,Number(page)||1);
                  if(key==='signalbridge')renderSignalList('');
                  if(key==='receivers')renderReceiverList('');
                  if(key==='listeners')renderListenerList('');
                  if(key.startsWith('listenerDetail:'))renderSignalListenerDetail(key.substring('listenerDetail:'.length),{silent:true});
                  if(key==='devices')renderDeviceList('');
                  if(key==='virtualBlockDevices')renderVirtualBlockList('');
                  if(key==='actions')renderActionList('');
                  if(key==='actionTemplates')renderActionTemplateList('');
                  if(key==='history')renderHistoryListPage('');
                  if(key==='config')renderConfigList('');
                  if(key==='users')renderUserList('');
                  if(key==='regions')renderRegionList('');
                  if(key==='regionControllers')renderRegionControllerList('');
                }
                function waPagination(key,page){
                  if(page.total<=page.pageSize)return `<div class="wa-pagination"><span class="wa-page-meta">共 ${esc(page.total)} 条 · 每页 ${esc(page.pageSize)} 条</span></div>`;
                  const nums=[1,2,3].filter(n=>n<=page.pages);
                  if(page.pages>4)nums.push('…');
                  if(page.pages>3)nums.push(page.pages);
                  const prev=Math.max(1,page.current-1), next=Math.min(page.pages,page.current+1);
                  return `<div class="wa-pagination"><div class="wa-page-buttons"><button class="wa-page-btn" onclick="setWaPage('${key}',${prev})">‹</button>${nums.map(n=>n==='…'?`<span class="wa-page-meta">…</span>`:`<button class="wa-page-btn ${n===page.current?'active':''}" onclick="setWaPage('${key}',${n})">${n}</button>`).join('')}<button class="wa-page-btn" onclick="setWaPage('${key}',${next})">›</button></div><span class="wa-page-meta">共 ${esc(page.total)} 条</span><span class="wa-page-meta">每页 ${esc(page.pageSize)} 条</span></div>`;
                }
                function modalSnapshot(kind,draft){
                  const k=String(kind||'');
                  if(!draft)return '';
                  if(k==='device_metadata')return JSON.stringify({displayName:String(draft.displayName||''),note:String(draft.note||''),iconKey:String(draft.iconKey||'auto')});
                  if(k==='device_basic_config')return JSON.stringify({enabled:!!draft.enabled,channel:normalizeChannelName(draft.channel)});
                  if(k==='device_extended_config'){const values={}, clear={};(draft.supportedFields||Object.keys(draft.values||{})).forEach(field=>{const value=(draft.values||{})[field];values[field]=isExtendedTickField(field)?Number(value||0):(isExtendedChannelField(field)?normalizeChannelName(value):String(value??''));if((draft.clear||{})[field])clear[field]=true;});return JSON.stringify({values,clear});}
                  if(k==='action_relay_actions')return actionRelayActionsEditableJson(draft.actions||[]);
                  if(k==='vbd_native_triggers')return vbdNativeTriggerEditableJson(draft);
                  if(k==='interaction_item_matcher')return interactionItemMatcherEditableJson(draft);
                  if(k==='channel_metadata')return JSON.stringify({displayName:String(draft.displayName||''),note:String(draft.note||''),iconKey:String(draft.iconKey||'auto')});
                  if(k==='signal_listener_basic_config')return JSON.stringify({enabled:!!draft.enabled,channel:normalizeChannelName(draft.channel),cooldownTicks:Number(draft.cooldownTicks||0)});
                  if(k==='signal_listener_create')return JSON.stringify({name:String(draft.name||''),channel:normalizeChannelName(draft.channel),enabled:draft.enabled!==false,cooldownTicks:Number(draft.cooldownTicks||0)});
                  if(k==='selection_create_virtual_block')return JSON.stringify({targetPlayerName:String(draft.targetPlayerName||'').trim(),channel:normalizeChannelName(draft.channel),displayName:String(draft.displayName||'').trim(),note:String(draft.note||''),iconKey:String(draft.iconKey||'auto'),enabled:draft.enabled!==false});
                  return JSON.stringify(draft);
                }
                function markModalInitialSnapshot(kind,draft){if(draft&&!draft.initialSnapshot)draft.initialSnapshot=modalSnapshot(kind,draft);}
                function modalDraftDirty(kind,draft){if(!draft)return false;const current=modalSnapshot(kind,draft);if(!draft.initialSnapshot){draft.initialSnapshot=current;return false;}return current!==draft.initialSnapshot;}
                function syncModalDraftBeforeClose(kind,id){
                  const k=String(kind||'');
                  if(k==='device_metadata'){const d=appState.deviceMetadataEdit;if(d){d.displayName=document.getElementById('metadata-display-name')?.value||'';d.note=document.getElementById('metadata-note')?.value||'';d.iconKey=document.getElementById('metadata-icon')?.value||'auto';}}
                  if(k==='device_basic_config')updateDeviceBasicConfigDraftFromForm(id,false);
                  if(k==='device_extended_config')updateDeviceExtendedConfigDraftFromForm(id);
                  if(k==='action_relay_actions')syncActionRelayActionsDraftFromForm(id);
                  if(k==='vbd_native_triggers')syncVbdNativeTriggerDraftFromForm(id);
                  if(k==='interaction_item_matcher')syncInteractionItemMatcherDraftFromForm(id);
                  if(k==='device_config')applyDeviceConfigDraftsFromForm(id);
                  if(k==='channel_metadata'){const d=appState.channelMetadataEdit;if(d){d.displayName=document.getElementById('channel-metadata-display-name')?.value||'';d.note=document.getElementById('channel-metadata-note')?.value||'';d.iconKey=document.getElementById('channel-metadata-icon')?.value||'auto';}}
                  if(k==='signal_listener_basic_config')updateSignalListenerBasicConfigDraftFromForm(id,false);
                  if(k==='signal_listener_create')updateSignalListenerCreateDraftFromForm(false);
                  if(k==='selection_create_virtual_block'){const d=appState.selectionCreateVirtualBlock;if(d&&d.step==='config')appState.selectionCreateVirtualBlock=selectionDraftFromForm();}
                }
                function isDeviceConfigModalDirty(deviceId){
                  const meta=appState.deviceMetadataEdit,basic=appState.deviceBasicConfigEdit,ext=appState.deviceExtendedConfigEdit,actions=appState.actionRelayActionsEdit,native=appState.vbdNativeTriggerEdit,matcher=appState.interactionItemMatcherEdit;
                  return !!((meta&&meta.deviceId===deviceId&&modalDraftDirty('device_metadata',meta))||(basic&&basic.deviceId===deviceId&&modalDraftDirty('device_basic_config',basic))||(ext&&ext.deviceId===deviceId&&modalDraftDirty('device_extended_config',ext))||(actions&&actions.deviceId===deviceId&&modalDraftDirty('action_relay_actions',actions))||(native&&sameDeviceRef(native.deviceId,deviceId)&&modalDraftDirty('vbd_native_triggers',native))||(matcher&&sameDeviceRef(matcher.deviceId,deviceId)&&modalDraftDirty('interaction_item_matcher',matcher)));
                }
                function cancelDiscardModalClose(){
                  appState.modalDiscardConfirmOpen=false;
                  const layer=document.getElementById('wa-discard-confirm');
                  if(layer)layer.remove();
                }
                function confirmDiscardModalClose(){
                  cancelDiscardModalClose();
                  closeWebAdminModal(true,true);
                }
                function openDiscardChangesConfirm(){
                  const root=document.getElementById('wa-modal-root');
                  if(!root||appState.modalDiscardConfirmOpen)return;
                  appState.modalDiscardConfirmOpen=true;
                  const layer=document.createElement('div');
                  layer.id='wa-discard-confirm';
                  layer.className='wa-discard-confirm-layer';
                  layer.setAttribute('data-discard-confirm-modal','true');
                  layer.onclick=event=>{if(event.target===layer)cancelDiscardModalClose();};
                  layer.innerHTML=`<section class="wa-discard-confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="wa-discard-confirm-title" onclick="event.stopPropagation()"><header><h3 id="wa-discard-confirm-title">未保存的修改</h3></header><p>当前编辑内容尚未保存。关闭后这些修改会丢失。</p><footer><button class="wa-btn ghost" type="button" onclick="cancelDiscardModalClose()">继续编辑</button><button class="wa-btn danger" type="button" onclick="confirmDiscardModalClose()">放弃修改并关闭</button></footer></section>`;
                  root.appendChild(layer);
                }
                function openWebAdminModal(title,body,footer='',options={}){
                  const existing=document.getElementById('wa-modal-root');
                  const existingClosing=existing&&String(existing.className||'').includes('closing');
                  const wrap=existing&&!existingClosing?existing:document.createElement('div');
                  if(existing&&existingClosing)existing.remove();
                  appState.modalDismissPromise=null;
                  wrap.className='wa-modal-backdrop';
                  wrap.id='wa-modal-root';
                  appState.modalCloseHandler=options.onClose||null;
                  appState.modalDirtyChecker=options.dirtyCheck||null;
                  appState.modalSyncBeforeClose=options.syncBeforeClose||null;
                  appState.modalDiscardConfirmOpen=false;
                  const closeAttr=options.closeAttr||'onclick="closeWebAdminModal()"';
                  wrap.innerHTML=`<section class="wa-modal wa-modal-viewport ${esc(options.className||'')}" role="dialog" aria-modal="true" aria-labelledby="wa-modal-title"><header class="wa-modal-head"><h2 id="wa-modal-title" class="wa-modal-title">${esc(title)}</h2>${waIconButton('关闭','close',closeAttr)}</header><div class="wa-modal-body">${body}</div><footer class="wa-modal-foot">${footer||waButton('关闭','',closeAttr,'ghost')}</footer></section>`;
                  wrap.onclick=event=>{if(event.target===wrap)closeWebAdminModal();};
                  if(!existing||existingClosing)document.body.appendChild(wrap);
                }
                function closeWebAdminModal(runHandler=true,force=false){
                  if(appState.modalClosePromise)return appState.modalClosePromise;
                  if(!force&&appState.modalDiscardConfirmOpen){cancelDiscardModalClose();return Promise.resolve(false);}
                  if(!force&&runHandler&&typeof appState.modalSyncBeforeClose==='function')appState.modalSyncBeforeClose();
                  if(!force&&runHandler&&typeof appState.modalDirtyChecker==='function'&&appState.modalDirtyChecker()){openDiscardChangesConfirm();return Promise.resolve(false);}
                  const handler=appState.modalCloseHandler;
                  appState.modalDirtyChecker=null;appState.modalSyncBeforeClose=null;
                  if(runHandler&&handler){appState.modalCloseHandler=null;appState.modalClosePromise=Promise.resolve(handler()).finally(()=>{appState.modalClosePromise=null;});return appState.modalClosePromise;}
                  appState.modalCloseHandler=null;
                  return dismissWebAdminModal();
                }
                function dismissWebAdminModal(){
                  const root=document.getElementById('wa-modal-root');
                  if(!root)return Promise.resolve();
                  appState.modalDirtyChecker=null;appState.modalSyncBeforeClose=null;appState.modalCloseHandler=null;appState.modalDiscardConfirmOpen=false;
                  const discard=document.getElementById('wa-discard-confirm');if(discard)discard.remove();
                  if(String(root.className||'').includes('closing'))return appState.modalDismissPromise||Promise.resolve();
                  root.classList.add('closing');
                  if(root.setAttribute)root.setAttribute('data-modal-closing','true');
                  if(root.dataset)root.dataset.modalClosing='true';
                  let promise;
                  promise=new Promise(resolve=>{
                    let done=false;
                    const finish=()=>{if(done)return;done=true;clearTimeout(timer);root.removeEventListener('animationend',onEnd);root.removeEventListener('animationcancel',finish);if(document.getElementById('wa-modal-root')===root)root.remove();if(appState.modalDismissPromise===promise)appState.modalDismissPromise=null;resolve();};
                    const onEnd=(event)=>{if(event.target===root||String(event.target?.className||'').includes('wa-modal'))finish();};
                    const timer=setTimeout(finish,260);
                    root.addEventListener('animationend',onEnd);
                    root.addEventListener('animationcancel',finish);
                  });
                  appState.modalDismissPromise=promise;
                  return promise;
                }
                function editModalFooter(saving=false){
                  return `<button class="wa-btn ghost" type="button" onclick="closeWebAdminModal()">${icon('close')}<span>取消</span></button><button class="wa-btn primary" type="button" ${saving?'disabled':''} onclick="document.querySelector('#wa-modal-root form')?.requestSubmit()">${icon('check-pass')}<span>${saving?'保存中...':'保存'}</span></button>`;
                }
                document.addEventListener('keydown',event=>{if(event.key==='Escape'){if(appState.modalDiscardConfirmOpen){event.preventDefault();cancelDiscardModalClose();return;}if(appState.openDeviceMoreMenuId){event.preventDefault();closeDeviceMoreMenu(false);return;}closeWebAdminModal();}});
                function unavailableFeature(title='功能暂未开放',message='当前版本没有完整后端支持，因此该操作保持不可用。'){
                  openWebAdminModal(title,`<p>${esc(message)}</p><div class="wa-disabled-note">本轮前端重构只接入只读展示和布局基础，不新增 API 或真实写能力。</div>`);
                }
                function dashboardCard(title,desc,iconName,rows,target){
                  const attrs=target?navDataAttr(target,`打开${title}`):'aria-disabled="true"';
                  return `<article class="wa-dashboard-card${target?'':' disabled'}" ${attrs}><div class="wa-dash-title"><span class="wa-icon-bubble icon-bubble-${iconClassName(iconName)}">${icon(iconName)}</span><div><h2>${esc(title)}</h2><p>${esc(desc)}</p></div></div><div class="wa-metric-list">${rows.map(r=>`<div class="wa-metric-row"><span class="mini-icon mini-icon-${iconClassName(r.icon||'dashboard')}">${icon(r.icon||'dashboard')}</span><span class="label">${esc(r.label)}</span><span class="value">${esc(r.value)}</span></div>`).join('')}</div><span class="wa-card-link">查看详情 →</span></article>`;
                }
                function asArray(result){return result&&result.ok&&Array.isArray(result.data)?result.data:[];}
                async function renderDashboard(options={}){
                  if(!options.silent)setView(loading('正在加载总览...'));
                  const [status,devices,channels,history,doctor,regions,actions,users]=await Promise.all([
                    settle('/api/status'),settle('/api/devices'),settle('/api/signals/channels'),settle('/api/signals/history?limit=10'),settle('/api/doctor'),settle('/api/regions'),settle('/api/actions'),settle('/api/webadmin/users')
                  ]);
                  const deviceList=asArray(devices), channelList=asArray(channels), regionList=asArray(regions), actionList=asArray(actions), hist=asArray(history);
                  const doc=doctor.ok?doctor.data:{summary:{errorCount:0,warningCount:0,infoCount:0},issues:[]};
                  const userList=asArray(users);
                  const receivers=deviceList.filter(d=>String(d.type||'').toUpperCase()==='SIGNAL_RECEIVER');
                  const relays=deviceList.filter(d=>String(d.type||'').toUpperCase()==='ACTION_RELAY');
                  const virtuals=deviceList.filter(d=>String(d.type||'').toUpperCase()==='VIRTUAL_BLOCK_DEVICE');
                  const signalEvents=hist.length;
                  setView(`<section class="wa-page">
                    ${waPageHead('总览','关键系统概览与快速访问',waButton('刷新','refresh','onclick="renderDashboard()"','ghost'))}
                    <section class="wa-dashboard-grid">
                      ${dashboardCard('设备概览','信号设备、虚拟方块和动作继电器状态','device-overview',[
                        {icon:'signal-device',label:'信号设备',value:deviceList.length},
                        {icon:'signal-receiver',label:'信号接收器',value:receivers.length},
                        {icon:'virtual-block-device',label:'虚拟方块设备',value:virtuals.length},
                        {icon:'action-relay',label:'动作继电器',value:relays.length}
                      ],'#/devices')}
                      ${dashboardCard('Doctor 概览','系统健康检查与潜在问题诊断','doctor-overview',[
                        {icon:'critical-issue',label:'严重问题',value:doc.summary?.errorCount||0},
                        {icon:'warning-issue',label:'警告',value:doc.summary?.warningCount||0},
                        {icon:'info-issue',label:'信息提示',value:doc.summary?.infoCount||0},
                        {icon:'check-pass',label:'检查通过',value:Math.max(0,deviceList.length-(doc.summary?.affectedDeviceCount||0))}
                      ],'#/doctor')}
                      ${dashboardCard('Signal 概览','信号通道与监听器运行状态','signal-overview',[
                        {icon:'active-channel',label:'活跃信号通道',value:channelList.length},
                        {icon:'listener-receiver',label:'信号监听/接收',value:channelList.reduce((n,c)=>n+Number(c.listenerCount||0)+Number(c.receiverCount||0),0)},
                        {icon:'recent-event',label:'最近事件',value:signalEvents},
                        {icon:'response-time',label:'平均响应',value:'--'}
                      ],'#/signals')}
                      ${dashboardCard('区域概览','区域控制器和区域运行状态','region-overview',[
                        {icon:'region-controller',label:'区域控制器',value:regionList.length},
                        {icon:'active-region',label:'活跃区域',value:regionList.filter(r=>r.enabled!==false).length},
                        {icon:'action-binding',label:'动作绑定',value:regionList.reduce((n,r)=>n+Number(r.enterActionCount||0)+Number(r.exitActionCount||0)+Number(r.stayActionCount||0),0)},
                        {icon:'today-trigger',label:'今日触发',value:'--'}
                      ],'#/regions')}
                      ${dashboardCard('动作概览','动作执行与配置使用情况','action-overview',[
                        {icon:'action-total',label:'动作总数',value:actionList.length},
                        {icon:'enabled',label:'启用中',value:actionList.filter(a=>a.enabled!==false).length},
                        {icon:'today-trigger',label:'今日触发',value:actionList.reduce((n,a)=>n+Number(a.executionCountToday||0),0)},
                        {icon:'success-rate',label:'执行成功率',value:'--'}
                      ],'#/actions')}
                      ${dashboardCard('用户概览','用户与权限管理','user-overview',[
                        {icon:'user-total',label:'用户总数',value:userList.length||'权限限制'},
                        {icon:'current-user',label:'当前用户',value:appState.me?.displayName||appState.me?.username||'-'},
                        {icon:'current-role',label:'当前角色',value:labelRole(appState.me?.role)},
                        {icon:'session',label:'Session',value:status.ok?status.data.webAdmin?.sessionCount||0:'--'}
                      ],'#/users')}
                    </section>
                  </section>`,options);
                }
                function signalDotClass(c){const status=String(c.doctorStatus||'').toUpperCase();if(status==='ERROR')return 'error';if(status==='WARNING')return 'warning';if(consumerCount(c)===0)return 'muted';return 'ok';}
                function signalStatusLabel(c){const status=String(c.doctorStatus||'UNKNOWN').toUpperCase();if(status==='ERROR')return '错误';if(status==='WARNING')return '警告';if(consumerCount(c)===0)return '无消费者';return '正常';}
                async function renderSignals(options={}){
                  if(!options.silent)setView(loading('正在加载 SignalBridge...'));
                  let channels;try{channels=await api('/api/signals/channels')}catch(err){if(options.silent){toast('SignalBridge 实时刷新失败，已保留当前页面。');return;}setView(errorBlock(err.message));return;}
                  storeSignalChannelOptions(channels);
                  appState.signals=channels||[];
                  renderSignalList('',options);
                }
                function renderSignalList(focusId,options={}){
                  waEnsureState();
                  const channels=appState.signals||[], filtered=filterSignalChannels(channels);
                  const page=waPageItems('signalbridge',filtered,10);
                  const hasConsumers=channels.filter(c=>consumerCount(c)>0).length;
                  const abnormal=channels.filter(c=>['WARNING','ERROR'].includes(String(c.doctorStatus||'').toUpperCase())).length;
                  if(setView(`<section class="wa-page">
                    ${waPageHead('SignalBridge','管理信号频道（Channel），查看消费者连接关系与逻辑链入口。',waButton('刷新','refresh','onclick="renderSignals()"','ghost'))}
                    <section class="wa-card-grid wa-metrics-4">
                      ${waMetric('频道总数',channels.length,'所有已创建的信号频道','channel-total')}
                      ${waMetric('有消费者频道',hasConsumers,'至少有消费者的频道','channel-with-consumers','ok')}
                      ${waMetric('无消费者频道',channels.length-hasConsumers,'暂无任何消费者连接','channel-orphan','warning')}
                      ${waMetric('异常频道',abnormal,'存在问题的频道','channel-error',abnormal?'error':'')}
                    </section>
                    <section class="wa-table-card">
                      <div class="wa-filter-bar">
                        <label class="filter-field search-control"><span>搜索</span><input class="input" id="signal-search" placeholder="搜索频道名或频道 ID..." value="${esc(appState.signalFilters.search)}"></label>
                        <label class="filter-field"><span>状态</span>${waSelect('signal-status',['ALL','RECENT','NO_RECENT','WARNING'],appState.signalFilters.status,signalOptionLabel)}</label>
                        <label class="filter-field"><span>排序</span>${waSelect('signal-sort',['RECENT','CHANNEL','CONSUMERS'],appState.signalFilters.sort,signalOptionLabel)}</label>
                        ${waButton('刷新','refresh','onclick="renderSignals()"','ghost')}
                      </div>
                      ${page.items.length===0?empty(channels.length===0?'当前暂无 SignalBridge 频道数据。':'没有匹配当前筛选条件的频道。'):signalBridgeTable(page.items)}
                      ${waPagination('signalbridge',page)}
                    </section>
                  </section>`,options))bindSignalFilters(focusId);
                }
                function signalBridgeTable(items){
                  return `<div class="wa-table-scroll"><table class="wa-table"><thead><tr><th>显示名</th><th>Raw Channel</th><th>消费者摘要</th><th>最后触发</th><th>Doctor 状态</th><th>操作</th></tr></thead><tbody>${items.map(c=>{const target=signalHash(c.channel), title=c.displayName||c.channel||'未命名频道';return `<tr class="wa-clickable-row" ${navDataAttr(target,`查看频道 ${title}`)}><td><div class="wa-row-title"><span class="wa-status-dot ${signalDotClass(c)}"></span><span class="wa-truncate"><strong>${esc(title)}</strong></span></div></td><td class="truncate" title="${esc(c.channel||'')}">${esc(c.channel||'-')}</td><td>${signalConsumerSummary(c)}</td><td>${fmtTime(c.lastTriggeredAt)}</td><td>${pill(c.doctorStatus)} <span class="muted">${esc(signalStatusLabel(c))}</span></td><td><div class="wa-action-cell"><button class="wa-btn ghost" ${navDataAttr(target,`查看频道 ${title}`)}>详情</button>${waIconButton('更多','more','disabled')}</div></td></tr>`;}).join('')}</tbody></table></div>`;
                }
                function signalConsumerSummary(c){
                  const regionCount=Number(c.regionControllerCount||c.regionCount||c.regionControllerConsumerCount||0);
                  const pairs=[['consumer-listener','listener',Number(c.listenerCount||0)],['consumer-receiver','receiver',Number(c.receiverCount||0)],['consumer-relay','relay',Number(c.actionRelayCount||0)],['consumer-region','region',regionCount]];
                  return `<span class="wa-consumers">${pairs.map(([name,kind,count])=>`<span class="wa-consumer wa-consumer-${kind}">${icon(name)}<span>${esc(count)}</span></span>`).join('')}</span>`;
                }
                function bindSignalFilters(focusId){
                  const update=(event)=>{appState.signalFilters.search=document.getElementById('signal-search')?.value||'';appState.signalFilters.consumer=document.getElementById('signal-consumer')?.value||'ALL';appState.signalFilters.status=document.getElementById('signal-status')?.value||'ALL';appState.signalFilters.sort=document.getElementById('signal-sort')?.value||'RECENT';appState.uiPages.signalbridge=1;renderSignalList(event?.target?.id||'');};
                  ['signal-search','signal-status','signal-sort'].forEach(id=>document.getElementById(id)?.addEventListener(id==='signal-search'?'input':'change',update));
                  if(focusId){const el=document.getElementById(focusId);if(el){el.focus();if(el.setSelectionRange&&el.value)el.setSelectionRange(el.value.length,el.value.length);}}
                }
                const RECEIVER_DEFAULT_PULSE_TICKS=5;
                function firstPulseValue(...values){for(const value of values){if(!isBlank(value))return value;}return null;}
                function normalizePulseTicks(value){const n=Number(value);return Number.isFinite(n)&&n>0?n:null;}
                function receiverPulseValueFrom(data){
                  if(!data)return null;
                  return firstPulseValue(data.pulseTicks,data.receiverPulseTicks,data.config?.pulseTicks,data.configSummary?.pulseTicks,data.extendedConfig?.values?.pulseTicks,data.extendedConfig?.pulseTicks,data.debug?.pulseTicks,data.debugSummary?.pulseTicks,data.blockEntity?.pulseTicks);
                }
                function receiverPulseState(d){
                  const direct=normalizePulseTicks(receiverPulseValueFrom(d));
                  if(direct!==null)return{state:'ready',value:direct,source:'list'};
                  const id=String(d?.id||''), cache=id?(appState.receiverDetailCache||{})[id]:null;
                  if(cache&&cache.status==='ok'){
                    const cached=normalizePulseTicks(receiverPulseValueFrom(cache.detail));
                    if(cached!==null)return{state:'ready',value:cached,source:'detail'};
                    if(isReceiver(d))return{state:'ready',value:RECEIVER_DEFAULT_PULSE_TICKS,source:'default'};
                  }
                  if(cache&&cache.status==='error')return{state:'error',message:cache.message||'详情加载失败'};
                  if(isReceiver(d)&&id)return{state:'loading'};
                  return{state:'missing'};
                }
                function receiverPulseTicks(d){const state=receiverPulseState(d);return state.state==='ready'?state.value:null;}
                function receiverPulseText(ticks){const n=normalizePulseTicks(ticks);return n===null?'--':`${n} tick`;}
                function receiverPulseCell(d){
                  const state=receiverPulseState(d);
                  if(state.state==='ready')return `<span class="wa-pulse-value">${esc(receiverPulseText(state.value))}</span>`;
                  if(state.state==='loading')return '<span class="wa-skeleton-text">加载中</span>';
                  if(state.state==='error')return `<span class="muted" title="${esc(state.message||'详情加载失败')}">--</span>`;
                  return '<span class="muted">--</span>';
                }
                async function refreshVisibleReceiverDetails(items,options={}){
                  waEnsureState();
                  if(document.hidden)return;
                  const now=Date.now(), force=!!options.force;
                  const targets=(items||[]).filter(d=>isReceiver(d)&&!isBlank(d.id)).slice(0,10);
                  const jobs=[];
                  targets.forEach(d=>{
                    const id=String(d.id), cache=appState.receiverDetailCache[id];
                    if(cache&&cache.status==='loading')return;
                    if(cache&&cache.status==='ok'&&!force)return;
                    if(cache&&cache.status==='error'&&!force&&now-Number(cache.at||0)<5000)return;
                    if(cache&&cache.status==='ok')appState.receiverDetailCache[id]={...cache,refreshing:true};
                    else appState.receiverDetailCache[id]={status:'loading',at:now};
                    jobs.push((async()=>{
                      try{
                        const detail=await api(`/api/devices/${encodeURIComponent(id)}`);
                        appState.receiverDetailCache[id]={status:'ok',detail,at:Date.now(),refreshing:false};
                        return true;
                      }catch(err){
                        const latest=appState.receiverDetailCache[id];
                        if(latest&&latest.status==='ok'){
                          appState.receiverDetailCache[id]={...latest,refreshing:false,errorAt:Date.now(),message:err.message||'详情加载失败'};
                          return false;
                        }
                        appState.receiverDetailCache[id]={status:'error',message:err.message||'详情加载失败',at:Date.now()};
                        return true;
                      }
                    })());
                  });
                  if(jobs.length===0)return;
                  const results=await Promise.all(jobs);
                  if(results.some(Boolean)&&currentRouteHash()==='#/receivers')renderReceiverList('',{silent:true,skipDetailRefresh:true});
                }
                function receiverOutputActive(d){return !!(d.outputActive||d.outputting||String(d.outputStatus||'').toUpperCase()==='OUTPUTTING');}
                function isReceiver(d){return String(d.type||'').toUpperCase()==='SIGNAL_RECEIVER'||String(d.subType||'').toLowerCase()==='signal_receiver';}
                async function renderReceivers(options={}){
                  if(!options.silent)setView(loading('正在加载接收器...'));
                  let devices;try{devices=await api('/api/devices')}catch(err){if(options.silent){toast('接收器实时刷新失败，已保留当前页面。');return;}setView(errorBlock(err.message));return;}
                  appState.receivers=(devices||[]).filter(isReceiver);
                  renderReceiverList('',options);
                }
                function renderReceiverList(focusId,options={}){
                  waEnsureState();
                  const receivers=appState.receivers||[], filtered=filterReceivers(receivers);
                  const page=waPageItems('receivers',filtered,10);
                  const enabled=receivers.filter(d=>d.enabled).length, outputting=receivers.filter(receiverOutputActive).length, today=receivers.reduce((n,d)=>n+Number(d.triggerCountToday||d.todayTriggerCount||0),0);
                  const rendered=setView(`<section class="wa-page">
                    ${waPageHead('接收器','信号接收器用于监听指定频道，当接收到 signal 时输出红石脉冲。',`${waButton('添加接收器','plus','disabled','primary')}${waButton('批量导入','upload','disabled','ghost')}${waButton('导出配置','download','disabled','ghost')}`)}
                    <section class="wa-card-grid wa-metrics-5">
                      ${waMetric('接收器总数',receivers.length,'较昨日 --','receiver-total')}
                      ${waMetric('启用中',enabled,'已启用接收器','receiver-enabled','ok')}
                      ${waMetric('禁用中',receivers.length-enabled,'已禁用接收器','receiver-disabled','warning')}
                      ${waMetric('当前输出中',outputting,'正在输出红石脉冲','receiver-outputting',outputting?'error':'')}
                      ${waMetric('今日触发次数',today||'--','API 未提供时显示占位','receiver-trigger-today')}
                    </section>
                    <section class="wa-two-column">
                      <div class="wa-table-card">
                        <div class="wa-filter-bar">
                          <label class="filter-field search-control"><span>搜索</span><input class="input" id="receiver-search" placeholder="搜索接收器名称 / ID / 频道..." value="${esc(appState.receiverFilters.search)}"></label>
                          <label class="filter-field"><span>状态</span>${waSelect('receiver-enabled',['ALL','ENABLED','DISABLED'],appState.receiverFilters.enabled,optionLabel)}</label>
                          <label class="filter-field"><span>输出状态</span>${waSelect('receiver-output',['ALL','OUTPUTTING','IDLE'],appState.receiverFilters.output,v=>({ALL:'全部输出状态',OUTPUTTING:'输出中',IDLE:'空闲'}[v]||v))}</label>
                          ${waButton('刷新','refresh','onclick="renderReceivers()"','ghost')}
                        </div>
                        ${page.items.length===0?empty(receivers.length===0?'当前暂无信号接收器。':'没有匹配当前筛选条件的接收器。'):receiverTable(page.items)}
                        ${waPagination('receivers',page)}
                      </div>
                      ${receiverRightRail(receivers)}
                    </section>
                  </section>`,options);
                  if(rendered){bindReceiverFilters(focusId);if(!options.skipDetailRefresh)refreshVisibleReceiverDetails(page.items,{force:!!options.silent});}
                }
                function receiverTable(items){
                  return `<div class="wa-table-scroll"><table class="wa-table"><thead><tr><th>接收器名称 / ID</th><th>监听频道</th><th>位置</th><th>脉冲时长</th><th>输出状态</th><th>启用状态</th><th>最近触发</th><th>操作</th></tr></thead><tbody>${items.map(d=>{const target=deviceHash(d,'signal_receiver'), title=d.displayName||d.id;return `<tr class="wa-clickable-row" ${navDataAttr(target,`查看接收器 ${title}`)}><td><span class="device-name"><span class="device-icon">${icon('receiver-row')}</span><span><strong>${esc(title)}</strong><span class="device-subtitle">ID: ${esc(shortId(d.id))}</span></span></span></td><td class="truncate">${channelCell(d.channel)}</td><td class="truncate">${esc(posText(d.pos))}</td><td>${receiverPulseCell(d)}</td><td>${receiverOutputActive(d)?textPill('输出中','error'):textPill('空闲','info')}</td><td>${pill(d.enabled?'OK':'WARNING')} ${esc(d.enabled?'启用':'禁用')}</td><td>${fmtTime(d.lastTriggeredAt)}</td><td><div class="wa-action-cell"><button class="wa-btn ghost" ${navDataAttr(target,`查看接收器 ${title}`)}>查看</button>${waIconButton('更多','more','disabled')}</div></td></tr>`;}).join('')}</tbody></table></div>`;
                }
                function filterReceivers(items){
                  const f=appState.receiverFilters||{};
                  return items.filter(d=>{const hay=[d.id,d.displayName,d.channel,posText(d.pos),d.world].join(' ').toLowerCase();if(f.search&&!hay.includes(f.search.toLowerCase()))return false;if(f.enabled==='ENABLED'&&!d.enabled)return false;if(f.enabled==='DISABLED'&&d.enabled)return false;if(f.output==='OUTPUTTING'&&!receiverOutputActive(d))return false;if(f.output==='IDLE'&&receiverOutputActive(d))return false;return true;});
                }
                function bindReceiverFilters(focusId){
                  const update=(event)=>{appState.receiverFilters.search=document.getElementById('receiver-search').value;appState.receiverFilters.enabled=document.getElementById('receiver-enabled').value;appState.receiverFilters.output=document.getElementById('receiver-output').value;appState.uiPages.receivers=1;renderReceiverList(event.target.id);};
                  ['receiver-search','receiver-enabled','receiver-output'].forEach(id=>document.getElementById(id)?.addEventListener(id==='receiver-search'?'input':'change',update));
                  if(focusId){const el=document.getElementById(focusId);if(el){el.focus();if(el.setSelectionRange&&el.value)el.setSelectionRange(el.value.length,el.value.length);}}
                }
                function receiverRightRail(receivers){
                  const enabled=receivers.filter(d=>d.enabled).length, outputting=receivers.filter(receiverOutputActive).length;
                  const pulseBuckets=receiverPulseBuckets(receivers);
                  return `<aside class="wa-right-rail">
                    <article class="wa-panel"><h2>全部接收器状态分布</h2>${progressList([{label:'输出中',value:outputting,total:receivers.length,kind:'error'},{label:'空闲中',value:Math.max(0,enabled-outputting),total:receivers.length,kind:'ok'},{label:'禁用中',value:receivers.length-enabled,total:receivers.length,kind:'warning'}])}</article>
                    <article class="wa-panel"><h2>脉冲时长分布</h2>${progressList(pulseBuckets)}</article>
                    <article class="wa-panel"><h2>筛选</h2><div class="wa-rail-filter"><label><span>频道</span><input class="input" id="receiver-rail-search" placeholder="搜索频道名称..." oninput="document.getElementById('receiver-search').value=this.value;appState.receiverFilters.search=this.value;appState.uiPages.receivers=1;renderReceiverList('receiver-rail-search')"></label><label><span>启用状态</span>${waSelect('receiver-rail-enabled',['ALL','ENABLED','DISABLED'],appState.receiverFilters.enabled,optionLabel)}</label><div class="wa-button-row"><button class="wa-btn primary" onclick="appState.receiverFilters.enabled=document.getElementById('receiver-rail-enabled').value;appState.uiPages.receivers=1;renderReceiverList()">应用筛选</button><button class="wa-btn ghost" onclick="appState.receiverFilters={search:'',enabled:'ALL',output:'ALL',channel:'ALL'};appState.uiPages.receivers=1;renderReceiverList()">重置筛选</button></div></div></article>
                    <article class="wa-panel"><h2>快速操作</h2><div class="wa-quick-grid">${waButton('批量启用','receiver-enabled','disabled','ghost')}${waButton('批量禁用','receiver-disabled','disabled','ghost')}${waButton('测试脉冲','pulse-duration','disabled','ghost')}${waButton('清空记录','channel-error','disabled','danger')}</div><p class="wa-disabled-note">批量操作和清空记录需要后端写入、权限、CSRF、审计与 edit lock 完整支持，当前保持禁用。</p></article>
                  </aside>`;
                }
                function receiverPulseBuckets(items){
                  const buckets=[{label:'1-10 tick',value:0,total:items.length,kind:'ok'},{label:'11-20 tick',value:0,total:items.length,kind:''},{label:'21+ tick',value:0,total:items.length,kind:'warning'},{label:'加载中',value:0,total:items.length,kind:'info'},{label:'--',value:0,total:items.length,kind:'info'}];
                  items.forEach(d=>{const state=receiverPulseState(d);if(state.state==='ready'){const t=Number(state.value);if(t<=10)buckets[0].value++;else if(t<=20)buckets[1].value++;else buckets[2].value++;}else if(state.state==='loading')buckets[3].value++;else buckets[4].value++;});
                  return buckets.filter(b=>b.value>0||items.length===0);
                }
                """).append("""
                function waDash(value){return isBlank(value)?'--':value;}
                function waCount(items,predicate){return (items||[]).filter(predicate).length;}
                function sumNumeric(items,keys){let found=false,total=0;(items||[]).forEach(item=>{for(const key of keys){const value=key.split('.').reduce((acc,part)=>acc&&acc[part],item);if(!isBlank(value)){const n=Number(value);if(Number.isFinite(n)){total+=n;found=true;}break;}}});return found?total:'--';}
                function firstKnown(item,keys){for(const key of keys){const value=key.split('.').reduce((acc,part)=>acc&&acc[part],item);if(!isBlank(value))return value;}return '';}
                function uniqueNonBlank(values){return [...new Set((values||[]).filter(v=>!isBlank(v)).map(v=>String(v)))].sort((a,b)=>a.localeCompare(b));}
                function countBy(items,selector){
                  const counts={};
                  (Array.isArray(items)?items:[]).forEach(item=>{
                    let key;
                    if(typeof selector==='function')key=selector(item);
                    else if(!isBlank(selector))key=String(selector).split('.').reduce((acc,part)=>acc==null?undefined:acc[part],item);
                    else key=item;
                    const normalized=isBlank(key)?'UNKNOWN':String(key);
                    counts[normalized]=(counts[normalized]||0)+1;
                  });
                  return counts;
                }
                function parseHashParams(hash){
                  const raw=String(hash||((typeof window!=='undefined'&&window.location)?window.location.hash:'')||'');
                  const index=raw.indexOf('?');
                  if(index<0&&!raw.startsWith('?'))return {};
                  const query=raw.startsWith('?')?raw.slice(1):raw.slice(index+1);
                  try{
                    const result={};
                    new URLSearchParams(query).forEach((value,key)=>{result[key]=value;});
                    return result;
                  }catch(_){
                    return {};
                  }
                }
                function endpointEnabledText(value){return value?'启用':'停用';}
                function deviceTypeIcon(type){return {SIGNAL_EMITTER:'signal-device',SIGNAL_RECEIVER:'signal-receiver',ACTION_RELAY:'action-relay',VIRTUAL_BLOCK_DEVICE:'virtual-block-device'}[String(type||'').toUpperCase()]||'device';}
                function deviceTypeTone(type){return {SIGNAL_EMITTER:'',SIGNAL_RECEIVER:'',ACTION_RELAY:'warning',VIRTUAL_BLOCK_DEVICE:'ok'}[String(type||'').toUpperCase()]||'';}
                function distributionItems(items,mapper,labeler,totalOverride){
                  const total=Number(totalOverride??items.length), counts=countBy(items,mapper);
                  return Object.entries(counts).sort((a,b)=>b[1]-a[1]).map(([key,value])=>({label:labeler?labeler(key):key,value,total,kind:key==='ERROR'||key==='FAILED'||key==='DISABLED'?'error':(key==='WARNING'?'warning':(key==='OK'||key==='ENABLED'?'ok':''))}));
                }

                async function renderSignalListenerDetail(rawId,options={}){
                  const routeInfo=detailRoute(rawId,'#/listeners'), listenerId=routeInfo.id||'';
                  if(!options.silent)setView(loading('正在加载监听器详情...'));
                  const result=await loadSignalListenerDetail(listenerId);
                  if(!result.ok){
                    if(options.silent){toast('监听器详情实时刷新失败，已保留当前页面。');return;}
                    setView(`<section class="wa-page"><div class="back-row">${backButton(routeInfo,'返回信号监听器')}</div>${waPageHead('监听器详情不可用','当前没有稳定 listener id 或频道详情数据可定位该监听器。',`${waButton('编辑监听器','settings','disabled','ghost')}${waButton('创建动作','plus','disabled','primary')}`)}${empty(result.message||'请返回列表，通过已有频道详情确认当前监听器。')}</section>`);
                    return;
                  }
                  const data=result.data, listener=data.listener||{}, detail=data.detail||{}, channel=listener.channel||data.channel||detail.channel||'', title=listener.name||listener.id||listenerId||'未命名监听器';
                  const [history,config]=await Promise.all([channel?settle(`/api/signals/history?channel=${encodeURIComponent(channel)}&limit=8`):Promise.resolve({ok:true,data:[]}),listener.id?settle(`/api/webadmin/signal-listener-basic-config/${encodeURIComponent(listener.id)}`):Promise.resolve({ok:false})]);
                  const actions=listener.actions||[], recent=history.ok?(history.data||[]):[], cfg=config.ok?(config.data||{}):{};
                  const editAction=listener.id&&canEditSignalListenerBasicConfig()?waButton('编辑基本信息','settings',htmlHandler(`startSignalListenerBasicConfigEdit(${jsString(listener.id)},${jsString(channel)})`),'primary'):waButton('编辑基本信息','settings','disabled','ghost');
                  const deleteAction=listener.id&&canDeleteSignalListener()?waButton('删除监听器','channel-error',htmlHandler(`openSignalListenerDeleteModal(${jsString(listener.id)},${jsString(channel)})`),'danger'):waButton('删除监听器','channel-error','disabled','danger');
                  const listenerStatus=listener.enabled!==false?'启用':'停用', actionCount=listener.actionCount??actions.length, cooldown=listener.cooldownTicks??cfg.cooldownTicks;
                  const advancedRows=[
                    ['listener.id',listener.id||listenerId],
                    ['listener.name',title],
                    ['listener.channel',channel],
                    ['listener.enabled',listenerStatus],
                    ['listener.cooldownTicks',cooldown],
                    ['listener.actionCount',actionCount],
                    ['listener.lastTriggeredAt',formatDateTime(listener.lastTriggeredAt)],
                    ['config.fingerprint',cfg.fingerprint||cfg.expectedFingerprint]
                  ];
                  const rendered=setView(`<section class="wa-page wa-detail-shell" data-detail-kind="listener">
                    ${detailHeader({back:backButton(routeInfo,'返回信号监听器'),kicker:'信号监听器 / 监听器详情',iconName:'listener-receiver',title:title,subtitle:listener.description||`监听频道：${channel||'未绑定'}`,copyValue:listener.id||listenerId,badges:[`<span class="pill info">ID: ${esc(listener.id||listenerId||'无稳定 ID')}</span>`,pill(listener.enabled!==false?'OK':'WARNING')],actions:[waButton('测试触发','play','disabled','ghost'),waButton('复制监听器','copy','disabled','ghost'),waButton('更多','more','disabled','ghost')]})}
                    ${detailTabs(['基本信息','动作列表','最近事件','触发统计','递归检查'])}
                    <section class="wa-detail-first-row">
                      ${detailCard('基本信息',detailInfoGrid([
                        ['监听器名称',title],
                        ['ID',listener.id||listenerId||'无稳定 ID'],
                        ['频道 Channel',safeHtml(channel?channelButton(channel):'<span class="muted">未绑定</span>')],
                        ['状态',safeHtml(pill(listener.enabled!==false?'OK':'WARNING')+' '+esc(listenerStatus))],
                        ['冷却时间',formatTicks(cooldown)||'0 tick'],
                        ['描述',listener.description||cfg.note||'暂无描述'],
                        ['创建/修改时间',cfg.updatedAt?`${formatDateTime(cfg.updatedAt)} · ${cfg.updatedBy||'未知用户'}`:'暂无']
                      ]),editAction)}
                      ${detailCard('状态与统计',`${detailStatGrid([
                        {label:'动作数量',value:actionCount,sub:'只读 actions',icon:'action-total'},
                        {label:'今日触发次数',value:listener.triggerCountToday ?? 0,sub:'today',icon:'today-trigger'},
                        {label:'最后触发',value:formatDateTime(listener.lastTriggeredAt),sub:'last trigger',icon:'recent-event'},
                        {label:'总触发次数',value:listener.totalTriggerCount ?? listener.triggerCountTotal ?? 0,sub:'total',icon:'history'}
                      ])}<h3 class="wa-detail-subhead">消费者关系 / 关联对象</h3>${detailConsumerGrid([
                        {label:'来源频道',value:labelChannel(channel),icon:'active-channel',target:channel?signalHash(channel):''},
                        {label:'动作列表',value:actionCount,icon:'action-total'},
                        {label:'频道消费者',value:`${(detail.listeners||[]).length} / ${(detail.receivers||[]).length}`,icon:'consumer-listener',target:channel?signalHash(channel):''},
                        {label:'Doctor',value:labelStatus(listener.doctorStatus||detail.doctorStatus||'UNKNOWN'),icon:'doctor-overview',target:'#/doctor'}
                      ])}`)}
                    </section>
                    ${detailFixedLayout([
                      detailCard(`动作列表（共 ${actions.length} 个）`,`${listenerActionList(actions)}<p class="wa-disabled-note">当前版本仅只读展示动作列表；新增、删除和 reorder action 没有完整后端支持。</p>`,'','detail-card-stretchable'),
                      detailCard('最近事件',listenerRecentEvents(recent,channel))
                    ],[
                      detailCard('冷却状态',detailInfoGrid([['启用状态',listenerStatus],['冷却时间',formatTicks(cooldown)||'0 tick'],['最后触发',safeHtml(fmtTime(listener.lastTriggeredAt))],['绑定频道',safeHtml(channel?channelButton(channel):'<span class="muted">未绑定</span>')]]),'','detail-card-stretchable'),
                      detailCard('操作工具',`<div class="wa-quick-grid">${editAction}${waButton('新增动作','plus','disabled','primary')}${deleteAction}${waButton('导出监听器','download','disabled','ghost')}</div><p class="wa-disabled-note">Signal Listener 删除使用危险确认 modal；Action 列表编辑、导出和 matcher / itemSubmit / ConditionEngine 仍保持禁用。</p>`)
                    ],[
                      advancedDetailCard('listeners',listener.id||listenerId,advancedRows,[
                      {title:'listener config',rows:advancedRowsFromObject({listener,cfg},'listener')},
                      {title:'cooldown state / debug info',rows:advancedRowsFromObject({lastTriggeredAt:listener.lastTriggeredAt,cooldownTicks:cooldown,doctorStatus:listener.doctorStatus||detail.doctorStatus,recent},'runtime')},
                      {title:'关联频道详情',rows:advancedRowsFromObject({channel,channelDetail:{stats:detail.stats,doctorIssues:detail.doctorIssues,listeners:detail.listeners,receivers:detail.receivers}},'channel')}
                    ])
                    ])}
                  </section>`,options);
                  if(rendered)renderIcons(appView());
                }
                async function loadSignalListenerDetail(listenerId){
                  if(isBlank(listenerId))return {ok:false,message:'缺少 listener id。'};
                  const cached=(appState.listeners||[]).find(l=>listenerMatches(l,listenerId));
                  if(cached){
                    const channel=cached.channel||cached.sourceChannel||'';
                    const detail=channel?await settle(`/api/signals/channels/${encodeURIComponent(channel)}`):{ok:false};
                    return {ok:true,data:{listener:cached,channel,detail:detail.ok?detail.data:{}}};
                  }
                  let channels;try{channels=await api('/api/signals/channels');}catch(err){return {ok:false,message:err.message};}
                  const candidates=(channels||[]).filter(c=>Number(c.listenerCount||0)>0);
                  const details=await Promise.allSettled(candidates.map(c=>api(`/api/signals/channels/${encodeURIComponent(c.channel||'')}`)));
                  for(let index=0;index<details.length;index++){
                    const result=details[index];if(result.status!=='fulfilled')continue;
                    const detail=result.value||{}, channel=detail.channel||candidates[index]?.channel||'';
                    const found=(detail.listeners||[]).find(l=>listenerMatches(l,listenerId));
                    if(found)return {ok:true,data:{listener:{...found,channel:found.channel||channel,sourceChannel:channel,sourceDisplayName:detail.metadata?.effectiveDisplayName||channel},channel,detail}};
                  }
                  return {ok:false,message:'未能在现有频道详情数据中找到该监听器。'};
                }
                function listenerMatches(listener,id){const target=String(id||'');return [listener?.id,listener?.name,listener?.ref,listener?.listenerRef].filter(v=>!isBlank(v)).some(v=>String(v)===target);}
                function listenerInfoRows(listener,channel,cfg){const rows=[['监听器 ID',listener.id||'--'],['显示名称',listener.name||'--'],['频道',channel||'--'],['启用',listener.enabled!==false?'启用':'停用'],['冷却时间',formatTicks(listener.cooldownTicks??cfg.cooldownTicks)||'0 tick'],['来源频道',listener.sourceDisplayName||listener.sourceChannel||channel||'--'],['配置 fingerprint',cfg.fingerprint||cfg.expectedFingerprint||'--']];return rows.map(([k,v])=>`<div class="kv-row"><span class="muted">${esc(k)}</span><strong>${esc(v)}</strong></div>`).join('');}
                function listenerActionList(actions){if(!actions||actions.length===0)return empty('当前监听器没有可展示的动作。');return `<div class="wa-table-scroll" data-table-row-stretch="false"><table class="wa-table"><thead><tr><th>动作</th><th>类型</th><th>摘要</th><th>状态</th></tr></thead><tbody>${actions.map(a=>`<tr><td>${a.id?actionButton(a.id,a.name||a.id):esc(a.name||a.id||'未命名动作')}</td><td>${textPill(labelActionType(a.type),'info')}</td><td class="truncate" title="${esc(cleanActionSummary(a.summary||''))}">${esc(cleanActionSummary(a.summary||'--'))}</td><td>${pill(a.doctorStatus||'UNKNOWN')}</td></tr>`).join('')}</tbody></table></div>`;}
                function listenerRecentEvents(items,channel){if(!items||items.length===0)return empty('暂无该频道最近事件。');return `<div class="list-stack">${items.slice(0,8).map(h=>`<div class="event-row"><strong>${esc(labelChannel(h.channel||channel))}</strong><span class="meta">${fmtTime(h.time)} · ${esc(labelSourceType(h.sourceType))} · ${esc(labelStatus(h.result))}</span><span>${esc(h.description||h.sourceName||'暂无详情')}</span></div>`).join('')}</div>`;}
                async function renderListeners(options={}){
                  if(!options.silent)setView(loading('正在加载信号监听器...'));
                  let channels=[];
                  try{channels=await api('/api/signals/channels');}
                  catch(err){if(options.silent){toast('信号监听器实时刷新失败，已保留当前页面。');return;}setView(errorBlock(err.message));return;}
                  const listenerChannels=(channels||[]).filter(c=>Number(c.listenerCount||0)>0).slice(0,50);
                  const details=await Promise.allSettled(listenerChannels.map(c=>api(`/api/signals/channels/${encodeURIComponent(c.channel||'')}`)));
                  const listeners=[];
                  details.forEach((result,index)=>{
                    if(result.status!=='fulfilled')return;
                    const detail=result.value||{}, channel=detail.channel||listenerChannels[index]?.channel||'';
                    (detail.listeners||[]).forEach(item=>listeners.push({...item,channel:item.channel||channel,sourceChannel:channel,sourceDisplayName:detail.metadata?.effectiveDisplayName||detail.channel||channel}));
                  });
                  appState.listenerSourceChannels=channels||[];
                  appState.listeners=listeners;
                  appState.listenerLoadLimited=(channels||[]).filter(c=>Number(c.listenerCount||0)>0).length>listenerChannels.length;
                  renderListenerList('',options);
                }
                function renderListenerList(focusId,options={}){
                  waEnsureState();
                  const listeners=appState.listeners||[], channels=appState.listenerSourceChannels||[], filtered=filterListeners(listeners), page=waPageItems('listeners',filtered,10);
                  const enabled=waCount(listeners,l=>l.enabled!==false), bound=uniqueNonBlank(listeners.map(l=>l.channel)).length, today=sumNumeric(channels,['triggerCountToday']);
                  const rendered=setView(`<section class="wa-page">
                    ${waPageHead('信号监听器','后台虚拟监听器用于监听 channel 并执行 actions，不等同于 signal_receiver 方块。',`${canCreateSignalListener()?waButton('新建监听器','plus',htmlHandler('openSignalListenerCreateModal()'),'primary'):waButton('新建监听器','plus','disabled','primary')}${waButton('批量导入','upload','disabled','ghost')}${waButton('导出配置','download','disabled','ghost')}`)}
                    <section class="wa-card-grid wa-metrics-5">
                      ${waMetric('监听器总数',listeners.length,appState.listenerLoadLimited?'仅加载前 50 个有监听器频道':'来自频道详情 API','consumer-listener')}
                      ${waMetric('启用监听器',enabled,'enabled=true','enabled','ok')}
                      ${waMetric('停用监听器',listeners.length-enabled,'enabled=false','receiver-disabled',listeners.length-enabled?'warning':'')}
                      ${waMetric('绑定频道总数',bound,'去重频道数量','active-channel')}
                      ${waMetric('今日触发次数',today,'来自频道统计','today-trigger')}
                    </section>
                    <section class="wa-table-card">
                      <div class="wa-filter-bar">
                        <label class="filter-field search-control"><span>搜索</span><input class="input" id="listener-search" placeholder="搜索监听器名称 / ID / 频道..." value="${esc(appState.listenerFilters.search)}"></label>
                        <label class="filter-field"><span>状态</span>${waSelect('listener-enabled',['ALL','ENABLED','DISABLED'],appState.listenerFilters.enabled,optionLabel)}</label>
                        <label class="filter-field"><span>频道</span>${waSelect('listener-channel',['ALL',...uniqueNonBlank(listeners.map(l=>l.channel))],appState.listenerFilters.channel,v=>v==='ALL'?'全部频道':v)}</label>
                        ${waButton('刷新','refresh','onclick="renderListeners()"','ghost')}
                      </div>
                      ${page.items.length===0?empty(listeners.length===0?'当前没有可从频道详情 API 读取到的信号监听器。':'没有匹配当前筛选条件的监听器。'):listenerTable(page.items)}
                      ${waPagination('listeners',page)}
                    </section>
                  </section>`,options);
                  if(rendered)bindListenerFilters(focusId);
                }
                function listenerTable(items){
                  return `<div class="wa-table-scroll"><table class="wa-table"><thead><tr><th>监听器名称 / ID</th><th>频道</th><th>状态</th><th>冷却时间</th><th>动作数量</th><th>最后触发</th><th>操作</th></tr></thead><tbody>${items.map(l=>{const title=l.name||l.id||'未命名监听器', channel=l.channel||l.sourceChannel||'', channelTarget=isBlank(channel)?'':signalHash(channel), detailTarget=isBlank(l.id)?'':listenerHash(l.id), deleteAttrs=l.id&&canDeleteSignalListener()?htmlHandler(`event.stopPropagation();openSignalListenerDeleteModal(${jsString(l.id)},${jsString(channel)})`):'disabled';return `<tr class="${detailTarget?'wa-clickable-row':''}" ${detailTarget?navDataAttr(detailTarget,`查看监听器 ${title}`):'aria-disabled="true"'}><td><span class="device-name"><span class="device-icon">${icon('consumer-listener')}</span><span><strong>${esc(title)}</strong><span class="device-subtitle">ID: ${esc(shortId(l.id))}</span></span></span></td><td class="truncate" title="${esc(channel)}">${channelCell(channel)}</td><td>${pill(l.enabled!==false?'OK':'WARNING')} ${esc(endpointEnabledText(l.enabled!==false))}</td><td>${esc(formatTicks(l.cooldownTicks)||'0 tick')}</td><td>${esc(l.actionCount ?? '--')}</td><td>${fmtTime(l.lastTriggeredAt)}</td><td><div class="wa-action-cell">${detailTarget?`<button class="wa-btn ghost" ${navDataAttr(detailTarget,`查看监听器 ${title}`)}>详情</button>`:`<button class="wa-btn ghost" disabled>详情</button>`}${channelTarget?`<button class="wa-btn ghost" ${navDataAttr(channelTarget,`查看频道 ${channel}`)}>频道</button>`:`<button class="wa-btn ghost" disabled>频道</button>`}<button class="wa-btn danger" ${deleteAttrs}>删除</button></div></td></tr>`;}).join('')}</tbody></table></div>`;
                }
                function filterListeners(items){
                  const f=appState.listenerFilters||{};
                  return (items||[]).filter(l=>{const hay=[l.id,l.name,l.channel,l.sourceDisplayName].join(' ').toLowerCase();if(f.search&&!hay.includes(f.search.toLowerCase()))return false;if(f.enabled==='ENABLED'&&l.enabled===false)return false;if(f.enabled==='DISABLED'&&l.enabled!==false)return false;if(f.channel&&f.channel!=='ALL'&&String(l.channel||'')!==f.channel)return false;return true;});
                }
                function bindListenerFilters(focusId){
                  const update=(event)=>{appState.listenerFilters.search=document.getElementById('listener-search')?.value||'';appState.listenerFilters.enabled=document.getElementById('listener-enabled')?.value||'ALL';appState.listenerFilters.channel=document.getElementById('listener-channel')?.value||'ALL';appState.uiPages.listeners=1;renderListenerList(event?.target?.id||'');};
                  ['listener-search','listener-enabled','listener-channel'].forEach(id=>document.getElementById(id)?.addEventListener(id==='listener-search'?'input':'change',update));
                  if(focusId){const el=document.getElementById(focusId);if(el){el.focus();if(el.setSelectionRange&&el.value)el.setSelectionRange(el.value.length,el.value.length);}}
                }
                async function renderDevices(options={}){
                  if(!options.silent)setView(loading('正在加载信号设备...'));
                  let devices;try{devices=await api('/api/devices')}catch(err){if(options.silent){toast('信号设备实时刷新失败，已保留当前页面。');return;}setView(errorBlock(err.message));return;}
                  appState.devices=devices||[];
                  renderDeviceList('',options);
                }
                function renderDeviceList(focusId,options={}){
                  waEnsureState();
                  const devices=appState.devices||[], filtered=filterDevices(devices), page=waPageItems('devices',filtered,10), worlds=uniqueNonBlank(devices.map(d=>d.world));
                  const enabled=waCount(devices,d=>d.enabled), today=sumNumeric(devices,['triggerCountToday','todayTriggerCount']), recent=devices.filter(d=>!isBlank(d.lastTriggeredAt)).sort((a,b)=>String(b.lastTriggeredAt||'').localeCompare(String(a.lastTriggeredAt||'')))[0];
                  const rendered=setView(`<section class="wa-page">
                    ${waPageHead('信号设备','统一查看 signal_emitter、signal_receiver、action_relay 与 virtual_block_device。',`${waButton('添加设备','plus','disabled','primary')}${waButton('批量导入','upload','disabled','ghost')}${waButton('导出设备','download','disabled','danger')}`)}
                    <section class="wa-card-grid">
                      ${waMetric('设备总数',devices.length,'来自 /api/devices','device-overview')}
                      ${waMetric('启用中',enabled,'enabled=true','enabled','ok')}
                      ${waMetric('禁用中',devices.length-enabled,'enabled=false','receiver-disabled',devices.length-enabled?'warning':'')}
                      ${waMetric('今日触发次数',today,'API 未提供时显示 --','today-trigger')}
                      ${waMetric('最近触发设备',recent?.displayName||'--',recent?formatRelativeTime(recent.lastTriggeredAt):'暂无','recent-event')}
                      ${waMetric('未绑定频道',waCount(devices,d=>isBlank(d.channel)),'channel 为空','channel-orphan','warning')}
                    </section>
                    <section class="wa-two-column">
                      <div class="wa-table-card">
                        <div class="wa-filter-bar">
                          <label class="filter-field search-control"><span>搜索</span><input class="input" id="device-search" placeholder="搜索设备名称 / ID / channel / 坐标..." value="${esc(appState.deviceFilters.search)}"></label>
                          <label class="filter-field"><span>类型</span>${waSelect('device-type',['ALL','SIGNAL_EMITTER','SIGNAL_RECEIVER','ACTION_RELAY','VIRTUAL_BLOCK_DEVICE','UNKNOWN'],appState.deviceFilters.type,optionLabel)}</label>
                          <label class="filter-field"><span>状态</span>${waSelect('device-enabled',['ALL','ENABLED','DISABLED'],appState.deviceFilters.enabled,optionLabel)}</label>
                          <label class="filter-field"><span>Doctor</span>${waSelect('device-doctor',['ALL','OK','INFO','WARNING','ERROR','UNKNOWN'],appState.deviceFilters.doctor,optionLabel)}</label>
                          <label class="filter-field"><span>世界</span>${waSelect('device-world',['ALL',...worlds],appState.deviceFilters.world,v=>v==='ALL'?'全部世界':v)}</label>
                          ${waButton('刷新','refresh','onclick="renderDevices()"','ghost')}
                        </div>
                        ${page.items.length===0?empty(devices.length===0?'当前暂无设备数据。':'没有匹配当前筛选条件的设备。'):deviceTable(page.items)}
                        ${waPagination('devices',page)}
                      </div>
                      ${deviceRightRail(devices)}
                    </section>
                  </section>`,options);
                  if(rendered)bindDeviceFilters(focusId);
                }
                """).append("""
                function ensurePopoverRoot(){let root=document.getElementById('wa-popover-root');if(!root){root=document.createElement('div');root.id='wa-popover-root';document.body.appendChild(root);}return root;}
                function cssEscape(value){return (typeof CSS!=='undefined'&&CSS.escape)?CSS.escape(String(value)):String(value).replace(/["\\\\]/g,'\\\\$&');}
                function deviceForMoreMenu(deviceId){return (appState.devices||[]).find(d=>String(d.id||'')===String(deviceId))||((appState.currentDeviceDetail&&String(appState.currentDeviceDetail.id||'')===String(deviceId))?appState.currentDeviceDetail:null);}
                function toggleDeviceMoreMenu(deviceId,event){if(event){event.preventDefault();event.stopPropagation();}if(appState.openDeviceMoreMenuId===deviceId){closeDeviceMoreMenu(false);return;}openDeviceMorePopover(deviceId,event?.currentTarget||document.querySelector(`[data-device-more-trigger="${cssEscape(deviceId)}"]`));}
                function closeDeviceMoreMenu(updateButton=true){appState.openDeviceMoreMenuId='';appState.deviceMorePopover=null;const root=document.getElementById('wa-popover-root');if(root)root.innerHTML='';document.querySelectorAll('[data-device-more-trigger]').forEach(btn=>btn.setAttribute('aria-expanded','false'));}
                function openDeviceMorePopover(deviceId,anchor){const device=deviceForMoreMenu(deviceId);if(!device||!anchor)return;appState.openDeviceMoreMenuId=String(deviceId);appState.deviceMorePopover={deviceId:String(deviceId)};document.querySelectorAll('[data-device-more-trigger]').forEach(btn=>btn.setAttribute('aria-expanded',String(btn===anchor)));const root=ensurePopoverRoot();root.innerHTML=`<div id="wa-device-more-popover" class="wa-menu-pop wa-device-more-popover" data-floating-popover="device-more" data-table-popover-portal="true" role="menu" onclick="event.stopPropagation()">${deviceMoreMenuItems(device)}</div>`;positionDeviceMorePopover(anchor);}
                function positionDeviceMorePopover(anchor){const pop=document.getElementById('wa-device-more-popover');if(!pop||!anchor)return;const rect=anchor.getBoundingClientRect(), margin=8, width=Math.min(280,Math.max(240,window.innerWidth-margin*2));pop.style.width=`${width}px`;pop.style.left='0px';pop.style.top='0px';const height=pop.offsetHeight||240;let left=Math.min(window.innerWidth-width-margin,Math.max(margin,rect.right-width));let top=rect.bottom+6;if(top+height>window.innerHeight-margin)top=Math.max(margin,rect.top-height-6);pop.style.left=`${left}px`;pop.style.top=`${top}px`;}
                function deviceMoreMenuItems(d){
                  const id=String(d.id||''), target=deviceHash(d);
                  const actionLock=actionRelayLockForDevice(d), lockedByOther=actionRelayLockHeldByOther(actionLock);
                  const actionRelayItem=isActionRelay(d)?(lockedByOther?`<button class="wa-menu-item" type="button" data-action-relay-more-menu-entry="true" onclick='event.stopPropagation();closeDeviceMoreMenu(false);openActionRelayActionsReadonlyModal(${jsString(id)})'>${icon('action-relay')}<span>只读查看 Action 列表</span></button><div class="wa-menu-note">${esc(actionRelayLockMessage(actionLock))}</div>`:`<button class="wa-menu-item" type="button" data-action-relay-more-menu-entry="true" onclick='event.stopPropagation();closeDeviceMoreMenu(false);openActionRelayActionsModal(${jsString(id)})'>${icon('action-relay')}<span>${canEditActionRelayActions()?'编辑 Action 列表':'查看 Action 列表'}</span></button>`):'';
                  const deleteItem=isVirtualBlockDevice(d)?(canDeleteVirtualBlockDevice()?`<button class="wa-menu-item danger" type="button" onclick='event.stopPropagation();closeDeviceMoreMenu(false);openVirtualBlockDeviceDeleteModal(${jsString(id)})'>${icon('channel-error')}<span>删除 / 解绑 VBD</span></button>`:`<div class="wa-menu-note">需要 EDITOR / OWNER 权限才能删除 / 解绑 VBD。</div>`):`<div class="wa-menu-note" data-physical-device-delete-disabled="true">物理设备不提供 WebUI 删除；需要删除时请在游戏内破坏方块。</div>`;
                  const canEditConfig=canEditDeviceConfig(d), configLockMessage=deviceConfigLockMessage(d);
                  const editConfigItem=canEditConfig&&!configLockMessage?`<button class="wa-menu-item" type="button" data-device-config-more-menu-entry="true" onclick='event.stopPropagation();closeDeviceMoreMenu(false);startDeviceConfigEdit(${jsString(id)})'>${icon('settings')}<span>编辑设备配置</span></button>`:`<button class="wa-menu-item" type="button" data-device-config-more-menu-entry="true" data-device-config-lock-disabled="${configLockMessage?'true':'false'}" disabled>${icon('settings')}<span>编辑设备配置</span></button><div class="wa-menu-note">${esc(configLockMessage||'需要 EDITOR / OWNER 权限才能编辑设备配置。')}</div>`;
                  return `<button class="wa-menu-item" type="button" onclick='event.stopPropagation();closeDeviceMoreMenu(false);location.hash=${jsString(target)}'>${icon('eye')}<span>查看详情</span></button>${editConfigItem}${actionRelayItem}${d.channel?`<button class="wa-menu-item" type="button" onclick='event.stopPropagation();closeDeviceMoreMenu(false);location.hash=${jsString(signalHash(d.channel))}'>${icon('active-channel')}<span>打开频道</span></button>`:''}<button class="wa-menu-item" type="button" onclick='event.stopPropagation();copyTextToClipboard(${jsString(id)});closeDeviceMoreMenu(false)'>${icon('copy')}<span>复制 ID</span></button>${deleteItem}`;
                }
                function deviceMoreMenu(d){const id=String(d.id||''), open=appState.openDeviceMoreMenuId===id;return `<div class="wa-menu-wrap" data-device-more-menu="${esc(id)}" onclick="event.stopPropagation()"><button class="wa-icon-btn" type="button" title="更多" aria-label="更多" data-device-more-trigger="${esc(id)}" aria-expanded="${open?'true':'false'}" onclick='toggleDeviceMoreMenu(${jsString(id)},event)'>${icon('more')}</button></div>`;}
                function deviceTable(items){
                  return `<div class="wa-table-scroll"><table class="wa-table"><thead><tr><th>设备名称 / ID</th><th>类型</th><th>频道</th><th>位置</th><th>状态</th><th>最近触发</th><th>Doctor</th><th>操作</th></tr></thead><tbody>${items.map(d=>{const target=deviceHash(d), title=d.displayName||d.id;return `<tr class="wa-clickable-row" ${navDataAttr(target,`查看设备 ${title}`)}><td><span class="device-name"><span class="device-icon">${icon(deviceTypeIcon(d.type))}</span><span><strong>${esc(title)}</strong><span class="device-subtitle">ID: ${esc(shortId(d.id))}</span></span></span></td><td>${textPill(labelType(d.type),deviceTypeTone(d.type)||'info')}</td><td class="truncate" title="${esc(d.channel||'')}">${channelCell(d.channel)}</td><td class="truncate" title="${esc((d.world||'')+' '+posText(d.pos))}">${esc(d.world||'-')} / ${esc(posText(d.pos))}</td><td>${pill(d.enabled?'OK':'WARNING')} ${esc(labelBool(d.enabled))}</td><td>${fmtTime(d.lastTriggeredAt)}</td><td>${pill(d.doctorStatus)}</td><td><div class="wa-action-cell"><button class="wa-btn ghost" ${navDataAttr(target,`查看设备 ${title}`)}>查看</button>${deviceMoreMenu(d)}</div></td></tr>`;}).join('')}</tbody></table></div>`;
                }
                function filterDevices(items){const f=appState.deviceFilters||{};return (items||[]).filter(d=>{const hay=[d.id,d.displayName,d.channel,d.world,posText(d.pos),d.type,d.doctorStatus].join(' ').toLowerCase();if(f.search&&!hay.includes(f.search.toLowerCase()))return false;if(f.type!=='ALL'&&String(d.type||'UNKNOWN').toUpperCase()!==f.type)return false;if(f.enabled==='ENABLED'&&!d.enabled)return false;if(f.enabled==='DISABLED'&&d.enabled)return false;if(f.doctor!=='ALL'&&String(d.doctorStatus||'UNKNOWN').toUpperCase()!==f.doctor)return false;if(f.world!=='ALL'&&d.world!==f.world)return false;return true;});}
                function bindDeviceFilters(focusId){
                  const update=(event)=>{appState.deviceFilters.search=document.getElementById('device-search')?.value||'';appState.deviceFilters.type=document.getElementById('device-type')?.value||'ALL';appState.deviceFilters.enabled=document.getElementById('device-enabled')?.value||'ALL';appState.deviceFilters.doctor=document.getElementById('device-doctor')?.value||'ALL';appState.deviceFilters.world=document.getElementById('device-world')?.value||'ALL';appState.uiPages.devices=1;renderDeviceList(event?.target?.id||'');};
                  ['device-search','device-type','device-enabled','device-doctor','device-world'].forEach(id=>document.getElementById(id)?.addEventListener(id==='device-search'?'input':'change',update));
                  if(focusId){const el=document.getElementById(focusId);if(el){el.focus();if(el.setSelectionRange&&el.value)el.setSelectionRange(el.value.length,el.value.length);}}
                }
                function deviceRightRail(devices){
                  const enabled=waCount(devices,d=>d.enabled), total=devices.length;
                  return `<aside class="wa-right-rail">
                    <article class="wa-panel"><h2>设备类型分布</h2>${progressList(distributionItems(devices,d=>String(d.type||'UNKNOWN').toUpperCase(),labelType,total))}</article>
                    <article class="wa-panel"><h2>状态分布</h2>${progressList([{label:'启用中',value:enabled,total,kind:'ok'},{label:'禁用中',value:total-enabled,total,kind:'warning'}])}</article>
                    <article class="wa-panel"><h2>快速筛选</h2><div class="wa-quick-grid">${['SIGNAL_EMITTER','SIGNAL_RECEIVER','ACTION_RELAY','VIRTUAL_BLOCK_DEVICE'].map(type=>`<button class="wa-btn ghost" onclick="appState.deviceFilters.type='${type}';appState.uiPages.devices=1;renderDeviceList()">${esc(labelType(type))}</button>`).join('')}<button class="wa-btn ghost" onclick="appState.deviceFilters={search:'',type:'ALL',enabled:'ALL',doctor:'ALL',world:'ALL'};appState.uiPages.devices=1;renderDeviceList()">重置筛选</button></div></article>
                    <article class="wa-panel"><h2>快速操作</h2><div class="wa-quick-grid">${waButton('批量启用','enabled','disabled','ghost')}${waButton('批量禁用','receiver-disabled','disabled','ghost')}${waButton('测试设备','signal-device','disabled','ghost')}${waButton('清空统计','channel-error','disabled','danger')}</div><p class="wa-disabled-note">写操作需要后端权限、CSRF、审计和 edit lock 支持，本轮保持禁用。</p></article>
                  </aside>`;
                }
                function isVirtualBlockDevice(d){return String(d.type||'').toUpperCase()==='VIRTUAL_BLOCK_DEVICE'||String(d.subType||'').toLowerCase()==='virtual_block_device';}
                async function renderVirtualBlockDevices(options={}){
                  if(!options.silent)setView(loading('正在加载虚拟方块设备...'));
                  let devices;try{devices=await api('/api/devices')}catch(err){if(options.silent){toast('虚拟方块设备实时刷新失败，已保留当前页面。');return;}setView(errorBlock(err.message));return;}
                  appState.virtualBlockDevices=(devices||[]).filter(isVirtualBlockDevice);
                  renderVirtualBlockList('',options);
                }
                function virtualDetail(d){const cache=(appState.virtualBlockDetailCache||{})[String(d?.id||'')];return cache&&cache.status==='ok'?cache.detail:null;}
                function virtualConfig(d){return virtualDetail(d)?.configSummary||d.configSummary||{};}
                function virtualBlockId(d){return firstKnown(virtualConfig(d),['blockId','block','minecraftBlockId','boundBlockId']);}
                function virtualTriggerType(d){const cfg=virtualConfig(d);if(!cfg||Object.keys(cfg).length===0)return 'unknown';if(cfg.containerEnabled)return 'container';if(cfg.interactionEnabled)return 'interact';if(cfg.conditionEnabled)return 'blockstate';return firstKnown(cfg,['mode','triggerType'])||'unknown';}
                function labelTriggerType(value){const v=String(value||'unknown').toLowerCase();return {redstone_rising:'红石上升沿',redstone_falling:'红石下降沿',redstone_both:'红石双沿',redstone:'红石触发',blockstate:'方块状态',interact:'玩家交互',container:'容器事件',unknown:'未知'}[v]||value;}
                function virtualConditionText(d){const cfg=virtualConfig(d), parts=[];if(cfg.conditionEnabled)parts.push('条件启用');if(Number(cfg.itemConditionCount||0)>0)parts.push(`物品条件 ${cfg.itemConditionCount}`);if(cfg.interactionItem?.enabled)parts.push('交互物品匹配');if(Number(cfg.itemSubmitRequirementCount||0)>0)parts.push(`提交要求 ${cfg.itemSubmitRequirementCount}`);return parts.length?parts.join(' / '):'--';}
                async function refreshVisibleVirtualBlockDetails(items,options={}){
                  waEnsureState();if(document.hidden)return;
                  const force=!!options.force, now=Date.now(), jobs=[];
                  (items||[]).filter(d=>isVirtualBlockDevice(d)&&!isBlank(d.id)).slice(0,10).forEach(d=>{
                    const id=String(d.id), cache=appState.virtualBlockDetailCache[id];
                    if(cache&&cache.status==='loading')return;
                    if(cache&&cache.status==='ok'&&!force)return;
                    if(cache&&cache.status==='error'&&!force&&now-Number(cache.at||0)<5000)return;
                    appState.virtualBlockDetailCache[id]=cache&&cache.status==='ok'?{...cache,refreshing:true}:{status:'loading',at:now};
                    jobs.push((async()=>{try{const detail=await api(`/api/devices/${encodeURIComponent(id)}`);appState.virtualBlockDetailCache[id]={status:'ok',detail,at:Date.now(),refreshing:false};return true;}catch(err){const latest=appState.virtualBlockDetailCache[id];if(latest&&latest.status==='ok'){appState.virtualBlockDetailCache[id]={...latest,refreshing:false,errorAt:Date.now(),message:err.message||'详情加载失败'};return false;}appState.virtualBlockDetailCache[id]={status:'error',message:err.message||'详情加载失败',at:Date.now()};return true;}})());
                  });
                  if(jobs.length===0)return;
                  const results=await Promise.all(jobs);
                  if(results.some(Boolean)&&['#/virtual-block-devices','#/block-devices'].includes(currentRouteHash()))renderVirtualBlockList('',{silent:true,skipDetailRefresh:true});
                }
                function renderVirtualBlockList(focusId,options={}){
                  waEnsureState();
                  const devices=appState.virtualBlockDevices||[], filtered=filterVirtualBlocks(devices), page=waPageItems('virtualBlockDevices',filtered,10), worlds=uniqueNonBlank(devices.map(d=>d.world));
                  const enabled=waCount(devices,d=>d.enabled), today=sumNumeric(devices,['triggerCountToday','todayTriggerCount']);
                  const conditions=devices.some(d=>!isBlank(virtualConditionText(d))&&virtualConditionText(d)!=='--')?waCount(devices,d=>virtualConditionText(d)!=='--'):'--';
                  const interact=devices.some(d=>!isBlank(virtualConfig(d).interactionEnabled))?waCount(devices,d=>!!virtualConfig(d).interactionEnabled):'--';
                  const rendered=setView(`<section class="wa-page">
                    ${waPageHead('虚拟方块设备','绑定已有 Minecraft 方块作为虚拟事件源；方块材质不使用 WebAdmin 图标伪造。',`${canStartObjectSelection()?waButton('新建虚拟方块设备','plus',htmlHandler('openCreateVirtualBlockDeviceModal()'),'primary'):waButton('新建虚拟方块设备','plus','disabled','primary')}${waButton('批量导入','upload','disabled','ghost')}${waButton('导出配置','download','disabled','danger')}`)}
                    <section class="wa-card-grid">
                      ${waMetric('虚拟设备总数',devices.length,'type=virtual_block_device','virtual-block-device')}
                      ${waMetric('启用中',enabled,'enabled=true','enabled','ok')}
                      ${waMetric('禁用中',devices.length-enabled,'enabled=false','receiver-disabled',devices.length-enabled?'warning':'')}
                      ${waMetric('今日触发次数',today,'API 未提供时显示 --','today-trigger')}
                      ${waMetric('已配置条件',conditions,'来自可见详情缓存','action-binding')}
                      ${waMetric('交互触发设备',interact,'来自可见详情缓存','pulse-duration')}
                    </section>
                    <section class="wa-two-column">
                      <div class="wa-table-card">
                        <div class="wa-filter-bar">
                          <label class="filter-field search-control"><span>搜索</span><input class="input" id="virtual-search" placeholder="搜索设备名称 / ID / 方块 / channel..." value="${esc(appState.virtualBlockFilters.search)}"></label>
                          <label class="filter-field"><span>状态</span>${waSelect('virtual-enabled',['ALL','ENABLED','DISABLED'],appState.virtualBlockFilters.enabled,optionLabel)}</label>
                          <label class="filter-field"><span>触发类型</span>${waSelect('virtual-trigger',['ALL','redstone','blockstate','interact','container'],appState.virtualBlockFilters.trigger,v=>v==='ALL'?'全部触发类型':labelTriggerType(v))}</label>
                          <label class="filter-field"><span>世界</span>${waSelect('virtual-world',['ALL',...worlds],appState.virtualBlockFilters.world,v=>v==='ALL'?'全部世界':v)}</label>
                          ${waButton('刷新','refresh','onclick="renderVirtualBlockDevices()"','ghost')}
                        </div>
                        ${page.items.length===0?empty(devices.length===0?'当前暂无虚拟方块设备。':'没有匹配当前筛选条件的虚拟方块设备。'):virtualBlockTable(page.items)}
                        ${waPagination('virtualBlockDevices',page)}
                      </div>
                      ${virtualBlockRightRail(devices)}
                    </section>
                  </section>`,options);
                  if(rendered){bindVirtualBlockFilters(focusId);if(!options.skipDetailRefresh)refreshVisibleVirtualBlockDetails(page.items,{force:!!options.silent});}
                }
                function virtualBlockTable(items){
                  return `<div class="wa-table-scroll"><table class="wa-table"><thead><tr><th>设备名称 / ID</th><th>绑定方块</th><th>位置</th><th>触发类型</th><th>通道</th><th>状态</th><th>条件配置</th><th>最近触发</th><th>操作</th></tr></thead><tbody>${items.map(d=>{const target=deviceHash(d), title=d.displayName||d.id, block=virtualBlockId(d), trigger=virtualTriggerType(d), deleteAttrs=canDeleteVirtualBlockDevice()?htmlHandler(`event.stopPropagation();openVirtualBlockDeviceDeleteModal(${jsString(d.id)})`):'disabled';return `<tr class="wa-clickable-row" ${navDataAttr(target,`查看虚拟方块设备 ${title}`)}><td><span class="device-name"><span class="device-icon">${icon('virtual-block-device')}</span><span><strong>${esc(title)}</strong><span class="device-subtitle">ID: ${esc(shortId(d.id))}</span></span></span></td><td><span>${esc(block||'--')}</span><span class="device-subtitle">原版材质未在列表 API 中提供时仅显示文本</span></td><td class="truncate" title="${esc((d.world||'')+' '+posText(d.pos))}">${esc(d.world||'-')} / ${esc(posText(d.pos))}</td><td>${textPill(labelTriggerType(trigger),trigger==='container'?'warning':(trigger==='interact'?'ok':'info'))}</td><td class="truncate" title="${esc(d.channel||'')}">${channelCell(d.channel)}</td><td>${pill(d.enabled?'OK':'WARNING')} ${esc(labelBool(d.enabled))}</td><td>${esc(virtualConditionText(d))}</td><td>${fmtTime(d.lastTriggeredAt)}</td><td><div class="wa-action-cell"><button class="wa-btn ghost" ${navDataAttr(target,`查看虚拟方块设备 ${title}`)}>查看</button><button class="wa-btn danger" ${deleteAttrs}>解绑</button></div></td></tr>`;}).join('')}</tbody></table></div>`;
                }
                function filterVirtualBlocks(items){
                  const f=appState.virtualBlockFilters||{};
                  return (items||[]).filter(d=>{const cfg=virtualConfig(d), trigger=virtualTriggerType(d), hay=[d.id,d.displayName,d.channel,d.world,posText(d.pos),virtualBlockId(d),trigger,virtualConditionText(d)].join(' ').toLowerCase();if(f.search&&!hay.includes(f.search.toLowerCase()))return false;if(f.enabled==='ENABLED'&&!d.enabled)return false;if(f.enabled==='DISABLED'&&d.enabled)return false;if(f.trigger!=='ALL'&&String(trigger).toLowerCase()!==String(f.trigger).toLowerCase()&&!(f.trigger==='redstone'&&String(trigger).startsWith('redstone')))return false;if(f.world!=='ALL'&&d.world!==f.world)return false;if(f.doctor!=='ALL'&&String(d.doctorStatus||'UNKNOWN').toUpperCase()!==f.doctor)return false;return true;});
                }
                function bindVirtualBlockFilters(focusId){
                  const update=(event)=>{appState.virtualBlockFilters.search=document.getElementById('virtual-search')?.value||'';appState.virtualBlockFilters.enabled=document.getElementById('virtual-enabled')?.value||'ALL';appState.virtualBlockFilters.trigger=document.getElementById('virtual-trigger')?.value||'ALL';appState.virtualBlockFilters.world=document.getElementById('virtual-world')?.value||'ALL';appState.uiPages.virtualBlockDevices=1;renderVirtualBlockList(event?.target?.id||'');};
                  ['virtual-search','virtual-enabled','virtual-trigger','virtual-world'].forEach(id=>document.getElementById(id)?.addEventListener(id==='virtual-search'?'input':'change',update));
                  if(focusId){const el=document.getElementById(focusId);if(el){el.focus();if(el.setSelectionRange&&el.value)el.setSelectionRange(el.value.length,el.value.length);}}
                }
                function virtualBlockRightRail(devices){
                  const total=devices.length, triggerItems=distributionItems(devices,d=>String(virtualTriggerType(d)||'unknown').toLowerCase(),labelTriggerType,total);
                  return `<aside class="wa-right-rail">
                    <article class="wa-panel"><h2>触发类型分布</h2>${progressList(triggerItems)}</article>
                    <article class="wa-panel"><h2>快速筛选</h2><div class="wa-rail-filter"><label><span>所在世界</span>${waSelect('virtual-rail-world',['ALL',...uniqueNonBlank(devices.map(d=>d.world))],appState.virtualBlockFilters.world,v=>v==='ALL'?'全部世界':v)}</label><label><span>触发类型</span>${waSelect('virtual-rail-trigger',['ALL','redstone','blockstate','interact','container'],appState.virtualBlockFilters.trigger,v=>v==='ALL'?'全部触发类型':labelTriggerType(v))}</label><div class="wa-button-row"><button class="wa-btn primary" onclick="appState.virtualBlockFilters.world=document.getElementById('virtual-rail-world').value;appState.virtualBlockFilters.trigger=document.getElementById('virtual-rail-trigger').value;appState.uiPages.virtualBlockDevices=1;renderVirtualBlockList()">应用筛选</button><button class="wa-btn ghost" onclick="appState.virtualBlockFilters={search:'',enabled:'ALL',trigger:'ALL',world:'ALL',doctor:'ALL'};appState.uiPages.virtualBlockDevices=1;renderVirtualBlockList()">重置筛选</button></div></div></article>
                    <article class="wa-panel"><h2>快速操作</h2><div class="wa-quick-grid">${waButton('批量启用','enabled','disabled','ghost')}${waButton('批量禁用','receiver-disabled','disabled','ghost')}${waButton('测试触发','pulse-duration','disabled','ghost')}${waButton('清空触发记录','channel-error','disabled','danger')}</div><p class="wa-disabled-note">绑定、条件编辑、matcher、itemSubmit 和测试写操作需要后端写入支持，本轮不启用。</p></article>
                  </aside>`;
                }

                async function renderActionTemplatesPage(options={}){
                  if(!options.silent)setView(loading('正在加载动作模板...'));
                  let actions;try{actions=await api('/api/actions')}catch(err){if(options.silent){toast('动作模板实时刷新失败，已保留当前页面。');return;}actions=[];}
                  appState.actions=Array.isArray(actions)?actions:[];
                  appState.actionTemplates=deriveActionTemplates(appState.actions);
                  renderActionTemplateList('',options);
                }
                function deriveActionTemplates(actions){
                  const source=Array.isArray(actions)?actions:[];
                  return source.map((a,index)=>{const type=String(a.type||'UNKNOWN').toUpperCase(), title=a.name||`${labelActionType(type)} 模板 ${index+1}`;return {id:`template:${a.id||index}`,name:title,type,actionCount:Math.max(1,Number(a.referencedByCount||a.actionCount||1)),description:cleanActionSummary(a.summary||`${labelActionType(type)} 动作的只读模板候选。`),favorite:Number(a.executionCount||a.executionCountToday||0)>0,status:actionAvailable(a)?'ENABLED':'DISABLED',createdAt:a.createdAt||'',updatedAt:a.updatedAt||a.lastExecutedAt||'',usageCount:Number(a.executionCount||a.executionCountToday||0),ownerType:a.ownerType||'ACTION',sourceActionId:a.id||'',channel:a.channel||'',doctorStatus:a.doctorStatus||'UNKNOWN'};});
                }
                function renderActionTemplateList(focusId,options={}){
                  waEnsureState();
                  const templates=appState.actionTemplates||[], filtered=filterActionTemplates(templates), page=waPageItems('actionTemplates',filtered,10);
                  const enabled=templates.filter(t=>t.status==='ENABLED').length, favorites=templates.filter(t=>t.favorite).length, today=sumNumeric(templates,['usageCount']);
                  const rendered=setView(`<section class="wa-page">
                    ${waPageHead('动作模板','从现有 Action 只读数据派生模板候选；不创建、不编辑、不删除真实模板。',`${waButton('添加模板','plus','disabled','primary')}${waButton('导入模板','upload','disabled','ghost')}${waButton('导出配置','download','disabled','ghost')}${waButton('批量操作','more','disabled','danger')}`)}
                    <section class="wa-card-grid wa-metrics-5">
                      ${waMetric('模板总数',templates.length,'来自 /api/actions 只读派生','action-template')}
                      ${waMetric('启用中',enabled,'Doctor OK / 可用动作','enabled','ok')}
                      ${waMetric('禁用中',templates.length-enabled,'需要关注或不可用','receiver-disabled',templates.length-enabled?'warning':'')}
                      ${waMetric('今日使用次数',today,'API 未提供时显示 --','today-trigger')}
                      ${waMetric('收藏模板',favorites,'当前按使用记录标记','active-channel')}
                    </section>
                    <section class="wa-two-column">
                      <div class="wa-table-card">
                        <div class="wa-filter-bar">
                          <label class="filter-field search-control"><span>搜索</span><input class="input" id="template-search" placeholder="搜索模板名称 / ID / 类型 / 描述..." value="${esc(appState.templateFilters.search)}"></label>
                          <label class="filter-field"><span>类型</span>${waSelect('template-type',['ALL',...uniqueNonBlank(templates.map(t=>t.type))],appState.templateFilters.type,templateOptionLabel)}</label>
                          <label class="filter-field"><span>状态</span>${waSelect('template-status',['ALL','ENABLED','DISABLED'],appState.templateFilters.status,templateOptionLabel)}</label>
                          <label class="filter-field"><span>收藏</span>${waSelect('template-favorite',['ALL','FAVORITE','NORMAL'],appState.templateFilters.favorite,templateOptionLabel)}</label>
                          ${waButton('刷新','refresh','onclick="renderActionTemplatesPage()"','ghost')}
                        </div>
                        ${page.items.length===0?empty(templates.length===0?'当前没有可从现有 Action 派生的模板候选。':'没有匹配当前筛选条件的动作模板。'):actionTemplateTable(page.items)}
                        ${waPagination('actionTemplates',page)}
                      </div>
                      ${actionTemplateRightRail(templates,filtered)}
                    </section>
                  </section>`,options);
                  if(rendered)bindActionTemplateFilters(focusId);
                }
                function actionTemplateTable(items){return `<div class="wa-table-scroll"><table class="wa-table"><thead><tr><th>模板名称 / ID</th><th>类型</th><th>动作数量</th><th>描述</th><th>收藏</th><th>状态</th><th>创建时间</th><th>最后更新</th><th>操作</th></tr></thead><tbody>${items.map(t=>`<tr><td><span class="device-name"><span class="device-icon">${icon('action-template')}</span><span><strong>${esc(t.name)}</strong><span class="device-subtitle">ID: ${esc(shortId(t.id))}</span></span></span></td><td>${textPill(labelActionType(t.type),actionTypeTone(t.type))}</td><td>${esc(t.actionCount)}</td><td class="truncate" title="${esc(t.description||'')}">${esc(t.description||'--')}</td><td>${textPill(t.favorite?'已收藏':'未收藏',t.favorite?'ok':'info')}</td><td>${textPill(t.status==='ENABLED'?'启用':'禁用',t.status==='ENABLED'?'ok':'warning')} ${pill(t.doctorStatus||'UNKNOWN')}</td><td>${fmtTime(t.createdAt)}</td><td>${fmtTime(t.updatedAt)}</td><td><div class="wa-action-cell"><button class="wa-btn ghost" disabled>使用</button>${t.sourceActionId?`<button class="wa-btn ghost" ${navDataAttr(actionHash(t.sourceActionId),`查看来源动作 ${t.name}`)}>来源</button>`:`<button class="wa-btn ghost" disabled>来源</button>`}${waIconButton('编辑不可用','settings','disabled')}</div></td></tr>`).join('')}</tbody></table></div>`;}
                function filterActionTemplates(items){const f=appState.templateFilters||{};const filtered=(items||[]).filter(t=>{const hay=[t.id,t.name,t.type,t.description,t.ownerType,t.channel].join(' ').toLowerCase();if(f.search&&!hay.includes(f.search.toLowerCase()))return false;if(f.type&&f.type!=='ALL'&&String(t.type)!==f.type)return false;if(f.status==='ENABLED'&&t.status!=='ENABLED')return false;if(f.status==='DISABLED'&&t.status!=='DISABLED')return false;if(f.favorite==='FAVORITE'&&!t.favorite)return false;if(f.favorite==='NORMAL'&&t.favorite)return false;return true;});return filtered.sort((a,b)=>{if(f.sort==='TYPE')return String(a.type).localeCompare(String(b.type))||String(a.name).localeCompare(String(b.name));if(f.sort==='UPDATED')return String(b.updatedAt||'').localeCompare(String(a.updatedAt||''));return String(a.name).localeCompare(String(b.name));});}
                function bindActionTemplateFilters(focusId){const update=(event)=>{appState.templateFilters.search=document.getElementById('template-search')?.value||'';appState.templateFilters.type=document.getElementById('template-type')?.value||'ALL';appState.templateFilters.status=document.getElementById('template-status')?.value||'ALL';appState.templateFilters.favorite=document.getElementById('template-favorite')?.value||'ALL';appState.uiPages.actionTemplates=1;renderActionTemplateList(event?.target?.id||'');};['template-search','template-type','template-status','template-favorite'].forEach(id=>document.getElementById(id)?.addEventListener(id==='template-search'?'input':'change',update));restoreFocusEnd(focusId);}
                function actionTemplateRightRail(items,filtered){const total=Math.max(1,items.length);return `<aside class="wa-right-rail"><article class="wa-panel"><h2>类型分布</h2>${progressList(distributionItems(filtered,t=>String(t.type||'UNKNOWN'),labelActionType,Math.max(1,filtered.length)))}</article><article class="wa-panel"><h2>使用频率</h2>${progressList([{label:'已有使用记录',value:items.filter(t=>Number(t.usageCount||0)>0).length,total,kind:'ok'},{label:'暂无使用记录',value:items.filter(t=>Number(t.usageCount||0)<=0).length,total,kind:'info'},{label:'需关注',value:items.filter(t=>t.status!=='ENABLED').length,total,kind:'warning'}])}</article><article class="wa-panel"><h2>快速筛选</h2><div class="wa-rail-filter"><label><span>模板类型</span>${waSelect('template-rail-type',['ALL',...uniqueNonBlank(items.map(t=>t.type))],appState.templateFilters.type,templateOptionLabel)}</label><div class="wa-button-row"><button class="wa-btn primary" onclick="appState.templateFilters.type=document.getElementById('template-rail-type').value;appState.uiPages.actionTemplates=1;renderActionTemplateList()">应用筛选</button><button class="wa-btn ghost" onclick="appState.templateFilters={search:'',type:'ALL',status:'ALL',favorite:'ALL',sort:'NAME'};appState.uiPages.actionTemplates=1;renderActionTemplateList()">重置筛选</button></div></div></article><article class="wa-panel"><h2>快速操作</h2><div class="wa-quick-grid">${waButton('添加模板','plus','disabled','primary')}${waButton('使用模板','play','disabled','ghost')}${waButton('导入模板','upload','disabled','ghost')}${waButton('导出配置','download','disabled','ghost')}</div><p class="wa-disabled-note">模板创建、使用、导入、导出和批量操作尚无完整后端写入链路，本轮保持禁用且不发送写请求。</p></article></aside>`;}
                function templateOptionLabel(value){return {ALL:'全部',ENABLED:'启用',DISABLED:'禁用',FAVORITE:'收藏',NORMAL:'未收藏'}[String(value||'')]||labelActionType(value);}
                async function renderActionsPage(options={}){
                  if(!options.silent)setView(loading('正在加载动作列表...'));
                  let actions;try{actions=await api('/api/actions')}catch(err){if(options.silent){toast('动作列表实时刷新失败，已保留当前页面。');return;}setView(errorBlock(err.message));return;}
                  appState.actions=actions||[];
                  renderActionList('',options);
                }
                function actionAvailable(a){return String(a.doctorStatus||'UNKNOWN').toUpperCase()==='OK';}
                function renderActionList(focusId,options={}){
                  waEnsureState();
                  const actions=appState.actions||[], filtered=filterActions(actions), page=waPageItems('actions',filtered,10), ownerTypes=uniqueNonBlank(actions.map(a=>String(a.ownerType||'UNKNOWN').toUpperCase()));
                  const ok=waCount(actions,actionAvailable), failed=waCount(actions,a=>String(a.lastResult||'').toUpperCase()==='FAILED'), today=sumNumeric(actions,['executionCountToday','todayExecutionCount']);
                  const rendered=setView(`<section class="wa-page">
                    ${waPageHead('动作列表','ActionEngine 动作只读展示；不包含动作模板或动作编辑器。',`${waButton('添加动作','plus','disabled','primary')}${waButton('批量导入','upload','disabled','ghost')}${waButton('导出配置','download','disabled','danger')}`)}
                    <section class="wa-card-grid wa-metrics-5">
                      ${waMetric('动作总数',actions.length,'来自 /api/actions','action-total')}
                      ${waMetric('可用动作',ok,'Doctor OK','enabled','ok')}
                      ${waMetric('需关注',actions.length-ok,'Doctor 非 OK','warning-issue',actions.length-ok?'warning':'')}
                      ${waMetric('今日执行次数',today,'API 未提供时显示 --','today-trigger')}
                      ${waMetric('失败次数',failed,'最近结果 FAILED','critical-issue',failed?'error':'')}
                    </section>
                    <section class="wa-two-column">
                      <div class="wa-table-card">
                        <div class="wa-filter-bar">
                          <label class="filter-field search-control"><span>搜索</span><input class="input" id="action-search" placeholder="搜索动作名称 / ID / 类型 / owner / channel..." value="${esc(appState.actionFilters.search)}"></label>
                          <label class="filter-field"><span>类型</span>${waSelect('action-type',['ALL','COMMAND','MESSAGE','SOUND','SIGNAL','UNKNOWN'],appState.actionFilters.type,actionOptionLabel)}</label>
                          <label class="filter-field"><span>归属</span>${waSelect('action-owner',['ALL',...ownerTypes],appState.actionFilters.owner,actionOptionLabel)}</label>
                          <label class="filter-field"><span>结果</span>${waSelect('action-result',['ALL','SUCCESS','FAILED','UNKNOWN'],appState.actionFilters.result,actionOptionLabel)}</label>
                          <label class="filter-field"><span>排序</span>${waSelect('action-sort',['NAME','TYPE','OWNER','RECENT'],appState.actionFilters.sort,actionOptionLabel)}</label>
                          ${waButton('刷新','refresh','onclick="renderActionsPage()"','ghost')}
                        </div>
                        ${page.items.length===0?empty(actions.length===0?'当前暂无动作数据。':'没有匹配当前筛选条件的动作。'):actionTable(page.items)}
                        ${waPagination('actions',page)}
                      </div>
                      ${actionRightRail(actions)}
                    </section>
                  </section>`,options);
                  if(rendered)bindActionFilters(focusId);
                }
                function actionTable(items){
                  return `<div class="wa-table-scroll"><table class="wa-table"><thead><tr><th>动作名称 / ID</th><th>类型</th><th>描述</th><th>状态</th><th>标签</th><th>最近执行</th><th>操作</th></tr></thead><tbody>${items.map(a=>{const target=actionHash(a.id), title=a.name||a.id;return `<tr class="wa-clickable-row" ${navDataAttr(target,`查看动作 ${title}`)}><td><span class="device-name"><span class="device-icon">${icon(actionIcon(a.type))}</span><span><strong>${esc(title)}</strong><span class="device-subtitle">ID: ${esc(shortId(a.id))}</span></span></span></td><td>${textPill(labelActionType(a.type),actionTypeTone(a.type))}</td><td class="truncate" title="${esc(cleanActionSummary(a.summary||''))}">${esc(cleanActionSummary(a.summary||'--'))}</td><td>${pill(a.doctorStatus||'UNKNOWN')} ${esc(actionAvailable(a)?'可用':'需关注')}</td><td><span class="pill info">${esc(labelOwnerType(a.ownerType))}</span> ${isBlank(a.channel)?'<span class="muted">无频道</span>':channelCell(a.channel)}</td><td>${fmtTime(a.lastExecutedAt)}</td><td><div class="wa-action-cell"><button class="wa-btn ghost" ${navDataAttr(target,`查看动作 ${title}`)}>查看</button><button class="wa-btn ghost" disabled>编辑</button>${waIconButton('更多','more','disabled')}</div></td></tr>`;}).join('')}</tbody></table></div>`;
                }
                function actionIcon(type){return {COMMAND:'settings',MESSAGE:'history',SOUND:'pulse-duration',SIGNAL:'signalbridge-main',UNKNOWN:'action'}[String(type||'UNKNOWN').toUpperCase()]||'action';}
                function actionTypeTone(type){return {COMMAND:'ok',MESSAGE:'info',SOUND:'warning',SIGNAL:''}[String(type||'').toUpperCase()]||'info';}
                function bindActionFilters(focusId){
                  const update=(event)=>{appState.actionFilters.search=document.getElementById('action-search')?.value||'';appState.actionFilters.type=document.getElementById('action-type')?.value||'ALL';appState.actionFilters.owner=document.getElementById('action-owner')?.value||'ALL';appState.actionFilters.result=document.getElementById('action-result')?.value||'ALL';appState.actionFilters.sort=document.getElementById('action-sort')?.value||'NAME';appState.uiPages.actions=1;renderActionList(event?.target?.id||'');};
                  ['action-search','action-type','action-owner','action-result','action-sort'].forEach(id=>document.getElementById(id)?.addEventListener(id==='action-search'?'input':'change',update));
                  if(focusId){const el=document.getElementById(focusId);if(el){el.focus();if(el.setSelectionRange&&el.value)el.setSelectionRange(el.value.length,el.value.length);}}
                }
                function actionRightRail(actions){
                  const total=actions.length, ok=waCount(actions,actionAvailable);
                  return `<aside class="wa-right-rail">
                    <article class="wa-panel"><h2>动作类型分布</h2>${progressList(distributionItems(actions,a=>String(a.type||'UNKNOWN').toUpperCase(),labelActionType,total))}</article>
                    <article class="wa-panel"><h2>状态分布</h2>${progressList([{label:'可用动作',value:ok,total,kind:'ok'},{label:'需关注',value:total-ok,total,kind:'warning'}])}</article>
                    <article class="wa-panel"><h2>快速筛选</h2><div class="wa-rail-filter"><label><span>类型</span>${waSelect('action-rail-type',['ALL','COMMAND','MESSAGE','SOUND','SIGNAL','UNKNOWN'],appState.actionFilters.type,actionOptionLabel)}</label><label><span>标签搜索</span><input class="input" id="action-rail-search" placeholder="搜索 owner / channel..." value="${esc(appState.actionFilters.search)}"></label><div class="wa-button-row"><button class="wa-btn primary" onclick="appState.actionFilters.type=document.getElementById('action-rail-type').value;appState.actionFilters.search=document.getElementById('action-rail-search').value;appState.uiPages.actions=1;renderActionList()">应用筛选</button><button class="wa-btn ghost" onclick="appState.actionFilters={search:'',type:'ALL',owner:'ALL',result:'ALL',doctor:'ALL',sort:'NAME'};appState.uiPages.actions=1;renderActionList()">重置筛选</button></div></div></article>
                    <article class="wa-panel"><h2>动作类型说明</h2><div class="list-stack"><div class="kv-row"><span class="muted">command</span><strong>执行服务器命令</strong></div><div class="kv-row"><span class="muted">message</span><strong>发送消息</strong></div><div class="kv-row"><span class="muted">sound</span><strong>播放音效</strong></div><div class="kv-row"><span class="muted">signal</span><strong>发送下游 signal</strong></div><div class="kv-row"><span class="muted">unknown</span><strong>聚合或不可识别动作</strong></div></div></article>
                  </aside>`;
                }
                function filterHistoryItems(items){
                  const f=appState.historyFilters||{}, now=Date.now();
                  const filtered=(Array.isArray(items)?items:[]).filter(h=>{
                    const hay=[h.channel,h.sourceType,h.sourceName,h.sourceId,h.result,h.world,h.playerName,h.description].join(' ').toLowerCase();
                    if(f.search&&!hay.includes(String(f.search).toLowerCase()))return false;
                    if(f.channel&&f.channel!=='ALL'&&h.channel!==f.channel)return false;
                    if(f.sourceType&&f.sourceType!=='ALL'&&String(h.sourceType||'UNKNOWN').toUpperCase()!==f.sourceType)return false;
                    if(f.result&&f.result!=='ALL'&&String(h.result||'UNKNOWN').toUpperCase()!==f.result)return false;
                    if(f.range&&f.range!=='ALL'){
                      const t=parseTime(h.time);if(!t)return false;
                      const age=now-t.getTime();
                      if(f.range==='M10'&&age>10*60*1000)return false;
                      if(f.range==='H1'&&age>60*60*1000)return false;
                      if(f.range==='H24'&&age>24*60*60*1000)return false;
                    }
                    return true;
                  });
                  return filtered.sort((a,b)=>{
                    const at=parseTime(a.time)?.getTime()||0, bt=parseTime(b.time)?.getTime()||0;
                    if(f.sort==='OLDEST')return at-bt;
                    return bt-at;
                  });
                }
                function historyOptionLabel(id,value){
                  const v=String(value||'');
                  if(v==='ALL')return '全部';
                  if(id==='history-source')return labelSourceType(v);
                  if(id==='history-result')return {SUCCESS:'成功',FAILED:'失败',UNKNOWN:'未知'}[v]||labelStatus(v);
                  if(id==='history-range')return {M10:'最近 10 分钟',H1:'最近 1 小时',H24:'最近 24 小时'}[v]||v;
                  return v;
                }
                async function renderHistoryPage(queryTail='',options={}){
                  const params=parseHashParams(queryTail);if(params.channel)appState.historyFilters.channel=params.channel;
                  if(!options.silent)setView(loading('正在加载事件历史...'));
                  let history;try{history=await api('/api/signals/history?limit=500')}catch(err){if(options.silent){toast('事件历史实时刷新失败，已保留当前页面。');return;}setView(errorBlock(err.message));return;}
                  appState.historyItems=history||[];
                  renderHistoryListPage('',options);
                }
                function renderHistoryListPage(focusId,options={}){
                  waEnsureState();
                  const items=appState.historyItems||[], filtered=filterHistoryItems(items), page=waPageItems('history',filtered,10), channels=uniqueNonBlank(items.map(h=>h.channel)), sourceTypes=uniqueNonBlank(items.map(h=>h.sourceType));
                  const todayStart=new Date();todayStart.setHours(0,0,0,0);const today=items.filter(h=>{const t=parseTime(h.time);return t&&t.getTime()>=todayStart.getTime();}).length, success=waCount(items,h=>String(h.result||'').toUpperCase()==='SUCCESS'), failed=waCount(items,h=>String(h.result||'').toUpperCase()==='FAILED');
                  const rendered=setView(`<section class="wa-page">
                    ${waPageHead('事件历史','统一查看当前已有 Signal history；Live 后端未完整接入时保持不可用。',waButton('刷新','refresh','onclick="renderHistoryPage()"','ghost'))}
                    <div class="wa-tabs wa-tabs-scroll"><button class="wa-tab active">事件列表</button><button class="wa-tab" disabled>实时流 Live</button></div>
                    <section class="wa-two-column">
                      <div class="wa-table-card">
                        <div class="wa-filter-bar">
                          <label class="filter-field search-control"><span>搜索</span><input class="input" id="history-search" placeholder="搜索频道 / 来源 / 对象 / 结果..." value="${esc(appState.historyFilters.search)}"></label>
                          <label class="filter-field"><span>频道</span>${waSelect('history-channel',['ALL',...channels],appState.historyFilters.channel,v=>v==='ALL'?'全部频道':v)}</label>
                          <label class="filter-field"><span>来源</span>${waSelect('history-source',['ALL',...sourceTypes],appState.historyFilters.sourceType,historyOptionLabel.bind(null,'history-source'))}</label>
                          <label class="filter-field"><span>结果</span>${waSelect('history-result',['ALL','SUCCESS','FAILED','UNKNOWN'],appState.historyFilters.result,historyOptionLabel.bind(null,'history-result'))}</label>
                          <label class="filter-field"><span>时间</span>${waSelect('history-range',['ALL','M10','H1','H24'],appState.historyFilters.range,historyOptionLabel.bind(null,'history-range'))}</label>
                          ${waButton('刷新','refresh','onclick="renderHistoryPage()"','ghost')}
                        </div>
                        ${page.items.length===0?empty(items.length===0?'暂无事件历史。':'没有匹配当前筛选条件的事件。'):historyTable(page.items)}
                        ${waPagination('history',page)}
                      </div>
                      ${historyRightRail(items,filtered,today,success,failed)}
                    </section>
                  </section>`,options);
                  if(rendered)bindHistoryFilters(focusId);
                }
                function historyTable(items){
                  return `<div class="wa-table-scroll"><table class="wa-table"><thead><tr><th>时间</th><th>频道</th><th>来源类型</th><th>来源对象</th><th>动作数</th><th>结果</th><th>操作</th></tr></thead><tbody>${items.map(h=>`<tr><td>${fmtTime(h.time)}</td><td class="truncate" title="${esc(h.channel||'')}">${channelCell(h.channel)}</td><td>${textPill(labelSourceType(h.sourceType),'info')}</td><td><span class="wa-source-object"><strong>${esc(h.sourceName||h.sourceId||'--')}</strong><span class="device-subtitle">${esc([h.world,posText(h.pos),h.playerName].filter(v=>!isBlank(v)&&v!=='-').join(' / ')||h.description||'--')}</span></span></td><td>--</td><td>${pill(h.result||'UNKNOWN')}</td><td><div class="wa-action-cell">${historyAction(h)}${waIconButton('详情不可用','more','disabled')}</div></td></tr>`).join('')}</tbody></table></div>`;
                }
                function bindHistoryFilters(focusId){
                  const update=(event)=>{appState.historyFilters.search=document.getElementById('history-search')?.value||'';appState.historyFilters.channel=document.getElementById('history-channel')?.value||'ALL';appState.historyFilters.sourceType=document.getElementById('history-source')?.value||'ALL';appState.historyFilters.result=document.getElementById('history-result')?.value||'ALL';appState.historyFilters.range=document.getElementById('history-range')?.value||'ALL';appState.uiPages.history=1;renderHistoryListPage(event?.target?.id||'');};
                  ['history-search','history-channel','history-source','history-result','history-range'].forEach(id=>document.getElementById(id)?.addEventListener(id==='history-search'?'input':'change',update));
                  if(focusId){const el=document.getElementById(focusId);if(el){el.focus();if(el.setSelectionRange&&el.value)el.setSelectionRange(el.value.length,el.value.length);}}
                }
                function historyRightRail(items,filtered,today,success,failed){
                  const total=items.length;
                  return `<aside class="wa-right-rail">
                    <article class="wa-panel"><h2>今日事件统计</h2><div class="summary-grid">${waMetric('今日事件数',today,'本地日期','recent-event')}${waMetric('总触发次数',total,'当前内存历史','today-trigger')}${waMetric('成功',success,'result=SUCCESS','check-pass','ok')}${waMetric('失败',failed,'result=FAILED','critical-issue',failed?'error':'')}</div></article>
                    <article class="wa-panel"><h2>来源类型</h2>${progressList(distributionItems(filtered,h=>String(h.sourceType||'UNKNOWN').toUpperCase(),labelSourceType,Math.max(1,filtered.length)))}</article>
                    <article class="wa-panel"><h2>操作</h2><div class="wa-quick-grid">${waButton('实时流 Live','signalbridge-main','disabled','ghost')}${waButton('导出事件记录','download','disabled','ghost')}${waButton('清空历史记录','channel-error','disabled','danger')}</div><p class="wa-disabled-note">导出、清空和完整实时流没有后端写入或流式能力，本轮保持禁用。</p></article>
                  </aside>`;
                }
                """).append("""
                async function renderConfigPage(options={}){
                  if(!options.silent)setView(loading('正在加载配置管理...'));
                  const [settings,status,capabilities]=await Promise.all([settle('/api/webadmin/settings'),settle('/api/status'),settle('/api/webadmin/write/capabilities')]);
                  if(!settings.ok){if(options.silent){toast('配置管理实时刷新失败，已保留当前页面。');return;}setView(errorBlock(settings.error.message));return;}
                  appState.configData={settings:settings.data||{},status:status.ok?status.data:{},capabilities:capabilities.ok?capabilities.data:{}};
                  renderConfigList('',options);
                }
                function renderConfigList(focusId,options={}){
                  waEnsureState();
                  const data=appState.configData||{}, settings=data.settings||{}, storage=settings.storage||{}, service=settings.service||{}, rows=filterConfigRows(configRows(data));
                  const page=waPageItems('config',rows,10), warning=rows.filter(r=>r.status!=='OK').length;
                  const rendered=setView(`<section class="wa-page">
                    ${waPageHead('配置管理','只读查看 WebAdmin 配置文件、存储状态与写入边界；不实现发布、回滚或版本系统。',`${waButton('新建草稿','plus','disabled','primary')}${waButton('导入配置包','upload','disabled','ghost')}${waButton('导出当前配置','download','disabled','ghost')}`)}
                    <section class="wa-card-grid wa-metrics-5">
                      ${waMetric('当前配置版本',storage.configExists?'当前文件':'未检测到','来自 /api/webadmin/settings','settings')}
                      ${waMetric('草稿数量','--','无配置版本系统','archive')}
                      ${waMetric('备份数量','--','无备份列表 API','archive')}
                      ${waMetric('未发布变更','--','无发布流程 API','warning-issue',warning?'warning':'')}
                      ${waMetric('最近发布时间','--','当前后端未提供','clock')}
                      ${waMetric('配置警告',warning,'路径隐藏或文件缺失','warning-issue',warning?'warning':'')}
                    </section>
                    <div class="wa-tabs wa-tabs-scroll"><button class="wa-tab active">配置版本列表</button><button class="wa-tab" disabled>草稿</button><button class="wa-tab" disabled>发布记录</button><button class="wa-tab" disabled>备份记录</button><button class="wa-tab" disabled>导入记录</button><button class="wa-tab" disabled>变更对比 Diff</button><button class="wa-tab" disabled>审计日志</button></div>
                    <section class="wa-two-column">
                      <div class="wa-table-card">
                        <div class="wa-filter-bar">
                          <label class="filter-field search-control"><span>搜索</span><input class="input" id="config-search" placeholder="搜索配置项 / 路径 / 类型..." value="${esc(appState.configFilters.search)}"></label>
                          <label class="filter-field"><span>类型</span>${waSelect('config-type',['ALL','SERVICE','STORAGE','SECURITY','USERS','AUDIT'],appState.configFilters.type,configOptionLabel)}</label>
                          <label class="filter-field"><span>状态</span>${waSelect('config-status',['ALL','OK','WARNING','DISABLED'],appState.configFilters.status,configOptionLabel)}</label>
                          ${waButton('刷新','refresh','onclick="renderConfigPage()"','ghost')}
                        </div>
                        ${page.items.length===0?empty('没有匹配当前筛选条件的配置项。'):configTable(page.items)}
                        ${waPagination('config',page)}
                      </div>
                      ${configRightRail(data,rows,service)}
                    </section>
                  </section>`,options);
                  if(rendered)bindConfigFilters(focusId);
                }
                function configRows(data){
                  const settings=data.settings||{}, storage=settings.storage||{}, service=settings.service||{}, security=settings.security||{}, audit=settings.audit||{}, system=settings.system||{};
                  const hidden=storage.restricted?'受限信息已隐藏':'';
                  return [
                    {name:'WebAdmin 服务配置',type:'SERVICE',status:service.running?'OK':'WARNING',path:`${service.host||'-'}:${service.port||'-'}`,desc:`访问模式：${labelAccessMode(service.accessMode)}`},
                    {name:'当前配置文件',type:'STORAGE',status:storage.configExists?'OK':'WARNING',path:hidden||storage.configPath||'--',desc:`存储作用域：${storage.scope||'WORLD_SAVE'}`},
                    {name:'用户配置文件',type:'USERS',status:storage.usersExists?'OK':'WARNING',path:hidden||storage.usersPath||'--',desc:'WebAdmin 用户与角色存储文件'},
                    {name:'审计日志文件',type:'AUDIT',status:audit.enabled?(storage.auditLogExists?'OK':'WARNING'):'DISABLED',path:hidden||storage.auditLogPath||'--',desc:`审计日志：${audit.enabled?'启用':'关闭'}`},
                    {name:'认证与 Session',type:'SECURITY',status:'OK',path:security.sessionCookieName||'--',desc:`${security.authMode||'USERNAME_PASSWORD'} / ${formatMinutes(security.sessionTtlMinutes)}`},
                    {name:'运行环境摘要',type:'SERVICE',status:'OK',path:system.worldName||'--',desc:`${system.minecraftVersion||'--'} / ${system.serverType||'--'}`}
                  ];
                }
                function filterConfigRows(rows){const f=appState.configFilters;return (rows||[]).filter(r=>{const hay=[r.name,r.type,r.status,r.path,r.desc].join(' ').toLowerCase();if(f.search&&!hay.includes(f.search.toLowerCase()))return false;if(f.type!=='ALL'&&r.type!==f.type)return false;if(f.status!=='ALL'&&r.status!==f.status)return false;return true;});}
                function bindConfigFilters(focusId){
                  const update=(event)=>{appState.configFilters.search=document.getElementById('config-search')?.value||'';appState.configFilters.type=document.getElementById('config-type')?.value||'ALL';appState.configFilters.status=document.getElementById('config-status')?.value||'ALL';appState.uiPages.config=1;renderConfigList(event?.target?.id||'');};
                  ['config-search','config-type','config-status'].forEach(id=>document.getElementById(id)?.addEventListener(id==='config-search'?'input':'change',update));
                  restoreFocusEnd(focusId);
                }
                function configTable(items){return `<div class="wa-table-scroll"><table class="wa-table"><thead><tr><th>配置项</th><th>类型</th><th>状态</th><th>位置 / 标识</th><th>说明</th><th>操作</th></tr></thead><tbody>${items.map(r=>`<tr><td><span class="device-name"><span class="device-icon">${icon(configIcon(r.type))}</span><span><strong>${esc(r.name)}</strong><span class="device-subtitle">${esc(r.type)}</span></span></span></td><td>${textPill(configOptionLabel(r.type),'info')}</td><td>${pill(r.status)}</td><td class="truncate" title="${esc(r.path)}">${esc(r.path)}</td><td class="truncate" title="${esc(r.desc)}">${esc(r.desc)}</td><td><div class="wa-action-cell"><button class="wa-btn ghost" disabled>Diff</button>${waIconButton('更多不可用','more','disabled')}</div></td></tr>`).join('')}</tbody></table></div>`;}
                function configRightRail(data,rows,service){const settings=data.settings||{}, storage=settings.storage||{};return `<aside class="wa-right-rail">
                  <article class="wa-panel"><h2>配置状态</h2><div class="list-stack">${rows.map(r=>`<div class="kv-row"><span class="muted">${esc(r.name)}</span><strong>${pill(r.status)}</strong></div>`).join('')}</div></article>
                  <article class="wa-panel"><h2>最近发布记录</h2>${empty('当前没有配置发布记录 API。')}</article>
                  <article class="wa-panel"><h2>最近备份</h2>${empty('当前没有备份列表 API。')}</article>
                  <article class="wa-panel"><h2>快速操作</h2><div class="wa-quick-grid">${waButton('创建备份','archive','disabled','ghost')}${waButton('查看 Diff','eye','disabled','ghost')}${waButton('发布','enabled','disabled','primary')}${waButton('回滚到此版本','warning-issue','disabled','danger')}</div><p class="wa-disabled-note">发布、回滚、导入覆盖和删除没有完整后端安全能力，本轮保持禁用。</p></article>
                  <article class="wa-panel"><h2>存储边界</h2><div class="list-stack"><div class="kv-row"><span class="muted">作用域</span><strong>${esc(storage.scope||'WORLD_SAVE')}</strong></div><div class="kv-row"><span class="muted">按世界隔离</span><strong>${esc(storage.worldScoped?'是':'否')}</strong></div><div class="kv-row"><span class="muted">当前服务</span><strong>${esc(service.url||'--')}</strong></div></div></article>
                </aside>`;}
                function configOptionLabel(value){return {ALL:'全部',SERVICE:'服务',STORAGE:'存储',SECURITY:'安全',USERS:'用户',AUDIT:'审计',OK:'正常',WARNING:'需关注',DISABLED:'禁用'}[String(value||'')]||value;}
                function configIcon(type){return {SERVICE:'settings',STORAGE:'archive',SECURITY:'doctor-ok',USERS:'user-total',AUDIT:'history'}[String(type||'')]||'settings';}
                function restoreFocusEnd(focusId){if(focusId){const el=document.getElementById(focusId);if(el){el.focus();if(el.setSelectionRange&&el.value)el.setSelectionRange(el.value.length,el.value.length);}}}
                async function renderUsersPage(options={}){
                  if(!options.silent)setView(loading('正在加载用户与权限...'));
                  let data;try{data=await api('/api/webadmin/users')}catch(err){if(options.silent){toast('用户与权限实时刷新失败，已保留当前页面。');return;}setView(`<section class="wa-page">${waPageHead('用户与权限','查看 WebAdmin 用户、角色、权限与 session 状态。',`${waButton('添加用户','plus','disabled','primary')}${waButton('角色管理','user','disabled','ghost')}${waButton('权限策略管理','critical-issue','disabled','danger')}`)}${err.status===403?errorBlock('权限不足：只有所有者可以查看用户与权限。'):errorBlock(err.message)}</section>`);return;}
                  appState.usersData=data||{summary:{},users:[],roles:[]};
                  renderUserList('',options);
                }
                function renderUserList(focusId,options={}){
                  waEnsureState();
                  const data=appState.usersData||{summary:{},users:[],roles:[]}, users=data.users||[], summary=data.summary||{}, filtered=filterUsersStep3(users), page=waPageItems('users',filtered,10);
                  const rendered=setView(`<section class="wa-page">
                    ${waPageHead('用户与权限','查看 WebAdmin 用户、角色、权限与登录状态；不启用用户写操作。',`${waButton('添加用户','plus','disabled','primary')}${waButton('角色管理','user','disabled','ghost')}${waButton('权限策略管理','critical-issue','disabled','danger')}`)}
                    <section class="wa-card-grid wa-metrics-5">
                      ${waMetric('用户总数',summary.totalCount ?? users.length,'来自 /api/webadmin/users','user-total')}
                      ${waMetric('在线用户',summary.onlineCount ?? users.filter(u=>u.online).length,'当前 session','current-user','ok')}
                      ${waMetric('角色数量',(data.roles||[]).length || 4,'OWNER / EDITOR / TESTER / VIEWER','current-role')}
                      ${waMetric('今日登录次数','--','后端未提供登录统计','today-trigger')}
                      ${waMetric('今日操作次数','--','审计聚合未提供','action-total')}
                    </section>
                    <section class="wa-two-column">
                      <div class="wa-table-card">
                        <div class="wa-filter-bar">
                          <label class="filter-field search-control"><span>搜索</span><input class="input" id="user-search" placeholder="搜索用户名 / UID / 角色..." value="${esc(appState.userFilters.search)}"></label>
                          <label class="filter-field"><span>角色</span>${waSelect('user-role',['ALL','OWNER','EDITOR','TESTER','VIEWER'],appState.userFilters.role,userOptionLabel)}</label>
                          <label class="filter-field"><span>状态</span>${waSelect('user-enabled',['ALL','ENABLED','DISABLED'],appState.userFilters.enabled,userOptionLabel)}</label>
                          <label class="filter-field"><span>在线</span>${waSelect('user-online',['ALL','ONLINE','OFFLINE'],appState.userFilters.online,userOptionLabel)}</label>
                          ${waButton('刷新','refresh','onclick="renderUsersPage()"','ghost')}
                        </div>
                        ${page.items.length===0?empty(users.length?'没有匹配当前筛选条件的用户。':'暂无 WebAdmin 用户。'):userTableStep3(page.items)}
                        ${waPagination('users',page)}
                      </div>
                      ${userRightRail(data,users)}
                    </section>
                  </section>`,options);
                  if(rendered)bindUserFiltersStep3(focusId);
                }
                function filterUsersStep3(users){const f=appState.userFilters;return (users||[]).filter(u=>{const hay=[u.username,u.displayName,u.role,u.roleDisplayName,u.createdBy].join(' ').toLowerCase();if(f.search&&!hay.includes(f.search.toLowerCase()))return false;if(f.role!=='ALL'&&String(u.role||'').toUpperCase()!==f.role)return false;if(f.enabled==='ENABLED'&&!u.enabled)return false;if(f.enabled==='DISABLED'&&u.enabled)return false;if(f.online==='ONLINE'&&!u.online)return false;if(f.online==='OFFLINE'&&u.online)return false;return true;});}
                function bindUserFiltersStep3(focusId){const update=(event)=>{appState.userFilters.search=document.getElementById('user-search')?.value||'';appState.userFilters.role=document.getElementById('user-role')?.value||'ALL';appState.userFilters.enabled=document.getElementById('user-enabled')?.value||'ALL';appState.userFilters.online=document.getElementById('user-online')?.value||'ALL';appState.uiPages.users=1;renderUserList(event?.target?.id||'');};['user-search','user-role','user-enabled','user-online'].forEach(id=>document.getElementById(id)?.addEventListener(id==='user-search'?'input':'change',update));restoreFocusEnd(focusId);}
                function userTableStep3(users){return `<div class="wa-table-scroll"><table class="wa-table"><thead><tr><th>用户名 / UID</th><th>角色</th><th>状态</th><th>在线状态</th><th>最后登录</th><th>今日操作</th><th>操作</th></tr></thead><tbody>${users.map(u=>`<tr><td><span class="device-name"><span class="device-icon">${icon('user')}</span><span><strong>${esc(u.displayName||u.username)}</strong><span class="device-subtitle">UID: ${esc(u.username)}</span></span></span></td><td>${textPill(u.roleDisplayName||labelRoleFull(u.role),'info')}</td><td>${textPill(labelEnabledState(u.enabled),u.enabled?'ok':'warning')}</td><td>${textPill(labelOnline(u.online),u.online?'ok':'info')} <span class="muted">${esc(Number(u.sessionCount||0))} session</span></td><td>${fmtTime(u.lastLoginAt)}</td><td>--</td><td><div class="wa-action-cell"><button class="wa-btn ghost" disabled>查看</button><button class="wa-btn ghost" disabled>编辑</button>${waIconButton('更多不可用','more','disabled')}</div></td></tr>`).join('')}</tbody></table></div>`;}
                function userRightRail(data,users){const roles=data.roles||[];return `<aside class="wa-right-rail">
                  <article class="wa-panel"><h2>角色分布</h2>${progressList(roles.map(r=>({label:r.displayName||labelRoleFull(r.role),value:r.count,total:Math.max(1,users.length),kind:'info'})))}</article>
                  <article class="wa-panel"><h2>状态分布</h2>${progressList([{label:'启用中',value:users.filter(u=>u.enabled).length,total:Math.max(1,users.length),kind:'ok'},{label:'禁用中',value:users.filter(u=>!u.enabled).length,total:Math.max(1,users.length),kind:'error'},{label:'在线',value:users.filter(u=>u.online).length,total:Math.max(1,users.length),kind:'info'}])}</article>
                  <article class="wa-panel"><h2>权限概览</h2><div class="list-stack"><div class="kv-row"><span class="muted">权限策略</span><strong>当前命令 / 服务端配置管理</strong></div><div class="kv-row"><span class="muted">密码策略</span><strong>PBKDF2 哈希</strong></div><div class="kv-row"><span class="muted">明文密码</span><strong>不展示</strong></div></div></article>
                  <article class="wa-panel"><h2>快速操作</h2><div class="wa-quick-grid">${waButton('重置密码','key','disabled','ghost')}${waButton('禁用用户','receiver-disabled','disabled','ghost')}${waButton('踢出会话','logout','disabled','ghost')}${waButton('删除用户','critical-issue','disabled','danger')}</div><p class="wa-disabled-note">用户写操作没有完整 WebAdmin HTTP 后端和审计边界，本轮保持禁用。</p></article>
                </aside>`;}
                async function renderSettingsPage(options={}){
                  if(!options.silent)setView(loading('正在加载系统设置...'));
                  const [settings,status,capabilities]=await Promise.all([settle('/api/webadmin/settings'),settle('/api/status'),settle('/api/webadmin/write/capabilities')]);
                  if(!settings.ok){if(options.silent){toast('系统设置实时刷新失败，已保留当前页面。');return;}setView(errorBlock(settings.error.message));return;}
                  const data=settings.data||{}, service=data.service||{}, storage=data.storage||{}, security=data.security||{}, audit=data.audit||{}, system=data.system||{}, visibility=data.visibility||{}, runtime=status.ok?status.data:{};
                  const sessionCount=runtime.webAdmin?.sessionCount ?? system.sessionCount ?? '--';
                  const accessMode=service.accessMode||security.accessMode;
                  setView(`<section class="wa-page wa-settings-shell" data-settings-layout="true" data-responsive-layout="true">
                    ${settingsHeader(`${waButton('保存设置','settings','disabled','primary')}${waButton('重置为默认','archive','disabled','ghost')}`)}
                    <div class="wa-tabs wa-tabs-scroll wa-settings-tabs" data-settings-tabs="true"><button class="wa-tab active">通用设置</button><button class="wa-tab" disabled>安全设置</button><button class="wa-tab" disabled>性能设置</button><button class="wa-tab" disabled>界面设置</button><button class="wa-tab" disabled>通知设置</button><button class="wa-tab" disabled>备份与维护</button><button class="wa-tab" disabled>关于</button></div>
                    <section class="wa-settings-layout">
                      <div class="wa-settings-main">
                        <article class="wa-panel wa-settings-card"><h2>平台信息（只读）</h2>${settingsInfoGrid([
                          {icon:'settings',label:'平台名称',value:'TZZ Mod WebAdmin'},
                          {icon:'archive',label:'平台版本',value:system.modVersion||'unknown'},
                          {icon:'device',label:'Minecraft 版本',value:system.minecraftVersion||'--'},
                          {icon:'server-online',label:'服务器类型',value:labelServerType(system.serverType)},
                          {icon:'region',label:'当前世界 / 存档',value:system.worldName||'--'},
                          {icon:'doctor-overview',label:'访问模式',value:labelAccessMode(accessMode)}
                        ])}</article>
                        <article class="wa-panel wa-settings-card"><h2>服务信息（只读）</h2>${settingsInfoGrid([
                          {icon:'enabled',label:'服务状态',value:settingsServiceStatus(service)},
                          {icon:'active-channel',label:'服务地址',value:service.url||'--'},
                          {icon:'signalbridge-main',label:'监听地址',value:`${service.host||'127.0.0.1'}:${service.port||'18080'}`},
                          {icon:'current-user',label:'当前用户',value:service.currentUser||appState.me?.username||'--'},
                          {icon:'current-role',label:'当前角色',value:labelRoleFull(service.currentRole||appState.me?.role)},
                          {icon:'session',label:'Session 数量',value:sessionCount}
                        ])}</article>
                        <article class="wa-panel wa-settings-card"><h2>功能开关（只读）</h2><div class="wa-settings-switch-grid" data-settings-switch-grid="true">${settingReadonlyToggle('启用 SignalBridge','信号系统由当前服务端模块提供',true)}${settingReadonlyToggle('启用事件记录','记录 signal/action 等已有历史',true)}${settingReadonlyToggle('启用审计日志','WebAdmin 写操作审计',!!audit.enabled || !!security.auditEnabled)}${settingReadonlyToggle('启用 Web API','当前 WebAdmin API 正在服务',true)}${settingReadonlyToggle('启用自动修复','Doctor 自动修复未开放',false)}${settingReadonlyToggle('启用设置写入','系统设置写入未开放',false)}</div></article>
                        <article class="wa-panel wa-settings-card"><h2>运行环境信息（只读）</h2><div class="wa-settings-env-grid">${settingsEnvItem('Java 版本',runtime.javaVersion||runtime.jvm?.version||system.javaVersion||'--','settings')}${settingsEnvItem('CPU 使用率',`${settingsPercent(runtime.cpu?.usagePercent ?? runtime.system?.cpuUsagePercent ?? runtime.cpuUsagePercent,0)}%`,'pulse-duration')}${settingsEnvItem('内存使用率',`${settingsPercent(runtime.memory?.usagePercent ?? runtime.jvm?.memoryUsagePercent ?? runtime.memoryUsagePercent,0)}%`,'response-time')}${settingsEnvItem('磁盘使用率',`${settingsPercent(runtime.disk?.usagePercent ?? storage.diskUsagePercent,0)}%`,'archive')}${settingsEnvItem('TPS',runtime.server?.tps ?? runtime.tps ?? system.tps ?? '--','today-trigger')}${settingsEnvItem('访问模式',labelAccessMode(accessMode),'doctor-overview')}</div></article>
                        <div class="wa-settings-readonly-note">${icon('info')}<span>系统设置当前主要为只读信息。保存、重置、重新加载配置等全局操作缺少完整后端安全能力，本轮保持禁用且不发送写请求。</span><button class="link-button" disabled>了解系统设置</button></div>
                      </div>
                      ${settingsRightRail(data,runtime,capabilities.ok?capabilities.data:{})}
                    </section>
                  </section>`,options);
                }
                function settingsServiceStatus(service){
                  const running=service&&service.running;
                  const kind=running===true?'OK':running===false?'WARNING':'UNKNOWN';
                  const text=running===true?'运行中':running===false?'未运行':'未知';
                  return safeHtml(pill(kind)+' '+esc(text));
                }
                function settingsHeader(actions=''){
                  return `<header class="wa-settings-header"><div class="wa-settings-title"><span class="wa-settings-icon">${icon('settings')}</span><div><div class="wa-detail-kicker">系统管理 / 系统设置</div><h1>系统设置</h1><p>查看 WebAdmin 服务状态、运行环境、安全边界和功能开关。</p></div></div><div class="wa-settings-notice"><strong>只读设置</strong><span>当前仅展示已有服务信息，未实现的写操作保持不可用。</span></div>${actions?`<div class="wa-actions">${actions}</div>`:''}</header>`;
                }
                function settingsInfoGrid(items){
                  return `<div class="wa-settings-info-grid">${(items||[]).map(item=>`<div class="wa-settings-info-item"><span class="wa-settings-mini-icon">${icon(item.icon||'settings')}</span><span><small>${esc(item.label)}</small><strong>${detailValue(item.value)}</strong></span></div>`).join('')}</div>`;
                }
                function settingReadonlyToggle(label,desc,enabled){return `<div class="setting-row"><span><strong>${esc(label)}</strong><small>${esc(desc)}</small></span><span class="wa-switch ${enabled?'is-on':'is-off'}" aria-hidden="true"><i></i></span><em>${esc(enabled?'启用':'禁用')}</em></div>`;}
                function settingsEnvItem(label,value,iconName){return `<div class="wa-settings-env-item"><span>${icon(iconName||'info')}</span><small>${esc(label)}</small><strong>${esc(value||'--')}</strong></div>`;}
                function settingsPercent(value,fallback=0){const n=Number(value);if(!Number.isFinite(n))return fallback;const pct=n>0&&n<=1?n*100:n;return Math.max(0,Math.min(100,Math.round(pct)));}
                function settingsProgressItem(label,value,display,kind=''){const pct=settingsPercent(value,0);return `<div class="wa-progress-item"><div><span>${esc(label)}</span><strong>${esc(display||`${pct}%`)}</strong></div><div class="wa-progress-track"><div class="wa-progress-bar ${esc(kind)}" style="width:${pct}%"></div></div></div>`;}
                function settingsRailLine(label,value,iconName='info'){return `<div class="wa-settings-rail-line"><span class="wa-settings-mini-icon">${icon(iconName)}</span><span><small>${esc(label)}</small><strong>${detailValue(value)}</strong></span></div>`;}
                function settingsRailAction(label,iconName,danger=false){return `<button class="wa-btn ${danger?'danger':'ghost'}" disabled>${icon(iconName)}<span>${esc(label)}</span></button>`;}
                function settingsRightRail(data,status,capabilities){
                  const storage=data.storage||{}, security=data.security||{}, service=data.service||{}, system=data.system||{};
                  const cpu=settingsPercent(status.cpu?.usagePercent ?? status.system?.cpuUsagePercent ?? status.cpuUsagePercent,0);
                  const mem=settingsPercent(status.memory?.usagePercent ?? status.jvm?.memoryUsagePercent ?? status.memoryUsagePercent,0);
                  const disk=settingsPercent(status.disk?.usagePercent ?? storage.diskUsagePercent,0);
                  const tps=status.server?.tps ?? status.tps ?? system.tps ?? '--';
                  const sessionCount=status.webAdmin?.sessionCount ?? system.sessionCount ?? '--';
                  return `<aside class="wa-settings-rail" data-settings-status-rail="true">
                    <article class="wa-panel wa-settings-card"><h2>系统状态</h2><div class="wa-settings-status-list">${settingsRailLine('平台状态',settingsServiceStatus(service),'server-online')}${settingsRailLine('已运行时间',status.uptime||status.server?.uptime||'--','history')}${settingsRailLine('Java 版本',status.javaVersion||status.jvm?.version||system.javaVersion||'--','settings')}${settingsProgressItem('CPU 使用率',cpu,`${cpu}%`,'ok')}${settingsProgressItem('内存使用率',mem,`${mem}%`,'ok')}${settingsProgressItem('磁盘使用率',disk,`${disk}%`,'info')}${settingsRailLine('活动连接数',sessionCount,'session')}${settingsRailLine('TPS',tps,'pulse-duration')}</div></article>
                    <article class="wa-panel wa-settings-card"><h2>环境摘要</h2><div class="wa-settings-status-list">${settingsRailLine('存储作用域',storage.scope||'WORLD_SAVE','archive')}${settingsRailLine('按世界隔离',storage.worldScoped?'是':'否','region')}${settingsRailLine('敏感路径',storage.restricted||security.sensitiveStorageHidden?'已隐藏':'可见','eye')}${settingsRailLine('旧全局文件',storage.legacyGlobalFilesDetected?'检测到':'未检测到','warning-issue')}</div></article>
                    <article class="wa-panel wa-settings-card"><h2>快速操作</h2><div class="wa-settings-action-list">${settingsRailAction('查看系统日志','eye')}${settingsRailAction('查看审计日志','history')}${settingsRailAction('检查更新','refresh')}${settingsRailAction('导出系统信息','download')}${settingsRailAction('重新加载配置','critical-issue',true)}</div><p class="wa-disabled-note">保存、重置、导出、检查更新和重新加载配置没有完整后端安全能力，本轮保持禁用。</p></article>
                  </aside>`;}
                async function renderRegionsPage(options={}){
                  if(!options.silent)setView(loading('正在加载区域列表...'));
                  let regions;try{regions=await api('/api/regions?limit=500')}catch(err){if(options.silent){toast('区域列表实时刷新失败，已保留当前页面。');return;}setView(errorBlock(err.message));return;}
                  appState.regions=regions||[];
                  renderRegionList('',options);
                }
                function renderRegionList(focusId,options={}){
                  waEnsureState();
                  const regions=appState.regions||[], worlds=uniqueNonBlank(regions.map(r=>r.world)), filtered=filterRegionsStep3(regions), page=waPageItems('regions',filtered,10);
                  const enabled=regions.filter(r=>r.enabled).length, warnings=regions.filter(r=>['WARNING','ERROR'].includes(String(r.doctorStatus||'').toUpperCase())).length;
                  const rendered=setView(`<section class="wa-page">
                    ${waPageHead('区域列表','查看已登记区域、坐标边界和当前只读运行状态；不是 RegionController 编辑器。',`${waButton('添加区域','plus','disabled','primary')}${waButton('导入区域配置','upload','disabled','ghost')}${waButton('导出区域配置','download','disabled','ghost')}`)}
                    <section class="wa-card-grid wa-metrics-5">
                      ${waMetric('区域总数',regions.length,'来自 /api/regions','region')}
                      ${waMetric('启用中',enabled,'enabled=true','enabled','ok')}
                      ${waMetric('禁用中',regions.length-enabled,'enabled=false','receiver-disabled',regions.length-enabled?'warning':'')}
                      ${waMetric('今日进入事件','--','当前 API 未提供','today-trigger')}
                      ${waMetric('今日离开事件','--','当前 API 未提供','history')}
                      ${waMetric('有问题区域',warnings,'Doctor 非 OK','warning-issue',warnings?'warning':'')}
                    </section>
                    <section class="wa-two-column">
                      <div class="wa-table-card">
                        <div class="wa-filter-bar">
                          <label class="filter-field search-control"><span>搜索</span><input class="input" id="region-search" placeholder="搜索区域名称 / ID / world / channel..." value="${esc(appState.regionFilters.search)}"></label>
                          <label class="filter-field"><span>世界</span>${waSelect('region-world',['ALL',...worlds],appState.regionFilters.world,v=>v==='ALL'?'全部世界':v)}</label>
                          <label class="filter-field"><span>状态</span>${waSelect('region-enabled',['ALL','ENABLED','DISABLED'],appState.regionFilters.enabled,regionOptionLabel)}</label>
                          <label class="filter-field"><span>Doctor</span>${waSelect('region-doctor',['ALL','OK','INFO','WARNING','ERROR','UNKNOWN'],appState.regionFilters.doctor,regionOptionLabel)}</label>
                          <label class="filter-field"><span>排序</span>${waSelect('region-sort',['NAME','WORLD','PLAYERS','RECENT'],appState.regionFilters.sort,regionOptionLabel)}</label>
                          ${waButton('刷新','refresh','onclick="renderRegionsPage()"','ghost')}
                        </div>
                        ${page.items.length===0?empty(regions.length?'没有匹配当前筛选条件的区域。':'当前暂无区域数据。'):regionTableStep3(page.items)}
                        ${waPagination('regions',page)}
                      </div>
                      ${regionsRightRail(regions)}
                    </section>
                  </section>`,options);
                  if(rendered)bindRegionFiltersStep3(focusId);
                }
                function filterRegionsStep3(items){const f=appState.regionFilters;const filtered=(items||[]).filter(r=>{const hay=[r.id,r.name,r.world,r.boundChannel,r.targetFilter].join(' ').toLowerCase();if(f.search&&!hay.includes(f.search.toLowerCase()))return false;if(f.world!=='ALL'&&r.world!==f.world)return false;if(f.enabled==='ENABLED'&&!r.enabled)return false;if(f.enabled==='DISABLED'&&r.enabled)return false;if(f.doctor!=='ALL'&&String(r.doctorStatus||'UNKNOWN').toUpperCase()!==f.doctor)return false;if(f.players==='HAS_PLAYERS'&&Number(r.playersInside||0)<=0)return false;if(f.players==='NO_PLAYERS'&&Number(r.playersInside||0)>0)return false;return true;});return filtered.sort((a,b)=>{if(f.sort==='WORLD')return String(a.world||'').localeCompare(String(b.world||''))||String(a.name||'').localeCompare(String(b.name||''));if(f.sort==='PLAYERS')return Number(b.playersInside||0)-Number(a.playersInside||0);if(f.sort==='RECENT')return String(b.lastEventAt||'').localeCompare(String(a.lastEventAt||''));return String(a.name||a.id||'').localeCompare(String(b.name||b.id||''));});}
                function bindRegionFiltersStep3(focusId){const update=(event)=>{appState.regionFilters.search=document.getElementById('region-search')?.value||'';appState.regionFilters.world=document.getElementById('region-world')?.value||'ALL';appState.regionFilters.enabled=document.getElementById('region-enabled')?.value||'ALL';appState.regionFilters.doctor=document.getElementById('region-doctor')?.value||'ALL';appState.regionFilters.sort=document.getElementById('region-sort')?.value||'NAME';appState.uiPages.regions=1;renderRegionList(event?.target?.id||'');};['region-search','region-world','region-enabled','region-doctor','region-sort'].forEach(id=>document.getElementById(id)?.addEventListener(id==='region-search'?'input':'change',update));restoreFocusEnd(focusId);}
                function regionTableStep3(items){return `<div class="wa-table-scroll"><table class="wa-table"><thead><tr><th>区域名称 / ID</th><th>类型</th><th>所在世界</th><th>坐标范围</th><th>大小</th><th>状态</th><th>描述</th><th>操作</th></tr></thead><tbody>${items.map(r=>{const target=regionHash(r.id), title=r.name||r.id, type=String(r.type||'').toUpperCase(), desc=r.description||(r.boundChannel?`绑定频道 ${r.boundChannel}`:`目标 ${labelTargetFilter(r.targetFilter)}`);return `<tr class="wa-clickable-row" ${navDataAttr(target,`查看区域 ${title}`)}><td><span class="device-name"><span class="device-icon">${icon('region')}</span><span><strong>${esc(title)}</strong><span class="device-subtitle">ID: ${esc(shortId(r.id))}</span></span></span></td><td>${textPill(type==='POLYGON'?'polygon':(type==='CONTROLLER'?'controller':'region'),'info')}</td><td>${esc(r.world||'--')}</td><td class="truncate" title="${esc(boundsText(r.bounds))}">${esc(boundsText(r.bounds))}</td><td><span>${esc(boundsSize(r.bounds))}</span><span class="device-subtitle">体积 ${esc(boundsVolume(r.bounds))}</span></td><td>${textPill(labelEnabledState(r.enabled),r.enabled?'ok':'warning')} ${pill(r.doctorStatus||'UNKNOWN')}</td><td class="truncate" title="${esc(desc)}">${esc(desc)}</td><td><div class="wa-action-cell"><button class="wa-btn ghost" ${navDataAttr(target,`查看区域 ${title}`)}>查看</button>${waIconButton('更多不可用','more','disabled')}</div></td></tr>`;}).join('')}</tbody></table></div>`;}
                function regionsRightRail(regions){const total=regions.length;return `<aside class="wa-right-rail">
                  <article class="wa-panel"><h2>区域统计</h2><div class="summary-grid">${waMetric('总区域数',total,'','region')}${waMetric('启用中',regions.filter(r=>r.enabled).length,'','enabled','ok')}${waMetric('已禁用',regions.filter(r=>!r.enabled).length,'','receiver-disabled')}${waMetric('多边形区域','--','当前 API 未提供','virtual-block-device')}</div></article>
                  <article class="wa-panel"><h2>Doctor 分布</h2>${progressList(distributionItems(regions,r=>String(r.doctorStatus||'UNKNOWN').toUpperCase(),labelStatus,Math.max(1,total)))}</article>
                  <article class="wa-panel"><h2>快速筛选</h2><div class="wa-rail-filter"><label><span>状态</span>${waSelect('region-rail-enabled',['ALL','ENABLED','DISABLED'],appState.regionFilters.enabled,regionOptionLabel)}</label><button class="wa-btn primary" onclick="appState.regionFilters.enabled=document.getElementById('region-rail-enabled').value;appState.uiPages.regions=1;renderRegionList()">应用筛选</button><button class="wa-btn ghost" onclick="appState.regionFilters={search:'',world:'ALL',enabled:'ALL',doctor:'ALL',players:'ALL',sort:'NAME'};appState.uiPages.regions=1;renderRegionList()">重置筛选</button></div></article>
                  <article class="wa-panel"><h2>快速操作</h2><div class="wa-quick-grid">${waButton('定位区域','eye','disabled','ghost')}${waButton('编辑区域','pencil','disabled','ghost')}${waButton('导出区域配置','download','disabled','ghost')}${waButton('清空禁用区域缓存','critical-issue','disabled','danger')}</div><p class="wa-disabled-note">区域增删改、定位和导入导出没有 WebAdmin 写 API，本轮保持禁用。</p></article>
                </aside>`;}
                async function renderRegionControllersPage(options={}){
                  if(!options.silent)setView(loading('正在加载区域控制器...'));
                  let controllers;try{controllers=await api('/api/regions?limit=500')}catch(err){if(options.silent){toast('区域控制器实时刷新失败，已保留当前页面。');return;}setView(errorBlock(err.message));return;}
                  appState.regionControllers=(controllers||[]).filter(c=>Number(c.controllerCount||0)>0||!isBlank(c.controllerId));
                  renderRegionControllerList('',options);
                }
                function renderRegionControllerList(focusId,options={}){
                  waEnsureState();
                  const items=appState.regionControllers||[], filtered=filterRegionControllers(items), page=waPageItems('regionControllers',filtered,10), enabled=items.filter(c=>c.enabled).length, actionTotal=sumRegionControllerActions(items);
                  const rendered=setView(`<section class="wa-page">
                    ${waPageHead('区域控制器','查看 RegionController enter / exit / stay 触发配置；不实现 ConditionEngine 或动作链编辑。',`${waButton('添加控制器','plus','disabled','primary')}${waButton('导入配置','upload','disabled','ghost')}${waButton('导出配置','download','disabled','danger')}`)}
                    <section class="wa-card-grid wa-metrics-5">
                      ${waMetric('总控制器数',items.length,'来自 /api/regions','region-controller')}
                      ${waMetric('启用中',enabled,'enabled=true','enabled','ok')}
                      ${waMetric('已禁用',items.length-enabled,'enabled=false','receiver-disabled',items.length-enabled?'warning':'')}
                      ${waMetric('关联区域数',uniqueNonBlank(items.map(c=>c.id)).length,'controller id','region')}
                      ${waMetric('今日触发次数','--','当前 API 未提供','today-trigger')}
                      ${waMetric('动作执行次数',actionTotal,'enter / exit / stay','action-total')}
                    </section>
                    <section class="wa-two-column">
                      <div class="wa-table-card">
                        <div class="wa-filter-bar">
                          <label class="filter-field search-control"><span>搜索</span><input class="input" id="region-controller-search" placeholder="搜索控制器 / 区域 / 目标过滤..." value="${esc(appState.regionControllerFilters.search)}"></label>
                          <label class="filter-field"><span>状态</span>${waSelect('region-controller-enabled',['ALL','ENABLED','DISABLED'],appState.regionControllerFilters.enabled,regionOptionLabel)}</label>
                          <label class="filter-field"><span>目标</span>${waSelect('region-controller-target',['ALL','OP','TAG','TEAM','UNKNOWN'],appState.regionControllerFilters.target,regionTargetOptionLabel)}</label>
                          <label class="filter-field"><span>事件</span>${waSelect('region-controller-event',['ALL','ENTER','EXIT','STAY'],appState.regionControllerFilters.event,regionEventOptionLabel)}</label>
                          ${waButton('刷新','refresh','onclick="renderRegionControllersPage()"','ghost')}
                        </div>
                        ${page.items.length===0?empty(items.length?'没有匹配当前筛选条件的控制器。':'当前暂无 RegionController 数据。'):regionControllerTable(page.items)}
                        ${waPagination('regionControllers',page)}
                      </div>
                      ${regionControllerRightRail(items)}
                    </section>
                  </section>`,options);
                  if(rendered)bindRegionControllerFilters(focusId);
                }
                function filterRegionControllers(items){const f=appState.regionControllerFilters;return (items||[]).filter(c=>{const hay=[c.id,c.name,c.world,c.targetFilter,c.boundChannel].join(' ').toLowerCase();if(f.search&&!hay.includes(f.search.toLowerCase()))return false;if(f.enabled==='ENABLED'&&!c.enabled)return false;if(f.enabled==='DISABLED'&&c.enabled)return false;if(f.target!=='ALL'&&String(c.targetFilter||'UNKNOWN').toUpperCase()!==f.target)return false;if(f.event==='ENTER'&&Number(c.enterActionCount||0)<=0)return false;if(f.event==='EXIT'&&Number(c.exitActionCount||0)<=0)return false;if(f.event==='STAY'&&Number(c.stayActionCount||0)<=0)return false;return true;});}
                function bindRegionControllerFilters(focusId){const update=(event)=>{appState.regionControllerFilters.search=document.getElementById('region-controller-search')?.value||'';appState.regionControllerFilters.enabled=document.getElementById('region-controller-enabled')?.value||'ALL';appState.regionControllerFilters.target=document.getElementById('region-controller-target')?.value||'ALL';appState.regionControllerFilters.event=document.getElementById('region-controller-event')?.value||'ALL';appState.uiPages.regionControllers=1;renderRegionControllerList(event?.target?.id||'');};['region-controller-search','region-controller-enabled','region-controller-target','region-controller-event'].forEach(id=>document.getElementById(id)?.addEventListener(id==='region-controller-search'?'input':'change',update));restoreFocusEnd(focusId);}
                function regionControllerTable(items){return `<div class="wa-table-scroll"><table class="wa-table"><thead><tr><th>名称 / ID</th><th>关联区域</th><th>目标过滤</th><th>状态</th><th>事件配置</th><th>动作数</th><th>今日触发</th><th>操作</th></tr></thead><tbody>${items.map(c=>{const target=regionHash(c.id), total=regionControllerActionCount(c);return `<tr><td><span class="device-name"><span class="device-icon">${icon('region-controller')}</span><span><strong>${esc(c.name||c.id)}</strong><span class="device-subtitle">ID: ${esc(shortId(c.id))}</span></span></span></td><td>${regionButton(c.id,c.name||c.id)}<span class="device-subtitle">${esc(c.world||'world 未提供')}</span></td><td>${textPill(labelTargetFilter(c.targetFilter),'info')}</td><td>${textPill(labelEnabledState(c.enabled),c.enabled?'ok':'warning')} ${pill(c.doctorStatus||'UNKNOWN')}</td><td>${controllerEventChips(c)}</td><td>${esc(total)}</td><td>--</td><td><div class="wa-action-cell"><button class="wa-btn ghost" ${navDataAttr(target,`查看关联区域 ${c.name||c.id}`)}>查看区域</button><button class="wa-btn ghost" disabled>编辑</button>${waIconButton('更多不可用','more','disabled')}</div></td></tr>`;}).join('')}</tbody></table></div>`;}
                function controllerEventChips(c){return `<span class="pill info">进 ${esc(c.enterActionCount||0)}</span> <span class="pill ok">出 ${esc(c.exitActionCount||0)}</span> <span class="pill warning">停 ${esc(c.stayActionCount||0)}</span>`;}
                function regionControllerActionCount(c){return Number(c.enterActionCount||0)+Number(c.exitActionCount||0)+Number(c.stayActionCount||0);}
                function sumRegionControllerActions(items){return (items||[]).reduce((sum,item)=>sum+regionControllerActionCount(item),0);}
                function regionControllerRightRail(items){const total=items.length;return `<aside class="wa-right-rail">
                  <article class="wa-panel"><h2>事件类型说明</h2><div class="list-stack"><div class="kv-row"><span class="muted">enter / 进</span><strong>玩家进入区域触发</strong></div><div class="kv-row"><span class="muted">exit / 出</span><strong>玩家离开区域触发</strong></div><div class="kv-row"><span class="muted">stay / 停</span><strong>玩家停留间隔触发</strong></div></div></article>
                  <article class="wa-panel"><h2>触发目标说明</h2><div class="list-stack"><div class="kv-row"><span class="muted">all</span><strong>所有玩家</strong></div><div class="kv-row"><span class="muted">op</span><strong>服务器管理员</strong></div><div class="kv-row"><span class="muted">tag/team</span><strong>标签或队伍过滤</strong></div></div></article>
                  <article class="wa-panel"><h2>事件配置分布</h2>${progressList([{label:'配置 enter',value:items.filter(c=>Number(c.enterActionCount||0)>0).length,total:Math.max(1,total),kind:'info'},{label:'配置 exit',value:items.filter(c=>Number(c.exitActionCount||0)>0).length,total:Math.max(1,total),kind:'ok'},{label:'配置 stay',value:items.filter(c=>Number(c.stayActionCount||0)>0).length,total:Math.max(1,total),kind:'warning'}])}</article>
                  <article class="wa-panel"><h2>快速操作</h2><div class="wa-quick-grid">${waButton('批量启用','enabled','disabled','ghost')}${waButton('批量禁用','receiver-disabled','disabled','ghost')}${waButton('测试 enter','play','disabled','ghost')}${waButton('测试 exit','logout','disabled','ghost')}${waButton('清空今日统计','critical-issue','disabled','danger')}</div><p class="wa-disabled-note">RegionController 新增、编辑、测试和清空统计没有 WebAdmin 写 API，本轮保持禁用。</p></article>
                </aside>`;}
                function regionTargetOptionLabel(value){return {ALL:'全部目标',OP:'管理员',TAG:'标签过滤',TEAM:'队伍过滤',UNKNOWN:'未知'}[String(value||'')]||value;}
                function regionEventOptionLabel(value){return {ALL:'全部事件',ENTER:'进入 enter',EXIT:'离开 exit',STAY:'停留 stay'}[String(value||'')]||value;}
                function progressList(items){
                  if(!items||items.length===0)return empty('暂无数据。');
                  return `<div class="wa-progress-list">${items.map(item=>{const total=Math.max(1,Number(item.total||item.value||0)), pct=Math.min(100,Math.round(Number(item.value||0)*100/total));return `<div class="wa-progress-item"><div><span>${esc(item.label)}</span><strong>${esc(item.value)}</strong></div><div class="wa-progress-track"><div class="wa-progress-bar ${esc(item.kind||'')}" style="width:${pct}%"></div></div></div>`;}).join('')}</div>`;
                }
                function renderPlaceholder(title,message){setView(`<div class="page-head"><div><h1>${esc(title)}</h1><p>${esc(message)}</p></div></div>${empty('该模块将在后续版本接入。')}`)}
                initLogin();initApp();
                """).toString();
    }
}
