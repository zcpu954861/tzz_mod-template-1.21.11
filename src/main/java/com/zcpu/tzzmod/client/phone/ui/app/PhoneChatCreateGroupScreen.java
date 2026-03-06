package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.client.phone.chat.PhoneChatClient;
import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import com.zcpu.tzzmod.client.phone.ui.PhoneButtonWidget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PhoneChatCreateGroupScreen extends AbstractPhoneScreen {
    private TextFieldWidget nameField;
    // selection state for member buttons: uuid -> selected
    private final Map<String, Boolean> selectedMembers = new HashMap<>();
    // keep the contacts list for rendering
    private List<PhoneChatClient.ContactData> contactsList = new ArrayList<>();
    // map uuid -> button for updating label when toggled (we keep for possible future use)
    private final Map<String, ButtonWidget> memberButtonByUuid = new HashMap<>();

    public PhoneChatCreateGroupScreen(Screen parent) {
        super(Text.translatable("phone.tzz_mod.chat.create_group"), parent);
    }

    @Override
    protected void init() {
        super.init();

        addPhoneButton(Text.translatable("phone.tzz_mod.back"), contentX, contentY + contentHeight - s(24), s(72), s(20), button -> close());

        addPhonePrimaryButton(Text.translatable("phone.tzz_mod.chat.create"), contentX + contentWidth - s(82), contentY + contentHeight - s(24), s(82), s(20), button -> createGroup());

        // Compute positions so labels are always above fields with enough gap
        int fieldHeight = s(20);
        int verticalGapBetweenFields = s(12);
        // extra offset to move members label+field further down to avoid overlap with other UI
        int extraMembersOffset = s(12);

        int nameFieldY = contentY + s(36);
        nameField = new TextFieldWidget(textRenderer, contentX, nameFieldY, contentWidth, fieldHeight, Text.empty());
        nameField.setPlaceholder(Text.translatable("phone.tzz_mod.chat.group_name"));
        nameField.setMaxLength(32);
        addDrawableChild(nameField);

        // Build a vertical list of buttons for each contact instead of a free-form text field.
        int listStartY = nameFieldY + fieldHeight + verticalGapBetweenFields + extraMembersOffset;
        // get a mutable copy of contacts
        contactsList = new ArrayList<>(PhoneChatClient.getContacts());

        // Ensure local player is present (singleplayer or when server doesn't include self in contacts)
        String selfUuid = "";
        try {
            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc != null && mc.player != null) {
                selfUuid = mc.player.getUuidAsString();
                boolean found = false;
                for (PhoneChatClient.ContactData c : contactsList) {
                    if (c.uuid().equals(selfUuid)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    // prepend local player so they appear at top
                    contactsList.add(0, new PhoneChatClient.ContactData(selfUuid, mc.player.getName().getString()));
                }
            }
        } catch (Exception ignored) {
        }
        // If still empty (very unlikely), add a placeholder for local player name
        if (contactsList.isEmpty()) {
            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            String name = mc != null && mc.player != null ? mc.player.getName().getString() : "Player";
            String uuid = mc != null && mc.player != null ? mc.player.getUuidAsString() : "";
            contactsList.add(new PhoneChatClient.ContactData(uuid, name));
            if (selfUuid.isEmpty()) selfUuid = uuid;
        }

        // initialize selection state: ensure self is always selected
        for (PhoneChatClient.ContactData c : contactsList) {
            selectedMembers.put(c.uuid(), c.uuid().equals(selfUuid));
        }

        // reuse fieldHeight for button height
        int rowGap = s(3);
        for (int i = 0; i < contactsList.size(); i++) {
            PhoneChatClient.ContactData contact = contactsList.get(i);
            int y = listStartY + i * (fieldHeight + rowGap);
            final String uuid = contact.uuid();
            final boolean isSelf = uuid.equals(selfUuid);
            // create a transparent phone-styled button that captures clicks for the row
            ButtonWidget btn = addPhoneButton(Text.empty(), contentX, y, contentWidth, fieldHeight,
                    PhoneButtonWidget.Variant.GHOST,
                    () -> false,
                    button -> {
                        if (isSelf) return;
                        boolean newState = !selectedMembers.getOrDefault(uuid, false);
                        selectedMembers.put(uuid, newState);
                    });
            // make local player's row visually non-interactive
            if (isSelf) {
                try {
                    btn.active = false; // mark as disabled so it appears non-clickable
                } catch (Throwable ignored) {}
            }
            memberButtonByUuid.put(uuid, btn);
        }
    }

    private void createGroup() {
        if (!PhoneChatClient.isOp()) {
            return;
        }

        String name = nameField.getText().trim();
        List<String> members = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : selectedMembers.entrySet()) {
            if (entry.getValue()) {
                members.add(entry.getKey());
            }
        }

        PhoneChatClient.createGroup(name, members);
        close();
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.chat.create_group"), contentX + contentWidth / 2, contentY + s(8));
        // Draw labels relative to the fields so text never overlaps input
        int fieldHeight = s(20);
        int labelGap = s(8);
        int verticalGapBetweenFields = s(12);
        int extraMembersOffset = s(12);

        int nameFieldY = contentY + s(36);
        int nameLabelY = nameFieldY - labelGap - textRenderer.fontHeight;
        int listStartY = nameFieldY + fieldHeight + verticalGapBetweenFields + extraMembersOffset;
        int membersLabelY = listStartY - labelGap - textRenderer.fontHeight;

        context.drawTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.chat.group_name"), contentX, nameLabelY, 0xFFECECEC);
        context.drawTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.chat.members_hint"), contentX, membersLabelY, 0xFFBFC7D5);

        // Draw each contact row name and a checkbox at the right. Buttons are transparent and capture clicks.
        int rowGap = s(3);
        int boxSize = s(12);
        int boxPadding = s(6);
        int boxBorderColor = 0xFF7F8A97;
        int boxFillColor = 0xFF3CB371; // green fill for selected
        int checkColor = 0xFFFFFFFF;

        for (int i = 0; i < contactsList.size(); i++) {
            PhoneChatClient.ContactData contact = contactsList.get(i);
            int y = listStartY + i * (fieldHeight + rowGap);
            // draw name left-aligned and trim if too long so it doesn't overlap the checkbox
            int maxNameWidth = contentWidth - (boxPadding + boxSize + s(12));
            String displayName = textRenderer.trimToWidth(contact.name(), Math.max(1, maxNameWidth));
            context.drawTextWithShadow(textRenderer, Text.literal(displayName), contentX + s(4), y + Math.max(0, (fieldHeight - textRenderer.fontHeight) / 2), 0xFFECECEC);

            // compute checkbox position
            int boxX = contentX + contentWidth - boxPadding - boxSize;
            int boxY = y + Math.max(0, (fieldHeight - boxSize) / 2);

            boolean selected = selectedMembers.getOrDefault(contact.uuid(), false);
            if (selected) {
                // filled box
                context.fill(boxX, boxY, boxX + boxSize, boxY + boxSize, boxFillColor);
                // draw inner white check (use text)
                String check = "✓";
                int checkW = textRenderer.getWidth(check);
                int checkX = boxX + (boxSize - checkW) / 2;
                int checkY = boxY + Math.max(0, (boxSize - textRenderer.fontHeight) / 2);
                context.drawTextWithShadow(textRenderer, Text.literal(check), checkX, checkY, checkColor);
            } else {
                // draw box outline: top
                int t = Math.max(1, s(1));
                context.fill(boxX, boxY, boxX + boxSize, boxY + t, boxBorderColor);
                // bottom
                context.fill(boxX, boxY + boxSize - t, boxX + boxSize, boxY + boxSize, boxBorderColor);
                // left
                context.fill(boxX, boxY, boxX + t, boxY + boxSize, boxBorderColor);
                // right
                context.fill(boxX + boxSize - t, boxY, boxX + boxSize, boxY + boxSize, boxBorderColor);
            }
        }
    }
}
