package com.liderafest.tickets;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private static final String PREFS = "lidera_tickets_native_prefs";
    private static final String KEY_BASE_URL = "base_url";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_EVENT_ID = "event_id";
    private static final String KEY_EVENT_TITLE = "event_title";
    private static final String KEY_LIST_SHORT_ID = "list_short_id";
    private static final String KEY_LIST_NAME = "list_name";
    private static final int REQ_CAMERA = 7401;

    private final int INDIGO = Color.parseColor("#1E3A5F");
    private final int CORAL = Color.parseColor("#FF6B4A");
    private final int GOLD = Color.parseColor("#F4C430");
    private final int TEAL = Color.parseColor("#2EC4B6");
    private final int BONE = Color.parseColor("#FAFBFC");
    private final int CHARCOAL = Color.parseColor("#2D3436");
    private final int GRAY = Color.parseColor("#636E72");
    private final int BORDER = Color.parseColor("#DFE6E9");
    private final int ERROR = Color.parseColor("#D63031");
    private final int SUCCESS = Color.parseColor("#00B894");

    private SharedPreferences prefs;
    private LinearLayout root;
    private ProgressBar progressBar;
    private TextView screenTitle;
    private String pendingEmail;
    private String pendingPassword;

    private String currentEventId;
    private String currentEventTitle;
    private String currentListShortId;
    private String currentListName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        setupSystemBars();
        showShell();

        String baseUrl = prefs.getString(KEY_BASE_URL, "");
        String token = prefs.getString(KEY_TOKEN, "");
        currentEventId = prefs.getString(KEY_EVENT_ID, "");
        currentEventTitle = prefs.getString(KEY_EVENT_TITLE, "");
        currentListShortId = prefs.getString(KEY_LIST_SHORT_ID, "");
        currentListName = prefs.getString(KEY_LIST_NAME, "");

        if (isBlank(baseUrl) || isBlank(token)) {
            showLogin();
        } else if (!isBlank(currentEventId) && !isBlank(currentListShortId)) {
            showScannerDashboard();
        } else {
            loadEvents();
        }
    }

    private void setupSystemBars() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(INDIGO);
            window.setNavigationBarColor(INDIGO);
        }
    }

    private int systemBarTopInset() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : 0;
    }

    private int systemBarBottomInset() {
        int id = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : 0;
    }

    private void showShell() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BONE);
        root.setPadding(0, systemBarTopInset(), 0, 0);
        root.setClipToPadding(false);
        setContentView(root);
        renderTopBar();

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.GONE);
        root.addView(progressBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3)));
    }

    private void renderTopBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(10), dp(6), dp(10), dp(6));
        bar.setBackgroundColor(INDIGO);

        screenTitle = new TextView(this);
        screenTitle.setText("Lidera Tickets");
        screenTitle.setTextColor(Color.WHITE);
        screenTitle.setTextSize(20);
        screenTitle.setTypeface(Typeface.DEFAULT_BOLD);
        screenTitle.setGravity(Gravity.CENTER_VERTICAL);
        screenTitle.setSingleLine(true);
        bar.addView(screenTitle, new LinearLayout.LayoutParams(0, dp(52), 1));

        Button logout = topButton("Salir");
        logout.setOnClickListener(v -> logout());
        bar.addView(logout);

        root.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(64)));
    }

    private Button topButton(String text) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(text);
        b.setTextSize(11);
        b.setTextColor(Color.WHITE);
        b.setBackground(bg(Color.parseColor("#24486F"), dp(14), Color.parseColor("#31587F"), 1));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(74), dp(42));
        p.setMargins(dp(4), 0, 0, 0);
        b.setLayoutParams(p);
        return b;
    }

    private void setContent(View view) {
        while (root.getChildCount() > 2) {
            root.removeViewAt(2);
        }
        root.addView(view, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
    }

    private ScrollView scrollWith(LinearLayout content) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BONE);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(20), dp(20), dp(28) + systemBarBottomInset());
        scroll.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return scroll;
    }

    private void showLogin() {
        screenTitle.setText("Lidera Tickets");
        LinearLayout content = new LinearLayout(this);
        setContent(scrollWith(content));

        ImageView logo = new ImageView(this);
        logo.setImageResource(getResources().getIdentifier("lidera_logo", "drawable", getPackageName()));
        logo.setAdjustViewBounds(true);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(126), dp(126));
        logoParams.gravity = Gravity.CENTER_HORIZONTAL;
        logoParams.setMargins(0, dp(4), 0, dp(12));
        content.addView(logo, logoParams);

        TextView title = title("Entrar por API");
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        content.addView(title);
        TextView sub = paragraph("Inicia sesión en Hi.Events, elige el evento y después la lista de invitados/check-in. No usa WebView.");
        sub.setGravity(Gravity.CENTER_HORIZONTAL);
        content.addView(sub);

        EditText baseUrl = input("URL base de Hi.Events", "https://tickets.tudominio.com", InputType.TYPE_TEXT_VARIATION_URI);
        baseUrl.setText(prefs.getString(KEY_BASE_URL, ""));
        content.addView(label("URL base"));
        content.addView(baseUrl);

        EditText email = input("Correo", "correo@dominio.com", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        email.setText(prefs.getString(KEY_EMAIL, ""));
        content.addView(label("Correo electrónico"));
        content.addView(email);

        EditText password = input("Contraseña", "Tu contraseña", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        content.addView(label("Contraseña"));
        content.addView(password);

        Button login = primaryButton("Iniciar sesión y cargar eventos");
        login.setOnClickListener(v -> {
            String b = sanitizeBaseUrl(value(baseUrl));
            String e = value(email);
            String p = value(password);
            if (isBlank(b)) { toast("Falta la URL base"); return; }
            if (isBlank(e)) { toast("Falta el correo"); return; }
            if (isBlank(p)) { toast("Falta la contraseña"); return; }
            prefs.edit().putString(KEY_BASE_URL, b).putString(KEY_EMAIL, e).apply();
            pendingEmail = e;
            pendingPassword = p;
            loginRequest(e, p, null);
        });
        content.addView(login);

        if (!isBlank(prefs.getString(KEY_TOKEN, ""))) {
            Button saved = secondaryButton("Usar sesión guardada");
            saved.setOnClickListener(v -> loadEvents());
            content.addView(saved);
        }

        TextView note = smallNote("Si tu usuario pertenece a varias cuentas, la app te pedirá escoger una después del primer intento.");
        content.addView(note);
    }

    private void loginRequest(String email, String password, String accountId) {
        showLoading(true);
        async(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("email", email);
                body.put("password", password);
                if (!isBlank(accountId)) body.put("account_id", Integer.parseInt(accountId));

                ApiResponse res = request("POST", "/auth/login", body.toString(), false);
                if (res.code >= 200 && res.code < 300) {
                    JSONObject json = new JSONObject(res.body);
                    String token = json.optString("token", "");
                    if (isBlank(token)) token = res.authToken;

                    JSONArray accounts = json.optJSONArray("accounts");
                    if (isBlank(token) && accounts != null && accounts.length() > 1) {
                        runOnUiThread(() -> showAccountChooser(accounts));
                        return;
                    }
                    if (isBlank(token) && accounts != null && accounts.length() == 1) {
                        JSONObject a = accounts.getJSONObject(0);
                        loginRequest(email, password, String.valueOf(a.opt("id")));
                        return;
                    }
                    if (isBlank(token)) {
                        fail("Login recibido, pero no vino token. Revisa si tu cuenta requiere elegir account_id.");
                        return;
                    }
                    prefs.edit()
                            .putString(KEY_TOKEN, token)
                            .putString(KEY_EMAIL, email)
                            .remove(KEY_EVENT_ID)
                            .remove(KEY_EVENT_TITLE)
                            .remove(KEY_LIST_SHORT_ID)
                            .remove(KEY_LIST_NAME)
                            .apply();
                    currentEventId = "";
                    currentEventTitle = "";
                    currentListShortId = "";
                    currentListName = "";
                    runOnUiThread(this::loadEvents);
                } else {
                    fail(errorMessage(res, "No se pudo iniciar sesión"));
                }
            } catch (Exception ex) {
                fail("Error de login: " + ex.getMessage());
            } finally {
                showLoading(false);
            }
        });
    }

    private void showAccountChooser(JSONArray accounts) {
        showLoading(false);
        screenTitle.setText("Elegir cuenta");
        LinearLayout content = new LinearLayout(this);
        setContent(scrollWith(content));
        content.addView(title("Elige la cuenta"));
        content.addView(paragraph("Tu usuario pertenece a varias cuentas de Hi.Events. Selecciona con cuál quieres entrar."));

        for (int i = 0; i < accounts.length(); i++) {
            try {
                JSONObject account = accounts.getJSONObject(i);
                String id = String.valueOf(account.opt("id"));
                String name = account.optString("name", "Cuenta " + id);
                Button b = cardButton(name, "ID: " + id);
                b.setOnClickListener(v -> loginRequest(pendingEmail, pendingPassword, id));
                content.addView(b);
            } catch (JSONException ignored) { }
        }
    }

    private void requireSessionThenEvents() {
        if (isBlank(prefs.getString(KEY_TOKEN, ""))) showLogin(); else loadEvents();
    }

    private void loadEvents() {
        screenTitle.setText("Eventos");
        showLoading(true);
        async(() -> {
            try {
                ApiResponse res = request("GET", "/events?perPage=100", null, true);
                if (res.code == 401 || res.code == 403) {
                    clearTokenOnly();
                    fail("Sesión expirada. Inicia sesión otra vez.");
                    runOnUiThread(this::showLogin);
                    return;
                }
                if (res.code < 200 || res.code >= 300) {
                    fail(errorMessage(res, "No se pudieron cargar eventos"));
                    return;
                }
                JSONArray events = new JSONObject(res.body).optJSONArray("data");
                if (events == null) events = new JSONArray();
                JSONArray finalEvents = events;
                runOnUiThread(() -> showEvents(finalEvents));
            } catch (Exception ex) {
                fail("Error cargando eventos: " + ex.getMessage());
            } finally {
                showLoading(false);
            }
        });
    }

    private void showEvents(JSONArray events) {
        screenTitle.setText("Eventos");
        LinearLayout content = new LinearLayout(this);
        setContent(scrollWith(content));
        content.addView(title("Selecciona el evento"));
        content.addView(paragraph("Después elegirás la lista de invitados/check-in para controlar qué tickets entran."));

        if (events.length() == 0) {
            content.addView(empty("No encontré eventos en esta cuenta."));
            Button retry = secondaryButton("Recargar eventos");
            retry.setOnClickListener(v -> loadEvents());
            content.addView(retry);
            return;
        }

        for (int i = 0; i < events.length(); i++) {
            try {
                JSONObject event = events.getJSONObject(i);
                String id = String.valueOf(event.opt("id"));
                String eventTitle = event.optString("title", "Evento " + id);
                String status = event.optString("status", "");
                String start = event.optString("start_date", "");
                Button b = cardButton(eventTitle, "ID: " + id + (isBlank(status) ? "" : " · " + status) + (isBlank(start) ? "" : " · " + start));
                b.setOnClickListener(v -> {
                    currentEventId = id;
                    currentEventTitle = eventTitle;
                    prefs.edit().putString(KEY_EVENT_ID, id).putString(KEY_EVENT_TITLE, eventTitle).apply();
                    loadCheckInLists(id, eventTitle);
                });
                content.addView(b);
            } catch (JSONException ignored) { }
        }
    }

    private void loadCheckInLists(String eventId, String eventTitle) {
        screenTitle.setText("Listas");
        showLoading(true);
        async(() -> {
            try {
                ApiResponse res = request("GET", "/events/" + enc(eventId) + "/check-in-lists?perPage=100", null, true);
                if (res.code == 401 || res.code == 403) {
                    clearTokenOnly();
                    fail("Sesión expirada. Inicia sesión otra vez.");
                    runOnUiThread(this::showLogin);
                    return;
                }
                if (res.code < 200 || res.code >= 300) {
                    fail(errorMessage(res, "No se pudieron cargar listas"));
                    return;
                }
                JSONArray lists = new JSONObject(res.body).optJSONArray("data");
                if (lists == null) lists = new JSONArray();
                JSONArray finalLists = lists;
                runOnUiThread(() -> showCheckInLists(eventId, eventTitle, finalLists));
            } catch (Exception ex) {
                fail("Error cargando listas: " + ex.getMessage());
            } finally {
                showLoading(false);
            }
        });
    }

    private void showCheckInLists(String eventId, String eventTitle, JSONArray lists) {
        screenTitle.setText("Listas");
        LinearLayout content = new LinearLayout(this);
        setContent(scrollWith(content));
        content.addView(title(eventTitle));
        content.addView(paragraph("Selecciona la lista de check-in/invitados que usarán los voluntarios."));

        if (lists.length() == 0) {
            content.addView(empty("Este evento no tiene Check-In Lists. Crea una en Hi.Events primero."));
            Button back = secondaryButton("Volver a eventos");
            back.setOnClickListener(v -> loadEvents());
            content.addView(back);
            return;
        }

        for (int i = 0; i < lists.length(); i++) {
            try {
                JSONObject list = lists.getJSONObject(i);
                String shortId = list.optString("short_id", "");
                String name = list.optString("name", "Lista " + shortId);
                int total = list.optInt("total_attendees", 0);
                int checked = list.optInt("checked_in_attendees", 0);
                boolean active = list.optBoolean("is_active", true);
                boolean expired = list.optBoolean("is_expired", false);
                String desc = checked + "/" + total + " registrados" + (active ? "" : " · INACTIVA") + (expired ? " · EXPIRADA" : "");
                Button b = cardButton(name, desc + " · " + shortId);
                b.setOnClickListener(v -> {
                    currentEventId = eventId;
                    currentEventTitle = eventTitle;
                    currentListShortId = shortId;
                    currentListName = name;
                    prefs.edit()
                            .putString(KEY_EVENT_ID, eventId)
                            .putString(KEY_EVENT_TITLE, eventTitle)
                            .putString(KEY_LIST_SHORT_ID, shortId)
                            .putString(KEY_LIST_NAME, name)
                            .apply();
                    showScannerDashboard();
                });
                content.addView(b);
            } catch (JSONException ignored) { }
        }

        Button changeEvent = secondaryButton("Cambiar evento");
        changeEvent.setOnClickListener(v -> loadEvents());
        content.addView(changeEvent);
    }

    private void showScannerDashboard() {
        screenTitle.setText("Escáner");
        currentEventId = prefs.getString(KEY_EVENT_ID, currentEventId);
        currentEventTitle = prefs.getString(KEY_EVENT_TITLE, currentEventTitle);
        currentListShortId = prefs.getString(KEY_LIST_SHORT_ID, currentListShortId);
        currentListName = prefs.getString(KEY_LIST_NAME, currentListName);

        LinearLayout content = new LinearLayout(this);
        setContent(scrollWith(content));

        content.addView(title("Escáner API"));
        content.addView(pill("Evento: " + safe(currentEventTitle)));
        content.addView(pill("Lista: " + safe(currentListName) + " · " + safe(currentListShortId)));
        content.addView(paragraph("Escanea el QR del ticket. El QR de Hi.Events contiene el public_id del asistente, por ejemplo A-XXXX."));

        Button eventsButton = secondaryButton("Cambiar evento o lista");
        eventsButton.setOnClickListener(v -> loadEvents());
        content.addView(eventsButton);

        Button scan = primaryButton("Escanear QR");
        scan.setOnClickListener(v -> startQrScan());
        content.addView(scan);

        EditText manual = input("Código del ticket", "A-XXXXXXXX", InputType.TYPE_CLASS_TEXT);
        content.addView(label("Código manual"));
        content.addView(manual);

        Button manualButton = secondaryButton("Validar código manual");
        manualButton.setOnClickListener(v -> {
            String code = extractAttendeePublicId(value(manual));
            if (isBlank(code)) { toast("Código inválido. Debe verse como A-XXXX"); return; }
            checkInAttendee(code);
        });
        content.addView(manualButton);

        Button changeList = secondaryButton("Cambiar lista de invitados/check-in");
        changeList.setOnClickListener(v -> loadCheckInLists(currentEventId, currentEventTitle));
        content.addView(changeList);

        Button changeEvent = tertiaryButton("Cambiar evento");
        changeEvent.setOnClickListener(v -> loadEvents());
        content.addView(changeEvent);
    }

    private void startQrScan() {
        if (!hasCameraPermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
            }
            return;
        }
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setCaptureActivity(LideraCaptureActivity.class);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Escanea el QR del ticket");
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(true);
        try {
            integrator.initiateScan();
        } catch (Exception ex) {
            showResult(false, "No se pudo abrir cámara", "Error: " + ex.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                toast("Escaneo cancelado");
            } else {
                String attendeeId = extractAttendeePublicId(result.getContents());
                if (isBlank(attendeeId)) {
                    showResult(false, "QR no reconocido", "Contenido leído: " + result.getContents());
                } else {
                    checkInAttendee(attendeeId);
                }
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startQrScan();
            } else {
                toast("Permiso de cámara denegado");
            }
        }
    }

    private boolean hasCameraPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void checkInAttendee(String attendeePublicId) {
        if (isBlank(currentListShortId)) {
            toast("Primero selecciona una lista de check-in");
            return;
        }
        showLoading(true);
        async(() -> {
            try {
                // First fetch attendee to give clearer errors and show name.
                ApiResponse attendeeRes = request("GET", "/public/check-in-lists/" + enc(currentListShortId) + "/attendees/" + enc(attendeePublicId), null, false);
                String attendeeName = attendeePublicId;
                if (attendeeRes.code >= 200 && attendeeRes.code < 300) {
                    JSONObject attendee = new JSONObject(attendeeRes.body).optJSONObject("data");
                    if (attendee != null) {
                        attendeeName = (attendee.optString("first_name", "") + " " + attendee.optString("last_name", "")).trim();
                        if (isBlank(attendeeName)) attendeeName = attendeePublicId;
                        JSONObject checkIn = attendee.optJSONObject("check_in");
                        if (checkIn != null) {
                            String finalName = attendeeName;
                            runOnUiThread(() -> showResult(false, "Ya ingresó", finalName + " ya aparece con check-in en esta lista."));
                            return;
                        }
                    }
                }

                JSONObject item = new JSONObject();
                item.put("public_id", attendeePublicId);
                item.put("action", "check-in");
                JSONArray attendees = new JSONArray();
                attendees.put(item);
                JSONObject body = new JSONObject();
                body.put("attendees", attendees);

                ApiResponse res = request("POST", "/public/check-in-lists/" + enc(currentListShortId) + "/check-ins", body.toString(), false);
                if (res.code >= 200 && res.code < 300) {
                    JSONObject json = new JSONObject(res.body);
                    JSONObject errors = json.optJSONObject("errors");
                    if (errors != null && errors.has(attendeePublicId)) {
                        String msg = errors.optString(attendeePublicId, "No se pudo registrar");
                        runOnUiThread(() -> showResult(false, "No permitido", msg));
                        return;
                    }
                    String finalName = attendeeName;
                    runOnUiThread(() -> showResult(true, "Check-in correcto", finalName + " fue registrado correctamente."));
                } else {
                    String msg = errorMessage(res, "No se pudo hacer check-in");
                    runOnUiThread(() -> showResult(false, "Error", msg));
                }
            } catch (Exception ex) {
                runOnUiThread(() -> showResult(false, "Error", ex.getMessage()));
            } finally {
                showLoading(false);
            }
        });
    }

    private void showResult(boolean ok, String title, String message) {
        LinearLayout content = new LinearLayout(this);
        setContent(scrollWith(content));
        TextView big = new TextView(this);
        big.setText(ok ? "✓" : "!");
        big.setTextSize(72);
        big.setGravity(Gravity.CENTER_HORIZONTAL);
        big.setTextColor(ok ? SUCCESS : ERROR);
        big.setTypeface(Typeface.DEFAULT_BOLD);
        content.addView(big);

        TextView t = title(title);
        t.setTextColor(ok ? SUCCESS : ERROR);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        content.addView(t);

        TextView msg = paragraph(message == null ? "" : message);
        msg.setGravity(Gravity.CENTER_HORIZONTAL);
        content.addView(msg);

        Button again = primaryButton("Escanear otro ticket");
        again.setOnClickListener(v -> startQrScan());
        content.addView(again);

        Button panel = secondaryButton("Volver al panel");
        panel.setOnClickListener(v -> showScannerDashboard());
        content.addView(panel);
    }

    private String extractAttendeePublicId(String raw) {
        if (raw == null) return "";
        String value = raw.trim();
        if (value.matches("(?i)^A-[A-Z0-9_-]{3,}$")) return value;
        Matcher matcher = Pattern.compile("(?i)(A-[A-Z0-9_-]{3,})").matcher(value);
        if (matcher.find()) return matcher.group(1);
        return "";
    }

    private ApiResponse request(String method, String path, String body, boolean auth) throws Exception {
        String base = sanitizeBaseUrl(prefs.getString(KEY_BASE_URL, ""));
        URL url = new URL(base + "/api" + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        String token = prefs.getString(KEY_TOKEN, "");
        if (auth && !isBlank(token)) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Cookie", "token=" + token);
        }
        if (body != null) {
            conn.setDoOutput(true);
            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(body);
            writer.flush();
            writer.close();
            os.close();
        }
        int code = conn.getResponseCode();
        InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        String response = readStream(is);
        String authToken = conn.getHeaderField("X-Auth-Token");
        conn.disconnect();
        return new ApiResponse(code, response, authToken);
    }

    private String readStream(InputStream is) throws Exception {
        if (is == null) return "";
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        return sb.toString();
    }

    private String errorMessage(ApiResponse res, String fallback) {
        try {
            JSONObject json = new JSONObject(res.body == null ? "{}" : res.body);
            if (json.has("message")) return json.optString("message", fallback);
            JSONObject errors = json.optJSONObject("errors");
            if (errors != null) return errors.toString();
        } catch (Exception ignored) { }
        return fallback + " (HTTP " + res.code + ")";
    }

    private void async(Runnable runnable) {
        new Thread(runnable).start();
    }

    private void showLoading(boolean loading) {
        runOnUiThread(() -> progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));
    }

    private void fail(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
    }

    private void logout() {
        prefs.edit()
                .remove(KEY_TOKEN)
                .remove(KEY_EVENT_ID)
                .remove(KEY_EVENT_TITLE)
                .remove(KEY_LIST_SHORT_ID)
                .remove(KEY_LIST_NAME)
                .apply();
        currentEventId = currentEventTitle = currentListShortId = currentListName = "";
        showLogin();
    }

    private void clearTokenOnly() {
        prefs.edit().remove(KEY_TOKEN).apply();
    }

    private String sanitizeBaseUrl(String raw) {
        if (raw == null) return "";
        String url = raw.trim();
        if (url.isEmpty()) return "";
        if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://" + url;
        while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        if (url.endsWith("/api")) url = url.substring(0, url.length() - 4);
        return url;
    }

    private String enc(String s) throws Exception {
        return URLEncoder.encode(s == null ? "" : s, "UTF-8");
    }

    private String value(EditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String safe(String s) {
        return isBlank(s) ? "Sin seleccionar" : s;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView title(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(INDIGO);
        v.setTextSize(26);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setPadding(0, dp(8), 0, dp(8));
        return v;
    }

    private TextView paragraph(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(GRAY);
        v.setTextSize(15);
        v.setLineSpacing(dp(2), 1.05f);
        v.setPadding(0, dp(4), 0, dp(12));
        return v;
    }

    private TextView label(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(INDIGO);
        v.setTextSize(13);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setPadding(0, dp(10), 0, dp(4));
        return v;
    }

    private TextView smallNote(String text) {
        TextView v = paragraph(text);
        v.setTextSize(13);
        v.setPadding(dp(12), dp(12), dp(12), dp(12));
        v.setBackground(bg(Color.WHITE, dp(12), BORDER, 1));
        return v;
    }

    private TextView empty(String text) {
        TextView v = paragraph(text);
        v.setGravity(Gravity.CENTER_HORIZONTAL);
        v.setPadding(dp(18), dp(28), dp(18), dp(28));
        v.setBackground(bg(Color.WHITE, dp(16), BORDER, 1));
        return v;
    }

    private TextView pill(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(13);
        v.setTextColor(CHARCOAL);
        v.setPadding(dp(12), dp(8), dp(12), dp(8));
        v.setBackground(bg(Color.WHITE, dp(20), BORDER, 1));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, dp(4), 0, dp(6));
        v.setLayoutParams(p);
        return v;
    }

    private EditText input(String label, String hint, int inputType) {
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setHint(hint);
        e.setTextSize(15);
        e.setInputType(inputType);
        e.setTextColor(CHARCOAL);
        e.setHintTextColor(Color.parseColor("#9AA4A8"));
        e.setPadding(dp(14), 0, dp(14), 0);
        e.setBackground(bg(Color.WHITE, dp(12), BORDER, 1));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        p.setMargins(0, 0, 0, dp(2));
        e.setLayoutParams(p);
        return e;
    }

    private Button primaryButton(String text) {
        Button b = baseButton(text);
        b.setTextColor(Color.WHITE);
        b.setBackground(bg(CORAL, dp(14), CORAL, 1));
        return b;
    }

    private Button secondaryButton(String text) {
        Button b = baseButton(text);
        b.setTextColor(INDIGO);
        b.setBackground(bg(GOLD, dp(14), GOLD, 1));
        return b;
    }

    private Button tertiaryButton(String text) {
        Button b = baseButton(text);
        b.setTextColor(INDIGO);
        b.setBackground(bg(Color.WHITE, dp(14), BORDER, 1));
        return b;
    }

    private Button baseButton(String text) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(text);
        b.setTextSize(15);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        p.setMargins(0, dp(8), 0, dp(8));
        b.setLayoutParams(p);
        return b;
    }

    private Button cardButton(String main, String sub) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        b.setText(main + "\n" + sub);
        b.setTextSize(14);
        b.setTextColor(CHARCOAL);
        b.setPadding(dp(14), 0, dp(14), 0);
        b.setBackground(bg(Color.WHITE, dp(16), BORDER, 1));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(82));
        p.setMargins(0, dp(6), 0, dp(6));
        b.setLayoutParams(p);
        return b;
    }

    private GradientDrawable bg(int color, int radius, int strokeColor, int strokeWidth) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius);
        if (strokeWidth > 0) g.setStroke(dp(strokeWidth), strokeColor);
        return g;
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed() {
        if (!isBlank(currentListShortId)) {
            currentListShortId = "";
            currentListName = "";
            prefs.edit().remove(KEY_LIST_SHORT_ID).remove(KEY_LIST_NAME).apply();
            if (!isBlank(currentEventId)) loadCheckInLists(currentEventId, currentEventTitle); else loadEvents();
        } else if (!isBlank(currentEventId)) {
            currentEventId = "";
            currentEventTitle = "";
            prefs.edit().remove(KEY_EVENT_ID).remove(KEY_EVENT_TITLE).apply();
            loadEvents();
        } else {
            super.onBackPressed();
        }
    }

    private static class ApiResponse {
        final int code;
        final String body;
        final String authToken;
        ApiResponse(int code, String body, String authToken) {
            this.code = code;
            this.body = body == null ? "" : body;
            this.authToken = authToken == null ? "" : authToken;
        }
    }
}
