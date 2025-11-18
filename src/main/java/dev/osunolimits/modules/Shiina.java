package dev.osunolimits.modules;

import java.io.IOException;
import java.io.StringWriter;

import dev.osunolimits.main.App;
import dev.osunolimits.main.WebServer;
import dev.osunolimits.modules.ShiinaRoute.ShiinaRequest;
import dev.osunolimits.modules.utils.ThemeLoader;
import dev.osunolimits.plugins.NavbarRegister;
import dev.osunolimits.routes.get.modular.ModuleRegister;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import spark.Request;
import spark.Response;
import spark.Route;

public class Shiina implements Route {

    private String analytics;
    private String domain;

    public Shiina() {
        this.analytics = XmlConfig.getInstance().getOrDefault("plausible.js.url", " ");
        this.domain = App.env.get("DOMAIN").replaceAll("https://", "");
    }

    @Override
    public Object handle(Request req, Response res) throws Exception {
        throw new UnsupportedOperationException("Unimplemented method 'handle'");
    }

    public String renderTemplate(String template, ShiinaRequest shiina, Response res, Request req) {
        res.header("Content-Encoding", "gzip");
        res.header("Content-Type", "text/html; charset=utf-8");
        if (shiina.mysql != null)
            shiina.mysql.close();
        shiina.data.put("navbarItems", NavbarRegister.getItems());
        shiina.data.put("adminNavbarItems", NavbarRegister.getAdminItems());
        shiina.data.put("docs", ShiinaDocs.docs);
        shiina.data.put("analytics", analytics);
        shiina.data.put("domain", domain);
        shiina.data.put("devMode", App.devMode);
        shiina.data.put("themeIdent", ThemeLoader.generatedIdent);
        
        shiina.data.put("globalModules", ModuleRegister.getModulesRawForPage("globals", req, res, shiina));
        shiina.data.put("profileNavbarItems", NavbarRegister.getProfileItems());
        try {
            Template templateFree = WebServer.freemarkerCfg.getTemplate(template);
            try (StringWriter out = new StringWriter()) {
                templateFree.process(shiina.data, out);
                return out.toString();
            } catch (TemplateException e) {
                App.log.error("Error processing template (" + template + ")", e);
                return null;
            }
        } catch (IOException e) {
            App.log.error("Error loading template (" + template + ")", e);
            res.status(500);
            return null;
        }

    }

    public static void setCachePolicy(Response res, int days) {
        res.header("Cache-Control", "public, max-age=" + (days * 86400));
    }

    public Object redirect(Response response, ShiinaRequest shiina, String location) {
        if (shiina.mysql != null)
            shiina.mysql.close();
        response.status(302);
        response.redirect(location);
        return null;
    }

    public Object notFound(Response response, ShiinaRequest shiina) {
        if (shiina.mysql != null)
            shiina.mysql.close();
        response.status(404);
        return null;

    }

}
