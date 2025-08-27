package ru.manhunt.enums;

public enum PlayerRole {
    SPEEDRUNNER("§aСпидранер", "§a", "§a[Ранер]"),
    HUNTER("§cОхотник", "§c", "§c[Охотник]"),
    JUDGE("§6Судья", "§6", "§6[Судья]"),
    SPECTATOR("§7Наблюдатель", "§7", "§7[Зритель]");
    
    private final String displayName;
    private final String colorCode;
    private final String titleSuffix;
    
    PlayerRole(String displayName, String colorCode, String titleSuffix) {
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.titleSuffix = titleSuffix;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getColorCode() {
        return colorCode;
    }
    
    public String getTitleSuffix() {
        return titleSuffix;
    }
}