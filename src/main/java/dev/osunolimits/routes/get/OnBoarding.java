package dev.osunolimits.routes.get;

import dev.osunolimits.modules.Shiina;
import dev.osunolimits.modules.ShiinaDocs;
import dev.osunolimits.modules.ShiinaRoute;
import dev.osunolimits.modules.ShiinaDocs.DocsModel;
import dev.osunolimits.modules.ShiinaRoute.ShiinaRequest;
import spark.Request;
import spark.Response;

public class OnBoarding extends Shiina {

    @Override
    public Object handle(Request req, Response res) throws Exception {
        ShiinaRequest shiina = new ShiinaRoute().handle(req, res);

        shiina.data.put("actNav", 0);
        for (DocsModel docs : ShiinaDocs.docs) {
            if (docs.getRoute().equals("connect")) {
                shiina.data.put("doc", docs);
                break;
            }
            
        }

        return renderTemplate("onboarding.html", shiina, res, req);
    }
    
}
