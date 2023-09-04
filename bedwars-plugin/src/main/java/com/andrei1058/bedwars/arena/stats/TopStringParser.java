package com.andrei1058.bedwars.arena.stats;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.stats.PlayerGameStats;
import com.andrei1058.bedwars.api.arena.stats.GameStatistic;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.language.Language;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TopStringParser {

    private int index = 0;
    private final List<PlayerGameStats> ordered;
    private final IArena arena;
    private BoundsPolicy boundsPolicy = BoundsPolicy.EMPTY;

    public TopStringParser(@NotNull IArena arena, String orderBy) {
        this.arena = arena;
        if (!arena.getStatsHolder().hasStatistic(orderBy)) {
            throw new RuntimeException("Invalid order by. Provided: " + orderBy);
        }
        ordered = arena.getStatsHolder().getOrderedBy(orderBy);
    }

    /**
     * @param string           string to be placeholder replaced.
     * @param emptyReplacement replace empty top position with this string.
     */
    public @Nullable String parseString(String string, @Nullable Language lang, String emptyReplacement) {
        if (index >= ordered.size()) {
            if (boundsPolicy == BoundsPolicy.SKIP) {
                if (string.isBlank()){
                    return string;
                }

                boolean hasPlaceholders = false;

                for (String placeholder : new String[]{
                        "{topPlayerName}", "{topPlayerDisplayName}", "{topTeamColor}", "{topTeamName}"
                }) {
                    if (string.contains(placeholder)) {
                        hasPlaceholders = true;
                        break;
                    }
                }

                if (!hasPlaceholders) {
                    for (String registered : arena.getStatsHolder().getRegistered()) {
                        if (string.contains("{topValue-" + registered + "}")) {
                            hasPlaceholders = true;
                            break;
                        }
                    }
                }
                return hasPlaceholders ? null : string;
            }

            string = string
                    .replace("{topPlayerName}", emptyReplacement)
                    .replace("{topPlayerDisplayName}", emptyReplacement)
                    .replace("{topTeamColor}", emptyReplacement)
                    .replace("{topTeamName}", emptyReplacement);

            for (String registered : arena.getStatsHolder().getRegistered()) {
                string = string.replace("{topValue-" + registered + "}", "");
            }

            return string;
        }

        PlayerGameStats stats = ordered.get(index);

        boolean increment = string.contains("{topPlayerName}") || string.contains("{topPlayerDisplayName}");

        Player online = Bukkit.getPlayer(stats.getPlayer());
        ITeam team = null == online ? arena.getExTeam(stats.getPlayer()) : arena.getTeam(online);

        string = string.replace("{topPlayerName}", stats.getUsername())
                .replace("{topPlayerDisplayName}", stats.getDisplayPlayer())
                .replace("{topTeamColor}", null == team ? "" : team.getColor().chat().toString())
                .replace("{topTeamName}", null == team ? "" : team.getDisplayName(lang));

        for (String registered : arena.getStatsHolder().getRegistered()) {
            GameStatistic<?> statistic = stats.getStatistic(registered);

            if (!increment && string.contains("{topValue-" + registered + "}")) {
                increment = true;
            }

            string = string.replace("{topValue-" + registered + "}", null == statistic ? "" : statistic.getDisplayValue(lang));
        }

        if (increment) {
            index++;
        }
        return string;
    }

    public void resetIndex() {
        index = 0;
    }

    public void setBoundsPolicy(BoundsPolicy boundsPolicy) {
        this.boundsPolicy = boundsPolicy;
    }

    /**
     * What to do when iterating stats line and there are no more players to show.
     * This is used when there are more placeholders used than the actual player count.
     */

    public enum BoundsPolicy {
        /**
         * Skip line. Do not send it to receivers.
         */
        SKIP,
        /**
         * Send empty line to receivers.
         */
        EMPTY
    }
}
