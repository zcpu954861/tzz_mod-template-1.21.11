package com.zcpu.tzzmod.webadmin;

import com.zcpu.tzzmod.action.ActionType;
import com.zcpu.tzzmod.action.schema.ActionCapabilityMatrix;
import com.zcpu.tzzmod.action.schema.ActionFieldType;
import com.zcpu.tzzmod.action.schema.ActionFieldOption;
import com.zcpu.tzzmod.action.schema.ActionFieldSchema;
import com.zcpu.tzzmod.action.schema.ActionOwnerCapability;
import com.zcpu.tzzmod.action.schema.ActionSchema;
import com.zcpu.tzzmod.action.schema.ActionSchemaRegistry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WebAdminActionSchemaScripts {
    private WebAdminActionSchemaScripts() {
    }

    public static String appJs() {
        String actionOrderJson = WebAdminJsonResponse.GSON.toJson(actionOrder());
        String actionsJson = WebAdminJsonResponse.GSON.toJson(actions());
        String ownersJson = WebAdminJsonResponse.GSON.toJson(owners());
        String fieldTypesJson = WebAdminJsonResponse.GSON.toJson(fieldTypes());
        String nonOwnersJson = WebAdminJsonResponse.GSON.toJson(explicitNonOwners());
        return """
                // data-typed-action-schema-export="true"
                // Typed Action editor data 来自 Java ActionSchemaRegistry / ActionCapabilityMatrix。
                // 前端只用它渲染字段和过滤 owner 可选项；后端 ActionValidationService 仍是保存时的权威校验。
                function typedActionEditorActions(){return Object.freeze("""
                + actionsJson
                + """
                );
                }
                function typedActionEditorOwners(){return Object.freeze("""
                + ownersJson
                + """
                );
                }
                function typedActionEditorData(){return Object.freeze({version:'9.2-phase3',actionOrder:"""
                + actionOrderJson
                + """
                ,supportedFieldTypes:"""
                + fieldTypesJson
                + """
                ,actions:typedActionEditorActions(),owners:typedActionEditorOwners(),explicitNonOwners:"""
                + nonOwnersJson
                + """
                });}
                const TZZ_ACTION_EDITOR_DATA = typedActionEditorData();
                function actionEditorSchemaData(){return TZZ_ACTION_EDITOR_DATA;}
                function normalizeActionTypeId(type){return String(type||'').trim().toLowerCase();}
                function actionSchemaByType(type){const id=normalizeActionTypeId(type);return (TZZ_ACTION_EDITOR_DATA.actions||{})[id]||null;}
                function labelActionType(type){const id=normalizeActionTypeId(type), legacy={command:'命令',signal:'Signal',message:'消息',sound:'音效',state_variable:'状态变量动作',timer_start:'启动 Timer',timer_cancel:'取消 Timer'};const schema=actionSchemaByType(id);return legacy[id]||schema?.displayName||String(type||'未知动作');}
                function actionOwnerCapability(ownerId){const id=String(ownerId||'').trim().toLowerCase();return (TZZ_ACTION_EDITOR_DATA.owners||{})[id]||null;}
                function actionOwnerIdFromTimerBucket(bucket){
                  const b=String(bucket||'').trim().toLowerCase();
                  if(['timer_on_start','start','onstartactions','on_start','on-start'].includes(b))return 'timer_on_start';
                  if(['timer_on_tick','tick','ontickactions','on_tick','on-tick'].includes(b))return 'timer_on_tick';
                  if(['timer_on_complete','complete','oncompleteactions','on_complete','on-complete'].includes(b))return 'timer_on_complete';
                  if(['timer_on_cancel','cancel','oncancelactions','on_cancel','on-cancel'].includes(b))return 'timer_on_cancel';
                  return '';
                }
                function actionOwnerId(ownerType,bucket=''){
                  const owner=String(ownerType||'').trim().toLowerCase(), b=String(bucket||'').trim().toLowerCase();
                  if(actionOwnerCapability(owner))return owner;
                  if(owner==='listener'||owner==='signal_listener'||owner==='signal-listener')return 'signal_listener';
                  if(owner==='action_relay'||owner==='action-relay')return 'action_relay';
                  if(owner==='region_enter'||owner==='region-enter'||(owner==='region_controller'&&['enter','enteractions'].includes(b)))return 'region_enter';
                  if(owner==='region_exit'||owner==='region-exit'||(owner==='region_controller'&&['exit','exitactions'].includes(b)))return 'region_exit';
                  if(owner==='region_stay'||owner==='region-stay'||(owner==='region_controller'&&['stay','stayactions'].includes(b)))return 'region_stay';
                  if(owner==='timer')return actionOwnerIdFromTimerBucket(b);
                  return '';
                }
                function actionSupportedTypesForOwner(ownerId,fallbackTypes=null){
                  const id=String(ownerId||'').trim().toLowerCase(), cap=actionOwnerCapability(id);
                  if(cap)return (cap.supportedActionTypes||[]).filter(type=>!!actionSchemaByType(type));
                  if(id&&(TZZ_ACTION_EDITOR_DATA.explicitNonOwners||[]).includes(id))return [];
                  if(Array.isArray(fallbackTypes))return fallbackTypes.map(normalizeActionTypeId).filter(type=>!!actionSchemaByType(type));
                  return [];
                }
                function actionOwnerSupports(ownerId,type){return actionSupportedTypesForOwner(ownerId,[]).includes(normalizeActionTypeId(type));}
                function actionTypeOptions(value,ownerId='',fallbackTypes=null){
                  const selected=normalizeActionTypeId(value||'signal'), ownerKey=String(ownerId||'').trim().toLowerCase(), cap=actionOwnerCapability(ownerKey);
                  let types=actionSupportedTypesForOwner(ownerId,fallbackTypes);
                  const canPreserveSelected=!!cap||(Array.isArray(fallbackTypes)&&fallbackTypes.length>0);
                  if(canPreserveSelected&&selected&&actionSchemaByType(selected)&&!types.includes(selected))types=[selected,...types];
                  return types.map(type=>`<option value="${type}" ${selected===type?'selected':''}>${esc(labelActionType(type))} · ${type}</option>`).join('');
                }
                function typedActionUnsupportedOwnerIds(){return [...(TZZ_ACTION_EDITOR_DATA.explicitNonOwners||[])];}
                """;
    }

    static Map<String, Object> actionEditorDataForTest() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("version", "9.2-phase3");
        data.put("actionOrder", actionOrder());
        data.put("supportedFieldTypes", fieldTypes());
        data.put("actions", actions());
        data.put("owners", owners());
        data.put("explicitNonOwners", explicitNonOwners());
        return data;
    }

    private static List<String> fieldTypes() {
        List<String> result = new ArrayList<>();
        for (ActionFieldType type : ActionFieldType.values()) {
            result.add(type.id());
        }
        return List.copyOf(result);
    }

    private static List<String> explicitNonOwners() {
        return List.of("vbd_trigger", "item_submit", "container_change", "branch");
    }

    private static List<String> actionOrder() {
        List<String> order = new ArrayList<>();
        for (ActionSchema schema : ActionSchemaRegistry.schemas()) {
            order.add(schema.id());
        }
        return List.copyOf(order);
    }

    private static Map<String, Object> actions() {
        Map<String, Object> actions = new LinkedHashMap<>();
        for (ActionSchema schema : ActionSchemaRegistry.schemas()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", schema.id());
            item.put("displayName", schema.displayName());
            item.put("supportsConditionGroup", schema.supportsConditionGroup());
            item.put("requiresTargetPicker", schema.requiresTargetPicker());
            item.put("fields", fields(schema.fields()));
            actions.put(schema.id(), item);
        }
        return actions;
    }

    private static List<Map<String, Object>> fields(List<ActionFieldSchema> schemas) {
        List<Map<String, Object>> fields = new ArrayList<>();
        for (ActionFieldSchema field : schemas) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", field.id());
            item.put("label", field.label());
            item.put("type", field.type().id());
            item.put("required", field.required());
            item.put("defaultValue", field.defaultValue());
            item.put("maxLength", field.maxLength());
            item.put("minNumber", field.minNumber());
            item.put("maxNumber", field.maxNumber());
            List<Map<String, String>> options = options(field.options());
            if (!options.isEmpty()) {
                item.put("options", options);
            }
            fields.add(item);
        }
        return List.copyOf(fields);
    }

    private static List<Map<String, String>> options(List<ActionFieldOption> options) {
        List<Map<String, String>> result = new ArrayList<>();
        for (ActionFieldOption option : options) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("value", option.value());
            item.put("label", option.label());
            result.add(item);
        }
        return List.copyOf(result);
    }

    private static Map<String, Object> owners() {
        Map<String, Object> owners = new LinkedHashMap<>();
        for (ActionOwnerCapability capability : ActionCapabilityMatrix.capabilities()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", capability.ownerType().id());
            item.put("displayName", capability.ownerType().displayName());
            item.put("maxActions", capability.maxActions());
            item.put("actionConditionTargetType", capability.actionConditionTargetType().name());
            item.put("supportsSameBucketReorder", capability.supportsSameBucketReorder());
            item.put("supportedActionTypes", supportedActionTypes(capability));
            owners.put(capability.ownerType().id(), item);
        }
        return owners;
    }

    private static List<String> supportedActionTypes(ActionOwnerCapability capability) {
        List<String> result = new ArrayList<>();
        for (String id : actionOrder()) {
            ActionSchemaRegistry.findById(id)
                    .map(ActionSchema::actionType)
                    .filter(type -> capability.supports((ActionType) type))
                    .ifPresent(type -> result.add(id));
        }
        return List.copyOf(result);
    }
}
