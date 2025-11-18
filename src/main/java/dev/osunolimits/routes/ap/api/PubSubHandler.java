package dev.osunolimits.routes.ap.api;

import java.io.File;
import java.sql.ResultSet;

import com.google.gson.Gson;

import dev.osunolimits.main.App;
import dev.osunolimits.models.UserInfoObject;
import dev.osunolimits.modules.Shiina;
import dev.osunolimits.modules.ShiinaRoute;
import dev.osunolimits.modules.ShiinaRoute.ShiinaRequest;
import dev.osunolimits.modules.utils.AuditLogger;
import dev.osunolimits.plugins.events.admin.OnAddDonorEvent;
import dev.osunolimits.routes.ap.api.PubSubModels.AddPrivInput;
import dev.osunolimits.routes.ap.api.PubSubModels.AlertAllInput;
import dev.osunolimits.routes.ap.api.PubSubModels.GiveDonatorInput;
import dev.osunolimits.routes.ap.api.PubSubModels.RemovePrivInput;
import dev.osunolimits.routes.ap.api.PubSubModels.RestrictInput;
import dev.osunolimits.routes.ap.api.PubSubModels.UnrestrictInput;
import dev.osunolimits.routes.ap.api.PubSubModels.WipeInput;
import dev.osunolimits.utils.osu.PermissionHelper;
import spark.Request;
import spark.Response;

public class PubSubHandler extends Shiina {
    private final Gson GSON;

    public PubSubHandler() {
        this.GSON = new Gson();
    }

    public enum MessageType {
        RANK,
        RESTRICT,
        UNRESTRICT,
        WIPE,
        ALERT_ALL,
        GIVEDONATOR,
        ADDPRIV,
        REMOVEPRIV,
        NONE,
        RMPB,
        RMUP
    }

    public Object handle(Request req, Response res) throws Exception {
        ShiinaRequest shiina = new ShiinaRoute().handle(req, res);

        if (shiina.user == null || !shiina.loggedIn) {
            return redirect(res, shiina, "/login");
        }

        if (!PermissionHelper.hasPrivileges(shiina.user.priv, PermissionHelper.Privileges.STAFF)) {
            return redirect(res, shiina, "/");
        }

        MessageType type = MessageType.NONE;
        if (req.queryParams("type") != null) {
            try {
                type = MessageType.valueOf(req.queryParams("type").toUpperCase());
            } catch (Exception e) {
                type = MessageType.NONE;
            }
        }

        if (type == MessageType.NONE) {
            shiina.mysql.close();
            return "invalid type";
        }

        PubSubModels models = new PubSubModels();
        AuditLogger auditLogger = new AuditLogger(shiina.mysql, type);
        
        switch (type) {
            case RESTRICT: {
                if (!PermissionHelper.hasPrivileges(shiina.user.priv, PermissionHelper.Privileges.MODERATOR)) {
                    return redirect(res, shiina, "/");
                }
                RestrictInput restrictInput = models.new RestrictInput();
                restrictInput.setId(Integer.parseInt(req.queryParams("id")));
                restrictInput.setUserId(shiina.user.id);
                restrictInput.setReason(req.queryParams("reason"));
                auditLogger.restrictUser(shiina.user, restrictInput.getId(), restrictInput.getReason());
                App.jedisPool.publish("restrict", GSON.toJson(restrictInput));
                break;
            }
            case UNRESTRICT: {
                if (!PermissionHelper.hasPrivileges(shiina.user.priv, PermissionHelper.Privileges.MODERATOR)) {
                    return redirect(res, shiina, "/");
                }
                UnrestrictInput unrestrictInput = models.new UnrestrictInput();
                unrestrictInput.setId(Integer.parseInt(req.queryParams("id")));
                unrestrictInput.setUserId(shiina.user.id);
                unrestrictInput.setReason(req.queryParams("reason"));
                auditLogger.unrestrictUser(shiina.user, unrestrictInput.getId(), unrestrictInput.getReason());

                App.jedisPool.publish("unrestrict", GSON.toJson(unrestrictInput));
                break;
            }
            case WIPE:
            if (!PermissionHelper.hasPrivileges(shiina.user.priv, PermissionHelper.Privileges.MODERATOR)) {
                return redirect(res, shiina, "/");
            }
                WipeInput wipeInput = models.new WipeInput();
                wipeInput.setId(Integer.parseInt(req.queryParams("id")));
                wipeInput.setMode(Integer.parseInt(req.queryParams("mode")));
                String reason = req.queryParams("reason");
                auditLogger.wipeUser(shiina.user, wipeInput.getId(), wipeInput.getMode(), reason);

                App.jedisPool.publish("wipe", GSON.toJson(wipeInput));
                break;
            case ALERT_ALL: {
                if (!PermissionHelper.hasPrivileges(shiina.user.priv, PermissionHelper.Privileges.MODERATOR)) {
                    return redirect(res, shiina, "/");
                }
                AlertAllInput alertAllInput = models.new AlertAllInput();
                alertAllInput.setMessage(req.queryParams("message"));
                auditLogger.alertAll(shiina.user, alertAllInput.getMessage());

                App.jedisPool.publish("alert_all", GSON.toJson(alertAllInput));
                break;
            }
            case GIVEDONATOR: {
                if (!PermissionHelper.hasPrivileges(shiina.user.priv, PermissionHelper.Privileges.ADMINISTRATOR)) {
                    return redirect(res, shiina, "/");
                }
                GiveDonatorInput giveDonatorInput = models.new GiveDonatorInput();
                giveDonatorInput.setId(Integer.parseInt(req.queryParams("id")));
                giveDonatorInput.setDuration(req.queryParams("duration"));
                auditLogger.giveDonator(shiina.user, giveDonatorInput.getId(), giveDonatorInput.getDuration());
                App.jedisPool.publish("givedonator", GSON.toJson(giveDonatorInput));
                Thread.sleep(500);
                UserInfoObject obj = GSON.fromJson(App.appCache.get("shiina:user:" + giveDonatorInput.getId()), UserInfoObject.class); 
                ResultSet privRs = shiina.mysql.Query("SELECT `priv` FROM `users` WHERE `id` = ?", giveDonatorInput.getId());
                obj.priv = privRs.next() ? privRs.getInt("priv") : 0;
                String userJson = GSON.toJson(obj);
                App.appCache.del("shiina:user:" + giveDonatorInput.getId());
                App.appCache.set("shiina:user:" + giveDonatorInput.getId(), userJson);

                new OnAddDonorEvent(giveDonatorInput.getDuration(), giveDonatorInput.getId(), shiina.user.id).callListeners();
                
                
                break;
            }
            case ADDPRIV: {
                if (!PermissionHelper.hasPrivileges(shiina.user.priv, PermissionHelper.Privileges.ADMINISTRATOR)) {
                    return redirect(res, shiina, "/");
                }
                AddPrivInput addPrivInput = models.new AddPrivInput();
                addPrivInput.setId(Integer.parseInt(req.queryParams("id")));
                addPrivInput.setPrivs(req.queryParamsValues("privs"));
                auditLogger.addPriv(shiina.user, addPrivInput.getId(), addPrivInput.getPrivs());

                App.jedisPool.publish("addpriv", GSON.toJson(addPrivInput));

                Thread.sleep(500);

                UserInfoObject obj = GSON.fromJson(App.appCache.get("shiina:user:" + addPrivInput.getId()), UserInfoObject.class);
                ResultSet privRs = shiina.mysql.Query("SELECT `priv` FROM `users` WHERE `id` = ?", addPrivInput.getId());
                obj.priv = privRs.next() ? privRs.getInt("priv") : 0;
                String userJson = GSON.toJson(obj);
                App.appCache.del("shiina:user:" + addPrivInput.getId());
                App.appCache.set("shiina:user:" + addPrivInput.getId(), userJson);
                res.redirect("/ap/user?id=" + addPrivInput.getId());
                break;
            }
            case REMOVEPRIV: {
                if (!PermissionHelper.hasPrivileges(shiina.user.priv, PermissionHelper.Privileges.ADMINISTRATOR)) {
                    return redirect(res, shiina, "/");
                }
                RemovePrivInput removePrivInput = models.new RemovePrivInput();
                removePrivInput.setId(Integer.parseInt(req.queryParams("id")));
                removePrivInput.setPrivs(req.queryParamsValues("privs"));

                App.jedisPool.publish("removepriv", GSON.toJson(removePrivInput));

                Thread.sleep(500);
                UserInfoObject obj = GSON.fromJson(App.appCache.get("shiina:user:" + removePrivInput.getId()), UserInfoObject.class);
                ResultSet privRs = shiina.mysql.Query("SELECT `priv` FROM `users` WHERE `id` = ?", removePrivInput.getId());
                obj.priv = privRs.next() ? privRs.getInt("priv") : 0;
                String userJson = GSON.toJson(obj);
                App.appCache.del("shiina:user:" + removePrivInput.getId());
                App.appCache.set("shiina:user:" + removePrivInput.getId(), userJson);

                auditLogger.removePriv(shiina.user, removePrivInput.getId(), removePrivInput.getPrivs());
                res.redirect("/ap/user?id=" + removePrivInput.getId());
              
                break;
            }

            case RMUP: {
                if (!PermissionHelper.hasPrivileges(shiina.user.priv, PermissionHelper.Privileges.MODERATOR)) {
                    return redirect(res, shiina, "/");
                }
                int userId = Integer.parseInt(req.queryParams("id"));
                ResultSet userpageRs = shiina.mysql.Query("SELECT * FROM `userpages` WHERE `user_id` = ?", userId);
                if (userpageRs.next()) {
                    shiina.mysql.Exec("DELETE FROM `userpages` WHERE `user_id` = ?", userId);
                }
                auditLogger.removeUserpage(shiina.user, userId, req.queryParams("reason"));
                res.redirect("/ap/user?id=" + userId);
                break;
            }

            case RMPB: {
                if (!PermissionHelper.hasPrivileges(shiina.user.priv, PermissionHelper.Privileges.MODERATOR)) {
                    return redirect(res, shiina, "/");
                }
                int userId = Integer.parseInt(req.queryParams("id"));
                String reasonPb = req.queryParams("reason");
                File avatarDir = new File(App.env.get("AVATARFOLDER"));
                File avatar = new File(avatarDir, userId + ".png");
                if (avatar.exists()) {
                    avatar.delete();
                }
                File avatarJpg = new File(avatarDir, userId + ".jpg");
                if (avatarJpg.exists()) {
                    avatarJpg.delete();
                }
                auditLogger.removeProfilePicture(shiina.user, userId, reasonPb);
                break;
            }

            default:
                break;
        }

        shiina.mysql.close();
        res.status(200);
        return "success";
    }
}
