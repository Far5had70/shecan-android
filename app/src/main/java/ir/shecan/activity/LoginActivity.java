package ir.shecan.activity;

import android.os.Bundle;
import android.view.ViewTreeObserver;

import androidx.appcompat.app.AppCompatActivity;

import ir.shecan.databinding.FragmentLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private FragmentLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = FragmentLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


    }
}
