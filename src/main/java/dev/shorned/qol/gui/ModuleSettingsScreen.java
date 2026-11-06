package dev.shorned.qol.gui;

import dev.shorned.qol.config.Config;
import java.util.*;
import net.minecraft.client.gui.screens.Screen;

/** Paged settings for all existing tracker/notification options; changes save immediately. */
public final class ModuleSettingsScreen extends HubScreen {
  private final Object settings;

  public ModuleSettingsScreen(Screen parent, String category, Object settings) {
    super(parent, category + " settings");
    this.settings = settings;
  }

  @Override
  protected void content() {
    for (var field : settings.getClass().getFields()) {
      try {
        String name = field.getName();
        Object value = field.get(settings);
        if (field.getType() == boolean.class)
          action(
              name + ": " + value,
              () -> {
                try {
                  field.setBoolean(settings, !field.getBoolean(settings));
                  Config.save();
                  rebuildWidgets();
                } catch (IllegalAccessException e) {
                  throw new IllegalStateException(e);
                }
              });
        else if (value instanceof Number)
          action(
              name + ": " + value,
              () -> {
                var m = new LinkedHashMap<String, String>();
                m.put(name, "" + value);
                open(
                    new FormScreen(
                        this,
                        name,
                        m,
                        v -> {
                          try {
                            String s = v.get(name);
                            if (field.getType() == int.class)
                              field.setInt(settings, Integer.parseInt(s));
                            else if (field.getType() == double.class) {
                              double n = Double.parseDouble(s);
                              if (!Double.isFinite(n))
                                throw new IllegalArgumentException("Use a finite number");
                              field.setDouble(settings, n);
                            }
                            Config.get().sanitize();
                            Config.save();
                          } catch (IllegalAccessException e) {
                            throw new IllegalStateException(e);
                          }
                        }));
              });
        else if (value instanceof List<?>) {
          action(
              name + " (" + ((List<?>) value).size() + ")", () -> open(new Patterns(this, name)));
        }
      } catch (IllegalAccessException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  private static class Patterns extends HubScreen {
    private final String type;

    Patterns(Screen p, String type) {
      super(p, type);
      this.type = type;
    }

    private List<String> patterns() {
      return type.equals("spawnPatterns")
          ? Config.get().fishing.spawnPatterns
          : Config.get().fishing.killPatterns;
    }

    @Override
    protected void content() {
      action(
          "Add recognition regex",
          () -> {
            var m = new LinkedHashMap<String, String>();
            m.put("Regex", "");
            open(
                new FormScreen(
                    this,
                    "Recognition regex",
                    m,
                    v -> {
                      String regex = v.get("Regex");
                      if (regex.isBlank())
                        throw new IllegalArgumentException("Pattern cannot be blank");
                      java.util.regex.Pattern.compile(regex);
                      patterns().add(regex);
                      Config.save();
                    }));
          });
      for (String pattern : List.copyOf(patterns()))
        action(
            "Remove: " + pattern,
            () ->
                open(
                    new ConfirmScreen(
                        this,
                        "Remove pattern?",
                        () -> {
                          patterns().remove(pattern);
                          Config.save();
                        })));
    }
  }
}
