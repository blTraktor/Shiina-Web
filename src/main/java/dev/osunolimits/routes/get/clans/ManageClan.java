package dev.osunolimits.routes.get.clans;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import dev.osunolimits.main.App;
import dev.osunolimits.models.UserInfoObject;
import dev.osunolimits.modules.Shiina;
import dev.osunolimits.modules.ShiinaRoute;
import dev.osunolimits.modules.ShiinaRoute.ShiinaRequest;
import lombok.Data;
import spark.Request;
import spark.Response;

public class ManageClan extends Shiina {

    private final String clanPermQuery = "SELECT `clan_priv`, `clan_id` FROM `users` WHERE `id` = ?";
    private final String clanQuery = "SELECT * FROM `clans` WHERE `id` = ?";
    private final String clanMemberQuery = "SELECT `id`, `name` FROM `users` WHERE `clan_id` = ? AND `clan_priv` != 3;";

    private final String clanPendingQuery = "SELECT * FROM `sh_clan_pending` WHERE `clanid` = ?";
    private final String clanDeniedQuery = "SELECT * FROM `sh_clan_denied` WHERE `clanid` = ?";


    @Override
    public Object handle(Request req, Response res) throws Exception {
        ShiinaRequest shiina = new ShiinaRoute().handle(req, res);
        String clanId = req.params("id");

        if(clanId == null) {
            return notFound(res, shiina);
        }

        if(!shiina.loggedIn) {
            res.redirect("/login");
            return notFound(res, shiina);
        }

        ResultSet clanPermRS = shiina.mysql.Query(clanPermQuery, shiina.user.id);
        if(!clanPermRS.next()) {
            return notFound(res, shiina);
        }

        if(clanPermRS.getInt("clan_priv") < 2) {
            return notFound(res, shiina);
        }

        if(clanPermRS.getInt("clan_id") != Integer.parseInt(clanId)) {
            return notFound(res, shiina);
        }

        ResultSet clanRS = shiina.mysql.Query(clanQuery, clanId);
        if(!clanRS.next()) {
            return notFound(res, shiina);
        }

        Clan clan = new Clan();
        clan.id = clanRS.getInt("id");
        clan.name = clanRS.getString("name");
        clan.tag = clanRS.getString("tag");
        clan.owner = clanRS.getInt("owner");
        clan.createdAt = clanRS.getString("created_at");

        UserInfoObject userInfo = new Gson().fromJson(App.appCache.get("shiina:user:" + clan.owner), UserInfoObject.class);
        clan.ownerName = userInfo.name;
        shiina.data.put("clan", clan);

        List<UserInfoObject> clanMembers = new ArrayList<>(); 
        ResultSet clanMemberRS = shiina.mysql.Query(clanMemberQuery, clanId);
        while(clanMemberRS.next()) {
            UserInfoObject member = new UserInfoObject();
            member.id = clanMemberRS.getInt("id");
            member.name = clanMemberRS.getString("name");
            clanMembers.add(member);
        }

        shiina.data.put("members", clanMembers);

        List<UserInfoObject> pendingMembers = new ArrayList<>();
        ResultSet clanPendingRS = shiina.mysql.Query(clanPendingQuery, clanId);
        Gson gson = new Gson();
        while(clanPendingRS.next()) {
            UserInfoObject pending = gson.fromJson(App.appCache.get("shiina:user:" + clanPendingRS.getInt("userid")), UserInfoObject.class);
            pendingMembers.add(pending);
        }

        shiina.data.put("pending", pendingMembers);

        List<UserInfoObject> deniedMembers = new ArrayList<>();
        ResultSet clanDeniedRS = shiina.mysql.Query(clanDeniedQuery, clanId);
        while(clanDeniedRS.next()) {
            UserInfoObject denied = gson.fromJson(App.appCache.get("shiina:user:" + clanDeniedRS.getInt("userid")), UserInfoObject.class);
            deniedMembers.add(denied);
        }

        shiina.data.put("denied", deniedMembers);
            
        shiina.data.put("actNav", 4);
        return renderTemplate("clans/manage.html", shiina, res, req);
    }

    @Data
    public class Clan {
        private int id;
        private String name;
        private String tag;
        private int owner;
        private String ownerName;
        @SerializedName("created_at")
        private String createdAt;
    }
}
