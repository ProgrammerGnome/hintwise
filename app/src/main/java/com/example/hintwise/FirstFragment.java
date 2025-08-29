package com.example.hintwise;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.hintwise.data.AppDatabase;
import com.example.hintwise.data.SavedText;
import com.example.hintwise.databinding.FragmentFirstBinding;
import com.example.hintwise.util.JsonLoader;

import java.util.List;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;
    private AppDatabase db;

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

        binding.buttonFirst.setOnClickListener(v ->
                androidx.navigation.fragment.NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment)
        );

        binding.buttonRandom.setOnClickListener(v -> {
            SavedText randomText = db.savedTextDao().getRandomText();
            if (randomText != null) {
                renderLatex(randomText.problem);
            } else {
                binding.webViewLatex.loadData("Nincs adat az adatbázisban.", "text/html", "UTF-8");
            }
        });

        String exampleLatex = "Egy derékszögű háromszög befogói $a$ és $b$, átfogója $c$. Ekkor: $$c = \\sqrt{a^2 + b^2}$$";
        renderLatex(exampleLatex);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void renderLatex(String latex) {
        binding.webViewLatex.getSettings().setJavaScriptEnabled(true);
        binding.webViewLatex.getSettings().setDomStorageEnabled(true);

        // WebViewClient beállítása, hogy tudjuk, mikor töltődött be a HTML
        binding.webViewLatex.setWebViewClient(new android.webkit.WebViewClient() {
            @Override
            public void onPageFinished(android.webkit.WebView view, String url) {
                super.onPageFinished(view, url);
                // JSON-ból érkező dupla backslash-ek javítása
                String safeLatex = latex.replace("\\", "\\\\").replace("`", "\\`");
                // LaTeX renderelése a betöltött HTML-en
                binding.webViewLatex.evaluateJavascript("renderLatex(`" + safeLatex + "`);", null);
            }
        });

        // Betöltjük az assets HTML-t
        binding.webViewLatex.loadUrl("file:///android_asset/katex_page.html");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
