package dev.shorned.qol.config;
public class Config { public static class Mining { public boolean enabled = true, chatFallback = true; } public Mining mining = new Mining();
  static Config i = new Config(); public static Config get() { return i; } }
