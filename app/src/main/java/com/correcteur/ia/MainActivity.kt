package com.correcteur.ia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

private val Green = Color(0xFF0B8F4A)
private val GreenDark = Color(0xFF087F43)
private val Red = Color(0xFFF02B25)
private val Orange = Color(0xFFFF5A1F)
private val SoftGreen = Color(0xFFEAF8F0)
private val SoftRed = Color(0xFFFFECEA)

private val AppColors = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    secondary = Red,
    onSecondary = Color.White,
    tertiary = Orange,
    background = Color(0xFFF7FAF8),
    surface = Color.White
)

class MainActivity : ComponentActivity() {
    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        setContent { CorrecteurApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CorrecteurApp(vm: CorrecteurViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    var vocabulary by remember { mutableStateOf("") }
    var protectedWord by remember { mutableStateOf("") }
    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }

    MaterialTheme(colorScheme = AppColors) {
        Scaffold(
            containerColor = AppColors.background,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(R.drawable.correcteur_logo),
                                contentDescription = "Logo CorrecteurIA",
                                modifier = Modifier.size(42.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("CorrecteurIA", fontWeight = FontWeight.ExtraBold)
                                Text(
                                    if (state.internetEnabled) "IA + Internet optionnel" else "IA privée hors connexion",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = GreenDark,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(listOf(Green, GreenDark, Red)),
                                RoundedCornerShape(24.dp)
                            )
                            .padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(R.drawable.correcteur_logo),
                                contentDescription = null,
                                modifier = Modifier.size(92.dp)
                            )
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(
                                    "CORRECTEUR INTELLIGENT",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Écrivez mieux.
À votre façon.",
                                    color = Color.White,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    "Correction • reformulation • mémoire",
                                    color = Color.White.copy(alpha = 0.9f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = state.input,
                    onValueChange = vm::setInput,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 210.dp),
                    label = { Text("Votre texte") },
                    placeholder = { Text("Écrivez ou collez votre texte ici…") },
                    shape = RoundedCornerShape(18.dp)
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = vm::correct,
                        modifier = Modifier.weight(1f),
                        enabled = state.input.isNotBlank() && !state.busy,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Green)
                    ) {
                        Text("✓ Corriger")
                    }
                    Button(
                        onClick = vm::rewrite,
                        modifier = Modifier.weight(1f),
                        enabled = state.input.isNotBlank() && !state.busy,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Red)
                    ) {
                        Text("✨ Reformuler")
                    }
                }

                if (state.busy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth(), color = Green)
                }

                if (state.output.isNotBlank()) {
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = SoftGreen)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Résultat", fontWeight = FontWeight.Bold, color = GreenDark)
                            Spacer(Modifier.height(6.dp))
                            Text(state.output, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = vm::useResult) { Text("Remplacer") }
                        OutlinedButton(onClick = vm::clearOutput) { Text("Effacer") }
                    }
                }

                Text(state.message, style = MaterialTheme.typography.bodySmall, color = GreenDark)

                HorizontalDivider()
                Text("✨ Reformulation IA avancée", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Red)
                Text(
                    "Choisissez le ton, puis le niveau de transformation. L'IA conserve le sens tout en variant les mots et la structure. Les styles s'appuient sur les modes de réécriture disponibles sur l'appareil.",
                    style = MaterialTheme.typography.bodySmall
                )

                Text("🎯 Style", fontWeight = FontWeight.Bold, color = GreenDark)
                val styles = listOf(
                    "Standard", "Humanisé", "Naturel", "Professionnel",
                    "Formel", "Simple", "Chaleureux", "Amical",
                    "Concis", "Détaillé", "Créatif"
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    styles.chunked(4).forEach { rowStyles ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowStyles.forEach { style ->
                                FilterChip(
                                    selected = state.style == style,
                                    onClick = { vm.setStyle(style) },
                                    label = { Text(style) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SoftGreen,
                                        selectedLabelColor = GreenDark
                                    )
                                )
                            }
                        }
                    }
                }

                Text("⚙️ Intensité", fontWeight = FontWeight.Bold, color = GreenDark)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Légère", "Équilibré", "Fort").forEach { intensity ->
                        FilterChip(
                            selected = state.rewriteIntensity == intensity,
                            onClick = { vm.setRewriteIntensity(intensity) },
                            label = { Text(intensity) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SoftRed,
                                selectedLabelColor = Red
                            )
                        )
                    }
                }

                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftRed)
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Correction Internet", fontWeight = FontWeight.Bold, color = Red)
                            Text(
                                if (state.internetEnabled) "Internet si disponible, puis secours local."
                                else "Aucun texte n’est envoyé en ligne.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = state.internetEnabled,
                            onCheckedChange = vm::setInternetEnabled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Red
                            )
                        )
                    }
                }

                HorizontalDivider()
                Text("🧠 Mémoire personnelle", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = GreenDark)
                Text("Vocabulaire, mots protégés et corrections préférées sont mémorisés localement.")

                OutlinedTextField(
                    value = vocabulary,
                    onValueChange = { vocabulary = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Ajouter au vocabulaire") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
                Button(
                    onClick = {
                        if (vocabulary.isNotBlank()) {
                            vm.addVocabulary(vocabulary)
                            vocabulary = ""
                        }
                    },
                    enabled = vocabulary.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Green)
                ) { Text("➕ Mémoriser ce mot") }

                if (state.vocabulary.isNotEmpty()) {
                    Text("Vocabulaire : " + state.vocabulary.joinToString(" • "))
                    TextButton(onClick = { vm.removeVocabulary(state.vocabulary.last()) }) {
                        Text("Retirer le dernier")
                    }
                }

                OutlinedTextField(
                    value = protectedWord,
                    onValueChange = { protectedWord = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Mot à ne jamais corriger") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
                Button(
                    onClick = {
                        if (protectedWord.isNotBlank()) {
                            vm.addProtectedWord(protectedWord)
                            protectedWord = ""
                        }
                    },
                    enabled = protectedWord.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Red)
                ) { Text("🛡️ Protéger ce mot") }

                if (state.protectedWords.isNotEmpty()) {
                    Text("Protégés : " + state.protectedWords.joinToString(" • "))
                    TextButton(onClick = { vm.removeProtectedWord(state.protectedWords.last()) }) {
                        Text("Retirer le dernier")
                    }
                }

                Text("🔁 Correction préférée", fontWeight = FontWeight.Bold, color = GreenDark)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = from,
                        onValueChange = { from = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Avant") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = to,
                        onValueChange = { to = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Après") },
                        singleLine = true
                    )
                }
                Button(
                    onClick = {
                        if (from.isNotBlank() && to.isNotBlank()) {
                            vm.learnReplacement(from, to)
                            from = ""
                            to = ""
                        }
                    },
                    enabled = from.isNotBlank() && to.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Green)
                ) { Text("🧠 Apprendre cette correction") }

                if (state.learnedReplacements.isNotEmpty()) {
                    Text("Préférences mémorisées :")
                    state.learnedReplacements.entries.take(8).forEach { entry ->
                        Text("• " + entry.key + " → " + entry.value)
                    }
                    TextButton(onClick = { vm.removeReplacement(state.learnedReplacements.keys.last()) }) {
                        Text("Oublier la dernière")
                    }
                }

                HorizontalDivider()
                Text("🗂️ Historique", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = GreenDark)
                Text("Les 20 dernières corrections et reformulations restent sur l’appareil.")
                state.history.take(5).forEach { item ->
                    OutlinedButton(
                        onClick = { vm.loadHistory(item) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(item.mode + " — " + item.input.take(70))
                    }
                }

                Row {
                    if (state.history.isNotEmpty()) {
                        TextButton(onClick = vm::clearHistory) { Text("Effacer l’historique", color = Red) }
                    }
                    TextButton(onClick = vm::clearPersonalMemory) { Text("Effacer préférences") }
                    TextButton(onClick = vm::clearMemory) { Text("Tout effacer", color = Red) }
                }

                HorizontalDivider()
                Text("🔒 Confidentialité", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = GreenDark)
                Text("La mémoire avancée reste locale. Le bouton Internet concerne la correction LanguageTool en ligne. La reformulation IA utilise le moteur GenAI disponible sur l’appareil.")
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}
