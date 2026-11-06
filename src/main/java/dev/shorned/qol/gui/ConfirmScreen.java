package dev.shorned.qol.gui;

import net.minecraft.client.gui.screens.Screen;

public final class ConfirmScreen extends HubScreen {
  private final Runnable confirm;

  public ConfirmScreen(Screen parent, String question, Runnable confirm) {
    super(parent, question);
    this.confirm = confirm;
  }

  @Override
  protected void content() {
    action(
        "Confirm",
        () -> {
          confirm.run();
          onClose();
        });
    action("Cancel", this::onClose);
  }
}
