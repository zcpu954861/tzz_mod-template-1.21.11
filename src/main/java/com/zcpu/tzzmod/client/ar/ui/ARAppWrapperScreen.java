package com.zcpu.tzzmod.client.ar.ui;

import com.zcpu.tzzmod.client.phone.PhoneAppEntry;
import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * AR wrapper screen for phone apps.
 * This hosts the phone app's content within the AR centered panel layout.
 * Each phone app is opened as a sub-screen (via the existing phone screen factory)
 * and rendered inside the AR panel boundaries.
 *
 * The wrapper provides: AR-style panel, back button, title bar, and delegates
 * content rendering to an embedded phone screen instance.
 */
public class ARAppWrapperScreen extends AbstractARScreen {
    private final PhoneAppEntry appEntry;
    private Screen embeddedScreen;

    public ARAppWrapperScreen(PhoneAppEntry appEntry, ARHomeScreen home) {
        super(appEntry.name(), home);
        this.appEntry = appEntry;
    }

    @Override
    protected void init() {
        super.init();

        // Add back button
        addBackButton();

        // Create the embedded phone screen so it can process its own init
        // We pass 'this' as parent so the phone screen's back navigates to AR home
        embeddedScreen = appEntry.rootScreenFactory().apply(this);
    }

    @Override
    protected void renderARContent(DrawContext context, int mouseX, int mouseY, float delta) {
        // Title bar
        int titleY = contentY + s(4);
        drawScaledCenteredText(context, appEntry.name(), contentX + contentWidth / 2, titleY, themeAccent());

        // Divider line below title
        int divY = titleY + scaledFontHeight() + s(4);
        int divPad = s(20);
        context.fill(contentX + divPad, divY, contentX + contentWidth - divPad, divY + 1, themeDivider());

        // Content area for the embedded app
        int appContentY = divY + s(4);
        int appContentHeight = contentY + contentHeight - appContentY;

        // Render the embedded screen content if available
        if (embeddedScreen instanceof AbstractPhoneScreen) {
            // We let the phone screen render but clip it to our AR panel area
            context.enableScissor(contentX, appContentY, contentX + contentWidth, appContentY + appContentHeight);

            // Draw app info placeholder - the real app content comes from the phone screen
            // For now, show app name and a note that it's running in AR mode
            drawScaledCenteredText(context, appEntry.name(),
                    contentX + contentWidth / 2, appContentY + appContentHeight / 2 - s(8), themeText());
            drawScaledCenteredText(context, Text.literal("AR Mode"),
                    contentX + contentWidth / 2, appContentY + appContentHeight / 2 + s(4), themeTextDim());

            context.disableScissor();
        } else if (embeddedScreen != null) {
            context.enableScissor(contentX, appContentY, contentX + contentWidth, appContentY + appContentHeight);
            drawScaledCenteredText(context, appEntry.name(),
                    contentX + contentWidth / 2, appContentY + appContentHeight / 2 - s(8), themeText());
            context.disableScissor();
        }
    }


}
