package dev.osunolimits.models;

import java.util.List;

import com.google.gson.Gson;

import dev.osunolimits.main.App;
import lombok.Data;

@Data
public class UserInfoObject {
    private final static Gson gson = new Gson();
    public UserInfoObject() {
    }   
    public UserInfoObject(int id) {
        UserInfoObject userInfo = gson.fromJson(App.appCache.get("shiina:user:" + id),
                        UserInfoObject.class);
        this.id = userInfo.id;
        this.name = userInfo.name;
        this.safe_name = userInfo.safe_name;
        this.priv = userInfo.priv;
        this.groups = userInfo.groups;
    }

    public int id;
    public String name;
    public String safe_name;
    public int priv;
    public List<Group> groups;
}
