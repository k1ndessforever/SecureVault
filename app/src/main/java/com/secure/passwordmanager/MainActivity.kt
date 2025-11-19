package com.secure.passwordmanager

import android.animation.ObjectAnimator
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.card.MaterialCardView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import org.json.JSONObject
import java.security.MessageDigest
import android.util.Base64
import java.security.SecureRandom
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var emptyView: LinearLayout
    private lateinit var searchBar: androidx.appcompat.widget.SearchView
    private lateinit var themeToggle: ImageButton
    private lateinit var passwordAdapter: PasswordAdapter
    private lateinit var secureStorage: SecureStorage
    private lateinit var biometricPrompt: BiometricPrompt
    private var isUnlocked = false
    private var currentTheme = Theme.DARK

    enum class Theme { LIGHT, DARK, AUTO }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySavedTheme()
        setContentView(R.layout.activity_main)

        supportActionBar?.elevation = 0f
        supportActionBar?.title = "🔐 Secure Vault"

        secureStorage = SecureStorage(this)

        initializeViews()
        setupRecyclerView()
        setupListeners()
        setupBiometricAuth()

        if (secureStorage.isFirstTime()) {
            showWelcomeAnimation()
            showSetupDialog()
        } else {
            authenticateUser()
        }
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.recyclerView)
        fabAdd = findViewById(R.id.fabAdd)
        emptyView = findViewById(R.id.emptyView)
        searchBar = findViewById(R.id.searchBar)
        themeToggle = findViewById(R.id.themeToggle)

        fabAdd.scaleX = 0f
        fabAdd.scaleY = 0f
        fabAdd.animate().scaleX(1f).scaleY(1f).setDuration(300).setStartDelay(200).setInterpolator(DecelerateInterpolator()).start()
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        passwordAdapter = PasswordAdapter(
            onItemClick = { password -> showPasswordDetails(password) },
            onDeleteClick = { password -> deletePassword(password) },
            onEditClick = { password -> showAddPasswordDialog(password) },
            getEmoji = { category -> getCategoryEmoji(category) }
        )
        recyclerView.adapter = passwordAdapter
        recyclerView.itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator().apply {
            addDuration = 300
            removeDuration = 300
        }
    }

    private fun setupListeners() {
        fabAdd.setOnClickListener {
            if (isUnlocked) {
                showAddPasswordDialog()
            } else {
                showSnackbar("Please unlock vault first")
            }
        }
        themeToggle.setOnClickListener { showThemeSelector() }
        searchBar.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                passwordAdapter.filter(newText ?: "")
                return true
            }
        })
    }

    private fun setupBiometricAuth() {
        val executor: Executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    unlockVault()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    showSnackbar("Authentication error: $errString")
                }
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    showSnackbar("Authentication failed")
                }
            })
    }

    private fun authenticateUser() {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("🔓 Unlock Vault")
            .setSubtitle("Authenticate to access your passwords")
            .setNegativeButtonText("Use Master Password")
            .build()
        biometricPrompt.authenticate(promptInfo)
    }

    private fun showWelcomeAnimation() {
        val welcome = findViewById<TextView>(R.id.tvWelcome)
        welcome.visibility = View.VISIBLE
        welcome.alpha = 0f
        welcome.translationY = -50f
        welcome.animate().alpha(1f).translationY(0f).setDuration(600).setInterpolator(DecelerateInterpolator()).withEndAction {
            welcome.animate().alpha(0f).setStartDelay(2000).setDuration(400).start()
        }.start()
    }

    private fun showSetupDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_setup_master, null)
        val etMasterPassword = dialogView.findViewById<TextInputEditText>(R.id.etMasterPassword)
        val etConfirmPassword = dialogView.findViewById<TextInputEditText>(R.id.etConfirmPassword)
        val strengthMeter = dialogView.findViewById<ProgressBar>(R.id.strengthMeter)
        val strengthText = dialogView.findViewById<TextView>(R.id.strengthText)

        etMasterPassword.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val strength = calculatePasswordStrength(s.toString())
                strengthMeter.progress = strength
                strengthText.text = when {
                    strength < 25 -> "Weak 😟"
                    strength < 50 -> "Fair 😐"
                    strength < 75 -> "Good 🙂"
                    else -> "Strong 💪"
                }
                strengthText.setTextColor(when {
                    strength < 25 -> ContextCompat.getColor(this@MainActivity, R.color.error)
                    strength < 50 -> ContextCompat.getColor(this@MainActivity, R.color.warning)
                    strength < 75 -> ContextCompat.getColor(this@MainActivity, R.color.primary_light)
                    else -> ContextCompat.getColor(this@MainActivity, R.color.success)
                })
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        MaterialAlertDialogBuilder(this, R.style.RoundedDialog)
            .setTitle("🔐 Create Master Password")
            .setMessage("This password protects all your data. Make it strong and memorable!")
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton("Create Vault") { _, _ ->
                val master = etMasterPassword.text.toString()
                val confirm = etConfirmPassword.text.toString()

                if (master.length < 8) {
                    showSnackbar("Password must be 8+ characters")
                    showSetupDialog()
                } else if (master != confirm) {
                    showSnackbar("Passwords don't match")
                    showSetupDialog()
                } else {
                    secureStorage.setMasterPassword(master)
                    unlockVault()
                    showSnackbar("✅ Vault created successfully!")
                }
            }.show()
    }

    private fun unlockVault() {
        isUnlocked = true
        loadPasswords()
        animateUnlock()
        showSnackbar("🔓 Vault unlocked")
    }

    private fun animateUnlock() {
        val lockIcon = findViewById<ImageView>(R.id.lockIcon)
        lockIcon.animate().alpha(0f).scaleX(0f).scaleY(0f).setDuration(400).start()
    }

    private fun loadPasswords() {
        val passwords = secureStorage.getAllPasswords()
        passwordAdapter.setPasswords(passwords)
        emptyView.visibility = if (passwords.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (passwords.isEmpty()) View.GONE else View.VISIBLE
        if (passwords.isEmpty()) animateEmptyView()
    }

    private fun animateEmptyView() {
        emptyView.alpha = 0f
        emptyView.animate().alpha(1f).setDuration(400).start()
    }

    private fun showAddPasswordDialog(passwordToEdit: PasswordEntry? = null) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_password, null)
        val etService = dialogView.findViewById<TextInputEditText>(R.id.etService)
        val etUsername = dialogView.findViewById<TextInputEditText>(R.id.etUsername)
        val etPassword = dialogView.findViewById<TextInputEditText>(R.id.etPassword)
        val etNotes = dialogView.findViewById<TextInputEditText>(R.id.etNotes)
        val btnGenerate = dialogView.findViewById<MaterialButton>(R.id.btnGeneratePassword)
        val categoryChips = dialogView.findViewById<ChipGroup>(R.id.categoryChips)

        passwordToEdit?.let {
            etService.setText(it.service)
            etUsername.setText(it.username)
            etPassword.setText(it.password)
            etNotes.setText(it.notes)
            etService.isEnabled = false

            val chipToSelect = when (it.category.lowercase()) {
                "email" -> R.id.chipEmail
                "social" -> R.id.chipSocial
                "banking" -> R.id.chipBanking
                "shopping" -> R.id.chipShopping
                "work" -> R.id.chipWork
                else -> R.id.chipOther
            }
            categoryChips.check(chipToSelect)
        }

        btnGenerate.setOnClickListener {
            val generated = generateStrongPassword(20)
            etPassword.setText(generated)
            etPassword.setSelection(generated.length)
            btnGenerate.icon = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_view)
        }

        MaterialAlertDialogBuilder(this, R.style.RoundedDialog)
            .setTitle(if (passwordToEdit == null) "➕ Add New Password" else "✏️ Edit Password")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val service = etService.text.toString()
                if (service.isBlank()) {
                    showSnackbar("Service name cannot be empty")
                    return@setPositiveButton
                }

                val category = when (categoryChips.checkedChipId) {
                    R.id.chipEmail -> "Email"
                    R.id.chipSocial -> "Social"
                    R.id.chipBanking -> "Banking"
                    R.id.chipShopping -> "Shopping"
                    R.id.chipWork -> "Work"
                    else -> "Other"
                }

                val password = PasswordEntry(
                    service = service,
                    username = etUsername.text.toString(),
                    password = etPassword.text.toString(),
                    notes = etNotes.text.toString(),
                    category = category,
                    created = passwordToEdit?.created ?: System.currentTimeMillis()
                )
                secureStorage.savePassword(password)
                loadPasswords()
                showSnackbar("✅ Password saved securely")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPasswordDetails(password: PasswordEntry) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_password_details, null)
        val tvService = dialogView.findViewById<TextView>(R.id.tvDetailService)
        val tvUsername = dialogView.findViewById<TextView>(R.id.tvDetailUsername)
        val tvPassword = dialogView.findViewById<TextView>(R.id.tvDetailPassword)
        val tvNotes = dialogView.findViewById<TextView>(R.id.tvDetailNotes)
        val tvCategory = dialogView.findViewById<TextView>(R.id.tvDetailCategory)
        val tvCreated = dialogView.findViewById<TextView>(R.id.tvDetailCreated)
        val btnTogglePassword = dialogView.findViewById<ImageButton>(R.id.btnTogglePassword)

        tvService.text = password.service
        tvUsername.text = password.username
        tvPassword.text = "••••••••••••"
        tvNotes.text = password.notes.ifEmpty { "No notes" }
        tvCategory.text = "${getCategoryEmoji(password.category)} ${password.category}"
        tvCreated.text = "Created: ${formatDate(password.created)}"

        var isPasswordVisible = false
        btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            tvPassword.text = if (isPasswordVisible) password.password else "••••••••••••"
            btnTogglePassword.setImageResource(if (isPasswordVisible) android.R.drawable.ic_menu_view else android.R.drawable.ic_menu_close_clear_cancel)
        }

        MaterialAlertDialogBuilder(this, R.style.RoundedDialog)
            .setView(dialogView)
            .setPositiveButton("📋 Copy Password") { _, _ -> copyToClipboard(password.password) }
            .setNeutralButton("✏️ Edit") { _, _ -> showAddPasswordDialog(password) }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun deletePassword(password: PasswordEntry) {
        MaterialAlertDialogBuilder(this, R.style.RoundedDialog)
            .setTitle("🗑️ Delete Password?")
            .setMessage("Are you sure you want to delete the password for ${password.service}?")
            .setPositiveButton("Delete") { _, _ ->
                secureStorage.deletePassword(password.service)
                loadPasswords()
                showSnackbar("🗑️ Password deleted")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showThemeSelector() {
        val themes = arrayOf("🌙 Dark", "☀️ Light", "🔄 Auto (System)")
        MaterialAlertDialogBuilder(this, R.style.RoundedDialog)
            .setTitle("🎨 Choose Theme")
            .setSingleChoiceItems(themes, currentTheme.ordinal) { dialog, which ->
                currentTheme = Theme.values()[which]
                applyTheme(currentTheme)
                secureStorage.saveTheme(currentTheme.name)
                dialog.dismiss()
                recreate()
            }.show()
    }

    private fun applySavedTheme() {
        currentTheme = try { Theme.valueOf(secureStorage.getTheme()) } catch (e: Exception) { Theme.DARK }
        applyTheme(currentTheme)
    }

    private fun applyTheme(theme: Theme) {
        when (theme) {
            Theme.LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            Theme.DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            Theme.AUTO -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("password", text)
        clipboard.setPrimaryClip(clip)
        showSnackbar("📋 Copied to clipboard")
    }

    private fun showSnackbar(message: String) {
        com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), message, 2000)
            .setBackgroundTint(ContextCompat.getColor(this, R.color.primary))
            .show()
    }

    private fun calculatePasswordStrength(password: String): Int {
        var strength = 0
        if (password.length >= 8) strength += 25
        if (password.length >= 12) strength += 15
        if (password.any { it.isUpperCase() }) strength += 20
        if (password.any { it.isLowerCase() }) strength += 20
        if (password.any { it.isDigit() }) strength += 10
        if (password.any { !it.isLetterOrDigit() }) strength += 10
        return strength.coerceIn(0, 100)
    }

    private fun formatDate(timestamp: Long): String {
        return java.text.SimpleDateFormat("MMM dd, yyyy • HH:mm", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
    }
    
    private fun getCategoryEmoji(category: String): String {
        return when (category.lowercase()) {
            "social" -> "📱"
            "email" -> "✉️"
            "banking" -> "🏦"
            "shopping" -> "🛒"
            "work" -> "💼"
            "entertainment" -> "🎮"
            else -> "📁"
        }
    }

    private fun generateStrongPassword(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+-="
        val random = SecureRandom()
        return (1..length).map { chars[random.nextInt(chars.length)] }.joinToString("")
    }
}

data class PasswordEntry(
    val service: String,
    val username: String,
    val password: String,
    val notes: String,
    val category: String = "Other",
    val created: Long
)

class PasswordAdapter(
    private val onItemClick: (PasswordEntry) -> Unit,
    private val onDeleteClick: (PasswordEntry) -> Unit,
    private val onEditClick: (PasswordEntry) -> Unit,
    private val getEmoji: (String) -> String
) : RecyclerView.Adapter<PasswordAdapter.ViewHolder>() {

    private var allPasswords = listOf<PasswordEntry>()
    private var filteredPasswords = listOf<PasswordEntry>()

    fun setPasswords(newPasswords: List<PasswordEntry>) {
        allPasswords = newPasswords
        filteredPasswords = newPasswords
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        filteredPasswords = if (query.isEmpty()) allPasswords else allPasswords.filter {
            it.service.contains(query, ignoreCase = true) || it.username.contains(query, ignoreCase = true)
        }
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.cardPassword)
        val tvService: TextView = view.findViewById(R.id.tvService)
        val tvUsername: TextView = view.findViewById(R.id.tvUsername)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val iconService: TextView = view.findViewById(R.id.iconService)
        val btnMore: ImageButton = view.findViewById(R.id.btnMore)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context).inflate(R.layout.item_password_enhanced, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val password = filteredPasswords[position]
        holder.tvService.text = password.service
        holder.tvUsername.text = password.username
        holder.tvCategory.text = getEmoji(password.category)
        holder.iconService.text = password.service.firstOrNull()?.uppercase() ?: "?"

        holder.card.alpha = 0f
        holder.card.translationY = 50f
        holder.card.animate().alpha(1f).translationY(0f).setDuration(300).setStartDelay((position * 50L)).start()

        holder.card.setOnClickListener { onItemClick(password) }
        holder.btnMore.setOnClickListener { showContextMenu(holder.itemView, password) }
    }

    override fun getItemCount() = filteredPasswords.size

    private fun showContextMenu(view: View, password: PasswordEntry) {
        val popup = android.widget.PopupMenu(view.context, view)
        popup.inflate(R.menu.password_context_menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit -> { onEditClick(password); true }
                R.id.action_delete -> { onDeleteClick(password); true }
                else -> false
            }
        }
        popup.show()
    }
}

class SecureStorage(private val context: android.content.Context) {

    private val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_passwords_v2",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun isFirstTime() = !sharedPreferences.contains("master_password_hash")

    fun setMasterPassword(password: String) {
        sharedPreferences.edit().putString("master_password_hash", hashPassword(password)).apply()
    }

    fun savePassword(entry: PasswordEntry) {
        val json = JSONObject().apply {
            put("username", entry.username)
            put("password", entry.password)
            put("notes", entry.notes)
            put("category", entry.category)
            put("created", entry.created)
        }.toString()
        sharedPreferences.edit().putString("pwd_${entry.service}", json).apply()
    }

    fun getAllPasswords(): List<PasswordEntry> {
        return sharedPreferences.all.filter { it.key.startsWith("pwd_") }.mapNotNull { (key, value) ->
            try {
                val json = JSONObject(value as String)
                PasswordEntry(
                    service = key.removePrefix("pwd_"),
                    username = json.getString("username"),
                    password = json.getString("password"),
                    notes = json.optString("notes", ""),
                    category = json.optString("category", "Other"),
                    created = json.getLong("created")
                )
            } catch (e: Exception) { null }
        }.sortedByDescending { it.created }
    }

    fun deletePassword(service: String) {
        sharedPreferences.edit().remove("pwd_$service").apply()
    }

    fun saveTheme(theme: String) {
        sharedPreferences.edit().putString("app_theme", theme).apply()
    }

    fun getTheme(): String {
        return sharedPreferences.getString("app_theme", "DARK") ?: "DARK"
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray())
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }
}