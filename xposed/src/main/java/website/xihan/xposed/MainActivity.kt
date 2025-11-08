package website.xihan.xposed

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import website.xihan.kv.KVStorage
import website.xihan.xposed.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                website.xihan.kv.KVFileTransfer.saveFileUri("website.xihan.kv.storage", it)
                binding.tvFileUri.text = it.toString()
            }
        }

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

            btnSelectFile.setOnClickListener {
                filePickerLauncher.launch(arrayOf("*/*"))
            }

            val savedUri = KVStorage.getString("SHARED_SETTINGS", "fileUri")
            tvFileUri.text = savedUri.ifEmpty { "未选择文件" }
        }
    }

}