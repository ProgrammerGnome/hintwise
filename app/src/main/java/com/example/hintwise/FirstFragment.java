package com.example.hintwise;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.hintwise.data.AppDatabase;
import com.example.hintwise.data.SavedText;
import com.example.hintwise.databinding.FragmentFirstBinding;
import com.example.hintwise.util.JsonLoader;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;
    private AppDatabase db;
    private SavedText currentProblem;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS) // kapcsolat timeout
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)   // írási timeout
            .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)   // olvasási timeout
            .build();

    private static final String PROMPT_TEXT = "Kérlek a következő feladat megoldásához szükséges matematikai eszközöket " +
            "(fogalmakat, definíciókat, tételeket) add meg általánosan és tömören. " +
            "Különösen figyelj rá, hogy megoldási ötletet ne adj, csak eszközöket! A feladat: ";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    private String loadApiKey() {
        try {
            InputStream is = requireContext().getAssets().open("apikey.txt");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            return new String(buffer).trim();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        db = AppDatabase.getInstance(requireContext());

        if (db.savedTextDao().count() == 0) {
            List<SavedText> problems = JsonLoader.loadProblems(requireContext());
            if (problems != null) {
                db.savedTextDao().insertAll(problems);
            }
        }

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        DrawerLayout drawerLayout = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        Toolbar toolbar = binding.toolbar;

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            activity.setSupportActionBar(toolbar);
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    getActivity(), drawerLayout, toolbar,
                    R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();
        }

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_second) {
                androidx.navigation.fragment.NavHostFragment.findNavController(this)
                        .navigate(R.id.SecondFragment);
            } else if (id == R.id.nav_third) {
                androidx.navigation.fragment.NavHostFragment.findNavController(this)
                        .navigate(R.id.ThirdFragment);
            }
            drawerLayout.closeDrawers();
            return true;
        });

        binding.buttonRandom.setOnClickListener(v -> {
            SavedText randomText = db.savedTextDao().getRandomText();
            if (randomText != null) {
                currentProblem = randomText;
                renderLatex(randomText.problem);
            } else {
                binding.webViewLatex.loadData("Nincs adat az adatbázisban.", "text/html", "UTF-8");
            }
        });

        binding.buttonHint.setOnClickListener(v -> {
            if (currentProblem != null) {
                requestHintFromGemini(currentProblem.problem);
            } else {
                renderHint("Előbb tölts be egy feladatot!");
            }
        });

        binding.webViewHint.setVerticalScrollBarEnabled(true);
        binding.webViewHint.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        binding.webViewHint.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void renderLatex(String latex) {
        binding.webViewLatex.getSettings().setJavaScriptEnabled(true);
        binding.webViewLatex.getSettings().setDomStorageEnabled(true);

        binding.webViewLatex.setWebViewClient(new android.webkit.WebViewClient() {
            @Override
            public void onPageFinished(android.webkit.WebView view, String url) {
                super.onPageFinished(view, url);
                String safeLatex = latex.replace("\\", "\\\\").replace("`", "\\`");
                binding.webViewLatex.evaluateJavascript("renderLatex(`" + safeLatex + "`);", null);
            }
        });

        binding.webViewLatex.loadUrl("file:///android_asset/katex_page.html");
    }

    private void requestHintFromGemini(String problemText) {

        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        try {
            JSONObject root = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject contentObj = new JSONObject();
            contentObj.put("parts", new JSONArray().put(new JSONObject().put("text",
                    PROMPT_TEXT + problemText)));
            contents.put(contentObj);

            root.put("contents", contents);

            RequestBody body = RequestBody.create(root.toString(), JSON);

            String API_KEY = loadApiKey();
            if (API_KEY == null) {
                renderHint("API kulcs betöltése sikertelen!");
                return;
            }

            String GEMINI_API_URL = API_URL + API_KEY;

            requireActivity().runOnUiThread(() -> renderHint("Válaszra várunk..."));

            Request request = new Request.Builder()
                    .url(GEMINI_API_URL)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    requireActivity().runOnUiThread(() ->
                            renderHint("Hálózati hiba: " + e.getMessage()));
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        requireActivity().runOnUiThread(() ->
                                renderHint("API hiba: " + response.code() + " " + response.message()));
                        if (response.code() == 429) {
                            try {
                                Thread.sleep(5000); // 5 másodperc várakozás
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            requestHintFromGemini(currentProblem.problem);
                        }
                        return;
                    }
                    String responseBody = response.body().string();
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        JSONArray candidates = json.getJSONArray("candidates");
                        if (candidates.length() > 0) {
                            JSONObject first = candidates.getJSONObject(0);
                            JSONObject content = first.getJSONObject("content");
                            JSONArray parts = content.getJSONArray("parts");
                            if (parts.length() > 0) {
                                String hint = parts.getJSONObject(0).getString("text");
                                requireActivity().runOnUiThread(() ->
                                        renderHint(hint.trim()));
                            } else {
                                requireActivity().runOnUiThread(() ->
                                        renderHint("Nincs hint a válaszban."));
                            }
                        } else {
                            requireActivity().runOnUiThread(() ->
                                    renderHint("Nincs candidate a válaszban."));
                        }
                    } catch (Exception e) {
                        requireActivity().runOnUiThread(() ->
                                renderHint("Parsing hiba: " + e.getMessage() + "\nRaw: " + responseBody));
                    }
                }
            });

        } catch (Exception e) {
            renderHint("JSON hiba: " + e.getMessage());
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void renderHint(String hint) {
        binding.webViewHint.getSettings().setJavaScriptEnabled(true);
        binding.webViewHint.getSettings().setDomStorageEnabled(true);
        binding.webViewHint.getSettings().setLoadWithOverviewMode(true);
        binding.webViewHint.getSettings().setUseWideViewPort(true);

        String boldedHint = hint.replaceAll("\\*\\*(.+?)\\*\\*", "<b>$1</b>");
        binding.webViewHint.setWebViewClient(new android.webkit.WebViewClient() {
            @Override
            public void onPageFinished(android.webkit.WebView view, String url) {
                super.onPageFinished(view, url);

                String safeHint = boldedHint.replace("\\", "\\\\").replace("`", "\\`").replace("\n", "<br>");
                binding.webViewHint.evaluateJavascript("renderLatex(`" + safeHint + "`);", null);
            }
        });
        binding.webViewHint.loadUrl("file:///android_asset/webview_hint.html");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
