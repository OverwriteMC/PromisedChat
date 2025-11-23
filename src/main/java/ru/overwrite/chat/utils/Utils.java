package ru.overwrite.chat.utils;

import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.experimental.UtilityClass;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class Utils {

    private final Object2ObjectMap<String, ChatColor> colorCodesPermissions = new Object2ObjectOpenHashMap<>();
    private final Char2ObjectMap<String> colorCodesMap = new Char2ObjectOpenHashMap<>();

    private final Object2ObjectMap<String, ChatColor> colorStylesPermissions = new Object2ObjectOpenHashMap<>();
    private final Char2ObjectMap<String> colorStylesMap = new Char2ObjectOpenHashMap<>();

    static {
        colorCodesPermissions.put("pchat.color.black", ChatColor.BLACK);
        colorCodesPermissions.put("pchat.color.dark_blue", ChatColor.DARK_BLUE);
        colorCodesPermissions.put("pchat.color.dark_green", ChatColor.DARK_GREEN);
        colorCodesPermissions.put("pchat.color.dark_aqua", ChatColor.DARK_AQUA);
        colorCodesPermissions.put("pchat.color.dark_red", ChatColor.DARK_RED);
        colorCodesPermissions.put("pchat.color.dark_purple", ChatColor.DARK_PURPLE);
        colorCodesPermissions.put("pchat.color.gold", ChatColor.GOLD);
        colorCodesPermissions.put("pchat.color.gray", ChatColor.GRAY);
        colorCodesPermissions.put("pchat.color.dark_gray", ChatColor.DARK_GRAY);
        colorCodesPermissions.put("pchat.color.blue", ChatColor.BLUE);
        colorCodesPermissions.put("pchat.color.green", ChatColor.GREEN);
        colorCodesPermissions.put("pchat.color.aqua", ChatColor.AQUA);
        colorCodesPermissions.put("pchat.color.red", ChatColor.RED);
        colorCodesPermissions.put("pchat.color.light_purple", ChatColor.LIGHT_PURPLE);
        colorCodesPermissions.put("pchat.color.yellow", ChatColor.YELLOW);
        colorCodesPermissions.put("pchat.color.white", ChatColor.WHITE);

        colorStylesPermissions.put("pchat.style.obfuscated", ChatColor.MAGIC);
        colorStylesPermissions.put("pchat.style.bold", ChatColor.BOLD);
        colorStylesPermissions.put("pchat.style.strikethrough", ChatColor.STRIKETHROUGH);
        colorStylesPermissions.put("pchat.style.underline", ChatColor.UNDERLINE);
        colorStylesPermissions.put("pchat.style.italic", ChatColor.ITALIC);
        colorStylesPermissions.put("pchat.style.reset", ChatColor.RESET);

        colorCodesMap.put('0', "black");
        colorCodesMap.put('1', "dark_blue");
        colorCodesMap.put('2', "dark_green");
        colorCodesMap.put('3', "dark_aqua");
        colorCodesMap.put('4', "dark_red");
        colorCodesMap.put('5', "dark_purple");
        colorCodesMap.put('6', "gold");
        colorCodesMap.put('7', "gray");
        colorCodesMap.put('8', "dark_gray");
        colorCodesMap.put('9', "blue");
        colorCodesMap.put('a', "green");
        colorCodesMap.put('b', "aqua");
        colorCodesMap.put('c', "red");
        colorCodesMap.put('d', "light_purple");
        colorCodesMap.put('e', "yellow");
        colorCodesMap.put('f', "white");

        colorStylesMap.put('l', "bold");
        colorStylesMap.put('k', "obfuscated");
        colorStylesMap.put('m', "strikethrough");
        colorStylesMap.put('n', "underline");
        colorStylesMap.put('o', "italic");
        colorStylesMap.put('r', "reset");
    }

    private final Pattern HEX_PATTERN = Pattern.compile("&#([a-fA-F\\d]{6})");

    public String colorize(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder builder = new StringBuilder(message.length() + 32);
        while (matcher.find()) {
            char[] group = matcher.group(1).toCharArray();
            matcher.appendReplacement(builder,
                    ChatColor.COLOR_CHAR + "x" +
                            ChatColor.COLOR_CHAR + group[0] +
                            ChatColor.COLOR_CHAR + group[1] +
                            ChatColor.COLOR_CHAR + group[2] +
                            ChatColor.COLOR_CHAR + group[3] +
                            ChatColor.COLOR_CHAR + group[4] +
                            ChatColor.COLOR_CHAR + group[5]);
        }
        message = matcher.appendTail(builder).toString();
        return translateAlternateColorCodes('&', message);
    }

    public String translateAlternateColorCodes(char altColorChar, String textToTranslate) {
        final char[] b = textToTranslate.toCharArray();

        for (int i = 0, length = b.length - 1; i < length; i++) {
            if (b[i] == altColorChar && isValidColorCharacter(b[i + 1])) {
                b[i++] = ChatColor.COLOR_CHAR;
                b[i] |= 0x20;
            }
        }

        return new String(b);
    }

    private boolean isValidColorCharacter(char c) {
        return switch (c) {
            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'A', 'B', 'C', 'D',
                 'E', 'F', 'r', 'R', 'k', 'K', 'l', 'L', 'm', 'M', 'n', 'N', 'o', 'O', 'x', 'X' -> true;
            default -> false;
        };
    }

    public boolean USE_PAPI;

    public String replacePlaceholders(Player player, String message) {
        if (!USE_PAPI) {
            return message;
        }
        if (PlaceholderAPI.containsPlaceholders(message)) {
            message = PlaceholderAPI.setPlaceholders(player, message);
        }
        return message;
    }

    public String getTime(int time, String hoursMark, String minutesMark, String secondsMark) {
        final int hours = getHours(time);
        final int minutes = getMinutes(time);
        final int seconds = getSeconds(time);

        final StringBuilder result = new StringBuilder();

        if (hours > 0) {
            result.append(hours).append(hoursMark);
        }

        if (minutes > 0 || hours > 0) {
            result.append(minutes).append(minutesMark);
        }

        result.append(seconds).append(secondsMark);

        return result.toString();
    }

    public int getHours(int time) {
        return time / 3600;
    }

    public int getMinutes(int time) {
        return (time % 3600) / 60;
    }

    public int getSeconds(int time) {
        return time % 60;
    }

    public String replaceEach(String text, String[] searchList, String[] replacementList) {
        if (text == null || text.isEmpty() || searchList.length == 0 || replacementList.length == 0) {
            return text;
        }

        if (searchList.length != replacementList.length) {
            throw new IllegalArgumentException("Search and replacement arrays must have the same length.");
        }

        final StringBuilder result = new StringBuilder(text);

        for (int i = 0; i < searchList.length; i++) {
            String search = searchList[i];
            String replacement = replacementList[i];

            int start = 0;

            while ((start = result.indexOf(search, start)) != -1) {
                result.replace(start, start + search.length(), replacement);
                start += replacement.length();
            }
        }

        return result.toString();
    }

    public String formatByPerm(Player player, String message) {
        if (player.hasPermission("pchat.style.hex")) {
            return colorize(message);
        }
        final char[] chars = message.toCharArray();
        if (chars[chars.length - 1] == '&') {
            return message;
        }
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '&' && isValidColorCharacter(chars[i + 1])) {

                char code = chars[i + 1] |= 0x20;
                String colorPerm = "pchat.color." + colorCodesMap.get(code);
                String stylePerm = "pchat.style." + colorStylesMap.get(code);

                if (player.hasPermission(colorPerm)) {
                    ChatColor color = colorCodesPermissions.get(colorPerm);
                    if (color != null) {
                        chars[i] = ChatColor.COLOR_CHAR;
                    }
                }
                if (player.hasPermission(stylePerm)) {
                    ChatColor style = colorStylesPermissions.get(stylePerm);
                    if (style != null) {
                        chars[i] = ChatColor.COLOR_CHAR;
                    }
                }
            }
        }
        return new String(chars);
    }
}
