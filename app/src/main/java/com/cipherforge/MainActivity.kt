package com.cipherforge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cipherforge.crypto.AesCipher
import com.cipherforge.crypto.KeyCodec
import com.cipherforge.viewmodel.CipherType
import com.cipherforge.viewmodel.CipherViewModel
import com.cipherforge.viewmodel.EcdhViewModel
import com.cipherforge.viewmodel.RsaViewModel

enum class Screen(val label: String) {
    SYMMETRIC("Классика / AES"),
    RSA("RSA"),
    ECDH("ECDH")
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
        }
    }
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

/** Экран RSA (гибридное шифрование). Вся логика — в RsaViewModel. */
@Composable
fun RsaScreen(viewModel: RsaViewModel = viewModel()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "RSA-2048 + гибридное шифрование: сообщение шифруется AES-ключом, " +
                "а сам AES-ключ — публичным RSA-ключом получателя.",
            style = MaterialTheme.typography.bodySmall
        )

        Button(onClick = { viewModel.generateKeys() }) {
            Text("Сгенерировать пару ключей")
        }

        viewModel.keyPair?.let { kp ->
            Text("Публичный ключ (можно передавать открыто):", style = MaterialTheme.typography.labelLarge)
            SelectionContainer { Text(KeyCodec.encodePublicKey(kp.public), maxLines = 3) }
            Text("Приватный ключ (хранить только у себя!):", style = MaterialTheme.typography.labelLarge)
            SelectionContainer { Text(KeyCodec.encodePrivateKey(kp.private), maxLines = 3) }
        }

        OutlinedTextField(
            value = viewModel.inputText,
            onValueChange = { viewModel.inputText = it },
            label = { Text("Текст") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
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

/** Экран ECDH (обмен ключами). Вся логика — в EcdhViewModel. */
@Composable
fun EcdhScreen(viewModel: EcdhViewModel = viewModel()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "ECDH: ECC сама по себе не шифрует данные — она позволяет двум сторонам " +
                "согласовать общий секрет по открытому каналу, из которого потом выводится AES-ключ.",
            style = MaterialTheme.typography.bodySmall
        )

        Button(onClick = { viewModel.generateKeys() }) {
            Text("Сгенерировать ключи Алисы и Боба")
        }

        if (viewModel.aliceKeyPair != null && viewModel.bobKeyPair != null) {
            Button(onClick = { viewModel.deriveSharedKeys() }) {
                Text("Согласовать общий секрет")
            }
        }

        viewModel.aliceSharedKey?.let {
            Text("Ключ, выведенный Алисой:", style = MaterialTheme.typography.labelSmall)
            SelectionContainer { Text(AesCipher.keyToString(it)) }
        }
        viewModel.bobSharedKey?.let {
            Text("Ключ, выведенный Бобом:", style = MaterialTheme.typography.labelSmall)
            SelectionContainer { Text(AesCipher.keyToString(it)) }
        }
        if (viewModel.aliceSharedKey != null && viewModel.bobSharedKey != null) {
            val match = AesCipher.keyToString(viewModel.aliceSharedKey!!) ==
                AesCipher.keyToString(viewModel.bobSharedKey!!)
            Text(
                if (match) "✓ Ключи совпали" else "✗ Ключи не совпадают",
                color = if (match) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
            )
        }

        OutlinedTextField(
            value = viewModel.inputText,
            onValueChange = { viewModel.inputText = it },
            label = { Text("Сообщение от Алисы") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        viewModel.errorText?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.aliceEncrypts() }) { Text("Алиса шифрует") }
            Button(onClick = { viewModel.bobDecrypts() }) { Text("Боб расшифровывает") }
        }

        HorizontalDivider()
        Text("Зашифровано:", style = MaterialTheme.typography.labelLarge)
        SelectionContainer { Text(viewModel.encryptedText) }
        Text("Расшифровано Бобом:", style = MaterialTheme.typography.labelLarge)
        SelectionContainer { Text(viewModel.decryptedText) }
    }
}
