package client;

import handling.Buffstat;
import handling.SendPacketOpcode;
import java.io.Serializable;

public enum MapleBuffStat
        implements Serializable, Buffstat {

    WATK(1, 1),
    WDEF(2, 1),
    MATK(4, 1),
    SURPLUS(0x200, 10),
    FIRE_AURA(0x80000000, 10),
    MDEF(8, 1),
    moon1(0x80000, 10),
    rising5(0x1000, 10),
    rising1(0x10000, 10),
    rising2(0x80000000, 9),
    rising3(0x40000, 9),
    ASURA(0x10000, 9),
    moon2(0x80000000, 7),
    moon3(0x100, 7),
    CR_PERCENT(0x40000000, 0),
    moon4(0x80000000, 9),
    moon5(0x4000, 9),
    ris1(0x10000, 10),
    DMG_DEC(0x10000, 10),
    fal1(0x10000, 10),
    fal2(0x80000000, 9),
    fal3(0x40000, 9),
    PERCENT_ACC(0x20000, 0),
    ADD_AVOID(0x1000, 0),
    ADD_ACC(0x2000, 7),
    ACC(16, 1),
    SKILL_COUNT(0x40000000, 10),
    LUMINOUS_GAUGE(0x200, 1),
    STACK_WATK(0x40000000, 2),
    AVOID(32, 1),
    HANDS(64, 1),
    SPEED(128, 1),
    JUMP(256, 1),
    MAGIC_GUARD(512, 1),
    DARKSIGHT(1024, 1),
    LIGHTNING(0x80, 10),
    PERCENT_DAMAGE_BUFF(0x80000000, 7),
    BOOSTER(2048, 1),
    POWERGUARD(4096, 1),
    MAXHP(8192, 1),
    MAXMP(16384, 1),
    INVINCIBLE(32768, 1),
    SOULARROW(65536, 1),
    COMBO(2097152, 1),
    SUMMON(2097152, 1),
    WK_CHARGE(4194304, 1),
    DRAGONBLOOD(8388608, 1),
    HOLY_SYMBOL(16777216, 1),
    MESOUP(33554432, 1),
    SHADOWPARTNER(67108864, 1),
    PICKPOCKET(134217728, 1),
    PUPPET(134217728, 1),
    MESOGUARD(268435456, 1),
    HP_LOSS_GUARD(536870912, 1),
    MORPH(2, 2),
    RECOVERY(4, 2),
    MAPLE_WARRIOR(8, 2),
    STANCE(16, 2),
    STATUS_RESIST(16, 2),
    ELEMENT_RESIST(32, 2),
    SHARP_EYES(32, 2),
    ALBA1(0x4000, 10),
    ALBA2(0x40000, 9),
    ALBA3(0x100, 7),
    ALBA4(0x400, 5),
    ALBA5(0x1000, 5),
    MANA_REFLECTION(64, 2),
    SPIRIT_CLAW(256, 2),
    INFINITY(512, 2),
    HOLY_SHIELD(1024, 2),
    HAMSTRING(2048, 2),
    BLIND(4096, 2),
    CONCENTRATE(8192, 2),
    ECHO_OF_HERO(32768, 2),
    MESO_RATE(65536, 2),
    GHOST_MORPH(131072, 2),
    ARIANT_COSS_IMU(262144, 2),
    DROP_RATE(1048576, 2),
    EXPRATE(4194304, 2),
    ACASH_RATE(8388608, 2),
    ILLUSION(16777216, 2),
    ATTACK_COUNT(0x80000000, 1),
    BERSERK_FURY(134217728, 2),
    DIVINE_BODY(268435456, 2),
    SPARK(536870912, 2),
    ARIANT_COSS_IMU2(1073741824, 2),
    DEFENCE_R(67108864, 2),
    SLOW(-2147483648, 2),
    FINALATTACK(-2147483648, 2),
    Ignore_DEF(-2147483648, 2),
    ELEMENT_RESET(0x80000000, 4), // CHECK
    WIND_WALK(1, 3),
    ARAN_COMBO(16, 3),
    COMBO_DRAIN(32, 3),
    COMBO_BARRIER(64, 3),
    BODY_PRESSURE(128, 3),
    SMART_KNOCKBACK(256, 3),
    PYRAMID_PQ(512, 3),
    MAGIC_SHIELD(8192, 3),
    MAGIC_RESISTANCE(16384, 3),
    SOUL_STONE(67108864, 7),
    SOARING(0x10000, 3),
    LIGHTNING_CHARGE(0x400000, 3),
    ENRAGE(2097152, 3),
    OWL_SPIRIT(4194304, 3),
    FINAL_CUT(4194304, 3),
    DAMAGE_BUFF(8388608, 3),
    ATTACK_BUFF(16777216, 3),
    RAINING_MINES(33554432, 3),
    ENHANCED_MAXHP(67108864, 3),
    ENHANCED_MAXMP(134217728, 3),
    ENHANCED_WATK(268435456, 3),
    ENHANCED_MATK(536870912, 3),
    MIN_CRITICAL_DAMAGE(-2147483648, 3),
    ENHANCED_WDEF(1, 4),
    ENHANCED_MDEF(2, 4),
    PERFECT_ARMOR(4, 4),
    SATELLITESAFE_PROC(8, 4),
    SATELLITESAFE_ABSORB(16, 4),
    TORNADO(32, 4),
    CRITICAL_RATE_BUFF(16, 4),
    MP_BUFF(32, 4),
    DAMAGE_TAKEN_BUFF(64, 4),
    DODGE_CHANGE_BUFF(128, 4),
    CONVERSION(256, 4),
    REAPER(512, 4),
    INFILTRATE(1024, 4),
    MECH_CHANGE(2048, 4),
    AURA(4096, 4),
    DARK_AURA(8192, 4),
    BLUE_AURA(16384, 4),
    YELLOW_AURA(32768, 4),
    BODY_BOOST(65536, 4),
    FELINE_BERSERK(131072, 4),
    DICE_ROLL(262144, 4),
    DIVINE_SHIELD(524288, 4),
    PIRATES_REVENGE(1048576, 4),
    TELEPORT_MASTERY(2097152, 4),
    COMBAT_ORDERS(4194304, 4),
    BEHOLDER(8388608, 4),
    ONYX_SHROUD(16777216, 4),
    GIANT_POTION(33554432, 4),
    ONYX_WILL(536870912, 4),
    BLESS(-2147483648, 4),
    THREATEN_PVP(4, 5),
    ICE_KNIGHT(8, 5),
    STR(64, 5),
    INT(128, 5),
    DEX(256, 5),
    LUK(512, 5),
    ANGEL_ATK(1024, 5, true),
    ANGEL_MATK(2048, 5, true),
    HP_BOOST(4096, 5, true),
    MP_BOOST(8192, 5, true),
    ANGEL_ACC(16384, 5, true),
    ANGEL_AVOID(32768, 5, true),
    ANGEL_JUMP(65536, 5, true),
    ANGEL_SPEED(131072, 5, true),
    ANGEL_STAT(262144, 5, true),
    PVP_DAMAGE(2097152, 5),
    PVP_ATTACK(4194304, 5),
    INVINCIBILITY(8388608, 5),
    HIDDEN_POTENTIAL(16777216, 5),
    ELEMENT_WEAKEN(33554432, 5),
    SNATCH(67108864, 5),
    FROZEN(134217728, 5),
    ICE_SKILL(536870912, 5),
    BOUNDLESS_RAGE(-2147483648, 5),
    HOLY_MAGIC_SHELL(1, 6),
    ARCANE_AIM(4, 6),
    BUFF_MASTERY(8, 6),
    ABNORMAL_STATUS_R(16, 6),
    ELEMENTAL_STATUS_R(32, 6),
    WATER_SHIELD(64, 6),
    DARK_METAMORPHOSIS(128, 6),
    BARREL_ROLL(256, 6),
    SPIRIT_SURGE(512, 6),
    SPIRIT_LINK(1024, 6, true),
    Dusk_Guard(8192, 6, true),
    SPIRIT_damage(32768, 6),
    VIRTUE_EFFECT(4096, 6),
    NO_SLIP(1048576, 6),
    FAMILIAR_SHADOW(2097152, 6),
    DEFENCE_BOOST_R(67108864, 6),
    ABSORB_DAMAGE_HP(536870912, 6),
    UNKNOWN8(32, 7),
    HP_BOOST_PERCENT(8, 7, true),
    MP_BOOST_PERCENT(16, 7, true),
    UNKNOWN12(4096, 7),
    KILL_COUNT(131072, 7),
    INDIEBOOSTER(4096, 7, true),
    MANA_WELL(524288, 7, true),
    UNKNOWN9(8388608, 7),
    Ignore_resistances(33554432, 7),
    PHANTOM_MOVE(134217728, 7),
    JUDGMENT_DRAW(1073741824, 7),
    ARIA_ARMOR(4194304, 7),
    ATTACKUP_indieDamR(-2147483648, 7, true),
    UNKNOWN10(16, 8),
    Dark_Crescendo(1024, 8),
    Black_Blessing(2048, 8),
    PRESSURE_VOID(4096, 8),
    Lunar_Tide(8192, 8),
    KAISER_COMBO(32768, 8),
    Ignores_monster_DEF(65536, 8),
    KAISER_MODE_CHANGE(131072, 8),
    Tempest_Blades(1048576, 8),
    Crit_Damage(2097152, 8),
    Damage_Absorbed(8388608, 8),
    DASH_SPEED(67108864, 8),
    DASH_JUMP(134217728, 8),
    SPEED_INFUSION(536870912, 1),
    HOMING_BEACON(1073741824, 8),
    DEFAULT_BUFFSTAT(-2147483648, 8),
    Null(0, 8),
    BOSS_ATTDMG(16777216, 9),
    ENERGY_CHARGE(33554432, 9),
    MONSTER_RIDING(0x10000000, 10),
    Xenon_supply_surplus(0x2, 10),
    fly(0x10, 10),
    PROP(256, 10),
    Frozen_Shikigami_Haunting(1024, 10),
    SPEED_LEVEL(32768, 10),
    Battoujutsu_Stance(33554432, 11),
    Haku_Reborn(1048576, 10),
    QUIVERKARTRIGE((int) 0x40L, 1),;
    private static final long serialVersionUID = 0L;
    private final int buffstat;
    private final int first;
    private boolean stacked = false;

    private MapleBuffStat(int buffstat, int first) {
        this.buffstat = buffstat;
        this.first = first;
    }

    private MapleBuffStat(int buffstat, int first, boolean stacked) {
        this.buffstat = buffstat;
        this.first = first;
        this.stacked = stacked;
    }

    public final int getPosition() {
        return getPosition(false);
    }

    public final int getPosition(boolean fromZero) {
        if (!fromZero) {
            return this.first;
        }
        switch (this.first) {
            case 12:
                return 0;
            case 11:
                return 1;
            case 10:
                return 2;
            case 9:
                return 3;
            case 8:
                return 4;
            case 7:
                return 5;
            case 6:
                return 6;
            case 5:
                return 7;
            case 4:
                return 8;
            case 3:
                return 9;
            case 2:
                return 10;
            case 1:
                return 11;
        }
        return 0;
    }

    public final int getValue() {
        return this.buffstat;
    }

    public final boolean canStack() {
        return this.stacked;
    }
}