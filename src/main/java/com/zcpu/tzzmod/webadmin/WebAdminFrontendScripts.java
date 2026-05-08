package com.zcpu.tzzmod.webadmin;

public final class WebAdminFrontendScripts {
    private static final String[] FLAT_ICON_KEYS = {
            "logo", "dashboard", "signalbridge-main", "receiver-main", "history", "doctor", "region", "device",
            "action", "user", "settings", "server-online", "logout", "device-overview", "doctor-overview", "signal-overview",
            "region-overview", "action-overview", "user-overview", "signal-device", "signal-receiver", "virtual-block-device", "action-relay", "critical-issue",
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
                const appState={me:null,status:null,capabilities:null,channelOptions:null,channelOptionsError:null,deviceMetadataEdit:null,deviceMetadataLockTimer:null,deviceBasicConfigEdit:null,deviceBasicConfigLockTimer:null,deviceExtendedConfigEdit:null,deviceExtendedConfigLockTimer:null,channelMetadataEdit:null,channelMetadataLockTimer:null,signalListenerBasicConfigEdit:null,signalListenerBasicConfigLockTimer:null,deviceFilters:{search:'',type:'ALL',enabled:'ALL',doctor:'ALL',world:'ALL'},signalFilters:{search:'',consumer:'ALL',status:'ALL',sort:'RECENT'},doctorFilters:{search:'',severity:'ALL',objectType:'ALL',jump:'ALL'},historyFilters:{search:'',channel:'ALL',sourceType:'ALL',result:'ALL',range:'ALL',sort:'NEWEST'},userFilters:{search:'',role:'ALL',enabled:'ALL',online:'ALL'},regionFilters:{search:'',world:'ALL',enabled:'ALL',doctor:'ALL',players:'ALL',sort:'NAME'},actionFilters:{search:'',type:'ALL',owner:'ALL',result:'ALL',doctor:'ALL',sort:'NAME'}};
                appState.realtime={source:null,status:'DISCONNECTED',reconnectTimer:null,reconnectAttempt:0,lastEventAt:'',lastSeenSeq:0,lastEventId:'',wasDisconnected:false,missed:false,offline:typeof navigator!=='undefined'&&!navigator.onLine,refreshTimers:{},dirtyRoutes:{},pendingRefresh:{},refreshSeq:{},pollTimer:null,pollHash:null};
                function esc(value){return String(value ?? '').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));}
                function isBlank(value){return value===undefined||value===null||String(value).trim()==='';}
                function iconKey(name){
                  const raw=String(name||'').trim().toLowerCase().replace(/_/g,'-');
                  const compact=raw.replace(/[^a-z0-9]/g,'');
                  const aliases={
                    signal:'signalbridge-main',signalbridge:'signalbridge-main',channel:'active-channel',broadcast:'signal-overview',
                    receiver:'receiver-main',listener:'consumer-listener',relay:'action-relay',virtual:'virtual-block-device',auto:'device',
                    cube:'device-overview',deviceoverview:'device-overview',doctoroverview:'doctor-overview',signaloverview:'signal-overview',regionoverview:'region-overview',actionoverview:'action-overview',useroverview:'user-overview',
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
                function hydrateIcons(){
                  if(Object.keys(ICON_GEOMETRY).length===0)document.documentElement.classList.add('flat-icons-missing');
                  document.querySelectorAll('[data-icon]').forEach(el=>{el.innerHTML=icon(el.dataset.icon);});
                }
                function statusClass(value){const v=String(value||'').toUpperCase();if(v==='ERROR'||v==='FAILED')return'error';if(v==='WARNING')return'warning';if(v==='INFO'||v==='UNKNOWN')return'info';return'ok';}
                function pill(value){return `<span class="pill ${statusClass(value)}">${esc(labelStatus(value))}</span>`}
                function textPill(text, kind='info'){return `<span class="pill ${esc(kind)}">${esc(text)}</span>`}
                function labelStatus(value){const v=String(value||'UNKNOWN').toUpperCase();return {OK:'正常',INFO:'信息',WARNING:'警告',ERROR:'错误',UNKNOWN:'未知',SUCCESS:'成功',FAILED:'失败',SKIPPED:'跳过'}[v]||value;}
                function labelBool(value){return value?'已启用':'已禁用';}
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
                  if(!force&&Array.isArray(appState.channelOptions))return appState.channelOptions;
                  try{appState.channelOptions=await api('/api/signals/channels');appState.channelOptionsError=null;}
                  catch(err){appState.channelOptions=Array.isArray(appState.channelOptions)?appState.channelOptions:[];appState.channelOptionsError=err.message||'频道候选加载失败';}
                  return appState.channelOptions||[];
                }
                function normalizeChannelName(value){return String(value||'').trim();}
                function findChannelOption(channel,options){const name=normalizeChannelName(channel).toLowerCase();if(!name)return null;return (options||[]).find(c=>String(c.channel||'').trim().toLowerCase()===name)||null;}
                function channelOptionLabel(c){const parts=[`消费者：${consumerCount(c)}`];if(!isBlank(c?.lastTriggeredAt))parts.push(`最近触发：${formatDateTime(c.lastTriggeredAt)}`);if(!isBlank(c?.doctorStatus))parts.push(`诊断：${labelStatus(c.doctorStatus)}`);return parts.join(' · ');}
                function filteredChannelOptions(options,channel){const query=normalizeChannelName(channel).toLowerCase();const list=(options||[]).filter(c=>!query||String(c.channel||'').toLowerCase().includes(query));return list.slice(0,50);}
                function channelComboOptionsHtml(deviceId,draft){
                  if(draft.channelOptionsError||appState.channelOptionsError)return '<div class="channel-combo-empty">频道候选加载失败，仍可手动输入新的频道名。</div>';
                  const options=filteredChannelOptions(draft.channelOptions||appState.channelOptions||[],draft.channel), current=normalizeChannelName(draft.channel).toLowerCase(), active=Math.max(0,Number(draft.channelComboIndex||0));
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
                  if(openMenu){draft.channelComboOpen=true;draft.channelComboIndex=0;}
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
                function openDeviceBasicConfigChannelMenu(deviceId){const draft=appState.deviceBasicConfigEdit;if(!draft||draft.deviceId!==deviceId)return;updateDeviceBasicConfigDraftFromForm(deviceId,false);draft.channelComboOpen=true;syncDeviceBasicConfigChannelCombo(deviceId);}
                function closeDeviceBasicConfigChannelMenu(deviceId){const draft=appState.deviceBasicConfigEdit;if(!draft||draft.deviceId!==deviceId)return;draft.channelComboOpen=false;syncDeviceBasicConfigChannelCombo(deviceId);}
                function toggleDeviceBasicConfigChannelMenu(deviceId){const draft=appState.deviceBasicConfigEdit;if(!draft||draft.deviceId!==deviceId)return;updateDeviceBasicConfigDraftFromForm(deviceId,false);draft.channelComboOpen=!draft.channelComboOpen;syncDeviceBasicConfigChannelCombo(deviceId);document.getElementById('basic-channel')?.focus();}
                function selectDeviceBasicConfigChannel(deviceId,channel){const draft=appState.deviceBasicConfigEdit;if(!draft||draft.deviceId!==deviceId)return;draft.channel=channel||'';draft.channelComboOpen=false;draft.channelComboIndex=0;const input=document.getElementById('basic-channel');if(input)input.value=draft.channel;const hint=document.getElementById('basic-channel-hint');if(hint)hint.innerHTML=channelHintHtml(draft.channel,draft.channelOptions||appState.channelOptions||[],draft.channelOptionsError||appState.channelOptionsError);syncDeviceBasicConfigChannelCombo(deviceId);}
                function handleDeviceBasicConfigChannelKey(event,deviceId){
                  const draft=appState.deviceBasicConfigEdit;if(!draft||draft.deviceId!==deviceId)return;
                  const options=filteredChannelOptions(draft.channelOptions||appState.channelOptions||[],document.getElementById('basic-channel')?.value||draft.channel);
                  if(event.key==='Escape'){draft.channelComboOpen=false;syncDeviceBasicConfigChannelCombo(deviceId);return;}
                  if(event.key==='ArrowDown'||event.key==='ArrowUp'){event.preventDefault();draft.channelComboOpen=true;const max=Math.max(0,options.length-1), next=event.key==='ArrowDown'?Number(draft.channelComboIndex||0)+1:Number(draft.channelComboIndex||0)-1;draft.channelComboIndex=Math.min(max,Math.max(0,next));syncDeviceBasicConfigChannelCombo(deviceId);return;}
                  if(event.key==='Enter'&&draft.channelComboOpen&&options.length>0){event.preventDefault();selectDeviceBasicConfigChannel(deviceId,options[Math.min(options.length-1,Number(draft.channelComboIndex||0))].channel);return;}
                }
                function extendedFieldId(field){return String(field||'').replace(/[^a-zA-Z0-9_-]/g,'_');}
                function isExtendedChannelField(field){return ['interactChannel','successChannel','failChannel'].includes(String(field||''));}
                function isExtendedTickField(field){return ['interactionCooldownTicks','pulseTicks','cooldownTicks'].includes(String(field||''));}
                function extendedChannelComboOptionsHtml(deviceId,field,draft){
                  if(draft.channelOptionsError||appState.channelOptionsError)return '<div class="channel-combo-empty">频道候选加载失败，仍可手动输入新的频道名。</div>';
                  const value=(draft.values||{})[field]||'', options=filteredChannelOptions(draft.channelOptions||appState.channelOptions||[],value), current=normalizeChannelName(value).toLowerCase(), indexes=draft.channelComboIndex||{}, active=Math.max(0,Number(indexes[field]||0));
                  if(options.length===0)return '<div class="channel-combo-empty">没有匹配的已有频道，可直接保存为新频道</div>';
                  return options.map((c,index)=>`<button type="button" class="channel-combo-option ${index===active?'active':''} ${String(c.channel||'').trim().toLowerCase()===current?'selected':''}" role="option" onmousedown="event.preventDefault()" onclick='selectDeviceExtendedConfigChannel(${jsString(deviceId)},${jsString(field)},${jsString(c.channel||'')})'><strong>${esc(c.channel||'未命名频道')}</strong><span>${esc(channelOptionLabel(c))}</span></button>`).join('');
                }
                function renderDeviceExtendedConfigChannelCombo(deviceId,field,draft){
                  const id=extendedFieldId(field), open=(draft.channelComboOpen||{})[field]?' open':'', value=(draft.values||{})[field]||'';
                  return `<div id="extended-channel-combo-${id}" class="channel-combo extended-channel-combo${open}"><div class="channel-combo-control"><input id="extended-${id}" class="input" maxlength="128" value="${esc(value)}" placeholder="选择已有频道或输入新频道" autocomplete="off" role="combobox" aria-expanded="${(draft.channelComboOpen||{})[field]?'true':'false'}" aria-controls="extended-${id}-menu" onfocus='openDeviceExtendedConfigChannelMenu(${jsString(deviceId)},${jsString(field)})' oninput='updateDeviceExtendedConfigDraftFromForm(${jsString(deviceId)},${jsString(field)})' onkeydown='handleDeviceExtendedConfigChannelKey(event,${jsString(deviceId)},${jsString(field)})'><button class="channel-combo-toggle" type="button" onclick='toggleDeviceExtendedConfigChannelMenu(${jsString(deviceId)},${jsString(field)})' aria-label="显示已有频道">⌄</button></div><div id="extended-${id}-menu" class="channel-combo-menu" role="listbox">${extendedChannelComboOptionsHtml(deviceId,field,draft)}</div></div>`;
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
                  if(openField){draft.channelComboOpen[openField]=true;draft.channelComboIndex[openField]=0;}
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
                function openDeviceExtendedConfigChannelMenu(deviceId,field){const draft=appState.deviceExtendedConfigEdit;if(!draft||draft.deviceId!==deviceId)return;updateDeviceExtendedConfigDraftFromForm(deviceId,'');draft.channelComboOpen=draft.channelComboOpen||{};draft.channelComboOpen[field]=true;syncDeviceExtendedConfigChannelCombo(deviceId,field);}
                function closeDeviceExtendedConfigChannelMenu(deviceId,field){const draft=appState.deviceExtendedConfigEdit;if(!draft||draft.deviceId!==deviceId)return;draft.channelComboOpen=draft.channelComboOpen||{};draft.channelComboOpen[field]=false;syncDeviceExtendedConfigChannelCombo(deviceId,field);}
                function toggleDeviceExtendedConfigChannelMenu(deviceId,field){const draft=appState.deviceExtendedConfigEdit;if(!draft||draft.deviceId!==deviceId)return;updateDeviceExtendedConfigDraftFromForm(deviceId,'');draft.channelComboOpen=draft.channelComboOpen||{};draft.channelComboOpen[field]=!draft.channelComboOpen[field];syncDeviceExtendedConfigChannelCombo(deviceId,field);document.getElementById(`extended-${extendedFieldId(field)}`)?.focus();}
                function selectDeviceExtendedConfigChannel(deviceId,field,channel){const draft=appState.deviceExtendedConfigEdit;if(!draft||draft.deviceId!==deviceId)return;draft.values=draft.values||{};draft.clear=draft.clear||{};draft.channelComboOpen=draft.channelComboOpen||{};draft.channelComboIndex=draft.channelComboIndex||{};draft.values[field]=channel||'';draft.clear[field]=false;draft.channelComboOpen[field]=false;draft.channelComboIndex[field]=0;const id=extendedFieldId(field), input=document.getElementById(`extended-${id}`), clear=document.getElementById(`extended-clear-${id}`);if(input)input.value=draft.values[field];if(clear)clear.checked=false;const hint=document.getElementById(`extended-${id}-hint`);if(hint)hint.innerHTML=channelHintHtml(draft.values[field],draft.channelOptions||appState.channelOptions||[],draft.channelOptionsError||appState.channelOptionsError);syncDeviceExtendedConfigChannelCombo(deviceId,field);}
                function handleDeviceExtendedConfigChannelKey(event,deviceId,field){
                  const draft=appState.deviceExtendedConfigEdit;if(!draft||draft.deviceId!==deviceId)return;
                  const options=filteredChannelOptions(draft.channelOptions||appState.channelOptions||[],document.getElementById(`extended-${extendedFieldId(field)}`)?.value||(draft.values||{})[field]);
                  draft.channelComboOpen=draft.channelComboOpen||{};draft.channelComboIndex=draft.channelComboIndex||{};
                  if(event.key==='Escape'){draft.channelComboOpen[field]=false;syncDeviceExtendedConfigChannelCombo(deviceId,field);return;}
                  if(event.key==='ArrowDown'||event.key==='ArrowUp'){event.preventDefault();draft.channelComboOpen[field]=true;const max=Math.max(0,options.length-1), next=event.key==='ArrowDown'?Number(draft.channelComboIndex[field]||0)+1:Number(draft.channelComboIndex[field]||0)-1;draft.channelComboIndex[field]=Math.min(max,Math.max(0,next));syncDeviceExtendedConfigChannelCombo(deviceId,field);return;}
                  if(event.key==='Enter'&&draft.channelComboOpen[field]&&options.length>0){event.preventDefault();selectDeviceExtendedConfigChannel(deviceId,field,options[Math.min(options.length-1,Number(draft.channelComboIndex[field]||0))].channel);return;}
                }
                function maybeReleaseDeviceExtendedConfigEditForRoute(hash){const draft=appState.deviceExtendedConfigEdit;if(!draft)return;if(String(hash||'').startsWith('#/devices/')){const info=detailRoute(String(hash).substring('#/devices/'.length),'#/devices');if(info.id===draft.deviceId)return;}releaseDeviceExtendedConfigLock(draft,true);appState.deviceExtendedConfigEdit=null;stopDeviceExtendedConfigLockHeartbeat();}
                async function startDeviceExtendedConfigEdit(deviceId){if(!canEditDeviceExtendedConfig())return;try{const cfg=await api(`/api/webadmin/device-extended-config/${encodeURIComponent(deviceId)}`);if(cfg.supported===false||!(cfg.supportedFields||[]).length){toast(cfg.unsupportedReason||'该设备类型暂无可编辑扩展配置。');return;}const result=await api('/api/webadmin/edit-locks/acquire',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'device_extended_config',targetId:deviceId})});if(!result.success){toast(result.message||'无法获取编辑锁');await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});return;}const lock=result.data?.lock||{}, channelOptions=await loadSignalChannelOptions();appState.deviceExtendedConfigEdit={deviceId,values:{...(cfg.values||{})},originalValues:{...(cfg.values||{})},supportedFields:[...(cfg.supportedFields||[])],fieldLabels:{...(cfg.fieldLabels||{})},clearableFields:{...(cfg.clearableFields||{})},clear:{},channelOptions,channelOptionsError:appState.channelOptionsError,channelComboOpen:{},channelComboIndex:{},expectedFingerprint:cfg.expectedFingerprint||'',lockId:lock.lockId||'',lock,errors:[],saving:false,conflict:null};scheduleDeviceExtendedConfigLockHeartbeat();await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});}catch(err){toast(err.message||'无法获取编辑锁');}}
                async function cancelDeviceExtendedConfigEdit(deviceId){const draft=appState.deviceExtendedConfigEdit;if(draft&&draft.deviceId===deviceId){await releaseDeviceExtendedConfigLock(draft,false);appState.deviceExtendedConfigEdit=null;stopDeviceExtendedConfigLockHeartbeat();}await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});}
                async function reloadDeviceExtendedConfigAfterConflict(deviceId){const draft=appState.deviceExtendedConfigEdit;if(draft&&draft.deviceId===deviceId)await releaseDeviceExtendedConfigLock(draft,true);appState.deviceExtendedConfigEdit=null;stopDeviceExtendedConfigLockHeartbeat();await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});}
                function scheduleDeviceExtendedConfigLockHeartbeat(){stopDeviceExtendedConfigLockHeartbeat();appState.deviceExtendedConfigLockTimer=setTimeout(async()=>{await heartbeatDeviceExtendedConfigLock();if(appState.deviceExtendedConfigEdit)scheduleDeviceExtendedConfigLockHeartbeat();},30000);}
                function stopDeviceExtendedConfigLockHeartbeat(){if(appState.deviceExtendedConfigLockTimer){clearTimeout(appState.deviceExtendedConfigLockTimer);appState.deviceExtendedConfigLockTimer=null;}}
                async function heartbeatDeviceExtendedConfigLock(){const draft=appState.deviceExtendedConfigEdit;if(!draft||!draft.lockId)return;try{const result=await api('/api/webadmin/edit-locks/heartbeat',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'device_extended_config',targetId:draft.deviceId,lockId:draft.lockId})});if(result.success){draft.lock=result.data?.lock||draft.lock;appState.deviceExtendedConfigEdit=draft;return;}draft.errors=[{message:result.message||'编辑锁续期失败'}];appState.deviceExtendedConfigEdit=draft;stopDeviceExtendedConfigLockHeartbeat();await renderDeviceDetail(currentDeviceRouteArg(draft.deviceId),{silent:true});}catch(err){draft.errors=[{message:err.message||'编辑锁续期失败'}];appState.deviceExtendedConfigEdit=draft;stopDeviceExtendedConfigLockHeartbeat();}}
                async function releaseDeviceExtendedConfigLock(draft,silent){if(!draft||!draft.lockId)return;try{await api('/api/webadmin/edit-locks/release',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'device_extended_config',targetId:draft.deviceId,lockId:draft.lockId})});}catch(err){if(!silent)toast(err.message||'编辑锁释放失败，将等待自动过期。');}}
                function deviceExtendedConfigPatchBody(draft){const body={expectedFingerprint:draft.expectedFingerprint||'',lockId:draft.lockId||''};const original=draft.originalValues||{};const addChannel=(field,value,clearName,valueName)=>{if((draft.clear||{})[field]){body[clearName]=true;return;}const text=String(value??'').trim(), before=String(original[field]??'').trim();if(text||before)body[valueName]=value||'';};(draft.supportedFields||[]).forEach(field=>{const value=(draft.values||{})[field];if(field==='interactChannel')addChannel(field,value,'clearInteractChannel','interactChannel');else if(field==='successChannel')addChannel(field,value,'clearSuccessChannel','successChannel');else if(field==='failChannel')addChannel(field,value,'clearFailChannel','failChannel');else if(field==='interactionCooldownTicks')body.interactionCooldownTicks=Number(value);else if(field==='pulseTicks')body.pulseTicks=Number(value);else if(field==='cooldownTicks')body.cooldownTicks=Number(value);});return body;}
                async function saveDeviceExtendedConfig(deviceId){const draft=appState.deviceExtendedConfigEdit||{deviceId};updateDeviceExtendedConfigDraftFromForm(deviceId);draft.saving=true;draft.errors=[];draft.conflict=null;appState.deviceExtendedConfigEdit=draft;renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});try{const result=await api(`/api/webadmin/device-extended-config/${encodeURIComponent(deviceId)}`,{method:'PATCH',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify(deviceExtendedConfigPatchBody(draft))});if(result.success){appState.deviceExtendedConfigEdit=null;stopDeviceExtendedConfigLockHeartbeat();toast(result.changed?(result.message||'设备扩展配置已保存。'):'没有变更。');await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});return;}draft.saving=false;draft.errors=result.validationErrors&&result.validationErrors.length?result.validationErrors:[{message:result.message||'保存失败'}];draft.conflict=result.conflict||null;appState.deviceExtendedConfigEdit=draft;if(['edit_lock_expired','edit_lock_conflict','edit_lock_required'].includes(result.code))stopDeviceExtendedConfigLockHeartbeat();toast(result.message||'保存失败');await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});}catch(err){draft.saving=false;draft.errors=[{message:err.message||'保存失败'}];appState.deviceExtendedConfigEdit=draft;toast(err.message||'保存失败');await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});}}
                function signalHash(channel){return `#/signals/${encodeURIComponent(channel||'')}`;}
                function historyHash(channel){return isBlank(channel)?'#/history':`#/history?channel=${encodeURIComponent(channel)}`;}
                function currentRouteHash(){return decodeURIComponent(location.hash||'#/dashboard');}
                function isDetailHash(hash){const h=String(hash||'');return h.startsWith('#/devices/')||h.startsWith('#/signals/')||h.startsWith('#/regions/')||h.startsWith('#/actions/');}
                function isValidReturnHash(hash){const h=String(hash||'');if(!h.startsWith('#/'))return false;if(h.startsWith('#/login'))return false;if(h.includes('://'))return false;return ['#/dashboard','#/devices','#/virtual-block-devices','#/block-devices','#/receivers','#/listeners','#/signal-listeners','#/signals','#/signalbridge','#/doctor','#/history','#/events','#/users','#/permissions','#/users-permissions','#/settings','#/system-settings','#/config','#/config-management','#/settings/config','#/regions','#/region-list','#/region-controllers','#/regionctl','#/actions'].some(prefix=>h===prefix||h.startsWith(prefix+'/')||h.startsWith(prefix+'?'));}
                function withReturnContext(targetHash){const target=String(targetHash||'#/dashboard');if(!isDetailHash(target))return target;const source=currentRouteHash();if(!isValidReturnHash(source))return target;return `${target}${target.includes('?')?'&':'?'}returnTo=${encodeURIComponent(source)}`;}
                function navigateTo(targetHash){location.hash=withReturnContext(targetHash);}
                function detailRoute(raw,fallback){const text=String(raw||''), index=text.indexOf('?'), id=index>=0?text.substring(0,index):text, query=index>=0?text.substring(index+1):'', params=new URLSearchParams(query);const returnTo=params.get('returnTo')||'';return {id, fallback, returnTo:isValidReturnHash(returnTo)?returnTo:''};}
                function goBackOrFallback(returnTo,fallback){location.hash=isValidReturnHash(returnTo)?returnTo:fallback;}
                function jsString(value){return JSON.stringify(String(value||''));}
                function htmlHandler(script){return `onclick="${esc(script)}"`;}
                function navigationAttr(target,stop=true){return htmlHandler(`${stop?'event.stopPropagation();':''}navigateTo(${jsString(target)})`);}
                function navDataAttr(target,label='打开详情'){if(isBlank(target))return 'aria-disabled="true"';return `data-nav-route="${esc(target)}" role="link" tabindex="0" aria-label="${esc(label)}"`;}
                function activateNavRoute(el){const target=el&&el.dataset?el.dataset.navRoute:'';if(!target)return;navigateTo(target);}
                function backButton(route,fallbackLabel){const label=route.returnTo?'返回上一页':fallbackLabel;return `<button class="secondary" ${htmlHandler(`goBackOrFallback(${jsString(route.returnTo)},${jsString(route.fallback)})`)}>${esc(label)}</button>`}
                function channelButton(channel){if(isBlank(channel))return '<span class="muted">未设置</span>';return `<button class="link-button" ${navigationAttr(signalHash(channel))}>${esc(channel)}</button>`}
                function navigationButton(target,label){if(isBlank(target))return esc(label||'-');if(String(target).startsWith('device:'))return `<button class="link-button" ${navigationAttr('#/devices/'+encodeURIComponent(String(target).substring(7)))}>${esc(label)}</button>`;if(String(target).startsWith('channel:'))return `<button class="link-button" ${navigationAttr(signalHash(String(target).substring(8)))}>${esc(label)}</button>`;if(String(target).startsWith('region:'))return `<button class="link-button" ${navigationAttr('#/regions/'+encodeURIComponent(String(target).substring(7)))}>${esc(label)}</button>`;if(String(target).startsWith('action:'))return `<button class="link-button" ${navigationAttr('#/actions/'+encodeURIComponent(String(target).substring(7)))}>${esc(label)}</button>`;return esc(label||target);}
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
                  const hash=decodeURIComponent(location.hash || '#/dashboard');
                  maybeReleaseDeviceMetadataEditForRoute(hash);
                  maybeReleaseDeviceBasicConfigEditForRoute(hash);
                  maybeReleaseDeviceExtendedConfigEditForRoute(hash);
                  maybeReleaseChannelMetadataEditForRoute(hash);
                  maybeReleaseSignalListenerBasicConfigEditForRoute(hash);
                  document.querySelectorAll('.nav-item').forEach(btn=>btn.classList.toggle('active', isRouteActive(btn.dataset.route,hash)));
                  enterRealtimeRoute(hash);
                  if(hash==='#/dashboard') return renderDashboard(options);
                  if(hash==='#/devices') return renderDevices(options);
                  if(hash.startsWith('#/devices/')) return renderDeviceDetail(hash.substring('#/devices/'.length),options);
                  if(hash==='#/virtual-block-devices'||hash==='#/block-devices') return renderVirtualBlockDevices(options);
                  if(hash==='#/listeners'||hash==='#/signal-listeners') return renderListeners(options);
                  if(hash==='#/receivers') return renderReceivers(options);
                  if(hash==='#/signals'||hash==='#/signalbridge') return renderSignals(options);
                  if(hash.startsWith('#/signals/')) return renderSignalDetail(hash.substring('#/signals/'.length),options);
                  if(hash==='#/doctor') return renderDoctorPage(options);
                  if(hash.startsWith('#/history')) return renderHistoryPage(hash.substring('#/history'.length),options);
                  if(hash.startsWith('#/events')) return renderHistoryPage(hash.substring('#/events'.length),options);
                  if(hash==='#/config'||hash==='#/config-management'||hash==='#/settings/config') return renderConfigPage(options);
                  if(hash==='#/users'||hash==='#/permissions'||hash==='#/users-permissions') return renderUsersPage(options);
                  if(hash==='#/settings'||hash==='#/system-settings') return renderSettingsPage(options);
                  if(hash==='#/regions'||hash==='#/region-list') return renderRegionsPage(options);
                  if(hash==='#/region-controllers'||hash==='#/regionctl') return renderRegionControllersPage(options);
                  if(hash.startsWith('#/regions/')) return renderRegionDetail(hash.substring('#/regions/'.length),options);
                  if(hash==='#/actions') return renderActionsPage(options);
                  if(hash.startsWith('#/actions/')) return renderActionDetail(hash.substring('#/actions/'.length),options);
                  renderPlaceholder('页面暂未接入','该页面将在后续版本接入。');
                }
                function isRouteActive(route,hash){
                  const r=String(route||''), h=String(hash||'#/dashboard');
                  if(!r)return false;
                  if(r==='#/signals')return h==='#/signals'||h==='#/signalbridge'||h.startsWith('#/signals/');
                  if(r==='#/listeners')return h==='#/listeners'||h==='#/signal-listeners';
                  if(r==='#/history')return h.startsWith('#/history')||h.startsWith('#/events');
                  if(r==='#/devices')return h==='#/devices'||h.startsWith('#/devices/');
                  if(r==='#/virtual-block-devices')return h==='#/virtual-block-devices'||h==='#/block-devices';
                  if(r==='#/actions')return h==='#/actions'||h.startsWith('#/actions/');
                  if(r==='#/regions')return h==='#/regions'||h==='#/region-list'||h.startsWith('#/regions/');
                  if(r==='#/region-controllers')return h==='#/region-controllers'||h==='#/regionctl';
                  if(r==='#/users')return h==='#/users'||h==='#/permissions'||h==='#/users-permissions';
                  if(r==='#/settings')return h==='#/settings'||h==='#/system-settings';
                  if(r==='#/config')return h==='#/config'||h==='#/config-management'||h==='#/settings/config';
                  return h===r||h.startsWith(r+'/')||h.startsWith(r+'?');
                }
                async function settle(path){try{return{ok:true,data:await api(path)}}catch(err){return{ok:false,error:err}}}
                const REALTIME_EVENT_TYPES=['realtime_connected','heartbeat','sync_required','device_registered','device_removed','device_changed','device_config_changed','device_metadata_changed','receiver_changed','receiver_pulse_changed','virtual_block_device_changed','signal_channel_changed','signal_emitted','signal_history_appended','history_appended','signal_listener_changed','signal_listener_enabled_changed','signal_listener_action_changed','action_changed','action_history_appended','action_execution_appended','region_changed','region_controller_changed','region_event_appended','doctor_issues_changed','webadmin_user_changed','webadmin_audit_appended','webadmin_settings_changed','device_updated','doctor_changed','action_executed','receiver_pulse','region_event','config_changed','write_audit_appended','permission_denied','validation_failed','user_changed','system_settings_changed','signal_config_changed','channel_metadata_changed','signal_listener_config_changed','region_config_changed','action_config_changed','edit_lock_changed','webadmin_user_connected','webadmin_user_disconnected'];
                const REALTIME_KNOWN_ROUTE_KEYS=['dashboard','signals','receivers','listeners','actions','devices','virtualBlockDevices','history','doctor','regions','regionControllers','users','settings','config'];
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
                    flushVisibleRealtimeRefresh(data.type==='sync_required'?'sync_required':'seq_gap');
                    return;
                  }
                  if(data.type==='heartbeat'||data.type==='realtime_connected')return;
                  markRealtimeRoutesForEvent(data);
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
                  if(String(hash||'').startsWith('#/signals/'))return event.channel===routeDetailId(hash,'#/signals/')||(type==='edit_lock_changed'&&['channel_metadata','signal_listener_basic_config'].includes(String(event.payload?.targetType||'')));
                  if(String(hash||'').startsWith('#/devices/'))return !!event.deviceId&&event.deviceId===routeDetailId(hash,'#/devices/');
                  if(String(hash||'').startsWith('#/regions/'))return !!event.regionId&&event.regionId===routeDetailId(hash,'#/regions/');
                  if(String(hash||'').startsWith('#/actions/'))return !!event.actionId&&event.actionId===routeDetailId(hash,'#/actions/');
                  return false;
                }
                function markRealtimeRoutesForEvent(event){
                  const keys=realtimeRouteKeysForEvent(event);
                  keys.forEach(key=>markRealtimeRouteKeyDirty(key,event));
                }
                function realtimeRouteKeysForEvent(event){
                  const type=String(event?.type||''), keys=new Set();
                  const add=(...items)=>items.filter(Boolean).forEach(item=>keys.add(item));
                  const starts=(...prefixes)=>prefixes.some(prefix=>type.startsWith(prefix));
                  const isAny=(...items)=>items.includes(type);
                  if(isAny('config_changed'))add(...REALTIME_KNOWN_ROUTE_KEYS);
                  if(starts('device_')||starts('receiver_')||starts('virtual_block_device_')||isAny('device_updated','receiver_pulse','device_config_changed')){
                    add('dashboard','devices','receivers','virtualBlockDevices','doctor');
                    if(isAny('device_config_changed'))add('config');
                    if(event?.deviceId)add(`deviceDetail:${event.deviceId}`);
                  }
                  if(starts('signal_')||isAny('signal_emitted','history_appended','signal_history_appended','channel_metadata_changed','signal_listener_config_changed','signal_config_changed')){
                    add('dashboard','signals','listeners','history','doctor');
                    if(isAny('signal_listener_changed','signal_listener_enabled_changed','signal_listener_action_changed','signal_listener_config_changed','signal_config_changed'))add('config');
                    if(event?.channel)add(`signalDetail:${event.channel}`);
                  }
                  if(starts('action_')||isAny('action_executed','action_config_changed')){
                    add('dashboard','actions','history');
                    if(isAny('action_changed','action_config_changed'))add('config','regionControllers');
                    if(event?.actionId)add(`actionDetail:${event.actionId}`);
                  }
                  if(starts('region_')||isAny('region_event','region_config_changed')){
                    add('dashboard','regions','regionControllers','history');
                    if(isAny('region_changed','region_config_changed','region_controller_changed'))add('config');
                    if(event?.regionId)add(`regionDetail:${event.regionId}`);
                  }
                  if(starts('doctor_')||isAny('doctor_changed'))add('dashboard','doctor','settings');
                  if(starts('webadmin_user_')||isAny('webadmin_user_connected','webadmin_user_disconnected','user_changed'))add('dashboard','users');
                  if(starts('webadmin_audit_')||isAny('write_audit_appended'))add('dashboard','history','settings','config','users');
                  if(starts('webadmin_settings_')||isAny('system_settings_changed'))add('settings','config','dashboard');
                  if(type==='edit_lock_changed'){
                    const target=String(event?.payload?.targetType||'');
                    if(target.includes('device'))add('devices');
                    if(target.includes('channel'))add('signals');
                    if(target.includes('listener'))add('listeners','signals');
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
                  if(h.startsWith('#/regions/'))return `regionDetail:${routeDetailId(h,'#/regions/')}`;
                  if(h.startsWith('#/actions/'))return `actionDetail:${routeDetailId(h,'#/actions/')}`;
                  if(h.startsWith('#/history')||h.startsWith('#/events'))return 'history';
                  if(h==='#/dashboard')return 'dashboard';
                  if(h==='#/devices')return 'devices';
                  if(h==='#/virtual-block-devices'||h==='#/block-devices')return 'virtualBlockDevices';
                  if(h==='#/listeners'||h==='#/signal-listeners')return 'listeners';
                  if(h==='#/receivers')return 'receivers';
                  if(h==='#/signals'||h==='#/signalbridge')return 'signals';
                  if(h==='#/doctor')return 'doctor';
                  if(h==='#/regions'||h==='#/region-list')return 'regions';
                  if(h==='#/region-controllers'||h==='#/regionctl')return 'regionControllers';
                  if(h==='#/actions')return 'actions';
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
                  const nav=target&&target.closest?target.closest('[data-nav-route]'):null;
                  if(nav){
                    const interactive=target.closest('button,a,input,select,textarea,[data-no-nav]');
                    if(!interactive||interactive===nav){event.preventDefault();event.stopPropagation();activateNavRoute(nav);}
                  }
                });
                document.addEventListener('keydown',event=>{
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
                function deviceTable(items){return `<div class="table-wrap"><table class="data-table"><thead><tr><th>设备</th><th>类型</th><th>世界/维度</th><th>坐标</th><th>主频道</th><th>状态</th><th>最近触发</th><th>诊断</th><th>操作</th></tr></thead><tbody>${items.map(d=>{const target='#/devices/'+encodeURIComponent(d.id);return `<tr ${navigationAttr(target,false)}><td><span class="device-name"><span class="device-icon">${deviceMetadataIcon(d)}</span><span><strong>${esc(d.displayName)}</strong>${deviceSubtitle(d)}</span></span></td><td>${esc(labelType(d.type))}</td><td>${esc(d.world||'-')}</td><td>${esc(posText(d.pos))}</td><td>${channelCell(d.channel)}</td><td>${pill(d.enabled?'OK':'WARNING')} ${esc(labelBool(d.enabled))}</td><td>${fmtTime(d.lastTriggeredAt)}</td><td>${pill(d.doctorStatus)}</td><td><button class="text-button" ${navigationAttr(target)}>查看详情</button></td></tr>`;}).join('')}</tbody></table></div>`}
                function deviceSubtitle(d){const id=shortId(d.id);if(!isBlank(id)&&!String(id).toLowerCase().startsWith('minecraf'))return `<span class="device-subtitle">ID：${esc(id)}</span>`;if(!isBlank(d.world))return `<span class="device-subtitle">维度：${esc(d.world)}</span>`;return '';}
                function channelCell(channel){return channelButton(channel);}
                async function renderDeviceDetail(id,options={}){
                  if(!options.silent)setView(loading('正在加载设备详情...'));
                  const routeInfo=detailRoute(id,'#/devices'), encoded=encodeURIComponent(routeInfo.id);
                  let detail;try{detail=await api(`/api/devices/${encoded}`)}catch(err){if(options.silent){toast('设备详情实时刷新失败，已保留当前页面。');return;}setView(`<div class="back-row">${backButton(routeInfo,'返回设备管理')}</div>${err.status===404?errorBlock('设备不存在或已被删除。'):errorBlock(err.message)}`);return;}
                  const [debug,history,doctor,lockStatus,basicConfig,extendedConfig]=await Promise.all([settle(`/api/devices/${encoded}/debug`),detail.channel?settle(`/api/signals/history?channel=${encodeURIComponent(detail.channel)}&limit=10`):Promise.resolve({ok:true,data:[]}),settle('/api/doctor'),settle(`/api/webadmin/edit-locks/status?targetType=device_metadata&targetId=${encoded}`),settle(`/api/webadmin/device-basic-config/${encoded}`),settle(`/api/webadmin/device-extended-config/${encoded}`)]);
                  detail.metadataLock=lockStatus.ok?lockStatus.data:null;
                  detail.basicConfig=basicConfig.ok?basicConfig.data:null;
                  detail.basicConfigError=basicConfig.ok?null:basicConfig.error;
                  detail.extendedConfig=extendedConfig.ok?extendedConfig.data:null;
                  detail.extendedConfigError=extendedConfig.ok?null:extendedConfig.error;
                  const relatedDoctor=[...(detail.doctorIssues||[])];
                  if(doctor.ok){relatedDoctor.push(...(doctor.data.issues||[]).filter(i=>i.relatedObjectId===detail.id||(!isBlank(detail.channel)&&i.channel===detail.channel)));}
                  setView(`
                    <div class="back-row">${backButton(routeInfo,'返回设备管理')}</div>
                    <div class="page-head"><div><h1>${esc(detail.displayName)}</h1><p>世界/维度：${esc(detail.world||'暂无')} · 坐标：${esc(posText(detail.pos))} · 主频道：${esc(labelChannel(detail.channel))}</p></div><div class="overview-inline"><span class="pill">${esc(labelType(detail.type))}</span>${pill(detail.enabled?'OK':'WARNING')} ${pill(detail.doctorStatus||detail.debugSummary?.status||'UNKNOWN')}</div></div>
                    <section class="device-detail-layout">
                      <div class="device-detail-main">
                        <article class="panel-card"><h2>设备基础信息</h2><div class="identity-grid">${row('名称',esc(detail.displayName))}${row('类型',esc(labelType(detail.type)))}${row('启用状态',esc(labelBool(detail.enabled)))}${row('最近触发',fmtTime(detail.lastTriggeredAt))}</div></article>
                        <article class="panel-card"><h2>设备基础配置</h2>${deviceBasicConfigCard(detail)}</article>
                        <article class="panel-card"><h2>设备扩展配置</h2>${deviceExtendedConfigCard(detail)}</article>
                        <article class="panel-card"><h2>Debug 检查</h2>${debug.ok?debugChecks(debug.data):errorBlock(debug.error.message)}</article>
                        <article class="panel-card"><h2>最近事件</h2>${history.ok?historyList(history.data):errorBlock(history.error.message)}<p class="muted">${isBlank(detail.channel)?'当前设备暂无关联频道历史。':`<button class="link-button" ${navigationAttr(historyHash(detail.channel),false)}>查看相关历史</button>`}</p></article>
                        <article class="panel-card"><h2>配置摘要</h2>${configSummary(detail)}</article>
                      </div>
                      <aside class="device-detail-side">
                        <article class="panel-card metadata-card"><h2>WebAdmin 显示信息</h2>${deviceMetadataCard(detail)}</article>
                        <article class="panel-card side-card"><h2>Doctor 状态摘要</h2>${deviceDoctorSideCard(detail,uniqueIssues(relatedDoctor))}</article>
                        <article class="panel-card side-card"><h2>关联频道摘要</h2>${deviceChannelSideCard(detail)}</article>
                        <article class="panel-card side-card"><h2>设备标识 / 快捷信息</h2>${deviceIdentitySideCard(detail)}</article>
                        <article class="panel-card side-card"><h2>快捷跳转</h2>${deviceQuickLinksCard(detail,routeInfo)}</article>
                        <article class="panel-card side-card"><h2>最近状态</h2>${deviceRecentStatusCard(detail,history.ok?history.data:[])}</article>
                      </aside>
                    </section>`,options);
                }
                function canEditDeviceMetadata(){const role=String(appState.me?.role||'').toUpperCase();return role==='EDITOR'||role==='OWNER';}
                function canEditDeviceBasicConfig(){const role=String(appState.me?.role||'').toUpperCase();return role==='EDITOR'||role==='OWNER';}
                function canEditDeviceExtendedConfig(){const role=String(appState.me?.role||'').toUpperCase();return role==='EDITOR'||role==='OWNER';}
                function canEditChannelMetadata(){const role=String(appState.me?.role||'').toUpperCase();return role==='EDITOR'||role==='OWNER';}
                function canEditSignalListenerBasicConfig(){const role=String(appState.me?.role||'').toUpperCase();return role==='EDITOR'||role==='OWNER';}
                function csrfToken(){return appState.capabilities?.csrf?.token || '';}
                function metadataIconOptions(){return ['auto','signal_emitter','signal_receiver','action_relay','virtual_block_device','region','action','warning','key','chest','door','signal','custom_1'];}
                function labelMetadataIcon(value){return {auto:'自动图标',signal_emitter:'信号发射器',signal_receiver:'信号接收器',action_relay:'动作继电器',virtual_block_device:'虚拟方块设备',region:'区域',action:'动作',warning:'警告',key:'钥匙',chest:'箱子',door:'门',signal:'Signal',custom_1:'自定义 1'}[String(value||'auto')]||value;}
                function deviceMetadataCard(detail){
                  const meta=detail.metadata||{}, lock=detail.metadataLock||{}, editable=canEditDeviceMetadata(), editing=appState.deviceMetadataEdit&&appState.deviceMetadataEdit.deviceId===detail.id, lockedByOther=lock.locked&&!lock.heldByCurrentUser;
                  if(editing)return deviceMetadataForm(detail,appState.deviceMetadataEdit);
                  const note=isBlank(meta.note)?'<span class="muted">暂无备注</span>':esc(meta.note);
                  const updated=isBlank(meta.updatedAt)?'暂无':`${formatDateTime(meta.updatedAt)} · ${esc(meta.updatedBy||'未知用户')}`, version=Number(meta.version||0);
                  const lockHint=lockedByOther?`<div class="readonly-note">当前由 ${esc(lock.holderUsername||'其他用户')} 正在编辑，锁到期：${esc(formatDateTime(lock.expiresAt))}</div>`:'';
                  const action=editable&&!lockedByOther?`<button class="secondary" onclick='startDeviceMetadataEdit(${jsString(detail.id)},${jsString(meta.displayName||'')},${jsString(meta.note||'')},${jsString(meta.iconKey||'auto')},${version})'>编辑显示信息</button>`:(editable?lockHint:`<div class="readonly-note">需要 EDITOR 或 OWNER 权限才能编辑 WebAdmin 显示信息。</div>`);
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
                    <div class="form-actions"><button class="secondary" type="submit">${draft.saving?'保存中...':'保存'}</button><button class="text-button" type="button" onclick='cancelDeviceMetadataEdit(${jsString(detail.id)})'>取消</button></div>
                  </form>`;
                }
                function deviceBasicConfigCard(detail){
                  if(detail.basicConfigError)return errorBlock(detail.basicConfigError.message||'设备基础配置加载失败');
                  const cfg=detail.basicConfig||{}, lock=cfg.lockStatus||{}, editable=canEditDeviceBasicConfig(), editing=appState.deviceBasicConfigEdit&&appState.deviceBasicConfigEdit.deviceId===detail.id, lockedByOther=lock.locked&&!lock.heldByCurrentUser;
                  if(editing)return deviceBasicConfigForm(detail,appState.deviceBasicConfigEdit);
                  const supported=cfg.supported!==false;
                  const lockHint=lockedByOther?`<div class="readonly-note">当前由 ${esc(lock.holderUsername||'其他用户')} 正在编辑基础配置，锁到期：${esc(formatDateTime(lock.expiresAt))}</div>`:'';
                  let action='';
                  if(!supported)action=`<div class="readonly-note">${esc(cfg.unsupportedReason||'当前设备类型暂不支持基础配置编辑。')}</div>`;
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
                    <div class="form-actions"><button class="secondary" type="submit">${draft.saving?'保存中...':'保存'}</button><button class="text-button" type="button" onclick='cancelDeviceBasicConfigEdit(${jsString(detail.id)})'>取消</button></div>
                  </form>`;
                }
                function deviceExtendedConfigCard(detail){
                  if(detail.extendedConfigError)return errorBlock(detail.extendedConfigError.message||'设备扩展配置加载失败');
                  const cfg=detail.extendedConfig||{}, lock=cfg.lockStatus||{}, editable=canEditDeviceExtendedConfig(), editing=appState.deviceExtendedConfigEdit&&appState.deviceExtendedConfigEdit.deviceId===detail.id, lockedByOther=lock.locked&&!lock.heldByCurrentUser, fields=cfg.supportedFields||[];
                  if(editing)return deviceExtendedConfigForm(detail,appState.deviceExtendedConfigEdit);
                  if(cfg.supported===false||fields.length===0)return `<p class="muted">${esc(cfg.unsupportedReason||'该设备类型暂无可编辑扩展配置。')}</p>`;
                  const values=cfg.values||{}, labels=cfg.fieldLabels||{};
                  const rows=fields.map(field=>row(labels[field]||field,extendedFieldDisplay(field,values[field]))).join('');
                  const lockHint=lockedByOther?`<div class="readonly-note">当前由 ${esc(lock.holderUsername||'其他用户')} 正在编辑扩展配置，锁到期：${esc(formatDateTime(lock.expiresAt))}</div>`:'';
                  let action='';
                  if(editable&&!lockedByOther)action=`<button class="secondary" onclick='startDeviceExtendedConfigEdit(${jsString(detail.id)})'>编辑扩展配置</button>`;
                  else if(editable)action=lockHint;
                  else action='<div class="readonly-note">需要 EDITOR 或 OWNER 权限才能编辑设备扩展配置。</div>';
                  return `<div class="identity-grid">${rows}</div><p class="readonly-note">这些配置会影响设备交互、反馈频道或时间参数；不会编辑 itemSubmit、matcher、action、region、用户或系统设置。</p>${action}`;
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
                  const lock=draft.lock||{}, lockLine=lock.locked?`<div class="readonly-note">正在编辑扩展配置 · 锁到期：${esc(formatDateTime(lock.expiresAt))} · 持有人：${esc(lock.holderUsername||appState.me?.username||'当前用户')}</div>`:'<div class="readonly-note">正在获取编辑锁...</div>';
                  const conflict=draft.conflict?`<div class="readonly-note">设备扩展配置已被其他操作修改，请刷新后再编辑。<button class="link-button" type="button" onclick='reloadDeviceExtendedConfigAfterConflict(${jsString(detail.id)})'>刷新当前配置</button></div>`:'';
                  const fields=draft.supportedFields||[], body=fields.length?fields.map(field=>extendedFieldForm(detail.id,field,draft)).join(''):'<p class="muted">该设备类型暂无可编辑扩展配置。</p>';
                  return `<form class="edit-form" onsubmit='event.preventDefault();saveDeviceExtendedConfig(${jsString(detail.id)})'>
                    ${lockLine}
                    ${body}
                    ${errors}
                    ${conflict}
                    <p class="muted">保存会写入真实设备扩展配置；仅限当前设备类型支持的扩展频道或基础时间参数。不会修改 itemSubmit、matcher、interactionItem 复杂条件、action、region、用户或系统设置。</p>
                    <div class="form-actions"><button class="secondary" type="submit">${draft.saving?'保存中...':'保存'}</button><button class="text-button" type="button" onclick='cancelDeviceExtendedConfigEdit(${jsString(detail.id)})'>取消</button></div>
                  </form>`;
                }
                function extendedFieldForm(deviceId,field,draft){
                  const label=(draft.fieldLabels||{})[field]||field, value=(draft.values||{})[field]??'', id=extendedFieldId(field), help=extendedFieldHelp(field);
                  if(isExtendedChannelField(field)){
                    const combo=renderDeviceExtendedConfigChannelCombo(deviceId,field,draft), hint=channelHintHtml(value,draft.channelOptions||appState.channelOptions||[],draft.channelOptionsError||appState.channelOptionsError), clearable=(draft.clearableFields||{})[field]===true, checked=(draft.clear||{})[field]?'checked':'';
                    return `<label>${esc(label)}${combo}</label>${clearable?`<label class="readonly-note"><input id="extended-clear-${id}" type="checkbox" ${checked} onchange='updateDeviceExtendedConfigDraftFromForm(${jsString(deviceId)})'> 设为未设置</label>`:''}<p id="extended-${id}-hint" class="readonly-note">${hint}</p><p class="muted">${esc(help)}</p>`;
                  }
                  const min=field==='pulseTicks'?1:0, max=72000;
                  return `<label>${esc(label)}<input id="extended-${id}" class="input" type="number" min="${min}" max="${max}" step="1" value="${esc(value)}" oninput='updateDeviceExtendedConfigDraftFromForm(${jsString(deviceId)})'></label><p class="muted">${esc(help)}</p>`;
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
                function maybeReleaseDeviceMetadataEditForRoute(hash){const draft=appState.deviceMetadataEdit;if(!draft)return;if(String(hash||'').startsWith('#/devices/')){const info=detailRoute(String(hash).substring('#/devices/'.length),'#/devices');if(info.id===draft.deviceId)return;}releaseDeviceMetadataLock(draft,true);appState.deviceMetadataEdit=null;stopDeviceMetadataLockHeartbeat();}
                async function startDeviceMetadataEdit(deviceId,displayName,note,iconKey,expectedVersion){
                  if(!canEditDeviceMetadata())return;
                  try{
                    const result=await api('/api/webadmin/edit-locks/acquire',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'device_metadata',targetId:deviceId})});
                    if(!result.success){toast(result.message||'无法获取编辑锁');await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});return;}
                    const lock=result.data?.lock||{};
                    appState.deviceMetadataEdit={deviceId,displayName,note,iconKey:iconKey||'auto',expectedVersion:Number(expectedVersion||0),lockId:lock.lockId||'',lock,errors:[],saving:false,conflict:null};
                    scheduleDeviceMetadataLockHeartbeat();
                    await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});
                  }catch(err){toast(err.message||'无法获取编辑锁');}
                }
                async function cancelDeviceMetadataEdit(deviceId){const draft=appState.deviceMetadataEdit;if(draft&&draft.deviceId===deviceId){await releaseDeviceMetadataLock(draft,false);appState.deviceMetadataEdit=null;stopDeviceMetadataLockHeartbeat();}await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});}
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
                    if(result.success){appState.deviceMetadataEdit=null;stopDeviceMetadataLockHeartbeat();toast(result.changed?(result.message||'WebAdmin 显示信息已保存。'):'没有变更。');await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});return;}
                    draft.saving=false;draft.errors=result.validationErrors&&result.validationErrors.length?result.validationErrors:[{message:result.message||'保存失败'}];draft.conflict=result.conflict||null;appState.deviceMetadataEdit=draft;if(['edit_lock_expired','edit_lock_conflict','edit_lock_required'].includes(result.code))stopDeviceMetadataLockHeartbeat();toast(result.message||'保存失败');await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});
                  }catch(err){
                    draft.saving=false;draft.errors=[{message:err.message||'保存失败'}];appState.deviceMetadataEdit=draft;toast(err.message||'保存失败');await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});
                  }
                }
                """).append("""
                function channelMetadataCard(detail){
                  const meta=detail.metadata||{}, draft=appState.channelMetadataEdit;
                  if(draft&&draft.channel===detail.channel)return channelMetadataForm(detail,draft);
                  const canEdit=canEditChannelMetadata(), lock=meta.lockStatus||{};
                  const lockedByOther=!!lock.locked&&!lock.heldByCurrentUser;
                  const lockHint=lockedByOther?`<p class="readonly-note">${esc(lock.holderUsername||'其他用户')} 正在编辑，锁到期：${esc(formatDateTime(lock.expiresAt))}</p>`:'';
                  const action=canEdit&&!lockedByOther?`<button class="secondary" onclick='startChannelMetadataEdit(${jsString(detail.channel)})'>编辑频道显示信息</button>`:(canEdit?lockHint:'<p class="readonly-note">需要 EDITOR 或 OWNER 权限才能编辑。</p>');
                  return `<div class="identity-grid">${row('显示名',esc(meta.displayName||'未设置'))}${row('原始频道',esc(detail.channel))}${row('备注',esc(meta.note||'暂无'))}${row('图标',esc(labelMetadataIcon(meta.iconKey||'auto')))}${row('最后修改',fmtTime(meta.updatedAt))}${row('修改人',esc(meta.updatedBy||'暂无'))}</div><p class="muted">此信息仅用于 WebAdmin 展示，不会创建频道，也不会改变 SignalBridge 运行语义。</p>${action}`;
                }
                function channelMetadataForm(detail,draft){
                  const errs=draft.errors?.length?`<ul class="validation-list">${draft.errors.map(e=>`<li>${esc(e.field?`${e.field}：`: '')}${esc(e.message||'保存失败')}</li>`).join('')}</ul>`:'';
                  const conflict=draft.conflict?`<div class="readonly-note">${esc(draft.errors?.[0]?.message||'频道显示信息已发生冲突，请刷新后再编辑。')} <button class="link-button" onclick='reloadChannelMetadataAfterConflict(${jsString(detail.channel)})'>刷新当前信息</button></div>`:'';
                  return `<form class="edit-form" onsubmit='event.preventDefault();saveChannelMetadata(${jsString(detail.channel)})'><label>显示名<input id="channel-metadata-display-name" class="input" maxlength="64" value="${esc(draft.displayName||'')}" placeholder="例如：大厅任务提交成功"></label><label>备注<textarea id="channel-metadata-note" maxlength="512" placeholder="仅用于 WebAdmin 展示">${esc(draft.note||'')}</textarea></label><label>图标<select id="channel-metadata-icon" class="select">${metadataIconOptions().map(key=>`<option value="${esc(key)}" ${key===(draft.iconKey||'auto')?'selected':''}>${esc(labelMetadataIcon(key))}</option>`).join('')}</select></label><p class="readonly-note">正在编辑频道显示信息。锁到期：${fmtTime(draft.lock?.expiresAt)}</p>${errs}${conflict}<div class="form-actions"><button class="primary" type="submit" ${draft.saving?'disabled':''}>${draft.saving?'保存中...':'保存'}</button><button class="secondary" type="button" onclick='cancelChannelMetadataEdit(${jsString(detail.channel)})'>取消</button></div></form>`;
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
                    scheduleChannelMetadataLockHeartbeat();
                    await renderSignalDetail(encodeURIComponent(channel),{silent:true});
                  }catch(err){toast(err.message||'无法获取编辑锁');}
                }
                async function cancelChannelMetadataEdit(channel){const draft=appState.channelMetadataEdit;if(draft&&draft.channel===channel){await releaseChannelMetadataLock(draft,false);appState.channelMetadataEdit=null;stopChannelMetadataLockHeartbeat();}await renderSignalDetail(encodeURIComponent(channel),{silent:true});}
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
                    if(result.success){appState.channelMetadataEdit=null;stopChannelMetadataLockHeartbeat();toast(result.changed?(result.message||'频道显示信息已保存。'):'没有变更。');await renderSignalDetail(encodeURIComponent(channel),{silent:true});return;}
                    draft.saving=false;draft.errors=result.validationErrors&&result.validationErrors.length?result.validationErrors:[{message:result.message||'保存失败'}];draft.conflict=result.conflict||null;appState.channelMetadataEdit=draft;if(['edit_lock_expired','edit_lock_conflict','edit_lock_required'].includes(result.code))stopChannelMetadataLockHeartbeat();toast(result.message||'保存失败');await renderSignalDetail(encodeURIComponent(channel),{silent:true});
                  }catch(err){draft.saving=false;draft.errors=[{message:err.message||'保存失败'}];appState.channelMetadataEdit=draft;toast(err.message||'保存失败');await renderSignalDetail(encodeURIComponent(channel),{silent:true});}
                }
                function maybeReleaseDeviceBasicConfigEditForRoute(hash){const draft=appState.deviceBasicConfigEdit;if(!draft)return;if(String(hash||'').startsWith('#/devices/')){const info=detailRoute(String(hash).substring('#/devices/'.length),'#/devices');if(info.id===draft.deviceId)return;}releaseDeviceBasicConfigLock(draft,true);appState.deviceBasicConfigEdit=null;stopDeviceBasicConfigLockHeartbeat();}
                async function startDeviceBasicConfigEdit(deviceId,enabled,channel,expectedFingerprint){
                  if(!canEditDeviceBasicConfig())return;
                  try{
                    const result=await api('/api/webadmin/edit-locks/acquire',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'device_basic_config',targetId:deviceId})});
                    if(!result.success){toast(result.message||'无法获取编辑锁');await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});return;}
                    const lock=result.data?.lock||{};
                    const channelOptions=await loadSignalChannelOptions();
                    appState.deviceBasicConfigEdit={deviceId,enabled:!!enabled,channel:channel||'',channelOptions,channelOptionsError:appState.channelOptionsError,channelComboOpen:false,channelComboIndex:0,expectedFingerprint:expectedFingerprint||'',lockId:lock.lockId||'',lock,errors:[],saving:false,conflict:null};
                    scheduleDeviceBasicConfigLockHeartbeat();
                    await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});
                  }catch(err){toast(err.message||'无法获取编辑锁');}
                }
                async function cancelDeviceBasicConfigEdit(deviceId){const draft=appState.deviceBasicConfigEdit;if(draft&&draft.deviceId===deviceId){await releaseDeviceBasicConfigLock(draft,false);appState.deviceBasicConfigEdit=null;stopDeviceBasicConfigLockHeartbeat();}await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});}
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
                    if(result.success){appState.deviceBasicConfigEdit=null;stopDeviceBasicConfigLockHeartbeat();toast(result.changed?(result.message||'设备基础配置已保存。'):'没有变更。');await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});return;}
                    draft.saving=false;draft.errors=result.validationErrors&&result.validationErrors.length?result.validationErrors:[{message:result.message||'保存失败'}];draft.conflict=result.conflict||null;appState.deviceBasicConfigEdit=draft;if(['edit_lock_expired','edit_lock_conflict','edit_lock_required'].includes(result.code))stopDeviceBasicConfigLockHeartbeat();toast(result.message||'保存失败');await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});
                  }catch(err){
                    draft.saving=false;draft.errors=[{message:err.message||'保存失败'}];appState.deviceBasicConfigEdit=draft;toast(err.message||'保存失败');await renderDeviceDetail(currentDeviceRouteArg(deviceId),{silent:true});
                  }
                }
                function chainPreview(detail){if(isBlank(detail.channel))return '<span class="muted">当前设备没有主频道。</span>';return `<div class="chain-row"><strong>${esc(detail.displayName)}</strong><span class="muted">→ 主频道：${esc(detail.channel)}</span><span class="muted">→ 可在频道详情页查看消费者与最近事件</span></div>`}
                function debugChecks(data){const checks=data?.checks||[];if(checks.length===0)return empty('当前设备暂无 debug 数据。');return `<div class="list-stack">${checks.map(c=>`<div class="check-row-card"><strong>${pill(c.status)} ${esc(debugTitle(c))}</strong><span class="muted">${esc(debugMessage(c))}</span></div>`).join('')}</div>`}
                function debugTitle(c){const name=String(c?.name||'');if(name.includes('_')&&!isBlank(c?.message))return localizeCheckMessage(c);return localizeCheckName(name);}
                function debugMessage(c){const name=String(c?.name||'');if(name.includes('_')&&!isBlank(c?.message))return '';return localizeCheckMessage(c);}
                function localizeCheckName(name){return {enabled:'设备状态',channel:'主频道',block_id:'方块 ID',blockId:'方块 ID'}[String(name||'')]||name||'检查项';}
                function localizeCheckMessage(c){const text=String(c?.message||'');if(text==='Device is enabled.')return'当前设备处于启用状态。';if(text==='Device is disabled.')return'当前设备处于禁用状态。';if(text==='Primary channel is empty.')return'当前设备没有设置主频道。';return text.replace('Device is enabled.','当前设备处于启用状态。').replace('Primary channel is empty.','当前设备没有设置主频道。');}
                function configSummary(detail){const obj=detail?.configSummary||{};if(!obj||Object.keys(obj).length===0)return empty('当前设备暂无配置摘要。');const cfg=obj, item=cfg.interactionItem||{};let html='';
                  html+=configSection('基础配置',[
                    ['短 ID',shortId(detail.id||cfg.shortId)],
                    ['设备类型',labelType(detail.type)],
                    ['方块 ID',cfg.blockId],
                    ['工作模式',cfg.mode],
                    ['冷却时间',formatTicks(cfg.cooldownTicks)],
                    ['脉冲时间',formatTicks(cfg.pulseTicks)]
                  ],true);
                  html+=configSection('信号配置',[
                    ['主频道',labelChannel(detail.channel)],
                    ['成功频道',item.successChannel],
                    ['失败频道',item.failChannel],
                    ['动作数量',cfg.actionCount]
                  ]);
                  html+=configSection('交互配置',[
                    ['普通交互',cfg.interactionEnabled?'已启用':''],
                    ['物品匹配',item.enabled?'已启用':''],
                    ['物品来源',item.sourceDisplayName||labelInteractionSource(item.source)],
                    ['原版交互策略',item.vanillaPolicyDisplayName||labelVanillaPolicy(item.vanillaPolicy)],
                    ['消耗策略',item.consumeEnabled?`${item.consumeCount||1} 个，${item.consumeSourceDisplayName||labelConsumeSource(item.consumeSource)}`:''],
                    ['背包消耗顺序',item.consumeEnabled?(item.inventoryConsumeOrderDisplayName||labelConsumeOrder(item.inventoryConsumeOrder)):''],
                    ['物品模板',item.templateSummary]
                  ]);
                  html+=configSection('容器配置',[
                    ['容器事件',cfg.containerEnabled?'已启用':''],
                    ['物品条件数量',cfg.itemConditionCount]
                  ]);
                  html+=configSection('物品提交',[
                    ['多物品提交',cfg.itemSubmitEnabled?'已启用':''],
                    ['提交条件数量',cfg.itemSubmitRequirementCount]
                  ]);
                  const raw=flatten(obj).slice(0,32).map(([k,v])=>`<div class="kv-row"><span class="muted">${esc(k)}</span><strong>${esc(v)}</strong></div>`).join('');
                  return `<div>${html || empty('当前设备没有可展示的关键配置。')}<details class="raw-config"><summary>高级 / 原始字段</summary><div class="list-stack">${raw}</div></details></div>`;
                }
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
                  const report=appState.doctorReport||{summary:{},issues:[]}, issues=report.issues||[], filtered=filterDoctorIssues(issues), summary=report.summary||{};
                  const jumpTargets=issues.filter(i=>!isBlank(i.navigationTarget)||!isBlank(i.channel)).length;
                  if(setView(`
                    <div class="page-head"><div><h1>Doctor 诊断</h1><p>查看设备、频道、Signal 与配置风险</p></div><span class="pill info">只读模式</span></div>
                    <section class="card-grid">
                      ${metric('错误数量',summary.errorCount||0,(summary.errorCount||0)>0?'error':'','doctor')}
                      ${metric('警告数量',summary.warningCount||0,(summary.warningCount||0)>0?'warning':'','warning')}
                      ${metric('信息数量',summary.infoCount||0,'','doctor')}
                      ${metric('受影响设备',summary.affectedDeviceCount ?? '暂无','','device')}
                      ${metric('受影响频道',summary.affectedChannelCount ?? '暂无','','signal')}
                      ${metric('最近诊断',issues.length?formatDateTime(issues[0].detectedAt):'暂无','','history')}
                    </section>
                    <div class="toolbar">
                      <input class="input" id="doctor-search" placeholder="搜索标题 / 对象 / 频道 / 建议" value="${esc(appState.doctorFilters.search)}">
                      ${doctorFilterSelect('严重级别','doctor-severity',['ALL','ERROR','WARNING','INFO'],appState.doctorFilters.severity)}
                      ${doctorFilterSelect('对象类型','doctor-object',['ALL','DEVICE','CHANNEL','LISTENER','RECEIVER','ACTION_RELAY','ACTION','REGION','SYSTEM','UNKNOWN'],appState.doctorFilters.objectType)}
                      ${doctorFilterSelect('跳转目标','doctor-jump',['ALL','HAS_TARGET','NO_TARGET'],appState.doctorFilters.jump)}
                    </div>
                    <section class="content-grid">
                      <article class="panel-card">${filtered.length?doctorTable(filtered):empty(issues.length?'没有匹配当前筛选条件的诊断问题。':'当前没有诊断问题。')}</article>
                      <aside class="panel-card"><h2>诊断摘要</h2>${doctorSummaryPanel(issues,jumpTargets)}</aside>
                    </section>`,options))bindDoctorFilters(focusId);
                }
                function doctorFilterSelect(label,id,options,value){return `<label class="filter-field"><span>${esc(label)}</span><select class="select" id="${id}">${options.map(o=>`<option value="${esc(o)}" ${o===value?'selected':''}>${esc(doctorOptionLabel(o))}</option>`).join('')}</select></label>`}
                function doctorOptionLabel(v){return {ALL:'全部',ERROR:'错误',WARNING:'警告',INFO:'信息',DEVICE:'设备',CHANNEL:'频道',LISTENER:'监听器',RECEIVER:'接收器',ACTION_RELAY:'动作继电器',ACTION:'动作',REGION:'区域',SYSTEM:'系统',UNKNOWN:'未知',HAS_TARGET:'有跳转目标',NO_TARGET:'无跳转目标'}[v]||v;}
                function bindDoctorFilters(focusId){
                  const update=(event)=>{appState.doctorFilters.search=document.getElementById('doctor-search').value;appState.doctorFilters.severity=document.getElementById('doctor-severity').value;appState.doctorFilters.objectType=document.getElementById('doctor-object').value;appState.doctorFilters.jump=document.getElementById('doctor-jump').value;renderDoctorList(event.target.id);};
                  ['doctor-search','doctor-severity','doctor-object','doctor-jump'].forEach(id=>document.getElementById(id).addEventListener(id==='doctor-search'?'input':'change',update));
                  if(focusId){const el=document.getElementById(focusId);if(el){el.focus();if(el.setSelectionRange&&el.value){el.setSelectionRange(el.value.length,el.value.length);}}}
                }
                function filterDoctorIssues(items){const f=appState.doctorFilters;return (items||[]).filter(i=>{const hay=[i.title,i.message,i.relatedObjectName,i.relatedObjectId,i.channel,i.suggestion,i.code,i.id].join(' ').toLowerCase();if(f.search&&!hay.includes(f.search.toLowerCase()))return false;if(f.severity!=='ALL'&&String(i.severity||'').toUpperCase()!==f.severity)return false;if(f.objectType!=='ALL'&&String(i.relatedObjectType||'UNKNOWN').toUpperCase()!==f.objectType)return false;const hasTarget=!isBlank(i.navigationTarget)||!isBlank(i.channel);if(f.jump==='HAS_TARGET'&&!hasTarget)return false;if(f.jump==='NO_TARGET'&&hasTarget)return false;return true;});}
                function doctorTable(items){return `<div class="table-wrap"><table class="data-table"><thead><tr><th>严重级别</th><th>问题标题</th><th>关联对象</th><th>对象类型</th><th>频道</th><th>影响说明</th><th>建议操作</th><th>诊断代码</th><th>操作</th></tr></thead><tbody>${items.map(i=>`<tr><td>${pill(i.severity)}</td><td><strong>${issueTitle(i)}</strong></td><td>${esc(i.relatedObjectName||i.relatedObjectId||'暂无')}</td><td>${esc(labelObjectType(i.relatedObjectType))}</td><td>${isBlank(i.channel)?'<span class="muted">暂无</span>':channelButton(i.channel)}</td><td>${issueMessage(i)}</td><td>${issueSuggestion(i)}</td><td><span class="muted">代码：${esc(i.id||'unknown')}</span></td><td>${issueNavigation(i)}</td></tr>`).join('')}</tbody></table></div>`}
                function doctorSummaryPanel(issues,jumpTargets){if(!issues||issues.length===0)return empty('当前没有诊断问题。');const bySeverity=countBy(issues,i=>String(i.severity||'UNKNOWN').toUpperCase()), byObject=countBy(issues,i=>String(i.relatedObjectType||'UNKNOWN').toUpperCase());const top=issues.slice(0,5).map(i=>`<div class="issue-row"><strong>${pill(i.severity)} ${issueTitle(i)}</strong><span class="meta">${esc(issueContext(i))}</span><span>${issueSuggestion(i)}</span></div>`).join('');return `<div class="summary-grid">${metric('可跳转问题',jumpTargets)}${metric('无跳转目标',issues.length-jumpTargets)}${metric('频道问题',byObject.CHANNEL||0)}${metric('设备问题',byObject.DEVICE||0)}</div><h3>严重级别分布</h3><div class="list-stack"><div class="kv-row"><span class="muted">错误</span><strong>${bySeverity.ERROR||0}</strong></div><div class="kv-row"><span class="muted">警告</span><strong>${bySeverity.WARNING||0}</strong></div><div class="kv-row"><span class="muted">信息</span><strong>${bySeverity.INFO||0}</strong></div></div><h3>最近问题</h3><div class="list-stack">${top}</div><p class="muted">Doctor 页面只读展示当前可低成本诊断结果，不扫描世界、不强制加载区块。</p>`;}
                function countBy(items,mapper){return (items||[]).reduce((acc,item)=>{const key=mapper(item)||'UNKNOWN';acc[key]=(acc[key]||0)+1;return acc;},{});}
                async function renderHistoryPage(queryTail='',options={}){
                  const params=parseHashParams(queryTail);appState.historyFilters.channel=params.channel||'ALL';
                  if(!options.silent)setView(loading('正在加载历史记录...'));
                  let history;try{history=await api('/api/signals/history?limit=500')}catch(err){if(options.silent){toast('历史记录实时刷新失败，已保留当前页面。');return;}setView(errorBlock(err.message));return;}
                  appState.historyItems=history||[];
                  renderHistoryListPage('',options);
                }
                function parseHashParams(tail){const raw=String(tail||'');const query=raw.startsWith('?')?raw.substring(1):(raw.includes('?')?raw.substring(raw.indexOf('?')+1):'');const params=new URLSearchParams(query);return {channel:params.get('channel')||''};}
                function renderHistoryListPage(focusId,options={}){
                  const items=appState.historyItems||[], filtered=filterHistoryItems(items), channels=uniqueValues(items.map(h=>h.channel).filter(v=>!isBlank(v))), sourceTypes=uniqueValues(items.map(h=>h.sourceType).filter(v=>!isBlank(v)));
                  const success=items.filter(h=>String(h.result||'').toUpperCase()==='SUCCESS').length, failed=items.filter(h=>String(h.result||'').toUpperCase()==='FAILED').length, latest=items[0]?.time||'';
                  if(setView(`
                    <div class="page-head"><div><h1>历史记录</h1><p>查看 Signal 事件、设备事件与系统观测记录</p></div><span class="pill info">只读模式</span></div>
                    <section class="card-grid">
                      ${metric('最近记录数',items.length,'','history')}
                      ${metric('成功事件',success,'','ok')}
                      ${metric('失败事件',failed,failed>0?'warning':'','warning')}
                      ${metric('涉及频道',channels.length,'','signal')}
                      ${metric('最近事件时间',fmtTime(latest),'','history')}
                    </section>
                    <div class="toolbar">
                      <input class="input" id="history-search" placeholder="搜索频道 / 来源 / 玩家 / 详情 / 结果" value="${esc(appState.historyFilters.search)}">
                      ${historyFilterSelect('频道','history-channel',['ALL',...channels],appState.historyFilters.channel)}
                      ${historyFilterSelect('来源类型','history-source',['ALL',...sourceTypes],appState.historyFilters.sourceType)}
                      ${historyFilterSelect('结果','history-result',['ALL','SUCCESS','FAILED','UNKNOWN'],appState.historyFilters.result)}
                      ${historyFilterSelect('时间范围','history-range',['ALL','M10','H1','H24'],appState.historyFilters.range)}
                      ${historyFilterSelect('排序','history-sort',['NEWEST','OLDEST'],appState.historyFilters.sort)}
                    </div>
                    <section class="content-grid">
                      <article class="panel-card">${filtered.length?historyTable(filtered):empty(items.length?'没有匹配当前筛选条件的历史事件。':'暂无历史事件。')}</article>
                      <aside class="panel-card"><h2>时间线摘要</h2>${historySummaryPanel(filtered,items)}</aside>
                    </section>`,options))bindHistoryFilters(focusId);
                }
                function historyFilterSelect(label,id,options,value){return `<label class="filter-field"><span>${esc(label)}</span><select class="select" id="${id}">${options.map(o=>`<option value="${esc(o)}" ${o===value?'selected':''}>${esc(historyOptionLabel(id,o))}</option>`).join('')}</select></label>`}
                function historyOptionLabel(id,v){if(v==='ALL')return'全部';if(id==='history-source')return labelSourceType(v);if(id==='history-result')return labelStatus(v);if(id==='history-range')return labelHistoryRange(v);if(id==='history-sort')return labelHistorySort(v);return v;}
                function bindHistoryFilters(focusId){
                  const update=(event)=>{appState.historyFilters.search=document.getElementById('history-search').value;appState.historyFilters.channel=document.getElementById('history-channel').value;appState.historyFilters.sourceType=document.getElementById('history-source').value;appState.historyFilters.result=document.getElementById('history-result').value;appState.historyFilters.range=document.getElementById('history-range').value;appState.historyFilters.sort=document.getElementById('history-sort').value;renderHistoryListPage(event.target.id);};
                  ['history-search','history-channel','history-source','history-result','history-range','history-sort'].forEach(id=>document.getElementById(id).addEventListener(id==='history-search'?'input':'change',update));
                  if(focusId){const el=document.getElementById(focusId);if(el){el.focus();if(el.setSelectionRange&&el.value){el.setSelectionRange(el.value.length,el.value.length);}}}
                }
                function filterHistoryItems(items){const f=appState.historyFilters;const cutoff=historyCutoff(f.range);const filtered=(items||[]).filter(h=>{const hay=[h.channel,h.sourceName,h.sourceId,h.playerName,h.result,h.description,h.sourceType].join(' ').toLowerCase();if(f.search&&!hay.includes(f.search.toLowerCase()))return false;if(f.channel!=='ALL'&&h.channel!==f.channel)return false;if(f.sourceType!=='ALL'&&h.sourceType!==f.sourceType)return false;if(f.result!=='ALL'&&String(h.result||'UNKNOWN').toUpperCase()!==f.result)return false;if(cutoff){const d=parseTime(h.time);if(!d||d.getTime()<cutoff)return false;}return true;});return filtered.sort((a,b)=>f.sort==='OLDEST'?String(a.time||'').localeCompare(String(b.time||'')):String(b.time||'').localeCompare(String(a.time||'')));}
                function historyCutoff(range){const now=Date.now();if(range==='M10')return now-10*60*1000;if(range==='H1')return now-60*60*1000;if(range==='H24')return now-24*60*60*1000;return 0;}
                function historyTable(items){return `<div class="table-wrap"><table class="data-table"><thead><tr><th>时间</th><th>事件类型</th><th>频道</th><th>来源对象</th><th>来源类型</th><th>玩家 / 用户</th><th>结果</th><th>详情</th><th>操作</th></tr></thead><tbody>${items.map(h=>`<tr><td>${fmtTime(h.time)}</td><td>Signal 事件</td><td>${channelButton(h.channel)}</td><td>${esc(h.sourceName||h.sourceId||'暂无')}</td><td>${esc(labelSourceType(h.sourceType))}</td><td>${esc(h.playerName||'无玩家上下文')}</td><td>${pill(h.result||'UNKNOWN')}</td><td>${esc(h.description||'暂无详情')}</td><td>${historyAction(h)}</td></tr>`).join('')}</tbody></table></div>`;}
                function historySummaryPanel(filtered,allItems){if(!allItems||allItems.length===0)return empty('暂无历史事件。');const byResult=countBy(filtered,h=>String(h.result||'UNKNOWN').toUpperCase()), bySource=countBy(filtered,h=>String(h.sourceType||'UNKNOWN').toUpperCase());const recent=filtered.slice(0,5).map(h=>`<div class="event-row"><strong>${esc(labelChannel(h.channel))}</strong><span class="meta">${fmtTime(h.time)} · ${esc(labelSourceType(h.sourceType))} · ${esc(labelStatus(h.result))}</span><span>${esc(h.description||'暂无详情')}</span></div>`).join('');return `<div class="summary-grid">${metric('当前筛选',filtered.length)}${metric('全部记录',allItems.length)}${metric('成功',byResult.SUCCESS||0)}${metric('失败',byResult.FAILED||0)}</div><h3>来源类型</h3><div class="list-stack">${Object.entries(bySource).slice(0,6).map(([k,v])=>`<div class="kv-row"><span class="muted">${esc(labelSourceType(k))}</span><strong>${v}</strong></div>`).join('')||empty('暂无来源统计。')}</div><h3>最近事件</h3><div class="list-stack">${recent||empty('当前筛选下暂无事件。')}</div><p class="muted">History 页面只读展示已有内存历史，不删除、不导出、不重放事件。</p>`;}
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
                async function renderSettingsPage(options={}){
                  if(!options.silent)setView(loading('正在加载系统设置...'));
                  let data;try{data=await api('/api/webadmin/settings')}catch(err){if(options.silent){toast('系统设置实时刷新失败，已保留当前页面。');return;}setView(errorBlock(err.message));return;}
                  const service=data.service||{}, storage=data.storage||{}, security=data.security||{}, audit=data.audit||{}, system=data.system||{}, visibility=data.visibility||{};
                  setView(`
                    <div class="page-head"><div><h1>系统设置</h1><p>查看 WebAdmin 服务、安全、存储与运行信息</p></div><span class="pill info">只读模式</span></div>
                    <section class="card-grid">
                      ${metric('服务状态',service.running?'运行中':'未运行',service.running?'':'warning','settings')}
                      ${metric('访问模式',labelAccessMode(service.accessMode),'','settings')}
                      ${metric('当前 Session',system.sessionCount ?? '暂无','','user')}
                      ${metric('审计日志',audit.enabled?'已启用':'已关闭',audit.enabled?'':'warning','history')}
                      ${metric('服务器类型',labelServerType(system.serverType),'','dashboard')}
                      ${metric('Mod 版本',system.modVersion||'暂无','','device')}
                    </section>
                    <section class="detail-grid" style="margin-top:16px">
                      <article class="panel-card"><h2>服务状态</h2><div class="identity-grid">${row('WebAdmin 服务',esc(service.running?'运行中':'未运行'))}${row('监听地址',esc(service.host||'暂无'))}${row('端口',esc(service.port ?? '暂无'))}${row('访问模式',esc(labelAccessMode(service.accessMode)))}${row('当前访问 URL',esc(service.url||'暂无'))}${row('当前登录用户',esc(service.currentUser||'暂无'))}${row('当前角色',esc(labelRoleFull(service.currentRole)))}</div></article>
                      <article class="panel-card"><h2>存储目录</h2>${storagePanel(storage,visibility)}</article>
                      <article class="panel-card"><h2>安全配置</h2><div class="identity-grid">${row('认证方式',esc(labelAuthMode(security.authMode)))}${row('密码哈希算法',esc(security.passwordHashAlgorithm||'暂无'))}${row('Session Cookie',esc(security.sessionCookieName||'暂无'))}${row('Session 有效期',esc(formatMinutes(security.sessionTtlMinutes)))}${row('记住我有效期',esc(formatMinutes(security.rememberMeTtlMinutes)))}${row('审计日志',esc(security.auditEnabled?'已启用':'已关闭'))}${row('远程访问',esc(security.remoteAccessAllowed?'允许当前访问模式远程协作':'本机访问'))}</div></article>
                      <article class="panel-card"><h2>审计 / History</h2><div class="identity-grid">${row('审计日志状态',esc(audit.enabled?'已启用':'已关闭'))}${row('审计文件',esc(audit.auditLogExists?'已存在':'暂无文件'))}${row('最近登录记录',esc(audit.recentLoginRecords||'暂无数据'))}${row('API 访问统计',esc(audit.apiAccessStats||'暂无数据'))}</div></article>
                      <article class="panel-card"><h2>系统信息</h2><div class="identity-grid">${row('TZZ Mod 版本',esc(system.modVersion||'暂无数据'))}${row('Minecraft 版本',esc(system.minecraftVersion||'暂无数据'))}${row('服务器类型',esc(labelServerType(system.serverType)))}${row('当前世界 / 存档',esc(system.worldName||'暂无数据'))}</div></article>
                      <article class="panel-card"><h2>危险操作说明</h2><p class="muted">6.5 系统设置页面只读展示。修改端口、访问模式、用户、密码等操作当前请使用 /tzz webadmin 命令或服务端配置文件；Web UI 写操作将在后续阶段谨慎开放。</p></article>
                    </section>`,options);
                }
                function storagePanel(storage,visibility){const restricted=storage.restricted||visibility.sensitiveStorageHidden;const hidden='受限信息已隐藏';return `<div class="identity-grid">${row('存储作用域',esc(storage.scope||'WORLD_SAVE'))}${row('按世界隔离',esc(storage.worldScoped?'是':'否'))}${row('WebAdmin 存储目录',esc(restricted?hidden:(storage.directory||'暂无')))}${row('配置文件',esc(restricted?hidden:(storage.configPath||'暂无')))}${row('用户文件',esc(restricted?hidden:(storage.usersPath||'暂无')))}${row('审计日志',esc(restricted?hidden:(storage.auditLogPath||'暂无')))}${row('配置文件存在',esc(storage.configExists?'是':'否'))}${row('用户文件存在',esc(storage.usersExists?'是':'否'))}${row('审计日志存在',esc(storage.auditLogExists?'是':'否'))}${row('旧全局文件提示',esc(storage.legacyGlobalFilesDetected?'检测到旧 config/tzz WebAdmin 文件，但不会自动加载':'未检测到旧全局文件'))}</div><p class="muted">WebAdmin 持久化文件统一放在当前世界 / 存档目录下的 tzz/webadmin/，不再使用全局 config/tzz。</p>`}
                function labelAuthMode(value){return {USERNAME_PASSWORD:'用户名 / 密码'}[String(value||'').toUpperCase()]||value||'暂无';}
                function labelServerType(value){return {DEDICATED:'专用服务器（DEDICATED）',INTEGRATED:'集成服务器（INTEGRATED）'}[String(value||'').toUpperCase()]||value||'暂无';}
                function formatMinutes(value){const n=Number(value||0);return n>0?`${n} 分钟`:'暂无';}
                function uniqueValues(items){return [...new Set(items)].sort((a,b)=>String(a).localeCompare(String(b)));}
                function regionHash(id){return `#/regions/${encodeURIComponent(id||'')}`;}
                function actionHash(id){return `#/actions/${encodeURIComponent(id||'')}`;}
                function regionButton(id,label){if(isBlank(id))return '<span class="muted">暂无区域</span>';return `<button class="link-button" ${navigationAttr(regionHash(id))}>${esc(label||id)}</button>`}
                function actionButton(id,label){if(isBlank(id))return '<span class="muted">暂无动作</span>';return `<button class="link-button" ${navigationAttr(actionHash(id))}>${esc(label||id)}</button>`}
                function boundsText(bounds){return bounds?`${bounds.minX} ${bounds.minY} ${bounds.minZ} → ${bounds.maxX} ${bounds.maxY} ${bounds.maxZ}`:'暂无';}
                function boundsSize(bounds){if(!bounds)return '暂无';const x=Math.abs(Number(bounds.maxX)-Number(bounds.minX))+1,y=Math.abs(Number(bounds.maxY)-Number(bounds.minY))+1,z=Math.abs(Number(bounds.maxZ)-Number(bounds.minZ))+1;return `${x} × ${y} × ${z}`;}
                function boundsVolume(bounds){if(!bounds)return '暂无';const x=Math.abs(Number(bounds.maxX)-Number(bounds.minX))+1,y=Math.abs(Number(bounds.maxY)-Number(bounds.minY))+1,z=Math.abs(Number(bounds.maxZ)-Number(bounds.minZ))+1;return `${x*y*z}`;}
                function boundsCenter(bounds){if(!bounds)return '暂无';return `${Math.floor((Number(bounds.minX)+Number(bounds.maxX))/2)} ${Math.floor((Number(bounds.minY)+Number(bounds.maxY))/2)} ${Math.floor((Number(bounds.minZ)+Number(bounds.maxZ))/2)}`;}
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
                  if(!detailRes.ok){if(options.silent){toast('区域详情实时刷新失败，已保留当前页面。');return;}setView(`<div class="back-row">${backButton(routeInfo,'返回区域管理')}</div>${detailRes.error.status===404?errorBlock('区域不存在或已被删除。'):errorBlock(detailRes.error.message)}`);return;}
                  const detail=detailRes.data, list=listRes.ok?listRes.data:[], entry=(list||[]).find(r=>r.id===detail.id)||{};
                  setView(`
                    <div class="back-row">${backButton(routeInfo,'返回区域管理')}</div>
                    <div class="page-head"><div><h1>${esc(detail.name||detail.id)}</h1><p>区域详情 · 只读</p></div><div class="inline-actions">${pill(entry.enabled?'OK':'WARNING')}${pill(entry.doctorStatus||'UNKNOWN')}<span class="pill info">只读模式</span></div></div>
                    <section class="detail-grid">
                      <article class="panel-card overview-card"><h2>区域概览</h2><div class="overview-inline"><span class="device-icon">${icon('region')}</span><strong>${esc(detail.name||detail.id)}</strong><span class="muted">世界：${esc(detail.world||'暂无')}</span><span class="muted">范围：${esc(boundsText(detail.bounds))}</span><span class="muted">玩家：${esc((detail.playersInside||[]).length || entry.playersInside || 0)}</span></div></article>
                      <article class="panel-card"><h2>基础信息</h2><div class="identity-grid">${row('区域 ID',esc(detail.id))}${row('名称',esc(detail.name||detail.id))}${row('世界/维度',esc(detail.world||'暂无'))}${row('启用状态',esc(entry.enabled?'启用':'暂无数据'))}${row('最近事件',fmtTime(entry.lastEventAt))}${row('Doctor 状态',pill(entry.doctorStatus||'UNKNOWN'))}</div></article>
                      <article class="panel-card"><h2>边界 / Bounds</h2><div class="identity-grid">${row('X 范围',esc(detail.bounds?`${detail.bounds.minX} → ${detail.bounds.maxX}`:'暂无'))}${row('Y 范围',esc(detail.bounds?`${detail.bounds.minY} → ${detail.bounds.maxY}`:'暂无'))}${row('Z 范围',esc(detail.bounds?`${detail.bounds.minZ} → ${detail.bounds.maxZ}`:'暂无'))}${row('中心点',esc(boundsCenter(detail.bounds)))}${row('尺寸',esc(boundsSize(detail.bounds)))}${row('体积',esc(boundsVolume(detail.bounds)))}</div></article>
                      <article class="panel-card"><h2>目标过滤</h2><div class="identity-grid">${row('目标类型',esc(labelTargetFilter(detail.targetFilter)))}${row('说明',esc(isBlank(detail.targetFilter)?'暂无目标过滤信息':'使用当前 RegionController 配置中的目标过滤。'))}</div></article>
                      <article class="panel-card"><h2>事件动作摘要</h2>${regionActionGroups(detail.actions||{})}</article>
                      <article class="panel-card"><h2>绑定 Channel / 逻辑链</h2>${regionChannels(detail.boundChannels||[])}</article>
                      <article class="panel-card"><h2>当前玩家 / 最近事件</h2>${regionPlayersAndEvents(detail)}</article>
                      <article class="panel-card"><h2>Doctor / Debug 摘要</h2>${doctorList(detail.doctorIssues||[],8)}</article>
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
                async function renderActionDetail(id,options={}){
                  if(!options.silent)setView(loading('正在加载动作详情...'));
                  const routeInfo=detailRoute(id,'#/actions');
                  const [detailRes,listRes]=await Promise.all([settle(`/api/actions/${encodeURIComponent(routeInfo.id)}`),settle('/api/actions')]);
                  if(!detailRes.ok){if(options.silent){toast('动作详情实时刷新失败，已保留当前页面。');return;}setView(`<div class="back-row">${backButton(routeInfo,'返回动作系统')}</div>${detailRes.error.status===404?errorBlock('动作不存在或已被删除。'):errorBlock(detailRes.error.message)}`);return;}
                  const detail=detailRes.data, list=listRes.ok?listRes.data:[], entry=(list||[]).find(a=>a.id===detail.id)||{}, owner=detail.owner||{};
                  setView(`
                    <div class="back-row">${backButton(routeInfo,'返回动作系统')}</div>
                    <div class="page-head"><div><h1>${esc(entry.name||detail.configSummary?.name||detail.id)}</h1><p>动作详情 · 只读</p></div><div class="inline-actions">${pill(entry.doctorStatus||'UNKNOWN')}<span class="pill info">只读模式</span></div></div>
                    <section class="detail-grid">
                      <article class="panel-card overview-card"><h2>动作概览</h2><div class="overview-inline"><span class="device-icon">${icon('action')}</span><strong>${esc(labelActionType(detail.type||entry.type))}</strong><span class="muted">归属：${esc(owner.ownerName||entry.ownerName||'暂无')}</span><span class="muted">最近结果：${esc(labelStatus(entry.lastResult||'UNKNOWN'))}</span><span class="muted">最近执行：${esc(formatDateTime(entry.lastExecutedAt))}</span></div></article>
                      <article class="panel-card"><h2>基础信息</h2><div class="identity-grid">${row('Action ID',esc(detail.id))}${row('类型',esc(labelActionType(detail.type||entry.type)))}${row('归属类型',esc(labelOwnerType(owner.ownerType||entry.ownerType)))}${row('归属对象',ownerLink(entry))}${row('关联频道',channelCell(owner.channel||entry.channel))}${row('引用次数',esc(entry.referencedByCount ?? detail.configSummary?.referencedByCount ?? 0))}${row('执行次数',esc(entry.executionCount ?? detail.configSummary?.executionCount ?? 0))}${row('最近结果',pill(entry.lastResult||'UNKNOWN'))}</div></article>
                      <article class="panel-card"><h2>配置摘要</h2>${actionConfigPanel(detail,entry)}</article>
                      <article class="panel-card"><h2>引用来源</h2><div class="list-stack"><div class="event-row"><strong>${esc(labelOwnerType(owner.ownerType||entry.ownerType))}</strong><span>${ownerLink(entry)}</span><span class="muted">6.6 只读展示当前可从配置收集到的引用来源。</span></div></div></article>
                      <article class="panel-card"><h2>最近执行记录</h2>${actionExecutions(detail.recentExecutions||[])}</article>
                      <article class="panel-card"><h2>Doctor / Debug 摘要</h2>${doctorList(detail.doctorIssues||[],8)}</article>
                    </section>`,options);
                }
                function actionConfigPanel(detail,entry){const type=String(detail.type||entry.type||'UNKNOWN').toUpperCase(), summary=cleanActionSummary(detail.summary||entry.summary);const rows=[['动作类型',labelActionType(type)],['摘要',summary],['下游频道',type==='SIGNAL'?(entry.channel||detail.owner?.channel):''],['Doctor 状态',entry.doctorStatus||detail.configSummary?.doctorStatus]];let html=configSection('关键配置',rows);if(type==='COMMAND')html+=`<p class="muted">命令动作仅只读展示摘要，不提供执行、复制执行或测试按钮。</p>`;if(type==='SIGNAL'&&!isBlank(entry.channel))html+=`<p>${channelButton(entry.channel)}</p>`;const raw=flatten(detail.configSummary||{}).slice(0,24).map(([k,v])=>`<div class="kv-row"><span class="muted">${esc(k)}</span><strong>${esc(v)}</strong></div>`).join('');return `${html||empty('暂无可用配置摘要。')}<details class="raw-config"><summary>高级 / 原始字段</summary><div class="list-stack">${raw||'<span class="muted">暂无原始字段。</span>'}</div></details>`}
                function actionExecutions(items){if(!items||items.length===0)return empty('暂无执行记录。');return `<div class="list-stack">${items.map(e=>`<div class="event-row"><strong>${fmtTime(e.time||e.executedAt)}</strong><span>${esc(e.owner||'暂无归属')} · ${esc(labelStatus(e.result||'UNKNOWN'))}</span><span class="muted">${esc(e.detail||'暂无详情')}</span></div>`).join('')}</div>`}
                async function renderSignals(options={}){
                  if(!options.silent)setView(loading('正在加载 Signal 频道...'));
                  let channels;try{channels=await api('/api/signals/channels')}catch(err){if(options.silent){toast('Signal 频道实时刷新失败，已保留当前页面。');return;}setView(errorBlock(err.message));return;}
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
                async function renderSignalDetail(channel,options={}){
                  const routeInfo=detailRoute(channel,'#/signals'), decoded=routeInfo.id||'';
                  if(!options.silent)setView(loading('正在加载频道详情...'));
                  let detail;try{detail=await api(`/api/signals/channels/${encodeURIComponent(decoded)}`)}catch(err){if(options.silent){toast('频道详情实时刷新失败，已保留当前页面。');return;}setView(`<div class="back-row">${backButton(routeInfo,'返回 Signal 管理')}</div>${err.status===404?errorBlock('频道不存在或当前没有可读取数据。'):errorBlock(err.message)}`);return;}
                  const metadataRes=await settle(`/api/webadmin/channel-metadata?channel=${encodeURIComponent(decoded)}`);
                  if(metadataRes.ok)detail.metadata=metadataRes.data;
                  const listenerConfigResults=await Promise.all((detail.listeners||[]).map(l=>{const ref=l.id||l.name||'';return ref?settle(`/api/webadmin/signal-listener-basic-config/${encodeURIComponent(ref)}`):Promise.resolve({ok:false});}));
                  (detail.listeners||[]).forEach((listener,index)=>{const result=listenerConfigResults[index];if(result&&result.ok)listener.basicConfig=result.data;});
                  const channelMeta=detail.metadata||{}, channelTitle=channelMeta.effectiveDisplayName||detail.channel;
                  const stats=detail.stats||{}, totalConsumers=Number(stats.listenerCount||0)+Number(stats.receiverCount||0)+Number(stats.actionRelayCount||0);
                  setView(`
                    <div class="back-row">${backButton(routeInfo,'返回 Signal 管理')}</div>
                    <div class="page-head"><div><h1>${esc(channelTitle)}</h1><p>${esc(detail.channel)} · 频道详情 / 横向逻辑链</p></div><div class="inline-actions">${pill((detail.doctorIssues||[]).some(i=>i.severity==='ERROR')?'ERROR':((detail.doctorIssues||[]).length?'WARNING':'OK'))}<span class="pill info">基础编辑</span></div></div>
                    <section class="card-grid">
                      ${metric('消费者数量',totalConsumers,'','receiver')}
                      ${metric('监听器',Number(stats.listenerCount||0),'','signal')}
                      ${metric('接收器',Number(stats.receiverCount||0),'','receiver')}
                      ${metric('动作继电器',Number(stats.actionRelayCount||0),'','relay')}
                      ${metric('关联来源',Number(stats.sourceCount||0),'','device')}
                      ${metric('下游 Signal',Number(stats.downstreamSignalCount||0),'','signal')}
                      ${metric('最近事件数',Number(stats.triggerCountToday||0),'','history')}
                      ${metric('最近触发',formatDateTime(stats.lastTriggeredAt),'','history')}
                    </section>
                    <section class="detail-grid" style="margin-top:16px">
                      <article class="panel-card overview-card"><h2>WebAdmin 频道显示信息</h2>${channelMetadataCard(detail)}</article>
                      <article class="panel-card overview-card"><h2>频道基础信息</h2><div class="identity-grid">${row('频道名',esc(detail.channel))}${row('显示名',esc(channelTitle))}${row('备注',esc(channelMeta.note||'暂无'))}${row('频道类型',esc(labelChannelType(detail.type)))}${row('最近触发',fmtTime(stats.lastTriggeredAt))}${row('消费者',esc(`${totalConsumers} 个`))}${row('监听器',esc(Number(stats.listenerCount||0)))}${row('接收器',esc(Number(stats.receiverCount||0)))}${row('动作继电器',esc(Number(stats.actionRelayCount||0)))}</div></article>
                      <article class="panel-card overview-card"><h2>横向逻辑链</h2>${logicChain(detail)}</article>
                      <article class="panel-card"><h2>消费者</h2>${endpointGroups(detail)}</article>
                      <article class="panel-card"><h2>动作 / 下游影响</h2>${actionsPanel(detail)}</article>
                      <article class="panel-card"><h2>最近 Signal 事件</h2>${historyList(detail.recentHistory||[])}<p class="muted"><button class="link-button" ${navigationAttr(historyHash(detail.channel),false)}>查看该频道历史</button></p></article>
                      <article class="panel-card"><h2>频道诊断</h2>${doctorList(detail.doctorIssues||[],8)}<p class="muted"><button class="link-button" onclick="location.hash='#/doctor'">查看全局诊断</button></p></article>
                    </section>`,options);
                }
                function logicChain(detail){
                  const sources=detail.sources||[], listeners=detail.listeners||[], receivers=detail.receivers||[], relays=detail.actionRelays||[], actions=detail.actions||[], downstream=detail.downstreamSignals||[];
                  return `<div class="logic-chain">
                    <div class="chain-node"><h3>触发源</h3>${endpointCompact(sources,'暂无可推断触发源')}</div>
                    <div class="chain-arrow">→</div>
                    <div class="chain-node"><h3>频道</h3><strong>${esc((detail.metadata&&detail.metadata.effectiveDisplayName)||detail.channel)}</strong><span class="muted">${esc(detail.channel)}</span><span class="muted">${esc(labelChannelType(detail.type))}</span>${pill((detail.doctorIssues||[]).length?'WARNING':'OK')}</div>
                    <div class="chain-arrow">→</div>
                    <div class="chain-node"><h3>消费者</h3>${endpointCompact([...listeners,...receivers,...relays],'暂无消费者')}</div>
                    <div class="chain-arrow">→</div>
                    <div class="chain-node"><h3>动作 / 下游影响</h3>${actions.length?actions.slice(0,4).map(a=>`<span>${actionButton(a.id,labelActionType(a.type))}：${esc(cleanActionSummary(a.summary||a.name||'-'))}</span>`).join(''):(downstream.length?downstream.map(c=>`<span>下游频道：${esc(c)}</span>`).join(''):'<span class="muted">暂无可用动作详情</span>')}</div>
                  </div>`;
                }
                function endpointCompact(items,emptyText){if(!items||items.length===0)return `<span class="muted">${esc(emptyText)}</span>`;return items.slice(0,4).map(e=>`<span>${navigationButton(e.navigationTarget,e.name||e.id)} <span class="muted">(${esc(labelEndpointType(e.type))})</span></span>`).join('');}
                """).append("""
                function signalListenerFieldId(ref){return String(ref||'').replace(/[^a-zA-Z0-9_-]/g,'_');}
                function signalListenerBasicConfigCard(e,detail){
                  const ref=e.id||e.name||'', draft=appState.signalListenerBasicConfigEdit, isEditing=draft&&draft.listenerRef===ref;
                  if(isEditing)return signalListenerBasicConfigForm(e,detail,draft);
                  const cfg=e.basicConfig||{}, lock=cfg.lockStatus||{}, canEdit=canEditSignalListenerBasicConfig(), lockedByOther=!!lock.locked&&!lock.heldByCurrentUser;
                  const enabled=cfg.listenerRef?cfg.enabled:e.enabled, channel=cfg.listenerRef?cfg.channel:e.channel, cooldown=cfg.listenerRef?cfg.cooldownTicks:e.cooldownTicks, actionCount=cfg.listenerRef?cfg.actionCount:e.actionCount;
                  const lockHint=lockedByOther?`<div class="readonly-note">${esc(lock.holderUsername||'其他用户')} 正在编辑，锁到期：${esc(formatDateTime(lock.expiresAt))}</div>`:'';
                  const action=canEdit&&!lockedByOther?`<button class="secondary" type="button" onclick='startSignalListenerBasicConfigEdit(${jsString(ref)},${jsString(detail.channel)})'>编辑基础配置</button>`:(canEdit?lockHint:'<span class="muted">需要 EDITOR 或 OWNER 权限才能编辑。</span>');
                  return `<div class="readonly-note"><div class="kv-row"><span class="muted">基础配置</span><strong>${enabled?'启用':'禁用'} / ${esc(channel||detail.channel||'未设置')} / 冷却 ${esc(cooldown ?? 0)} tick</strong></div><div class="kv-row"><span class="muted">Action</span><strong>${esc(actionCount ?? 0)} 个（只读）</strong></div>${action}</div>`;
                }
                function listenerChannelComboOptionsHtml(ref,draft){
                  if(draft.channelOptionsError||appState.channelOptionsError)return '<div class="channel-combo-empty">频道候选加载失败，仍可手动输入新的频道名。</div>';
                  const options=filteredChannelOptions(draft.channelOptions||appState.channelOptions||[],draft.channel), current=normalizeChannelName(draft.channel).toLowerCase(), active=Math.max(0,Number(draft.channelComboIndex||0));
                  if(options.length===0)return '<div class="channel-combo-empty">没有匹配的已有频道，可直接保存为新频道</div>';
                  return options.map((c,index)=>`<button type="button" class="channel-combo-option ${index===active?'active':''} ${String(c.channel||'').trim().toLowerCase()===current?'selected':''}" role="option" onmousedown="event.preventDefault()" onclick='selectSignalListenerBasicConfigChannel(${jsString(ref)},${jsString(c.channel||'')})'><strong>${esc(c.channel||'未命名频道')}</strong><span>${esc(channelOptionLabel(c))}</span></button>`).join('');
                }
                function renderSignalListenerConfigChannelCombo(ref,draft){
                  const id=signalListenerFieldId(ref), open=draft.channelComboOpen?' open':'';
                  return `<div id="listener-channel-combo-${id}" class="channel-combo listener-channel-combo${open}"><div class="channel-combo-control"><input id="listener-channel-${id}" class="input" maxlength="128" value="${esc(draft.channel||'')}" placeholder="选择已有频道或输入新频道" autocomplete="off" role="combobox" aria-expanded="${draft.channelComboOpen?'true':'false'}" aria-controls="listener-channel-menu-${id}" onfocus='openSignalListenerBasicConfigChannelMenu(${jsString(ref)})' oninput='updateSignalListenerBasicConfigDraftFromForm(${jsString(ref)},true)' onkeydown='handleSignalListenerBasicConfigChannelKey(event,${jsString(ref)})'><button class="channel-combo-toggle" type="button" onclick='toggleSignalListenerBasicConfigChannelMenu(${jsString(ref)})' aria-label="显示已有频道">⌄</button></div><div id="listener-channel-menu-${id}" class="channel-combo-menu" role="listbox">${listenerChannelComboOptionsHtml(ref,draft)}</div></div>`;
                }
                function signalListenerBasicConfigForm(e,detail,draft){
                  const id=signalListenerFieldId(draft.listenerRef), errs=draft.errors?.length?`<ul class="validation-list">${draft.errors.map(err=>`<li>${esc(err.field?`${err.field}：`: '')}${esc(err.message||'保存失败')}</li>`).join('')}</ul>`:'';
                  const conflict=draft.conflict?`<div class="readonly-note">${esc(draft.errors?.[0]?.message||'Listener 基础配置已发生冲突，请刷新后再编辑。')} <button class="link-button" onclick='reloadSignalListenerBasicConfigAfterConflict(${jsString(draft.listenerRef)},${jsString(draft.routeChannel||detail.channel)})'>刷新当前信息</button></div>`:'';
                  return `<form class="edit-form" onsubmit='event.preventDefault();saveSignalListenerBasicConfig(${jsString(draft.listenerRef)},${jsString(draft.routeChannel||detail.channel)})'><label>启用状态<select id="listener-enabled-${id}" class="select"><option value="true" ${draft.enabled?'selected':''}>启用</option><option value="false" ${!draft.enabled?'selected':''}>禁用</option></select></label><label>频道${renderSignalListenerConfigChannelCombo(draft.listenerRef,draft)}<span id="listener-channel-hint-${id}" class="muted">${channelHintHtml(draft.channel,draft.channelOptions||appState.channelOptions||[],draft.channelOptionsError||appState.channelOptionsError)}</span></label><label>冷却时间（ticks）<input id="listener-cooldown-${id}" class="input" type="number" min="0" max="72000" step="1" value="${esc(draft.cooldownTicks ?? 0)}"></label><p class="readonly-note">正在编辑 Signal Listener 基础配置。Action 列表保持只读，不会被修改。锁到期：${fmtTime(draft.lock?.expiresAt)}</p>${errs}${conflict}<div class="form-actions"><button class="primary" type="submit" ${draft.saving?'disabled':''}>${draft.saving?'保存中...':'保存'}</button><button class="secondary" type="button" onclick='cancelSignalListenerBasicConfigEdit(${jsString(draft.listenerRef)},${jsString(draft.routeChannel||detail.channel)})'>取消</button></div></form>`;
                }
                function updateSignalListenerBasicConfigDraftFromForm(ref,openMenu=false){
                  const draft=appState.signalListenerBasicConfigEdit;if(!draft||draft.listenerRef!==ref)return;
                  const id=signalListenerFieldId(ref);
                  draft.enabled=(document.getElementById(`listener-enabled-${id}`)?.value||'false')==='true';
                  draft.channel=document.getElementById(`listener-channel-${id}`)?.value||'';
                  draft.cooldownTicks=document.getElementById(`listener-cooldown-${id}`)?.value||'0';
                  if(openMenu){draft.channelComboOpen=true;draft.channelComboIndex=0;}
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
                function openSignalListenerBasicConfigChannelMenu(ref){const draft=appState.signalListenerBasicConfigEdit;if(!draft||draft.listenerRef!==ref)return;updateSignalListenerBasicConfigDraftFromForm(ref,false);draft.channelComboOpen=true;syncSignalListenerBasicConfigChannelCombo(ref);}
                function toggleSignalListenerBasicConfigChannelMenu(ref){const draft=appState.signalListenerBasicConfigEdit;if(!draft||draft.listenerRef!==ref)return;updateSignalListenerBasicConfigDraftFromForm(ref,false);draft.channelComboOpen=!draft.channelComboOpen;syncSignalListenerBasicConfigChannelCombo(ref);document.getElementById(`listener-channel-${signalListenerFieldId(ref)}`)?.focus();}
                function selectSignalListenerBasicConfigChannel(ref,channel){const draft=appState.signalListenerBasicConfigEdit;if(!draft||draft.listenerRef!==ref)return;draft.channel=channel||'';draft.channelComboOpen=false;draft.channelComboIndex=0;const id=signalListenerFieldId(ref), input=document.getElementById(`listener-channel-${id}`), hint=document.getElementById(`listener-channel-hint-${id}`);if(input)input.value=draft.channel;if(hint)hint.innerHTML=channelHintHtml(draft.channel,draft.channelOptions||appState.channelOptions||[],draft.channelOptionsError||appState.channelOptionsError);syncSignalListenerBasicConfigChannelCombo(ref);}
                function handleSignalListenerBasicConfigChannelKey(event,ref){
                  const draft=appState.signalListenerBasicConfigEdit;if(!draft||draft.listenerRef!==ref)return;
                  const options=filteredChannelOptions(draft.channelOptions||appState.channelOptions||[],document.getElementById(`listener-channel-${signalListenerFieldId(ref)}`)?.value||draft.channel);
                  if(event.key==='Escape'){draft.channelComboOpen=false;syncSignalListenerBasicConfigChannelCombo(ref);return;}
                  if(event.key==='ArrowDown'||event.key==='ArrowUp'){event.preventDefault();draft.channelComboOpen=true;const max=Math.max(0,options.length-1), next=event.key==='ArrowDown'?Number(draft.channelComboIndex||0)+1:Number(draft.channelComboIndex||0)-1;draft.channelComboIndex=Math.min(max,Math.max(0,next));syncSignalListenerBasicConfigChannelCombo(ref);return;}
                  if(event.key==='Enter'&&draft.channelComboOpen&&options.length>0){event.preventDefault();selectSignalListenerBasicConfigChannel(ref,options[Math.min(options.length-1,Number(draft.channelComboIndex||0))].channel);return;}
                }
                function maybeReleaseSignalListenerBasicConfigEditForRoute(hash){const draft=appState.signalListenerBasicConfigEdit;if(!draft)return;if(String(hash||'').startsWith('#/signals/')){const info=detailRoute(String(hash).substring('#/signals/'.length),'#/signals');if(info.id===(draft.routeChannel||draft.channel))return;}releaseSignalListenerBasicConfigLock(draft,true);appState.signalListenerBasicConfigEdit=null;stopSignalListenerBasicConfigLockHeartbeat();}
                async function startSignalListenerBasicConfigEdit(ref,routeChannel){
                  if(!canEditSignalListenerBasicConfig())return;
                  try{
                    const cfg=await api(`/api/webadmin/signal-listener-basic-config/${encodeURIComponent(ref)}`);
                    const result=await api('/api/webadmin/edit-locks/acquire',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'signal_listener_basic_config',targetId:cfg.listenerId||cfg.listenerRef||ref})});
                    if(!result.success){toast(result.message||'无法获取编辑锁');await renderSignalDetail(encodeURIComponent(routeChannel||cfg.channel||''),{silent:true});return;}
                    const lock=result.data?.lock||{}, channelOptions=await loadSignalChannelOptions();
                    appState.signalListenerBasicConfigEdit={listenerRef:cfg.listenerRef||ref,listenerId:cfg.listenerId||ref,displayName:cfg.displayName||ref,enabled:!!cfg.enabled,channel:cfg.channel||'',cooldownTicks:cfg.cooldownTicks ?? 0,actionCount:cfg.actionCount||0,actionSummaries:cfg.actionSummaries||[],expectedFingerprint:cfg.expectedFingerprint||'',routeChannel:routeChannel||cfg.channel||'',lockId:lock.lockId||'',lock,channelOptions,channelOptionsError:appState.channelOptionsError,channelComboOpen:false,channelComboIndex:0,errors:[],saving:false,conflict:null};
                    scheduleSignalListenerBasicConfigLockHeartbeat();
                    await renderSignalDetail(encodeURIComponent(routeChannel||cfg.channel||''),{silent:true});
                  }catch(err){toast(err.message||'无法获取编辑锁');}
                }
                async function cancelSignalListenerBasicConfigEdit(ref,routeChannel){const draft=appState.signalListenerBasicConfigEdit;if(draft&&draft.listenerRef===ref){await releaseSignalListenerBasicConfigLock(draft,false);appState.signalListenerBasicConfigEdit=null;stopSignalListenerBasicConfigLockHeartbeat();}await renderSignalDetail(encodeURIComponent(routeChannel||''),{silent:true});}
                async function reloadSignalListenerBasicConfigAfterConflict(ref,routeChannel){const draft=appState.signalListenerBasicConfigEdit;if(draft&&draft.listenerRef===ref)await releaseSignalListenerBasicConfigLock(draft,true);appState.signalListenerBasicConfigEdit=null;stopSignalListenerBasicConfigLockHeartbeat();await renderSignalDetail(encodeURIComponent(routeChannel||''),{silent:true});}
                function scheduleSignalListenerBasicConfigLockHeartbeat(){stopSignalListenerBasicConfigLockHeartbeat();appState.signalListenerBasicConfigLockTimer=setTimeout(async()=>{await heartbeatSignalListenerBasicConfigLock();if(appState.signalListenerBasicConfigEdit)scheduleSignalListenerBasicConfigLockHeartbeat();},30000);}
                function stopSignalListenerBasicConfigLockHeartbeat(){if(appState.signalListenerBasicConfigLockTimer){clearTimeout(appState.signalListenerBasicConfigLockTimer);appState.signalListenerBasicConfigLockTimer=null;}}
                async function heartbeatSignalListenerBasicConfigLock(){const draft=appState.signalListenerBasicConfigEdit;if(!draft||!draft.lockId)return;try{const result=await api('/api/webadmin/edit-locks/heartbeat',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'signal_listener_basic_config',targetId:draft.listenerId,lockId:draft.lockId})});if(result.success){draft.lock=result.data?.lock||draft.lock;return;}draft.errors=[{message:result.message||'编辑锁续期失败'}];stopSignalListenerBasicConfigLockHeartbeat();await renderSignalDetail(encodeURIComponent(draft.routeChannel||draft.channel||''),{silent:true});}catch(err){draft.errors=[{message:err.message||'编辑锁续期失败'}];stopSignalListenerBasicConfigLockHeartbeat();}}
                async function releaseSignalListenerBasicConfigLock(draft,silent){if(!draft||!draft.lockId)return;try{await api('/api/webadmin/edit-locks/release',{method:'POST',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({targetType:'signal_listener_basic_config',targetId:draft.listenerId,lockId:draft.lockId})});}catch(err){if(!silent)toast(err.message||'编辑锁释放失败，将等待自动过期。');}}
                async function saveSignalListenerBasicConfig(ref,routeChannel){
                  const draft=appState.signalListenerBasicConfigEdit||{listenerRef:ref,routeChannel};
                  updateSignalListenerBasicConfigDraftFromForm(ref,false);
                  draft.saving=true;draft.errors=[];draft.conflict=null;appState.signalListenerBasicConfigEdit=draft;renderSignalDetail(encodeURIComponent(routeChannel||draft.routeChannel||draft.channel||''),{silent:true});
                  try{
                    const result=await api(`/api/webadmin/signal-listener-basic-config/${encodeURIComponent(ref)}`,{method:'PATCH',headers:{'X-TZZ-WebAdmin-CSRF':csrfToken()},body:JSON.stringify({enabled:draft.enabled,channel:draft.channel,cooldownTicks:Number(draft.cooldownTicks),expectedFingerprint:draft.expectedFingerprint,lockId:draft.lockId})});
                    if(result.success){appState.signalListenerBasicConfigEdit=null;stopSignalListenerBasicConfigLockHeartbeat();toast(result.changed?(result.message||'Signal Listener 基础配置已保存。'):'没有变更。');await renderSignalDetail(encodeURIComponent(routeChannel||draft.routeChannel||draft.channel||''),{silent:true});return;}
                    draft.saving=false;draft.errors=result.validationErrors&&result.validationErrors.length?result.validationErrors:[{message:result.message||'保存失败'}];draft.conflict=result.conflict||null;appState.signalListenerBasicConfigEdit=draft;if(['edit_lock_expired','edit_lock_conflict','edit_lock_required'].includes(result.code))stopSignalListenerBasicConfigLockHeartbeat();toast(result.message||'保存失败');await renderSignalDetail(encodeURIComponent(routeChannel||draft.routeChannel||draft.channel||''),{silent:true});
                  }catch(err){draft.saving=false;draft.errors=[{message:err.message||'保存失败'}];appState.signalListenerBasicConfigEdit=draft;toast(err.message||'保存失败');await renderSignalDetail(encodeURIComponent(routeChannel||draft.routeChannel||draft.channel||''),{silent:true});}
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
                  appState.receiverFilters=appState.receiverFilters||{search:'',enabled:'ALL',output:'ALL',channel:'ALL'};
                  appState.receiverDetailCache=appState.receiverDetailCache||{};
                  appState.configFilters=appState.configFilters||{search:'',status:'ALL',type:'ALL'};
                  appState.userFilters=appState.userFilters||{search:'',role:'ALL',enabled:'ALL',online:'ALL'};
                  appState.regionFilters=appState.regionFilters||{search:'',world:'ALL',enabled:'ALL',doctor:'ALL',players:'ALL',sort:'NAME'};
                  appState.regionControllerFilters=appState.regionControllerFilters||{search:'',enabled:'ALL',target:'ALL',event:'ALL'};
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
                  if(key==='devices')renderDeviceList('');
                  if(key==='virtualBlockDevices')renderVirtualBlockList('');
                  if(key==='actions')renderActionList('');
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
                function openWebAdminModal(title,body,footer=''){
                  closeWebAdminModal();
                  const wrap=document.createElement('div');
                  wrap.className='wa-modal-backdrop';
                  wrap.id='wa-modal-root';
                  wrap.innerHTML=`<section class="wa-modal" role="dialog" aria-modal="true" aria-labelledby="wa-modal-title"><header class="wa-modal-head"><h2 id="wa-modal-title" class="wa-modal-title">${esc(title)}</h2>${waIconButton('关闭','close','onclick="closeWebAdminModal()"')}</header><div class="wa-modal-body">${body}</div><footer class="wa-modal-foot">${footer||waButton('关闭','','onclick="closeWebAdminModal()"','ghost')}</footer></section>`;
                  wrap.addEventListener('click',event=>{if(event.target===wrap)closeWebAdminModal();});
                  document.body.appendChild(wrap);
                }
                function closeWebAdminModal(){document.getElementById('wa-modal-root')?.remove();}
                document.addEventListener('keydown',event=>{if(event.key==='Escape')closeWebAdminModal();});
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
                  return `<div class="wa-table-scroll"><table class="wa-table"><thead><tr><th>接收器名称 / ID</th><th>监听频道</th><th>位置</th><th>脉冲时长</th><th>输出状态</th><th>启用状态</th><th>最近触发</th><th>操作</th></tr></thead><tbody>${items.map(d=>{const target='#/devices/'+encodeURIComponent(d.id), title=d.displayName||d.id;return `<tr class="wa-clickable-row" ${navDataAttr(target,`查看接收器 ${title}`)}><td><span class="device-name"><span class="device-icon">${icon('receiver-row')}</span><span><strong>${esc(title)}</strong><span class="device-subtitle">ID: ${esc(shortId(d.id))}</span></span></span></td><td class="truncate">${channelCell(d.channel)}</td><td class="truncate">${esc(posText(d.pos))}</td><td>${receiverPulseCell(d)}</td><td>${receiverOutputActive(d)?textPill('输出中','error'):textPill('空闲','info')}</td><td>${pill(d.enabled?'OK':'WARNING')} ${esc(d.enabled?'启用':'禁用')}</td><td>${fmtTime(d.lastTriggeredAt)}</td><td><div class="wa-action-cell"><button class="wa-btn ghost" ${navDataAttr(target,`查看接收器 ${title}`)}>查看</button>${waIconButton('更多','more','disabled')}</div></td></tr>`;}).join('')}</tbody></table></div>`;
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
                function endpointEnabledText(value){return value?'启用':'停用';}
                function deviceTypeIcon(type){return {SIGNAL_EMITTER:'signal-device',SIGNAL_RECEIVER:'signal-receiver',ACTION_RELAY:'action-relay',VIRTUAL_BLOCK_DEVICE:'virtual-block-device'}[String(type||'').toUpperCase()]||'device';}
                function deviceTypeTone(type){return {SIGNAL_EMITTER:'',SIGNAL_RECEIVER:'',ACTION_RELAY:'warning',VIRTUAL_BLOCK_DEVICE:'ok'}[String(type||'').toUpperCase()]||'';}
                function distributionItems(items,mapper,labeler,totalOverride){
                  const total=Number(totalOverride??items.length), counts=countBy(items,mapper);
                  return Object.entries(counts).sort((a,b)=>b[1]-a[1]).map(([key,value])=>({label:labeler?labeler(key):key,value,total,kind:key==='ERROR'||key==='FAILED'||key==='DISABLED'?'error':(key==='WARNING'?'warning':(key==='OK'||key==='ENABLED'?'ok':''))}));
                }
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
                    ${waPageHead('信号监听器','后台虚拟监听器用于监听 channel 并执行 actions，不等同于 signal_receiver 方块。',`${waButton('添加监听器','plus','disabled','primary')}${waButton('批量导入','upload','disabled','ghost')}${waButton('导出配置','download','disabled','ghost')}`)}
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
                  return `<div class="wa-table-scroll"><table class="wa-table"><thead><tr><th>监听器名称 / ID</th><th>频道</th><th>状态</th><th>冷却时间</th><th>动作数量</th><th>最后触发</th><th>操作</th></tr></thead><tbody>${items.map(l=>{const title=l.name||l.id||'未命名监听器', channel=l.channel||l.sourceChannel||'', channelTarget=isBlank(channel)?'':signalHash(channel);return `<tr><td><span class="device-name"><span class="device-icon">${icon('consumer-listener')}</span><span><strong>${esc(title)}</strong><span class="device-subtitle">ID: ${esc(shortId(l.id))}</span></span></span></td><td class="truncate" title="${esc(channel)}">${channelCell(channel)}</td><td>${pill(l.enabled!==false?'OK':'WARNING')} ${esc(endpointEnabledText(l.enabled!==false))}</td><td>${esc(formatTicks(l.cooldownTicks)||'0 tick')}</td><td>${esc(l.actionCount ?? '--')}</td><td>${fmtTime(l.lastTriggeredAt)}</td><td><div class="wa-action-cell">${channelTarget?`<button class="wa-btn ghost" ${navDataAttr(channelTarget,`查看频道 ${channel}`)}>频道</button>`:`<button class="wa-btn ghost" disabled>频道</button>`}${waIconButton('编辑不可用','settings','disabled')}</div></td></tr>`;}).join('')}</tbody></table></div>`;
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
                function deviceTable(items){
                  return `<div class="wa-table-scroll"><table class="wa-table"><thead><tr><th>设备名称 / ID</th><th>类型</th><th>频道</th><th>位置</th><th>状态</th><th>最近触发</th><th>Doctor</th><th>操作</th></tr></thead><tbody>${items.map(d=>{const target='#/devices/'+encodeURIComponent(d.id), title=d.displayName||d.id;return `<tr class="wa-clickable-row" ${navDataAttr(target,`查看设备 ${title}`)}><td><span class="device-name"><span class="device-icon">${icon(deviceTypeIcon(d.type))}</span><span><strong>${esc(title)}</strong><span class="device-subtitle">ID: ${esc(shortId(d.id))}</span></span></span></td><td>${textPill(labelType(d.type),deviceTypeTone(d.type)||'info')}</td><td class="truncate" title="${esc(d.channel||'')}">${channelCell(d.channel)}</td><td class="truncate" title="${esc((d.world||'')+' '+posText(d.pos))}">${esc(d.world||'-')} / ${esc(posText(d.pos))}</td><td>${pill(d.enabled?'OK':'WARNING')} ${esc(labelBool(d.enabled))}</td><td>${fmtTime(d.lastTriggeredAt)}</td><td>${pill(d.doctorStatus)}</td><td><div class="wa-action-cell"><button class="wa-btn ghost" ${navDataAttr(target,`查看设备 ${title}`)}>查看</button>${waIconButton('更多','more','disabled')}</div></td></tr>`;}).join('')}</tbody></table></div>`;
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
                    ${waPageHead('虚拟方块设备','绑定已有 Minecraft 方块作为虚拟事件源；方块材质不使用 WebAdmin 图标伪造。',`${waButton('添加虚拟设备','plus','disabled','primary')}${waButton('批量导入','upload','disabled','ghost')}${waButton('导出配置','download','disabled','danger')}`)}
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
                  return `<div class="wa-table-scroll"><table class="wa-table"><thead><tr><th>设备名称 / ID</th><th>绑定方块</th><th>位置</th><th>触发类型</th><th>通道</th><th>状态</th><th>条件配置</th><th>最近触发</th><th>操作</th></tr></thead><tbody>${items.map(d=>{const target='#/devices/'+encodeURIComponent(d.id), title=d.displayName||d.id, block=virtualBlockId(d), trigger=virtualTriggerType(d);return `<tr class="wa-clickable-row" ${navDataAttr(target,`查看虚拟方块设备 ${title}`)}><td><span class="device-name"><span class="device-icon">${icon('virtual-block-device')}</span><span><strong>${esc(title)}</strong><span class="device-subtitle">ID: ${esc(shortId(d.id))}</span></span></span></td><td><span>${esc(block||'--')}</span><span class="device-subtitle">原版材质未在列表 API 中提供时仅显示文本</span></td><td class="truncate" title="${esc((d.world||'')+' '+posText(d.pos))}">${esc(d.world||'-')} / ${esc(posText(d.pos))}</td><td>${textPill(labelTriggerType(trigger),trigger==='container'?'warning':(trigger==='interact'?'ok':'info'))}</td><td class="truncate" title="${esc(d.channel||'')}">${channelCell(d.channel)}</td><td>${pill(d.enabled?'OK':'WARNING')} ${esc(labelBool(d.enabled))}</td><td>${esc(virtualConditionText(d))}</td><td>${fmtTime(d.lastTriggeredAt)}</td><td><div class="wa-action-cell"><button class="wa-btn ghost" ${navDataAttr(target,`查看虚拟方块设备 ${title}`)}>查看</button>${waIconButton('更多','more','disabled')}</div></td></tr>`;}).join('')}</tbody></table></div>`;
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
                    <div class="wa-tabs"><button class="wa-tab active">事件列表</button><button class="wa-tab" disabled>实时流 Live</button></div>
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
                    <div class="wa-tabs"><button class="wa-tab active">配置版本列表</button><button class="wa-tab" disabled>草稿</button><button class="wa-tab" disabled>发布记录</button><button class="wa-tab" disabled>备份记录</button><button class="wa-tab" disabled>导入记录</button><button class="wa-tab" disabled>变更对比 Diff</button><button class="wa-tab" disabled>审计日志</button></div>
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
                  setView(`<section class="wa-page">
                    ${waPageHead('系统设置','只读查看 WebAdmin 服务状态、运行环境、安全设置和功能开关。',`${waButton('保存设置','settings','disabled','primary')}${waButton('重置默认','archive','disabled','ghost')}`)}
                    <div class="wa-tabs"><button class="wa-tab active">通用设置</button><button class="wa-tab" disabled>安全设置</button><button class="wa-tab" disabled>性能设置</button><button class="wa-tab" disabled>界面设置</button><button class="wa-tab" disabled>通知设置</button><button class="wa-tab" disabled>备份与维护</button><button class="wa-tab" disabled>关于</button></div>
                    <section class="wa-two-column">
                      <div class="wa-settings-main">
                        <article class="wa-panel"><h2>平台信息（只读）</h2><div class="identity-grid">${row('平台名称',esc('TZZ Mod WebAdmin'))}${row('平台版本',esc(system.modVersion||'unknown'))}${row('Minecraft 版本',esc(system.minecraftVersion||'--'))}${row('服务器类型',esc(labelServerType(system.serverType)))}${row('当前世界 / 存档',esc(system.worldName||'--'))}${row('访问模式',esc(labelAccessMode(service.accessMode)))}</div></article>
                        <article class="wa-panel"><h2>服务信息（只读）</h2><div class="identity-grid">${row('服务状态',pill(service.running?'OK':'WARNING'))}${row('服务地址',esc(service.url||'--'))}${row('监听地址',esc(`${service.host||'-'}:${service.port||'-'}`))}${row('当前用户',esc(service.currentUser||'--'))}${row('当前角色',esc(labelRoleFull(service.currentRole)))}${row('Session 数量',esc(runtime.webAdmin?.sessionCount ?? system.sessionCount ?? '--'))}</div></article>
                        <article class="wa-panel"><h2>功能开关（只读）</h2><div class="wa-setting-grid">${settingReadonlyToggle('启用 SignalBridge','信号系统由当前服务端模块提供',true)}${settingReadonlyToggle('启用事件记录','记录 signal/action 等已有历史',true)}${settingReadonlyToggle('启用审计日志','WebAdmin 写操作审计',!!audit.enabled)}${settingReadonlyToggle('启用 Web API','当前 WebAdmin API 正在服务',true)}${settingReadonlyToggle('启用自动修复','Doctor 自动修复未开放',false)}${settingReadonlyToggle('启用设置写入','系统设置写入未开放',false)}</div></article>
                        <article class="wa-panel"><h2>运行环境信息（只读）</h2><div class="identity-grid">${row('认证方式',esc(security.authMode||'USERNAME_PASSWORD'))}${row('密码算法',esc(security.passwordHashAlgorithm||'PBKDF2'))}${row('Session Cookie',esc(security.sessionCookieName||'--'))}${row('Session 有效期',esc(formatMinutes(security.sessionTtlMinutes)))}${row('Remember Me 有效期',esc(formatMinutes(security.rememberMeTtlMinutes)))}${row('Secure Cookie',esc(security.secureCookie?'启用':'未启用'))}</div></article>
                      </div>
                      ${settingsRightRail(data,runtime,capabilities.ok?capabilities.data:{})}
                    </section>
                  </section>`,options);
                }
                function settingReadonlyToggle(label,desc,enabled){return `<div class="setting-row"><span><strong>${esc(label)}</strong><small>${esc(desc)}</small></span>${textPill(enabled?'启用':'禁用',enabled?'ok':'info')}</div>`;}
                function settingsRightRail(data,status,capabilities){const storage=data.storage||{}, security=data.security||{}, service=data.service||{}, system=data.system||{};return `<aside class="wa-right-rail">
                  <article class="wa-panel"><h2>系统状态</h2><div class="list-stack"><div class="kv-row"><span class="muted">平台状态</span><strong>${pill(service.running?'OK':'WARNING')}</strong></div><div class="kv-row"><span class="muted">在线连接</span><strong>${esc(status.webAdmin?.sessionCount ?? system.sessionCount ?? '--')}</strong></div><div class="kv-row"><span class="muted">审计日志</span><strong>${esc(security.auditEnabled?'启用':'关闭')}</strong></div></div></article>
                  <article class="wa-panel"><h2>环境摘要</h2><div class="list-stack"><div class="kv-row"><span class="muted">存储作用域</span><strong>${esc(storage.scope||'WORLD_SAVE')}</strong></div><div class="kv-row"><span class="muted">按世界隔离</span><strong>${esc(storage.worldScoped?'是':'否')}</strong></div><div class="kv-row"><span class="muted">敏感路径</span><strong>${esc(storage.restricted?'已隐藏':'可见')}</strong></div><div class="kv-row"><span class="muted">旧全局文件</span><strong>${esc(storage.legacyGlobalFilesDetected?'检测到':'未检测到')}</strong></div></div></article>
                  <article class="wa-panel"><h2>快速操作</h2><div class="wa-quick-grid">${waButton('查看系统日志','eye','disabled','ghost')}${waButton('查看审计日志','history','disabled','ghost')}${waButton('检查更新','refresh','disabled','ghost')}${waButton('导出诊断','download','disabled','ghost')}${waButton('重启 WebAdmin','critical-issue','disabled','danger')}</div><p class="wa-disabled-note">保存、重启、清理缓存和危险维护操作没有完整后端安全能力，本轮保持禁用。</p></article>
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
