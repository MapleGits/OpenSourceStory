package scripting;

import client.MapleCharacter;
import client.MapleQuestStatus;
import client.MapleTrait;
import client.SkillFactory;
import handling.channel.ChannelServer;
import handling.world.MapleParty;
import handling.world.MaplePartyCharacter;
import handling.world.World;
import handling.world.exped.PartySearch;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.script.ScriptException;
import server.MapleCarnivalParty;
import server.MapleItemInformationProvider;
import server.MapleSquad;
import server.Timer;
import server.life.MapleMonster;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import server.quest.MapleQuest;
import tools.FileoutputUtil;
import tools.Pair;
import tools.packet.CField;
import tools.packet.CWvsContext;

public class EventInstanceManager {

    private List<MapleCharacter> chars = new LinkedList();
    private List<Integer> dced = new LinkedList();
    private List<MapleMonster> mobs = new LinkedList();
    private Map<Integer, Integer> killCount = new HashMap();
    private EventManager em;
    private int channel;
    private String name;
    private Properties props = new Properties();
    private long timeStarted = 0L;
    private long eventTime = 0L;
    private List<Integer> mapIds = new LinkedList();
    private List<Boolean> isInstanced = new LinkedList();
    private ScheduledFuture<?> eventTimer;
    private final ReentrantReadWriteLock mutex = new ReentrantReadWriteLock();
    private final Lock rL = this.mutex.readLock();
    private final Lock wL = this.mutex.writeLock();
    private boolean disposed = false;

    public EventInstanceManager(EventManager em, String name, int channel) {
        this.em = em;
        this.name = name;
        this.channel = channel;
    }

    public void changedMap(MapleCharacter chr, int mapid) {
        if (this.disposed) {
            return;
        }
        try {
            this.em.getIv().invokeFunction("changedMap", new Object[]{this, chr, Integer.valueOf(mapid)});
        } catch (NullPointerException npe) {
        } catch (Exception ex) {
            FileoutputUtil.log("Log_Script_Except.txt", new StringBuilder().append("Event name").append(this.em.getName()).append(", Instance name : ").append(this.name).append(", method Name : changedMap:\n").append(ex).toString());
            System.out.println(new StringBuilder().append("Event name").append(this.em.getName()).append(", Instance name : ").append(this.name).append(", method Name : changedMap:\n").append(ex).toString());
        }
    }

    public void timeOut(long delay, final EventInstanceManager eim) {
        if ((this.disposed) || (eim == null)) {
            return;
        }
        this.eventTimer = Timer.EventTimer.getInstance().schedule(new Runnable() {
            public void run() {
                if ((EventInstanceManager.this.disposed) || (eim == null) || (EventInstanceManager.this.em == null)) {
                    return;
                }
                try {
                    EventInstanceManager.this.em.getIv().invokeFunction("scheduledTimeout", new Object[]{eim});
                } catch (Exception ex) {
                    FileoutputUtil.log("Log_Script_Except.txt", "Event name" + EventInstanceManager.this.em.getName() + ", Instance name : " + EventInstanceManager.this.name + ", method Name : scheduledTimeout:\n" + ex);
                    System.out.println("Event name" + EventInstanceManager.this.em.getName() + ", Instance name : " + EventInstanceManager.this.name + ", method Name : scheduledTimeout:\n" + ex);
                }
            }
        }, delay);
    }

    public void stopEventTimer() {
        this.eventTime = 0L;
        this.timeStarted = 0L;
        if (this.eventTimer != null) {
            this.eventTimer.cancel(false);
        }
    }

    public void addHonourXP() {
        for (MapleCharacter chr : getPlayers()) {
            chr.addHonourExp(1000 * chr.getHonourLevel());
            chr.dropMessage(5, 1000 * chr.getHonourLevel() + " Honor Exp gained.");
        }
    }

    public void restartEventTimer(long time) {
        try {
            if (this.disposed) {
                return;
            }
            this.timeStarted = System.currentTimeMillis();
            this.eventTime = time;
            if (this.eventTimer != null) {
                this.eventTimer.cancel(false);
            }
            this.eventTimer = null;
            int timesend = (int) time / 1000;

            for (MapleCharacter chr : getPlayers()) {
                if (this.name.startsWith("PVP")) {
                    chr.getClient().getSession().write(CField.getPVPClock(Integer.parseInt(getProperty("type")), timesend));
                } else {
                    chr.getClient().getSession().write(CField.getClock(timesend));
                }
            }
            timeOut(time, this);
        } catch (Exception ex) {
            FileoutputUtil.outputFileError("Log_Script_Except.txt", ex);
            System.out.println(new StringBuilder().append("Event name").append(this.em.getName()).append(", Instance name : ").append(this.name).append(", method Name : restartEventTimer:\n").toString());
            ex.printStackTrace();
        }
    }

    public void startEventTimer(long time) {
        restartEventTimer(time);
    }

    public boolean isTimerStarted() {
        return (this.eventTime > 0L) && (this.timeStarted > 0L);
    }

    public long getTimeLeft() {
        return this.eventTime - (System.currentTimeMillis() - this.timeStarted);
    }

    public void registerParty(MapleParty party, MapleMap map) {
        if (this.disposed) {
            return;
        }
        for (MaplePartyCharacter pc : party.getMembers()) {
            registerPlayer(map.getCharacterById(pc.getId()));
        }
        PartySearch ps = World.Party.getSearch(party);
        if (ps != null) {
            World.Party.removeSearch(ps, "The Party Listing has been removed because the Party Quest started.");
        }
    }

    public void unregisterPlayerAzwan(MapleCharacter chr) {
        this.wL.lock();
        try {
            this.chars.remove(chr);
        } finally {
            this.wL.unlock();
        }
        chr.setEventInstance(null);
    }

    public void registerPlayer(MapleCharacter chr) {
        try {
            this.wL.lock();
            try {
                this.chars.add(chr);
            } finally {
                this.wL.unlock();
            }
            chr.setEventInstance(this);
            this.em.getIv().invokeFunction("playerEntry", new Object[]{this, chr});
        } catch (NullPointerException ex) {
            FileoutputUtil.outputFileError("Log_Script_Except.txt", ex);
            ex.printStackTrace();
        } catch (Exception ex) {
            FileoutputUtil.log("Log_Script_Except.txt", new StringBuilder().append("Event name").append(this.em.getName()).append(", Instance name : ").append(this.name).append(", method Name : playerEntry:\n").append(ex).toString());
            System.out.println(new StringBuilder().append("Event name").append(this.em.getName()).append(", Instance name : ").append(this.name).append(", method Name : playerEntry:\n").append(ex).toString());
        }
    }

    public void unregisterPlayer(MapleCharacter chr) {
        if (this.disposed) {
            chr.setEventInstance(null);
            return;
        }
        this.wL.lock();
        try {
            unregisterPlayer_NoLock(chr);
        } finally {
            this.wL.unlock();
        }
    }

    private boolean unregisterPlayer_NoLock(MapleCharacter chr) {
        if (this.name.equals("CWKPQ")) {
            MapleSquad squad = ChannelServer.getInstance(this.channel).getMapleSquad("CWKPQ");
            if (squad != null) {
                squad.removeMember(chr.getName());
                if (squad.getLeaderName().equals(chr.getName())) {
                    this.em.setProperty("leader", "false");
                }
            }
        }
        chr.setEventInstance(null);
        if (this.disposed) {
            return false;
        }
        if (this.chars.contains(chr)) {
            this.chars.remove(chr);
            return true;
        }
        return false;
    }

    public final boolean disposeIfPlayerBelow(final byte size, final int towarp) {
        if (disposed) {
            return true;
        }
        MapleMap map = null;
        if (towarp > 0) {
            map = this.getMapFactory().getMap(towarp);
        }

        wL.lock();
        try {
            if (chars != null && chars.size() <= size) {
                final List<MapleCharacter> chrs = new LinkedList<>(chars);
                for (MapleCharacter chr : chrs) {
                    if (chr == null) {
                        continue;
                    }
                    unregisterPlayer_NoLock(chr);
                    if (chr != null && map != null && towarp > 0) {
                        chr.changeMap(map, map.getPortal(0));
                    }
                }
                dispose_NoLock();
                return true;
            }
        } catch (Exception ex) {
            FileoutputUtil.outputFileError(FileoutputUtil.ScriptEx_Log, ex);
        } finally {
            wL.unlock();
        }
        return false;
    }

    public final void saveBossQuest(int points) {
        if (this.disposed) {
            return;
        }
        for (MapleCharacter chr : getPlayers()) {
            MapleQuestStatus record = chr.getQuestNAdd(MapleQuest.getInstance(150001));

            if (record.getCustomData() != null) {
                record.setCustomData(String.valueOf(points + Integer.parseInt(record.getCustomData())));
            } else {
                record.setCustomData(String.valueOf(points));
            }
            chr.modifyCSPoints(1, points / 5, true);
            chr.getTrait(MapleTrait.MapleTraitType.will).addExp(points / 100, chr);
        }
    }

    public final void saveNX(int points) {
        if (this.disposed) {
            return;
        }
        for (MapleCharacter chr : getPlayers()) {
            chr.modifyCSPoints(1, points, true);
        }
    }

    public List<MapleCharacter> getPlayers() {
        if (this.disposed) {
            return Collections.emptyList();
        }
        this.rL.lock();
        try {
            return new LinkedList(this.chars);
        } finally {
            this.rL.unlock();
        }
    }

    public List<Integer> getDisconnected() {
        return this.dced;
    }

    public final int getPlayerCount() {
        if (this.disposed) {
            return 0;
        }
        return this.chars.size();
    }

    public void registerMonster(MapleMonster mob) {
        if (this.disposed) {
            return;
        }
        this.mobs.add(mob);
        mob.setEventInstance(this);
    }

    public void unregisterAll() {
        this.wL.lock();
        try {
            for (MapleCharacter chr : this.chars) {
                chr.setEventInstance(null);
            }
            this.chars.clear();
        } finally {
            this.wL.unlock();
        }
    }

    public void unregisterMonster(MapleMonster mob) {
        mob.setEventInstance(null);
        if (this.disposed) {
            return;
        }
        if (this.mobs.contains(mob)) {
            this.mobs.remove(mob);
        }
        if (this.mobs.size() == 0) {
            try {
                this.em.getIv().invokeFunction("allMonstersDead", new Object[]{this});
            } catch (Exception ex) {
                FileoutputUtil.log("Log_Script_Except.txt", new StringBuilder().append("Event name").append(this.em.getName()).append(", Instance name : ").append(this.name).append(", method Name : allMonstersDead:\n").append(ex).toString());
                System.out.println(new StringBuilder().append("Event name").append(this.em.getName()).append(", Instance name : ").append(this.name).append(", method Name : allMonstersDead:\n").append(ex).toString());
            }
        }
    }

    public void playerKilled(MapleCharacter chr) {
        if (this.disposed) {
            return;
        }
        try {
            this.em.getIv().invokeFunction("playerDead", new Object[]{this, chr});
        } catch (Exception ex) {
            FileoutputUtil.log("Log_Script_Except.txt", new StringBuilder().append("Event name").append(this.em.getName()).append(", Instance name : ").append(this.name).append(", method Name : playerDead:\n").append(ex).toString());
            System.out.println(new StringBuilder().append("Event name").append(this.em.getName()).append(", Instance name : ").append(this.name).append(", method Name : playerDead:\n").append(ex).toString());
        }
    }

    public boolean revivePlayer(MapleCharacter chr) {
        if (this.disposed) {
            return false;
        }
        try {
            Object b = this.em.getIv().invokeFunction("playerRevive", new Object[]{this, chr});
            if ((b instanceof Boolean)) {
                return ((Boolean) b).booleanValue();
            }
        } catch (Exception ex) {
            FileoutputUtil.log("Log_Script_Except.txt", new StringBuilder().append("Event name").append(this.em.getName()).append(", Instance name : ").append(this.name).append(", method Name : playerRevive:\n").append(ex).toString());
            System.out.println(new StringBuilder().append("Event name").append(this.em.getName()).append(", Instance name : ").append(this.name).append(", method Name : playerRevive:\n").append(ex).toString());
        }
        return true;
    }

    public void playerDisconnected(MapleCharacter chr, int idz) {
        if (this.disposed) {
            return;
        }
        byte ret;
        try {
            ret = ((Double) this.em.getIv().invokeFunction("playerDisconnected", new Object[]{this, chr})).byteValue();
        } catch (Exception e) {
            ret = 0;
        }

        this.wL.lock();
        try {
            if (this.disposed) {
                return;
            }
            if ((chr == null) || (chr.isAlive())) {
                this.dced.add(Integer.valueOf(idz));
            }
            if (chr != null) {
                unregisterPlayer_NoLock(chr);
            }
            if (ret == 0) {
                if (getPlayerCount() <= 0) {
                    dispose_NoLock();
                }
            } else if (((ret > 0) && (getPlayerCount() < ret)) || ((ret < 0) && ((isLeader(chr)) || (getPlayerCount() < ret * -1)))) {
                List<MapleCharacter> chrs = new LinkedList(this.chars);
                for (MapleCharacter player : chrs) {
                    if (player.getId() != idz) {
                        removePlayer(player);
                    }
                }
                dispose_NoLock();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            FileoutputUtil.outputFileError("Log_Script_Except.txt", ex);
        } finally {
            this.wL.unlock();
        }
    }

    public void monsterKilled(MapleCharacter chr, MapleMonster mob) {
        if (this.disposed) {
            return;
        }
        try {
            int inc = ((Double) this.em.getIv().invokeFunction("monsterValue", new Object[]{this, Integer.valueOf(mob.getId())})).intValue();
            if ((this.disposed) || (chr == null)) {
                return;
            }
            Integer kc = (Integer) this.killCount.get(Integer.valueOf(chr.getId()));
            if (kc == null) {
                kc = Integer.valueOf(inc);
            } else {
                kc = Integer.valueOf(kc.intValue() + inc);
            }
            this.killCount.put(Integer.valueOf(chr.getId()), kc);
            if ((chr.getCarnivalParty() != null) && ((mob.getStats().getPoint() > 0) || (mob.getStats().getCP() > 0))) {
                this.em.getIv().invokeFunction("monsterKilled", new Object[]{this, chr, Integer.valueOf(mob.getStats().getCP() > 0 ? mob.getStats().getCP() : mob.getStats().getPoint())});
            }
        } catch (ScriptException ex) {
            System.out.println(new StringBuilder().append("Event name").append(this.em == null ? "null" : this.em.getName()).append(", Instance name : ").append(this.name).append(", method Name : monsterValue:\n").append(ex).toString());
            FileoutputUtil.log("Log_Script_Except.txt", new StringBuilder().append("Event name").append(this.em == null ? "null" : this.em.getName()).append(", Instance name : ").append(this.name).append(", method Name : monsterValue:\n").append(ex).toString());
        } catch (NoSuchMethodException ex) {
            System.out.println(new StringBuilder().append("Event name").append(this.em == null ? "null" : this.em.getName()).append(", Instance name : ").append(this.name).append(", method Name : monsterValue:\n").append(ex).toString());
            FileoutputUtil.log("Log_Script_Except.txt", new StringBuilder().append("Event name").append(this.em == null ? "null" : this.em.getName()).append(", Instance name : ").append(this.name).append(", method Name : monsterValue:\n").append(ex).toString());
        } catch (Exception ex) {
            ex.printStackTrace();
            FileoutputUtil.outputFileError("Log_Script_Except.txt", ex);
        }
    }

    public void monsterDamaged(MapleCharacter chr, MapleMonster mob, int damage) {
        if ((this.disposed) || (mob.getId() != 9700037)) {
            return;
        }
        try {
            this.em.getIv().invokeFunction("monsterDamaged", new Object[]{this, chr, Integer.valueOf(mob.getId()), Integer.valueOf(damage)});
        } catch (ScriptException ex) {
            System.out.println(new StringBuilder().append("Event name").append(this.em == null ? "null" : this.em.getName()).append(", Instance name : ").append(this.name).append(", method Name : monsterValue:\n").append(ex).toString());
            FileoutputUtil.log("Log_Script_Except.txt", new StringBuilder().append("Event name").append(this.em == null ? "null" : this.em.getName()).append(", Instance name : ").append(this.name).append(", method Name : monsterValue:\n").append(ex).toString());
        } catch (NoSuchMethodException ex) {
            System.out.println(new StringBuilder().append("Event name").append(this.em == null ? "null" : this.em.getName()).append(", Instance name : ").append(this.name).append(", method Name : monsterValue:\n").append(ex).toString());
            FileoutputUtil.log("Log_Script_Except.txt", new StringBuilder().append("Event name").append(this.em == null ? "null" : this.em.getName()).append(", Instance name : ").append(this.name).append(", method Name : monsterValue:\n").append(ex).toString());
        } catch (Exception ex) {
            ex.printStackTrace();
            FileoutputUtil.outputFileError("Log_Script_Except.txt", ex);
        }
    }

    public void addPVPScore(MapleCharacter chr, int score) {
        if (this.disposed) {
            return;
        }
        try {
            this.em.getIv().invokeFunction("addPVPScore", new Object[]{this, chr, Integer.valueOf(score)});
        } catch (ScriptException ex) {
            System.out.println(new StringBuilder().append("Event name").append(this.em == null ? "null" : this.em.getName()).append(", Instance name : ").append(this.name).append(", method Name : monsterValue:\n").append(ex).toString());
            FileoutputUtil.log("Log_Script_Except.txt", new StringBuilder().append("Event name").append(this.em == null ? "null" : this.em.getName()).append(", Instance name : ").append(this.name).append(", method Name : monsterValue:\n").append(ex).toString());
        } catch (NoSuchMethodException ex) {
            System.out.println(new StringBuilder().append("Event name").append(this.em == null ? "null" : this.em.getName()).append(", Instance name : ").append(this.name).append(", method Name : monsterValue:\n").append(ex).toString());
            FileoutputUtil.log("Log_Script_Except.txt", new StringBuilder().append("Event name").append(this.em == null ? "null" : this.em.getName()).append(", Instance name : ").append(this.name).append(", method Name : monsterValue:\n").append(ex).toString());
        } catch (Exception ex) {
            ex.printStackTrace();
            FileoutputUtil.outputFileError("Log_Script_Except.txt", ex);
        }
    }

    public int getKillCount(MapleCharacter chr) {
        if (this.disposed) {
            return 0;
        }
        Integer kc = (Integer) this.killCount.get(Integer.valueOf(chr.getId()));
        if (kc == null) {
            return 0;
        }
        return kc.intValue();
    }

    public void dispose_NoLock() {
        if ((this.disposed) || (this.em == null)) {
            return;
        }
        String emN = this.em.getName();
        try {
            this.disposed = true;
            for (MapleCharacter chr : this.chars) {
                chr.setEventInstance(null);
            }
            this.chars.clear();
            this.chars = null;
            if (this.mobs.size() >= 1) {
                for (MapleMonster mob : this.mobs) {
                    if (mob != null) {
                        mob.setEventInstance(null);
                    }
                }
            }
            this.mobs.clear();
            this.mobs = null;
            this.killCount.clear();
            this.killCount = null;
            this.dced.clear();
            this.dced = null;
            this.timeStarted = 0L;
            this.eventTime = 0L;
            this.props.clear();
            this.props = null;
            for (int i = 0; i < this.mapIds.size(); i++) {
                if (((Boolean) this.isInstanced.get(i)).booleanValue()) {
                    getMapFactory().removeInstanceMap(((Integer) this.mapIds.get(i)).intValue());
                }
            }
            this.mapIds.clear();
            this.mapIds = null;
            this.isInstanced.clear();
            this.isInstanced = null;
            this.em.disposeInstance(this.name);
        } catch (Exception e) {
            System.out.println(new StringBuilder().append("Caused by : ").append(emN).append(" instance name: ").append(this.name).append(" method: dispose:").toString());
            e.printStackTrace();
            FileoutputUtil.outputFileError("Log_Script_Except.txt", e);
        }
    }

    public void dispose() {
        this.wL.lock();
        try {
            dispose_NoLock();
        } finally {
            this.wL.unlock();
        }
    }

    public ChannelServer getChannelServer() {
        return ChannelServer.getInstance(this.channel);
    }

    public List<MapleMonster> getMobs() {
        return this.mobs;
    }

    public final void broadcastPlayerMsg(int type, String msg) {
        if (this.disposed) {
            return;
        }
        for (MapleCharacter chr : getPlayers()) {
            chr.dropMessage(type, msg);
        }
    }

    public final List<Pair<Integer, String>> newPair() {
        return new ArrayList();
    }

    public void addToPair(List<Pair<Integer, String>> e, int e1, String e2) {
        e.add(new Pair(Integer.valueOf(e1), e2));
    }

    public final List<Pair<Integer, MapleCharacter>> newPair_chr() {
        return new ArrayList();
    }

    public void addToPair_chr(List<Pair<Integer, MapleCharacter>> e, int e1, MapleCharacter e2) {
        e.add(new Pair(Integer.valueOf(e1), e2));
    }

    public final void broadcastPacket(byte[] p) {
        if (this.disposed) {
            return;
        }
        for (MapleCharacter chr : getPlayers()) {
            chr.getClient().getSession().write(p);
        }
    }

    public final void broadcastTeamPacket(byte[] p, int team) {
        if (this.disposed) {
            return;
        }
        for (MapleCharacter chr : getPlayers()) {
            if (chr.getTeam() == team) {
                chr.getClient().getSession().write(p);
            }
        }
    }

    public final MapleMap createInstanceMap(int mapid) {
        if (this.disposed) {
            return null;
        }
        int assignedid = EventScriptManager.getNewInstanceMapId();
        this.mapIds.add(Integer.valueOf(assignedid));
        this.isInstanced.add(Boolean.valueOf(true));
        return getMapFactory().CreateInstanceMap(mapid, true, true, true, assignedid);
    }

    public final MapleMap createInstanceMapS(int mapid) {
        if (this.disposed) {
            return null;
        }
        int assignedid = EventScriptManager.getNewInstanceMapId();
        this.mapIds.add(Integer.valueOf(assignedid));
        this.isInstanced.add(Boolean.valueOf(true));
        return getMapFactory().CreateInstanceMap(mapid, false, false, false, assignedid);
    }

    public final MapleMap setInstanceMap(int mapid) {
        if (this.disposed) {
            return getMapFactory().getMap(mapid);
        }
        this.mapIds.add(Integer.valueOf(mapid));
        this.isInstanced.add(Boolean.valueOf(false));
        return getMapFactory().getMap(mapid);
    }

    public final MapleMapFactory getMapFactory() {
        return getChannelServer().getMapFactory();
    }

    public final MapleMap getMapInstance(int args) {
        if (this.disposed) {
            return null;
        }
        try {
            boolean instanced = false;
            int trueMapID = -1;
            if (args >= this.mapIds.size()) {
                trueMapID = args;
            } else {
                trueMapID = ((Integer) this.mapIds.get(args)).intValue();
                instanced = ((Boolean) this.isInstanced.get(args)).booleanValue();
            }
            MapleMap map = null;
            if (!instanced) {
                map = getMapFactory().getMap(trueMapID);
                if (map == null) {
                    return null;
                }

                if ((map.getCharactersSize() == 0)
                        && (this.em.getProperty("shuffleReactors") != null) && (this.em.getProperty("shuffleReactors").equals("true"))) {
                    map.shuffleReactors();
                }
            } else {
                map = getMapFactory().getInstanceMap(trueMapID);
                if (map == null) {
                    return null;
                }

                if ((map.getCharactersSize() == 0)
                        && (this.em.getProperty("shuffleReactors") != null) && (this.em.getProperty("shuffleReactors").equals("true"))) {
                    map.shuffleReactors();
                }
            }

            return map;
        } catch (NullPointerException ex) {
            FileoutputUtil.outputFileError("Log_Script_Except.txt", ex);
            ex.printStackTrace();
        }
        return null;
    }

    public final void schedule(final String methodName, long delay) {
        if (this.disposed) {
            return;
        }
        Timer.EventTimer.getInstance().schedule(new Runnable() {
            public void run() {
                if ((EventInstanceManager.this.disposed) || (EventInstanceManager.this == null) || (EventInstanceManager.this.em == null)) {
                    return;
                }
                try {
                    EventInstanceManager.this.em.getIv().invokeFunction(methodName, new Object[]{EventInstanceManager.this});
                } catch (NullPointerException npe) {
                } catch (Exception ex) {
                    System.out.println("Event name" + EventInstanceManager.this.em.getName() + ", Instance name : " + EventInstanceManager.this.name + ", method Name : " + methodName + ":\n" + ex);
                    FileoutputUtil.log("Log_Script_Except.txt", "Event name" + EventInstanceManager.this.em.getName() + ", Instance name : " + EventInstanceManager.this.name + ", method Name(schedule) : " + methodName + " :\n" + ex);
                }
            }
        }, delay);
    }

    public final String getName() {
        return this.name;
    }

    public final void setProperty(String key, String value) {
        if (this.disposed) {
            return;
        }
        this.props.setProperty(key, value);
    }

    public final Object setProperty(String key, String value, boolean prev) {
        if (this.disposed) {
            return null;
        }
        return this.props.setProperty(key, value);
    }

    public final String getProperty(String key) {
        if (this.disposed) {
            return "";
        }
        return this.props.getProperty(key);
    }

    public final void setPropertyAswan(String key, String value) {
        this.props.setProperty(key, value);
    }

    public final Object setPropertyAswan(String key, String value, boolean prev) {
        return this.props.setProperty(key, value);
    }

    public final String getPropertyAswan(String key) {
        return this.props.getProperty(key);
    }

    public final Properties getProperties() {
        return this.props;
    }

    public final void leftParty(MapleCharacter chr) {
        if (this.disposed) {
            return;
        }
        try {
            this.em.getIv().invokeFunction("leftParty", new Object[]{this, chr});
        } catch (Exception ex) {
            System.out.println(new StringBuilder().append("Event name").append(this.em.getName()).append(", Instance name : ").append(this.name).append(", method Name : leftParty:\n").append(ex).toString());
            FileoutputUtil.log("Log_Script_Except.txt", new StringBuilder().append("Event name").append(this.em.getName()).append(", Instance name : ").append(this.name).append(", method Name : leftParty:\n").append(ex).toString());
        }
    }

    public final void disbandParty() {
        if (this.disposed) {
            return;
        }
        try {
            this.em.getIv().invokeFunction("disbandParty", new Object[]{this});
        } catch (Exception ex) {
            System.out.println(new StringBuilder().append("Event name").append(this.em.getName()).append(", Instance name : ").append(this.name).append(", method Name : disbandParty:\n").append(ex).toString());
            FileoutputUtil.log("Log_Script_Except.txt", new StringBuilder().append("Event name").append(this.em.getName()).append(", Instance name : ").append(this.name).append(", method Name : disbandParty:\n").append(ex).toString());
        }
    }

    public final void finishPQ() {
        if (this.disposed) {
            return;
        }
        try {
            this.em.getIv().invokeFunction("clearPQ", new Object[]{this});
        } catch (Exception ex) {
            System.out.println(new StringBuilder().append("Event name").append(this.em.getName()).append(", Instance name : ").append(this.name).append(", method Name : clearPQ:\n").append(ex).toString());
            FileoutputUtil.log("Log_Script_Except.txt", new StringBuilder().append("Event name").append(this.em.getName()).append(", Instance name : ").append(this.name).append(", method Name : clearPQ:\n").append(ex).toString());
        }
    }

    public final void removePlayer(MapleCharacter chr) {
        if (this.disposed) {
            return;
        }
        try {
            this.em.getIv().invokeFunction("playerExit", new Object[]{this, chr});
        } catch (Exception ex) {
            System.out.println(new StringBuilder().append("Event name").append(this.em.getName()).append(", Instance name : ").append(this.name).append(", method Name : playerExit:\n").append(ex).toString());
            FileoutputUtil.log("Log_Script_Except.txt", new StringBuilder().append("Event name").append(this.em.getName()).append(", Instance name : ").append(this.name).append(", method Name : playerExit:\n").append(ex).toString());
        }
    }

    public final void registerCarnivalParty(MapleCharacter leader, MapleMap map, byte team) {
        if (this.disposed) {
            return;
        }
        leader.clearCarnivalRequests();
        List characters = new LinkedList();
        MapleParty party = leader.getParty();

        if (party == null) {
            return;
        }
        for (MaplePartyCharacter pc : party.getMembers()) {
            MapleCharacter c = map.getCharacterById(pc.getId());
            if (c != null) {
                characters.add(c);
                registerPlayer(c);
                c.resetCP();
            }
        }
        PartySearch ps = World.Party.getSearch(party);
        if (ps != null) {
            World.Party.removeSearch(ps, "The Party Listing has been removed because the Party Quest started.");
        }
        MapleCarnivalParty carnivalParty = new MapleCarnivalParty(leader, characters, team);
        try {
            this.em.getIv().invokeFunction("registerCarnivalParty", new Object[]{this, carnivalParty});
        } catch (ScriptException ex) {
            System.out.println(new StringBuilder().append("Event name").append(this.em.getName()).append(", Instance name : ").append(this.name).append(", method Name : registerCarnivalParty:\n").append(ex).toString());
            FileoutputUtil.log("Log_Script_Except.txt", new StringBuilder().append("Event name").append(this.em.getName()).append(", Instance name : ").append(this.name).append(", method Name : registerCarnivalParty:\n").append(ex).toString());
        } catch (NoSuchMethodException ex) {
        }
    }

    public void onMapLoad(MapleCharacter chr) {
        if (this.disposed) {
            return;
        }
        try {
            this.em.getIv().invokeFunction("onMapLoad", new Object[]{this, chr});
        } catch (ScriptException ex) {
            System.out.println(new StringBuilder().append("Event name").append(this.em.getName()).append(", Instance name : ").append(this.name).append(", method Name : onMapLoad:\n").append(ex).toString());
            FileoutputUtil.log("Log_Script_Except.txt", new StringBuilder().append("Event name").append(this.em.getName()).append(", Instance name : ").append(this.name).append(", method Name : onMapLoad:\n").append(ex).toString());
        } catch (NoSuchMethodException ex) {
        }
    }

    public boolean isLeader(MapleCharacter chr) {
        return (chr != null) && (chr.getParty() != null) && (chr.getParty().getLeader().getId() == chr.getId());
    }

    public void registerSquad(MapleSquad squad, MapleMap map, int questID) {
        if (this.disposed) {
            return;
        }
        int mapid = map.getId();

        for (String chr : squad.getMembers()) {
            MapleCharacter player = squad.getChar(chr);
            if ((player != null) && (player.getMapId() == mapid)) {
                if (questID > 0) {
                    player.getQuestNAdd(MapleQuest.getInstance(questID)).setCustomData(String.valueOf(System.currentTimeMillis()));
                }
                registerPlayer(player);
                if (player.getParty() != null) {
                    PartySearch ps = World.Party.getSearch(player.getParty());
                    if (ps != null) {
                        World.Party.removeSearch(ps, "The Party Listing has been removed because the Party Quest has started.");
                    }
                }
            }
        }
        squad.setStatus((byte) 2);
        squad.getBeginMap().broadcastMessage(CField.stopClock());
    }

    public boolean isDisconnected(MapleCharacter chr) {
        if (this.disposed) {
            return false;
        }
        return this.dced.contains(Integer.valueOf(chr.getId()));
    }

    public void removeDisconnected(int id) {
        if (this.disposed) {
            return;
        }
        this.dced.remove(id);
    }

    public EventManager getEventManager() {
        return this.em;
    }

    public void applyBuff(MapleCharacter chr, int id) {
        MapleItemInformationProvider.getInstance().getItemEffect(id).applyTo(chr);
        chr.getClient().getSession().write(CWvsContext.InfoPacket.getStatusMsg(id));
    }

    public void applySkill(MapleCharacter chr, int id) {
        SkillFactory.getSkill(id).getEffect(1).applyTo(chr);
    }
}