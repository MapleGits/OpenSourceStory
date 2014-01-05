package handling.login.handler;

import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleClient;
import client.SkillEntry;
import client.SkillFactory;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import constants.GameConstants;
import constants.ServerConstants;
import handling.channel.ChannelServer;
import handling.login.LoginInformationProvider;

import handling.login.LoginInformationProvider.JobType;
import handling.login.LoginServer;
import handling.login.LoginWorker;
import handling.world.World;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import server.MapleItemInformationProvider;
import server.quest.MapleQuest;
import tools.data.LittleEndianAccessor;
import tools.packet.CField;
import tools.packet.LoginPacket;
import tools.packet.PacketHelper;

public class CharLoginHandler {

    private static final boolean loginFailCount(MapleClient c) {
        c.loginAttempt = ((short) (c.loginAttempt + 1));
        if (c.loginAttempt > 5) {
            return true;
        }
        return false;
    }
    public static final void login(final LittleEndianAccessor slea, final MapleClient c) {
        String pwd = slea.readMapleAsciiString();
        String login = slea.readMapleAsciiString().replace("NP12:auth06:5:0:","");

        final boolean ipBan = c.hasBannedIP();
        final boolean macBan = c.hasBannedMac();

        int loginok = c.login(login, pwd, ipBan || macBan);
        final Calendar tempbannedTill = c.getTempBanCalendar();

        if (loginok == 0 && (ipBan || macBan) && !c.isGm()) {
            loginok = 3;
            if (macBan) {
                // this is only an ipban o.O" - maybe we should refactor this a bit so it's more readable
                MapleCharacter.ban(c.getSession().getRemoteAddress().toString().split(":")[0], "Enforcing account ban, account " + login, false, 4, false);
            }
        }
        if (loginok != 0) {
            if (!loginFailCount(c)) {
                c.clearInformation();
                c.getSession().write(LoginPacket.getLoginFailed(loginok));
            } else {
                c.getSession().close();
            }
        } else if (tempbannedTill.getTimeInMillis() != 0) {
            if (!loginFailCount(c)) {
                c.clearInformation();
                c.getSession().write(LoginPacket.getTempBan(PacketHelper.getTime(tempbannedTill.getTimeInMillis()), c.getBanReason()));
            } else {
                c.getSession().close();
            }
        } else {
            c.loginAttempt = 0;
            LoginWorker.registerClient(c);
        }
    }

    public static void ServerListRequest(MapleClient c) {
        c.getSession().write(LoginPacket.getServerList(0, LoginServer.getLoad()));

        c.getSession().write(LoginPacket.getEndOfServerList());
    }

   public static final void ServerStatusRequest(MapleClient c) {
        int numPlayer = LoginServer.getUsersOn();
        int userLimit = LoginServer.getUserLimit();
        if (numPlayer >= userLimit) {
            c.getSession().write(LoginPacket.getServerStatus(2));
        } else if (numPlayer * 2 >= userLimit) {
            c.getSession().write(LoginPacket.getServerStatus(1));
        } else {
            c.getSession().write(LoginPacket.getServerStatus(0));
        }
    }

    public static final void CharlistRequest(LittleEndianAccessor slea, MapleClient c) {
        if (!c.isLoggedIn()) {
            c.getSession().close(true);
            return;
        }
        if (GameConstants.GMS) {
            slea.readByte();
        }
        int server = slea.readByte();
        int channel = slea.readByte() + 1;
        if ((!World.isChannelAvailable(channel)) || (server != 0)) {
            c.getSession().write(LoginPacket.getLoginFailed(10));
            return;
        }

        List chars = c.loadCharacters(server);
        if ((chars != null) && (ChannelServer.getInstance(channel) != null)) {
            c.setWorld(server);
            c.setChannel(channel);

            c.getSession().write(LoginPacket.getCharList(c.getSecondPassword(), chars, c.getCharacterSlots()));
        } else {
            c.getSession().close(true);
        }
    }

    public static final void updateCCards(LittleEndianAccessor slea, MapleClient c) {
        if ((slea.available() != 36) || (!c.isLoggedIn())) {
            c.getSession().close(true);
            return;
        }
        Map<Integer, Integer> cids = new LinkedHashMap();
        for (int i = 1; i <= 9; i++) {
            int charId = slea.readInt();
            if (((!c.login_Auth(charId)) && (charId != 0)) || (ChannelServer.getInstance(c.getChannel()) == null) || (c.getWorld() != 0)) {
                c.getSession().close(true);
                return;
            }
            cids.put(Integer.valueOf(i), Integer.valueOf(charId));
        }
        c.updateCharacterCards(cids);
    }

    public static final void CheckCharName(String name, MapleClient c) {
        c.getSession().write(LoginPacket.charNameResponse(name, (!MapleCharacterUtil.canCreateChar(name, c.isGm())) || ((LoginInformationProvider.getInstance().isForbiddenName(name)) && (!c.isGm()))));
    }

    public static void CreateChar(LittleEndianAccessor slea, MapleClient c) {
        String name = slea.readMapleAsciiString();
        slea.skip(4);
        final JobType jobType = JobType.getByType(slea.readInt()); // BIGBANG: 0 = Resistance, 1 = Adventurer, 2 = Cygnus, 3 = Aran, 4 = Evan, 5 = Mercedes
        final short subCategory = slea.readShort(); //whether dual blade = 1 or adventurer = 0
        final byte gender = slea.readByte();
        byte skinColor = slea.readByte(); // 01
        int hairColor = 0;
        slea.skip(1);
        boolean mercedes = (jobType == JobType.Mercedes);
        boolean demon = (jobType == JobType.Demon);
        boolean adventurer = (jobType == JobType.Adventurer);
        int face = slea.readInt();
        int hair = slea.readInt();
        
        final int demonMark = demon ? slea.readInt() : 0;
        int top = slea.readInt();
        int bottom = (mercedes || demon || adventurer) ? 0 : slea.readInt();
        int shoes = slea.readInt();
        int weapon = slea.readInt();
        int shield = demon ? slea.readInt() : (mercedes ? 1352000 : 0);

        MapleCharacter newchar = MapleCharacter.getDefault(c);
        newchar.setWorld((byte) c.getWorld());
        newchar.setFace(face);
        newchar.setHair(hair + hairColor);
        newchar.setGender(gender);
        newchar.setName(name);
        newchar.setSkinColor(skinColor);
        newchar.setDemonMarking(demonMark);
        newchar.setSubcategory(subCategory);
        
        MapleInventory equip = newchar.getInventory(MapleInventoryType.EQUIPPED);        
        Item eq_top = MapleItemInformationProvider.getInstance().getEquipById(top);
        eq_top.setPosition((byte) -5);
        equip.addFromDB(eq_top);
        Item eq_bottom = MapleItemInformationProvider.getInstance().getEquipById(bottom);
        eq_bottom.setPosition((byte) -6);
        Item eq_shoes = MapleItemInformationProvider.getInstance().getEquipById(shoes);
        eq_shoes.setPosition((byte) -7);
        equip.addFromDB(eq_shoes);
        Item eq_weapon = MapleItemInformationProvider.getInstance().getEquipById(weapon);
        eq_weapon.setPosition((byte) -11);
        equip.addFromDB(eq_weapon);


        if (MapleCharacterUtil.canCreateChar(name, false) && !LoginInformationProvider.getInstance().isForbiddenName(name)) {
            MapleCharacter.saveNewCharToDB(newchar, (short) 0);
            c.getSession().write(LoginPacket.addNewCharEntry(newchar, true));
            c.createdChar(newchar.getId());
        } else {
            c.getSession().write(LoginPacket.addNewCharEntry(newchar, false));
        }
        newchar = null;
    }

    public static final void DeleteChar(LittleEndianAccessor slea, MapleClient c) {
        String Secondpw_Client = GameConstants.GMS ? slea.readMapleAsciiString() : null;
        if (Secondpw_Client == null) {
            if (slea.readByte() > 0) {
                Secondpw_Client = slea.readMapleAsciiString();
            }
            slea.readMapleAsciiString();
        }

        int Character_ID = slea.readInt();

        if ((!c.login_Auth(Character_ID)) || (!c.isLoggedIn()) || (loginFailCount(c))) {
            c.getSession().close(true);
            return;
        }
        byte state = 0;

        if (c.getSecondPassword() != null) {
            if (Secondpw_Client == null) {
                c.getSession().close(true);
                return;
            }
            if (!c.CheckSecondPassword(Secondpw_Client)) {
                state = 20;
            }

        }

        if (state == 0) {
            state = (byte) c.deleteCharacter(Character_ID);
        }
        c.getSession().write(LoginPacket.deleteCharResponse(Character_ID, state));
    }

    public static final void Character_WithoutSecondPassword(LittleEndianAccessor slea, MapleClient c, boolean haspic, boolean view) {
        slea.readByte();
        slea.readByte();
        int charId = slea.readInt();
        if (view) {
            c.setChannel(1);
            c.setWorld(slea.readInt());
        }
        String currentpw = c.getSecondPassword();
        if ((!c.isLoggedIn()) || (loginFailCount(c)) || ((currentpw != null) && ((!currentpw.equals("")) || (haspic))) || (!c.login_Auth(charId)) || (ChannelServer.getInstance(c.getChannel()) == null) || (c.getWorld() != 0)) {
            c.getSession().close(true);
            return;
        }
        slea.readMapleAsciiString();
        c.updateMacs(slea.readMapleAsciiString());
        if (slea.available() != 0L) {
            String setpassword = slea.readMapleAsciiString();

            if ((setpassword.length() >= 6) && (setpassword.length() <= 16)) {
                c.setSecondPassword(setpassword);
                c.updateSecondPassword();
            } else {
                c.getSession().write(LoginPacket.secondPwError((byte) 20));
                return;
            }
        } else if ((GameConstants.GMS) && (haspic)) {
            return;
        }
        if (c.getIdleTask() != null) {
            c.getIdleTask().cancel(true);
        }
        String s = c.getSessionIPAddress();
        LoginServer.putLoginAuth(charId, s.substring(s.indexOf('/') + 1, s.length()), c.getTempIP());
        c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION, s);
        c.getSession().write(CField.getServerIP(c, Integer.parseInt(ChannelServer.getInstance(c.getChannel()).getIP().split(":")[1]), charId));
    }

    public static final void Character_WithSecondPassword(LittleEndianAccessor slea, MapleClient c, boolean view) {
        String password = slea.readMapleAsciiString();
        int charId = slea.readInt();
        if (view) {
            c.setChannel(1);
            c.setWorld(slea.readInt());
        }
        if ((!c.isLoggedIn()) || (loginFailCount(c)) || (c.getSecondPassword() == null) || (!c.login_Auth(charId)) || (ChannelServer.getInstance(c.getChannel()) == null) || (c.getWorld() != 0)) {
            c.getSession().close(true);
            return;
        }
        if (GameConstants.GMS) {
            c.updateMacs(slea.readMapleAsciiString());
        }
        if ((c.CheckSecondPassword(password)) && (password.length() >= 6) && (password.length() <= 16)) {
            if (c.getIdleTask() != null) {
                c.getIdleTask().cancel(true);
            }
            String s = c.getSessionIPAddress();
            LoginServer.putLoginAuth(charId, s.substring(s.indexOf('/') + 1, s.length()), c.getTempIP());
            c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION, s);
            c.getSession().write(CField.getServerIP(c, Integer.parseInt(ChannelServer.getInstance(c.getChannel()).getIP().split(":")[1]), charId));
            System.out.println("Sent serverIp");
        } else {
            c.getSession().write(LoginPacket.secondPwError((byte) 0x14));
        }
    }

    public static void ViewChar(LittleEndianAccessor slea, MapleClient c) {
        Map<Byte, ArrayList<MapleCharacter>> worlds = new HashMap<Byte, ArrayList<MapleCharacter>>();
        List<MapleCharacter> chars = c.loadCharacters(0);
        c.getSession().write(LoginPacket.showAllCharacter(chars.size()));
        for (MapleCharacter chr : chars) {
            if (chr != null) {
                ArrayList<MapleCharacter> chrr;
                if (!worlds.containsKey(Byte.valueOf(chr.getWorld()))) {
                    chrr = new ArrayList<MapleCharacter>();
                    worlds.put(Byte.valueOf(chr.getWorld()), chrr);
                } else {
                    chrr = (ArrayList) worlds.get(Byte.valueOf(chr.getWorld()));
                }
                chrr.add(chr);
            }
        }
        for (Entry<Byte, ArrayList<MapleCharacter>> w : worlds.entrySet()) {
            c.getSession().write(LoginPacket.showAllCharacterInfo(((Byte) w.getKey()).byteValue(), (List) w.getValue(), c.getSecondPassword()));
        }
    }

public static final void login(String username, MapleClient c, String pwd) {
        String login = username;
        int loginok = 0;
        boolean isBanned = c.hasBannedIP() || c.hasBannedMac() || c.hasProxyBan();
        loginok = c.login(login, pwd, isBanned);
        Calendar tempbannedTill = c.getTempBanCalendar();


        if ((loginok == 0) && (isBanned)) {
            loginok = 3;
        }
        if (loginok != 0) {
            if (!loginFailCount(c)) {
                c.clearInformation();
                c.getSession().write(LoginPacket.getLoginFailed(loginok));
            } else {
                c.getSession().close(true);
            }
        } else if (tempbannedTill.getTimeInMillis() != 0L) {
            if (!loginFailCount(c)) {
                c.clearInformation();
                c.getSession().write(LoginPacket.getTempBan(PacketHelper.getTime(tempbannedTill.getTimeInMillis()), c.getBanReason()));
            } else {
                c.getSession().close(true);
            }
        } else {
            c.loginAttempt = 0;
            LoginWorker.registerClient(c);
        }
    } 
}