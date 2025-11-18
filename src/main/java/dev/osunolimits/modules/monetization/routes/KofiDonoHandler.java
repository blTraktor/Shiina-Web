package dev.osunolimits.modules.monetization.routes;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;

import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import ch.qos.logback.classic.Logger;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import dev.osunolimits.main.App;
import dev.osunolimits.models.UserInfoObject;
import dev.osunolimits.modules.Shiina;
import dev.osunolimits.modules.ShiinaRoute;
import dev.osunolimits.modules.ShiinaRoute.ShiinaRequest;
import dev.osunolimits.modules.monetization.MonetizationConfig;
import dev.osunolimits.plugins.events.admin.OnDonationEvent;
import dev.osunolimits.modules.XmlConfig;
import dev.osunolimits.routes.ap.api.PubSubModels;
import dev.osunolimits.routes.ap.api.PubSubModels.GiveDonatorInput;
import dev.osunolimits.routes.api.get.ShiinaAPIHandler;
import spark.Request;
import spark.Response;

public class KofiDonoHandler extends Shiina {
    private final Logger log = (Logger) LoggerFactory.getLogger("KofiDonoHandler");
    private final Gson GSON = new Gson();

    public KofiDonoHandler() {
        XmlConfig.getInstance().getOrDefault("monetization.discord.webhook", "");
        XmlConfig.secretConfigKeys.add("monetization.discord.webhook");
    }

    @Override
    public Object handle(Request req, Response res) throws Exception {
        ShiinaRequest shiina = new ShiinaRoute().handle(req, res);
        ShiinaAPIHandler apiHandler = new ShiinaAPIHandler();

        // Extract and decode the URL-encoded "data" parameter
        String encodedData = req.queryParams("data");
        if (encodedData == null) {
            apiHandler.addRequiredParameter("data", "string", "no data provided");
            return apiHandler.renderIssues(shiina, res);
        }

        String decodedData = URLDecoder.decode(encodedData, StandardCharsets.UTF_8);
        JSONObject data = new JSONObject(decodedData);

        // Extract required fields
        String verificationToken = data.optString("verification_token");
        String transactionId = data.optString("kofi_transaction_id");
        Double amount = Double.parseDouble(data.optString("amount"));
        String fromName = data.optString("from_name");
        String currency = data.optString("currency");
        String email = data.optString("email");

        new OnDonationEvent(transactionId, amount, currency, fromName + "(" + email + ")").callListeners();

        if (!MonetizationConfig.KOFI_CONFIG.getVerificationToken().equals(verificationToken)) {
            apiHandler.addRequiredParameter("verification_token", "string", "invalid verification token");
            return apiHandler.renderIssues(shiina, res);
        }

        String kofiWebhookUrl = XmlConfig.getInstance().getOrDefault("monetization.discord.webhook", "");
        if (!kofiWebhookUrl.isEmpty()) {
            WebhookClientBuilder webhookClientBuilder = new WebhookClientBuilder(kofiWebhookUrl);
            webhookClientBuilder.setWait(true);
            WebhookEmbed embed = new WebhookEmbedBuilder()
                    .setTitle(new WebhookEmbed.EmbedTitle("Kofi Donation", null))
                    .addField(new WebhookEmbed.EmbedField(true, "From", fromName))
                    .addField(new WebhookEmbed.EmbedField(true, "Amount", String.format("$%.2f", amount)))
                    .addField(new WebhookEmbed.EmbedField(false, "Transaction ID", transactionId))
                    .build();

            webhookClientBuilder.build().send(embed);
        }

        int donationMultiplier = (int) Math.floor(amount / MonetizationConfig.KOFI_CONFIG.getDonationAmount());
        if (donationMultiplier <= 0) {
            log.warn("Donation amount too low: {}", amount);
            apiHandler.addRequiredParameter("amount", "number", "donation amount too low");
            return apiHandler.renderIssues(shiina, res);
        }

        int weeks = donationMultiplier * 4;
        int months = donationMultiplier; // 1 donation unit = 1 month

        String safeName = fromName.toLowerCase().replaceAll(" ", "_");
        ResultSet checkForUser = shiina.mysql.Query("SELECT * FROM `users` WHERE `safe_name` = ?", safeName);
        if (!checkForUser.next()) {
            apiHandler.addRequiredParameter("from_name", "string", "user not found");
            return apiHandler.renderIssues(shiina, res);

        }

        int userId = checkForUser.getInt("id");
        GiveDonatorInput giveDonatorInput = new PubSubModels().new GiveDonatorInput();
        giveDonatorInput.setId(userId);
        giveDonatorInput.setDuration(weeks + "w");

        App.jedisPool.publish("givedonator", GSON.toJson(giveDonatorInput));

        Thread.sleep(500);

        UserInfoObject obj = GSON.fromJson(App.appCache.get("shiina:user:" + userId), UserInfoObject.class);
        ResultSet privRs = shiina.mysql.Query("SELECT `priv` FROM `users` WHERE `id` = ?", giveDonatorInput.getId());
        obj.priv = privRs.next() ? privRs.getInt("priv") : 0;
        String userJson = GSON.toJson(obj);
        App.appCache.set("shiina:user:" + userId, userJson);

        log.info("Kofi Donation: userId={}, months={}, amount={}, transactionId={}", userId, months, amount,
                transactionId);

        shiina.mysql.Exec(
                "INSERT INTO `sh_payments`(`user_id`, `months`, `total`, `payment_id`) VALUES (?,?,CAST(? AS DECIMAL(10,2)),?)",
                userId, months, amount, transactionId);
        log.info("Kofi Donation Success: {}", transactionId);

        return apiHandler.renderJSON(giveDonatorInput, shiina, res);
    }
}
