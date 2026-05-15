package com.zcpu.tzzmod.condition;

public final class ConditionNodeType {
    public static final String GROUP = "group";
    public static final String ALWAYS_TRUE = "always_true";
    public static final String ALWAYS_FALSE = "always_false";
    public static final String CONTEXT_EXISTS = "context_exists";
    public static final String CONTEXT_FIELD_EXISTS = "context_field_exists";
    public static final String CONTEXT_EQUALS = "context_equals";
    public static final String PLAYER_EXISTS = "player_exists";
    public static final String PLAYER_ONLINE = "player_online";
    public static final String PLAYER_IS_OP = "player_is_op";
    public static final String PLAYER_HAS_TAG = "player_has_tag";
    public static final String PLAYER_LACKS_TAG = "player_lacks_tag";
    public static final String PLAYER_TEAM_EQUALS = "player_team_equals";
    public static final String PLAYER_GAMEMODE_EQUALS = "player_gamemode_equals";
    public static final String PLAYER_ALIVE = "player_alive";
    public static final String PLAYER_DEAD = "player_dead";
    public static final String SOURCE_TYPE_EQUALS = "source_type_equals";
    public static final String SOURCE_ID_EQUALS = "source_id_equals";
    public static final String CHANNEL_EQUALS = "channel_equals";
    public static final String WORLD_EQUALS = "world_equals";
    public static final String DEVICE_ID_EQUALS = "device_id_equals";
    public static final String LISTENER_ID_EQUALS = "listener_id_equals";
    public static final String REGION_ID_EQUALS = "region_id_equals";
    public static final String ACTION_ID_EQUALS = "action_id_equals";
    public static final String GAME_TIME_COMPARE = "game_time_compare";
    public static final String EVENT_METADATA_EXISTS = "event_metadata_exists";
    public static final String EVENT_METADATA_EQUALS = "event_metadata_equals";
    public static final String STATE_VARIABLE_EXISTS = "state_variable_exists";
    public static final String STATE_VARIABLE_BOOL_EQUALS = "state_variable_bool_equals";
    public static final String STATE_VARIABLE_INT_COMPARE = "state_variable_int_compare";
    public static final String STATE_VARIABLE_STRING_EQUALS = "state_variable_string_equals";
    public static final String STATE_VARIABLE_STRING_CONTAINS = "state_variable_string_contains";
    public static final String ITEM_STACK_EXISTS = "item_stack_exists";
    public static final String ITEM_STACK_MATCHES = "item_stack_matches";
    public static final String INVENTORY_CONTAINS_ITEM = "inventory_contains_item";
    public static final String INVENTORY_ITEM_COUNT_COMPARE = "inventory_item_count_compare";
    public static final String CONTAINER_SLOT_EMPTY = "container_slot_empty";
    public static final String CONTAINER_SLOT_ITEM_MATCHES = "container_slot_item_matches";
    public static final String CONTAINER_ITEM_COUNT_COMPARE = "container_item_count_compare";

    private ConditionNodeType() {
    }
}
