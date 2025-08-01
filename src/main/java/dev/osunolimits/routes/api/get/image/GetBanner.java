package dev.osunolimits.routes.api.get.image;

import java.nio.file.Files;
import java.nio.file.Path;

import spark.Request;
import spark.Response;
import spark.Route;

public class GetBanner implements Route {
    private static final String BANNER_DIR = "data/banners";

    @Override
    public Object handle(Request req, Response res) throws Exception {
        String userId = req.params(":id");

        if (userId == null || !userId.matches("\\d+")) {
            res.status(400);
            return "Invalid user ID";
        }

        Path bannerPath = Path.of(BANNER_DIR, userId + ".png");

        if (Files.exists(bannerPath)) {
            res.type("image/png");
            return Files.readAllBytes(bannerPath);
        } else {
            res.status(404);
            return "Banner not found";
        }
    }

}
