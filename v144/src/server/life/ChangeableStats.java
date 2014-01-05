package server.life;

import constants.GameConstants;

public class ChangeableStats extends OverrideMonsterStats {

    public int watk, matk, acc,eva,PDRate,MDRate,pushed,level,speed;

    public ChangeableStats(MapleMonsterStats stats, OverrideMonsterStats ostats) {
        this.hp = ostats.getHp();
        this.exp = ostats.getExp();
        this.mp = ostats.getMp();
        this.watk = stats.getPhysicalAttack();
        this.matk = stats.getMagicAttack();
        this.acc = stats.getAcc();
        this.eva = stats.getEva();
        this.PDRate = stats.getPDRate();
        this.MDRate = stats.getMDRate();
        this.pushed = stats.getPushed();
        this.speed = stats.getSpeed();
        this.level = stats.getLevel();
    }

    public ChangeableStats(MapleMonsterStats stats, int newLevel, boolean pqMob) {
        double mod = newLevel / stats.getLevel();
        double hpRatio = stats.getHp() / stats.getExp();
        double pqMod = pqMob ? 1.5D : 1.0D;
        this.hp = Math.round((!stats.isBoss() ? GameConstants.getMonsterHP(newLevel) : stats.getHp() * mod) * pqMod);
        this.exp = ((int) Math.round((!stats.isBoss() ? GameConstants.getMonsterHP(newLevel) / hpRatio : stats.getExp()) * pqMod));
        this.mp = ((int) Math.round(stats.getMp() * mod * pqMod));
        this.watk = ((int) Math.round(stats.getPhysicalAttack() * mod));
        this.matk = ((int) Math.round(stats.getMagicAttack() * mod));
        this.acc = Math.round(stats.getAcc() + Math.max(0, newLevel - stats.getLevel()) * 2);
        this.eva = Math.round(stats.getEva() + Math.max(0, newLevel - stats.getLevel()));
        this.PDRate = Math.min(stats.isBoss() ? 30 : 20, (int) Math.round(stats.getPDRate() * mod));
        this.MDRate = Math.min(stats.isBoss() ? 30 : 20, (int) Math.round(stats.getMDRate() * mod));
        this.pushed = ((int) Math.round(stats.getPushed() * mod));
        this.speed = (int) Math.round(stats.getSpeed() * mod);
        this.level = newLevel;
    }
}