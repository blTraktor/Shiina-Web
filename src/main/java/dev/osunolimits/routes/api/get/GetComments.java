package dev.osunolimits.routes.api.get;

import java.sql.ResultSet;
import java.util.ArrayList;

import dev.osunolimits.main.App;
import dev.osunolimits.models.UserInfoObject;
import dev.osunolimits.modules.ShiinaRoute.ShiinaRequest;
import dev.osunolimits.modules.utils.MySQLRoute;
import dev.osunolimits.modules.utils.ShiinaSupporterBadge;
import dev.osunolimits.utils.Validation;
import dev.osunolimits.utils.osu.PermissionHelper;
import lombok.Data;
import spark.Request;
import spark.Response;

public class GetComments extends MySQLRoute{

    private final String GET_COMMENTS_SQL = "SELECT * FROM `comments` WHERE `target_id` = ? AND `target_type` = ? ORDER BY `comments`.`id` DESC LIMIT ? OFFSET ?";

    @Override
    public Object handle(Request req, Response res) throws Exception {
        ShiinaRequest shiina = getRequest();
        ShiinaAPIHandler shiinaAPIHandler = new ShiinaAPIHandler();

        Integer id = null;
        if (req.queryParams("id") != null && Validation.isNumeric(req.queryParams("id"))) {
            id = Integer.parseInt(req.queryParams("id"));
        }else {
            shiinaAPIHandler.addRequiredParameter("id", "int", "missing or invalid");
        }

        String target = null;
        if (req.queryParams("target") != null) {
            target = req.queryParams("target");
        }else {
            shiinaAPIHandler.addRequiredParameter("target", "string", "missing or invalid");
        }

        Integer offset = 0;
        if (req.queryParams("offset") != null && Validation.isNumeric(req.queryParams("offset"))) {
            offset = Integer.parseInt(req.queryParams("offset"));
        }else {
            shiinaAPIHandler.addRequiredParameter("offset", "int", "missing or invalid");
        }

        boolean hasNextPage = false;

        if (shiinaAPIHandler.hasIssues()) {
            return shiinaAPIHandler.renderIssues(shiina, res);
        }

        ArrayList<Comment> comments = new ArrayList<>();
        ResultSet getCommentsResultSet = shiina.mysql.Query(GET_COMMENTS_SQL, id, target, 6, offset);
        while(getCommentsResultSet.next()) {
            Comment comment = new Comment();
            comment.setTime(getCommentsResultSet.getInt("time"));
            comment.setComment(getCommentsResultSet.getString("comment"));
            UserInfoObject user = shiinaAPIHandler.getGson().fromJson(App.appCache.get("shiina:user:" + getCommentsResultSet.getInt("userid")), UserInfoObject.class); 
            if(PermissionHelper.hasPrivileges(user.priv, PermissionHelper.Privileges.SUPPORTER)) {
                user.groups.add(ShiinaSupporterBadge.getInstance().getGroup());
                comment.setSupporter(true);
            }
            comment.setUser(user);
            comments.add(comment);
        }

        if(comments.size() == 6) {
            hasNextPage = true;
            comments.remove(5);
        }

        return shiinaAPIHandler.renderJSON(new CommentResponse(comments, hasNextPage), shiina, res);
    }   

    @Data 
    public class CommentResponse {
        private final ArrayList<Comment> comments;
        private final boolean hasNextPage;
    }
    
    @Data
    public class Comment {
       private int time;
       private String comment;
       private UserInfoObject user;
       private boolean supporter = false;
    }
}
