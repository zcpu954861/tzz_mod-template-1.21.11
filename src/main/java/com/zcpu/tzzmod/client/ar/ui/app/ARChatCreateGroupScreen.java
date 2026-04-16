package com.zcpu.tzzmod.client.ar.ui.app;

import com.zcpu.tzzmod.client.ar.ui.AbstractARScreen;
import com.zcpu.tzzmod.client.phone.chat.ChatUiUtil;
import com.zcpu.tzzmod.client.phone.chat.PhoneChatClient;
import com.zcpu.tzzmod.client.photo.GalleryAvatarRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ARChatCreateGroupScreen extends AbstractARScreen {
        private final Map<String, Boolean> selectedMembers = new HashMap<>();
        private final List<PhoneChatClient.ContactData> contacts = new ArrayList<>();

        private TextFieldWidget nameField;
        private String selfUuid = "";
        private int scrollOffset;

        public ARChatCreateGroupScreen(Screen parent) {
                super(Text.translatable("phone.tzz_mod.chat.create_group"), parent);
        }

        @Override
        protected void init() {
                super.init();
                addBackButton();
                addARPrimaryButton(Text.translatable("phone.tzz_mod.chat.create_group_confirm"), contentX + contentWidth - s(66), contentY + contentHeight - s(20), s(66), s(16), button -> createGroup());

                int labelH = scaledFontHeight() + s(4);
                int fieldX = contentX + s(4);
                int fieldWidth = contentWidth - s(8);
                int fieldY = contentY + s(24);
                nameField = new TextFieldWidget(textRenderer, fieldX, fieldY + labelH, fieldWidth, textRenderer.fontHeight, Text.translatable("phone.tzz_mod.chat.group_name"));
                nameField.setMaxLength(64);
                styleTextField(nameField);
                addDrawableChild(nameField);
                setFocused(nameField);
                nameField.setFocused(true);

                rebuildContacts();
        }

        private void rebuildContacts() {
                contacts.clear();
                contacts.addAll(PhoneChatClient.getContacts());

                MinecraftClient minecraftClient = MinecraftClient.getInstance();
                var player = minecraftClient.player;
                if (player != null) {
                        selfUuid = player.getUuidAsString();
                        boolean hasSelf = contacts.stream().anyMatch(contact -> contact.uuid().equals(selfUuid));
                        if (!hasSelf) {
                                contacts.add(new PhoneChatClient.ContactData(selfUuid, player.getName().getString()));
                        }
                }

                contacts.sort(Comparator.comparing(PhoneChatClient.ContactData::name, String.CASE_INSENSITIVE_ORDER));
                if (!selfUuid.isBlank()) {
                        contacts.sort((left, right) -> Boolean.compare(!left.uuid().equals(selfUuid), !right.uuid().equals(selfUuid)));
                }

                selectedMembers.clear();
                for (PhoneChatClient.ContactData contact : contacts) {
                        selectedMembers.put(contact.uuid(), contact.uuid().equals(selfUuid));
                }
                clampScroll();
        }

        private void createGroup() {
                if (nameField == null) {
                        return;
                }
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                        return;
                }
                List<String> members = selectedMembers.entrySet().stream()
                                .filter(Map.Entry::getValue)
                                .map(Map.Entry::getKey)
                                .toList();
                PhoneChatClient.createGroup(name, members);
                close();
        }

        private int getListTop() {
                TextFieldWidget field = nameField;
                if (field == null) {
                        int labelH = scaledFontHeight() + s(4);
                        int fieldY = contentY + s(24) + labelH;
                        return fieldY + textRenderer.fontHeight + scaledFontHeight() + s(8);
                }
                return field.getY() + field.getHeight() + scaledFontHeight() + s(8);
        }

        private int getListBottom() {
                return contentY + contentHeight - s(24);
        }

        private int getRowHeight() {
                return s(18);
        }

        private int getRowSpacing() {
                return s(3);
        }

        private int getListHeight() {
                return contacts.size() * (getRowHeight() + getRowSpacing());
        }

        private int getMaxScroll() {
                return Math.max(0, getListHeight() - Math.max(1, getListBottom() - getListTop()));
        }

        private void clampScroll() {
                scrollOffset = Math.max(0, Math.min(scrollOffset, getMaxScroll()));
        }

        @Override
        public boolean mouseClicked(Click click, boolean doubleClick) {
                if (super.mouseClicked(click, doubleClick)) {
                        return true;
                }

                int mouseX = (int) click.x();
                int mouseY = (int) click.y();
                if (mouseX < contentX || mouseX > contentX + contentWidth || mouseY < getListTop() || mouseY > getListBottom()) {
                        return false;
                }

                int y = getListTop() - scrollOffset;
                for (PhoneChatClient.ContactData contact : contacts) {
                        if (mouseX >= contentX && mouseX <= contentX + contentWidth - s(4)
                                        && mouseY >= y && mouseY <= y + getRowHeight()) {
                                if (!contact.uuid().equals(selfUuid)) {
                                        selectedMembers.put(contact.uuid(), !selectedMembers.getOrDefault(contact.uuid(), false));
                                }
                                return true;
                        }
                        y += getRowHeight() + getRowSpacing();
                }
                return false;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
                if (mouseX >= contentX && mouseX <= contentX + contentWidth && mouseY >= getListTop() && mouseY <= getListBottom()) {
                        scrollOffset -= (int) Math.round(verticalAmount * s(18));
                        clampScroll();
                        return true;
                }
                return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        @Override
        protected void renderARContent(DrawContext context, int mouseX, int mouseY, float delta) {
                int fieldX = contentX + s(4);
                int fieldWidth = contentWidth - s(8);
                int labelH = scaledFontHeight() + s(4);
                int nameY = contentY + s(24);

                drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.chat.create_group"), contentX + contentWidth / 2, contentY + s(6), themeAccent());
                drawScaledText(context, Text.translatable("phone.tzz_mod.chat.group_name"), fieldX, nameY, themeText());
                drawARInputFrame(context, fieldX, nameY + labelH, fieldWidth, textRenderer.fontHeight);
                if (nameField != null) {
                        renderStyledTextFieldBackground(context, nameField);
                        nameField.render(context, mouseX, mouseY, delta);
                }
                drawScaledText(context, Text.translatable("phone.tzz_mod.chat.members_hint"), fieldX, getListTop() - scaledFontHeight() - s(4), themeTextDim());

                int top = getListTop();
                int bottom = getListBottom();
                int y = top - scrollOffset;
                context.enableScissor(contentX, top, contentX + contentWidth, bottom);
                for (PhoneChatClient.ContactData contact : contacts) {
                        if (y + getRowHeight() >= top && y <= bottom) {
                                boolean selected = selectedMembers.getOrDefault(contact.uuid(), false);
                                int fill = selected ? 0x3315A389 : 0x22091420;
                                int border = selected ? themeAccent() : themeBorder();
                                ChatUiUtil.drawAngularFrame(context, contentX, y, contentWidth - s(4), getRowHeight(), s(3), fill, border);
                                GalleryAvatarRenderer.drawAvatar(context, contact.uuid(), contentX + s(4), y + s(2), s(14), themeAccent());
                                drawScaledText(context, Text.literal(contact.name()), contentX + s(22), y + s(4), themeText());
                                if (contact.uuid().equals(selfUuid)) {
                                        drawScaledText(context, Text.translatable("phone.tzz_mod.chat.self"), contentX + contentWidth - s(26), y + s(4), themeTextDim());
                                } else if (selected) {
                                        drawScaledText(context, Text.translatable("phone.tzz_mod.chat.selected"), contentX + contentWidth - s(34), y + s(4), themeAccent());
                                }
                        }
                        y += getRowHeight() + getRowSpacing();
                }
                context.disableScissor();
                renderScrollbar(context, top, bottom, Math.max(bottom - top, getListHeight()), scrollOffset);
        }
}
