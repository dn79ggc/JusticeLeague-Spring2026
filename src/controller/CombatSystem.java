package controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import model.Consumable;
import model.EffectType;
import model.FlavorText;
import model.Game;
import model.Monster;
import model.MonsterAbility;
import model.Player;
import model.Room;
import model.StatusEffect;
import model.Weapon;
import view.GameView;

/**
 * Manages turn-based combat between the player and a monster.
 * Handles attack, defend, flee, item use, and status-effect ticks.
 * 
 * @author Joseph Nguyen
 */
public class CombatSystem {
    private static final int COMBAT_DURATION = -1;
    private static final double BASE_FLEE_CHANCE = 0.50;
    private static final int DAMAGE_VARIANCE = 3;

    private final Game game;
    private final GameView view;
    private final Random rng = new Random();

    private Room combatRoom;
    private Monster rootMonster;
    private Monster activeMonster;
    private boolean inCombat;
    private boolean endingReached;

    public enum CombatResult {
        MONSTER_DEAD,
        PLAYER_WIN_FLEE,
        PLAYER_WIN_BARRICADE,
        PLAYER_FLED,
        PLAYER_DEAD
    }

    private enum ActionResult {
        ACTION_SUCCESS,
        ACTION_FREE,
        ACTION_MISSED,
        ACTION_SKIPPED,
        ACTION_INVALID,
        ACTION_FAILED,
        ACTION_CANCELLED,
        ACTION_ITEM_USED
    }

    public CombatSystem(Game game, GameView view) {
        this.game = game;
        this.view = view;
    }

    public boolean isInCombat() {
        return inCombat;
    }

    public Monster getActiveMonster() {
        return activeMonster;
    }

    public boolean isEndingReached() {
        return endingReached;
    }

    public void startCombat(Player player, Monster monster, Room room) {
        combatRoom = room;
        rootMonster = monster;
        activeMonster = monster;
        inCombat = true;

        if (monster != null && monster.isType("Shadow")) {
            monster.setIsInBuilding(room != null && room.isIndoor());
        }

        view.showCombatStart(activeMonster.getName(), activeMonster.getLevel());
        view.updateMonsterStats(activeMonster.getName(), activeMonster.getCurrentHp(), activeMonster.getMaxHp());
        view.displayMessage(FlavorText.get("COMBAT_FIRST", "You have encountered " + activeMonster.getName() + "!"),
                GameView.MessageType.NARRATION);
        view.displayMessage(getMonsterStartFlavor(activeMonster), GameView.MessageType.NARRATION);
        view.displayMessage("--- Combat begins ---", GameView.MessageType.SEPARATOR);

        view.displayMessage("The " + activeMonster.getName() + " attacks before you can react!",
                GameView.MessageType.SYSTEM);
        executeMonsterTurn(player, activeMonster);

        if (!inCombat || activeMonster == null || combatRoom == null) {
            return;
        }

        if (checkCombatEnd(player, activeMonster, combatRoom)) {
            return;
        }

        view.setCombatActionsEnabled(true);
        view.displayMessage("--- Your turn ---", GameView.MessageType.SEPARATOR);
    }

    public void executeCombatCycle(String action, Player player) {
        if (!inCombat || activeMonster == null || combatRoom == null) {
            return;
        }

        player.setItemUsedThisTurn(false);

        ActionResult playerResult = executePlayerAction(action, player, activeMonster, combatRoom);
        if (playerResult == ActionResult.ACTION_INVALID || playerResult == ActionResult.ACTION_CANCELLED) {
            view.setCombatActionsEnabled(true);
            return;
        }

        if (checkCombatEnd(player, activeMonster, combatRoom)) {
            return;
        }

        if (playerResult != ActionResult.ACTION_FREE && playerResult != ActionResult.ACTION_ITEM_USED) {
            executeMonsterTurn(player, activeMonster);
        }

        if (!inCombat || activeMonster == null || combatRoom == null) {
            return;
        }

        if (checkCombatEnd(player, activeMonster, combatRoom)) {
            return;
        }

        tickStatusEffects(player);

        if (!inCombat || activeMonster == null || combatRoom == null) {
            return;
        }

        if (checkCombatEnd(player, activeMonster, combatRoom)) {
            return;
        }

        view.setCombatActionsEnabled(true);
        view.displayMessage("--- Your turn ---", GameView.MessageType.SEPARATOR);
    }

    private ActionResult executePlayerAction(String action, Player player, Monster monster, Room room) {
        String normalized = action == null ? "" : action.trim().toLowerCase();
        return switch (normalized) {
            case "attack" -> executePlayerAttack(player, monster);
            case "defend" -> {
                player.setDefending(true);
                view.displayMessage("You brace yourself for the next attack.", GameView.MessageType.SYSTEM);
                yield ActionResult.ACTION_SUCCESS;
            }
            case "flee" -> executePlayerFlee(player, monster, room);
            case "inspectenemy", "inspect enemy" -> {
                view.showInspectEnemy(player.inspectEnemy(monster));
                view.setCombatActionsEnabled(true);
                yield ActionResult.ACTION_FREE;
            }
            default -> {
                if (normalized.startsWith("use ")) {
                    yield executeUseItem(player, normalized.substring(4).trim());
                }
                if (normalized.equals("useitem")) {
                    yield executeUseItem(player, null);
                }
                if (normalized.startsWith("equip ")) {
                    yield executeEquipWeapon(player, normalized.substring(6).trim());
                }
                if (normalized.equals("equip")) {
                    yield executeEquipWeapon(player, null);
                }
                view.displayMessage("Invalid combat action.", GameView.MessageType.ERROR);
                yield ActionResult.ACTION_INVALID;
            }
        };
    }

    private ActionResult executePlayerAttack(Player player, Monster monster) {
        Weapon weapon = player.getEquippedWeapon();

        if (player.hasStatusEffect(EffectType.STUNNED)) {
            view.displayMessage("You are stunned and cannot act this turn!", GameView.MessageType.SYSTEM);
            player.removeStatusEffect(EffectType.STUNNED);
            return ActionResult.ACTION_SKIPPED;
        }

        if (player.hasStatusEffect(EffectType.TERRORIZED)) {
            StatusEffect effect = player.getStatusEffect(EffectType.TERRORIZED);
            view.displayMessage("Terror grips you - your attack is useless!", GameView.MessageType.DAMAGE);
            if (effect != null) {
                effect.consumeCharge();
                if (effect.isExpired()) {
                    player.removeStatusEffect(EffectType.TERRORIZED);
                }
            }
            return ActionResult.ACTION_SKIPPED;
        }

        if (player.hasStatusEffect(EffectType.SHACKLED) && (weapon == null || !weapon.isRanged())) {
            view.displayMessage("You are shackled - you can only use ranged weapons!", GameView.MessageType.ERROR);
            return ActionResult.ACTION_INVALID;
        }

        String weaponType = weapon != null ? weapon.getWeaponType() : "melee";

        if (weapon != null && weapon.isRanged() && !monster.isGuaranteedHit() && weapon.didMiss()) {
            view.displayMessage("Your shot misses!", GameView.MessageType.SYSTEM);
            return ActionResult.ACTION_MISSED;
        }

        if (player.hasStatusEffect(EffectType.EVADE_PENALTY)) {
            StatusEffect penaltyEffect = player.getStatusEffect(EffectType.EVADE_PENALTY);
            double hitPenalty = penaltyEffect == null ? 0.0 : penaltyEffect.getModifier();
            player.removeStatusEffect(EffectType.EVADE_PENALTY);
            if (!monster.isGuaranteedHit() && rng.nextDouble() < hitPenalty) {
                view.displayMessage("The possessor dodges your attack!", GameView.MessageType.SYSTEM);
                return ActionResult.ACTION_MISSED;
            }
        }

        int rawATK = player.getEffectiveATK();
        int adjustedDamage = (int) (rawATK * monster.getDamageModifier(weaponType));

        if (monster.hasFortifyActive() && "melee".equalsIgnoreCase(weaponType)) {
            double chance = monster.getSpecialFlag("FORTIFY_MELEE_HALF_CHANCE");
            if (chance <= 0) {
                chance = 0.40;
            }
            if (rng.nextDouble() < chance) {
                adjustedDamage = adjustedDamage / 2;
                view.displayMessage("The possessor's fortified stance absorbs half the blow!",
                        GameView.MessageType.SYSTEM);
            }
            monster.setFortifyActive(false);
        }

        monster.takeDamage(adjustedDamage);
        view.displayMessage(monster.getName() + " takes " + adjustedDamage + " damage! HP: "
                + monster.getCurrentHp() + " / " + monster.getMaxHp(), GameView.MessageType.DAMAGE);
        view.updateMonsterStats(monster.getName(), monster.getCurrentHp(), monster.getMaxHp());
        return ActionResult.ACTION_SUCCESS;
    }

    private void executeMonsterTurn(Player player, Monster monster) {
        if (!inCombat || monster == null || monster.isDefeated()) {
            return;
        }

        view.setCombatActionsEnabled(false);
        view.displayMessage(monster.getName() + " acts...", GameView.MessageType.SYSTEM);

        MonsterAbility ability = selectAbility(monster);
        if (ability == null) {
            view.displayMessage(monster.getName() + " hesitates.", GameView.MessageType.SYSTEM);
            return;
        }

        executeAbility(ability, monster, player);
        player.setDefending(false);
    }

    private MonsterAbility selectAbility(Monster monster) {
        if (monster.isType("Freak")) {
            double threshold = monster.getSpecialFlag("FLEE_HP_THRESHOLD");
            double fleeChance = monster.getSpecialFlag("FLEE_CHANCE_WHEN_LOW");
            if (threshold > 0 && monster.getCurrentHp() <= (int) (monster.getMaxHp() * threshold)
                    && rng.nextDouble() < fleeChance) {
                return monster.getAbilityByName("Flee");
            }
        }

        List<MonsterAbility> available = new ArrayList<>(monster.getAbilities());
        if (monster.isType("Freak")) {
            available.removeIf(a -> "Flee".equalsIgnoreCase(a.getName()));
        }
        if (monster.isType("Poltergeist")) {
            // Keep combat outcome deterministic; do not allow random barricade-flee exits.
            available.removeIf(a -> "Barricade".equalsIgnoreCase(a.getName()));
        }

        if (available.isEmpty()) {
            return null;
        }
        return available.get(rng.nextInt(available.size()));
    }

    private void executeAbility(MonsterAbility ability, Monster monster, Player player) {
        String name = ability.getName();
        double damagePercent = ability.getDamagePercent();
        double hitChance = ability.getHitChance();
        EffectType statusEffect = ability.getStatusEffect();
        double effectChance = ability.getEffectChance();

        if (monster.isType("Shadow") && monster.isInBuilding()) {
            hitChance += monster.getSpecialFlag("BUILDING_HIT_BONUS");
        }

        double darkenMultiplier = 1.0;
        if (monster.isType("Shadow") && monster.isDarkenActive()) {
            darkenMultiplier = monster.getSpecialFlag("DARKEN_MULTIPLIER");
            if (darkenMultiplier <= 0) {
                darkenMultiplier = 1.5;
            }
            monster.setDarkenActive(false);
        }

        if ("Darken".equalsIgnoreCase(name)) {
            if (rng.nextDouble() < hitChance) {
                monster.setDarkenActive(true);
                view.displayMessage("The Shadow strengthens itself with darkness...", GameView.MessageType.SYSTEM);
            } else {
                view.displayMessage("The Shadow stirs but nothing happens.", GameView.MessageType.SYSTEM);
            }
            return;
        }

        if ("Evade".equalsIgnoreCase(name)) {
            double penalty = monster.getSpecialFlag("EVADE_PLAYER_HIT_PENALTY");
            if (penalty <= 0) {
                penalty = 0.15;
            }
            player.addStatusEffect(new StatusEffect(EffectType.EVADE_PENALTY, penalty, 1));
            view.displayMessage("The Possessor coils to dodge your next attack!", GameView.MessageType.SYSTEM);
            return;
        }

        if ("Fortify".equalsIgnoreCase(name)) {
            monster.setFortifyActive(true);
            view.displayMessage("The Possessor hardens itself against melee strikes!", GameView.MessageType.SYSTEM);
            return;
        }

        if ("Barricade".equalsIgnoreCase(name)) {
            String direction = combatRoom.getRandomUnbarricadedExit();
            if (direction != null) {
                combatRoom.barricadeExit(direction);
            }
            monster.setHasBarricaded(true);
            Map<String, String> params = new HashMap<>();
            params.put("direction", direction == null ? "nearest" : direction);
            view.displayMessage(
                    FlavorText.get("BARRICADE_FLEE", "The Poltergeist FLEES and barricades an exit.", params),
                    GameView.MessageType.SYSTEM);
            endCombat(player, monster, CombatResult.PLAYER_WIN_BARRICADE);
            return;
        }

        if ("Flee".equalsIgnoreCase(name)) {
            monster.setHasFled(true);
            view.displayMessage("The Freak is severely weakened - it FLEES the room!", GameView.MessageType.SYSTEM);
            endCombat(player, monster, CombatResult.PLAYER_WIN_FLEE);
            return;
        }

        if ("Summon".equalsIgnoreCase(name)) {
            if (!monster.isSummoning()) {
                monster.startSummonCast();
                view.displayMessage("The Freak begins a terrible incantation...", GameView.MessageType.SYSTEM);
            } else {
                monster.tickSummonCast();
                if (monster.isSummonReady()) {
                    executeSummon(monster, player);
                }
            }
            return;
        }

        if ("Shotgun".equalsIgnoreCase(name)) {
            double shotgunMax = monster.getSpecialFlag("SHOTGUN_MAX");
            if (shotgunMax > damagePercent) {
                damagePercent = damagePercent + (rng.nextDouble() * (shotgunMax - damagePercent));
            }
        }

        if ("Boil".equalsIgnoreCase(name) && player.hasKeyItem("Lantern")) {
            double negated = monster.getSpecialFlag("BOIL_NEGATED_DAMAGE");
            if (negated > 0) {
                damagePercent = negated;
                view.displayMessage("Your protective item absorbs most of the foul liquid!",
                        GameView.MessageType.SYSTEM);
            }
        }

        if (rng.nextDouble() >= hitChance) {
            view.displayMessage(monster.getName() + "'s " + name + " misses!", GameView.MessageType.SYSTEM);
            return;
        }

        double defendMultiplier = player.isDefending() ? 0.5 : 1.0;
        int rawDamage = calculateMonsterDamage(player, damagePercent, darkenMultiplier, defendMultiplier);
        int finalDamage = Math.max(0, rawDamage - player.getDefenseValue());

        player.takeDamage(finalDamage);
        view.displayMessage(monster.getName() + " uses " + name + "! You take " + finalDamage + " damage! HP: "
                + player.getCurrentHp() + " / " + player.getMaxHP(), GameView.MessageType.DAMAGE);
        view.updatePlayerStats(player.getCurrentHp(), player.getMaxHP(), player.getAttackValue(),
                player.getDefenseValue());

        // Keep combat progression consistent by avoiding hard-lock status chains from
        // monster attacks.
    }

    private int calculateMonsterDamage(Player player, double damagePercent, double damageMultiplier,
            double defendMultiplier) {
        // Scale percent-based monster damage so long playthroughs remain survivable.
        int baseDamage = (int) Math.round(player.getMaxHP() * damagePercent * 0.35);
        int variance = rng.nextInt(DAMAGE_VARIANCE * 2 + 1) - DAMAGE_VARIANCE;
        int variedDamage = Math.max(0, baseDamage + variance);
        return (int) Math.round(variedDamage * damageMultiplier * defendMultiplier);
    }

    private void applyStatusEffect(EffectType effectType, Player player) {
        switch (effectType) {
            case STUNNED -> {
                if (!player.hasStatusEffect(EffectType.STUNNED)) {
                    player.addStatusEffect(new StatusEffect(EffectType.STUNNED, 0.0, 1));
                    view.displayMessage("You are STUNNED and will lose your next turn!", GameView.MessageType.SYSTEM);
                }
            }
            case TERRORIZED -> {
                if (!player.hasStatusEffect(EffectType.TERRORIZED)) {
                    player.addStatusEffect(new StatusEffect(EffectType.TERRORIZED, 0.0, 2));
                    view.displayMessage("TERROR grips you - your next two attacks are useless!",
                            GameView.MessageType.SYSTEM);
                }
            }
            case DISEASED -> {
                if (!player.hasStatusEffect(EffectType.DISEASED)) {
                    player.addStatusEffect(new StatusEffect(EffectType.DISEASED, 0.15, COMBAT_DURATION));
                    view.displayMessage("You are DISEASED - healing items are less effective!",
                            GameView.MessageType.SYSTEM);
                }
            }
            case SHACKLED -> {
                if (!player.hasStatusEffect(EffectType.SHACKLED)) {
                    player.addStatusEffect(new StatusEffect(EffectType.SHACKLED, 0.0, COMBAT_DURATION));
                    view.displayMessage("Your shadow is SHACKLED - you can only use ranged weapons!",
                            GameView.MessageType.SYSTEM);
                }
            }
            default -> {
            }
        }
    }

    private void tickStatusEffects(Player player) {
        List<StatusEffect> toRemove = new ArrayList<>();
        for (StatusEffect effect : player.getStatusEffects()) {
            if (effect.getDuration() == COMBAT_DURATION) {
                continue;
            }
            effect.tick();
            if (effect.isExpired()) {
                toRemove.add(effect);
                Map<String, String> params = new HashMap<>();
                params.put("effectName", effect.getName());
                view.displayMessage(FlavorText.get("STATUS_EXPIRE", effect.getName() + " has worn off.", params),
                        GameView.MessageType.SYSTEM);
            }
        }
        for (StatusEffect effect : toRemove) {
            player.removeStatusEffect(effect.getEffectType());
        }
    }

    private ActionResult executeUseItem(Player player, String itemNameOrNull) {
        if (player.isItemUsedThisTurn()) {
            view.displayMessage("You have already used an item this turn.", GameView.MessageType.ERROR);
            return ActionResult.ACTION_INVALID;
        }

        List<Consumable> consumables = player.getConsumables();
        if (consumables.isEmpty()) {
            view.displayMessage("You have no usable items.", GameView.MessageType.ERROR);
            return ActionResult.ACTION_INVALID;
        }

        Consumable selected = null;
        if (itemNameOrNull != null && !itemNameOrNull.isBlank()) {
            for (Consumable c : consumables) {
                if (c.getName().equalsIgnoreCase(itemNameOrNull)) {
                    selected = c;
                    break;
                }
            }
        }
        if (selected == null) {
            selected = consumables.get(0);
        }

        int healAmount = selected.getHpEffect();
        if (healAmount > 0 && player.hasStatusEffect(EffectType.DISEASED)) {
            StatusEffect diseased = player.getStatusEffect(EffectType.DISEASED);
            double reduction = diseased == null ? 0.15 : diseased.getModifier();
            healAmount = (int) (healAmount * (1.0 - reduction));
        }

        if (healAmount > 0) {
            player.heal(healAmount);
            view.displayMessage("You use " + selected.getName() + " and recover " + healAmount + " HP. HP: "
                    + player.getCurrentHp() + " / " + player.getMaxHP(), GameView.MessageType.HEAL);
        }

        if (selected.getDefEffect() > 0) {
            player.addStatusEffect(
                    new StatusEffect(EffectType.TEMP_DEF, selected.getDefEffect(), selected.getDefDuration()));
            view.displayMessage("You gain +" + selected.getDefEffect() + " DEF for " + selected.getDefDuration()
                    + " turn(s).", GameView.MessageType.SYSTEM);
        }

        if (selected.getHpPenalty() > 0) {
            player.takeDamage(selected.getHpPenalty());
        }

        player.removeItem(selected);
        player.setItemUsedThisTurn(true);
        view.displayMessage("You may still act this turn.", GameView.MessageType.SYSTEM);
        return ActionResult.ACTION_ITEM_USED;
    }

    private ActionResult executeEquipWeapon(Player player, String weaponNameOrNull) {
        List<Weapon> weapons = player.getWeapons();
        if (weapons.isEmpty()) {
            view.displayMessage("You have no weapons to swap to.", GameView.MessageType.ERROR);
            return ActionResult.ACTION_INVALID;
        }

        Weapon selected = null;
        if (weaponNameOrNull != null && !weaponNameOrNull.isBlank()) {
            for (Weapon weapon : weapons) {
                if (weapon.getName().equalsIgnoreCase(weaponNameOrNull)) {
                    selected = weapon;
                    break;
                }
            }
        }
        if (selected == null) {
            selected = weapons.get(0);
        }

        if (player.getEquippedWeapon() != null) {
            player.addToInventory(player.getEquippedWeapon());
        }
        player.equipWeapon(selected);
        player.getInventory().remove(selected);

        view.displayMessage("You equip the " + selected.getName() + ". Equipping costs your turn.",
                GameView.MessageType.SYSTEM);
        view.updateEquipment(selected.getName(), selected.getAtkBonus(),
                player.getEquippedArmor() != null ? player.getEquippedArmor().getName() : "None",
                player.getDefenseValue());
        return ActionResult.ACTION_SUCCESS;
    }

    private ActionResult executePlayerFlee(Player player, Monster monster, Room room) {
        if (rng.nextDouble() < BASE_FLEE_CHANCE) {
            Room previousRoom = player.getPreviousRoom();
            monster.reset();
            if (previousRoom != null) {
                player.setCurrentRoom(previousRoom);
                int previousRoomNumber = game.getRoomNumberById(previousRoom.getRoomId());
                if (previousRoomNumber > 0) {
                    player.setLocationNoMove(previousRoomNumber);
                }
            }
            player.clearStatusEffects();
            view.displayMessage("You managed to escape!", GameView.MessageType.SYSTEM);
            endCombat(player, monster, CombatResult.PLAYER_FLED);
            return ActionResult.ACTION_SUCCESS;
        }

        view.displayMessage("You failed to escape! The " + monster.getName() + " blocks your way!",
                GameView.MessageType.SYSTEM);
        return ActionResult.ACTION_FAILED;
    }

    private void executeSummon(Monster freak, Player player) {
        int healAmount = (int) (freak.getMaxHp() * freak.getSpecialFlag("SUMMON_HEAL_PERCENT"));
        if (healAmount <= 0) {
            healAmount = (int) (freak.getMaxHp() * 0.15);
        }
        freak.heal(healAmount);

        Monster summonedMonster = createRandomGhost();
        if (summonedMonster == null) {
            return;
        }

        freak.setSummonedGhost(summonedMonster);
        freak.setSummonActive(true);
        freak.setHasSummoned(true);
        freak.resetSummonTurnCount();

        view.displayMessage("The Freak's incantation completes! It heals for " + healAmount
                + " HP and summons an ally!", GameView.MessageType.SYSTEM);
        view.displayMessage("A " + summonedMonster.getName() + " appears and takes The Freak's place!",
                GameView.MessageType.SYSTEM);

        activeMonster = summonedMonster;
        activeMonster.setGuaranteedHit(true);
    }

    private Monster createRandomGhost() {
        List<Monster> templates = new ArrayList<>();
        for (Monster monster : game.getAllMonsters()) {
            if (!monster.isType("Freak")) {
                templates.add(monster);
            }
        }
        if (templates.isEmpty()) {
            return null;
        }
        return templates.get(rng.nextInt(templates.size())).createSummonedVariant();
    }

    private void tickSummonedPhase(Monster freak, Monster summonedMonster, Player player) {
        freak.incrementSummonTurnCount();
        int maxTurns = (int) freak.getSpecialFlag("SUMMON_MAX_TURNS");
        if (maxTurns <= 0) {
            maxTurns = 20;
        }

        if (summonedMonster.isDefeated() || freak.getSummonTurnCount() >= maxTurns) {
            view.displayMessage("The summoned ghost vanishes!", GameView.MessageType.SYSTEM);
            freak.setSummonActive(false);
            freak.setSummonedGhost(null);
            activeMonster = freak;
            view.displayMessage("The Freak returns to finish what it started!", GameView.MessageType.NARRATION);
            view.updateMonsterStats(activeMonster.getName(), activeMonster.getCurrentHp(), activeMonster.getMaxHp());
        }
    }

    private boolean checkCombatEnd(Player player, Monster monster, Room room) {
        if (!inCombat) {
            return true;
        }

        Monster currentMonster = monster != null ? monster : activeMonster;
        if (currentMonster == null) {
            return true;
        }

        if (!player.isAlive()) {
            endCombat(player, currentMonster, CombatResult.PLAYER_DEAD);
            return true;
        }

        if (rootMonster != null && rootMonster.isType("Freak") && rootMonster.isSummonActive()) {
            Monster summoned = rootMonster.getSummonedGhost();
            if (summoned != null) {
                tickSummonedPhase(rootMonster, summoned, player);
                return false;
            }
        }

        if (currentMonster.isDefeated()) {
            endCombat(player, currentMonster, CombatResult.MONSTER_DEAD);
            return true;
        }

        return false;
    }

    private void endCombat(Player player, Monster monster, CombatResult result) {
        player.clearStatusEffects();
        player.setDefending(false);
        player.setItemUsedThisTurn(false);

        switch (result) {
            case MONSTER_DEAD -> {
                combatRoom.removeMonster();
                view.displayMessage(getMonsterDeathFlavor(monster), GameView.MessageType.NARRATION);
                view.displayMessage("--- Combat over ---", GameView.MessageType.SEPARATOR);
                checkCleanseWinCondition();
                view.displayMessage(player.getCurrentRoom() != null ? player.getCurrentRoom().getFullDescription()
                        : combatRoom.getFullDescription(), GameView.MessageType.NARRATION);
            }
            case PLAYER_WIN_FLEE -> {
                combatRoom.removeMonster();
                monster.setHasFled(true);
                view.displayMessage("--- Combat over (enemy fled) ---", GameView.MessageType.SEPARATOR);
                checkCleanseWinCondition();
            }
            case PLAYER_WIN_BARRICADE -> {
                combatRoom.removeMonster();
                view.displayMessage("--- Combat over (enemy fled) ---", GameView.MessageType.SEPARATOR);
            }
            case PLAYER_FLED -> view.displayMessage("--- Combat over (you fled) ---", GameView.MessageType.SEPARATOR);
            case PLAYER_DEAD -> {
                view.displayMessage(FlavorText.get("DEATH_CAUSE_COMBAT", "You have been defeated..."),
                        GameView.MessageType.DAMAGE);
                view.displayMessage(FlavorText.get("DEATH_HEADER", "--- YOU HAVE FALLEN ---"),
                        GameView.MessageType.SYSTEM);
                view.showGameOverScreen();
            }
        }

        inCombat = false;
        rootMonster = null;
        activeMonster = null;
        combatRoom = null;
    }

    public void checkCleanseWinCondition() {
        if (endingReached) {
            return;
        }
        Map<String, Room> roomGraph = game.getRoomGraph();
        for (String roomId : List.of("GH-03", "GS-01", "TH-03", "GS-04")) {
            Room room = roomGraph.get(roomId);
            if (room == null) {
                return;
            }
            Monster monster = room.getMonster();
            boolean defeated = (monster == null) || (monster.isType("Freak") && monster.hasFled());
            if (!defeated) {
                return;
            }
        }

        view.displayMessage(FlavorText.get("CLEANSE_1", "The last of the town's tormentors has fallen."),
                GameView.MessageType.NARRATION);
        view.displayMessage(FlavorText.get("CLEANSE_2", "The shadows recede and the cold lifts."),
                GameView.MessageType.NARRATION);
        view.displayMessage(FlavorText.get("CLEANSE_3", "The town falls silent."),
                GameView.MessageType.NARRATION);
        view.displayMessage(FlavorText.get("CLEANSE_WIN", "--- CLEANSE ENDING ---"), GameView.MessageType.SYSTEM);
        endingReached = true;
        view.showWinScreen(GameView.WinType.CLEANSE);
    }

    public boolean checkEscapeWinCondition(Player player) {
        if (endingReached) {
            return true;
        }
        Room current = player.getCurrentRoom();
        if (current != null && "GH-01".equals(current.getRoomId()) && player.hasKeyItem("Front Gate Key")) {
            view.displayMessage(FlavorText.get("ESCAPE_1", "You use the Front Gate Key."),
                    GameView.MessageType.NARRATION);
            view.displayMessage(FlavorText.get("ESCAPE_2", "The gate swings open."),
                    GameView.MessageType.NARRATION);
            view.displayMessage(FlavorText.get("ESCAPE_3", "Cold air rushes in."),
                    GameView.MessageType.NARRATION);
            view.displayMessage(FlavorText.get("ESCAPE_4", "You step through and do not look back."),
                    GameView.MessageType.NARRATION);
            view.displayMessage(FlavorText.get("ESCAPE_WIN", "--- YOU ESCAPED ---"), GameView.MessageType.SYSTEM);
            endingReached = true;
            view.showWinScreen(GameView.WinType.ESCAPE);
            return true;
        }
        return false;
    }

    private String getMonsterStartFlavor(Monster monster) {
        if (monster == null) {
            return "";
        }
        if (monster.getCombatStartText() != null && !monster.getCombatStartText().isBlank()) {
            return monster.getCombatStartText();
        }
        Map<String, String> params = new HashMap<>();
        params.put("monsterName", monster.getName());
        return FlavorText.get("MONSTER_START_GENERIC", monster.getDescription(), params);
    }

    private String getMonsterDeathFlavor(Monster monster) {
        if (monster == null) {
            return FlavorText.get("MONSTER_DEATH_GENERIC", "A monster has fallen.");
        }
        if (monster.getCombatDeathText() != null && !monster.getCombatDeathText().isBlank()) {
            return monster.getCombatDeathText();
        }
        Map<String, String> params = new HashMap<>();
        params.put("monsterName", monster.getName());
        return FlavorText.get("MONSTER_DEATH_GENERIC", "The {monsterName} is defeated!", params);
    }
}
