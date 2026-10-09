package gg.crystalized.lobby;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class QuestScopeTest {

    @Test
    void singleGameRequiresOneGameCategory() {
        assertTrue(Quest.isValidScope(false, Quest.Category.ko_kills));
        assertFalse(Quest.isValidScope(false, Quest.Category.ko_games_won));
    }

    @Test
    void multiGameAcceptsAnyCategory() {
        assertTrue(Quest.isValidScope(true, Quest.Category.ko_kills));
        assertTrue(Quest.isValidScope(true, Quest.Category.ko_games_won));
    }

    @Test
    void winCountCategoriesAreNeverSingleGame() {
        Set<Quest.Category> wins = EnumSet.of(
                Quest.Category.ls_was_winner,
                Quest.Category.ko_games_won,
                Quest.Category.cb_games_won);
        for (Quest.Category c : Quest.Category.values()) {
            if (c == Quest.Category.empty) {
                continue;
            }
            if (wins.contains(c)) {
                assertFalse(c.forOneGame, c + " must not be single-game (0/1 column, MAX() can never satisfy amount > 1)");
            } else {
                assertTrue(c.forOneGame, c + " must be single-game capable or single-game quests can never roll it");
            }
        }
    }
}
