package com.cipherforge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cipherforge.crypto.AesCipher
import com.cipherforge.qr.QrCodeGenerator
import com.cipherforge.viewmodel.CipherType
import com.cipherforge.viewmodel.CipherViewModel
import com.cipherforge.viewmodel.EcdhViewModel
import com.cipherforge.viewmodel.RsaViewModel
import com.cipherforge.viewmodel.VaultViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

enum class Screen(val label: String) {
    SYMMETRIC("Классика / AES"),
    RSA("RSA"),
    ECDH("ECDH"),
    VAULT("Хранилище")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RootScreen()
                }
            }
        }
    }
}

@Composable
fun RootScreen() {
    var screen by remember { mutableStateOf(Screen.SYMMETRIC) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = screen.ordinal) {
            Screen.values().forEach { s ->
                Tab(
                    selected = screen == s,
                    onClick = { screen = s },
                    text = { Text(s.label) }
                )
            }
        }
        when (screen) {
            Screen.SYMMETRIC -> CipherScreen()
            Screen.RSA -> RsaScreen()
            Screen.ECDH -> EcdhScreen()
            Screen.VAULT -> VaultScreen()
        }
    }
}

/** Диалог с QR-кодом для переданной строки (публичного ключа). */
@Composable
fun QrCodeDialog(text: String, onDismiss: () -> Unit) {
    val bitmap = remember(text) { QrCodeGenerator.generate(text) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = onDismiss) { Text("Закрыть") } },
        text = {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "QR-код публичного ключа",
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

/** Экран классических шифров и AES. Вся логика — в CipherViewModel, здесь только UI. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CipherScreen(viewModel: CipherViewModel = viewModel()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("CipherForge", style = MaterialTheme.typography.headlineMedium)

        Text("Алгоритм:", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CipherType.values().forEach { type ->
                FilterChip(
                    selected = viewModel.selectedCipher == type,
                    onClick = { viewModel.selectCipher(type) },
                    label = { Text(type.label) }
                )
            }
        }

        OutlinedTextField(
            value = viewModel.inputText,
            onValueChange = { viewModel.inputText = it },
            label = { Text("Текст") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        when (viewModel.selectedCipher) {
            CipherType.CAESAR -> OutlinedTextField(
                value = viewModel.paramText,
                onValueChange = { viewModel.paramText = it.filter { c -> c.isDigit() || c == '-' } },
                label = { Text("Сдвиг (число)") },
                modifier = Modifier.fillMaxWidth()
            )
            CipherType.VIGENERE -> OutlinedTextField(
                value = viewModel.paramText,
                onValueChange = { viewModel.paramText = it },
                label = { Text("Ключевое слово (латиница)") },
                modifier = Modifier.fillMaxWidth()
            )
            CipherType.AES -> {
                OutlinedTextField(
                    value = viewModel.aesKey?.let { AesCipher.keyToString(it) } ?: "",
                    onValueChange = { viewModel.setAesKeyFromInput(it) },
                    label = { Text("Ключ AES (base64) — оставь пустым и нажми «Сгенерировать»") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = { viewModel.generateAesKey() }) {
                    Text("Сгенерировать ключ")
                }
            }
        }

        viewModel.errorText?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.encrypt() }) { Text("Зашифровать") }
            Button(onClick = { viewModel.decrypt() }) { Text("Расшифровать") }
        }

        HorizontalDivider()
        Text("Результат:", style = MaterialTheme.typography.labelLarge)
        SelectionContainer { Text(viewModel.outputText) }
    }
}

/**
 * Экран RSA (гибридное шифрование). "Мой ключ" — для приёма сообщений (его публичную
 * часть показываешь/передаёшь по QR другим). "Ключ получателя" — вставляется или
 * сканируется, им шифруется то, что отправляешь ты. Расшифровка — всегда своим приватным.
 */
@Composable
fun RsaScreen(viewModel: RsaViewModel = viewModel()) {
    var showMyQr by remember { mutableStateOf(false) }
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { viewModel.recipientPublicKeyInput = it }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("1. Мой ключ (для получения сообщений)", style = MaterialTheme.typography.labelLarge)
        Button(onClick = { viewModel.generateKeys() }) { Text("Сгенерировать ключ") }

        viewModel.myPublicKeyEncoded?.let { publicKey ->
            SelectionContainer { Text(publicKey, maxLines = 3) }
            Button(onClick = { showMyQr = true }) { Text("Показать QR") }
        }
        if (showMyQr) {
            viewModel.myPublicKeyEncoded?.let { QrCodeDialog(it) { showMyQr = false } }
        }

        HorizontalDivider()

        Text("2. Ключ получателя (для отправки)", style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = viewModel.recipientPublicKeyInput,
            onValueChange = { viewModel.recipientPublicKeyInput = it },
            label = { Text("Публичный ключ получателя") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )
        Button(onClick = {
            scanLauncher.launch(ScanOptions().setOrientationLocked(false))
        }) { Text("Сканировать QR") }

        HorizontalDivider()

        Text("3. Сообщение", style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = viewModel.inputText,
            onValueChange = { viewModel.inputText = it },
            label = { Text("Текст") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        viewModel.errorText?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.encrypt() }) { Text("Зашифровать для получателя") }
            Button(onClick = { viewModel.decrypt() }) { Text("Расшифровать (мне)") }
        }

        HorizontalDivider()
        Text("Результат:", style = MaterialTheme.typography.labelLarge)
        SelectionContainer { Text(viewModel.outputText) }
    }
}

/**
 * Экран обмена ключами (ECDH). Практический сценарий: генерируешь свой ключ,
 * передаёшь свой публичный ключ собеседнику (сообщением, почтой и т.п.),
 * вставляешь присланный им публичный ключ — получаете общий AES-ключ, которым
 * можно шифровать переписку в обе стороны.
 */
@Composable
fun EcdhScreen(viewModel: EcdhViewModel = viewModel()) {
    var showMyQr by remember { mutableStateOf(false) }
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { viewModel.theirPublicKeyInput = it }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("1. Твой ключ", style = MaterialTheme.typography.labelLarge)
        Button(onClick = { viewModel.generateMyKeyPair() }) {
            Text("Сгенерировать ключ")
        }
        viewModel.myPublicKeyEncoded?.let { publicKey ->
            Text("Отправь это собеседнику:", style = MaterialTheme.typography.labelSmall)
            SelectionContainer { Text(publicKey, maxLines = 3) }
            Button(onClick = { showMyQr = true }) { Text("Показать QR") }
        }
        if (showMyQr) {
            viewModel.myPublicKeyEncoded?.let { QrCodeDialog(it) { showMyQr = false } }
        }

        HorizontalDivider()

        Text("2. Ключ собеседника", style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = viewModel.theirPublicKeyInput,
            onValueChange = { viewModel.theirPublicKeyInput = it },
            label = { Text("Вставь публичный ключ, присланный собеседником") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scanLauncher.launch(ScanOptions().setOrientationLocked(false))
            }) { Text("Сканировать QR") }
            Button(onClick = { viewModel.deriveSharedKey() }) {
                Text("Согласовать общий ключ")
            }
        }

        viewModel.sharedKey?.let {
            Text("Общий ключ согласован:", style = MaterialTheme.typography.labelSmall)
            SelectionContainer { Text(AesCipher.keyToString(it)) }
        }

        HorizontalDivider()

        Text("3. Сообщение", style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = viewModel.inputText,
            onValueChange = { viewModel.inputText = it },
            label = { Text("Текст") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        viewModel.errorText?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.encrypt() }) { Text("Зашифровать") }
            Button(onClick = { viewModel.decrypt() }) { Text("Расшифровать") }
        }

        HorizontalDivider()
        Text("Результат:", style = MaterialTheme.typography.labelLarge)
        SelectionContainer { Text(viewModel.outputText) }
    }
}

/**
 * Экран защищённого хранилища заметок. Ключ шифрования выводится из мастер-пароля
 * (PBKDF2), сам пароль нигде не хранится — только ключ, полученный из него.
 */
@Composable
fun VaultScreen(viewModel: VaultViewModel = viewModel()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!viewModel.isUnlocked) {
            Text(
                "Ключ шифрования выводится из мастер-пароля через PBKDF2 — сам пароль " +
                    "не сохраняется, хранится только производный ключ (в памяти, на время сессии).",
                style = MaterialTheme.typography.bodySmall
            )

            OutlinedTextField(
                value = viewModel.masterPasswordInput,
                onValueChange = { viewModel.masterPasswordInput = it },
                label = { Text("Мастер-пароль") },
                modifier = Modifier.fillMaxWidth()
            )

            viewModel.errorText?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!viewModel.vaultExists) {
                    Button(onClick = { viewModel.createVault() }) { Text("Создать хранилище") }
                } else {
                    Button(onClick = { viewModel.unlock() }) { Text("Разблокировать") }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Хранилище разблокировано", style = MaterialTheme.typography.labelLarge)
                Button(onClick = { viewModel.lock() }) { Text("Заблокировать") }
            }

            HorizontalDivider()
            Text("Новая заметка:", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = viewModel.newTitle,
                onValueChange = { viewModel.newTitle = it },
                label = { Text("Заголовок") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = viewModel.newContent,
                onValueChange = { viewModel.newContent = it },
                label = { Text("Содержимое (пароль, секрет и т.д.)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            viewModel.errorText?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = { viewModel.addNote() }) { Text("Сохранить заметку") }

            HorizontalDivider()
            Text("Заметки (${viewModel.notes.size}):", style = MaterialTheme.typography.labelLarge)
            viewModel.notes.forEach { note ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(note.title, style = MaterialTheme.typography.bodyLarge)
                    if (viewModel.revealedNoteId == note.id) {
                        SelectionContainer { Text(viewModel.revealedContent) }
                        Button(onClick = { viewModel.hideRevealed() }) { Text("Скрыть") }
                    } else {
                        Button(onClick = { viewModel.reveal(note) }) { Text("Показать") }
                    }
                }
                HorizontalDivider()
            }
        }
    }
}
