package website.xihan.xposed

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import website.xihan.xposed.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.apply {
            editText.apply {
                setText(ModuleConfig.textViewText)
                addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable) {

                    }

                    override fun beforeTextChanged(
                        s: CharSequence, start: Int, count: Int, after: Int
                    ) {

                    }

                    override fun onTextChanged(
                        s: CharSequence, start: Int, before: Int, count: Int
                    ) {
                        ModuleConfig.textViewText = s.toString()
                    }

                })
            }

            switch1.apply {
                isChecked = ModuleConfig.swithchEnable
                setOnCheckedChangeListener { _, isChecked ->
                    ModuleConfig.swithchEnable = isChecked
                }
            }
        }
    }

}