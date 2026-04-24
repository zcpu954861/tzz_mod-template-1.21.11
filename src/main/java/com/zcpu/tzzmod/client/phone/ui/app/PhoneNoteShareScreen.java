package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.client.note.NoteClient;
import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PhoneNoteShareScreen extends AbstractPhoneScreen {
    private static final int FACE_U = 8;
    private static final int FACE_V = 8;
    private static final int HAT_U = 40;
    private static final int HAT_V = 8;
    private static final int SKIN_TEXTURE_SIZE = 64;

    private final String noteId;
    private final Map<String, Boolean> selection = new HashMap<>();
    private final Map<String, Identifier> skinTextureCache = new HashMap<>();
    private Runnable stateListener;
    private int scrollOffset;

    public PhoneNoteShareScreen(Screen parent, String noteId) {
        super(Text.translatable("phone.tzz_mod.notes.share"), parent);
        this.noteId = noteId;
    }

    public boolean referencesNote(String removedNoteId) {
        return noteId.equals(removedNoteId);
    }

    public Screen getNotesHomeScreen() {
        if (parent instanceof PhoneNoteDetailScreen detailScreen) {
            return detailScreen.getNotesHomeScreen();
        }
        return parent == null ? new PhoneNotesAppScreen(null) : parent;
    }

    @Override
    protected void init() {
        super.init();
        int bottomY = contentY + contentHeight - s(24);
        addPhoneButton(Text.translatable("phone.tzz_mod.back"), contentX, bottomY, s(70), s(20), button -> close());
        addPhonePrimaryButton(Text.translatable("phone.tzz_mod.notes.save"), contentX + contentWidth - s(70), bottomY, s(70), s(20), button -> saveSelection());
        seedSelection();
        stateListener = () -> {
            seedSelection();
            clampScrollOffset();
        };
        NoteClient.addListener(stateListener);
        NoteClient.requestShareTargets(noteId);
    }

    private void seedSelection() {
        for (NoteClient.ShareTargetData target : NoteClient.getShareTargets(noteId)) {
            selection.putIfAbsent(target.uuid(), target.selected());
        }
    }

    private void saveSelection() {
        List<String> shared = new ArrayList<>();
        for (NoteClient.ShareTargetData target : NoteClient.getShareTargets(noteId)) {
            if (selection.getOrDefault(target.uuid(), target.selected())) {
                shared.add(target.uuid());
            }
        }
        NoteClient.setSharedUsers(noteId, shared);
        if (client != null) {
            client.setScreen(getNotesHomeScreen());
        }
    }

    @Override
    public void removed() {
        super.removed();
        if (stateListener != null) {
            NoteClient.removeListener(stateListener);
            stateListener = null;
        }
    }

    private int listTop() {
        return contentY + s(38);
    }

    private int listBottom() {
        return contentY + contentHeight - s(32);
    }

    private int rowHeight() {
        return s(22);
    }

    private int rowGap() {
        return s(3);
    }

    private int totalHeight() {
        List<NoteClient.ShareTargetData> targets = NoteClient.getShareTargets(noteId);
        return targets.isEmpty() ? 0 : targets.size() * rowHeight() + Math.max(0, targets.size() - 1) * rowGap();
    }

    private void clampScrollOffset() {
        int max = Math.max(0, totalHeight() - Math.max(1, listBottom() - listTop()));
        scrollOffset = Math.max(0, Math.min(scrollOffset, max));
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.notes.share"), contentX + contentWidth / 2, contentY + s(8));
        drawScaledText(context, Text.translatable("phone.tzz_mod.notes.share_hint"), contentX + s(2), contentY + s(26), themeTextDim());

        List<NoteClient.ShareTargetData> targets = NoteClient.getShareTargets(noteId);
        clampScrollOffset();
        if (targets.isEmpty()) {
            drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.notes.no_targets"),
                    contentX + contentWidth / 2, contentY + s(88), themeTextDim());
            return;
        }

        int y = listTop() - scrollOffset;
        for (NoteClient.ShareTargetData target : targets) {
            if (y + rowHeight() >= listTop() && y <= listBottom()) {
                renderTargetRow(context, target, y);
            }
            y += rowHeight() + rowGap();
        }
        renderPhoneScrollbar(context, listTop(), listBottom(), totalHeight(), scrollOffset);
    }

    private void renderTargetRow(DrawContext context, NoteClient.ShareTargetData target, int y) {
        int avatarSize = Math.max(4, rowHeight() - s(4));
        int avatarX = contentX + s(4);
        int avatarY = y + (rowHeight() - avatarSize) / 2;
        drawPlayerHead(context, target.uuid(), target.online(), avatarX, avatarY, avatarSize);

        int boxSize = s(12);
        int boxX = contentX + contentWidth - s(8) - boxSize;
        int boxY = y + (rowHeight() - boxSize) / 2;
        int nameX = avatarX + avatarSize + s(5);
        int nameMaxW = Math.max(1, boxX - nameX - s(5));
        String suffix = target.online() ? "" : " *";
        String shownName = textRenderer.trimToWidth(target.name() + suffix, Math.max(1, Math.round(nameMaxW / getTextScale())));
        drawScaledText(context, Text.literal(shownName), nameX, y + Math.max(0, (rowHeight() - scaledFontHeight()) / 2), target.online() ? themeText() : themeTextDim());

        boolean selected = selection.getOrDefault(target.uuid(), target.selected());
        if (selected) {
            context.fill(boxX, boxY, boxX + boxSize, boxY + boxSize, 0xFF3CB371);
            context.drawText(textRenderer, Text.literal("✓"), boxX + (boxSize - textRenderer.getWidth("✓")) / 2,
                    boxY + Math.max(0, (boxSize - textRenderer.fontHeight) / 2), 0xFFFFFFFF, false);
        } else {
            int t = Math.max(1, s(1));
            context.fill(boxX, boxY, boxX + boxSize, boxY + t, themeBorder());
            context.fill(boxX, boxY + boxSize - t, boxX + boxSize, boxY + boxSize, themeBorder());
            context.fill(boxX, boxY, boxX + t, boxY + boxSize, themeBorder());
            context.fill(boxX + boxSize - t, boxY, boxX + boxSize, boxY + boxSize, themeBorder());
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (super.mouseClicked(click, doubleClick)) {
            return true;
        }
        int mx = (int) click.x();
        int my = (int) click.y();
        if (mx < contentX || mx > contentX + contentWidth || my < listTop() || my > listBottom()) {
            return false;
        }
        List<NoteClient.ShareTargetData> targets = NoteClient.getShareTargets(noteId);
        int y = listTop() - scrollOffset;
        for (NoteClient.ShareTargetData target : targets) {
            if (my >= y && my <= y + rowHeight()) {
                selection.put(target.uuid(), !selection.getOrDefault(target.uuid(), target.selected()));
                return true;
            }
            y += rowHeight() + rowGap();
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isHelpModeActive()) {
            return true;
        }
        int mx = (int) mouseX;
        int my = (int) mouseY;
        if (mx >= contentX && mx <= contentX + contentWidth && my >= listTop() && my <= listBottom()) {
            scrollOffset -= (int) Math.round(verticalAmount * (rowHeight() + rowGap()));
            clampScrollOffset();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void drawPlayerHead(DrawContext context, String uuid, boolean online, int x, int y, int size) {
        Identifier skin = online ? getCachedSkin(uuid) : null;
        context.fill(x, y, x + size, y + size, online ? 0x44000000 : (isLightMode() ? 0x4480A0B8 : 0x44334455));
        if (skin == null) {
            drawScaledCenteredText(context, Text.literal("?"), x + size / 2, y + Math.max(0, (size - scaledFontHeight()) / 2), themeTextDim());
            return;
        }
        drawSkinRegion(context, skin, x, y, size, FACE_U, FACE_V);
        drawSkinRegion(context, skin, x, y, size, HAT_U, HAT_V);
    }

    private Identifier getCachedSkin(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return null;
        }
        Identifier cached = skinTextureCache.get(uuid);
        if (cached != null) {
            return cached;
        }
        Identifier resolved = resolveSkin(uuid);
        if (resolved != null) {
            skinTextureCache.put(uuid, resolved);
        }
        return resolved;
    }

    private Identifier resolveSkin(String uuid) {
        if (client == null || uuid == null || uuid.isBlank()) {
            return null;
        }
        try {
            var handler = client.getNetworkHandler();
            if (handler == null) {
                return null;
            }
            if (client.player != null && uuid.equals(client.player.getUuidAsString())) {
                SkinTextures skin = client.player.getSkin();
                return skin == null || skin.body() == null ? null : skin.body().texturePath();
            }
            var entry = handler.getPlayerListEntry(UUID.fromString(uuid));
            if (entry == null) {
                return null;
            }
            SkinTextures skin = entry.getSkinTextures();
            return skin == null || skin.body() == null ? null : skin.body().texturePath();
        } catch (Exception ignored) {
            return null;
        }
    }

    private void drawSkinRegion(DrawContext context, Identifier texture, int x, int y, int size, int u, int v) {
        context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, (float) u, (float) v, size, size, 8, 8, SKIN_TEXTURE_SIZE, SKIN_TEXTURE_SIZE, -1);
    }
}
